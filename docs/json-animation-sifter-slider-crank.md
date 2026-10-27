# Sifter JSON slider-crank animation — Forge 1.20.1

**Branch:** `feature/sifter-json-slider-crank`, based on
`feature/json-driven-animation` (animation PR #12), which is based on
`feature/bronze-age-foundry` (Bronze PR #11).

**Status:** Merged into animation PR #12. The developer has visually confirmed
that the Sifter's test-pack crank/rod mechanism cycles in Minecraft and works
in all four compass directions. The known wooden-frame clipping is accepted
and deferred. The numerical fixture passed, but the fresh full Gradle build,
malformed-pack fallbacks and performance checks are not independently verified.


## Why this is a different milestone

The first animation pilot controlled an already-baked Lathe rotor's model,
pivot and Gear V2-driven rotation. The Sifter tests a **physical constraint**:
one input shaft angle must determine the crank pin, a rigid connecting rod,
the follower and the reciprocating sieve carriage. Independent sine animations
would cause the rod to stretch, detach, or pass through the follower.

There was already a functional inline Java implementation of this exact
mechanism in `MechanicalSifterBlockEntityRenderer` and
`MechanicalSifterBlockEntity.getShakeOffset`. This milestone preserves the
existing geometry and motion while moving visual bindings and dimensions
into client JSON:

`assets/magneticraft2/animations/mechanical_sifter.json`

The Sifter is opted into the same `magneticraft2:visual_animation` schema
and generic resource-pack renderer as the Lathe.

## Solver definition

```json
"mechanisms": {
  "sieve_drive": {
    "type": "slider_crank",
    "input": "drive_angle",
    "rotation_axis": "x",
    "slide_axis": "z",
    "center": [0.925, 0.5, 0.5],
    "radius": 0.12,
    "rod_length": 0.4375,
    "phase_degrees": 0,
    "parts": {
      "crank": "sieve_crank",
      "rod": "sieve_rod",
      "follower": "sieve_follower",
      "slider": "sieve_carriage"
    }
  }
}
```

For this milestone, the typed solver supports **X rotation / Z slide only**,
matching the real Sifter. Do not silently reinterpret the axes. Future
mechanisms may extend the solver with separately tested orientations.

Given input shaft angle θ, eccentric radius r, rod length L, and shaft center
(cx, cy, cz):

- Crank pin Y = cy + r cosθ
- Crank pin Z = cz + r sinθ
- Rod angle about X = asin(r cosθ / L)
- Slider displacement Z = r sinθ + sqrt(L² − (r cosθ)²) − L
- Follower endpoint Z = cz + L + slider displacement
- Follower endpoint Y = cy

`L > r > 0` is required, and the rod is rigid for every frame.

All four mechanism roles are **root components** in canonical SOUTH local
coordinates. The generic renderer applies their solver pose before their
normal JSON local offsets, rotations, and children:

- `crank`: translate to shaft center; rotate about local +X
- `rod`: translate to eccentric pin; rotate rigidly around +X
- `follower`: translate to the constrained rod endpoint
- `slider`: translate along +Z by the computed displacement

The model geometry remains standard Minecraft baked models with real UVs.
No texture stretching, dynamic model baking or per-frame JSON parsing.

## Carriage, tilted tray, replaceable mesh and item motion

`sieve_carriage` is a transform-only joint receiving the slider displacement.
Two model components are parented to it:

- `upper_tray`: includes a **static** 10° +X tilt around
  `[0.5, 0.78, 1.2]` in the child's coordinate system, plus a typed
  model-variant selector `sifter.wear_stage` (0..5). Stage 0 uses the
  normal `upper_tray` model; stages 1..5 reuse the existing
  `mechanical_sifter_upper_tray_wear_N` baked models.
- `slide_shoes`: travels horizontally with the carriage but does **not**
  inherit the tray's tilt, so its shoes remain seated on the fixed guide rails.

The Java controller samples the existing Gear V2 visual angle ONCE per render
frame and supplies actual `running`/`stopped`, `overloaded`, `worn`, and
`sifter.wear_stage` values. Its server-side recipe progress, maintenance
rules, item handling, multiblock layout and collision are untouched.

The JSON renderer returns the same solved **slider displacement** to the
Sifter's item renderer. The input workpiece that lies on the moving sieve
follows its configured stroke. The stationary fines pan and guide rails stay
on their existing Java rendering path.

## Moddability / resource pack test

The project contains a self-contained resource pack at
`docs/examples/json-animation-sifter-pack/`. Copy this directory to
`run/resourcepacks/`, or zip its *contents* with `pack.mcmeta` at the root.

The pack:
- replaces the wooden/crank metal texture with a **gold crank** model,
- replaces the wooden connecting rod with a **red** model,
- sets `phase_degrees: 90` for the whole mechanism, not for a single part.

If the JSON pipeline works, the crank, rigid rod, follower and tray must shift
phase together while preserving the contact points and the exact rod length.
A non-90° phase can be tested by editing that single JSON value and pressing
F3+T. **Do not change `radius` without also changing the crank model's
actual physical pin offset**, or its rendered pin and the mathematical pin
will disagree. The solver will keep the rod rigid, but it cannot dynamically
rebuild a crank mesh.

Malformed geometry definitions, unsupported axes, component cycles, missing
baked models, invalid selectors and mismatched input module names are rejected.
The Sifter then falls back to the **original** Java crank + tray + wear +
item-motion renderer. Static blocks, collision and machine logic are not
affected.

## Fast numerical checks

```bash
node tools/animation/check-sifter-slider-crank.mjs
node tools/animation/check-scene-fixtures.mjs
```

The first test checks **481 shaft angles** from −720° to +720°, a rigid
rod/follower endpoint, the old formula's stroke, all wear model references
and the test pack's phase shift. These are data/math fixtures, **not a Java
compilation substitute**.

## Minecraft acceptance checks before merge

1. `./gradlew build` passes on the child branch.
2. Default unmodified Sifter faces SOUTH, NORTH, EAST and WEST. The crank
   pin visibly attaches to the rod; the far rod pin stays on the follower,
   and the slide shoes remain seated on the stationary guide rails.
3. Powered Gear V2 drive: both ±RPM directions, speed changes, zero speed,
   overload and resumptions. No independent animation clock or frame jumps.
4. Sifter processing vs idle: the sieve may continue shaking when input
   is powered, but processing items must be positioned relative to the actual
   shaking tray. No item teleporting or incorrect chute destinations.
5. Test all six wear stages: the mesh deterioration remains visible, no
   duplicate mesh or missing-texture cubes, and replacing the mesh resets
   the correct visual.
6. Enable the red-rod/gold-crank resource pack, press F3+T, and confirm that
   **all constrained parts** change phase together. Disable and reload.
7. Deliberately break `rod_length` (`<= radius`), `rotation_axis`, parent
   chain, required model or driver; the log should explain the error and the
   full original Java motion should be visible.
8. Compare FPS with the base Sifter, multiple formed Sifters, chunk
   unload/reload, and simultaneous Bronze Lathe animations.
9. Verify the old gold Lathe resource pack still behaves as before.

No world save/NBT format, server-side collision, multiblock datapack,
recipe, quality/yield or machine wear rules are modified.
