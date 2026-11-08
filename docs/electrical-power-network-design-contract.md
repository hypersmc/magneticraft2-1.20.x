# Electric Generator Age — Voltage, Current, Watt and real power lines

**Magneticraft2 · Forge 1.20.1 · architectural design 2026-11-08**  
**State:** design only; NO electrical machines, cable registry, voltage system or source changes are authorized by this document.  
**Age boundary:** Steel makes required structural parts; Steam drives heavy shafts; Gasoline introduces combustion engines; **Electric Generator Age** unlocks generators, electrical power grids and motors. See [Steel → Steam handoff](steel-age-steam-readiness-roadmap.md).

## 1. Why this exists

Electrical infrastructure should become the next engineering challenge rather than an invisible FE-style pool: output voltage, flowing current, delivered Watt, resistive loss, transformer ratio, cable rating and protective devices matter. Bare and insulated cables differ physically. A power pole needs mechanically plausible insulators and line support. An Electric Motor returns useful work to the **existing** shaft/gear network; a Generator is a real mechanical load, not a power source appearing out of nothing.

The design aims for believable player-experimentation physics, not a continuous transient electromagnetic or full waveform simulator.

## 2. Units and strict accounting

| Quantity | Symbol / units | Meaning | Ownership |
| --- | --- | --- | --- |
| Voltage | V, volts | RMS potential difference between supply and return nodes | Network / source |
| Current | I, amperes | Flow through an energized branch | Computed from connected source, load, and impedance |
| Electrical power | P, watts (W) | **Rate** of delivered electrical work | Measured from V × I in simplified unity-power-factor AC |
| Stored energy | E, joules (J) | Energy in a finite store | Generator/battery/consumer; conserved |
| Cable resistance | R, ohms (Ω) | Resistance of conductor plus return path at modeled operating temperature | Material/size/length/temperature |
| Shaft RPM | RPM | Existing Mechanical Power rotational speed | `GearNetworkManager` |
| Shaft torque | game's torque units | Existing Mechanical Power load capacity | `GearNetworkManager` |

For a simplified, single-phase balanced power circuit with power factor fixed to 1:

```text
P_delivered = V_load * I_load
Voltage drop approx: Delta_V = I * R_loop
Resistive heat loss: P_loss = I^2 * R_loop
E_J delivered over one 20-TPS game tick = P_W / 20
```

When reactive loads/frequency become meaningful, extend the model explicitly with real/reactive/apparent power and proper AC power factor. Do not silently label a non-unity-power-factor circuit with P=V×I. Do not treat W as stored energy. No fractional Joules should disappear by repeated floor/truncate: use bounded fixed-point accumulators or another proven conservation scheme, with overflow and invalid-parameter validation.

**Illustrative idealized example**, not approved device balance: a 600 W consumer draws 5 A at 120 V, 2.5 A at 240 V, 1.25 A at 480 V (ignoring line drop). If total line-loop resistance were 1 Ω, associated I²R loss would be 25 W, 6.25 W and 1.5625 W respectively. A real loaded network must account for receiving voltage being lower than the source voltage.

## 3. First-generation current type and voltage classes

**Provisional first implementation: simplified single-phase AC, RMS values**, common nominal frequency, unity power factor, no waveform or phase-angle simulation. This direction makes an early transformer meaningful while keeping a manageable electrical solver. The final 50/60 Hz convention, AC frequency effects, and the possibility of a separate DC subnetwork are **not decided**. A later battery/rectifier/inverter must have explicit conversion losses and storage, never an implicit same-wire AC/DC exchange.

**Candidate voltage classes (NOT balanced or approved):**

| Candidate class | Example voltage | Motivation |
| --- | --- | --- |
| Low-voltage experimental | 48 V RMS | Demonstration circuits and small motors |
| Workshop | 120/240 V RMS | Near-machine wiring and tools |
| Industrial | 480 V RMS | High-power factory motors and distribution |
| Transmission | TBD | High voltage/low current over distance, with safe line support |

