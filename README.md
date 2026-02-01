Essentials is an all-in-one utility plugin for Hytale server administrators.

[Download here](https://www.curseforge.com/hytale/mods/essentials-core)

# Features

*   All messages customizable!
*   Homes (multi-home support)
*   Server warps
*   Server spawn with protection
*   TPA (teleport requests)
*   Kits (with cooldowns and GUI)
*   Chat formatting (per-rank)
*   Build protection (global or spawn-only)
*   Random teleport
*   /back on death
*   Sleep percentage
*   Private messaging
*   Custom join/leave messages (removes ugly default join/leave messages)
*   Starter kits for new players 
*   Other useful commands: /list, /heal, /freecam, /god, /tphere, /top

![Homes](https://raw.githubusercontent.com/nhulston/Essentials/refs/heads/main/images/homes.png) ![TPA](https://raw.githubusercontent.com/nhulston/Essentials/refs/heads/main/images/tpa.png) ![Warps](https://raw.githubusercontent.com/nhulston/Essentials/refs/heads/main/images/warps.png)

![Chat Format](https://raw.githubusercontent.com/nhulston/Essentials/refs/heads/main/images/chatformat.png) ![Chat Format2](https://raw.githubusercontent.com/nhulston/Essentials/refs/heads/main/images/chatformat2.png)

![Kits](https://raw.githubusercontent.com/nhulston/Essentials/refs/heads/main/images/kits.png)

# Commands

| Command                          | Description                       | Permission                           |
|----------------------------------|-----------------------------------|--------------------------------------|
| <code>/sethome</code>            | Set a home                        | <code>essentials.sethome</code>      |
| <code>/home</code>               | Teleport to your home             | <code>essentials.home</code>         |
| <code>/home PLAYER:</code>       | List another player's homes       | <code>essentials.home.others</code>  |
| <code>/home PLAYER:HOME</code>   | Teleport to another player's home | <code>essentials.home.others</code>  |
| <code>/delhome</code>            | Delete a home                     | <code>essentials.delhome</code>      |
| <code>/setwarp</code>            | Set a server warp                 | <code>essentials.setwarp</code>      |
| <code>/warp</code>               | Teleport to a warp                | <code>essentials.warp</code>         |
| <code>/warp NAME PLAYER</code>   | Teleport another player to a warp | <code>essentials.warp.others</code>  |
| <code>/delwarp</code>            | Delete a warp                     | <code>essentials.delwarp</code>      |
| <code>/setspawn</code>           | Set server spawn                  | <code>essentials.setspawn</code>     |
| <code>/spawn</code>              | Teleport to spawn                 | <code>essentials.spawn</code>        |
| <code>/spawn PLAYER</code>       | Teleport another player to spawn  | <code>essentials.spawn.others</code> |
| <code>/tpa</code>                | Request to teleport to a player   | <code>essentials.tpa</code>          |
| <code>/tpaccept</code>           | Accept a teleport request         | <code>essentials.tpaccept</code>     |
| <code>/kit</code>                | Open kit selection GUI            | <code>essentials.kit</code>          |
| <code>/kit create</code>         | Create a kit from your inventory  | <code>essentials.kit.create</code>   |
| <code>/kit delete</code>         | Delete a kit                      | <code>essentials.kit.delete</code>   |
| <code>/kit KITNAME PLAYER</code> | Give another player a kit         | <code>essentials.kit.other</code>    |
| <code>/back</code>               | Teleport to your last death       | <code>essentials.back</code>         |
| <code>/rtp</code>                | Random teleport                   | <code>essentials.rtp</code>          |
| <code>/list</code>               | List online players               | <code>essentials.list</code>         |
| <code>/heal</code>               | Restore your health to full       | <code>essentials.heal</code>         |
| <code>/freecam</code>            | Toggle freecam mode               | <code>essentials.freecam</code>      |
| <code>/god</code>                | Toggle god mode (invincibility)   | <code>essentials.god</code>          |
| <code>/msg</code>                | Send a private message            | <code>essentials.msg</code>          |
| <code>/r</code>                  | Reply to last message             | <code>essentials.msg</code>          |
| <code>/tphere</code>             | Teleport a player to you          | <code>essentials.tphere</code>       |
| <code>/top</code>                | Teleport to highest block         | <code>essentials.top</code>          |
| <code>/essentials reload</code>  | Reload configuration              | <code>essentials.reload</code>       |
| <code>/shout</code>              | Broadcast message to all players  | <code>essentials.shout</code>        |
| <code>/repair</code>             | Repair the item in your hand      | <code>essentials.repair</code>       |
| <code>/rules</code>              | Display server rules              | None                                 |
| <code>/trash</code>              | Throw away some items             | <code>essentials.trash</code>        |

# Permissions

If you don't have a permissions mod, you can add a permission to all players with the command:
`/perm group add Adventure essentials.kit`

If you have a permissions mod, follow the instructions for that mod.

**NOTE**: Permissions are case sensitive!

| Permission                                     | Description                                                                                 |
|------------------------------------------------|---------------------------------------------------------------------------------------------|
| <code>essentials.sethome</code>                | Set homes                                                                                   |
| <code>essentials.home</code>                   | Teleport to homes                                                                           |
| <code>essentials.home.others</code>            | View and teleport to other players' homes                                                   |
| <code>essentials.delhome</code>                | Delete homes                                                                                |
| <code>essentials.homes.TIER</code>             | Home limit for tier (e.g., essentials.homes.vip). Configure tiers in config.toml            |
| <code>essentials.setwarp</code>                | Create warps                                                                                |
| <code>essentials.warp</code>                   | Teleport to warps                                                                           |
| <code>essentials.warp.others</code>            | Teleport other players to warps (console always has access)                                 |
| <code>essentials.delwarp</code>                | Delete warps                                                                                |
| <code>essentials.setspawn</code>               | Set server spawn                                                                            |
| <code>essentials.spawn</code>                  | Teleport to spawn                                                                           |
| <code>essentials.spawn.others</code>           | Teleport other players to spawn (console always has access)                                 |
| <code>essentials.tpa</code>                    | Send teleport requests                                                                      |
| <code>essentials.tpaccept</code>               | Accept teleport requests                                                                    |
| <code>essentials.build.bypass</code>           | Build when global building is disabled                                                      |
| <code>essentials.spawn.bypass</code>           | Build in spawn protection area                                                              |
| <code>essentials.kit</code>                    | Open kit selection GUI                                                                      |
| <code>essentials.kit.kitname</code>            | Access to claim a specific kit. Kit names are case sensitive--they should be all lowercase! |
| <code>essentials.kit.create</code>             | Create new kits                                                                             |
| <code>essentials.kit.delete</code>             | Delete kits                                                                                 |
| <code>essentials.kit.cooldown.bypass</code>    | Bypass kit cooldowns                                                                        |
| <code>essentials.teleport.bypass</code>        | Bypass teleport delay                                                                       |
| <code>essentials.back</code>                   | Teleport to last death location                                                             |
| <code>essentials.afk.cooldown.bypass</code>    | Bypass AFK cooldown                                                                         |
| <code>essentials.rtp</code>                    | Random teleport                                                                             |
| <code>essentials.rtp.cooldown.bypass</code>    | Bypass RTP cooldown                                                                         |
| <code>essentials.list</code>                   | List online players                                                                         |
| <code>essentials.heal</code>                   | Restore health to full                                                                      |
| <code>essentials.freecam</code>                | Toggle freecam                                                                              |
| <code>essentials.god</code>                    | Toggle god mode (invincibility)                                                             |
| <code>essentials.msg</code>                    | Send private messages and reply (aliases: /m, /message, /whisper, /pm, /r, /reply)          |
| <code>essentials.tphere</code>                 | Teleport players to you                                                                     |
| <code>essentials.top</code>                    | Teleport to highest block                                                                   |
| <code>essentials.reload</code>                 | Reload configuration files                                                                  |
| <code>essentials.chat.color</code>             | Use color codes in chat messages                                                            |
| <code>essentials.shout</code>                  | Broadcast messages to all players (aliases: /broadcast)                                     |
| <code>essentials.repair</code>                 | Repair items (aliases: /fix)                                                                |
| <code>essentials.repair.cooldown.bypass</code> | Bypass repair cooldown                                                                      |
| <code>essentials.trash</code>                  | Open /trash                                                                                 |

# Configuration

Configuration is stored in `config.toml`.

**Kits**

Kits are configured in `kits.toml`. Create kits in-game with `/kit create <name>` or edit the file directly.

*   `display-name` - Name shown in the kit GUI
*   `cooldown` - Cooldown in seconds (0 = no cooldown)
*   `type` - `"add"` to add items to inventory, `"replace"` to clear inventory first

Each kit requires `essentials.kit.kitNameHere` permission to claim. Items that don't fit in the intended slot (e.g., armor when already wearing armor) will go to the player's inventory, and only drop on the ground if the inventory is full.

# Community & Support

Join our Discord for support, bugs, and suggestions:  
[https://discord.gg/z53BDHS89M](https://discord.gg/z53BDHS89M)

***

Note: Essentials is inspired by but not affiliated with the EssentialsX Minecraft plugin.