# Iron Age — full block, machine, material and progression plan

**Magneticraft2 / Forge 1.20.1**  
**Prepared:** 2026-11-07 · **Branch:** `feature/iron-age-foundations` / draft PR #18  
**Status:** Comprehensive proposed content plan, **not a blanket approval to implement**. The first Wrought Iron Forging Die (Iron 0A) was player-accepted; on 2026-11-07 the player also produced a Wrought Iron Plate through the Iron 0B process. Other candidates are not implemented unless explicitly marked, and full build/regressions are still open.

## 1. Iron Age identity and rules

Stone → Copper → Bronze → **Iron** → Steel → Steam → Gasoline → Electric Generator → Nuclear.

Bronze gives us precision casting, mechanical manufacturing, industrial ore preparation, HEAT/AIR handling, a Bloomery, and Wrought Iron. Iron should transform that Wrought Iron into **forged structural components, stronger mechanical transmission, and heavy manufacturing**. Every block must either make something new, improve an existing mechanical bottleneck, or meaningfully interact with the world.

**Don't duplicate Bronze machines.** Extend the Forge Hammer, Rolling Mill, Press, Lathe, Boring Machine and Assembly Station instead of adding identical Iron machines. Don't generate torque by changing material. Don't cast low-carbon wrought iron as if it were bronze. Steelmaking, boilers and steam engines belong to later stages unless explicitly redesigned.

**Legend:** DONE = already implemented and player-confirmed; CORE = proposed main age; CONDITIONAL = only when a real consumer/use is proven; DEFERRED = later age or separate system.

## 2. Existing infrastructure to keep relevant

| Existing machine/system | Real source capability | Iron extension |
| --- | --- | --- |
| Magnetite Stamp Mill → Washer → Sifter | Material preparation | No new ore processing line needed |
| Bloomery + Industrial Air Blower | Magnetite/Calcite/Charcoal + HEAT/AIR → Iron Bloom + Slag | Preserve working Wrought Iron supply |
| Refractory Kiln | Server-timed reheating of **Iron Bloom** | Extend carefully to hot ingot/bar workpieces (currently **not** generalized) |
| Bronze Forge Hammer | JSON processing + actual power; hot-bloom consolidation | Form wrought plates, bars and gear blanks using real hot-stock checks; Forging Die already 384/768 |
| Bronze Rolling Mill | Data-driven processing, Gear V2 load and serviced sleeves | Plate → sheet and suitable prepared bar profiles; check HEAT and NBT |
| Bronze Mechanical Press | Data-driven sheet forming and die wear | Stamped wrought panel / casing blanks, if material/die capability is justified |
| Bronze Mechanical Lathe | **Hardcoded** Bronze casting → Bronze precision-part outputs | Extend to JSON-defined Iron workpieces/cutters, keeping existing Bronze behavior |
| Bronze Precision Boring Machine | Recipe-driven bore operations; preserves source NBT | Finish iron bearing housings |
| Bronze Mechanical Assembly Station | Multi-input recipe assembly and actual Gear V2 load | Produce supported iron transmission/structural parts using its **actual slot count** |
| Existing gears, shafts, flywheel, belts and arms | Gear V2 topology, ratios, load, inertia, physical item movement | Extend only where there's a measured bottleneck; reuse IO and animation system |

**Measured in existing Java source (not proposed balance):** wooden medium gear = **8 torque**; wooden gearbox/custom gearbox = **8**; wooden shaft = **16**; large wooden gear = **16**; Mechanical Input Module = **16**; small water wheel = up to **6**, large water wheel = up to **16**, depending on water flow. Wooden flywheel has a 6-torque temporary inertial-source parameter.

**Result:** Replacing the 16-capacity wooden shaft alone will not overcome an upstream 8-capacity wooden gear or gearbox. Test the complete loaded network. New 16+ torque ratings are **not** free power.

## 3. Full proposed item/material catalog

