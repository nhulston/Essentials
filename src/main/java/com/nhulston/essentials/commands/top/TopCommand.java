package com.nhulston.essentials.commands.top;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.Essentials;
import com.nhulston.essentials.util.MessageManager;
import com.nhulston.essentials.util.Msg;
import com.nhulston.essentials.util.TeleportUtil;

import javax.annotation.Nonnull;

/**
 * Command to teleport to the highest block at current position.
 * Usage: /top
 */
public class TopCommand extends AbstractPlayerCommand {
    private static final int MAX_HEIGHT = 256;
    private final MessageManager messages;

    public TopCommand() {
        super("top", "Teleport to the highest block");
        this.messages = Essentials.getInstance().getMessageManager();
        requirePermission("essentials.top");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        // Get player's current position
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            Msg.send(context, messages.get("commands.top.position-error"));
            return;
        }

        Vector3d pos = transform.getPosition();
        int blockX = (int) Math.floor(pos.x);
        int blockZ = (int) Math.floor(pos.z);

        // Get chunk at player's position
        long chunkIndex = ChunkUtil.indexChunkFromBlock(blockX, blockZ);
        WorldChunk chunk = world.getChunk(chunkIndex);
        if (chunk == null) {
            Msg.send(context, messages.get("commands.top.chunk-not-loaded"));
            return;
        }

        // Find highest solid block from top down
        Integer topY = findHighestSolidBlock(chunk, blockX, blockZ);
        if (topY == null) {
            Msg.send(context, messages.get("commands.top.no-ground"));
            return;
        }

        // Teleport to one block above the highest solid block, centered on the block
        double targetY = topY + 1;
        double centerX = Math.floor(pos.x) + 0.5;
        double centerZ = Math.floor(pos.z) + 0.5;
        Vector3d targetPos = new Vector3d(centerX, targetY, centerZ);
        
        // Round yaw to cardinal direction and zero pitch to avoid Hytale model bug
        float yaw = transform.getRotation().y;
        float cardinalYaw = TeleportUtil.roundToCardinalYaw(yaw);
        Vector3f rotation = new Vector3f(0, cardinalYaw, 0);

        Teleport teleport = new Teleport(world, targetPos, rotation);
        store.putComponent(ref, Teleport.getComponentType(), teleport);

        Msg.send(context, messages.get("commands.top.teleported"));
    }

    /**
     * Finds the highest solid block at the given X/Z position.
     * @return Y coordinate of highest solid block, or null if none found
     */
    private Integer findHighestSolidBlock(WorldChunk chunk, int x, int z) {
        for (int y = MAX_HEIGHT; y >= 0; y--) {
            BlockType blockType = chunk.getBlockType(x, y, z);
            if (blockType != null && blockType.getMaterial() == BlockMaterial.Solid) {
                return y;
            }
        }
        return null;
    }
}
