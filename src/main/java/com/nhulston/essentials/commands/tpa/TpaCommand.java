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
import com.nhulston.essentials.Essentials;
import com.nhulston.essentials.managers.TpaManager;
import com.nhulston.essentials.util.MessageManager;
import com.nhulston.essentials.util.Msg;
import com.nhulston.essentials.util.SoundUtil;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Command to request teleportation to another player.
 * Usage: /tpa <player>
 */
public class TpaCommand extends AbstractPlayerCommand {
    private final TpaManager tpaManager;
    private final MessageManager messages;
    private final RequiredArg<PlayerRef> targetArg;

    public TpaCommand(@Nonnull TpaManager tpaManager) {
        super("tpa", "Request to teleport to a player");
        this.tpaManager = tpaManager;
        this.messages = Essentials.getInstance().getMessageManager();
        this.targetArg = withRequiredArg("player", "Player to teleport to", ArgTypes.PLAYER_REF);

        requirePermission("essentials.tpa");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        PlayerRef target = context.get(targetArg);

        if (target == null) {
            Msg.send(context, messages.get("commands.tpa.player-not-found"));
            return;
        }

        Ref<EntityStore> targetRef = target.getReference();
        if (targetRef == null || !targetRef.isValid()) {
            Msg.send(context, messages.get("commands.tpa.player-not-found"));
            return;
        }

        if (target.getUuid().equals(playerRef.getUuid())) {
            Msg.send(context, messages.get("commands.tpa.cannot-self"));
            return;
        }

        boolean created = tpaManager.createRequest(playerRef, target);
        if (!created) {
            Msg.send(context, messages.get("commands.tpa.already-pending", Map.of("player", target.getUsername())));
            return;
        }

        // Notify the requester
        Msg.send(context, messages.get("commands.tpa.request-sent", Map.of("player", target.getUsername())));

        // Notify the target
        SoundUtil.playSound(target, "SFX_Alchemy_Bench_Close");
        Msg.send(target, messages.get("commands.tpa.request-received", Map.of("player", playerRef.getUsername())));
        Msg.send(target, messages.get("commands.tpa.accept-instruction", Map.of("player", playerRef.getUsername())));
    }
}
