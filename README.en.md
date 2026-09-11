# Manhunt Wildcard

English | [简体中文](README.md)

Manhunt Wildcard is a Fabric mod for Minecraft Manhunt. Players split into hunters and runners, and random wildcard events fire periodically during the round to keep the chase unpredictable.

![Manhunt Wildcard](Manhunt-Wildcard.jpg)

## Inspiration

Part of the idea comes from the recent APEX Legends wildcard seasons: temporary rule twists layered on top of the normal match so every round brings new risks, chances, and tactical calls. Manhunt Wildcard brings that "in-round rule variant" concept to Minecraft Manhunt so the chase no longer relies on fixed routes and pacing.

## Features

- Hunter vs runner team gameplay
- Operator-controlled game start, stop, and wildcard testing
- Preparation phase to prevent messy starts
- Tracking compass for hunters, refreshed by server config
- Selectable compass target: sneak + right-click cycles nearest / each runner, right-click opens a menu; the item name shows target and distance
- Red hunter names, blue runner names; the vanilla locator bar only shows your own side
- Kill credit: a death within 15 s of a hunter hit counts for that hunter; pure environment deaths convert by count
- Black-screen death wait, then a random surface respawn away from the death point (runners also away from hunters)
- Survive-time rounds get a square world border (configurable radius) so runners cannot kite forever
- Players without a side spectate; several rules can be changed live mid-round
- Periodic random wildcards shown through HUD, BossBar, and chat messages
- Multiple runner and hunter win conditions
- Configurable respawn modes, lives, death drops, and preparation boundary
- Configurable hunter-to-runner damage multiplier and hunter / runner speed multipliers
- Configurable piglin bartering ender pearl chance (40% by default)
- "Round Setup" HUD shown automatically in the lobby; non-operators press `M` during the round for a status panel
- English and Simplified Chinese language files using Minecraft's native lang system

## How a Round Plays

### 1. Preparation

Starting a game begins a countdown. Hunters are held inside a boundary around spawn while runners get a head start. The BossBar at the top shows the remaining preparation time.

![Preparation](screenshot/GAMEPLAY1.png)

### 2. The Chase

Once preparation ends, hunters receive a tracking compass. The objective panel on the left shows the runners' current win goal (survive time, collect items, and so on) and the action bar shows your role.

![Round running](screenshot/GAMEPLAY2.png)

### 3. Wildcards

Every so often a wildcard is drawn. A short reveal animation plays, the rule description appears at the top left, and the BossBar counts down its duration. Below: "Featherweight" in effect.

![Wildcard active](screenshot/GAMEPLAY4.png)

### 4. Kill and Respawn Feedback

A hunter kill pops a feedback card at the top right; in kill-count mode it also shows how many kills remain. Runner respawns get a matching notice.

![Kill feedback](screenshot/GAMEPLAY5.png)

![Respawn notice](screenshot/GAMEPLAY6.png)

### 5. Round End

When either side meets its win condition the round ends: the result shows at the top right, a full summary is printed to chat, and the winners get fireworks.

![Round result](screenshot/GAMEPLAY3.png)

### 6. Status HUDs

While a round is being set up, a "Round Setup" panel appears at the top left for everyone, listing win conditions, respawn rules and wildcard settings. It disappears automatically once the round starts.

![Round setup](screenshot/HUD_lobby.png)

During the round, non-operators press `M` to toggle a "Round Status" panel with the phase countdown, current wildcard and time left.

![Round status](screenshot/HUD_status.png)

## New Wildcards

| Key Scramble | Tiny Players |
| --- | --- |
| ![Key Scramble](screenshot/WILDCARD_key_scramble.png) | ![Tiny Players](screenshot/WILDCARD_tiny_players.png) |

| Fragile | Who Are You? |
| --- | --- |
| ![Fragile](screenshot/WILDCARD_fragile.png) | ![Who Are You?](screenshot/WILDCARD_who_are_you.png) |

| Stay Away! | Backrooms! |
| --- | --- |
| ![Stay Away!](screenshot/WILDCARD_stay_away.png) | ![Backrooms!](screenshot/WILDCARD_backrooms.png) |

When the Backrooms end you drop back over your entry point with no fall damage:

![Backrooms return](screenshot/WILDCARD_backrooms_return.png)

## Wildcards

