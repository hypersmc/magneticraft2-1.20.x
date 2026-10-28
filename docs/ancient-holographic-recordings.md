# Ancient holographic recordings — implementation design (Forge 1.20.1)

Status: design contract, **not yet implemented or build-verified**.
Base: `feature/bronze-age-foundry` after merged PR #12.

## Player contract

- A discoverable, reusable Ancient Holographic Projector item has exactly one immutable recording ID.
- Right-click a supporting block face to place the device, oriented toward the player-selected direction; playback begins automatically without a GUI.
- A 1:1-scale, non-colliding, non-interactable 3D recorded scene appears in the world. Players can walk around and through it. World geometry occludes the projection normally.
- Playback is predetermined. It can show correct operations, failures, repairs and consequences, but cannot change the real world or machine state.
- On normal completion the projector removes itself and drops an item preserving the recording ID; breaking it early also drops the same item and stops playback.
- Every nearby client sees the same server-authoritative playback time, including late joiners. Leaving range unloads visuals; returning resumes at the current time.
- Each recording has a computed integer bounding box. Tooltip lore presents an understated `W × H × D` engraving with no explanatory label. Dimension convention is local scene X/Y/Z, rotated with projector facing.
- Recordings are optional learning and exploration rewards, never progression gates. Book and multiblock blueprint projector remain independent.

## Integration with merged PR #12

PR #12 provides `assets/<namespace>/animations/*.json`, scene hierarchy, model references, typed driven tracks, `AnimationFrame`, `AnimationResourceLoader`, and `MachineVisualResourceLoader`.

Do **not** fork that evaluator or create a second per-machine renderer. Introduce a recording playback adapter that constructs an `AnimationFrame` from a deterministic recording clock and timeline. The adapter must feed explicitly authored named angles, integer inputs and states into the existing scene evaluation/render path. Real machine manifests are bound to actual Java-supplied channels and should **not** be used as the recording data schema. No gameplay inputs are invented for actual machines.

Static multiblock geometry and specialized Java effects are not fully controlled by PR #12. Start with explicit scene models and existing animation components, then add narrowly scoped visual adapters for fluids/items as needed. Resource-pack overrides must remain supported. Model registration must happen before baking, consistent with the existing resource reload lifecycle.

## Suggested recording resource

`assets/magneticraft2/holograms/mechanical_transmission.json` is a **proposed** resource location, not an existing loader contract.

A recording references:
- stable ID and archive number;
- optional display translation keys;
- a local 3D scene layout and placement anchor;
- existing animation scene IDs and static baked models;
- finite duration in ticks;
- keyed timeline channels and visibility changes;
- optional effects (particles, sound, callouts) with explicit limits.

Bounds are derived from actual scene extents at authoring/load time, including moving components where possible, rather than entered as arbitrary lore. Validate finite values, resource existence, total extent, total components, animation keys, and duration. Reject malformed resources safely without crashing the client.

## Playback and synchronization

Server block entity owns `recordingId`, `startGameTime`, `facing` and lifecycle. Never spawn real tutorial blocks or per-block entities. Clients render from `serverGameTime - startGameTime + partialTick` and evaluate the timeline directly, so skipped frames do not desynchronize motion. On expiry, the server performs a single idempotent item drop/removal. Early break uses normal loot handling without duplication. On chunk unload do not duplicate drops; document whether playback pauses or continues, and use a consistent server-time policy.

Scene visibility should use ordinary depth testing, appropriate transparency sorting, frustum/distance culling and cached static geometry. Do not render through terrain or walls. Avoid per-tick allocations and bound the number of simultaneously rendered recordings.

## First vertical slice

1. One projector item/block with fixed recording `mechanical_transmission`.
2. Placement and drop lifecycle (including break, chunk unload and multiplayer).
3. One static 1:1 scene of a crank, shaft and gears with proper world occlusion.
4. Deterministic rotation and overload/stop/recovery timeline using the PR #12 animation scene renderer.
5. Tooltip engraving computed from scene dimensions.
6. Resource reload and missing-model fallback.
7. Loot distribution and more recordings only after the runtime is proven.

## Acceptance tests

- `./gradlew build` succeeds on the feature branch.
- Place projector in all four horizontal directions; scene rotates without double-facing transforms.
- Hologram is walk-through and cannot be clicked/harvested as machinery.
- Existing world blocks occlude the projection; no blocks are modified.
- Two clients and a late joiner see matching phase and completion.
- Completion and early break each return exactly one projector with its original recording.
- Save/reload, chunk unload/reload, and F3+T do not crash or duplicate drops.
- Missing/invalid JSON fails gracefully; other machine visuals remain unchanged.
- Verify FPS with multiple projected scenes; avoid regression to earlier multiblock rendering issues.

## Open implementation details

Confirm the current `AnimationResourceLoader` and `AnimatedComponentRenderer` APIs before writing the playback adapter. Confirm client model registration and whether the current scene evaluator accepts a supplied `AnimationFrame` without a live machine block entity. Do not silently claim specialized washer fluids or ore physics work in holograms.

## Hologram appearance — current in-game prototype

The recording resource under `assets/<namespace>/holograms/<name>.json` now has an optional client-side `appearance` section:

