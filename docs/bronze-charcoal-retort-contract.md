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

### 2026-10-24 — Roof-only copper slab pass (V3.2, visual review pending)

The user explicitly asked to **stop changing the whole machine at once**. The next iteration changes only the **top construction layer (Y=3)** in the generator: eight real roof cells appear as a joined field of Minecraft cut-copper slabs, with one unobstructed centre AIR cell, a square open copper chimney, and **exactly two** dark, continuous longitudinal iron bands running front-to-back along the left and right slab rows. No top-side exposed bricks or additional transverse decoration. This follows the roof portion of the original reference concept without promising full-machine similarity yet.

All Y=0–2 generated models, their textures, the existing real log/charcoal/tar port models, and the 3x4x3 **formation JSON and all recipes are unchanged**. The Y=3 Voxel collision profile is reduced to a 12-pixel-high slab to match its metal bands, and the chimney's collision walls now start at pixel 4; safely unform and re-form existing Retorts to refresh stored collision. The chimney shaft has no central cap; the AIR throat remains clear.

The end-to-end process still yields four charcoal plus one Wood Tar as an **item**. Physically wood tar is a viscous, dark liquid/semiliquid condensate from destructive wood distillation; deciding whether to use the mod's fluid system, tanks, pipes and separate pyrolysis distillate outputs is a *future gameplay decision*, not part of this roof model change.

**Review gate:** first confirm the roof in a high overhead and front-three-quarter in-game screenshot. Do not touch the lower three layers for another aesthetic pass until this one roof composition is accepted. Gradle compilation and runtime rendering remain unverified.

### 2026-10-24 — V3.3 roof grooves recessed into copper

The reference roof's two dark lengthwise lines are **carved grooves**, not raised rails. Only the top-layer model geometry changed: each of the three left roof tiles and three right roof tiles has a 2-pixel-wide channel with dark iron-coloured material at **Y=8.75 local pixels**, below the **Y=10 copper roof surface**. Copper walls on both sides of each channel remain visible. The six channels align continuously over the three front-to-back tiles and are correctly rotated for NORTH/SOUTH/EAST/WEST. The middle copper slabs remain flush and the original hollow chimney remains unchanged.

The previous roof collision slab is reduced from 12 to 10 model pixels to match the real copper surface. Exactly eight roof collision cells remain. All Y=0–2 model assets, port skins, formation patterns, process recipes, fuel logic, JEI and Patchouli data are **identical to the previous commit**. Re-form an existing machine to refresh stored voxel collision; use top and front three-quarter screenshots to accept this one roof detail before moving down a layer.

**Status:** model geometry statically verified; Minecraft render appearance and `./gradlew build` not yet verified.

### 2026-10-24 — V3.4 chimney-only copper vent collar

The copper slab roof and its twin inset grooves are unchanged. **Only the central roof AIR-host model** is altered, with one generated model for each SOUTH/NORTH/EAST/WEST orientation.

- **Seat**: wider but block-local copper lower mounting ring (local Y 9.5–10.75), meeting the actual roof surface at Y=10.
- **Vent body**: four strong corner posts joined by separate wall courses. Front, back, left and right have **two genuine horizontal rectangular cut-outs** each, at local Y 11–12.25 and 13.25–14.5. The wall geometry is absent inside each aperture, rather than an opaque wall textured dark.
- **Top lip**: copper collar widened 1px inside its designated AIR-host block (local X/Z 1–15) at Y=15–16, with a restrained dark line immediately below it.
- **Interior**: limited soot texturing on inner wall courses only; the central 6x6 chimney throat has no solid geometry and remains open. No smoke source or machine behavior was changed.
- **Safety**: no element extends beyond its own [0,16] block coordinates, and the model generator's face partitioner avoids overlapping quads. Coarse chimney collision is intentionally unchanged, since these narrow openings do not admit a player.
- **Scope**: precisely four chimney `part_*.json` files plus generator/tests/docs. No change to 8 copper roof slabs, inset grooves, real item ports, 3x4x3 formation, processes, item/fluid contracts or other multiblocks.

**Visual acceptance pending Minecraft screenshots**, particularly the front and both sides from a high enough angle to see the vent apertures, lower seat and lip. Forge build not claimed.

### 2026-10-24 — V3.5 chimney cleanup: UVs, inner faces and lower collar

After the user's close-up screenshot showed stretched-looking pixels, a dark/untextured-looking underside, and a strange protruding base platform, the chimney was simplified **without touching any other Retort layer**.

- **No lower collar:** the old floating, four-piece bronze platform at Y=9.5–10.75 is removed. The plain copper chimney body now begins directly at the copper roof top **Y=10**.
- **Clean rectangular vent openings:** four walls have two actual rectangular cuts in the copper geometry at Y=11–12 and Y=13–14, with no soot or copper panel hiding the slots.
- **One-piece visual language:** solid structural corner posts, narrow horizontal copper courses, and one consistent 1px-high top lip at Y=15–16. The hollow central throat stays genuinely open.
- **No texture distortion:** model geometry is pixel-aligned (integer local positions), the generator retains per-face UVs sized to each face in model pixels, and an automated test verifies 1:1 UV span and within-atlas boundaries for every rendered chimney face.
- **Inside/underside fixed:** the open inner shaft gets a soot finish by changing the texture of its **existing inward-facing quads after geometry partitioning**—no thin coplanar overlay geometry. All exposed lower and overhanging rim faces keep explicit copper textures and UVs.
- The four chimney meshes are regenerated in SOUTH/NORTH/EAST/WEST with fewer elements; collision, the 3x3 copper-slab roof, inset bands, Y=0–2 details, real input/output modules, machine functions, recipes and structure JSON are unchanged.

Still **pending actual Forge compilation and in-game screenshot acceptance**. No changes to the other Bronze machines. Roof-only visual iteration remains in effect.

### 2026-10-24 — V3.6 flush chimney seat + fully copper-textured inner shaft

User screenshots of V3.5 revealed two distinct remaining defects: the central chimney looked like it was **floating over an unfilled roof-cell gap**, and the inside walls looked black/untextured. The center roof cell was previously visualized only at local **Y=10..16**, while eight neighboring copper-slab roof cells had their top at **Y=10**.

The chimney AIR-host now includes **four copper surface segments** covering the entire center roof cell from Y=0 to Y=10, **except for the 6x6 open shaft (X/Z=5..11)**. Its exposed top is therefore **flush** with the eight real surrounding Cut Copper roof models, rather than a second raised collar/platform. The chimney walls still begin above the same Y=10 roof surface; the two genuine vent rows and open top remain.

The inner faces at X/Z=5 and 11 now use `#roof`, which resolves to Minecraft's Cut Copper texture in the roof models, including inside the new support down to the roof underside. This replaces the near-black soot-textured faces that visually resembled untextured/missing geometry. The shaft itself remains physically empty. No new overlay quads were added.

**Collision alignment:** the center AIR roof proxy's former four coarse wall boxes are replaced with eight collision boxes: four flush copper seat segments (Y=0..10) and four upper chimney-wall regions (Y=10..16), retaining an open 6x6 chimney interior. Total Retort voxel shapes change **34 -> 38**. The eight genuine copper roof slabs retain their height, as do the recessed dark channels. **Existing formed machines must be unformed and re-formed** to load the new stored voxel collision.

Scope: the **center roof model in four directions** plus neighboring Y=3 model files automatically updated by face culling at their touching edges; this does **not** alter neighboring copper slab geometry/texture or the lower three layers, ports, formation blueprint, processing, fuel, or recipes. Four-orientation visual acceptance and an actual Forge build remain pending.
