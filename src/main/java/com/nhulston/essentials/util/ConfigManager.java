package com.nhulston.essentials.util;

import org.tomlj.Toml;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigManager {
    private static final int DEFAULT_MAX_HOMES = 5;
    private static final String DEFAULT_CHAT_FORMAT = "&7%player%&f: %message%";
    private static final int DEFAULT_SPAWN_PROTECTION_RADIUS = 16;

    private final Path configPath;
    private int maxHomes = DEFAULT_MAX_HOMES;

    // Chat settings
    private boolean chatEnabled = true;
    private String chatFallbackFormat = DEFAULT_CHAT_FORMAT;
    private final LinkedHashMap<String, String> chatFormats = new LinkedHashMap<>();

    // Build settings
    private boolean disableBuilding = false;

    // Spawn settings
    private boolean firstJoinSpawnEnabled = true;
    private boolean everyJoinSpawnEnabled = false;
    private boolean deathSpawnEnabled = true;

    // Spawn protection settings
    private boolean spawnProtectionEnabled = true;
    private int spawnProtectionRadius = DEFAULT_SPAWN_PROTECTION_RADIUS;
    private int spawnProtectionMinY = -1;
    private int spawnProtectionMaxY = -1;
    private boolean spawnProtectionPreventPvp = true;
    private boolean spawnProtectionShowTitles = true;
    private String spawnProtectionEnterTitle = "Entering Spawn";
    private String spawnProtectionEnterSubtitle = "This is a protected area";
    private String spawnProtectionExitTitle = "Leaving Spawn";
    private String spawnProtectionExitSubtitle = "You can now build";

    public ConfigManager(@Nonnull Path dataFolder) {
        this.configPath = dataFolder.resolve("config.toml");
        load();
    }

    private void load() {
        if (!Files.exists(configPath)) {
            createDefault();
        }

        try {
            TomlParseResult config = Toml.parse(configPath);

            if (config.hasErrors()) {
                config.errors().forEach(error -> Log.error("Config error: " + error.toString()));
                Log.warning("Using default config values due to errors.");
                return;
            }

            // Homes config
            maxHomes = Math.toIntExact(config.getLong("homes.max-homes", () -> (long) DEFAULT_MAX_HOMES));

            // Chat config
            chatEnabled = config.getBoolean("chat.enabled", () -> true);
            chatFallbackFormat = config.getString("chat.fallback-format", () -> DEFAULT_CHAT_FORMAT);

            // Load chat formats (preserve order for priority)
            chatFormats.clear();
            TomlTable formatsTable = config.getTable("chat.formats");
            if (formatsTable != null) {
                for (String group : formatsTable.keySet()) {
                    String format = formatsTable.getString(group);
                    if (format != null) {
                        chatFormats.put(group.toLowerCase(), format);
                    }
                }
            }

            // Build config
            disableBuilding = config.getBoolean("build.disable-building", () -> false);

            // Spawn config
            firstJoinSpawnEnabled = config.getBoolean("spawn.first-join", () -> true);
            everyJoinSpawnEnabled = config.getBoolean("spawn.every-join", () -> false);
            deathSpawnEnabled = config.getBoolean("spawn.death-spawn", () -> true);

            // Spawn protection config
            spawnProtectionEnabled = config.getBoolean("spawn-protection.enabled", () -> true);
            spawnProtectionRadius = Math.toIntExact(config.getLong("spawn-protection.radius", 
                    () -> (long) DEFAULT_SPAWN_PROTECTION_RADIUS));
            spawnProtectionMinY = Math.toIntExact(config.getLong("spawn-protection.min-y", () -> -1L));
            spawnProtectionMaxY = Math.toIntExact(config.getLong("spawn-protection.max-y", () -> -1L));
            spawnProtectionPreventPvp = config.getBoolean("spawn-protection.prevent-pvp", () -> true);
            spawnProtectionShowTitles = config.getBoolean("spawn-protection.show-titles", () -> true);
            spawnProtectionEnterTitle = config.getString("spawn-protection.enter-title", () -> "Entering Spawn");
            spawnProtectionEnterSubtitle = config.getString("spawn-protection.enter-subtitle", () -> "This is a protected area");
            spawnProtectionExitTitle = config.getString("spawn-protection.exit-title", () -> "Leaving Spawn");
            spawnProtectionExitSubtitle = config.getString("spawn-protection.exit-subtitle", () -> "You can now build");

            Log.info("Config loaded!");
        } catch (IOException e) {
            Log.error("Failed to load config: " + e.getMessage());
            Log.warning("Using default config values.");
        }
    }

    private void createDefault() {
        try {
            Files.createDirectories(configPath.getParent());

            try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.toml")) {
                if (is != null) {
                    Files.copy(is, configPath);
                    Log.info("Created default config.");
                    return;
                }
            }

            Log.error("Could not find config.toml in resources.");
        } catch (IOException e) {
            Log.error("Failed to create default config: " + e.getMessage());
        }
    }

    public int getMaxHomes() {
        return maxHomes;
    }

    public boolean isChatEnabled() {
        return chatEnabled;
    }

    @Nonnull
    public String getChatFallbackFormat() {
        return chatFallbackFormat;
    }

    @Nonnull
    public Map<String, String> getChatFormats() {
        return chatFormats;
    }

    public boolean isBuildingDisabled() {
        return disableBuilding;
    }

    public boolean isFirstJoinSpawnEnabled() {
        return firstJoinSpawnEnabled;
    }

    public boolean isEveryJoinSpawnEnabled() {
        return everyJoinSpawnEnabled;
    }

    public boolean isDeathSpawnEnabled() {
        return deathSpawnEnabled;
    }

    public boolean isSpawnProtectionEnabled() {
        return spawnProtectionEnabled;
    }

    public int getSpawnProtectionRadius() {
        return spawnProtectionRadius;
    }

    public boolean isSpawnProtectionPreventPvp() {
        return spawnProtectionPreventPvp;
    }

    public int getSpawnProtectionMinY() {
        return spawnProtectionMinY;
    }

    public int getSpawnProtectionMaxY() {
        return spawnProtectionMaxY;
    }

    public boolean isSpawnProtectionShowTitles() {
        return spawnProtectionShowTitles;
    }

    @Nonnull
    public String getSpawnProtectionEnterTitle() {
        return spawnProtectionEnterTitle;
    }

    @Nonnull
    public String getSpawnProtectionEnterSubtitle() {
        return spawnProtectionEnterSubtitle;
    }

    @Nonnull
    public String getSpawnProtectionExitTitle() {
        return spawnProtectionExitTitle;
    }

    @Nonnull
    public String getSpawnProtectionExitSubtitle() {
        return spawnProtectionExitSubtitle;
    }
}
