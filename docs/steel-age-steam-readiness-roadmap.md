# Steel II–V: fabrication, transmission and Steam-ready machinery

**Magneticraft2 · Forge 1.20.1 · design baseline 2026-11-08**  
**Status:** planning contract, NOT implemented gameplay. See [existing Steel full content plan](steel-age-full-content-plan.md) for materials, ore policy and first-loop invariants.  
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
