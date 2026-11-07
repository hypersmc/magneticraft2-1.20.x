# Iron Age — foundations and development roadmap (Forge 1.20.1)

**Status (2026-11-07):** Iron 0A's Wrought Iron Forging Die has player acceptance, and Iron 0B's first Wrought Iron Plate has now been produced successfully by the player in-game. Bars/rods, factory blocks and other planned features remain proposals. Build and broader regression results are tracked separately.
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

**Player confirmation (2026-11-07):** The player reported **"yep, got
a wrought iron plate"**, confirming that the new Refractory Kiln →
Forge Hammer production chain yielded a Wrought Iron Plate in-game.
This verifies the main successful crafting/processing path, **not**
independent proof of fuel balance, exact yield over repeated batches,
expiry/reheat rejection, quality-tag preservation, blocked outputs,
die wear, save/reload, item visuals, or old-recipe regressions.
`./gradlew build` and the Node static fixture have no recorded results
here. These verification tasks remain open.
Bars, rods and general Iron blocks are *not* part of this slice.
Multiplayer work remains deferred until AFTER Steam Age.

## Iron 0C — Wrought Iron Bar/Rod rolling (source implemented; tests pending)

The existing **Bronze Rolling Mill** now performs Iron-specific hot rolling.
Players can roll a still-hot Wrought Iron Ingot into **one Bar** (180
powered ticks, 16 RPM, 6 torque), or reheat the Bar in the existing
fuel-fired Refractory Kiln and roll **one Bar into four Rods** (200
powered ticks, 16 RPM, 6 torque). Each Bar is one ingot-equivalent;
each Rod is one-quarter. The Mill checks the same server-time 120-second
heat stamp on every processing tick and transfers only persistent quality
NBT to output. Original cold Copper Sheet production is unchanged.
Recipe selection is machine-based: Forge Hammer makes Plate from hot
Ingot; Rolling Mill makes Bar, so no recipe ambiguity.

**Status:** code/JSON proposed in PR #18, full build and single-player
tests pending. No new machine/multiblock. Multiplayer remains
deferred until after Steam Age.

## Iron 0D — first factory-building blocks (source implemented; tests pending)

Three simple survival-craftable Iron construction blocks now follow from
the first hot-worked materials: **Iron Structural Beam** (X/Y/Z I-beam
with collision matching its metal cross-section), **Iron Plate Panel**
(thin facing-oriented wall/casing with thin collision) and **Iron Machine
Foundation** (full stone-and-iron support block). They use existing
Wrought Iron Bars/Rods/Plates plus stone bricks in conservative recipes.
The palette is genuinely useful for industrial player builds with no
new machine, block entities, animation system or per-tick physics.

All three have BlockItems, native-UV Minecraft block models, blockstates,
loot/drop tables, pickaxe tags, localized names, JEI crafting and Patchouli.
The first Iron multiblocks can later use these structural blocks where
approved, without making factory construction depend on new machinery.

**Status:** source implementation only; actual `./gradlew build` and
in-game appearance/placement/rotation/drops/survival recipe testing
remain open. Dedicated multiplayer remains deferred until after Steam.

## Iron II-A — first Iron Medium Gear (source implemented; verification pending)

**Source candidate:** The first actual Gear V2 Wrought Iron Medium Gear
(`wrought_iron_gear_medium`) is registered with the existing medium
gear's **8-tooth** topology, placement restrictions, blockstate rotations,
power-driven rendering and sync. Only the material's maximum transmitted
torque differs: the wood medium gear remains **8**, while the wrought
iron medium gear is **16**. The new block entity delegates visual
rendering to the proven wooden medium gear renderer. No new Gear V2
network logic, source torque or arbitrary speed is created.

**Survival manufacturing loop:**

1. Existing Refractory Kiln heats a Wrought Iron Bar as before.
2. A still-hot Bar is worked by the powered Forge Hammer at 20 RPM /
   6 torque for 180 ticks into an `iron_gear_blank_medium`. This
   extends the existing hot-workpiece output whitelist.
