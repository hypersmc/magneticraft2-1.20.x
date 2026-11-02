# Steam network and sudden rupture — architecture decision

**Decision:** Sudden Steam Pipe failure on structural overpressure. No warning damage stages, ticking individual pipe block entities, automatic repair, or arbitrary random breakage while below a rating.

**Status (implementation pass):** First server-side steam reservoir, one-rupture-per-network behavior, persistent ruptured block, and visible active vent were implemented as an initial foundation. Forge compilation and in-game visual/runtime verification remain pending. No Steam Age boiler/engine and no steam crafting progression have been introduced.

**Gameplay priority:** Intact pipes NEVER wear out and NEVER break randomly below their pressure rating. Failure happens only following actual overpressure. Even under continued overpressure, one broken pipe per connected component provides a vent; the network does not cascade through every pipe. This is intentionally a challenge players can engineer around, not daily maintenance.

## 1. Existing implementation and gap

At commit e7b5dd299aa36e763a6482df933da83d93a56622:

- PressurizedPipeNetwork searches up to 256 loaded Steam Pipe blocks and delivers between machine ports. There is **no network-owned gas inventory**; pipe blocks are passive.
- PressurizedGasStorage holds machine gas in abstract integer units, computes an approximate pressure, and **rejects gas that would exceed its own rating**. Reusing this as the pipe network reservoir would make rupture impossible. Pressure rating of a vessel is not the pressure rating of every attached pipe.
- SteamPipeBlock stores only six connection booleans. Its current block model reuses the Bronze Air Duct mesh with an iron-texture override. This is temporary and must be redesigned.
- No steam boilers, consumers, regulators, pressure relief valves, genuine fluid phase conversion, pipe failure block or steam particle rendering are implemented.
- LegacyAirPort preserves Bronze Air Duct air charges and must remain separate from steam. The old Mechanical Power System is independent of both transport networks.

**Required architectural change:** Make a dimension-owned, server-authoritative Steam Network manager with its own gas inventory and pressure state. Treat steam pipe materials/rating and localized failures separately from machine vessel ratings and pressure control. Do not try to add failure by randomly replacing pipes based on a machine's stored integer count.

## 2. Data ownership — no BlockEntity for every pipe

| Owner | State |
| --- | --- |
| SteamNetworkManager, runtime per server dimension | Loaded topology, connected-component mapping, dirty components, available connected ports, cached pipe volumes/material limits, branch flow allocations and update cadence |
| SteamNetworkSavedData, dimension-owned persistent data | Conserved steam amount, thermal energy/temperature representation, component/partition persistence and format version; dirty-on-change and load validation |
| Steam Pipe BlockState | Six visible connector directions, and either intact/ruptured status with rupture direction, or a distinct Ruptured Steam Pipe block with equivalent connections |
| Pipe material definition | Internal volume, maximum safe pressure, temperature rating, resistance, rupture opening area, render/material properties; data-driven and validated |
| Boiler, steam engine and future machines | Their own water, steam, HEAT, fuel, mechanical/electrical demand, control valves, operation budgets, capability ports and save state |
| Client effects | Only visible ruptured pipe positions and state for particles/sound; never authoritative gas quantity or fault decisions |

**Preferred block representation:** a separate RupturedSteamPipeBlock using the same connection booleans and an outward breach direction. A rupture performs an ordinary server-side block replacement, so it persists naturally in chunk save data and its distinctive model survives reconnects/reloads. The ruptured block remains a member of the Steam Network, but acts as a high-loss vent. Full severance/explosion is a *different* possible future mode and is not part of this decision.

No per-pipe BlockEntity or per-frame per-segment gas simulation. If pipe quality/wear later requires persistent per-position variables, store only non-default exceptions in a **sparse** dimension/chunk data structure, not an unconditional BE for every pipe. Sudden failure does not require a wear counter.

## 3. Steam accounting, pressure and topology

- The steam network stores a conserved inventory and a temperature/thermal-energy aggregate, with capacity derived from its connected pipe internal volume. Machine inventories remain distinct; boiler output transfers steam into the network, not magically to a distant receiver.
- A first performant model may assume a common bulk pressure within a connected loaded component; explicit demand and line resistance determine delivered flow. Later, local pressure can be estimated on cached graph branches where relevant. Do **not** label a fixed drop per pipe as actual physics: pressure loss is **flow-dependent** and must tend to zero when flow is zero.
- Use a documented, calibrated pressure equation and explicit units before exposing permanent kPa ratings. The present abstract gas amount and provisional 0.20 kPa/pipe transport loss are not accepted physical constants.
- Network operating pressure may temporarily exceed a *pipe* rating. It must **not** be silently clipped by PressurizedGasStorage. The solver applies mass/energy inputs, computes pressure, handles safety/relief mechanisms if fitted, checks structural limits, applies rupture and leak losses, then resolves delivery. A boiler vessel has its **own** limit and failure policy; a weak pipe is not an excuse to bypass boiler safety.
- Connected-component topology is rebuilt only when connection-relevant blocks/ports/chunks change, not by rescanning the full network every game tick. Once built, process active/pressurized networks at a configurable bounded interval; keep event response immediate on the next pressure evaluation. Unloaded chunks are never force-loaded or traversed.
- A network is bounded for performance. A 256-loaded-pipe scan is the current implementation limit, **not** a final maximum usable network size or a rule that gas disappears beyond 256 pipes. Oversized graphs must be partitioned/scheduled, not silently truncated into fake connected networks.

