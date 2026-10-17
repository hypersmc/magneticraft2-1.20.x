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
