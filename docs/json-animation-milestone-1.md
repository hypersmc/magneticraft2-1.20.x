# JSON animation runtime — Java milestone 1: resource-pack model overrides

> **Update — scene graph milestone:** This document records the original
> single-shaft pilot. The branch now includes multiple components, a parent
> hierarchy, typed machine states and a cross-repository Studio export bridge.
> The **current** format, migration behavior, test pack and acceptance matrix
> are in [json-animation-scene-graph.md](json-animation-scene-graph.md).
> Earlier statements below that only one rotor is supported are historical.

**Base branch:** `feature/bronze-age-foundry` (Bronze PR #11)
**Feature branch:** `feature/json-driven-animation`
**Status:** source implementation; `./gradlew build` and Minecraft acceptance tests outstanding.

## Goal

Prove that a modder can replace **both the animated model and its motion**
using a normal client resource pack, with no change to Java or Gear V2.

The Bronze Mechanical Lathe's **input rotor** is the first opt-in component.
Other Lathe visuals, Washer, Sifter, the Studio, multiblock construction,
mechanical load calculations, and Voxel V2 do not change.

## Contract and ownership

- The Java machine continues to supply the true interpolated mechanical angle
  from `BronzeMechanicalLatheBlockEntity.getInputRotation(partial)`.
- `assets/magneticraft2/animations/bronze_mechanical_lathe.json`
  describes named model components and their typed animation tracks.
- Every new animation document should declare
  `"format": "magneticraft2:visual_animation"`. The identifier is separate
  from integer `format_version: 1` and prevents Magneticraft2 from
  accidentally treating other mods' unrelated animation files as ours.
  Previously authored animation JSON without `format` remains supported
  when it uses the documented Magneticraft2 fields.
- The **component name** `input_rotor` is the stable interface between
  the Java machine and the resource pack. The model resource location in the
  definition is **not** hardcoded in the generic renderer.
- `model_offset` and `pivot` are **controller-relative block coordinates**,
  in the canonical south-facing frame. The default rotor is at
  `[0, 0, -2]` (its input module sits two cells behind the controller), and
  rotates about the model-local point `[0.5, 0.5, 0.5]`.
- The machine's parent transform rotates all content for NORTH/WEST/EAST
  exactly once, matching the existing V2 renderer conventions.
- The initial typed `mechanical.visual_angle_degrees` input is sampled in
  Java. No arbitrary JSON scripting, extra clocks, or authority transfer.
- The generic renderer resolves `component.model` through `ModelManager`,
  and uses the component's `render_layer` (`solid`, `cutout`, or
  `translucent`; default `cutout`). The renderer draws it after applying
  `T(model_offset) T(pivot) R(axis, angle * scale) T(-pivot)`.
- If the animation definition, required input, track or baked model is
  missing/invalid, the old hardcoded Lathe input rotor render path is used.

## Model discovery and hot reload

`ModelEvent.RegisterAdditional` loads the animation definitions from the
**current resource pack stack** and registers every validated component
`model` ID **before Forge bakes models**. This is separate from the normal
JSON reload listener, so the first bake and F3+T do not depend on a
*previous* generation of compiled definitions. A pack may reference
`magneticraft2:animated/...`, `othermod:custom/...`, or another valid
resource location provided by the enabled pack stack.

The client reload listener validates and publishes an immutable definition
map on reload. Model discovery and reload use the same ownership test:
all `magneticraft2`-namespace files in this directory are validated;
other namespaces are accepted only if they declare the Magneticraft2
`format` identifier or contain all four legacy fields
(`coordinate_system`, `components`, `inputs`, `animations`).
Unrelated GeckoLib `animations/*.animation.json` resources are ignored
silently rather than logged as invalid. Missing baked-model warnings are
limited to once per model per reload; the renderer never loads or parses
JSON each frame.

