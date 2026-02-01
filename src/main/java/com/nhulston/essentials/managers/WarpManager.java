package com.nhulston.essentials.managers;

import com.nhulston.essentials.Essentials;
import com.nhulston.essentials.models.Warp;
import com.nhulston.essentials.util.MessageManager;
import com.nhulston.essentials.util.StorageManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.regex.Pattern;

public class WarpManager {
    private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");
    private static final int MAX_NAME_LENGTH = 16;

    private final StorageManager storageManager;
    private final MessageManager messages;

    public WarpManager(@Nonnull StorageManager storageManager) {
        this.storageManager = storageManager;
        this.messages = Essentials.getInstance().getMessageManager();
    }

    @Nullable
    public String validateWarpName(@Nonnull String name) {
        if (name.isEmpty()) {
            return messages.get("validation.warp.name-empty");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            return messages.get("validation.warp.name-too-long", Map.of("max", String.valueOf(MAX_NAME_LENGTH)));
        }
        if (!VALID_NAME_PATTERN.matcher(name).matches()) {
            return messages.get("validation.warp.name-invalid");
        }
        return null;
    }

    @Nullable
    public String setWarp(@Nonnull String name, @Nonnull String world,
                          double x, double y, double z, float yaw, float pitch) {
        String validationError = validateWarpName(name);
        if (validationError != null) {
            return validationError;
        }

        Warp warp = new Warp(world, x, y, z, yaw, pitch);
        storageManager.setWarp(name, warp);
        return null;
    }

    @Nullable
    public Warp getWarp(@Nonnull String name) {
        return storageManager.getWarp(name);
    }

    @Nonnull
    public Map<String, Warp> getWarps() {
        return storageManager.getWarps();
    }

    public boolean deleteWarp(@Nonnull String name) {
        return storageManager.deleteWarp(name);
    }
}
