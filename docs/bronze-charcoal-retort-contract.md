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

The **three real working modules** are: logs at the front-left Item Input, charcoal at the front-right Primary Output and tar at the rear-middle Byproduct Output.

**Patchouli orientation correction (2026-10-24):** use `OBU` for the book's front module row even though the SOUTH formation JSON uses `UBO`. The projector mirrors its displayed row when aligned by the controller. The live formation log showed Primary Output occupying the Input position. The regression test now compares every book layer against the corresponding actual SOUTH blueprint, accounting for that projection reversal. No physical multiblock JSON has been changed.
 They store controller slots 0/1/2. No decorative fake hopper/output inventory and no controller-wide item capability bypass. Controller right-click with fuel adds one item; empty hand shows status; sneak-empty-hand removes unused fuel. Chimney smoke is displayed only while the fuel firebox is lit.

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

## 2026-10-24 — Copper-clad V2 visual model (review pending)

The original formed model was effectively a plain 3×4×3 brick box with a full-block copper roof. V2 is modeled after the approved **direction** of the single-machine concept (not yet a visual sign-off):

- Lower dark stone foundation with narrow bronze/iron bands and a separate recessed, status-lit controller firebox.
- Recessed central sealed hatch, distinct upper ventilation grille, reddish refractory masonry courses and reinforced side corner details.
- Thinner tiled copper roof with raised corner/edge caps and a real hollow square chimney in its single designated AIR proxy.
- Six original 16×16 native-pixel material textures. The exposed metal frame and foundation reuse established Settling Tank material roles. UV coordinates are cropped to each element; no full texture compressed over narrow bars.
- Original **3×4×3 JSON construction and all three real modules unchanged**. No decorative inventory, extra fake pipes, hidden module overlays or moved block positions.
- Per-facing parts are rotated by the generated models; formed controller blockstates do not apply a second Y rotation. An unformed standalone controller still uses its normal facing rotation.
- Runtime ore/fuel/charcoal/tar recipe, capacity, burn timer and output reservation remain unchanged.
- The new Voxel V2 profile follows the shortened roof, hollow chimney and front-center recessed hatch at a coarse physical level. **Safely unform and re-form existing machines** to rebuild the Voxel V2 collision snapshots; the blueprint blocks themselves do not need rearranging.

Reproducible model build: `node tools/charcoal-retort/build.mjs`, and drift validation via `node tools/charcoal-retort/build.mjs --check`. Existing `node tools/charcoal-retort/test-contract.mjs` checks the base process/guide contract plus V2 asset integrity.

**Not claimed:** completed Forge `./gradlew build`, FPS acceptance, in-game appearance, or final user-approved textures. V2 is a playable art prototype for screenshot-based iteration. Remember to check first-person interaction with all three real modules and the firebox at all facings.


### 2026-10-24 — V2 geometry-placement repair, not a new layout

The user's later close-up shows the real block construction is correct: the fault is overlapping **baked model geometry**. A generator audit found 39 intersecting cuboid pairs in 13 SOUTH model files (and their rotated counterparts): lower stone footplates, controller button, middle hatch, side trim and chimney. For example a stone foundation [0,0,0]..[16,12,16] intersected two front bands in the same cell.

`tools/charcoal-retort/build.mjs` now partitions later trim from earlier opaque material and bakes only actually exposed surfaces, including shared multiblock filler boundaries. The repaired models stay strictly inside their respective 16×16×16 block cells with no runtime per-frame re-meshing. No JSON construction, I/O, recipe, copper/brick texture, or Voxel profile changes are needed. The previous V3 visual concept remains **optional after** the user inspects this corrected V2 result in Minecraft. **Neither appearance nor Forge compilation is yet signed off.**

## 2026-10-24 — V3 visual implementation (review pending)

The first actual V3 shell is now authored from the previously accepted *direction* of the concept, replacing the V2's visually bland surfaces. The physical 3×4×3 footprint, exact JSON materials, recipes, module types, item quantities and server-side processing remain unchanged. New geometry emphasizes dark stone plinths, a dominant framed one-block kiln door, a smaller high vent, copper roof cap, four-sided open chimney, corner pilasters and the separate bottom firebox. It remains pixelated/baked, with geometry confined to each structural cell.

**Crucial real-port integration:** The shared Item Input / Primary Output / Byproduct Output blocks now use a custom dynamic model selector with the **original generic model as fallback**. Only when linked to a formed Charcoal Retort does each real port switch to the V3 intake hopper, charcoal drawer or tar drain. Forge capabilities, existing block entity, slot semantics, coordinates, drops and item interactions are retained. The Retort now links its three existing ports to its controller as part of formation; it does not change any other machine's formation. Static models are registered for all four facings and pre-baked by ModelManager.

V3 port meshes temporarily take their real block cell locations during asset generation to allow the collision-free face partitioner to remove hidden faces against the surrounding masonry. They are then renamed to formed-state models **and removed from the V2 filler part names**. No fake port filler remains in the world.

**Current status:** source prototype, pending compilation and in-game visual comparison to the single concept image. Existing formed Retorts should be safely unformed and formed again to establish port-controller links and refresh Voxel collision. Test all four facing variants, port automation, reload behavior and FPS with multiple machines. No visual acceptance asserted.

### 2026-10-24 — V3.1 silhouette revision from five in-game views

The user provided all four elevations plus a roof screenshot. They confirmed the V3 source is *rendering*, but the model still reads as a flat brick box rather than the detailed reference. The ports blend more naturally; the main remaining problems are **weak depth at the main kiln hatch**, **chimney too close to the copper roof plane**, flat corners and relatively indistinct input/output interiors.

V3.1 is a **visual-only correction**: the physical build stays 3x4x3, with all three genuine modules, formation JSON, recipes, automation/capabilities, power/fuel contract and item outputs untouched. We replace the front center full brick cube with inset masonry and dark backplate, add several independent thin grate bars and a thicker door surround; reduce roof field height beneath its coping to expose more of the hollow raised chimney; lightly recess outside side masonry behind ribs; and make log/charcoal port interiors visually distinct. No additional multiblock blocks, hidden output or renderer loop. The baked model repair step still partitions and culls intersecting surfaces.

Source/data geometry sanity checks include a chimney rim at least four model pixels above roof edge, a genuinely recessed dark hatch, model bounds and formed port interiors. **Acceptance pending screenshot-based in-game comparison and an actual Gradle build.** No new texture atlas, extra construction block or functional change is claimed.
