package com.nhulston.essentials.managers;

import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.nhulston.essentials.models.Home;
import com.nhulston.essentials.models.PlayerData;
import com.nhulston.essentials.util.ConfigManager;
import com.nhulston.essentials.util.StorageManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class HomeManager {
    private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");
    private static final int MAX_NAME_LENGTH = 16;
    private static final String DEFAULT_HOME_NAME = "home";
    private static final String HOME_LIMIT_PERMISSION_PREFIX = "essentials.homes.";

    private final StorageManager storageManager;
    private final ConfigManager configManager;

    public HomeManager(@Nonnull StorageManager storageManager, @Nonnull ConfigManager configManager) {
        this.storageManager = storageManager;
        this.configManager = configManager;
    }

    @Nonnull
    public String getDefaultHomeName() {
        return DEFAULT_HOME_NAME;
    }

    /**
     * Gets the maximum number of homes for a player based on their permissions.
     * Checks each configured tier and returns the highest limit the player has permission for.
     */
    public int getMaxHomes(@Nonnull UUID playerUuid) {
        Map<String, Integer> limits = configManager.getHomeLimits();
        int maxLimit = 0;

        for (Map.Entry<String, Integer> entry : limits.entrySet()) {
            String tier = entry.getKey();
            int limit = entry.getValue();
            String permission = HOME_LIMIT_PERMISSION_PREFIX + tier;

            if (PermissionsModule.get().hasPermission(playerUuid, permission)) {
                maxLimit = Math.max(maxLimit, limit);
            }
        }

        return maxLimit;
    }

    @Nullable
    public String validateHomeName(@Nonnull String name) {
        if (name.isEmpty()) {
            return "Home name cannot be empty.";
        }
        if (name.length() > MAX_NAME_LENGTH) {
            return "Home name cannot be longer than " + MAX_NAME_LENGTH + " characters.";
        }
        if (!VALID_NAME_PATTERN.matcher(name).matches()) {
            return "Home name must be alphanumeric only.";
        }
        return null;
    }

    @Nullable
    public String setHome(@Nonnull UUID playerUuid, @Nonnull String name, @Nonnull String world,
                          double x, double y, double z, float yaw, float pitch) {
        String validationError = validateHomeName(name);
        if (validationError != null) {
            return validationError;
        }

        PlayerData data = storageManager.getPlayerData(playerUuid);
        String lowerName = name.toLowerCase();

        int maxHomes = getMaxHomes(playerUuid);
        if (data.getHome(lowerName) == null && data.getHomeCount() >= maxHomes) {
            return "You have reached the maximum of " + maxHomes + " homes.";
        }

        Home home = new Home(world, x, y, z, yaw, pitch, System.currentTimeMillis());
        data.setHome(lowerName, home);
        storageManager.savePlayerData(playerUuid);

        return null;
    }

    @Nullable
    public Home getHome(@Nonnull UUID playerUuid, @Nonnull String name) {
        return storageManager.getPlayerData(playerUuid).getHome(name);
    }

    @Nonnull
    public Map<String, Home> getHomes(@Nonnull UUID playerUuid) {
        return storageManager.getPlayerData(playerUuid).getHomes();
    }

    public boolean deleteHome(@Nonnull UUID playerUuid, @Nonnull String name) {
        PlayerData data = storageManager.getPlayerData(playerUuid);
        if (data.getHome(name) == null) {
            return false;
        }
        data.deleteHome(name);
        storageManager.savePlayerData(playerUuid);
        return true;
    }
}
