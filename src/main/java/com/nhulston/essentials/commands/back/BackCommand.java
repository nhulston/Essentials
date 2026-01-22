package com.nhulston.essentials.commands.back;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.managers.BackManager;
import com.nhulston.essentials.managers.TeleportManager;
import com.nhulston.essentials.util.Msg;

import javax.annotation.Nonnull;

/**
 * Command to teleport back to the player's last location (death or pre-teleport).
 * Usage: /back
 */
public class BackCommand extends AbstractPlayerCommand {
    private final BackManager backManager;
    private final TeleportManager teleportManager;

    public BackCommand(@Nonnull BackManager backManager, @Nonnull TeleportManager teleportManager) {
        super("back", "Teleport to your previous location");
        this.backManager = backManager;
        this.teleportManager = teleportManager;

        requirePermission("essentials.back");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        java.util.UUID playerUuid = playerRef.getUuid();
        BackManager.BackLocation backLocation = backManager.getBackLocation(playerUuid);

        if (backLocation == null) {
            Msg.fail(context, "You have no previous location to return to.");
            return;
        }

        // Save current location before teleporting
        Vector3d currentPos = playerRef.getTransform().getPosition();
        Vector3f currentRot = playerRef.getTransform().getRotation();
        backManager.setTeleportLocation(playerUuid, world.getName(),
            currentPos.getX(), currentPos.getY(), currentPos.getZ(),
            currentRot.getY(), currentRot.getX());

        Vector3d startPosition = playerRef.getTransform().getPosition();

        teleportManager.queueTeleport(
            playerRef, ref, store, startPosition,
            backLocation.getWorldName(),
            backLocation.getX(),
            backLocation.getY(),
            backLocation.getZ(),
            backLocation.getYaw(),
            backLocation.getPitch(),
            "Teleported to your previous location.",
            () -> backManager.clearBackLocation(playerUuid)
        );
    }
}
