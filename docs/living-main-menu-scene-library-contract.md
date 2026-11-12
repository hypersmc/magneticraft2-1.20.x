# Living Main Menu — scene library contract (proposed)

**Status:** A new *client-only, path-gated* startup override now attempts to reduce the menu integrated server's initial spawn preparation from vanilla 441 chunks to a scene-configured square (default radius 3 = 49 chunks). The override changes both the START ticket and the completion threshold together; every ordinary world retains vanilla values. This implementation is **source-level only** until Forge compilation, Mixin injection and in-game startup are verified. A no-save shutdown is still unimplemented. Keep PR #21 draft.

## Goal

Package a collection of actual **Minecraft singleplayer worlds**, each with its own camera and world presentation settings. The integrated server continues to simulate the existing Magneticraft2 blocks, machine block entities, inventories, power networks and recipes. Do not replace the real world with a video, decorative model or fake simulation.

## Directory structure

Use the Minecraft game directory, not the normal saves folder, for the source library:

```text
<game-directory>/
  mgcmainmenu/
    scenes/
      copper_workshop/
        scene.json
        world/
          level.dat
          region/
          data/
          ...
      steam_factory/
        scene.json
        world/
          level.dat
          region/
          ...
  saves/
    _magneticraft2_living_menu_runtime/   # disposable, never a source
```

Scene IDs are safe lowercase directory names such as `copper_workshop`. The loader sorts enabled, valid, non-symlinked scenes alphabetically and selects them in **round-robin order on successive client launches**. It reads `mgcmainmenu/.last-successful-scene` (plain text ID) outside source saves, advances after a previous successful scene, and updates the state file **only after the new integrated menu world has actually joined**. A return from Singleplayer/Multiplayer during the *same JVM* reuses the same scene; no automatic in-menu world cycling or repeated 12-second load stalls. Invalid/disabled scenes are skipped; if none qualify, the existing `saves/magneticraft2_menu_source` legacy fallback remains supported. The directory is created automatically. Never silently move or delete the source save.

## Per-scene settings

Example `mgcmainmenu/scenes/steam_factory/scene.json`:

```json
{
  "schemaVersion": 1,
  "name": "Steam Factory",
  "enabled": true,
  "camera": {
    "position": { "x": 16.5, "y": 73.0, "z": 22.5 },
    "yaw": 90.0,
    "pitch": 12.0,
    "rotation": { "enabled": true, "degreesPerSecond": 1.5 }
  },
  "world": {
    "timeOfDay": 6000,
    "daylightCycle": false,
    "randomTickSpeed": 3
  },
  "limits": {
    "maxWorldMiB": 64,
    "maxStoredChunks": 512,
    "renderDistanceChunks": 4,
    "simulationDistanceChunks": 5,
    "initialSpawnRadiusChunks": 3
  }
}
```

