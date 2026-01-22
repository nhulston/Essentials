package com.nhulston.essentials.commands.rtp;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.managers.BackManager;
import com.nhulston.essentials.managers.TeleportManager;
import com.nhulston.essentials.models.PlayerData;
import com.nhulston.essentials.util.ConfigManager;
import com.nhulston.essentials.util.CooldownUtil;
import com.nhulston.essentials.util.Msg;
import com.nhulston.essentials.util.StorageManager;
import com.nhulston.essentials.util.TeleportUtil;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Command to randomly teleport a player within a configured radius.
 * Usage: /rtp
 */
public class RtpCommand extends AbstractPlayerCommand {
    private static final int MAX_ATTEMPTS = 5;
    private static final String COOLDOWN_BYPASS_PERMISSION = "essentials.rtp.cooldown.bypass";

    private final ConfigManager configManager;
    private final StorageManager storageManager;
    private final TeleportManager teleportManager;
    private final BackManager backManager;

    public RtpCommand(@Nonnull ConfigManager configManager, @Nonnull StorageManager storageManager,
                      @Nonnull TeleportManager teleportManager, @Nonnull BackManager backManager) {
        super("rtp", "Randomly teleport to a location");
        this.configManager = configManager;
        this.storageManager = storageManager;
        this.teleportManager = teleportManager;
        this.backManager = backManager;

        requirePermission("essentials.rtp");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        UUID playerUuid = playerRef.getUuid();
        PlayerData data = storageManager.getPlayerData(playerUuid);

        // Check cooldown (skip if player has bypass permission)
        int cooldownSeconds = configManager.getRtpCooldown();
        boolean bypassCooldown = PermissionsModule.get().hasPermission(playerUuid, COOLDOWN_BYPASS_PERMISSION);
        if (cooldownSeconds > 0 && !bypassCooldown) {
            Long lastUse = data.getLastRtpTime();
            if (lastUse != null) {
                long elapsed = (System.currentTimeMillis() - lastUse) / 1000;
                long remaining = cooldownSeconds - elapsed;
                if (remaining > 0) {
                    Msg.fail(context, "RTP is on cooldown. " + CooldownUtil.formatCooldown(remaining) + " remaining.");
                    return;
                }
            }
        }

        // Determine which world to RTP in
        String currentWorldName = world.getName();
        String rtpWorldName;
        Integer radius = configManager.getRtpRadius(currentWorldName);
        
        if (radius != null) {
            // Player's current world is configured for RTP
            rtpWorldName = currentWorldName;
        } else {
            // Fall back to default world
            rtpWorldName = configManager.getRtpDefaultWorld();
            radius = configManager.getRtpRadius(rtpWorldName);
            
            if (radius == null) {
                Msg.fail(context, "RTP is not enabled in this world.");
                return;
            }
        }

        // Verify the world exists
        World rtpWorld = Universe.get().getWorld(rtpWorldName);
        if (rtpWorld == null) {
            Msg.fail(context, "RTP world '" + rtpWorldName + "' is not loaded.");
            return;
        }

        boolean isCrossWorld = !rtpWorldName.equals(currentWorldName);
        
        // Save current location before teleporting
        Vector3d currentPos = playerRef.getTransform().getPosition();
        Vector3f currentRot = playerRef.getTransform().getRotation();
        backManager.setTeleportLocation(playerUuid, currentWorldName,
            currentPos.getX(), currentPos.getY(), currentPos.getZ(),
            currentRot.getY(), currentRot.getX());

        if (isCrossWorld) {
            // Cross-world RTP - use async chunk loading
            // Capture start position now, on the correct thread
            Vector3d startPosition = playerRef.getTransform().getPosition().clone();

            findSafeLocationAsync(rtpWorld, radius, 0)
                .thenAccept(result -> {
                    if (result == null) {
                        Msg.fail(playerRef, "Could not find a safe location after " + MAX_ATTEMPTS + " attempts. Try again.");
                        return;
                    }
                    
                    // Execute teleport back on the player's current world thread
                    world.execute(() -> {
                        teleportManager.queueTeleport(
                            playerRef, ref, store, startPosition,
                                rtpWorldName, result.x, result.y, result.z,
                            0.0f, 0.0f,
                            "Randomly teleported!",
                            () -> {
                                data.setLastRtpTime(System.currentTimeMillis());
                                storageManager.savePlayerData(playerUuid);
                            }
                        );
                    });
                })
                .exceptionally(ex -> {
                    Msg.fail(playerRef, "RTP failed. Please try again.");
                    return null;
                });
        } else {
            // Same-world RTP - use sync chunk access
            findSafeLocationSync(rtpWorld, radius, playerRef, ref, store, rtpWorldName, data, playerUuid);
        }
    }

    /**
     * Synchronously finds a safe RTP location (for same-world teleports).
     */
    private void findSafeLocationSync(World rtpWorld, int radius, PlayerRef playerRef, 
                                       Ref<EntityStore> ref, Store<EntityStore> store,
                                       String rtpWorldName, PlayerData data, UUID playerUuid) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            double x = random.nextDouble(-radius, radius);
            double z = random.nextDouble(-radius, radius);

            Double safeY = TeleportUtil.findSafeRtpY(rtpWorld, x, z);
            
            if (safeY != null) {
                Vector3d startPosition = playerRef.getTransform().getPosition();

                teleportManager.queueTeleport(
                    playerRef, ref, store, startPosition,
                    rtpWorldName, x, safeY, z,
                    0.0f, 0.0f,
                    "Randomly teleported!",
                    () -> {
                        data.setLastRtpTime(System.currentTimeMillis());
                        storageManager.savePlayerData(playerUuid);
                    }
                );
                return;
            }
        }

        Msg.fail(playerRef, "Could not find a safe location after " + MAX_ATTEMPTS + " attempts. Try again.");
    }

    /**
     * Asynchronously finds a safe RTP location (for cross-world teleports).
     * Recursively tries up to MAX_ATTEMPTS locations.
     */
    private CompletableFuture<RtpLocation> findSafeLocationAsync(World rtpWorld, int radius, int attempt) {
        if (attempt >= MAX_ATTEMPTS) {
            return CompletableFuture.completedFuture(null);
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        double x = random.nextDouble(-radius, radius);
        double z = random.nextDouble(-radius, radius);

        return TeleportUtil.findSafeRtpYAsync(rtpWorld, x, z)
            .thenCompose(safeY -> {
                if (safeY != null) {
                    return CompletableFuture.completedFuture(new RtpLocation(x, safeY, z));
                }
                // Try next attempt
                return findSafeLocationAsync(rtpWorld, radius, attempt + 1);
            });
    }

    /**
     * Simple data class to hold RTP coordinates.
     */
    private static class RtpLocation {
        final double x, y, z;
        
        RtpLocation(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
