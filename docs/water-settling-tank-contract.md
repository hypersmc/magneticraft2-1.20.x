# Bronze II — Water Settling Tank (V4.2 port and rim integration)

**Status:** Implemented in the open, draft Bronze Age PR #11.
The full Gradle build and final Minecraft appearance are pending user testing.

## Why V4.2 replaced V4.1

The V4.1 in-game screenshots exposed physical layout issues that cannot be
corrected by moving one decorative fitting: all three service ports were
part of the **foundation layer**, while the rest of the machine stood on a
full-height stone floor. Because the module block models are only partial
cubes, this left deep holes, a sunken wooden tray and an external copper
output appearing unsupported. The rear rim ended without connecting to
the left and right side rails, leaving obvious dark wedges at both corners.

**V4.2 fixes the actual block layout.** No new machine, fluid mechanic,
byproduct, or bespoke module type is necessary.

## Formation: 5 wide × 2 high × 5 deep

Canonical SOUTH orientation, controller at local (0,0,0).
All rows run **west to east**. Rows listed back/NORTH (z=-4) to
front/SOUTH (z=0):

```text
               BACK / DIRTY WATER INLET
Y=1    SSISS    SAAAS    SAAAS    SAAAO    SPSSS
Y=0    SSSSS    SSSSS    SSSSS    SSSSS    SSCSS
               FRONT / ITEM OUTPUT; EAST SIDE / WATER OUTPUT
```

- `S`: 37 Stone Bricks: 24 in the foundation and 13 in the walls
- `A`: nine REQUIRED AIR cells, six of which become Voxel V2 models
- `C`: controller; only non-stone foundation block, at (0,0,0)
- `I`: real Fluid Input in REAR upper wall, at (0,1,-4)
- `P`: real Primary Item Output in FRONT upper wall, at (-1,1,0)
- `O`: real Fluid Output in EAST/RIGHT upper wall, at (2,1,-1)

The shared fluid modules automatically orient outward on formation,
using their offset from the controller. The rear inlet therefore faces
**NORTH** and the clean-water side outlet faces **EAST** in the canonical
layout. Connect pipes to those outward faces; do not place the clean
outlet in the upper front-right wall where it would point east INTO stone.
Other three machine orientations rotate both outward directions.

All three module cells are entirely real blocks, not V2 proxies. A
load-bearing Stone Brick foundation sits immediately below each one.
The generated model displays a thin bronze support plate on each
foundation and does not render any fake pipe inside a module cell.
The front sediment hatch stays accessible to players and item automation;
the right-side clean-water port remains exposed for external pipes.

The two rear corner returns and two front corner returns are genuine
stitched shapes with zero overlapping cuboids or same-facing coplanar faces.
No visually unsupported rim overhangs remain at the rear wall/sidewall
junctions. The machine remains two blocks tall, has an open top and no
overhead supports or catwalk.

**Old V4/V4.1 tanks MUST be safely emptied, unformed and rebuilt.**
Moving the ports from Y=0 to Y=1 changes the required building pattern;
a resource reload alone cannot make an old multiblock valid.

## Processing contract (unchanged)

| Actual input | Time | Actual server-side output |
| --- | --- | --- |
| 600 mB Magneticraft2 Dirty Water | 240 ticks | 480 mB ordinary Minecraft Water + 1 Settled Sediment |

- The remaining 120 mB per batch is irrecoverable process loss.
- Four Settled Sediment produce one Clay Ball.
- The machine remains passive, with no Gear V2 load.
- Dirty and clean storage are separate 12,000 mB fluid tanks.
- Sediment is always stored in the actual primary item output slot.
- Both output capacities are simulated before dirty fluid is consumed.
- Blocked outputs preserve material and progress, not phantom items.

The passive `multiblock_processing` datapack recipe is unchanged. The
existing fluid modules and JEI category remain shared with other machines.

## Generated art and fluid placement

The generator `tools/water-settling-tank/build.mjs` is the single source
of truth for four rotated JSON formation structures, 180 local block
models, their native-scale texture UVs and the 115-box Voxel V2 collision
profile. Seven required-air hosts form only after validation:
one recessed inlet throat and the six original baffle/lip pieces.