```json
"appearance": {
  "tint": [0.38, 0.90, 1.0],
  "opacity": 0.32,
  "fade_in_ticks": 24,
  "fade_out_ticks": 36,
  "flicker": 0.28,
  "preserve_texture": true
}
```

- **`preserve_texture: true` (default):** Keep the original baked model atlas UVs. Existing wood/stone textures, resource-pack replacements and animation geometry remain intact, with a client-only translucent cyan tint.
- **`preserve_texture: false`:** Legacy flat-color/silhouette mode; sample a neutral atlas texel instead of material texture UVs. Useful for archive-style schematic projections and A/B tests.
- **`opacity`:** 0 to 1, currently reduced from 0.55 to 0.32 to lessen the visual buildup where several translucent faces overlap.
- **`tint`:** Linear per-channel vertex color multiplier; does not magically recolor source texture pixels to monochrome cyan.
- **`fade_in_ticks`/`fade_out_ticks`:** 0 to 200, evaluated against synchronized recording time; `flicker`: 0 to 0.40, scales a gentle continuous signal drift plus short, smoothly eased opacity dips about every 5–8 seconds (zero disables both). The dip schedule is deterministic and uses the synchronized recording clock.

This feature still reuses the exact existing PR #12 baked-model/animation scene. It only changes the `MultiBufferSource` supplied to that renderer and always uses vanilla `RenderType.translucent()` for depth-tested, non-colliding visuals. Real machine render paths are unchanged. Detailed mode avoids atlas lookups in the render loop; flat mode resolves the neutral sprite against the current atlas after resource reload.

**Visual testing note:** The first 0.02 flicker setting was effectively invisible at 0.32 opacity. The updated 0.28 default produces visible but brief signal loss rather than a high-frequency strobe. Observe at least 10–15 seconds of playback, not just the fade-in. A screenshot cannot establish whether flicker is perceptible.\n\n**Known tradeoffs:** Transparency is not perfect order-independent transparency. Through multiple gear teeth, bearings and support surfaces, alpha accumulates; reducing per-surface opacity limits but does not eliminate that. Sampling the original colorful textures with a cyan vertex multiplier preserves details, but dark woods can appear teal/dim rather than uniformly bright cyan. A custom shader using texture luminance would be a later, optional enhancement—not part of this prototype.

**Developer visual QA required:** build with `./gradlew build`; check source texture details on the wooden gears and stone shafts, gear-tooth overlap from front/side/top, world occlusion, materialization/dissolve, F3+T resource reload, resource-pack model override, and multiple simultaneous projections/FPS. Do not count these as passed until tested in-game.

## Compact artifact and cinematic projection — experimental PR #16 update

### Physical projector

- Placed model is a **small octagonal disc**, approximately 9/16 block wide and less than 4/16 block high, with a graphite-toned casing, metallic retaining ring, recessed blue energy lens and four blue status markers.
- Three custom **16 × 16 pixel-art PNG textures** now supply a graphite casing, brushed metal frame and bright blue plasma emitter (`ancient_projector_housing`, `ancient_projector_rim`, `ancient_projector_emitter`). The model keeps block-model JSON geometry and standard Minecraft texture-atlas lookup. These replace the temporary vanilla blackstone/ice textures.
- Models remain resource-pack JSON; no GeckoLib or runtime entity animation is added to the item.
- The placed block has a matching compact voxel selection/collision shape and emits a modest block light level of 8.
- The handheld, inventory, dropped and frame display transforms are defined in `models/item/ancient_projector.json`. The item should read as a portable high-technology artifact, not a 1-block pedestal.

### Dedicated hologram shader

`HologramRenderType` registers an isolated client shader through Forge 1.20.1's `RegisterShadersEvent`, using `DefaultVertexFormat.BLOCK` to accept the existing PR #12 baked models. It changes **only** the rendering material; it does not duplicate or replace the machine evaluator. The shader resources are:

- `assets/magneticraft2/shaders/core/hologram.json`
- `assets/magneticraft2/shaders/core/hologram.vsh`
- `assets/magneticraft2/shaders/core/hologram.fsh`

The fragment stage adds horizontal raster scanlines, a slowly travelling scan band, and stronger cyan view-angle/silhouette highlights. Source block atlas textures are still sampled and tinted by `HologramBufferSource`; the existing recording timeline provides materialization, dissolve and deterministic signal-dip flicker. The custom render layer uses translucent blending, depth testing, color-only writes and backface culling to limit excessive alpha accumulation. **No physical lights are attached to hologram components; no blocks or entities are created for the recording.**

`appearance.shader_effect` is optional, defaults to `true` and selects the specialized shader. Set it to `false` in a resource-pack recording to return to plain vanilla `RenderType.translucent()`. If the registered shader reference has not loaded, the renderer also selects the vanilla translucent fallback. A shader program that fails resource reload still needs fixing; this runtime fallback cannot prevent an invalid GLSL program from failing the reload itself.

The built-in Mechanical Transmission recording enables the shader:

```json
"appearance": {
  "tint": [0.38, 0.9, 1.0],
  "opacity": 0.32,
  "fade_in_ticks": 24,
  "fade_out_ticks": 36,
  "flicker": 0.28,
  "preserve_texture": true,
  "shader_effect": true
}
```

