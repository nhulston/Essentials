package com.nhulston.essentials.commands.spawn;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.spawn.GlobalSpawnProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.managers.SpawnManager;
import com.nhulston.essentials.util.Msg;

import javax.annotation.Nonnull;

public class SetSpawnCommand extends AbstractPlayerCommand {
    private final SpawnManager spawnManager;

    public SetSpawnCommand(@Nonnull SpawnManager spawnManager) {
        super("setspawn", "Set the server spawn location");
        this.spawnManager = spawnManager;

        requirePermission("essentials.setspawn");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            Msg.fail(context, "Could not get your position. Try again.");
            return;
        }

        Vector3d position = transform.getPosition();
        HeadRotation headRotation = store.getComponent(ref, HeadRotation.getComponentType());
        Vector3f rotation = (headRotation != null) ? headRotation.getRotation() : new Vector3f(0.0F, 0.0F, 0.0F);

        spawnManager.setSpawn(
                world.getName(),
                position.getX(),
                position.getY(),
                position.getZ(),
                rotation.getY(),
                rotation.getX()
        );

        // Also update the world's spawn provider so the map marker updates
        // Must create new Vector3d/Vector3f instances - Transform stores references, not copies!
        Vector3d spawnPosition = new Vector3d(position.getX(), position.getY(), position.getZ());
        Vector3f spawnRotation = new Vector3f(0, rotation.getY(), 0);
        Transform spawnTransform = new Transform(spawnPosition, spawnRotation);
        world.getWorldConfig().setSpawnProvider(new GlobalSpawnProvider(spawnTransform));

        Msg.success(context, "Spawn set!");
    }
}
