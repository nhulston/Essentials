package com.nhulston.essentials.events;

import com.hypixel.hytale.builtin.beds.sleep.components.PlayerSleep;
import com.hypixel.hytale.builtin.beds.sleep.components.PlayerSomnolence;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.util.ColorUtil;
import com.nhulston.essentials.util.ConfigManager;
import com.nhulston.essentials.util.Log;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles sleep percentage - skips night when enough players are sleeping.
 */
public class SleepPercentageEvent {
    private final ConfigManager configManager;

    public SleepPercentageEvent(@Nonnull ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void register(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        if (!configManager.isSleepEnabled()) {
            return;
        }

        registry.registerSystem(new SleepTrackingSystem(configManager));
        Log.info("Sleep percentage system enabled (" + configManager.getSleepPercentage() + "%).");
    }

    /**
     * Tracks when players enter/exit beds using RefChangeSystem on PlayerSomnolence.
     */
    private static class SleepTrackingSystem extends RefChangeSystem<EntityStore, PlayerSomnolence> {
        private static final double MORNING_TIME = 0.25; // ~5:00 AM
        private static final double NIGHT_START = 0.875; // 9:00 PM
        private static final double NIGHT_END = 0.25; // ~6:00 AM

        private final ConfigManager config;
        // Track sleeping players per world
        private final Map<String, AtomicInteger> sleepingCountPerWorld = new ConcurrentHashMap<>();

        SleepTrackingSystem(ConfigManager config) {
            this.config = config;
        }

        @Override
        public @NotNull ComponentType<EntityStore, PlayerSomnolence> componentType() {
            return PlayerSomnolence.getComponentType();
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Query.any();
        }

        @Override
        public void onComponentAdded(@NotNull Ref<EntityStore> ref, @NotNull PlayerSomnolence somnolence,
                                     @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> buffer) {
            if (isInBed(somnolence)) {
                onPlayerEnteredBed(store);
            }
        }

        @Override
        public void onComponentSet(@NotNull Ref<EntityStore> ref, PlayerSomnolence oldSomnolence,
                                   @NotNull PlayerSomnolence newSomnolence,
                                   @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> buffer) {
            boolean wasInBed = isInBed(oldSomnolence);
            boolean isInBed = isInBed(newSomnolence);

            if (!wasInBed && isInBed) {
                onPlayerEnteredBed(store);
            } else if (wasInBed && !isInBed) {
                onPlayerLeftBed(store);
            }
        }

        @Override
        public void onComponentRemoved(@NotNull Ref<EntityStore> ref, @NotNull PlayerSomnolence somnolence,
                                       @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> buffer) {
            if (isInBed(somnolence)) {
                onPlayerLeftBed(store);
            }
        }

        /**
         * Checks if the player is in a bed (NoddingOff, Slumber, or MorningWakeUp).
         */
        private boolean isInBed(PlayerSomnolence somnolence) {
            if (somnolence == null) {
                return false;
            }
            PlayerSleep sleepState = somnolence.getSleepState();
            // Count NoddingOff (getting into bed), Slumber (sleeping), and MorningWakeUp (waking)
            return sleepState instanceof PlayerSleep.NoddingOff 
                || sleepState instanceof PlayerSleep.Slumber;
        }

        private void onPlayerEnteredBed(Store<EntityStore> store) {
            EntityStore entityStore = store.getExternalData();
            World world = entityStore.getWorld();

            String worldName = world.getName();
            AtomicInteger count = sleepingCountPerWorld.computeIfAbsent(worldName, _ -> new AtomicInteger(0));
            int newCount = count.incrementAndGet();

            checkAndSkipNight(world, store, newCount);
        }

        private void onPlayerLeftBed(Store<EntityStore> store) {
            EntityStore entityStore = store.getExternalData();
            World world = entityStore.getWorld();

            String worldName = world.getName();
            AtomicInteger count = sleepingCountPerWorld.get(worldName);
            if (count != null) {
                // Ensure counter doesn't go below 0
                count.updateAndGet(c -> Math.max(0, c - 1));
            }
        }

        private void checkAndSkipNight(World world, Store<EntityStore> store, int sleepingCount) {
            int totalPlayers = world.getPlayerCount();
            if (totalPlayers == 0) return;

            int sleepPercentage = (sleepingCount * 100) / totalPlayers;
            int requiredPercentage = config.getSleepPercentage();

            if (sleepPercentage >= requiredPercentage) {
                // Only skip if it's actually nighttime
                WorldTimeResource timeResource = store.getResource(WorldTimeResource.getResourceType());

                float dayProgress = timeResource.getDayProgress();
                boolean isNight = dayProgress >= NIGHT_START || dayProgress < NIGHT_END;
                
                if (isNight) {
                    skipToMorning(world, store);
                }
            }
        }

        private void skipToMorning(World world, Store<EntityStore> store) {
            WorldTimeResource timeResource = store.getResource(WorldTimeResource.getResourceType());

            // Set time to morning
            timeResource.setDayTime(MORNING_TIME, world, store);

            // Reset sleeping count for this world
            String worldName = world.getName();
            AtomicInteger count = sleepingCountPerWorld.get(worldName);
            if (count != null) {
                count.set(0);
            }

            // Broadcast message
            world.sendMessage(ColorUtil.colorize("&eGoodnight! Skipping to morning..."));
            Log.info("Night skipped in world '" + worldName + "' due to sleep percentage.");
        }
    }
}