### Test plan — not yet verified

1. Run `./gradlew build` and `runClient` on Forge 1.20.1; check for Java errors **and** shader compile/link/reload errors.
2. `/give @p magneticraft2:ancient_projector`: inspect in inventory, first-person hand, dropped on ground, and placed on a block. The device should be physically compact with a blue center, not a pedestal/cube.
3. Place the projector in all four horizontal directions. Observe moving horizontal scanlines, a slower scan band and brighter silhouettes on the real wooden gears; gear motion and structural alignment should be unchanged.
4. Stand behind a stone wall; the projection must be occluded by the wall and must never draw as X-ray geometry. Walk around and through the recording.
5. Set `shader_effect: false` in a test resource pack. The same scene should revert to the previous plain translucent material while keeping fades and flicker.
6. Test Fancy **and** Fabulous graphics, night-time lighting, F3+T resource reload, resource-pack replacement textures, and FPS with several nearby projectors.
7. Verify full 45-second playback and the projector item return once. Breaking early, late-join synchronization and other lifecycle cases remain separately unverified.

The current core shader is an **attempt at a cinematic 3D projection look**, not actual volumetric light simulation or order-independent transparency. Shader availability, alpha sorting, graphics-mode differences and texture-pack compatibility require a real in-game test before PR #16 can be merged.

## Archive 017 — Mechanical Transmission film (prototype)

The first recording is now authored as a **45-second non-interactive film**, using the existing PR #12 scene graph. No real gear network runs during playback, and none of these states affect world blocks.

| Tick (time) | Recorded visual |
| --- | --- |
| 0 (0s) | Hologram materializes; intact wooden transmission at rest |
| 75 (3.75s) | Gears begin turning in opposite directions |
| 200–345 (10–17.25s) | Transmission runs at steady load |
| 355 (17.75s) | Brake load is introduced against the right gear |
| 405–460 (20.25–23s) | Machine slows; at tick 460 the right gear loses teeth and the shaft stops under overload |
| 460–600 (23–30s) | Broken gear remains visible, illustrating the failure |
| 615 (30.75s) | Scene briefly fades out and reconstructs with an intact gear and reinforced timber bridge; brake load removed |
| 615–840 (30.75–42s) | Corrected transmission resumes |
| 840–900 (42–45s) | Transmission stops; hologram dissolves and projector returns as an item |

### Data-driven filming extensions

- `visual_channels`: optional mapping of **named integer scene inputs** to stepwise frames. Example:

  ```json
  "visual_channels": {
    "gear_condition": [
      {"tick": 0, "value": 0},
      {"tick": 460, "value": 1},
      {"tick": 615, "value": 2}
    ]
  }
  ```

  Each value is in 0..5, begins at tick 0, and selects one of the corresponding PR #12 `model_variants` options. Up to 16 tracks and 128 keyframes per track. All are fully stateless and deterministic with respect to the recording clock.

- `transitions`: optional brief reconstruction-fade moments, e.g. `{"tick":615,"fade_ticks":12}`. The scene fades to zero at the boundary so replacement geometry is not seen snapping in place, then fades back in. Fade windows are validated not to overlap, are limited to 1..40 ticks per side, and stay inside the recording duration.
- Source wooden gear UVs remain unchanged in the 20-element damaged variant; it has five missing tooth pieces compared with the intact model. The reinforced bridge is a separate model and can be replaced independently by a resource pack. The existing real Mechanical Brake frame and the shared live brake drum/shoe renderer illustrate load; empty model variants contain no geometry.
- No new hardcoded animation geometry or independent rendering engine was introduced.

### Projector drop lifecycle

Timed completion and early removal now share one server-side `onRemove` path. The block entity returns at most one item bearing its `Recording` NBT, while normal block loot returns no extra duplicate. The block entity persists a one-shot return guard; chunk unload does not itself call `onRemove` or create an artifact.

### Still pending

- Local `./gradlew build` and live in-game visual verification of these latest film/return changes.
- Server-side recording duration currently uses a fixed **900 ticks**. The client JSON duration matches this for Archive 017 but cannot yet govern the server's expiration for arbitrary future recordings.
- Visual cue sound effects, additional recording films, measured multiplayer late-join behavior, all four rotations, and frame-time performance profiling.

## Recording reliability and extensibility — current branch update

### Dedicated server recording catalog

The server now loads versioned datapack metadata from
`data/<namespace>/hologram_recordings/<id>.json`. Each definition declares:

```json
{
  "format": "magneticraft2:hologram_recording",
  "format_version": 1,
  "archive": "017",
  "title": "Mechanical Transmission",
  "duration_ticks": 900
}
```

The server accepts **1–12000 ticks**. New archives are created by combining a server datapack definition with corresponding client assets:
`assets/<namespace>/holograms/<id>.json` (visual film) and its referenced
`assets/<namespace>/animations/<scene>.json` plus models. The ID is stored
unchanged in the projector item as `Recording` NBT.

