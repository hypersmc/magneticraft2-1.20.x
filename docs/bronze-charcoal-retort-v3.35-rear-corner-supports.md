# Charcoal Retort V3.35 — Matching rear side-wall corner supports

**Project:** Magneticraft2 · Forge 1.20.1  
**PR:** Draft #11 · `feature/bronze-age-foundry`  
**Status:** Static geometry verified; Minecraft visual review pending.

## What the user showed
The October 24 side screenshot shows the Retort's physical masonry and copper-framed vents reaching the rear, but the **rear ends of both left and right upper side walls lack the substantial copper-and-iron corner termination** visible toward the front.

## Root cause
Rear copper uprights previously ran only 2px into the side wall and the iron top/bottom cap returned only 0.5px. Brick and mortar geometry behind them was therefore left exposed, making the rear side frame look unfinished.

## Fix
The generator `tools/charcoal-retort/v3-models.mjs` now emits identical mirrored fittings in **rear upper Y2 left and right corner blocks only**, local SOUTH coordinates:
- Left: X0..3; right: X13..16; both Z0..3.05.
- Iron foot: Y0..2.75.
- Copper upright: Y2.75..13.1.
- Iron upper collar: Y13.1..15.65.
- Narrow copper roof-contact top: Y15.65..16.

All pieces are a short corner return, not a horizontal iron band. They end before the first real vent aperture at U4.2, and preserve the rear copper vent mullions, bricks and lower machine appearance. Existing older right/left partial corner pieces were replaced rather than duplicated, preventing overlapping equal-depth faces.

## Scope and validation
Exactly **8 rear upper Y2 models** were redesigned (two side blocks × four rotations). Regenerating shared-face culling also affects **8 neighboring Y1** and **8 roof-underside Y3** model JSONs. No machine logic, recipe, block placement, chimney, main roof-top outline, ports, front geometry, or other upper side-wall cells were changed.

In-memory generation and partitioning validated matched posts in all four rotations. Static checks checked the foot/body/collar/top, unchanged side/rear vent interiors, unmodified masonry away from the end, unchanged front block models, unchanged sampled visible Y1 faces and roof-top geometry, all modified face UVs, in-bounds elements, and defined texture aliases. V3.35 rear-corner checks were added to the source contract.

**Still required:** Run `node tools/charcoal-retort/build.mjs --check`, `node tools/charcoal-retort/test-contract.mjs` and `./gradlew build` locally; inspect both rear side corners and rear view in Minecraft before visual approval.
