package com.nhulston.essentials.util;

import org.tomlj.Toml;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigManager {
    private static final int DEFAULT_MAX_HOMES = 5;
    private static final String DEFAULT_CHAT_FORMAT = "&7%player%&f: %message%";
    private static final int DEFAULT_SPAWN_PROTECTION_RADIUS = 16;
    private static final int DEFAULT_TELEPORT_DELAY = 3;
    private static final String DEFAULT_RTP_WORLD = "default";
    private static final int DEFAULT_RTP_RADIUS = 5000;
    private static final int DEFAULT_RTP_COOLDOWN = 300;

    // Pattern to match top-level section headers like [section] or [section-name]
    private static final Pattern SECTION_PATTERN = Pattern.compile("^\\[([a-zA-Z0-9_-]+)]\\s*$");

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

    // Teleport settings
    private int teleportDelay = DEFAULT_TELEPORT_DELAY;

    // Spawn protection settings
    private boolean spawnProtectionEnabled = true;
    private int spawnProtectionRadius = DEFAULT_SPAWN_PROTECTION_RADIUS;
    private int spawnProtectionMinY = -1;
    private int spawnProtectionMaxY = -1;
    private boolean spawnProtectionInvulnerable = true;
    private boolean spawnProtectionShowTitles = true;
    private String spawnProtectionEnterTitle = "Entering Spawn";
    private String spawnProtectionEnterSubtitle = "This is a protected area";
    private String spawnProtectionExitTitle = "Leaving Spawn";
    private String spawnProtectionExitSubtitle = "You can now build";

    // RTP settings
    private String rtpWorld = DEFAULT_RTP_WORLD;
    private int rtpRadius = DEFAULT_RTP_RADIUS;
    private int rtpCooldown = DEFAULT_RTP_COOLDOWN;

    // MOTD settings
    private boolean motdEnabled = true;
    private String motdMessage = "&6Welcome to the server, &e%player%&6!";

    // Sleep settings
    private boolean sleepEnabled = true;
    private int sleepPercentage = 20;

    public ConfigManager(@Nonnull Path dataFolder) {
        this.configPath = dataFolder.resolve("config.toml");
        load();
    }

    private void load() {
        if (!Files.exists(configPath)) {
            createDefault();
        } else {
            // Check for missing sections and add them
            migrateConfig();
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

            // Teleport config
            teleportDelay = Math.toIntExact(config.getLong("teleport.delay", () -> (long) DEFAULT_TELEPORT_DELAY));

            // Spawn protection config
            spawnProtectionEnabled = config.getBoolean("spawn-protection.enabled", () -> true);
            spawnProtectionRadius = Math.toIntExact(config.getLong("spawn-protection.radius", 
                    () -> (long) DEFAULT_SPAWN_PROTECTION_RADIUS));
            spawnProtectionMinY = Math.toIntExact(config.getLong("spawn-protection.min-y", () -> -1L));
            spawnProtectionMaxY = Math.toIntExact(config.getLong("spawn-protection.max-y", () -> -1L));
            spawnProtectionInvulnerable = config.getBoolean("spawn-protection.invulnerable", () -> true);
            spawnProtectionShowTitles = config.getBoolean("spawn-protection.show-titles", () -> true);
            spawnProtectionEnterTitle = config.getString("spawn-protection.enter-title", () -> "Entering Spawn");
            spawnProtectionEnterSubtitle = config.getString("spawn-protection.enter-subtitle", () -> "This is a protected area");
            spawnProtectionExitTitle = config.getString("spawn-protection.exit-title", () -> "Leaving Spawn");
            spawnProtectionExitSubtitle = config.getString("spawn-protection.exit-subtitle", () -> "You can now build");

            // RTP config
            rtpWorld = config.getString("rtp.world", () -> DEFAULT_RTP_WORLD);
            rtpRadius = Math.toIntExact(config.getLong("rtp.radius", () -> (long) DEFAULT_RTP_RADIUS));
            rtpCooldown = Math.toIntExact(config.getLong("rtp.cooldown", () -> (long) DEFAULT_RTP_COOLDOWN));

            // MOTD config
            motdEnabled = config.getBoolean("motd.enabled", () -> true);
            motdMessage = config.getString("motd.message", () -> "&6Welcome to the server, &e%player%&6!");

            // Sleep config
            sleepEnabled = config.getBoolean("sleep.enabled", () -> true);
            sleepPercentage = Math.toIntExact(config.getLong("sleep.percentage", () -> 20L));

            Log.info("Config loaded!");
        } catch (IOException e) {
            Log.error("Failed to load config: " + e.getMessage());
            Log.warning("Using default config values.");
        }
    }

    /**
     * Migrates the user's config by adding any missing sections from the default config.
     * Preserves user's existing values and comments.
     */
    private void migrateConfig() {
        String defaultConfig = loadDefaultConfigFromResources();
        if (defaultConfig == null) {
            return;
        }

        try {
            String userConfig = Files.readString(configPath, StandardCharsets.UTF_8);
            
            // Find sections in both configs
            Set<String> userSections = findTopLevelSections(userConfig);
            Map<String, String> defaultSections = extractSections(defaultConfig);
            
            // Find missing sections
            List<String> missingSections = new ArrayList<>();
            for (String section : defaultSections.keySet()) {
                if (!userSections.contains(section)) {
                    missingSections.add(section);
                }
            }
            
            if (missingSections.isEmpty()) {
                return;
            }
            
            // Append missing sections to user config
            StringBuilder newConfig = new StringBuilder(userConfig);
            if (!userConfig.endsWith("\n")) {
                newConfig.append("\n");
            }
            
            for (String section : missingSections) {
                newConfig.append("\n");
                newConfig.append(defaultSections.get(section));
                Log.info("Added missing config section: [" + section + "]");
            }
            
            Files.writeString(configPath, newConfig.toString(), StandardCharsets.UTF_8);
            Log.info("Config migrated with " + missingSections.size() + " new section(s).");
            
        } catch (IOException e) {
            Log.warning("Failed to migrate config: " + e.getMessage());
        }
    }

    /**
     * Loads the default config.toml content from resources.
     */
    @Nullable
    private String loadDefaultConfigFromResources() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.toml")) {
            if (is == null) {
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
        } catch (IOException e) {
            Log.warning("Failed to load default config from resources: " + e.getMessage());
            return null;
        }
    }

    /**
     * Finds all top-level section names in a TOML config string.
     * Only matches [section], not [section.subsection].
     */
    @Nonnull
    private Set<String> findTopLevelSections(@Nonnull String config) {
        LinkedHashMap<String, Boolean> map = new LinkedHashMap<>();
        
        for (String line : config.split("\n")) {
            Matcher matcher = SECTION_PATTERN.matcher(line.trim());
            if (matcher.matches()) {
                map.put(matcher.group(1), true);
            }
        }
        
        return map.keySet();
    }

    /**
     * Extracts all top-level sections from a TOML config string.
     * Returns a map of section name -> full section content (including header, comments, and values).
     */
    @Nonnull
    private Map<String, String> extractSections(@Nonnull String config) {
        Map<String, String> sections = new LinkedHashMap<>();
        String[] lines = config.split("\n");
        
        String currentSection = null;
        StringBuilder currentContent = new StringBuilder();
        List<String> pendingComments = new ArrayList<>();
        
        for (String line : lines) {
            Matcher matcher = SECTION_PATTERN.matcher(line.trim());
            
            if (matcher.matches()) {
                // Save previous section
                if (currentSection != null) {
                    sections.put(currentSection, currentContent.toString());
                }
                
                // Start new section
                currentSection = matcher.group(1);
                currentContent = new StringBuilder();
                
                // Add any pending comments (comments before this section header)
                for (String comment : pendingComments) {
                    currentContent.append(comment).append("\n");
                }
                pendingComments.clear();
                
                currentContent.append(line).append("\n");
            } else if (currentSection != null) {
                // Add line to current section
                currentContent.append(line).append("\n");
            } else {
                // Lines before any section (could be comments for first section)
                if (line.trim().startsWith("#") || line.trim().isEmpty()) {
                    pendingComments.add(line);
                }
            }
        }
        
        // Save last section
        if (currentSection != null) {
            sections.put(currentSection, currentContent.toString());
        }
        
        return sections;
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

    public int getTeleportDelay() {
        return teleportDelay;
    }

    public boolean isSpawnProtectionEnabled() {
        return spawnProtectionEnabled;
    }

    public int getSpawnProtectionRadius() {
        return spawnProtectionRadius;
    }

    public boolean isSpawnProtectionInvulnerable() {
        return spawnProtectionInvulnerable;
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

    @Nonnull
    public String getRtpWorld() {
        return rtpWorld;
    }

    public int getRtpRadius() {
        return rtpRadius;
    }

    public int getRtpCooldown() {
        return rtpCooldown;
    }

    public boolean isMotdEnabled() {
        return motdEnabled;
    }

    @Nonnull
    public String getMotdMessage() {
        return motdMessage;
    }

    public boolean isSleepEnabled() {
        return sleepEnabled;
    }

    public int getSleepPercentage() {
        return sleepPercentage;
    }
}
