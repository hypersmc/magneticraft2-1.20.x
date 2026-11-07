# Iron 0C/0D — Wrought Iron Bar, Rod and first factory blocks

**Status (2026-11-07):** source implementation in draft PR #18;
build and in-game acceptance are NOT confirmed. Player verified only the
preceding Forge Die (Iron 0A) and the first Wrought Iron Plate (Iron 0B).

## Manufacturing choices

- **Forge Hammer:** 1 hot Wrought Iron Ingot -> 1 Wrought Iron Plate.
- **Rolling Mill:** 1 hot Wrought Iron Ingot -> 1 Wrought Iron Bar
  (16 RPM, 6 torque, 180 supplied ticks).
- **Refractory Kiln:** Reheat 1 Bar in 240 HEAT-supplied ticks, with
  real fuel and a 2400-world-tick server-owned heat timer.
- **Rolling Mill:** 1 hot Bar -> 4 slender Wrought Iron Rods
  (16 RPM, 6 torque, 200 supplied ticks).

The two Iron Mill recipes have different input IDs, and the Forge Hammer
separately produces Plates from Ingots. This **avoids input ambiguity**
in `MultiblockRecipeHandler` without adding recipe chooser UI or a
second machine.

**Metal conservation:** 1 ingot-equivalent = 1 Bar = 4 Rods (each
1/4-equivalent). Item models have proportional visual cross-sectional
volume for Bar vs Rod; Minecraft model volume is illustrative rather
than a literal material mass measurement. Quality/source NBT is copied
on real machine outputs while forging heat is stripped, exactly as the
existing Bloom/Plate approach.

**Real heat:** The Rolling Mill validates its Iron recipe's input
temperature on every server processing tick; output backpressure and
diagnostics use the actual quality-tagged ItemStack. If the source
cools it must return to the Kiln. The existing cold Copper Sheet
recipe stays heat-exempt.

## First three factory blocks

| ID | Obtaining / purpose | Appearance/placement |
| --- | --- | --- |
| `iron_structural_beam` | 4 Wrought Bars + 1 Rod -> 1 Beam; posts and rafters | I-beam, 3 axis variants (X/Y/Z), matching collision geometry |
| `iron_plate_panel` | 2 Wrought Iron Plates -> 1 Panel; industrial wall sections | Thin wall panel with 4 small fasteners; 4 horizontal facing states, thin collision |
| `iron_machine_foundation` | 2 Plates + 1 Structural Beam + 6 Stone Bricks -> 1 Foundation | Full-block stone plinth with dark worked-iron top, no BE/tick |

Blocks are construction materials, not simulated load-bearing physics.
Future Iron Stock Shear/Hearth structures may reuse these exact blocks,
but no multiblock layout/consumer has been changed here.
Simple Minecraft crafting does **not** carry item quality NBT into the
placed block; only metal processing item outputs currently propagate it.
Do not silently invent material-grade block variants.

## Acceptance checklist (single-player only)

- [ ] JDK 17 `./gradlew build`; record output
- [ ] `node tools/iron-age/check-bar-rod-factory-contract.mjs` and existing
      Bronze-to-Iron/Forge Die/Plate static contracts; record output
- [ ] Heat real Ingot and roll into exactly 1 Bar; new Bar is **cold**
- [ ] Reheat Bar, observe hot/cold tooltip and roll exactly 4 Rods
- [ ] Verify copper-sheet rolling still works at room temperature
- [ ] Reject cold/expired Iron stock, release load and reset progress
- [ ] Stop/restart wheel power; test output blocked, worn roller sleeves,
      multi-stacks with different quality, full 4-Rod output slot
- [ ] Save/reload hot bars and a mid-roll workpiece; timer must not restart
- [ ] Place beams vertically, X and Z; check model/real collision
- [ ] Place panels from four sides, rotate/mirror and inspect backing,
      fasteners, collision, occlusion and neighboring blocks
- [ ] Place/break Foundation; verify loot, harvesting, recipes, JEI and
      Patchouli for all three blocks and material items
- [ ] Inspect all items/blocks from multiple camera positions for native
      UV density, disconnected surfaces or floating geometry
- [ ] Run old Iron Bloom consolidation and Forge Die regressions

**Multiplayer:** all multiplayer-related work and testing is deferred
until *after Steam Age* as previously agreed. Keep it out of this gate.
