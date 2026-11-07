# Iron I-C — cold-finished Wrought Iron Sheet

**Branch:** `feature/iron-age-foundations` / draft PR #18.
**Status:** source implementation. No compiled or in-game result claimed.

## Functional contract

The existing Bronze Rolling Mill already cold-rolls Copper and handles
hot Wrought Iron Ingots and Bars. It now offers a harder *cold-finishing*
pass for a **previously forged Wrought Iron Plate**, without adding a
new machine or assuming heat metadata that forged Plates do not keep.

| Processing recipe | Input | Output | Load | Time |
| --- | --- | --- | --- | --- |
| `roll_wrought_iron_sheet` | 1 Wrought Iron Plate | 2 Wrought Iron Sheets | 12 torque, ≥16 RPM | 240 supplied ticks |
| existing `roll_wrought_iron_bar` | 1 HOT Wrought Iron Ingot | 1 Bar | 6 torque, ≥16 RPM | 180 |
| existing `roll_wrought_iron_rods` | 1 HOT Wrought Iron Bar | 4 Rods | 6 torque, ≥16 RPM | 200 |

One Plate has one ingot-equivalent metal; each new Sheet has
**one-half ingot-equivalent**. The new sheet route does not clone
a Plate, generate extra ingots or expose a backwards Sheet → Plate
conversion. It requires a real 12-torque driven Rolling Mill
(i.e. a correctly built stronger mechanical path), powered ticks and
normal roller-sleeve wear. The Mill still reserves output before
consumption, pauses if blocked and never produces a batch when stopped.

Material quality and HEAT are **separate** contracts. Sheet output
inherits input quality NBT via the existing `consolidate` helper
that strips only temporary heat. This is selected by the new
`preservesRollingQuality()` method for Iron Sheet and the existing
hot Bar/Rod recipes. `requiresRollingHeat()` retains its original
Hot Wrought Bar/Rod-only restrictions. Copper cold sheet output remains
untouched and does not receive new quality transfer behavior.

## Useful downstream crafting

- The manual `2 Wrought Iron Plates → 1 Iron Plate Panel` recipe is
  **preserved**. Alternative `2 Sheets → 1 Panel` uses half the iron
  (one finished Plate per Panel) but requires real powered sheet rolling.
- The original `2 Plates + Bar + Rod → 2 Iron Service Hatches` recipe
  is **preserved**. `2 Sheets + Bar + Rod → 2 Hatches` uses an equal
  number of thinner panel pieces for a framed hinged assembly, with
  lower metal consumption because those sheet panels are thinner.
- The completed crafting-table BlockItem has vanilla fixed quality
  (no automatic input ingredient NBT copying). Quality NBT remains
  meaningful on the sheet item itself for future quality-aware steps.

## In-game acceptance and regression checklist

1. Forge a Plate from the existing hot-ingot process and feed it to
   a valid formed Bronze Rolling Mill. Cold Plate must be accepted
   for sheet finishing; hot timer should **not** be required.
2. Supply a 12-torque-capable source and an actual Iron-rated network
   at ≥16 RPM. Compare with the exact same network using a wooden
   8-torque bottleneck: confirm refusal/overload and no output under
   insufficient source capacity, without periodic free-power reset.
3. After 240 supplied ticks, confirm precisely 2 Sheets for one Plate,
   real roller-sleeve wear and correct inventory count. No duplication
   when output is blocked, power is interrupted or chunk is reloaded.
4. Check quality metadata before/after rolling with a nondefault
   quality Plate: quality should survive into output (and be stack
   compatible only with matching quality). No forged hot tag on output.
5. Re-run old hot Ingot→Bar and hot Bar→Rod recipes, proving cold
   input rejection, proper 6-torque loads and unchanged outputs.
   Re-run Copper cold sheet with identical previous output.
6. Craft Panel and Hatch using BOTH old Plate and new Sheet variants;
   confirm amount/yield, JEI, item model and normal manual/redstone/
   waterlogging behavior of the existing placed hatch.
7. Inspect the new 3D sheet item texture from all sides and on ground.
   Check proportional UV (14×14 top, 14×1 edges), correct nearest
   sampling and visibility. MGC2 Studio may polish visuals later.
8. Restart/restore an existing world with active Copper/Hot Iron Mill
   batches and confirm the existing machine ID, saved recipe/progress,
   and roller durability are backward compatible.

## Technical checks

Run from a full JDK17 project checkout:
```sh
node tools/iron-age/check-iron-sheet-contract.mjs
./gradlew build
```

Static JSON/source fixture and GitHub source inspection do **not**
substitute for running those commands or Minecraft. Do not mark
this source slice complete until player testing demonstrates the
real driven mill throughput and quality/output handling.

**No gameplay changes made to:** Iron Flywheel, Forge Hearth, Service
Hatch placed behavior, Gear V2 manager, machine formation, Copper
recipes, Steel/Steam pipeline or multiplayer systems. Keep PR #18
draft and unmerged; multiplayer-specific work remains after Steam Age.

## Player report — 2026-11-08

The player said they had **"just tested the plate one"** before asking to
continue development. This records that a recent Plate-related in-game
check occurred, but does **not** identify which acceptance cases passed or
independently confirm sheet output, exact 12-torque behavior, saved NBT or
old-recipe regressions. Keep those individual tests open until reported.
