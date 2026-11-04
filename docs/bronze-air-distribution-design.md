# Magneticraft2 — Bronze combustion-air architecture

**Update:** K1 air-charge gameplay remains compatible, but a medium-aware Air/Steam transport foundation has now been introduced as an implementation candidate on draft PR #11. Gas amounts are still abstract; neither full steam thermodynamics nor Bloomery reduction has been implemented. See [pressure-and-gas-networks.md](pressure-and-gas-networks.md).

## A functional transport system, not decorative pipes

```text
Mechanical Power line shaft
        | 16 RPM, 5 torque (real network demand)
Industrial Air Blower — formed FRONT controller outlet
        | (optional direct pressure receiver still supported)
    Bronze Air Duct — 1-block pipe, 6-way connections
       / \ 
      /   +-- Ore Roasting Kiln formed FRONT controller intake
     +------- Primitive Furnace formed bellows module
               (other explicitly pressure-capable receivers later)
```

The passive duct blocks never simulate fluid, store pressure, use block entities, perform autonomous ticking or create new power. Each active blower searches a maximum of **128 loaded duct blocks** from its front outlet every five ticks while it has buffered air. The two interfaces are still Forge's existing `CapabilityPressure.PRESSURE`: source **send-only** and formed machine **receive-only on its actual inlet face**. The source rate remains **one air unit per five powered ticks**, buffer **16 units**, max **one delivered unit per game tick** across all consumers. A branch is selected round-robin *when a transfer succeeds*. Different blowers have independent budgets; an individual receiver may refuse full buffers.

Ducts do not connect to Water Pipes and never query unsided fluid or pressure capabilities. Existing legacy air-charge receivers continue to use integer charges without inventing a kPa rating. New typed gas endpoints apply a provisional pressure differential and pipe-length drop; this is not a physical gas simulation. This deliberate distinction prevents integer `PRESSURE` counts being mistaken for liters, pascals or actual gas mass.

## Why a Bloomery is different

The planned **Bloomery Furnace (5×5×5 concept)** is the first industrial device that should *require* forced combustion air for its iron-reduction recipes. Before adding it, define:

- A physically identifiable blast-air nozzle or tuyere, with one logical input position in each JSON orientation, accessible from the outside without routing pipes through its hot interior.
- Server-authoritative gating on **sustained air delivery, actual HEAT, charcoal fuel, suitable iron ore preparation and flux**. A source with RPM and a stored counter alone cannot reduce iron ore.
- Staged material outputs **iron bloom** and slag with consistent yield and material-quality behavior. Hot consolidation and forging follow. No direct iron-ingot shortcut or silent removal of vanilla smelting compatibility.
- A pressure/flow upgrade that distinguishes a fan's throughput from delivery pressure and models restrictions/tube sizes if the simpler unit system proves insufficient. A cold external bronze duct must not be shown immersed in a ~1200°C reactor; use a proper refractory nozzle or heat-safe connection in the furnace model.
- Failure behavior: stalled blowers, loss of air, blocked exhaust, overtemperature/overpressure, dirty air intake, worn machine components as appropriate to progression. No invented magical air-to-HEAT conversion.

**Important:** The Ore Roasting Kiln's earlier optional +20% processing bonus remains *provisional*. This K1 duct change preserves it for save and recipe compatibility, but it is not a final air-chemistry model. We should replace it only after specifying oxidation behavior and validating backward compatibility. The Refractory Kiln does not automatically gain an air input.

## Acceptance checks

1. `node tools/bronze-air-duct/test-contract.mjs`; run `./gradlew build`. The first is structural/source verification, not Minecraft.
2. Form blower and Ore Roasting Kiln in different positions; route ducts with bends, branches, vertical steps and long sections. Verify the actual connectors light up and match blockstate.
3. With no ducts, old direct connection still transfers. Without Mechanical Power supply neither path produces new air. Disconnect any section: no air continues through it.
4. Test a full receiver, two receivers, multiple blowers, over 128 pipes, unloaded chunks and absent/incomplete receivers. The maximum source budget must remain 1 unit/tick under both pipe and external capability extraction.
5. Save/reload with full blower and kiln pressure buffers, breaking pipe sections and reforming the multiblocks; no phantom capacity/pressure created.
6. Inspect the duct collars and 16-pixel UV density from all sides, confirm Water Pipe networks don't connect, and profile CPU/FPS under load. Dedicated-server and dual-client regression remain mandatory.
