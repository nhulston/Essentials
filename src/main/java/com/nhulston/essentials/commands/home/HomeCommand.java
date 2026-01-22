package com.nhulston.essentials.commands.home;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.managers.BackManager;
import com.nhulston.essentials.managers.HomeManager;
import com.nhulston.essentials.managers.TeleportManager;
import com.nhulston.essentials.models.Home;
import com.nhulston.essentials.util.Msg;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;

public class HomeCommand extends AbstractPlayerCommand {
    private final HomeManager homeManager;
    private final TeleportManager teleportManager;
    private final BackManager backManager;

    public HomeCommand(@Nonnull HomeManager homeManager, @Nonnull TeleportManager teleportManager,
                      @Nonnull BackManager backManager) {
        super("home", "Teleport to your home");
        this.homeManager = homeManager;
        this.teleportManager = teleportManager;
        this.backManager = backManager;

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
            Msg.fail(context, "You don't have any homes set. Use /sethome to set one.");
            return;
        }

        if (homes.size() == 1) {
            String homeName = homes.keySet().iterator().next();
            doTeleportToHome(context, store, ref, playerRef, currentWorld, homeName, homeManager, teleportManager, backManager);
        } else {
            Msg.prefix(context, "Homes", String.join(", ", homes.keySet()));
        }
    }

    static void doTeleportToHome(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                                 @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef,
                                 @Nonnull World currentWorld, @Nonnull String homeName,
                                 @Nonnull HomeManager homeManager, @Nonnull TeleportManager teleportManager,
                                 @Nonnull BackManager backManager) {
        Home home = homeManager.getHome(playerRef.getUuid(), homeName);
        if (home == null) {
            Msg.fail(context, "Home '" + homeName + "' not found.");
            return;
        }

        // Save current location before teleporting
        Vector3d currentPos = playerRef.getTransform().getPosition();
        Vector3f currentRot = playerRef.getTransform().getRotation();
        backManager.setTeleportLocation(playerRef.getUuid(), currentWorld.getName(),
            currentPos.getX(), currentPos.getY(), currentPos.getZ(),
            currentRot.getY(), currentRot.getX());

        Vector3d startPosition = playerRef.getTransform().getPosition();
        
        teleportManager.queueTeleport(
            playerRef, ref, store, startPosition,
            home.getWorld(), home.getX(), home.getY(), home.getZ(), home.getYaw(), home.getPitch(),
            "Teleported to home '" + homeName + "'."
        );
    }

    private static class HomeNamedCommand extends AbstractPlayerCommand {
        private final HomeManager homeManager;
        private final TeleportManager teleportManager;
        private final BackManager backManager;
        private final RequiredArg<String> nameArg;

        HomeNamedCommand(@Nonnull HomeManager homeManager, @Nonnull TeleportManager teleportManager,
                        @Nonnull BackManager backManager) {
            super("Teleport to a specific home");
            this.homeManager = homeManager;
            this.teleportManager = teleportManager;
            this.backManager = backManager;
            this.nameArg = withRequiredArg("name", "Home name", ArgTypes.STRING);
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            String homeName = context.get(nameArg);
            doTeleportToHome(context, store, ref, playerRef, world, homeName, homeManager, teleportManager, backManager);
        }
    }
}