3. The existing Bronze Mechanical Lathe, supplied with its flint-edged
   cutter, machines the blank through a specific JSON recipe (160 ticks,
   12 RPM / 5 torque) to a `precision_iron_gear_medium`. Working Iron
   consumes **2 cutter durability**, compared with **1** for Bronze.
   Bronze machining outputs and previous worlds keep their original
   behavior. The Lathe passes the blank's quality NBT to the part.
4. The player assembles the actual gear block from the precision gear,
   three Wrought Iron Plates and one Wrought Iron Bar at the crafting
   table. Vanilla crafting does **not** preserve ingredient quality
   tags into the block ItemStack. Gear torque is intentionally material
   capacity only; quality-driven gear ratings remain a separate,
   unapproved rule. This limitation must not be hidden.

The first Iron gear has its own machine/item registrations, 3D voxel
model, native-density UVs, all orientation blockstates, pickaxe
harvesting/drop, creative tab, recipes and Patchouli pages. Its
dynamically rotating model uses the shared renderer. The wooden gear
type/renderer/capacity are untouched except for additive protected
constructors needed for reuse.

**Important incomplete acceptance:** An Iron medium gear alone is
*not* proof of sustained >8-torque factory operation. The source and
full transmission path must permit that load; a loaded consumer higher
than 8 torque is not yet introduced. The proposed Stock Shear is a later
Iron milestone. In-game test the Iron/Wood gear meshes, phase and
clearance, the actual 16-torque node value, all axes, rendered-to-collision
consistency, old save states and source overload. No arbitrary offset or
hidden mesh geometry is an acceptable fix.

**Status:** implementation candidate only. No confirmed
`./gradlew build`, Java runtime or player in-game verification.
Bar/Rod rolling (Iron 0C) is still awaiting specific player acceptance.
Multiplayer implementation/testing is deferred until after Steam Age.

## Iron II-B — Wrought Iron Shaft and Gearbox (source implementation, pending test)

Two more Gear V2 nodes are now source-implemented on PR #18:
`wrought_iron_shaft` (axial 1:1 drive, 24-torque structural
capacity) and `wrought_iron_gearbox` (right-angle reversible 1:1
drive, 16-torque capacity). The wooden shaft remains 16 and the
wooden right-angle gearbox remains 8.

**Actual benefit:** The Iron gearbox removes an 8-torque right-angle
bottleneck if the large water wheel, other gear nodes and actual
consumer support greater load. The new shaft's capacity above 16
is future headroom, *not* more current source power. A separate
higher-than-8-torque factory consumer is still needed for end-to-end
proof, and mixed wood/iron networks remain limited by their weakest
node.

**Implementation:** Both blocks specialize the existing Wood
Shaft/Gearbox code, keeping axial port rules, right-angle direction,
in-world port swapping, Gear V2 load semantics, update sync and server
ticks. Each has its own block entity/registry ID so saved wooden
installations are not retyped. Iron Shaft uses the same backed model
rotation and axle geometry with dark worked iron, while the Iron
Gearbox reuses the established *dynamic bevel gear rendering geometry*
with metal/bronze textures rather than copying or repainting the
wooden enclosure at runtime. Stationary cages use the existing
proportionate model with Iron/Bronze texture assignments.

**Survival:** Shaft = one wrought bar + two precision bronze bearings.
Gearbox = two wrought shafts, two precision iron gears, four wrought
plates and one precision bronze bearing. Existing forge/rolling/
lathe production chains supply them; neither recipe is circular.
All have block items, creative-tab entries, simple crafting recipes,
loot, pickaxe tags, named models and Patchouli pages. There is no
new forged material quality modifier for their torque ratings:
vanilla crafting-table assembly does not propagate source quality
NBT, as with the first iron gear.

