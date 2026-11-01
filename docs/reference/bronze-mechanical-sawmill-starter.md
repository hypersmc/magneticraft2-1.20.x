# Mechanical Sawmill K1 — editable 3D starter

This is a **sample for later Studio model work**, not the actual V2 machine renderer model. The source on draft PR #11 remains `tools/multiblock-models/designs/bronze_mechanical_sawmill.json`.

- **Blockbench 5+**: open `bronze-mechanical-sawmill-starter.bbmodel`. It contains named editable groups, baked-in 16×16 placeholder material colors, cube-based frame/gearbox, and a separate **24-tooth saw mesh**.
- **Interchange**: `bronze-mechanical-sawmill-starter.obj` plus the matching `.mtl` file can be imported in other 3D editors. The OBJ has material groups but no animation pivots.
- The **blade** group pivots at `(8,24,-4)` around local Z. The **real Gear V2 coupling** pivots at `(8,8,-24)` around local Z. The moving log is only a hidden/export-excluded example; item rendering still happens in the game.
- Toggle the hidden `08_Port_reference_GUIDES_HIDDEN` group while editing. Real modules are at rear `x0..16,y0..16,z-32..-16`, left input `x-32..-16,y0..16,z-16..0`, right output `x32..48,y0..16,z-16..0`, and front controller `x0..16,y0..16,z0..16`.
- K1 spans **5 blocks wide, 3 high, 3 deep** (model px bounds: x `-32..48`, y `0..48`, z `-32..16`). All static cuboid elements are <=16 px along each axis, using native-density placeholder UVs.
- The rear gearbox is deliberately **enclosed**, transferring the low rear input toward the raised blade axle. The sample does not assert a gear-tooth ratio or replace the mechanical simulation.
- When your final Studio model is ready, migrate the geometry back into the existing canonical design and use its V2 asset generator. Keep the blade and module coupling in dynamic rendering; **do not** directly replace the machine's generated in-game assets with this sample.

Files here are source examples, not a build verification. Blockbench GUI import and Minecraft rendering must still be checked. Original separately downloadable GLB sample and visual preview are also available from the chat deliverable.
