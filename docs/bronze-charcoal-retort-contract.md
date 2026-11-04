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

### 2026-10-24 — V3.7 upper-brick-chamber-only visual pass

The user accepted the **copper slab roof, recessed twin grooves, and corrected flush chimney**. These are now a **locked/accepted visual baseline** for the Retort; do not modify them again while styling the remaining layers unless a regression is demonstrated.

The user marked the reddish masonry region immediately below the roof in the original reference. This spans portions of more than one physical machine course from the viewing angle, so the next isolated model iteration intentionally targets **physical Y=2 only**. That layer is eight real Brick construction blocks around one required center AIR throat; Y=1 contains the lower hatch and real ports and will be reviewed separately.

Y=2 now uses native **Minecraft brick masonry (1:1 16px UV scale)**, a structurally deep upper front ventilation opening with narrow bronze framing, four metal separators forming five dark recessed slots, two substantial dark riveted corner fasteners, and restrained structural side bands. The visual mass remains brick with metal/copper as accents; no fake mechanical shaft, new block or invented vent behavior.

**Hard model-scope check:** exactly 32 generated `part_*_2_*.json` files changed (eight Y=2 parts per facing). All Y=3 roof/chimney files, all Y=0/Y=1 models, all formed port skins, all construction JSON, recipes, JEI/Patchouli data and Voxel collision profiles are **byte-for-byte unchanged**. The mesh generator verifies block-local geometry and eliminates hidden/coplanar faces without touching those accepted layers.

**Status:** proposed Y=2 visual prototype, *not yet accepted in-game*. Compare front, side and three-quarter screenshots with the marked concept; keep the accepted roof fixed while iterating. Full `./gradlew build` is not claimed in this turn.

### 2026-10-24 — V3.8 Y2 correction following in-game/reference comparison

V3.7 failed visual review: its brick face looked flat and dark compared with the red refractory masonry in the reference, the supposed copper trim appeared tan/wooden, and the vent surround, reinforcing posts and heavy corner brackets were insufficiently defined. The accepted copper roof seemed jagged in a low-angle screenshot; its **underlying assets are unchanged** and that appearance must be evaluated at a comparable elevated angle before diagnosing a roof regression.

V3.8 changes **only eight Y=2 brick part models per orientation (32 files)**, reverting the Y2 brick material to Magneticraft2's established `charcoal_retort/refractory` texture, setting Y2 bronze/copper accents to the same `minecraft:block/cut_copper` material used by the accepted roof, and making all Y2 `join` details riveted dark iron rather than tan bronze. It uses a larger central *recessed* five-slot grille with exactly four visible copper dividers and a dark backing, adds paired front copper pilasters just inward from the corner brackets, keeps the massive outer roof-level iron fastening blocks, and adds a restrained lower sill. All details stay within their 16x16x16 block cells and receive proportional 1:1 face UVs.

The generator's adjacent-face culling outputs for **every Y=0/Y=1/Y=3 part are byte-for-byte identical to V3.7**. Construction, chimney shaft/roof grooves, item port models, process recipes, all gameplay data, and the stored Voxel profile are unchanged. This is **not visual approval**: check front, side, elevated three-quarter shots against the actual design reference before proceeding to Y=1. The complete Forge Gradle build and in-game result are unverified.

### 2026-10-24 — V3.9 Y2 face placement and front copper-band continuation

The user's V3.8 three-quarter screenshot marks two missing dark-line terminations at the upper roof front lip, and again rejects the Y=2 masonry/front as inaccurate. Inspection found a concrete layout defect: **both front Y=2 brick shoulder cuboids terminated at local Z=13**, leaving the visible wall field recessed three pixels behind its surrounding copper trim and corner supports.

This patch brings the **visible middle of those brick shoulder cells (local Y=1..15)** forward to the front face at **Z=16**, while retaining the exact existing **Y=0..1 and Y=15..16 interface footprints** used for adjacent layer face culling. It adds a single restrained under-roof dark iron band at local Y=14..15 across the three upper front cells.

For the two front copper-roof cells only, the dark recessed longitudinal grooves are now **physically continued down the vertical front lip** (inset z-depth from the copper face, not raised bars). The remaining six copper roof slabs, center chimney, deep grooved top surfaces and all Y=0/Y=1 machine parts are untouched. The 3x4x3 construction blueprint, recipes, mechanical/heat/item behavior, port skins and collision profile are also unchanged.

**Scope and QA:** twelve Y=2 front model assets (three per facing) plus eight front Y=3 roof model assets (two per facing), 20 total generated models, with no changes outside those positions. Generation static checks confirm all four facings have two groove terminations, the front brick surfaces are visible, UV spans match exposed geometry, and no coplanar faces overlap. Retort V3.9 remains a **visual candidate, not yet user-accepted**. Confirm front corner/brick appearance and both roof-band ends in an elevated three-quarter screenshot before any Y=1 work.

