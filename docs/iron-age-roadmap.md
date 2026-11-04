# Iron Age — foundations and development roadmap (Forge 1.20.1)

**Status:** kickoff / proposed gameplay scope; not yet an implemented Iron Age.
**Base:** `master` immediately after Bronze PR #11 merge
(`5e5cb3440daa8bd642574c5f473bc5028fecfcb0`, 2026-11-04).
**Branch:** `feature/iron-age-foundations`.
**Next milestone:** *use the first Wrought Iron Ingot for a meaningful,
installed upgrade of an existing working machine or mechanical power network*.

## What is already working (not to be recreated)

- The player has completed the Magnetite → Stamp Mill/Washer/Sifter →
  concentrated iron ore → Bloomery → Iron Bloom → heated bloom in the
  Refractory Kiln → Mechanical Forge Hammer → **Wrought Iron Ingot** route.
  Required extra fuel, flux and other consumables were supplied. The hot
  bloom displayed a real countdown.
- The current `wrought_iron_ingot` is already registered in
  `ItemRegistry`, and is produced by the existing Bronze-to-Iron recipe.
  It is the **material entry point**, not an invitation to duplicate iron
  smelting with a vanilla furnace recipe.
- Existing Gear V2 supplies RPM, torque/overload and gears/shafts; existing
  Bronze Lathe, Forge Hammer, Rolling Mill, Press, Boring Machine and
  Assembly Station should remain relevant to Iron processing.
- The JSON multiblock, V2 models/voxels, maintenance system, JEI, Patchouli
  and JSON-driven item/progress visual interface exist. Extend their
  machine-specific bindings; do not introduce a parallel animation engine.
- Bronze PR #11 merged into `master` on 2026-11-04. Separate long-running
  refactoring PR #17 must not be accidentally merged or rewritten.

## Iron Age design principle

**Iron increases strength, scale, repairability, and operating capability.**
Bronze is for precision casting and workshop tooling; Iron is for forged
load-bearing parts and genuinely heavier industrial processes. Stronger
materials do **not** provide free torque or RPM. A reinforced shaft can
transmit more power from a legitimate source, but cannot create power.

Iron must advance existing production loops without making Stone, Copper
or Bronze machinery obsolete. This age is **not** Steel Age; controlled
industrial steel metallurgy and its advanced equipment belong to the
later Steel design unless explicitly agreed otherwise.

## Proposed milestone sequence (not yet approved or implemented)

### Iron 0 — practical use for Wrought Iron (first implementation target)

**Candidate pilot:** the first *Wrought Iron powertrain upgrade*
(for example a stronger geared transmission part, bearing/coupling, or
shaft in a genuinely demanding configuration). It must be made from
the existing Bronze workshop's Wrought Iron output and solve a measurable
mechanical bottleneck. The exact item is **not yet settled**.

**Gear V2 audit (source-checked against merged Bronze `master`):**
- `ShaftBlockEntity_wood.getGearMaxTorque()` already returns **16**.
  Consequently, an iron shaft offering only 16 torque or a cosmetic
  material change does **not** automatically unlock a useful capability.
- `GearBlockEntity.getGearMaxTorque()` defaults to **8**, while some
  components override it. That is a possible bottleneck, not a reason
  to change all wooden gear limits or invent iron source power.
- `GearNetworkManager` propagates available torque by
  `min(availableTorque, neighbor.getMaxTorque())`, evaluates actual
  machine load separately and preserves axial shaft connections.
  Therefore an isolated reinforced shaft cannot bypass a weaker
  upstream wooden gear, gearbox or power source.
- **Implementation decision:** reproduce a real case where the player's
  Bronze-era power path becomes transmission-limited, determine the
  minimum meaningful Iron replacement part(s) and only then assign
  capacities, wear and recipes. If no such case exists, choose another
  truly useful first Wrought Iron application rather than adding
  decorative "higher tier" parts.

Before writing gameplay code, audit existing `ShaftBlock_wood`,
`ShaftBlockEntity_wood`, `GearNode`, `GearNetworkManager`, placement,
registries, connection rules, overload semantics, drops and saved NBT.
Reuse the established Gear V2 node behavior.

Acceptance criteria:

