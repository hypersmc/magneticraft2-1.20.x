# Bronze IV — Bloomery Furnace: K0 design and K1 implementation contract

**Status: K1 source candidate (2026-11-02). Recipe, controller, first Magnetite processing route, four-facing multiblock, basic V2 model/voxel assets, JEI and Patchouli have been authored. Forge compilation, in-game processing/rendering, multiplayer and full regression remain UNVERIFIED.** This is a proposed machine, not a formed multiblock. Track implementation in [Bronze production plan](bronze-age-production-plan.md), which remains canonical. [Concept footprint diagram](concepts/bloomery-k0-layout.svg) is an editable reference, not an accepted blueprint. A separate rendered concept image was generated in the design conversation.

## Role and age boundary

The Bloomery performs **direct reduction of prepared iron ore** using charcoal, a carbonate flux, refractory lining and sustained combustion air. Outputs are **iron bloom + slag**. The bloom is porous, slag-bearing metal; it is **not** an Iron Ingot and must subsequently be **hot consolidated/forged into wrought iron**. First usable wrought iron, not merely the first bloom, ends Bronze Age. Do not add an ordinary furnace recipe that bypasses this. Existing vanilla iron smelting behavior stays intact in K1 until its modpack compatibility is explicitly settled.

## First material route — one mineral, fully connected

Use existing `magneticraft2:magnetite_ore` and its Deepslate variant without renaming blocks. Proposed K1 chain:

```text
Magnetite Ore
  -> Grinder or Stamp Mill -> Crushed Magnetite
  -> existing Ore Washer (water -> dirty water) -> Washed Magnetite
  -> existing Sifter -> Magnetite Concentrate
  -> charcoal + Calcite flux + continuous AIR + real HEAT in Bloomery
  -> Iron Bloom + Slag
  -> K2: hot consolidation -> Wrought Iron
```

Do **not** introduce Hematite, Limonite and Galena chains in K1. Do not duplicate a new Crusher/Washer/Sifter. The candidate first batch is **4 Magnetite Concentrate + 1 `minecraft:calcite` + charcoal fuel** to yield **1 Iron Bloom + 1 guaranteed Slag**. Numerical recipe counts, time and heat cost are preliminary balancing values, not established final iron chemistry. Charcoal is an actual consumed fuel, not a decorative ingredient. The entire Magnetite path must become playable in the same K1 slice, rather than adding unused intermediate items.

The shared `multiblock_processing` serializer supports **one item ingredient**, not both concentrate and Calcite. A Bloomery-specific datapack recipe, with explicit input + flux counts and proper client sync/JEI, is preferable to altering existing machine semantics; a real prepared/fluxed charge stage is only an alternative if it becomes a meaningful player-visible operation. Do not hide flux costs in Java or allow the machine to process with no flux.

## Physical structure: proposed 5 × 5 × 5, four facings

A stout **refractory-lined vertical shaft**, stone/brick outer shell, narrow service/charging opening, low **bloom and slag tapping region** and open top/chimney. External wood support is acceptable only away from the hot core. No air duct, wooden shaft, steel pipe or bronze part floats in the furnace or crosses the hot chamber. Respect real registered blocks and the existing `fired_refractory_ceramic` progression; first verify whether that name is an item or a placeable block. Never invent a replacement block solely for model convenience.

Use the existing JSON multiblock architecture, genuine item-input / primary-output / byproduct-output modules and V2 voxel/render generation; keep all four rotation layouts physically equivalent. Provide a distinct, visible, **side-facing refractory tuyere**, accessible *outside* the hot chamber. Air arrives via a Bronze Air Duct or a direct, side-correct blower connection, not a controller capability visible through solid masonry. Model exterior duct-to-tuyere connection; do not route a cold duct through the ~hot reaction chamber. Share baked static geometry and preserve native UV density without stretching, clipping or unexplained intersections.

## Functional K1 contract

