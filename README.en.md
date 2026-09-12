# Manhunt Wildcard

English · [简体中文](README.zh-CN.md) · [Modrinth](https://modrinth.com/mod/manhunt-wildcard) · [Changelog](CHANGELOG.md)

**Hunt, escape, and adapt to random rules that change the chase.**

Minecraft `1.21.1–26.2` · Fabric · 28 wildcards · English / Chinese

![Manhunt Wildcard](Manhunt-Wildcard.jpg)

[Getting started](#getting-started) · [Interface guide](#interface-guide) · [Presets](#presets) · [Wildcards](#wildcards) · [Commands and configuration](#commands-and-configuration)

## Getting started

Install the matching mod build on the client and server, together with Fabric API for that Minecraft version. Use Fabric Loader `0.19.3` or newer.

| Minecraft | Java | Release file |
| --- | --- | --- |
| 1.21.1 | 21+ | `MC-Manhunt-Wildcard-1.4.6-mc1.21.1.jar` |
| 1.21.2 | 21+ | `MC-Manhunt-Wildcard-1.4.6-mc1.21.2.jar` |
| 1.21.3 | 21+ | `MC-Manhunt-Wildcard-1.4.6-mc1.21.3.jar` |
| 1.21.4 | 21+ | `MC-Manhunt-Wildcard-1.4.6-mc1.21.4.jar` |
| 1.21.5 | 21+ | `MC-Manhunt-Wildcard-1.4.6-mc1.21.5.jar` |
| 1.21.6 | 21+ | `MC-Manhunt-Wildcard-1.4.6-mc1.21.6.jar` |
| 1.21.7 | 21+ | `MC-Manhunt-Wildcard-1.4.6-mc1.21.7.jar` |
| 1.21.8 | 21+ | `MC-Manhunt-Wildcard-1.4.6-mc1.21.8.jar` |
| 1.21.9 | 21+ | `MC-Manhunt-Wildcard-1.4.6-mc1.21.9.jar` |
| 1.21.10 | 21+ | `MC-Manhunt-Wildcard-1.4.6-mc1.21.10.jar` |
| 1.21.11 | 21+ | `MC-Manhunt-Wildcard-1.4.6-mc1.21.11.jar` |
| 26.1 | 25+ | `MC-Manhunt-Wildcard-1.4.6-mc26.1.jar` |
| 26.1.1 | 25+ | `MC-Manhunt-Wildcard-1.4.6-mc26.1.1.jar` |
| 26.1.2 | 25+ | `MC-Manhunt-Wildcard-1.4.6-mc26.1.2.jar` |
| 26.2 | 25+ | `MC-Manhunt-Wildcard-1.4.6-mc26.2.jar` |

Each file supports its listed stable version. Download from [Modrinth](https://modrinth.com/mod/manhunt-wildcard/versions) or [GitHub Releases](https://github.com/xiaoming6680/MC-Manhunt-Wildcard/releases). See [Building](docs/BUILDING.md) for source builds.

All 15 downloads contain the same wildcard lineup. Before Minecraft 1.21.6 there is no vanilla locator bar, so its setting is hidden. Before 1.21.11, supply drops use a trident instead of the diamond spear; copper armor applies only on versions that include it. Each client and server must run the same Minecraft version.

The current mod version is `1.4.6`. Install this same JAR on both sides and remove old copies. New settings use protocol v3 and cannot be mixed with builds using older protocols. Incompatible peers receive an explicit update message.

1. Place the matching `MC-Manhunt-Wildcard-1.4.6-mc<game-version>.jar` in the instance's `mods/` folder.
2. Enter a world, press **M**, and join the red Hunters or blue Runners.
3. An operator chooses a preset or adjusts rules, clicks **Apply**, then starts the round once both sides have players.

Hunters track runners with a compass; runners complete the victory objective. Right-click the compass to choose a target, or sneak and right-click to cycle. Unassigned players spectate.

While waiting to respawn, automatically watch a living teammate. Press **Z / X** for the previous / next teammate (rebindable in Controls), or fly freely when no teammates remain. Eliminated players start in free spectator mode: **Z / X** visits any other online player's position, including opponents, while retaining free flight. Respawn timers, remaining lives, and respawn locations still follow the existing rules.

Nether and End deaths return to the Overworld: keep a valid Overworld respawn point, or fall back to world spawn. For deaths in other dimensions, random respawn searches around that Overworld destination. Watching teammates never changes the destination.

Every successful round start resets advancement progress for all online players, including spectators and partially completed advancements. Team death-drop rules also apply during preparation.

## Interface guide

Screenshots use Chinese; the interface also follows Minecraft's English language setting.

### Match

Team cards show members and status, followed by each side's objective. Join actions use team colors; leaving a team is red.

![Match and teams](screenshot/UI_lobby.png)

### Rules

The overview groups the current rules. Use the sidebar or a small **Adjust** link to edit a category. Inactive parameters and repeated explanations stay hidden. Time inputs accept `minutes:seconds` or seconds.

![Rules overview at GUI scale 4](screenshot/UI_rules_scale4.png)

Changes stay in a draft until **Apply**; **Undo changes** discards them. Everyone can read the rules; operators can edit them. During a round, only supported live settings are editable. Conflicts or failed saves preserve the draft and show an error.

### Wildcards

Four categories retain the matrix layout. Search names, effects, or IDs. Hover the small **i** for an explanation; **Settings** edits parameters only. Enable or disable wildcards in the matrix; resetting parameter defaults preserves their switches. Timing inputs sit directly above the matrix.

![Wildcard matrix](screenshot/UI_matrix.png)

Global **Enable all / Disable all** affects every wildcard. Category actions affect only their category. Search does not narrow the scope of bulk changes.

<details>
<summary>Item picker, hover explanations, and display preferences</summary>

Search item names for collection objectives, or enter an item ID.

![Item picker](screenshot/UI_items.png)

Hover explanations show only the effect and wrap automatically.

![Wildcard explanation](screenshot/UI_wildcard_tooltip.png)

Local preferences control HUD scale, opacity, margins, animation, flashes, and feedback duration. **H** toggles the status HUD.

![Display preferences](screenshot/UI_display.png)

</details>

## Presets

Hover to preview, click to create an editable draft, then apply.

| Preset | Runner objective | Respawns and wildcards |
| --- | --- | --- |
| Classic Manhunt | Defeat the Ender Dragon | One runner life, unlimited hunter respawns; all wildcards off |
| Dragon Hunt | Defeat the Ender Dragon | Three runner lives; current wildcard switches retained |
| Timed Survival | Survive 15 minutes | Three runner lives; current wildcard switches retained |
| Collection Race | Collect 16 diamonds | Three runner lives; current wildcard switches retained |

Classic follows the dragon-hunt format of [Dream's Manhunt](https://www.youtube.com/watch?v=qqOxkuO3ip0): vanilla drops and respawn locations, 1× damage and speed, and no extra piglin trade boost. This mod retains a one-second preparation and hunter respawn transition. With multiple runners, any runner's elimination ends the round; these rules remain adjustable.

## Wildcards

Wildcards trigger at the configured interval. The top-left introduction stays visible until the wildcard ends; its i in the M menu also explains the effect. Combat and match feedback appear at the top right.

| Key Scramble | Backrooms |
| --- | --- |
| ![Key Scramble](screenshot/WILDCARD_key_scramble.png) | ![Backrooms](screenshot/WILDCARD_backrooms.png) |

<details>
<summary>All 28 wildcards and effects</summary>

| Category | Wildcard | Effect |
| --- | --- | --- |
| Combat | Backstab | PvP hits only land from behind, at double damage; frontal hits do nothing, not even knockback |
| Combat | Vampire | Damage dealt to any living thing heals you |
| Combat | Last Stand | At three hearts or less every hit you land is lethal |
| Combat | Weapon Overheat | Rapid attacks build heat; a HUD bar under the crosshair shows it |
| Combat | Stay Away! | Runners get an unbreakable Sharpness 255 golden sword that cannot be dropped or picked up by hunters |
| Combat | Fragile | Everyone's max health drops to three hearts |
| Combat | Explosive Death | Deaths or kills trigger explosions |
| Combat | Key Scramble | Every hit shuffles movement keys; a top-right HUD shows the current layout |
| Mobility | Flash | Everyone gets Speed X |
| Mobility | Shadow Step | Players you hit blink 1-4 blocks away |
| Mobility | Hurt Teleport | Taking damage teleports you 1-15 blocks away; lava is fair game |
| Mobility | Space Shift | Every 60 s (configurable) everyone in the same dimension is dealt a random position (possibly their own), with a running countdown |
| Mobility | Portals | Random pairs each get a one-shot portal (end gateway blocks) that leads to the partner's portal; an odd player joins a random pair |
| Mobility | Pearl Frenzy | Temporary ender pearls with possible side effects; unused grants expire at the end, preserving your own pearls |
| Mobility | Wind Charge Brawl | Periodic wind charges |
| Mobility | Light Load | Light armor speeds up, heavy armor slows down |
| Mobility | Hunger Chase | Eating anything grants 10 s of Speed VI |
| Vision | Still Glow | Stand still for 1 s and you glow (red hunters, blue runners); moving stops it |
| Vision | Sneak Freeze | Sneaking makes you invisible and invulnerable, but you cannot move, jump or attack |
| Vision | Hunter Radar | Runners glow all along; they get a warning when a hunter comes within the configured range |
| Vision | Who Are You? | Everyone becomes Steve, name tags hide, tab list and chat show "Player" |
| Vision | Tiny Players | Everyone shrinks to about one block tall |
| Vision | World Tilt | After a 30 s countdown, gravity turns sideways together with the camera, body collision and controls. Walk and jump on walls; sideways falls cause damage. Normal gravity returns when the wildcard ends |
| World & Items | Supply Drop | Chests land at the midpoint between the sides (one per runner); a beacon marks the spot 20 s ahead |
| World & Items | Drop Bomb | Anything thrown from the inventory explodes after 2 s without breaking blocks or destroying drops |
| World & Items | Chain Mining | Breaking a block also clears the 3x3x3 ahead along your look direction, only what your tool can harvest |
| World & Items | Block Decay | Newly placed blocks vanish after a delay |
| World & Items | Backrooms! | Everyone drops into a yellow maze dimension; every 20 s both sides glow for 5 s (hunters red, runners blue); fall through a false floor to get home; once a whole side is out everyone returns |

</details>

<details>
<summary>HUD, kill feedback, and results (test scenarios)</summary>

![Match HUD](screenshot/UI_wildcard_intro.png)

![Kill feedback](screenshot/UI_combat.png)

![Results](screenshot/UI_result.png)

</details>

## Commands and configuration

Server rules: `config/hunterwildcard.json`. Local display preferences: `config/hunterwildcard-ui.json`; these do not change server rules.

New installations default to wildcard dragon hunts: runners have 3 total lives, hunters respawn indefinitely, and hunters win once all runners are out. All 28 wildcards are enabled. Both sides drop their inventory on death and use 1× damage and movement speed.

| Default option | Setting |
| --- | --- |
| Preparation / results display | 60 seconds / 10 seconds |
| Compass refresh | Every 3 seconds |
| Ordinary wildcards | Fixed 180-second gap and 120-second duration; the gap starts after the previous event ends, with another 5 seconds for the draw |
| Initial ranges when random timing is selected | Gaps of 120–240 seconds, durations of 90–150 seconds; fixed timing is the default |
| Backrooms time limit | Separate 180-second timer; escape conditions can end it early |
| Respawn wait | 10 seconds for both sides; each previous hunter death adds 3 seconds, capped at 60 extra seconds |
| Random respawn | Enabled; runners use a 150–300 block search and hunters 64–128 blocks; hunters try to stay at least 96 blocks from runners. Deaths in other dimensions use the Overworld destination as their search origin |
| Piglin pearl trade chance | Boost enabled, 20% |
| Blaze rod drop chance | Override disabled by default; enable in Rules → Balance to set 0–100%, initially 50% |
| Wind charge explosion multiplier | 150% |
| Initial objective when survive-time mode is selected | 15 minutes, with a 500-block border radius; the default victory objective remains the dragon |

Upgrading preserves existing configuration values; missing fields use the new defaults. For settings exposed in the M menu, use **Reset page** in the relevant category or wildcard settings page, review the draft, and click **Apply**; only editable fields on the current page are restored. Settings absent from the menu, such as compass refresh and results duration, require editing the JSON and running `/hw config reload`. The Classic Manhunt preset still uses one life and no wildcards.

<details>
<summary>Player and operator commands</summary>

| Command | Description |
| --- | --- |
| `/hw join hunter` | Join hunters |
| `/hw join runner` | Join runners |
| `/hw leave` | Leave your team |
| `/hw status` | Show the current game status |
| `/hw wildcard list` | List wildcard enable states |

## Operator Commands

These commands require OP:

| Command | Description |
| --- | --- |
| `/hw start` | Start the game |
| `/hw stop` | Stop the game |
| `/hw wildcard roll` | Roll a wildcard now |
| `/hw wildcard stop` | Stop the current wildcard |
| `/hw wildcard test <id>` | Test a specific wildcard |
| `/hw config reload` | Reload config |
| `/hw config save` | Save config |
| `/hw debug true` | Enable the debug page |
| `/hw debug false` | Disable the debug page |

</details>

## Inspiration and license

Inspired by Minecraft Manhunt and the temporary rule changes in APEX Legends wildcard events.

[MIT License](LICENSE)
