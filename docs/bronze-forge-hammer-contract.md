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

## 2026-11-01 — Visible input but no hammer movement

A formed Forge Hammer visibly shows the copper stock on its anvil even when
there is no mechanical power. The initial K1 recipe **requires both 20 RPM
and 5 torque**, not just four Copper Ingots in the input. A standard small
water wheel produces at most 16 RPM without suitable gearing; simply
attaching an unpowered wooden shaft does not operate this industrial press.
The existing wooden **Large Gear With Handle** is a temporary source: one
activation runs for **100 ticks**, less than this forge's **160 supplied
ticks per batch**. If the handle has enough transmitted torque and speed,
its first activation should partially progress the batch; operate it again
or use a continuous source to finish. This is not a broken forging recipe.

**Do not reduce the required RPM/torque or invent free motion to hide an
underpowered network.**

The server-side status on an ordinary right-click now reports the actual
input count, required recipe count, observed and required Gear V2 RPM/torque,
whether the mechanical load is supplied, and a concise reason for being
idle. It also distinguishes missing power modules, overloaded networks,
blocked output, a broken die, invalid/undersized ingredients and a missing
recipe. This is a gameplay diagnostic, not proof that the user's particular
shaft is unpowered; check the in-game numbers to identify the real cause.

**Regression:** form a Forge Hammer, insert exactly four ingots and inspect
the status with (a) no connected power, (b) a small waterwheel,
(c) an adequate larger source, (d) deliberate overload and (e) a blocked
output. Ensure the numbers/reasons are correct and the machine runs only
under actually supplied mechanical load. Test independently of the
provisional visual model and check for no additional per-tick allocations.

## 2026-11-01 — Stalling with 48.6 RPM and 8 torque (large water wheel)

Player's in-game status: **Input 4/4, RPM 48.6/20, torque 8.0/5.0,
Running false, Load supplied false, progress 0/160**.

**Root-cause candidate verified in the code:** K1 queried
`node.getEffectiveSpeed()` for `canAttempt`, then passed that boolean to
`setMechanicalLoad(..., active)`. Gear V2 intentionally masks
`getEffectiveSpeed()` to ZERO while overloaded, but **preserves raw speed**
via `getSpeed()` so source-path and gear-ratio calculations remain valid.
This caused the Forge Hammer to *deactivate its mechanical load as soon as
a source overloaded*; on the next network evaluation the source recovered,
which reactivated the load, cycling without completing a batch. Switching
the active-load qualification to **raw `getSpeed()`** stabilizes the
demand. The actual processing decision still uses the manager's
`MechanicalLoadState.supplied()`; a genuinely overloaded source can NOT
produce output or consume ingredients.

**Important:** enough local RPM and torque does not guarantee the water
wheel can supply the source-equivalent load. Gear V2 correctly converts:
`source_demand = local_torque × (input_RPM / source_RPM)` and sums all
active machine demands on that source. E.g. 5 torque at 48 RPM driven by
a 24 RPM wheel requires **10 torque at the wheel**, not 5. A higher gear
ratio increases speed while reducing available force. The *actual* source
RPM/torque and other loads are not known from the screenshot; do not
assert that the user's source is definitely overloaded.

The controller now also displays the load's **source-equivalent demand,
total upstream demand, source capacity, active state and source presence**.
If total demand exceeds source capacity, ease the speed-up gearing (aim
around 20–24 RPM directly from a well-powered large wheel) or reduce
other loads before assuming the Forge recipe is broken.

**Acceptance:** idle with 4 ingots must not flicker between active and
inactive load when a motor is genuinely underpowered. Underpowered
networks stay in a stable blocked state; once source supply becomes
adequate, processing should resume and yield exactly 4 Copper Plates
after 160 supplied ticks. Verify after reload and from the actual
controller output, then review whether the 20 RPM/5 torque values need
balancing; do not silently lower them to mask source limits.