Do not add tier labels that confer magic conductivity. Supply and consumer specify actual voltage ranges; protection/cable insulation has actual limits. Exact voltages require mechanical/electrical balance calibration and user acceptance.

**Multiple generators** connected to the same AC component are a later explicit design stage, not free additive power. Initially allow a single regulated generator per island and explicit transformer-coupled islands. Later parallel generators require frequency/phase/synchronization or a clearly documented simplified compatibility rule, startup and load sharing; reject incompatible source connections safely.

## 4. Real circuits, not one-way point teleportation

The first grid has an explicit **source and return circuit**, so current cannot move through a dangling wire. A two-core insulated block may carry both internal conductors; a bare overhead route uses a clearly paired supply/return arrangement on correct insulators. The protective-earth/ground conductor, if modeled, is distinct and should not be treated as a free return through arbitrary world blocks. Grounding and fault paths need their own validated rules.

Nodes/edges represent terminal pairs, junctions, cables and protective elements. Switches and fuses can open a circuit. Insulation prevents unintended contacts but does not eliminate current and thermal limitations. The network is not a Minecraft redstone signal or Forge Energy storage shortcut.

### Conductors, insulation and supports

| Component | Conductor / insulation concept | Current/rating purpose | Stage |
| --- | --- | --- | --- |
| Bare copper wire | Exposed copper; supported on ceramic insulators | Affordable, hazardous when energized and exposed; no arbitrary insulation rating | Electric Gen |
| Basic insulated copper wire | Copper with a heat/voltage-rated covering | Safer indoor circuits at the conductor's actual current limit | Electric Gen |
| Heavy insulated copper cable | Larger copper cross-section with rated insulation | Higher load / reduced resistance, not automatically higher allowable voltage | Electric Gen |
| Overhead power line | Bare appropriately spaced conductors on poles and purpose-built insulators | Long-distance distribution; line geometry and clearances matter | Electric Gen |
| Junction / terminal block | Connects paired conductors | Explicit topology; rated current and connection compatibility | Electric Gen |
| Fuse / breaker / disconnect | Opens the circuit when triggered or actuated | Predictable overload protection and service isolation | Electric Gen |
| Transformer | Separate rated AC input and output islands | Steps voltage with efficiency and power limit | Electric Gen |
| Voltmeter / ammeter / wattmeter | Read server-computed quantities | Diagnose low voltage, high current, efficiency and network overload | Electric Gen |

**Important:** Bare copper is not inherently low-voltage, and insulation does not raise conductor ampacity. Current rating primarily depends on conductor area, material, thermal conditions and installation; allowed voltage depends on insulation/clearance/support. Use **independent** data properties for resistance, cross-section, safe current, insulation rating, support and allowable exposure. No unintentional texture scaling on cable/support models. No random cable deterioration while under rating.

Rubber/plastic insulated wire should respect the existing Gasoline-era chemical/material progression when applicable; earlier fabric/ceramic insulation can be investigated without retconning prior material recipes.

## 5. Network ownership and calculation

**One authority:** a server-side, per-dimension ElectricalNetworkManager; passive cable blocks store only connection/render state. Electrical consumers/sources/protection/transformers can have BEs, not every cable. Caches are invalidated on placement, removal, side-connection changes, neighbor updates, switch transitions, and loaded chunk boundary changes. Never force-load a chunk to complete a circuit.

Per network solve, after checking valid topology:

