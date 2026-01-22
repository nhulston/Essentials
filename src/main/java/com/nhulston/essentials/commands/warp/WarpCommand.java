package com.nhulston.essentials.commands.warp;

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
import com.nhulston.essentials.managers.TeleportManager;
import com.nhulston.essentials.managers.WarpManager;
import com.nhulston.essentials.models.Warp;
import com.nhulston.essentials.util.Msg;

import javax.annotation.Nonnull;
import java.util.Map;

public class WarpCommand extends AbstractPlayerCommand {
    private final WarpManager warpManager;

    public WarpCommand(@Nonnull WarpManager warpManager, @Nonnull TeleportManager teleportManager,
                      @Nonnull BackManager backManager) {
        super("warp", "Teleport to a warp");
        this.warpManager = warpManager;

        requirePermission("essentials.warp");
        addUsageVariant(new WarpNamedCommand(warpManager, teleportManager, backManager));
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        // /warp (no args) - list all warps
        Map<String, Warp> warps = warpManager.getWarps();

        if (warps.isEmpty()) {
            Msg.fail(context, "No warps have been set.");
            return;
        }

        Msg.prefix(context, "Warps", String.join(", ", warps.keySet()));
    }

    private static class WarpNamedCommand extends AbstractPlayerCommand {
        private final WarpManager warpManager;
        private final TeleportManager teleportManager;
        private final BackManager backManager;
        private final RequiredArg<String> nameArg;

        WarpNamedCommand(@Nonnull WarpManager warpManager, @Nonnull TeleportManager teleportManager,
                        @Nonnull BackManager backManager) {
            super("Teleport to a specific warp");
            this.warpManager = warpManager;
            this.teleportManager = teleportManager;
            this.backManager = backManager;
            this.nameArg = withRequiredArg("name", "Warp name", ArgTypes.STRING);
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            String warpName = context.get(nameArg);
            Warp warp = warpManager.getWarp(warpName);

            if (warp == null) {
                Msg.fail(context, "Warp '" + warpName + "' not found.");
                return;
            }

            // Save current location before teleporting
            Vector3d currentPos = playerRef.getTransform().getPosition();
            Vector3f currentRot = playerRef.getTransform().getRotation();
            backManager.setTeleportLocation(playerRef.getUuid(), world.getName(),
                currentPos.getX(), currentPos.getY(), currentPos.getZ(),
                currentRot.getY(), currentRot.getX());

            Vector3d startPosition = playerRef.getTransform().getPosition();

            teleportManager.queueTeleport(
                playerRef, ref, store, startPosition,
                warp.getWorld(), warp.getX(), warp.getY(), warp.getZ(), warp.getYaw(), warp.getPitch(),
                "Teleported to warp '" + warpName + "'"
            );
        }
    }
}
