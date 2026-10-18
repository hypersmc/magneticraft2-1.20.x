# Bronze Stamp Mill — implementation contract

Status: **Bronze II prototype on PR #11**, pending Gradle build and in-game verification. See [Bronze industrial plan](bronze-age-production-plan.md) for progression.

## Purpose

2 Chalcocite Ore or 2 Cassiterite Ore -> 2 corresponding crushed ores (115 ticks; 24 RPM minimum; torque 6). Each batch independently has 20% chance of 1 Gravel tailing. Existing Grinder remains a low-power fallback; ore still proceeds to the existing Washer and Sifter.

## Full-size module integration: 5 x 3 x 4

**All I/O modules occupy complete Minecraft blocks and retain their original, reusable wooden/copper item-port models.** None are fillers or decorative geometry. To avoid floating hoppers and keep item access clear, all three ports now sit side-by-side at **ground level on an open front service shelf**, one block south of the controller. Each has a side/front face exposed to the world for players and transport automation.

The former 5x3x3 draft with three ports in the raised front wall is obsolete. A second draft buried the ports inside the stone-brick foundation; that was rejected because they could not be accessed. The authoritative layout is now 5 wide, 4 deep, 3 tall (rotated to 4 wide, 5 deep, 3 tall for east/west). The core Stamp Mill frame retains its original 5x3x3 section and adds a service shelf with a matching bronze/timber canopy and **three physical chutes** linking the crushing trough to the hoppers.

South blueprint, each row is 5 blocks west-to-east, rows appear rear-to-front, and layers are bottom-up:

```text
Y=0   S S I S S    S S S S S    S S C S S    S U Y O S
Y=1   P P P P P    B _ _ _ B    B _ _ _ B    B _ _ _ B
Y=2   B P P P B    B _ _ _ B    B P P P B    B P P P B
```

S = Stone Bricks; P = Oak Planks; B = Cut Copper; C = Stamp Mill Controller; I = Mechanical Input Module; U = Item Input; Y = Byproduct Output; O = Primary Output; _ = AIR. Build by Patchouli **Visualize**, which stores its dimensions transposed relative to the raw JSON. Do not copy JSON Z rows directly into Patchouli's X columns.

Blueprints: `src/main/resources/data/magneticraft2/multiblocks/bronze_stamp_mill_{south,west,north,east}.json`

### Rendering V2 and physical-port rules

- Authoring: `tools/multiblock-models/designs/bronze_stamp_mill.json`; canonical occlusion-culled source: `tools/multiblock-models/sources/bronze_stamp_mill.json`.
- Each facing now owns **57 render hosts**, including 9 explicitly selected AIR anchors that become V2 filler proxies on formation. The three item modules, Gear V2 module and the actual controller are NOT swapped for artificial port meshes; module render parts must remain EMPTY.
- Every module's complete local AABB is reserved for that module. Neither static V2 boxes nor local Voxel V2 collision/selection may overlap any of the four module spaces. The regression test asserts this for the model and both voxel profiles.
- Ground-level port models stay visible. Raised structural columns, front canopy and metal transport chutes sit around and ABOVE them; none use the port cell as an attachment/render host. No globally modified I/O models or blockstates.
- BER handles the independently moving stamp heads, 120-degree-phased cams, loaded ore and Gear V2 input shroud. Stamp heads are now centered over the three crushing channels at X -8, +8, +24 model pixels, with a resting shoe height of 24 pixels above controller ground.
- `bronze_stamp_mill_hammer_worn.json` visually indicates the shared shoe wear state; only successful batches decrement maintenance.

**Never manually edit the 228 generated V2 part files**. Regenerate from the authored design and isolate diffs to this machine:
```bash
node tools/multiblock-models/build-designs.mjs
node tools/multiblock-models/generate.mjs
```
Check output for other machines (especially the accepted Bronze Lathe) before committing. Do not accept stretched UVs or overlapping faces.

## Resources and state

- Port U connects to controller slot 0 (recipe-validated insertion).
- Port O connects to slot 1 (primary output; extraction only).
- Port Y connects to slot 2 (Gravel byproduct; extraction only).
- A batch reserves both outputs, picks the optional byproduct once, and does not consume ore or wear the shoe until success. Blocked products or inadequate torque/speed pause it.
- Replaceable Stamp Shoe lasts 384 successful batches. Maintenance and unfinished recipes are saved in NBT.
- Recipe type `magneticraft2:multiblock_processing`, machine `magneticraft2:bronze_stamp_mill`. Datapacks and JEI category/catalyst stay supported.
- Gear V2 power and maintenance logic remain unchanged by this visual-layout fix.

