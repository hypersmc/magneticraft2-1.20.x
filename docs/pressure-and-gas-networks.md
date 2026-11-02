# Air and Steam Transport — architecture and verification

Status: implementation candidate on draft PR #11. Neither Forge compilation nor Minecraft runtime was verified when authored.

## Stable system terminology

- **Mechanical Power System**: the existing shaft, gear, belt, pulley and mechanical-load network, with RPM, ratios, torque, overload and stall. Its legacy Java names (`GEAR` package / `GearNetworkManager`) are retained for API and save compatibility, but **Gear V2 is no longer a gameplay name**.
- **Pneumatics / Air Network**: pressurized air, duct routing, flow demands and combustion-air supply. A mechanically driven blower is one possible compressor, not the identity of the transport network.
- **Steam Network**: physically separate steam pipes, medium-specific pressure/temperature storage and typed input/output ports. Boiler heat, water conversion, steam quality and engine behavior remain later work.

## Implementation contract

- `GasMedium`: AIR / STEAM. `CapabilityPressurizedGas.GAS` exposes a sided `IPressurizedGasHandler`. It is **not** Forge fluid transport and must never silently map one medium to the other.
- `PressurizedGasStorage`: gas amount, capacity, a pressure approximation in **gauge kPa**, temperature in **Kelvin**, and bounded transfer. Gas amounts are *abstract conserved units*, not liters, mols, or direct kPa values. The simple temperature/fill pressure relation is an explicit gameplay approximation, not an ideal-gas or water-steam equation of state.
- Sided port implementations must be deterministic: `simulate=true` must predict exactly what an immediately following real transfer of the accepted amount will do on the **same server thread**, with no intervening state changes. The transport calls source extraction first; nondeterministic destinations are a programmer error and are rejected, not silently compensated through free gas.
- Source machines own generation, mechanical/thermal energy consumption, per-tick production and output budget, tanks and save data. The network owns **only traversal and delivery**, never generation.
- `PressurizedPipeNetwork`: searches a maximum of 128 loaded air ducts or 256 loaded steam pipes, following six faces. No chunk loading, per-pipe block entities or autonomous pipe ticks. A transfer follows a positive pressure differential after a provisional resistance of 0.20 kPa per pipe; selects a receiver fairly after successful delivery. This distance loss is a first gameplay approximation, **not** fluid dynamics or final pipe ratings.
- `BronzeAirDuctBlock` keeps its existing state and ID. `SteamPipeBlock` adds a distinct STEAM-only block ID and uses the existing UV-correct, cached connected geometry with iron texture. Neither links to water pipes or the other medium's pipe.
- `LegacyAirPort` bridges pre-existing integer `CapabilityPressure.PRESSURE` air charges and formed furnace modules without rewriting NBT. Legacy air charges have **no meaningful kPa measurement**, so pressure-drop gating is skipped whenever a legacy adapter participates. The blower keeps its existing 16-unit buffer, 1 unit/5 powered ticks and max 1 unit/tick output, including combined direct/capability/duct consumers.
- The old `IPressureStorage` capability is intentionally retained. No silent removal of older blocks, saves, direct blower connections, input/output faces, or furnace recipe behavior.

## Progression boundaries

This work makes a Steam Pipe block with a usable 6-directional transport infrastructure and a typed machine capability, but does **not** invent a generator, boiler, turbine, conversion recipe, valve or forced air requirement for an unimplemented Bloomery. Steam Pipes are Creative-only until their material quality, recipe, pressure rating and Steam Age prerequisites are agreed. The Ore Roasting Kiln's provisional +20% combustion-air bonus remains unchanged.

A future Steam Age implementation must specify: boiler water consumption, heat budget, pressure limits, reliable relief and fault handling, dryness/condensation, thermal exchange, blast behavior, fluid quantities, save migration, pipe material quality and machine input/output connectors. Actual pressure distribution and branch competition must be validated under load before describing this as a complete simulation.

## Test and review checklist — unverified

- [ ] `./gradlew build` (Forge 1.20.1 with JDK 17).
- [ ] Cold-start and resource reload with both pipe models: inspect all six connection directions and every UV face from multiple camera angles.
- [ ] Blower -> air ducts -> formed Ore Roasting Kiln and Primitive Furnace; old direct connection still works, no changed saved air balance or recipe output.
- [ ] Full/empty buffer, stalled blower, multiple blowers/receivers, branch fairness and per-tick transfer rate across direct and capability extraction.
- [ ] Water/AIR/STEAM pipe connections are always isolated; native steam endpoints only connect from explicitly exposed faces.
- [ ] Simulated vs real transfers conserve exactly the same number of units; failed transfers don't create gas. Test different pressures, line lengths, max-flow caps and temperature limits.
- [ ] Save/reload vessels with legitimate and corrupted/wrong-medium NBT; prevent wrong-medium migration or overpressure.
- [ ] Break/reconnect pipe, move unloaded chunk boundary, enforce 128/256 block traversal bounds. Verify no chunk loads, tick spikes or accidental per-frame allocations.
- [ ] Dedicated server and two-client checks, including machine formation/rotation and client connection rendering.
- [ ] Verify no Steam Age survival recipe bypass and no Bloomery gameplay changes.
