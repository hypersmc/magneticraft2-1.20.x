# Bronze Age industrial progression (Magneticraft2 1.20.1)

Status: **approved design direction**, not a claim that the proposed machines are implemented. Commit new content incrementally and preserve Copper-era functionality. Target progression: **Copper workshop -> Bronze industrial factory -> first iron bloom -> Iron Age**.

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
| **Bronze II — Ore dressing and industrial fuel** | *Stamp Mill + optional Rotary Ore Classifier source implemented in PR #11; in-game/Gradle verification pending* | Stamp Mill (ore breaker) -> optional Rotary Ore Classifier (3 fines + 1 coarse return per 4 crushed) -> Washer -> Sifter; settling/recycled-water tank, roasting kiln and charcoal retort still planned. First test line: 64 ore into product + tailings with real machine power and water. |
| **Bronze III — Manufacturing and networks** | *Planned* | Refractory kiln, forge hammer, rolling mill, mechanical press, boring machine, sawmill, mold-sand mixer, assembly station. Introduce larger line shafts, mechanical item distribution, lubrication, maintenance and meaningful quality differences. |
| **Bronze IV — Iron preparation** | *Planned* | Iron ore concentration, controlled reduction/smelting, flux and charcoal, high-temperature refractories, mechanical air blast, bloomery furnace. First iron bloom completes Bronze's core progression and opens Iron Age. |

## Multiblock candidates (footprints preliminary)

| Machine | Suggested footprint | Input -> output / distinct role | Dependency |
|---|---|---|---|
| Stamp Mill | 5x3x4 | Raw ore -> crushed fraction, optional waste; large inertia and hammer wear | Bronze bearings, Gear V2 |
| Rotary Ore Classifier | **3x3x5** | 3 screened fines + 1 reusable coarse crushed ore per 4 crushed (no tailings generation yet) | Stamp Mill |
| Ore Roasting Kiln | 5x3x5 | Sulfide concentrate -> roasted feed plus exhaust/impurity | Refractory and fuel |
| Water Settling Tank | 5x3x5 | Dirty water -> reusable clean water + captured sediment, bounded efficiency | Ore Washer |
| Charcoal Retort | 3x4x3 | Wood -> charcoal plus recoverable byproduct/heat | Furnace, airflow |
| Refractory Kiln | 5x4x5 | Clay/sand/flux -> refractory ceramics | Improved fuel |
| Mechanical Forge Hammer | 5x4x3 | Hot metal blank -> forged part | Bronze power |
| Bronze Rolling Mill | 5x3x3 | Heated billet -> sheet/rod | Precision bearings |
| Mechanical Press | 3x3x3 | Sheet/blank -> stamped part | Rolling mill |
| Precision Boring Machine | 5x3x3 | Casting -> precision bore | Lathe toolmaking |
| Mechanical Sawmill | 5x3x3 | Timber -> accurately cut lumber/pattern stock | Gear V2 |
| Foundry Sand Mixer | 3x2x3 | Used sand + binder -> reusable molding sand, with loss | Foundry |
| Industrial Air Blower | 3x3x3 | Gear V2 -> pressurized process air | Forge/Bloomery |
| Mechanical Assembly Station | 5x3x5 | Prepared components -> composite machinery | Bronze III |
| Mechanical Ore Breaker | 3x3x3 | Oversized/irregular rock -> feed for Stamp Mill | Optional precrusher |
| Bloomery Furnace | 5x5x5 | Prepared iron ore + charcoal + flux + air -> bloom + slag | Bronze IV |

Footprints and intermediate chemical recipes are **design proposals**, not final build requirements. Real ores need individual paths; chalcocite, cassiterite, quartz and iron minerals must not all magically smelt identically.

## Initial, existing material chain

* Chalcocite Ore -> existing Grinder -> Crushed Chalcocite -> Washer -> Washed Chalcocite -> Sifter -> Copper Concentrate.
* Cassiterite Ore -> existing Grinder -> Crushed Cassiterite -> Washer -> Washed Cassiterite -> Sifter -> Tin Concentrate.
* Both existing chains are deliberately retained as the Bronze baseline. The new Stamp Mill should yield greater *throughput* or reliable size control rather than eliminating the processing steps.
* Bronze casting is already scaffolded by Foundry, mold production and Lathe. The next engineering job is to connect it to industrial ore dressing.

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
4. Implement bounded dirty-water reclamation, then selective metallurgy and Iron Age prerequisites.

## Pre-next-multiblock consistency sweep (2026-10-19)

The registered ore catalog and mining rewards are now fixed as part of PR #11:
all **42 ore blocks** have self-drop loot tables and Stone-pickaxe mining tags,
including Quartz and Cassiterite needed in earlier ages. The previously
untagged Stamp Mill/Lathe controllers are harvestable with Stone pickaxes too.
See [ore processing by age](ore-processing-by-age.md) for the 31-family matrix
and implementation states. This did not change ore worldgen or furnace yields.

The next substantial ore-processing multiblock should be chosen based on a
usable input/output chain, not just a new machine design. Current candidates
remain the Rotary Ore Classifier and Dirty Water Settling Tank; neither is
claimed implemented. Iron ore reduction still needs dedicated concentrates,
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
