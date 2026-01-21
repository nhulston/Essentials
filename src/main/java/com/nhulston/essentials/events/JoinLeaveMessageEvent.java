package com.nhulston.essentials.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.util.ColorUtil;
import com.nhulston.essentials.util.ConfigManager;
import com.nhulston.essentials.util.Log;
import com.nhulston.essentials.util.StorageManager;

import javax.annotation.Nonnull;

/**
 * Broadcasts join and leave messages to all players.
 * Differentiates between first-time joins and returning players.
 * 
 * Thread Safety:
 * - PlayerReadyEvent fires on any thread (uses registerGlobal)
 * - Must use world.execute() to access store.getComponent() for PlayerRef
 * - Universe.get().sendMessage() is thread-safe (can call from world thread)
 * - StorageManager.hasPlayerJoined() is thread-safe (ConcurrentHashMap)
 */
public class JoinLeaveMessageEvent {
    private final ConfigManager configManager;
    private final StorageManager storageManager;

    public JoinLeaveMessageEvent(@Nonnull ConfigManager configManager, 
                                 @Nonnull StorageManager storageManager) {
        this.configManager = configManager;
        this.storageManager = storageManager;
    }

    public void register(@Nonnull EventRegistry eventRegistry) {
        // Disable Hytale's default join message
        eventRegistry.registerGlobal(AddPlayerToWorldEvent.class, event -> {
            event.setBroadcastJoinMessage(false);
        });

        // Join messages - PlayerReadyEvent fires when player is ready in world
        eventRegistry.registerGlobal(PlayerReadyEvent.class, event -> {
            if (!configManager.isJoinMessageEnabled()) {
                return;
            }

            Ref<EntityStore> ref = event.getPlayerRef();
            if (!ref.isValid()) {
                return;
            }

            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            if (world == null) {
                return;
            }

            // Execute on world thread to access PlayerRef component safely
            world.execute(() -> {
                if (!ref.isValid()) {
                    return;
                }

                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef == null) {
                    return;
                }

                String playerName = playerRef.getUsername();
                boolean isFirstJoin = !storageManager.hasPlayerJoined(playerRef.getUuid());

                // Choose appropriate message
                String message;
                if (isFirstJoin) {
                    message = configManager.getFirstJoinMessage();
                } else {
                    message = configManager.getJoinMessage();
                }

                // Replace placeholder
                message = message.replace("%player%", playerName);

                // Broadcast to all players (thread-safe, can call from any thread)
                Universe.get().sendMessage(ColorUtil.colorize(message));
            });
        });

        // Leave messages - PlayerDisconnectEvent fires when player disconnects
        eventRegistry.registerGlobal(PlayerDisconnectEvent.class, event -> {
            if (!configManager.isLeaveMessageEnabled()) {
                return;
            }

            PlayerRef playerRef = event.getPlayerRef();
            String playerName = playerRef.getUsername();
            String message = configManager.getLeaveMessage();

            // Replace placeholder
            message = message.replace("%player%", playerName);

            // Broadcast to all remaining players (thread-safe)
            Universe.get().sendMessage(ColorUtil.colorize(message));
        });

        Log.info("Join/leave message broadcasts registered.");
    }
}