### 2026-10-24 — V3.10 front-only chamber matching (review pending)

User marked the entire Y2 front region against the target concept and explicitly identified the **continuous dark strip under the copper roof** as an unwanted detail introduced by V3.9. The front remains the priority before changing the side or lower layers.

- **Removed:** the full-width, local Y=14..15 iron ribbon at the front of every Y2 masonry position. The intentional discrete outer square corner fittings and copper roof's two dark **vertical** groove terminations remain.
- **Brick front:** switched only the three **front-facing** Y2 brick model cells to native Minecraft `minecraft:block/bricks`, with readable, light-grey mortar joints and native-scale UV cropping. The two side walls and rear of Y2 stay on their established `charcoal_retort/refractory` texture and geometry. This is a deliberate front-only trial based on the supplied reference, not a global material change.
- **Vent:** kept a recessed soot-backed five-opening grille and a copper lintel/sill, adjusted the four slim copper bars so they stop below the upper lintel rather than visually filling the slots. Real copper vertical supports and dark corner fittings remain on the two adjacent masonry cells.
- **Locked regions:** all Y0/Y1 part models, all formed item-port models, all other Y2 side/rear models, the chimney and all exposed copper-roof/groove geometry remain visually unchanged. Four Y3 west/east front-roof files are regenerated only because the face-culling step *merges two equivalent underside quads* after the bordering Y2 geometry changes; their visible top, sides, band geometry and texture mappings are identical. The physical 3x4x3 formation, collision profile, recipes and fuel/port behavior are unchanged.

All four orientations were checked for valid 16-pixel model bounds, UV proportions and no coplanar overlapping rendered faces. **This is still a candidate**, not a claim that the in-game appearance matches the reference. Wait for a frontal screenshot before changing the side or the next physical layer. `./gradlew build` was not executed.

### 2026-10-24 — V3.11: FIRST front-left Y2 brick block in isolation

After V3.10 still failed the reference comparison, the user chose a tighter approach: **work on the single upper-chamber front-left block** from the red rectangle in the reference, and explicitly preserve the **three-dimensional feel**. This is *one physical 16x16x16 brick cell*, not a redesign of the full facade.

**Precise SOUTH-facing local model recipe** (`part_-1_2_0.json`; all coordinates 0–16 model pixels):
- Structural brick backing: main recessed wall from Z=0..13, plus side reveals at X=0..3 and X=13..16, a bottom reveal Y=1..3 and a top reveal Y=13..15. The open central brick panel at X=4..13, Y=3..13 exposes its brick texture at **Z=13**, a **3px recess** behind the frontmost trim.
- Left copper corner pillar: X=0.5..3, Y=3..13, Z=14..16, with a wider copper bottom shoulder at X=0..4, Y=3..4.5.
- Substantial dark iron top-left bracket: X=0..4, Y=12..16, Z=14..16, with inset riveted-metal frame and a separate darker foundation-textured boss at X=1..3, Y=13..15, Z=15.5..16.
- Bottom outer support: dark iron foot X=0..4, Y=0..3, Z=14..16.
- Inner edge near the unchanged central vent block: copper separator X=13..15, Y=3..13, Z=14..16, with separate small iron foot/cap.
- Bottom sill: **short copper segment** X=4..13, Y=0.5..1.75, Z=14.5..16. **No** full-width iron band beneath the copper roof.
- Material map remains identical to V3.10: `minecraft:block/bricks` for this front brick panel, `minecraft:block/cut_copper` for copper, and existing Retort iron/stone materials. All exposed faces use proportional UVs (no stretched textures).

**Scope:** one authored local cell `x=-1,y=2,z=0`; generated for SOUTH/WEST/NORTH/EAST as four front-left parts. Six *neighbor model files* can regenerate from automatic internal seam/underside culling because adjacent faces are shared. Those are **not additional facade redesigns**. The roof's visible top/grooves/chimney, all other front decoration, Y0/Y1, ports, formation, recipes, power, animations and Voxel collision remain unchanged.

**Status:** candidate for a single-block in-game review only. Do not copy it to the right block, alter the central vent or continue down layers until the user explicitly accepts this front-left block. No `./gradlew build` claim.

### 2026-10-24 — V3.12 front-left brick return only

The screenshot after V3.11 revealed a missing/hollow strip of masonry immediately beside the **right-hand (inner) copper divider** of the front-left Y=2 block. The agreed change is minimal: leave the exterior copper pillar, center vent, iron corner fittings, short sill and original three-pixel-recessed main brick panel alone, and close the notch where the panel joins the right copper divider.

Added exactly **one local brick cuboid** to the front-left chamber cell: `from=[11,3,13]`, `to=[13,13,15]`, in the SOUTH-authored frame. This two-pixel-wide masonry return is one model pixel behind the front of the copper divider (front at Z=16), while the bulk brick panel remains at Z=13, so the section retains visible 3D depth. The model generator outputs four variants, one per cardinal facing, and does not change any neighbor model file.

