package com.nhulston.essentials.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.Essentials;
import com.nhulston.essentials.gui.common.GridEntry;
import com.nhulston.essentials.gui.common.GridPageBuilder;
import com.nhulston.essentials.managers.KitManager;
import com.nhulston.essentials.models.Kit;
import com.nhulston.essentials.util.ConfigManager;
import com.nhulston.essentials.util.CooldownUtil;
import com.nhulston.essentials.util.MessageManager;
import com.nhulston.essentials.util.Msg;

/**
 * A GUI page for selecting kits.
 * Uses shared GridPageBuilder for consistent layout with warps and homes.
 */
public class KitPage extends InteractiveCustomUIPage<KitPage.KitPageData> {
    private static final String COOLDOWN_BYPASS_PERMISSION = "essentials.kit.cooldown.bypass";
    
    /** Kit uses legacy UI files with different selectors */
    private static final String KIT_PAGE_UI = "Pages/Essentials_KitPage.ui";
    private static final String KIT_ENTRY_UI = "Pages/Essentials_KitEntry.ui";
    private static final String KIT_SELECTION_KEY = "Kit";
    private static final String KIT_ROWS_SELECTOR = "#KitRows";

    private final KitManager kitManager;
    private final ConfigManager configManager;
    private final MessageManager messages;

    public KitPage(@Nonnull PlayerRef playerRef, @Nonnull KitManager kitManager, @Nonnull ConfigManager configManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, KitPageData.CODEC);
        this.kitManager = kitManager;
        this.configManager = configManager;
        this.messages = Essentials.getInstance().getMessageManager();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        
        List<GridEntry> entries = buildKitEntries();
        
        // Use GridPageBuilder with kit-specific UI files and selectors
        GridPageBuilder.build(
                commandBuilder, eventBuilder, entries,
                GridPageBuilder.DEFAULT_COLUMNS,
                KIT_PAGE_UI, KIT_ENTRY_UI, KIT_SELECTION_KEY, KIT_ROWS_SELECTOR
        );
    }
    
    /**
     * Builds the list of kit entries, applying filters and calculating status.
     */
    private List<GridEntry> buildKitEntries() {
        List<Kit> allKits = new ArrayList<>(kitManager.getKits());
        
        // Filter out starter kit if configured
        if (configManager.isStarterKitEnabled()) {
            String starterKitName = configManager.getStarterKitName();
            if (!starterKitName.isEmpty()) {
                allKits.removeIf(kit -> kit.getId().equalsIgnoreCase(starterKitName));
            }
        }
        
        // Filter out kits the player doesn't have permission for (if configured)
        if (configManager.isKitsHideNoPermission()) {
            allKits.removeIf(kit -> {
                String permission = "essentials.kit." + kit.getId();
                return !PermissionsModule.get().hasPermission(playerRef.getUuid(), permission);
            });
        }
        
        // Convert to GridEntry list
        List<GridEntry> entries = new ArrayList<>();
        for (Kit kit : allKits) {
            String status = calculateKitStatus(kit);
            entries.add(new GridEntry(kit.getId(), kit.getDisplayName(), status));
        }
        
        return entries;
    }
    
    /**
     * Calculates the status text for a kit (permission check and cooldown).
     */
    private String calculateKitStatus(@Nonnull Kit kit) {
        String permission = "essentials.kit." + kit.getId();
        boolean hasPermission = PermissionsModule.get().hasPermission(playerRef.getUuid(), permission);

        if (!hasPermission) {
            return "No access";
        }
        
        long remainingCooldown = kitManager.getRemainingCooldown(playerRef.getUuid(), kit.getId());
        if (remainingCooldown > 0) {
            return CooldownUtil.formatCooldown(remainingCooldown);
        }
        
        return "Ready";
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull KitPageData data) {
        if (data.kit == null || data.kit.isEmpty()) {
            return;
        }

        Kit kit = kitManager.getKit(data.kit);
        if (kit == null) {
            Msg.send(playerRef, messages.get("gui.kit.not-found"));
            this.close();
            return;
        }

        // Check permission
        String permission = "essentials.kit." + kit.getId();
        if (!PermissionsModule.get().hasPermission(playerRef.getUuid(), permission)) {
            Msg.send(playerRef, messages.get("gui.kit.no-permission"));
            this.close();
            return;
        }

        // Check cooldown (unless player has bypass permission)
        boolean canBypassCooldown = PermissionsModule.get().hasPermission(playerRef.getUuid(), COOLDOWN_BYPASS_PERMISSION);
        if (!canBypassCooldown) {
            long remainingCooldown = kitManager.getRemainingCooldown(playerRef.getUuid(), kit.getId());
            if (remainingCooldown > 0) {
                Msg.send(playerRef, messages.get("gui.kit.cooldown", Map.of("time", CooldownUtil.formatCooldown(remainingCooldown))));
                this.close();
                return;
            }
        }

        // Get player inventory
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            Msg.send(playerRef, messages.get("gui.kit.inventory-error"));
            this.close();
            return;
        }

        Inventory inventory = player.getInventory();
        if (inventory == null) {
            Msg.send(playerRef, messages.get("gui.kit.inventory-error"));
            this.close();
            return;
        }

        // Apply kit (overflow items will be dropped on the ground)
        KitManager.applyKit(kit, inventory, ref, store);

        // Sync inventory changes to client
        player.sendInventory();

        // Set cooldown
        if (kit.getCooldown() > 0) {
            kitManager.setKitUsed(playerRef.getUuid(), kit.getId());
        }

        Msg.send(playerRef, messages.get("gui.kit.received", Map.of("kit", kit.getDisplayName())));
        this.close();
    }

    /**
     * Event data for kit selection.
     * Uses "Kit" as the codec key for backward compatibility with existing UI files.
     */
    public static class KitPageData {
        public static final BuilderCodec<KitPageData> CODEC = BuilderCodec.builder(KitPageData.class, KitPageData::new)
                .append(new KeyedCodec<>("Kit", Codec.STRING), (data, s) -> data.kit = s, data -> data.kit)
                .add()
                .build();

        private String kit;

        public String getKit() {
            return kit;
        }
    }
}
