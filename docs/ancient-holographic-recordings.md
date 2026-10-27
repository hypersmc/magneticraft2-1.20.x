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
