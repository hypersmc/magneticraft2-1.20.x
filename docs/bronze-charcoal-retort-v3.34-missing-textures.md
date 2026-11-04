# Charcoal Retort V3.34 — Fix unresolved masonry textures

**Forge 1.20.1** · **Draft PR #11** · **Branch:** `feature/bronze-age-foundry`

## Bug exposed by Minecraft screenshots
The V3.33 left and rear walls generated actual `#sideBrick` and `#mortar` face references, but `v3-models.mjs` attached those texture-map keys only when `c.z===0` or `c.x===1`. As a result, new upper left and rear faces used undefined aliases and appeared purple/black in Minecraft. Geometry and UV-validation checks did not catch the missing texture definitions.

## Minimal correction
Define `mortar: "minecraft:block/stone"` and `sideBrick: "minecraft:block/bricks"` for **every Y2 B cell**, independent of its local position. Keep structural brick backing exactly as before: the three front cells retain vanilla bricks and side/rear structural backing remains `magneticraft2:block/charcoal_retort/refractory`. Roof, doors, ports, actual masonry shapes, ventilation, collision and machine logic do not change.

## Validation
Exactly 20 affected generated upper Y2 model JSONs receive texture mappings across SOUTH, WEST, NORTH, EAST. Every Retort model's material alias was validated against its own `textures` dictionary; zero unresolved aliases remain. The generated element arrays and per-face material references are unchanged byte-for-byte, so geometry, UV coordinates and existing face partitioning do not change. The regression contract in `tools/charcoal-retort/test-contract.mjs` now explicitly checks all upper models for unresolved texture aliases, and checks masonry material mappings.

**Not run here:** a local Node CLI run, Gradle Forge build, or Minecraft. The user should reload the resource assets and inspect both newly raised side and rear masonry faces: they should now render with normal brick and mortar textures instead of the purple missing-texture pattern.
