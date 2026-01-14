package com.nhulston.essentials.managers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages death locations for the /back command.
 * Stores locations in memory only - not persisted across restarts.
 */
public class BackManager {
    private final ConcurrentHashMap<UUID, DeathLocation> deathLocations = new ConcurrentHashMap<>();

    /**
     * Records a player's death location.
     */
    public void setDeathLocation(@Nonnull UUID playerUuid, @Nonnull String worldName,
                                  double x, double y, double z, float yaw, float pitch) {
        deathLocations.put(playerUuid, new DeathLocation(worldName, x, y, z, yaw, pitch));
    }

    /**
     * Gets and clears a player's death location.
     * Returns null if no death location is stored.
     */
    @Nullable
    public DeathLocation getAndClearDeathLocation(@Nonnull UUID playerUuid) {
        return deathLocations.remove(playerUuid);
    }

    /**
     * Stores death location data.
     */
    public static class DeathLocation {
        private final String worldName;
        private final double x, y, z;
        private final float yaw, pitch;

        DeathLocation(String worldName, double x, double y, double z, float yaw, float pitch) {
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
