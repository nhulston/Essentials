package com.nhulston.essentials.commands.spawn;

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
import com.nhulston.essentials.managers.SpawnManager;
import com.nhulston.essentials.managers.TeleportManager;
import com.nhulston.essentials.models.Spawn;
import com.nhulston.essentials.util.Msg;

import javax.annotation.Nonnull;

public class SpawnCommand extends AbstractPlayerCommand {
    private final SpawnManager spawnManager;
    private final TeleportManager teleportManager;
    private final BackManager backManager;

    public SpawnCommand(@Nonnull SpawnManager spawnManager, @Nonnull TeleportManager teleportManager,
                       @Nonnull BackManager backManager) {
        super("spawn", "Teleport to the server spawn");
        this.spawnManager = spawnManager;
        this.teleportManager = teleportManager;
        this.backManager = backManager;

        addAliases("s");
        requirePermission("essentials.spawn");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        Spawn spawn = spawnManager.getSpawn();

        if (spawn == null) {
            Msg.fail(context, "Spawn has not been set.");
            return;
        }

        // Save current location before teleporting
        Vector3d currentPos = playerRef.getTransform().getPosition();
        Vector3f currentRot = playerRef.getTransform().getRotation();
        backManager.setTeleportLocation(playerRef.getUuid(), world.getName(),
            currentPos.getX(), currentPos.getY(), currentPos.getZ(),
            currentRot.getY(), currentRot.getX());

        Vector3d startPosition = playerRef.getTransform().getPosition();

        teleportManager.queueTeleport(
            playerRef, ref, store, startPosition,
            spawn.getWorld(), spawn.getX(), spawn.getY(), spawn.getZ(), spawn.getYaw(), spawn.getPitch(),
            "Teleported to spawn."
        );
    }
}
