package com.nhulston.essentials.commands.msg;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.Essentials;
import com.nhulston.essentials.util.MessageManager;
import com.nhulston.essentials.util.Msg;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Command to reply to the last person who messaged you.
 * Usage: /r <message>
 * Aliases: /reply
 */
public class ReplyCommand extends AbstractPlayerCommand {
    private final MessageManager messages;

    public ReplyCommand() {
        super("r", "Reply to your last message");
        this.messages = Essentials.getInstance().getMessageManager();
        
        // Allow extra arguments since we parse them manually
        setAllowsExtraArguments(true);
        
        addAliases("reply");
        requirePermission("essentials.msg");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        // Parse from raw input: "/r <message...>"
        String rawInput = context.getInputString();
        String[] parts = rawInput.split("\\s+", 2); // Split into [command, message]
        
        if (parts.length < 2) {
            Msg.send(context, messages.get("commands.reply.usage"));
            return;
        }
        
        String message = parts[1];

        // Get last message partner
        UUID targetUuid = MsgCommand.getLastMessagePartner(playerRef.getUuid());
        if (targetUuid == null) {
            Msg.send(context, messages.get("commands.reply.no-one"));
            return;
        }

        // Find target player
        PlayerRef target = Universe.get().getPlayer(targetUuid);
        if (target == null) {
            Msg.send(context, messages.get("commands.reply.player-offline"));
            return;
        }

        MsgCommand.sendMessage(playerRef, target, message, context, messages);
    }
}
