# Bronze II — Charcoal Retort: first source prototype

**Status:** source-implemented candidate on draft PR #11; **no Forge build or live in-game verification** has been performed in this turn. The brick/copper V1 appearance is **not** an accepted visual baseline.

## Purpose and material balance

The Retort is industrial charcoal production for improved heat sources and iron reduction. It is not another ore crusher, not a replacement for the Foundry and does not invent Gear V2 power.

- A `magneticraft2:multiblock_processing` recipe consumes **4 burnable logs** and outputs **4 charcoal + 1 Wood Tar** after **720 active ticks**.
- The firebox consumes **a separate fuel item** (ordinary furnace-compatible solid fuel, excluding items with container returns; or Wood Tar).
- Each Wood Tar yields **200 burn ticks in this Retort**. Tar is immediately reusable fuel but cannot power its entire own 720-tick production batch.
- Fuel continues burning during an output blockage. This prevents the machine from pausing firebox time to create free heat.
- Mechanical airflow is deferred until the Industrial Air Blower; the Retort currently requires no Gear V2 input and provides no pressure capability.

## In-world interaction

Form a 3x4x3 JSON multiblock with Stone Brick foundation, Brick walls, Cut Copper roof and a center chimney AIR cell. Controller sits in the bottom front center. Build according to Bronze Age Patchouli Visualize; all four horizontal orientations are present.

The **three real working modules** are: logs at the front-left Item Input, charcoal at the front-right Primary Output and tar at the rear-middle Byproduct Output. They store controller slots 0/1/2. No decorative fake hopper/output inventory and no controller-wide item capability bypass. Controller right-click with fuel adds one item; empty hand shows status; sneak-empty-hand removes unused fuel. Chimney smoke is displayed only while the fuel firebox is lit.

## Save and safety contract

- Server simulation alone chooses and completes a recipe; item counts are authoritative.
- Both output slots are simulated before progress and before burning **new** fuel. Full outputs stall processing and keep existing logs and progress.
- Per-batch metadata: active recipe id and partial progress are saved. Fuel queue and remaining burn duration are saved independently.
- Controller dismantle drops queued fuel and inventory contents through the base multiblock behavior; a failed/blocked operation never duplicates the products.
- Item input module checks recipe acceptance; it does not accept arbitrary unrelated blocks. A new fuel automation module is deliberately **not** being claimed in the first slice.
- The structure uses Voxel V2 for full solid-cell collision plus small chimney geometry; no 3D model is stretched across block boundaries.

## Verification before acceptance

1. Run `node tools/charcoal-retort/test-contract.mjs` and `./gradlew build` (neither is claimed run here).
2. Form, break, unload/reload, and rebuild in **all four** directions. Confirm the roof's AIR proxy is present and the actual item ports remain usable from outside.
3. Feed precisely 4 logs and no fuel: no charcoal. Then add fuel and produce **exactly** 4 charcoal + 1 tar in 720 processing ticks, accounting for ignition and pauses.
4. Fully block either output; verify no log loss or phantom tar/charcoal, and that existing fuel continues to burn. Clear blockage and resume without restarting the batch.
5. Test hopper/item automation on real Input, Primary Output and Byproduct Output. The controller must not expose every slot via Forge ITEM_HANDLER. Fuel auto-input is intentionally pending.
6. Resume active batches through chunk unload/server restart; test fuel queue and last burn ticks across reload, and controller demolition salvage.
7. Open JEI to verify the recipe and catalyst; form from the Patchouli Visualize preview; confirm correct rotations and chimney smoke only when lit.
8. Inspect full 3x4x3 model from all sides and FPS impact. V1 reuses native Minecraft masonry/metal tiles to prove layout; new pixel-art styling remains a separate visual-review task.

**No production-readiness claims until compilation and live acceptance.**
