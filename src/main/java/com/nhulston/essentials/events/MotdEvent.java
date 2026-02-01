package com.nhulston.essentials.events;

import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.nhulston.essentials.util.ColorUtil;
import com.nhulston.essentials.util.ConfigManager;

import javax.annotation.Nonnull;

/**
 * Displays the Message of the Day to players on join.
 */
public class MotdEvent {
    private final ConfigManager configManager;

    public MotdEvent(@Nonnull ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void register(@Nonnull EventRegistry eventRegistry) {
        eventRegistry.registerGlobal(PlayerConnectEvent.class, event -> {
            if (!configManager.isMotdEnabled()) {
                return;
            }

            String message = configManager.getMotdMessage();
            String playerName = event.getPlayerRef().getUsername();

            // Replace placeholder
            message = message.replace("%player%", playerName);

            // Normalize line endings (remove \r from Windows line endings)
            message = message.replace("\r", "");

            // Split by newlines and send each line
            String[] lines = message.split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    event.getPlayerRef().sendMessage(ColorUtil.colorize(line));
                }
            }
        });
    }
}
