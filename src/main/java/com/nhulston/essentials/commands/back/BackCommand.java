package com.nhulston.essentials.commands.back;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
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
 * Command to teleport back to the player's last death location.
 * Usage: /back
 */
public class BackCommand extends AbstractPlayerCommand {
    private final BackManager backManager;
    private final TeleportManager teleportManager;

    public BackCommand(@Nonnull BackManager backManager, @Nonnull TeleportManager teleportManager) {
        super("back", "Teleport to your last death location");
        this.backManager = backManager;
        this.teleportManager = teleportManager;

        requirePermission("essentials.back");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        BackManager.DeathLocation deathLocation = backManager.getAndClearDeathLocation(playerRef.getUuid());

        if (deathLocation == null) {
            Msg.fail(context, "You have no death location to return to.");
            return;
        }

        Vector3d startPosition = playerRef.getTransform().getPosition();

        teleportManager.queueTeleport(
            playerRef, ref, store, startPosition,
            deathLocation.getWorldName(),
            deathLocation.getX(),
            deathLocation.getY(),
            deathLocation.getZ(),
            deathLocation.getYaw(),
            deathLocation.getPitch(),
            "Teleported to your last death location."
        );
    }
}
