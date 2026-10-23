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

- Muted warm-gray refractory masonry, smoothly glazed neutral sage-gray lining
- Restrained warm-bronze rims, clean riveted neutral iron and upper Cut Copper posts
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
node tools/water-settling-tank/build-textures.mjs --check
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


## Texture-only redesign V3 — 2026-10-23

The V2 in-game review showed that the previous textures looked overly noisy,
heavily pre-shaded and visually disconnected, like a collection of borrowed
game materials. The underlying V2 model and flow contract were not rejected
by this texture decision. **V3 changes only the six pixel textures and their
reproducibility/validation documentation; it does not alter geometry,
formation, item/fluid ports, world saves, recipes, physics, animations or
collision.** Old already-formed V2 Settling Tanks need no rebuild for V3.

The new hand-authored palette replaces scattered procedural speckles with
deliberately painted 16x16 pixel clusters. All six textures are unique
8-bit opaque RGB PNGs using 5–11 colors apiece:

- **refractory:** muted warm-gray large masonry courses, subtle mortar;
- **lining:** pale desaturated sage-gray ceramic panels, minimal seams;
- **bronze:** warm copper-brown cast plate with restrained highlight and bolts;
- **riveted_iron:** cool neutral-gray metal plate with two clear fasteners;
- **grate:** large readable steel bars and dark negative spaces;
- **sludge:** quiet earth-brown, gently layered wet deposits.

No palette is copied from a stock texture or another game's asset. Lighting
comes from Minecraft, not from high-frequency painted noise. Each 16x16 tile
retains the *same resource name* as V2 and is applied at the existing native
UV density, so models and their texture references remain untouched.

The six source-defined pixel grids are at
\`tools/water-settling-tank/build-textures.mjs\`; this script uses built-in
Node PNG encoding without external dependencies. Use \`--check\` to ensure
the committed PNGs exactly match the editable source. The main structure
asset generator is intentionally unchanged. In-game approval is pending
a resource reload and a visual comparison at typical block-view distances.
