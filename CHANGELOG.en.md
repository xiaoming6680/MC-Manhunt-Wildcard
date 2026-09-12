# Changelog

English | [Simplified Chinese](CHANGELOG.md)

## 1.4.0

### Fixes

- Backrooms: the server crashed (integrated servers simply closed) when a whole side left the maze. The wildcard stopped itself synchronously from its own tick, nulling the active rule while the manager was still using it; the stop is now applied after the tick.
- Death drops: the mod now owns vanilla `keepInventory` during a round (records it at start, forces it off and re-checks every second, restores it afterwards), so a host toggling the vanilla rule no longer disables the per-side drop settings.
- Menu key screen: empty or invalid text fields no longer block page switching, scrolling or closing; they revert to the last valid value with a toast. Scrolling and server syncs no longer lose the text being typed or the focus, and ESC always closes.

### Wildcard rework

- 12 new wildcards: Backstab, Vampire, Flash, Shadow Step, Hurt Teleport, Space Shift, Portals, Still Glow, Sneak Freeze, World Tilt, Drop Bomb, Chain Mining.
- Removed the pure-buff cards: Speed Rush, Featherweight, Glowing, Night Hunt, Compass Chaos, Disabled.
- Reworked: Supply Drop now lands at the midpoint between the sides, one chest per runner, coordinates broadcast 20 s ahead; Hunter Radar now makes runners glow for the whole card and warns them when a hunter closes in (range configurable); Blood Rage makes every hit lethal below three hearts; Hunger Chase is just 10 s of Speed VI per meal; the Weapon Overheat bar is bigger and labelled.
- Backrooms: every 20 s both sides glow for 5 s, hunters red and runners blue (all glow effects are team-coloured).
- Players glow in their team colour while picking sides in the lobby.
- A respawned runner sees the hunter who killed them glow for 10 s, with their name, so they know which way to run.
- The kill feedback card is back to its normal size; the status card hides while a draw animation plays.
- Backrooms ambience is half as loud.
- Kill credit gained a "Count environment deaths" toggle (Rules > Kill credit); off means pure environment deaths never add to the hunters' kill count.
- Drop Bomb: fused items no longer merge with nearby drops, which used to make some of them vanish without exploding.
- World Tilt: gravity really turns sideways (no vertical gravity, a horizontal pull); mouse look, movement, jumping, standing on walls and the player model all follow the new gravity frame with a 3-second transition. Hitboxes stay upright, a vanilla limitation.
- Wildcard interval and duration can each be fixed or random (min-max); cards drawn earlier this round are weighted down by 1/(1+draws) so fresh ones come up first.
- Menu: wildcard toggles are grouped into Combat / Mobility / Vision / World & Items cards, each with all-on/all-off; the home page puts team picking and Start at the top; scrolling no longer rebuilds widgets (fixes inputs and dropdowns that sometimes would not react), server syncs wait while a dropdown is open, leaving a wildcard's settings returns to the previous scroll position, and the settings header no longer shows a format error.
- Config: toggles moved to an `enabledWildcards` map (old `enableXxx` keys migrate automatically), radar interval became `hunterRadarWarningDistance`, and `spaceShiftIntervalSeconds` plus the timing-mode fields were added.

### Gameplay

- Tracking compass: sneak + right-click cycles "nearest runner → runner A → runner B → …", right-click opens a selection menu; the item name shows the current target and distance ("Tracking Compass → Steve (137 blocks)").
- Kill credit rework: any runner death within 15 s (configurable) of being hit by a hunter counts as that hunter's kill; pure environment deaths (falls, mobs, suicide) are counted separately and every N of them (default 1, configurable) count as one hunter kill, closing the free-teleport-by-suicide loophole. Environment kills show as "Environment" in the kill feedback. Every runner death now gets a top-right card (including environment-death progress); kill cards are larger with a red flash and heavier sound.
- Death wait: the screen is fully black while waiting to respawn, showing only the countdown, cause of death and lives left; the player is held in place, so spectator scouting is gone.
- Respawn point: by default you respawn at a random surface spot X to 2X blocks from where you died (runners from 150 blocks and at least 150 blocks from every hunter where possible, hunters from 50 blocks); hunters no longer get a free trip back to spawn by dying. Can be disabled; distances are configurable. Hunters also respawn at least 200 blocks from every runner (configurable) and wait 5 s longer per death (configurable, 60 s cap), so a death is not an instant re-engage.
- Survive-time mode: a square world border centred on spawn (radius 500 blocks by default, configurable, can be disabled) so runners cannot win by kiting forever.
- Players who did not join a side spectate for the round and are restored when it ends.
- Some rules can be changed mid-round (speeds, damage multiplier, victory target values, wildcard toggles and parameters, respawn timing and placement, kill credit, drops); saving broadcasts "Rules updated".
- Backrooms: the large exit clearing is gone; exits are now rare scattered 4×4 false-floor cells (about one per 500 cells) that only appear inside corridor cells, never under a wall.

### UI and HUD

- Leaner status HUD: the lobby panel drops the hunter respawn and prep-time rows, the in-game panel only shows phase, role and current wildcard; the role action bar only runs for the first 15 s.
- Hunter names are red and runner names blue (tab list, chat, name tags, locator bar) via scoreboard teams that are removed when the round ends.
- The vanilla locator bar stays on during a round but only shows players of your own side (can be disabled).
- The objective panel gained a hunter progress line: "Hunter kills 3/10" in kill-count mode, "Runners alive 2/3 · 4 lives" in elimination mode.
- The menu key screen is reduced to three pages (Game / Rules / Wildcards, plus Debug), with Rules split into sub-pages (Timing & boundary / Victory / Respawn & death / Kill credit / Balance). Team joining moved to the Game page, the round status HUD is toggled from a button there, and ending time, compass refresh, action-bar interval and boundary warning distance are no longer in the UI (still in the config file).

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