## Verification commands

```bash
./gradlew build
node tools/bronze-stamp-mill/test-contract.mjs
node tools/multiblock-models/build-designs.mjs --check
node tools/multiblock-models/generate.mjs --check
```

In-game checklist:

1. Dismantle the previous 5x3x3 version and rebuild this 5x3x4 blueprint. Test all orientations.
2. Confirm the controller forms the machine and the nine AIR proxy cells are placed without replacing ANY of the four actual modules.
3. Inspect the **grounded, front-accessible** three port models, the three separate chutes, canopy support and raised crushing tray. Check no invisible barriers, z-fighting, texture distortions or protruding modules.
4. Insert two raw Chalcocite or Cassiterite through port U; extract crushed output from O and any Gravel tailing from Y. Test item automation from outside the front service row.
5. Watch hammer shoes travel vertically and strike above the tray without clipping the modules.
6. Pause/restore Gear V2 power mid-batch, unload/reload chunks, fill either output, and verify no free items or lost progress.
7. Wear and replace the Stamp Shoe; confirm visual wear changes and correct job inhibition.
8. Continue crushed ore into Washer and Sifter, checking existing ore yields and fluid/byproducts.

**No `./gradlew build` or Minecraft runtime verification has been performed for this revision.**

## 2026-10-17 — central load-frame and visible-surface cleanup

After the first in-game 5×3×4 screenshot showed a floating crushing rack and substantial Z-fighting, the Stamp Mill's **physical model** was rebuilt without moving any required construction blocks. Two continuous under-tray girders now run across the stamp lanes, with cross-members and grounded side braces transmitting loads into the original front/rear sills. Inner vertical hangers support two long upper guide rails; both connect through transverse top bridges to the outer corner posts. Four separated bronze crank pillow blocks mount the longitudinal drive shaft **between** moving cams, replacing the formerly intersecting independent `stamp_top_brace_*` and `cam_bearing_*` boxes. Extra side skids connect the outer service shelf back into the sills. The static-solid connectivity contract verifies that *every* authored cuboid can be reached from the bottom front sill through physical face joints (not just point contacts).

The moving hammer body was narrowed so that, at rest and during its vertical stroke, it fits between the bronze crushing-trough lips; a separate upper guide yoke continues to slide between the fixed guides. The animated hammers (normal and worn), cam, and input frame have been reduced to **visible exterior face panels**, rather than full overlapping cuboids. Ore now renders **above** the supported crushing floor, not inside the subframe. The paired chutes have small overlapping joints so they actually connect. Decorative trims/caps that overlapped the upper driving mechanism were removed. New tests reject coplanar duplicate animated faces, stretched UVs and a return to detached support components.

**Port contract unchanged:** All three item I/O modules remain real, unobstructed 1×1 blocks on the lower south service shelf, and the rear Gear V2 input retains its full cell. The 5×3×4 four-way JSON structures, Pattern/Visualize page, processing recipes, Gear V2 load, wear/persistence and item port implementation are unchanged. The Voxel V2 profile **did change**, so dismantle and reform an existing Stamp Mill after rebuilding to refresh saved voxel snapshots. Regression source checks are implemented; Gradle and Minecraft rendering verification are still required.

A second static/dynamic face audit found the yokes' outer X edges met the fixed crank stands on the same Y=34 plane. The normal and worn models now use narrower **12.2-pixel guide yokes**, retaining a 0.2-pixel lateral clearance to the crank posts. A regression check compares resting hammer, cam and formed input-frame surfaces to the canonical static V2 source; no coplanar cross-model faces are permitted in the parked position. The first in-game pass is still required to validate every animated phase and actual lighting/texture behavior.


## 2026-10-17 — revised gearbox and outer belt drive (post-gameplay screenshot)

The first transmission prototype was rejected after in-game screenshots showed
the long upper shaft turning **opposite** to the three eccentric cams, plus
multiple misaligned and floating pieces beneath the mill. The Z input had been
drawn with an X-axis shaft asset, the horizontal shaft repeated beyond its
actual joint positions, and belt half-wrap geometry was mistakenly rotating.

