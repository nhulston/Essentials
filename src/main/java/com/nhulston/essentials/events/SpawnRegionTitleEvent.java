package com.nhulston.essentials.events;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.nhulston.essentials.managers.SpawnProtectionManager;
import com.nhulston.essentials.util.ConfigManager;
import com.nhulston.essentials.util.Log;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnRegionTitleEvent {
    private final SpawnProtectionManager spawnProtectionManager;
    private final ConfigManager configManager;
    
    // Static reference for cleanup access
    private static final Map<UUID, Boolean> playerInSpawn = new ConcurrentHashMap<>();

    public SpawnRegionTitleEvent(@Nonnull SpawnProtectionManager spawnProtectionManager, 
                                  @Nonnull ConfigManager configManager) {
        this.spawnProtectionManager = spawnProtectionManager;
        this.configManager = configManager;
    }

    public void register(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        if (!spawnProtectionManager.isEnabled() || !configManager.isSpawnProtectionShowTitles()) {
            return;
        }

        registry.registerSystem(new SpawnRegionTitleSystem(spawnProtectionManager, configManager));
        Log.info("Spawn region titles enabled.");
    }

    /**
     * Clean up player data when they disconnect.
     */
    public static void onPlayerQuit(UUID uuid) {
        playerInSpawn.remove(uuid);
    }

    /**
     * Ticking system that detects when players enter/exit spawn area and shows titles.
     */
    private static class SpawnRegionTitleSystem extends EntityTickingSystem<EntityStore> {
        private final SpawnProtectionManager manager;
        private final ConfigManager config;

        SpawnRegionTitleSystem(SpawnProtectionManager manager, ConfigManager config) {
            this.manager = manager;
            this.config = config;
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Query.any();
        }

        @Override
        public void tick(float deltaTime, int index, ArchetypeChunk<EntityStore> chunk,
                         @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> buffer) {
            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef == null) {
                return;
            }

            UUID uuid = playerRef.getUuid();
            String worldName = store.getExternalData().getWorld().getName();
            boolean isInSpawn = manager.isInProtectedArea(worldName, playerRef.getTransform().getPosition());
            Boolean wasInSpawn = playerInSpawn.get(uuid);

            // First time seeing this player, just record state
            if (wasInSpawn == null) {
                playerInSpawn.put(uuid, isInSpawn);
                return;
            }

            // Check for state change
            if (isInSpawn && !wasInSpawn) {
                // Player entered spawn
                String enterTitle = config.getSpawnProtectionEnterTitle();
                String enterSubtitle = config.getSpawnProtectionEnterSubtitle();
                if (!enterTitle.isEmpty() || !enterSubtitle.isEmpty()) {
                    EventTitleUtil.hideEventTitleFromPlayer(playerRef, 0);
                    EventTitleUtil.showEventTitleToPlayer(
                            playerRef,
                            Message.raw(enterTitle),
                            Message.raw(enterSubtitle),
                            true
                    );
                }
            } else if (!isInSpawn && wasInSpawn) {
                // Player left spawn
                String exitTitle = config.getSpawnProtectionExitTitle();
                String exitSubtitle = config.getSpawnProtectionExitSubtitle();
                if (!exitTitle.isEmpty() || !exitSubtitle.isEmpty()) {
                    EventTitleUtil.hideEventTitleFromPlayer(playerRef, 0);
                    EventTitleUtil.showEventTitleToPlayer(
                            playerRef,
                            Message.raw(exitTitle),
                            Message.raw(exitSubtitle),
                            false
                    );
                }
            }

            playerInSpawn.put(uuid, isInSpawn);
        }
    }
}
