# Bronze II — Water Settling Tank (V4.4 formed-port wall inserts)

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
of truth for four rotated JSON formation structures, 176 filler block
models, two additional pre-baked fluid-port wall insert models, their
native-scale texture UVs and the 105-box Voxel V2 collision profile.
Only the six original weir and retaining-lip AIR hosts form.
The fluid-port inserts render inside the actual occupied module cells,
not as filler blocks or alternate shared-module textures.

The real rear, front and right-side upper modules are no longer drawn
as ornamental copper risers. They display their **real block models**. The low bronze rim,
dark masonry, ceramic lining and divider are purposefully tiled in their
own block-local parts.

The fluid BER reads real tank amounts:
- dirty rear reservoir: broad center X [-0.985,1.985], Z [-2.985,-1.29]
- muddy left rear pocket: X [-0.985,-0.205], Z [-3.60,-2.985]
- muddy right rear pocket: X [1.205,1.985], Z [-3.60,-2.985]
- clean front reservoir: X [-0.985,1.985], Z [-0.985,-0.14]
- clean side outlet approach: X [1.985,2.20], Z [-0.84,-0.20],
  only visible when fluid is above the metal sill at Y=1.135
- display level Y = 1.045 + 0.495 × fill ratio, only if non-empty.

Both real Fluid Input and Fluid Output block models occupy only the
central 12px of their 16px cells, leaving narrow empty margins. Two
dedicated, tank-only baked JSON models now draw masonry side walls,
matching bronze trim, and upper/lower support inside those same
occupied cells. The input insert is NORTH-facing; the output insert
is EAST-facing relative to the canonical SOUTH tank orientation.
The main copper frame and exterior pipe face remain visible and functional.
No generated V2 filler geometry extends into a real module block.
The water renderer uses one connected silhouette per reservoir, rather
than stretching a single rectangle through walls. Dirty Water occupies
both rear side pockets beside the inlet, keeping the solid central wall
returns dry. Clean Water approaches the outlet inside its open center
and nearly meets the front lip without clipping masonry. The extent
depends solely on synchronized stored fluid; the server process is unchanged. The sediment output item is drawn at (-0.5,1.775,0.5), over
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
- both purpose-baked port inserts fit entirely inside their own real
  module cells and do not intersect any of the generic port's nine cuboids
- no V2 filler model extends into real modules; their capabilities,
  facing, block states, interactions and collision remain unchanged
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

## V4.4 — Tank-specific walls INSIDE real fluid module cells (2026-10-24)

The V4.3 input throat (in an adjacent AIR cell) was **the wrong design**.
The intended effect is for the basin wall to continue on the LEFT and RIGHT
**within the 16px block occupied by the fluid module itself**. The same
applies to the right-side clean-water Fluid Output.

V4.4 removes the seventh AIR proxy at (0,1,-3), including its backward
model protrusion and extra Voxel V2 collision. The six original weir/lip
AIR proxies remain. The generator now emits two separately registered
baked render models:

- `multiblock/water_settling_tank_input_port_insert` (module faces NORTH)
- `multiblock/water_settling_tank_output_port_insert` (module faces EAST)

Each contains eight simple block-local cuboids: left/right masonry cheeks,
bronze beam-height trim, small steel upper brackets, a low support sill
and an inboard bronze lintel. The original module center aperture,
forward-facing fluid pipe connection and working block entity are preserved.
The output model is rotated to match the actual east-facing base model,
not the SOUTH-oriented multiblock offset convention.

The formed Settling Tank BER renders those two pre-baked models at the
precise REAL input (0,1,-4) and output (2,1,-1) locations. They are only
visible while this tank is formed. No shared module blockstates,
models, block entities, or pipe capability code were changed. Real module
blocks continue to handle interaction/collision themselves: do not use
filler proxies for that space.

**Counts:** 176 regular per-cell V2 filler models in four directions,
two additional tank-only baked models, 105 V2 collision boxes, six AIR
anchors. Recipe remains 600 mB Dirty Water -> 480 mB Water + 1 sediment
per 240 ticks. The 5×2×5 construction and physical module positions
are unchanged. Old formed V4.3 tanks should be safely unformed/reformed
once to delete their obsolete AIR throat and update cached collision;
there is no need to move construction blocks.

Regression tests verify the exact source port model cuboids, each insert's
placement and native UVs, module orientation, complete absence of solid
intersections, all four blueprint rotations and the preserved water/recipe
accounting. A full Forge build and in-game review are still required.

## V4.5 — Fluid-volume contour pass (2026-10-24)

The latest in-game screenshots showed that the dirty reservoir still
left two unfilled spaces immediately beside the rear intake. The clean
reservoir also stopped short of the right-side Fluid Output and the
front retaining wall. Neither required new blocks or textures.

