# Steel II–V: fabrication, transmission and Steam-ready machinery

**Magneticraft2 · Forge 1.20.1 · design baseline 2026-11-08**  
**Status:** ongoing roadmap; Steel I–III have player-tested gameplay slices, and Steel IV-A has a source implementation awaiting build and in-game verification. Unimplemented Steel IV/V candidates below remain plans. See [existing Steel full content plan](steel-age-full-content-plan.md) for materials, ore policy and first-loop invariants.  
**Progression:** Stone → Copper → Bronze → Iron (merged) → **Steel** → Steam → Gasoline → Electric Generator → Nuclear.

## 1. Already accepted, versus proposed

**Steel I — player-reported working:** the distinct Steel Coke Oven (Coal → Metallurgical Coke), Steel Blast Furnace (Magnetite Concentrate + Coke + Calcite → Pig Iron + Slag), Steel Refining Furnace (Pig Iron → Refined Steel + Slag), and the replaceable Steel Forge Die on the existing Forge Hammer (1,536 completed batches versus Iron 768 and Bronze 384). The report covers gameplay acceptance of the first production chain, not a full regression suite or a verified Gradle build.

**Do not reimplement:** the Iron Shaft/gears, Steel first-loop machines, Bronze Industrial Air Blower, Bronze Rolling Mill/Press/Lathe, Iron Forge Hearth, Refractory Kiln, JSON multiblock formation, physical item-port backend, material quality NBT, mechanical source/load graph or typed AIR/STEAM network foundations. Work with those components and their actual limits.

**Design boundary:** Steel makes industrial materials and constructs Steam-ready infrastructure. A live Steam Boiler or Steam Engine is a *Steam Age* machine. A generator, functional electric cable grid or Electric Motor is an *Electric Generator Age* machine, after the Gasoline milestone, unless progression is explicitly revised. Designing their interfaces now is not permission to ship them early.

## 2. Steel II — useful steel stock and tooling

| Candidate output | Existing preferred route | Specific downstream use | Status |
| --- | --- | --- | --- |
| Refined Steel Plate | Heat/forge Steel using existing machine where verified, else narrowly extend its processing contract | Boiler shell plate, heavy brackets, steel structure | CORE candidate |
| Refined Steel Bar and Rod | Reuse Rolling Mill/Forge Hammer with validated hot-stock and material strength constraints | Engine linkages, shafts, machine fittings, fasteners | CORE candidate |
| Heavy Steel Plate | Additional heavy forming pass if required by confirmed load/material limits | Pressure-vessel walls and boiler end plates | CORE candidate with defined consumer |
| Steel Shaft Stock / machined coupling | Existing Lathe and Assembly Station when capable | Heavy shaft, supported gearbox/coupling | CORE candidate |
| Steel Rivets / sealed flange fasteners | Existing Stock Shear/Press if tooling permits | Boiler flanges, construction, pressure connections | CORE candidate |
| Hardened Steel Tooling | Controlled heat treatment after steel stock exists | More durable lathe cutters, shear blades or forge tooling | CONDITIONAL: prove actual wear advantage |
| Steel Spring | Formed and heat-treated steel | Safety-valve spring, governor and clutch components | CONDITIONAL on implementing real spring-driven control |
| Steel Sheets and panels | Existing Rolling Mill / Press if capable | Construction and enclosure | CONDITIONAL: do not duplicate Iron panels |

Use pre-existing machinery **unless a measured operation justifies an upgrade**. Do not duplicate a Rolling Mill or Forge Hammer merely to use Steel textures. Any proposed heavy plate-bending operation must be distinct from rolling thickness or ordinary sheet pressing.

### Material quality and heat treatment

- Existing material-quality/provenance metadata remains authoritative; it does **not** equate to carbon composition or temper.
- Default first-loop `refined_steel_ingot` remains plain carbon Steel. Do not silently invent hardness grades, hidden chemistry percentages, or a second general-purpose ingot tier.
- **Optional** controlled heat treatment: known stock/tool piece → heat in a legitimate furnace → quench (hard, potentially brittle) → temper (toughness), only once two named consumers need the difference. Tooling should be the first candidate; ordinary pressure-vessel plates should not magically improve by quenching.
- Preserve mass equivalence, source quality and identity; temporary hot state expires by server game time. No free output by repeatedly heating or cooling an item.
- A material service life is reduced on completed machine batches, not idle ticks. Failure should have a player-visible cause.

