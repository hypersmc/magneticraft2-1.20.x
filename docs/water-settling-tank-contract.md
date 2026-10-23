# Bronze II — Water Settling Tank (V4 open-basin design)

**Status:** Source implementation on draft PR #11, not yet Gradle-built or
accepted from Minecraft screenshots.

## Purpose and material conservation

The machine passively recovers useful water from the existing Ore Washer's
Dirty Water output. It is *not* powered, and it is not an ore-production bonus.

| Input | Duration | Actual completed output |
| --- | --- | --- |
| 600 mB Magneticraft2 Dirty Water | 240 server ticks | 480 mB ordinary Minecraft Water + one Settled Sediment |

The remaining 120 mB per batch is process loss in wet sediment. Four sediment
items craft one Clay Ball. Storage remains two independent 12,000 mB fluid
tanks, and sediment is stored in the controller's actual output item slot 1.

The recipe is the existing datapack
`data/magneticraft2/recipes/settling_dirty_water.json`. Fluid-only
multiblock recipes require a nonempty fluid input. The controller simulates
both full output batches BEFORE consuming dirty water; a full clean tank,
blocked sediment slot or insufficient input pauses operation without
resetting incomplete progress. No change to Washer, pipe capabilities,
Gear V2, processing time or amounts is part of V4.

## Build — 5 wide × 2 high × 5 long

V4 deliberately **removes the four tall Cut Copper corner posts, overhead
crossbeams and inspection walkway** that dominated V2/V3 screenshots.
Only a low masonry basin and slender real-connected pipes remain.

Controller anchor local `(0,0,0)`, canonical SOUTH orientation.
Read rear (NORTH, z=-4) to front (SOUTH, z=0); each row west-to-east:

```text
                    REAR (dirty inlet)
Y=1     SSSSS    SAAAS    SAAAS    SAAAS    SAAAS
Y=0     SSISS    SSSSS    SSSSS    SSSSS    SPCSO
                    FRONT (service ports)
```

- `S`: Stone Bricks, including the real floor and low perimeter wall
- `A`: required AIR (no blocks before formation)
- `I`: real Dirty Water Fluid Input at rear-centre `(0,0,-4)`
- `P`: real Primary Item Output at front-left `(-1,0,0)` for sediment
- `C`: one controller at front-centre `(0,0,0)`
- `O`: real Clean Water Fluid Output at outer EAST edge `(2,0,0)`

All four directions have JSON blueprints generated from one source.
Six inner AIR cells form slim Voxel V2 proxies: three segments for a
**transverse low overflow weir** (`z=-2`) and three for the front
retaining lip (`z=-1`). The other six upper interior AIR cells stay open.
The model has 41 local static parts per direction (164 total), down from
48 per direction (192). The 36 old models no longer referenced by V4
have been deleted. Real ports and their accessible outside faces
remain unobstructed. World collision uses the same Voxel V2 boxes.

**Compatibility:** A V3 tank must be unformed/dismantled and rebuilt:
the structure now has TWO layers and no Cut Copper posts. Replacing
textures alone was safe in V3; the V4 geometry is not a resource-only update.

## Visual treatment: follow the process

The large rear settling pool receives Dirty Water through a small
metal riser physically attached to its real rear inlet, rather than a
hovering hopper. A low crosswise glazed weir separates muddy water from
the narrow front cleaned-water channel. A restrained right-side
downpipe reaches the existing real Fluid Output on the east edge.
The sediment output remains a real module, not a decorative bin.

Fluid surfaces now occupy the **full interior width** in both reservoirs:

- Dirty: rear two rows, from local Z -3.53 to -1.29
- Clean: front collection row, from local Z -1.10 to -0.24
- Both: interior X -1.28 to 2.28, with a server-driven height
  `1.045 + 0.535 × fillRatio` above controller origin (only if nonempty)

At full display height the water just reaches the low weir; it stays below
the front retaining wall. The renderer draws native-tiled water texture,
translucent surfaces and visible vertical sides, not a solid brown floor.
These are inventory-backed visual representations of the two tanks, **not
a physical fluid-flow simulation** or a new transfer mechanism.

Six original 16×16 V3 textures are deliberately retained:
`refractory`, `lining`, `bronze`, `riveted_iron`, `grate`
and `sludge`. Geometry uses native pixel-span UVs, without stretching
tiles over multiple blocks or applying artificial noisy highlights.
The metal grate is a small floor drain guard, not another bridge.
Sediment material on the bed is only a static stain; actual extractable
sediment is shown from the real stored output slot.

## Generation and verification

```bash
node tools/water-settling-tank/build.mjs --check
node tools/water-settling-tank/build-textures.mjs --check
node tools/water-settling-tank/test-contract.mjs
./gradlew build
```

The generator emits four formation JSON files, per-cell models,
the Voxel V2 profile, block/item models, recipes and loot. The contract
tests four rotations, native UV density, unblocked module cells, six AIR
anchors, absence of old upper posts, plumbing continuity, fluid-level
bounds, no water duplication, book layout and texture signatures.

The geometry generator was checked for output bounds and model counts
before committing. The complete Node contract, Forge build and in-game
appearance have **not** been verified in this chat.

**In-game review:** rebuild from the book; test all four facings,
dirty-only / clean-only / both-full / both-empty, visual separation,
rear inlet connection, east-edge output access, sediment visibility and
extractability, blocked outputs, chunk reload and multiple formed tanks.
View both from walking height and above: the two visible water stages
should dominate, not copper rails. Watch for Z-fighting and any missing
stone support or invisible collision. PR #11 stays draft until accepted.

## Prior iterations

- V1: bare stone trough with flat-looking fluid and unrelated wood.
- V2: custom refractory/ceramic/metal details, but too many beams/posts
  and a front bridge obscuring the treatment area.
- V3: original simple, hand-painted pixel textures; these were retained.
- V4: process-first geometry and fluid layout, low 5×2×5 silhouette
  and smaller functional fittings, without changing the processing contract.
