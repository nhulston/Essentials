# Hytale Commands Guide

## Overview

This guide covers how to implement commands in Hytale, including arguments (required and optional), permissions, player targeting, subcommands, and common patterns. Every concept includes working code examples from the Essentials plugin.

**Important Note:** Hytale's command system does not support `OptionalArg` - instead, use **Usage Variants** to implement optional arguments (see sections 3 and 7).

## Table of Contents

1. [Base Command Classes](#base-command-classes)
2. [Required Arguments](#required-arguments)
3. [Optional Arguments](#optional-arguments)
4. [Permissions](#permissions)
5. [Player Targeting](#player-targeting)
6. [Subcommands](#subcommands)
7. [Usage Variants](#usage-variants)
8. [Advanced Patterns](#advanced-patterns)
9. [Common Argument Types](#common-argument-types)
10. [Best Practices](#best-practices)

---

## Base Command Classes

Hytale provides two main base classes for commands:

### AbstractPlayerCommand
For commands that **require a player executor** (cannot be run from console).

```java
public class HealCommand extends AbstractPlayerCommand {
    public HealCommand() {
        super("heal", "Restore your health to full");
        requirePermission("essentials.heal");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        // Command logic - playerRef is guaranteed to be non-null
        EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
        EntityStat health = stats.getStat(DefaultStatType.HEALTH);
        health.setCurrentValue(health.getMaxValue());
        
        Msg.send(context, "Your health has been restored!");
    }
}
```

**When to use:**
- Command requires a player context (teleport, inventory, health)
- Cannot be executed from console
- Provides direct access to `playerRef`, `store`, `ref`, and `world`

### AbstractCommand
For commands that **can be executed by console OR players**.

```java
public class ShoutCommand extends AbstractCommand {
    public ShoutCommand() {
        super("shout", "Broadcast a message to all players");
        addAliases("broadcast");
        requirePermission("essentials.shout");
        setAllowsExtraArguments(true);  // Allow multi-word message
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        String rawInput = context.getInputString();
        String[] parts = rawInput.split("\\s+", 2);
        
        if (parts.length < 2) {
            Msg.send(context, "Usage: /shout <message>");
            return CompletableFuture.completedFuture(null);
        }
        
        String message = parts[1];
        Universe.get().sendMessage(Message.raw(message));
        
        return CompletableFuture.completedFuture(null);
    }
}
```

**When to use:**
- Command should work from console
- Doesn't require player-specific context
- Returns `CompletableFuture<Void>` for async operations

---

## Required Arguments

Required arguments must be provided or the command fails with usage information.

### Basic Required Argument

**Example: TphereCommand** - Teleport a player to you

```java
public class TphereCommand extends AbstractPlayerCommand {
    private final RequiredArg<PlayerRef> targetArg;

    public TphereCommand() {
        super("tphere", "Teleport a player to you");
        this.targetArg = withRequiredArg("player", "Player to teleport", ArgTypes.PLAYER_REF);
        requirePermission("essentials.tphere");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        // Get the required argument value
        PlayerRef target = context.get(targetArg);
        
        // Validate the target
        if (target == null) {
            Msg.send(context, "Player not found.");
            return;
        }

        if (target.getUuid().equals(playerRef.getUuid())) {
            Msg.send(context, "You cannot teleport yourself to yourself!");
            return;
        }

        // Teleport the target to the command sender
        TeleportUtil.teleportToPlayer(target, playerRef);
        Msg.send(context, "Teleported " + target.getUsername() + " to you.");
    }
}
```

**Usage:** `/tphere PlayerName`

### Multiple Required Arguments

**Example: KitCreateCommand** - Create a kit from inventory

```java
public class KitCreateCommand extends AbstractPlayerCommand {
    private final RequiredArg<String> nameArg;

    public KitCreateCommand(@Nonnull KitManager kitManager) {
        super("create", "Create a kit from your current inventory");
        requirePermission("essentials.kit.create");
        this.nameArg = withRequiredArg("name", "Kit name", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        String kitName = context.get(nameArg);
        
        // Validate kit name
        if (kitManager.kitExists(kitName)) {
            Msg.send(context, "A kit with that name already exists!");
            return;
        }

        // Create kit from player's current inventory
        Kit kit = kitManager.createKitFromPlayer(kitName, store, ref);
        kitManager.saveKit(kit);
        
        Msg.send(context, "Created kit: " + kitName);
    }
}
```

**Usage:** `/kit create StarterKit`

**Key Pattern:**
1. Declare `RequiredArg<T>` field in class
2. Initialize with `withRequiredArg(name, description, type)` in constructor
3. Retrieve value with `context.get(arg)` in execute method
4. Validate the value (null checks, business logic)

---

## Optional Arguments

**Important Note:** Hytale's command system does not have a working `OptionalArg` type. To implement optional arguments, you must use **Usage Variants** (see the [Usage Variants](#usage-variants) section below). Usage variants allow you to define a base command with no arguments and a variant with required arguments.

### Pattern for Optional Arguments (Using Usage Variants)

To implement commands with optional arguments, create a base command for the no-argument case and add a usage variant for the with-argument case.

**Example: TpacceptCommand** - Accept teleport request

```java
public class TpacceptCommand extends AbstractPlayerCommand {
    private final TpaManager tpaManager;
    private final TeleportManager teleportManager;
    private final BackManager backManager;

    public TpacceptCommand(@Nonnull TpaManager tpaManager, @Nonnull TeleportManager teleportManager,
                          @Nonnull BackManager backManager) {
        super("tpaccept", "Accept a teleport request");
        this.tpaManager = tpaManager;
        this.teleportManager = teleportManager;
        this.backManager = backManager;
        addAliases("tpyes");
        requirePermission("essentials.tpaccept");
        
        // Add usage variant for accepting specific player's request
        addUsageVariant(new TpacceptNamedCommand(tpaManager, teleportManager, backManager));
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        // /tpaccept with no arguments - accept most recent request
        TpaRequest request = tpaManager.acceptMostRecentRequest(playerRef);
        if (request == null) {
            Msg.send(context, "You have no pending teleport requests.");
            return;
        }

        // Process the accepted request
        teleportManager.teleport(request.getRequester(), playerRef);
        Msg.send(context, "Accepted teleport request from " + request.getRequester().getUsername());
    }

    // Usage variant for /tpaccept <player>
    private static class TpacceptNamedCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> playerArg;
        private final TpaManager tpaManager;
        private final TeleportManager teleportManager;

        TpacceptNamedCommand(@Nonnull TpaManager tpaManager, @Nonnull TeleportManager teleportManager,
                            @Nonnull BackManager backManager) {
            super("Accept a teleport request from a specific player");
            this.tpaManager = tpaManager;
            this.teleportManager = teleportManager;
            this.playerArg = withRequiredArg("player", "Player whose request to accept", ArgTypes.STRING);
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            // /tpaccept <player>
            String requesterName = context.get(playerArg);
            
            // Accept request from specific player
            TpaRequest request = tpaManager.acceptRequest(playerRef, requesterName);
            if (request == null) {
                Msg.send(context, "No teleport request from " + requesterName);
                return;
            }

            // Process the accepted request
            teleportManager.teleport(request.getRequester(), playerRef);
            Msg.send(context, "Accepted teleport request from " + requesterName);
        }
    }
}
```

**Usage:**
- `/tpaccept` - Accepts most recent request
- `/tpaccept PlayerName` - Accepts request from specific player

**Key Pattern:**
1. Create a base command class for the no-argument case
2. Create an inner class extending `AbstractPlayerCommand` for the with-argument case
3. In the inner class, use `RequiredArg<T>` for the argument
4. Register the variant with `addUsageVariant(new VariantCommand(...))` in the base constructor
5. The command system will automatically route to the appropriate variant based on arguments provided

---

## Permissions

Permissions control who can execute commands and what features they can access.

### Basic Permission Check

```java
public class HealCommand extends AbstractPlayerCommand {
    public HealCommand() {
        super("heal", "Restore your health to full");
        requirePermission("essentials.heal");  // Requires permission to execute
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        // Only executes if player has essentials.heal permission
        EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
        EntityStat health = stats.getStat(DefaultStatType.HEALTH);
        health.setCurrentValue(health.getMaxValue());
        
        Msg.send(context, "Health restored!");
    }
}
```

### Runtime Permission Checks (Bypass Permissions)

Some commands need to check additional permissions at runtime:

**Example: RepairCommand with cooldown bypass**

```java
public class RepairCommand extends AbstractPlayerCommand {
    private static final String COOLDOWN_BYPASS_PERMISSION = "essentials.repair.cooldown.bypass";
    private final RepairManager repairManager;

    public RepairCommand(@Nonnull RepairManager repairManager) {
        super("repair", "Repair the item in your hand");
        this.repairManager = repairManager;
        addAliases("fix");
        requirePermission("essentials.repair");  // Base permission
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        UUID playerUuid = playerRef.getUuid();
        
        // Check if player can bypass cooldown
        boolean bypassCooldown = PermissionsModule.get()
            .hasPermission(playerUuid, COOLDOWN_BYPASS_PERMISSION);
        
        int cooldownSeconds = configManager.getRepairCooldown();
        
        // Enforce cooldown only if player doesn't have bypass permission
        if (cooldownSeconds > 0 && !bypassCooldown) {
            long lastUsed = repairManager.getLastUsedTime(playerUuid);
            long now = System.currentTimeMillis();
            long cooldownMillis = cooldownSeconds * 1000L;
            
            if (now - lastUsed < cooldownMillis) {
                long remaining = (cooldownMillis - (now - lastUsed)) / 1000;
                Msg.send(context, "Repair on cooldown. Wait " + remaining + " seconds.");
                return;
            }
        }

        // Repair the item
        repairManager.repairHeldItem(store, ref);
        
        if (!bypassCooldown) {
            repairManager.setLastUsedTime(playerUuid, System.currentTimeMillis());
        }
        
        Msg.send(context, "Item repaired!");
    }
}
```

### Per-Resource Permissions

**Example: Kit access permissions**

```java
public class KitClaimCommand extends AbstractPlayerCommand {
    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        String kitName = context.get(kitNameArg);
        Kit kit = kitManager.getKit(kitName);
        
        if (kit == null) {
            Msg.send(context, "Kit not found: " + kitName);
            return;
        }

        // Check if player has permission for this specific kit
        String kitPermission = "essentials.kit." + kitName.toLowerCase();
        if (!PermissionsModule.get().hasPermission(playerRef.getUuid(), kitPermission)) {
            Msg.send(context, "You don't have permission to claim this kit.");
            return;
        }

        // Check cooldown (unless player has bypass permission)
        boolean bypassCooldown = PermissionsModule.get()
            .hasPermission(playerRef.getUuid(), "essentials.kit.cooldown.bypass");
        
        if (!bypassCooldown && kitManager.isOnCooldown(playerRef.getUuid(), kitName)) {
            long remaining = kitManager.getCooldownRemaining(playerRef.getUuid(), kitName);
            Msg.send(context, "Kit on cooldown. Wait " + remaining + " seconds.");
            return;
        }

        // Give kit to player
        kitManager.giveKit(playerRef, kit, store, ref);
        Msg.send(context, "Claimed kit: " + kit.getDisplayName());
    }
}
```

**Permission Hierarchy for Kits:**
- `essentials.kit` - Base permission to use `/kit` command
- `essentials.kit.create` - Permission to create new kits
- `essentials.kit.delete` - Permission to delete kits
- `essentials.kit.cooldown.bypass` - Bypass all kit cooldowns
- `essentials.kit.starterkit` - Access to claim "starterkit" specifically
- `essentials.kit.vipkit` - Access to claim "vipkit" specifically

**Key Pattern:**
1. Use `requirePermission()` for base command access
2. Use `PermissionsModule.get().hasPermission()` for runtime checks
3. Create bypass permissions for cooldowns and restrictions
4. Use per-resource permissions for kits, warps, etc.

---

## Player Targeting

Commands often need to target other players. Hytale provides multiple ways to do this.

### Using ArgTypes.PLAYER_REF

The simplest and most type-safe approach:

```java
public class TphereCommand extends AbstractPlayerCommand {
    private final RequiredArg<PlayerRef> targetArg;

    public TphereCommand() {
        super("tphere", "Teleport a player to you");
        this.targetArg = withRequiredArg("player", "Player to teleport", ArgTypes.PLAYER_REF);
        requirePermission("essentials.tphere");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        PlayerRef target = context.get(targetArg);
        
        if (target == null) {
            Msg.send(context, "Player not found.");
            return;
        }

        if (target.getUuid().equals(playerRef.getUuid())) {
            Msg.send(context, "Cannot teleport yourself to yourself!");
            return;
        }

        // Get target's world and execute teleport on that world's thread
        World targetWorld = target.getWorld();
        if (targetWorld == null) {
            Msg.send(context, "Target player is not in a world.");
            return;
        }

        // Capture command executor's position
        TransformComponent executorTransform = store.getComponent(ref, TransformComponent.getComponentType());
        Vector3d executorPos = executorTransform.getPosition();
        Vector3f executorRot = executorTransform.getRotation();

        // Execute teleport on target's world thread
        targetWorld.execute(() -> {
            target.teleport(world.getName(), executorPos, executorRot);
            Msg.send(target, "You were teleported to " + playerRef.getUsername());
        });

        Msg.send(context, "Teleported " + target.getUsername() + " to you.");
    }
}
```

**Benefits:**
- Type-safe `PlayerRef` returned
- Automatic player name validation
- Tab completion support
- Null if player not found

### Manual Player Lookup

For more complex scenarios or when using raw input:

```java
public class MsgCommand extends AbstractPlayerCommand {
    public MsgCommand(@Nonnull PrivateMessageManager messageManager) {
        super("msg", "Send a private message to a player");
        this.messageManager = messageManager;
        addAliases("m", "message", "whisper", "pm");
        requirePermission("essentials.msg");
        setAllowsExtraArguments(true);  // Allow multi-word messages
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        String rawInput = context.getInputString();
        String[] parts = rawInput.split("\\s+", 3);  // ["/msg", "player", "message"]
        
        if (parts.length < 3) {
            Msg.send(context, "Usage: /msg <player> <message>");
            return;
        }

        String targetName = parts[1];
        String message = parts[2];

        // Manual player lookup
        PlayerRef target = findPlayer(targetName);
        if (target == null) {
            Msg.send(context, "Player not found: " + targetName);
            return;
        }

        if (target.getUuid().equals(playerRef.getUuid())) {
            Msg.send(context, "You cannot message yourself!");
            return;
        }

        // Send the message
        messageManager.sendMessage(playerRef, target, message);
        
        Msg.send(context, "To " + target.getUsername() + ": " + message);
        Msg.send(target, "From " + playerRef.getUsername() + ": " + message);
    }

    @Nullable
    private static PlayerRef findPlayer(String name) {
        List<PlayerRef> players = Universe.get().getPlayers();
        for (PlayerRef player : players) {
            if (player.getUsername().equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }
}
```

**When to use manual lookup:**
- Command needs raw multi-word input
- Custom name matching logic (partial names, nicknames)
- Searching offline players (not just online)

### Targeting All Players

**Example: ShoutCommand** - Broadcast to everyone

```java
public class ShoutCommand extends AbstractCommand {
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        String rawInput = context.getInputString();
        String[] parts = rawInput.split("\\s+", 2);
        
        if (parts.length < 2) {
            Msg.send(context, "Usage: /shout <message>");
            return CompletableFuture.completedFuture(null);
        }
        
        String message = parts[1];
        
        // Broadcast to all online players (thread-safe)
        Universe.get().sendMessage(Message.raw("[ANNOUNCEMENT] " + message));
        
        return CompletableFuture.completedFuture(null);
    }
}
```

---

## Subcommands

Subcommands allow commands to have sub-actions with their own arguments and permissions.

### Basic Subcommand Pattern

**Parent Command: KitCommand**

```java
public class KitCommand extends AbstractPlayerCommand {
    public KitCommand(@Nonnull KitManager kitManager) {
        super("kit", "Open the kit selection menu");
        this.addAliases("kits");
        requirePermission("essentials.kit");
        
        // Register subcommands
        addSubCommand(new KitCreateCommand(kitManager));
        addSubCommand(new KitDeleteCommand(kitManager));
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        // Base command logic - open GUI
        kitManager.openKitGui(playerRef);
    }
}
```

**Subcommand: KitCreateCommand**

```java
public class KitCreateCommand extends AbstractPlayerCommand {
    private final RequiredArg<String> nameArg;
    private final KitManager kitManager;

    public KitCreateCommand(@Nonnull KitManager kitManager) {
        super("create", "Create a kit from your current inventory");
        this.kitManager = kitManager;
        requirePermission("essentials.kit.create");  // Separate permission
        this.nameArg = withRequiredArg("name", "Kit name", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        String kitName = context.get(nameArg);
        
        if (kitManager.kitExists(kitName)) {
            Msg.send(context, "A kit with that name already exists!");
            return;
        }

        // Create kit from player's inventory
        Kit kit = kitManager.createKitFromPlayer(kitName, store, ref);
        kitManager.saveKit(kit);
        
        Msg.send(context, "Created kit: " + kitName);
    }
}
```

**Subcommand: KitDeleteCommand**

```java
public class KitDeleteCommand extends AbstractPlayerCommand {
    private final RequiredArg<String> nameArg;
    private final KitManager kitManager;

    public KitDeleteCommand(@Nonnull KitManager kitManager) {
        super("delete", "Delete a kit");
        this.kitManager = kitManager;
        requirePermission("essentials.kit.delete");  // Separate permission
        this.nameArg = withRequiredArg("name", "Kit name to delete", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        String kitName = context.get(nameArg);
        
        if (!kitManager.kitExists(kitName)) {
            Msg.send(context, "Kit not found: " + kitName);
            return;
        }

        kitManager.deleteKit(kitName);
        Msg.send(context, "Deleted kit: " + kitName);
    }
}
```

**Usage:**
- `/kit` - Opens GUI (base command)
- `/kit create StarterKit` - Creates new kit
- `/kit delete StarterKit` - Deletes kit

**Key Pattern:**
1. Parent command uses `addSubCommand()` in constructor
2. Each subcommand is a separate class extending AbstractPlayerCommand
3. Subcommands have their own permissions and arguments
4. Base command logic still runs for `/kit` alone

---

## Usage Variants

Usage variants are the correct way to implement optional arguments in Hytale's command system. They allow you to handle different argument patterns for the same logical command.

**Note:** This is the pattern you should use for implementing optional arguments, as `OptionalArg` does not work in Hytale. See the [Optional Arguments](#optional-arguments) section for more details.

### Usage Variant Pattern

**Example: HomeCommand with variants**

```java
public class HomeCommand extends AbstractPlayerCommand {
    private final HomeManager homeManager;

    public HomeCommand(@Nonnull HomeManager homeManager, @Nonnull TeleportManager teleportManager,
                      @Nonnull BackManager backManager) {
        super("home", "Teleport to your home");
        this.homeManager = homeManager;
        addAliases("homes");
        requirePermission("essentials.home");
        
        // Add usage variant for named homes
        addUsageVariant(new HomeNamedCommand(homeManager, teleportManager, backManager));
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World currentWorld) {
        // /home with no arguments
        List<Home> homes = homeManager.getHomes(playerRef.getUuid());
        
        if (homes.isEmpty()) {
            Msg.send(context, "You have no homes. Use /sethome <name> to create one.");
            return;
        }

        if (homes.size() == 1) {
            // Only one home - teleport to it
            Home home = homes.get(0);
            teleportManager.teleport(playerRef, home.getLocation());
            Msg.send(context, "Teleported to home: " + home.getName());
        } else {
            // Multiple homes - show list
            String homeList = String.join(", ", homes.stream()
                .map(Home::getName)
                .collect(Collectors.toList()));
            Msg.send(context, "Your homes: " + homeList);
            Msg.send(context, "Use /home <name> to teleport to a specific home");
        }
    }

    // Usage variant for /home <name>
    private static class HomeNamedCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> nameArg;
        private final HomeManager homeManager;

        HomeNamedCommand(@Nonnull HomeManager homeManager, @Nonnull TeleportManager teleportManager,
                        @Nonnull BackManager backManager) {
            super("Teleport to a specific home");
            this.homeManager = homeManager;
            this.nameArg = withRequiredArg("name", "Home name", ArgTypes.STRING);
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            // /home <name>
            String homeName = context.get(nameArg);
            
            Home home = homeManager.getHome(playerRef.getUuid(), homeName);
            if (home == null) {
                Msg.send(context, "Home not found: " + homeName);
                return;
            }

            teleportManager.teleport(playerRef, home.getLocation());
            Msg.send(context, "Teleported to home: " + home.getName());
        }
    }
}
```

**Usage:**
- `/home` - Lists homes or teleports to only home
- `/home mybase` - Teleports to specific home

**When to use variants vs subcommands:**
- **Variants**: Same logical action, different argument patterns (`/home`, `/home name`) - **Use this for optional arguments**
- **Subcommands**: Different actions under same command (`/kit`, `/kit create`, `/kit delete`)

**Key Points:**
1. Usage variants are how you implement optional arguments in Hytale
2. The base command handles the no-argument case
3. The variant command handles the with-argument case using `RequiredArg`
4. The command system automatically routes to the correct variant based on input

---

## Advanced Patterns

### Manual Argument Parsing (Multi-Word Input)

Some commands need to parse multi-word arguments (messages, descriptions).

**Example: MsgCommand**

```java
public class MsgCommand extends AbstractPlayerCommand {
    public MsgCommand(@Nonnull PrivateMessageManager messageManager) {
        super("msg", "Send a private message to a player");
        this.messageManager = messageManager;
        addAliases("m", "message", "whisper", "pm");
        requirePermission("essentials.msg");
        setAllowsExtraArguments(true);  // IMPORTANT: Allow extra args
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        String rawInput = context.getInputString();  // "/msg player hello world"
        String[] parts = rawInput.split("\\s+", 3);  // Split into max 3 parts
        
        // parts[0] = "/msg"
        // parts[1] = "player"
        // parts[2] = "hello world" (rest of message)
        
        if (parts.length < 3) {
            Msg.send(context, "Usage: /msg <player> <message>");
            return;
        }

        String targetName = parts[1];
        String message = parts[2];

        PlayerRef target = findPlayer(targetName);
        if (target == null) {
            Msg.send(context, "Player not found: " + targetName);
            return;
        }

        messageManager.sendMessage(playerRef, target, message);
        Msg.send(context, "To " + target.getUsername() + ": " + message);
        Msg.send(target, "From " + playerRef.getUsername() + ": " + message);
    }
}
```

**Key:**
- Use `setAllowsExtraArguments(true)` in constructor
- Get raw input with `context.getInputString()`
- Use `split("\\s+", limit)` to parse multi-word arguments

### Command Aliases

Commands can have multiple names:

```java
public class HealCommand extends AbstractPlayerCommand {
    public HealCommand() {
        super("heal", "Restore your health to full");
        addAliases("hp", "health", "fullheal");  // Can use /heal, /hp, /health, /fullheal
        requirePermission("essentials.heal");
    }
}
```

### Async Operations with CompletableFuture

For commands that need to do I/O (database, file operations):

```java
public class StatsCommand extends AbstractPlayerCommand {
    private final DatabaseService database;

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        Msg.send(context, "Loading your stats...");

        // Run database query asynchronously
        CompletableFuture.runAsync(() -> {
            // Heavy I/O on background thread
            PlayerStats stats = database.loadPlayerStats(playerRef.getUuid());
            
            // Return to world thread to send message
            world.execute(() -> {
                Msg.send(playerRef, "Kills: " + stats.getKills());
                Msg.send(playerRef, "Deaths: " + stats.getDeaths());
            });
        }).exceptionally(ex -> {
            world.execute(() -> {
                Msg.send(playerRef, "Failed to load stats: " + ex.getMessage());
            });
            return null;
        });
    }
}
```

**Important:**
- Use `CompletableFuture.runAsync()` for I/O operations
- Return to world thread with `world.execute()` for ECS access or player messages
- Handle exceptions with `.exceptionally()`

### Teleport with Delay

Many teleport commands have delays that can be bypassed:

```java
public class TpaCommand extends AbstractPlayerCommand {
    private static final String BYPASS_PERMISSION = "essentials.teleport.bypass";

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        PlayerRef target = context.get(targetArg);
        
        boolean bypassDelay = PermissionsModule.get()
            .hasPermission(playerRef.getUuid(), BYPASS_PERMISSION);
        
        int delaySeconds = configManager.getTeleportDelay();
        
        if (bypassDelay || delaySeconds == 0) {
            // Instant teleport
            teleportManager.teleport(playerRef, target.getLocation());
            Msg.send(context, "Teleported to " + target.getUsername());
        } else {
            // Delayed teleport
            Msg.send(context, "Teleporting in " + delaySeconds + " seconds. Don't move!");
            
            // Store initial position
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            Vector3d startPos = transform.getPosition();
            
            // Schedule teleport
            HytaleServer.get().getScheduledExecutor().schedule(() -> {
                world.execute(() -> {
                    // Check if player moved
                    TransformComponent currentTransform = store.getComponent(ref, TransformComponent.getComponentType());
                    Vector3d currentPos = currentTransform.getPosition();
                    
                    if (startPos.distanceSquared(currentPos) > 1.0) {
                        Msg.send(playerRef, "Teleport cancelled - you moved!");
                        return;
                    }
                    
                    // Execute teleport
                    teleportManager.teleport(playerRef, target.getLocation());
                    Msg.send(playerRef, "Teleported to " + target.getUsername());
                });
            }, delaySeconds, TimeUnit.SECONDS);
        }
    }
}
```

---

## Common Argument Types

Hytale provides several built-in argument types via `ArgTypes`:

| Type | Description | Example |
|------|-------------|---------|
| `ArgTypes.STRING` | Any text string | `/sethome mybase` |
| `ArgTypes.INTEGER` | Whole number | `/give 64` |
| `ArgTypes.DOUBLE` | Decimal number | `/speed 1.5` |
| `ArgTypes.BOOLEAN` | true/false | `/toggle true` |
| `ArgTypes.PLAYER_REF` | Online player name | `/tphere PlayerName` |
| `ArgTypes.WORLD` | World name | `/tp world_nether` |

**Usage:**

```java
// String argument
private final RequiredArg<String> nameArg = 
    withRequiredArg("name", "Home name", ArgTypes.STRING);

// Integer argument
private final RequiredArg<Integer> amountArg = 
    withRequiredArg("amount", "Item amount", ArgTypes.INTEGER);

// Player argument
private final RequiredArg<PlayerRef> playerArg = 
    withRequiredArg("player", "Target player", ArgTypes.PLAYER_REF);

// World argument
private final OptionalArg<World> worldArg = 
    withOptionalArg("world", "Target world", ArgTypes.WORLD);
```

---

## Best Practices

### 1. Always Validate Arguments

Even required arguments can have invalid values:

```java
@Override
protected void execute(...) {
    String homeName = context.get(nameArg);
    
    // Validate length
    if (homeName.length() > 32) {
        Msg.send(context, "Home name too long (max 32 characters)");
        return;
    }
    
    // Validate characters
    if (!homeName.matches("[a-zA-Z0-9_]+")) {
        Msg.send(context, "Home name can only contain letters, numbers, and underscores");
        return;
    }
    
    // Proceed with command
}
```

### 2. Use world.execute() for Cross-World Operations

When teleporting players between worlds or accessing another world's EntityStore:

```java
@Override
protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                       @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World currentWorld) {
    World targetWorld = Universe.get().getWorld("adventure");
    
    if (targetWorld == null) {
        Msg.send(context, "World not found");
        return;
    }

    // Capture data from current world (safe)
    Vector3d targetPos = new Vector3d(0, 100, 0);
    Vector3f targetRot = new Vector3f(0, 0, 0);

    // Execute on target world's thread
    targetWorld.execute(() -> {
        playerRef.teleport(targetWorld.getName(), targetPos, targetRot);
    });

    Msg.send(context, "Teleporting to " + targetWorld.getName());
}
```

### 3. Provide Clear Error Messages

Users should understand why a command failed:

```java
// Bad
if (home == null) {
    Msg.send(context, "Error");
    return;
}

// Good
if (home == null) {
    Msg.send(context, "Home '" + homeName + "' not found. Use /homes to list your homes.");
    return;
}
```

### 4. Use Appropriate Base Class

- Use `AbstractPlayerCommand` for player-only commands
- Use `AbstractCommand` for console-compatible commands

### 5. Handle Edge Cases

```java
@Override
protected void execute(...) {
    PlayerRef target = context.get(targetArg);
    
    // Check if player still exists
    if (target == null) {
        Msg.send(context, "Player not found");
        return;
    }
    
    // Check if targeting self
    if (target.getUuid().equals(playerRef.getUuid())) {
        Msg.send(context, "You cannot target yourself");
        return;
    }
    
    // Check if player is in a world
    World targetWorld = target.getWorld();
    if (targetWorld == null) {
        Msg.send(context, "Target player is not in a world");
        return;
    }
    
    // Proceed with command
}
```

### 6. Thread Safety for Shared Data

Use thread-safe types for data accessed from multiple worlds:

```java
// Thread-safe counter
private final AtomicInteger teleportCount = new AtomicInteger(0);

// Thread-safe map
private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

@Override
protected void execute(...) {
    // Safe to access from any world thread
    teleportCount.incrementAndGet();
    cooldowns.put(playerRef.getUuid(), System.currentTimeMillis());
}
```

### 7. Use Cooldowns Wisely

Implement cooldowns for commands that could be spammed:

```java
public class RtpCommand extends AbstractPlayerCommand {
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private static final String BYPASS_PERMISSION = "essentials.rtp.cooldown.bypass";

    @Override
    protected void execute(...) {
        UUID playerUuid = playerRef.getUuid();
        boolean canBypass = PermissionsModule.get().hasPermission(playerUuid, BYPASS_PERMISSION);
        
        if (!canBypass) {
            long lastUsed = cooldowns.getOrDefault(playerUuid, 0L);
            long now = System.currentTimeMillis();
            long cooldownMillis = 60_000;  // 60 seconds
            
            if (now - lastUsed < cooldownMillis) {
                long remaining = (cooldownMillis - (now - lastUsed)) / 1000;
                Msg.send(context, "RTP on cooldown. Wait " + remaining + " seconds.");
                return;
            }
        }

        // Execute random teleport
        // ...

        if (!canBypass) {
            cooldowns.put(playerUuid, System.currentTimeMillis());
        }
    }
}
```

---

## Summary

Hytale's command system provides:

1. **Type-safe arguments** - Required and optional with built-in validation
2. **Permission system** - Base permissions and runtime checks for bypasses
3. **Player targeting** - Type-safe `ArgTypes.PLAYER_REF` or manual lookup
4. **Subcommands** - Separate classes with own permissions and arguments
5. **Usage variants** - Handle different argument patterns for same action
6. **Thread safety** - Use `world.execute()` for ECS access
7. **Async operations** - `CompletableFuture` for I/O operations

**Command Structure:**
```java
public class ExampleCommand extends AbstractPlayerCommand {
    // 1. Define arguments
    private final RequiredArg<String> arg1;
    private final OptionalArg<Integer> arg2;
    
    public ExampleCommand() {
        super("example", "Example command");
        // 2. Initialize arguments
        this.arg1 = withRequiredArg("name", "Description", ArgTypes.STRING);
        this.arg2 = withOptionalArg("amount", "Description", ArgTypes.INTEGER);
        // 3. Set permission
        requirePermission("plugin.example");
        // 4. Add aliases
        addAliases("ex", "demo");
    }
    
    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        // 5. Get argument values
        String name = context.get(arg1);
        Integer amount = context.get(arg2);
        
        // 6. Validate
        if (name.isEmpty()) {
            Msg.send(context, "Name cannot be empty");
            return;
        }
        
        // 7. Execute logic
        // ...
        
        // 8. Send feedback
        Msg.send(context, "Command executed successfully");
    }
}
```

All commands follow this consistent, thread-safe pattern that integrates seamlessly with Hytale's ECS architecture.
