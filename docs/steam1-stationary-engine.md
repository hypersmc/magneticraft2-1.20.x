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
- heavy foundations, Iron Structural Beams and Iron Plate Panels make up the paid structure;
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
