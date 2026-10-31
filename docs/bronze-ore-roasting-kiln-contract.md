# Ore Roasting Kiln — first reference visual rebuild (2026-10-31)

**Source of truth:** the FIRST Ore Roasting Kiln concept selected by the user; a long masonry furnace with three large arched roasting vents per side, substantial iron structural ribs, timber headers, a pitched segmented stone roof, a clear front charging door/firebox, and a rear brick chimney. **Only the illustration's unrealistic large gear and external shaft are intentionally omitted.**

**Breaking prototype dimension change explicitly approved:** 5×3×9 → **5×6×9**. The former 3-high model was a flat brick box and too far from the reference. This rebuild puts the foundation at y=0, brick walls at y=1–3, the pitched roof at y=4, and a real 3×3 hollow brick chimney at y=5, seven blocks behind the front controller. The chimney has a continuous AIR path through y=4 and y=5.

## Materials and geometry

Use **vanilla textures** whenever their visual identity matches the artwork: minecraft:block/bricks on the main walls, minecraft:block/stone_bricks for the base and roof, minecraft:block/deepslate_tiles for the roof bands/caps, and minecraft:block/spruce_planks for actual under-eave timber supports. Reuse existing generic magneticraft2 refractory material. Keep purpose-made 16×16 tiles only for iron frame, recessed soot, glowing ore/embers, wrought-iron grilles, and the functional item modules. The previous bespoke brick/foundation/roof PNG assets remain for source history but are no longer used by the formed model.

Three multi-block-high arch bays on each side, with recessed ember/soot interiors, lower masonry sills and stepped brick shoulders; continuous roof slopes produced as four 4px sections per roof block. The Voxel V2 roof collision follows the same subdivisions but merges continuous pitched-roof Z spans and adds eight full-height masonry feet directly under the raised brick chimney ring, eliminating the original unsupported gap; it stays below the maximum 512 boxes. No fake gearbox, entity renderer, floating chimney, large gear, or decorative drive shaft.

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

## 2026-10-31 — screenshot-directed aperture and roof seam correction

The user's two in-game screenshots showed **visible sky/terrain in the side arches**, three pairs of separate slit-shaped windows rather than three unified arches, **floating/detached roof ribs**, and undersized, weakly integrated front item-port faces. These were caused by authored block-model geometry, not texture resolution or a need for arbitrary positional offsets.

The two-cell arches previously placed masonry jambs against their *shared seam*, producing a false centre pillar and uncovered outer edges. They now use one full-cell opaque recessed soot backplate per segment, with masonry only on the *actual external edges* of each two-cell arch. The taller roasting vents and bars remain static.

The segmented roof previously emitted **top-only faces** for its pitched courses, missing vertical riser/edge faces, and used a separate `roofTop()` height for ribs/caps. The roof now has all exposed vertical riser and gable faces and derives the position of every structural rib and ridge cap from the exact same `roofStep()` height as its own stone-tile surface and Voxel V2 profile. The silhouette, voxel profile and all four real blueprint structures are unchanged. Genuine input/output port blocks gain larger, integral iron mounting plates; their actual capabilities and coordinates do not change.

**Status:** Source/static regression checks only. Minecraft screenshots from front, both arch sides, and roof at several angles are required before claiming any visual fix.

## One-bay oblique-angle geometry pilot (2026-10-31)

The user's latest grazing-angle screenshot proves the side openings still
behave like near-planar decals. A specific modeling error was also found:
`modelBox` sent local east/west faces to the wrong world-facing normals
for the *west* and *east* orientation rotations. Source normals now follow
the exact coordinate transforms for all four orientations.

**Scope intentionally limited to one bay, not six:** right-hand wall
(`x=+2` in canonical SOUTH coordinates), *nearest the controller/front*,
`z=-2,-1`, for all three wall heights `y=1,2,3`. This is one
two-block-wide, three-block-high aperture, generated across all orientations.
The other five existing roasting arches are unchanged visually pending
side-by-side in-game review.

The pilot is a real closed masonry/refractory assembly: backing brick volume
`x=0..9`, opaque soot lined chamber `x=9..10`, heated lining `x=10..10.5`,
three-pixel-deep forged iron grilles `x=11.5..14.5`, and load-bearing
outer jambs/sill/crown extending to `x=16`. Cuboids have all six face
textures so they reveal solid side, top and inner return faces at grazing
angles. Two halves do not introduce a center pillar. This uses the same
vanilla brick texture and existing kiln material textures and does not alter
blueprints, collision, dimensions, gameplay or add dynamic rendering.

Run `node tools/ore-roasting-kiln/generate.mjs --check` and
`node tools/ore-roasting-kiln/test-contract.mjs`, then Gradle and
an **in-game A/B comparison at a very shallow side angle**. Check all four
facing rotations, view the pilot bay from both ends and below the arches,
and compare it with the unchanged bays. These are static source changes:
**visual acceptance remains unverified until Minecraft screenshots**.

## 2026-10-31 — reference correction from in-game screenshots

The user rejected the squat 6-high chimney and noisy custom textures shown in
actual Minecraft screenshots. The deliberately breaking **5×8×9** blueprint
places a *real* three-block-tall 3×3 brick chimney at heights y=5,6,7 with
a single continuous center AIR shaft. The upper ring is finished with vanilla
Deepslate Tiles coping; the chimney does not float or render invisible extra
blocks. The Stone Bricks + Brick structure otherwise retains the same width
and length. Six-layer formed machines must be dismantled before upgrading and
rebuilt with the eight-layer Patchouli Visualize pattern. This is a structural
migration, not save-data auto-migration.

All six roasting arches now use the same enclosed 3D volume geometry instead
of one pilot and five planar copies. Interior soot is vanilla Black Concrete,
low glowing ore-bed texture is vanilla Magma Block, and true 3px-thick
metal grilles use vanilla Iron Block. Arch shoulders and sill use vanilla
Bricks/Smooth Stone; frames use vanilla Polished Deepslate, and roof/wood
retain existing vanilla materials. Only real I/O panel markings still use the
machine's custom PNGs. The previous machine-specific PNGs remain in source as
unused legacy tiles but are **not referenced by formed models**.

A geometric coplanarity audit of every baked model now reports zero overlapping
faces with the same outward normal in the same plane, alongside the older
local-geometry and proportional-UV audits. A source-side absence of coplanar
model faces is **not** proof of an in-game fix; all orientations and grazing
camera angles still require screenshots. The HEAT, fuel and selective roasting
mechanics remain out of scope.
