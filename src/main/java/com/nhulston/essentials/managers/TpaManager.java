package com.nhulston.essentials.managers;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.nhulston.essentials.util.Log;
import com.nhulston.essentials.util.Msg;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Manages teleport requests between players.
 * A target player can have multiple pending requests from different players.
 */
public class TpaManager {
    // Map of target player UUID -> Map of requester UUID -> request
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, TpaRequest>> pendingRequests = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    private static final long EXPIRATION_SECONDS = 20;

    /**
     * Creates a teleport request from one player to another.
     * @param requester The player requesting to teleport
     * @param target The player being requested to accept
     * @return true if request was created, false if there's already a pending request from this requester
     */
    public boolean createRequest(@Nonnull PlayerRef requester, @Nonnull PlayerRef target) {
        UUID targetUuid = target.getUuid();
        UUID requesterUuid = requester.getUuid();
        
        // Get or create the map of requests for this target
        ConcurrentHashMap<UUID, TpaRequest> targetRequests = pendingRequests.computeIfAbsent(
            targetUuid, _ -> new ConcurrentHashMap<>()
        );
        
        // Check if there's already a pending request from this requester
        if (targetRequests.containsKey(requesterUuid)) {
            return false;
        }
        
        // Create new request
        TpaRequest request = new TpaRequest(requesterUuid, requester.getUsername(), target.getUsername());
        targetRequests.put(requesterUuid, request);
        
        // Schedule expiration
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            expireRequest(targetUuid, requesterUuid);
        }, EXPIRATION_SECONDS, TimeUnit.SECONDS);
        request.setExpirationFuture(future);
        
        Log.info("TPA request created: " + requester.getUsername() + " -> " + target.getUsername());
        return true;
    }

    /**
     * Accepts a teleport request from a specific player.
     * @param target The player accepting the request
     * @param requesterName The name of the requester
     * @return The TpaRequest if found and valid, null otherwise
     */
    @Nullable
    public TpaRequest acceptRequest(@Nonnull PlayerRef target, @Nonnull String requesterName) {
        UUID targetUuid = target.getUuid();
        ConcurrentHashMap<UUID, TpaRequest> targetRequests = pendingRequests.get(targetUuid);
        
        if (targetRequests == null || targetRequests.isEmpty()) {
            return null;
        }
        
        // Find the request by requester name (case-insensitive)
        TpaRequest foundRequest = null;
        UUID foundRequesterUuid = null;
        
        for (Map.Entry<UUID, TpaRequest> entry : targetRequests.entrySet()) {
            if (entry.getValue().getRequesterName().equalsIgnoreCase(requesterName)) {
                foundRequest = entry.getValue();
                foundRequesterUuid = entry.getKey();
                break;
            }
        }
        
        if (foundRequest == null) {
            return null;
        }
        
        // Remove the request and cancel its expiration
        targetRequests.remove(foundRequesterUuid);
        foundRequest.cancel();
        
        // Clean up empty maps
        if (targetRequests.isEmpty()) {
            pendingRequests.remove(targetUuid);
        }
        
        Log.info("TPA request accepted: " + foundRequest.getRequesterName() + " -> " + target.getUsername());
        return foundRequest;
    }

    /**
     * Expires a request and notifies the requester.
     */
    private void expireRequest(UUID targetUuid, UUID requesterUuid) {
        ConcurrentHashMap<UUID, TpaRequest> targetRequests = pendingRequests.get(targetUuid);
        if (targetRequests == null) {
            return;
        }
        
        TpaRequest request = targetRequests.remove(requesterUuid);
        if (request != null) {
            // Clean up empty maps
            if (targetRequests.isEmpty()) {
                pendingRequests.remove(targetUuid);
            }
            
            // Notify the requester that their request expired
            PlayerRef requester = Universe.get().getPlayer(requesterUuid);
            if (requester != null) {
                Msg.fail(requester, "Your teleport request to " + request.getTargetName() + " has expired.");
            }
        }
    }

    /**
     * Shuts down the manager and cancels all pending requests.
     */
    public void shutdown() {
        scheduler.shutdownNow();
        pendingRequests.clear();
    }

    /**
     * Represents a teleport request.
     */
    public static class TpaRequest {
        private final UUID requesterUuid;
        private final String requesterName;
        private final String targetName;
        private ScheduledFuture<?> expirationFuture;

        public TpaRequest(UUID requesterUuid, String requesterName, String targetName) {
            this.requesterUuid = requesterUuid;
            this.requesterName = requesterName;
            this.targetName = targetName;
        }

        public UUID getRequesterUuid() {
            return requesterUuid;
        }

        public String getRequesterName() {
            return requesterName;
        }

        public String getTargetName() {
            return targetName;
        }

        void setExpirationFuture(ScheduledFuture<?> future) {
            this.expirationFuture = future;
        }

        void cancel() {
            if (expirationFuture != null) {
                expirationFuture.cancel(false);
            }
        }
    }
}
