# Charcoal Retort V3.21 — Center Facade Width and Brick Relief

**Branch:** `feature/bronze-age-foundry`  
**Pull request:** Draft PR #11  
**Status:** Model candidate; in-game visual approval and Forge build outstanding

## Request and scope

Following the V3.20 connected three-block copper header, the player identified four narrow fixtures around the front-center grille and a flat center brick field. V3.21 is an intentional one-block redesign of the front-center upper chamber. It MUST keep the V3.19 shallow arched apron and V3.20 cross-block header intact.

The source of truth is `tools/charcoal-retort/v3-models.mjs`; the SOUTH-facing authored chamber part is local `x=0,y=2,z=0`, emitted as `part_0_2_0.json` in all four cardinal orientations. All coordinates below use the 0..16 Minecraft model-pixel coordinate system, front is local Z=16.

## Structure and actual cuboid geometry

| Detail | Previous | V3.21 geometry |
|---|---|---|
| Structural center wall | `[0,0,9]→[16,10.5,15]` flat | Backing to **Z14** with side returns to Z15 |
| Visible mortar | No separate model layer | `[3.15,3.9,14]→[12.85,10.45,14.2]`; `minecraft:block/stone` |
| Lower brick course | Flat texture | Y4.05..5.85; X3.25..6.1, X6.5..9.25, X9.65..12.75 |
| Middle brick course | Flat texture | Y6.2..8.1; X3.25..4.55, X4.95..8.25, X8.65..12.75 |
| Upper brick course | Flat texture | Y8.45..10.25; X3.25..6.9, X7.3..10.05, X10.45..12.75 |
| Brick relief | Single face at Z15 | Back Z14.2, faces at **Z15.45/Z15.70** |
| Left/right grille iron fixings | X0.6..2.5 / X13.5..15.4 | **X0.25..2.75 / X13.25..15.75**; inset stone center Z15.65 |
| Copper uprights | X1.7..2.8 / X13.2..14.3 | **X1.05..2.95 / X13.05..14.95**, Y2.3..11.5 |
| Lower iron feet | X1.5..3 / X13..14.5 | **X0.95..3.2 / X12.8..15.05**, Y0..2.4 |
| Upper copper seam returns | X0..0.6 / X15.4..16 | **X0..0.25 / X15.75..16**, matching wider iron plates |
| Main center lintel | X2.5..13.5 | X2.75..13.25, leaving iron plates visible |

All brick courses are genuine 3D geometry, not decals, with 0.3–0.4px mortar joints between staggered bricks; frontmost brick is still 0.3px behind copper. Face UVs are proportional to surface spans.

## Non-regression contract

1. **Five recessed grille openings** and four unchanged copper mullions stay visible in all facings. Soot backing remains at front depth Z13.75.
2. **Seven V3.19 curved copper arch segments** retain their exact X/Y profile, with center higher than both ends.
3. **No gaps between the three front blocks:** the top copper/iron header remains continuous over all 48 model pixels at local Y15.7, including both seams.
4. Copper uprights physically meet their upper iron mountings at Y11.5; the widened iron feet remain visible beneath the curved arch.
5. Brick relief must include all three staggered rows, with face depths Z15.45/Z15.7 and exposed mortar at Z14.2.
6. No face stretching, coplanar geometry/z-fighting, cuboid bounds violations, or changes to the accepted roof/left/right facade.
7. No change to formation, controller, inventory, port skins, recipes, power, animation, processing or collision.
8. Do not mark visually accepted until the user checks an in-game close-up; do not claim Forge compilation unless `./gradlew build` succeeds.

## Suggested in-game screenshots

Take one frontal close-up showing all four support/foot edges, then one shallow three-quarter shot at eye level showing brick-face depth and mortar shadows. The first checks widened proportions; the second checks that the center matches the established left-block 3D style.

**Generated output scope:** four rotated center Y2 models, four re-partitioned contact faces on the lower Y1 center models; all other model assets byte-identical.
