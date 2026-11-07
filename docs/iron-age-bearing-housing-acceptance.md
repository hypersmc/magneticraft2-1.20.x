# Machined Iron Bearing Housing — core Iron manufacturing component

**Scope:** additive, user-approved Iron progression slice on draft PR #18.
**Stage:** source and resource implementation; full JDK17 build and in-game
verification NOT yet performed.

## Mechanical purpose

The existing forged **Medium Iron Gear Blank** is one ingot-equivalent of
Wrought Iron after Forge Hammer processing of a **hot Wrought Iron Bar**.
Now that same useful rough forging has TWO precision manufacturing paths:

- **Existing Mechanical Lathe -> Precision Medium Iron Gear** (unchanged).
- **Existing Bronze Precision Boring Machine -> Machined Iron Bearing
  Housing** (new JSON recipe): 20 RPM, 8 torque, 240 supplied ticks,
  one bit service cycle, 1 blank -> 1 housing.

A machined Iron Housing is a *structural journal seat*, NOT a bare-iron
sliding bearing surface. Bronze Bushings made by the existing Boring Machine
act as the wear-compatible lining when assembling the machine. The housing
item's baked 3D model includes a through-pocket and two attached mounting
feet, native 1-model-pixel-per-texel UVs, and existing dark-work-iron
texture. No detached collars, floating shafts or stretched textures.

## Two immediate consumers (no rebalancing of existing machines)

1. **Wrought Iron Right-Angle Gearbox alternative**:
   3 forged Plates + 2 precision Medium Iron Gears + 2 Wrought Iron
   Shafts + 1 machined Housing + 1 Bronze Bushing.
   The housing's forged blank is one Bar/ingot-equivalent, so the
   **four-iron-unit housing/panel cost** stays equivalent to the
   original 4-Plate + Precision Bronze Bearing gearbox, but the
   bronze machining path differs. Recipe ID:
   `wrought_iron_gearbox_bearing_housing`.
2. **Wrought Iron Flywheel alternative**:
   4 Plates + 1 Housing + 1 Bronze Bushing + 1 Riveted Iron Frame +
   1 Large Precision Iron Gear + 1 Iron Shaft.
   The original recipe instead uses 4 Plates + **2 Bars** with the same
   frame, gear and shaft. The housing requires one **forged Bar** and
   a precision-boring operation; the alternative saves one Iron Bar
   in exchange for genuine workshop processing and a Bronze Bushing.
   Recipe ID: `iron_flywheel_bearing_housing`.

Both original crafting recipes are untouched. The resulting registered
gearbox/flywheel Blocks, BE IDs, models, power, torque, efficiency,
inertia, placement and save data are **identical regardless of recipe**.
This is a manufacturing alternative and structural prerequisite
demonstration, NOT an unearned power upgrade. Do not retroactively
require this part in previously assembled machines.

## NBT / compatibility

The existing Forge Hammer consolidates the hot-bar forging output,
preserving material-quality tags and removing temporary hot timestamps.
The existing Boring Machine's `taggedOutput` copies the resulting
blank's NBT to the Housing before checking/adding the output.
Ordinary vanilla shaped crafting does NOT transfer the Housing's
quality NBT onto the final BlockItem. No new per-block
quality/strength/torque mechanic is introduced.

The new item registry ID is `magneticraft2:iron_bearing_housing`.
All previously saved worlds, workpieces and old recipes continue to
use their original identifiers. There are no new world blocks,
multiblock controllers, machine states or network update rules.

## In-game single-player acceptance

1. Produce a **Medium Iron Gear Blank** from a genuinely hot Bar
   using the existing Forge Hammer. Cold Bar must still be rejected;
   source material quality must persist into the forged blank.
2. Feed ONE blank through the Boring Machine's real input. Supply
   >=20 RPM / >=8 torque, install a usable Abrasive Boring Bit and
   confirm exactly 240 supplied ticks produce ONE Iron Bearing
   Housing with one bit wear; quality NBT survives the operation.
3. Stop power, overload the network, fill output, remove/wear the
   bit and save/reload mid-batch. Confirm no free output, lost
   progress, duplicated material or incorrect wear.
4. Inspect the Housing in GUI/hand/ground and as a dropped item.
   Its full 4x4-pixel through-pocket and two mounting feet should
   be visible and touching; no floating or distorted texture.
5. Craft one Gearbox and one Flywheel using their new housing
   alternatives and verify they behave **exactly** like existing
   examples, with correct real mechanical connections and visuals.
6. Craft both machines again with their **original** recipes.
   Confirm those outputs and the Boring Machine's Bronze Bushing
   and Iron Toolholder recipes still work unchanged.
7. Check JEI (new boring recipe plus both vanilla crafting
   alternatives), creative tab, Iron Age Patchouli, language,
   old-world startup, item/block drop and save/reload.
8. Test the separate Lathe path from the same Iron Gear Blank to
   ensure recipe routing still selects the correct machine.

## Draw Bench design gate — NOT implemented

The 2026-11-07 Industrial Factory plan requires at least TWO real
consumers and a distinct industrial advantage before implementing a
Mechanical Draw Bench. Existing Wrought Iron Rods already work for
Rivets, machine components and the Iron Toolholder. None currently
specifically requires dimensionally-calibrated drawn rod/pin/wire.
Do not generate new precision-stock items or redundant machines
solely to meet a 16-block design target. Revisit when Steel or
later-era construction provides authentic requirements.

## Technical checks

```sh
node tools/iron-age/check-iron-bearing-housing-contract.mjs
./gradlew build
```

GitHub source/resource inspection and local structural checks do
not prove full compilation, JEI recipe loading, Minecraft rendering,
machine formation, multiplayer behavior or runtime saved-data safety.
PR #18 remains draft/unmerged; multiplayer-specific work stays
deferred until AFTER Steam Age.
