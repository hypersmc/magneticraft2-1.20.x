# First playable Steel — single-player acceptance gate

**Branch:** `feature/steel-age-foundations`, stacked on open Iron PR #18.
**Stage:** Source implementation/prototype. Compilation and Minecraft gameplay
unverified. No merge authorized or performed.

## Rules

- This is a complete *vertical loop*, NOT Steel Age content completion.
- Coke Oven is genuinely separate from Wood Retort; Coke is not charcoal
  and does not generate Wood Tar.
- Coke/Blast/Refining use existing `MultiblockController` formation,
  physical item modules, HEAT and (Blast/Refining) AIR capabilities.
- Shared 5-slot controller layout: Input0=0, Product=1, Slag=2,
  Input1=3, Input2=4. A physical input port displays 1/3 input slots
  depending on machine recipe; output ports cannot insert.
- The Blast/Refining air intake is the **LEFT side wall**, 1 up,
  2 behind, actual formed filler on a 5×5×5 structure. The air duct
  connects to the outward face, just like the existing Bloomery.
- No oil, steam, pressure vessel, pressure-rated fitting, coal-tar
  liquid, Draw Bench or extra generator is added by this slice.
- Screenshots/videos must determine visual acceptance; static code
  cannot prove correct rendered geometry.

## Test the process

1. Run JDK17 `./gradlew build` from the complete branch checkout.
   Run `node tools/steel-age/check-first-steel-contract.mjs`.
   Preserve actual output; do not substitute static review.
2. Form each of the 3 multiblocks in SOUTH, NORTH, EAST and WEST from
   the Steel Age Patchouli pages. All 12 rotations must form with real
   Input/Output/Slag ports. Confirm physical shape/selection and visible
   supported bricks from ALL camera angles; no stretched UV/floating geometry.
3. Verify Coke Oven 4 Coal + normal separate fuel -> exactly 4 Coke
   over 720 supplied ticks. Interrupt before completion; no Coke awarded.
   Full output must block both new fuel ignition and progression.
4. Form Blast Furnace with no Steel parts; feed 4 Magnetite Concentrate,
   2 Coke and 1 Calcite through the physical input port's three slots.
   Connect existing Blower -> Bronze AIR Duct -> side tuyere. Supply
   extra Coke as combustion fuel. Confirm 2 Pig Iron + 2 Slag after
   1200 supplied ticks. Remove AIR, fuel, flux or Coke during progress;
   machine must stall/recover, never award free metal.
5. Form Refining Furnace; feed 2 Pig Iron. Supply Coke furnace fuel and
   AIR via tuyere. Confirm exactly 2 Refined Steel Ingots + 1 Slag
   after 1000 supplied ticks. A full slag output MUST block refining.
6. Craft Steel Forge Die from 4 Steel and an existing Iron Forge Die.
   Install in the SAME Bronze Forge Hammer. Verify 1536 life vs Iron
   768 vs Bronze 384; older saved installed Iron/Bronze die retains
   its material and exact remaining life. Test die replacement.
7. Material accounting: one Coke batch outputs only 4 Coke; Blast
   consumes 2 per operation plus additional firebox Coke; net Pig Iron
   cannot exceed recipe output. Refining retains 2 metal pieces of
   approximate ingot-equivalent mass and creates byproduct Slag.
   Validate source-quality tag transfer and heat-tag removal.
8. Save/reload at half progress, with active burning and partially full
   real input/output inventories. Break/unform/reform all three, verify
   no ghost products, duplicated fuel/items or stale AIR capabilities.
9. Test Wood Tar Retort, Iron Hearth, Bronze Bloomery, old Forge Hammer
   die servicing, JEI/Patchouli construction, and vanilla Coal usage
   to catch regressions.
10. **Player acceptance:** produce and install one Steel Forge Die
   in a normal test world before deciding whether to merge Iron PR #18.

## Model boundary

The current shared baked-palette geometry is temporary and constrained
to vanilla 16×16 UVs, not the finished MGC2 Studio design. File cosmetic
model and animation issues against Studio. Do not accept serious
collision holes, invisible port locations, save bugs, severe FPS
regressions or incorrect fuel/AIR handling as mere Studio work.

Dedicated multiplayer-specific implementation and tests remain deferred
until AFTER Steam Age per project decision.
