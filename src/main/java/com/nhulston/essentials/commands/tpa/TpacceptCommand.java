package com.nhulston.essentials.commands.tpa;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.Essentials;
import com.nhulston.essentials.managers.BackManager;
import com.nhulston.essentials.managers.TeleportManager;
import com.nhulston.essentials.managers.TpaManager;
import com.nhulston.essentials.util.MessageManager;
import com.nhulston.essentials.util.Msg;
import com.nhulston.essentials.util.TeleportUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * Command to accept a teleport request from another player.
 * Usage: /tpaccept [player]
 * If no player is specified, accepts the most recent request.
 */
public class TpacceptCommand extends AbstractPlayerCommand {
    private final TpaManager tpaManager;
    private final TeleportManager teleportManager;
    private final BackManager backManager;
    private final MessageManager messages;
    
    public TpacceptCommand(@Nonnull TpaManager tpaManager, @Nonnull TeleportManager teleportManager,
                          @Nonnull BackManager backManager) {
        super("tpaccept", "Accept a teleport request");
        this.tpaManager = tpaManager;
        this.teleportManager = teleportManager;
        this.backManager = backManager;
        this.messages = Essentials.getInstance().getMessageManager();

        addAliases("tpyes");
        requirePermission("essentials.tpaccept");
        
        // Add usage variant for accepting specific player's request
        addUsageVariant(new TpacceptNamedCommand(tpaManager, teleportManager, backManager));
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        // /tpaccept with no arguments - accept most recent request
        TpaManager.TpaRequest request = tpaManager.acceptMostRecentRequest(playerRef);
        if (request == null) {
            Msg.send(context, messages.get("commands.tpaccept.no-requests"));
            return;
        }
        
        String requesterName = request.getRequesterName();
        
        // Get the requester's PlayerRef
        PlayerRef requester = Universe.get().getPlayer(request.getRequesterUuid());
        if (requester == null) {
            Msg.send(context, messages.get("commands.tpaccept.player-offline", Map.of("player", requesterName)));
            return;
        }

        // Get the requester's entity ref and store
        Ref<EntityStore> requesterRef = requester.getReference();
        if (requesterRef == null || !requesterRef.isValid()) {
            Msg.send(context, messages.get("commands.tpaccept.player-unavailable", Map.of("player", requesterName)));
            return;
        }
        Store<EntityStore> requesterStore = requesterRef.getStore();
        
        // Get requester's world to execute on their thread
        World requesterWorld = requesterStore.getExternalData().getWorld();

        // Notify the target that the request was accepted
        Msg.send(context, messages.get("commands.tpaccept.accepted", Map.of("player", requesterName)));

        // Save requester's location and queue teleport (must be on their world thread)
        executeTeleport(playerRef, requester, requesterRef, requesterStore, requesterWorld, backManager, messages, teleportManager);
    }

    /**
     * Usage variant for /tpaccept <player>
     */
    private static class TpacceptNamedCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> playerArg;
        private final TpaManager tpaManager;
        private final TeleportManager teleportManager;
        private final BackManager backManager;
        
        TpacceptNamedCommand(@Nonnull TpaManager tpaManager, @Nonnull TeleportManager teleportManager,
                            @Nonnull BackManager backManager) {
            super("Accept a teleport request from a specific player");
            this.tpaManager = tpaManager;
            this.teleportManager = teleportManager;
            this.backManager = backManager;
            this.playerArg = withRequiredArg("player", "Player whose request to accept", ArgTypes.STRING);
        }
        
        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            MessageManager messages = Essentials.getInstance().getMessageManager();
            String requesterName = context.get(playerArg);
            
            // Player name specified - look them up
            PlayerRef requester = findPlayer(requesterName);
            
            if (requester == null) {
                Msg.send(context, messages.get("commands.tpaccept.player-offline", Map.of("player", requesterName)));
                return;
            }
            
            // Accept request from specific player
            TpaManager.TpaRequest request = tpaManager.acceptRequest(playerRef, requester.getUsername());
            if (request == null) {
                Msg.send(context, messages.get("commands.tpaccept.no-request-from", Map.of("player", requester.getUsername())));
                return;
            }

            // Get the requester's entity ref and store
            Ref<EntityStore> requesterRef = requester.getReference();
            if (requesterRef == null || !requesterRef.isValid()) {
                Msg.send(context, messages.get("commands.tpaccept.player-unavailable", Map.of("player", requester.getUsername())));
                return;
            }
            Store<EntityStore> requesterStore = requesterRef.getStore();
            
            // Get requester's world to execute on their thread
            World requesterWorld = requesterStore.getExternalData().getWorld();

            // Notify the target that the request was accepted
            Msg.send(context, messages.get("commands.tpaccept.accepted", Map.of("player", requester.getUsername())));

            // Save requester's location and queue teleport (must be on their world thread)
            executeTeleport(playerRef, requester, requesterRef, requesterStore, requesterWorld, backManager, messages, teleportManager);
        }
        
        /**
         * Find a player by name (case-insensitive).
         */
        @Nullable
        private static PlayerRef findPlayer(String name) {
            for (PlayerRef player : Universe.get().getPlayers()) {
                if (player.getUsername().equalsIgnoreCase(name)) {
                    return player;
                }
            }
            return null;
        }
    }

    private static void executeTeleport(@Nonnull PlayerRef playerRef, PlayerRef requester, Ref<EntityStore> requesterRef, Store<EntityStore> requesterStore, World requesterWorld, BackManager backManager, MessageManager messages, TeleportManager teleportManager) {
        requesterWorld.execute(() -> {
            if (!requesterRef.isValid()) {
                return;
            }

            backManager.setBackLocation(requesterStore, requesterRef, requester, requesterWorld);
            Vector3d startPosition = TeleportUtil.getStartPosition(requesterStore, requesterRef);
            if (startPosition == null) {
                Msg.send(requester, messages.get("errors.generic"));
                return;
            }

            teleportManager.queueTeleportToPlayer(
                    requester, requesterRef, requesterStore, startPosition,
                    playerRef,  // target player
                    messages.get("commands.tpaccept.teleported", Map.of("player", playerRef.getUsername()))
            );
        });
    }
}