Static tests verify that all four directional models contain both the main recessed panel and its nearer right brick return at local front depth 15 with native 2×10 UV scaling. No Y=3 accepted roof, chimney, Y=0/Y=1 layers, center grille, opposite front block, real port skins, formation JSON, recipes or collision data changes. This remains **pending in-game appearance acceptance and an actual Gradle build**. Do not mirror or continue to the next block yet.

### 2026-10-24 — V3.13 single-block wall instead of a hollow alcove

After side-by-side screenshots of V3.12 in Minecraft versus the target render, the user approved changing the approach for **only the front-left Y=2 chamber cell**. The key realization is that the reference shows an essentially solid brick wall with structural copper and iron framing slightly proud of it—not a deeply recessed panel with reveal walls.

V3.13 replaces that cell's previous partial brick backing + side reveals + small right return with **one continuous masonry cuboid from local `[0,0,0]` to `[16,16,15]`**. It reaches 15/16 pixels of the block depth. The outer left copper column, its dark riveted corner bracket, inner copper separator and lower iron foot still reach local Z=16, giving a restrained **1px depth difference** rather than the rejected 3px brick cavity. The old floating-looking multi-step lower sill is simplified to one narrow copper strip from `[4,1,15]` to `[13,2,16]`.

Exactly one authored local cell (`x=-1,y=2,z=0`) changes, exported as four cardinal facing models. The neighboring center grille and adjacent roof model files may regenerate **hidden contact faces** as a mesh-partitioner consequence; their visible front/roof outlines and materials are unchanged. No opposite front block, other roof or chimney surfaces, Y0/Y1 models, real ports, collision profile, formation, heat/power or recipe code changes.

**Static QA:** all four model orientations have at least 120 square model pixels of visible solid brick face at Z-depth 15, with copper/iron trim at depth 16; no remaining depth-13 front bricks, overlapping faces or UV scaling errors. **Not yet an accepted in-game result**; inspect exactly this left block before mirroring, touching the center grille, or moving to other layers. Full Forge compilation not run.

### 2026-10-24 — V3.14, one-block physical brickwork rebuild (visual candidate)

After V3.13's flat, nearly full-cube wall failed the user's visual comparison, this pass rebuilds **only the upper front-left brick cell** (`SOUTH: x=-1, y=2, z=0`) from explicit cuboids rather than patching the wallpaper-like face.

**SOUTH-facing design geometry**, coordinates are 0–16 within the block:
- Structural `brick` core: `[0,0,0] -> [16,16,14]`.
- Thin gray `mortar` backing: `[3,2.5,14] -> [13,15,14.35]`. Texture `minecraft:block/stone` is scoped exclusively to this one cell.
- Course A, Y 3.00–6.55, face base Z14.35, individual bricks X 3.25–7.9 and 8.35–12.75.
- Course B, Y 7.05–10.60, individual bricks X 3.25–5.45, 5.9–10.15, 10.6–12.75.
- Course C, Y 11.10–14.55, individual bricks X 3.25–8.2 and 8.65–12.75.
- Brick front depth: normally Z15; slightly offset brick faces Z14.85 in selected segments to create subtle relief without a cavernous alcove. Visible mortar joints are 0.45–0.55px wide.
- Outer copper structural post: `[0.5,3,14] -> [3,13,16]`, shoulder `[0,3,14] -> [4,5,16]`, small step `[0.5,5,14.5] -> [3.5,6,16]`.
- Dark iron outer foot `[0,0,14] -> [4,3,16]`, upper bracket `[0,12,14] -> [4,16,16]`, riveted plate `[0.5,12.5,15] -> [3.5,15.5,16]`, inset dark boss `[1,13,15.5] -> [3,15,16]`.
- Narrow copper inner separator `[13,3,14] -> [15,13,16]`, iron foot `[13,0,14] -> [15.5,3,16]` and top cap `[13,13,14] -> [15.5,15,16]`.
- Minimal bottom copper sill `[4,1,15] -> [13,2,16]`. No full-width iron roof band.

The build generator authors this one SOUTH-local cell and rotates it for the other three facings. As a side effect, the four adjacent center-grille meshes and four touching roof meshes are regenerated for hidden internal face partitioning; their exterior visible faces, material maps, geometry, item port functions and construction coordinates remain unchanged. Other machine layers, roof/chimney shapes, formation blueprint, recipes, fuel/production and voxel collision remain unchanged.

Validation criteria: three **staggered** rows of visible physical brick geometry, real thin mortar seams, outer/inner copper supports, darker bracket/foot, 16px bounds, per-face 1:1 UV dimensions, and no overlapping model faces across all four facings. **The visual match remains unapproved** pending a front three-quarter Minecraft screenshot; `./gradlew build` has not been run. Do not copy this model to the right front block or redesign other layers until approved.

