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
    private static final int MAX_SAFE_SEARCH = 32;

    /** Player height in blocks (need 2 air blocks for player to fit) */
    private static final int PLAYER_HEIGHT = 2;

    private TeleportUtil() {}

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
        Vector3f rotation = new Vector3f(pitch, yaw, 0.0F);

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
        Vector3f rotation = new Vector3f(pitch, yaw, 0.0F);

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
        
        // Teleport the player
        Teleport teleport = new Teleport(targetWorld, targetPos, new Vector3f(0, 0, 0));
        playerStore.putComponent(playerRef, Teleport.getComponentType(), teleport);
    }

    /**
     * Teleports a player to spawn, finding a safe Y position if needed.
     *
     * @param player The player to teleport
     * @param spawn The spawn location
     * @return null if successful, error message if failed
     */
    @Nullable
    public static String teleportToSpawn(@Nonnull PlayerRef player, @Nonnull Spawn spawn) {
        Ref<EntityStore> playerRef = player.getReference();
        if (playerRef == null || !playerRef.isValid()) {
            return "Player reference is invalid.";
        }

        World targetWorld = Universe.get().getWorld(spawn.getWorld());
        if (targetWorld == null) {
            return "World '" + spawn.getWorld() + "' is not loaded.";
        }

        // Find safe Y position
        double safeY = findSafeY(targetWorld, spawn.getX(), spawn.getY(), spawn.getZ());

        Store<EntityStore> store = playerRef.getStore();
        Vector3d position = new Vector3d(spawn.getX(), safeY, spawn.getZ());
        Vector3f rotation = new Vector3f(spawn.getPitch(), spawn.getYaw(), 0.0F);

        Teleport teleport = new Teleport(targetWorld, position, rotation);
        store.putComponent(playerRef, Teleport.getComponentType(), teleport);
        return null;
    }

    /**
     * Teleports a player to spawn using a CommandBuffer (for use within systems).
     * Finds a safe Y position if needed.
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

        // Find safe Y position
        double safeY = findSafeY(targetWorld, spawn.getX(), spawn.getY(), spawn.getZ());

        Vector3d position = new Vector3d(spawn.getX(), safeY, spawn.getZ());
        Vector3f rotation = new Vector3f(spawn.getPitch(), spawn.getYaw(), 0.0F);

        Teleport teleport = new Teleport(targetWorld, position, rotation);
        buffer.putComponent(ref, Teleport.getComponentType(), teleport);
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
}
