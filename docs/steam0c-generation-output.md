# Steam 0-C — finite phase conversion and Steam-network output

**Date:** 2026-11-10  
**Branch:** `feature/steel-age-foundations`, draft PR #19  
**Status:** source implemented; Forge compilation and Minecraft runtime verification pending.

## Scope

This slice converts the already-working vessel-owned water/HEAT state into a finite, conserved `GasMedium.STEAM` buffer and feeds the **existing** `SteamNetworkManager`. It does not add a second pressure graph, legacy AIR pressure, electricity, an engine, automatic water pumping or a true relief valve.

The accepted Steel IV-C vessel geometry, Firebox V2 structure, service-fitting block IDs and their recipes remain unchanged.

## Default provisional balance

The default datapack rules are intentionally gameplay coefficients, not a real equation of state:

| Rule | Default |
| --- | ---: |
| Water capacity | 8,000 mB |
| HEAT capacity | 36,000 |
| Phase threshold | 24,000 HEAT |
| Water per Steam unit | 250 mB |
| HEAT per Steam unit | 1,000 |
| Max phase conversion | 1 unit/tick |
| Vessel Steam buffer | 16 units |
| Steam network temperature | 453.15 K |
| Max boiler export | 4 units/tick |
| Boiler source cutoff | 500 kPa |
| Existing pipe rupture threshold | 600 kPa |

Format-1 `boiler_warmup/default.json` datapacks remain valid but leave Steam conversion **disabled**. Format 2 explicitly opts into Steam production.

## Conservation and ownership

`BoilerWarmupState` now records `waterToSteam` and `heatToSteam` as explicit sinks. Its invariants are:

```text
water admitted
= water stored + water drained + water spilled + water converted to Steam

generated HEAT
= HEAT stored + passive/flue loss + drained-water HEAT
  + dismantle loss + HEAT converted to Steam
```

The new dependency-free `BoilerSteamState` separately enforces:

```text
Steam produced = Steam buffered + Steam exported + Steam spilled
```

A Steam unit is created only after the water and HEAT debit succeeds. Backpressure never repeats that debit. If the final water charge is converted, any leftover sensible HEAT is explicitly accounted as loss so an empty vessel cannot retain a hot-water energy budget.

## Physical interlocks

Phase conversion requires all of the following to be loaded and correct:

- formed Steel Boiler Vessel;
- linked formed Firebox V2;
- physical Water Inlet;
- all three Flue Chimney sections and clear exhaust;
- physical Steam Outlet;
- physical Safety Service Mount.

The safety mount is only an **installation interlock** in this slice; its visible valve is not yet a genuine venting relief valve.

A Steam Pipe is optional for phase conversion. With no pipe, or with a network refusing more Steam, the vessel's 16-unit buffer fills and conversion stops.

The Steam Pipe must be placed at the exact external face aligned with the baked horizontal takeoff flange. `SteamPipeBlock` recognizes that physical port for visual connectivity. It remains STEAM-only and does not connect to water pipes or AIR ducts.

## Pressure-aware export

The boiler uses `SteamNetworkManager.acceptSteamBelowPressure(...)`. This computes the projected connected-network pressure before admission and accepts only the number of units that keep the shared component at/below the configured source cutoff.

The default **500 kPa** source cutoff is intentionally below the existing **600 kPa** pipe rupture threshold. The two values have different roles:

- 500 kPa: boiler source refuses further normal admission;
- 600 kPa: existing Steam Pipe structural fault threshold.

The boiler cutoff is **not** a relief valve and cannot protect a network that becomes unsafe through another source or topology change.

## Persistence and teardown

Save format 2 adds the phase-conversion ledgers and Steam-buffer ledger. Format-1 boiler saves migrate with zero prior Steam production.

Dismantling:

- spills remaining liquid water;
- dissipates stored HEAT;
- discards remaining fuel budget;
- vents the vessel's buffered Steam;
- does not create water containers, fuel items or Steam items.

Existing structural recovery remains responsible for the vessel/firebox/fittings.

## Verification performed

- Dependency-free Java 17 sanity compile of the new water/HEAT conversion logic and Steam ledger passed locally.
- A dedicated Steam ledger test performed 100,012 assertions, including 100,000 randomized produce/export/reload/spill operations.
- 20 committed-source/resource cross-checks passed for phase ledgers, persistence migration, buffer limits, pressure-aware network admission, physical outlet helper, Steam-pipe connection and format-2 data.
- Full `./gradlew build --no-daemon` was attempted from the available container but DNS could not resolve `github.com`; Forge compilation is therefore **unverified**.

## Required in-game regression

1. Build on JDK 17.
2. Start with 8,000 mB water, complete firebox/chimney, Steam Outlet and Safety Mount.
3. Heat above 24,000 HEAT with no Steam Pipe. Confirm water/HEAT decrease and the status line shows a vessel Steam buffer, stopping at 16 units.
4. Attach a Steam Pipe to the visible takeoff flange. Confirm the buffer transfers into the existing network and status reports network kPa.
5. Use a very short network and confirm the boiler stops admitting Steam at the source cutoff rather than deliberately driving normal operation into pipe rupture.
6. Expand pipe volume and confirm more buffered Steam can transfer without resource duplication.
7. Remove/disconnect the pipe with buffered Steam present; reconnect and confirm the same stored units continue transferring.
8. Save/reload with buffered Steam and again with network Steam; confirm exact persistence.
9. Break the vessel with buffered Steam: structural drops must remain exact while water/HEAT/Steam contents are discarded/vented.
10. Test all four vessel facings; ensure the Steam Pipe visually connects only on the actual horizontal takeoff face.
11. Re-run warm-up, Firebox V2, network rupture and Steel-age regression checks.
12. Dedicated two-client and chunk-boundary tests remain required before completion.

## Deferred

Still not implemented: automatic Water Pipe intake, genuine boiler relief/vent hardware, a calibrated temperature/enthalpy model, condensate, Steam Engine, mechanical output, automatic fuel feed and electrical generation.