## 3. Steel III — higher-load Mechanical Power System

| Candidate | What it improves | What it must NOT do |
| --- | --- | --- |
| Steel Shaft and bearings | Carries greater allowable mechanical torque and connects heavy engines | Generate torque |
| Steel Gear / larger gear | Stronger teeth, accurate mechanical ratios | Increase power through a gear ratio |
| Heavy Steel Gearbox | Higher torque at correctly meshed 90-degree shafts | Add motion/energy in an unloaded network |
| Reinforced Coupling | Connect separated supported shafts at rated load | Ignore shaft directions and alignments |
| Heavy Clutch / Overload Disconnect | Deliberately isolate engines/large machines | Reset overload without an explicit operating rule |

The existing `GearNetworkManager` already owns RPM, torque, source load, ratios, overload and flywheel integration. Do not create a second rotational-power graph. A water wheel remains capped by its actual source torque, even with Steel gearing. Steel parts increase *transmission capacity*, making them useful when Steam engines later deliver higher source torque.

Any new heavy machine's required RPM/torque must be demonstrated as achievable **before** Steam using current sources, or deliberately postponed to Steam. No unattainable prerequisite chain.

## 4. Steel IV — fabricate physical Steam plant components

| Candidate item/block | Purpose | Implementation rule |
| --- | --- | --- |
| Boiler Shell Section / Curved Steel Plate | Physically assembled vessel wall | Require accurately formed heavy plate; no magic full-boiler craft |
| Boiler End Plate | Seal shell ends | Distinct plate finishing; conserve material quantity |
| Pressure-rated Flange / Gasket Seat | Sealed pipe and vessel joints | Clear connection and pressure rating; do not require undeveloped rubber early |
| Steam Pipe / Reinforced Pipe Fitting | Steam-only transport, temperature/pressure capacity | Reuse existing `SteamPipeBlock` / Steam network, not AIR or liquid pipes |
| Valve Body / Isolating Valve | Manual shutoff for maintenance | Must actually block flow, including after reload |
| Spring-loaded Safety Valve | Relieve high pressure in advance of pipe rupture | Needs real downstream pressure interface and defined setpoint |
| Pressure Gauge | Diagnostic reading of server-side pressure | Must not invent an independent quantity called pressure |
| Engine Cylinder, Piston, Crank, Connecting Rod | Construction parts for first Steam Engine | Needs toleranced metalworking; engine gameplay belongs to Steam |

**Plate Bending Machine — conditional new 3D multiblock:** Steel plates → curved boiler sections. Approve it only if the existing Press/Rolling Mill cannot credibly support bending, and if boiler construction actually consumes its output. Gear V2-powered, replaceable tooling, JSON structure, visible real stock and real output slots. Do not add it as a mere decorative requirement.

Do not add an entire separate high-temperature furnace just to shape Steel if the Iron Forge Hearth or Refractory Kiln can be correctly extended using their existing HEAT and processing contracts.

## 5. Steel V — measured handoff to Steam Age

The first Steam implementation should use **steel-built** parts, but keep these roles separate:

1. **Boiler:** consumes actual water + combustion fuel/HEAT; introduces mass and thermal energy to the existing typed STEAM reservoir. A hot pipe cannot create steam by itself.
2. **Steam pipes and valves:** draw from the already existing `SteamNetworkManager` / `SteamNetworkSavedData` and `IPressurizedGasHandler` semantics. No STEAM↔AIR mix, no free condensed steam, no per-pipe ticking BE.
3. **Engine:** consumes actual delivered steam under valid pressure/temperature difference and produces **mechanical** torque/RPM through the existing Mechanical Power System. Exhaust/condensate policies must be chosen before completion.
4. **Protection:** safety valves and pressure gauge address overpressure. Preserve the existing decision: intact pipes do not suffer random wear; an eligible pipe ruptures from real overpressure, produces one persistent vent per connected component and loses conserved steam.
5. **Future electrical conversion:** generator is an explicitly loaded mechanical *consumer* connected to an AC grid when unlocked. It does not belong in the Steel/Steam first delivery even though the interface can be designed now.

Do not claim that current Steam pipe code is a finished engineering simulator. Existing pressure law, flow resistance, thermal rules, 256-pipe traversal bound and rupture threshold remain provisional. Validate and calibrate before making permanent machine/pipe ratings.

## 6. Non-circular progression test

