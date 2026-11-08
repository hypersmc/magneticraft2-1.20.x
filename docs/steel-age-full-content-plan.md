# Steel Age — content and progression plan

**Magneticraft2 / Forge 1.20.1** · **2026-11-08**  
**State:** design and implementation gates, **not gameplay source implemented**.  
**Stacked on:** Iron Age draft PR #18 (`feature/iron-age-foundations`).  
**Age order:** Stone → Copper → Bronze → Iron → **Steel** → Steam → Gasoline → Electric Generator → Nuclear.

## 1. Established direction

The earlier Steel plan specified **Coke Oven → Blast Furnace → Steel Refining
Furnace**, followed by larger-scale rolling/pressing and infrastructure
that makes a later Steam Age feasible. A Refractory Kiln and Industrial
Air Blower were listed in that historical plan but **already exist in the
Bronze/Iron workshop**; do not register clones just to repeat the list.
Iron's Forge Hearth is a 4-piece reheater, **not** a Steel converter.

Steel's distinction from wrought Iron is **controlled carbon and hotter
metallurgy**, with a real upgrading sequence. It must not consist of an
Iron Ingot + Coal crafting recipe or an arbitrary “steel = faster Iron”
tier. Iron's rods, sheets, shafts, gearboxes, flywheel, housings, rivets,
frames and casings must remain useful for building the first Steel plant.
Do not add a machine only to meet a numerical content quota.

## 2. Actual inherited source capabilities (checked on the Iron branch)

| Existing system | Can reuse | Cannot pretend it already does |
| --- | --- | --- |
| Magnetite → Stamp Mill → Washer → Sifter | Supplies prepared Iron concentrate | Does not automatically make pig iron or Steel |
| Bronze Bloomery with Calcite, charcoal, fueled HEAT and sustained AIR | Proven staged iron reduction pattern and actual slag output | Existing output is **Iron Bloom**, not pig iron |
| Charcoal Retort | Real separate fuel and volatile/byproduct handling, including Wood Tar fluid | A Coke Oven is **coal carbonization**, not a renamed log-to-charcoal recipe; Wood Tar is not Coal Tar |
| Refractory Kiln and Iron Forge Hearth | Current finite reheating/HEAT and NBT quality conventions | Cannot melt/refine Steel by merely changing a JSON output |
| Bronze Industrial Air Blower + typed AIR ducts | Existing forced-combustion infrastructure | STEAM is a separate gas medium; AIR is not electricity or a free heat source |
| Forge Hammer, Rolling Mill, Press, Lathe, Boring Machine | Mechanical service/wear, item handling, forging and machining patterns | Their existing recipes and tools are **not** proved suitable for molten or hardened Steel |
| Wrought Iron mechanical network | Real gearing/torque/inertia and Steel-era construction stock | Large Water Wheel supplies up to **16** torque only; stronger shafts do not create extra power |
| JSON multiblocks + voxel V2 + resource-pack animations | Structure, ports, persistent collision, visuals, JEI/Patchouli conventions | Do not bypass measured port/inventory limits or render unsupported parts |

## 3. First complete production loop (required before standalone items ship)

```text
Existing Iron factory + Bronze foundry infrastructure
  + ordinary Coal + real fuel/HEAT
      → Steel Coke Oven (sealed carbonization)
      → Metallurgical Coke (actual consumable, not ordinary charcoal)
          │
Prepared Magnetite Concentrate (existing ore-dressing line)
  + Calcite flux + Metallurgical Coke
  + high-temperature HEAT + sustained typed AIR
      → Steel Blast Furnace → Pig Iron + Slag
                                  │
               Pig Iron + oxidizing/refining conditions
               + fuel/HEAT + controlled AIR/slag removal
                    → Steel Refining Furnace
                    → Refined Plain-Carbon Steel stock + refining slag
                    → first Steel tool/structural consumer
```

