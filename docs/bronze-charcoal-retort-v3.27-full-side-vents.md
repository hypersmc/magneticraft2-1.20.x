# Charcoal Retort V3.27 — Four vent openings across the full three-block side

**Magneticraft2, Forge 1.20.1** | Draft PR #11 | Branch `feature/bronze-age-foundry` | **In-game review pending**

## User correction
The V3.26 generator placed all four copper-framed holes in the *front-most* upper side block, leaving the other two upper blocks as ordinary brickwork. The reference must instead show **four openings total, spread along all three blocks** on the right, mirrored on the left.

## Single continuous 48-pixel side design
In canonical SOUTH orientation, define U=(z+2)*16+localZ: rear U0..16, middle U16..32, front U32..48. The copper side assembly ends at U2.2 and U45 to preserve the rear copper and front iron corner mounts.

- **Five copper divider sections:** U2.2..4.2, 12.4..14.4, 22.6..24.6, 32.8..34.8, 43..45.
- **Exactly four recessed dark slots:** U4.2..12.4, 14.4..22.6, 24.6..32.8 and 34.8..43. Two slots cross real block boundaries at U16 and U32 without gaining visible seam bars.
- Dividers run from Y13.35 to Y15.075; existing lower V3.25 copper cap remains below. The new upper copper beam continues from U2.2..45 at Y15.075..16.
- Material: `minecraft:block/cut_copper` on the copper framing, original soot in the visually recessed sealed slot backing.
- Both sides are generated through the same global U coordinates, with local X faces mirrored; each individual JSON element remains inside 0..16.
- The old left upper full-length iron stripe has been removed to reveal this vent; the rear-facing end-cap metal surface remains in place.
- Right upper brick course is shortened to end beneath the vent, maintaining four lower raised masonry rows and a small visible fifth course edge.

## Scope, source and review
Source: `tools/charcoal-retort/v3-models.mjs`. 24 upper Y2 side-wall parts are regenerated, plus 16 adjacent roof-underface meshes through shared-face culling. Roof top, accepted front and rear outward appearances, main grille, I/O ports, recipes, 3x4x3 construction JSON, collision and chimney are unchanged.

Static generator/mesh partitioning, four-direction left/right vent sampling, per-face proportional UVs, cell bounds, block seam checks and roof top comparisons passed in memory. Regression contracts now verify four distributed openings rather than the old four-in-one-block behavior.

**Still required:** `node tools/charcoal-retort/build.mjs --check`, `node tools/charcoal-retort/test-contract.mjs`, `./gradlew build`, and Minecraft screenshot review. Inspect both side walls at the front and full side; confirm four total holes, not four per cell, and no visual discontinuity at each real block seam.
