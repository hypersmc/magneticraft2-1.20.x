# Bronze II — Water Settling Tank

**Status:** Source-implemented in draft Bronze PR #11. The machine is not yet
Gradle-compiled or accepted from in-game screenshots.

## Function and material accounting

Passive sedimentation turns **600 mB Dirty Water** produced by the existing
Ore Washer into **480 mB clean Minecraft Water** plus **one Settled Sediment**
over **240 server ticks** (12 seconds at normal TPS). The unrecovered
120 mB is retained in the wet sediment for accounting purposes; this is a
process loss, not water duplication. Four sediment units can be crafted into
one Clay Ball. No metal-bearing tailings or bonus ore is produced.

The balance lives in the fluid-only datapack recipe
\`data/magneticraft2/recipes/settling_dirty_water.json\`. The generic
\`multiblock_processing\` serializer now supports missing item input **only when
a nonempty fluid_input exists**. Existing Grinder/Washer/Sifter/Stamp
recipes keep their item input requirements and item matching.

Inputs and outputs are server-owned: 12,000 mB Dirty Water in the inlet
reservoir; 12,000 mB clean Water in the outlet reservoir; one item slot at
controller slot 1 for settled sediment. **No Gear V2 node, Watts or
pressure system is required**, because the settling is passive.

If either output cannot accept the full result, input remains untouched and
the 240-tick progress freezes. Processing also stops safely on fluid shortage,
reload or an unformed structure. Production commits only after checking
both output destinations. The machine stores both fluids, sediment inventory,
processing state, active recipe ID, formed blueprint and V2 voxel snapshots.

## Exact physical build (canonical SOUTH)

**5 wide × 3 high × 5 deep**, controller at local \`(0,0,0)\`,
rear Z=-4 to front Z=0. Rows are west-to-east, shown rear to front.
The clean-water port sits at x=+2, on the exposed outer EAST edge:

\`\`\`text
Y=2     W___W    _____    _____    _____    W___W
Y=1     SSSSS    S___S    S___S    S___S    S___S
Y=0     SSISS    SSSSS    SSSSS    SSSSS    SPCSO
\`\`\`

Legend: S = Stone Bricks; W = Oak Planks; I = the REAL
Multiblock Fluid Input; P = REAL Primary Item Output for sediment;
C = controller; O = REAL Multiblock Fluid Output; _ = required AIR.

Full port cells and their immediately outward-facing service faces stay
free from generated V2 decorative models. The floor is ground-supported,
all four upper posts sit on the masonry walls, and the central divider
contains six actual AIR-hosted static V2 pieces. The controller is at the
south/front centre. Models are generated in all four orientations.

**Source of truth:** \`node tools/water-settling-tank/build.mjs\` regenerates
the four JSON formation layouts, native-density static baked per-cell meshes,
collision profile, basic item/block models and the recipe files together.
Do not manually edit the 180 generated part models. Their six render AIR
anchors are formed only after the structure validates.

Voxel V2 owns collision/selection for the physical floor, side walls, rear wall,
baffle, discharge lip and four corner posts. Fluid surfaces in the BER only
depict the actual synchronized Dirty/Clean tank fill; no decorative phantom
fluid entities are spawned. The only rendered sediment item appears when
the controller's stored sediment output slot is nonempty.

## Ports and basic use

The real rear-centre **Fluid Input** accepts the Forge Dirty Water fluid from
the Ore Washer via existing water pipes or a connected fluid tank. The
right-edge **Fluid Output** drains only newly recovered Minecraft Water.
It faces **EAST** from controller offset (2,0,0), leaving its outward pipe
connection in open air rather than against the masonry foundation. Recovered
water can be piped/tanked and fed to the Washer's existing input.
The front-left **Primary Item Output** exposes controller slot 1 for
sediment extraction. The controller is status-only; it is not a separate
item insertion/extraction interface. It reports both tank fill levels,
sediment count and settling progress.

This machine is deliberately usable without electricity or gears.
There is no new fluid network and no change to the Washers, Sifter or
other Bronze machine animation. Automatic insertion/extraction is
performed through the existing module blocks.

## Validation

\`\`\`bash
node tools/water-settling-tank/build.mjs --check
node tools/water-settling-tank/test-contract.mjs
node tools/multiblock-models/build-designs.mjs --check
node tools/multiblock-models/generate.mjs --check
./gradlew build
\`\`\`

**Gameplay matrix (not yet completed):** Build and form all SOUTH/WEST/NORTH/EAST
layouts from Patchouli. Check the floor/wall contacts, upper corner posts,
internal baffle, physical ports and actual filled-fluid levels. Confirm
the rear Fluid Input faces NORTH into air, the clean Fluid Output faces EAST
into air and the sediment port is accessible from the southern service row. Feed 600 mB
Dirty Water and verify 480 mB clean output, one sediment item and zero
extra items. Fill the clean output then sediment port independently: the
process must pause, preserve exact quantities and resume; test a mid-batch
world unload/reload and dismantle. Drain clean water into Washer and check
it remains standard Minecraft Water. Ensure empty ports have no ghost
previews, collision isn't a solid invisible lid, and FPS is stable.

**Caveat:** This is source-level acceptance only until the full project build
and live Minecraft testing have passed. Preexisting worlds contain no such
formed tank, so no legacy-structure migration is required.
