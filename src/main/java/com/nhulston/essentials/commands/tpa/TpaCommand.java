package com.nhulston.essentials.commands.tpa;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.managers.TpaManager;
import com.nhulston.essentials.util.Msg;
import com.nhulston.essentials.util.SoundUtil;

import javax.annotation.Nonnull;

/**
 * Command to request teleportation to another player.
 * Usage: /tpa <player>
 */
public class TpaCommand extends AbstractPlayerCommand {
    private final TpaManager tpaManager;
    private final RequiredArg<PlayerRef> targetArg;

    public TpaCommand(@Nonnull TpaManager tpaManager) {
        super("tpa", "Request to teleport to a player");
        this.tpaManager = tpaManager;
        this.targetArg = withRequiredArg("player", "Player to teleport to", ArgTypes.PLAYER_REF);

        requirePermission("essentials.tpa");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        PlayerRef target = context.get(targetArg);

        if (target == null) {
            Msg.fail(context, "Player not found.");
            return;
        }

        if (target.getUuid().equals(playerRef.getUuid())) {
            Msg.fail(context, "You cannot send a teleport request to yourself.");
            return;
        }

        boolean created = tpaManager.createRequest(playerRef, target);
        if (!created) {
            Msg.fail(context, "You already have a pending teleport request to " + target.getUsername() + ".");
            return;
        }

        // Notify the requester
        Msg.success(context, "Teleport request sent to " + target.getUsername() + ".");

        // Notify the target
        SoundUtil.playSound(target, "SFX_Alchemy_Bench_Close");
        Msg.info(target, playerRef.getUsername() + " has requested to teleport to you.");
        Msg.info(target, "Type '/tpaccept " + playerRef.getUsername() + "' to accept.");
    }
}
