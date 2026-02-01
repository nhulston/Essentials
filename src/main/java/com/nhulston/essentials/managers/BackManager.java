package com.nhulston.essentials.managers;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages back locations for the /back command.
 * Tracks both death locations and pre-teleport locations.
 * Stores locations in memory only - not persisted across restarts.
 */
public class BackManager {
    private final ConcurrentHashMap<UUID, BackLocation> backLocations = new ConcurrentHashMap<>();

    /**
     * Records a player's back location (death or teleport).
     * Gets position from EntityStore to avoid stale cache after world changes.
     * Always overwrites - most recent location is saved.
     */
    public void setBackLocation(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                                @Nonnull PlayerRef playerRef, @Nonnull World world) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;
        
        Vector3d pos = transform.getPosition();
        Vector3f rot = transform.getRotation();
        backLocations.put(playerRef.getUuid(), 
            new BackLocation(world.getName(), pos.getX(), pos.getY(), pos.getZ(), rot.getY(), rot.getX()));
    }

    /**
     * Gets a player's back location without clearing it.
     * Returns null if no location is stored.
     */
    @Nullable
    public BackLocation getBackLocation(@Nonnull UUID playerUuid) {
        return backLocations.get(playerUuid);
    }

    /**
     * Clears a player's back location.
     */
    public void clearBackLocation(@Nonnull UUID playerUuid) {
        backLocations.remove(playerUuid);
    }

    /**
     * Cleans up back location for a player when they disconnect.
     */
    public void onPlayerQuit(@Nonnull UUID playerUuid) {
        backLocations.remove(playerUuid);
    }

    /**
     * Stores back location data.
     */
    public static class BackLocation {
        private final String worldName;
        private final double x, y, z;
        private final float yaw, pitch;

        BackLocation(String worldName, double x, double y, double z, float yaw, float pitch) {
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public String getWorldName() {
            return worldName;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }

        public float getYaw() {
            return yaw;
        }

        public float getPitch() {
            return pitch;
        }
    }
}