**Verification not performed:** `./gradlew build`, committed Node
fixtures and in-game motor/gear orientation, visual, torque, break,
reload, performance tests. Multiplayer remains explicitly deferred
until after Steam Age.

## Iron III-A — Stock Shear, Rivets and Riveted Frame (source only)

A new JSON-driven 3×3×2 Stock Shear now introduces a genuinely
higher-demand Gear V2 consumer: **12 torque at 16 RPM** over
180 supplied ticks. It cuts one existing Wrought Iron Rod into
four quality-preserving Rivets. Four Rivets with four Wrought
Iron Bars yield one useful Riveted Iron Frame. The shear blade
lasts 512 completed batches and must be replaced when worn.
The first machine is built from prior Wrought Iron parts, so
no self-gating metal loop is introduced.

The existing Bronze Mechanical Press and Gear V2 physics remain
untouched; the Shear has its own controller, BE, JSON machine
recipes, I/O and material model. New construction requires
existing Iron Foundations/Beams plus real modules. The book
shows the full structure and the operational requirements.

**Stage:** source/prototype only, Gradle compile, real machine
formation, model/animation and loaded power network NOT verified.
Do not assume a 16-capacity source can run a 12-torque consumer
until confirmed with actual water flow, load module and all
intervening gear/gearbox nodes. All multiplayer work remains
postponed until after Steam.

## Iron II-C — 16-tooth Wrought Iron Large Gear (source implemented, pending test)

A new 24-rated Large Gear uses the existing 16-tooth Gear V2 placement, ratios,
network phase and wooden large-gear rendering path. It is assembled from a
machined rim, wrought plates and an existing wrought shaft. Its rough rim is
crafted from four bars, four plates and a Riveted Iron Frame; the existing
Bronze Lathe cuts it at 12 RPM / 8 torque over 160 supplied ticks with three
cutter durability. Medium Iron remains two wear, Bronze remains one. There is
no new machine, power generation or change to old saved gear IDs.

Commit `f4a91992ca0b52fb6d829cf676789954c3d01f84`. The model has
16 rooted teeth, a 2-pixel axle and native-density iron UVs. The 24-torque
rating is a transmission limit, not a source; Iron medium gears/gearboxes
and Mechanical Input Modules still limit their connected path to 16.

### Existing input module — formed Iron visual tier

Commit `6cecfacdc29ad833753524e525a7bb1df83da0a1` replaces the
formed Iron Stock Shear's copper/wood module appearance with a supported iron
housing, bronze bearing and 2-pixel iron shaft. The controller is identified
from an opt-in data-pack block tag and already persisted controller position;
there is **no new mechanical input block, save ID, machine blueprint or torque
upgrade**. Copper and Bronze retain the original visual family.

**Status:** Both changes source-implemented and statically inspected. Full
JDK17 Gradle build, actual Node fixture runs, Minecraft rendering, machining,
module coupling, overload, collision and save/reload remain **unverified**.
See `docs/iron-age-large-gear-acceptance.md` and
`docs/iron-age-input-bearing-acceptance.md`. PR #18 remains draft; model
texture finishing is deferred to MGC2 Studio and multiplayer to after Steam.

## Remaining proposed milestones (after Iron II-C source implementation)

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


## Iron IV-A — Forge Hearth source milestone (2026-11-07)

Source commit `d6f039e11d65a8042d0cb2d9e4b0fa21d243c1ae`
implements fueled four-piece reheating in a 3×3×2 JSON multiblock, reusing
current HEAT, item ports, recipe serializer, hot-state NBT and server logic.
The Kiln continues to fire ceramics and reheat single pieces/Blooms.
The authoritative layout is in Patchouli. The concept sheet is
`docs/reference/iron-forge-hearth-studio.svg`; visual finishing is deferred
to MGC2 Studio. No mechanical input, AIR network, steel recipe or
new material quality system is added. **The player confirmed on
2026-11-08 that the Hearth works in-game and was previously tested.** Its
main functionality is accepted; full JDK17 Gradle build, exact fuel/HEAT
accounting, all-facing visuals, chunk save/reload and individual regressions
are not independently documented. PR #18 stays draft.


