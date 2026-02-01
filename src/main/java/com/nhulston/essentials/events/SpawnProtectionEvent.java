package com.nhulston.essentials.events;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.managers.SpawnProtectionManager;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class SpawnProtectionEvent {
    private static final String PROTECTED_MESSAGE = "This area is protected.";
    private static final String PROTECTED_COLOR = "#FF5555";

    private final SpawnProtectionManager spawnProtectionManager;

    public SpawnProtectionEvent(@Nonnull SpawnProtectionManager spawnProtectionManager) {
        this.spawnProtectionManager = spawnProtectionManager;
    }

    private static void sendProtectedMessage(PlayerRef playerRef) {
        if (playerRef != null) {
            playerRef.sendMessage(Message.raw(PROTECTED_MESSAGE).color(PROTECTED_COLOR));
        }
    }

    /**
     * Common block event protection logic.
     * @return true if event should be canceled, false otherwise
     */
    private static boolean shouldCancelBlockEvent(
            @Nonnull SpawnProtectionManager manager,
            @Nonnull Store<EntityStore> store,
            @Nonnull ArchetypeChunk<EntityStore> chunk,
            int index,
            @Nonnull com.hypixel.hytale.math.vector.Vector3i blockPos) {
        
        if (!manager.isEnabled()) {
            return false;
        }

        // Get current world name
        String worldName = store.getExternalData().getWorld().getName();

        if (!manager.isInProtectedArea(worldName, blockPos)) {
            return false;
        }

        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef != null && manager.canBypass(playerRef.getUuid())) {
            return false;
        }

        sendProtectedMessage(playerRef);
        return true;
    }

    public void register(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        // Register block break protection
        registry.registerSystem(new BreakBlockProtectionSystem(spawnProtectionManager));

        // Register block place protection
        registry.registerSystem(new PlaceBlockProtectionSystem(spawnProtectionManager));

        // Register block damage protection (mining progress)
        registry.registerSystem(new DamageBlockProtectionSystem(spawnProtectionManager));

        // Register PvP protection using FilterDamageGroup
        registry.registerSystem(new SpawnDamageFilterSystem(spawnProtectionManager));
    }

    /**
     * Prevents block breaking in spawn area.
     */
    private static class BreakBlockProtectionSystem 
            extends EntityEventSystem<EntityStore, BreakBlockEvent> {
        
        private final SpawnProtectionManager manager;

        BreakBlockProtectionSystem(SpawnProtectionManager manager) {
            super(BreakBlockEvent.class);
            this.manager = manager;
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Query.any();
        }

        @Override
        public void handle(int index, @NotNull ArchetypeChunk<EntityStore> chunk,
                           @NotNull Store<EntityStore> store,
                           @NotNull CommandBuffer<EntityStore> buffer,
                           @NotNull BreakBlockEvent event) {
            if (event.isCancelled()) {
                return;
            }

            if (shouldCancelBlockEvent(manager, store, chunk, index, event.getTargetBlock())) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Prevents block placing in spawn area.
     */
    private static class PlaceBlockProtectionSystem 
            extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
        
        private final SpawnProtectionManager manager;

        PlaceBlockProtectionSystem(SpawnProtectionManager manager) {
            super(PlaceBlockEvent.class);
            this.manager = manager;
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Query.any();
        }

        @Override
        public void handle(int index, @NotNull ArchetypeChunk<EntityStore> chunk,
                           @NotNull Store<EntityStore> store,
                           @NotNull CommandBuffer<EntityStore> buffer,
                           @NotNull PlaceBlockEvent event) {
            if (event.isCancelled()) {
                return;
            }

            if (shouldCancelBlockEvent(manager, store, chunk, index, event.getTargetBlock())) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Prevents block damage (mining progress) in spawn area.
     */
    private static class DamageBlockProtectionSystem 
            extends EntityEventSystem<EntityStore, DamageBlockEvent> {
        
        private final SpawnProtectionManager manager;

        DamageBlockProtectionSystem(SpawnProtectionManager manager) {
            super(DamageBlockEvent.class);
            this.manager = manager;
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Query.any();
        }

        @Override
        public void handle(int index, @NotNull ArchetypeChunk<EntityStore> chunk,
                           @NotNull Store<EntityStore> store,
                           @NotNull CommandBuffer<EntityStore> buffer,
                           @NotNull DamageBlockEvent event) {
            if (event.isCancelled()) {
                return;
            }

            if (shouldCancelBlockEvent(manager, store, chunk, index, event.getTargetBlock())) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Filters damage in spawn area by running in the FilterDamageGroup.
     * When invulnerable is enabled, cancels ALL damage to players in spawn.
     */
    private static class SpawnDamageFilterSystem extends DamageEventSystem {
        
        private final SpawnProtectionManager manager;

        SpawnDamageFilterSystem(SpawnProtectionManager manager) {
            super();
            this.manager = manager;
        }

        @Override
        public SystemGroup<EntityStore> getGroup() {
            // Use the FilterDamageGroup to run in the filtering phase
            return DamageModule.get().getFilterDamageGroup();
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Query.any();
        }

        @Override
        public void handle(int index, @NotNull ArchetypeChunk<EntityStore> chunk,
                           @NotNull Store<EntityStore> store,
                           @NotNull CommandBuffer<EntityStore> buffer,
                           @NotNull Damage event) {
            // Only apply if spawn protection and invulnerability are both enabled
            if (!manager.isEnabled() || !manager.isInvulnerableEnabled() || event.isCancelled()) {
                return;
            }

            // Check if the victim is a player
            PlayerRef victimRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (victimRef == null) {
                return;
            }

            // Get current world name
            String worldName = store.getExternalData().getWorld().getName();

            // Check if victim is in protected area
            if (!manager.isInProtectedArea(worldName, victimRef.getTransform().getPosition())) {
                return;
            }

            // Cancel ALL damage when player is in spawn with invulnerability enabled
            event.setCancelled(true);
            event.setAmount(0);
        }
    }
}