The per-block-entity duration is **latched at placement**, included in update
packets and save data, and never read from client assets for authoritative
completion. An in-progress recording is unaffected by `/reload` changes
to its datapack definition. For compatibility, old projectors without stored
duration retain the original 900-tick default. Unknown/missing server
recordings preserve their IDs but use the legacy duration until a matching
datapack returns. Do not distribute missing-ID artifacts intentionally.

The client receives the server's `startTime` and `durationTicks` through
Minecraft's standard block-entity update tag/packet. It evaluates at:
`sceneTick = (clientGameTime - startTime + partialTick) *
clientVisualDuration / serverDuration`.
This keeps the requested scene shape while allowing server datapacks to
select longer or shorter films. No separate entity-per-component or
per-frame network packets are required.

### Archive-specific tooltip and dimensions

The item tooltip now uses a client-only Forge tooltip event:
- Detects the fixed `Recording` ID, or the default Archive 017 for plain
  creative items
- Retrieves archive number, title, and animation ID from the client recording
  resource rather than hardcoding Mechanical Transmission in the item
- Preserves the partially obfuscated teaser styling
- Computes the rounded-up projected W × H × D from **actual baked models**
  and their JSON offsets, considering all model variants and the radial sweep
  of driven rotations; this is cached per animation and invalidated when
  resources reload

The bound calculation is conservative by design, and complex nested
parented animations/sliding components may need more precise authored
bounds support later. Missing client models display unknown engraving rather
than an invented size. Dimensions are local to the scene and intentionally
exclude the empty offset between the physical projector and projected scene.

### In-game QA scenarios

1. **Ordinary Archive 017:** `/give @p magneticraft2:ancient_projector`;
   verify the film remains 45 seconds and returns one item.
2. **Datapack override:** Change only `duration_ticks` to `1200` in the
   server datapack, `/reload`, place a **new** projector; it should last 60s
   with the same film rescaled. Existing playing projectors keep their
   previously latched duration.
3. **Persistence:** Start playback, save/quit and rejoin; playback resumes
   from the saved start time without restarting or creating another item.
4. **Chunk unload:** Leave range and return; playback follows server world
   time and doesn't spawn a second artifact.
5. **Multiplayer:** Join midway from another client, verify the same visual
   stage and final completion; the client may differ by network-clock latency
   but the server controls lifecycle.
6. **New recording ID:** Add both the server datapack metadata and client
   visual scene in a resource pack; use an item with
   `{Recording:"namespace:archive_id"}`; no Java registry change needed.
7. **Visuals:** Check engraving matches the approximate actual footprint,
   swap a model via F3+T and verify the cache is refreshed, and confirm
   malformed data is logged and skipped safely.

Status: structural verification only until a new Gradle build and the in-game
scenarios pass; PR #16 remains draft.

## Projection visibility and collectible-item preservation

### Frustum culling

Forge 1.20.1 obtains BlockEntityRenderer visibility bounds from the **BlockEntity**'s `getRenderBoundingBox()` (not a method with that signature on the renderer). Projector `shouldRenderOffScreen` is now `false`, allowing normal frustum culling of projects that are entirely outside the camera view. The expanded block-entity bounding box includes the recording's distant scene rather than merely the compact physical projector.

The server recording metadata supports an optional `render_radius_blocks` field, integer **1..64**, with a default of **8**; the value is latched, saved and synced to clients along with playback duration. The installed Mechanical Transmission archive declares `8`. This is a conservative **visibility radius**, not the item's 3D engraved dimensions. Pack authors creating much larger or offset recordings must increase the value so projections aren't culled while still visible. Do not use excessively large radii without a reason; the purpose is to avoid rendering lots of unseen animated holograms.

### The same physical artifact returns

The original ItemStack's **full item NBT** is captured on placement into `ArtifactItemTag` in the projector block entity and restored on normal finish or early break. A canonical `Recording` tag is always rewritten using the block entity's stored recording ID, so arbitrary custom tags cannot switch the permanent archive. This preserves anvil-assigned names and item-defined custom lore, rather than returning a blank new item containing only the archive ID. Older worlds without `ArtifactItemTag` remain compatible: they return a default projector preserving the saved recording ID.

Exactly one return is still coordinated by the existing `itemReturned` guard and shared `onRemove` path. Unloading chunks does not return an artifact or stop the server's world-time-based recording clock.

### In-game tests for this iteration

- Pull current PR #16 and run `./gradlew build`, then place Archive 017 near a wall. Walk around it: the projection should remain visible when the tiny physical disc is outside the camera frustum but the **recorded gears** are in view.
- Look away so both disc and full projection leave the camera. Check F3 or a profiler with multiple active nearby projectors; the off-screen renderer should now be skipped. Compare identical scenarios, not just headline FPS.
- Test all four facing directions; projections must not disappear from incorrect culling bounds.
- In an anvil, name a projector (e.g. `Archive Recovery 017`). Place it, break it early and pick up its drop. The name and selected recording ID must remain. Repeat with a second named projector allowed to finish its 45-second film.
- Save and reload during a named recording; break afterwards. Verify original name/custom lore survives the block entity serialization.
- For a modpack archive that projects farther away, specify a larger `render_radius_blocks` in the server datapack. Verify a previously placed projector retains its own latched radius after `/reload`.

