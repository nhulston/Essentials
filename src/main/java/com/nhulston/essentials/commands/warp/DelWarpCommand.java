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
import com.nhulston.essentials.Essentials;
import com.nhulston.essentials.managers.WarpManager;
import com.nhulston.essentials.util.MessageManager;
import com.nhulston.essentials.util.Msg;

import javax.annotation.Nonnull;
import java.util.Map;

public class DelWarpCommand extends AbstractPlayerCommand {
    private final WarpManager warpManager;
    private final MessageManager messages;
    private final RequiredArg<String> nameArg;

    public DelWarpCommand(@Nonnull WarpManager warpManager) {
        super("delwarp", "Delete a warp");
        this.warpManager = warpManager;
        this.messages = Essentials.getInstance().getMessageManager();
        this.nameArg = withRequiredArg("name", "Warp name", ArgTypes.STRING);

        requirePermission("essentials.delwarp");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        String warpName = context.get(nameArg);

        boolean deleted = warpManager.deleteWarp(warpName);

        if (deleted) {
            Msg.send(context, messages.get("commands.delwarp.success", Map.of("warp", warpName)));
        } else {
            Msg.send(context, messages.get("commands.delwarp.not-found", Map.of("warp", warpName)));
        }
    }
}
