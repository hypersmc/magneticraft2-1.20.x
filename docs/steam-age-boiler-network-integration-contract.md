# Steam Age 0 — Boiler, water, heat and Steam integration contract

**Magneticraft2, Forge 1.20.1 — 2026-11-10**  
**State: architecture/design handoff only. No running boiler, water conversion, new port or Steam engine is implemented by this document.**  
**Working branch:** `feature/steel-age-foundations`, draft PR #19.

## 1. Scope and accepted construction boundary

The player has fabricated the Steel IV boiler components and shown the 3×4×5 vessel formed in Minecraft; the current formed model and Patchouli preview are accepted **for now**. Preserve its controller/block IDs, 24 Boiler Shell Panels, two Boiler End Joints, 16 Iron Plate Panels, ten Iron Machine Foundations, one Steel Boiler Vessel Controller, interior air, rotation and V2 static models/collisions. Do not modify those models to add fake pipes or floating valves. A positive player reply about verification is useful but does not replace a recorded, per-test output, Java build or multi-client run.

**Steel IV-C** provides the inert structural pressure vessel. **Steel V** is its validated, non-circular handoff. **Steam 0** equips the vessel to use real water + finite combustion heat to create a finite amount of STEAM and then consume steam in a mechanically loaded engine. The existing Steel controller intentionally has `fluidcape() = false`, `heatcape() = false`, `pressurecape() = false`, zero buffers and no ticker. Do not quietly expose an operational capability to old Steel worlds or make an existing boiler auto-start on load.

### Flow and ownership

```text
Existing water source -> Mechanical Water Pump -> Water Pipe Network
   -> BOILER LOW WATER PORT [Forge IFluidHandler: liquid only]
Existing genuine fuel -> furnace/firebox/HEAT input
   -> BOILER OWNER [water mB, retained thermal energy, phase-conversion budget,
                    boiler steam buffer, formation, quality, safety, fault state]
   -> HIGH STEAM PORT [GasMedium.STEAM only]
   -> SteamNetworkManager + SteamNetworkSavedData [conserved network STEAM]
   -> STEAM ENGINE PORT [actually delivered pressure and gas/thermal budget]
   -> Existing Mechanical Power System [RPM + torque / overload]
   -> exhaust/condensate sink [explicitly accounted for, to be decided]
```

Keep the **liquid**, **HEAT**, **AIR** and **STEAM** pathways physically and logically distinct. AIR/legacy `IPressureStorage` has no valid steam kPa semantics. Network pipes do not turn water into steam; a pressure reading alone does not supply mechanical power. Electrical conversion belongs to a later Age.

## 2. Confirmed current code (no guessed replacement systems)

| Existing component | Actual behavior and limitation | Integration consequence |
| --- | --- | --- |
| `MechanicalWaterPumpBlockEntity` | Uses real source water below; 15 RPM minimum, 2 torque, up to 200 mB/t to a `ForgeCapabilities.FLUID_HANDLER` endpoint via `FluidPipeNetwork` | Use a liquid-water input capability; never use the GAS capability for water |
| `IHeatStorage` / `CapabilityHeat` | Abstract HEAT amount, receive/extract methods | If borrowing HEAT, define an explicit conversion/calibration to boiler thermal state; cannot equate HEAT units to joules without validation |
| `SteelBoilerVesselBlockEntity` | Pure structure controller, saved formation/voxels, no operational tick or exposed fluid/gas | Prefer small server-authoritative operating component or controlled extension with versioned NBT and a manual safe enablement step; preserve saves |
| `IPressurizedGasHandler` / `GasMedium.STEAM` | Sided, abstract conserved quantity, temperature K, gauge kPa; simulate and real calls must match | Steam output must have bounded, orientation-aware capability exposure and real mass/energy accounting |
| `PressurizedGasStorage` | Rejects amounts beyond simplified rated pressure and has provisional temperature model | Do not silently use as a physical boiler equation of state; its clipping does not replace relief/failure |
| `SteamNetworkManager` | `simulateAcceptSteam`, `acceptSteam`, `takeSteam`, `gaugePressureKPa` with sparse SavedData and periodic solve; components capped at 256 loaded pipes | Reuse it, but audit extraction/accepted flow and avoid double-injecting through both the sided transport and direct API |
| `SteamNetworkMath` | Provisional 453.15 K reference, 100 pseudo-kPa per unit per pipe | Do not treat as final boiler/pipe pressure rating or ideal water/steam physics |
| `SteamPipeBlock` | STEAM-only, no BE; connected pipe/port faces expose actual GAS endpoint | Formed multiblock filler blocks do **not** automatically expose gas/water ports; an explicit physical port design is required |
| `steam-network-rupture-architecture.md` | Sudden overpressure rupture, one persistent vent per connected component, conserved losses, no passive pipe wear | Preserve safety rule; an operating boiler also needs a **separate** protective response, not merely sacrifice a pipe |

