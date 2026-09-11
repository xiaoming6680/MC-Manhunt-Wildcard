# Changelog

English | [Simplified Chinese](CHANGELOG.md)

## 1.3.1

- Fixed the Backrooms ambience never playing: the loop fades in from volume 0 and the sound engine skips silent sounds, so it is now flagged always-play; the sound event is registered during main init, the loop starts one second after entering and restarts itself if the dimension switch killed it, and the volume goes from 0.08 to 0.2. The client test now asserts the ambience is playing.

## 1.3.0

### New Wildcards

- Key Scramble: every hit shuffles forward, back, left, right, jump, sneak and sprint; a new top-right HUD shows the current layout. Original keys are restored when the wildcard ends or the client disconnects.
- Tiny Players: all participants shrink to half size (about one block tall), with camera and hitbox to match.
- Fragile: everyone's max health drops to three hearts; health is scaled back when the wildcard ends.
- Who Are You?: everyone gets the Steve skin, name tags are hidden, and tab list and chat show every name as "Player".
- Stay Away!: runners get an unbreakable Sharpness 255 golden sword that cannot be dropped or picked up by hunters; it is removed when the wildcard ends.
- Backrooms!: new `hunterwildcard:backrooms` dimension (ported from The Fourth Frequency's unrendered layer, textures and ambience included). Everyone sinks through the floor behind a blackout and lands spread out in the maze; hunters glow red for one second every 20 s, runners hide, hunters chase with the compass. No mobs, no block breaking, placed blocks vanish after 10 s. Falling through a false floor drops you back over your entry point with no fall damage (caves / Nether / End return in place). Once a whole side is out or time runs out, everyone returns. Deaths inside keep the inventory, reconnects are sent home, view distance is locked to 6 chunks, and the duration is configured separately (240 s by default).

### New Settings

- Piglin bartering: toggle and set the overall chance that a gold ingot barter yields ender pearls. Enabled by default at 40% (vanilla is about 2%). Found on the Basic page of the config screen.
- Role balance: hunter-to-runner damage multiplier plus hunter and runner movement speed multipliers (100% = vanilla).

### UI and HUD

- Default menu key changed from `H` to `M`.
- A "Round Setup" HUD is shown at the top left while a game is being set up so every player can see win conditions, respawn rules, and wildcard settings.
- Once the round starts, non-operators press `M` to toggle a "Round Status" panel; the server syncs status every second.
- Fixed clipped config cards: status pills wrap instead of truncating, labels that do not fit beside a control move above it, and partially scrolled wildcard tiles now draw their icon and name.
- README rewritten with screenshots produced by the automated client test (`./gradlew runClientGameTest`).
- While "Who Are You?" is active, hunter radar, kill / respawn feedback and death messages also show "Player" instead of real names.

## 1.2.0 - Modrinth Initial Release

This is the first Manhunt Wildcard version published on Modrinth. It is not an update from an older Modrinth release; it is the initial public release with the core gameplay experience included.

### Initial Features

- Adds the hunter vs runner Manhunt game framework.
- Supports `/hw` commands for operators to start and stop games, manage teams, and test wildcards.
- Adds a preparation phase to reduce chaotic starts and give players time to join teams and position themselves.
- Provides hunters with a tracking compass, with targets refreshed according to server configuration.
- Periodically triggers random wildcard events, shown through the HUD, BossBar, and chat messages.
- Supports multiple runner and hunter win conditions.
- Supports configurable respawn modes, lives, death drops, preparation boundary, wildcard interval, and wildcard duration.
- Provides an in-game config screen where OP players can adjust major rules while the game is waiting.
- Includes English and Simplified Chinese language files using Minecraft's native language system.

### Wildcards

This version includes 16 wildcards:

- Speed Rush
- Featherweight
- Glowing
- Night Hunt
- Explosive Death
- Supply Drop
- Hunter Radar
- Compass Chaos
- Hunger Chase
- Weapon Overheat
- Light Load
- Block Decay
- Pearl Frenzy
- Wind Charge Brawl
- Blood Rage
- Disabled Wildcard

### Compatibility

- Minecraft `1.21.11`
- Fabric Loader `0.16.0+`
- Fabric API
- Java `21`

Installing the mod on both client and server is recommended. The server runs the game logic; the client provides the config screen, HUD, and local language display.
