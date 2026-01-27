package com.nhulston.essentials.managers;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.Essentials;
import com.nhulston.essentials.util.ConfigManager;
import com.nhulston.essentials.util.Log;
import com.nhulston.essentials.util.MessageManager;
import com.nhulston.essentials.util.Msg;
import com.nhulston.essentials.util.SoundUtil;
import com.nhulston.essentials.util.TeleportUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages delayed teleports with movement cancellation.
 * Players must stand still during the teleport delay or the teleport is canceled.
 * Teleport destinations are stored as data and executed via buffer.run() callback.
 */
public class TeleportManager {
    private static final String BYPASS_PERMISSION = "essentials.teleport.bypass";
    private static final double CANCEL_DISTANCE = 2.0;
    private static final String TELEPORT_SOUND = "SFX_Portal_Neutral_Teleport_Local";

    private final ConfigManager configManager;
    private final MessageManager messages;
    private final ConcurrentHashMap<UUID, PendingTeleport> pendingTeleports = new ConcurrentHashMap<>();

    public TeleportManager(@Nonnull ConfigManager configManager) {
        this.configManager = configManager;
        this.messages = Essentials.getInstance().getMessageManager();
    }

    /**
     * Queues a coordinate-based teleport (for homes, warps, spawn).
     */
    public void queueTeleport(@Nonnull PlayerRef playerRef, @Nonnull Ref<EntityStore> entityRef,
                              @Nonnull Store<EntityStore> store, @Nonnull Vector3d startPosition,
                              @Nonnull String worldName, double x, double y, double z,
                              float yaw, float pitch, @Nullable String successMessage) {
        queueTeleport(playerRef, entityRef, store, startPosition, worldName, x, y, z, yaw, pitch, successMessage, null);
    }

    /**
     * Queues a coordinate-based teleport with an optional success callback.
     */
    public void queueTeleport(@Nonnull PlayerRef playerRef, @Nonnull Ref<EntityStore> entityRef,
                              @Nonnull Store<EntityStore> store, @Nonnull Vector3d startPosition,
                              @Nonnull String worldName, double x, double y, double z,
                              float yaw, float pitch, @Nullable String successMessage,
                              @Nullable Runnable onSuccess) {
        UUID playerUuid = playerRef.getUuid();
        int delay = configManager.getTeleportDelay();

        // Check bypass permission or if delay is 0
        if (delay <= 0 || PermissionsModule.get().hasPermission(playerUuid, BYPASS_PERMISSION)) {
            // Execute immediately
            String error = TeleportUtil.teleportSafe(store, entityRef, worldName, x, y, z, yaw, pitch);
            if (error != null) {
                Msg.send(playerRef, error);
            } else {
                SoundUtil.playSound(playerRef, TELEPORT_SOUND);
                if (successMessage != null) {
                    Msg.send(playerRef, successMessage);
                }
                if (onSuccess != null) {
                    onSuccess.run();
                }
            }
            return;
        }

        // Create pending teleport and add atomically
        TeleportDestination destination = new TeleportDestination(worldName, x, y, z, yaw, pitch);
        PendingTeleport pending = new PendingTeleport(playerRef, startPosition, destination, successMessage, delay, onSuccess);
        PendingTeleport existing = pendingTeleports.putIfAbsent(playerUuid, pending);
        
        if (existing != null) {
            Msg.send(playerRef, messages.get("teleport.already-pending"));
            return;
        }

        Msg.send(playerRef, messages.get("teleport.countdown", Map.of("delay", String.valueOf(delay))));
    }

