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

### Steel sample-matching visual pass (2026-11-08)

- The player-provided in-game screenshot still showed the three furnaces as flat masonry despite the initial silhouette pass. The separate generated design samples are the visual references, NOT screenshots of the implemented game models.
- Coke now has recessed working doors, a heavier inspection grille, plated front corners, and a dark-lined top vent.
- Blast now has a supported front hearth in its existing front-center AIR cell, stronger jambs/lintel, a plated shoulder ledge, an inset upper-shaft intake, and a soot-lined top flue.
- Refining now has a THREE-block-wide recessed hearth (two existing brick-wing cells plus the front-center AIR cell), separate overhead vent panels, an iron roof cornice, and a lined exhaust hood.
- Structural footprint, port locations, input/output roles, heat/fuel/AIR processing, and recipe JSONs were not changed. The previously empty front-center layer-2 cell in Blast/Refining becomes a formed-state V2 proxy at controller-relative [0,1,0]; prior to formation it MUST still be built as air.
- Voxel collision/selection for the formed hearth aligns with the new recessed geometry. The saved V2 part IDs have changed, so **unform and rebuild existing test structures**; do not use old filler states as evidence of this pass.
- Uses only previously existing Magneticraft2 16px textures, native-scale UVs, and once-baked model parts. No continuous renderer, entity or tick allocation was introduced.
- **Verification gate:** source/resource structural checks and native UV bounds; run JDK 17 Gradle build, test all 12 orientations, examine the furnace fronts and sides from near and far, check AIR/ports, controller interaction and performance in-game. Cosmetic differences versus the samples remain subject to player acceptance.

### Steel item quantity synchronization review — 2026-11-08

Static inspection found the controller's change detector compared item identity/tags but not count; the saved display input snapshots also reduced counts to one. Port input/output quantity changes with the same item ID could therefore be missed by client sync. The detector now uses `ItemStack.matches` and saves full input stack snapshots. Recipe identity matching deliberately still ignores stack counts so topping up ingredients does not erase ongoing progress. The contract fixture asserts these safeguards.

**Player regression:** with all three machines formed, add and remove part of an existing input stack and extract only part of output/slag; verify the actual displayed stack updates without F3+T and production progress is unaffected by topping up. Forge compilation and in-game behavior are not yet verified.

### Steel AIR inlet isolation review — 2026-11-08

The actual Steel side tuyere previously exposed its internal legacy `IPressureStorage`. The shared legacy storage can permit extraction even when its `canSend()` reports false, due to its internal extraction implementation. The formed Steel tuyere now returns a receive-only pressure view and a typed GAS/AIR view wrapping that same guarded inlet. Internal recipe AIR consumption continues to read the internal buffer without any new power source. Both public capabilities are invalidated/revived on BE lifecycle transitions. This change is restricted to Steel; it does not modify global pressure storage behavior or existing Bronze machines.

**Player regression:** verify powered Bronze Industrial Air Blower → AIR duct → left-side Steel furnace tuyere supplies air; callers cannot extract AIR through either input capability; dismantling/reloading makes an inlet unavailable rather than a free source. Repeat after unloading/reloading chunks. Typed STEAM must not connect. Multiplayer-specific testing remains deferred.