The follow-up on PR #11 implements a physically connected **1:1 visual drive
path** without changing the server-side Gear V2 load or the 5×3×4 blueprint:

1. **Input:** the existing rear-center Gear V2 module still provides the real
   rotational state at controller offset `(0,0,-2)`. Its new Z-aligned spindle
   extends at `(x=8,y=8,z=-26..-10)` in controller-relative model pixels.
2. **Lower bevel pair:** a Z-facing bevel centered around `(8,8,-12.5)`
   engages a separate X-facing bevel around `(11.5,8,-10)`. Only the Z-input
   spin is reversed for the bevel. The lower crossshaft is actually aligned
   with the X gear and occupies `x=8..47.2, y=8, z=-10`.
3. **External belt:** both X-axis pulleys are centered at `x=47.2,z=-10`,
   with centers at `y=8` and `y=38.5`. Their thinned mounting faces fit
   entirely within the 5-wide footprint (max local x 47.9475). The belt's two
   static visible flights meet without overlap at y=24, while its top and
   bottom half-wraps remain stationary geometry. The **pulleys** rotate.
4. **Upper crankshaft:** four uninterrupted 16-pixel spindle segments span
   `x=-24..40`; the end tip continues to `x=47.2`. All four stationary
   crank pillow blocks support this shaft. Its rotation is `+shaftAngle`,
   exactly like the three eccentrics (`+120°` relative phases). The
   original `mount_upper_drive_shaft` static cuboid was removed so there is
   no second non-moving axle drawn through it.

The updated authoring design has **101 anchored static boxes**. Its canonical
visible surface model and **179 of 228** V2 directional part files were
regenerated; the remaining parts are unchanged. No other machine models or
their Voxel V2 geometry were modified. Both the lower shaft and the belt and
pulley envelopes have been checked against the entire authored static frame.
The real input module and all three actual item ports retain their full,
untouched block volumes. The book's blueprint dimensions and placement have
not changed.

**Verification status:** source-level checks have been added to
`tools/bronze-stamp-mill/test-contract.mjs` for the corrected drive layout,
texture references, native UV proportions, static shaft removal, no physical
intersections and the positive upper drive phase. The patch has not been
verified by running `./gradlew build`, nor rendered in Minecraft after this
correction. The belt's surface does not yet scroll visually; the pulley
rotation and shaft phase are the meaningful moving elements at this stage.

**Next in-game test:** rebuild/reform the 5×3×4 mill, then inspect the lower
bevel connection from behind and in spectator mode; the *right outer side*
belt and its two pulleys; the top shaft and all three cams while driven; the
stopped state; all four orientations; and any flickering or frame/shaft
clipping. Pay particular attention to the four crank pillow bearings, the
underside of the wooden front support, and clearance to adjacent blocks.


## 2026-10-17 — supported, close-meshing 90-degree gearbox revision

A second in-game screenshot showed that the two lower gears still appeared as
floating, interpenetrating decoration and their apparent rotation did not
convincingly drive one another. That earlier gearbox stage is **superseded**.

The corrected **renderer-only** powertrain now has a compact bronze/iron
eight-tooth bevel pair in the *front-right* quadrant of the lower axes
intersection at model pixel `(8,8,-10)`. Both gears rotate around that
same intersection but their *wheel bodies* are displaced along different
axes: the Z pinion is ahead of the intersection, while the X pinion sits
right of it. Their outer radial teeth meet with a deliberately tiny
clearance, but the bodies never occupy the same physical volume.

The signed rotation `inputAngle = -shaftAngle` for the Z axis and
`shaftAngle` for the X axis means **equal tangent velocity at the actual
(+X,+Z) tooth-contact corner**, which is the correct meshing relationship
for this arrangement. The X shaft continues at +shaftAngle to both pulleys,
the upper crankshaft, and the three 120-degree-offset cams.

- The input shaft spans `z=-26..-10`, then a separate short spindle
  continues to `z=-3.8` through the Z pinion's hollow hub.
- The X output shaft starts at `x=10.5` (rather than the former `x=8`);
  it joins the full crossshaft at `x=24` and then the end tip at `x=40`.
  This stops two perpendicular solid axles from intersecting at the
  gearbox centre.
- The open wood/bronze gearbox frame has real skids and upper hangers
  touching both underside girders. There is also a bearing suspended from
  the same frame halfway along the lower shaft, plus a bronze side bearing
  touching the foundation brace immediately before the lower pulley.