### 2026-10-24 — V3.15 L-shaped frame on the first front-left brick block

The user compared V3.14 in-game to the reference and **liked the staggered 3D masonry in the middle**, but the surrounding copper/iron frame was visually wrong. The reference has one large left vertical copper pillar and a **wide horizontal copper header**, reading as an **L-shape**. V3.14 unintentionally had a visually symmetrical U-shape: an additional thick right copper pillar, an upper-right iron cap, lower-right iron foot and a spanning copper bottom sill.

V3.15 retains the V3.14 three staggered masonry courses, 0.45–0.55px exposed mortar gaps, 14–15px shallow brick depths, left copper pillar, substantial left-top dark bracket with riveted inset boss, and lower-left stepped shoulder. In the SAME single front-left Y2 cell only:

- Extend the brick course last spans to local X=15.75; mortar bed extends to X=16 so there is a masonry face beneath the removed right post.
- Introduce a broad cut-copper horizontal header at `[3,12.75,14.5] -> [16,16,16]`, plus a 0.5px narrow copper lower step at `[3.75,12.25,15] -> [16,12.75,16]`. It connects visually to the outer post without inventing a dark continuous under-roof metal band.
- **Remove** the right vertical copper strip (`X=13..15,Y=3..13`), its top dark cap, its lower dark foot, and the long lower copper sill.
- Keep the central grille, entire right front cell, all other chamber/side blocks, chimney/roof grooves, Y0/Y1, recipes, multiblock formation and collision profile unchanged.

Generated asset scope is 12 files: four copies of the single authored left cell, four center-neighbor files whose **outward faces do not change**, and four accepted-roof-edge files whose **non-bottom faces do not change**. Static geometry checks confirm two copper header segments and zero remaining tall right copper uprights, retained physical brick courses/mortar, model-local bounds, zero coplanar rendered overlaps, and native proportional UV mapping in all orientations.

**Status: V3.15 pending visual acceptance in Minecraft.** This is not a claim to have performed a Forge Gradle build or in-game screenshot test. Do not copy the design to the right/front-center or proceed to the next layer until the user accepts this block.

### 2026-10-24 — V3.16 restore visible 3D brick relief (front-left ONLY)

User review of V3.15 confirms the **L-shaped copper frame is finally in the correct position**, but the staggered brick courses no longer read as strongly 3D in Minecraft. Preserve the accepted-in-principle placement and fix ONLY the relief of the existing masonry courses.

The wall's solid structural core remains `Z=0..14`, the thin mortar backing is `Z=14..14.2` instead of the former `Z=14..14.35`, and the **same individual staggered brick cuboids** now run `Z=14.2..15.45` or `Z=14.2..15.70`. This produces real model-pixel side faces with **1.25px and 1.50px physical relief** over the mortar, approximately 2–3× the previous effect. The copper/iron outer pillar, top-left bracket, and full horizontal header **remain unmodified at depth 16**; the right vertical pillar remains absent. Individual brick X/Y spans and three-course layout are identical to V3.15, and no new texture assets or overlays are introduced.

Only the one authored front-left Y2 block is changed in four orientations. Four center-Y2 meshes regenerate automatically for internal face-culling changes; their outward visible faces are unchanged. No accepted roof/chimney Y3 assets, opposite/front-right block, Y0/Y1, port skins, blueprint, recipes, mechanical or collision data change. Static QA checks per-face proportional UVs, no coplanar rendered faces, retained course staggering, and alternating 1.25/1.50px depth. **Minecraft appearance and full `./gradlew build` remain unverified**. Await in-game evaluation of this block before changing anything else.

### 2026-10-24 — V3.17, correct the TWO left corner fittings

The user's side-by-side annotated screenshots identify only the upper-left bracket and the lower-left shoulder as the remaining wrong details on the first Y2 front-left cell. In the in-game V3.16 model, the top bracket read as a flat blue-grey iron corner, while the bottom read as an oversized dark iron foot. The target reference instead shows a **square iron bracket with a dark recessed center** and a **protruding copper shoulder above a smaller dark iron foot**.

**Only the front-left Y2 cell is authored:** The upper bracket remains local X=0..4, Y=12..16, but is now modeled as a backplate ending at Z=15.25 with four raised iron border rails to Z=16, four thinner inset riveted rails, and a central dark-stone 2x2 square whose exposed face ends at **Z=15.65**. The previously solid upper frame is split to avoid hiding that recess. The structural top contact against the approved roof remains filled.

The lower 4px-wide shoulder is copper from **Y=1.25 to Y=4.5** at Z=14..16, replacing the lower part of the old 3px-tall dark-iron foot. Dark iron is retained as the shorter base from **Y=0..1.25**; the concealed backing behind the copper is still solid. The left vertical copper pillar, accepted-in-principle horizontal L-shaped header, and all raised brick-course/mortar geometry from V3.16 are **source-identical**. There is still no right-hand pillar.

