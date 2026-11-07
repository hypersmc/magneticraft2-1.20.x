# Iron II-B — Wrought Iron Shaft and Right-Angle Gearbox

**Forge 1.20.1 · Draft PR #18 · Implementation source, not in-game accepted.**
**Multiplayer-specific work and tests explicitly deferred until after Steam Age.**

## Scope and actual mechanical effect

| Component | Gear V2 capacity | Mechanical behavior |
| --- | ---: | --- |
| Wooden Shaft (unchanged) | 16 torque | Existing axial 1:1 drive |
| **Wrought Iron Shaft (new)** | **24 torque** | Same axial X/Y/Z 1:1 drive, directional placement |
| Wooden Right-Angle Gearbox (unchanged) | 8 torque | Existing reversible two-port 1:1 / 90° transfer |
| **Wrought Iron Right-Angle Gearbox (new)** | **16 torque** | Same reversible two-port 1:1 / 90° transfer |

The **real immediate transmission bottleneck** is the wooden **gearbox's
8-torque ceiling**. The new Iron gearbox can pass up to 16 **only if**
the source and *every intervening component* supports that load.
Wooden medium gears still cap at 8; wooden shafts and the Mechanical
Input Module already cap at 16; the large water wheel supplies up to
16 depending on flowing water. The Iron Shaft's 24-capacity structural
rating is *future headroom*, **not proof of higher usable output**
from currently available sources.

Neither block generates power, creates a new gear ratio or changes
the existing Gear V2 propagation logic.

## Existing code reused, without new architecture

- Shaft block, shaft BE and client rendering inherit/delegate from
  `ShaftBlock_wood`, `ShaftBlockEntity_wood` and
  `ShaftBlockEntity_woodRenderer`. The new block/BE types have
  separate registry IDs and their own 24-torque rating.
- Right-angle gearbox block, BE and renderer reuse the existing
  `GearboxBlock_wood`, `GearboxBlockEntity_wood` and existing
  bevel renderer geometry. This preserves INPUT/OUTPUT port
  swapping, axis-change direction, neighbor connections, server
  ticks, network removal and save compatibility.
- The wooden gearbox renderer takes opt-in material overrides.
  The **wooden default still uses oak and its previous minimum
  texture-window behavior**. The Iron version draws existing bevel
  gears/port shafts with `dark_work_iron` and bearing supports
  with `bronze_machined`. Its UV mapping now follows true face
  dimensions rather than stretching a 2-texel minimum across tiny
  metal parts. Static iron gearbox housing reuses the wood frame
  layout with corresponding pixel-density-correct texture mapping.
- No duplicate per-frame model assembly, new entities or separate
  render framework. Iron shaft rotation shares the proven baked
  state renderer. No models from the player-accepted Iron medium
  gear are changed.
- Neither source nor old block entity types, old recipes or existing
  world data are replaced. The only existing Java changes add
  protected constructors/helper access and optional renderer material
  configuration.

## Survival recipes and material costs

- **Iron Shaft:** 1 rolled Wrought Iron Bar + 2 Precision Bronze
  Bearings → 1 Wrought Iron Shaft (crafted).
- **Iron Gearbox:** 2 Iron Shafts + 2 Machined Iron Gears
  (`precision_iron_gear_medium`) + 4 Wrought Iron Plates +
  1 Precision Bronze Bearing → 1 Iron Right-Angle Gearbox.

The two machined gear items represent fitted gearbox input/output
gearing, while the existing shared renderer supplies its established
bevel-gear visual assembly. Exact bevel tooth cutter profiles are
not separately represented as items in this slice; do not claim a
new miter-gear machining process exists. More specific bevel
components would require an agreed later machine/tooling design.

These are survival crafting-table recipes. **Input material quality
NBT is not propagated into crafted BlockItems**, matching the
current Iron Gear implementation; no quality-dependent torque boost
or service life system was silently introduced.

## Manufacturing design boundary

The shaft currently uses a rolled iron bar and **two genuinely machined
precision Bronze bearings**. It is an attainable preliminary iron drive
shaft, not proof that the Bronze Lathe now supports general wrought
shaft turning; that separate `machined_iron_shaft` item remains a
conditional later refinement if a real tolerance/wear need emerges.

The gearbox recipe's two precision machined iron gears are treated as
source parts for the proven in-world bevel mechanism; a dedicated
bevel-gear manufacturing workpiece was **not** introduced in this
slice. Its material and mechanical output remain provisional until
survival testing and visual gear meshing are accepted.

## First in-game visual feedback and targeted correction — 2026-11-07

The player's in-game screenshot shows two **brown/wood-looking exposed
Iron Shaft ends** and an Iron Gearbox whose internals appear wooden
inside a dark iron cage. This was not acceptable visual evidence
for an Iron Age drivetrain.

