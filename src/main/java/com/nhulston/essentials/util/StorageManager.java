package com.nhulston.essentials.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.nhulston.essentials.models.PlayerData;
import com.nhulston.essentials.models.Spawn;
import com.nhulston.essentials.models.Warp;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class StorageManager {
    private final Path dataFolder;
    private final Path playersFolder;
    private final Gson gson;
    private final ConcurrentHashMap<UUID, PlayerData> cache;
    private final ConcurrentHashMap<String, Warp> warps;
    private volatile Spawn spawn;

    private static final Type WARPS_TYPE = new TypeToken<Map<String, Warp>>(){}.getType();

    public StorageManager(@Nonnull Path dataFolder) {
        this.dataFolder = dataFolder;
        this.playersFolder = dataFolder.resolve("players");
        this.gson = new GsonBuilder().create();
        this.cache = new ConcurrentHashMap<>();
        this.warps = new ConcurrentHashMap<>();

        try {
            Files.createDirectories(this.playersFolder);
        } catch (IOException e) {
            Log.error("Failed to create players folder: " + e.getMessage());
        }

        loadWarps();
        loadSpawn();
    }

    // Player data methods

    @Nonnull
    public PlayerData getPlayerData(@Nonnull UUID playerUuid) {
        return cache.computeIfAbsent(playerUuid, this::loadPlayerData);
    }

    public void savePlayerData(@Nonnull UUID playerUuid) {
        PlayerData data = cache.get(playerUuid);
        if (data != null) {
            savePlayerDataAsync(playerUuid, data);
        }
    }

    @Nonnull
    private PlayerData loadPlayerData(@Nonnull UUID playerUuid) {
        Path file = getPlayerFile(playerUuid);
        if (Files.exists(file)) {
            try {
                String json = Files.readString(file);
                PlayerData data = gson.fromJson(json, PlayerData.class);
                if (data != null) {
                    return data;
                }
            } catch (IOException e) {
                Log.warning("Failed to load player data for " + playerUuid + ": " + e.getMessage());
            }
        }
        return new PlayerData();
    }

    private void savePlayerDataAsync(@Nonnull UUID playerUuid, @Nonnull PlayerData data) {
        CompletableFuture.runAsync(() -> {
            Path file = getPlayerFile(playerUuid);
            try {
                String json = gson.toJson(data);
                Files.writeString(file, json);
            } catch (IOException e) {
                Log.error("Failed to save player data for " + playerUuid + ": " + e.getMessage());
            }
        });
    }

    @Nonnull
    private Path getPlayerFile(@Nonnull UUID playerUuid) {
        return playersFolder.resolve(playerUuid + ".json");
    }

    public void unloadPlayer(@Nonnull UUID playerUuid) {
        cache.remove(playerUuid);
    }

    /**
     * Checks if a player has joined the server before (player data file exists).
     */
    public boolean hasPlayerJoined(@Nonnull UUID playerUuid) {
        Path playerFile = getPlayerFile(playerUuid);
        return Files.exists(playerFile);
    }

    /**
     * Marks a player as having joined by creating their data file.
     */
    public void markPlayerJoined(@Nonnull UUID playerUuid) {
        getPlayerData(playerUuid);
        savePlayerData(playerUuid);
    }

    // Warp methods
    @Nonnull
    public Map<String, Warp> getWarps() {
        return warps;
    }

    public Warp getWarp(@Nonnull String name) {
        return warps.get(name.toLowerCase());
    }

    public void setWarp(@Nonnull String name, @Nonnull Warp warp) {
        warps.put(name.toLowerCase(), warp);
        saveWarpsAsync();
    }

    public boolean deleteWarp(@Nonnull String name) {
        if (warps.remove(name.toLowerCase()) != null) {
            saveWarpsAsync();
            return true;
        }
        return false;
    }

    private void loadWarps() {
        Path file = dataFolder.resolve("warps.json");
        if (Files.exists(file)) {
            try {
                String json = Files.readString(file);
                Map<String, Warp> loaded = gson.fromJson(json, WARPS_TYPE);
                if (loaded != null) {
                    warps.putAll(loaded);
                }
            } catch (IOException e) {
                Log.warning("Failed to load warps: " + e.getMessage());
            }
        }
    }

    private void saveWarpsAsync() {
        CompletableFuture.runAsync(() -> {
            Path file = dataFolder.resolve("warps.json");
            try {
                String json = gson.toJson(warps);
                Files.writeString(file, json);
            } catch (IOException e) {
                Log.error("Failed to save warps: " + e.getMessage());
            }
        });
    }

    // Spawn methods
    @Nullable
    public Spawn getSpawn() {
        return spawn;
    }

    public void setSpawn(@Nonnull Spawn spawn) {
        this.spawn = spawn;
        saveSpawnAsync();
    }

    private void loadSpawn() {
        Path file = dataFolder.resolve("spawn.json");
        if (Files.exists(file)) {
            try {
                String json = Files.readString(file);
                Spawn loaded = gson.fromJson(json, Spawn.class);
                if (loaded != null) {
                    this.spawn = loaded;
                }
            } catch (IOException e) {
                Log.warning("Failed to load spawn: " + e.getMessage());
            }
        }
    }

    private void saveSpawnAsync() {
        CompletableFuture.runAsync(() -> {
            Path file = dataFolder.resolve("spawn.json");
            try {
                String json = gson.toJson(spawn);
                Files.writeString(file, json);
            } catch (IOException e) {
                Log.error("Failed to save spawn: " + e.getMessage());
            }
        });
    }

    public void shutdown() {
        // Save player data
        for (Map.Entry<UUID, PlayerData> entry : cache.entrySet()) {
            Path file = getPlayerFile(entry.getKey());
            try {
                String json = gson.toJson(entry.getValue());
                Files.writeString(file, json);
            } catch (IOException e) {
                Log.error("Failed to save player data on shutdown for " + entry.getKey() + ": " + e.getMessage());
            }
        }
        cache.clear();

        // Save warps
        Path warpsFile = dataFolder.resolve("warps.json");
        try {
            String json = gson.toJson(warps);
            Files.writeString(warpsFile, json);
        } catch (IOException e) {
            Log.error("Failed to save warps on shutdown: " + e.getMessage());
        }

        // Save spawn
        if (spawn != null) {
            Path spawnFile = dataFolder.resolve("spawn.json");
            try {
                String json = gson.toJson(spawn);
                Files.writeString(spawnFile, json);
            } catch (IOException e) {
                Log.error("Failed to save spawn on shutdown: " + e.getMessage());
            }
        }
    }
}
