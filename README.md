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
*   [PlaceholderAPI](https://www.curseforge.com/hytale/mods/placeholder-api) Support
*   Other useful commands: /list, /heal, /freecam, /god, /tphere, /top

![Homes](https://raw.githubusercontent.com/nhulston/Essentials/refs/heads/main/images/homes.png) ![TPA](https://raw.githubusercontent.com/nhulston/Essentials/refs/heads/main/images/tpa.png) ![Warps](https://raw.githubusercontent.com/nhulston/Essentials/refs/heads/main/images/warps.png)

![Chat Format](https://raw.githubusercontent.com/nhulston/Essentials/refs/heads/main/images/chatformat.png) ![Chat Format2](https://raw.githubusercontent.com/nhulston/Essentials/refs/heads/main/images/chatformat2.png)

![Kits](https://raw.githubusercontent.com/nhulston/Essentials/refs/heads/main/images/kits.png)

# Commands

| Command                          | Description                         | Permission                           |
|----------------------------------|-------------------------------------|--------------------------------------|
| <code>/sethome</code>            | Set a home                          | <code>essentials.sethome</code>      |
| <code>/home</code>               | Teleport to your home               | <code>essentials.home</code>         |
| <code>/home PLAYER:</code>       | List another player's homes         | <code>essentials.home.others</code>  |
| <code>/home PLAYER:HOME</code>   | Teleport to another player's home   | <code>essentials.home.others</code>  |
| <code>/delhome</code>            | Delete a home                       | <code>essentials.delhome</code>      |
| <code>/setwarp</code>            | Set a server warp                   | <code>essentials.setwarp</code>      |
| <code>/warp</code>               | Teleport to a warp                  | <code>essentials.warp</code>         |
| <code>/warp NAME PLAYER</code>   | Teleport another player to a warp   | <code>essentials.warp.others</code>  |
| <code>/delwarp</code>            | Delete a warp                       | <code>essentials.delwarp</code>      |
| <code>/setspawn</code>           | Set server spawn                    | <code>essentials.setspawn</code>     |
| <code>/spawn</code>              | Teleport to spawn                   | <code>essentials.spawn</code>        |
| <code>/spawn PLAYER</code>       | Teleport another player to spawn    | <code>essentials.spawn.others</code> |
| <code>/tpa</code>                | Request to teleport to a player     | <code>essentials.tpa</code>          |
| <code>/tpaccept</code>           | Accept a teleport request           | <code>essentials.tpaccept</code>     |
| <code>/kit</code>                | Open kit selection GUI              | <code>essentials.kit</code>          |
| <code>/kit create</code>         | Create a kit from your inventory    | <code>essentials.kit.create</code>   |
| <code>/kit delete</code>         | Delete a kit                        | <code>essentials.kit.delete</code>   |
| <code>/kit KITNAME PLAYER</code> | Give another player a kit           | <code>essentials.kit.other</code>    |
| <code>/back</code>               | Teleport to your last death         | <code>essentials.back</code>         |
| <code>/rtp</code>                | Random teleport                     | <code>essentials.rtp</code>          |
| <code>/list</code>               | List online players                 | <code>essentials.list</code>         |
| <code>/heal</code>               | Restore your health to full         | <code>essentials.heal</code>         |
| <code>/freecam</code>            | Toggle freecam mode                 | <code>essentials.freecam</code>      |
| <code>/god</code>                | Toggle god mode (invincibility)     | <code>essentials.god</code>          |
| <code>/msg</code>                | Send a private message              | <code>essentials.msg</code>          |
| <code>/r</code>                  | Reply to last message               | <code>essentials.msg</code>          |
| <code>/socialspy</code>          | Toggle viewing all private messages | <code>essentials.socialspy</code>    |
| <code>/tphere</code>             | Teleport a player to you            | <code>essentials.tphere</code>       |
| <code>/top</code>                | Teleport to highest block           | <code>essentials.top</code>          |
| <code>/essentials reload</code>  | Reload configuration                | <code>essentials.reload</code>       |
| <code>/shout</code>              | Broadcast message to all players    | <code>essentials.shout</code>        |
| <code>/repair</code>             | Repair the item in your hand        | <code>essentials.repair</code>       |
| <code>/rules</code>              | Display server rules                | None                                 |
| <code>/trash</code>              | Throw away some items               | <code>essentials.trash</code>        |

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
| <code>essentials.warp</code>                   | Use the /warp command                                                                       |
| <code>essentials.warps.warpname</code>         | Access to a specific warp (e.g., essentials.warps.shop). Warp names are lowercase!          |
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
| <code>essentials.rtp</code>                    | Random teleport                                                                             |
| <code>essentials.rtp.cooldown.bypass</code>    | Bypass RTP cooldown                                                                         |
| <code>essentials.list</code>                   | List online players                                                                         |
| <code>essentials.heal</code>                   | Restore health to full                                                                      |
| <code>essentials.freecam</code>                | Toggle freecam                                                                              |
| <code>essentials.god</code>                    | Toggle god mode (invincibility)                                                             |
| <code>essentials.msg</code>                    | Send private messages and reply (aliases: /m, /message, /whisper, /pm, /r, /reply)          |
| <code>essentials.socialspy</code>              | Toggle viewing all private messages between players                                         |
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

# PlaceholderAPI Integration

Essentials has optional PlaceholderAPI integration in its chat formatting, in addition to providing the following placeholders:

| Placeholder                           | Description (for the requested player unless specified) |
|---------------------------------------|---------------------------------------------------------|
| %essentials_max_homes%                | Max homes                                               |
| %essentials_homes_num%                | Number of homes                                         |
| %essentials_homes_names%              | Names of homes (split by ", ")                          |
| %essentials_all_kits_num%             | Number of all kits on server                            |
| %essentials_all_kits_names%           | Names of all kits on server (split by ", ")             |
| %essentials_allowed_kits_num%         | Number of kits this player can access                   |
| %essentials_allowed_kits_names%       | Names of kits this player can access (split by ", ")    |
| %essentials_all_warps_num%            | Number of all warps on server                           |
| %essentials_all_warps_names%          | Names of all warps on server (split by ", ")            |
| %essentials_\<warp/home>_\<name>_world% | World name of a particular warp/home                    |
| %essentials_\<warp/home>_\<name>_coords% | Coords of a particular warp/home (x y z)                |
| %essentials_\<warp/home>_\<name>_x%   | X coord of a warp/home                                  |
| %essentials_\<warp/home>_\<name>_y%   | Y coord of a warp/home                                  |
| %essentials_\<warp/home>_\<name>_z%   | Z coord of a warp/home                                  |
| %essentials_\<warp/home>_\<name>_yaw% | Yaw of a warp/home                                      |
| %essentials_\<warp/home>_\<name>_pitch% | Pitch of a warp/home                                    |
| %essentials_warp_\<name>_allowed%     | Can this player access this warp                        |
| %essentials_home_\<name>_createdat%   | Timestamp of when a home was created                    |
| %essentials_kit_\<name>_name%         | Display name of a kit                                   |
| %essentials_kit_\<name>_id%           | Id of a kit                                             |
| %essentials_kit_\<name>_type%         | Type of a kit                                           |
| %essentials_kit_\<name>_cooldown%     | Cooldown of a kit                                       |
| %essentials_kit_\<name>_isreplacemode% | Whether kit has replacemode set to true/false           |
| %essentials_kit_\<name>_itemsnum%     | Number of items in a kit                                |
| %essentials_kit_\<name>_allowed%      | Can this player access the kit                          |



# Community & Support

Join our Discord for support, bugs, and suggestions:  
[https://discord.gg/z53BDHS89M](https://discord.gg/z53BDHS89M)

***

Note: Essentials is inspired by but not affiliated with the EssentialsX Minecraft plugin.