1. **Server-authoritative formation and ports.** An incomplete/unformed Bloomery never processes or accepts free gas. The tuyere receives only AIR from the Pneumatics network; it cannot accept STEAM or water. For compatibility, a sided legacy `CapabilityPressure.PRESSURE` input may bridge existing air charges until all sources are typed.
2. **Real charcoal/HEAT dependency.** Consume charcoal through an actual fuel burn cycle and existing HEAT API. K1 HEAT units are not equivalent to Celsius. Meet a configurable process threshold, pay real heat cost per progress tick and cool when idle.
3. **Sustained air requirement.** Do not advance without recurring air delivery. The current Industrial Blower produces 1 abstract air unit every 5 powered ticks; prototype consumption around that cadence, with bounded receiving buffer. Air is not mechanical torque or a magical source of HEAT. A stopped blower must eventually stop progress.
4. **Inputs and byproducts.** Require appropriate concentrate, flux and solid fuel. Check **both** output slots for available space before any batch commits. Slag output is guaranteed, not a random chance. Avoid partial consumption, racey insertion, dropped outputs and duplication.
5. **Persistence and stalls.** Save fuel, current burn, heat, air buffer, selected recipe ID, input/flux identity and progress. Output full, missing flux, broken duct, no heat, absent recipe or dismantled structure must pause/reset safely. If a load changes mid-process, no remaining progress is allowed to turn it into free new output. On break, drop all retained inventory exactly once.
6. **Automation and visuals.** Genuine physical input, bloom output and slag output must work with belts, arms and hoppers; no unsided controller inventory shortcut. Add JEI machine catalyst and Patchouli multiblock instructions for every facing. Visually show hot vs cold, draft vs idle, output/slag blockage without heavy per-frame allocations.

## Staged delivery — no false completion

- **K0 (prior):** authored gameplay/physical contract and editable footprint reference.
- **K1 implementation candidate:** one Magnetite preparation and Bloomery reduction path is now source-authored, including real charcoal+flux+air+heat gating, guaranteed bloom+slag, JEI and book. Shared/reused block textures and item icons are placeholders requiring artistic revision; no Gradle or Minecraft acceptance has passed.
- **K1 verification:** `./gradlew build` on JDK 17 / Forge 1.20.1, contract checks, one successful real in-game batch and all failure/regression scenarios below. A code change alone is not verification.
- **K2 and Bronze exit:** hot bloom consolidation into wrought iron; later ore-family expansion, quality/yield integration and properly scoped replaceable-part wear. Do not call the Iron Age unlocked on bloom alone.

## Mandatory verification checklist (unchecked)

- [ ] JDK 17 `./gradlew build`; datapack JSON, recipe codec and four orientations validated.
- [ ] All Magnetite steps run, consume water and report correct byproducts/JEI; preserve old Copper and other Bronze ore recipes.
- [ ] Form and break correctly in four facings, module item ports face and expose real inventories, book layout matches in-world structure.
- [ ] Air duct direct/bent/branch paths to the actual formed tuyere; no AIR/STEAM/water crossover, chunk loading or per-tick graph explosion.
- [ ] Interrupt AIR, charcoal, heat, flux and concentrate **separately**. Each correctly prevents progress. Test full bloom output and full slag output.
- [ ] Save/reload mid-batch, change input NBT, replace recipe datapack, dismantle/reform and break a port; no hidden/free inputs, duplicated drops or vanished slag.
- [ ] Inspect native texture UVs, model contact/bearings/chimney/tuyere, collision and performance from all camera angles.
- [ ] Regression: current Industrial Air Blower, Ore Roasting Kiln, Primitive Furnace bellows, Refractory Kiln, Assembly Station, Grinder, Washer, Sifter, Mechanical Power System.
- [ ] Dedicated server + two actual clients; compatibility with existing saves and vanilla iron recipes.

**PR #11 stays draft and unmerged until the existing milestones and this new work are truly verified.**

### Formation correction — 2026-11-02

The user rebuilt the Bloomery exactly according to Patchouli. The in-game diagnostic showed **four and only four mismatches** on the south-facing candidate, all four on the front ground-level service row: the displayed ports were mirrored against the JSON layout. The correct fix is to match the already-built guide reference by changing all four orientation-specific JSONs (no machinery or voxel geometry changes). The SOUTH `layer1` row at z=4 is now **`YOCUS`**, not `SUCOY`, while the book's authored pattern remains `SU0OY` because its visible left/right orientation is opposite to the raw structure x-index. The north layout rotates to `SUCOY`. The static contract now asserts both in-world port ordering and book matching with the proper visual transform. Successful formation and in-game rendering still require actual verification.

