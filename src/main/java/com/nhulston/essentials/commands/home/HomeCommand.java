package com.nhulston.essentials.commands.home;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.Essentials;
import com.nhulston.essentials.managers.BackManager;
import com.nhulston.essentials.managers.HomeManager;
import com.nhulston.essentials.managers.TeleportManager;
import com.nhulston.essentials.models.Home;
import com.nhulston.essentials.util.MessageManager;
import com.nhulston.essentials.util.Msg;
import com.nhulston.essentials.util.StorageManager;
import com.nhulston.essentials.util.TeleportUtil;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;

public class HomeCommand extends AbstractPlayerCommand {
    private final HomeManager homeManager;
    private final TeleportManager teleportManager;
    private final BackManager backManager;
    private final MessageManager messages;

    public HomeCommand(@Nonnull HomeManager homeManager, @Nonnull TeleportManager teleportManager,
                      @Nonnull BackManager backManager) {
        super("home", "Teleport to your home");
        this.homeManager = homeManager;
        this.teleportManager = teleportManager;
        this.backManager = backManager;
        this.messages = Essentials.getInstance().getMessageManager();

        addAliases("homes");
        requirePermission("essentials.home");
        addUsageVariant(new HomeNamedCommand(homeManager, teleportManager, backManager));
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World currentWorld) {
        UUID playerUuid = playerRef.getUuid();
        Map<String, Home> homes = homeManager.getHomes(playerUuid);

        if (homes.isEmpty()) {
            Msg.send(context, messages.get("commands.home.no-homes"));
            return;
        }

        if (homes.size() == 1) {
            String homeName = homes.keySet().iterator().next();
            doTeleportToHome(context, store, ref, playerRef, currentWorld, homeName, homeManager, teleportManager, backManager, messages);
        } else {
            Msg.send(context, messages.get("commands.home.list-prefix") + ": " + String.join(", ", homes.keySet()));
        }
    }

    static void doTeleportToHome(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                                 @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef,
                                 @Nonnull World currentWorld, @Nonnull String homeName,
                                 @Nonnull HomeManager homeManager, @Nonnull TeleportManager teleportManager,
                                 @Nonnull BackManager backManager, @Nonnull MessageManager messages) {
        Home home = homeManager.getHome(playerRef.getUuid(), homeName);
        if (home == null) {
            Msg.send(context, messages.get("commands.home.not-found", Map.of("home", homeName)));
            return;
        }

        backManager.setBackLocation(store, ref, playerRef, currentWorld);
        Vector3d startPosition = TeleportUtil.getStartPosition(store, ref);
        if (startPosition == null) {
            Msg.send(context, messages.get("errors.generic"));
            return;
        }

        teleportManager.queueTeleport(
            playerRef, ref, store, startPosition,
            home.getWorld(), home.getX(), home.getY(), home.getZ(), home.getYaw(), home.getPitch(),
            messages.get("commands.home.teleported", Map.of("home", homeName))
        );
    }

    /**
     * Handles /home <name> and /home <player>:<home> syntax.
     * Also handles /home <player> to list another player's homes.
     */
    private static class HomeNamedCommand extends AbstractPlayerCommand {
        private static final String OTHERS_PERMISSION = "essentials.home.others";
        
        private final HomeManager homeManager;
        private final TeleportManager teleportManager;
        private final BackManager backManager;
        private final RequiredArg<String> nameArg;

        HomeNamedCommand(@Nonnull HomeManager homeManager, @Nonnull TeleportManager teleportManager,
                        @Nonnull BackManager backManager) {
            super("Teleport to a specific home or view another player's homes");
            this.homeManager = homeManager;
            this.teleportManager = teleportManager;
            this.backManager = backManager;
            this.nameArg = withRequiredArg("name", "Home name or player:home", ArgTypes.STRING);
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            MessageManager messages = Essentials.getInstance().getMessageManager();
            StorageManager storageManager = Essentials.getInstance().getStorageManager();
            String arg = context.get(nameArg);
            
            // Check for player:home syntax
            if (arg.contains(":")) {
                handleOtherPlayerHome(context, store, ref, playerRef, world, arg, messages, storageManager);
                return;
            }
            
            // Check if this could be a player name (user has permission to view others' homes)
            if (PermissionsModule.get().hasPermission(playerRef.getUuid(), OTHERS_PERMISSION)) {
                UUID targetUuid = storageManager.getUuidByUsername(arg);
                if (targetUuid != null && !targetUuid.equals(playerRef.getUuid())) {
                    // It's a valid player name, list their homes
                    listOtherPlayerHomes(context, targetUuid, arg, messages);
                    return;
                }
            }
            
            // Default: treat as own home name
            doTeleportToHome(context, store, ref, playerRef, world, arg, homeManager, teleportManager, backManager, messages);
        }
        
        private void handleOtherPlayerHome(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef,
                                           @Nonnull World world, @Nonnull String arg,
                                           @Nonnull MessageManager messages, @Nonnull StorageManager storageManager) {
            // Check permission
            if (!PermissionsModule.get().hasPermission(playerRef.getUuid(), OTHERS_PERMISSION)) {
                Msg.send(context, messages.get("no-permission"));
                return;
            }
            
            // Split player:home
            String[] parts = arg.split(":", 2);
            String targetName = parts[0];
            String homeName = parts.length > 1 ? parts[1] : "";
            
            if (targetName.isEmpty()) {
                Msg.send(context, messages.get("commands.home.player-not-found", Map.of("player", "")));
                return;
            }
            
            // Look up target player UUID
            UUID targetUuid = storageManager.getUuidByUsername(targetName);
            if (targetUuid == null) {
                Msg.send(context, messages.get("commands.home.player-not-found", Map.of("player", targetName)));
                return;
            }
            
            // If no home specified (e.g., "player:"), list their homes
            if (homeName.isEmpty()) {
                listOtherPlayerHomes(context, targetUuid, targetName, messages);
                return;
            }
            
            // Get the home
            Home home = homeManager.getHome(targetUuid, homeName);
            if (home == null) {
                Msg.send(context, messages.get("commands.home.other-not-found", 
                    Map.of("player", targetName, "home", homeName)));
                return;
            }
            
            backManager.setBackLocation(store, ref, playerRef, world);
            Vector3d startPosition = TeleportUtil.getStartPosition(store, ref);
            if (startPosition == null) {
                Msg.send(context, messages.get("errors.generic"));
                return;
            }
            
            teleportManager.queueTeleport(
                playerRef, ref, store, startPosition,
                home.getWorld(), home.getX(), home.getY(), home.getZ(), home.getYaw(), home.getPitch(),
                messages.get("commands.home.other-teleported", Map.of("player", targetName, "home", homeName))
            );
        }
        
        private void listOtherPlayerHomes(@Nonnull CommandContext context, @Nonnull UUID targetUuid,
                                          @Nonnull String targetName, @Nonnull MessageManager messages) {
            Map<String, Home> homes = homeManager.getHomes(targetUuid);
            
            if (homes.isEmpty()) {
                Msg.send(context, messages.get("commands.home.other-no-homes", Map.of("player", targetName)));
                return;
            }
            
            Msg.send(context, messages.get("commands.home.other-list-prefix", Map.of("player", targetName)) 
                + ": " + String.join(", ", homes.keySet()));
        }
    }
}
