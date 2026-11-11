# Steam I — stationary horizontal engine

**2026-11-10 — draft PR #19. Source + resources implemented; Forge/Minecraft verification pending.**

## Role
First true Steam consumer and first Steam-age mechanical source:
`Boiler -> Steam Pipe -> Engine -> Gear V2 shaft network`.

It does not generate electricity. Dynamo/generator work follows only after this engine is verified.

## Physical contract
- footprint: 5 wide x 4 high x 7 long;
- controller: rear-centre foundation cell;
- physical Steam Inlet on cylinder side;
- physical Crankshaft Output on opposite crankshaft side;
- exact paid structure: 22 Iron Machine Foundations + 41 Iron Structural Beams + 18 Iron Plate Panels + controller + Steam Inlet + Crankshaft Output = **84 blocks**;
- formed visual is a large horizontal single-cylinder engine with bed, cylinder, crosshead guides, crankshaft and large side flywheel.

Four JSON blueprints are generated from the canonical SOUTH layout. Inlet/output modules survive formation as real BlockEntities.

## Motion
The client uses the existing production `slider_crank` constraint solver already used by the Mechanical Sifter:
- shaft phase comes from the real Gear V2 output node;
- crank radius 0.38 blocks;
- rigid rod length 1.8 blocks;
- piston/crosshead displacement is solved from that same shaft angle;
- flywheel uses the same output angle;
- no independent animation clock, stretched connecting rod or entity workaround.

## Runtime
Default datapack rules:
- 120 kPa minimum inlet;
- 500 kPa rated;
- 12 -> 32 RPM pressure range;
- up to 32 torque;
- 4 milli-Steam/tick idle;
- +1 milli-Steam/tick per source-equivalent torque demand;
- max draw 2 whole network Steam units/tick into a conserved milli-Steam working credit.

Gear V2 still owns ratios, overload, flywheels and consumer loads. The engine reads the previous/most-recent source demand only for Steam metering.

Exhaust Steam is explicitly accounted and released to atmosphere in this first prototype. Condensate is deferred.

## Required in-game regression
1. Form all four directions from Patchouli/real blocks.
2. Confirm the inlet and output modules remain visible/physical after formation.
3. Connect Steam Pipe only to the inlet's outward face.
4. Below 120 kPa: no Gear V2 source motion.
5. Raise inlet pressure and confirm RPM rises toward 32.
6. Connect a Steel Shaft to the output and verify existing gears/ratios work.
7. Add real machine loads and confirm Steam consumption increases with demand.
8. Overload beyond available torque: output stalls through Gear V2; linkage/flywheel must visually stop with it.
9. Remove Steam: engine stops; no fake animation.
10. Save/reload while running and stopped.
11. Break controller, inlet and output separately; the original multiblock must restore exactly once.
12. Inspect all camera angles for floating piston/rod/flywheel, intersections, UV stretching and FPS regression.
13. Dedicated two-client synchronization remains required before completion.

A full Forge build and Minecraft runtime test have not been performed by this implementation pass.


## Rendering/physics implementation notes — 2026-11-10

The first single-controller 7-block static model draft was deliberately replaced before runtime testing. Standard Minecraft baked block models are not used as one 112-pixel-long object. The current implementation uses the established Multiblock Rendering/Voxel V2 architecture:

- controller and each formed filler own only block-local baked static geometry;
- the original coherent stationary-engine body is clipped into 83 visible local fragments per facing, preserving its continuous bed/frame/cylinder shape instead of repeating one generic beam model for every paid beam;
- clipped cuboids remain inside normal 0..16 model bounds and their UV ranges are cropped proportionally, never stretched to fit a cell;
- 29 blueprint-air cells per facing become explicit non-occluding proxies: 27 are occupied by the coherent static body and two additional anchors cover the moving crank/rod/flywheel swept area;
- a 213-box Voxel V2 profile is compiled once on data reload and clipped into block-local collision/selection snapshots on formation;
- only crank, rigid connecting rod, crosshead/piston and flywheel use the dynamic renderer;
- every moving model was re-authored within standard baked-model bounds and all checked UVs remain proportional.

Static validation performed on the committed resources:
- static split validation preserves 92,652 model-pixel² of original visible surface per facing exactly, with 83 local fragments, no element outside 0..16 and no generated UV outside 0..16;
- directional required-air anchors, V2 profile selection and static palette availability remain part of runtime verification;
- moving/module model element bounds and proportional UV mapping remain part of the established static checks;
- 22 source assertions for V2 ownership/render hooks and Steam/Gear registrations.

These checks are not compilation or Minecraft visual acceptance.

## Current verification state

**Implementation:** source/resources are present on PR #19.

**Static verification:** the structure, render palette, animation definitions, module registration and Voxel V2 resource contracts pass the checks listed above.

**Compilation:** unverified. The available build container could not resolve GitHub while attempting to obtain a checkout, and no CI status is attached to the tested commit.

**Runtime/visual verification:** unverified until the engine is built and tested in Minecraft.
