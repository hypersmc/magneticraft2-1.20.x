# Magneticraft2 machine visual packages — resource-pack contract v1

**Branch:** `feature/json-driven-animation` / draft PR #12
**Scope:** CLIENT visuals for existing machine types (Forge 1.20.1)

This is the layer between **a real Java machine** and **its customizable
model / animation scene**. It lets resource packs select a completely
different scene, as opposed to merely overriding the default scene under
its fixed animation ID.

## Three independent files

For an existing Java-backed Primitive Grinder, the client looks up:

1. **Machine visual manifest**:
   `assets/magneticraft2/machine_visuals/primitive_grinder.json`
2. **Animation scene**, selected by the manifest:
   `assets/magneticraft2/animations/primitive_grinder.json` by default,
   but it may point to `my_pack:machines/grinder_restyle`.
3. **Baked model(s)**, selected by the scene's named components:
   e.g. `assets/magneticraft2/models/multiblock/primitive_grinder_rotor.json`,
   or an arbitrary resource-pack model ID.

Minecraft's standard pack priority selects each JSON file. Model
registration scans the **current** resource stack before baking, so models
outside the old `models/multiblock` folder can be referenced.

### Machine visual manifest example

```json
{
  "format": "magneticraft2:machine_visual",
  "format_version": 1,
  "animation": "magneticraft2:primitive_grinder",
  "angle_channels": {
    "drive_shaft": "mechanical_input"
  },
  "integer_channels": {}
}
```

The left side `drive_shaft` is a **visual alias** the animation JSON uses.
The right side `mechanical_input` is a physical, interpolated value the
Primitive Grinder's Java renderer really supplies. An animation input can
then declare:

```json
"inputs": {
  "drive": {
    "source": "mechanical.visual_angle_degrees",
    "module": "drive_shaft"
  }
}
```

The alias name is not baked into Java; the manifest can map several visual
aliases to the same actual shaft, and select a different animation scene by
its resource location. The renderer receives the real synced angle,
`running`/`stopped`/`overloaded`/`worn` state flags and, if available,
typed integer inputs such as a mesh wear stage.

An integer model-variant selector is likewise an alias:

```json
"integer_channels": {
  "mesh_condition": "sifter.wear_stage"
}
```

and the selected animation's component uses
`"model_variants": {"source": "mesh_condition", "models": {"1": "..."} }`.
This works ONLY if the Java machine actually supplies
`sifter.wear_stage` in its frame.

The current implementation exposes **six machines** this way: the
Bronze Lathe, Sifter, Primitive Grinder, Ore Washer, Stamp Mill and Rotary
Classifier. Their Java renderers determine the actual available channels
and provide original-renderer fallbacks.

## What is independently customizable?

- Scene ID, component names/hierarchy, and additional decorative nodes.
- Each moving model's baked model ID, texture references, position, pivot,
  layer and static local orientation.
- Rotation axis, signed ratio and state-specific tracks.
- Typed driver aliases connected to the machine's actual Gear V2 visual
  phases, not a separate clock.
- Real machine states, mesh wear variants, and the Sifter's exact X/Z
  slider-crank mechanism. Multiple components may share one real phase.
- **Static-only scenes** with model components and no tracks, if the machine
  renderer opts into the scene (legacy fallback still applies).

Static formed V2 geometry still lives in the machine's per-cell baked
models. The manifest does not replace the multiblock formation JSON,
voxel collision or the existing V2 model generator.

### Existing machine vs entirely new gameplay machine

A resource pack can **replace/extend visuals for an opted-in Java machine**.
It cannot invent physical item ports, water connections, heat/pressure inputs
or power from JSON alone.

Examples of unavailable binding behavior:

- `"drive_shaft": "steam_pressure"` on the Primitive Grinder is rejected
  because its Java frame has no `steam_pressure` angle.
- `"mesh_condition": "fictional_wear"` fails unless the renderer supplies
  a corresponding integer value.
- A valid machine manifest referring to an absent/invalid animation scene
  falls back to the machine's original Java moving parts. Other specialized
  drawing remains active.

This prevents a visual mod from silently altering the server's
authoritative mechanics or creating client-only functional states.

## Lifecycle and safety

- Forge's `ModelEvent.RegisterAdditional` registers all validated animation
  model references from the current pack stack **before baking**.
- `AnimationResourceLoader` compiles scenes; `MachineVisualResourceLoader`
  compiles manifests. Both publish immutable snapshots during client resource
  reload. Neither parses JSON each frame.
- `MachineVisualRenderer` resolves the selected scene and aliases only
  **physical inputs supplied by the active Java renderer**. It preflights
  angle and integer bindings before drawing. The generic scene renderer
  checks visible models and constraint inputs. Failed scenes return `null`
  before emitting animated geometry, so legacy Java fallbacks work.
- Resource references are parsed as ResourceLocations; JSON does not
  evaluate scripts or arbitrary expressions. GeckoLib animation files in the
  shared `animations` directory are still ignored.
- The original parent-facing transform remains applied exactly once by
  the machine renderer. JSON components are authored in canonical SOUTH,
  controller-relative **block** units.

## Test pack: alternate Grinder scene

A standalone test pack is in
`docs/examples/json-machine-visual-binding-pack/`.

It overrides the Grinder manifest to select an animation at an entirely
different ID (`magneticraft2:demo/primitive_grinder_restyle`), maps
`rebranded_shaft -> mechanical_input`, loads a new gold rotor model from
`models/animated/`, and sets that rotor's visual multiplier to 0 while the
axle still rotates. Disabling the pack restores the original model and
direction; the actual machine never stops receiving real Gear V2 power.

This is a test of **machine -> scene selection -> input alias -> model
resolution**, not a proposal to freeze the real Grinder in normal gameplay.

## Verification

```bash
node tools/animation/check-machine-visual-bindings.mjs
node tools/animation/audit-multiblock-coverage.mjs --require-mechanical-json
node tools/animation/check-scene-fixtures.mjs
node tools/animation/check-sifter-slider-crank.mjs
node tools/animation/check-mechanical-drives.mjs
./gradlew build
```

Then verify in game:
1. All six default machines still render as before, including all previously
   tested Sifter orientations.
2. Enable the new Grinder test pack; verify gold/static rotor and rotating
   input axle. The physical processing remains unchanged.
3. Press F3+T and disable/re-enable the pack. The selected animation scene,
   model and input alias must update without stale geometry.
4. Replace `mechanical_input` with a nonexistent physical input; confirm
   a warning and the old Java fallback instead of a missing-model cube.
5. Remove the selected animation JSON or reference a nonexistent model;
   verify fallback before partial rendering.
6. Recheck old gold Lathe and red-rod Sifter packs, then the Washer,
   Stamp Mill and Classifier. No model duplication or new frame-rate spikes.

**Known limitations:** This is not a general machine gameplay scripting API
or complete animation authoring toolkit. Steam pressure, heat, fluid-level
and material flow are not yet general typed animation drivers. Complex
belts, ores, piston cam lifts, flames and fluid surfaces retain their Java
helpers; external packs cannot currently override their entire behavior
with arbitrary animation tracks. Oversized models may exceed existing
client block-entity render bounds. Studio's native editor currently has an
animation exporter but not a finished timeline/viewport rig preview.

**Build status:** The new manifest binding layer and sample pack are committed,
but the full Forge build and in-game validation for it have **not yet run**.
Keep PR #12 draft until those checks pass.