### Iron Forge Hearth — geometry readability correction

Player screenshots exposed full-height formed brick models blocking the
AIR-anchor firebed. The 3×3×2 full-brick construction is preserved while
formed render parts are lowered and opened at the front. Voxel V2 collision
is generated directly from the low masonry, rather than invisible full
brick cells. Texture UVs follow actual face dimensions. Existing formed
structures must be unformed/reformed to refresh persisted snapshots.
Forge compilation/in-game visual tests are still outstanding.


## Iron IV-B — Wrought Iron Flywheel source milestone

The proposed heavy flywheel is implemented at the source stage: actual
3×3×1 vertical footprint, two foundation supports, six reserved AIR
cells, one 2.4-block diameter rotating rim, Gear V2's original finite
coasting with 12-torque Iron limit. Build, physical meshing, coasting,
multiblock teardown/reload, visuals and FPS remain unverified.
See `docs/iron-age-flywheel-acceptance.md`. PR #18 stays draft.


### Iron Flywheel axial-bearing correction (in-game screenshot follow-up)

Player screenshots demonstrated unsupported side arms and a wood-colored
wheel near the Iron supports. The source Iron BER itself binds no wood
textures, so the wood identity must be checked in an isolated runtime
case. Meanwhile the actual geometry defect is corrected: split front
and rear iron bearing rings, supported by crossarms reaching the two
foundation-supported uprights, straddle the center wheel plane in the
existing 3×3×1 structure. 43 matching Voxel V2 physical regions; no
3×3×3 expansion or new power network. Regenerate with
`tools/iron-age/build-iron-flywheel-bearings.mjs --check`.
This remains a source prototype pending Gradle compilation, actual
visual/rotational checks, collisions and durability regressions.

## Iron II-D — Iron Service Hatch (source milestone, 2026-11-07)

The remaining planned factory-access block `iron_service_hatch` is now
source-implemented on draft PR #18 as an ordinary manually operable,
redstone-driven, waterloggable **vanilla TrapDoorBlock** rather than a new
block entity or machine interface. The new iron-sounding BlockSetType
keeps the normal IRON trapdoor metal open/close sounds while enabling
manual operation; normal 1.20.1 FACING/HALF/OPEN/POWERED/WATERLOGGED states,
placement and neighbor handling are delegated to vanilla. This does
not introduce a pressure seal or automatic maintenance-hatch protocol.

Two Wrought Iron Plates, one Bar and one Rod craft two hatches, requiring
only existing early-Iron materials rather than the Stock Shear's rivets.
The item appears in the construction creative tab, has native-density
metal-framed block models, drop table and pickaxe tags, and a Patchouli
entry. Geometry follows the vanilla 16×3×16 closed and 16×16×3 opened
hitboxes, including every horizontal facing.

**Unverified:** full JDK17 Forge build, placement top/bottom, hand/redstone
interaction, waterlogging, survival crafting, drops, reload and model
UV/rendering in Minecraft. The independent JSON/source fixture is in
`tools/iron-age/check-iron-service-hatch-contract.mjs`. Test before
marking the hatch complete. No change was made to the flywheel or HEAT
systems. PR #18 stays draft, multiplayer remains deferred until after Steam.

## Iron I-C — Cold Wrought Iron Sheet (source implementation, 2026-11-07)

The first thin wrought stock now comes from the existing powered Bronze
Rolling Mill, not a duplicate Iron machine: **1 forged Wrought Iron Plate
→ 2 Wrought Iron Sheets** in **240 supplied ticks at ≥16 RPM / 12 torque**.
This is a *cold-finishing pass* on already forged, low-carbon wrought
iron; it does not falsely require a new plate-reheat path or invent
permanent hot sheet items. The genuinely higher load needs a working
Iron drivetrain, and the Mill retains real roller-sleeve wear,
output reservation, power interruptions and progress handling.

