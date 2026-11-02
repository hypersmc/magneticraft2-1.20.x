# Bronze Age industrial progression (Magneticraft2 1.20.1)

Status: **canonical 16-machine Bronze Age implementation roadmap**, reconciled against draft PR #11 on **2026-10-31**. The earlier historical milestone notes below are retained for context.  In the machine tables below, **source implemented** means the code/resources exist on the draft branch, not that the full Forge build and in-game acceptance are complete. **Planned** means not yet implemented. Commit new content incrementally and preserve Copper-era functionality. Target progression: **Copper workshop -> Bronze industrial factory -> first iron bloom -> Iron Age**.

## Current development direction — 2026-11-02

**Bronze remains the active age.** The Industrial Air Blower is **done for now as a scope decision**, not a claim that full Gradle/in-game verification has passed. Do not reopen its design or introduce more Steam Age machinery in this slice. Steam transport/rupture infrastructure on PR #11 is parked until the Steam Age workstream; it remains unverified and is not part of the Bronze gameplay acceptance milestone.

**Bronze III milestone:** Mechanical Assembly Station is K1 source-implemented (four ingredient slots, separate output and actual Mechanical Power load) but Forge, gameplay, visual and multiplayer verification remain pending. The next active design/implementation workstream is the [Bloomery contract](bronze-bloomery-contract.md). Start with [its functional and integration contract](bronze-mechanical-assembly-station-contract.md) rather than a decorative block. The first **candidate** automated assembly produces the **existing Mechanical Water Pump** from the same 2 planks + Bronze Bushing + Wooden Shaft + Small Wooden Pulley as its existing shaped recipe. Manual crafting remains. The source implementation uses a separate `mechanical_assembly` serializer and a genuine four-slot input port, distinct from the genuine primary output. The first pump batch has provisional 200-tick, 18-RPM, 5-torque settings. Gradle/in-game testing and visual acceptance remain outstanding. No additional ore, wear, air or steam scope was introduced.

**Sequence:** verify Assembly Station -> implement Bloomery K1 with one complete Magnetite preparation/reduction route -> verify air, heat, flux, outputs and gameplay -> consolidate hot bloom into wrought iron -> extend the wider ore-by-ore progression. K1 source availability is not in-game acceptance. The Refractory Kiln is already **K2 source-implemented** with a provisional clay/sand batch firing loop (see its current contract), not just a K1 structural mock-up. Build and end-to-end gameplay verification remain pending. Preserve the existing progression quality and maintenance decisions, including that not everything should wear out or need constant upkeep.

## Non-negotiable design rules

1. Every formed machine has a concrete purpose: new material transformation, meaningful throughput/quality upgrade, or a connection between established processes. Avoid adding machines solely for item-gating.
2. The existing Mechanical Grinder, Ore Washer and Sifter remain useful. The Stamp Mill is a higher-throughput ore breaker, not a mandatory replacement for the grinder. The Washer concentrates crushed ore with water, and the Sifter classifies washed material. Keep these independent, and do not duplicate their conversions.
3. Use the established JSON multiblock and V2 render/voxel pipelines with physical module blocks for Gear V2 input, item and fluid ports; form from a proper Patchouli multiblock guide with four orientations. Models/UVs must be optimized, native-tiled, not stretched, and avoid overlapping faces. BER only for moving parts.
4. Gear V2 demand must be consumed while processing, released when idle/stalled/broken, and never freeze an upstream machine on failure. Use a consistent load/state protocol. Reserve recipe outputs before consuming input; save unfinished work and don't duplicate items on pause/chunk reload.
5. Recipes use `magneticraft2:multiblock_processing` with machine-specific `machine` IDs, JEI catalyst, book instructions, and datapack-editable operations. Distinguish item/fluid processes from purely visual approximations. Never change the existing Copper chain silently.
6. Respect agreed ingot/material quality levels, including Minecraft Copper quality and lower-grade mod-derived Copper. Add traceable input quality propagation and ore impurity later; don't add multiple incompatible quality systems. Design wear parts as visible, replaceable components whose remaining life persists and wears per completed operation.
7. Preserve mass/material accounting and provide uses or recycling routes for dirty water, tailings, slag, casting sand and metal chips. No infinite yield loops from recycling. Explicitly define input/output for every recipe.
8. Keep Stone/Copper tools useful as fallbacks, with Bronze unlocking scale/precision and not instant free multipliers. Iron Age upgrades machine components and process capability instead of replacing every machine.
9. Treat 3x3x3 only as a convenience, not a footprint cap. Use bigger multiblocks when their purpose and internal mechanical motion warrant it.
10. Do not call work compiled without `./gradlew build`. Test models, blueprints, collision, all facings, JEI, recipe conservation and integrated automation in-game.

## Release sequence and dependencies

| Phase | Role | Production milestones and first verification |
|---|---|---|
| **Bronze I — Casting and machine tools** | *Implemented in PR #11; Minecraft/Gradle validation pending* | Complete and stabilize Pattern Maker -> Mold Table -> Foundry -> Casting Basin -> precision Lathe; bronze bearings/gears, input modules, visible tooling and wear. Prove an uninterrupted and a stalled machining batch; compare ladle and direct-pour losses. |
| **Bronze II — Ore dressing and industrial fuel** | *Stamp Mill, optional Rotary Ore Classifier, Water Settling Tank and Charcoal Retort source implemented on PR #11; build/end-to-end verification pending* | Stamp Mill -> optional classifier -> Washer/Sifter -> Settling Tank. Charcoal Retort produces charcoal and liquid Wood Tar, with a dedicated fluid output. Settling Tank V5.1 visual baseline accepted. Selective Ore Roasting Kiln K2 source candidate adds 4 Copper Concentrate -> 4 Roasted Copper Concentrate, fuel/HEAT and a downstream Primitive Furnace recipe; build and in-game verification pending. Test fuel, item/fluid backpressure, reload, and the full ore/water circuit before marking this phase complete. |
| **Bronze III — Manufacturing and networks** | *Forge Hammer and Mechanical Press basic operation user-confirmed; Rolling Mill powered processing not explicitly confirmed; Boring Machine K1 source candidate; regressions pending* | Refractory kiln, forge hammer, rolling mill, mechanical press, boring machine, sawmill, mold-sand mixer, assembly station. Introduce larger line shafts, mechanical item distribution, lubrication, maintenance and meaningful quality differences. |
| **Bronze IV — Iron preparation** | *K1 Bloomery source candidate, unverified in Minecraft* | Iron ore concentration, controlled reduction/smelting, flux and charcoal, high-temperature refractories, mechanical air blast, bloomery furnace. First consolidated Wrought Iron after Bloomery reduction completes the Bronze-to-Iron milestone; an unfinished bloom alone does not. |

## Agreed development order — finish machines, then integrate full ore progression (2026-11-01)

**Decision:** complete the planned multiblocks and their first functioning gameplay
loops before the broader **ore-by-ore progression pass**. The current
Copper-focused recipes (including the Ore Roasting Kiln and Bronze III
manufacturing loop) are baseline integration tests, **not** the final
material/ore catalog. This is a sequencing decision, not permission to
introduce new ores or redesign working machinery immediately.