**Important:** An external pack must include the actual model file at
`assets/<model_namespace>/models/<model_path>.json`. A JSON animation
reference does not synthesize geometry. The animation asset itself stays
client-side; gameplay and collision definitions are server/datapack concerns.

## Working example pack

A test pack is checked in under
`docs/examples/json-animation-lathe-pack/`. To try it, copy that **directory**
to Minecraft's `resourcepacks` folder (or zip its contents with
`pack.mcmeta` at the zip root), enable it in Resource Packs and press F3+T.

The pack supplies:
- `assets/magneticraft2/animations/bronze_mechanical_lathe.json`:
  keeps `input_rotor` and the real Gear V2 input, but selects
  `magneticraft2:animated/lathe_gold_input_rotor`, and sets `scale: -1`.
- `assets/magneticraft2/models/animated/lathe_gold_input_rotor.json`:
  inherits the original rotor geometry but switches the metal textures
  to gold for a visually obvious change.
- `pack.mcmeta` for Minecraft 1.20.1 (resource pack format 15).

**Why `models/animated`?** The mod's old `Clientsetup` only scans
`models/multiblock` in addition to explicitly registered model IDs.
This example deliberately places its NEW model outside that scanned folder
to verify that the JSON-driven registration works.

## Manual validation matrix

1. Check out the feature branch and run `./gradlew build`. Do not claim
   compilation until it passes.
2. With **no test pack**, form the Bronze Mechanical Lathe facing SOUTH,
   NORTH, WEST and EAST. Verify the input rotor and its gearbox connection,
   angle, pivots and textures are unchanged compared with Bronze PR #11.
3. Power it from an actual Gear V2 drive. Stop, overload, reverse, and alter
   RPM; inspect the shaft and output for consistent real mechanical phase.
4. Enable the provided test pack: the input rotor should use the **gold**
   model and rotate with reversed sign, without altering the output rotor,
   frame, machine logic, or other instances in the world.
5. Remove the pack and press F3+T; the original rotor model and direction
   should return. Re-enable it and press F3+T again. Verify there are no
   missing-model cubes or stale baked models after the second reload.
6. Override `model_offset` and `pivot` in a copy of the test pack; confirm
   its model physically moves and the pivot changes without Java changes.
7. Override the pack's animation model ID with a nonexistent
   `magneticraft2:animated/not_present` path, reload, and inspect logs.
   Only one missing-model warning per reload is expected. The **original**
   Java rotor should render, not a pink-and-black missing-model cube.
8. Corrupt `format_version`, `axis`, `render_layer` or `input` in a
   separate copy, reload and confirm an actionable error and the same
   Java fallback.
9. With GeckoLib enabled (as in the normal developer environment), check
   that loading and F3+T do **not** print `Rejected animation geckolib:`
   or `Cannot register animated models from geckolib:`. Check that
   `Loaded 1 Magneticraft2 JSON animation definitions` is still logged.
10. Verify the model/animation across chunk boundaries, rejoining the world,
   with two differently oriented Lathes simultaneously, and around a
   vanilla resource pack reload.
11. Compare FPS and render bounds with PR #11. Verify the Copper Washer,
    Sifter, Bronze Stamp Mill, classifier, static V2 geometry, saved voxel
    collision and recipe processing are unchanged.

## Current limits / next milestones

This pilot covers one connected input rotor, one typed rotation track,
and a JSON-specified baked model. It does **not** migrate the entire Lathe
or support keyframes, model hierarchies, slider-crank constraints,
independently animated meshes, or generic binding of *new* machines to
animation definitions. A new machine still needs a renderer to opt in
to a stable component ID; the per-part geometry is no longer hardcoded.

Next: verify this resource-pack contract in Forge, then widen to a
scene-graph/component registry and integrate the Studio's `native-linux`
exporter using the same stable component names and coordinate system.

**Validation performed in this environment:** connector-side source and
resource inspection and GitHub commits. The full repository and ForgeGradle
dependencies are inaccessible from the execution container; `./gradlew build`
and live Minecraft rendering have **not** been run.