**No output of a new Coke or Pig Iron item is considered a completed
playable milestone until the *next* consumer is actually connected.**
The full loop must be possible without Steel parts in the construction
recipes of machines that make first Steel. Keep the existing Bloomery
→ Wrought Iron path fully available.

**Carbon meaning:** Pig Iron is high-carbon furnace iron that needs
refining. Refined Steel must pass a process that removes/controls carbon;
merely crafting pig iron with wrought Iron is not refining. Initial
plain-carbon Steel is sufficient: **do not invent multiple carbon grades,
composition percentages or permanent quality tiers until a concrete
metadata/recipe contract exists**. Retain existing material-quality
tags and origin where relevant, but do not promise material-grade
mechanics unsupported by the current API.

## 4. Prioritized planned machines and components

| Content | Stage | Distinct purpose | Policy |
| --- | --- | --- | --- |
| **Coke Oven** | Foundation | Coal → metallurgical coke via sealed fuel-consuming carbonization | CORE; one new distinct JSON multiblock, no Wood Tar substitution |
| **Blast Furnace** | First Steel production | Concentrate + coke + flux + HEAT/AIR → pig iron + real slag | CORE; not a second Bloomery with its ID swapped |
| **Steel Refining Furnace / Converter** | First Steel production | Real refining/decarburization of pig iron → Steel + slag | CORE; controlled atmosphere/heat needed |
| **First useful Steel tooling** | Complete the first loop | A genuine higher-temperature/hardened cutting or forging component | CORE only alongside first Steel, and with practical old-vs-new effect |
| **Steel-appropriate Rolling/Finishing capability** | After first Steel | Sheets, plates, stock whose temperature/strength older Mill cannot process | CONDITIONAL: extend old Mill if credible; new heavy Mill only with a measured limit |
| **Heavy Mechanical Press** | After first Steel | New shapes/load not handled by Bronze Press or Iron Forge Hammer | CONDITIONAL; prove power available before inventing torque demands |
| **Heat-Treatment Furnace** | After first Steel | Controlled hardening/tempering of steel tooling | CONDITIONAL until tool/wear/heat differences exist |
| **Steel industrial framing / pressure vessel parts** | Steam handoff | Pressure-rated tanks, validated valves/fittings, stronger high-heat structures | CORE *eventually* for safe Steam; not boilers or engines |
| **Steel pipes/valves** | Steam handoff | Material ratings and isolation for existing typed STEAM-only architecture | Integrate prior steam-pipe work, do not fork a second network |
| **Riveting Machine** | Optional | Only if faster/repeatable production differs from current Shear + Forge Hammer | CONDITIONAL, otherwise omit |
| **Upgraded Industrial Air Blower** | Optional | Only if actual furnace AIR demand exceeds existing blower/network | CONDITIONAL, not an arbitrary extra source |
| **Steel specialty alloys** | Later Steel / later ages | Named useful strength/heat/wear applications | CONDITIONAL, not a baseline prerequisite |

This is a **candidate register**, not approval for automatic implementation
of every machine. The Steam boiler, piston engine, steam power generation
and working boiler-pressure gameplay belong to **Steam Age**, not Steel.

## 5. Ore and fuel policy (preserve source-of-truth age register)

- First Steel requires **existing Magnetite** concentrate, **vanilla coal**
  for Coke, and **existing Calcite** flux. Do **not** gate first Steel on
  newly added ore deposits, Manganese, Chromite, Nickel or Diamond.
- `manganese`, `garnierite` and `kimberlite` are **conditional** Steel
  candidates in `docs/ore-processing-by-age.json`, only to be activated
  with an actual alloy or abrasive consumer. `chromite` is currently
  placed in a *later* electrical/advanced materials stage unless a
  separate earlier process is justified.
- Existing `graphite` and `anthracite` are *conditional Iron* candidates,
  not compulsory Coke or Steel inputs; Coke production can use Coal.
- No new ore blocks, changed worldgen/harvest rules, duplicated slag,
  inflated metal yields or mandatory universal processing steps.