The new sheets are **one-half plate (one-half ingot equivalent) each**.
The Roll's quality-NBT propagation is intentionally separate from the
hot-stock requirement: Iron Sheet, hot Iron Bars and hot Iron Rods
all preserve quality without transferring temporary heat; original
Copper cold rolling keeps its previous output-tag behavior.

The immediately useful consumers are two **additive**, opt-in
efficient factory construction recipes:
- 2 Sheets (one original Plate worth of metal) → 1 Iron Plate Panel;
- 2 Sheets + 1 Wrought Iron Bar + 1 Rod → 2 Iron Service Hatches.

These do **not** remove or alter the original 2-Plate Panel recipe nor
the original 2-Plate/Bar/Rod Hatch recipe. The material-saving benefit
of spending powered machine time is explicit. Finished crafted blocks,
as with all ordinary crafting table outputs, do not propagate the
input ingredient quality NBT automatically.

**Status:** source and JSON implemented only; in-game shaping, power
load, finish time, output quality, copper/hot-iron recipe regressions,
blocked outputs, roller wear, runtime visuals and JDK17 Gradle build
remain unverified. See `docs/iron-age-sheet-acceptance.md`. Keep
PR #18 draft and multiplayer work postponed until after Steam Age.

## Wrought Iron Roller Sleeves — maintenance follow-on (2026-11-08)

The existing Bronze Rolling Mill's 512-batch Bronze sleeve service limit now
supports an optional 1024-batch Wrought Iron pair, reusing the Forge Hammer's
material-tier maintenance contract. Iron sheet rolling is still 12 torque and
16 RPM; this is a wear/replacement benefit, not free power or improved yield.
Original saved `MillRollers` Bronze NBT and Bronze sleeves remain intact.
Full implementation and acceptance scope: `docs/iron-age-roller-sleeves-acceptance.md`.
Source candidate only; JDK 17 Gradle build and in-game results are pending.

## Precision Iron Lathe Tooling — machine component follow-on (2026-11-08)

A cold Wrought Iron Rod is precision-bored on the **existing Bronze Precision
Boring Machine** (20 RPM / 8 torque / 240 supplied ticks) into a machined
Iron Toolholder, retaining source-quality NBT. The holder + one bored Bronze
Bushing + Flint assemble a Wrought Iron Lathe Cutter (128 durability vs the
64-durability existing Bronze Cutter). The original Lathe accepts either and
still uses 1 durability on Bronze, 2 on medium Iron and 3 on large Iron.
No machine block, new metal tier, machining output or torque rule changes.
Vanilla assembly currently does not copy toolholder quality to the cutter.
See `docs/iron-age-precision-tooling-acceptance.md` for regression tests.
Source implementation only; Gradle build, Node fixture and in-game tests pending.

## Mechanical Metal Shear — high-load source integration audit (2026-11-08)

The Large Water Wheel is **24-tooth** at its external rim but has a separate
axle connection. Its 8–16 torque / 18–24 RPM source range means a 24→8 gear
mesh delivers at most 5.33 torque and a 24→16 mesh at most 10.67: neither
can run a 12-torque Shear even with Iron parts. Test a genuine 1:1 axle/shaft
path first, then compare equivalent-ratio wood (8 capacity) and Iron (16+)
paths. Do not misdiagnose ratio loss as material-capacity failure.

The source-copied independent Shear motion classes compiled using JDK 21
with `javac --release 17` (Java 17 class format) and passed 1,441
linkage poses in this session. This does not verify a JDK 17 Gradle build. No complete ForgeGradle build,
contract-fixture run from the full checkout or Minecraft end-to-end runtime
test occurred. The source integration checklist is documented in
`docs/iron-age-stock-shear-acceptance.md`; PR #18 remains draft.