These checks are not yet build/in-game verified in this environment. Keep PR #16 draft; world generation, additional archives, audiovisual cues and Studio authoring remain separate future work.

## Prerecorded audio cues (experimental)

Optional `audio_cues` on the **client** recording JSON allows films to play sparse,
localized audio synchronized with their authoritative server clock:

```json
"audio_cues": [
  {"tick": 8, "sound": "minecraft:block.beacon.activate", "volume": 0.16, "pitch": 1.65},
  {"tick": 355, "sound": "minecraft:block.piston.extend", "volume": 0.28, "pitch": 0.65},
  {"tick": 460, "sound": "minecraft:block.anvil.land", "volume": 0.25, "pitch": 0.65},
  {"tick": 615, "sound": "minecraft:block.amethyst_block.chime", "volume": 0.30, "pitch": 1.35}
]
```

The built-in Mechanical Transmission film has seven cues in total: archive
power-on, transmission engagement, brake load, pre-failure warning,
mechanical failure, reconstruction, and shutdown. These are deliberately
**one-shot** cues; we are not playing an endless gear loop that would
become noisy with several active projectors.

- JSON validation: 0..32 entries; cue ticks `0 <= tick < duration_ticks`, no
  duplicate ticks; sound IDs must be valid resource locations;
  `0 < volume <= 0.65`; `0.5 <= pitch <= 2.0`.
- Sounds resolve through the client's installed Forge sound registry. Missing
  events are logged and skipped, never silently replaced by a different
  machine sound. Resource packs can provide sound definitions/overrides.
- Audio is ticked **once per client game tick through the projector BE**, not
  once per rendered frame. Existing hologram geometry, shader and
  synchronized animation remain unaffected.
- The first observation of a projector begins *silently at the current
  point in the film*. This prevents a late joiner or newly loaded chunk
  from replaying sounds from 20 seconds ago.
- Sound playback is local, positional and within 24 blocks of the player's
  client; it creates no server entities and sends no per-event network
  packets. Cues while the listener is outside range are intentionally skipped.
- Clock jumps, resource reloads, rewind and invalid server durations reset
  or skip cue playback to prevent bursts. At most one sound is submitted per
  projector per client tick, including strongly time-scaled datapack recordings.

### Test this audio pass

1. `./gradlew build` and place Archive 017. Listen for the quiet startup
   chime, engagement, load, warning, break, rebuild, and shutdown cues at
   approximately 0.4, 3.75, 17.75, 21.5, 23, 30.75 and 42 seconds.
2. Watch a projector while facing away. Audio should continue while the
   chunk is loaded, independent of whether the full model is on screen.
3. Reload resource packs (`F3+T`) during playback and join from another
   client midway. Historical sounds must not replay as a burst.
4. Leave hearing range and return later; old events must not catch up.
5. Change server `duration_ticks` to `1200`, `/reload`, place a new
   projector: cue moments should scale to the 60-second playback.
6. Place several projectors. The short effects may overlap naturally;
   no cue should be emitted every frame.

Not yet compiled or tested from this environment. Sound levels are
intentionally conservative and may need an in-game balance pass.

## Visual-event tracks and large Archive 042 (prototype)

### Generic client-side event format

Recording resource packs can now add an optional `visual_events` array,
independent of the animation model's normal rotation and selection channels:

```json
"visual_events": [
  {
    "tick": 575,
    "duration_ticks": 90,
    "interval_ticks": 10,
    "kind": "warning",
    "position": [0.5, 1.35, 2.6],
    "count": 7,
    "spread": 0.62
  },
  {
    "tick": 680,
    "duration_ticks": 12,
    "interval_ticks": 4,
    "kind": "spark",
    "position": [0.5, 1.4, 2.6],
    "count": 10,
    "spread": 0.45
  },
  {
    "tick": 929,
    "duration_ticks": 36,
    "interval_ticks": 9,
    "kind": "reconstruct",
    "position": [0.5, 1.4, 1.55],
    "count": 9,
    "spread": 0.8
  }
]
```

The effect kinds are a deliberately **closed whitelist**:
`spark` emits a brief cyan outward spray; `warning` produces a low,
warm-colored stress ring; `reconstruct` sends a few cool particles
upward. Each supports an optional `color: [red,green,blue]` with
normalized components. `position` is in the same scene-local block
coordinates used by the animation, then transformed through the recording
placement offset and the physical block's horizontal facing. There are no
physical entities, actual sparks, powered wires, block updates, or game logic.

This implementation supports up to 24 event definitions per recording.
Events must start at a tick >= 1 and end before the film duration, repeat
for at most 120 ticks with an authored 4–40 tick interval, emit 1–10
particles per pulse with spread 0..1.5 blocks, and specify a location
within +/-64 blocks per axis. The emitter enforces a **24 particles per
projector/client tick** ceiling and rejects cues beyond 32 blocks from
the viewer's **actual projected event position**. Late joins, resource
reloads, clock jumps and unloaded chunks skip historical events. No
continuous high-frequency flashing or separate game-entity render loops.

Archive 017 demonstrates three small effects at its existing stress,
gear failure and reconstruction moments. Its 45-second storyline and
gear animations remain unchanged. Its former invented brake-jaw
stand-in has been replaced by the **existing Mechanical Brake Y-axis
frame** and a driven Wooden Shaft model, positioned on the right gear's
vertical output axis.