- **Boot-time scene selection**: Round-robin across valid enabled scenes; on a new game launch start after the last *successfully loaded* scene, with deterministic first-scene selection if no state exists. A JVM session keeps its chosen scene on later menu visits, even after closing Singleplayer/Multiplayer screens. Disable all but one scene for fixed selection. The tiny `.last-successful-scene` state file lives in `mgcmainmenu`, not under `scenes/*/world` or `saves/`, and uses atomic replacement when supported. A malformed, unreadable or symlinked state file is ignored or safely refused; scene startup continues. There is no in-menu world switching; a scene change needs a fresh Minecraft launch.
- **Camera**: position and orientation are applied through a server-authoritative teleport of the actual integrated-server menu player, rather than a fake camera. Rotation changes that client's viewpoint each tick. Omitting `camera.position` keeps the source world's saved player position; omitting `rotation` defaults to 1.5 degrees/second. These changes affect the disposable runtime world only.
- **Time**: `timeOfDay` sets real ServerLevel daytime (0..23999) and `daylightCycle` sets the actual gamerule on the disposable server after the menu player connects. The underlying level ticks and machine processing remain real.
- **Ticks**: `randomTickSpeed` is now applied as Minecraft's actual **random block tick** gamerule and is **not** overall server ticks per second. Real machine simulation remains the standard **20 TPS** in this slice. User-requested adjustable global server TPS remains a separate unimplemented feature requiring mechanical-network and performance compatibility study.
- **World limits**: `maxWorldMiB` and `maxStoredChunks` are hard source-admission checks. Count stored chunks by reading region headers, not counting region filenames or guessing from compressed save size. Reject oversize/corrupt scenes with an actionable log; never truncate or delete the user's source.
- **Runtime player load radius**: `renderDistanceChunks` accepts 2–8 (default 4), and `simulationDistanceChunks` accepts 2–8 in legacy manifests (new default **5**). **Minecraft 1.20.1 enforces a minimum simulation distance of 5**, even when render distance is 2–4; simulation may legitimately exceed render. Older scene JSON requesting simulation 2–4 is still accepted but gets an explicitly logged *effective cap of 5*. IntegratedServer copies client Options values each tick, so the menu temporarily limits both in memory. The actual user settings are restored before Options/Mods and before real gameplay; the cap resumes **on the next client tick** after `OptionsScreen.removed()` saves real preferences. Other mods saving client options while the menu is active could still persist temporary caps; test compatibility.
- **Actual initial spawn preparation (experimental)**: `initialSpawnRadiusChunks` (2–8, default **3**) controls the radius of the initially prepared square for **only** the disposable menu server. Vanilla Minecraft 1.20.1 always creates a START ticket level 11 and waits for **441 chunks** in `MinecraftServer.prepareLevels()`. With radius 3, a client-only Mixin changes the START region-ticket radius from 11 to 4 and the completion target to **(2 × 3 + 1)² = 49 chunks**. A third injection changes the startup progress listener's radius in `loadLevel()` to agree. These are *three matching changes*; changing only one can deadlock the loading screen. The Mixin is gated by `IntegratedServer`, an armed expected runtime path, and a real marker file inside the generated disposable save. Ordinary worlds and dedicated servers retain vanilla constants. For nonstandard scenes, pre-generate the area near the **world's spawn** and keep it near the camera to avoid deferred terrain generation when the player teleports. This does **not** impose an absolute cap on chunks requested later by other mods or machine networks.
- **Why an isolated Mixin rather than a void generator?** A void world makes each chunk cheaper but still prepares 441 chunks unless its ticket count is reduced. This patch changes only the two coupled startup constants, plus the loading progress radius; it does not replace the vanilla chunk engine, forge machine ticking or world generator. A void generator may still be useful for scenery but is not required to test the performance improvement. Actual 14.1-second baseline vs new time remains to be measured.
- **Mixin registration and acceptance**: The repo already uses `org.spongepowered.mixin` Gradle plugin; this slice registers `magneticraft2.mixins.json`, a generated refmap and jar `MixinConfigs` manifest attribute, and the Mixin annotation processor. **Do not assume this injection works until both runClient and a built production JAR have been tested.** If a mapping or target changes in Forge, `require=1` should expose that mismatch instead of silently changing only half the coupled chunk values.
- **No-save shutdown**: The source world is never modified, and all disposable runtime changes are deleted only after server shutdown and save-lock release. Vanilla `MinecraftServer.stopServer()` explicitly resets every `ServerLevel.noSave` to false and forces saving; `ServerChunkCache.close()` also saves chunks. Therefore we have **not** added an unsafe fake `noSave` flag, a forced thread kill, or partial save-hook suppression. Fully eliminating runtime writes is still a separate unimplemented, mod-compatibility-sensitive change.
- Validate numeric ranges, finite coordinates, supported schema versions, safe paths, no symlinks, and malformed JSON. On error skip that scene and retain the vanilla menu without corrupting real worlds.

## Visual bounds

Use a handcrafted small scene centered on the camera, with intentional occlusion: walls, hills, buildings, underground rooms, industrial yards, fog or a designed sky boundary. A static/slowly turning viewpoint makes a bounded scene practical. Do not use visually obvious world cuts or stretched textures. Pre-generate all chunks needed at the chosen view radius and measure time to first rendered frame.