| ID (candidate unless noted) | Created using | Downstream purpose | Priority |
| --- | --- | --- | --- |
| `wrought_iron_ingot` | Existing Bloomery → hot Bloom → Forge Hammer | Entry material | DONE |
| `wrought_iron_forge_die` | Existing 4 wrought ingots + blackstone recipe | Forge Hammer 768 vs Bronze 384 batches | DONE |
| `wrought_iron_plate` | Heated ingot, existing Forge Hammer | Casing, frames, Press, gear blanks | PLAYER-CONFIRMED first plate (2026-11-07); further verification open |
| `wrought_iron_bar` | Hot ingot → existing powered Rolling Mill | Shaft stock and first structural beams; 1 ingot-equivalent per Bar | SOURCE IMPLEMENTED — testing pending |
| `wrought_iron_rod` | Hot Bar → existing Rolling Mill, 4 Rods | Quarter-ingot fastener/structural stock; precision drawing is later | SOURCE IMPLEMENTED — testing pending |
| `wrought_iron_sheet` | Existing Rolling Mill on prepared plate | Riveted iron panels and light fittings | CORE |
| `wrought_iron_rivets` | Cut/punch rods or bar, with valid yield ledger | Assembly, frames, casing | CORE |
| `iron_gear_blank_medium` | Forge 1 hot Wrought Bar at existing Forge Hammer | Existing Lathe with flint-tipped cutter | SOURCE IMPLEMENTED (not tested) |
| `precision_iron_gear_medium` | Existing Bronze Lathe, 160 ticks at 12 RPM / 5 torque, double cutter wear | Craft 16-torque Iron gear with Plates/Bar | SOURCE IMPLEMENTED (not tested) |
| `iron_gear_blank_large` | Forge larger blank with more ingot-equivalents | Lathe into large precision gear | CORE, later |
| `precision_iron_gear_large` | Lathe with appropriate size/tool gating | Actual installed large Iron Gear | CORE, later |
| `machined_iron_shaft` | Turn wrought iron bar in Lathe | Installed shaft, gearbox, flywheel | CORE |
| `iron_bearing_housing` | Forging and existing Precision Boring Machine | Gearbox/flywheel, physical bearing seats | CORE |
| `iron_lathe_cutter` | Forge/grind serviceable iron-capable cutter | Work Iron on existing Lathe | CORE if Bronze cutter cannot |
| `iron_shear_blade` | Forged tool from wrought stock | Replaceable blade in Stock Shear | CORE |
| `iron_press_die` | Iron tool upgrade | Iron panel forming or longer service | CONDITIONAL |
| `iron_roller_sleeves` | Iron tool upgrade | More demanding rolling, real wear benefit | CONDITIONAL |
| `graphite_lubricant` | Actual processed Graphite | Bearing maintenance reduction | CONDITIONAL |
| `iron_machine_fittings` | Forged brackets/fasteners | Assembly, only if distinct from rivets | CONDITIONAL |

Avoid unnecessary ingot → plate → bar → rod loops that create mass. Establish an **ingot-equivalent ledger** before approving ratios. A rod can yield multiple small rivets by *part count* without creating four ingots of metal. Recoverable offcuts/scrap require real output slots and consumers. Quality metadata must be conserved on every process. Hot state uses the **existing item ID with server-authoritative expiring metadata**, never fake permanent "hot ingot" clones.

## 4. Core new blocks (proposed)

**Expanded proposal (2026-11-07): 16 new registered blocks**, including
**three distinct JSON-formed multiblocks**, **five mechanical infrastructure
blocks** and **eight structural/factory-construction blocks**. This is a
design target, not a feature quota or approval to implement all proposals.
The Mechanical Draw Bench is gated on genuine consumers. The Mechanical
Hoist/Gantry is a separate, conditional design and not included in 16.
See [Iron industrial factory expansion](iron-age-industrial-factory-expansion.md).