### Archive 042 — Multi-Stage Power Distribution

The larger second recording is a 60-second (1200 tick) engineering film,
not a replacement for Archive 017.

- Resource pack film: `assets/magneticraft2/holograms/power_distribution.json`
- Reusable visual scene: `assets/magneticraft2/animations/hologram_power_distribution.json`
- Authoritative server metadata:
  `data/magneticraft2/hologram_recordings/power_distribution.json`
- Example film for pack authors:
  `docs/examples/hologram-recordings/power_distribution.json`

The assembly has **eight genuine 8-tooth wooden medium gears** in a
T-shaped distribution network and **one genuine 16-tooth large wooden
input gear**, all on the **same horizontal gear plane**. A five-gear
cross-shaft branches into a three-gear output. Its nine gear centers
are now aligned to the actual Minecraft 1x1 placement grid. The large
input gear connects diagonally by one block in X and one in Z, exactly
as `GearPlacementValidator.isValidExternalMeshOffset` requires, and
rotates in the opposite direction at half its driven neighbor's speed.
The original raised drive-wheel tower and its two long shaft components
were **removed** following the 28 October screenshot showing a
floating-looking assembly. The large gear is now supported by an actual
Mechanical Input Module, like all eight medium gears. The existing
Mechanical Brake frame remains on the rear branch, while the real
up-facing Overload Disconnect is installed **directly on the rear
output gear's shaft**, not four blocks into the air. There is no
fabricated gantry or extra brake shaft overlapping the real brake's
drum. The repaired branch can show the existing Flywheel frame.
The footprint is roughly seven blocks wide and five deep, but no longer
has the excessively tall projected tower.

The film starts the gear assembly, applies load to the real brake
frame on the rear branch, and highlights stress with amber particles
and warning audio before showing gear-tooth failure. The real
**output-mounted Overload Disconnect opens** as the gear train stops.
The image reconstructs an intact rear gear and supporting frame
while the disconnect returns to its closed model, before the
distribution train resumes. Eight medium gears rotate with alternating
signs and matched phases. The larger input gear has the correct
opposite-direction 8T:16T ratio. This is a **recorded illustration**
of mechanical behavior, not a real simulation.

The original custom brake-jaw models (`power_overload_jaw` and
`transmission_brake_load`) were **deleted** after reviewing Magneticraft2's
existing mechanics. This film reuses the actual models
`mechanical_brake_wood_frame_y`, `shaft_wood`,
`overload_disconnect_wood_open` and `overload_disconnect_wood_closed`.
**No `power_*` custom models remain in the active scene**.
The dedicated `power_pier`, `power_gantry_*`, `power_stress_outline`
and `power_reinforced_hub` model assets have now been deleted.
The recording reuses actual Copper Age models plus only the
`hologram/gear_medium_damaged` mesh (a modified original wooden gear)
and the empty projection placeholder for model variants. None are new
placeable gameplay blocks. The stress effect is timed amber particles,
not a fake "warning block".

**Shared procedural parts (current):** The real brake's wooden drum,
leather-lined shoes and copper clamp arms are now projected by invoking
`MechanicalBrakeBlockEntity_woodRenderer.renderProcedural` from the PR #12
scene renderer at the exact authored component pose. The same drawing method
is invoked by an actual placed Mechanical Brake, supplied with its real
block entity rotation and braking state. For holograms, the film provides
the rotation and engagement window instead. This uses the same geometry and
real textures, not a fake brake block, cloned machine model, or additional
entity renderer. The dedicated hologram RenderType binds the brake's
individual texture PNGs rather than treating their UVs as block-atlas UVs.
**Compilation and in-game positioning/texture verification are still pending.**

Seven visual-event tracks, ten low-volume audio cues, three discrete
visual channels and one reconstruction transition are provided in this
second film.

### How to test Archive 042

This mod targets Forge **1.20.1**, so the old-style item NBT /give
syntax applies:

```mcfunction
/give @p magneticraft2:ancient_projector{Recording:"magneticraft2:power_distribution"} 1
```

- Look at the artifact tooltip: it should show **Archive 042** and
  larger model-derived dimensions than Archive 017, retaining the
  scrambled inscription.
- Place it in the open with at least an 8x6 block area. Confirm all
  eight medium gears and the ninth, **large input gear**, sit on the
  same level. Check that the large gear meshes diagonally with the
  westmost medium gear and rotates **in the opposite direction at
  half the speed**. Each gear should visibly sit on a genuine
  Mechanical Input Module, without an unrelated tall shaft tower.
- The real Mechanical Brake frame and shared procedural drum/shoes
  should sit on the rear branch. The Overload Disconnect should be
  mounted directly above the **rear output gear**. At ~27 seconds the
  brake loads, amber warnings begin near ~29 seconds, and around
  ~34 seconds the damaged gear and cyan sparks appear while the
  output disconnect switches to its open model and the drive stops.
- At ~46.5 seconds the assembly should fade/reconstruct with stronger
  bearing supports, then restart; the final fade completes at 60 seconds,
  returning **exactly one** projector containing Archive 042.
