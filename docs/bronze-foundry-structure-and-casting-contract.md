# Bronze Foundry — structure and casting contract

**Status:** PR #11 first in-game **structure/visual prototype**. Formation, facing, model, collision and Patchouli Visualize are included. Fuel, heat, molten bronze and casting are **NOT implemented in this slice**. Right-clicking the formed controller says so.

## First build

A 3 wide × 2 high × 3 deep JSON multiblock. The bottom-front centre is the controller and tapping/front access side.

```text
Floor (y=0)        Upper layer (y=1)
B B B              B B B
B B B              B _ B
B C B              B _ B

B = minecraft:bricks, C = magneticraft2:bronze_foundry
_ = air, front toward bottom
```

**15 Bricks blocks + 1 Controller + 2 air.** There are four genuinely rotated JSON layouts, a model for each facing, and a physical collision shell that leaves the upper chamber and front opening empty. The furnace now has a **recessed lower firebox**, a stepped, visibly empty refractory crucible above, shorter upper walls with corner buttresses, bronze reinforcement, and a physically joined front tapping channel with a valve. Visual geometry is generated from named source volumes and tiled surface panels. Model coordinates and UVs are clipped into legal block-local bounds without stretching or compression.

Recipe: 5 Bricks blocks, 2 Copper Plates, 1 Furnace, 1 Bronze Ingot make the controller. The remaining 15 Bricks blocks are placed in-world. The pre-existing Copper/Tin/Bronze ingot loop remains intact.

## Functional design for the following slice (not implemented)

1. **Heat and charge.** Foundry melts already-made Bronze Ingots, not Bronze Alloy Charge and not ore. Use the existing Magneticraft HEAT capability/storage; track fuel energy entering a thermal mass, cooling losses and a melt-readiness threshold. Heating must cost fuel and time, and survive chunk reload without duplicating charge. Plan forced-air upgrades around Copper mechanical Bellows. Final temperatures, heat capacity, fuel efficiency and melting speed are not yet approved: make them configurable rather than inventing real temperatures.
2. **Visible states.** Discrete, visible fuel chamber and bronze charge zones. Empty crucible is dark; heated molten metal has a distinct fill level and glow. On stalls the metal remains in one recorded state and never consumes a second ingot accidentally.
3. **Dedicated front tap.** Molten Bronze is a measured, persistent amount, NOT a normal placeable bucket fluid. Begin with a refractory pouring ladle/crucible, then add a Casting Basin receiver. Verify donor/receiver state and capacity before a single atomic transfer. Ordinary water pipes cannot carry molten metal by default.
4. **Casting Basin.** Consumes one of the new Gear/Bearing Green Sand Molds for a valid pour. The *wooden* patterns remain reusable at the separate Mold Table. Reject incorrect molds, quantities and insufficient molten stock without deleting either input. Cool into **rough bronze castings**, not precise finished gears.
5. **Mechanical Lathe.** Gear V2 powers precision finishing of the rough castings; torque/load and tool wear matter. Bronze parts carry greater mechanical loads but never generate extra power. Preserve material-quality metadata through the full path, with actual tier rules undecided.
6. **Safety/wear.** Refractory liner/lid/service parts should be replaceable and visibly worn, not a universal durability bar. Cooling/solidification and safe manual metal handling need explicit acceptance tests.

### Test the structure prototype

- Each of the four Patchouli orientations forms, even if the controller was placed in a different direction.
- Visual front/tap matches the actual controller for every orientation.
- Upper chamber and access opening stay hollow (no invisible whole-cube collision).
- Textures are neither missing nor stretched; corner bracing and spout visibly connect.
- Chunk reload and client multiplayer restore formed geometry, and breaking formed/unformed structure does not leave invisible fillers or dupe blocks.
- Only the Foundry *shell* exists at this stage: it must not silently convert items into castings.

The next implementation step is heat + measured molten storage + safe ladle-to-Basin pouring as a separately testable system.


## Rendering V2 migration (PR #11)

The original oversized Foundry shell was not loadable as one vanilla model; V2 now splits the one canonical south model into 16 controller/filler-local meshes per facing, baked into chunk geometry. Static visuals and local collision survive an unloaded controller chunk. See `docs/multiblock-rendering-v2.md` for the script, limitations and regression checklist. Heat and Casting Basin are still not implemented.

## Foundry visual redesign (PR #11)

The source-of-truth visual volumes are in `tools/multiblock-models/designs/bronze_foundry.json`. Recompile them using `node tools/multiblock-models/build-designs.mjs`, then regenerate the 64 block-local assets with `node tools/multiblock-models/generate.mjs`. Do not edit the generated files directly.

The raised crucible is open and dark when cold, and the tap is presently a static visual (no fuel or molten metal can flow). The large front firebox is hollow, with an inset refractory lining and grate. Upper brickwork forms low retaining walls instead of enclosing the entire second layer; the original 15 Bricks + Controller multiblock layout and crafting requirements have not changed.

The Foundry has a **hand-authored simplified collision outline** that now follows the open firebox and raised hollow crucible rather than its old solid brick bounding box. It is not yet the generalized Voxel V2 implementation. Verify hitting the firebox, reaching the crucible from above, walking near the spout, all four facings, and dismantling after formation.

## V2 collision anchors for the open crucible/tap

The assembled model extends into the two required-AIR cells of the upper 3×2×3 structure. Before forming they stay AIR and do not change the construction instructions. The JSON `settings.render_air_anchors` now explicitly names those controller-relative locations; they are replaced with non-occluding render/collision proxy blocks **only after formation**. The proxies receive per-block cached collision and model IDs and restore to AIR on dismantle. Re-form Foundries built with older code to install the new anchors. The tap nose projects 7/16 of a block beyond the outer front face. Each V2 required-air proxy can preserve up to half a block of its real, intersected collision shape beyond its local cell, so that short projection can now provide support. This is not a general collision system for large overhangs beyond a structure; the full Voxel V2 design still needs dedicated anchors.
