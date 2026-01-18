package com.nhulston.essentials.afk;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.Essentials;
import com.nhulston.essentials.util.ConfigManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class AfkSystem extends DelayedEntitySystem<EntityStore> {

    private static final String BYPASS_PERMISSION = "essentials.afk.bypass";
    private static final double EPSILON = 0.0000001;

    private final ConfigManager configManager;

    public AfkSystem(final @Nonnull ConfigManager configManager) {
        super(1.0f);

        this.configManager = configManager;
    }

    @Override
    public void tick(float dt, int index, @NotNull ArchetypeChunk<EntityStore> archetypeChunk, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null) return;

        Player player = archetypeChunk.getComponent(index, Player.getComponentType());
        if (player == null) return;

        AfkComponent afk = archetypeChunk.getComponent(index, Essentials.getInstance().getAfkComponentType());
        if (afk == null) return;

        TransformComponent transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
        if (transform == null) return;

        if (player.hasPermission(BYPASS_PERMISSION)) return;

        if (afk.getLastLocation().distanceSquaredTo(transform.getPosition()) > EPSILON) {
            afk.setLastLocation(transform.getPosition().clone());
            afk.setSecondsSinceLastMoved(0);
            return;
        }

        afk.setSecondsSinceLastMoved(afk.getSecondsSinceLastMoved() + 1);

        if (afk.getSecondsSinceLastMoved() < configManager.getAfkKickTime()) return;

        commandBuffer.run(_ -> playerRef.getPacketHandler().disconnect(configManager.getAfkKickMessage()));
    }

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return Query.and(TransformComponent.getComponentType(),
                Player.getComponentType(),
                Essentials.getInstance().getAfkComponentType());
    }
}
