package com.nhulston.essentials.managers;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.nhulston.essentials.models.Spawn;
import com.nhulston.essentials.util.ConfigManager;
import com.nhulston.essentials.util.StorageManager;

import javax.annotation.Nonnull;
import java.util.UUID;

public class SpawnProtectionManager {
    private static final String BYPASS_PERMISSION = "essentials.spawn.bypass";

    private final ConfigManager configManager;
    private final StorageManager storageManager;

    public SpawnProtectionManager(@Nonnull ConfigManager configManager, @Nonnull StorageManager storageManager) {
        this.configManager = configManager;
        this.storageManager = storageManager;
    }

    /**
     * Checks if spawn protection is enabled.
     */
    public boolean isEnabled() {
        return configManager.isSpawnProtectionEnabled();
    }

    /**
     * Checks if invulnerability is enabled in spawn area.
     */
    public boolean isInvulnerableEnabled() {
        return configManager.isSpawnProtectionInvulnerable();
    }

    /**
     * Checks if a block position is within the protected spawn area.
     * @param worldName The world name to check against
     * @param blockPos The block position to check
     */
    public boolean isInProtectedArea(@Nonnull String worldName, @Nonnull Vector3i blockPos) {
        Spawn spawn = storageManager.getSpawn();
        if (spawn == null) {
            return false;
        }

        // Check if we're in the correct world
        if (!spawn.getWorld().equals(worldName)) {
            return false;
        }

        int radius = configManager.getSpawnProtectionRadius();
        double dx = Math.abs(blockPos.getX() - spawn.getX());
        double dz = Math.abs(blockPos.getZ() - spawn.getZ());

        // Check square radius (X/Z)
        if (dx > radius || dz > radius) {
            return false;
        }

        // Check Y range if configured
        return isInYRange(blockPos.getY());
    }

    /**
     * Checks if an entity position is within the protected spawn area.
     * @param worldName The world name to check against
     * @param entityPos The entity position to check
     */
    public boolean isInProtectedArea(@Nonnull String worldName, @Nonnull Vector3d entityPos) {
        Spawn spawn = storageManager.getSpawn();
        if (spawn == null) {
            return false;
        }

        // Check if we're in the correct world
        if (!spawn.getWorld().equals(worldName)) {
            return false;
        }

        int radius = configManager.getSpawnProtectionRadius();
        double dx = Math.abs(entityPos.getX() - spawn.getX());
        double dz = Math.abs(entityPos.getZ() - spawn.getZ());

        // Check square radius (X/Z)
        if (dx > radius || dz > radius) {
            return false;
        }

        // Check Y range if configured
        return isInYRange((int) entityPos.getY());
    }

    /**
     * Checks if a Y coordinate is within the configured Y range.
     * Returns true if Y range is disabled (min-y and max-y are both -1).
     */
    private boolean isInYRange(int y) {
        int minY = configManager.getSpawnProtectionMinY();
        int maxY = configManager.getSpawnProtectionMaxY();

        // If both are -1, Y range is disabled (protect all Y levels)
        if (minY == -1 && maxY == -1) {
            return true;
        }

        // Check bounds
        if (minY != -1 && y < minY) {
            return false;
        }
        if (maxY != -1 && y > maxY) {
            return false;
        }

        return true;
    }

    /**
     * Checks if a player has permission to bypass spawn protection.
     */
    public boolean canBypass(@Nonnull UUID playerUuid) {
        return PermissionsModule.get().hasPermission(playerUuid, BYPASS_PERMISSION);
    }

}
