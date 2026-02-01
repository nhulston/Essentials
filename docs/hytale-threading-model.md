# Hytale's Multi-Threaded Architecture

## Core Concepts

### Threading Model
Hytale uses a **multi-world threading architecture** where each world runs on its own dedicated thread, unlike single-threaded servers like Minecraft where everything runs sequentially on one thread.

**Key differences:**
- **Single-threaded (Minecraft)**: One thread processes all worlds sequentially. If World A lags, all worlds lag.
- **Multi-threaded (Hytale)**: Each world has its own thread running in parallel. If World A lags, World B and C are unaffected.

### Server Hierarchy
```
HytaleServer (singleton)
├── ScheduledExecutor (for background tasks)
└── Universe (singleton)
    ├── ConcurrentHashMap<UUID, PlayerRef> (thread-safe player lookup)
    └── Worlds (each has own thread)
        ├── EntityStore (thread-bound)
        ├── ChunkStore (thread-bound)
        └── Configurable TPS per world
```

### Thread Binding
- **EntityStore** in each world can ONLY be accessed from that world's thread
- Attempting to access from wrong thread throws `IllegalStateException` immediately
- This is intentional - crashes are better than silent data corruption

## Thread Safety Rules

### Always Safe (Can call from any thread)
- `Universe.get().getPlayer(uuid)` - uses concurrent hashmap internally
- `PlayerRef.sendMessage()` - immutable data, network operations queue properly
- `PlayerRef.getUsername()` - immutable once set
- `HytaleServer.get().getScheduledExecutor()` - threading tool itself
- Network operations - packets queue up properly

### World Thread ONLY (Will crash otherwise)
- `Store.getComponent()` - reads entity data
- `Store.addComponent()` - modifies entity
- `Store.removeComponent()` - changes entity structure
- **Almost all ECS operations** (systems, queries, command buffers, everything in EntityStore)

### The Bridge: `world.execute()`
Use `world.execute(() -> { ... })` to bridge from any thread to a world thread.

**Example:**
```java
// WRONG - crashes with IllegalStateException
HytaleServer.get().getScheduledExecutor().scheduleAtFixedRate(() -> {
    store.getComponent(ref, SomeComponent.class); // Wrong thread!
}, 0, 5, TimeUnit.SECONDS);

// CORRECT - use world.execute()
HytaleServer.get().getScheduledExecutor().scheduleAtFixedRate(() -> {
    world.execute(() -> {
        store.getComponent(ref, SomeComponent.class); // Safe on world thread
    });
}, 0, 5, TimeUnit.SECONDS);
```

## Race Conditions

### The Problem
When multiple threads share data without synchronization:

```java
// DANGER: Race condition
private int globalKills = 0;

public void onEntityKilled() {
    globalKills++; // Actually 3 operations: read, increment, write
}
```

If two worlds call this simultaneously:
1. Thread A reads 0
2. Thread B reads 0
3. Thread A writes 1
4. Thread B writes 1
Result: 1 (expected: 2) - **One kill was lost!**

### The Solution: Thread-Safe Types

#### Atomic Types
Use for counters, flags, and simple values:

```java
private final AtomicInteger globalKills = new AtomicInteger(0);
private final AtomicBoolean trackingEnabled = new AtomicBoolean(true);

public void onEntityKilled() {
    int newValue = globalKills.incrementAndGet(); // Atomic operation
}
```

#### ConcurrentHashMap
Use for shared maps:

```java
private final Map<UUID, Integer> killsPerPlayer = new ConcurrentHashMap<>();

public void onEntityKilled(UUID playerUuid) {
    killsPerPlayer.merge(playerUuid, 1, Integer::sum); // Atomic increment
}
```

#### Volatile
Use for simple flags that one thread writes and others read:

```java
private volatile boolean trackingEnabled = true;

// All threads see the latest value immediately
```

## Common Patterns

### 1. ECS System (Thread-Safe)
```java
public class KillTrackerSystem extends RefChangeSystem<EntityStore, DeathComponent> {
    private final AtomicInteger globalKills = new AtomicInteger(0);
    private final Map<UUID, Integer> killsPerPlayer = new ConcurrentHashMap<>();
    
    @Override
    public void onComponentAdded(Ref<EntityStore> ref, DeathComponent component,
                                  Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        // Already on world thread - safe to access store
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return;
        
        // Atomic operations are thread-safe across all worlds
        globalKills.incrementAndGet();
        killsPerPlayer.merge(playerRef.getUuid(), 1, Integer::sum);
    }
}
```

