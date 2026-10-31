# Ore Roasting Kiln — first reference visual rebuild (2026-10-31)

**Source of truth:** the FIRST Ore Roasting Kiln concept selected by the user; a long masonry furnace with three large arched roasting vents per side, substantial iron structural ribs, timber headers, a pitched segmented stone roof, a clear front charging door/firebox, and a rear brick chimney. **Only the illustration's unrealistic large gear and external shaft are intentionally omitted.**

**Breaking prototype dimension change explicitly approved:** 5×3×9 → **5×6×9**. The former 3-high model was a flat brick box and too far from the reference. This rebuild puts the foundation at y=0, brick walls at y=1–3, the pitched roof at y=4, and a real 3×3 hollow brick chimney at y=5, seven blocks behind the front controller. The chimney has a continuous AIR path through y=4 and y=5.

## Materials and geometry

Use **vanilla textures** whenever their visual identity matches the artwork: minecraft:block/bricks on the main walls, minecraft:block/stone_bricks for the base and roof, minecraft:block/deepslate_tiles for the roof bands/caps, and minecraft:block/spruce_planks for actual under-eave timber supports. Reuse existing generic magneticraft2 refractory material. Keep purpose-made 16×16 tiles only for iron frame, recessed soot, glowing ore/embers, wrought-iron grilles, and the functional item modules. The previous bespoke brick/foundation/roof PNG assets remain for source history but are no longer used by the formed model.

Three multi-block-high arch bays on each side, with recessed ember/soot interiors, lower masonry sills and stepped brick shoulders; continuous roof slopes produced as four 4px sections per roof block. The Voxel V2 roof collision follows the same subdivisions but merges contiguous Z spans into 24 long boxes (instead of 176 per-cell roof boxes); it stays below the maximum 512 boxes. No fake gearbox, entity renderer, floating chimney, large gear, or decorative drive shaft.

## Construction, existing saves, gameplay

The JSON multiblocks and Patchouli Visualize are generated from one canonical SOUTH layout and four orientations. **Old formed 5×3×9 visual-prototype machines must be dismantled before upgrading.** Their saved Voxel V2 and filler states are incompatible with the intentionally changed dimensions. Rebuild the new 5×6×9 structure. Existing Refractory Kiln gameplay and recipes are unchanged.

This is **visual/structural only**. Selective ore roasting, charcoal/HEAT, processing duration, ore impurity outputs, pollution and smoke remain undecided and unimplemented. The real item modules stay input-locked until valid recipes exist.

## Verification

```bash
node tools/ore-roasting-kiln/build-textures.mjs --check
node tools/ore-roasting-kiln/generate.mjs --check
node tools/ore-roasting-kiln/test-contract.mjs
./gradlew build
```

The generator assets were checked for four rotated blueprints, six Patchouli layers, block-local geometry, texture alias validity and UV width correspondence. **Gradle, in-game formation, multi-angle visual inspection, smoke flue, save/reload and multiplayer are unverified.** Visual acceptance requires an actual screenshot/video; do not declare reference fidelity from static checks.