**Pass A — multiblock foundations first:** each planned machine must be
formable with real JSON modules, have a useful functional recipe, existing
Gear V2/HEAT/pressure connections where appropriate, working inventory/fluid
I/O, persistence and a preliminary in-game model. Preserve a separate
Magneticraft2 Studio visual reference for future art refinement. Verify
individual milestones as they are built; deferring mineral breadth does
**not** defer compilation, correctness, or essential regression checks.

**Pass B — full multi-ore/material progression afterward:** use the
existing [ore-processing-by-age matrix](ore-processing-by-age.md), ore
catalog, and established Stone/Copper/Bronze/Iron progression to map each
ore and mineral to appropriate *existing* operations. Expand the data-driven
recipes for crushing, screening, washing, sifting, selective roasting,
smelting/reduction, casting, forging, rolling and forming **only where the
material's properties and machine purpose make sense**. A machine does
not need to process every ore, and roasting must not become a universal
mandatory step. The ore-specific path should have a real downstream
consumer rather than dead-end intermediate items.

During that integration pass, reconcile the previously agreed **material
quality/ingot grades**, impurities, yields, fuel/HEAT/air requirements,
machine-part wear and mass-conserving byproducts (tailings, dirty water,
slag and metal scraps). Reuse rather than replace the existing
Chalcocite/Cassiterite/Copper processing. Validate complete chains in
survival, item/fluid automation, JEI/Patchouli and save/reload, including
compatibility with vanilla resources, before calling the progression
release-ready.

**Important separation:** *machine K1 works* and *ore/material progression
is complete* are different milestones. Do not silently label the latter
complete merely because a machine processes one copper test recipe.
The exact recipes, mineral-specific routes, numerical balance and rollout
order of Pass B are **not agreed yet**; define and test them after the
multiblock foundations, without inventing a universal ore pipeline.

## Multiblock candidates (footprints preliminary)


**Status snapshot (2026-10-31):** reconciled with `feature/bronze-age-foundry` / draft PR #11 at commit `ccbae12933ca`. The earlier screenshot/table remains the scope baseline (16 machines), but implementation status reflects later commits. **Source implemented** = machine logic/assets exist on that branch, **not merged or certified by `./gradlew build` and end-to-end Minecraft tests**. **Visuals accepted** = appearance only, not a sign-off on mechanics, I/O, persistence, automation or performance. **Planned** = no corresponding implementation on this branch. **Optional** = discretionary and not a prerequisite. Footprints remain preliminary except where an implemented machine's current layout is stated.

