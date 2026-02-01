# Hytale's Entity Component System (ECS)

## What is ECS?

Entity Component System (ECS) is a different paradigm from traditional Object-Oriented Programming (OOP). Instead of using inheritance hierarchies, ECS uses **composition** to build game entities from reusable parts.

### The Problem with OOP for Game Entities

**Traditional OOP approach:**
```
GameObject
├── Movable
│   ├── Character
│   │   ├── Player
│   │   └── NPC
│   └── Projectile
└── Static
    └── Building
```

**Problems:**
1. **Rigid hierarchies** - A class can only have one parent
2. **Diamond problem** - Multiple inheritance conflicts
3. **Class explosion** - Every feature combination needs its own class
4. **God classes** - Base classes become bloated
5. **Tight coupling** - Changes break dependent classes

**Example problem:** Where does a "vehicle" go? It's like a building (has interior, players can enter) but also moves (needs physics). Can't inherit from both Static and Movable!

### The ECS Solution

Instead of asking **"What IS this?"**, ask **"What does this HAVE?"**

**Three Pillars of ECS:**

1. **Entity** - Just an ID (like entity 42, entity 99, entity 337)
2. **Component** - Pure data, no behavior (Position, Health, Velocity)
3. **System** - Pure logic that processes entities with matching components

**Example: Flying Pig**
- OOP: Need `FlyingMountableFireBreathingCharacter` class
- ECS: Just attach components: Position, Flying, FireBreath, Mountable

## Hytale's ECS Hierarchy

```
Server
└── Universe (singleton)
    ├── PlayerRef registry (ConcurrentHashMap - thread-safe)
    ├── PlayerStorage (saves data to disk)
    └── Worlds (multiple)
        └── EntityStore (the ECS heart)
            ├── All entities in this world
            └── ChunkStore (blocks and terrain)
```

### Key Classes

#### Universe
- **Singleton** - only one exists per server
- Access with `Universe.get()`
- Contains all worlds and connected players
- Thread-safe player lookups

```java
Universe universe = Universe.get();
List<PlayerRef> players = universe.getPlayers();
World world = universe.getWorld("worldName");
```

#### World
- One game instance
- Contains EntityStore, ChunkStore
- List of players currently in this world
- World-specific settings (time, weather, TPS)

```java
World world = universe.getWorld("adventure");
EntityStore store = world.getEntityStore();
List<PlayerRef> playersInWorld = world.getPlayers();
```

#### PlayerRef
- **Bridge** between network connection and ECS entity
- Holds UUID, username, network handler
- Links to ECS entity when player joins a world
- Persists even when player leaves world

```java
PlayerRef playerRef = Universe.get().getPlayer(uuid);
String username = playerRef.getUsername();
UUID playerId = playerRef.getUuid();
```

## Core ECS Types

### Ref<EntityStore>
A **pointer** to one specific entity. Think of it as a house address.

```java
Ref<EntityStore> ref = event.getPlayerRef();

// Check if entity still exists
if (ref.isValid()) {
    // Process entity
}
```

**Note:** In most cases (like event handlers), you don't need to check `isValid()`.

### Store (EntityStore)
The **container** where all entities in a world live. This is where the ECS magic happens.

```java
Store<EntityStore> store = ref.getStore();

// Get component from entity
PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

// Add component to entity
store.addComponent(ref, new PoisonedComponent());

// Remove component from entity
store.removeComponent(ref, PoisonedComponent.getComponentType());

// Check if entity has component
boolean hasPoi = store.hasComponent(ref, PoisonedComponent.getComponentType());
```

### ComponentType
A unique identifier for each component class. Used for fast lookup (integer index instead of class name).

```java
ComponentType<EntityStore, Health> healthType = Health.getComponentType();
Health health = store.getComponent(ref, healthType);
```

## The Core ECS Read Pattern

This is the fundamental pattern you'll use constantly:

```java
// 1. Get the ref (pointer to entity)
Ref<EntityStore> ref = event.getPlayerRef();

// 2. Get the store (entity container)
Store<EntityStore> store = ref.getStore();

// 3. Get the component
TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());

// 4. Read the data
Vector3d position = transform.getPosition();
double x = position.getX();
double y = position.getY();
double z = position.getZ();
```

## Writing Components

### Add Component
```java
TeleportComponent teleport = new TeleportComponent(worldName, x, y, z, yaw, pitch);
store.addComponent(ref, teleport);
```

