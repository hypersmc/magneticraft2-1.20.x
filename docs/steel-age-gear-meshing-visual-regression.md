# Steel / Iron gears: visible tooth-mesh gap

**2026-11-09 · player-reported visual defect · geometry patch pushed, in-game retest pending**  
**Scope:** Steel III Steel Large Gear (inherits `wrought_iron_gear_large` geometry) and the shared Iron gearing / Gear V2 placement rules. This records the geometry diagnosis, the focused model-resource correction, and the MGC2 Studio acceptance contract. No gameplay/network rule has changed.

## User evidence and reproduced geometry

The player screenshot shows two rotating large, top-facing Steel gears with a visible gap between their teeth. Their relative placement/rotation functions, but their teeth do not visually interlock as intended. Do **not** call Steel III visually complete based on rotation or recipe acceptance alone.

Inspected current sources (Steel PR #19):

- `SteelLargeGearBlock` extends `WroughtIronLargeGearBlock`; the Steel block/item models inherit the same Iron large-gear baked model. The shared Copper Gear V2 BER rotates the actual state model about the block center.
- `GearPlacementValidator.isValidExternalMeshOffset` requires **large/large = (2,0)** in the gear plane, **medium/medium = (1,0)**, **medium/large = (1,1)**. Axle centers stay on exact Minecraft block centers, and network phase uses the 16-tooth / 8-tooth gear counts.
- The large Iron model's 16 visible tooth cuboids extend outward about **15.2 model pixels** from its center; the maximum planar corner radius is about **15.24 pixels**. Two large axles two blocks apart have a center distance of **32 model pixels**, therefore the farthest tips cannot reach each other: a minimum radial-envelope gap of about **1.5 pixels**, varying with rotation/phase. This is the direct source of the visual gap in the screenshot, without changing the center-distance rule.
- The medium Iron model has an outer radial envelope about **8.3 pixels**. Adjacent medium/medium and diagonal medium/large meshes have different available contact distances and must remain valid too.
- A single common gear module with pitch radii 8 and 16 pixels would require a medium/large center distance of **24 pixels**. The allowed one-block diagonal is **16√2 ≈ 22.63 pixels** (about 1.37 pixels closer). Thus all three supported pair types cannot be exact equal-module circles on the present block grid; treat this as a design constraint, not an excuse to invent hidden renderer offsets.

## Why the first geometry proposal was withheld

Extending every large tooth from radius ~15.2 to ~16.6 would reach into gaps on the large/large layout, but static 2D cuboid overlap sampling of the currently allowed medium/large diagonal flagged **additional element intersections** compared with the existing model. Narrowing or sliding the teeth alone did not eliminate the incompatibility in the sampled layouts. A patch that merely hides the gap by changing all 16 tooth tips would knowingly trade one visible failure for another.

The following are expressly **not** acceptable workarounds:

- Translating the animated gear renderer or axle away from its block center; this misaligns shafts and bearings.
- Reducing large/large center distance to one block; that intersects large gear bodies.
- Stretching the baked model or its texture coordinates globally; each face must use geometrically proportional UV mapping.
- Disabling placement validation, altering mesh connections, changing ratios or faking the sign/phase solely for visuals.
- Changing accepted Iron/Bronze/Copper machinery without a targeted regression reason.

## MGC2 Studio model contract

1. Produce a **coherent tooth profile** for 8- and 16-tooth materials that fits the existing three grid layouts, or present a narrowly scoped, justified migration proposal for a changed layout. The final teeth should visibly enter the **opposite gear's spaces**, not simply touch tip-to-tip.
2. Keep all axle centers at the physical block center. Keep 2-pixel-diameter Steel/Iron axial shaft and real gear bearing/collar connections coherent. Do not leave floating gears or support gaps.
3. Preserve the existing Gear V2 RPM ratios and opposite mesh directions: 8T↔8T 1:1, 8T↔16T 2:1, 16T↔16T 1:1. Teeth should keep consistent phase over a **full powered revolution**, while accelerating, coasting, stopped and after save/reload.
4. Test **all three pair types**, including mixed material Steel/Iron and both orientations/orders of medium/large, on all three gear planes (XZ, XY, YZ).
5. Test chains and triangles/corners with clearance rules: no new collisions in the diagonal medium/large reserved-corner cells; shaft-to-gearbox and bearing connections still align.
6. Use native 16×16 Minecraft pixel geometry / correct per-face UV sizes. No stretched/compressed pixels, z-fighting, excessive transparency, unsupported geometry outside the allowed model range or high per-frame renderer overhead.
7. Compare stationary and animated models from top, below, side, diagonal and up-close at day/night light. Recheck FPS against the pre-change setup.
8. Once models are ready, update the relevant Steel **and** Iron inherited resource references carefully and run `./gradlew build`, data/resource validation, texture checks and actual in-game visual inspection. A source/model change is not verification.

For reproducible measurements from a repository checkout:
```bash
node tools/steel-age/inspect-gear-mesh-geometry.mjs
```

The inspector reports raw model extents and idealized pitch mismatches. It is an **early diagnostic only**: its radial estimates do not replace animated tooth-intersection tests or in-game screenshots.

**Status:** candidate shared baked-model fix committed on 2026-11-09, **not verified in game**. Medium/large diagonal meshing remains a regression risk. PR #19 stays draft until visual acceptance.

## First actual geometry correction — 2026-11-09

**Implemented in the shared Iron baked model** (also inherited by the Steel Large Gear): the 16 large-gear tooth cuboids have their outer reach extended from radius 15.2 to **16.25 model pixels**, while preserving each existing axle, gear rim, inner tooth root, tooth count, material texture, angular tooth spacing, block placement rule and network rotation phase. Each changed face has a regenerated 1:1 model-pixel/UV mapping. The modification affects the *actual* model resource, unlike the previous diagnosis-only commit.

At the permitted 32-pixel large/large centre distance, the radial tooth envelope now reaches beyond the midpoint, allowing tips to enter opposing tooth spaces. A static phase/cuboid sample of the two large gears found no solid-to-solid collisions under the currently used tooth-phase relationship. This is an **implemented candidate**, not yet a proven moving mesh in Minecraft.

**Known regression risk:** the game also supports medium/large diagonal meshing at 16√2 pixels. The earlier static overlap approximation flags more possible interpenetrations with larger tips. This is **not resolved by a proof**. In particular, visually inspect medium/large links across full rotation; if collisions occur, the next change should be a consistent model tooth/root/phase redesign, not a renderer translation or hidden geometry. MGC2 Studio remains the owner of final model work.

**Verify:** restart/reload resource packs; connect two Steel Large Gears two blocks apart on XZ, XY and YZ planes, from all angles and while moving/paused; inspect Steel+Iron Large pair and diagonal Steel Large/Steel Medium; test stops/restarts, gear ratio and overlays, shaft alignment and texture UVs. Do not label Steel III visuals complete until all configurations pass.
