# Charcoal Retort V3.36 — Front lower-center furnace door

**Magneticraft2 | Forge 1.20.1 | Draft PR #11** — Visual candidate for in-game review.

## Visual comparison
The user's current front screenshot shows the Y1 middle as a thin exposed vertical grille beneath the copper Y2 crown. The reference shows a substantial recessed, dark iron furnace door framed by copper structural posts. The new design addresses **only the single front-center Y1 block**; the upper copper stepped crown, all side and rear masonry, ports and roof remain as accepted.

## Generator changes
In `tools/charcoal-retort/v3-models.mjs`, the `front&&x===0&&y===1` branch now:
- Positions three narrow iron grate bars deeper inside a proper square bezel, rather than leaving them floating without a door.
- Builds four physical iron perimeter rails around the recessed dark cavity and adds two thinner dim-grey bevel strips inside.
- Replaces the oversized Y1 straight top copper crossbar with a dark iron inset beneath the retained Y2 stepped copper crown. This does not introduce a copper strip in the Y2 model.
- Adds small copper corner shoulders and short iron feet tied to the existing two full-height copper uprights.
- Reuses `minecraft:block/cut_copper` for Y1 center copper and `magneticraft2:block/settling_tank/riveted_iron` for the small dark joint, matching the accepted Y2 center instead of the old bronze palette.

No fabricated permanently-lit flames are added to Y1. The working, dynamic lit firebox remains the actual **Y0 C controller**, which has its own state-dependent model.

## Strict scope / checks
Only four generated copies of `part_0_1_0.json` change (`south/west/north/east`); no other asset files change except this generator, updated visual regression contract, and documentation. In-memory generation checks passed: matching copper/iron texture aliases, nine targeted visible-surface probes in each orientation, a clear copper-free central door, proportionate UVs, valid 0..16 element bounds, and defined texture aliases.

**Pending:** `node tools/charcoal-retort/build.mjs --check`, `node tools/charcoal-retort/test-contract.mjs`, `./gradlew build`, and actual Minecraft screenshots. Inspect straight on and from a slight front diagonal, checking clear dark iron bezel, visible grating, depth, lower copper feet, and how it meets the Y2 apron and Y0 controller.