1. Find powered islands and each ordered conductor/return edge, their resistance and rating. Open switches and incompatible source/return paths break the circuit.
2. Evaluate regulated source voltage, energy/power capacity and consumers' requested power and valid operating-voltage range.
3. Solve a **bounded, numerically stable, deterministic** steady-state approximation for branch currents, nodal receiving voltages, circuit losses and delivered work. Include both forward and return resistance. For constant-power loads the solver must iterate V and I rather than using source V as receiving V. Explicitly handle nonconvergence and no-power/near-zero-voltage cases by shedding loads or failing safely, not producing unlimited current.
4. Constrain transfers by generator available mechanical power, transformer ratings, switch/breaker ratings, cable ampacity and consumers' accepted power. Recompute the actual accepted solution under constraints; do not generate load power that cannot pass the wiring.
5. Commit **one** server-owned accounting transaction per solve interval: mechanical input / electrical generation / line losses / transformer and conversion losses / useful load / storage changes. Heat effects may be derived from losses; a zero-current open circuit has no I²R loss.
6. Apply fuse/breaker rules and bounded deterministic overload heating/short consequences. Produce client-only indicators and audio for actual server faults. Sync a compact network state to dashboards/meters and relevant visual blocks.

A practical first test network is a **simple radial line** (one source, one branch, one load); only then support multiple branches, converging loops, transformers and large factories. A solver that just distributes the source's nominal Watts equally to connected devices does not meet this design. Cache component graphs and resistance; no per-cable BE ticks, per-frame traversal or network-wide recompute merely because an animated model is visible.

For component splits/merges and save/load, a disconnected cable should not retain imaginary "stored Watts." Only real storage and machine thermal fault state persist. Support loaded boundaries as open/sealed endpoints; no accidental remote chunk activation. Rebuild graph from actual blocks; do not serialize transient random component IDs as the only identity.

## 6. Generator and electric motor: Mechanical Power bridge

**The existing mechanical system is authoritative**, with `GearNetworkManager.setMechanicalLoad(level, loadPos, inputPos, torqueDemand, active)` and `getMechanicalLoadState(level, loadPos)`. It already accounts for source-to-load RPM ratios and aggregate source torque demand. Do not directly increase source RPM/torque or create a fake rotating entity to get generation.

Current mechanical torque values are gameplay-scale units, **not physical N·m**. Define a **calibration constant κ**, with units W / (gameTorque × RPM), only after we inspect current machine demand/source balance. Then:

```text
P_mechanical_available_W = κ × T_input_game × RPM_input
P_electrical_generated_W <= efficiency_generator × P_mechanical_supplied_W
T_required_game = P_mechanical_requested_W / (κ × RPM_input)
```

The displayed physical-equivalent power does not require interpreting existing torque units as SI torque. All conversion is server authoritative, bounded by actual available shaft load, generator machine rating, RPM range and output-voltage constraints. At zero RPM, generation is zero; avoid division by zero. Register/recalculate the generator's mechanical demand in the existing manager, respect `MechanicalLoadState.supplied()`, source overload, disconnect and stop; no output on a stalled/unsupplied input.

**Do not create a circular free-power loop** with Generator → Electric Motor → shaft → Generator. Each conversion has efficiency strictly below one; storage and momentum are finite and losses/conservation are enforced. A motor must request real electrical work before declaring supplied torque. Its output enters the existing Mechanical Power System as a genuine bounded source; avoid treating a powered motor as an unexplained new torque constant.

A Steam Engine later drives that same mechanical interface with bounded pressure/steam consumption; a Gasoline Engine later offers a different fuel-driven mechanical source. Neither needs to know whether a connected shaft eventually drives a Lathe or a Generator.

## 7. Legacy API and migration

Source inspection of `CapabilityWatt.WATT`, `IWattStorage`, `WattStorage` and `BaseBlockEntityMagneticraft2` confirms a legacy **integer storage** exposed as `getWattStored()`, `receiveWatt()`, `extractWatt()`, with `capacityW()` / `maxtransferW()` and saved `"watt"` NBT for existing capable machines.