| Machine | Implementation status | Suggested footprint | Input -> output / distinct role | Dependency |
|---|---|---|---|---|
| Stamp Mill | **Source implemented** (PR #11; validation pending) | 5x3x4 | Raw ore -> crushed fraction, optional waste; large inertia and hammer wear | Bronze bearings, Gear V2 |
| Rotary Ore Classifier | **Source implemented** (PR #11; validation pending) | **3x3x5** | 3 screened fines + 1 reusable coarse crushed ore per 4 crushed (no tailings generation yet) | Stamp Mill |
| Ore Roasting Kiln | **K2 roasting source candidate** (fuel/HEAT and 4->4 concentrate batch; build/in-game verification pending; visuals frozen) | **5x8x9** | Copper Concentrate -> Roasted Copper Concentrate (provisional); smelt later for 4 Copper Ingots, old 3-ingot route retained | Refractory and fuel |
| Water Settling Tank | **Source implemented; V5.1 visuals accepted** (gameplay/build validation pending) | **5x2x5** | 600 mB dirty water -> 480 mB reusable clean water + 1 settled sediment per 240 ticks | Ore Washer |
| Charcoal Retort | **Source implemented; partial in-game fluid extraction confirmed by user** (PR #11; latest NBT/render fixes need retest) | **3x4x3** | 4 burnable logs + separately supplied solid fuel -> 4 charcoal + **250 mB liquid Wood Tar** (720 ticks); 8,000 mB internal output tank, legacy item retained for saves | Furnace; advanced airflow still planned |
| Refractory Kiln | **K1 structural prototype authored; build/gameplay verification pending** | **5x4x5 source footprint** | Prepared clay/sand/mineral batch -> fired refractory ceramics, using sustained HEAT from real fuel; no output recipe/ratios approved yet | Charcoal/solid fuel, existing HEAT system |
| Mechanical Forge Hammer | **K1 functional source candidate** (4 Copper Ingots -> 4 Copper Plates under Gear V2; wear; Gradle/gameplay validation pending) | **5x4x3** | Powered copper plate batches now; hot metal -> forged parts when thermal stock exists | Bronze power |
| Bronze Rolling Mill | **K1 source candidate** (4 Copper Plates -> 4 Copper Sheets; Gear V2; sleeves; Gradle/in-game tests pending) | 5x3x3 | Cold copper sheets now; hot billet -> bronze sheet/rod later | Precision bearings |
| Mechanical Press | **K1 basic gameplay user-confirmed (2026-11-01)** (4 Copper Sheets -> 4 Copper Panels; Gear V2; targeted regressions, Gradle and multiplayer outstanding) | **3x3x3** | Cold copper sheet -> useful stamped Copper Panel | Rolling Mill |
| Precision Boring Machine | **K1 source candidate** (rough bronze bearing casting -> Bronze Bushing; Gear V2, bit wear, NBT quality; Gradle/runtime pending) | **5x3x3** | Precision internal bore, distinct from Lathe external turning; optional pump crafting sink | Lathe toolmaking + Foundry |
| Mechanical Sawmill | **K1 source candidate (build/gameplay pending)** | **5x3x3** | 1 burnable log -> 3 precision-cut pattern stock (6 Pattern Maker patterns); 256-cut blade; Gear V2 | Precision bearings, Pattern Maker |
| Foundry Sand Mixer | **K1 source candidate (build/gameplay pending)** | **3x2x3** | Cooled casting -> 1 spent sand; 4 spent + 1 clay -> blend -> 3 reclaimed sand usable at Mold Table; Gear V2 | Foundry, Gear V2 |
| Industrial Air Blower + Bronze Air Duct | **K1 duct transport source candidate (build/gameplay pending)** | **Blower 3x3x3; one-block ducts** | Gear V2 16 RPM / 5 torque -> bounded six-way ducts (max 128) -> Ore Roaster/Primitive Bellows; optional Roaster +20% oxidation currently provisional; physical Bloomery inlet/requirements not yet agreed | Gear V2 + Pressure capability |
| Mechanical Assembly Station | **K1 source implemented; compilation, runtime, visual and multiplayer verification pending** | **5x3x5** | Existing 2 planks + Bronze Bushing + Wooden Shaft + Small Wooden Pulley -> Mechanical Water Pump; four true inputs, powered assembly, real output module | Bronze III |
| Mechanical Ore Breaker | **Optional — not implemented** | 3x3x3 | Oversized/irregular rock -> feed for Stamp Mill | Optional precrusher |
| Bloomery Furnace | **K1 source authored: four-facing multiblock, charcoal/HEAT/AIR, Magnetite+Calcite, Iron Bloom+Slag; build/gameplay/visual unverified** | **5x5x5 first layout** | Prepared Magnetite + Calcite flux + charcoal-fueled HEAT + sustained AIR -> Iron Bloom + Slag; hot consolidation later | Bronze IV |

Also **source implemented in draft PR #11**, but not multiblock *candidates* in this table: **Bronze Pattern Maker**, **Bronze Mold Table**, **Bronze Foundry**, **Bronze Casting Basin** and **Bronze Mechanical Lathe**. Their full build/in-game acceptance remains separate from source availability; see `bronze-age-foundry-roadmap.md`, `bronze-foundry-structure-and-casting-contract.md`, and `bronze-mechanical-lathe-contract.md`.

Footprints and intermediate chemical recipes for **unimplemented** machines are design proposals, not final build requirements. Real ores need individual paths; chalcocite, cassiterite, quartz and iron minerals must not all magically smelt identically.

## Initial, existing material chain

* Chalcocite Ore -> existing Grinder -> Crushed Chalcocite -> Washer -> Washed Chalcocite -> Sifter -> Copper Concentrate.
* Cassiterite Ore -> existing Grinder -> Crushed Cassiterite -> Washer -> Washed Cassiterite -> Sifter -> Tin Concentrate.
* Both existing chains are deliberately retained as the Bronze baseline. The new Stamp Mill should yield greater *throughput* or reliable size control rather than eliminating the processing steps.
* Bronze casting (Pattern Maker, Mold Table, Foundry, Casting Basin and Lathe) and industrial ore dressing (Stamp Mill, optional Rotary Ore Classifier and Water Settling Tank) are source implemented on the draft branch. The next engineering job is integrated gameplay verification and the Charcoal Retort's useful fuel/byproduct chain.

## Future iron handoff (not yet implemented)

Iron ore deposits -> selective breaking/classification -> concentration -> dewatering/optional roasting according to mineral -> refractory Bloomery + charcoal + suitable flux + Gear V2-blown air -> **iron bloom** -> hot forging/consolidation -> first usable iron. No ordinary furnace recipe should bypass this chain once the gating strategy has been tested and accepted. Existing vanilla iron recipes require a compatibility pass before gating; do **not** silently remove them.

## Acceptance definition for each multiblock

- Functional construction and clear non-GUI interactions, including valid and invalid builds for all orientations.
- A connected mechanical power path, working item/fluid port capability and clear machine status when blocked or worn.
- Persistence of internal inventory, partial progress, power-paused state, wear and pending byproducts; resources are conserved through unload/reload, break and automation.
- A V2 static model plus inexpensive synchronized moving components; no UV distortion, z-fighting, hidden-module gaps or floating workpieces.
- Crafting recipes, JEI recipes and catalyst, Patchouli crafting/multiblock chapters, translations and item/loot models.
- Structural/processing regression tests and verified `./gradlew build`, plus a repeatable in-game checklist.

## Work order

1. Commit roadmap and material-flow table.
2. **Stamp Mill source implemented:** six raw feedstocks, Gear V2 input, replaceable stamp shoe, physical item ports, JSON recipes and guide/JEI integration. In-game animation polish and a verified build are still outstanding.
3. **Still required before the next ore-processing multiblock:** verify ore -> crushed -> Washer -> Sifter end-to-end in-game, including block harvests, full output ports, dirty-water backpressure and automation. Do not label source-level checks as gameplay verification.
4. **Water Settling Tank source implemented:** 5x2x5; V5.1 visuals accepted. Verify actual 600 -> 480 mB + sediment operation, pipe/item ports, output blocking, reload persistence and integrated water reuse; no full Forge build or complete gameplay acceptance claimed.
5. **Charcoal Retort implemented as a source candidate:** sealed 3x4x3 multiblock with real liquid Wood Tar (250 mB per four-log batch), fuel, dedicated rear output capability, JSON recipe, JEI and Patchouli. The user confirmed fluid extraction; however the latest legacy-NBT crash fix and rear-port rendering still require retesting. Visual polishing remains deferred. The 250 mB output is a provisional balance value.
6. **Refractory Kiln is the next agreed new machine (Bronze III):** follow [`bronze-refractory-kiln-contract.md`](bronze-refractory-kiln-contract.md). Start with the material and thermal contract, then a JSON-formed 5x4x5 prototype and its full four-facing/boundary tests. Do not mark it source-implemented until code, materials, assets, and recipes actually exist. Do not alter vanilla iron or Stone/Copper progression in this slice.

## Pre-next-multiblock consistency sweep (2026-10-19)

The registered ore catalog and mining rewards are now fixed as part of PR #11:
all **42 ore blocks** have self-drop loot tables and Stone-pickaxe mining tags,
including Quartz and Cassiterite needed in earlier ages. The previously
untagged Stamp Mill/Lathe controllers are harvestable with Stone pickaxes too.
See [ore processing by age](ore-processing-by-age.md) for the 31-family matrix
and implementation states. This did not change ore worldgen or furnace yields.

The next substantial ore-processing multiblock should be chosen based on a
usable input/output chain, not just a new machine design. At this historical snapshot,
the Rotary Ore Classifier and Dirty Water Settling Tank were candidates; both
were subsequently source implemented in PR #11 (see the current status table).
Iron ore reduction still needs dedicated concentrates,
refractory technology, charcoal/flux and Bloomery design decisions. Do not
add dead-end iron crushed-items or smelting recipes before that path is agreed.


## 2026-10-19 — Rotary Ore Classifier implementation slice

The redesigned **3x3x5** JSON classifier is now source-implemented with 4-facing models using the first original Copper/Bronze texture kit, Gear V2-driven drum and replaceable screen. It consumes four crushed Chalcocite/Cassiterite/Quartz and produces three screened fines plus one original crushed coarse return; all material is preserved. Three fines wash in one 240-tick/600 mB Washer batch and then proceed to the existing Sifter. This optional throughput/water improvement leaves all plain crushed-ore recipes untouched. **Do not mark compiled or gameplay-verified** until the tests in [the classifier contract](bronze-rotary-classifier-contract.md) run successfully, including the previous prerequisite end-to-end Stamp->Washer->Sifter line. The dirty-water settling/reclamation machine is still the next planned utility; ore roasting is still selective metallurgy work.

### Classifier visual acceptance update (2026-10-19)

The first 5x3x5 prototype was rejected after in-game screenshots showed a forest of repeated posts, a tiny nearly invisible drum, an empty-looking body and unclear mechanical/ore connections. The replacement is **3x3x5** with four structural timber supports, a much larger three-section open bronze screen, an internal lower fines collection pan and two functional, exposed front output modules. All four blueprints, V2 geometry/physics, guide Visualize and drum models are regenerated together from the same design. Source/contract checks are pending after this revision; the prior prototype must be unformed before upgrading in a test world, because its physical build layout has changed. See [the classifier contract](bronze-rotary-classifier-contract.md).

### Classifier real-port clarity correction (2026-10-23)

Screenshots exposed a UI/mechanical affordance error: the classifier rendered an extra upper hopper and two wooden/bronze collection holders over already-interactable U/O/Y modules. These looked clickable but were mere V2 filler geometry. Port-clarity revision removes all 23 faux holder/spout model boxes, frees the AIR cell above actual Input, keeps only a short metal feed coupler and a noninteractive **internal** fines screen pan, and moves inventory previews onto their **actual module surfaces**. There are still two real output modules; processing/capabilities/recipes are unchanged. The changed formation blueprint (no former Cut Copper cap) requires re-forming the existing test machine. Tests reject future fake item-holder geometry and port occlusion.

### Rotary classifier structural-support pass (2026-10-23)

Real client screenshots showed unsupported intake and rear bearing geometry and a front drum ending beyond its rollers. The source was revised without changing the mechanical recipe contract or structure formation: rear bearings and the short hollow feed sleeve are anchored to their timber/stone support path; two front guide rollers now meet an **open-ended** rotating drum rim; small collection guides are attached to the pan and stop at the physical outputs. The generator now rejects disconnected static components and maintains the original U/I/O/Y module cells. Validation still requires `./gradlew build`, end-to-end automated operation and in-game visual/FPS comparison before PR #11 can be accepted.

### Classifier V5 exterior bearing supports (2026-10-23)

In-game screenshots revealed that the latest "grounded" V4 design used long rear metal struts across the open drum. The existing Gear V2 Mechanical Input module already renders its own central bearing/shaft, so those seven duplicate fixed bearing/mount pieces are being removed in favor of two short external roller supports on the rear hoop. Source-level classification and normal/worn drum animation remain unchanged, as do all 3×3×5 multiblock cells and ore/Washer/Sifter recipes. Before merging PR #11, confirm the central input bearing aligns with the drum spindle in-game and nothing clips through the rear or front open ends.


### 2026-10-23 — Water Settling Tank (draft PR #11, unverified)

The first Bronze II recycling utility is now source-implemented as a
passive 5×3×5 multiblock with two distinct Forge fluid storage tanks and
real fluid in/out plus item byproduct modules. It takes 600 mB Washer
Dirty Water -> 480 mB reusable clean Water + one settled sediment in 240 ticks.
Sediment can be combined 4:1 into Clay. There is no new Gear V2 load and
no invented bonus ore or infinite closed-loop yield. Outputs are reserved
before input is drained and unfinished progress survives blockage.

See [Water Settling Tank contract](water-settling-tank-contract.md).
The existing plan's "still planned" references document the earlier
milestone and are superseded for this specific machine. Gradle and
in-game visuals remain unverified; the next proposed industrial
multiblock is the Charcoal Retort.


### 2026-10-23 — Water Settling Tank visual redesign V2

User's in-game video rejected the previous large trough, opaque muddy
surface, narrow clean channel, generic wood supports and unintegrated ports.
The replacement keeps all processing amounts and stored inventory behavior,
but changes the model and four formation blueprints to equal side-by-side
dirty/clean bays, a raised ceramic-lined bronze divider, a silt-trap grate,
inlet and outlet fittings, a metal service bridge and four Cut Copper upper
supports. It introduces six new native-density 16x16 PNG textures, with
192 per-facing static cell models overall.

**Compatibility:** previously formed tanks must be rebuilt; Oak Planks are
no longer the four upper supports. This is still draft PR #11 with Gradle
compilation and real-client approval outstanding. See
[Water Settling Tank V2](water-settling-tank-contract.md).


### 2026-10-23 — Water Settling Tank V4 process-first redesign

The V3 screenshot still looked like a framed bathtub, with heavy upper bronze
posts obscuring the treatment process. V4 has replaced the entire static
structure (not another texture pass): low **5×2×5** open basin instead of
5×3×5, no third-layer Cut Copper posts or inspection walkway, clear large
rear dirty-water pool, transverse low overflow weir, short front clean-water
channel, and only restrained visible connections to real fluid modules.
The six V3 hand-painted textures and all actual 600→480 mB/240-tick
mechanics remain untouched. Four rotated formation blueprints, Patchouli
and Voxel V2 physics/collision were updated together; obsolete model files
were deleted. Existing V3 test builds require reforming. This is still
PR #11, draft, **not gameplay-accepted or Gradle-verified**.
See [V4 contract](water-settling-tank-contract.md).

### 2026-10-23 — Settling Tank V4.1 geometry correction

Follow-up screenshot exposed actual Z-fighting and water drawn partly
through masonry, not merely a question of visual style. V4.1 eliminates
all 12 coplanar same-facing overlay pairs and all solid cuboid overlaps,
replaces the muddy-looking empty floor with dark refractory masonry,
simplifies inlet/outlet fittings, and clips both fluid surfaces to the
real basin interior (no 0.28-block intersections with each side wall).
The 5×2×5 multiblock JSON, ports, recipes, water loss, processing and
hand-painted V3 tile resources remain unchanged. Model/collider
snapshots of already formed machines require a safe re-form to refresh
collision, but no block construction change. Compile/client testing pending.

### 2026-10-23 — Water Settling Tank V4.2 port integration

New screenshots showed the floor-mounted rear Fluid Input, front Sediment
Output and external Clean Water Output as sunken/disconnected objects.
The upper rim also had empty, mismatched rear corner joints.

V4.2 relocates the existing REAL modules to the 2nd-layer perimeter:
rear inlet (0,1,-4), front-left sediment (-1,1,0) and EAST
side clean-water outlet (2,1,-1), all seated on bronze-covered Stone Bricks.
The shared modules automatically orient the inlet NORTH and outlet EAST,
so both connect outward instead of facing into a neighboring wall.
The rear and front rim corners are modeled as connected returns,
without same-facing coplanar surfaces. The 5×2×5 footprint, V3
hand-painted textures, 600→480 mB recipe, tanks, Gear V2 non-requirement
and pump/pipe capability semantics are unchanged. Four rotated JSONs,
Voxel V2 collision, renderer sediment location, Patchouli projection,
generator and geometry regression contract are updated as a group.

This revision changes the actual multiblock blueprint; **existing V4.1
tanks need a safe unform/rebuild.** Compile and live in-game appearance
are still pending. PR #11 remains draft and unmerged.

### 2026-10-24 — Settling Tank V4.3 inlet throat integration

The inlet's own 12px-wide model left a 16px-wide gap in both rear
screenshot views. The proposed V4.3 adds a tank-specific copper and
refractory throat in the AIR cell immediately ahead of the actual rear
Fluid Input, plus adjoining wall returns. The frame deliberately meets
but does not overlap the real module's copper model; its center and
outward pipe capability remain accessible. Dirty Water now starts just
beyond the inner lip. Four rotated models, Voxel V2 geometry, Patchouli
and the regression tests are updated. Builds use identical physical
blocks and unchanged mechanics; old formed structures need a one-time
re-form to receive the seventh AIR proxy. PR #11 stays draft until the
actual Forge build and gameplay visuals pass.

### 2026-10-24 — Water Settling Tank V4.4 real-port integration

The intended rear inlet correction was not another bridge/throat in an
adjacent AIR block. Both the actual Fluid Input and Fluid Output should
contain LEFT/RIGHT tank-wall segments **inside their own real module
block models**. V4.4 replaces the V4.3 air-collar with two tank-specific
pre-baked visual overlays drawn only by the formed Settling Tank renderer.
The shared working input/output modules, their copper centers, pipe
capabilities, blockstates and hitboxes are unchanged. The input faces
NORTH and the output EAST. Existing six weir/lip V2 proxies remain
but the seventh (0,1,-3) AIR proxy is removed. The tank retains
5×2×5 formation, original ports, custom 16×16 textures, 600→480 mB
water process and sediment output. Generated V2 static model count
returns to 176, plus two port inserts. Safely re-form an already formed
V4.3 tank to clear the old proxy; in-game and Gradle verification pending.

### 2026-10-24 — Water Settling Tank V4.5 liquid footprint correction

User screenshots exposed missing muddy water in the two rear pockets
beside the input and missing clean water near the output/front lip.
V4.5 changes only the dynamic client fluid renderer: a three-area
U-shaped dirty-water surface, connected clean front channel and a
small raised clean-water tongue inside the real EAST fluid output.
Exterior contour sides eliminate internal transparent seam faces and
maintain native fluid UV density. Checks ensure all rendered volumes
remain outside the solid masonry and copper module/insert geometries.
No blueprint/model/texture/processing change, and no rebuild required.
Patchouli/JEI and conserved 600→480 mB water process are intact;
Gradle build and Minecraft appearance still require verification.

### 2026-10-24 — V4.6 fluid finishing before texture review

A new screenshot showed missing water directly under the copper rear
inlet and along both sides of the clean output throat. Rather than
altering the shared modules or the V4.4 tank-only inserts, V4.6 adds
a small dirty-water center tongue **only above the real inlet sill**
and broadens the existing clean-water outlet approach toward the actual
copper frame and front retaining lip. Both are edge-connected to their
reservoirs with shallow/full exterior contours, without Z-fighting
or liquid crossing physical walls. The only game-source change is
WaterSettlingTankRenderer.java; no multiblock rebuild or texture changes.
Source-level tests passed; Gradle/in-game rendering still pending.
Next planned design discussion: user's texture improvements.


### 2026-10-24 — Water Settling Tank V5 art-family redesign

After verifying the V4.6 model from front, side and overhead screenshots,
the remaining criticism was **uniform materials**, not the silhouette or
liquid shapes. V5 retains V4.6 machine logic and geometry while establishing
nine purposeful, original 16×16 material tiles: dark foundation, lighter
refractory outer masonry, smoother ceramic lining, distinct watertight
floor top, clean cast bronze, darker bronze joining plates, working iron,
and preserved grate/sediment art. The generated floor cuboids now use
different texture roles on up vs. side faces without adding geometry.
Bronze corner returns, port footplates, and port caps use dark joint
metal; straight rails keep cast bronze; weir and front retention use steel.
No texture stretching, random speckle, fake grating, or decorative mud.
Voxel shapes, recipes, rotated construction, actual fluid ports, fluid
rendering and item output remain unchanged. PR #11 remains draft until
visual feedback and the full Forge build are accepted.

### 2026-10-24 — V5.1 settling tank texture alignment and lighting

In-game screenshots revealed clipped brick mortar patterns at partial
model elements and on the port footplates, green wet-floor tiles sitting
on exposed exterior side ledges, and differently lit port inserts.
V5.1 replaces start-at-zero UV mapping with Minecraft-native cropped
face UVs in all four orientations. Side ledges receive dry refractory
tops, while real service footplates display bronze only on their top
faces; their sides continue the load-bearing foundation's courses.
The BER samples packed light at the actual rotated fluid-port positions,
not at the controller. A small foundation palette lift improves detail
under directional shading without flattening the Minecraft material.
There are **no geometry, collision, multiblock formation, water-volume,
recipe, controller, or shared fluid-module changes**. Existing tanks
should only require restarting the development client. Geometry/source
assertions passed, but full Gradle and live game acceptance are pending.

### 2026-10-24 — V5.1 user visual sign-off and live status table

After the final V5.1 visual review, the **Water Settling Tank V5.1 appearance is accepted as the visual baseline**. This does **not** clear the separate `./gradlew build`, automation, fluid/item I/O, pause/reload and end-to-end ore-processing acceptance requirements. PR #11 remains **open and draft**, and the next proposed build is the **Charcoal Retort**, not another Mechanical Lathe. The machine-status table above is the current summary; earlier dated sections record historical states.

### 2026-10-24 — Charcoal Retort first source implementation

A source-only 3x4x3 Charcoal Retort has been added to the draft Bronze PR #11: manual fuel queue, 4 burnable logs -> 4 charcoal + 1 Wood Tar in 720 active ticks, output reservation, real item I/O modules, persisted fuel and batch progress, lit chimney smoke, four rotated JSON layouts and Voxel V2 collision, JEI and construction instructions. Wood Tar can be reused as 200-tick fuel in this Retort. The V1 model is a deliberately basic vanilla brick/copper prototype; **do not mark visually accepted, compiled or gameplay validated**. The subsequent Refractory Kiln is the intended next candidate once this machine passes integration testing.

### 2026-10-24 — Retort copper-clad V2 visual prototype

The formable Charcoal Retort now has a regenerated static shell with separate dark foundation, recessed kiln door, inspection grille, native 16×16 refractory/bronze art, layered copper roof and open framed chimney. Four facing-specific block-local model sets and the Voxel V2 profile were updated together. The real Input, Charcoal Output and Wood Tar Output **remain unchanged and never receive fake replacement models**; formation JSON, Patchouli recipe, processing and firebox logic are unchanged. Existing formed V1 instances must be safely unformed and re-formed to refresh physical collision. **Visual/Forge/gameplay acceptance remains pending.**

### 2026-10-24 — Charcoal Retort V3 visual source implementation

Generated V3 block-local roof, front hatch, foundation and chimney geometry is added, alongside formed-state *real port* model switching for log input / charcoal output / tar output. The port BEs keep their Forge capabilities and their generic unformed/other-machine models; the Retort links them during formation. The construction JSON, 3×4×3 footprint and original recipe remain untouched. V3 is **not yet Forge-build or gameplay accepted**; confirm from in-game screenshots before starting another machine.

### 2026-10-24 — Retort V3.1 visual proportions (review pending)

Following new front/side/rear/roof gameplay screenshots, the Retort model generator was revised to deepen the hatch, increase apparent chimney height by lowering the adjacent roof field, recess side masonry behind corner ribs and differentiate real intake/charcoal compartments. Structural 3x4x3 JSON, actual ports, recipes and processing remain unchanged; **in-game acceptance/Gradle compilation still pending**.

### 2026-10-24 — Retort V3.2 top-layer-only art pass

As requested, the next visual prototype replaces **only the Y=3 roof** with continuous copper slabs, two dark front-to-back bands and a real open-centred chimney. The 3x4x3 construction/ports/processing are unchanged; roof Voxel collision tracks its new height, so re-form the machine. **Visual acceptance pending**; no lower-layer redesign in this commit. Wood Tar remains an item pending a separate fluid-processing decision.

### 2026-10-24 — Retort V3.3 inset roof channels

The two lengthwise dark roof lines now sit in **recessed grooves (Y=8.75)** cut into the **copper slab surface (Y=10)** instead of raised straps. Only top-layer models and roof Voxel collision were updated. Real modules, lower three layers, recipes and formation JSON are unchanged. Await screenshot-based roof sign-off before touching the next layer.

### 2026-10-24 — Retort V3.4 chimney-only vent correction

The center roof chimney model now has real inset-through-wall vent apertures (two rows on all four exposed sides), an anchoring copper base, stronger open top lip and limited internal soot details. All eight grooved copper roof tiles, Y=0–2 models, working modules, formation layout and recipe system are unchanged; the existing coarse collision profile is intentionally preserved. Static tests ensure four directional variants, clear slots/throat, and zero overlapping rendered faces. **In-game screenshot and Gradle acceptance pending.**

### 2026-10-24 — Retort V3.5 chimney-only texture and geometry cleanup

Removed the visually distracting floating mounting ring, rebuilt the vent wall with integer-pixel geometry, preserved two genuine vent rows and the hollow opening, and gave interior/exposed underside faces properly mapped textures without extra overlapping planes. Only four chimney models plus their generator/tests/docs changed; roof slabs, lower three layers, collision, ports and production are untouched. **Minecraft appearance and Gradle build still require verification.**

### 2026-10-24 — Retort V3.6 flush chimney integration

Corrected floating chimney by filling the *center AIR-host model only* with four flush Cut Copper roof-seat segments at Y=0..10 while preserving the open 6x6 flue. Inner faces now use copper rather than near-black soot. The center seat updates voxel collisions (34 -> 38), so re-form existing Retorts. Adjacent roof-part face culling is regenerated but their shape/texture, real ports, lower layers, structure JSON and recipes remain unchanged. Screenshot approval and Gradle build pending.

### 2026-10-24 — Retort V3.7 Y2 chamber (visual review)

Copper roof/chimney are **visually accepted and locked** after user approval. The next pass changes **only the eight Y=2 brick chamber models for each of four orientations** (32 generated model files): tiled brick walls, stronger corner brackets and a recessed five-slot copper/iron vent. The user's marked region partly crosses into Y=1, which remains for a subsequent isolated pass. JSON formation, lower machine, real ports, process/fuel, collision and roof model bytes are unchanged. In-game Y2 acceptance and Gradle compilation pending.

### 2026-10-24 — Retort Y2 V3.8 reference alignment (visual review pending)

Corrected the visually wrong Y2 materials and missing industrial details: native refractory masonry again, cut-copper pilasters/vent bars/sill, dark iron fasteners, and a larger recessed five-slot vent. Exactly 32 upper-layer model outputs change. The approved Y3 roof/chimney, real ports and Y0/Y1 lower parts, JSON formation, recipes, and collision are unchanged. **Await side/front/reference comparison before accepting this layer.**

### 2026-10-24 — Retort V3.9 front masonry and roof-lip line fix (review pending)

Fixed an actual missing Y2 front-wall field: the shoulder brick base ended at Z=13, leaving the decorated front 3px hollow. Its visible middle now reaches the face without altering adjoining layer contacts. The two accepted roof grooves now also continue down the front copper fascia, at the user's marked positions, while their horizontal paths, chimney and all other roof cells remain intact. Generated change: 12 Y2 front models plus 8 roof-front models only. No structural, I/O, process, or collision changes. In-game approval pending.

### 2026-10-24 — Retort V3.10 front-focused correction

Removed the unwanted continuous under-roof iron strip, switched only the Y2 **front** brick blocks to mortar-lined Minecraft brick textures matching the reference, and refined the copper-framed five-slot grille. Only 12 front Y2 model files (three per orientation) and four equivalent/culling-only Y3 roof underside outputs changed. No changes to chimney, roof top and grooves, sides/rear, real ports, Y0/Y1, formation, recipes or collision. **Pending frontal in-game acceptance; do not proceed to side/lower layer yet.**

### 2026-10-24 — Retort V3.11 one-block front-left facade trial

User requested **one physical block at a time**: edit only Y2 SOUTH-local x=-1,z=0 to produce a 3D masonry inset, raised left copper pillar, large dark square corner bracket, lower iron foot and slim right separator. All four orientations generated. Six neighboring asset meshes may be regenerated solely for shared-face culling, not restyled. The accepted roof/chimney, central/right front, lower machine, port layout, recipes and collision remain unchanged. **Wait for one-block in-game sign-off before mirroring or progressing.**

### 2026-10-24 — Retort V3.12 one-block masonry right-edge fill

Fixed only the small void next to the front-left Y2 block's inner copper divider by adding a 2px-wide brick return (local X 11..13, Y 3..13, Z 13..15), preserving the recessed center panel at Z=13. Exactly four facing-specific versions of **the same one block** regenerate; all adjacent parts and collision remain unchanged. Pending one-block screenshot sign-off and Forge build.

### 2026-10-24 — Retort V3.13 first left upper brick block (pending review)

Changed only the front-left Y2 brick cell from a 3px-deep alcove to a continuous near-flush brick wall (face at Z15) with copper and iron supports protruding to Z16, retaining a modest 3D effect. Removed the old side reveals, right fill patch and stepped lower sill; neighboring center/roof files regenerate only for hidden face culling. No gameplay/collision alterations. Await in-game acceptance of this single block.

### 2026-10-24 — Retort V3.14 physical masonry on one front-left Y2 cell (review pending)

Rebuilt only the SOUTH-local x=-1,y=2,z=0 front-left brick block with three staggered raised brick courses over a narrow exposed mortar bed, integrated copper outer/inner framing, and an iron corner bracket/foot. The 1px relief preserves solidity without a deep alcove. All four rotations generate; the center vent and adjacent roof assets regenerate only internal hidden faces. No other facade block, chimney, roof top/grooves, Y0/Y1, ports, recipes or collision changed. Await in-game visual acceptance and Gradle build.

### 2026-10-24 — Retort V3.15 L-shaped framing for first upper front-left block

Preserved the newly accepted-in-principle individual brick courses and their mortar gaps from V3.14. Replaced the incorrect symmetrical copper U-frame with the target L-shaped silhouette: left corner post and dark iron bracket plus a broad cut-copper header, extending the exposed masonry to the right edge and removing the unwanted right post, both right iron caps/feet and the bottom sill. Four rotations generated. Neighbor/roof assets changed only for hidden face partitioning; all neighboring *outward* faces remain identical. Still awaiting in-game sign-off before other blocks are touched.

### 2026-10-24 — Retort V3.16 restore brick relief, preserve front-left frame

The user confirmed V3.15 framing is placed correctly, while the 3D brick effect became too weak. V3.16 leaves the copper L-frame unchanged, increases the original staggered brick courses to 1.25/1.50px relief above the mortar (brick Z15.45/15.70, mortar Z14.2), and changes nothing beyond the four directional versions of this one authored Y2 block and four hidden-contact center-neighbor meshes. All Y3 roof assets remain byte-identical. Pending in-game acceptance and Forge build.

### 2026-10-24 — Charcoal Retort V3.17 top bracket + lower copper shoulder (review pending)

User highlighted two front-left Y2 differences: top iron corner must be a recessed-square fixing rather than plain cap, and lower projecting shoulder must be copper with a smaller dark metal foot beneath. Rebuilt only those two fittings, leaving staggered raised masonry and correctly positioned copper L-frame identical in source. Roof, chimney, other layers, real ports, collision and gameplay unchanged; visual sign-off pending.

### 2026-10-24 — Retort V3.18 upper front-center block only (await screenshot)

Following the single-block approach, rebuilt the center Y2 block from the reference: five-slot copper grille backed by recessed soot, inset iron mounting plates on both ends, solid brick lower field, two copper load-bearing uprights, short stepped copper arch and dark feet. Single-authored part in four orientations; 16 adjacent Y1/Y2/Y3 meshes regenerated only for shared hidden-face culling, with their outward front geometry unchanged. Left/right front facades, accepted copper roof/chimney and all structure, ports, recipes and collision untouched. Gradle/in-game confirmation pending; do not advance to right block yet.

### 2026-10-24 — Retort V3.19 connected front-center supports + arched copper apron

The front-center Y2 block's two copper uprights now meet their iron vent mountings at Y=11.5 (previous 0.3px gaps removed). Replaced the old flat four-box lower bridge with a seven-piece symmetric shallow arched copper trim, using physically connected segments and a higher center. The five recessed grille slots, accepted front-left facade, roof/chimney, real lower hatch/ports, collision and processing are unchanged. Four Y2 directional models and two hidden-contact Y1 models regenerate. In-game approval and Gradle build pending.

### 2026-10-24 — Retort V3.20 connect the full front copper header (review pending)

After V3.19, horizontal left–center–right connection remained missing. V3.20 retains the left Y2 L-frame, fills the two 0..0.6 / 15.4..16 px copper gaps at the center grille edges, and adds a matching cut-copper header to the right front Y2 block while setting brick back behind it. Iron grille mountings remain visible and correctly interrupt the copper. No new dark roof strip. All four facings pass a 48px continuity sampling test; 12 Y2 meshes and 8 roof underside-only meshes regenerate, with no visible roof/left-front change. Grille and curved arch retained; game logic, lower ports, collision unchanged. Await in-game screenshot and build verification.

### 2026-10-24 — Retort V3.21 center width + physical 3D brickwork (visual review pending)

On the front-center Y2 cell only, widen the two iron mounting brackets 1.9→2.5px, copper uprights 1.1→1.9px and dark feet 1.5→2.25px; replace flat brick front with nine raised staggered bricks over a Z14.2 mortar backing, with Z15.45/Z15.70 faces. Preserve the five recessed grille slots, V3.19 seven-piece arch and V3.20 cross-block copper header. Four center Y2 assets plus four contact-only Y1 meshes regenerate. Roof, left/right facade, parts/ports, recipes, collision and gameplay unchanged. Static model checks passing; in-game comparison and Gradle build outstanding.

### 2026-10-24 — Retort V3.22 front-right upper block (await visual sign-off)

Rebuilt right/front Y2 cell as physical staggered-brick masonry over recessed stone mortar, with a single substantial copper outer-right post, raised header connecting to center, recessed square iron top-right corner boss and a copper shoulder above a narrow iron foot. Removed the generic right-hand inner frame and lower continuous sill to match the reference. Four right-cell facings, four center internal-seam meshes and four roof undersides regenerate; roof outward and center outward visuals remain unchanged. No formation, port, machine logic or collision changes. Static mesh checks pass; Minecraft appearance and Gradle build not yet verified.

### 2026-10-24 — Retort V3.23 upper right side-wall masonry (visual review pending)

Two SOUTH-local Y2 right-side cells (x1/z−1 and x1/z−2) now display five staggered physical brick courses on the outward +X face: original refractory structural core, 0.2px mortar at X14..14.2, individually raised cut brick faces at X15.45/15.70. The two Z course layouts alternate to avoid simple repeated wallpaper patterns. Removed only their rejected full-width upper/lower dark iron bands. Rear copper corner and compact rear iron end caps remain; rear face material areas unchanged. Other side remains on original model for now. Eight authored rotated right-side meshes plus 20 hidden-contact meshes regenerate, 28 model files in total. Roof exterior, front-right facade and lower side outward faces unchanged. Pending in-game screenshot comparison and Forge build.


## 2026-10-31 — Canonical roadmap reconciliation and next implementation

This document's **16 named machines and their distinct roles** are the authoritative
planning scope. The separate `bronze-age-foundry-roadmap.md` describes the
already-implemented casting sub-chain, not an alternative/shorter machine list.

- **Source implemented, not accepted:** Stamp Mill, Rotary Ore Classifier,
  Water Settling Tank and Charcoal Retort. The user has verified real liquid
  Wood Tar extraction from the Retort but the latest NBT fix and render model
  are pending follow-up verification.
- **Also implemented on draft PR #11, outside this 16-machine candidate list:**
  Pattern Maker, Mold Table, Bronze Foundry, Casting Basin and Mechanical Lathe.
  Presence of source does not imply the full compiled/visual/gameplay
  acceptance criteria passed.
- **Planned:** the remaining 12 listed machines, including the Refractory
  Kiln, Ore Roasting Kiln, Industrial Air Blower and Bloomery.
- **Next build target:** Refractory Kiln, preliminary 5x4x5 footprint,
  intended to fire appropriate clay/sand/mineral refractory batches with
  sustained HEAT and finite fuel. The batch recipe, relative material quality,
  heat/cooling thresholds and fuel ratios require explicit source/contract
  decisions before gameplay balancing. The currently existing refractory
  brick texture is **not** evidence of a registered brick item or Kiln.
- **Parallel verification track:** run `./gradlew build` and the Retort
  NBT/multiplayer/fluid-port regressions; never merge PR #11 on mere
  source presence or one working demonstration.

The named machines in the original table remain intact, and the previously
planned footprint values remain proposals, not silently finalized builds.

### 2026-10-31 — Refractory Kiln K1 structural slice

K1 adds the physical 5×4×5 JSON-based controller/structure in four facings, a brick V2 static model/voxel profile, two genuine front item modules and Patchouli formation. The module input is intentionally disabled until K2 can accept prepared refractory batches. NO firing/HEAT/ceramic output exists yet; verify build and formation before treating K1 as a working prototype. Full visual model/texture polish is deferred but no missing textures or incorrect structure/ports are acceptable.

### 2026-10-31 — Ore Roasting Kiln reference-driven 5×8×9 visual rebuild

The original 5×3×9 visual prototype failed the user’s screenshot comparison:
it appeared as a low brick box, not the industrial roaster shown in the
**first** concept image. The user explicitly approved increasing the height.

Draft PR #11 now contains a **5×8×9 structural prototype**: one
Stone Bricks base, three courses of brick roasting walls with **three tall
arched openings on each side**, a separately pitched stone roof with real
Voxel V2 collision, timber headers, dark structural iron ribs and a **raised
3×3 brick chimney with a continuous AIR shaft**. The full geometry has
four orientation-specific JSON structures, generated baked model parts, a
matching six-layer Patchouli visualizer and real item input/output modules.
The picture's large realistic gear and external shaft are intentionally
absent. The brick/stone/roof areas use appropriate vanilla Minecraft
textures; bespoke 16×16 art is reserved for grilles, dark iron, embers, soot
and I/O surfaces.

This change **breaks the earlier 3-high blueprint on purpose**. Prototype
worlds must dismantle the old formed roaster before updating and rebuild the
six-high version. Roasting chemistry, fuel, HEAT, exhaust pollution and ore
byproducts remain unimplemented, and the item input stays locked to prevent
loss. A fully passed Gradle build and actual *in-game visual acceptance*
are still required; do not mark the prototype complete yet.

### 2026-10-31 — taller chimney and vanilla-texture correction

The user showed that the 6-high chimney looked like a squat box and that the
custom ember/grille textures were visually inappropriate. The prototype is
**5×8×9**, with a genuine three-course 3×3 Bricks chimney ring at the rear,
continuous 1×1 AIR flue, and stone-coped upper rim. All six side arches now
share a recessed, full-volume geometry, replacing the earlier one-bay-only
comparison and paper-thin panel variants. Most visible furnace textures are
vanilla: Bricks, Stone Bricks, Deepslate Tiles, Spruce Planks, Smooth Stone,
Polished Deepslate, Iron Block, Black Concrete and Magma Block. Only the two
real item-port symbols still use roaster-specific PNG textures.

A source geometry check found and removed **same-plane, same-normal overlaps**
that caused z-fighting in the old arches, brick piers, roof strips and chimney
supports. Visual and build acceptance remain **pending**. Old 6-high formed
prototype structures must be dismantled before updating to this breaking
blueprint size. No roasting gameplay has been added.

## 2026-11-01 — Mechanical Forge Hammer (Bronze III K1)

After the user tested Refractory Kiln firing and Ore Roasting Kiln K2 successfully,
start Bronze III with the **Mechanical Forge Hammer** while leaving the kiln
visuals frozen for Studio. Source candidate: one **5x4x3** JSON structure in
four facings, with real rear Gear V2 mechanical input, separate front item
input/output, V2 model/voxel and a dynamic die ram driven by the existing
mechanical rotation. Initial work loop: **4 vanilla Copper Ingots -> 4 Copper
Plates in 160 supplied ticks**, 20 minimum speed, 5 torque, one bronze die
wear cycle per completed batch. This supplies a useful existing item rather
than a dead-end future hot-metal intermediate; the Primitive Anvil remains a
valid manual fallback. Cold copper forging is only the K1 recipe: the intended
hot-blank/forge/roll/press chain must use real HEAT and useful materials later.

Validate all four formation facings, actual Gear V2 load release on idle/stall,
input and output reservation, wear replacement, NBT reload, source models/UV
and performance, and the unchanged older machines. No claim of compilation
or gameplay success until tests actually pass. See
[`bronze-forge-hammer-contract.md`](bronze-forge-hammer-contract.md).

## 2026-11-01 — Bronze Rolling Mill (Bronze III K1)

After the user verified the Forge Hammer's **4 Copper Ingots -> 4 Copper Plates**
process following the exact corrected server ticker, advance the Bronze III
manufacturing line with a **5×3×3 mechanically driven Rolling Mill**. The
source-stage K1 uses a conservative cold copper route: **4 Copper Plates ->
4 Copper Sheets** in 180 supplied ticks at 16 RPM and 6 torque, with real
rear mechanical drive, LEFT input and RIGHT output modules. Copper Sheets
have an immediate, additive use: **4 sheets -> 8 Water Pipes** versus the
untouched 6 Copper Plates -> 8 Water Pipes recipe. No copper duplication:
each rolled plate yields one thinner sheet; pipe cost saving is a provisional
industrial forming efficiency, not a final mass-balance model.

Roller sleeves wear once per successfully completed batch (512 cycles),
are replaced through the controller, and are persisted independently of
inventory. The static Voxel V2 geometry and two baked rotating rollers
are generated independently; do not move the real input/output block BEs
or force the Studio concept image into the current JSON footprint.
The first mill is not a heated billet system. Heat/material grades, bronze
sheet and rods remain later recipe decisions with actual downstream uses.

See [Rolling Mill contract](bronze-rolling-mill-contract.md) for exact
construction blueprint, lifecycle, visual reference and regression tests.
This is a **source candidate**, not verified gameplay or compiled software.

## 2026-11-01 — Mechanical Press (Bronze III K1)

The user confirmed that the **Copper Sheet** display name now works. This
does **not** by itself verify the Rolling Mill's complete powered processing
or animation cycle; test that separately before claiming an entire chain
works. The next Bronze III milestone is a **3×3×3 Mechanical Press** for
sheet forming, distinct from cold plate forging and thin-sheet rolling.

One JSON recipe **4 Copper Sheets -> 4 Copper Panels** in **160 supplied
ticks**, requiring **18 input RPM and 8 torque**, with real Gear V2 load,
LEFT input/RIGHT output modules and a physical rear drive. A serviceable
Bronze Press Die has **384 completed-batch cycles**. Four panels plus
two Glass give a Copper Fluid Tank through a new additive crafting
recipe; the older 6 Copper Plates + 2 Glass recipe remains untouched.

The K1 controller uses the existing server-only mechanical-load/batch
protocol, not an independent energy network. Its immutable block model
and generated Voxel V2 geometry originate in
`tools/multiblock-models/designs/bronze_mechanical_press.json`.
The separate press ram follows the actual Gear V2 cam phase; no item
entities or per-frame model generation. A concept reference and editable
SVG contract document the intended later Studio art. K1 does **not**
include heated metal blanks, configurable mould tooling, a sheet-quality
system or rod forming; these are separate design decisions.

See [Mechanical Press contract](bronze-mechanical-press-contract.md)
and [Studio vector reference](reference/bronze-mechanical-press-studio.svg).
**Status: Mechanical Press K1 basic operation confirmed by user in Minecraft (2026-11-01); build, comprehensive regressions and multiplayer remain unverified.**

## 2026-11-01 — Mechanical Press K1 first in-game acceptance

The user reported **"works"** after testing the Mechanical Press introduced at
`a6aee2fd3fe91fb6ffce94294eb33702166db662`. Record this as **basic
single-player in-game functionality user-confirmed**, not as automatic proof
that every item-flow, power-interruption, alternate recipe, die replacement,
save/reload, four-facing, performance or multi-client regression passed.
No source or resource code changes were needed for this acceptance report.
The Forge Hammer's basic operation has separately been confirmed; the Rolling
Mill's full powered sheet conversion and animations have **not** yet been
explicitly confirmed, so the three-machine production chain is not fully
end-to-end verified. The next *planned* machine is Precision Boring Machine;
this status update does not authorize its implementation.

## 2026-11-01 — Precision Boring Machine (Bronze III K1)

After completing the planned multiblock foundations, the broader
ore-by-ore recipe/quality pass will follow; this K1 is a minimal functional
casting operation, **not a change to ore processing**. The 5×3×3 Precision
Boring Machine reuses the existing Foundry/Basin's **Rough Bronze Bearing
Casting**, removing an internal bore to make **one Bronze Bushing** in
200 supplied ticks at **20 RPM and 6 torque**. The reusable Abrasive Boring
Bit wears once per completed piece (384 batches). The existing Mechanical
Lathe still finishes precision bronze gears and bearings; the Boring Machine
makes a distinct sleeve/bushing. Bushing stock is consumed by a new, additive
Mechanical Water Pump crafting recipe (2 Oak/other planks, 1 wooden shaft,
1 pulley, 1 bronze bushing); the original Copper Age pump recipe remains.

NBT from the rough bronze casting is preserved on the machined bushing to
avoid losing existing ingot-quality metadata. The alternative vanilla
crafting recipe does **not** yet transfer bushing quality into the finished
pump: assembled-component quality propagation remains part of the agreed
post-multiblock integration pass. No new ore types are registered.

A real rear Gear V2 input drives a supported, enclosed gear housing, a
horizontal boring bar and a stationary clamped casting. Static frame models
and Voxel V2 collision come from one generator design with four facings;
real module slots are kept clear. Separate baked shaft and rear coupling
models use the actual input's visual phase. Editable design JSON and a
single-machine SVG reference are retained for Magneticraft2 Studio.
See [Precision Boring Machine contract](bronze-precision-boring-machine-contract.md)
and [Studio vector reference](reference/bronze-precision-boring-machine-studio.svg).
**Status: source candidate.** Compilation, four-facing formation, actual
boring, quality NBT, wear, save/reload, automation, FPS and multiplayer are
not verified in Minecraft; PR #11 stays draft.