- The pinion teeth have a 22.5-degree modeled offset. Conservatively
  rotating every tooth around its full axis produces a maximum swept
  radius of approximately 4.004 pixels for Z and 3.977 pixels for X.
  Both are less than the 4.06-pixel envelope that would reach the
  perpendicular gear, preventing the previous visual collisions.
- `tools/bronze-stamp-mill/build-drive-models.mjs` now maintains all of
  the new render-only gearbox models; `--check` verifies their generated
  assets. `tools/bronze-stamp-mill/test-contract.mjs` additionally
  enforces tooth-clearance, rotation polarity and physical attachment.

**Kept unchanged:** the 5×3×4 blueprint, all actual full-block ports, Gear
V2 input node and loads, recipes, controller, inventory, guidebook pattern,
Static V2 source/voxel geometry, and accepted Mechanical Lathe. No
reformation is required specifically for this BER-only visual revision
if the last 5×3×4 structure is already formed.

**Still pending:** Gradle build and an actual in-game viewing pass. Test
the underside gearbox while moving and stopped, right-side pulley/belt
transfer, top cam/shaft phase, four orientations, resource reloads, and
close-up Z-fighting. We intentionally did not add extra JSON construction
blocks just to render gearbox bearings.


## 2026-10-17 — real module ownership, visibly meeting pitch wheels (supersedes earlier drive sketches)

The third underside Minecraft screenshot showed **both detached-looking pinions
and texture flicker at the input shaft**. The earlier bounding-box-only
regression checks were inadequate: simply avoiding intersection does not
establish believable contact, and they did not model the separate Mechanical
Input Module block-entity renderer.

This is the authoritative rendering contract for the Stamp Mill's drive:

1. **Existing real input module**: Its OWN reusable
   `MechanicalInputModuleBlockEntityRenderer` draws the 6-pixel-wide
   wooden shaft and copper bearing inside controller offset `(0,0,-2)`,
   covering model-space z=-32..-16. Do **not** render
   `bronze_stamp_mill_input_frame` or a second rotor in that block. Its
   block entity and Gear V2 wiring remain untouched.
2. **Wood-to-steel transition**: A stationary-looking hollow bronze coupling
   actually turns with the input and covers the steel extension immediately
   OUTSIDE the module. The extension spans model z=-16..-10; the spindle
   continues z=-10..-3.8 through the input bevel. No newly rendered
   Stamp Mill steel geometry invades z<-16, and no cap face is drawn on
   the real wooden shaft's front plane.
3. **Bevel pair at (8,8,-10)**: The input Z pinion and output X pinion
   have octagonal 3.85px bronze pitch rims and eight staggered teeth.
   The formerly 2.65px-wide square pitch wheels were misleadingly small:
   only their isolated tooth tips came close. The enlarged pitch faces
   now approach the opposite wheel's 4.06px engagement plane with
   about 0.21px spacing, without crossing it. Conservative full-rotation
   tooth/cuboid radius is approximately 4.018px, leaving at least
   0.041px margin before the perpendicular axle's gear.
4. **Correct rotation phase in every facing**: The shared input module BER
   rotates around the *positive world X or Z axis*, regardless of the
   FACING sign. The multiblock's local +Z maps onto that axis for SOUTH
   and EAST, and against it for NORTH and WEST. Thus
   `getStampInputRotationDegrees()` corrects the local sign, and
   `getStampCrankRotationDegrees()` reverses it exactly once through
   the right-angle bevel pair. The OPEN belt carries the same output
   phase to the upper shaft; all three 120-degree cams and the
   animated hammer lift use that output phase.
5. **No doubled spinning surfaces**: Adjacent Z and X shaft parts have
   no coincident hidden end caps. The first upper shaft segment has one
   real visible end cap, and the other segments have open internal joints.
   This removes another likely cause of flickering along the visible
   long iron drive axle.
6. **Physical support**: The wood/bronze gearbox frame, middle bearing
   and right-hand pulley support remain anchored to the authored load
   frame without taking space from the three actual item ports or
   changing Voxel V2 or the 5×3×4 multiblock pattern.

The original input-frame model is retained as an unused resource for now,
but MUST NOT be called from the formed Stamp Mill renderer.

