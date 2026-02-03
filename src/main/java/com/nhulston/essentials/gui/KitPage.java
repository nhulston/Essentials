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
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.Essentials;
import com.nhulston.essentials.managers.KitManager;
import com.nhulston.essentials.models.Kit;
import com.nhulston.essentials.util.ConfigManager;
import com.nhulston.essentials.util.CooldownUtil;
import com.nhulston.essentials.util.MessageManager;
import com.nhulston.essentials.util.Msg;

/**
 * A GUI page for selecting kits.
 */
public class KitPage extends InteractiveCustomUIPage<KitPage.KitPageData> {
    private static final String COOLDOWN_BYPASS_PERMISSION = "essentials.kit.cooldown.bypass";

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
        commandBuilder.append("Pages/Essentials_KitPage.ui");

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
        
        if (allKits.isEmpty()) {
            // No kits available - could add a "No kits available" message element
            return;
        }

        // Create a grid layout with 3 kits per row
        int kitsPerRow = 3;
        int totalRows = (int) Math.ceil((double) allKits.size() / kitsPerRow);

        for (int row = 0; row < totalRows; row++) {
            // Create a row group for this row
            commandBuilder.appendInline("#KitRows", 
                "Group { LayoutMode: Left; Anchor: (Height: 128); Padding: (Horizontal: 4); }");
            
            String rowSelector = "#KitRows[" + row + "]";
            
            // Calculate start and end index for this row
            int startIdx = row * kitsPerRow;
            int endIdx = Math.min(startIdx + kitsPerRow, allKits.size());
            
            for (int col = 0; col < (endIdx - startIdx); col++) {
                int kitIdx = startIdx + col;
                Kit kit = allKits.get(kitIdx);
                
                // Append kit entry to this row
                commandBuilder.append(rowSelector, "Pages/Essentials_KitEntry.ui");
                
                // Select the kit card within the row
                String cardSelector = rowSelector + "[" + col + "]";
                
                commandBuilder.set(cardSelector + " #Name.Text", kit.getDisplayName());

                // Check permission and cooldown status
                String permission = "essentials.kit." + kit.getId();
                boolean hasPermission = PermissionsModule.get().hasPermission(playerRef.getUuid(), permission);

                String status;
                if (!hasPermission) {
                    status = "No access";
                } else {
                    long remainingCooldown = kitManager.getRemainingCooldown(playerRef.getUuid(), kit.getId());
                    if (remainingCooldown > 0) {
                        status = CooldownUtil.formatCooldown(remainingCooldown);
                    } else {
                        status = "Ready";
                    }
                }
                commandBuilder.set(cardSelector + " #Status.Text", status);

                eventBuilder.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        cardSelector,
                        EventData.of("Kit", kit.getId())
                );
            }
        }
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
