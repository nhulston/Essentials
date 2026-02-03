package com.nhulston.essentials.integration.papi;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;

public interface PlaceholderAPI {
    @NotNull
    String setPlaceholders(final PlayerRef ref, @NotNull final String message);
}
