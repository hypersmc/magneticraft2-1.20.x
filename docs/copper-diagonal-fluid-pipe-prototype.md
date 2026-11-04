# Copper pipes: physical 45-degree runs

**Status: source prototype only; Gradle and game verification pending.**

## Placement and scope

With existing **Copper Fluid Pipe** items, **sneak-right-click** an ordinary pipe to select the first endpoint, then sneak-right-click another ordinary pipe to complete the run. Sneak-select the same endpoint to cancel. Normal placement continues unchanged. The endpoint distance must be 45 degrees across two axes, 2–8 grid steps. A run consumes 2–8 ordinary pipe items, one per real section; creative consumes none.

The placed cells travel one cardinal step from endpoint A, follow a 45-degree path, then join endpoint B with a final cardinal fitting. Both possible elbow orientations are checked so the clear one can be chosen.

Checks before placement: unchanged same-dimension endpoints, air in every occupied cell, corner clearance (both side cells around each diagonal crossing must be air), chunk availability, world border/build limit, and normal player use permissions. Failed checks leave blocks and inventory unchanged. Breaking one physical piece drops one ordinary Copper Fluid Pipe.

## Implementation

- `DiagonalWaterPipeBlock` is a real block without a block entity, tick or special item. Its saved `axis`, `section` and `socket` properties describe the tube and end fittings.
- Existing `WaterPipeBlock` remains cardinal-only; it connects to the special segment only at its explicit cardinal socket.
- `FluidPipeNetwork` remains the only fluid-network traversal. Diagonal cells connect exclusively through matching opposite ends and open corner clearance; there is no alternative fluid logic.
- Static baked 45° models reuse existing `fluid_pipe` texture and ordinary end fittings. Their two 8√2-pixel tilted half lengths each use exactly 8√2 UV pixels, preventing intentional texture stretching. Cached voxel shapes allow selection/collision without entities.
- The same Copper Fluid Pipe item ID, recipe, loot and pump/tank/capability endpoints are preserved.

## In-game tests — all pending

- [ ] JDK 17 `./gradlew build` and JSON/model/loot load.
- [ ] Existing straight/cardinal pipes still connect to pumps, tanks, washer and fluid ports.
- [ ] Build XY up/down, YZ up/down and XZ horizontal diagonal connections at 2 and 8 steps; confirm correctly connected 45° bodies, texture tiling and end fittings.
- [ ] Confirm neither missing model nor stretched textures from all camera positions.
- [ ] Block the preferred route; second legal orientation must work. Block both routes and ensure failure without edits or item loss.
- [ ] Test insufficient pipe count, protected cell, world border, chunk boundary and invalid angle/length.
- [ ] Pump water through standard pipe → angled section → standard pipe → tank/washer, including outputs if applicable.
- [ ] Break one middle section: fluid must stop, one pipe drops and the remaining parts stay selectable. Rebuild after recovering the old pieces.
- [ ] Place an obstruction in a previously clear corner. Fluid traversal must stop, then recover when obstruction is removed.
- [ ] Save/reload, cross chunk boundaries and two-client dedicated server test for no ghost connections or duplication.
- [ ] Check performance with many angled lines: no BE/tick/render-time geometry generation.

## Deliberate prototype limits

Only fixed 45-degree runs and existing ordinary endpoint pipes, with one short elbow at each end. No free-angle route, invisible collisions, new machinery, separate fluid graph, or multiblock JSON changes. If a block is placed into an already occupied diagonal corner corridor, flow becomes blocked but the physical pipe model remains visible and may clip that block; this is a known visual limitation to resolve if the prototype is accepted.
