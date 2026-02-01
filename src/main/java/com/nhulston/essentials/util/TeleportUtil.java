package com.nhulston.essentials.util;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.entity.teleport.PendingTeleport;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import com.nhulston.essentials.Essentials;
import com.nhulston.essentials.managers.BackManager;
import com.nhulston.essentials.models.Spawn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class TeleportUtil {

    /** Maximum blocks to search upward for a safe position */
    private static final int MAX_SAFE_SEARCH = 128;

    /** Player height in blocks (need 2 air blocks for player to fit) */
    private static final int PLAYER_HEIGHT = 2;

    // Cardinal direction yaw values (in radians)
    private static final float YAW_NORTH = 0f;
    private static final float YAW_EAST = (float) Math.toRadians(-90);   // -π/2
    private static final float YAW_SOUTH = (float) Math.PI;              // π
    private static final float YAW_WEST = (float) Math.toRadians(90);   // π/2

    /**
     * Gets player's start position for teleport countdown.
     * Uses EntityStore to avoid stale cache after world changes.
     */
    @Nullable
    public static Vector3d getStartPosition(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        return transform != null ? transform.getPosition() : null;
    }

    /**
     * Helper to perform cross-world teleports with proper thread safety.
     * Handles same-world and cross-world cases automatically.
     * 
     * @param playerStore The player's store
     * @param playerRef The player's entity reference
     * @param playerWorld The player's current world
     * @param targetStore The target entity's store
     * @param targetRef The target entity's reference
     * @param targetWorld The target entity's world
     * @param onSuccess Callback on success (optional)
     * @param onError Callback on error (optional)
     */
    private static void executeCrossWorldTeleportToEntity(
            @Nonnull Store<EntityStore> playerStore,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull World playerWorld,
            @Nonnull Store<EntityStore> targetStore,
            @Nonnull Ref<EntityStore> targetRef,
            @Nonnull World targetWorld,
            @Nullable Runnable onSuccess,
            @Nullable java.util.function.Consumer<String> onError) {
        
        if (targetWorld == playerWorld) {
            // Same world - safe to access directly
            TransformComponent targetTransform = targetStore.getComponent(targetRef, TransformComponent.getComponentType());
            if (targetTransform == null) {
                if (onError != null) {
                    onError.accept("Could not get target position.");
                }
                return;
            }
            
            Vector3d targetPos = targetTransform.getPosition();
            Vector3f rotation = new Vector3f(0, roundToCardinalYaw(targetTransform.getRotation().y), 0);
            
            Teleport teleport = new Teleport(targetWorld, targetPos, rotation);
            playerStore.putComponent(playerRef, Teleport.getComponentType(), teleport);
            
            if (onSuccess != null) {
                onSuccess.run();
            }
        } else {
            // Cross-world teleport - read target position on target world's thread
            targetWorld.execute(() -> {
                if (!targetRef.isValid()) {
                    if (onError != null) {
                        onError.accept("Target is not available.");
                    }
                    return;
                }
                
                TransformComponent targetTransform = targetStore.getComponent(targetRef, TransformComponent.getComponentType());
                if (targetTransform == null) {
                    if (onError != null) {
                        onError.accept("Could not get target position.");
                    }
                    return;
                }
                
                // Capture position data (clone to prevent mutation)
                Vector3d targetPos = targetTransform.getPosition().clone();
                Vector3f rotation = new Vector3f(0, roundToCardinalYaw(targetTransform.getRotation().y), 0);
                
                // Execute teleport on player's world thread
                playerWorld.execute(() -> {
                    if (!playerRef.isValid()) {
                        if (onError != null) {
                            onError.accept("Player is not available.");
                        }
                        return;
                    }
                    
                    Teleport teleport = new Teleport(targetWorld, targetPos, rotation);
                    playerStore.putComponent(playerRef, Teleport.getComponentType(), teleport);
                    
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                });
            });
        }
    }

    /**
     * Executes a teleport action on a player's world thread with full validation.
     * Handles player lookup, world thread bridging, and error messaging automatically.
     * If validation fails, sends an error message to the command context and returns without executing.
     * 
     * @param targetPlayer The player to teleport (can be null)
     * @param context Command context for sending error messages
     * @param action The action to execute on the player's world thread
     */
    public static void executeOnPlayerWorld(@Nullable PlayerRef targetPlayer,
                                            @Nonnull CommandContext context,
                                            @Nonnull Runnable action) {
        if (targetPlayer == null) {
            Msg.send(context, Essentials.getInstance().getMessageManager().get("commands.spawn.player-not-found"));
            return;
        }

        Ref<EntityStore> ref = targetPlayer.getReference();
        if (ref == null || !ref.isValid()) {
            Msg.send(context, Essentials.getInstance().getMessageManager().get("commands.spawn.player-not-found"));
            return;
        }

        Store<EntityStore> store = ref.getStore();
        EntityStore entityStore = store.getExternalData();
        World targetWorld = entityStore.getWorld();

        // Execute on target player's world thread
        targetWorld.execute(action);
    }

    /**
     * Rounds the yaw to the nearest cardinal direction.
     * Workaround for Hytale bug where teleporting while looking down causes player model issues.
     * Hytale uses radians for rotation. Cardinal directions in radians:
     * - North: 0
     * - East: -π/2 (-1.5708)
     * - South: π (3.1416)
     * - West: π/2 (1.5708)
     *
     * @param yawRadians The current yaw value in radians
     * @return The yaw rounded to the nearest cardinal direction (in radians)
     */
    public static float roundToCardinalYaw(float yawRadians) {
        // Convert from radians to degrees for easier comparison
        float yawDegrees = (float) Math.toDegrees(yawRadians);
        
        // Normalize yaw to -180 to 180 range
        yawDegrees = yawDegrees % 360;
        if (yawDegrees > 180) yawDegrees -= 360;
        if (yawDegrees < -180) yawDegrees += 360;

        // Find nearest cardinal direction, return in radians
        // North: 0°, East: -90°, South: 180°, West: 90°
        if (yawDegrees >= -45 && yawDegrees < 45) {
            return YAW_NORTH; // 0
        } else if (yawDegrees >= 45 && yawDegrees < 135) {
            return YAW_WEST; // π/2
        } else if (yawDegrees >= 135 || yawDegrees < -135) {
            return YAW_SOUTH; // π
        } else {
            return YAW_EAST; // -π/2
        }
    }



    /**
     * Teleports an entity to the specified location, finding a safe Y position if needed.
     * Use this for player-set destinations (homes) where the terrain may have changed.
     * @return null if successful, error message if failed
     */
    @Nullable
    public static String teleportSafe(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                                      @Nonnull String worldName, double x, double y, double z,
                                      float yaw, float pitch) {
        World targetWorld = Universe.get().getWorld(worldName);
        if (targetWorld == null) {
            MessageManager messages = Essentials.getInstance().getMessageManager();
            return messages.get("teleport.world-not-loaded", Map.of("world", worldName));
        }
        double safeY = findSafeY(targetWorld, x, y, z);

        Vector3d position = new Vector3d(x, safeY, z);
        Vector3f rotation = new Vector3f(0, roundToCardinalYaw(yaw), 0);

        // CRITICAL FIX: Remove PendingTeleport component to reset teleportId counter
        // After death/respawn, the PendingTeleport component persists with stale teleportId state
        // This causes "Incorrect teleportId" kicks when the client's expected ID doesn't match
        PendingTeleport existingPending = store.getComponent(ref, PendingTeleport.getComponentType());
        if (existingPending != null) {
            store.removeComponent(ref, PendingTeleport.getComponentType());
        }

        Teleport teleport = new Teleport(targetWorld, position, rotation);
        store.putComponent(ref, Teleport.getComponentType(), teleport);
        return null;
    }

    /**
     * Teleports one player to another player's location.
     * THREAD-SAFE: Handles cross-world teleports correctly.
     *
     * @param player The player to teleport
     * @param target The player to teleport to
     */
    public static void teleportToPlayer(@Nonnull PlayerRef player, @Nonnull PlayerRef target) {
        Ref<EntityStore> playerRef = player.getReference();
        Ref<EntityStore> targetRef = target.getReference();
        
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }
        
        if (targetRef == null || !targetRef.isValid()) {
            return;
        }
        
        Store<EntityStore> playerStore = playerRef.getStore();
        Store<EntityStore> targetStore = targetRef.getStore();
        
        // Get target's world
        EntityStore targetEntityStore = targetStore.getExternalData();
        World targetWorld = targetEntityStore.getWorld();
        
        // Get player's world
        EntityStore playerEntityStore = playerStore.getExternalData();
        World playerWorld = playerEntityStore.getWorld();
        
        executeCrossWorldTeleportToEntity(playerStore, playerRef, playerWorld, 
                                          targetStore, targetRef, targetWorld, 
                                          null, null);
    }

    /**
     * Teleports a player to spawn using a CommandBuffer (for use within systems).
     * Does NOT perform safe Y calculation as that can trigger chunk loading during store processing.
     * Spawn should be set to a safe location by admins.
     *
     * @param ref The entity reference
     * @param buffer The command buffer to queue the teleport
     * @param spawn The spawn location
     */
    public static void teleportToSpawnBuffered(@Nonnull Ref<EntityStore> ref,
                                               @Nonnull CommandBuffer<EntityStore> buffer,
                                               @Nonnull Spawn spawn) {
        World targetWorld = Universe.get().getWorld(spawn.getWorld());
        if (targetWorld == null) {
            return;
        }

        // Use spawn coordinates directly - no safe Y check
        Vector3d position = new Vector3d(spawn.getX(), spawn.getY(), spawn.getZ());
        Vector3f rotation = new Vector3f(0, roundToCardinalYaw(spawn.getYaw()), 0);

        Teleport teleport = new Teleport(targetWorld, position, rotation);
        buffer.putComponent(ref, Teleport.getComponentType(), teleport);
    }

    /**
     * Teleports a player to another player by UUID (for delayed teleports).
     * THREAD-SAFE: Handles cross-world teleports correctly using callbacks.
     *
     * @param store The entity store (player's current world)
     * @param playerRef The player to teleport
     * @param targetUuid The UUID of the target player
     * @param onSuccess Callback invoked after successful teleport
     * @param onError Callback invoked with error message if failed
     */
    public static void teleportToPlayerByUuid(@Nonnull Store<EntityStore> store,
                                              @Nonnull Ref<EntityStore> playerRef,
                                              @Nonnull java.util.UUID targetUuid,
                                              @Nullable Runnable onSuccess,
                                              @Nullable java.util.function.Consumer<String> onError) {
        PlayerRef target = Universe.get().getPlayer(targetUuid);
        if (target == null) {
            if (onError != null) {
                onError.accept("Target player is no longer online.");
            }
            return;
        }

        Ref<EntityStore> targetRef = target.getReference();
        if (targetRef == null || !targetRef.isValid()) {
            if (onError != null) {
                onError.accept("Target player is not available.");
            }
            return;
        }

        Store<EntityStore> targetStore = targetRef.getStore();

        // Get target's world
        EntityStore targetEntityStore = targetStore.getExternalData();
        World targetWorld = targetEntityStore.getWorld();

        // Get player's world
        EntityStore playerEntityStore = store.getExternalData();
        World playerWorld = playerEntityStore.getWorld();

        executeCrossWorldTeleportToEntity(store, playerRef, playerWorld, 
                                          targetStore, targetRef, targetWorld, 
                                          onSuccess, onError);
    }

    /**
     * Finds a safe Y position for teleportation by searching upward for air blocks.
     * A position is safe when there are at least 2 non-solid blocks (for player height).
     *
     * @param world The world to check blocks in
     * @param x X coordinate
     * @param y Starting Y coordinate  
     * @param z Z coordinate
     * @return Safe Y coordinate, or original Y if no safe position found
     */
    private static double findSafeY(@Nonnull World world, double x, double y, double z) {
        int blockX = (int) Math.floor(x);
        int blockY = (int) Math.floor(y);
        int blockZ = (int) Math.floor(z);

        // Get the chunk at this position
        long chunkIndex = ChunkUtil.indexChunkFromBlock(blockX, blockZ);
        WorldChunk chunk = world.getChunk(chunkIndex);
        if (chunk == null) {
            // Chunk not loaded, return original position
            return y;
        }

        // Search upward for a safe position (2 air blocks for player to fit)
        for (int offsetY = 0; offsetY < MAX_SAFE_SEARCH; offsetY++) {
            int checkY = blockY + offsetY;
            
            if (hasSpaceForPlayer(chunk, blockX, checkY, blockZ)) {
                // Found safe position
                return checkY;
            }
        }

        // No safe position found, return original
        return y;
    }

    /**
     * Checks if there's enough space for a player (2 blocks tall) at the given position.
     */
    private static boolean hasSpaceForPlayer(@Nonnull WorldChunk chunk, int x, int y, int z) {
        for (int i = 0; i < PLAYER_HEIGHT; i++) {
            if (isSolidBlock(chunk, x, y + i, z)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if a block at the given position is solid.
     */
    private static boolean isSolidBlock(@Nonnull WorldChunk chunk, int x, int y, int z) {
        BlockType blockType = chunk.getBlockType(x, y, z);
        if (blockType == null) {
            return false; // No block = air = not solid
        }
        BlockMaterial material = blockType.getMaterial();
        return material == BlockMaterial.Solid;
    }

    /**
     * Checks if a position contains fluid (water or lava).
     * Fluids are stored separately from blocks in Hytale.
     * TODO: Update when Hytale provides non-deprecated fluid API.
     */
    @SuppressWarnings("removal")
    private static boolean hasFluid(@Nonnull WorldChunk chunk, int x, int y, int z) {
        return chunk.getFluidId(x, y, z) > 0;
    }

    /**
     * Finds a safe Y position for RTP by searching from top down.
     * Finds the highest solid block, then checks if player can stand there safely.
     * Returns null if location has fluid (water/lava).
     *
     * @param world The world to check
     * @param x X coordinate
     * @param z Z coordinate
     * @return Safe Y coordinate (one above ground), or null if unsafe (fluid/no ground)
     */
    @Nullable
    public static Double findSafeRtpY(@Nonnull World world, double x, double z) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);

        long chunkIndex = ChunkUtil.indexChunkFromBlock(blockX, blockZ);
        WorldChunk chunk = world.getChunk(chunkIndex);
        if (chunk == null) {
            return null; // Chunk not loaded
        }

        return findSafeRtpYFromChunk(chunk, blockX, blockZ);
    }

    /**
     * Asynchronously finds a safe Y position for RTP by searching from top down.
     * Uses getChunkAsync to safely access chunks from any thread.
     *
     * @param world The world to check
     * @param x X coordinate
     * @param z Z coordinate
     * @return CompletableFuture with safe Y coordinate (one above ground), or null if unsafe
     */
    @Nonnull
    public static CompletableFuture<Double> findSafeRtpYAsync(@Nonnull World world, double x, double z) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);

        long chunkIndex = ChunkUtil.indexChunkFromBlock(blockX, blockZ);
        
        return world.getChunkAsync(chunkIndex).thenApply(chunk -> {
            if (chunk == null) {
                return null; // Chunk not loaded
            }
            return findSafeRtpYFromChunk(chunk, blockX, blockZ);
        });
    }

    /**
     * Finds a safe Y position using an already-loaded chunk.
     * Internal helper for both sync and async methods.
     */
    @Nullable
    private static Double findSafeRtpYFromChunk(@Nonnull WorldChunk chunk, int blockX, int blockZ) {
        // Search from top down to find first solid block
        int startY = 200;
        int minY = 0;
        
        for (int checkY = startY; checkY >= minY; checkY--) {
            // Check for fluid at this level - if found, abort this location
            if (hasFluid(chunk, blockX, checkY, blockZ)) {
                return null; // Hit water/lava, this location is no good
            }
            
            // Check if this block is solid (ground)
            if (isSolidBlock(chunk, blockX, checkY, blockZ)) {
                // Found ground! Player spawns at checkY + 1
                int spawnY = checkY + 1;
                
                // Verify there's space for player (2 blocks) and no fluid
                if (hasFluid(chunk, blockX, spawnY, blockZ) || 
                    hasFluid(chunk, blockX, spawnY + 1, blockZ)) {
                    return null; // Fluid above ground
                }
                
                // Make sure head space isn't blocked
                if (isSolidBlock(chunk, blockX, spawnY + 1, blockZ)) {
                    // Only 1 block of space, keep searching down
                    continue;
                }
                
                return (double) spawnY;
            }
        }

        return null; // No solid ground found
    }

    /**
     * Saves a player's current location for /back before teleporting.
     * Used internally by instant teleport methods.
     *
     * @param targetPlayer The player whose location to save
     * @param backManager The back manager to save location
     */
    private static void saveBackLocation(@Nonnull PlayerRef targetPlayer,
                                          @Nonnull com.nhulston.essentials.managers.BackManager backManager) {
        Ref<EntityStore> targetRef = targetPlayer.getReference();
        if (targetRef != null && targetRef.isValid()) {
            Store<EntityStore> targetStore = targetRef.getStore();
            EntityStore targetEntityStore = targetStore.getExternalData();
            World targetWorld = targetEntityStore.getWorld();
            
            backManager.setBackLocation(targetStore, targetRef, targetPlayer, targetWorld);
        }
    }

    /**
     * Saves a player's current location for /back and teleports them to coordinates instantly.
     * Used for admin/console commands that bypass teleport delays.
     *
     * @param targetPlayer The player to teleport
     * @param backManager The back manager to save location
     * @param worldName Target world name
     * @param x Target X coordinate
     * @param y Target Y coordinate
     * @param z Target Z coordinate
     * @param yaw Target yaw rotation
     * @param pitch Target pitch rotation
     * @return Error message if failed, null if successful
     */
    @Nullable
    public static String saveLocationAndTeleport(@Nonnull PlayerRef targetPlayer,
                                                   @Nonnull com.nhulston.essentials.managers.BackManager backManager,
                                                   @Nonnull String worldName,
                                                   double x, double y, double z,
                                                   float yaw, float pitch) {
        saveBackLocation(targetPlayer, backManager);

        Ref<EntityStore> targetRef = targetPlayer.getReference();
        if (targetRef != null && targetRef.isValid()) {
            return teleportSafe(targetRef.getStore(), targetRef, worldName, x, y, z, yaw, pitch);
        }
        
        return "Could not access player data.";
    }

    /**
     * Saves a player's current location for /back and teleports them to spawn instantly.
     * Used for admin/console commands that bypass teleport delays.
     *
     * @param targetPlayer The player to teleport
     * @param backManager The back manager to save location
     * @param spawn The spawn location
     */
    public static void saveLocationAndTeleportToSpawn(@Nonnull PlayerRef targetPlayer,
                                                        @Nonnull BackManager backManager,
                                                        @Nonnull Spawn spawn) {
        saveBackLocation(targetPlayer, backManager);

        Ref<EntityStore> playerRef = targetPlayer.getReference();
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }

        World targetWorld = Universe.get().getWorld(spawn.getWorld());
        if (targetWorld == null) {
            return;
        }

        double safeY = findSafeY(targetWorld, spawn.getX(), spawn.getY(), spawn.getZ());
        Store<EntityStore> store = playerRef.getStore();
        Vector3d position = new Vector3d(spawn.getX(), safeY, spawn.getZ());
        Vector3f rotation = new Vector3f(0, roundToCardinalYaw(spawn.getYaw()), 0);

        Teleport teleport = new Teleport(targetWorld, position, rotation);
        store.putComponent(playerRef, Teleport.getComponentType(), teleport);
    }
}
