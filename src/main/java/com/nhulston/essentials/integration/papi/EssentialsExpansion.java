package com.nhulston.essentials.integration.papi;

import at.helpch.placeholderapi.PlaceholderAPI;
import at.helpch.placeholderapi.expansion.PlaceholderExpansion;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.nhulston.essentials.Essentials;
import com.nhulston.essentials.managers.HomeManager;
import com.nhulston.essentials.managers.KitManager;
import com.nhulston.essentials.models.Home;
import com.nhulston.essentials.models.Kit;
import com.nhulston.essentials.models.Warp;
import com.nhulston.essentials.util.StorageManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EssentialsExpansion extends PlaceholderExpansion {
    private static final PermissionsModule PERMISSIONS = PermissionsModule.get();
    private static final Pattern ARGUMENT_DELIMITER = Pattern.compile("_");
    private static final DecimalFormat TWO_DECIMAL = new DecimalFormat("0.00");

    private final HomeManager homeManager;
    private final KitManager kitManager;
    private final StorageManager storageManager;

    public EssentialsExpansion(@NotNull final HomeManager homeManager, @NotNull final KitManager kitManager,
                               @NotNull final StorageManager storageManager) {
        this.homeManager = homeManager;
        this.kitManager = kitManager;
        this.storageManager = storageManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "essentials";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Nicholas Hulston";
    }

    @Override
    public @NotNull String getVersion() {
        return Essentials.getInstance().getManifest().getVersion().toString();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(final PlayerRef player, @NotNull final String params) {
        switch (params) {
            case "max_homes":
                return String.valueOf(homeManager.getMaxHomes(player.getUuid()));
            case "homes_num":
                return String.valueOf(homeManager.getHomes(player.getUuid()).size());
            case "homes_names":
                return String.join(", ", homeManager.getHomes(player.getUuid()).keySet());
            case "all_kits_num":
                return String.valueOf(kitManager.getKits().size());
            case "all_kits_names":
                return kitManager.getKits().stream()
                        .map(Kit::getDisplayName)
                        .collect(Collectors.joining(", "));
            case "allowed_kits_num":
                return String.valueOf(kitManager.getKits().stream()
                        .filter(kit -> PERMISSIONS.hasPermission(player.getUuid(), "essentials.kit." + kit.getId()))
                        .count());
            case "allowed_kits_names":
                return kitManager.getKits().stream()
                        .filter(kit -> PERMISSIONS.hasPermission(player.getUuid(), "essentials.kit." + kit.getId()))
                        .map(Kit::getDisplayName)
                        .collect(Collectors.joining(", "));
            case "all_warps_num":
                return String.valueOf(storageManager.getWarps().size());
            case "all_warps_names":
                return String.join(", ", storageManager.getWarps().keySet());
        }

        final String[] args = ARGUMENT_DELIMITER.split(params);

        if (args.length == 1) {
            return null;
        }

        if (args.length != 3) {
            return null;
        }

        switch (args[0]) {
            case "warp":
                final Warp warp = storageManager.getWarp(args[1]);

                if (warp == null) {
                    return null;
                }

                switch (args[2]) {
                    case "world": return warp.getWorld();
                    case "coords": return twoDec(warp.getX()) + " " + twoDec(warp.getY()) + " " + twoDec(warp.getZ());
                    case "x": return twoDec(warp.getX());
                    case "y": return twoDec(warp.getY());
                    case "z": return twoDec(warp.getZ());
                    case "yaw": return String.valueOf(warp.getYaw());
                    case "pitch": return String.valueOf(warp.getPitch());
                }

            case "kit":
                final Kit kit = kitManager.getKit(args[1]);

                if (kit == null) {
                    return null;
                }

                switch (args[2]) {
                    case "name":
                        return kit.getDisplayName();
                    case "id":
                        return kit.getId();
                    case "type":
                        return kit.getType();
                    case "cooldown":
                        return String.valueOf(kit.getCooldown());
                    case "isreplacemode":
                        return PlaceholderAPI.booleanValue(kit.isReplaceMode());
                    case "itemsnum":
                        return String.valueOf(kit.getItems().size());
                    case "allowed":
                        return PlaceholderAPI.booleanValue(PERMISSIONS.hasPermission(player.getUuid(), "essentials.kit." + kit.getId()));
                }

            case "home":
                final Home home = homeManager.getHome(player.getUuid(), args[1]);

                if (home == null) {
                    return null;
                }

                switch (args[2]) {
                    case "world": return home.getWorld();
                    case "coords": return twoDec(home.getX()) + " " + twoDec(home.getY()) + " " + twoDec(home.getZ());
                    case "x": return twoDec(home.getX());
                    case "y": return twoDec(home.getY());
                    case "z": return twoDec(home.getZ());
                    case "yaw": return String.valueOf(home.getYaw());
                    case "pitch": return String.valueOf(home.getPitch());
                    case "createdat": return String.valueOf(home.getCreatedAt());
                }
        }

        return null;
    }

    @NotNull
    private static String twoDec(final double num) {
        return TWO_DECIMAL.format(num);
    }
}
