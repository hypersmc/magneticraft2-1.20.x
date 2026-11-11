# Living Main Menu — scene library contract (proposed)

**Status:** Design contract only. This document does not imply that scene loading, configuration, tick-rate control, or size limiting has been implemented. Keep PR #21 in draft until real in-game acceptance.

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

Scene IDs are safe directory names such as `copper_workshop`. Sort deterministically. First iteration: use one enabled scene; add scene cycling after stable startup and navigation. Preserve compatibility with the current manually created `saves/magneticraft2_menu_source` until users have a migration path. Never silently move or delete it.

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
    "simulationDistanceChunks": 4
  }
}
```

- **Camera**: initial position and orientation are applied through the real integrated server's player teleport/synchronization, rather than changing a fake camera. Rotation is client-side viewing only; it must not modify gameplay saves. An optional camera section should use the source world's saved player position.
- **Time**: `timeOfDay` initializes the disposable world (Minecraft time, 0..23999) and `daylightCycle` controls whether it advances. The underlying level time/ticks and machine processing remain real.
- **Ticks**: `randomTickSpeed` is Minecraft's **random block tick** gamerule and **not** overall server ticks per second. Real machine simulation stays at ordinary **20 TPS** in the first implementation. Any separately requested global TPS control needs performance and mechanical-network compatibility study; it must not be simulated or misrepresented.
- **World limits**: `maxWorldMiB` and `maxStoredChunks` are hard source-admission checks. Count stored chunks by reading region headers, not counting region filenames or guessing from compressed save size. Reject oversize/corrupt scenes with an actionable log; never truncate or delete the user's source.
- **Runtime load radius**: `renderDistanceChunks` and `simulationDistanceChunks` are *target caps*, not yet implemented. They need careful isolation/restoration of Minecraft client/server distance settings and a measurement of actual worldgen/startup behavior. A world border alone does not stop all chunk loading or generation.
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

## Acceptance and regression

- [ ] Existing legacy scene still works when the new library is absent.
- [ ] Each scene loads a real server world using its own camera/time settings and actual machines tick at normal 20 TPS.
- [ ] A missing, invalid, oversized or symlinked scene never overwrites any save.
- [ ] Scene source remains untouched; disposable runtime can reset/restart safely.
- [ ] Different camera rotation settings, day/night presets and random tick gamerules work.
- [ ] Quitting and re-entering normal worlds restores any temporarily adjusted settings.
- [ ] Options/Mods, Singleplayer/Multiplayer/Realms navigation remains responsive.
- [ ] Actual startup/copy/chunk timings are compared before and after the bounds.

**Implementation order:** first verify PR #21's current menu-navigation fix; then scene folder + validated JSON selection with legacy fallback; then real server camera/time configuration; then map/chunk admission limits and measured startup optimization; finally multiple-scene rotation and polished scene assets.