Generated models change only within Y=2, in up to four rotations of the left facade plus a couple of internal contacting neighbor mesh variants due to face culling. No Y=3 roof/chimney, Y0/Y1, opposite block or front vent surfaces, collision profile, JSON structure, process, animation or recipe changes. Static checks enforce visibly recessed upper stone, four-edge bracket rails, copper-over-iron lower support, native UVs and no coplanar rendered faces. **In-game visual approval and `./gradlew build` remain pending.**

### 2026-10-24 — V3.18: FIRST upper center-grille block (one-block visual candidate)

After the user moved on from the front-left fitting, the next target is **the single front-center Y=2 brick controller-side piece**, `part_0_2_0.json`, using the newly supplied side-by-side screenshots. The old in-game part showed freely exposed grille bars deep in a large empty opening, no solid copper faceplate, and no reference-style copper supports or stepped arch below. The target picture instead has a **five-slot recessed copper vent with dark metal end brackets**, brick under it, two vertical copper structural ribs, dark lower feet, and a short stepped copper arch at its lower edge.

V3.18 replaces ONLY the center-front Y2 authored cuboids. The construction block itself is unchanged. The retained material language is vanilla bricks, `minecraft:block/cut_copper`, Retort soot, foundation-dark insets and existing riveted iron; no new or stretched texture asset.

**SOUTH-facing 16px model specifics:** structural brick core to Z9; solid lower brick masonry ends at Z15 through Y10.5; two solid outer brick sides surround an upper vent from X3.1..12.9. The soot backing is at **Z13.75**, so all **five actual 1.25px-wide open slots** are visible with depth instead of a pitch-black hole. The copper faceplate runs to Z16: top copper lintel Y14.1..16, bottom sill Y10.5..11.3, side reveals X2.5..4 and X12.45..13.5, with **four 0.55px copper dividers** between Y11.3..14.1 (slots near X4..12.45). Each end bracket has a raised four-rail iron edge and an actual dark recessed boss terminating at Z15.65 (two total).

The two vertical copper supports are at X1.7..2.8 and X13.2..14.3, Y2.3..11.2, with separate dark iron feet. The lower arch consists of a low bridge and two stepped raised shoulders, all built inside the center Y2 cell at Y0.7..3.2; lower Y1 item ports and hatch remain physically untouched.

Generated visual-model files: **20 total** across four facing orientations (four edited center Y2 models and 16 neighboring seam/culling outputs). Exterior front faces of the adjacent left/right upper blocks, lower Y1 service block, and upper Y3 copper roof block stay identical in each facing. Side/rear chamber, chimney, controller, recipes, formation JSON, item ports, animations, heat and power, and the retort voxel collision profile remain unchanged.

Static validation: four orientations, zero coplanar overlapping faces, no stretched UVs, four copper bars + **five individually sampled visible soot slots** per orientation, two dark metal bosses, paired lower vertical supports and the copper arch. **No Forge `./gradlew build` or in-game confirmation has been performed.** V3.18 is a visual candidate, not an accepted design. Work on the center block only until the user accepts it.

### 2026-10-24 — V3.19: join the upper copper supports and curve the lower apron

The user reviewed the in-game V3.18 front-center Y2 block and marked two concrete defects:
1. The two **vertical copper supports stop below their dark iron vent mounting brackets**, leaving visible gaps between parts intended to form one connected structural assembly.
2. The **bottom copper detail is a flat stepped tray**, not the curved/arched trim over the lower hatch shown in the reference.

**Exact SOUTH-local geometry change (all in the existing one 16×16×16 block):**
- Raise the top of the LEFT copper rib `[1.7,2.3,14.5] .. [2.8,11.2,16]` to `[1.7,2.3,14.5] .. [2.8,11.5,16]`.
- Raise the RIGHT copper rib `[13.2,2.3,14.5] .. [14.3,11.2,16]` to `[13.2,2.3,14.5] .. [14.3,11.5,16]`.
- The iron bracket's bottom rails already begin at **Y=11.5**, so the copper directly meets the metal with overlapping horizontal footprints. No overlay, disconnected cap, or added material is necessary.
- Remove the four legacy copper apron boxes and use **seven symmetric stepped arc segments** with X start/end and Y bottom/top:
  `[2.8,4.0;0.90,2.65]`, `[4.0,5.2;1.25,3.0]`, `[5.2,6.4;1.70,3.40]`, `[6.4,9.6;2.10,3.85]`, then mirror to `[9.6,10.8;1.70,3.40]`, `[10.8,12.0;1.25,3.0]`, `[12.0,13.2;0.90,2.65]`. All segments extend from local Z=14.75 to Z=16. Their contiguous x edges and overlapping Y spans create a connected, blocky copper arch with BOTH contours rising toward the center. It remains above the existing Y1 hatch, which is not rebuilt.