- Walk around the projection and verify normal world occlusion, every
  facing, relative particle positions, and FPS against Archive 017.
- Join a multiplayer world halfway through playback: no backlog of
  sound/particle events should fire. Repeat with particles set to Minimal,
  F3+T resource reload, pause/save/reload, and early break.
- If the film is not visible, check the client log for baked
  animation/model errors (especially the native `_y` overload disconnect models).
  The one-time hologram model diagnostic now logs the exact model ID if a
  baked quad uses the missing-texture sprite.

**Verification status:** source and JSON integration only until the
user's `./gradlew build` and in-game test pass. PR #16 remains a draft.

### Archive 042 screenshot follow-up

The first test screenshot showed too much translucent gantry geometry
and a possible purple/magenta missing-texture/model region. The 27 October
cleanup removed nine unnecessary scaffold components, retaining only
the connected rear/right L-shaped supports. Because the purple patch
might also be unrelated background geometry, its source **has not
been proven**. The next in-game run should inspect that area with the
projector isolated from other test blocks. If it persists, check
`latest.log` for `Unable to load model`, `missing model`,
`MissingTexture`, or `hologram_power_distribution` warnings before
guessing which resource is involved.

A separate render-quality pass may still be needed: translucent faces
blend with the surrounding hologram, so a visually complicated object
can be hard to decipher even when its models are technically correct.

### Archive 042 — second screenshot corrective pass

A second screenshot showed a **textbook purple/black missing-texture
checkerboard** in the raised drivetrain and the supporting scaffolding
still read as invented gameplay blocks. The follow-up on 27 October:

- **Removed every custom `power_*` model reference** and deleted seven
  unused `power_*` model assets from the branch. Eight authentic
  `block/mechanical_input_module` models now form the lower shaft
  supports rather than `hologram/power_pier`. The elevated frame,
  bearings and brake are likewise existing Magneticraft2 block models.
- Replaced the previously rotated horizontal
  `overload_disconnect_wood_closed/open` models with the correct
  *already-existing up-facing* `overload_disconnect_wood_closed_y`
  and `overload_disconnect_wood_open_y`. This is a scene-only change
  and does not modify any player machine blocks.
- Added explicit standalone baking registration for the real block
  models referenced by the recording; `AnimationResourceLoader` keeps
  its generic JSON model scan so third-party archives remain possible.
- Added a **hologram-only missing atlas sprite fallback**: a projected
  vertex sampling Minecraft's missing-texture checkerboard instead
  samples the neutral white-concrete sprite, keeping a cyan hologram
  while logging a warning. Normal source textures and UVs are not
  altered, and actual machine block rendering is untouched.
- Replaced the custom repaired bearing hub with the real
  `flywheel_wood_frame_y` baked model. Removed the custom bearing
  outline model, retaining amber stress particles and sound cues.

The original screenshot does not conclusively identify *which* model
was failing, so this pass fixes the native vertical-model selection,
ensures its models are baked, and adds a fallback. If the geometry
still looks like a cube, save `logs/latest.log` and inspect which model
reported an atlas warning.

**Tests:** `./gradlew build`; re-run Archive 042 and check the purple
checkerboard is gone, the eight gear shafts visibly meet the existing
Mechanical Input Module supports, the raised disconnect's red
indicator is legible, the repair frame fits the branch, and no
hologram-only gantry appears. Verify 34-second overload and 46.5-second
reconstruction, four facings, F3+T, FPS, Archive 017 regression and
the one-item return. Neither the Java code nor the new scene have
been independently compiled or observed in-game from this environment.

### Scene-relative audio positions (optional)

Audio cues now accept an optional `position` in the **same scene-relative
coordinate system** as `visual_events`. This is important for larger
archives: a mechanical failure sound should originate at the failing
machine, not at the small artifact placed several blocks away.

```json
{
  "tick": 680,
  "sound": "minecraft:block.anvil.land",
  "volume": 0.35,
  "pitch": 0.7,
  "position": [0.5, 1.4, 2.6]
}
```

The optional `position` is `[x,y,z]` in full Minecraft block units relative
to the recording's canonical south-facing scene; each coordinate must be a
finite number within +/-64 blocks. The projection placement offset and
the projector's facing are applied by the **same utility used by
holographic sparks and stress indicators**. This avoids particles being
on one side of a machine while sounds come from the other.

Without `position`, cues remain backwards-compatible: they play at the
physical projector's center. Sounds are checked against the listener's
24-block hearing radius using their **actual world-space source**, not the
projector's origin. Existing cue limits, time scaling, and skip-on-late-join
behavior remain in effect.

Archive 017 positions its brake and repair cues at the small mechanism;
Archive 042 positions the mechanical-load, break and reconstruction cues
near their filmed components. Startup and shutdown remain centered
on the artifact. **Real in-game positional audio checks are still needed.**

### Client-visible metadata isolation

The projector preserves the full `ArtifactItemTag` on the **server** so
anvil names, custom lore and non-archive tags survive early breaking or
normal completion. Client `getUpdateTag()` and block-entity update
packets now omit `ArtifactItemTag` and `ReturnedArtifact` entirely.
The packet receiver consumes only recording ID, start time, duration
and render radius, preventing a partial network update from overwriting
the saved collectible metadata. Chunk load still receives sufficient
playback fields.

