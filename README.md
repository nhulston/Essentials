Essentials is an all-in-one utility plugin for Hytale server administrators.

[Download here](https://www.curseforge.com/hytale/mods/essentials-core)

# Features

- Homes (multi-home support)
- Server warps
- Server spawn with protection
- TPA (teleport requests)
- Kits (with cooldowns and GUI)
- Chat formatting (per-rank)
- Build protection (global or spawn-only)

![Homes](https://raw.githubusercontent.com/nhulston/Essentials/refs/heads/main/images/homes.png) ![TPA](https://raw.githubusercontent.com/nhulston/Essentials/refs/heads/main/images/tpa.png) ![Warps](https://raw.githubusercontent.com/nhulston/Essentials/refs/heads/main/images/warps.png)

![Chat Format](https://raw.githubusercontent.com/nhulston/Essentials/refs/heads/main/images/chatformat.png) ![Chat Format2](https://raw.githubusercontent.com/nhulston/Essentials/refs/heads/main/images/chatformat2.png)

![Kits](https://raw.githubusercontent.com/nhulston/Essentials/refs/heads/main/images/kits.png)

# Commands

| Command                  | Description                      | Permission                         |
|--------------------------|----------------------------------|------------------------------------|
| <code>/sethome</code>    | Set a home                       | <code>essentials.sethome</code>    |
| <code>/home</code>       | Teleport to your home            | <code>essentials.home</code>       |
| <code>/delhome</code>    | Delete a home                    | <code>essentials.delhome</code>    |
| <code>/setwarp</code>    | Set a server warp                | <code>essentials.setwarp</code>    |
| <code>/warp</code>       | Teleport to a warp               | <code>essentials.warp</code>       |
| <code>/delwarp</code>    | Delete a warp                    | <code>essentials.delwarp</code>    |
| <code>/setspawn</code>   | Set server spawn                 | <code>essentials.setspawn</code>   |
| <code>/spawn</code>      | Teleport to spawn                | <code>essentials.spawn</code>      |
| <code>/tpa</code>        | Request to teleport to a player  | <code>essentials.tpa</code>        |
| <code>/tpaccept</code>   | Accept a teleport request        | <code>essentials.tpaccept</code>   |
| <code>/kit</code>        | Open kit selection GUI           | <code>essentials.kit</code>        |
| <code>/kit create</code> | Create a kit from your inventory | <code>essentials.kit.create</code> |
| <code>/kit delete</code> | Delete a kit                     | <code>essentials.kit.delete</code> |
| <code>/back</code>       | Teleport to your last death      | <code>essentials.back</code>       |

# Permissions

| Permission                                  | Description                            |
|---------------------------------------------|----------------------------------------|
| <code>essentials.sethome</code>             | Set homes                              |
| <code>essentials.home</code>                | Teleport to homes                      |
| <code>essentials.delhome</code>             | Delete homes                           |
| <code>essentials.setwarp</code>             | Create warps                           |
| <code>essentials.warp</code>                | Teleport to warps                      |
| <code>essentials.delwarp</code>             | Delete warps                           |
| <code>essentials.setspawn</code>            | Set server spawn                       |
| <code>essentials.spawn</code>               | Teleport to spawn                      |
| <code>essentials.tpa</code>                 | Send teleport requests                 |
| <code>essentials.tpaccept</code>            | Accept teleport requests               |
| <code>essentials.build.bypass</code>        | Build when global building is disabled |
| <code>essentials.spawn.bypass</code>        | Build in spawn protection area         |
| <code>essentials.kit</code>                 | Open kit selection GUI                 |
| <code>essentials.kit.\<kitname\></code>     | Access to claim a specific kit         |
| <code>essentials.kit.create</code>          | Create new kits                        |
| <code>essentials.kit.delete</code>          | Delete kits                            |
| <code>essentials.kit.cooldown.bypass</code> | Bypass kit cooldowns                   |
| <code>essentials.teleport.bypass</code>     | Bypass teleport delay                  |
| <code>essentials.back</code>                | Teleport to last death location        |

To setup permissions, please view these unofficial docs: [https://hytale-docs.pages.dev/modding/plugins/permissions/](https://hytale-docs.pages.dev/modding/plugins/permissions/)

# Configuration

Configuration is stored in `config.toml`. Key options:

**Homes**

*   `homes.max-homes` - Max homes per player (default: 5)

**Chat**

*   `chat.enabled` - Enable custom chat formatting
*   `chat.fallback-format` - Format for players without a rank
*   `chat.formats.<group>` - Format per permission group
*   Placeholders: `%player%`, `%message%`
*   Color codes: `&0-&9`, `&a-&f`, `&#RRGGBB`

**Build Protection**

*   `build.disable-building` - Disable building globally (bypass: <code>essentials.build.bypass</code>)

**Teleport**

*   `teleport.delay` - Delay in seconds before teleporting (default: 3, set to 0 to disable)

Players must stand still during the delay or the teleport is canceled. Players with `essentials.teleport.bypass` skip the delay.

**Spawn**

*   `spawn.first-join` - Teleport to spawn on first join (default: true)
*   `spawn.every-join` - Teleport to spawn on every join (default: false)
*   `spawn.death-spawn` - Teleport to spawn on death (default: true)

**Spawn Protection**

*   `spawn-protection.enabled` - Enable spawn area protection
*   `spawn-protection.radius` - Protection radius in blocks (default: 16)
*   `spawn-protection.min-y` / `max-y` - Y range limit (-1 to disable)
*   `spawn-protection.prevent-pvp` - Disable PvP in spawn
*   `spawn-protection.show-titles` - Show enter/exit titles
*   `spawn-protection.enter-title` / `enter-subtitle` - Title on enter
*   `spawn-protection.exit-title` / `exit-subtitle` - Title on exit

**Kits**

Kits are configured in `kits.toml`. Create kits in-game with `/kit create <name>` or edit the file directly.

*   `display-name` - Name shown in the kit GUI
*   `cooldown` - Cooldown in seconds (0 = no cooldown)
*   `type` - `"add"` to add items to inventory, `"replace"` to clear inventory first

Each kit requires `essentials.kit.<kitname>` permission to claim. Items that don't fit in the intended slot (e.g., armor when already wearing armor) will go to the player's inventory, and only drop on the ground if the inventory is full.

# Community & Support

Join our Discord for support, bugs, and suggestions:  
[https://discord.gg/z53BDHS89M](https://discord.gg/z53BDHS89M)

***

Note: Essentials is inspired by but not affiliated with the EssentialsX Minecraft plugin.