## Loading performance

The current prototype copies its source save **synchronously on the client thread**. A size limit only prevents worst-case copies; it does **not** guarantee fast first-frame rendering. Measure separately:

1. Source-copy I/O time and bytes.
2. Integrated-server startup.
3. Initial chunk generation/load and client synchronization.
4. Time to first real scene frame; CPU, RAM and FPS.

Potential staged improvements: validate/bound source scenes first; move file copy off the render thread safely; keep non-gameplay submenus connected to the same live world; use a small pre-generated chunk set and scoped view/simulation distances; consider a prepared disposable cache if crash recovery and source-change detection are correct. Never tick the server during mod registration/resource initialization.

## Activating a scene now

1. Enable the existing Magneticraft2 client config option `Living Main Menu (experimental)`.
2. Create or copy a small real Minecraft save at `<game-directory>/mgcmainmenu/scenes/steam_factory/world/` with `level.dat` inside that `world` directory.
3. Copy `docs/examples/living-main-menu/scene.json` to `<game-directory>/mgcmainmenu/scenes/steam_factory/scene.json`; edit camera position and time to match your world.
4. Restart the game. If no valid enabled scene exists, the current legacy `run/saves/magneticraft2_menu_source` still works.

A scene's actual source world is never opened as gameplay by the menu. The loader works exclusively with an isolated copy in the existing runtime save folder. The source folder and name must not be symlinks.

## Acceptance and regression

- [ ] Existing legacy scene still works when the new library is absent.
- [ ] Each scene loads a real server world using its own camera/time settings and actual machines tick at normal 20 TPS.
- [ ] With three scenes installed, three successive **restarts** select copper_workshop, stone_camp and waterworks_quarry in order (wrapping from the previously loaded scene).
- [ ] Returning to the main menu from Singleplayer during the same client session does not advance the selected scene.
- [ ] Disabling or removing a scene skips it on the next launch; an all-invalid library retains the legacy fallback.
- [ ] No selection state is recorded after failed scene loading; a symlinked or invalid state file cannot redirect writes.
- [ ] A missing, invalid, oversized or symlinked scene never overwrites any save.
- [ ] Scene source remains untouched; disposable runtime can reset/restart safely.
- [ ] Different camera rotation settings, day/night presets and random tick gamerules work.
- [ ] Quitting and re-entering normal worlds restores any temporarily adjusted settings, even after a failed load/shutdown.
- [ ] While visiting Options and Mods, real user render/simulation settings are visible and saving them doesn't permanently store the menu-only caps.
- [ ] Confirm the actual integrated server uses the scene player radius without changes to a normal world's render/simulation options.
- [ ] Confirm `initialSpawnRadiusChunks:3` genuinely prepares **49** initial chunks and starts faster than the prior measured **14.1 seconds**, with the same scene/mod set.
- [ ] Confirm dedicated servers and ordinary Singleplayer saves still prepare vanilla spawn chunks (441), without modifying their source worlds.
- [ ] Test both `runClient` and a built/reobfuscated Forge JAR so the Mixin config and refmap actually resolve.
- [ ] Check `initialSpawnRadiusChunks:2` and `8` for safe initialization and verify camera/spawn locations outside the small prepared area generate necessary chunks normally.
- [ ] Check abnormal load timeouts never switch the start-ticket target back to 441 while the menu server is still preparing only 49 chunks.
- [ ] For a future no-save shutdown implementation, verify a graceful Forge shutdown and resource cleanup under Mekanism and other installed mods.
- [ ] Options/Mods, Singleplayer/Multiplayer/Realms navigation remains responsive.
- [ ] Actual startup/copy/chunk timings are compared before and after the bounds.

**Implementation status:** Real integrated-server startup, per-scene JSON, bounded startup chunk preparation and boot-time round-robin selection exist in PR #21 source. Multiple-scene source saves are distributed as an additional test ZIP; source worlds are never modified by selection. In-menu scene switching, faster teardown and full verification remain future work. Keep the PR draft pending build and in-game regression tests.