| Candidate ID | Type | Primary in-game function | Phase |
| --- | --- | --- | --- |
| `wrought_iron_gear_medium` | Gear V2 8-tooth gear | 16 vs wooden 8 torque capacity, forge/lathe/assembly path | SOURCE IMPLEMENTED (not yet in-game verified) |
| `wrought_iron_gear_large` | Gear V2 16-tooth gear | Strength/ratio choices, matching established mesh pitch | Iron II-B |
| `wrought_iron_shaft` | Gear V2 X/Y/Z axial shaft | 24 max torque, existing wood 16; current 16-torque sources unchanged | SOURCE IMPLEMENTED — in-game tests pending |
| `wrought_iron_gearbox` | Gear V2 gearbox | Existing 1:1 right-angle ports, 16 max torque vs wood 8 | SOURCE IMPLEMENTED — in-game tests pending |
| `riveted_iron_frame` | Structural full block | Genuine heavy machine support in multiblock layouts | Iron II-B |
| `iron_stock_shear` | **New JSON multiblock** | High-load mechanical stock cutting/punching and fasteners | Iron III |
| `riveted_iron_casing` | Full iron casing block | Enclosure and structural ingredient for heavy machinery | Iron III |
| `iron_forge_hearth` | **New JSON multiblock** | Batch heating of iron stock, fuel/HEAT/AIR logistics, not another Bloomery | Iron III/IV |
| `iron_flywheel` | Gear V2 inertia block | More stored angular momentum than the wooden flywheel | Iron IV |
| `iron_structural_beam` | X/Y/Z structural beam | Visible supported columns, factory spans and multiblock framing | Iron I/II |
| `iron_floor_grating` | Cutout floor/walkway block | Industrial platforms and catwalks with proper collision | Iron I/II |
| `iron_plate_panel` | Thin facing-oriented panel | Machine bay walls, lighter than a full Riveted Casing | Iron I/II |
| `iron_machine_foundation` | Full foundation block | Heavy machine flooring and valid multiblock construction support | Iron I/II |
| `iron_catwalk_railing` | Connected edge railing | Safe walkways and industrial service access | Iron II |
| `iron_service_hatch` | Hinged access block | Factory service access without extra machine interface | Iron II |
| `iron_mechanical_draw_bench` | **Third proposed JSON multiblock**, consumer-gated | Draw precise wrought rods/pins through a powered, wear-limited die | Iron III |

**Not all 16 need to be built immediately.** The initial Iron material
forms and factory construction starter palette should be available through
the reused Bronze workshop, without demanding a machine to make the parts
needed to build itself. The three new multiblocks must have real,
distinct processes; the Draw Bench is not implemented unless precise
drawn stock has real consumers. General construction blocks need not
invent simulation systems to justify their usefulness.

### New Iron Stock Shear (core)

- **Tentative size:** 3×3×2 with true controller, existing mechanical module, physical item input/output, blade service position and visibly supported eccentric/crank.
- **Job:** Cut heavy bar or plate into defined length/pattern, including rivets from rods. Different from the Bronze Press (sheet forming), Boring Machine (accurate internal bores) and Lathe (turning).
- **Candidate mechanical load:** 10–12 torque at a tested valid RPM. First prove the large water wheel can actually supply it through correct Iron gearing. A wooden medium-gear route should fail the same demand.
- **Wear:** `iron_shear_blade` loses life **only after a completed batch**. No consumption during stalls or when output is blocked.
- **Real industrial outputs:** rivets, accurately cut bar/plate blanks and possibly recovered offcuts if a conservative ledger justifies them. No output without a named downstream consumer.
- **Rendering:** connected bearings and shaft, blade/slide motion linked to actual processing, real input/output visuals via JSON. No floating mechanical parts, stretched UVs or item entities.

### New Iron Forge Hearth (core, later)

- **Tentative size:** 3×3×2 or 3×3×3, **not** a final multiblock layout. Refractory hearth, charcoal fuel, HEAT buffer, controllable AIR via existing Bronze blower/duct, safe stock positions.
- **Job:** Heat **several** wrought ingots/bars/plates per batch or supply better throughput than the Refractory Kiln. The **same** physical items are returned with bounded forging-heat metadata.
- **Balance:** More fuel/air and machine-building cost for higher throughput; no free smelting, metal production or indefinite hotness.
- **Unlock rule:** The existing Refractory Kiln must be sufficient to produce the first iron components, so the Hearth cannot create a circular crafting gate.
- **Steel boundary:** Not a blast furnace, steel converter, pig-iron duplication trick or Steam boiler.