## 3. Physical attachment contract (design proposal, not added to multiblock JSON)

1. **Lower liquid inlet:** install/attach a real water-intake fitting at the vessel's low side, drawing only Forge water mB from existing pipes; no invisible center/controller fill and no AIR/STEAM connection. It must have a clear facing, accessible side and visible flanged/bolted seal.
2. **Upper steam takeoff:** a distinct high port, connected to existing STEAM pipes and only advertising `GasMedium.STEAM`. Reverse liquid filling through this port is prohibited.
3. **Heat/firebox:** physical supported firebox beneath or alongside the barrel, with explicit fuel inventory/burn timing and measured heat transferred into the water. The vessel's iron supports must not masquerade as burning blocks. The first engine should use **real pre-Steam fuel and pre-Steam pumps**; do not require a Steam-powered machine to start the first boiler.
4. **Pressure/safety service:** visible externally accessible gauge and independent spring relief/safe shutoff, with dedicated tested behavior. A broken or absent valve must not secretly auto-correct pressure. Safety control belongs to the authoritative server state.
5. **Attachment strategy:** before adding blocks, inspect existing formed-filler proxy interaction/capability paths and existing physical item/fluid modules; choose a **minimal compatible** real block/module or adjacent fitting that carries the capability. Do not add arbitrary block offsets or allow placing a pipe against an invisible capability behind the drum. Preserve a correct collision/UV model once port geometry is accepted.

**Layout is not finalized:** exact low inlet side, takeoff position, firebox footprint, fitting count, directional JSON representation, and whether a separately installed operational controller is preferable require visual/formation and reuse review. Keep the Steel IV-C JSON and models intact while deciding.

## 4. Boiler ownership and authoritative state

Only one server-side boiler owner advances conversion on an appropriate active tick. Suggested persisted, validated state:
- formed/inert vs fitted/commissioned, design/schema version; safety installed/available; facing and pressure rating derived from validated material quality when quality transfer is actually implemented;
- liquid-water tank (mB), actual fuel remaining (or correctly accounted external HEAT), stored thermal energy and temperature, boil phase/progress;
- steam buffer amount (abstract units) plus energy/temperature; operating budget and blocked-output state;
- safe shutdown/venting/fault/loss counters, without fabricating a functioning gauge.

Never expose unlimited Forge tank capability or unlimited STEAM extraction. Server state controls limits, tick cadence and sync; client visuals only read state. Save/reload, chunk unload and formation/dismantling must not duplicate water, fuel or steam; active pressure is handled safely on break rather than turning saved inventories into dropped free items. Structure quality metadata is **not currently propagated** through Assembly Station-produced block items; no pressure-bearing operation is permitted until quality is either explicitly stored/validated or a carefully justified uniform starter-grade rating policy is accepted.

## 5. Conversion and conservation (invariants, not numerical balance)

For each authorized operation, compute on the server:
- **Water**: `water_before + admitted_mB - consumed_mB - drained_mB = water_after`. Sourcewater pumping and Forge capability simulate/execute must conserve accepted volume.
- **Steam**: `boiler_steam_before + produced - admitted_to_network - deliberately_vented = boiler_steam_after`. On network admission, only the units the existing manager actually accepts may leave the boiler buffer; no retry without rechecking receiver and source budgets.
- **Network**: `network_before + admitted - engine_consumed - rupture_vented - intentional_drained = network_after`. Separate engine-owned exhaust/water/heat ledger.
- **Thermal energy**: `E_before + fuel_or_external_heat - phase_transition - exhaust_or_vent_energy - heat_loss = E_after` within documented numerical tolerance; no negative or NaN energy, no temperature jump from low-fill buffers.
- **Mechanical energy**: Steam engine output `torque × angular_speed` remains bounded by accepted steam enthalpy/thermal energy per tick, and is reported in the existing Mechanical Power System's RPM and torque conventions. An unloaded/stalled engine cannot generate free work.
- **Simulation**: A `simulate` call may not mutate inventories or thermal state. A subsequent real transfer, without intervening changes, must produce the same accepted quantity.

