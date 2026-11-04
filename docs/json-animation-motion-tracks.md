# Reusable mechanical motion tracks — JSON animation v1 extension

**Branch:** `feature/json-mechanical-motion-tracks`, child of
`feature/json-driven-animation` (draft PR #12).

This milestone adds **general phase-offset rotation** and **bounded
Gear-driven linear translation** to the existing typed animation scene
graph. The Bronze Stamp Mill's 3 rotating cams and 3 up/down hammers are
the first production consumer.

## Why it matters

Previously we could rotate wheels/shafts via JSON and solve one specific
Sifter X/Z slider-crank. The Stamp Mill still hardcoded:
- three shaft-driven cams, at 0°, 120° and 240°;
- three vertically guided hammer heads with
  `0.32 * (0.5 + 0.5 * cos(theta + phase))` blocks of travel;
- a separate client visual routine calculating dust and ore tremor from
  its original Java hammer formula.

After this change, **cam models, axes, center pivots, angular phases,
hammer models and lift profiles** all come from one scene JSON.
`MachineVisualRenderer` still supplies the real Gear V2 output shaft
angle, synchronized processing/wear states, and the correct facing.
No extra clock, server-side physics, or machine NBT is created.

## Rotation with phase offset

```json
{
  "target": "cam_middle",
  "property": "rotation",
  "axis": "x",
  "input": "output_phase",
  "scale": 1,
  "phase_degrees": 120
}
```

Computed local rotation: `angle = shaft_angle * scale + phase_degrees`.

Existing rotation tracks with no `phase_degrees` remain exactly as
before (default phase = 0). Principal axes `x/y/z/-x/-y/-z` are
supported. For a negative axis, the parsed phase and scale are both
negated; this is not a hidden second rotation.

## Straight-line motion from a real mechanical phase

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
  "phase_degrees": 120,
  "when": "running"
}
```

For `theta = shaft_angle * scale + phase_degrees`:

```text
displacement = distance * (bias + amplitude * waveform(theta))
```

Supported `waveform` values are `cosine` and `sine`, calculated in
degrees. Movement is along a fixed principal axis **in the parent
component's local frame**, before any local static/driven rotation.
A component can combine a translation and a rotation track.
Translation itself never stretches the baked geometry or its texture UVs.

`when` uses existing engine states:
`always`, `running`, `stopped`, `overloaded`, `worn`. The state
precedence is overload, worn, running, stopped, always. If no matching
translation track exists, travel is **zero**. This is why the Stamp
Mill's hammers return to their idle position while a still-powered shaft
can continue turning the cams.

The parser enforces:
- input and target must refer to known validated scene entries;
- valid finite `scale` (absolute <= 128) and phase (absolute <= 3600°);
- finite `distance` (absolute <= 16 blocks);
- finite `bias` and `amplitude` (each absolute <= 4);
- maximum theoretical offset `abs(distance) *
  (abs(bias) + abs(amplitude)) <= 16` blocks;
- no duplicate same-property/condition track on a component;
- no double-driving a rigid slider-crank role with an unrelated track.

Animation input names remain aliases mapped by
`machine_visuals/<machine>.json` to **real** Java-synchronized inputs.
The schema cannot create new torque, processing steps, item outputs, or
a functional steam/hydraulic power connection.

## Stamp Mill model contract

The existing 18-part V2 powertrain component IDs are unchanged. This milestone adds
nine more animation components:

| Station | Cam | Hammer | Wear plate | Phase |
| --- | --- | --- | --- | --- |
| Left | `cam_left` | `hammer_left` | `plate_left` | 0° |
| Middle | `cam_middle` | `hammer_middle` | `plate_middle` | 120° |
| Right | `cam_right` | `hammer_right` | `plate_right` | 240° |

The cam pivots, hammer guide positions and strike plate surfaces exactly
match the old `BronzeStampMillRenderer` transforms:
- upper cam shaft Y = 38.5/16 blocks, Z = -10/16;
- hammer rest Y = 24/16 blocks and guide Z = -18/16;
- lower wear plate Y = 23/16;
- input Gear V2 phase and output bevel reversal are still provided by
  the block entity, not re-simulated in the renderer.

`stamp_mill.wear_stage` is exposed as a typed integer from the existing
replaceable hammer part. For stages 2..5 the JSON selects the original
worn hammer model, preserving the same damaged geometry as Java did.
The machine does not gain or lose a replacement part.

**The ore impact FX now also use the exact evaluated JSON hammer travel**:
the generic renderer evaluates translation once per frame, exposes it in
`SceneResult.translationOffset(componentName)`, and the existing ore
visualization reads it. When JSON fails, that path uses the original
`BronzeStampMillBlockEntity.getHammerLift` formula instead. This keeps
dust/tremor synchronized if a resource pack shifts a cam/hammer pair.

**Backward compatibility:** The previous `json-animation-multiblock-pack`
example contains only the old **18** Stamp Mill powertrain components.
That pack continues to render its customized JSON drivetrain, while the
original Java renderer supplies the three complete cam/hammer/plate
stations and their ore FX. A new **27**-component scene instead renders
the complete mechanism through JSON. A partially defined head set is
rejected *before drawing*, to avoid duplicated or unconnected heads.

The module's structural V2 meshes, Gear V2 behavior, leather belt
UV-tiling renderer, inventory ports, recipe progress, collision,
rotation sign and ore positions are otherwise unchanged.

## Test pack

`docs/examples/json-stamp-mill-cam-pack/` replaces the middle cam with
a **gold** parent model and the middle hammer (including worn variants)
with a **red** model. It changes **both** middle cam and middle hammer
phases from 120° to 180°, proving their appearance and synchronized
relative phase can be altered without modifying Java.

Enabling the pack must not cause the leather loop or shafts to jump.
The cams still turn with the real Gear V2 axle. The hammers move only
when `running` is active. Invalid/missing JSON or models should
fall back to the original Java powertrain **and** all three hammer heads.

## Verification

```bash
node tools/animation/check-motion-tracks.mjs
node tools/animation/check-machine-visual-bindings.mjs
node tools/animation/audit-multiblock-coverage.mjs --require-mechanical-json
node tools/animation/check-sifter-slider-crank.mjs
node tools/animation/check-mechanical-drives.mjs
./gradlew build
```

The new fixture checks **5772 angle/facing samples** across three
stations, compares the original hammer lift and cam phase exactly,
checks the track and model coordinates, and verifies original wear
variants. The 27 component model references were also inspected.

**Not yet proven:** Forge compilation, in-game mesh and cam contact,
resource-pack reload, reverse-drive and overload behavior, effect
timing in-game, culling and FPS. This child PR remains **draft**.

## What this does NOT yet support

This milestone introduces bounded sinusoidal/cosine cam-style linear
motion. It does **not** implement general hand-keyframed motion curves,
spline interpolation, physically constrained arbitrarily shaped cams,
custom hinged rods or procedural UV-scroll belts.

The Stamp Mill leather belt remains on its existing carefully
UV-correct, continuously unwrapped Java renderer. A future belt track
needs a typed continuous **surface-distance** input and a reusable
loop-geometry/tiling renderer; merely rotating a belt model would stretch
or detach the leather. That should be a separate tested extension, not
a claim of support in this PR.
