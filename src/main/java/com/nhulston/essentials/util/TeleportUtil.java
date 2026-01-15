package com.nhulston.essentials.util;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import com.nhulston.essentials.models.Spawn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

    private TeleportUtil() {}

    /**
     * Rounds the yaw to the nearest cardinal direction.
     * Workaround for Hytale bug where teleporting while looking down causes player model issues.
     * 
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
     * Creates a cardinal-safe rotation vector (pitch set to 0, yaw rounded to cardinal).
     * Use this to avoid the Hytale bug where looking down causes player model issues.
     *
     * @param currentRotation The player's current rotation
     * @return A new rotation with pitch=0 and yaw rounded to cardinal direction
     */
    private static Vector3f cardinalRotation(@Nonnull Vector3f currentRotation) {
        float cardinalYaw = roundToCardinalYaw(currentRotation.y);
        return new Vector3f(0, cardinalYaw, 0);
    }

    /**
     * Teleports an entity to the specified location.
     * Does NOT adjust position for safety - use teleportSafe() for player-set destinations.
     * @return null if successful, error message if failed
     */
    @Nullable
    public static String teleport(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                                  @Nonnull String worldName, double x, double y, double z,
                                  float yaw, float pitch) {
        World targetWorld = Universe.get().getWorld(worldName);
        if (targetWorld == null) {
            return "World '" + worldName + "' is not loaded.";
        }

        Vector3d position = new Vector3d(x, y, z);
        // Round yaw to cardinal direction and zero pitch to avoid Hytale model bug
        Vector3f rotation = new Vector3f(0, roundToCardinalYaw(yaw), 0);

        Teleport teleport = new Teleport(targetWorld, position, rotation);
        store.putComponent(ref, Teleport.getComponentType(), teleport);

        return null;
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
            return "World '" + worldName + "' is not loaded.";
        }

        // Find safe Y position
        double safeY = findSafeY(targetWorld, x, y, z);
        
        Vector3d position = new Vector3d(x, safeY, z);
        // Round yaw to cardinal direction and zero pitch to avoid Hytale model bug
        Vector3f rotation = new Vector3f(0, roundToCardinalYaw(yaw), 0);

        Teleport teleport = new Teleport(targetWorld, position, rotation);
        store.putComponent(ref, Teleport.getComponentType(), teleport);

        return null;
    }

    /**
     * Teleports one player to another player's location.
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
        
        // Get target's position
        TransformComponent targetTransform = targetStore.getComponent(targetRef, TransformComponent.getComponentType());
        if (targetTransform == null) {
            return;
        }
        
        Vector3d targetPos = targetTransform.getPosition();
        
        // Get target's world
        EntityStore targetEntityStore = targetStore.getExternalData();
        World targetWorld = targetEntityStore.getWorld();
        
        // Get target's rotation and round to cardinal direction
        Vector3f rotation = cardinalRotation(targetTransform.getRotation());

        // Teleport the player
        Teleport teleport = new Teleport(targetWorld, targetPos, rotation);
        playerStore.putComponent(playerRef, Teleport.getComponentType(), teleport);
    }

    /**
     * Teleports a player to spawn, finding a safe Y position if needed.
     *
     * @param player The player to teleport
     * @param spawn  The spawn location
     */
    public static void teleportToSpawn(@Nonnull PlayerRef player, @Nonnull Spawn spawn) {
        Ref<EntityStore> playerRef = player.getReference();
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }

        World targetWorld = Universe.get().getWorld(spawn.getWorld());
        if (targetWorld == null) {
            return;
        }

        // Find safe Y position
        double safeY = findSafeY(targetWorld, spawn.getX(), spawn.getY(), spawn.getZ());

        Store<EntityStore> store = playerRef.getStore();
        Vector3d position = new Vector3d(spawn.getX(), safeY, spawn.getZ());
        // Round yaw to cardinal direction and zero pitch to avoid Hytale model bug
        Vector3f rotation = new Vector3f(0, roundToCardinalYaw(spawn.getYaw()), 0);

        Teleport teleport = new Teleport(targetWorld, position, rotation);
        store.putComponent(playerRef, Teleport.getComponentType(), teleport);
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

        // Use spawn coordinates directly - no safe Y check as it can trigger chunk loading
        // during store processing which causes IllegalStateException
        Vector3d position = new Vector3d(spawn.getX(), spawn.getY(), spawn.getZ());
        // Round yaw to cardinal direction and zero pitch to avoid Hytale model bug
        Vector3f rotation = new Vector3f(0, roundToCardinalYaw(spawn.getYaw()), 0);

        Teleport teleport = new Teleport(targetWorld, position, rotation);
        buffer.putComponent(ref, Teleport.getComponentType(), teleport);
    }

    /**
     * Teleports a player to another player by UUID (for delayed teleports).
     *
     * @param store The entity store
     * @param playerRef The player to teleport
     * @param targetUuid The UUID of the target player
     * @return null if successful, error message if failed
     */
    @Nullable
    public static String teleportToPlayerByUuid(@Nonnull Store<EntityStore> store,
                                                @Nonnull Ref<EntityStore> playerRef,
                                                @Nonnull java.util.UUID targetUuid) {
        PlayerRef target = Universe.get().getPlayer(targetUuid);
        if (target == null) {
            return "Target player is no longer online.";
        }

        Ref<EntityStore> targetRef = target.getReference();
        if (targetRef == null || !targetRef.isValid()) {
            return "Target player is not available.";
        }

        Store<EntityStore> targetStore = targetRef.getStore();

        // Get target's position
        TransformComponent targetTransform = targetStore.getComponent(targetRef, TransformComponent.getComponentType());
        if (targetTransform == null) {
            return "Could not get target position.";
        }

        Vector3d targetPos = targetTransform.getPosition();

        // Get target's world
        EntityStore targetEntityStore = targetStore.getExternalData();
        World targetWorld = targetEntityStore.getWorld();

        // Get target's rotation and round to cardinal direction
        Vector3f rotation = cardinalRotation(targetTransform.getRotation());

        // Teleport the player
        Teleport teleport = new Teleport(targetWorld, targetPos, rotation);
        store.putComponent(playerRef, Teleport.getComponentType(), teleport);

        return null;
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
}