Generation for new moving drivetrain parts is maintained by
`node tools/bronze-stamp-mill/build-drive-models.mjs`, with a matching
`--check` mode. The contract tests cover visual pitch-wheel proximity,
rotation polarity, module-cell ownership, end-cap removal, and native
unstretched UVs.

**Still pending**: `./gradlew build` and actual Minecraft verification.
No client-side render was available for this patch. Inspect the Z-to-X
gear engagement from the underside (stopped and rotating), wooden bearing
to bronze coupling, all four facings, and belt-to-upper-crank motion.
The large wheel farther out on the horizontal output shaft is a
**belt pulley**: it should not mesh directly with the inner bevel pair.


## 2026-10-17 — bevel teeth actually interleave (latest in-game follow-up)

The next game screenshot confirmed that the lower bearing, housing and wheel
placement now look much better, but the wheel teeth were still visibly
**not interacting**. Their previous geometry had a 4.018-pixel outer
swept-radius restriction, while the opposing pitch plane began at about
4.06 pixels from the common Z/X intersection. This *prevented* the teeth
from entering the opposing tooth gaps by design. A purely static
non-intersection check was not enough.

**Changes to the two existing moving wheel models only:**

- Keep both gear pivots, the 90-degree shaft intersection, the 8:8 1:1
  speed ratio, the 22.5-degree half-tooth phase offset, the signed
  rotation calculation, housing, bearing mounts and adjacent modules.
- Replace the short decorative gear teeth with eight **three-section
  bronze flanks** on each wheel: roots embedded in the existing bronze
  pitch rim (radial 3.65–4.18 model px), flanks (4.12–4.55 px) and
  narrower protruding tips (4.48–5.20 px). Each section overlaps its
  predecessor, so there are no floating tooth pieces.
- The tips now deliberately extend **past** the opposite wheel's
  4.06-pixel exclusion plane and enter the spaces between its teeth.
  The other gear's 22.5-degree stagger and counter-rotation are what
  prevent interference—not an artificial rule forbidding teeth from
  reaching each other.
- `build-drive-models.mjs` now exports its authored solid cuboids for
  regression testing without rewriting any assets when imported.
  `test-bevel-mesh.mjs` rotates both sets of genuine solid cuboids
  through the signed motion range (±45 degrees in 0.25-degree samples),
  checks 15 possible OBB separating axes where bounding boxes approach,
  requires physical attachment of all 24 tooth segments per gear,
  verifies the teeth reach into the counterpart's gaps and enforces
  near-contact without physical collisions.
- The source-level sweep evaluated **976,144 gear-cuboid pairs**;
  it found zero detected solid intersections. It recorded tooth-to-tooth
  AABB gaps of approximately **0.040 to 0.731 model pixels** across sampled
  phases, with 136 phases having gaps below 0.25 pixels.
- The Stamp Mill's overall contract now invokes the actual meshing test.
  The superseded global `radius < 4.06` tests have been removed, since
  they mathematically made a functioning bevel gear impossible.

This is **a moving-geometry-only change**. No blueprint, Voxel V2,
Patchouli construction, item module, processing recipe, Gear V2 load,
machine layout or accepted Mechanical Lathe geometry was changed.

**Verification boundary:** authored geometry and generated model parity
have been checked, but no `./gradlew build` or in-game render test was
performed in this change. Review the actual meshing from below with the
Stamp Mill stopped and rotating, particularly the copper teeth meeting
near the +X/+Z corner. Check that the new tips are not excessively large,
do not shimmer or clip through each other, and still look like bronze
machine gears in the Minecraft art style. Also verify unloaded/loaded
motion and all four orientations before accepting this design.


## 2026-10-17 — rollback of oversized bevel teeth; compact corrective model

The next in-game screenshot showed the previous `5.20px` three-stage tooth
profiles looked like oversized bronze fingers and visually ruined the
gearbox that had otherwise been accepted. **The preceding extended-tooth
description is superseded.** The acceptable frame, bearings, shafts,
pulleys and input-module rendering are unchanged.

The two bevel-wheel models now use the following authored design:

- Octagonal bronze pitch rims move inward by only `0.30` model pixels,
  from axial `12.2` to `11.90`; outer rim radius `3.65px` rather
  than `3.85px`. Rim corner bands are slightly widened so **all eight**
  tooth roots physically overlap the wheel, including diagonal teeth.
- Eight **two-stage** bronze teeth per wheel: wider embedded roots
  (radial `3.45..3.91px`) and narrower compact tips
  (radial `3.85..4.45px`). There are no extra long third-stage tips.