- [ ] Item/recipe consumes legitimate Wrought Iron from the existing chain.
- [ ] A real powered installation behaves correctly under low/high load,
      reversal, source stop, overload, adjacent wooden shaft and
      gear/gearbox connections; a high-capacity part does **not** increase
      the actual power source's output.
- [ ] Material limits and wear differences are explicit, testable and
      visibly communicable; no unexplained default-torque boost.
- [ ] Existing wooden networks and saved worlds behave as before.
- [ ] Correct model/UV, mechanical connection, block/item registry, loot,
      recipe, JEI, Patchouli and all horizontal/vertical orientations.
- [ ] `./gradlew build` and in-game regression, not just static inspection.

**Decision required before K1:** first item type and numerical
transmission/durability contract. Do not invent this silently.

### Iron I — wrought-iron manufacturing

Introduce **only the material forms needed by actual machine components**:
forged shaft stock, plate/sheet, bars/rods, forged fittings or fasteners.
First investigate whether the Bronze Forge Hammer, Rolling Mill and Lathe
can perform their realistic operations with an additive recipe rather than
creating new single-purpose copies.

- [ ] Conservation and byproducts for each step (offcuts/slag where real).
- [ ] HEAT conditions for hot forging and sensible cold-working limits.
- [ ] Input quality metadata propagation using existing conventions.
- [ ] No bypass around first Wrought Iron from Bloomery/Kiln/Hammer.

### Iron II — robust power transmission and machine upgrades

Reassess material-specific gearing/shafts, couplings, bearing housings,
higher process loads and visible replaceable wear parts **based on concrete
consumers**, not on new decorative content. Build on the existing Gear V2
load/state model and prove that mixed wooden/bronze/iron networks work.

### Iron III — useful heavy manufacturing

Candidate directions for later design review: heavier forming or cutting,
larger industrial throughput, material handling and service access.
A machine must solve a real production bottleneck or enable a new
material; **none of these candidate machines is yet authorized or
registered**. Prefer upgrades to existing multiblocks where credible.

### Iron IV — Steel Age handoff

Establish meaningful iron-part production and the industrial requirements
for controlled carbon/temperature work. Do not prematurely add a
Steel Ingot smelting shortcut or claim a specific steelmaking furnace
until the Steel Age contract is decided.

## Cross-age systems: preserved decisions

- **Material quality:** Existing vanilla-vs-mod Copper quality decisions
  must be preserved. Iron part quality/impurity consequences need a
  separate agreed specification, not unrelated new grade maths.
- **Wear:** Use existing `ReplaceableMachinePart` where appropriate,
  but not every part should decay simply to add upkeep.
- **Pollution:** An established cross-age systems goal, not silently
  declared implemented or required for this first Iron pilot.
  Sources, emissions, environmental effects, mitigation and balance
  require their own system design across Bronze → later ages.
- **Resources:** The 31-mineral age matrix lists Graphite, Anthracite and
  Sphalerite as *conditional* Iron candidates. Do not add ore processing
  without a specific named consumer, meaningful industrial difference
  and compatible resource/material conservation.
- **Visuals:** Work in MGC2 Studio is separate. Java may expose its real
  synchronized item/progress state to the existing animation system;
  do not start throwaway custom animation infrastructure.
- **Pipes:** AIR/STEAM/water are different networks. The requested
  all-orientation diagonal pipe variants remain separately scoped,
  with Steam rupture physics preserved. Do not gate first Iron work
  on Steam's implementation.
- **Multiplayer:** The user scheduled dedicated-server multi-client
  acceptance after Steam Age. Continue server-authoritative logic
  and preserve save compatibility meanwhile.

## First practical engineering checkpoint

1. Confirm PR #11 has actually merged and use its `master` commit as base.
2. Audit the existing Gear V2 shaft/gear/overload limits and identify the
   smallest safe iron material upgrade compatible with real existing
   machines. Include why a wooden shaft is insufficient in that test.
3. Agree the first Iron part's gameplay role and mechanical limits.
4. Implement *one* end-to-end loop (Wrought Iron → useful installed
   component) with the existing recipe/JEI/book/data-driven patterns.
5. Run JDK 17 `./gradlew build`, in-game placement, power, wear,
   render/facing and old-world regressions; only then grow Iron I.

**This kickoff is a planning artifact, not an Iron Age gameplay feature,
not a merged code change, and not a claim of build or runtime validation.**
