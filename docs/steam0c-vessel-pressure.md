# Steam 0-C vessel-pressure correction

**2026-11-10 — draft PR #19. Source implemented; Forge/Minecraft verification pending.**

## Why this correction exists

Player testing exposed a conceptual error in the first safety implementation: the boiler-mounted gauge read only connected **network pressure**, so a disconnected boiler with stored Steam displayed 0 kPa. The relief valve also vented the pipe network rather than the pressure-bearing vessel it was physically mounted on.

Format 4 corrects that architecture.

## Two pressure domains

### Boiler vessel pressure

The vessel-owned `BoilerSteamState` is now explicitly pressure-bearing. Its pressure is calculated from:

- locally stored Steam units;
- configured Steam temperature;
- configured vessel Steam-space volume.

The provisional gameplay equation deliberately uses the same unit scale as the pipe network:

```text
boiler kPa = 100
           * (stored Steam units / boiler pipe-volume equivalents)
           * (Steam temperature / 453.15 K)
```

Default `boiler_volume_pipe_equivalents = 2.5`.

At the default 453.15 K:

| Boiler Steam | Boiler pressure |
| ---: | ---: |
| 0 units | 0 kPa |
| 10 units | 400 kPa |
| 13 units | 520 kPa |
| 14 units | 560 kPa |
| 16 units | 640 kPa |

These are gameplay coefficients, not a claimed real boiler equation of state.

### Steam network pressure

Steam pipes keep their existing shared network pressure calculation and rupture behavior. Network pressure is not used as the boiler gauge value in format 4.

## Outlet flow

The old hard 500 kPa output ceiling is not used by format 4.

Instead, transfer is pressure-driven:

1. compare current boiler pressure with connected network pressure;
2. if boiler pressure is not higher, transfer nothing;
3. try up to the configured per-tick output batch;
4. project boiler pressure **after** those units leave;
5. use the existing network projection API to ensure the receiving network would not end above that post-transfer boiler pressure;
6. transfer only the largest discrete amount that satisfies both sides.

This gives discrete pressure equalization without creating a second network solver.

Legacy format-2 and format-3 datapacks retain their previous network-cutoff behavior for compatibility.

## Gauge and relief

In format 4:

- the boiler gauge always reads **local vessel pressure**;
- it works with no Steam Pipe connected;
- the relief valve acts on **local BoilerSteamState inventory**;
- default relief opens at 550 kPa;
- default relief closes at 500 kPa;
- default vent rate is up to 4 real Steam units/tick;
- vented units are recorded separately in the Steam conservation ledger.

The conservation rule is now:

```text
Steam produced
= Steam stored
+ Steam exported to network
+ Steam vented through relief
+ Steam discarded on teardown
```

The visible valve cap, white vent particles and hiss are therefore backed by actual removed Steam.

## Save compatibility

Boiler save format is now version 4.

- versions 1–3 remain accepted;
- old Steam snapshots migrate with `SteamVented = 0`;
- format-4 saves persist local relief vent accounting.

Datapack formats are separate from save versions:

- format 1: warm-up only;
- format 2: legacy Steam output cutoff;
- format 3: legacy network gauge/relief;
- format 4: pressure-bearing boiler vessel and local safety hardware.

## Required in-game regression

1. Disconnect every Steam Pipe. Heat the boiler and confirm the gauge climbs as local Steam accumulates.
2. At default values, around 14 stored units should cross the 550 kPa relief threshold. Confirm cap lift, hiss/white venting, local Steam decreases and gauge falls.
3. Confirm relief closes again at/below 500 kPa.
4. Connect an empty Steam Pipe network. Confirm Steam transfers from higher boiler pressure toward lower network pressure.
5. Confirm transfer stops when one additional discrete unit would make network pressure exceed the post-transfer boiler pressure.
6. Verify the status line shows **boiler kPa** and **network kPa** separately.
7. Save/reload with local Steam and again while the relief is open/near threshold. Conservation must remain valid.
8. Confirm legacy format-3 datapacks still use the previous network-domain behavior.
9. Re-test pipe rupture above 600 kPa as a separate network-side structural fault.
10. Test all four boiler facings and two clients for gauge/valve synchronization.

## Verification performed

22 focused committed-source/resource assertions passed for:

- local pressure helper;
- pressure-domain switch;
- local relief debit;
- projected pressure transfer;
- Steam vent conservation;
- save-format-4 migration;
- datapack format-4 plus legacy format-3 path;
- configured vessel volume and thresholds;
- dependency-free pressure test registration.

A full Forge build and Minecraft runtime test have not been executed in this environment.


## Runtime confirmation — 2026-11-10

Player in-game testing confirmed the implemented behavior works in the current single-player test world. This confirms runtime behavior for the tested manual fuel queue and the corrected boiler-local pressure/gauge/relief path. It does **not** replace the outstanding full Forge build record, dedicated two-client verification, chunk-boundary coverage, or all teardown/save-regression cases documented above.
