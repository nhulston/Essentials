package com.nhulston.essentials.commands.warp;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.managers.WarpManager;
import com.nhulston.essentials.models.Warp;
import com.nhulston.essentials.util.Msg;
import com.nhulston.essentials.util.TeleportUtil;

import javax.annotation.Nonnull;
import java.util.Map;

public class WarpCommand extends AbstractPlayerCommand {
    private final WarpManager warpManager;

    public WarpCommand(@Nonnull WarpManager warpManager) {
        super("warp", "Teleport to a warp");
        this.warpManager = warpManager;

        requirePermission("essentials.warp");
        addUsageVariant(new WarpNamedCommand(warpManager));
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
        private final RequiredArg<String> nameArg;

        WarpNamedCommand(@Nonnull WarpManager warpManager) {
            super("Teleport to a specific warp");
            this.warpManager = warpManager;
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

            String error = TeleportUtil.teleport(store, ref, warp.getWorld(),
                    warp.getX(), warp.getY(), warp.getZ(), warp.getYaw(), warp.getPitch());

            if (error != null) {
                Msg.fail(context, error);
                return;
            }

            Msg.success(context, "Teleported to warp '" + warpName + "'");
        }
    }
}