## 4. Sudden failure contract

1. The authoritative server computes pressure available at a steam pipe. For the first version, use the component's bulk pressure consistently and document that local gradients are approximate.
2. Compare against the pipe's material/quality pressure rating, including explicit temperature derating if/when defined. **Rating values, material tiers and safety margin are not decided in this document.**
3. If the rating is exceeded, build a bounded set of **loaded, intact, physically pressurized, eligible pipe blocks** whose local pressure is over their individual structural rating. Pipes in unloaded chunks, detached/unpressurized branches, already ruptured blocks and non-steam ducts are not candidates.
4. Choose **one** of these eligible pipes using the server-side RNG, optionally weighted by material weakness or overpressure severity. This randomness is only in *which* eligible pipe ruptures, never whether an unsafe network can indefinitely ignore its pressure limit. No progressive warning or damage accumulation.
5. Replace the chosen pipe immediately with its ruptured variant, preserving connector directions and choosing an exposed leak orientation. Send normal block-state updates so the client sees the hole and steam plume. The network registers the vent and marks topology/conductance dirty.
6. Remove steam from the **conserved network reservoir** using a pressure- and opening-dependent leak rate, bounded by available inventory. Losing steam also removes corresponding thermal energy. The pressure solver recalculates the new state.
7. A ruptured section stays connected and continues venting while steam is available; nearby machines experience pressure/flow loss. The failure persists through saves, chunk reloads, and client reconnects. Repair/replacement ends the vent.
8. A connected network with ANY existing ruptured segment does not create another automatic rupture while that vent remains. Repairing a pipe before fixing continued overpressure may cause a new single failure at the next evaluation. This prevents runaway chain failures and constant maintenance.

Do not pick any random loaded pipe in a component if its computed local pressure is *below* its rating. In the initial uniform-pressure approximation, that condition is simplified to the common component pressure. Safety valves/regulators should be designed as explicit upstream control components, never an automatic hidden correction by the pipe manager.

## 5. Repair and visible effects

- Default break is immediate: no precursor warning crack stages. A ruptured pipe must have a clear hole/seam opening, revised model geometry and a visibly directional steam escape. The current recolored Bronze Air Duct is **not** an accepted final Steam Pipe model.
- Failure and leakage are server authoritative. The client derives particle/sound intensity from a small synchronized leak activity/severity state and only emits effects for visible nearby damaged blocks. No entity-based steam simulation, per-frame network scans, giant transparent quads, or falsely stretched UVs. Visual particles do not determine network consumption.
- In the first implementation, replacing the ruptured pipe with a real intact pipe restores the seal only if the player has the replacement item and a valid connection. Do not allow a free right-click reset. Recommended operating procedure: shut off/isolate, depressurize, physically replace; decide the exact repair tool and safety interactions alongside Steam Age gameplay.
- Intact and ruptured pipe geometry must support plausible flanges/seals, elbow orientation and a non-floating breach. Verify directional textures, colliders, occlusion and view angles in-game. Reference concept art and an editable Studio model are separate visual design work; do not substitute a concept image for the actual block model.
- Steam loss may cause pressure collapse, reduced engine output and wasted boiler fuel. An audible hiss and nearby plume tell the player **where** to repair, rather than showing anonymous damage in a GUI.

## 6. Network persistence and conservation

Steam must not duplicate or vanish during split, merge, placement, breakage, save/reload, unloaded chunks, or dimension transitions.

- Define an explicit inventory ledger: **before + admitted - delivered - vented - intentional drained = after**, with thermal-energy balance also checked. Every transaction has exactly one authoritative owner.
- On split, divide the existing gas inventory and energy by enclosed internal volume, or by a more detailed spatial distribution if implemented. Use deterministic integer rounding (largest remainder) so the sum of children is **exactly** the parent amount, aside from the documented steam vented by physically lost pipe volume.
- On merge, combine the component inventories and energy exactly once. Recalculate volume/pressure and then evaluate the resulting overpressure; do not silently cap steam to a newly recomputed capacity.
- On pipe removal, explicitly vent the gas fraction attributable to removed volume; don't delete all network steam. On placement, never create free steam.
- On chunk unload, close the edges to unloaded terrain, persist/seal isolated partitions and never simulate or select failures in absent chunks. Reconnection restores the saved partition exactly once. A transient unloaded boundary must not drain or duplicate shared inventories.
- All allocations, topology IDs, partition transitions and mutations happen on the server thread. SavedData schema is versioned, changes call setDirty(), and stale/corrupt entries are validated. Client-side saves or particles cannot alter server inventory.
- Stable component identity must not depend solely on a single pipe position; topology can split/merge/change and an anchor may be broken.

## 7. Proposed code layout (NOT yet implemented)

