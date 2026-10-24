# Charcoal Retort V3.31 — Restore exposed brickwork above stepped copper crown

**Forge 1.20.1** · Draft PR #11 · `feature/bronze-age-foundry` · **Minecraft visual review pending**

## Issue in V3.30
The user compared the result to the original blast-furnace-style reference and pointed out that the thick raised copper arch and copper center pillar were **covering the refractory brick texturing** behind the hood. The reference has a stepped copper **outline/crown** around recessed dark iron, leaving the brick field visibly separate above.

## V3.31 geometry
The existing iron drip strip (Y0.1..0.62) and copper bottom sill (Y0.62..1.02) stay. The seven copper arch segments become a thin symmetrical staircase:
- X2.8..4 Y1.05..1.65; X4..5.2 Y1.65..2.25; X5.2..6.4 Y2.25..2.95;
- X6.4..9.6 Y2.95..3.85; mirrored segments on the right.
The tall solid copper pier X6.7..9.3 Y1.02..3.25 is **removed**. Recessed iron backing follows the staircase and remains at Z15.78 behind the copper facade Z16. The highest visible copper at Y3.85 stops before the raised masonry starts at Y4.05.

## Strict one-block scope
Only `tools/charcoal-retort/v3-models.mjs`'s front-center Y2 branch is modified and **four rotated copies of `part_0_2_0.json`** are regenerated. The multiblock layout, Y1 furnace hatch, center upper grille, roof, left/right blocks, side vents, textures, ports, recipes and functional logic are untouched.

In-memory generation/face partitioning passed. Static checks verify both recessed dark-iron areas, seven copper crown samples, restored brick/mortar at Y4.0..4.5, unchanged upper front appearance from Y4.6 upwards, cell bounds and proportional UVs. Updated regression tests intentionally replace V3.30 assumptions that required the obstructing copper center pillar.

**Still pending:** actual Node CLI tests/build check, full `./gradlew build` and in-game screenshots. Compare a straight front shot with the original reference, checking exposed raised brick courses, copper stair-step crown and iron shadow beneath.
