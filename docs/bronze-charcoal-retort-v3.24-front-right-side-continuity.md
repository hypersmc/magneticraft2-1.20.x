# Charcoal Retort V3.24 — Front-right side-wall continuity

**Draft PR:** #11, branch `feature/bronze-age-foundry`  
**Forge:** 1.20.1  
**Visual status:** Candidate — Minecraft screenshot review required

## Problem
The front-right Y2 brick cell (`x=1,y=2,z=0` in SOUTH-local coordinates) retained a flat right-facing brick face with broad upper and lower iron bands. V3.23 had only fixed the two cells behind it. The front-to-side join therefore did not match the user's October 24 reference.

## Change
The right-facing portion of that same front corner now uses five staggered rows of physical bricks with depths 15.45/15.70 pixels and 0.2-pixel recessed mortar backing. The solid brick side core stops at X14 and the exposed brick rows at Z12.85 so the existing front copper corner upright remains connected. The previous full-length iron contact strips stop at Z13, retaining the corner fittings without creating bands across the entire side. A `sideBrick` texture key is added to this front-right model only; original front masonry texture and fittings remain.

No construction layout, machine logic, recipe, ports, roof shape or chimney geometry was changed. Changes to four roof-neighbor JSONs are hidden-face regeneration only. The actual changed generated assets are four upper front-right meshes and four contacting roof undersides (four orientations).

## Checks
In-memory deterministic generator and face-partition pass completed for SOUTH, WEST, NORTH and EAST; each front-right part shows 15 raised side bricks across 5 courses, two brick depths and recessed mortar, with no long dark side bands or out-of-bounds elements. The outward roof top faces are unchanged. New V3.24 assertions are in `tools/charcoal-retort/test-contract.mjs`.

**Not run:** actual `node tools/charcoal-retort/build.mjs --check`, `node tools/charcoal-retort/test-contract.mjs`, `./gradlew build`, Minecraft client. Re-form and inspect from front-right, right side, and back-right in all orientations; particularly the copper upright, bracket, brick joints and roof seam. Do not mark visually accepted until screenshots are reviewed.