- Retain five copper-framed recessed soot vent slots, four copper mullions, both dark iron mounting insets, brickwork, and the iron feet. Nothing in the front-left or front-right part is authored or restyled.

**Generator scope:** four facing-specific models of the *one* center Y2 block, plus two lower Y1 center models regenerated for internal-face partitioning only. All outward faces of those two Y1 models are unchanged. Y3 roof/chimney, remaining Y2 and lower models, port skins, formation JSON, recipes, Voxel collision and processing remain byte-identical.

**Status:** V3.19 static geometry candidate, pending in-game acceptance. Checks cover direct iron-copper connection, seven contiguous symmetric curved pieces, unchanged five recessed slots, no coplanar overlapping faces and proportional UV mappings in all four orientations. A full Forge `./gradlew build` and in-game appearance test are not claimed.

### 2026-10-24 — V3.20: continuous left → center → right copper header (visual candidate)

**Issue shown by the user:** V3.19 connected the two center supports vertically to the grille mountings and curved the lower arch, but did **not connect the front-left and front-right Y2 chamber blocks horizontally** across the center. The copper lintel on the left ended at its block boundary, the center copper faceplate ended before the center's own block edges, and the front-right block had no matching copper header. The result was three isolated sections.

**This is a three-block visual-seam integration pass, not a new roof band.** The reference shows a copper structural header broken only by functional dark iron mounting plates. Do not resurrect the previously rejected dark full-width iron strip below the roof.

Precise authored SOUTH-facing Y2 details in local 0–16 model coordinates:

- **Left front (`part_-1_2_0`):** No authored cuboids changed. Its existing broad `cut_copper` header `[3,12.75,14.5] → [16,16,16]` already meets the x=16 seam, with the outer dark iron bracket at x0..4. Keep its V3.17 rivet, copper shoulder and accepted 3D masonry intact.
- **Center front (`part_0_2_0`):** Retain the existing grille/header `[2.5,14.1,14.5] → [13.5,16,16]`, the five soot-backed slots and all dark metal faceplates. Add **two copper seam returns**, `[0,14.1,14.5] → [0.6,16,16]` and `[15.4,14.1,14.5] → [16,16,16]`. The dark iron inset mountings at x0.6..2.5 and x13.5..15.4 remain visible and function as intentional structural interruption of the copper header; do not paint across them or close the vent.
- **Right front (`part_1_2_0`):** Split the old brick block from a solid visible midsection at local Y1..15 into lower brick `[0,1,0] → [16,14.1,16]` and concealed support `[0,14.1,0] → [16,15,14.5]`. Keep the top `[0,15,0] → [16,16,13]`. Add a matching **cut-copper** upper band `[0,14.1,14.5] → [13,16,16]`, which joins the center at x0 and terminates against the existing dark iron outer corner at x13..16. The original Y0..1 foundation contact is unchanged. The copper band itself occupies the same visual system as the center grille lintel, NOT a separate dark metal under-roof ribbon.

Each of these edits rotates into SOUTH/WEST/NORTH/EAST automatically. The source only adds center/right details, because the left already touches its seam. The generator regenerates **12 Y2 models and eight roof Y3 hidden-face meshes** (20 model assets); the accepted left block's outward face, roof top/grooves, and all Y3 non-bottom visible geometry are unchanged. All Y0/Y1 service blocks, ports, multiblock formation blueprint, recipes, item/water/heat/power processing, chimney and voxel collision remain untouched.

**Static QA:** all four facings were checked over 192 interior samples along the full 48px front upper-header line (Y15.7, local front depth Z16). Every sample resolves to `#bronze` or `#metal`; both inter-block seams have no holes. UV sizes remain proportional and no coplanar overlapping faces were detected. The existing five-slot grille and curved V3.19 arch must remain unchanged; the test contract includes these. This is **pending in-game comparison** and no full Forge `./gradlew build` was executed here.

### 2026-10-24 — V3.21: wider center supports and real 3D center masonry

User supplied a frontal in-game screenshot after V3.20 and marked the **two upper iron vent mountings**, **vertical copper braces** and **dark iron lower feet** as too narrow; the center section also lost the physical masonry depth already established on the front-left block. The user requested an actual implementation as well as a concrete build specification.

**Precisely one authored physical cell:** front-center upper chamber `SOUTH x=0,y=2,z=0`, automatically rotated to other facings. Only the center's visible design changes. Three outer-front blocks remain mechanically and structurally identical; the accepted roof, chimney and left-front 3D masonry remain unchanged.