```text
Bronze/Iron workshop + accepted Steel I
    -> Refined Steel stock (plate/bar/rod)
    -> Higher-load Mechanical Power transmission
    -> Boiler shell + valve + flange + pipe parts
    -> [Steam Age] live Boiler + Steam Network + Steam Engine
    -> High-torque mechanical workshop
    -> [Gasoline Age] combustion-based alternative engine
    -> [Electric Generator Age] loaded generator -> AC distribution -> motors/machines
```

- No machine needs the output of itself to construct or power itself.
- The first Steel stock must remain producible on actual pre-Steam mechanical resources.
- Steel parts may be used in Steam infrastructure, but no Steam, powered generator or electric tool is required to craft the first Boiler/Engine.
- Existing Bloomery/Wrought Iron products, machines and quality tags stay useful and save-compatible.
- Existing Steam pipe resource IDs and rupture behavior are retained, subject to audited targeted changes.

## 7. Milestones, scope and acceptance gates

| Milestone | Implementation focus | Approval evidence |
| --- | --- | --- |
| Steel I | Coke → Pig Iron → Refined Steel → Steel Die | **Player-reported working**; regressions/build remain separate |
| Steel II | Basic processed stock, meaningful steel tool | Mass/quality checks, existing-machine compatibility, actual survival recipe |
| Steel III | High-load shaft/gear/coupling upgrade | RPM/torque, ratios, jam and overload tests; no source power creation |
| Steel IV | Boiler-ready shell, flanges, valve and gauge components | Complete non-circular Steel manufacturing chain |
| Steel V | Steam Age handoff accepted | Build/JEI/Patchouli, Steam pipe identity, safety architecture documented |
| Steam 0 | Boiler + network + engine vertical slice | Verified water/fuel/thermal/steam/mechanical conservation, physical safety and persistence |

Every implemented slice needs: `./gradlew build` (JDK 17), JSON validation, item and mass accounting, source/output blockage, load tests, break/unform, save/reload, formation and directional interaction, native-UV MGC2 Studio visual inspection, and an explicit in-game regression checklist. Dedicated multiplayer-specific work remains deferred until after Steam Age, but code must already be server-authoritative.

### Deliberately unresolved decisions

- Whether a physically distinct Plate Bending Machine is needed; inspect existing machines first.
- Exact Steel materials/tool wear values and which hardened-tool consumers justify heat treatment.
- Safe boiler pressure, water/steam rate, exhaust/condensation, safety valve response and mechanical Steam output balance.
- The later Electric Generator Age's AC/DC policy and voltage numbers: see [electrical design contract](electrical-power-network-design-contract.md), which uses AC-first as a **provisional design assumption**, not an approved balance value.

### Steel II player acceptance — 2026-11-08

**Player report:** The newly added Steel II stock-working path works in-game. This accepts the Steel II playable-production milestone: existing Iron Forge Hearth reheating, hot Steel Plate forging using the Steel Die, Steel Roller Sleeves upgrade, and hot Steel Bar/Rod rolling. The report is player test evidence, not an independently recorded full ForgeGradle build, exhaustive regression suite, or dedicated multiplayer test. Do not change the existing model-work ownership (MGC2 Studio).

**Next targeted milestone:** Steel III — heavy-load shafts/gears/transmission. Inspect actual existing Iron mechanical blocks, their torque limits and recipe/renderer systems before changing source. Design only the smallest upgrades that create a useful distinction for future Steam engine loads. The AC electrical grid belongs to Electric Generator Age and remains design-only.

### Steel II implementation candidate — 2026-11-08

This pass adds **source-implemented** Refined Steel Plate, Bar, Rod and Steel Roller Sleeves using the existing Iron Forge Hearth, Bronze Forge Hammer, and Bronze Rolling Mill. The ingot and bar have client-side hot/cold tooltips backed by the same server-time tag. The Steel Hearth recipe lookup caches its machine ID rather than allocating a new ID each tick. This is not yet player-verified, built or Steam Age complete. The first Coke/Pig Iron/Steel/Die loop remains player accepted.