### 2. Scheduled Tasks
```java
// Runs every 5 minutes on background thread
HytaleServer.get().getScheduledExecutor().scheduleAtFixedRate(() -> {
    // Heavy I/O on background thread - good!
    database.saveKills(globalKills.get());
}, 0, 5, TimeUnit.MINUTES);
```

### 3. Async Database Operations
```java
CompletableFuture.runAsync(() -> {
    // Runs on background thread pool
    database.saveKills(globalKills.get());
}).exceptionally(ex -> {
    Log.error("Failed to save kills", ex);
    return null;
});
```

### 4. Cross-World Teleport
```java
// CORRECT: Capture data first, then execute on target
World targetWorld = getTargetWorld();
Vector3d targetPos = new Vector3d(x, y, z);
Vector3f targetRot = new Vector3f(yaw, pitch, 0);

// Execute on player's current world thread
targetWorld.execute(() -> {
    playerRef.teleport(targetWorld.getName(), targetPos, targetRot);
});
```

### 5. Universe-Wide Broadcast
```java
// Always safe - uses concurrent hashmap internally
Universe.get().sendMessage(Message.raw("Server shutting down in 5 minutes"));
```

## Common Mistakes

### ❌ Mistake 1: Accessing Store from Wrong Thread
```java
// WRONG
HytaleServer.get().getScheduledExecutor().schedule(() -> {
    store.getComponent(ref, PlayerRef.class); // Crashes!
}, 1, TimeUnit.SECONDS);

// CORRECT
HytaleServer.get().getScheduledExecutor().schedule(() -> {
    world.execute(() -> {
        store.getComponent(ref, PlayerRef.class); // Safe
    });
}, 1, TimeUnit.SECONDS);
```

### ❌ Mistake 2: Blocking World Thread
```java
// WRONG - blocks entire tick
world.execute(() -> {
    CompletableFuture<Data> future = fetchDataAsync();
    Data data = future.join(); // DON'T BLOCK!
});

// CORRECT - use callbacks
fetchDataAsync().thenAccept(data -> {
    world.execute(() -> {
        // Process data on world thread
    });
});
```

### ❌ Mistake 3: Shared Mutable State
```java
// WRONG - race condition
private int counter = 0;

// CORRECT - thread-safe
private final AtomicInteger counter = new AtomicInteger(0);
```

### ❌ Mistake 4: Deadlock
```java
// WRONG - potential deadlock
worldA.execute(() -> {
    worldB.execute(() -> {
        // If worldB is waiting for worldA...deadlock!
    }).join(); // DON'T WAIT!
});

// CORRECT - use callbacks
worldA.execute(() -> {
    // Capture data from worldA
    Data data = prepareData();
    
    // Queue work on worldB (non-blocking)
    worldB.execute(() -> {
        processData(data);
    });
});
```

## Quick Reference

| Task | Thread-Safe Approach |
|------|---------------------|
| Counter across worlds | `AtomicInteger` |
| One-time initialization | `AtomicBoolean.compareAndSet()` |
| Shared map | `ConcurrentHashMap` |
| Simple flag | `volatile boolean` |
| ECS access | Use `world.execute()` |
| Heavy I/O | `CompletableFuture.runAsync()` |
| Scheduled tasks | `ScheduledExecutor` |
| Cross-world teleport | Capture data, then `world.execute()` |
| Broadcast message | `Universe.get().sendMessage()` |

## Three Golden Rules

1. **If you need ECS access → Use world thread** (`world.execute()`)
2. **If you have shared plugin data → Use thread-safe types** (`AtomicInteger`, `ConcurrentHashMap`, `volatile`)
3. **Never block waiting for another world → Use async patterns** (callbacks, `CompletableFuture`)

## Debugging

### Check Current Thread
```java
String threadName = Thread.currentThread().getName();
// World threads are named: "WorldThread - worldName"
```

### IllegalStateException
If you get `IllegalStateException: Assert not in thread!`:
- You're accessing EntityStore from wrong thread
- Wrap your code in `world.execute()`
- The crash is your friend - it tells you exactly what to fix

## TPS (Ticks Per Second)

- Minecraft: 20 TPS (50ms per tick)
- Hytale default: 30 TPS (33ms per tick)
- Per-world TPS configuration possible
- If tick takes longer than allocated time, server lags

Each tick:
1. Process input
2. Update game state
3. Send results to players
