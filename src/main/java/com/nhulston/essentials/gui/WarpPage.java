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
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
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
import com.nhulston.essentials.managers.TeleportManager;
import com.nhulston.essentials.managers.WarpManager;
import com.nhulston.essentials.models.Warp;
import com.nhulston.essentials.util.MessageManager;
import com.nhulston.essentials.util.Msg;
import com.nhulston.essentials.util.TeleportUtil;

/**
 * A GUI page for selecting warps.
 * Only shows warps the player has permission to access.
 */
public class WarpPage extends InteractiveCustomUIPage<SelectionPageData> {

    private static final String WARP_PAGE_UI = "Pages/Essentials_WarpPage.ui";
    private static final String WARP_ROWS_SELECTOR = "#WarpRows";

    private final WarpManager warpManager;
    private final TeleportManager teleportManager;
    private final BackManager backManager;
    private final MessageManager messages;

    public WarpPage(@Nonnull PlayerRef playerRef,
                    @Nonnull WarpManager warpManager,
                    @Nonnull TeleportManager teleportManager,
                    @Nonnull BackManager backManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, SelectionPageData.CODEC);
        this.warpManager = warpManager;
        this.teleportManager = teleportManager;
        this.backManager = backManager;
        this.messages = Essentials.getInstance().getMessageManager();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        
        List<GridEntry> entries = buildWarpEntries();

        GridPageBuilder.build(
                commandBuilder, eventBuilder, entries,
                GridPageBuilder.DEFAULT_COLUMNS,
                WARP_PAGE_UI, GridPageBuilder.DEFAULT_ENTRY_UI,
                GridPageBuilder.DEFAULT_SELECTION_KEY, WARP_ROWS_SELECTOR
        );
    }
    
    /**
     * Builds the list of warp entries, filtering by permission.
     */
    private List<GridEntry> buildWarpEntries() {
        Map<String, Warp> allWarps = warpManager.getWarps();
        List<GridEntry> entries = new ArrayList<>();
        UUID playerUuid = playerRef.getUuid();
        
        for (Map.Entry<String, Warp> entry : allWarps.entrySet()) {
            String warpName = entry.getKey();
            Warp warp = entry.getValue();
            
            // Only show warps the player has permission for
            String permission = "essentials.warps." + warpName.toLowerCase();
            if (!PermissionsModule.get().hasPermission(playerUuid, permission)) {
                continue;
            }
            
            // Format status as coordinates or world name
            String status = formatWarpStatus(warp);
            
            // Use warp name as both ID and display name (capitalize first letter)
            String displayName = GridEntry.capitalize(warpName);
            entries.add(new GridEntry(warpName, displayName, status));
        }
        
        return entries;
    }
    
    /**
     * Formats the warp status text showing world and coordinates.
     */
    private String formatWarpStatus(@Nonnull Warp warp) {
        return String.format("%s\n(%d, %d, %d)",
                warp.getWorld(),
                (int) warp.getX(),
                (int) warp.getY(),
                (int) warp.getZ());
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull SelectionPageData data) {
        if (!data.hasSelection()) {
            return;
        }

        String warpName = data.getSelection();
        Warp warp = warpManager.getWarp(warpName);
        
        if (warp == null) {
            Msg.send(playerRef, messages.get("gui.warp.not-found"));
            this.close();
            return;
        }

        // Double-check permission (in case permissions changed while GUI was open)
        String permission = "essentials.warps." + warpName.toLowerCase();
        if (!PermissionsModule.get().hasPermission(playerRef.getUuid(), permission)) {
            Msg.send(playerRef, messages.get("gui.warp.no-permission"));
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
                warp.getWorld(), warp.getX(), warp.getY(), warp.getZ(), warp.getYaw(), warp.getPitch(),
                messages.get("commands.warp.teleported", Map.of("warp", warpName))
        );
    }
    
}
