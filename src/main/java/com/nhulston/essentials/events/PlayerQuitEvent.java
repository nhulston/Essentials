package com.nhulston.essentials.events;

import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.nhulston.essentials.commands.freecam.FreecamCommand;
import com.nhulston.essentials.commands.msg.MsgCommand;
import com.nhulston.essentials.commands.socialspy.SocialSpyCommand;
import com.nhulston.essentials.managers.BackManager;
import com.nhulston.essentials.managers.TeleportManager;
import com.nhulston.essentials.managers.TpaManager;
import com.nhulston.essentials.util.Log;
import com.nhulston.essentials.util.StorageManager;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Handles player disconnect cleanup.
 * Removes player data from all in-memory caches and managers.
 */
public class PlayerQuitEvent {
    private final StorageManager storageManager;
    private final TpaManager tpaManager;
    private final TeleportManager teleportManager;
    private final BackManager backManager;

    public PlayerQuitEvent(@Nonnull StorageManager storageManager,
                           @Nonnull TpaManager tpaManager,
                           @Nonnull TeleportManager teleportManager,
                           @Nonnull BackManager backManager) {
        this.storageManager = storageManager;
        this.tpaManager = tpaManager;
        this.teleportManager = teleportManager;
        this.backManager = backManager;
    }

    public void register(@Nonnull EventRegistry eventRegistry) {
        eventRegistry.registerGlobal(PlayerDisconnectEvent.class, event -> {
            UUID playerUuid = event.getPlayerRef().getUuid();

            // Save and clean up player data
            storageManager.savePlayerData(playerUuid);
            storageManager.unloadPlayer(playerUuid);
            tpaManager.onPlayerQuit(playerUuid);
            teleportManager.onPlayerQuit(playerUuid);
            backManager.onPlayerQuit(playerUuid);

            // Clean up static command/event data
            MsgCommand.onPlayerQuit(playerUuid);
            FreecamCommand.onPlayerQuit(playerUuid);
            SocialSpyCommand.onPlayerQuit(playerUuid);
            SpawnRegionTitleEvent.onPlayerQuit(playerUuid);
        });
        
        Log.info("Player disconnect cleanup registered.");
    }
}