- The X pinion's visual tooth offset is `20.5 degrees`. This is a
  clearance-calibrated angle for the cuboid tooth profiles, not a change
  to the physical 1:1 speed ratio.
- The `test-bevel-mesh.mjs` 90-degree signed rotation sweep now
  requires sixteen connected tooth segments per wheel, tests the
  complete two-gear solid geometry using separating axes, measures tip
  proximity, and **rejects oversized teeth** (radial extent must be
  less than `4.9px`). It also rejects floating roots.
- Source-level test result: **698,896 rotated solid-pair comparisons**,
  no detected solid collisions, **185** close phases of 361, sampled
  tooth gap `0.059..0.688px`. This is not a Minecraft render result.

Only the two bevel JSON assets, their authoring generator and its
meshing regression have changed. No multiblock rebuild is needed.
Minecraft appearance and `./gradlew build` remain unverified.

**Next test:** close-up of the same underside view while stopped and moving.
Specifically check that the copper tips no longer resemble protruding
fingers, the two pitch wheels still visibly mesh, and there is no flicker.
If visuals remain unsatisfactory, do not extend these tooth tips again
without a new gear profile design and a complete in-game review.


## 2026-10-18 — restore missing input-frame details (accepted machine appearance)

The real issue after earlier render work was **not missing V2 directionals**:
all 228 directional parts remain, and the only removed V2 cuboid was the
static upper drive shaft replaced with the animated one. Instead, the
Stamp Mill-specific `bronze_stamp_mill_input_frame` stopped being drawn in
the BER when we switched to the reusable Mechanical Input Module's own
wooden shaft/bearing. The legacy frame still existed on disk but its
outer dark-metal and bronze details had silently disappeared.

The corrected Stamp Mill now renders a **separate exterior-only frame**
`bronze_stamp_mill_input_frame_detail.json` at the original input cell
`(0,0,-2)`. It retains all original outer steel/dark-metal rails, copper
corner pillars, and upper/lower crosspieces:

- The original input-frame model contains 63 visible panels: 43 outer
  support/detail panels, and 20 inner shaft-sleeve/collar panels.
- We preserve the 43 outer faces as authored, except for precisely
  clipping four rails where they physically cross the real shared-module
  housing. The final trimmed baked asset has 51 face panels: 39 fully
  unchanged, with 12 subfaces replacing four conflicting panels.
- The 20 inner faces remain delegated to the shared module's rotating
  wooden shaft and bronze bearing; restoring those old faces verbatim
  would create the same Z-fighting we previously eliminated.
- Both module assets are inspected in their true Y-to-Z transformed
  orientation. Source-level checks confirm the new detail intersects
  neither module housing nor shaft, and clears the **full swept radius**
  of the rotating square wooden shaft, not just its resting bounds.
- `tools/bronze-stamp-mill/build-input-frame-detail.mjs` regenerates
  the detail from the exact legacy asset and shared module models.
  Its `--check` runs inside the Stamp Mill contract, protecting
  original frame completeness and native, unstretched UV mapping.

**No changes** to the two bevel gears, their teeth/phase, rotating
shaft, bearings, belt, existing static V2 frame, construction JSON,
controller, item modules, gearbox loads, recipes, Patchouli pattern
or the accepted Mechanical Lathe.

This remains source-level validation. A successful `./gradlew build`
and a Minecraft visual pass have not been performed for this revision.
The next test should compare the restored dark frame and bronze corner
posts around the real wooden input shaft with the earlier in-game views,
both stopped and rotating, checking for missing parts and z-fighting.


## 2026-10-18 — continuous leather power belt and visible crushing faces

Full-machine in-game screenshots showed the main frame, item ports and
three stamps intact after the input-frame restoration. The right-hand
drive belt, however, was difficult to read. Inspection found a concrete
renderer placement mistake: the lower belt flight was at y=16..32
despite the lower pulley centre at y=8; the upper flight was at
y=32..46.5 despite the upper pulley centre at y=38.5. This left
an 8px visible gap below and an 8px overshoot above.

**Fixed** the lower flight to y=8..24 and upper to y=24..38.5,
meeting precisely between the two original pulleys. Removed hidden
top/bottom end-cap faces at belt junctions to prevent Z-fighting.
The pulley axle positions, Gear V2 sign/phase, transmission, 90°
bevel wheels and frame geometry remain unchanged.