Source-level root causes: the Iron Shaft's top/bottom faces used the
Bronze bearing texture as a *full end cap*; the gearbox's entire
floor used Bronze, and its original Wood gearbox renderer covered
the full-width structural cross-braces plus bearing cheeks with
Bronze. These are excessive material applications, not evidence
that the Gear V2 network needs changing.

**Focused correction:** The shaft sides and exposed ends use
`dark_work_iron` at native face UV size. The right-angle gearbox
keeps its existing stable 13-cuboid housing but changes the floor
to dark iron; the shared renderer now draws load-bearing braces
and bearing retaining cheeks in Iron for the metal variant while
leaving only the supported center bearing in Bronze. The Wooden
Gearbox continues to use its original oak/wood textures and UV
sampling. The existing Iron medium gear is not altered.

**Limitations:** This corrects the demonstrably bad texture
assignments. The external gearbox's **frame shape still derives
from the established wooden model**; a bespoke housing may be
appropriate later after the player accepts the present geometry.
It would be inaccurate to claim the whole gearbox has a new
3D model or realistic machined bevel tooth form. The screenshots
do not prove every orientation or motor behavior. Check the result
from several angles while rotating and idle.

## Iron shaft cross-section alignment — 2026-11-07

Player screenshot identified a **real geometric mismatch**: the
Wrought Iron Shaft was still 6×6 Minecraft model units (copied
from the wooden shaft), but the new Iron Medium Gear has a **2×2
through-axle** (model bounds [7,0,7] → [9,16,9]). The gearbox
renderer likewise had a 6×6 outer coupler inherited from wood.

**Correction:** Wrought Iron Shaft now has a 2×2 cross-section at
every axis, with X/Y/Z outline **and collision** boxes matching
the model. Its existing single-piece JSON uses unscaled, native
2×16 side-face UVs and 2×2 end-face UVs. The already accepted
Iron Medium Gear model and existing wooden shaft remain unchanged.

The existing gearbox renderer now exposes explicit axle and
coupling thicknesses to its Iron-only call site: **0.125 blocks**
(2 model pixels) for both. Original wooden gearbox axle (0.105)
and wide 0.375-block (6px) wooden coupling remain unchanged,
as does the previous four-argument constructor behavior.
This is purely an installed geometry alignment; network nodes,
ratios, max torques, materials, registry IDs and saved data are
untouched.

**In-game acceptance:** place Iron Shaft into an Iron Medium Gear
on X, Y and Z; both static and powered geometry should meet at
the same 2×2 cross-section. Connect through the Iron Gearbox's
two perpendicular outputs, inspect the actual shaft and coupling
junction, highlight/collision and rotation from multiple angles.
Check the wooden parts have not become narrow or changed behavior.

## Single-player acceptance checklist

- [ ] Run `node tools/iron-age/check-iron-shaft-gearbox-contract.mjs`.
- [ ] Run `./gradlew build` with JDK 17; preserve actual output.
- [ ] Craft both items in survival; confirm JEI, Patchouli, item
      model, pickaxe harvesting, drops and creative placement.
- [ ] Place Iron Shaft along X/Y/Z; confirm its new narrow 2×2
      model AND voxel/collision outline match the Iron Medium Gear
      axle. The original wooden shaft remains 6×6.
- [ ] Test Wooden Shaft → Iron Shaft → Iron Medium Gear and back,
      including stopped, reverse and stalled/overloaded states.
- [ ] Check that Iron Gearbox coupling and Iron Shaft both have a
      coherent 2×2 junction while the wooden coupling remains 6×6.
- [ ] Place Iron Gearbox with all practical perpendicular port
      pairings. Right-click to cycle OUTPUT and sneak-right-click
      with empty hand to swap INPUT/OUTPUT. Verify 1:1 rotation
      and handedness in both flow directions.
- [ ] Compare Iron vs Wooden Gearbox in identical networks. The
      node limit should report **16 vs 8**, but greater load must
      be tested with a real >8 consumer once one exists.
- [ ] Verify a Wooden Medium Gear (8), old wooden gearbox (8) or
      underpowered water wheel still limits the network as before.
      Iron Shaft (24) alone must not increase source output.
- [ ] Save/reload active Iron connections; break, replace and
      reorient both blocks; test Gear V2 stale-node recovery.
- [ ] Inspect cage/shaft/bearings and bevel-gear mesh from multiple
      angles when idle and rotating, X/Y/Z orientations, correct
      texture density and no floating geometry or intersections.
- [ ] Compare FPS with the same wooden network; no assumed
      performance result from code reuse.
- [ ] Regression: old Wood Shaft/Gearbox placement, swapped ports,
      medium gears and previous Bronze/Iron machining/heat chains.

**Verification distinctions:** Static source/JSON inspection can check
registration, UV dimensions, model shape and inherited APIs. It
does **not** demonstrate compilation, actual fluid/water-wheel power,
successful real Gear V2 network simulations, saved-world behavior or
in-game rendering. Leave PR #18 draft and unmerged until instructed.
