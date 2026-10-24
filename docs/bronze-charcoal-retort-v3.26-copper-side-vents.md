# Charcoal Retort V3.26 — Framed copper side vents

**Forge 1.20.1** · Draft PR #11 · branch `feature/bronze-age-foundry` · **Visual candidate**

## Source of truth
The user's Blockbench edit in commit `33781105bf8a289bde3bb139ffc489c282ddeff9`, `charcoal_retort_south/part_1_2_0.json`, demonstrates the intended feature: **five copper uprights with four square ventilation-style apertures**, bounded by copper below and above. A previous V3.25 copper strip alone did not reproduce this detail.

## Generator implementation
`tools/charcoal-retort/v3-models.mjs` now generates the corresponding detail as block-local static geometry in the **upper front-right and upper front-left cells only**; the left version is mirrored. SOUTH-local vent frame geometry, per 16px cell:
- Four recessed slots along Z: (1.1,2.9), (4,5.9), (7,8.9), (10,11.9).
- Five copper mullions: Z0..1.1, 2.9..4, 5.9..7, 8.9..10, 11.9..13, from Y13.35..15.075.
- Upper copper header: Y15.075..16, Z0..13. Existing V3.25 lower copper cap: Y12.45..13.35.
- Right-facing side posts occupy X14.85..16; left-facing mirrored posts X0..1.15.
- The openings expose recessed soot backing, not structural through-holes: right side X14.2..14.3 and left side X1.45..1.6. The sealed chamber and the actual multiblock construction remain untouched.
- The front-left side core is selectively recessed only at its rearward side face; its front-facing iron bracket is preserved. The front-right brick upper course ends at the base of the vent to keep the four slots open.

The user-created Blockbench model contained 0.1px out-of-bounds elements and Blockbench texture path formatting; the generator uses in-range 0..16 cuboids and the pre-existing valid `minecraft:block/cut_copper` material. The manual original remains available in the preceding Git commit.

## Scope / regression checks
All four orientations generated deterministically in memory. 16 generated Retort models or adjacent hidden-face meshes are affected; no recipes, items, formation JSON, genuine I/O, voxel collision, Y0/Y1 layers, or roof-top artwork change. Every front upper corner was sampled to verify **5 outer copper posts, 4 recessed dark openings and the upper copper beam**. Exposed front facades remain unchanged; roof top faces unchanged, no out-of-range geometry or stretched UVs.

Regression assertions were added to `tools/charcoal-retort/test-contract.mjs`, and the V3.24 top masonry course expectation now checks its preserved lower edge below the new vent.

**Pending:** Run `node tools/charcoal-retort/build.mjs --check`, `node tools/charcoal-retort/test-contract.mjs` and `./gradlew build` locally; verify front-right, front-left, and both side angles in Minecraft. In-game visual acceptance is not yet claimed.