- Four Steel Ingots/Steel Bars are reheated in the existing 4-slot-batch Forge Hearth using a real solid-fuel firebox, with a distinct configurable minimum Steel HEAT threshold (default 18,000 abstract HEAT; max against the existing wrought threshold). Existing refractory kiln cannot silently forge Steel. Metallurgical Coke is accepted as 3,200 burn ticks. No added gas/pressure/electricity.
- A hot Steel Ingot + an installed Steel Forge Die + 20 RPM / 10 torque → one Refined Steel Plate (200 powered ticks). The existing Forge Hammer maintenance contract enforces the installed tier; older Bronze/Iron outputs still work with their existing dies.
- 4 Steel Plates + 1 existing Wrought Iron Roller Sleeves → Steel Roller Sleeves. This Steel tier is an optional installed maintenance item in the existing Rolling Mill, with 2,048 completed batches (Bronze 512, Iron 1,024). No new machine multiblock.
- Hot Steel Ingot → 1 Bar (220 powered ticks) and hot Steel Bar → 4 Rods (240 powered ticks), each at 16 RPM / 10 torque, using installed Steel sleeves. The Hearth can reheat 4 bars; rod pieces are quarter-ingot stock. Quality/NBT is transferred by the existing HotBloomWorkpiece.consolidate helper with heat metadata removed on finished product.
- Four registered item models initially reuse existing Wrought Iron pixel-perfect geometry and dark worked-iron material texture as explicit MGC2 Studio placeholders; no new geometry/texture artwork is claimed.
- All recipes use the existing data-driven `multiblock_processing` or vanilla shaped recipe formats; the old IDs, save data, ports, machinery and acceptance tests are untouched. Steel HEAT defaults and tooling lifetimes remain provisional balance values.

**Regression:** reheating stalls on insufficient HEAT or full output; 4 inputs → 4 hot outputs; hot expires using server world time; blank Steel Die/old Roller Sleeves block new Steel-only work; Steel upgrades allow processing; correct RPM, torque, source overload and wear; input top-ups, output blocking and save/reload do not duplicate metal; total one ingot equivalent per plate or bar, 4 rods per bar; Copper/Wrought Iron work exactly as before. Run `./gradlew build` with JDK 17 and in-game checks before reporting completion.

### Steel III first transmission slice — 2026-11-08

**Source implemented, NOT built/runtime verified:** Steel Medium Gear (8 teeth, 32 game-torque), Steel Large Gear (16 teeth, 48), Steel Shaft (axial 48), Steel Gearbox (two-port, reversible 1:1 right-angle, 32). These block classes reuse the existing Iron placement/collision behavior. Their BEs reuse Copper's proven Gear V2 nodes and override only `getGearMaxTorque()`. No new mechanical network or generator has been added.

- Four distinct Steel blocks/BlockEntities/items, shaped recipes, pickaxe tags, self-drops, localized names and Patchouli crafting pages; recipes use existing Steel II Plates/Bars, Iron parts and Bronze precision bearings. Their recipes intentionally form an upgrade path; no Steel III machine is needed to make the first Steel.
- Static block/item models inherit actual Iron models and UVs, rather than copying or stretching geometry. The shared Gear V2 client renderers are delegated to. Final visual modeling remains MGC2 Studio's responsibility.
- Steel Medium↔Large gearing remains 8:16. Gearbox direction and ratio, shaft alignment, phase, source load, clutch/disconnect and overload evaluation remain unchanged.
- **Limitation:** the existing Large Water Wheel's up-to-16 torque cannot prove the 32/48 Steel headroom in-game. Real higher-load tests require the first Steam Engine or another approved strong source; a Steel node creates no torque.
- **Deferred:** Steel high-load clutch, coupling, brake and optional gearbox customization. Inspect the present clutch/protection implementations and demonstrate actual bottlenecks before adding duplicates.
- **Quality:** vanilla shaped crafting does not carry input item provenance/quality NBT; do not claim such transfer without a dedicated recipe system.

**In-game regression checklist:** craft four variants from actual Steel II stock; test all shaft/gear axes, 8:16 mesh and ratio, all gearbox port orientations and direction; inspect rotating/stopped BER appearance at multiple camera angles and after F3+T; connect wooden/Iron bottlenecks and verify overload remains; break/drop/reconnect, save/reload and chunk boundaries; check Copper/Bronze/Iron old mechanical machines unchanged. On Steam Age integration, test >16 torque source throughput, 32/48 ceilings, aggregation, clamp/stall, protective disconnect and regeneration prevention. Run `./gradlew build` under JDK 17 and the contract checker before accepting source as verified.

### Steel III visual mesh blocker — 2026-11-09