**Not agreed:** water mB per abstract Steam unit, latent/sensible heat, minimum boiling conditions, operating/rated kPa, max Steam mB-equivalent flow, useful engine torque/RPM/fuel efficiency, relief opening/reset, exhaust/condensation. These need a consistent staged calibration and test vectors. No hardcoded defaults are authorized by this contract.

## 6. Safe implementation and acceptance sequence

**Steam 0-A: non-operational installation and service layer**
1. Inspect existing multiblock module matching/teardown and the actual water-pipe/steam-pipe connection rules; choose physical water, STEAM, heat and relief attachment representations (reuse first).
2. Make Steel-built installation parts and valve/gauge geometry physically believable, count their material use, and provide four-facing JSON/JEI/Patchouli agreement. Keep them inert. Do not add processing or turn on pressure at this stage.
3. Verify two-client model/port states, invalid formation, dismantling with/without installed parts, exact once block returns, all four orientations, and save migration.

**Steam 0-B: water and heat without pressure**
1. Add bounded Forge-water intake and persisted tank, correct sided fill, no duplication on repeated simulate/execute, no free sourcewater generation inside the boiler.
2. Add real fuel/HEAT reception and measured server-side thermal bookkeeping. With an absent/nonconnected water or heat source, process stalls safely; no STEAM leaves.
3. Run water/thermal conservation tests, save/reload, blocked transfer, overflow, chunk boundaries and source shutdown.

**Steam 0-C: controlled generation into the existing Steam Network**
1. Once mass/energy conversion coefficients and pipe/boiler ratings are approved and tested, implement a finite phase conversion to `GasMedium.STEAM` with a bounded machine buffer.
2. Connect exactly one physically located high steam output. Use the existing `simulateAcceptSteam` + `acceptSteam` (or its audited sided-capability transfer, **not both**). Preserve temperature/thermal energy and run output-backpressure tests.
3. Provide genuine relief and observable gauge feedback before allowing sustained generation; validate pipe rupture is a separate network failure and prevent silent reservoir clipping.
4. Test inactive boiler, no-water dry firing, no-fuel, full/outlet-blocked, isolated pipe, one vented pipe, overpressure, split/merge, save/reload and disconnect/reconnect.

**Steam 0-D: first mechanical Steam engine**
1. Only after boiler and network pass, implement a mechanically loaded engine consuming real STEAM, with conserved exhaust/condensate policy and no free output.
2. Exercise torque, RPM, startup/stall, clutch/load isolation, output interruption, temperature/pressure thresholds, multi-engine demand, and cross-client server-authoritative state.
3. Preserve existing Steel III transmission/gear ratios and overload behavior; no electricity or new mechanical graph.

## 7. Acceptance and change boundaries

- Run `./gradlew build --no-daemon` on JDK17 and the existing Steel/steam Node checks. JSON-only checks are not compilation.
- Check actual UI and BE capability faces from all four directions, water-pipe pump compatibility, STEAM-only pipe connections, absent AIR/liquid crossover, precision of source/destination transactions, and saved-world migration.
- Test each step on a single-player integrated server and a genuine dedicated server with two clients; distinguish outcomes.
- Check shell UVs, drum/cap/joint alignment and collisions from all camera sides. No floating fittings, stretched textures, excessive transparent layers or per-frame heavy allocations.
- Profile loaded networks and boiler operation, especially graph traversals on hot paths; the present 256-pipe limit and periodic traversal are a known scaling limitation, not a validated production performance target.
- After installing actual equipment, breaking a formed vessel or a critical port must not drop steam/liquid inventories or leave ghost modules; return correct manufactured parts exactly once. No merge or Steam release until source test output and in-game acceptance are recorded.
- Existing Stone–Steel manufacturing, Gear V2/mechanical behavior, water pump, AIR duct/legacy furnace, and the Steel IV-C vessel model/book remain unchanged unless a demonstrated issue needs a narrowly scoped fix.

**No source modification or operating boiler was made in this design pass. PR #19 remains draft, open and unmerged.**