SOUTH-local implementation:
- Under the center front vent: replace the flat 16×10.5 brick face (to Z15) with structural brick ending at **Z14**, plus two narrow solid brick edge returns to Z15; these keep the wall solid at the perimeter, not a deep alcove.
- Recessed mortar `[3.15,3.9,14]→[12.85,10.45,14.2]`, `minecraft:block/stone` scoped ONLY to the center and the original front-left brick cells.
- Nine physically raised individual bricks (three staggered courses):
  - Y4.05–5.85, X [3.25,6.1], [6.5,9.25], [9.65,12.75]
  - Y6.2–8.1, X [3.25,4.55], [4.95,8.25], [8.65,12.75]
  - Y8.45–10.25, X [3.25,6.9], [7.3,10.05], [10.45,12.75].
  - Rear starts at Z14.2; brick faces terminate at Z15.45 or Z15.70 for **1.25/1.50px of visible depth** over mortar. Front copper ends at Z16. Physical gaps between bricks expose recessed mortar and preserve natural side shadows.
- Both upper dark iron grille mountings widen from 1.9 to **2.5px**, at X0.25..2.75 and X13.25..15.75. Their raised four-sided iron frames preserve visible dark stone inset bosses Z15.65. The existing five recessed soot slots and all four 0.55px copper dividers are untouched. Grille lintel/sill and side copper casing slightly narrow at their edges to avoid covering the wider brackets.
- Two continuous vertical copper uprights widen 1.1→**1.9px**, to X1.05..2.95 and X13.05..14.95 (Y2.3..11.5, Z14.5..16). Their tops continue to meet the dark iron bracket bottom rails at Y11.5.
- Two lower iron mounting feet widen 1.5→**2.25px**, to X0.95..3.2 and X12.8..15.05 (Y0..2.4, Z14.5..16). Some exposed front faces are naturally occluded by the existing arch; don't count mesh fragments to measure physical widths.
- The V3.20 copper header still touches both neighboring front blocks continuously. Its center seam returns are now X0..0.25 and X15.75..16, adjoining the new 2.5px iron plates; center lintel is X2.75..13.25. At Y15.7 the continuous copper/iron front remains at Z16 without air gaps.
- The **seven-segment V3.19 curved copper arch is unchanged**. All accepted roof top, grooved copper roof, chimney, left and right facades, real block ports, multiblock JSON, recipes, processing and voxel collision remain untouched.

**Generated assets:** four Y2 center models (one per facing) and four Y1 center contact models re-partitioned for internal face culling. No other generated models changed. Verified front-visible lower Y1 model surfaces must match the previous commit. Static checks: V3.18 five soot slots, V3.19 seven arch segments/iron-copper contact, V3.20 48px three-block header continuity and V3.21 masonry depth/wider trim pass in all four facings. No global coplanar overlaps, UV stretching, or model-cuboid bounds errors.

**Status: visual candidate awaiting user in-game sign-off, not accepted.** Full `./gradlew build` and Minecraft in-game rendering have not been executed in this workflow. Do not expand scope to other front blocks or layers until the user approves.

### 2026-10-24 — V3.22: front-right upper masonry and fittings (await in-game approval)

The user supplied two screenshots: the current in-game front-right upper block looked like a flat/recessed framed panel; the reference is a **solid raised-brick wall** with one **upper-right recessed square dark iron bracket** and a **lower-right copper shoulder** above a dark-iron foot. The front-center and left-center work had already established real raised bricks and shallow mortar; the right must use that same visual language, not another deep alcove or symmetrical U-shaped front frame.

**Authored SOUTH-local Y2 cell:** `x=1,y=2,z=0`; only this right-front block's design is edited, then rotated to the other three facings.

- Structural brick core `[0,0,0]→[16,16,14]`. Recessed exposed mortar `[0,2.5,14]→[13,14.1,14.2]`, texture `minecraft:block/stone` scoped to physical front blocks.
- Three staggered rows of actual brick solids, Z14.2 backing and Z15.45 / Z15.70 face depth: Y3–6.55 X0.25–7.65 and 8.1–12.75; Y7.05–10.6 X0.25–5.4, 5.85–10.1 and 10.55–12.75; Y11.1–13.8 X0.25–7.35 and 7.8–12.75. Joints expose the mortar behind and cast genuine tiny block-face shadows.
- Retain the V3.20 copper structural header `[0,14.1,14.5]→[13,16,16]`. It must join the center directly at x0 and terminate under the outer iron corner at x13, rather than becoming a separate dark horizontal band.
- Single strong copper RIGHT outer pillar `[13,3,13]→[16,13,16]` meets the existing outer side-wall boundary. No new copper post or broad foot along the **inner x=0 edge** toward the center grille.
- Small protruding copper right shoulder `[12,1.25,14]→[16,4.5,16]`, plus narrow step `[12.5,5,14.5]→[15.5,6,16]`. Dark iron base under it `[12,0,14]→[16,1.25,16]`, not another 3px-tall all-metal block.
- Structural iron roof/base contacts continue behind the front finish (to Z15.25 / Z15), keeping corner support into the side and roof without obscuring the fitting.
- Top-right square iron bracket X12–16 Y12–16: rear plate Z14–15.25; four raised iron perimeter bars to Z16, thinner riveted iron inset rails, dark recessed central 2×2 square `[13,13,15.25]→[15,15,15.65]`. This is intentionally **not** a flat solid metal cube.
- The previous shared generic right-front decor (full-height inner pilaster, long copper bottom sill and solid corner metal overlays) is bypassed **only for this one front-right cell**, since it would obscure the new details. All side/rear block generator paths remain unchanged.