    /**
     * Queues a player-to-player teleport (for TPA).
     */
    public void queueTeleportToPlayer(@Nonnull PlayerRef playerRef, @Nonnull Ref<EntityStore> entityRef,
                                      @Nonnull Store<EntityStore> store, @Nonnull Vector3d startPosition,
                                      @Nonnull PlayerRef targetPlayer, @Nullable String successMessage) {
        UUID playerUuid = playerRef.getUuid();
        int delay = configManager.getTeleportDelay();

        // Check bypass permission or if delay is 0
        if (delay <= 0 || PermissionsModule.get().hasPermission(playerUuid, BYPASS_PERMISSION)) {
            // Execute immediately
            TeleportUtil.teleportToPlayer(playerRef, targetPlayer);
            SoundUtil.playSound(playerRef, TELEPORT_SOUND);
            if (successMessage != null) {
                Msg.send(playerRef, successMessage);
            }
            return;
        }

        // Create pending teleport with target player UUID and add atomically
        PendingTeleport pending = new PendingTeleport(playerRef, startPosition, targetPlayer.getUuid(), 
                                                       targetPlayer.getUsername(), successMessage, delay);
        PendingTeleport existing = pendingTeleports.putIfAbsent(playerUuid, pending);
        
        if (existing != null) {
            Msg.send(playerRef, messages.get("teleport.already-pending"));
            return;
        }

        Msg.send(playerRef, messages.get("teleport.countdown", Map.of("delay", String.valueOf(delay))));
    }

    /**
     * Ticks pending teleports. Called from the tick system.
     */
    public void tick(@Nonnull UUID playerUuid, @Nonnull Ref<EntityStore> currentRef,
                     @Nonnull Vector3d currentPosition, float deltaTime,
                     @Nonnull CommandBuffer<EntityStore> buffer) {
        PendingTeleport pending = pendingTeleports.get(playerUuid);
        if (pending == null) {
            return;
        }

        // Check movement
        double distanceSquared = pending.getStartPosition().distanceSquaredTo(currentPosition);
        double maxDistanceSquared = CANCEL_DISTANCE * CANCEL_DISTANCE;

        if (distanceSquared > maxDistanceSquared) {
            cancelTeleport(playerUuid, messages.get("teleport.cancelled-moved"));
            return;
        }

        // Update elapsed time and check if ready to teleport
        pending.addElapsedTime(deltaTime);
        if (pending.isReady()) {
            executeTeleport(playerUuid, currentRef, buffer);
        }
    }

    /**
     * Executes a pending teleport using buffer.run() to defer execution.
     */
    private void executeTeleport(@Nonnull UUID playerUuid, @Nonnull Ref<EntityStore> currentRef,
                                 @Nonnull CommandBuffer<EntityStore> buffer) {
        PendingTeleport pending = pendingTeleports.remove(playerUuid);
        if (pending == null) {
            return;
        }

        // Use buffer.run() to execute after the tick system finishes processing
        buffer.run(store -> {
            try {
                if (!currentRef.isValid()) {
                    Msg.send(pending.getPlayerRef(), messages.get("teleport.failed"));
                    return;
                }

                if (pending.isPlayerTeleport()) {
                    // Teleport to another player (async, handles cross-world)
                    TeleportUtil.teleportToPlayerByUuid(
                        store, 
                        currentRef, 
                        pending.getTargetPlayerUuid(),
                        () -> {
                            // Success callback
                            SoundUtil.playSound(pending.getPlayerRef(), TELEPORT_SOUND);
                            if (pending.getSuccessMessage() != null) {
                                Msg.send(pending.getPlayerRef(), pending.getSuccessMessage());
                            }
                            if (pending.getOnSuccess() != null) {
                                pending.getOnSuccess().run();
                            }
                        },
                        error -> {
                            // Error callback
                            Msg.send(pending.getPlayerRef(), error);
                        }
                    );
                } else {
                    // Teleport to coordinates
                    TeleportDestination dest = pending.getDestination();
                    String error = TeleportUtil.teleportSafe(store, currentRef, dest.worldName,
                            dest.x, dest.y, dest.z, dest.yaw, dest.pitch);

                    if (error != null) {
                        Msg.send(pending.getPlayerRef(), error);
                    } else {
                        SoundUtil.playSound(pending.getPlayerRef(), TELEPORT_SOUND);
                        if (pending.getSuccessMessage() != null) {
                            Msg.send(pending.getPlayerRef(), pending.getSuccessMessage());
                        }
                        if (pending.getOnSuccess() != null) {
                            pending.getOnSuccess().run();
                        }
                    }
                }
            } catch (Exception e) {
                Log.error("Failed to execute teleport for " + playerUuid + ": " + e.getMessage());
                Msg.send(pending.getPlayerRef(), messages.get("teleport.failed"));
            }
        });
    }

