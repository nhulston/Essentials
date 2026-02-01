package com.nhulston.essentials.events;

import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.nhulston.essentials.util.ColorUtil;
import com.nhulston.essentials.util.ConfigManager;
import com.nhulston.essentials.util.Log;
import com.nhulston.essentials.util.StorageManager;

import javax.annotation.Nonnull;

/**
 * Broadcasts join and leave messages to all players.
 * Differentiates between first-time joins and returning players.
 */
public class JoinLeaveEvent {
    private final ConfigManager configManager;
    private final StorageManager storageManager;

    public JoinLeaveEvent(@Nonnull ConfigManager configManager,
                          @Nonnull StorageManager storageManager) {
        this.configManager = configManager;
        this.storageManager = storageManager;
    }

    public void register(@Nonnull EventRegistry eventRegistry) {
        // Disable Hytale's default join message
        eventRegistry.registerGlobal(AddPlayerToWorldEvent.class, event -> {
            event.setBroadcastJoinMessage(false);
        });

        // Join messages - PlayerConnectEvent fires when player first connects
        eventRegistry.registerGlobal(PlayerConnectEvent.class, event -> {
            PlayerRef playerRef = event.getPlayerRef();
            String playerName = playerRef.getUsername();
            
            // Register username -> UUID mapping for offline player lookups
            storageManager.registerPlayer(playerName, playerRef.getUuid());
            
            if (!configManager.isJoinMessageEnabled()) {
                return;
            }

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

            // Broadcast to all players
            Universe.get().sendMessage(ColorUtil.colorize(message));
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
