package com.nhulston.essentials.gui.common;

import javax.annotation.Nonnull;

/**
 * Represents a single entry in a grid-based selection page.
 * Used by WarpPage, HomePage, and KitPage for consistent UI display.
 *
 * @param id          Unique identifier for this entry (e.g., warp name, home name, kit id)
 * @param displayName The name shown to the user in the UI
 * @param status      Status text shown below the name (e.g., "Ready", cooldown time, coordinates)
 */
public record GridEntry(
        @Nonnull String id,
        @Nonnull String displayName,
        @Nonnull String status
) {
    /**
     * Capitalizes the first letter and lowercases the rest.
     * Useful for formatting warp/home names for display.
     */
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1).toLowerCase();
    }
}