The real rear, front and right-side upper modules are no longer drawn
as ornamental copper risers. They display their **real block models**. The low bronze rim,
dark masonry, ceramic lining and divider are purposefully tiled in their
own block-local parts.

The fluid BER reads real tank amounts:
- dirty rear reservoir X [-0.975,1.975], Z [-2.84,-1.29]
- clean front reservoir X [-0.975,1.975], Z [-0.96,-0.24]
- display level Y = 1.045 + 0.495 × fill ratio, only if non-empty.

The new tank-only inlet collar seats around the copper input's inboard
frame without touching its opening or outward pipe face. It occupies the
previously empty AIR cell (0,1,-3), reaching only 8 model pixels back
toward the real input cell. The copper model's inner ring spans X=2..14,
Y=2..14, Z=8..12; the collar fills only the free margins and begins
its detailed reveal where the copper terminates at Z=12.
Dirty-water rendering now starts at Z=-2.84, just beyond the collar's
forward edge at Z=-2.875, never through the casing. The sediment output item is drawn at (-0.5,1.775,0.5), over
the real elevated front-wall wooden tray. No fake particles or ghost
sediment entities are introduced. Existing six V3 16×16 hand-painted
texture assets are reused without stretching.

## Verification

```bash
node tools/water-settling-tank/build.mjs --check
node tools/water-settling-tank/build-textures.mjs --check
node tools/water-settling-tank/test-contract.mjs
./gradlew build
```

Source-level audit checks:
- four directional blueprints and Patchouli Visualize agreement
- all actual modules at Y=1 with underlying bronze bearing plates
- recessed input collar touches only empty margins of the actual copper
  module model, without intersecting any real input cuboid
- no filler model replaces or covers real modules
- rear and front rim corner joint continuity
- no cuboid overlap, coplanar matching faces, or stretched UVs
- correct dirty/clean surface clipping and true item output position
- preservation of the 600→480 mB recipe and item byproduct

**Live Minecraft acceptance still needed:** rebuild once, verify all
three module blocks face out, test accessible water pipes and item
extraction, look at the four corners and both output modules from front,
side and overhead, then run empty/full/blocked/reload scenarios.
Watch for lingering WTHIT "Multiblock Filler" tooltips: that is a
separate existing naming/UX problem, not resolved by this mesh repair.

## Design history

- V1: basic stone trough and flat-looking water.
- V2: highly ornamented ceramic/metal frame and walkway.
- V3: restrained hand-painted 16×16 textures.
- V4: simpler low, two-stage open tank.
- V4.1: removed Z-fighting; clipped liquids inside the real walls.
- V4.2: raised the real service modules, filled foundation holes and
  permanently joined the rear/front rim corner geometry.

## V4.3 — rear Fluid Input throat repair (2026-10-24)

Close-up screenshots showed that the copper inlet still floated in an
unframed 16px opening. The actual shared Fluid Input model occupies only
the center X=2..14/Y=2..14; its visual gaps cannot be repaired by adding
more texture to the neighboring wall.

The V4.3 tank-specific model fills these margins using slim rear-wall
cheeks, a solid lower saddle, a short bronze lintel and an inboard
metal-and-ceramic throat. The aperture and genuine NORTH fluid connection
remain open. The surrounding geometry is attached to a new Voxel V2 AIR
anchor at (0,1,-3), not a modification to the shared Fluid Input block.

This collar is the only intentional outside-cell model element: up to
8 pixels backward into the real inlet's EMPTY margins, allowed by the
V2 AIR-anchor collision limit. The test checks against the actual copper
cuboids and rejects intersections or same-facing coplanar overlays.
All four horizontal orientations share the same source geometry.

**No new physical construction blocks:** existing 5×2×5 V4.2 blueprint,
fluid ports, machine output, recipe and custom textures remain unchanged.
For an already formed tank, safely unform/reform it once so the extra
AIR proxy and Voxel V2 collision snapshots are rebuilt.

Source geometry checks were performed. Full Node contract, Gradle compile
and in-game appearance remain to be verified before merging PR #11.
