# Iron III-A — Stock Shear, Wrought Rivets and Riveted Frame

**Status 2026-11-07:** Source-level implementation candidate in draft
PR #18. NO Gradle build, Node fixture run or in-game acceptance has been
verified in this session. Multiplayer testing and multiplayer-specific
work are deferred until AFTER Steam Age per user instruction.

## Intent and balance

**First real >8-torque Iron consumer.** The existing Large Water Wheel
can supply up to 16 torque depending on water, but a Wooden Medium
Gear or Wooden Gearbox is still rated only 8. The Stock Shear needs
**12 torque and 16 RPM** for 180 *supplied* processing ticks.
A continuous Iron transmission path (Iron Medium Gear 16, Iron
Gearbox 16, Iron Shaft 24, existing Mechanical Input Module 16)
can potentially satisfy that load. The complete network needs
actual in-game proof; the source rating alone is not proof of work.

## Manufacture and metal ledger

- Shear Controller = 4 Wrought Iron Plates + 4 Iron Structural Beams
  + 1 Precision Iron Gear (crafting table)
- Replaceable Iron Shear Blade = 2 Wrought Iron Plates + 1 Wrought
  Iron Rod; 512 completed batches service life
- Machine: **1 Wrought Iron Rod → 4 Iron Rivets**, 180 ticks
- **Riveted Iron Frame** = 4 Wrought Iron Bars + 4 Iron Rivets
  (all obtained from existing earlier processes)

The first Shear has no self-dependency on Rivets or its own output,
and no imaginary duplicate Lathe/Rolling Mill/Mechanical Press.
Rivet quality metadata follows the input Rod (temporary heat
metadata is stripped); crafting the final frame does NOT yet
preserve input metal quality in the placed block.

## Actual JSON construction

3 wide × 3 deep × 2 tall. SOUTH orientation from JSON:

```text
Upper layer, y=1 (controller-facing top view):
B B B       B = Iron Structural Beam
B A B       A = required AIR (middle); render proxy on formation
B B B

Ground layer, y=0:
F M F       F = Iron Machine Foundation
U F O       M = Mechanical Input Module, U = Item Input
F C F       O = Primary Item Output, C = Shear controller
```

Five Foundations + eight Structural Beams + four functional blocks
(controller, M, U, O) + one AIR = 18 cells.
Four orientation JSONs, 18 actual baked models per direction,
one Voxel V2 collision/selection profile and a separately baked
moving shear-blade model. The default Patchouli Visualize view
mirrors U/O relative to SOUTH JSON as established on the Bronze Press.
Use actual modules as input/output, not fake hopper decorations.

The 18 SOUTH part models are checked-in canonical visual sources for
this first provisional machine. Generate the 54 other oriented model
parts using `node tools/iron-age/build-stock-shear-parts.mjs --write`.
Check reproducibility using `--check`. Further Studio modeling
can migrate the machine into the centralized editable designs pipeline
before final art acceptance; this step does NOT create a new rendering
architecture.

## Regression and in-game checklist (single player)

- [ ] JDK 17 `./gradlew build` (actual output recorded)
- [ ] `node tools/iron-age/build-stock-shear-parts.mjs --check`
- [ ] `node tools/iron-age/check-stock-shear-contract.mjs`
- [ ] Construct SOUTH and form with controller; confirm all required
      modules, all 8 top beams and precisely one center AIR cell
- [ ] Construct NORTH, EAST, WEST too; inspect previews, hitboxes,
      model orientation, controller, I/O modules, shaft alignment
- [ ] Insert only Wrought Iron Rod into actual input port; extract
      exactly **four** Rivets from real primary output module
- [ ] Verify 16 RPM / 12 torque, 180 supplied ticks and accurate
      stopped/stalled/working diagnostic messages
- [ ] Reproduce a failure via Wooden Medium Gear and Wooden Gearbox
      (8 torque), then replace constrained parts with 16-capacity
      Iron gears/gearbox; water flow and all other paths must permit load
- [ ] Stop power mid-batch: no free progress/extra Rivets; resume.
      Block output: no input loss, load or blade wear
- [ ] Force 512 completed operations (or inspect carefully controlled
      test state); blade worn stops new batches; blade replacement
      consumes one Blade and resets service life
- [ ] Save/reload during a powered batch and while output blocked;
      preserve active stock, quality metadata, blade life, progress
- [ ] Break and unform machine, recover contents as existing
      multiblock contract dictates; no duping or stale Gear V2 load
- [ ] Inspect moving blade, contact with stock, structural supports,
      item positions, texture/UV density, first- and third-person
      appearances, multiple camera angles and FPS
- [ ] Craft Riveted Iron Frame using real Rivets; verify 12-beam
      visible voxel outline/collision, harvesting/loot, creative
      tab, JEI and Patchouli
- [ ] Regression: old Bronze Mechanical Press, existing Iron Bar/Rod
      and Gearbox, water wheel, input/output item ports and saved worlds

**Current known limitations:** Visuals are provisional, not MGC2 Studio
final quality; machine uses 18 authored SOUTH pieces and a dedicated
rotated-model builder rather than new Studio-authored geometry.
Quality-derived frame/block strength or wear is not invented.
No Steel-age machinery or multiplayer testing has been started.
