package com.nhulston.essentials.commands.tphere;

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
import com.nhulston.essentials.util.MessageManager;
import com.nhulston.essentials.util.Msg;
import com.nhulston.essentials.util.TeleportUtil;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Command to teleport another player to yourself.
 * Usage: /tphere <player>
 */
public class TphereCommand extends AbstractPlayerCommand {
    private final MessageManager messages;
    private final RequiredArg<PlayerRef> targetArg;

    public TphereCommand() {
        super("tphere", "Teleport a player to you");
        this.messages = Essentials.getInstance().getMessageManager();
        this.targetArg = withRequiredArg("player", "Player to teleport", ArgTypes.PLAYER_REF);
        requirePermission("essentials.tphere");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        PlayerRef target = context.get(targetArg);

        if (target == null) {
            Msg.send(context, messages.get("commands.tphere.player-not-found"));
            return;
        }

        Ref<EntityStore> targetRef = target.getReference();
        if (targetRef == null || !targetRef.isValid()) {
            Msg.send(context, messages.get("commands.tphere.player-not-found"));
            return;
        }

        if (target.getUuid().equals(playerRef.getUuid())) {
            Msg.send(context, messages.get("commands.tphere.cannot-self"));
            return;
        }

        // Teleport target to the command sender
        TeleportUtil.teleportToPlayer(target, playerRef);

        Msg.send(context, messages.get("commands.tphere.success", Map.of("player", target.getUsername())));
        Msg.send(target, messages.get("commands.tphere.teleported", Map.of("player", playerRef.getUsername())));
    }
}