### Third multiblock and factory construction expansion

**Mechanical Draw Bench:** A long, real Gear V2-powered bench for drawing
prepared rod stock through replaceable dies. Distinct from the Rolling
Mill (rolling profiles) and Lathe (turning). **Proposed only:** require
at least two actual consumers for precisely drawn output before approving
its controller. See the
[Iron industrial factory expansion](iron-age-industrial-factory-expansion.md)
for complete process, port and validation gates.

**Industrial factory building palette:** In addition to existing planned
Frame and Casing, plan Beam, Grating, Panel, Foundation, Railing and Hatch.
The palette supports actual player-built machine halls and safe access
without custom block entities for ordinary construction. All blocks
are proposals, not registered content.

### Other conditional blocks (not included in the 16)

| Candidate | Only implement when… |
| --- | --- |
| Iron Bearing Support | A genuine shaft-span/support/wear rule needs a visible block |
| Mechanical Hoist / Gantry | Prove a distinctive, bounded lifting operation not already served by belts/arms; moving world blocks requires separate explicit approval and architecture |
| Gravity Iron Bulk Chute | Existing belts/arms cannot provide a needed bulk gravity route |
| Heavy Iron Upsetting Press | Unique thick-stock shape cannot be forged/stamped on the Bronze Hammer/Press |
| Reinforced Iron Tank | Named fluid/thermal consumer actually needs a different capacity/material |
| Iron Clutch / Pulley | Source audit demonstrates a real transmission bottleneck in the existing part |
| Iron service ladder, catwalk stairs/slabs, service door or bracing variants | A useful industrial build variant; shared modeling/recipe family and no unnecessary block entities |

**Do not build by default:** an Iron Grinder, Iron Washer, duplicate Lathe, second Rolling Mill, second Press, duplicate Boring Machine, new Copper/Steam pipe skins, or Iron-age steelmaking.

## 5. Complete production path

```text
EXISTING BRONZE EXIT
Magnetite → Stamp Mill → Washer → Sifter → Concentrate
  + Calcite + Charcoal + real HEAT and AIR → Bloomery
  → Iron Bloom + Slag
  → Refractory Kiln (hot bloom) → powered Forge Hammer
  → Wrought Iron Ingot
       └── Wrought Iron Forge Die [DONE, PLAYER-ACCEPTED]

IRON I — FORMED MATERIALS (REUSE BRONZE SHOP)
Wrought Iron Ingot → Refractory Kiln (real temporary heat)
  → Bronze Forge Hammer → Plate / Bar / Rod / Gear Blanks
  → Bronze Rolling Mill → Sheet and specified smaller profiles
  → Bronze Mechanical Press → Panel/casing blank
  → Bronze Precision Boring Machine → Bearing Housing
  → Bronze Mechanical Lathe [REQUIRES RECIPE REFACTOR]
       → precision gear / machined shaft
  → Bronze Assembly Station → installed iron gear and shaft components

IRON II — HEAVIER POWER TRANSMISSION
Large Water Wheel → Proper iron gear path + valid shaft/gearbox
  → actual >8 torque at a demanding consumer
  → Iron Stock Shear (materials cut to size + rivets)

IRON II / III / IV — FACTORY CONSTRUCTION AND HEAVY MANUFACTURING
First hot iron bars/plates → Beam + Grating + Panel + Foundation
  → player-built Iron workshop (no new heavy controller required)
Cut stock + rivets + rolled/pressed sheets → Riveted Frame + Casing
  → Iron Stock Shear (heavy stock cutting) 
  → Mechanical Draw Bench [only with useful precision-rod consumers]
  → Iron Forge Hearth (heating productivity)
  → Iron Flywheel (finite inertia / short-interruption support)
  → structural and machined material foundation for separate Steel Age
Optional Hoist/Gantry [only after world-interaction feasibility decision]
```

