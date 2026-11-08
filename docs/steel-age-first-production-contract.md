# First Steel loop — implementation contract, not yet approved code

**2026-11-08 · Forge 1.20.1**  
**Status:** source inspection + engineering design only; all machine
footprints, exact throughput and recipes pending focused implementation.
**Dependency:** Iron draft PR #18 is the source baseline; this is
a separate stacked Steel design branch.

## 1. Why the first three machines form one product

A lone Coke Oven leaves an otherwise unused material. A lone Blast
Furnace makes unusable pig iron. A machine that produces “Steel” directly
from Magnetite or a Wrought Iron Ingot and Coal would skip controlled
refining. Treat **Coke → Pig Iron → Refined Steel → first useful
consumer** as the vertical acceptance milestone. Each machine can be
implemented in small commits, but partial intermediates remain explicitly
*source prototypes*, not player-ready progression.

First consumer proposals (select after confirming hot-working and wear
contracts, not all at once):
1. **Real Steel cutting insert/tool** with physically credible hardening
   and longer tool life than the current flint-edged Iron Lathe Cutter.
2. **Steel structural/pressure-rated component** consumed by a later
   Steam infrastructure element, with an actual pressure/temperature
   rating rather than just a new texture.

**Do not require a Draw Bench** for the first Steel, and do not replace
the user-accepted Iron tooling and machines automatically.

## 2. Existing APIs and deliberate reuse

### Coke Oven

The existing Bronze Charcoal Retort proves separate feed, fuel and
byproduct reservations, a finite fluid tank and saved burn state.
Its input is **wood/logs**, with **Charcoal + Wood Tar** outputs. Coke
uses a distinct **coal** feed and different process/products. Reuse the
Retort's software patterns only where they actually fit; preserve
`charcoal_retort` ID, original wood recipe and saved NBT without
relabeling Wood Tar as Coal Tar.

First resource path should guarantee metallurgical Coke and define
how volatile matter is represented or intentionally vented. A *new*
Coal Tar/gas fluid should be introduced only once the correct output
storage/consumers are designed. For first-pass gameplay, expose real
fuel consumption and bounded finished Coke inventory; do not invent a
fake liquid byproduct or infinite Coke bootstrap.

### Blast Furnace

The Bronze Bloomery has a dedicated **`BloomeryRecipe`** with multiple
ingredients (Magnetite Concentrate and Calcite flux), an actual
byproduct (Slag), independently fueled heat and sustained AIR via a
single formed tuyere. This proves the pattern for Steel reduction.

A separate machine/recipe identity must:
- consume prepared Iron concentrate, *metallurgical Coke* as reductant
  or explicit feed and Calcite as flux;
- consume actual fuel/HEAT and **typed AIR** from the existing
  Pneumatics network;
- produce **Pig Iron** and Slag as separately reserved outputs;
- preserve source-quality decisions where applicable and avoid
  overlapping or overwriting Bloomery save keys;
- refuse formation without all actual ports and the required AIR inlet;
- remain constructible from existing Iron/Bronze components.

**The generic `multiblock_processing` recipe has one item ingredient**.
It cannot silently represent concentrate + coke + flux. Prefer a
dedicated validated recipe/serializer with explicit distinct item
inputs, analogous to `BloomeryRecipe`; do not hide Coke or Calcite
costs in Java. Machine logic must use recipe-provided quantities and
show them in JEI/Patchouli.

### Steel Refining Furnace

A refiner is NOT a second reduction furnace. It receives Pig Iron and
runs a controlled oxidizing/decarburizing process with sufficient
HEAT/AIR, slag removal and bounded steel output. Refining cannot be
modeled as just “Pig Iron melts and becomes Steel” if it consumes no
control input or process energy.

Prefer a distinct JSON-driven recipe/serializer if it needs multiple
inputs, refiner-specific quality/carbon validation, or multiple
outputs. Do not reuse `HotBloomWorkpiece` heat NBT for molten pig
iron without extending its material-type and expiry contract; the
current hot items are **Iron Bloom, Wrought Iron Ingot and Wrought
Iron Bar**, not Pig Iron or Steel.

## 3. Minimum machine state and ports

For **all three** machines:

- Only valid **formed** JSON multiblocks work. Use four correctly
  rotated layouts from the authoritative SOUTH model and match
  Patchouli visual formation, module blocks and AIR anchors.
- Real item/thermal/AIR/fluid ports must correspond to saved block
  entities. Do not let controller item capabilities bypass module IO.
- Server owns inventory, fuel queue, current burn time, HEAT, AIR,
  progress, item quality and output reservations. Client receives
  snapshots for rendering, never fabricates a product.
- Reserve *every* normal/byproduct output before consuming material.
  On output blockage or missing fuel/AIR, do not spend input or
  secretly advance progress; fuel already burning should obey the
  chosen physically consistent burn policy even while blocked.
- Input identity, counts, NBT/quality, recipe identity and active
  workpiece are checked on progress/resume. Persist enough state to
  avoid resets/duplication on chunk unload and world save.
- No entities as moving components, no per-tick model rebuilding,
  no new network manager where old Pneumatics/Power/HEAT paths suffice.
- Real powered-machine animations must be tied to source state,
  and supports/gears/ducts must visibly connect without stretched UVs.

## 4. Balance gates before choosing any numbers

* **Mass:** State ore-concentrate units per Pig Iron, Pig Iron
  units per Steel, consumed coke/flux, rejected slag and metal loss.
  One Steel item cannot represent arbitrarily more metal than its
  Pig Iron input. Round yields explicitly; quality is independent
  of metal quantity.
