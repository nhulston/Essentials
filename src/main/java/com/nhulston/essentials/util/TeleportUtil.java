package com.nhulston.essentials.util;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import com.nhulston.essentials.models.Spawn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TeleportUtil {

    private TeleportUtil() {}

    /**
     * Teleports an entity to the specified location.
     * @return null if successful, error message if failed
     */
    @Nullable
    public static String teleport(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                                  @Nonnull String worldName, double x, double y, double z,
                                  float yaw, float pitch) {
        World targetWorld = Universe.get().getWorld(worldName);
        if (targetWorld == null) {
            return "World '" + worldName + "' is not loaded.";
        }

        Vector3d position = new Vector3d(x, y, z);
        Vector3f rotation = new Vector3f(pitch, yaw, 0.0F);

        Teleport teleport = new Teleport(targetWorld, position, rotation);
        store.putComponent(ref, Teleport.getComponentType(), teleport);

        return null;
    }

    /**
     * Teleports one player to another player's location.
     *
     * @param player The player to teleport
     * @param target The player to teleport to
     */
    public static void teleportToPlayer(@Nonnull PlayerRef player, @Nonnull PlayerRef target) {
        Ref<EntityStore> playerRef = player.getReference();
        Ref<EntityStore> targetRef = target.getReference();
        
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }
        
        if (targetRef == null || !targetRef.isValid()) {
            return;
        }
        
        Store<EntityStore> playerStore = playerRef.getStore();
        Store<EntityStore> targetStore = targetRef.getStore();
        
        // Get target's position
        TransformComponent targetTransform = targetStore.getComponent(targetRef, TransformComponent.getComponentType());
        if (targetTransform == null) {
            return;
        }
        
        Vector3d targetPos = targetTransform.getPosition();
        
        // Get target's world
        EntityStore targetEntityStore = targetStore.getExternalData();
        World targetWorld = targetEntityStore.getWorld();
        
        // Teleport the player
        Teleport teleport = new Teleport(targetWorld, targetPos, new Vector3f(0, 0, 0));
        playerStore.putComponent(playerRef, Teleport.getComponentType(), teleport);
    }

    /**
     * Teleports a player to spawn.
     *
     * @param player The player to teleport
     * @param spawn The spawn location
     * @return null if successful, error message if failed
     */
    @Nullable
    public static String teleportToSpawn(@Nonnull PlayerRef player, @Nonnull Spawn spawn) {
        Ref<EntityStore> playerRef = player.getReference();
        if (playerRef == null || !playerRef.isValid()) {
            return "Player reference is invalid.";
        }

        World targetWorld = Universe.get().getWorld(spawn.getWorld());
        if (targetWorld == null) {
            return "World '" + spawn.getWorld() + "' is not loaded.";
        }

        Store<EntityStore> store = playerRef.getStore();
        Vector3d position = new Vector3d(spawn.getX(), spawn.getY(), spawn.getZ());
        Vector3f rotation = new Vector3f(spawn.getPitch(), spawn.getYaw(), 0.0F);

        Teleport teleport = new Teleport(targetWorld, position, rotation);
        store.putComponent(playerRef, Teleport.getComponentType(), teleport);
        return null;
    }

    /**
     * Teleports a player to spawn using a CommandBuffer (for use within systems).
     *
     * @param ref The entity reference
     * @param buffer The command buffer to queue the teleport
     * @param spawn The spawn location
     */
    public static void teleportToSpawnBuffered(@Nonnull Ref<EntityStore> ref,
                                               @Nonnull CommandBuffer<EntityStore> buffer,
                                               @Nonnull Spawn spawn) {
        World targetWorld = Universe.get().getWorld(spawn.getWorld());
        if (targetWorld == null) {
            return;
        }

        Vector3d position = new Vector3d(spawn.getX(), spawn.getY(), spawn.getZ());
        Vector3f rotation = new Vector3f(spawn.getPitch(), spawn.getYaw(), 0.0F);

        Teleport teleport = new Teleport(targetWorld, position, rotation);
        buffer.putComponent(ref, Teleport.getComponentType(), teleport);
    }
}