The player's screenshot confirms a **visible gap between two Steel Large Gear tooth rings**, despite working rotation. Inspection of the inherited Iron large-gear cuboids confirms outer tooth radius ≈15.2 model pixels versus 32-pixel large/large axle separation; naïve tooth extension risks intersecting the existing diagonal medium/large mesh. This is **not fixed** and Steel III visuals remain **unaccepted**. See [gear-meshing visual regression and MGC2 Studio model contract](steel-age-gear-meshing-visual-regression.md) for the measured gap, constraints and full in-game acceptance checklist. No gear position, power graph, UV or other gameplay rule was changed in this documentation pass.

### 2026-11-09 Steel III visual correction follow-up

The large/large visible tooth gap has received a **real shared baked-model tooth extension** (Iron Large Gear model, inherited by Steel), with per-face proportional UVs and no renderer or Gear V2 code change. This is an unverified candidate; medium/large diagonal meshing is a known possible visual regression. Review [gear mesh visual acceptance](steel-age-gear-meshing-visual-regression.md) and require in-game evidence before acceptance.


### Steel IV-A — Heavy Plate and boiler-shell forming (source implementation, 2026-11-09)

**Scope:** exactly the first Steel IV fabrication chain. This reuses the **existing Iron Forge Hearth, Bronze Forge Hammer and Bronze Mechanical Press**, which are already JSON multiblocks. No Plate Bending Machine, Boiler controller, Steam Engine or new pressure system was registered.

| Operation | Machine and real inputs | Valid output | Gate |
| --- | --- | --- | --- |
| Steel Bars → Hot Steel Bars | Existing Iron Forge Hearth: four identical bars, solid fuel + higher Steel HEAT | Same four temporarily hot bars | Steel reheat contract already accepted |
| Hot Bars → Heavy Steel Plate | Existing powered Forge Hammer: four hot `refined_steel_bar` | One `heavy_steel_plate` | Steel Forge Die, 20 RPM, 12 torque, 320 supplied ticks |
| Craft Steel Press Die | Vanilla shaped recipe: four `refined_steel_plate` and one existing `bronze_press_die` | One `steel_press_die` | No machine added, die is a service upgrade |
| Heavy Plate → Curved shell | Existing powered Mechanical Press: one `heavy_steel_plate` | One `boiler_shell_section` | Installed Steel Press Die, 18 RPM, 14 torque, 240 supplied ticks |

**Mass ledger:** one Steel Bar = one ingot-equivalent; four hot Bars = one four-ingot-equivalent Heavy Plate = one four-ingot-equivalent Shell Section. The tooling recipe consumes additional four Steel Plates in a reusable Steel Press Die (1,536 completed Press batches, versus 384 for Bronze). No material is duplicated by reheating, forging, or pressing. The heat expires by server game time; the Forge Hammer requires actual hot bars throughout a batch, and cannot work them cold.

**Quality and persistence:** the Forge Hammer already uses `HotBloomWorkpiece.consolidate()` to transfer the input bar's quality/provenance into the finished Heavy Plate while removing the expiring hot-tag. The Mechanical Press now does the same for this specific one-input/one-output shell-forming recipe and reserves the *tagged* output before consuming an input. Existing Copper Panel recipes still use their unchanged output flow. The Steel Press Die extends the existing `ReplaceableMachinePart` with stable item-ID NBT; old Press saves remain Bronze with their saved wear. Worn, absent or wrong-tier dies block new batches. Manual status feedback identifies the missing Steel Die.

**Non-circular balance:** the two new mechanical operations request at most 14 torque, within an existing maximum 16-torque water-wheel source before Steam, but an inadequate wooden or bronze drivetrain can still overload under real Gear V2 load. The 40-RPM handled Large Gear has only 8 torque and therefore cannot directly power these Steel IV forming operations. This is intentional and rewards stronger transmission.

**Visual ownership:** `heavy_steel_plate`, `boiler_shell_section` and `steel_press_die` use existing valid Steel item-model parents as **explicit temporary placeholders**, not final curved steel geometry. MGC2 Studio owns their final item/formed-workpiece visual identity; no machine multiblock structure was changed, so existing Patchouli build visualizations remain correct.

**Verification stage:** source implementation; static JSON/source contract checks and machine capacity/recipe review are not equivalent to `./gradlew build` or in-game acceptance. Next gate: execute `node tools/steel-age/check-steel-boiler-fabrication.mjs` and `./gradlew build` using a full JDK 17 checkout, then do the following in-game checks:

1. Forge four hot Steel Bars into one Heavy Plate, preserving a tagged bar's quality and removing the hot timeout from the output; reject cold bars, three bars, Bronze/Iron dies, blocked output and missing mechanical supply.
2. Verify available 20 RPM/12 torque and output reservation across save/reload, reload while mid-batch, and hot-stock expiry; confirm wear decreases by exactly one only on completed batches.
3. Craft Steel Press Die; install in the **same** formed Press and confirm its wear, status and save/reload. A Bronze die must still press Copper Panels but **not** a Boiler Shell; a Steel die must do both.
4. Press one Heavy Plate into one Shell at 18 RPM/14 torque, preserving its material quality; test output full, interrupted power and zero-load idle state. One Plate must never produce multiple Shell Sections.
5. Verify the JEI generic multiblock recipes and Patchouli pages, break/unform/ports, each formed facing, mixed Steel/Iron mechanical power networks and unobstructed item transfers.
6. Inspect temporary item models in inventory and world from multiple camera angles; delegate any finished curved section model to MGC2 Studio, with native UVs and no floating geometry.

**This is fabrication only:** Boiler Shell Section has no operational STEAM, WATER, HEAT, pressure, boiler formation, fuel consumption or engine power. Actual functioning Boiler/Engine remains Steam Age, after subsequent Steel IV component work.


### Steel IV-B — Boiler closure and pressure-fitting stock (source implemented, 2026-11-09)

The player confirmed Steel IV-A (hot Steel Bars → Heavy Plate → Boiler Shell Section) works in-game. Steel IV-B adds three further **static** fabrication items and upgrades the existing Boring Machine. Do not interpret these as a boiler, working valves or a new pressure network.

| Output item | Existing powered workshop | Input | Tool | Mechanical load |
| --- | --- | --- | --- | --- |
| `boiler_end_plate` | Bronze Precision Boring Machine | 1 Heavy Steel Plate (4 ingot-equivalents) | Steel Boring Bit | 20 RPM / 12 torque / 300 ticks |
| `boiler_reinforcement_flange` | Bronze Mechanical Press | 2 Refined Steel Plates (2 ingot-equivalents) | Steel Press Die | 18 RPM / 14 torque / 240 ticks |
| `pressure_pipe_coupling` | Bronze Precision Boring Machine | 1 Refined Steel Bar (1 ingot-equivalent) | Steel Boring Bit | 20 RPM / 12 torque / 260 ticks |

A Steel Boring Bit is crafted by upgrading the existing Abrasive Boring Bit with **three Refined Steel Plates and one Steel Rod**. It lasts 1,536 successful boring batches; the existing Abrasive Bit lasts 384. The bit is installed with the existing `ReplaceableMachinePart` interaction and persisted by registered item ID, not an unstable index. The original Bronze/Iron boring recipes, their quality behavior, wear, saves and UI remain on their established paths.

**Process semantics:** the single-input `multiblock_processing` recipe type already supports these recipes; no new serializer/network packet or machine BE is needed. The existing Press now recognizes flanges as another item requiring the Steel Press Die and preserves quality/provenance through a tagged-output reservation. The Boring Machine checks actual installed Steel Boring Bit for End Plates/Couplings on every supplied tick; those outputs preserve input quality but strip a temporary `hot_until` property. Other boring recipes continue copying their input NBT as before. Neither process consumes workpieces or wears tools when output is blocked or mechanical power is absent.

**Material accounting:** end plate consumes one Heavy Plate (four bars/ingots); flange consumes two ordinary Steel Plates (two ingot-equivalents); coupling consumes one Steel Bar (one ingot-equivalent). The upgraded bit is tooling, not extra output material. No fake duplication, hidden pressure storage, steam, or water functionality.

**Visual status:** the player noted that several existing Steel items look alike and hot/cold state is not clearly represented. All new item models reuse existing valid Magneticraft2 shapes (Heavy Plate, Iron Bearing Housing, Steel Shaft, Abrasive Bit) **only as placeholders**. This issue remains open for MGC2 Studio to produce original recognizable models and appropriate heated-stock visuals, without stretching textures or disrupting save/processing code.

