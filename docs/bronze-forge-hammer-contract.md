# Mechanical Forge Hammer — Bronze III first functional slice

**Status: source candidate on draft PR #11, compile and in-game tests pending.**
The canonical Bronze Age production plan remains authoritative. The Forge
Hammer starts the Bronze III manufacturing branch without forcing unimplemented
Iron Age ingredients or creating unused intermediate items.

## Initial, deliberately narrow gameplay loop

- **Footprint:** 5 wide × 4 tall × 3 deep, with a JSON blueprint and actual
  Patchouli multiblock guide for SOUTH, WEST, NORTH and EAST.
- **Input:** a real front-left Item Input module; **output:** a real front-right
  Primary Output module. A real rear-center Mechanical Input Module is the
  Gear V2 input. No fake physical port or new rotation network.
- **Recipe:** 4 `minecraft:copper_ingot` → 4
  `magneticraft2:copper_plate`, requiring 160 *supplied* ticks, speed ≥20
  and torque ≥5. Quantities, duration and mechanical demand are standard
  `magneticraft2:multiblock_processing` JSON values. The outputs are
  existing real Copper Plates; no intermediate or arbitrary yield increase.
- **Design distinction:** this is a powered, sustained, four-item **cold copper**
  forging operation. The Primitive Anvil's one-item manual four-strike route
  remains available earlier. The later authentic **heated blank → forged
  part → Rolling Mill/Press** chain must be implemented when useful outputs,
  HEAT preparation and material-quality propagation are established.
  Do NOT call this a completed hot forge or invent temperatures.

## Mechanical and maintenance contract

- Use `GearNetworkManager.setMechanicalLoad` and
  `getMechanicalLoadState` with the actual `mechanical_input` module.
  Progress occurs only with adequate speed, torque and supplied load.
  `removeMechanicalLoad` runs on idle, blocked output, failed/worn die
  and teardown; avoid persistent ghost loads after normal machine destruction.
- Reserve output via simulated insertion before removing input, and insert
  the exact recipe output once after completing its active ticks.
- Persist `ForgeProgress`, `ForgeRecipe`, `ForgeActiveInput`,
  `ForgeDie` and the base inventory/multiblock data. Input identity/tags
  changing invalidates partial progress. Unloaded chunks do not run.
- The reusable forging machine starts with a pristine bronze die, like the
  established Stamp Mill's starter shoe. The die loses one of **384**
  cycles per successfully completed batch. Right-click with a
  `bronze_forge_die` replacement to reset wear. Sneak-use empty-handed
  reports wear. A worn-out die stalls safely without applying mechanical load.
  Dies never enter normal input/output automation slots.
- The controller does not expose its inventory capability. Existing REAL
  ports enforce input acceptance and the proper output slot. Input/output
  access survives formed-state save/reload.

## Studio-ready provisional visual model

The frame is authored in `tools/multiblock-models/designs/bronze_forge_hammer.json`.
The existing source→faces→block-local model generator produces static models
for all four facings and collision from the same Voxel V2 geometry. The
stationary frame uses stone, spruce, cut-copper and dark iron-like vanilla
textures with native-sized UV mapping. The separate
`bronze_forge_hammer_ram` baked model moves vertically with the Gear V2
input module's actual visual shaft phase. Only the ram and current input item
are rendered each frame; no item entity, model re-bake or per-tick voxel update.

**Not yet accepted as final artwork.** Keep the original Magneticraft2 design
references and rebuild the machine in Studio when that capability is ready.
Neither an animated drive ratio nor heat-source linkage to the ram is
physically modelled beyond the present mechanical input and guided die.
Do not fabricate a bevel gearbox solely as decorative filler.

## Regression test checklist

1. Run `node tools/multiblock-models/build-designs.mjs --check`,
   `node tools/multiblock-models/generate.mjs --check`,
   `node tools/bronze-forge-hammer/test-contract.mjs` and `./gradlew build`.
2. Form each of four facings with the book, including AIR regions and real
   item/mechanical modules. Try an incomplete structure (must not form).
3. Feed 3 ingots (no work); add the fourth. Apply suitable speed and torque
   through Gear V2, check exactly four plates after one batch.
4. Stop/overload the drive mid-batch and ensure progress pauses and load is
   released. Resume without free work. Full output blocks work before consuming
   ingredients.
5. Save/reload during forging, with an unworn and a nearly broken die.
   Check no duplication, exactly-once output and retained wear.
6. Inspect die, replace at worn/broken state, confirm spent replacement item
   and resumed work; test normal teardown and item drops.
7. Examine the moving ram and actual anvil from all four camera directions.
   Verify it has a supported guide path, visible power module and no floating
   parts, UV distortion or severe FPS loss.
8. Run a separate two-client test for ports, model synchronization,
   save/reload and mechanical network behavior. Verify the Primitive Anvil,
   Stamp Mill and all working kilns remain unchanged.

## 2026-11-01 — Formation guide regression: Patchouli X-mirror

The first Forge Hammer in-game formation report failed on SOUTH at the real
front port: expected `multiblock_item_input`, found
`multiblock_item_primary_output` (world x=21, y=56, z=-43). The other
warnings came from the normal scan over nonmatching rotated layouts. The
server's SOUTH blueprint is correct and explicitly places **U at local
x=1, z=2** and **O at x=3, z=2**, but the newly-authored Patchouli pattern
had copied those raw rows verbatim. As already done in the *working*
Ore Roasting Kiln's book pattern, the Patchouli visual needs a **horizontal
X-axis mirror of the JSON layout**. The bottom displayed book row is now
`SO0US` rather than `SU0OS`; the server structure and Gear V2 input,
generated models, collision, recipes and I/O semantics are unchanged.
A source-contract test requires the book to mirror the entire SOUTH blueprint
to prevent a future copy-paste regression.

**Existing in-world construction built from the old book preview:** swap the
two front item port blocks; the previously positioned Primary Output at
the server's U location must become an Input, and vice versa. The rear
Mechanical Input is correctly placed (first rotated-candidate warning was
not a real mechanical-module problem). Follow the revised Patchouli
visualization for fresh builds. Verify actual formation in-game before
declaring this bug resolved.
