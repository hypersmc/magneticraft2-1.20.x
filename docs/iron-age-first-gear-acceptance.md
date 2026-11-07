# Iron II-A — first Wrought Iron Medium Gear: implementation and acceptance

**Forge 1.20.1 / draft PR #18 / source candidate, not verified in-game.**
**Multiplayer implementation and tests deferred until after Steam Age.**

## Production chain

```text
Existing Bloomery -> Iron Bloom -> Refractory Kiln -> Forge Hammer
  -> Wrought Iron Ingot
  -> Refractory Kiln -> Rolling Mill -> Wrought Iron Bar
  -> Refractory Kiln (heated Bar) -> Forge Hammer (180 ticks, 20 RPM, 6 torque)
  -> Iron Gear Blank
  -> Bronze Mechanical Lathe + existing flint-edged Bronze cutter
    (160 ticks, 12 RPM, 5 torque, 2 cutter durability)
  -> Precision Iron Gear
  + 3 Wrought Iron Plates + 1 Wrought Iron Bar (crafting)
  -> Wrought Iron Medium Gear [8 teeth; 16 torque transmission limit]
```

The lathe qualifies mechanical load against raw network RPM (matching
the already-correct Rolling Mill pattern), then requires Gear V2's
authoritative supplied state. This prevents source-overload load
flickering without allowing work while overloaded.

The lathe handles one new defined JSON recipe, preserving hardcoded
Bronze gear/bearing workpiece compatibility. The Forge Hammer uses
the existing hot-state gate; material-quality NBT survives forge and
lathe. The final vanilla shaped crafting operation **does not carry
input quality NBT into the new BlockItem**; no quality-dependent gear
durability or transmission ratings were approved, so none were added.

## Architecture

- Iron Block extends the existing wooden medium gear placement, collision
  and network rules with iron harvesting strength/sound.
- Iron BE subclasses the proven wooden medium Gear V2 entity; it is
  separately registered and overrides only the max torque from 8 to 16.
- Client BER delegates to the existing wood medium gear renderer; its
  iron model uses existing `dark_work_iron` and `bronze_machined`
  textures, original UV pixel density and symmetric 8-tooth teeth.
- A gear's 16-torque ceiling **does not create 16 torque**. Other
  weak gears, gearboxes, power sources and machine loads are unmodified.
- The Iron gear is static world geometry when idle and rotates
  through the existing BE renderer when driven. No new entities or
  per-frame model construction.
- No other Steel/Steam tech, multiblock redesign or global network
  torque adjustment in this implementation slice.

## Verification checklist (single-player)

- [ ] JDK 17 `./gradlew build` with actual output.
- [ ] `node tools/iron-age/check-iron-medium-gear-contract.mjs`
  and earlier Iron 0C/0D source fixtures with actual recorded output.
- [ ] Fuel/heat an Iron Bar. Confirm cold Bar rejected by Forge Hammer,
  hot Bar -> **one** Iron Gear Blank; verify one die wear batch.
- [ ] Insert blank into old formed Bronze Lathe with its flint-edged
  cutter; verify exactly one Machined Iron Gear after a powered batch.
- [ ] Cutter life decreases by **2** for Iron, **1** for Bronze; verify
  exhausted cutter, stopped power, broken/unformed Lathe and save/reload.
- [ ] Craft gear from precision Iron Gear, three Wrought Plates and one
  Wrought Bar; confirm item recipe, drops and harvesting.
- [ ] Place in X/Y/Z rotations, rotate under Gear V2 power and verify
  all existing mixed-size mesh clearance rules and phase.
- [ ] Compare wooden medium max torque **8** vs Iron medium **16**.
  Test load/overload with source capacity and *all* connected nodes.
  A genuine >8 load requires a later heavy consumer; don't claim this
  acceptance based only on a simulated node rating.
- [ ] Place multiple Iron/Wood gears and check for visible teeth overlap,
  floating rims, misplaced axle/bearing, rendering glitches, wrong UVs,
  FPS drops, collision mismatch and correct animation switching.
- [ ] Preserve old Copper/Bronze gear and Lathe gear/bearing processing,
  Bronze-to-Iron ore chain, machine progress, maintenance, saved worlds.
- [ ] JEI/Patchouli, creative tab, item models, localization and all
  model orientations in single-player.

**No feature is player accepted just from compiling or static source
inspection.** Keep PR #18 draft until specifically instructed otherwise.
