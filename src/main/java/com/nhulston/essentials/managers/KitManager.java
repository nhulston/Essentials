package com.nhulston.essentials.managers;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.models.Kit;
import com.nhulston.essentials.models.KitItem;
import com.nhulston.essentials.models.PlayerData;
import com.nhulston.essentials.util.Log;
import com.nhulston.essentials.util.StorageManager;
import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Manages kit loading, saving, and application.
 */
public class KitManager {
    private final Path kitsPath;
    private final StorageManager storageManager;
    private final Map<String, Kit> kits;
    private String fileHeader;

    public KitManager(@Nonnull Path dataFolder, @Nonnull StorageManager storageManager) {
        this.kitsPath = dataFolder.resolve("kits.toml");
        this.storageManager = storageManager;
        this.kits = new LinkedHashMap<>();
        this.fileHeader = "";
        load();
    }

    /**
     * Loads kits from kits.toml
     */
    private void load() {
        if (!Files.exists(kitsPath)) {
            createDefault();
        }

        try {
            // Read and cache the file header (everything before [kits])
            String fileContent = Files.readString(kitsPath);
            int kitsIndex = fileContent.indexOf("[kits]");
            if (kitsIndex > 0) {
                fileHeader = fileContent.substring(0, kitsIndex);
            }

            TomlParseResult config = Toml.parse(kitsPath);

            if (config.hasErrors()) {
                config.errors().forEach(error -> Log.error("Kits config error: " + error.toString()));
                Log.warning("Kit loading failed due to config errors.");
                return;
            }

            TomlTable kitsTable = config.getTable("kits");
            if (kitsTable == null) {
                Log.info("No kits configured in kits.toml");
                return;
            }

            kits.clear();
            for (String kitId : kitsTable.keySet()) {
                TomlTable kitTable = kitsTable.getTable(kitId);
                if (kitTable == null) continue;

                String displayName = kitTable.getString("display-name", () -> kitId);
                int cooldown = Math.toIntExact(kitTable.getLong("cooldown", () -> 0L));
                String type = kitTable.getString("type", () -> "add");

                List<KitItem> items = new ArrayList<>();
                TomlArray itemsArray = kitTable.getArray("items");
                if (itemsArray != null) {
                    for (int i = 0; i < itemsArray.size(); i++) {
                        TomlTable itemTable = itemsArray.getTable(i);
                        if (itemTable == null) continue;

                        String itemId = itemTable.getString("item-id");
                        if (itemId == null) continue;

                        int quantity = Math.toIntExact(itemTable.getLong("quantity", () -> 1L));
                        String section = itemTable.getString("section", () -> "hotbar");
                        int slot = Math.toIntExact(itemTable.getLong("slot", () -> 0L));

                        items.add(new KitItem(itemId, quantity, section, slot));
                    }
                }

                Kit kit = new Kit(kitId.toLowerCase(), displayName, cooldown, type, items);
                kits.put(kitId.toLowerCase(), kit);
                Log.info("Loaded kit: " + kitId + " with " + items.size() + " items");
            }

            Log.info("Loaded " + kits.size() + " kits from kits.toml");
        } catch (IOException e) {
            Log.error("Failed to load kits: " + e.getMessage());
        }
    }

    /**
     * Reloads kits from kits.toml
     */
    public void reload() {
        Log.info("Reloading kits...");
        load();
    }

    /**
     * Creates default kits.toml from resources
     */
    private void createDefault() {
        try {
            Files.createDirectories(kitsPath.getParent());

            try (InputStream is = getClass().getClassLoader().getResourceAsStream("kits.toml")) {
                if (is != null) {
                    Files.copy(is, kitsPath);
                    Log.info("Created default kits.toml");
                } else {
                    Log.error("Could not find kits.toml in resources");
                }
            }
        } catch (IOException e) {
            Log.error("Failed to create default kits.toml: " + e.getMessage());
        }
    }

    /**
     * Creates a new kit and saves it to kits.toml
     */
    public void createKit(@Nonnull String kitId, @Nonnull List<KitItem> items) {
        String id = kitId.toLowerCase();
        String displayName = capitalize(kitId);
        
        Kit kit = new Kit(id, displayName, 0, "add", items);
        kits.put(id, kit);
        
        saveKitAsync(kit);
    }

    /**
     * Saves a kit to kits.toml (appends to existing file)
     */
    private void saveKitAsync(@Nonnull Kit kit) {
        CompletableFuture.runAsync(() -> {
            try {
                StringBuilder toml = new StringBuilder();
                
                // Read existing content
                String existing = "";
                if (Files.exists(kitsPath)) {
                    existing = Files.readString(kitsPath);
                }
                
                // Build kit TOML
                toml.append("\n[kits.").append(kit.getId()).append("]\n");
                toml.append("display-name = \"").append(escapeToml(kit.getDisplayName())).append("\"\n");
                toml.append("cooldown = ").append(kit.getCooldown()).append("\n");
                toml.append("type = \"").append(kit.getType()).append("\"\n");
                
                for (KitItem item : kit.getItems()) {
                    toml.append("\n[[kits.").append(kit.getId()).append(".items]]\n");
                    toml.append("item-id = \"").append(escapeToml(item.itemId())).append("\"\n");
                    toml.append("quantity = ").append(item.quantity()).append("\n");
                    toml.append("section = \"").append(item.section()).append("\"\n");
                    toml.append("slot = ").append(item.slot()).append("\n");
                }
                
                // Append to file
                Files.writeString(kitsPath, existing + toml);
                Log.info("Saved kit: " + kit.getId());
            } catch (IOException e) {
                Log.error("Failed to save kit " + kit.getId() + ": " + e.getMessage());
            }
        });
    }

