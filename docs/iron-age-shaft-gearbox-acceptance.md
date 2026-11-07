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

## Single-player acceptance checklist

- [ ] Run `node tools/iron-age/check-iron-shaft-gearbox-contract.mjs`.
- [ ] Run `./gradlew build` with JDK 17; preserve actual output.
- [ ] Craft both items in survival; confirm JEI, Patchouli, item
      model, pickaxe harvesting, drops and creative placement.
- [ ] Place Iron Shaft along X/Y/Z; compare placement/collision
      and angular direction against existing Wooden Shaft.
- [ ] Test Wooden Shaft → Iron Shaft → Iron Medium Gear and back,
      including stopped, reverse and stalled/overloaded states.
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
