package com.nhulston.essentials.managers;

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
     * Type of back location.
     */
    public enum BackLocationType {
        DEATH,      // Player died here
        TELEPORT    // Player teleported from here
    }

    /**
     * Records a player's back location with a specific type.
     * DEATH type takes priority - won't be overwritten by TELEPORT until used.
     */
    public void setBackLocation(@Nonnull UUID playerUuid, @Nonnull String worldName,
                                double x, double y, double z, float yaw, float pitch,
                                @Nonnull BackLocationType type) {
        BackLocation existing = backLocations.get(playerUuid);
        
        // If existing location is DEATH and new is TELEPORT, don't overwrite
        if (existing != null && existing.getType() == BackLocationType.DEATH 
            && type == BackLocationType.TELEPORT) {
            return;
        }
        
        backLocations.put(playerUuid, new BackLocation(worldName, x, y, z, yaw, pitch, type));
    }

    /**
     * Records a player's death location.
     * Wrapper for setBackLocation with DEATH type.
     */
    public void setDeathLocation(@Nonnull UUID playerUuid, @Nonnull String worldName,
                                  double x, double y, double z, float yaw, float pitch) {
        setBackLocation(playerUuid, worldName, x, y, z, yaw, pitch, BackLocationType.DEATH);
    }

    /**
     * Records a player's pre-teleport location.
     * Wrapper for setBackLocation with TELEPORT type.
     * Won't overwrite death locations.
     */
    public void setTeleportLocation(@Nonnull UUID playerUuid, @Nonnull String worldName,
                                     double x, double y, double z, float yaw, float pitch) {
        setBackLocation(playerUuid, worldName, x, y, z, yaw, pitch, BackLocationType.TELEPORT);
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
        private final BackLocationType type;

        BackLocation(String worldName, double x, double y, double z, float yaw, float pitch, BackLocationType type) {
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.type = type;
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
        
        BackLocationType getType() {
            return type;
        }
    }
}