**Acceptance:** run `node tools/steel-age/check-steel-boiler-hardware.mjs` in a full checkout and `./gradlew build` with JDK 17; then conduct the following in-game checks:
1. With a Steel Boring Bit, machine 1 Heavy Plate → 1 End Plate, verify 4-ingot-equivalent conservation and quality-tag transfer; with only Abrasive Bit, verify process refuses without consuming/wearing.
2. Machine 1 cold or still-hot Steel Bar → 1 Pipe Coupling; ensure the output keeps quality but removes `hot_until`. Verify output-full, tool expiry and source overload conditions.
3. With Steel Press Die, press 2 Refined Steel Plates → 1 Reinforcement Flange; with Bronze Die, ensure no processing. Both variants should still press Copper Panels.
4. Save/reload every upgraded machine after part installation and during a batch; progress, input stock, installed tool IDs and remaining wear must persist appropriately. No duplicate output on chunk reload.
5. Confirm JEI's existing multiblock processing category lists the three recipes and Patchouli shows how to craft/install the Steel Boring Bit. Inspect material models in inventory, world and held state from multiple camera angles.
6. Re-test Iron Bearing Housing, Precision Toolholder, Boiler Shell Section, Copper Panels, Gear V2 load and material-quality rules. Multi-client and performance measurements remain unverified.

**Stage:** source implementation and static inspection, not compiled or accepted in Minecraft. Working pressurized components, end-plate joints, leak safety, boiler assembly and Steam engines are separate future milestones.


### Steel IV-C — vessel assembly design started (2026-11-09)

The player reported successfully producing all three Steel IV-B hardware items in-game. This accepts their *basic production paths* only; tooling, wear, quality, blocked outputs, persistence and compilation remain separately unverified. The player also encountered ambiguity between ordinary Refined Steel Plates and Heavy Steel Plates because the placeholder item models look alike. Distinct inventory-scale MGC2 Studio visual identity is a gameplay acceptance requirement before calling the full Steel IV experience complete.

**Design contract committed:** [Steel IV-C boiler vessel assembly](steel-iv-c-boiler-vessel-assembly-contract.md). The existing IV-B outputs are items, not world-placeable vessel blocks. Steel IV-C must establish a legitimate part-to-structure manufacturing route using existing mechanical assembly and JSON multiblock conventions; the 3×3 cross-section / 3–5-block-length horizontal vessel is preliminary and intentionally not implemented as an arbitrary final blueprint. Shell/end-cap/flange/coupling counts and faithful module geometry require resolution before registering structural blocks. No working boiler, fluid/steam port, fuel burner, operating valve, pressure law or Steam engine is authorized by this documentation. **State: documented design only, not implemented or built.**


### Steel IV-C structural modules — first source slice (2026-11-09)

The existing Bronze Mechanical Assembly Station now has two additional real four-slot recipes: one Boiler Shell Section + one Steel Rod + four Iron Rivets + one Riveted Iron Frame -> one placeable Boiler Shell Panel; one Boiler End Plate + one Flange + one Coupling + four Iron Rivets -> one placeable Boiler End Joint. The recipe requires 18 RPM, 10 torque, 320 powered ticks. Both blocks have horizontal facings, native-UV baked preliminary models, matching physical selection/collision, loot, pickaxe tags and book text. They contain **no Steam mechanics**. See [the Steel IV-C contract](steel-iv-c-boiler-vessel-assembly-contract.md). Source implementation is **partial**: no finished vessel/controller/JSON multiblock or quality-aware assembled pressure boundary yet, and no Gradle build or Minecraft player verification. Do not claim IV-C is complete.


### Steel IV-C formed vessel structure candidate — 2026-11-09

A 3×4×5 **static** horizontal Steel vessel has source implementation with four JSON blueprints, one native-UV baked model palette and Voxel V2 geometry; recipe, controller, Patchouli Visualize, stable IDs, drops and registry entries are included. Exact structure requires 24 Shell Panels, 2 End Joints, 16 Iron Plate Panels, 10 Foundations and 1 Controller. The inner drum remains air. It does not generate, store or transport water/steam/heat/pressure, and assembled quality is not preserved. Detailed constraints and remaining tests are in [Steel IV-C design/implementation](steel-iv-c-boiler-vessel-assembly-contract.md). Forge compilation and in-game verification remain **unverified**.


### Steel IV-C acceptance gate — 2026-11-10

The user has produced the IV-B hardware and both assembled IV-C structural parts in-game, has shown the **formed** 3×4×5 vessel, and accepts the current visual design and book preview for now. The final visual/preview polishing is explicitly deferred rather than silently treated as fully polished. Boiler water, Steam, pressure, combustion and safety remain outside the Steel Age implementation.