That API is **not** an electrical network specification. It treats named Watts like a stored quantity and, in `WattStorage.extractWatt()`, checks `canReceive()` instead of `canSend()`. These are audit findings, **not authorization to change the existing APIs or saved data**. Do not silently reinterpret stored ints as real W, A, V or J, remove capability registration or create FE/RF conversion.

Preferred future approach: add a **typed electrical power port/interface** (names/schema TBD), with immutable rating and snapshot data and explicit transactional supply/demand. If a legacy machine actively uses `IWattStorage`, introduce a documented adapter or versioned migration after identifying real consumers and saved usage; reject unknown units rather than guessing energy conversion. Preserve old IDs and NBT shape unless migration is specifically approved and tested.

## 8. Protection and fault physics

- **Undervoltage:** throttle or stop consumers within specified safe supply ranges. A stalled engine must not consume a successful full work-cycle quota.
- **Overcurrent:** fuse/breaker trips or cable temperature budget is exceeded. No instantaneous arbitrary destruction without an identified fault path/thermal rule.
- **Short circuit:** only when actual live/return/ground contact, wrong support or failed insulation creates a circuit; bounded source short-circuit current and protective cutoff, not infinite amps.
- **Overvoltage:** transformer/source mismatch causes protection fault or rated-device failure. No silent acceptance by every consumer.
- **Player contact:** a live bare conductor can be hazardous. Exposure, accessible geometry and configurable gameplay harm require their own separate approval.
- **Failure persistence:** tripped protection, real thermal damage and storage survive reload when relevant. Wires at normal load do not suffer random time-based wear.

**Steam pressure overrating** follows the existing distinct Steam rupture rule (one persistent leaking pipe per pressurized component), not the electrical fault algorithm.

## 9. Validation before implementation acceptance

1. Pure-Java numerical tests: same-current/voltage inputs yield deterministic V/A/W, correct I²R, 0 A for open return, no-load zero loss, correct long-line voltage drop, no negative current/NaN/divide-by-zero.
2. 600 W at 120/240/480 V example matches idealized current; actual receiving voltage and losses use the branch solver.
3. Motor and generator use existing Mechanical Power overload/source-load behavior; disconnection, clutch tripping and source stall stop output.
4. Generator and motor efficiency closed-loop test loses energy instead of making power; batteries cannot duplicate Joules through rounding/split/merge.
5. Test bare vs insulated cable independently for electrical ampacity, voltage/clearance, geometry/placement, normal rated operation and fault protection. No arbitrary ratings hidden in block code.
6. Build a simple cable pair, then branching load, open circuit, overload, different-length runs, transformer-isolated island, breaker trip/reset and chunk boundary tests.
7. Server authority, persistence, topology split/reconnect, invalid config/recipe reporting and reasonable CPU/memory bounds. No client simulation, forced chunks or per-segment BE ticks.
8. `./gradlew build` with JDK 17, actual in-game visual/UV checks, performance measurements and source compatibility. Dedicated multiplayer-specific implementation/testing remains deferred until after Steam Age as previously decided.

## 10. Unresolved decisions to ratify before coding

1. Is **simplified AC-first** the approved foundation, with DC later by explicit rectifiers/inverters? This document **assumes** AC for design but does not mark it user-decided.
2. Actual voltage classes and nominal frequency, and whether multiple generators are permitted in the first electrical slice.
3. Mechanical game-unit calibration κ, generator efficiency/power curve, motor efficiency/torque curve, minimal continuous output and line thermal units.
4. Exact conductor material recipes and insulation readiness by age, cross-section ratings, transformer capacity and component visual conventions.
5. Safe behavior for exposed energized conductors and arc faults, if implemented.
6. Final component graph cap/scheduling and future treatment of power factor, reactive motors and multiple AC sources.

**First implementation candidate, once authorized:** pure power/line/measurement math with repeatable tests, isolated from world blocks and legacy `WattStorage`; then a single loaded generator and cable pair, before adding multiple devices. No electricity-generating gameplay is delivered by this documentation commit.
