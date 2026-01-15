package com.nhulston.essentials.managers;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.spawn.GlobalSpawnProvider;
import com.nhulston.essentials.models.Spawn;
import com.nhulston.essentials.util.Log;
import com.nhulston.essentials.util.StorageManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SpawnManager {
    private final StorageManager storageManager;

    public SpawnManager(@Nonnull StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    public void setSpawn(@Nonnull String world, double x, double y, double z, float yaw, float pitch) {
        Spawn spawn = new Spawn(world, x, y, z, yaw, pitch);
        storageManager.setSpawn(spawn);
    }

    @Nullable
    public Spawn getSpawn() {
        return storageManager.getSpawn();
    }

    public boolean hasSpawn() {
        return storageManager.getSpawn() != null;
    }

    /**
     * Syncs the saved Essentials spawn with the world's native spawn provider.
     * This updates the spawn marker on the map.
     * Should be called after worlds are loaded.
     */
    public void syncWorldSpawnProvider() {
        Spawn spawn = getSpawn();
        if (spawn == null) {
            return;
        }

        World world = Universe.get().getWorld(spawn.getWorld());
        if (world == null) {
            Log.warning("Could not sync spawn provider: world '" + spawn.getWorld() + "' not found");
            return;
        }

        Vector3d position = new Vector3d(spawn.getX(), spawn.getY(), spawn.getZ());
        Vector3f rotation = new Vector3f(0, spawn.getYaw(), 0);
        Transform spawnTransform = new Transform(position, rotation);
        
        world.getWorldConfig().setSpawnProvider(new GlobalSpawnProvider(spawnTransform));
        Log.info("Synced spawn provider for world '" + spawn.getWorld() + "'");
    }
}