| Category | Wildcard | Effect |
| --- | --- | --- |
| Combat | Backstab | PvP hits only land from behind, at double damage; frontal hits do nothing, not even knockback |
| Combat | Vampire | Damage dealt to any living thing heals you |
| Combat | Blood Rage | At three hearts or less every hit you land is lethal |
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
| Mobility | Pearl Frenzy | Periodic ender pearls with possible side effects |
| Mobility | Wind Charge Brawl | Periodic wind charges |
| Mobility | Light Load | Light armor speeds up, heavy armor slows down |
| Mobility | Hunger Chase | Eating anything grants 10 s of Speed VI |
| Vision | Still Glow | Stand still for 1 s and you glow (red hunters, blue runners); moving stops it |
| Vision | Sneak Freeze | Sneaking makes you invisible and invulnerable, but you cannot move, jump or attack |
| Vision | Hunter Radar | Runners glow all along; they get a warning when a hunter comes within the configured range |
| Vision | Who Are You? | Everyone becomes Steve, name tags hide, tab list and chat show "Player" |
| Vision | Tiny Players | Everyone shrinks to about one block tall |
| Vision | World Tilt | 30 s after the draw gravity turns sideways (with a countdown) and the view rolls to match, until the wildcard ends |
| World & Items | Supply Drop | Chests land at the midpoint between the sides (one per runner); a beacon marks the spot 20 s ahead |
| World & Items | Drop Bomb | Anything thrown from the inventory explodes after 2 s without breaking blocks or destroying drops |
| World & Items | Chain Mining | Breaking a block also clears the 3x3x3 ahead along your look direction, only what your tool can harvest |
| World & Items | Block Decay | Newly placed blocks vanish after a delay |
| World & Items | Backrooms! | Everyone drops into a yellow maze dimension; every 20 s both sides glow for 5 s (hunters red, runners blue); fall through a false floor to get home; once a whole side is out everyone returns |

## Config Screen

Press `M` (rebindable) to open the menu at any time. There are only three pages, plus a Debug page for operators.

### Game page

Round status, join hunters or runners, start the round (OP). Once a round is running, a button here toggles the top-left "Round Status" panel.

![Game page](screenshot/GUI_game.png)

### Rules page

A hub with five sub-pages; press "Edit" to open one and use the breadcrumb to go back:

| Sub-page | Contents |
| --- | --- |
| Timing & boundary | preparation time, hunter preparation boundary |
| Victory | runner victory (dragon, survive time, reach location, collect items) with its parameters, the world border for survive mode, hunter victory (all runners out or kill count) |
| Respawn & death | respawn modes, lives and timers per side, random respawn point and distances, death drops |
| Kill credit | how long after a hunter hit a death still counts for that hunter; how many pure environment deaths make one hunter kill |
| Balance | hunter damage multiplier, movement speed per side, piglin pearl chance, team-only locator bar |

While a round is running operators can still change the "live" settings (speeds, damage, victory target values, wildcard toggles and parameters, respawn timing and placement, kill credit, drops); saving broadcasts "Rules updated". Preparation time, victory types and respawn modes are locked mid-round.

![Rules page](screenshot/GUI_basic.png)

### Wildcards page

Wildcard interval and duration, per-wildcard toggles, and sub-pages for wildcards with their own parameters.

![Wildcards page](screenshot/GUI_wildcards.png)

The **Debug page** requires `/hw debug true` and offers start / stop, roll now, and testing any single wildcard.

## Requirements

- Minecraft `1.21.11`
- Fabric Loader `0.16.0+`
- Fabric API
- Java `21`

Install on both client and server. The server runs the game logic; the client provides the config screen, HUDs, skin swap, and localized text.

## Installation

1. Install Fabric Loader.
2. Install Fabric API.
3. Download `MC-Manhunt-Wildcard-<version>.jar`.
4. Put the jar into the `mods/` folder on both client and server.
5. Launch the game or server.

The config file is created on first launch:

```text
config/hunterwildcard.json
```

The internal config name and mod ID stay `hunterwildcard` for compatibility with existing configs, language keys, and the network protocol.

## Player Commands

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

## Design Goals

Manhunt Wildcard aims to make Manhunt rounds more varied:

- Break the fixed rhythm of the chase
- Add randomness, on-the-spot judgment, and counterplay
- Keep the core objective feel of Minecraft Manhunt
- Give server owners a configurable, testable, localizable gameplay extension

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).
