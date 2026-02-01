package com.nhulston.essentials.commands.warp;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.Essentials;
import com.nhulston.essentials.managers.BackManager;
import com.nhulston.essentials.managers.TeleportManager;
import com.nhulston.essentials.managers.WarpManager;
import com.nhulston.essentials.models.Warp;
import com.nhulston.essentials.util.MessageManager;
import com.nhulston.essentials.util.Msg;
import com.nhulston.essentials.util.TeleportUtil;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Warp command.
 * Usage: /warp - List all warps (player only)
 * Usage: /warp <name> - Teleport to a warp (with delay)
 * Usage: /warp <name> <player> - Teleport another player to a warp instantly (requires essentials.warp.others or console)
 */
public class WarpCommand extends AbstractPlayerCommand {
    private final WarpManager warpManager;
    private final MessageManager messages;

    public WarpCommand(@Nonnull WarpManager warpManager, @Nonnull TeleportManager teleportManager,
                      @Nonnull BackManager backManager) {
        super("warp", "Teleport to a warp");
        this.warpManager = warpManager;
        this.messages = Essentials.getInstance().getMessageManager();

        requirePermission("essentials.warp");
        addUsageVariant(new WarpNamedCommand(warpManager, teleportManager, backManager));
        addUsageVariant(new WarpOtherCommand(warpManager, backManager));
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        // /warp (no args) - list all warps
        Map<String, Warp> warps = warpManager.getWarps();

        if (warps.isEmpty()) {
            Msg.send(context, messages.get("commands.warp.no-warps"));
            return;
        }

        Msg.send(context, messages.get("commands.warp.list-prefix") + ": " + String.join(", ", warps.keySet()));
    }

    /**
     * Variant for teleporting self to a warp.
     * Usage: /warp <name>
     */
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
            MessageManager messages = Essentials.getInstance().getMessageManager();

            if (warp == null) {
                Msg.send(context, messages.get("commands.warp.not-found", Map.of("warp", warpName)));
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
                warp.getWorld(), warp.getX(), warp.getY(), warp.getZ(), warp.getYaw(), warp.getPitch(),
                messages.get("commands.warp.teleported", Map.of("warp", warpName))
            );
        }
    }

    /**
     * Variant for teleporting another player to a warp (console or admin).
     * Usage: /warp <name> <player>
     */
    private static class WarpOtherCommand extends AbstractCommand {
        private final WarpManager warpManager;
        private final BackManager backManager;
        private final RequiredArg<String> nameArg;
        private final RequiredArg<PlayerRef> targetArg;

        WarpOtherCommand(@Nonnull WarpManager warpManager, @Nonnull BackManager backManager) {
            super("Teleport another player to a warp");
            this.warpManager = warpManager;
            this.backManager = backManager;
            this.nameArg = withRequiredArg("name", "Warp name", ArgTypes.STRING);
            this.targetArg = withRequiredArg("player", "Player to teleport", ArgTypes.PLAYER_REF);
            requirePermission("essentials.warp.others");
        }

        @Override
        protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
            MessageManager messages = Essentials.getInstance().getMessageManager();
            String warpName = context.get(nameArg);
            Warp warp = warpManager.getWarp(warpName);

            if (warp == null) {
                Msg.send(context, messages.get("commands.warp.not-found", Map.of("warp", warpName)));
                return CompletableFuture.completedFuture(null);
            }

            PlayerRef targetPlayer = context.get(targetArg);

            // Execute teleport on player's world thread with validation
            TeleportUtil.executeOnPlayerWorld(targetPlayer, context, () -> {
                String error = TeleportUtil.saveLocationAndTeleport(
                    targetPlayer, backManager,
                    warp.getWorld(), warp.getX(), warp.getY(), warp.getZ(), warp.getYaw(), warp.getPitch()
                );

                if (error != null) {
                    Msg.send(context, error);
                    return;
                }

                String senderName = "Console";
                Msg.send(context, messages.get("commands.warp.teleported-other", 
                    Map.of("player", targetPlayer.getUsername(), "warp", warpName)));
                Msg.send(targetPlayer, messages.get("commands.warp.teleported-by", 
                    Map.of("sender", senderName, "warp", warpName)));
            });

            return CompletableFuture.completedFuture(null);
        }
    }
}
