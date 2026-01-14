package com.nhulston.essentials.commands.tpa;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.managers.TpaManager;
import com.nhulston.essentials.util.Msg;
import com.nhulston.essentials.util.TeleportUtil;

import javax.annotation.Nonnull;

/**
 * Command to accept a teleport request from another player.
 * Usage: /tpaccept <player>
 */
public class TpacceptCommand extends AbstractPlayerCommand {
    private final TpaManager tpaManager;
    private final RequiredArg<String> playerArg;

    public TpacceptCommand(@Nonnull TpaManager tpaManager) {
        super("tpaccept", "Accept a teleport request");
        this.tpaManager = tpaManager;
        this.playerArg = withRequiredArg("player", "Player whose request to accept", ArgTypes.STRING);

        requirePermission("essentials.tpaccept");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        String requesterName = context.get(playerArg);

        TpaManager.TpaRequest request = tpaManager.acceptRequest(playerRef, requesterName);
        if (request == null) {
            Msg.fail(context, "No pending teleport request from " + requesterName + ".");
            return;
        }

        // Get the requester's PlayerRef
        PlayerRef requester = Universe.get().getPlayer(request.getRequesterUuid());
        if (requester == null) {
            Msg.fail(context, requesterName + " is no longer online.");
            return;
        }

        // Teleport the requester to the target (the player who accepted)
        TeleportUtil.teleportToPlayer(requester, playerRef);

        // Notify both players
        Msg.success(context, "Teleport request from " + requesterName + " accepted.");
        Msg.success(requester, "Teleporting to " + playerRef.getUsername() + "...");
    }
}
