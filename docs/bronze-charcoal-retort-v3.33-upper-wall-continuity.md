# Charcoal Retort V3.33 — Full masonry and copper continuity around the upper chamber

**Magneticraft2 · Minecraft Forge 1.20.1**  
**Draft PR:** #11 (`feature/bronze-age-foundry`)  
**Visual status:** Candidate for in-game review.

## Reported issue
The user's side screenshot shows a copper upright/iron footing at one end of the three-block wall, but the other end lacks a matching external support. The side wall's physical staggered masonry effect is also missing from the opposite side and likely from the back of the multiblock.

## Generator correction
In `tools/charcoal-retort/v3-models.mjs`:
- **Left side**: replace the former flat fallback model with five rows of individual staggered bricks and a recessed mortar substrate. The first, middle and rear upper left blocks now match the accepted right-hand masonry treatment; physical face depths are 15.45–15.70px, except the front-left return which uses 15.45px to avoid showing additional slivers on the locked front.
- **Both ends**: expose the missing copper end reinforcement while keeping the other existing copper uprights and the short iron feet. Remove the old 16px-long left-hand bottom iron stripe rather than reintroducing the broad rejected bands. Join the front-left side reinforcement behind the already accepted front-facing copper/iron bracket.
- **Back of chamber**: create individually raised brick courses and recessed mortar across all three rear Y2 blocks, terminating behind the original copper end buttresses.
- **Rear vent**: continue the existing side-wall styling across the rear via one copper-framed band with exactly four shadow-backed recessed openings over the full 48px width. Its joints cross the real block seams without added dividers. These are **visual vents**, not holes in the processing chamber.
- **Keep functional structures**: the 3×4×3 block layout, charcoal/tar I/O, Y1 hatches and port block entities, Y3 roof/chimney, center front grille, and active processing logic are not redesigned.

## Generated assets and checks
All four rotations were generated and checked. The diff contains 20 Y2 wall models, 12 neighboring Y1 models with hidden-face culling adjustments, and 16 adjacent Y3 roof models whose externally visible top geometry remains unchanged — **48 generated model files** in total, plus the generator, regression tests and this document.

In-memory checks passed: 24 side masonry samples, 12 rear masonry samples, 16 copper corner samples, 68 vent/frame samples, 288 preserved front-face samples, 48 lower-block samples, 16 roof-top model comparisons, 7,012 valid UV-bearing model faces and cell bounds. No sampled accepted front-facing geometry changes, and every raised block remains inside 0..16. This is a static candidate, **not** an in-game visual acceptance.

### What to test in Minecraft
Inspect both full side walls, each side's front and rear copper support, and the complete rear face. Verify identical staggered masonry depth, no long black horizontal rails, four shared openings over three blocks (including the rear), clean roof contacts and unchanged front/lower ports.

**Not run here:** `node tools/charcoal-retort/build.mjs --check`, `node tools/charcoal-retort/test-contract.mjs`, `./gradlew build`, and an in-game rendering check.
