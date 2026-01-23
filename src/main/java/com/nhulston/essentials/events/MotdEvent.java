package com.nhulston.essentials.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
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
        eventRegistry.registerGlobal(PlayerReadyEvent.class, event -> {
            if (!configManager.isMotdEnabled()) {
                return;
            }

            Ref<EntityStore> ref = event.getPlayerRef();
            if (!ref.isValid()) {
                return;
            }

            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();

            world.execute(() -> {
                if (!ref.isValid()) {
                    return;
                }

                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef == null) {
                    return;
                }

                String message = configManager.getMotdMessage();
                String playerName = playerRef.getUsername();

                // Replace placeholder
                message = message.replace("%player%", playerName);

                // Normalize line endings (remove \r from Windows line endings)
                message = message.replace("\r", "");

                // Split by newlines and send each line
                String[] lines = message.split("\n");
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        playerRef.sendMessage(ColorUtil.colorize(line));
                    }
                }
            });
        });
    }
}
