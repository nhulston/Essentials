package com.nhulston.essentials.util;

import org.tomlj.Toml;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {
    private static final String DEFAULT_CHAT_FORMAT = "&7%player%&f: %message%";
    private static final int DEFAULT_SPAWN_PROTECTION_RADIUS = 16;
    private static final int DEFAULT_TELEPORT_DELAY = 3;
    private static final int DEFAULT_RTP_COOLDOWN = 300;

    /**
     * Represents a chat format configuration entry with group name and format string.
     * Used to maintain ordering of chat formats for priority-based matching.
     */
    public record ChatFormat(@Nonnull String group, @Nonnull String format) {}

    private final Path configPath;

    // Home limits by permission tier (e.g., essentials.homes.default -> 5)
    private final ConcurrentHashMap<String, Integer> homeLimits = new ConcurrentHashMap<>();

    // Chat settings
    private volatile boolean chatEnabled = true;
    private volatile String chatFallbackFormat = DEFAULT_CHAT_FORMAT;
    private volatile List<ChatFormat> chatFormats = List.of();

    // Build settings
    private volatile boolean disableBuilding = false;

    // Spawn settings
    private volatile boolean firstJoinSpawnEnabled = true;
    private volatile boolean everyJoinSpawnEnabled = false;
    private volatile boolean deathSpawnEnabled = true;

    // Teleport settings
    private volatile int teleportDelay = DEFAULT_TELEPORT_DELAY;

    // TPA settings
    private volatile int tpaExpiration = 60;

    // Spawn protection settings
    private volatile boolean spawnProtectionEnabled = true;
    private volatile int spawnProtectionRadius = DEFAULT_SPAWN_PROTECTION_RADIUS;
    private volatile int spawnProtectionMinY = -1;
    private volatile int spawnProtectionMaxY = -1;
    private volatile boolean spawnProtectionInvulnerable = true;
    private volatile boolean spawnProtectionShowTitles = true;
    private volatile String spawnProtectionEnterTitle = "Entering Spawn";
    private volatile String spawnProtectionEnterSubtitle = "This is a protected area";
    private volatile String spawnProtectionExitTitle = "Leaving Spawn";
    private volatile String spawnProtectionExitSubtitle = "You can now build";

    // RTP settings
    private volatile int rtpCooldown = DEFAULT_RTP_COOLDOWN;
    private volatile String rtpDefaultWorld = "default";
    private final ConcurrentHashMap<String, Integer> rtpWorlds = new ConcurrentHashMap<>();

    // AFK settings
    private long afkKickTime = 0L;
    private String afkKickMessage = "You have been kicked for idling more than %period% seconds!";

    // MOTD settings
    private volatile boolean motdEnabled = true;
    private volatile String motdMessage = "&6Welcome to the server, &e%player%&6!";

    // Sleep settings
    private volatile boolean sleepEnabled = true;
    private volatile int sleepPercentage = 20;

    // Shout settings
    private volatile String shoutPrefix = "&0[&7Broadcast&0] &f";

    // Repair settings
    private volatile int repairCooldown = 43200;

    // Join/Leave message settings
    private volatile boolean joinMessageEnabled = true;
    private volatile String joinMessage = "&e%player% &ajoined the game";
    private volatile String firstJoinMessage = "&e%player% &6joined the game for the first time!";
    private volatile boolean leaveMessageEnabled = true;
    private volatile String leaveMessage = "&e%player% &cleft the game";
    
    // Rules settings
    private volatile String rulesMessage = "&6=== Server Rules ===\n&e1. &fBe respectful to all players\n&e2. &fNo griefing or stealing\n&e3. &fNo hacking or cheating\n&e4. &fHave fun!";

    // Starter kit settings
    private volatile boolean starterKitEnabled = false;
    private volatile String starterKitName = "";

    // Update notification settings
    private volatile boolean updateNotifyEnabled = true;

    public ConfigManager(@Nonnull Path dataFolder) {
        this.configPath = dataFolder.resolve("config.toml");
        load();
    }

    private void load() {
        if (!Files.exists(configPath)) {
            TomlMigrationHelper.createDefault(configPath, "config.toml");
        } else {
            TomlMigrationHelper.migrateToml(configPath, "config.toml");
        }

        try {
            String configContent = TomlMigrationHelper.readWithBom(configPath);
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
            TomlTable formatsTable = config.getTable("chat.formats");
            if (formatsTable != null) {
                List<ChatFormat> formats = new ArrayList<>();
                for (String group : formatsTable.keySet()) {
                    String format = formatsTable.getString(group);
                    if (format != null) {
                        formats.add(new ChatFormat(group.toLowerCase(), format));
                    }
                }
                chatFormats = List.copyOf(formats);
            } else {
                chatFormats = List.of();
            }

            // Build config
            disableBuilding = config.getBoolean("build.disable-building", () -> false);

            // Spawn config
            firstJoinSpawnEnabled = config.getBoolean("spawn.first-join", () -> true);
            everyJoinSpawnEnabled = config.getBoolean("spawn.every-join", () -> false);
            deathSpawnEnabled = config.getBoolean("spawn.death-spawn", () -> true);

            // Teleport config
            teleportDelay = getIntSafe(config, "teleport.delay", DEFAULT_TELEPORT_DELAY);

            // TPA config
            tpaExpiration = getIntSafe(config, "tpa.expiration", 60);

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

            afkKickTime = getIntSafe(config, "afk.threshold", 0);
            afkKickMessage = config.getString("afk.kick-message", () -> "You have been kicked for idling more than {0} seconds!")
                    .replace("%period%", String.valueOf(afkKickTime));

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
            
            // Rules config
            rulesMessage = config.getString("rules.message", () -> "&6=== Server Rules ===\n&e1. &fBe respectful to all players\n&e2. &fNo griefing or stealing\n&e3. &fNo hacking or cheating\n&e4. &fHave fun!");

            // Starter kit config
            starterKitEnabled = config.getBoolean("starter-kit.enabled", () -> false);
            starterKitName = config.getString("starter-kit.kit", () -> "");

            // Update notification config
            updateNotifyEnabled = config.getBoolean("updates.notify", () -> true);

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
    private int getIntSafe(@Nonnull TomlParseResult config, @Nonnull String key, int defaultValue) {
        try {
            Long value = config.getLong(key);
            return value != null ? Math.toIntExact(value) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
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
    public List<ChatFormat> getChatFormats() {
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

    public int getTpaExpiration() {
        return tpaExpiration;
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

    public boolean isAfkKickEnabled() {
        return afkKickTime > 0;
    }

    public Long getAfkKickTime() {
        return afkKickTime;
    }

    public String getAfkKickMessage() {
        return afkKickMessage;
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
    
    @Nonnull
    public String getRulesMessage() {
        return rulesMessage;
    }

    public boolean isStarterKitEnabled() {
        return starterKitEnabled;
    }

    @Nonnull
    public String getStarterKitName() {
        return starterKitName;
    }

    public boolean isUpdateNotifyEnabled() {
        return updateNotifyEnabled;
    }
}
