# JSON animation runtime — Java milestone 1

**Base branch:** `feature/bronze-age-foundry` (Bronze PR #11)
**Feature branch:** `feature/json-driven-animation`
**Status:** first implementation; requires Gradle build and in-game verification.

## Scope

The existing Bronze Mechanical Lathe's input rotor is the pilot. Its
`getInputRotation(partial)` still obtains the mechanically accurate interpolated
Gear V2 angle, including network stop and overload behavior. The renderer now
evaluates a typed rotation track from
`assets/magneticraft2/animations/bronze_mechanical_lathe.json`.

The first definition describes one component, one module-backed input,
and one driven rotation track. A reload listener parses and validates client
resource-pack JSON once per reload. Definitions are published atomically as
immutable maps. Missing/invalid definitions leave the original Java input-rotor
transform in place. Other Lathe parts, Ore Washer, Sifter, belt texture movement,
item rendering, models, server mechanics and Voxel V2 are unchanged.

Resource-pack override location:
`assets/<namespace>/animations/<name>.json`
(for this pilot, namespace and name must remain
`magneticraft2:bronze_mechanical_lathe`).

## Animation v1 pilot contract

- `format_version: 1` and `coordinate_system: { units: "blocks", origin:
  "controller", reference_facing: "south" }`.
- `components.<name>`: baked `model` resource location, `model_offset`
  and `pivot`, each a three-number vector in block units.
- `inputs.<name>`: `source: "mechanical.visual_angle_degrees"` and
  named `module: "mechanical_input"`.
- `animations.<name>`: `type: "driven"`; `tracks` contain `target`,
  `property: "rotation"`, `input`, `axis` (one principal axis)
  and `scale`.
- Axes are in the model's **canonical south-facing local frame**, before the
  machine's parent facing transform; positive degrees follow Minecraft's
  PoseStack right-handed rotation. The client is not an authority for
  mechanical simulation.
- The initial evaluator accepts a single matching rotation for a component,
  uses typed data, and does not evaluate expressions or user-supplied code.
  Future animation types require explicit new parser/evaluator support.
- Model IDs referenced by animation JSON must already be registered for
  baking via Forge's `ModelEvent.RegisterAdditional`. This pilot reuses the
  already registered Lathe input rotor; hot-loading a previously unknown
  model is not supported.

## Manual verification

1. On a checkout of this feature branch, run `./gradlew build`. Do **not**
   declare it compiling until the complete build passes.
2. Load an existing world and form the Bronze Mechanical Lathe in all four
   orientations. Verify its input rotor and outer gearbox have the same
   orientation, position and rotation as before.
3. Apply an actual Gear V2 drive. Compare rotor angle with other connected
   machines. Stop, overload, reverse, reconnect and vary speed. The rotor
   must not free-run from a made-up client animation timer.
4. Apply a resource pack with `scale: -1` in the pilot definition, then use
   F3+T. Only the pilot input rotor's direction should invert.
5. Apply a pack with an invalid `format_version`, an unknown source,
   missing component or bad axis. The log should identify the rejected JSON;
   the old Java rotor animation should still be visible.
6. Remove the pack and reload. Confirm the original direction returns,
   including when standing near chunk boundaries or after world rejoin.
7. Compare FPS and render bounds with the base Bronze PR. Confirm no
   magenta missing-model textures or texture distortion.
8. Verify no machine logic, structure, static V2 geometry or voxel collision
   changes and that the Copper Washer/Sifter continue to render unchanged.

## Limitations

This is intentionally not a complete scene graph, procedural geometry system,
keyframe editor, or generalized constraint solver. The first renderer call
site passes the actual angle and a known module name. Definition lookup and
immutable track access should allocate no per-frame JSON structures. The
resource loader compiles only on reload. Studio's `native-linux` branch is
not changed here.

**Validation performed:** inspected source and created the GitHub branch and
commits. `./gradlew build` and Minecraft gameplay tests were **not run** in
the current environment (repository download and dependencies unavailable).
