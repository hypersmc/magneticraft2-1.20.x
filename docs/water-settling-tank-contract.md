# Bronze II — Water Settling Tank (Visual Redesign V2)

**Status: PR #11, source implementation awaiting Gradle + in-game acceptance.**
The original trough was rejected after video review. This redesign changes the
real block-level construction, baked local geometry, collision and textures,
not just a recolor of Stone Bricks.

## Role and fixed processing contract

The passive water-recovery machine remains unchanged economically:

| In | Time | Out |
|---|---|---|
| 600 mB Magneticraft2 Dirty Water | 240 server ticks | 480 mB ordinary Minecraft Water + 1 Settled Sediment |

The missing 120 mB remains bound in wet solids. Four Settled Sediment craft
into one Clay Ball. Two separate 12,000 mB server-side fluid tanks back the
dirty and clean displays. The sediment item is held by actual output slot 1.

The recipe lives in \`data/magneticraft2/recipes/settling_dirty_water.json\`.
The generic multiblock recipe decoder supports no item input ONLY if
\`fluid_input\` exists. No new metal byproduct, no arbitrary Gear V2 load,
no added water from nothing and no change to Washer/pipe semantics.

The server checks BOTH the clean-water output capacity and the sediment slot
before consuming a batch. Full outlets or insufficient dirty input pause
progress and retain their contents. Active recipe, tanks, sediment,
formation state and voxel snapshots persist across unload/reload.

## Formation (canonical SOUTH orientation)

Still **5 wide × 3 high × 5 deep**, with the controller at local (0,0,0).
Read each row west → east, from the back (north) toward the front (south):

\`\`\`text
                  NORTH / rear
Y=2    R___R     _____     _____     _____     R___R
Y=1    SSSSS     S___S     S___S     S___S     S___S
Y=0    SSISS     SSSSS     SSSSS     SSSSS     SPCSO
                  SOUTH / front
\`\`\`

- S: Stone Bricks, the original hidden construction foundation / masonry
- R: **Cut Copper** cast-stanchion supports; these REPLACE the old Oak Planks
- I: existing REAL Multiblock Fluid Input (rear centre, NORTH service face)
- P: existing REAL Primary Item Output (front-left, for stored sediment)
- C: Water Settling Tank Controller (front centre)
- O: existing REAL Fluid Output (far EAST edge, outer service face)
- _: required AIR; do not put solid filler blocks here before forming

**Breaking and rebuilding is required** for a tank already formed with the
old Oak Planks layout. The controller recipe, sediment recipe, and processing
contract have NOT changed.

The nine air-hosted V2 model pieces represent an actual longitudinal
ceramic-bronze divider, rear silt-trap grating, two front retaining lips and
three elevated grated bridge segments. All other required air remains open.
The generated per-cell models and collision cannot overlap a real module.

## Design identity and custom materials

This is now an engineered Bronze Age treatment installation rather than
a generic untextured stone trough:

- Refractory dark masonry, separately glazed waterproof cyan-green lining
- Cast bronze rim/beading, riveted dark-iron frames and upper Cut Copper posts
- Raised internal weir dividing **two similarly sized side-by-side bays**
- Rear metal inlet sleeve terminating over the muddy chamber
- Outer right clean-water crossover fitting, aligned with the real outlet
- Thin grated inspection bridge and visible sediment trap below the dirty bay

Six purpose-authored **16×16 pixel textures** live at
\`assets/magneticraft2/textures/block/settling_tank/\`:
\`refractory.png\`, \`lining.png\`, \`bronze.png\`,
\`riveted_iron.png\`, \`grate.png\` and \`sludge.png\`.

Each box uses actual pixel-span UVs and remains inside its block-local model,
rather than stretching one 16×16 texture across the multiblock. They are
normal standalone resources referenced by the generated baked meshes,
not illustrations or pre-rendered sprites.

The liquid BER uses the actual Minecraft fluid still texture and real
controller tank fill ratio. Dirty (left) and clean (right) basins both occupy
roughly equal internal floor area. Their 3D translucent volumes include
side faces and floor clearance. Texture UVs are cropped per one-block tile;
no giant single UV or flat dirt-colored floor. Neither water volume appears
when its real tank is empty. No particles or new item entities are created.
Sediment remains visibly displayed only when the output inventory contains it.

## Generator / tests

\`\`\`bash
node tools/water-settling-tank/build.mjs --check
node tools/water-settling-tank/test-contract.mjs
./gradlew build
\`\`\`

The canonical JavaScript generator owns **four directional blueprints**,
**192 block-local JSON meshes**, the JSON Voxel V2 collision profile, controller
and sediment models, existing recipes and loot. Nine required-air pieces are
registered only upon formation; stale meshes from the old arrangement were
removed. The independent test checks all rotations, 16×16 PNG signatures,
custom material refs, UV density, empty real module collision cells, ports,
conservation, and the Patchouli projection.

**To test in game:** rebuild an old tank, then form SOUTH, WEST, NORTH and
EAST from the Patchouli Visualize projection. Inspect clean and dirty bays
empty / partly full / full; check that the baffle actually divides the liquid,
the output pipe faces EAST and the bridge has accurate hitboxes. Feed 600 mB
Dirty Water; ensure exactly 480 mB normal Water and one stored Sediment after
240 ticks. Block each output separately, then reload the world mid-batch.
Ensure no material loss, fake items, FPS drops or stretched texture polygons.
Use the client video to evaluate whether the inlet/outlet look connected.

The complete Forge Gradle build and in-game visual acceptance remain
**unverified** until they are explicitly run. Keep PR #11 draft/unmerged.