**Renderer-only changes:** Dirty water is now represented by one U-shaped
top surface, partitioned into three edge-touching areas so it reaches
both rear pockets without intersecting the central inlet/wall returns.
The clean surface approaches the actual front wall, plus a short narrow
tongue into the empty center of the EAST-facing output module. This
tongue appears only once the fluid level clears its existing lower
metal sill. It stops before the copper frame and leaves its external
pipe opening unobstructed.

Each reservoir has a single explicitly defined exterior perimeter:
only outer vertical fluid faces are rendered. Inner edges between
touching rectangular surface sections produce **no duplicate translucent
quads or artificial seams**. UVs remain tiled per Minecraft block,
including around both side pockets and the output tongue.

At maximum fluid level, every generated fluid area was checked against
the 105 Voxel V2 masonry boxes and both real copper module models and
their tank-specific wall inserts; all intersections are zero. Regression
checks parse the actual renderer shape constants and compare polygon area
to the union of the top sections, plus perimeter continuity and output
sill clearance.

**No construction changes or re-form necessary for V4.5.**
The entire change to gameplay source is confined to
`WaterSettlingTankRenderer.java`; the other changes are contract tests
and documentation. No physical blocks, ports, Voxel V2 collision,
Patchouli formation pattern, textures, recipes or server-side tanks
were modified. Reload client resources or restart the dev client
before visually evaluating the result. The full Forge build and
in-game acceptance remain outstanding.

## V4.6 — Port throat fluid finishing pass (2026-10-24)

Two close-up screenshots of the formed tank identified a thin dry gap
under the real copper Dirty Water Input, plus narrow uncovered strips
beside the clean-water Fluid Output opening.

**Only the dynamic client fluid renderer has changed:**

- At low dirty-water levels the original U-shaped pool and two rear
  side pockets are maintained. Above the copper inlet's 2px sill,
  an additional **center inlet strip** from local X=0.135..0.865,
  Z=-3.22..-2.985 meets the main dirty pool. It starts after the
  real copper input geometry ends at Z=-3.25. The strip has its own
  lower liquid depth (Y=1.135) to avoid passing through the metal sill.
- The clean-water channel reaches closer to the front retaining lip
  (Z=-0.135). Its outlet approach now occupies X=1.985..2.235,
  Z=-0.865..-0.135, leaving about 0.015 block before the actual copper
  output frame and avoiding both output side-wall inserts. This small
  outlet tongue still appears only when fluid clears Y=1.135.
- There are now two calculated fluid silhouettes for each reservoir
  (below and above its port sill). On every level, touching rectangles
  share only an edge and the renderer emits just one exterior vertical
  contour, **never duplicate transparent seam faces**. Native one-block
  fluid UV tiling is unchanged.

The V4.6 contract derives its sections and perimeters from actual Java
constants. It checks the inboard tongue and clean-outlet width, shallow
and full contour area/coverage, positive-volume fluid collision against
105 Voxel V2 solids and the real copper input/output meshes and tank-only
inserts, plus unchanged recipe, four facings and Patchouli arrangement.
5,585 source-level assertions passed in a virtual-file contract run.
The full native Node suite and `./gradlew build` could not be run
from the GitHub-only environment, and visual gameplay acceptance remains
outstanding.

**Compatibility:** No rebuilding or reforming is necessary for this
renderer-only pass. Restart the development client to load the new Java
renderer. The 5×2×5 construction, textures, fluid tanks and processing
mechanics are unchanged. This leaves the art/texture revision as a
separate decision for the next iteration.


## V5 — Role-based Minecraft pixel-art textures (2026-10-24)

The V4.6 machine works as a readable two-chamber tank, but its art was too
uniform: the lower wall, upper stone, dry floor, and wet inner lining were
visually close, and an identical bronze slab repeated around the entire rim.
V5 addresses the **material design**, not machine operation or geometry.

All nine tiles are original, hand-designed **16×16 opaque RGB pixel art**,
source-defined at `tools/water-settling-tank/build-textures.mjs`.

| Resource | Visual role | Applied to |
| --- | --- | --- |
| `foundation.png` **new** | Deep charcoal-fired stone | Lower visible masonry and structural bases |
| `refractory.png` **repaint** | Warmer, lighter stone courses | Upper exposed outer walls and the real port cheek inserts |
| `lining.png` **repaint** | Smooth, pale desaturated ceramic | Inner wet walls, ceramic baffle and retaining-lip sections |
| `basin_floor.png` **new** | Slate-green sealed floor panels | TOP faces of 16px foundation cells only |
| `bronze.png` **repaint** | Restrained cast bronze with no wood grain | Straight rim rails and selected visible reinforcement |
| `bronze_joint.png` **new** | Darker bolted/fastened bronze | All four corner joins, real-port footplates and in-block port headers |
| `riveted_iron.png` **repaint** | Neutral cool industrial steel | Functional weir crest, front lip, fastened port brackets |
| `grate.png` **unchanged** | True perforated metal | Reserved for a real grate; **not** painted onto solid flooring |
| `sludge.png` **unchanged** | Earth-brown wet sediment | Actual settled-sediment item model; no fake floor deposits |

