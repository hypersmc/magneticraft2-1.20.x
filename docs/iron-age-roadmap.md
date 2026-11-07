# Iron Age — foundations and development roadmap (Forge 1.20.1)

**Status (2026-11-07):** Iron Age gameplay has begun. Iron 0A's Wrought Iron Forging Die was implemented on PR #18 and the player has confirmed it works in-game. Further planned milestones remain proposals; release/build and systematic regression evidence are tracked separately.
**Base:** `master` immediately after Bronze PR #11 merge
(`5e5cb3440daa8bd642574c5f473bc5028fecfcb0`, 2026-11-04).
**Branch:** `feature/iron-age-foundations`.
**Current K1 implementation:** a Wrought Iron forging die gives the
existing Bronze Forge Hammer a longer service lifetime (768 vs 384
completed batches). Next is in-game verification, followed by an
independently justified stronger drivetrain application.

## Iron Age factory construction + third multiblock proposal — 2026-11-07

The complete Iron design now includes a cohesive **factory construction
palette** (Frame, Casing, Beam, Grating, Panel, Foundation, Railing,
Hatch), on top of the existing Gear V2 and hot-metal progression.
A **Mechanical Draw Bench** is the proposed third distinct Iron
multiblock, but requires actual calibrated-rod consumers before
implementation. An Iron Mechanical Hoist/Gantry remains conditional
until a credible lifting use and implementation are designed.

See [Iron Age industrial factory expansion](iron-age-industrial-factory-expansion.md)
for block identities, progression order, visual contracts and
Steel/Steam boundaries. **Total proposed core: 16 registered blocks
(3 multiblocks + 5 mechanical + 8 structural).** This is planning
and not a green light to implement every candidate.

## Detailed Iron Age content plan — 2026-11-07

The proposed full materials, blocks, three distinct proposed multiblocks, mechanical
upgrades, heavy manufacturing and Steel handoff are specified in
[Iron Age full content plan](iron-age-full-content-plan.md).

That file is a **design proposal**, not authorization to implement every
candidate. The first Forge Die is player-accepted; the rest require
incremental scope review, build and in-game verification. Multiplayer
remains deferred until after Steam.

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

## Iron 0A — Wrought Iron Forge Die (first focused K1 implementation)

The first Iron Age gameplay slice upgrades a **real, installed tool** rather
than introducing a decorative high-tier shaft. The existing Forge Hammer
accepts a Wrought Iron Forging Die made from four Wrought Iron Ingots
and a polished-blackstone striking insert. It lasts **768 completed
batches**, compared with **384** for the existing Bronze Forge Die.
Mechanical loads, heat, recipes, output quantities and the original
bronze die remain unchanged.

The maintenance contract now permits opt-in replacement variants and
saves the installed item registry ID, preserving the original
`RemainingCycles` NBT format for legacy machines and Bronze-die worlds.
Changing the installed material consumes one replacement die, even if
the previous one was not exhausted. The installed part is not an
extractable item or an automation input slot.

**Reason for this choice:** a standalone higher-torque shaft provides no
useful upgrade if its network still routes through a 8-torque wooden
gear. An upgraded forge die has an immediately measurable purpose
while leaving reinforced power transmission to Iron II.

**K1 acceptance:** craft from real Wrought Iron; install in an existing
formed Hammer; inspect current tier/life; complete cycles, reach wear
thresholds and run it to exhaustion; replace with either tier; restart
a world with existing Bronze Forge Hammer NBT; recover from a save with
installed Iron die; compare idle/paused and successful operations;
verify item display/model and Patchouli/JEI crafting.

**Player acceptance (2026-11-07):** The player reported **"yep works!"** after receiving the Iron 0A test checklist for crafting, die installation, service life, replacement, save/reload, and visual/book integration. Record the Iron 0A gameplay milestone as **player-confirmed functioning in-game**. The report is an overall confirmation, not separate logs for each test or measured wear/FPS data. The exact `node tools/iron-age/check-forge-die-contract.mjs` and JDK 17 `./gradlew build` outputs have not been independently supplied. Do not relabel those tasks as passed without logs.

**Multiplayer scope decision reaffirmed (2026-11-07):** **ALL multiplayer-related work and testing is deferred until Steam Age is finished**, per the user's explicit instruction. Do not make multiplayer verification a blocker for Iron Age development, or claim dedicated-server/multi-client testing passed. Continue to avoid knowingly client-only gameplay logic and preserve future multiplayer compatibility; full verification will be performed after Steam Age.


## Iron 0B — hot Wrought Iron Plate (source implementation; tests pending)

The first Iron 0B slice extends the **existing fueled Refractory Kiln**
and **powered Bronze Forge Hammer**, not a new multiblock. The Kiln
reheats a Wrought Iron Ingot in 240 HEAT-supplied ticks and grants
the existing 2400-world-tick (120-second) server-time heat state. The
Hammer requires at least 20 RPM, 6 torque and 160 supplied ticks to
convert ONE hot ingot into ONE Wrought Iron Plate.

Existing heat-NBT/quality propagation is reused. Finished plates do not
retain temporary forging heat. The ingot provides a hot/cold tooltip and
the plate has a UV-safe 3D item model using the existing dark worked-iron
texture. The original Bloom → Wrought Iron and Copper Plate recipes
must remain unchanged. JEI reads the new JSON machine recipes, and
Patchouli documents the two-machine route.

**Status:** source implemented on PR #18, NOT yet Gradle-compiled or
tested in-game. Verify actual fuel, temperature, expiry, NBT/quality,
output blocking, die wear, saving, power pauses, model and old recipes.
Bars, rods and general Iron blocks are *not* part of this slice.
Multiplayer work remains deferred until AFTER Steam Age.

## Next proposed milestones (following Iron 0A and Iron 0B)

### Iron II-A — investigate stronger transmission after first Iron tooling

**Future candidate:** a *Wrought Iron powertrain upgrade*
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

**Decision required before the transmission K1:** first drivetrain part
and numerical transmission/durability contract. The Forge Hammer's
Wrought Iron die is the first actual Iron 0A item, with its own
explicitly documented 384 → 768 wear benefit. Do not assign
unstated torque improvements to other Iron parts.

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

1. [x] Base PR #18 on merged Bronze PR #11 and preserve the refactor PR #17.
2. [x] Audit the existing Gear V2 shaft/gear limits without changing them.
3. [x] Implement one first end-to-end Wrought Iron → forging die →
   installed upgrade loop through existing crafting and Forge Hammer
   maintenance, including JEI/Patchouli and a textured 3D item model.
4. [ ] Compile with JDK 17 `./gradlew build`; test new/old maintenance
   data, item wear/replacement and visuals in Minecraft.
5. [ ] After the die is accepted, evaluate the real high-load network
   bottleneck and select a useful forged Iron drivetrain part before
   growing Iron I/II.

**Status (2026-11-07):** PR #18's first Iron Age gameplay feature has been
**accepted in-game by the player**. Full `./gradlew build` and independent
test-script results remain undocumented here. Additional Iron drivetrain
upgrades remain proposals, not registered features. The user has explicitly
deferred **all multiplayer-related implementation/testing until after Steam
Age is complete**. PR #18 stays draft and unmerged pending a separate
decision; player acceptance does not by itself authorize merging.
