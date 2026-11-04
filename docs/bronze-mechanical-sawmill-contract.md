# Mechanical Sawmill K1 — Bronze III (draft PR #11)

**Status:** Source candidate only. Forge build, dedicated server, in-game collision/UV/animation and regression checks are still required; do not merge.

## Gameplay purpose and balance

An industrial timber cutter for consistent Foundry wooden-pattern stock, distinct from the charcoal retort (fuel/tar) and Pattern Maker (manual finish). Existing vanilla plank crafting and manual pattern fabrication are **unchanged**.

- Input: 1 ordinary burnable log (minecraft:logs_that_burn); output 3 precision-cut pattern stock, recipe magneticraft2:bronze_sawmill_pattern_stock.
- Each stock consumed at the existing Pattern Maker manually yields two selected reusable wood Gear or Bearing Patterns. Existing single-plank route still gives one; thus 1 log gives 6 patterns via powered sawing vs 4 patterns via vanilla planks. No duplicate output from a single stock.
- A successful cycle needs 160 supplied ticks at >=16 raw Gear V2 RPM and >=4 torque. This is a machining *load*, never a source.
- Replaceable bronze saw blade starts installed and lasts 256 completed log cuts. Replaced by right-clicking with a matching item via ReplaceableMachinePart. Wear NBT survives reload.
- Recipe is datapack-editable. No chemistry, new ores, quality grading or log NBT proliferation is claimed. The source log's arbitrary NBT is **not** copied to multiple outputs.

## 5×3×3 structure

SOUTH view, rows rear -> front, layers bottom -> top, controller in bottom-front-center:

```text
SSISS  PAAAP  PPPPP
USSSO  PAAAP  PAAAP
SSCSS  PAAAP  PPPPP
```

S: Stone Bricks, P: Oak Planks, A: AIR, I: physical mechanical input, U: real item input, O: real primary item output, C: controller.
Rotate to other directions using the authored JSONs. The Patchouli preview reverses X so its bottom-center row reads OSSSU rather than USSSO.

## Model and power discipline

- Canonical geometry: tools/multiblock-models/designs/bronze_mechanical_sawmill.json; generated V2 source and four sets of per-host part models. A separate blade model is baked once; BER only spins it and renders one log item.
- Gearbox/brackets, bed rails, upright, guard and feed sled are static. Disc spins around local Z axis; stock feeds laterally across it. Real I/U/O module cells have **no** static render-host geometry, enforced via the shared generator.
- Disassembled/idle/stalled machines release mechanical demand. Item output insertion is simulated before ingredient extraction; progress, recipe identity, wear, module links and collision profiles are persisted.
- No invented block entity proxy or per-tick mesh regeneration.

## Verification required

1. Run node tools/multiblock-models/build-designs.mjs --check; node tools/multiblock-models/generate.mjs --check; ./gradlew build.
2. Form from Patchouli in SOUTH/WEST/NORTH/EAST, check every AIR row and the rear Gear V2 module, including chunk boundaries.
3. One burnable log -> exactly three stock after 160 supplied ticks; confirm three stock -> six patterns via the original Pattern Maker and four vanilla planks -> four patterns still work.
4. No consumption under low RPM, insufficient torque, unsupplied network, output blockage or worn blade. Ensure demand releases when stopped; no oscillating overload.
5. Reload partway through work, with logs/output and blade at low cycles remaining. Confirm no duplication, item loss or client-only processing.
6. Verify rear input axis and guarded blade rotation, support/collision, UV density and missing textures from all camera directions. Inspect FPS near multiple sawmills.
7. Dismantle/reform and break controller while holding stock and work; check drops once. Test item transfer and multiplayer with two clients separately.
8. Regression: Pattern Maker, retort, Lathe, Boring Machine, item ports and JEI category/catalyst.

## Editable 3D sample for later Studio modeling

A separate 3D starter with editable groups, saw-pivot and port guides is committed at `docs/reference/bronze-mechanical-sawmill-starter.bbmodel`; companion `.obj`, `.mtl`, and `.md` are in that directory. This is not the V2 source of truth or a game-ready replacement. Rebuild and verify the final Studio geometry against the real ports and JSON blueprints.
