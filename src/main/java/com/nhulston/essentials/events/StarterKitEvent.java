package com.nhulston.essentials.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.managers.KitManager;
import com.nhulston.essentials.models.Kit;
import com.nhulston.essentials.util.ConfigManager;
import com.nhulston.essentials.util.Log;
import com.nhulston.essentials.util.StorageManager;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gives new players a starter kit when they first join the server.
 */
public class StarterKitEvent {
    private final KitManager kitManager;
    private final ConfigManager configManager;
    private final StorageManager storageManager;
    
    // Track players who need the starter kit (detected during PlayerConnectEvent, applied during PlayerReadyEvent)
    private final Set<UUID> pendingStarterKits = ConcurrentHashMap.newKeySet();

    public StarterKitEvent(@Nonnull KitManager kitManager, @Nonnull ConfigManager configManager,
                           @Nonnull StorageManager storageManager) {
        this.kitManager = kitManager;
        this.configManager = configManager;
        this.storageManager = storageManager;
    }

    public void register(@Nonnull EventRegistry eventRegistry) {
        // Phase 1: Detect first-time joins before SpawnTeleportEvent marks them as joined
        eventRegistry.registerGlobal(PlayerConnectEvent.class, event -> {
            if (!configManager.isStarterKitEnabled()) {
                return;
            }

            String kitName = configManager.getStarterKitName();
            if (kitName.isEmpty()) {
                return;
            }

            PlayerRef playerRef = event.getPlayerRef();
            UUID uuid = playerRef.getUuid();

            // Check if this is a first-time join (before SpawnTeleportEvent marks them)
            if (storageManager.hasPlayerJoined(uuid)) {
                return;
            }

            // Check if the starter kit exists
            Kit kit = kitManager.getKit(kitName);
            if (kit == null) {
                Log.error("Starter kit '" + kitName + "' not found in kits.toml! New players will not receive a starter kit.");
                return;
            }

            // Check if player has permission for this kit
            String permission = "essentials.kit." + kit.getId();
            if (!PermissionsModule.get().hasPermission(uuid, permission)) {
                return;
            }

            // Mark for starter kit delivery
            pendingStarterKits.add(uuid);
        });

        // Phase 2: Give the kit once player is ready (has inventory)
        eventRegistry.registerGlobal(PlayerReadyEvent.class, event -> {
            // Get ref from event - PlayerReadyEvent is fired on scheduler thread
            Ref<EntityStore> ref = event.getPlayerRef();
            if (!ref.isValid()) {
                return;
            }

            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();

            String kitName = configManager.getStarterKitName();
            Kit kit = kitManager.getKit(kitName);
            if (kit == null) {
                return;
            }

            // Execute on the player's world thread for thread safety
            world.execute(() -> {
                if (!ref.isValid()) {
                    return;
                }

                // Now we're on the world thread - safe to access components
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef == null) {
                    return;
                }

                UUID uuid = playerRef.getUuid();
                
                // Check if this player needs a starter kit
                if (!pendingStarterKits.remove(uuid)) {
                    return;
                }

                Player player = store.getComponent(ref, Player.getComponentType());
                if (player == null) {
                    return;
                }

                Inventory inventory = player.getInventory();
                if (inventory == null) {
                    return;
                }

                KitManager.applyKit(kit, inventory, ref, store);
                player.sendInventory();

                Log.info("Gave starter kit '" + kit.getId() + "' to new player " + playerRef.getUsername());
            });
        });
    }
}