### Modify Component
```java
Health health = store.getComponent(ref, Health.getComponentType());
health.setCurrentHealth(health.getCurrentHealth() - 10);
```

### Remove Component
```java
store.removeComponent(ref, PoisonedComponent.getComponentType());
```

## Component as Trigger Pattern

Components can be used as **requests** that systems fulfill:

```java
// Add teleport component
TeleportComponent teleport = new TeleportComponent(worldName, x, y, z, yaw, pitch);
store.addComponent(ref, teleport);

// TeleportSystem runs every tick:
// 1. Finds entities with TeleportComponent
// 2. Moves them
// 3. Removes the component
```

This is very common in ECS - adding a component triggers behavior.

## Systems

Systems are where your logic runs. Different system types for different timing needs:

### EntityTickingSystem
Runs every frame for entities matching a query.

```java
public class PoisonDamageSystem extends EntityTickingSystem<EntityStore> {
    @Override
    public Query<EntityStore> getQuery() {
        // Only process entities with BOTH Health AND Poisoned components
        return Query.and(
            Query.has(Health.getComponentType()),
            Query.has(PoisonedComponent.getComponentType())
        );
    }
    
    @Override
    public void tick(Ref<EntityStore> ref, Store<EntityStore> store, 
                     CommandBuffer<EntityStore> buffer, float dt) {
        // dt = delta time (time since last frame in seconds)
        Health health = store.getComponent(ref, Health.getComponentType());
        PoisonedComponent poison = store.getComponent(ref, PoisonedComponent.getComponentType());
        
        // Deal 10 damage per second (frame rate independent)
        float damageThisTick = 10.0f * dt;
        health.setCurrentHealth(health.getCurrentHealth() - damageThisTick);
    }
}
```

**Delta Time (dt):** Time since last frame in seconds. At 60 FPS, dt ≈ 0.016s. Use this to make logic frame-rate independent.

### RefChangeSystem
Runs when entities are added/removed or when specific components change.

```java
public class WelcomeSystem extends RefChangeSystem<EntityStore, PlayerRef> {
    @Override
    public ComponentType<EntityStore, PlayerRef> componentType() {
        return PlayerRef.getComponentType();
    }
    
    @Override
    public Query<EntityStore> getQuery() {
        return Query.has(PlayerRef.getComponentType());
    }
    
    @Override
    public void onComponentAdded(Ref<EntityStore> ref, PlayerRef playerRef,
                                  Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        // Player joined
        playerRef.sendMessage(Message.raw("Welcome!"));
    }
    
    @Override
    public void onComponentRemoved(Ref<EntityStore> ref, PlayerRef playerRef,
                                    Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        // Player left
        Log.info(playerRef.getUsername() + " left the world");
    }
}
```

**Use cases:** Welcome messages, initialization, cleanup, tracking joins/leaves

### DamageEventSystem
Intercepts and modifies damage before it's applied.

```java
public class ArmorSystem extends DamageEventSystem<EntityStore> {
    @Override
    public float onDamage(DamageEvent event, Ref<EntityStore> ref, 
                          Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        ArmorComponent armor = store.getComponent(ref, ArmorComponent.getComponentType());
        if (armor != null) {
            float reduction = armor.getArmorValue() * 0.5f;
            return event.getDamageAmount() - reduction;
        }
        return event.getDamageAmount();
    }
}
```

**Use cases:** Armor, damage modifiers, invulnerability, damage logging

## Queries

Queries are **filters** that tell systems which entities to process. Without queries, you'd have to manually check every entity.

### Query Patterns

```java
// Entities with BOTH Health AND Poisoned
Query.and(
    Query.has(Health.getComponentType()),
    Query.has(PoisonedComponent.getComponentType())
)

// Entities that are EITHER Players OR NPCs
Query.or(
    Query.has(PlayerRef.getComponentType()),
    Query.has(NPCComponent.getComponentType())
)

// Entities with Health but NOT Invulnerable
Query.and(
    Query.has(Health.getComponentType()),
    Query.not(Query.has(InvulnerableComponent.getComponentType()))
)
```

## Archetypes (Performance Secret)

Every entity has an **archetype** - a fingerprint of which component types it has.

**How it works:**
- Entities with the same components are grouped together
- Example: All players (archetype: Position + Health + PlayerRef)
- Example: All NPCs (archetype: Position + Health + AI)
- Example: All projectiles (archetype: Position + Velocity + Damage)