The block generator now accepts **per-face texture roles** without introducing
additional model cuboids: the same solid lower masonry block has
`north/south/east/west/down = foundation`, and `up = basin_floor`.
This is a texture assignment, not a physical waterproof layer and therefore
does not create face overlaps or different collision shapes. All faces retain
their native dimension-matching UV ranges: a two-pixel-high rail does not
stretch an entire sixteen-pixel pattern across its edge.

Likewise, original shapes are preserved but the copper rim is no longer
uniform: long straight runs use smooth cast bronze, corner pieces use darker
bronze joining plates, and the two fluid-port inserts receive matching
bolted caps. Working features use steel rather than more decorative bronze.
The four orientation-specific baked parts are generated from the same
canonical shapes and preserve their rotated dimensions.

**Exact compatibility:**
- Formation remains `5×2×5` with 37 Stone Bricks and the same four real
  working module/controller blocks.
- **105** Voxel V2 collision boxes and **six** AIR proxy hosts are unchanged.
- The V4.6 Dirty Water / clean Water client rendering code and synchronized
  tank amounts are untouched.
- Recipe remains **600 mB Dirty Water → 480 mB Water + 1 sediment per
  240 ticks**, with all previous blocking/retention conditions.
- There is **no need to reconstruct or re-form** an existing V4.6 tank;
  a resource/client reload is sufficient for revised art.
- The two original `grate` and `sludge` PNGs are retained byte-for-byte.

Reproduce/verify the artwork and models using:

```bash
node tools/water-settling-tank/build-textures.mjs --check
node tools/water-settling-tank/build.mjs --check
node tools/water-settling-tank/test-contract.mjs
./gradlew build
```

The V5 regression contract checks all nine unique tiles, 16×16 RGB format,
pixel-source reproducibility, the dry-floor top/side role split in all four
facings, functional steel at the weir and front lip, joined bronze at corners
and service ports, and unchanged UV/collision/recipe/blueprint contracts.
**The final in-game visual direction remains subject to screenshot approval.**

## V5.1 — Course alignment, dry ledges, and local light (2026-10-24)

Four test-world screenshots exposed visual defects in the first V5 materials:
masonry courses terminating or shifting at cut-off wall elements,
light-green waterproof floor tiles appearing on DRY exterior ledges,
bronze footplate side bands breaking the foundation courses, and
unusually dark/light custom port inserts.

The fixes are specific to the Settling Tank and retain its shape:

- **UV anchoring:** all generated JSON model faces now use the native
  Minecraft face crop based on each element's **from/to** coordinates.
  The former `[0,0,width,height]` reset caused a 2px support ledge
  and a 10px-high outer wall to start masonry at different texture rows,
  even though both had correct texel density. North/south/east/west/up/
  down each use its correct orientation. All four rotated variants and
  the two BER port models are generated using the same UV function.
- **Dry perimeter ledges:** exterior WEST/EAST top faces of the foundation
  use refractory stone rather than the wet-contact `basin_floor`.
  The interior floor remains glazed ceramic. The genuine clean output
  and other two service modules keep their visible bronze bearing-plate
  top textures.
- **Continuous module bases:** the sides of each 2px-tall bronze
  bearing plate now use foundation masonry; only the top is bolted bronze.
  The lower and upper segments have complementary, uninterrupted
  vertical texture UV coordinates.
- **Actual port lighting:** the formed-only input/output insert BER now
  queries `LevelRenderer.getLightColor` at each real fluid module's
  block position, with the SOUTH/NORTH/EAST/WEST rotation applied.
  Previously both inserts inherited the controller's packed light,
  even though they are several blocks away.
- **Foundation balance:** the same hand-authored 16x16 stone pixel pattern
  receives a modest brightness lift to avoid almost-black lower walls
  under ordinary directional lighting. No emissive faces or disabled
  global shading are introduced.

**Unchanged:** 5×2×5 construction, 37 Stone Bricks, all actual modules,
176 V2 static parts and two BER port inserts, 105 collision boxes,
six render-air anchors, water/dirty water display bounds, rates, tanks,
JEI, sediment outputs, all progression recipes and server processing.
No unform or structure rebuild is necessary. Java renderer changes
require a development-client restart; textures and model resource
reloads alone are insufficient for the packed-light code change.

The enhanced regression contract tests exact cropped UV coordinates on
every generated static face and port insert, complementary support
masonry courses, dry exterior ledges, correct port light sampling and
all previous geometry/fluid/recipe constraints. **8,233** source-level
assertions passed using generated files in an isolated in-memory test
harness; the real Node PNG/generator checks, full Gradle compile, and
live Forge/lighting appearance remain to be confirmed. The new foundation
PNG was generated with the source encoder and its exact Git blob SHA
verified independently.
