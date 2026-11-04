# JSON-driven visual belt loops — no-stretch UV pilot

**Branch:** `feature/json-driven-belt-loops` (draft child of PR #12,
`feature/json-driven-animation`). **Forge 1.20.1**.

The reusable `open_belt` visual loop is authored in
`assets/<namespace>/animations/<scene>.json` under the optional
`belt_loops` field. The Stamp Mill is the first opted-in machine.

This is **a client-only representation of a real mechanically driven
belt**. The feature does not create a functional Gear V2 connection, a
new belt collider, item conveyor, fluid input, or server power system.

## Example: Stamp Mill

```json
"belt_loops": {
  "drive_belt": {
    "type": "open_belt",
    "input": "output_phase",
    "texture": "magneticraft2:textures/block/bronze_stamp_mill_drive_belt.png",
    "start_center": [2.95, 0.5, -0.625],
    "end_center": [2.95, 2.40625, -0.625],
    "rotation_axis": "x",
    "start_radius": 0.2978125,
    "end_radius": 0.2978125,
    "drive_radius": 0.2978125,
    "half_width": 0.0496875,
    "half_thickness": 0.0428125,
    "texture_repeat": 1.0,
    "speed_scale": 1.0,
    "texture_offset": 0
  }
}
```

All dimensions are in **blocks**, controller-relative in the existing
canonical SOUTH orientation. `input` names a validated animation scene
angle input whose `module` is mapped by the machine's visual manifest
to a real Java-supplied mechanical input.

- `start_center`, `end_center`: the centers of two pulleys
- `rotation_axis`: one positive principal axis (`x`, `y`, `z`)
- `start_radius`, `end_radius`: **actual visual belt centerline radii**,
  not an imaginary extra clearance. BeltPath's built-in 0.03-block
  clearance is corrected once before generating the render geometry
- `drive_radius`: pulley radius converting angular input to moving
  surface distance; defaults to `start_radius`
- `half_width`, `half_thickness`: one-half the physical leather width
  and thickness, not UV values
- `texture`: client texture resource location, checked for existence
  during resource reload (it may come from another mod's resource pack)
- `texture_repeat`: the physical block length along the belt occupied
  by one full 0..1 UV image, default machine uses one block
- `speed_scale`: signed multiplier of visual texture travel, default 1
- `texture_offset`: static **distance in blocks**, not an angular offset

`BeltPath.createVisualSegments` is called exactly once per definition
during **resource-pack reload**, not per render frame. This preserves the
accepted open-pulley tangents and arc tessellation.

## How continuous motion remains aligned

The existing Stamp Mill keeps a **per-block-entity, weak-keyed,
per-input cumulative shaft phase tracker**, rather than repeatedly
mapping wrapped 0..360° angles directly to texture coordinates.

On each rendered frame, the tracker measures the signed shortest angle
delta between the previous and current interpolated Gear V2 phases.
The angular total is stored as a **double** to avoid precision loss
over long sessions. It is converted into surface distance:

```text
distance_blocks =
    radians(cumulative_shaft_degrees) * drive_radius * speed_scale
    + texture_offset
```

That distance is reduced modulo `texture_repeat`, and the loop is
split at exact UV tile boundaries. Forward, reverse, stop and 0°/360°
crossing use one continuous phase. After F3+T loads a different scene,
the old tracker is reset instead of retaining incompatible texture
phases.

## Why the texture does not stretch

The old Stamp Mill leather renderer already tiled its texture in **true
physical distance**. The new `AnimatedBeltLoopRenderer` retains those
same four-sided prism sections, same paths and same UV tiling method.

Long straight runs **never map a single image across their entire
length**; each is subdivided at the next repeat boundary. Narrow-width
faces get proportional V crops rather than a complete 16x16 texture.
If the pack changes repeat length, the cross-strip V density scales
with it so the texture is not compressed on the thin faces. Small
floating-point errors near a seam are normalized to zero to avoid
accidental UV ranges of 1..2.

The default Stamp Mill scene exactly reproduces the prior dimensions:
center radius `(4.08 + 5.45)/32`, half-width
`(8.72 - 7.13)/32`, half-thickness
`(5.45 - 4.08)/32`, and 1-block texture repeat.

## Compatibility and safety

- Up to **8 loops** per animation definition, with a combined **512
  estimated segment/tile budget** and a 192-per-loop budget to prevent
  pathological per-frame geometry costs.
- Finite and bounded radii, width, thickness, pulley spacing, speed,
  repeat, and texture offset. No invalid / empty BeltPath loops.
- The texture resource must exist in the active client's resource stack.
  Invalid or missing textures reject the animation during reload.
- Old **18-part** Stamp Mill and PR #14 **27-part** resource packs without
  a `belt_loops` object still draw the **original Java belt**, not an
  invisible/missing belt. If the scene fails to load, full legacy moving
  geometry and belt fallback remain available.
- JSON paths affect only the **client model**. Changing belt width or
  position does **not** change collision, drive ratios, machine ports,
  workload or item transport. Actual Gear V2 mechanics are unchanged.
- The `AnimatedBeltLoopRenderer` is reusable for other machines, but
  the **Stamp Mill is the only integrated pilot** in this child PR.
  The Ore Washer's wear-aware belt still uses its existing renderer and
  is deliberately NOT migrated yet.

## Visual resource-pack test

`docs/examples/json-belt-loop-pack/` now uses a **separate
machine-visual manifest redirect** to select its own animation scene at
`magneticraft2:demo/bronze_stamp_mill_blue_belt`, rather than replacing
the shared default `animations/bronze_stamp_mill.json` file. This is
important: the earlier gold-cam/red-hammer test pack and the old
four-machine pack also override that default file. If they took precedence,
they would mask the belt test completely, making the belt look unchanged
despite the new pack being enabled.

The revised diagnostic pack sets:
- an **emerald-green upper pulley model** as a distinct sign that
  the demo animation scene and its model registry were selected correctly
- `minecraft:textures/block/light_blue_wool.png` instead of leather
- slightly broader `half_width: 0.0625`
- `speed_scale: -1`, which **intentionally reverses the visual texture**
  relative to the real pulley rotation, demonstrating JSON motion control
- a `texture_offset: 0.125` block seam displacement

The blue belt and reversed moving texture are **not** the production
default. Enable the pack, F3+T, inspect all four directions, reverse
the real drive, and disable/reload to see the original leather return.

## Verification

```bash
node tools/animation/check-belt-loop-fixtures.mjs
node tools/animation/audit-multiblock-coverage.mjs --require-mechanical-json
node tools/animation/check-motion-tracks.mjs
node tools/animation/check-machine-visual-bindings.mjs
./gradlew build
```

In Minecraft, inspect texture repeat on long straight runs and curved
wraps without stretching, signed UV travel across a 360° phase wrap,
resource-pack reload, all four machine facings, missing texture fallback,
and FPS with one and several running Stamp Mills.

**Not yet confirmed:** Forge compilation, real-client load/reload,
live texture motion/UVs, and the full-facings/performance checks.
Leave the PR draft until the developer tests it.