- Keep Coke/Coal/Charcoal distinct: they can share a Minecraft item
  category but not silently share one efficiency or output everywhere.

## 6. Non-circular construction and energy boundaries

- Coke Oven is built from existing Iron/Bronze/Refractory materials,
  and its startup fuel cannot require its own Coke.
- Blast Furnace is assembled from existing Iron parts and
  pre-Steel refractory materials. Coke must be manufacturable first.
- Refining Furnace is built before any Steel recipe or high-tier Steel
  tool becomes mandatory; it may use pig iron and a controlled AIR path.
- First Steel can become tooling or structural input via a deliberately
  tested hot-working path; it does **not** require the later Heavy Mill.
- High-load Steel rolling cannot demand >16 input torque from
  a 16-max Water Wheel without introducing an approved stronger
  *pre-Steam* source or a correctly demonstrated reduction ratio.
  Gear V2 preserves power: ratios do not create mechanical energy.
- Real AIR and STEAM networks remain typed and separate. Existing
  pipe-rupture design is preserved. Don't substitute fluid tanks
  for pressure gas or invent a Steam boiler now.

## 7. Material ledger and quality

For *each* industrial Steel recipe record input metal units, carbon
source/fuel, flux, solid waste, fluid/gas outputs if represented, and
net finished metal amount. Avoid free mass via recycling, output-slot
mismatch, or repeated workpiece heating. Steel's chemistry implies
carbon and impurity removal, but do not claim HEAT units are Celsius or
numerically meaningful carbon composition until the physics model is
approved. Quality/provenance NBT must be copied deliberately, temporary
hot-state metadata must expire server-side, and assembly crafting
must not silently promise inherited NBT.

## 8. Deferred / separate work

- Mechanical Draw Bench stays conditional until *two* real precise
  rod/wire consumers; do not force it into Steel just because Iron
  reached 15/16 planned registered blocks.
- Industrial pollution is an intended **cross-age** system. Record
  candidate Coke/Blast/Refining sources, but no hidden per-tick
  pollution generation without its approved architecture.
- Specialized alloys, moving cranes, unlimited output automation,
  high-temperature casting and production of electrical power
  are separately gated.
- Multiplayer **implementation and dedicated tests are deferred until
  after Steam Age**, per project decision. Continue designing
  server-authoritative and save-safe operations now.

## 9. Work sequence and acceptance

| Slice | Deliverable | Must demonstrate before accepted |
| --- | --- | --- |
| **Steel design foundations** | This document and a complete loop contract | Existing process IDs/capabilities audited; no accidental new gameplay |
| Coke Oven | Actually sealed coal → coke production | Real fuel, heat/volatile handling, proper ports, save/reload; no new dead-end Coke release |
| Blast Furnace | Coke + concentrate + flux + AIR/HEAT → pig iron + slag | Actual multi-input JSON/serializer, output reserves, meaningful BF difference, no bypass of Bloomery |
| Refining Furnace | Pig Iron → Steel via controlled oxidation/refining | No free refining, bounded heat/air, real slag, one complete Steel loop |
| First Steel consumer | A useful material advantage that can be manufactured from first Steel | Hot work, real wear/load/material advantage, old Iron tools still useful |
| Steel fabrication | Production stock and pressure-rated construction | No circular upgraded machinery recipes; compatibility and visuals |
| Steam handoff | Real pressure-related Steel components | Existing typed network remains distinct; no premature boiler/engine |

For each functional slice: check resource JSONs and compiled source,
run `./gradlew build` under JDK17, form actual JSON multiblocks from
Patchouli, exercise full power/fuel/output blocking/recovery, saves and
drops, and inspect real visuals/UV/performance. Independent static checks
never substitute for a build or in-game test.

**Current state:** this document changes no gameplay, models,
registries, recipes or old machine behavior. No Steel production has
been implemented or accepted in-game. PR #18 remains draft/unmerged.
