package com.nhulston.essentials.integration.papi;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;

public class PAPIImplementation implements PlaceholderAPI {
    @Override
    public @NotNull String setPlaceholders(final PlayerRef ref, final @NotNull String message) {
        return at.helpch.placeholderapi.PlaceholderAPI.setPlaceholders(ref, message);
    }
}