* **Carbon:** Coke is different from charcoal. Pig Iron must not
  masquerade as Steel; air/refining changes its composition by
  an explicitly bounded gameplay mechanism.
* **Energy:** Current HEAT buffers are **abstract**, not real
  Celsius. Determine a credible higher furnace threshold from
  actual source constants and fuel budgets before assigning numbers.
* **Mechanical:** Up to **16 torque** can come from the Large
  Water Wheel depending on water flow, and throughput depends on
  gear ratios and 8-rated wooden bottlenecks. Avoid requiring an
  unexplained >16 torque for the machines that must exist before
  Steam supplies stronger power.
* **AIR:** Existing Bronze blower supplies discrete units and
  typed AIR can travel in ducts. Measure or choose demands that
  work with existing capacity; any upgraded blower is conditional,
  not an assumed new source.
* **Wear:** Actual furnace lining, valves, dies and tools may
  wear if gameplay uses replaceable parts. Do not invent arbitrary
  idle decay or a fragile hidden per-tick failure probability.
* **Recipe and machine balance:** The Bronze Bloomery remains the
  Wrought Iron line; the Steel Blast + Refiner must provide an
  additional product/ability, not obsolesce Bronze/Iron content.

## 5. First runnable end-to-end test

From a genuine Iron Age workshop:
1. Construct the Coke Oven using Iron/Bronze/refractory materials
   that do not themselves require Coke or Steel.
2. Feed **ordinary Coal** and the designed starter fuel. Confirm
   a finite Coke output; blocked output and stop/start cannot
   duplicate or create fuel.
3. Build the Blast Furnace without Steel. Supply genuine Magnetite
   Concentrate, correct Calcite and Coke, sufficient valid HEAT and
   typed AIR. Confirm **Pig Iron + Slag**, nothing on missing flux,
   coke, AIR, heat, or full byproduct output.
4. Refine Pig Iron in the separate Furnace with the required
   oxidizing input and fuel. Confirm **Steel stock + refining waste**,
   with correct mass accounting and no direct smelting shortcut.
5. Turn first Steel into at least one **useful, manufacturable**
   tool or structural component using only the facilities that
   already exist at this progression point.
6. Reload mid-batch, interrupt fuel/AIR, change input quality,
   fill outputs, dismantle/reform and compare all four directions.
   Verify no duplication, silent data loss, static visual errors
   or old Copper/Bronze/Iron recipe regressions.
7. Run focused source checks + a real JDK 17 `./gradlew build`
   and perform the gameplay test. Multiplayer testing stays
   deferred until after Steam Age.

## 6. What this contract does *not* authorize

No invented Steel machines have been registered here, and no
preliminary model is considered finished. We haven't approved
footprints, exact recipes, carbon-grade math, output yields,
manufacturing tool lifespan, fuel balance, new fluid gases or
Steel-specific power sources. The production-chain design must
be narrowed to individually reviewable source changes before
claiming implementation.

**Cross-reference:** `docs/steel-age-full-content-plan.md`,
`docs/ore-processing-by-age.json`,
`docs/pressure-and-gas-networks.md`, and PR #18's
`docs/iron-age-full-content-plan.md`.

## Source implementation checkpoint — 2026-11-08

The three first-Steel controllers, reusable server furnace backend,
validated `magneticraft2:steel_process` recipe type, 12 JSON
multiblock orientations, Voxel V2 profiles, 16×16-native temporary
baked rendering palette, AIR tuyere and genuine shared item ports are
source-implemented on Steel PR #19. The branch was synchronized with
Iron PR #18 including merged Wood Tar PR #20, but **Iron PR #18 has
not been merged into master**.

The initial manufacturing ledger is provisional and deliberately simple:

| Step | Real material inputs | Explicit energy/input conditions | Guaranteed outputs |
| --- | --- | --- | --- |
| Coke Oven | 4 Coal | separate solid firebox fuel, HEAT threshold | 4 Metallurgical Coke |
| Blast Furnace | 4 Magnetite Concentrate + 2 Coke + 1 Calcite | separate Coke furnace fuel, HEAT and sustained AIR | 2 Pig Iron + 2 Slag |
| Refining Furnace | 2 Pig Iron | separate Coke furnace fuel, higher HEAT and sustained AIR | 2 Refined Steel Ingots + 1 Slag |
| First useful Steel | 4 Refined Steel Ingots + 1 existing Iron Forge Die | existing Bronze Forge Hammer maintenance | Steel Forge Die: 1536 batches instead of Iron 768 / Bronze 384 |

The Forge Hammer extension uses the existing stable
`ReplaceableMachinePart` alternative-material NBT mechanism. The
existing Iron and Bronze forging recipes, mechanical loads and wear
remain unchanged. Coke fuel is not freely spawned by running the
carbonization furnace; coal input and separate solid fuel are consumed.

For this first gate AIR remains explicitly typed and transported by
existing Bronze air ducts; the compressor/blower is already developed.
Pressure/STEAM networks are neither replaced nor used as fuel.
The 3x4x3/5x5x5 placeholder brick/iron rendering is NOT MGC2 Studio
quality, but it provides identifiable physical furnace structures with
native-density textures and real collision, rather than floating parts.

**Source implementation is not verification.** A JDK17
`./gradlew build`, actual in-game item I/O/AIR/heat tests, saved-world
checks, rendering and JEI UI verification are still required before
the player can confirm first Steel and Iron PR #18 is considered for
merging. See `docs/steel-age-first-production-acceptance.md`.
