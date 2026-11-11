# Steam I reviewed prototype integration

Date: 2026-11-11
PR: #19

The rejected first Steam Engine formed-body geometry has been replaced for in-game review by the newest supplied `Steam_I_Untextured_Prototype` geometry baseline.

## Geometry contract retained

- 5 x 4 x 7 envelope.
- 85 fixed parts, 6 slider parts, 17 connecting-rod parts and 31 crank/flywheel/output parts.
- Piston stroke: 1 block.
- Connecting rod: 2.5 blocks pivot-to-pivot.
- Crank throw: 0.5 block.
- Flywheel diameter: 3.25 blocks.
- Open-channel guide revision retained.
- The optional later visible crosshead-pin cap is NOT included.

The renderer preserves one real Gear V2 angle. The generic JSON slider-crank solver now accepts an optional negative follower side; existing mechanisms remain positive by default. The crank OBJ is authored with a +90 degree rest correction and the Steam mechanism uses -90 degrees phase, reproducing the supplied source convention without scaling the rod or adding another animation clock.

## Temporary in-game rendering path

The free-mesh prototype uses Forge's built-in OBJ model loader. Four baked components are used:

- fixed body;
- crank / journals / flywheel;
- connecting rod;
- piston / rod / crosshead follower.

The real `steam_engine_output` block remains visible as the block-grid coupling housing, so an external Steel Shaft still connects on the normal Gear V2 centreline while the reviewed internal journal enters the housing. The prototype OBJ therefore omits only its separate cosmetic output-coupling solid.

V2 filler blocks remain in use for teardown and block-local collision/selection snapshots, but render the empty part during this temporary review pass. This avoids mixing the rejected static body with the prototype. It is not intended as the final performance architecture: after visual acceptance, the fixed body should be split back into chunk-baked V2 local meshes.

## Physical modules

To match the new machine:

- Steam Inlet moves to local (-2, 2, 1), through the cylinder-side wall. The current inlet only supports a horizontal flange; the final steam chest/top inlet is a later detail pass.
- Crankshaft Output moves to local (+2, 2, 5), at the flywheel/output station.
- The paid material bill remains unchanged: 22 foundations, 41 beams, 18 panels, controller, inlet and output = 84 blocks.

Patchouli Visualize is updated with the same positions.

## Voxel V2

The temporary collision profile has 90 SOUTH-source boxes:

- 85 AABBs derived from the supplied fixed convex parts;
- 5 conservative swept envelopes for slider, rod, crank, flywheel and journal motion.

These are a Minecraft collision approximation, not a claim that VoxelShape reproduces the supplied FCL convex-mesh sweep exactly.

## Verification status

The supplied prototype package reports 3,601 sampled poses with no unintended mesh penetration and independently checked Blender animation. Those are external prototype results.

This integration has static JSON/geometry consistency checks only. Forge compilation, OBJ bake, in-game visuals, animation direction/phase, FPS, save/reload, all four orientations, teardown and multiplayer are UNVERIFIED until tested.