### Recording/animation cross-file validation

A syntactically correct client film JSON can still refer to a missing
animation, a mechanical input not present in `channels`, or a
`model_variants` source whose stepwise `visual_channels` track is
missing. A model stage may exist for one component but not another
component sharing the same track.

`HologramRecordingValidator` now checks these cross-resource
relationships before cue playback and scene rendering. Invalid pairings
are logged by recording ID and rejected rather than producing an
unpredictable partial film. Checks are cached by immutable loaded
resource definitions and recalculated when client resources reload;
the same constraint definitions should eventually be used by the
standalone recording builder, which remains **out of scope** for PR #16
until the runtime is reliable.

The test data for Archives 017 and 042 has passed structural
cross-file validation. That does **not** replace `./gradlew build`
or manual tests of model baking and rendering.

### Reusing live machine geometry — real Mechanical Brake

A JSON recording can now opt into a small, strictly validated list of
procedural visual attachments. Currently the only allowed type is the
actual `magneticraft2:mechanical_brake_wood` renderer:

```json
"procedural_parts": [
  {
    "component": "brake_load",
    "type": "magneticraft2:mechanical_brake_wood",
    "axis": "y",
    "angle_channel": "mechanical_input",
    "angle_scale": -1,
    "visible_channel": "brake_condition",
    "visible_stage": 1,
    "clamp_from_tick": 355,
    "clamp_until_tick": 615
  }
]
```

- **component:** existing component name in the PR #12 animation scene.
  Geometry is emitted at that component's fully evaluated pose, so model
  offsets, parent transforms, pivots and projector facing are respected.
- **type:** explicitly whitelisted real machine visual. JSON cannot load
  arbitrary Java classes, create block entities or alter game mechanics.
- **axis:** `x`, `y` or `z`, just like a placed Mechanical Brake.
- **angle_channel:** real recording angle input, with bounded
  **angle_scale** between -8 and +8 (not zero).
- **visible_channel / visible_stage:** optional pair restricting the
  attachment to a particular discrete visual stage (Archive 017 uses
  `brake_condition=1`). If omitted, the attachment draws throughout
  the film (Archive 042's stationary frame).
- **clamp_from_tick / clamp_until_tick:** half-open interval when the
  real shoes close against the rotating wooden drum. The drum's
  rotational angle always follows the recorded phase, independent of
  the brake's load state.

Maximum eight procedural attachments per recording. Invalid types,
unsupported axes, missing components, unrecognized angle channels,
unavailable stages and invalid clamp intervals are rejected. The live
brake itself still draws through the same geometry routine using its
actual mechanical states.

Because the live brake uses separately bound PNG textures (oak planks,
leather belt and copper), procedural holograms have a **textured projection
render layer** with the recording's familiar depth-tested transparency,
blue tint and optional scanline shader. This layer is cached per texture
and does not change normal machine renderers.

Both Archive 017 and Archive 042 now use this hook. The 017 brake drum
clamps while braking is shown (~17.75–30.75s), whereas 042's shoes clamp
from ~26.5–46.5s during the load/failure portion. This is visually
authored playback only, not simulated brake torque.

**QA:** Rebuild, check the actual brake block still draws and animates
normally, then compare its drum and shoes with those projected in Archives
017 and 042. Verify all four facings, original wood/leather/copper textures,
correct clamp motion, no floating geometry, hologram transparency,
performance and F3+T reload behavior. Build and live verification
are not claimed yet.

### Archive 042 — real gear-plane correction (28 October 2026)

The latest screenshot after shared brake rendering showed the projector
working without the earlier purple/black checkerboard, but the elevated
large wheel, tall drive shaft and Overload Disconnect formed an awkward,
floating-looking tower detached from the lower gear train.

The scene now follows the **existing gear placement rules**, not a
hologram-only invented connection: an 8T medium and 16T large gear mesh
diagonally one block across X and Z and rotate in opposite directions,
with a **1:2 angular-speed relationship**. The large 16T gear was moved
to the lower plane and given a real `mechanical_input_module` support.
All eight medium shafts, their real supports, the large gear and the
output disconnect were snapped to integer X/Z locations. The former
upper shaft components were removed. The native up-facing Overload
Disconnect is now on the rear output shaft, at the same X/Z as that
gear, one block above it. The unnecessary extra shaft drawn *inside*
the real procedural Mechanical Brake was removed, avoiding doubled
overlapping wood surfaces.

Audio cues and colored visual events were adjusted to the new gear
positions. The source definitions still have the same duration and
failure/recovery schedule; only the arrangement and scene-relative
effect coordinates have changed.

**Acceptance tests:** the nine gears must look physically connected,
particularly the diagonal large-to-medium mesh. Confirm all parts are
on supported real module shafts; the brake and output safety device
must not hover; verify all horizontal facings, shared brake visuals,
relative sound/sparks, no missing-texture checkerboard and sensible FPS.
The archive should still recover at 46.5s and drop one item after 60s.

This layout and JSON passed a static reference/grid/gear-rotation check.
It has **not** been built or tested in Minecraft by the assistant.