    /**
     * Gets a kit by ID
     */
    @Nullable
    public Kit getKit(@Nonnull String kitId) {
        return kits.get(kitId.toLowerCase());
    }

    /**
     * Gets all loaded kits
     */
    @Nonnull
    public Collection<Kit> getKits() {
        return kits.values();
    }

    /**
     * Deletes a kit and removes it from kits.toml
     */
    public void deleteKit(@Nonnull String kitId) {
        String id = kitId.toLowerCase();
        kits.remove(id);
        
        // Rewrite the entire kits.toml file without the deleted kit
        saveAllKitsAsync();
    }

    /**
     * Saves all kits to kits.toml (rewrites entire file)
     */
    private void saveAllKitsAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                StringBuilder toml = new StringBuilder();
                toml.append(fileHeader);
                toml.append("[kits]\n");

                for (Kit kit : kits.values()) {
                    toml.append("\n[kits.").append(kit.getId()).append("]\n");
                    toml.append("display-name = \"").append(escapeToml(kit.getDisplayName())).append("\"\n");
                    toml.append("cooldown = ").append(kit.getCooldown()).append("\n");
                    toml.append("type = \"").append(kit.getType()).append("\"\n");

                    for (KitItem item : kit.getItems()) {
                        toml.append("\n[[kits.").append(kit.getId()).append(".items]]\n");
                        toml.append("item-id = \"").append(escapeToml(item.itemId())).append("\"\n");
                        toml.append("quantity = ").append(item.quantity()).append("\n");
                        toml.append("section = \"").append(item.section()).append("\"\n");
                        toml.append("slot = ").append(item.slot()).append("\n");
                    }
                }

                Files.writeString(kitsPath, toml.toString());
                Log.info("Saved all kits to kits.toml");
            } catch (IOException e) {
                Log.error("Failed to save kits: " + e.getMessage());
            }
        });
    }

    /**
     * Checks if a player is on cooldown for a kit
     * @return remaining cooldown in seconds, or 0 if not on cooldown
     */
    public long getRemainingCooldown(@Nonnull UUID playerUuid, @Nonnull String kitId) {
        Kit kit = getKit(kitId);
        if (kit == null || kit.getCooldown() <= 0) {
            return 0;
        }

        PlayerData data = storageManager.getPlayerData(playerUuid);
        Long lastUsed = data.getKitCooldown(kitId.toLowerCase());
        if (lastUsed == null) {
            return 0;
        }

        long elapsed = (System.currentTimeMillis() - lastUsed) / 1000;
        long remaining = kit.getCooldown() - elapsed;
        return Math.max(0, remaining);
    }

    /**
     * Sets the cooldown timestamp for a player's kit usage
     */
    public void setKitUsed(@Nonnull UUID playerUuid, @Nonnull String kitId) {
        PlayerData data = storageManager.getPlayerData(playerUuid);
        data.setKitCooldown(kitId.toLowerCase(), System.currentTimeMillis());
        storageManager.savePlayerData(playerUuid);
    }

    /**
     * Applies a kit to the player's inventory. Overflow items are dropped on the ground.
     */
    public static void applyKit(@Nonnull Kit kit, @Nonnull Inventory inventory,
                                @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        if (kit.isReplaceMode()) {
            inventory.clear();
        }

        for (KitItem kitItem : kit.getItems()) {
            try {
                ItemStack itemStack = new ItemStack(kitItem.itemId(), kitItem.quantity());
                ItemStack remainder = addItemWithOverflow(inventory, kitItem, itemStack);

                if (remainder != null && !remainder.isEmpty()) {
                    ItemUtils.dropItem(ref, remainder, store);
                }
            } catch (Exception e) {
                Log.warning("Kit '" + kit.getId() + "' contains invalid item: " + kitItem.itemId() + " - " + e.getMessage());
            }
        }
    }

    @Nullable
    private static ItemStack addItemWithOverflow(@Nonnull Inventory inventory, @Nonnull KitItem kitItem, @Nonnull ItemStack itemStack) {
        ItemContainer container = getContainerBySection(inventory, kitItem.section());

        if (container != null) {
            short slot = (short) kitItem.slot();
            if (slot >= 0 && slot < container.getCapacity()) {
                ItemStack existing = container.getItemStack(slot);
                if (existing == null || existing.isEmpty()) {
                    container.setItemStackForSlot(slot, itemStack);
                    return null;
                }
            }
            String section = kitItem.section().toLowerCase();
            if (section.equals("armor") || section.equals("utility") || section.equals("tools")) {
                ItemStackTransaction transaction = inventory.getCombinedHotbarFirst().addItemStack(itemStack);
                return transaction.getRemainder();
            }
            ItemStackTransaction transaction = container.addItemStack(itemStack);
            return transaction.getRemainder();
        } else {
            ItemStackTransaction transaction = inventory.getCombinedHotbarFirst().addItemStack(itemStack);
            return transaction.getRemainder();
        }
    }

    @Nullable
    private static ItemContainer getContainerBySection(@Nonnull Inventory inventory, @Nonnull String section) {
        return switch (section.toLowerCase()) {
            case "hotbar" -> inventory.getHotbar();
            case "storage" -> inventory.getStorage();
            case "armor" -> inventory.getArmor();
            case "utility" -> inventory.getUtility();
            case "tools" -> inventory.getTools();
            default -> null;
        };
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1).toLowerCase();
    }

    private static String escapeToml(String str) {
        return str.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