**Generated visual assets:** four rotations of the authored right Y2 part; four center Y2 shared-face meshes and four right roof Y3 underside meshes regenerate due to automatic face culling. The center's outward vent/grille faces are identical to V3.21; no roof top/sides/front appearance changes. Existing left front, all Y0/Y1 blocks, other side/rear wall models, actual item ports, roof/chimney, recipes, multiblock assembly, power/heat logic and voxel collision remain unchanged.

Static validation: all rotations have raised bricks (Z15.45/Z15.70), visible recessed mortar (Z14.2), an outer copper post, lower copper shoulder above dark iron foot, and distinct top-right dark inset at Z15.65; V3.18–V3.21 grille/arch/header tests still pass. Across all generated meshes no out-of-bounds models, UV scaling faults or coplanar faces were detected. **Minecraft in-game visual approval and a full `./gradlew build` are still outstanding**; do not mark the retort art as final without the user checking the right block.

### 2026-10-24 — V3.23: physically coursed upper RIGHT side wall (visual candidate)

User compared the reference Retort right-hand side (brick masonry extending past the front-right corner fitting) with Minecraft's current side showing **flat panels and heavy dark horizontal bands above and below the Y2 wall**. The reference has raised individual brick faces, relatively fine recessed mortar joints and no broad metal stripes interrupting the two-block chamber side. Scope is intentionally **only the two right-hand side upper Y2 cells** (`SOUTH: x=1,z=-1` and `x=1,z=-2`), not the opposite wall or rear production/hopper blocks.

**Design, SOUTH coordinates (0..16 model pixels):** side outward is **+X**, unlike the front facade's +Z. Both side cells retain a SOLID internal refractory brick core `[0,0,0]→[14,16,16]`. At exposed +X, recessed mortar is `[14,0,0]→[14.2,16,16]`, `minecraft:block/stone`. The raised individual side bricks occupy X14.2..15.45 or 15.70 (1.25–1.50px physical relief over mortar), with two independently staggered five-course Z patterns. Rows are local Y0.25–2.8, 3.25–5.85, 6.3–8.9, 9.35–11.95, 12.4–15.75, with ~0.45px horizontal and vertical mortar gaps. The middle and rear blocks use different Z brick spans to avoid an obvious repeated pattern across the seam. The side brick faces use a separate `sideBrick` material key mapped to `minecraft:block/bricks` so only their visible course faces change; the core still uses the original `magneticraft2:block/charcoal_retort/refractory` material, keeping its rear exterior material intact.

**What is removed:** the shared generic Y2 side decorations (full side-wall dark-iron bands at local Y0..3 and Y13..16 spanning the full Z length). They were the source of the prominent horizontal blue-grey strips in the user's Minecraft screenshot. Only the **right middle and right rear side cells** bypass those generic steel bands, not the front corner, opposite side, ports, lower floor or roof.

**Rear corner retained:** The rear-facing right edge (`SOUTH x=1,z=-2`) keeps its copper reinforcing upright `[13,3,0]→[16,14,2]`; the rear-facing upper and lower dark iron caps are retained as short 0.5px Z returns `[13,14,0]→[16,16,0.5]` and `[13,0,0]→[16,3,0.5]`. This preserves the old rear face's material area mix exactly (refractory 208px², dark iron 15px², copper 33px²), without continuing wide iron ribbons across the side.

**Generated scope:** eight rotated versions of the TWO authored Y2 right side cells, plus four Y2 front-right corner internal-contact meshes, eight lower Y1 contact meshes and eight roof Y3 underside models = **28 total generated model files**. Neighboring Y1 side outward faces, the front-right Y2 outward face and all Y3 roof outward/non-bottom faces remain unchanged. No opposite/left wall, front grille, center arch, chimney model, multiblock JSON, collision geometry, recipes, water/heat/power logic or actual port assets change.

**Static regression checks:** five raised rows on both right side cells in all four rotations, alternate 15.45/15.70px brick depths, exposed mortar at 14.2px, no long steel bands, rear copper attachment retained; V3.18–V3.22 front facade regression checks remain valid. All model faces checked for bounds, proportionally mapped UV and coplanar overlaps. **Minecraft side-view approval and `./gradlew build` have not been performed**; do NOT copy to the left wall or mark the machine visually complete until the user compares this side to the reference.
