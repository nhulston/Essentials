Essentials is an all-in-one utility plugin for Hytale server administrators.

[Download here](https://www.curseforge.com/hytale/mods/essentials-core)

# Features

- Homes (multi-home support)
- Server warps
- Server spawn with protection
- TPA (teleport requests)
- Chat formatting (per-rank)
- Build protection (global or spawn-only)

![Homes](images/homes.png)
![Warps](images/warps.png)
![TPA](images/tpa.png)
![Chat Format](images/chatformat.png)

# Commands

| Command              | Description                     | Permission              |
|----------------------|---------------------------------|-------------------------|
| `/sethome <name>`    | Set a home                      | `essentials.sethome`    |
| `/home <name>`       | Teleport to your home           | `essentials.home`       |
| `/delhome <name>`    | Delete a home                   | `essentials.delhome`    |
| `/setwarp <name>`    | Set a server warp               | `essentials.setwarp`    |
| `/warp <name>`       | Teleport to a warp              | `essentials.warp`       |
| `/delwarp <name>`    | Delete a warp                   | `essentials.delwarp`    |
| `/setspawn`          | Set server spawn                | `essentials.setspawn`   |
| `/spawn`             | Teleport to spawn               | `essentials.spawn`      |
| `/tpa <player>`      | Request to teleport to a player | `essentials.tpa`        |
| `/tpaccept <player>` | Accept a teleport request       | `essentials.tpaccept`   |

# Permissions

| Permission                | Description                            |
|---------------------------|----------------------------------------|
| `essentials.sethome`      | Set homes                              |
| `essentials.home`         | Teleport to homes                      |
| `essentials.delhome`      | Delete homes                           |
| `essentials.setwarp`      | Create warps                           |
| `essentials.warp`         | Teleport to warps                      |
| `essentials.delwarp`      | Delete warps                           |
| `essentials.setspawn`     | Set server spawn                       |
| `essentials.spawn`        | Teleport to spawn                      |
| `essentials.tpa`          | Send teleport requests                 |
| `essentials.tpaccept`     | Accept teleport requests               |
| `essentials.build.bypass` | Build when global building is disabled |
| `essentials.spawn.bypass` | Build in spawn protection area         |

To add permissions to default users, edit your `permissions.json`:

```json
"groups": {
  "Default": [],
  "OP": ["*"],
  "Mod": [
    "essentials.setwarp",
    "essentials.delwarp"
  ]
}
```

# Configuration

Configuration is stored in `config.toml`. Key options:

**Homes**
- `homes.max-homes` - Max homes per player (default: 5)

**Chat**
- `chat.enabled` - Enable custom chat formatting
- `chat.fallback-format` - Format for players without a rank
- `chat.formats.<group>` - Format per permission group
- Placeholders: `%player%`, `%message%`  
- Color codes: `&0-&9`, `&a-&f`, `&#RRGGBB`

**Build Protection**
- `build.disable-building` - Disable building globally (bypass: `essentials.build.bypass`)

**Spawn**
- `spawn.first-join` - Teleport to spawn on first join (default: true)
- `spawn.every-join` - Teleport to spawn on every join (default: false)
- `spawn.death-spawn` - Teleport to spawn on death (default: true)

**Spawn Protection**
- `spawn-protection.enabled` - Enable spawn area protection
- `spawn-protection.radius` - Protection radius in blocks (default: 16)
- `spawn-protection.min-y` / `max-y` - Y range limit (-1 to disable)
- `spawn-protection.prevent-pvp` - Disable PvP in spawn
- `spawn-protection.show-titles` - Show enter/exit titles
- `spawn-protection.enter-title` / `enter-subtitle` - Title on enter
- `spawn-protection.exit-title` / `exit-subtitle` - Title on exit

# Community & Support

Join our Discord for support, bugs, and suggestions:  
https://discord.gg/z53BDHS89M

---

Note: Essentials is inspired by but not affiliated with the EssentialsX Minecraft plugin.
