package com.nhulston.essentials.commands.essentials;

import com.hypixel.hytale.protocol.MaybeBool;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.nhulston.essentials.Essentials;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

/**
 * Main essentials command.
 * Usage: /essentials - Shows version info with clickable link
 * Usage: /essentials reload - Reloads configuration (requires essentials.reload permission)
 * Can be executed by console or players.
 */
public class EssentialsCommand extends AbstractCommand {
    private static final String CURSEFORGE_URL = "https://www.curseforge.com/hytale/mods/essentials-core";
    private static final String GREEN = "#55FF55";
    private static final String GRAY = "#AAAAAA";

    public EssentialsCommand() {
        super("essentials", "Show EssentialsCore version information");

        addAliases("ess");

        // Add reload subcommand
        addSubCommand(new EssentialsReloadCommand());
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        Message prefix = Message.raw("Running ").color(GRAY);
        Message versionText = Message.raw("EssentialsCore v" + Essentials.VERSION)
                .color(GREEN)
                .link(CURSEFORGE_URL);
        versionText.getFormattedMessage().underlined = MaybeBool.True;
        Message period = Message.raw(".").color(GRAY);

        context.sendMessage(Message.join(prefix, versionText, period));
        return CompletableFuture.completedFuture(null);
    }
}
