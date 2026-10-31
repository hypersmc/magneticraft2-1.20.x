# Ore Roasting Kiln — FIRST concept reference build

Visual reference: first long industrial Ore Roasting Kiln design selected by user. Agreed blueprint **5 wide, 3 high, 9 long**, superseding planned 5x3x5. Intended long brick hearth, three pairs of arched side vents, roof ridge and repeating dark iron structural bands, stone foundation, rear flue, and real item I/O at front. **No decorative large gear or shaft**.

This commit begins a **visual / structural prototype**. Gameplay and chemistry are deliberately not invented; input remains locked until an agreed sulfide concentrate recipe and byproduct handling. No existing refractory-kiln state or recipes modified. All geometry must remain block-local and collision must match roof. The chimney is kept within 3-high limit; a tall chimney requires a 4-high multiblock decision.

Visual acceptance requires side/front/roof/rear in-game screenshots, 4 directions, no stretched UV, no floating braces, port ownership, and correct book Visualize. Not a completed machine.

## Generator and static validation

Original 16×16 tiles: `tools/ore-roasting-kiln/texture-art.mjs`
and `build-textures.mjs`; canonical 5×3×9 structure/roof geometry:
`tools/ore-roasting-kiln/generate.mjs` and `visuals.mjs`. No borrowed
Retort/Kiln-specific art, and no dynamic per-frame rendering.

Run from repository root:
```bash
node tools/ore-roasting-kiln/build-textures.mjs --check
node tools/ore-roasting-kiln/generate.mjs --check
node tools/ore-roasting-kiln/test-contract.mjs
./gradlew build
```
These checks have **not** been executed in a full Forge checkout here. Tests
must include a formed machine in all four orientations, construction as shown
by Patchouli, the 9-block length, true open chimney, non-floating roof,
correct UVs and matching Voxel V2 collision. No roasting UI/recipe acceptance
is claimed before an actual thermal/material loop exists.