**Why it's fast:**
1. If your query needs PlayerRef, Hytale skips archetype groups that don't have it
2. Entities with same archetype are stored together in memory (cache-friendly)
3. CPU can prefetch data efficiently

This is called **Structure of Arrays (SoA)** and it's why Hytale's ECS is so performant.

## Important Components

### EntityStatMap
Flexible stat system for health, mana, stamina, hunger, etc.

```java
EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
EntityStat health = stats.getStat(DefaultStatType.HEALTH);

float currentHealth = health.getCurrentValue();
float maxHealth = health.getMaxValue();

health.setCurrentValue(maxHealth); // Full heal
```

**Why not a simple Health component?**
- Supports regeneration, modifiers, buffs/debuffs
- Easy to add custom stats
- Consistent API for all stat types

### TransformComponent
Position and rotation in the world.

```java
TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
Vector3d position = transform.getPosition();
Vector3f rotation = transform.getRotation(); // Body direction (where legs/body face)

// For head direction (where camera looks):
HeadRotationComponent headRot = store.getComponent(ref, HeadRotationComponent.getComponentType());
```

**Important:** For teleports, set both Transform (body) and HeadRotation (camera).

## Interactions (Not ECS!)

Interactions handle **timed actions** with animations (swing, charge, cooldowns). They eventually trigger ECS changes, but the timing isn't stored as components.

**Flow:**
1. Player clicks (e.g., attack button)
2. Interaction starts (swing animation begins)
3. Interaction ticks each frame (animation progresses)
4. At hit point: Interaction fires damage event
5. ECS system processes damage
6. Health component gets updated

**Common interaction types:**
- PlaceBlockInteraction
- DamageEntityInteraction
- ProjectileInteraction
- UseEntityInteraction

**Key point:** Interactions describe *what should happen*, but actual ECS changes happen when the interaction executes at the right moment.

## Mental Model Shift

### OOP Thinking
- "What IS this?" (inheritance hierarchies)
- Player → Character → Movable → GameObject

### ECS Thinking
- "What does this HAVE?" (composition)
- Player has: Position, Health, PlayerRef, Input

### Adding Features
- OOP: Create new class, extend hierarchy
- ECS: Attach component

## Quick Reference

| Task | Pattern |
|------|---------|
| Access universe | `Universe.get()` |
| Get world | `universe.getWorld("name")` |
| Get entity store | `world.getEntityStore()` |
| Get player | `Universe.get().getPlayer(uuid)` |
| Read component | `store.getComponent(ref, ComponentType)` |
| Add component | `store.addComponent(ref, component)` |
| Remove component | `store.removeComponent(ref, ComponentType)` |
| Check has component | `store.hasComponent(ref, ComponentType)` |
| Query filter | `Query.and()`, `Query.or()`, `Query.not()` |
| Run every tick | Extend `EntityTickingSystem` |
| Run on join/leave | Extend `RefChangeSystem` |
| Intercept damage | Extend `DamageEventSystem` |

## System Types Summary

| System Type | When It Runs | Use For |
|-------------|--------------|---------|
| EntityTickingSystem | Every frame | Continuous logic (poison damage, regen) |
| RefChangeSystem | Entity added/removed or component changed | Lifecycle events (welcome, cleanup) |
| DamageEventSystem | Before damage applied | Armor, damage modifiers |

## Key Differences: Systems vs Events vs Interactions

| Type | Purpose | Example |
|------|---------|---------|
| **System** | ECS processor, runs every tick | Auto-heal system heals over time |
| **Event** | Notification broadcast | Player chat event when someone types |
| **Interaction** | Timed action sequence | Sword swing with animation |

## ECS Benefits

1. **Flexible** - Mix and match any combination of features
2. **Reusable** - Health component works for players, NPCs, barrels, anything
3. **Decoupled** - Systems don't know about each other
4. **Performant** - Data laid out for fast iteration (archetypes)
5. **No hierarchies** - No diamond problem, no deep nesting

## Composition Over Inheritance

**Think in capabilities, not categories:**
- Can move? → Has Position + Velocity
- Can take damage? → Has Health
- Can think? → Has AI
- Flying pig that breathes fire? → Position + Flying + FireBreath + Mountable

Build entities from parts, not family trees!