A focused lifecycle verification contract and a reproducible manual regression matrix are now recorded in [Steel IV-C vessel acceptance](steel-iv-c-boiler-vessel-acceptance.md), with static Node contract checker `tools/steel-age/check-steel-boiler-vessel-lifecycle.mjs`. Direct live GitHub inspection confirmed all four blueprints have 24 Shell Panels, 2 End Joints, 16 Iron Plate Panels, 10 Iron Machine Foundations, 1 Controller and 7 air cells; source lifecycle/persistence assertions also passed. **This is not proof of in-game teardown**, and the Node checker itself has not been executed on a checkout. Gradle/JDK17 build is still blocked in the current environment due to unavailable GitHub DNS access. Required next results: user-run compilation; formation-negative cases; 4 facings; exact-once drops on filler and controller break (53 original block items); save/reload and chunk-boundary persistence; creative cleanup; two-client test if available; and Copper/Iron/Steel regression. PR #19 remains draft and unmerged. No lifecycle code was modified without a demonstrated failure.


### Steel V -> Steam 0 architecture handoff — 2026-11-10

A source-grounded integration contract has been added at [Steam 0 boiler, water, heat and STEAM interface](steam-age-boiler-network-integration-contract.md). It reuses the existing Steel IV-C boiler geometry and construction, the Forge-water pump and water-pipe network, `IHeatStorage` where appropriate, the medium-specific `IPressurizedGasHandler`, existing sparse `SteamNetworkManager` conservation/rupture infrastructure and the original mechanical torque/RPM system. The proposed Steam 0-A -> 0-D staging separates inert physical fittings, water/heat accounting, controlled steam production with safety, and the first load-bearing Steam Engine. No source code, block geometry, boiler pressure numbers, recipes or operating gameplay were implemented in this documentation pass. The operating pressure, water/steam conversion factor, enthalpy, safety response and exhaust/condensation policy remain decisions, not fabricated constants. The draft Steel PR remains open and unmerged; Steam 0 work must not be mistaken for completed Steel IV-C verification or a successful Gradle/in-game runtime test.


### Steam 0-A inert service fittings — first source slice (2026-11-10)

Four registered, inert, orientation-aware boiler fittings were added **externally** to the visually accepted Steel IV-C vessel: `boiler_water_inlet`, `boiler_steam_outlet`, `boiler_firebox_mount`, and `boiler_safety_mount`. The existing Bronze Mechanical Assembly Station fabricates all four with Steel/Iron hardware at 18 RPM, 10 torque and 320 powered ticks. Each uses one placeable block ID, four native-UV model variants, static collision, pickaxe mining, exact-one-item loot, Creative entry and Patchouli instructions. Placement is gated on the existing *formed* vessel controller and contact with an actual multiblock filler in the expected canonical boiler position. The 53-block boiler formation layout, models, quality metadata and Steam Network machinery are unchanged. The fittings have **no BlockEntity or water/heat/STEAM capability**, cannot run as a boiler, do not simulate safety relief, and remain separately owned if the vessel is dismantled. See [Steam 0-A service fittings](steam0a-boiler-service-fittings.md) and [Steam 0 boiler interface](steam-age-boiler-network-integration-contract.md). Four recipes and 16 model assets passed focused static inspection; **no Java compilation, in-game geometry acceptance, or actual 2-client testing was performed**. Do not confuse this first source slice with finished Steam 0-A or Steam 0-B/C/D.


### Steam 0-A fired-boiler identity visual pass — 2026-11-10

The player accepted a reference design of a visibly fired, riveted horizontal boiler after in-game feedback identified the assembled barrel plus bare fittings as reading like a **Steam receiver/tank**. That design is now represented by actual once-baked Minecraft block models on the draft Steel PR: a full front firebox with unlit stoking grate and ash access, a **fifth** independently crafted and supported `boiler_flue_chimney` directly over the firebox, a larger top Steam takeoff/dome, a top gauge/relief hardware cluster, and a flanged lower feedwater connection. All five fitting shapes are derived from a single static set of cuboids for models and collision, with native-size UVs. The original visually accepted Steel IV-C vessel structure, V2 shapes, original 53 required physical blocks, recipes and renderer are preserved unchanged. The model includes no automatic fire, smoke, HEAT, WATER, pressurized steam, true gauge reading or working valve: that functionality still belongs to later implementation. Boiler fittings remain independently mined/placed, outside the old multiblock footprint; chimney placement requires a matching firebox beneath it. See [Steam 0-A service fittings](steam0a-boiler-service-fittings.md). This is **source implementation**, not an in-game visual/build acceptance; `./gradlew build` and regression tests are still required. No Steam 0-B/C work was enabled.
