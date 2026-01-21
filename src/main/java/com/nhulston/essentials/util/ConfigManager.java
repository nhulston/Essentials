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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigManager {
    private static final String DEFAULT_CHAT_FORMAT = "&7%player%&f: %message%";
    private static final int DEFAULT_SPAWN_PROTECTION_RADIUS = 16;
    private static final int DEFAULT_TELEPORT_DELAY = 3;
    private static final int DEFAULT_RTP_COOLDOWN = 300;

    // Pattern to match section headers like [section], [section-name], or [section.subsection]
    private static final Pattern SECTION_PATTERN = Pattern.compile("^\\[([a-zA-Z0-9_.-]+)]\\s*$");

    private final Path configPath;

    // Home limits by permission tier (e.g., essentials.homes.default -> 5)
    private final HashMap<String, Integer> homeLimits = new HashMap<>();

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
    private int rtpCooldown = DEFAULT_RTP_COOLDOWN;
    private String rtpDefaultWorld = "default";
    private final HashMap<String, Integer> rtpWorlds = new HashMap<>();

    // MOTD settings
    private boolean motdEnabled = true;
    private String motdMessage = "&6Welcome to the server, &e%player%&6!";

    // Sleep settings
    private boolean sleepEnabled = true;
    private int sleepPercentage = 20;

    // Shout settings
    private String shoutPrefix = "&0[&7Broadcast&0] &f";

    // Repair settings
    private int repairCooldown = 43200;

    // Join/Leave message settings
    private boolean joinMessageEnabled = true;
    private String joinMessage = "&e%player% &ajoined the game";
    private String firstJoinMessage = "&e%player% &6joined the game for the first time!";
    private boolean leaveMessageEnabled = true;
    private String leaveMessage = "&e%player% &cleft the game";

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
            // Read file as bytes first to handle potential BOM
            byte[] bytes = Files.readAllBytes(configPath);
            String configContent;
            
            // Check for UTF-8 BOM and skip it if present
            if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
                configContent = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
            } else {
                configContent = new String(bytes, StandardCharsets.UTF_8);
            }
            
            TomlParseResult config = Toml.parse(configContent);

            if (config.hasErrors()) {
                config.errors().forEach(error -> Log.error("Config error: " + error.toString()));
                Log.warning("Using default config values due to errors.");
                return;
            }

            // Homes config - load permission-based limits
            homeLimits.clear();
            TomlTable homeLimitsTable = config.getTable("homes.limits");
            if (homeLimitsTable != null) {
                for (String tier : homeLimitsTable.keySet()) {
                    Long limit = homeLimitsTable.getLong(tier);
                    if (limit != null) {
                        homeLimits.put(tier.toLowerCase(), limit.intValue());
                    }
                }
            }

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
            teleportDelay = getIntSafe(config, "teleport.delay", DEFAULT_TELEPORT_DELAY);

            // Spawn protection config
            spawnProtectionEnabled = config.getBoolean("spawn-protection.enabled", () -> true);
            spawnProtectionRadius = getIntSafe(config, "spawn-protection.radius", DEFAULT_SPAWN_PROTECTION_RADIUS);
            spawnProtectionMinY = getIntSafe(config, "spawn-protection.min-y", -1);
            spawnProtectionMaxY = getIntSafe(config, "spawn-protection.max-y", -1);
            spawnProtectionInvulnerable = config.getBoolean("spawn-protection.invulnerable", () -> true);
            spawnProtectionShowTitles = config.getBoolean("spawn-protection.show-titles", () -> true);
            spawnProtectionEnterTitle = config.getString("spawn-protection.enter-title", () -> "Entering Spawn");
            spawnProtectionEnterSubtitle = config.getString("spawn-protection.enter-subtitle", () -> "This is a protected area");
            spawnProtectionExitTitle = config.getString("spawn-protection.exit-title", () -> "Leaving Spawn");
            spawnProtectionExitSubtitle = config.getString("spawn-protection.exit-subtitle", () -> "You can now build");

            // RTP config
            rtpCooldown = getIntSafe(config, "rtp.cooldown", DEFAULT_RTP_COOLDOWN);
            
            rtpWorlds.clear();
            TomlTable rtpWorldsTable = config.getTable("rtp.worlds");
            if (rtpWorldsTable != null) {
                for (String worldName : rtpWorldsTable.keySet()) {
                    Long radius = rtpWorldsTable.getLong(worldName);
                    if (radius != null) {
                        rtpWorlds.put(worldName, radius.intValue());
                    }
                }
            }

            String defaultWorld = config.getString("rtp.default-world");
            rtpDefaultWorld = defaultWorld != null ? defaultWorld : "default";

            // MOTD config
            motdEnabled = config.getBoolean("motd.enabled", () -> true);
            motdMessage = config.getString("motd.message", () -> "&6Welcome to the server, &e%player%&6!");

            // Sleep config
            sleepEnabled = config.getBoolean("sleep.enabled", () -> true);
            sleepPercentage = getIntSafe(config, "sleep.percentage", 20);

            // Shout config
            shoutPrefix = config.getString("shout.prefix", () -> "&0[&7Broadcast&0] &f");

            // Repair config
            repairCooldown = getIntSafe(config, "repair.cooldown", 43200);

            // Join/Leave messages config
            joinMessageEnabled = config.getBoolean("join-leave-messages.join-enabled", () -> true);
            joinMessage = config.getString("join-leave-messages.join-message", () -> "&8[&a+&8] &7%player%");
            firstJoinMessage = config.getString("join-leave-messages.first-join-message", () -> "&e%player% &6joined the game for the first time!");
            leaveMessageEnabled = config.getBoolean("join-leave-messages.leave-enabled", () -> true);
            leaveMessage = config.getString("join-leave-messages.leave-message", () -> "&8[&c-&8] &7%player%");

            Log.info("Config loaded!");
        } catch (Exception e) {
            Log.error("Failed to load config: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            if (e.getCause() != null) {
                Log.error("Caused by: " + e.getCause().getClass().getSimpleName() + " - " + e.getCause().getMessage());
            }
            Log.warning("Using default config values.");
        }
    }

    /**
     * Reloads the configuration from disk.
     */
    public void reload() {
        Log.info("Reloading config...");
        load();
    }

    /**
     * Migrates the user's config by adding any missing sections from the default config.
     * Preserves user's existing values and comments.
     */
    private void migrateConfig() {
        String defaultConfig = loadDefaultConfigFromResources();
        if (defaultConfig == null) {
            Log.warning("Could not load default config from resources for migration.");
            return;
        }

        try {
            // Read user config as bytes first to handle potential BOM
            byte[] bytes = Files.readAllBytes(configPath);
            String userConfig;
            
            // Check for UTF-8 BOM and skip it if present
            if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
                userConfig = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
            } else {
                userConfig = new String(bytes, StandardCharsets.UTF_8);
            }
            
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
            
        } catch (Exception e) {
            Log.warning("Config migration skipped: " + e.getClass().getName() + " - " + e.getMessage());
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
     * Finds all section names in a TOML config string.
     * Matches [section] and [section.subsection].
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
     * Extracts all sections from a TOML config string.
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

    /**
     * Safely gets an integer value from the config, with fallback to default.
     */
    private int getIntSafe(@Nonnull TomlParseResult config, @Nonnull String key, int defaultValue) {
        try {
            Long value = config.getLong(key);
            return value != null ? Math.toIntExact(value) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
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

    /**
     * Gets the home limits map (tier name -> limit).
     */
    @Nonnull
    public Map<String, Integer> getHomeLimits() {
        return homeLimits;
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

    public int getRtpCooldown() {
        return rtpCooldown;
    }

    @Nonnull
    public String getRtpDefaultWorld() {
        return rtpDefaultWorld;
    }

    /**
     * Gets the RTP radius for a specific world, or null if the world is not configured.
     */
    @Nullable
    public Integer getRtpRadius(@Nonnull String worldName) {
        return rtpWorlds.get(worldName);
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

    @Nonnull
    public String getShoutPrefix() {
        return shoutPrefix;
    }

    public int getRepairCooldown() {
        return repairCooldown;
    }

    public boolean isJoinMessageEnabled() {
        return joinMessageEnabled;
    }

    @Nonnull
    public String getJoinMessage() {
        return joinMessage;
    }

    @Nonnull
    public String getFirstJoinMessage() {
        return firstJoinMessage;
    }

    public boolean isLeaveMessageEnabled() {
        return leaveMessageEnabled;
    }

    @Nonnull
    public String getLeaveMessage() {
        return leaveMessage;
    }
}
