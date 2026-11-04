# Charcoal Retort V3.23 — Right Side Wall

**Project:** Magneticraft2 / Forge 1.20.1  
**Working branch:** `feature/bronze-age-foundry`  
**Pull request:** Draft #11  
**Status:** Visual candidate; compare Minecraft side view against the two October 24 screenshots

## Reference and scope

The reference shows continuous, physically textured red-brick masonry along the right side of the Retort, with fine recessed joints and small corner fixings. The existing in-game model showed comparatively flat brick panels between two broad, dark horizontal bands. This pass replaces the **right middle and right rear Y2 side-wall skins only**. The right-front corner (with the already-approved recessed square plate and copper shoulder), front grille/arch, copper roof/chimney, bottom services and the other side are not redesigned.

## Side-oriented local geometry

Unlike the front facade, the visible right-side surface faces **+X**, not +Z. The two authored cells in SOUTH coordinates are `(x=1,y=2,z=-1)` and `(x=1,y=2,z=-2)`.

| Detail | Model-pixel geometry | Material |
|---|---|---|
| Solid structural core | `[0,0,0]→[14,16,16]` | Original refractory |
| Recessed mortar backing | `[14,0,0]→[14.2,16,16]` | `minecraft:block/stone` |
| Physically raised bricks | X14.2→15.45/15.70; five staggered rows | `minecraft:block/bricks` via `sideBrick` |
| Rows | Y0.25..2.8, 3.25..5.85, 6.3..8.9, 9.35..11.95, 12.4..15.75 | Separate masonry courses |
| Stagger | Distinct per-block brick joint positions along Z; 0.45px mortar joints | Avoids repeated tiled pattern |
| Back-corner copper upright | Rear cell `[13,3,0]→[16,14,2]` | Cut copper |
| Small rear iron foot | Rear cell `[13,0,0]→[16,3,0.5]` | Existing dark iron |
| Small rear iron cap | Rear cell `[13,14,0]→[16,16,0.5]` | Existing dark iron |

**Remove:** the previous full-length Y0..3 and Y13..16 blue-grey iron strips from these two Y2 side cells. Do not remove rear corner fittings or the accepted roof/side structural joint.

**Texture caution:** Use a separate `sideBrick` key for side-facing raised masonry; keep `brick` as the original refractory for the structural backing. This retains the rear-facing material and prevents an accidental recolor of the back wall.

## In-game acceptance tests

1. At a front-right three-quarter angle, the square iron front corner should flow into two blocks of dimensional brickwork, with no visible air gap or abrupt hollow inset.
2. From the side, verify **five staggered physical masonry courses** and visible mortar shadows, with no horizontal dark iron bands crossing the wall.
3. At the rear corner, the copper upright and its dark end fittings still connect, without recoloring the rear structural face.
4. Rotate/form the Retort in all four orientations: the wall should remain on the machine's *right*, facing south/east/north/west as appropriate.
5. No UV stretching or z-fighting, and no hidden-face culling holes around the roof, front corner or lower service wall.
6. Keep the front facade's five-slot grille and curved arch, upper three-block copper header, Y1 ports, chimney and multiblock blueprint unchanged.
7. Only visually accept after Minecraft screenshot review; full `./gradlew build` must pass before claiming compilation.

## Source and verification

The shared generator is `tools/charcoal-retort/v3-models.mjs`, output files are `src/main/resources/assets/magneticraft2/models/multiblock/charcoal_retort_*/part_*.json`, regression tests live in `tools/charcoal-retort/test-contract.mjs`. Generated model diff scope is 28 files: eight two-cell side variants in four orientations, four front-right corner seam meshes, eight Y1 contact faces and eight roof undersides. The non-redesigned model outputs change only due to internal shared-face culling.