**Required gameplay gates:** (A) real Wrought Iron — DONE; (B) hot plate/bar/rod; (C) functional precision Iron gear through existing Lathe; (D) genuinely higher-capacity network proven under load; (E) stock cutting and structural Iron; (F) useful Hearth/inertia and Steel construction prerequisites.

Never require a machine to produce the gear that is needed to power the first machine that can make that gear.

## 6. Source-level engineering work before adding JSON recipes

The current runtime cannot be assumed to support every proposed recipe:

1. **Generalize real hot-stock behavior.** `HotBloomWorkpiece.requiresHeat()` currently recognizes output to Wrought Iron Ingot; its `isHot()` specifically checks Iron Bloom. A new plate/rod forging recipe alone would permit unsupported cold working. Design an additive reusable thermal workpiece contract with proper fuel/time/quality behavior while preserving the accepted Bloom chain.
2. **Extend the existing Lathe safely.** It presently accepts only rough Bronze gear/bearing castings and hardcodes output. Generalize genuine recipe lookup, item output and cutter suitability with a regression against all Bronze operations.
3. **Audit NBT propagation.** The Precision Boring Machine already forwards source tags. Other rolling/pressing/assembly paths must explicitly preserve approved material quality and remove/refresh heat only according to processing semantics.
4. **Existing assembly slots are authoritative.** Inspect `MechanicalAssemblyRecipe.INPUT_SLOTS` and physically available modules before selecting multi-item iron recipes.
5. **Keep resource accounting.** New processing recipes must reserve output before extracting input; handle blocked outputs, power interruption, wear, save/reload and machine break/unform.
6. **Use existing system JSON.** New multiblocks use existing JSON construction/voxel profiles, mechanical modules and typed animation scene inputs rather than another special Java animation framework.

## 7. Iron power balance and meaningful upgrades

**Test network:** Large Water Wheel (up to 16 torque, flow-dependent) → shaft → wooden medium gear (8) → real demand higher than 8. Verify failure is caused by the 8-limited path, not lack of water-wheel energy. Replace medium gear with Iron; confirm successful demand **only when all other gears and nodes permit it**. Then insert a wooden gearbox (8) to reproduce the constraint and confirm the Iron gearbox resolves that portion of the path.

Propose an **Iron medium gear capable of up to 16** as an investigation target, not an approved stat. Stronger shaft/large-gear capacities may exceed 16 for future source tiers, but that does not improve a 16-max water-wheel by itself. Preserve 8/16 tooth ratios, axes, direction, phase and overload. Iron flywheel stores finite momentum; it cannot act as an endless generator.

**Visual requirements:** Real tooth pitch and meshing clearance, bearings and supported shafts, suitable collision shape, correct rotated blockstates, natural texture UVs, smooth mechanical rotation, and performance checks from multiple camera angles.

## 8. Structural, wear, quality and ore system rules

