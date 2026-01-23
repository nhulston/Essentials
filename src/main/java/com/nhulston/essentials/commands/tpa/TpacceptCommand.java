package com.nhulston.essentials.commands.tpa;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.command.system.CommandContext;
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

import javax.annotation.Nonnull;
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
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        // Get raw input to check if player specified
        String rawInput = context.getInputString().trim();
        String[] parts = rawInput.split("\\s+");
        
        TpaManager.TpaRequest request;
        String requesterName;
        
        // If no player specified, accept most recent request
        if (parts.length == 1) {
            request = tpaManager.acceptMostRecentRequest(playerRef);
            if (request == null) {
                Msg.send(context, messages.get("commands.tpaccept.no-requests"));
                return;
            }
            requesterName = request.getRequesterName();
        } else {
            // Player name specified - look them up
            requesterName = parts[1];
            PlayerRef requester = findPlayer(requesterName);
            
            if (requester == null) {
                Msg.send(context, messages.get("commands.tpaccept.player-offline", Map.of("player", requesterName)));
                return;
            }
            
            // Accept request from specific player
            request = tpaManager.acceptRequest(playerRef, requester.getUsername());
            if (request == null) {
                Msg.send(context, messages.get("commands.tpaccept.no-request-from", Map.of("player", requester.getUsername())));
                return;
            }
            requesterName = requester.getUsername();
        }

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
        requesterWorld.execute(() -> {
            if (!requesterRef.isValid()) {
                return;
            }
            
            Vector3d currentPos = requester.getTransform().getPosition();
            Vector3f currentRot = requester.getTransform().getRotation();
            backManager.setTeleportLocation(requester.getUuid(), requesterWorld.getName(),
                currentPos.getX(), currentPos.getY(), currentPos.getZ(),
                currentRot.getY(), currentRot.getX());

            // Queue the teleport (startPosition must be captured on requester's world thread)
            Vector3d startPosition = currentPos.clone();
            
            teleportManager.queueTeleportToPlayer(
                requester, requesterRef, requesterStore, startPosition,
                playerRef,  // target player
                messages.get("commands.tpaccept.teleported", Map.of("player", playerRef.getUsername()))
            );
        });
    }
    
    /**
     * Find a player by name (case-insensitive).
     */
    private PlayerRef findPlayer(String name) {
        for (PlayerRef player : Universe.get().getPlayers()) {
            if (player.getUsername().equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }
}
