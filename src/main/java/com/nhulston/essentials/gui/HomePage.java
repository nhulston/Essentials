package com.nhulston.essentials.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.Essentials;
import com.nhulston.essentials.gui.common.GridEntry;
import com.nhulston.essentials.gui.common.GridPageBuilder;
import com.nhulston.essentials.gui.common.SelectionPageData;
import com.nhulston.essentials.managers.BackManager;
import com.nhulston.essentials.managers.HomeManager;
import com.nhulston.essentials.managers.TeleportManager;
import com.nhulston.essentials.models.Home;
import com.nhulston.essentials.util.MessageManager;
import com.nhulston.essentials.util.Msg;
import com.nhulston.essentials.util.TeleportUtil;

/**
 * A GUI page for selecting homes.
 * Shows all homes belonging to the player.
 */
public class HomePage extends InteractiveCustomUIPage<SelectionPageData> {

    private static final String HOME_PAGE_UI = "Pages/Essentials_HomePage.ui";
    private static final String HOME_ROWS_SELECTOR = "#HomeRows";

    private final HomeManager homeManager;
    private final TeleportManager teleportManager;
    private final BackManager backManager;
    private final MessageManager messages;

    public HomePage(@Nonnull PlayerRef playerRef,
                    @Nonnull HomeManager homeManager,
                    @Nonnull TeleportManager teleportManager,
                    @Nonnull BackManager backManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, SelectionPageData.CODEC);
        this.homeManager = homeManager;
        this.teleportManager = teleportManager;
        this.backManager = backManager;
        this.messages = Essentials.getInstance().getMessageManager();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        
        List<GridEntry> entries = buildHomeEntries();

        GridPageBuilder.build(
                commandBuilder, eventBuilder, entries,
                GridPageBuilder.DEFAULT_COLUMNS,
                HOME_PAGE_UI, GridPageBuilder.DEFAULT_ENTRY_UI,
                GridPageBuilder.DEFAULT_SELECTION_KEY, HOME_ROWS_SELECTOR
        );
    }
    
    /**
     * Builds the list of home entries for the player.
     */
    private List<GridEntry> buildHomeEntries() {
        UUID playerUuid = playerRef.getUuid();
        Map<String, Home> homes = homeManager.getHomes(playerUuid);
        List<GridEntry> entries = new ArrayList<>();
        
        for (Map.Entry<String, Home> entry : homes.entrySet()) {
            String homeName = entry.getKey();
            Home home = entry.getValue();
            
            // Show world name only (no coordinates for privacy/streamer safety)
            String status = home.getWorld();
            
            // Use home name as both ID and display name (capitalize first letter)
            String displayName = GridEntry.capitalize(homeName);
            entries.add(new GridEntry(homeName, displayName, status));
        }
        
        return entries;
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull SelectionPageData data) {
        if (!data.hasSelection()) {
            return;
        }

        String homeName = data.getSelection();
        Home home = homeManager.getHome(playerRef.getUuid(), homeName);
        
        if (home == null) {
            Msg.send(playerRef, messages.get("gui.home.not-found"));
            this.close();
            return;
        }

        // Close the GUI first
        this.close();
        
        // Get the player's current world for teleport
        World world = store.getExternalData().getWorld();
        
        // Save back location and queue teleport
        backManager.setBackLocation(store, ref, playerRef, world);
        Vector3d startPosition = TeleportUtil.getStartPosition(store, ref);
        
        if (startPosition == null) {
            Msg.send(playerRef, messages.get("errors.generic"));
            return;
        }

        teleportManager.queueTeleport(
                playerRef, ref, store, startPosition,
                home.getWorld(), home.getX(), home.getY(), home.getZ(), home.getYaw(), home.getPitch(),
                messages.get("commands.home.teleported", Map.of("home", homeName))
        );
    }
    
}