    /**
     * Cancels a pending teleport for a player.
     */
    public void cancelTeleport(@Nonnull UUID playerUuid, @Nullable String reason) {
        PendingTeleport pending = pendingTeleports.remove(playerUuid);
        if (pending != null && reason != null) {
            Msg.send(pending.getPlayerRef(), reason);
        }
    }

    /**
     * Checks if a player has a pending teleport.
     */
    public boolean hasPendingTeleport(@Nonnull UUID playerUuid) {
        return pendingTeleports.containsKey(playerUuid);
    }

    /**
     * Cleans up pending teleport for a player when they disconnect.
     */
    public void onPlayerQuit(@Nonnull UUID playerUuid) {
        pendingTeleports.remove(playerUuid);
    }

    /**
     * Shuts down the manager.
     */
    public void shutdown() {
        pendingTeleports.clear();
    }

    /**
     * Stores teleport destination data.
     */
    private static class TeleportDestination {
        final String worldName;
        final double x, y, z;
        final float yaw, pitch;

        TeleportDestination(String worldName, double x, double y, double z, float yaw, float pitch) {
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    /**
     * Represents a pending teleport.
     */
    private static class PendingTeleport {
        private final PlayerRef playerRef;
        private final Vector3d startPosition;
        private final TeleportDestination destination; // For coordinate teleports
        private final UUID targetPlayerUuid;           // For player teleports
        private final String targetPlayerName;         // For player teleports
        private final String successMessage;
        private final Runnable onSuccess;
        private final float delaySeconds;
        private float elapsedTime;

        // Constructor for coordinate teleport
        PendingTeleport(@Nonnull PlayerRef playerRef, @Nonnull Vector3d startPosition,
                        @Nonnull TeleportDestination destination, @Nullable String successMessage, 
                        int delaySeconds, @Nullable Runnable onSuccess) {
            this.playerRef = playerRef;
            this.startPosition = startPosition.clone(); // Clone to prevent mutation
            this.destination = destination;
            this.targetPlayerUuid = null;
            this.targetPlayerName = null;
            this.successMessage = successMessage;
            this.onSuccess = onSuccess;
            this.delaySeconds = delaySeconds;
            this.elapsedTime = 0f;
        }

        // Constructor for player teleport
        PendingTeleport(@Nonnull PlayerRef playerRef, @Nonnull Vector3d startPosition,
                        @Nonnull UUID targetPlayerUuid, @Nonnull String targetPlayerName,
                        @Nullable String successMessage, int delaySeconds) {
            this.playerRef = playerRef;
            this.startPosition = startPosition.clone(); // Clone to prevent mutation
            this.destination = null;
            this.targetPlayerUuid = targetPlayerUuid;
            this.targetPlayerName = targetPlayerName;
            this.successMessage = successMessage;
            this.onSuccess = null;
            this.delaySeconds = delaySeconds;
            this.elapsedTime = 0f;
        }

        PlayerRef getPlayerRef() {
            return playerRef;
        }

        Vector3d getStartPosition() {
            return startPosition;
        }

        TeleportDestination getDestination() {
            return destination;
        }

        UUID getTargetPlayerUuid() {
            return targetPlayerUuid;
        }

        boolean isPlayerTeleport() {
            return targetPlayerUuid != null;
        }

        String getSuccessMessage() {
            return successMessage;
        }

        Runnable getOnSuccess() {
            return onSuccess;
        }

        void addElapsedTime(float deltaTime) {
            this.elapsedTime += deltaTime;
        }

        boolean isReady() {
            return elapsedTime >= delaySeconds;
        }

    }
}