- **Structural blocks:** Iron Frame must actually hold machine multiblocks; Casing must have meaningful machine/HEAT protection or be a clear structural input. Don't introduce dozens of decorative variants prematurely.
- **Wear:** Reuse opt-in `ReplaceableMachinePart`, as proven by the **384-cycle Bronze / 768-cycle Wrought Iron Forging Die**. Wear should follow completed work or appropriately modeled overload, never arbitrary idle degradation. The installed part's wear must be visible and repairable.
- **Quality:** Preserve established quality handling (including vanilla vs mod Copper quality). Do not invent iron grade names/math or overwrite NBT in new Iron processing. Scrap must not duplicate quality or metal yield.
- **Conditional ores from the official age register:** `graphite` only with a real bearing/lubrication/refractory consumer; `anthracite` only with proven heat/fuel difference; `sphalerite` only with legitimate zinc processing and useful Brass consumers. Hematite/Limonite remain optional differentiated Iron Bloom sources, not mandatory new ore lines.
- **Pollution:** A planned **cross-age system**, not yet implemented by this plan. Candidate emissions from Bloomery, Hearth and later coke/steel operations need separately agreed sources, environmental effects and mitigation. No invented per-tick pollution simulation.
- **Wrought iron vs Steel:** Water quenching low-carbon wrought iron does not make it hardened Steel. Steel alloys, blast furnace, carbon control and hardened cutting tools are a **Steel Age** design decision.
- **Pipes:** AIR, water and STEAM must stay separate. All-plane 45-degree variants are another task; don't turn Iron into Steam Age prematurely.
- **Studio:** Models/animations authored in separate MGC2 Studio workstream; Java exposes real item/progress state and maintains functional placeholders without duplicated renderers.
- **Multiplayer:** As expressly agreed, **ALL multiplayer-specific implementation and testing is deferred until AFTER Steam Age is finished**. This is **not** an Iron or Steel gate. Keep gameplay server-authoritative and preserve saves, but do not schedule dedicated-server/multi-client work now.

## 9. Work order and acceptance criteria

| Phase | Deliverable | Acceptance evidence | Status |
| --- | --- | --- | --- |
| **Iron 0A** | Wrought Iron Forge Die in existing Bronze Hammer | Actual item, 768 life, service / save / visuals | **DONE — player confirmed 2026-11-07** |
| Iron 0B | First hot Wrought Iron Plate via existing Kiln and Hammer; bars/rods later | Player produced a plate in-game; fuel/HEAT expiry, output NBT, yield repetitions and old-system regressions still open | MAIN PATH PLAYER-CONFIRMED — build and full regressions pending |
| Iron I-A | Hot Bar/Rod rolling + first Structural Beam/Plate Panel/Foundation | Cold Iron rejected, Copper unchanged; X/Y/Z beams, facing panels, survival recipes and native UVs | SOURCE IMPLEMENTED — build and player tests pending |
| Iron I-B | Lathe Iron blank support + Boring Housing | Existing Bronze lathe production unaffected; precision products real | PROPOSAL |
| Iron II-A | Forge / Lathe / craft the 16-torque Iron Medium Gear | Block/item/JSON/renderer source implemented; **real >8-torque consumer and Gear V2 acceptance still untested** | SOURCE IMPLEMENTED — tests pending |
| Iron II-B | Iron Shaft/Large Gear/Gearbox + early factory construction palette | Connections, ratios and overload; usable Beam, Grating, Panel, Foundation, Railing and Hatch | PROPOSAL |
| Iron III-A | Iron Stock Shear and Blade | Heavy stock/fasteners, real load, blocked output, item wear and no duplication | PROPOSAL |
| Iron III-B | Riveted Casing/Frame and **consumer-gated Mechanical Draw Bench** | Factory building works; draw bench only if distinct precise rod output is genuinely useful | PROPOSAL |
| Iron IV-A | Iron Forge Hearth, Iron Flywheel | Real hot-batch throughput/inertia difference; kiln/flywheel still useful | PROPOSAL |
| Iron IV-B | Steel Age material/infrastructure handoff | Actual wrought components prepare new technological capability; **no premature Steel recipes** | PROPOSAL |

**Definition of functional completion:** Wrought products genuinely forged with heat and power; iron components improve a demonstrable transmission bottleneck; at least one genuinely new heavy-processing operation; a cohesive survival-craftable factory palette usable for player buildings and multiblock structures; genuine distinction for any Draw Bench; reliable player-testable road into Steel; regression tests for old world/recipes/power and a recorded JDK 17 `./gradlew build`.

**Separate acceptance:** Studio-authored finished visuals/polish and all multiplayer tests **after Steam**. Do not label entire Iron Age release-ready from one demonstration.

**Change management:** This document records the full proposed age scope. Do not silently implement optional candidates, change approved other-age mechanics, merge PR #18, modify unrelated PR #17, or claim unperformed builds and gameplay tests.