A new **opaque 16x16 pixel-art leather** texture at
`textures/block/bronze_stamp_mill_drive_belt.png` replaces the
`minecraft:block/brown_wool` placeholder in both flights and wraps.
Its warm leather tone and single small stitched joint every tile make
the drive easier to pick out from the dark spruce, without stretching UVs
or using the transparent leather-belt ITEM icon.

To make the working area legible, each of the three original hammer
stations now renders a small **wear plate**: dark backing, inset iron
anvil surface, four bronze rivets, and two short scuff lines. It is
visually integrated into the EXISTING trough rather than adding new
inventory slots, drop-offs, module blocks, or construction voxels.
Its authored range is y=23.05..23.58, above the trough floor at y=23
and at least 0.42 pixels below the lowest hammer shoe at y=24.
One baked model (`bronze_stamp_mill_strike_plate.json`) is reused three
times, with **65 culled/merged visible faces** per instance.

`tools/bronze-stamp-mill/build-strike-plate.mjs` owns the authored
strike cuboids, and `test-visual-details.mjs` checks floor/hammer
clearance, no V2 static intersections, native unstretched UVs, no
coplanar duplicate panels, input/output block avoidance, continuous
belt endpoints, no internal belt end caps, opaque RGBA pixels, correct
baked registration and actual renderer placement. Both are invoked
by the existing Stamp Mill contract.

**Scope preserved:** the 5x3x4 blueprint/guide, Voxel V2 static solids
and all four actual modules, torque/recipes/Gear V2, input/output,
gearbox and bearings, upper crank phase, and accepted Mechanical Lathe.

**Verification:** checked against authored geometry and source models.
Full `./gradlew build` and visual Minecraft testing remain pending.
Review the right-side leather belt, its lower and upper pulley contact,
all three visible strike plates (empty/ore loaded), hammer low position,
and possible FPS impact, especially with several mills in view.

## 2026-10-18 — belt visual regression: cords and hanging wheels

Minecraft's actual end-on view of the 2026-10-18 belt update showed an
unacceptable appearance: two skinny tan cords with exposed standalone
spoked wheels hanging off the side, despite all prior endpoint/UV tests
passing. It was a model-design error, not a Gear V2 error.

The belt stays in its **existing exterior location** (x=47.2, z=-10),
because moving it inside the frame would intersect the real crushing
trough and timber load-bearing structure. Both rotating pulleys, their
axes, the upper and lower shafts, and the synchronized hammer timing are
unchanged.

**Rendering V3 correction (BER-only):**

- The two leather flights now have approximately **0.97px radial
  thickness**, not 0.54px. Both run from y=8 to 38.5 through a shared
  seam at y=24. Their rectangular leather width follows the existing
  pulley axle's 1.5px depth.
- A matched semicircular leather wrap uses a faceted Minecraft-appropriate
  radius of **4.12..5.05px**, outside the 4.05px bronze pulley perimeter.
  The lower wrap still rotates 180 degrees; the upper remains at rest.
  The new wraps actually overlap the flights at the tangents.
- Replaced the bright cord-colored texture with a deep brown **opaque
  16x16 leather tile** with restrained grain and stitch marks. No
  stretching, texture atlas edits, or transparent pixels.
- Added two small renderer-only **belt frame segments**: dark-iron
  uprights and bronze cross ties behind the loop, with short wood anchors
  to the real lower side-bearing mount and upper structural timber bridge.
  The bands and pulleys do not touch this stationary structure, and the
  supports do not invade any authored V2 static cuboids.
- These are authored in `build-belt-models.mjs`, with generated model
  parity, 1px belt-thickness check, support-to-frame contact, pulley
  clearance, nonoverlapping UVs, belt wrap tangency, no double end caps,
  and a <=200-face visual budget checked in `test-visual-details.mjs`.

Preserved: the 5x3x4 blueprint, all 228 directional V2 fillers,
controller/recipes/ore output, physical I/O ports, mechanical input module,
rotating bevel gears, gear ratio, existing pulleys/shafts, the three
strike plates, and accepted Mechanical Lathe.

**Verification still pending:** `./gradlew build` and actual Minecraft
render check from the same end-on view. Source geometry tests validate
collision and assembly constraints, not artistic quality; do not declare
the new look accepted before another screenshot.