- SteamNetworkManager: per-dimension loaded graph, pipe ratings/ports, dirty rebuild, event-driven updates, single simulation owner.
- SteamNetworkSavedData: versioned inventory/energy/partition records.
- SteamNetworkState: component inventory, volume, bulk pressure, temperature, known sources/consumers, ruptures and cached vulnerability candidates.
- SteamPipeMaterialDefinition: JSON-driven volume, resistance, pressure/temp ratings and rupture geometry properties; registry or JSON validation and clear errors.
- SteamNetworkSolver: transfer/pressure calculation, conservation transactions, relief controls and validated limit checks.
- SteamRuptureHandler: find eligible pipe, pick one using server RNG, replace block and start server-side vent.
- SteamPipeBlock and RupturedSteamPipeBlock: geometry, sided connectivity, leak direction and synced blockstate; **no individual pipe tickers**.
- SteamLeakRenderer / client particle effects: client-only, nearby visible ruptures and rate-limited emission.
- Existing IPressurizedGasHandler/CapabilityPressurizedGas: retain compatibility while defining network boundary ports; do not silently change old AIR charge behavior.

This is a proposed architecture; none of these new classes except the existing SteamPipeBlock and generic transport components currently exist.

## 8. Acceptance tests before claiming implementation is complete

1. Test pressure < rating and == rating: no rupture. A pressure strictly above rating causes **one immediate eligible rupture** at the authoritative pressure evaluation.
2. Verify a random eligible **loaded** pipe fails, not the nearest by accident; different seed/position choices are supported. A higher-rated pipe below its own limit does not rupture.
3. Test no boiler supply, zero network gas, blocked/stalled production, and an isolated unpressurized branch: no spurious rupture or steam particles.
4. Once ruptured, steam loss is deducted each server update; pressure falls; upstream continued supply can maintain the leak. Only an intentional repair ends it.
5. Verify inventory and energy equations under pipe break, split, merge, direct/branch output, save/reload, chunk unload/reload and cross-dimension isolation.
6. Test a full producer/consumer network, multiple boilers/engines, valves, a weak material section, high temperature, and safety relief. Verify behavior without creating duplicate steam or energy.
7. Ensure air ducts, liquid water pipes and legacy bellows do not connect to, rupture with, or receive STEAM. Existing Bronze mechanics and save data remain identical.
8. Check ruptured model, opening direction, UV proportions, leak particles, frustum/distance culling and no hidden geometry; test from all sides with shaderless Minecraft rendering.
9. Profile many loaded networks and ruptures: no full BFS every tick, no per-pipe BE/ticker, no per-frame heavy allocations, and bounded server/client cost.
10. Run ./gradlew build, then single-player, dedicated-server and actual two-client test plans. Static source/design review is not compilation or gameplay verification.

## Scope of this decision

**Accepted:** Sudden pipe rupture after a pressure threshold is exceeded; randomly choose an eligible pipe, persistent visible breach and conserved steam venting; separate network state from block condition; no normal per-pipe BlockEntities.

**Not yet approved or implemented:** numeric ratings, boiler design, Steam Age recipes, explosion or player-damage radius, stochastic early failures, staged fatigue, pressure regulator mechanics, specific model/texture, component wear, actual fluid/steam phase physics, or runtime changes to the existing PR. These must be decided and tested separately.

## Initial implementation notes (development, not final Steam Age physics)

- `SteamNetworkSavedData` stores only nonempty steam-bearing positions in dimension SavedData. Parcels split/merge with pipe topology, and the solver redistributes connected loaded shares deterministically without a BlockEntity per pipe. This sparse representation is an initial way to conserve inventory while a full network/port simulation is designed.
- `SteamNetworkManager.acceptSteam`, `simulateAcceptSteam`, `takeSteam`, and `gaugePressureKPa` are server-only entry points. `PressurizedPipeNetwork.transfer` sends STEAM source output into the ledger rather than teleporting it past pipes to a distant receiver.
- `RupturedSteamPipeBlock` is a distinct persistent block ID with a leak facing, connection geometry, and client-only active particles/sound. A rupture never spreads to other pipes in its connected loaded component while that pipe remains ruptured.
- First gameplay calibration is intentionally **provisional**: reference temperature 453.15 K, 100 pseudo-kPa per abstract unit per pipe, configurable default rupture limit 600 pseudo-kPa, and a bounded leak evaluated every 20 loaded server ticks. These are not physical steam constants and should be redesigned with boiler/volume/enthalpy specifications.
- No passive wear, no random idle failure, no damaged warning state. Replacement is physical: break the ruptured pipe and place a new intact pipe. The current vent overlay is a diagnostic visual, not final Minecraft-style pipe geometry.
- Limits: the solver works on loaded connected components of at most 256 pipes; larger components decline new transfers rather than silently minting or draining gas. It recomputes connected topology on operations and active components every 20 ticks, not yet an event-cached production implementation. Unloaded parcels remain persisted; no forced chunk loads.
- **Not implemented:** boilers, actual water-to-steam conversion, steam engines, pressure valves/safety upgrades, real steam equation of state, configurable per-material ratings, repair equipment, real end-to-end Steam Age gameplay, in-game verification.
