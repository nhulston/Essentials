package com.nhulston.essentials.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.util.ColorUtil;
import com.nhulston.essentials.util.ConfigManager;
import com.nhulston.essentials.util.Log;
import com.nhulston.essentials.util.VersionChecker;

import javax.annotation.Nonnull;

/**
 * Notifies admins (players with * permission) when a plugin update is available.
 */
public class UpdateNotifyEvent {
    private static final String ADMIN_PERMISSION = "*";
    
    private final VersionChecker versionChecker;
    private final ConfigManager configManager;

    public UpdateNotifyEvent(@Nonnull VersionChecker versionChecker, @Nonnull ConfigManager configManager) {
        this.versionChecker = versionChecker;
        this.configManager = configManager;
    }

    public void register(@Nonnull EventRegistry eventRegistry) {
        eventRegistry.registerGlobal(PlayerReadyEvent.class, event -> {
            // Check if update notifications are enabled
            if (!configManager.isUpdateNotifyEnabled()) {
                return;
            }
            
            if (!versionChecker.isUpdateAvailable()) {
                return;
            }

            // Get ref from event - PlayerReadyEvent is fired on scheduler thread
            Ref<EntityStore> ref = event.getPlayerRef();
            if (!ref.isValid()) {
                return;
            }

            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();

            // Execute on world thread to safely access components
            world.execute(() -> {
                if (!ref.isValid()) {
                    return;
                }

                // Now we're on the world thread - safe to access components
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef == null) {
                    return;
                }

                // Check if player is admin (has * permission)
                if (!PermissionsModule.get().hasPermission(playerRef.getUuid(), ADMIN_PERMISSION)) {
                    return;
                }

                String latestVersion = versionChecker.getLatestVersion();
                String currentVersion = versionChecker.getCurrentVersion();

                // Send update notification
                String downloadUrl = "https://curseforge.com/hytale/mods/essentials-core";

                playerRef.sendMessage(ColorUtil.colorize("&8[&6Essentials&8] &eA new version is available!"));
                playerRef.sendMessage(ColorUtil.colorize("&7Current: &f" + currentVersion + " &8| &7Latest: &a" + latestVersion));
                playerRef.sendMessage(Message.join(
                        Message.raw("Download: ").color("#AAAAAA"),
                        Message.raw(downloadUrl).color("#55FFFF").link(downloadUrl)
                ));
            });
        });

        Log.info("Update notify event registered.");
    }
}