## K1 visual and side-air redesign — source change pending Minecraft verification

The first in-game model was an undifferentiated brick wall. The design now changes the same **5x5x5** multiblock to a broad masonry hearth, a front opening and lintel, and a **3x3 refractory upper shaft with an unobstructed chimney**. Existing vanilla and mod refractory textures are mapped at natural UV density and models are pre-baked from a small shared palette, avoiding per-frame geometry generation. Exterior timber trim belongs only to cool corner masonry; the hot shaft is masonry and iron. The three genuine item-port block entities stay functional, with new formed-only furnace skins.

The actual forced AIR intake is a copper-lined **side tuyere** built into the lower wall (canonical SOUTH x=0,y=1,z=2). Its cell is still an ordinary required Bricks block in the unformed JSON, then an existing formed multiblock filler exposes the existing controller's gas/legacy-air buffer only through that precise outward face. It forwards AIR to the same persisted storage; no new block ID, steam source or entity is created. The old controller-face port remains available temporarily for already-built air arrangements, but a new build should connect the Industrial Air Blower via a Bronze Air Duct to the side intake. The profile leaves the nozzle core genuinely hollow and adds a low hearth grate collision only at a dedicated required-AIR anchor.

Already-formed old Bloomery structures keep their old baked part references; **break and rebuild/reform** after upgrading to see the new 3x3 stepped shaft. Changed world blocks are an explicit visual/structure revision, not an automatic save migration. Actual Forge compilation, visual QA, AIR routing, chunk behavior and multiplayer remain unverified.

**Support-frame material correction:** The upper outer timber frame is not created from AIR: the JSON actually requires 8 `minecraft:spruce_log` corner cells and 12 `minecraft:spruce_planks` horizontal-beam cells, all physically supported by the lower masonry. On formation these real blocks are replaced by V2 proxies rendered as narrow posts and connected beams, preserving matching voxel collision. Only the front low hearth is a generated AIR anchor. The Patchouli blueprint lists both actual wood materials and the narrow open chimney, avoiding fictional wood appearing from nowhere.

### Z-fighting geometry correction (2026-11-02)

The initial stepped Bloomery model rendered overlapping opaque cuboids: e.g. wooden/metal trim intersected entire stone/refractory wall cuboids, and the controller and air-nozzle models had similar intersections. The fix does **not** offset coincident faces by arbitrary epsilons. Instead each repeated baked model is partitioned into non-overlapping axis-aligned regions (decorative parts take precedence and displace the masonry they occupy), with fragment UV ranges derived from the original element coordinates to retain one texture pixel per model pixel. On the front facade, masonry is intentionally set back **2 model pixels** within the normal 0..16 block bounds, leaving trimmed masonry piers/arch visibly raised and connected. The V2 collision footprints and existing save/block IDs remain unchanged. There is no new per-frame code or entity renderer.

Test added: `node tools/bronze-bloomery/test-model-overlaps.mjs` checks every shipped facing, the controller, all filler variants and formed port skins for overlapping volumes, same-direction coplanar faces, invalid boxes and UV stretching. This is static evidence only: real camera-angle/FPS inspection in Minecraft and `./gradlew build` are still required. Do not mark the model visually accepted until tested in game.

## New Bronze exit step — source authored 2026-11-02

The Bloomery still outputs ordinary Iron Bloom plus guaranteed Slag. The separate [Bronze-to-Iron acceptance contract](bronze-to-iron-acceptance.md) now specifies a genuinely hot Refractory Kiln bloom treatment followed by powered Forge Hammer consolidation to a new Wrought Iron Ingot. This builds on the existing furnace and forge without altering Bloomery chemistry, air input, item ports or visuals. Both recipes and the heat-state NBT have been authored; `./gradlew build`, full gameplay, save/reload and multiplayer acceptance are **pending**. The Bronze exit milestone is **not** verified or complete on source alone.
