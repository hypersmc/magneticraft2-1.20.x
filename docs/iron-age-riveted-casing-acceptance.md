# Riveted Iron Casing — Iron III-A

Source candidate for draft PR #18, designed as ordinary industrial construction.

## Contract
- 1 existing Riveted Iron Frame + 6 Wrought Iron Plates -> 1 Casing, shaped crafting.
- The frame requires four actual finished rivets: rod -> Metal Shear blanks ->
  Forge Hammer headed rivets. No circular recipes.
- Closed wrought-iron 1-block casing with exposed structural seams; the
  original dark-work-iron texture remains provisional pending MGC2 Studio.
- No block entity, ticking logic, machine, torque change or changed registry ID.
- Native UV dimensions on 22 static cuboids, with simple full-block collision.

## Acceptance: single-player
1. Execute `node tools/iron-age/check-riveted-casing-contract.mjs` and JDK 17 `./gradlew build`.
2. Produce the frame and plates in survival, craft exactly one casing, inspect JEI/creative/book.
3. Place, mine with the correct pickaxe, recover drops; inspect item model and all faces.
4. Check seams, adjacent solid blocks/casings, brightness, culling, collision and repeated-wall FPS.
5. Recheck the Frame shape/drop and Shear -> blanks -> Hammer -> finished rivets.

Compilation, in-game rendering, performance and game regressions are NOT
verified from this code-only upload. Keep PR #18 draft and unmerged.
