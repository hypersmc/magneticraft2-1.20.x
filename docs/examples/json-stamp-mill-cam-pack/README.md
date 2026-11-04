# Stamp Mill — generic phase-offset cam & hammer test pack

**Requires:** `feature/json-mechanical-motion-tracks` (draft PR #14), based
on `feature/json-driven-animation` (draft PR #12). Forge 1.20.1.

This is a RESOURCE PACK. It does not alter the Bronze Stamp Mill's power,
recipes, inputs, collision, block placement or actual mechanical load.

### In-game visual acceptance

Zip the **contents** of this directory so `pack.mcmeta` is at the
archive root. Place that ZIP in `run/resourcepacks/`, enable it, and
reload with **F3+T**. Form a powered Bronze Stamp Mill with a valid recipe.

**Default machine (no pack):**
- Three cams turn at 0°, 120°, 240° along the existing shared shaft.
- Each corresponding hammer's lift comes from a Gear V2 phase-driven
  `distance * (bias + amplitude * cos(angle + phase))`.
- The normal values remain 0.32 blocks, bias 0.5, amplitude 0.5.
- Idle hammers are at the bottom of their guide rails, but a powered shaft
  can continue rotating the cams regardless of whether an ore is being
  processed.
- Wear stages 2..5 use the existing worn hammer model.

**With this pack:**
- The **middle cam** becomes gold.
- The **middle hammer** becomes red, including its worn variant.
- Both middle cam and hammer switch to **180°** phase, instead of
  the original 120°. The left/right phases do NOT change.
- The two middle parts must move together: no cam-to-hammer phase drift.
- The ore impact dust and ore tremor read the **same evaluated JSON hammer
  travel** as the visible hammer. They must not trigger at the old 120° phase.
- No shaft should stall, gain torque or change its real power behavior.
- Disable the pack, F3+T again: the original three-way 120° stagger and
  material colors should return.

### Schema sample: phase-driven linear movement

```json
{
  "target": "hammer_middle",
  "property": "translation",
  "axis": "y",
  "input": "output_phase",
  "scale": 1,
  "waveform": "cosine",
  "distance": 0.32,
  "bias": 0.5,
  "amplitude": 0.5,
  "phase_degrees": 180,
  "when": "running"
}
```

This is a **linear motion in the parent's coordinate frame**. It reuses
the real output shaft angle, not independent world time or a fake 20 Hz
client clock. Conditional tracks default to zero travel when they are not
active; the stopped hammer returns to its original rest position.

Rotation tracks also accept `phase_degrees`:

```json
{
  "target": "cam_middle",
  "property": "rotation",
  "axis": "x",
  "input": "output_phase",
  "scale": 1,
  "phase_degrees": 180
}
```

`sine` is also supported as a translation waveform. Both types use
finite and bounded numerical inputs; the loader rejects invalid values,
duplicate same-property/condition tracks and unsupported axes. A rejected
scene should render the complete original Java mechanisms instead.

### Regression / important limits

1. All four Stamp Mill facings (N/E/S/W), including powered shafts in
   both directions. No disconnected parts, duplicate cams or missing
   textures.
2. All six wear stages; change the worn hammer while powered. The red test
   model should keep the original damaged geometry.
3. Stop processing while leaving the gearbox powered. The cams should
   continue their visual shaft phase but hammers should park at their
   rest position. This deliberately matches the existing behavior.
4. Resource reload with pack enabled and disabled. Check for frame-rate
   regressions from 27 model parts plus ore effects.
5. Delete the `animations/bronze_stamp_mill.json` definition or replace
   `waveform` with an invalid value, and verify complete legacy Java
   rendering appears rather than partial geometry.
6. The leather belt still uses the proven existing UV-tile belt renderer.
   It is NOT converted into a generic track in this PR, to avoid UV
   stretching and falsely claiming a new belt primitive exists.

For numeric checks, run
`node tools/animation/check-motion-tracks.mjs` from the repository root.
This is not a substitute for `./gradlew build` and in-game testing.
