# Charcoal Retort V3.22 — Front-right brick facade

**PR:** #11 (draft), branch `feature/bronze-age-foundry`  
**Visual status:** Candidate only; requires screenshot comparison.

## Reference and problem
The user's in-game right-front Y2 block looked like a flat recessed panel framed by excessive copper. The target reference instead shows a solid **3D brick masonry** wall, a **dark recessed square iron bracket at the upper-right**, and a **small protruding copper shoulder at the lower-right** on top of a narrow dark metal base.

## Exact geometry (SOUTH-local; model pixels 0..16)

| Component | Cuboid / scheme | Purpose |
|---|---|---|
| Wall backing | `[0,0,0] → [16,16,14]` | Solid brick core, no deep cavity |
| Mortar | `[0,2.5,14] → [13,14.1,14.2]` | Gray mortar behind raised courses |
| Brick row 1 | Y3..6.55, X0.25..7.65 and 8.1..12.75 | Physical lower brickwork |
| Brick row 2 | Y7.05..10.6, X0.25..5.4, 5.85..10.1 and 10.55..12.75 | Staggered middle joints |
| Brick row 3 | Y11.1..13.8, X0.25..7.35 and 7.8..12.75 | Staggered upper joints |
| Brick faces | Z15.45 and Z15.70; backing Z14.2 | 1.25 / 1.50 px relief |
| Copper top header | `[0,14.1,14.5] → [13,16,16]` | Continuous V3.20 left-center-right framing |
| Outer copper pillar | `[13,3,13] → [16,13,16]` | Strong right corner, preserved outer side boundary |
| Copper shoulder | `[12,1.25,14] → [16,4.5,16]` | Small protrusion above iron foot |
| Small copper step | `[12.5,5,14.5] → [15.5,6,16]` | Connects outer support visually |
| Dark foot | `[12,0,14] → [16,1.25,16]` | Supports copper shoulder |
| Upper-right iron bracket | X12..16, Y12..16, rear Z14..15.25 | A framed plate, not a full-face iron cube |
| Riveted inner rails | X12.5/13 and 15/15.5, Y12.5..15.5 | Raised edging at Z16 |
| Recessed dark boss | `[13,13,15.25] → [15,15,15.65]` | Square dark center with physical depth |

The first two courses and inner edge use masonry; there is **no vertical copper pillar at the x=0 center seam**. The opposite front-left block, center copper grille/arch, accepted roof and all side/rear blocks retain their existing designs.

## Verification and in-game acceptance

1. Three rows of **actual raised brick solids**, recessed mortar visible around joints, no wallpaper-flat brick surface and no deep alcove.
2. Copper header continues into center with no visible seam, same Z16 face depth.
3. Square iron plate at the **upper-right**, substantial raised dark frame and visibly recessed center; no z-fighting or opaque full metal cuboid covering it.
4. Copper lower-right shoulder **above** a dark thin foot; no unwanted broad copper sill.
5. Existing outer corner side wall remains structurally connected, no extra inner frame.
6. All four rotations, model-bounds, per-face proportional UV and coplanar-overlap checks pass. V3.18–V3.21 grille, arch and header tests stay passing.
7. Do not touch actual inventory ports, recipes, multiblock blueprint/voxel profiles or accepted roof geometry.
8. The user should inspect **front** and **front-right three-quarter** in Minecraft, comparing to the provided reference before accepting the artwork.
9. Full Forge `./gradlew build` and Minecraft visual verification have not been executed by this remote model-edit workflow.

Source: `tools/charcoal-retort/v3-models.mjs`; models under `src/main/resources/assets/magneticraft2/models/multiblock/charcoal_retort_*/`; regression tests `tools/charcoal-retort/test-contract.mjs`.
