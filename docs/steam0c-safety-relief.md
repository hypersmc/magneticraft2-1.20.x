# Steam 0-C safety layer — live gauge and relief valve

**2026-11-10 — draft PR #19. Source implemented; Forge/Minecraft verification pending.**

## Runtime behavior

The existing `boiler_safety_mount` keeps its registry ID and item. Its runtime block class now carries two server-updated blockstate properties:

- `pressure_step=0..5`: six discrete needle positions;
- `relieving=true|false`: closed/open relief-cap state.

This deliberately avoids a custom BlockEntityRenderer and per-frame allocations. The blockstate is changed only when its pressure band or relief state changes.

The gauge reads the actual connected Steam-network pressure through `SteamNetworkManager.gaugePressureKPa`. A disconnected outlet reads zero rather than inventing vessel pressure semantics.

## Default protection chain

| Layer | Default | Role |
| --- | ---: | --- |
| Boiler source cutoff | 500 kPa | Normal operating control; source refuses further admission |
| Relief opens | 550 kPa | Backup protection for pressure introduced by other sources/topology changes |
| Relief closes | 500 kPa | Hysteresis prevents chatter |
| Pipe rupture | 600 kPa | Existing structural fault mechanism |
| Relief rate | 4 Steam units/tick | Maximum real network inventory vented per server tick |
| Gauge full scale | 600 kPa | Six visual bands, 0 through 5 |

The relief path calls `SteamNetworkManager.ventSteam`, which removes actual conserved units and proportional thermal energy from the connected component. It does **not** create a ruptured pipe, refund water/HEAT or silently delete the boiler's local Steam buffer.

When real network Steam is vented, the safety mount emits white cloud particles and an intermittent hiss. The copper cap visibly lifts while the relief state is open.

## Datapack compatibility

- format 1: warm-up only; Steam and relief disabled;
- format 2: Steam generation/output; relief disabled;
- format 3: Steam generation/output plus gauge/relief fields.

Default format-3 fields:

```json
"relief_open_kpa": 550,
"relief_close_kpa": 500,
"max_relief_units_per_tick": 4,
"gauge_max_kpa": 600
```

## Verification performed

- 62 committed-source/resource assertions passed for properties, pressure source, hysteresis use, conserved vent path, particles/sound, config values, multipart references and proportional UVs.
- Dependency-free `BoilerSafetyLogic` and `BoilerSafetyLogicTest` were added to the existing Java-17 shell test path.
- Full Forge compilation and Minecraft runtime remain unverified in this environment.

## In-game regression

1. Build with JDK 17 and form a complete boiler.
2. Connect a Steam Pipe to the real takeoff. Watch the safety-mount needle move through multiple pressure bands as the network fills.
3. Confirm ordinary single-boiler operation stops at the 500 kPa source cutoff and the relief valve remains closed.
4. For a **temporary test datapack only**, set `output_cutoff_kpa` above 550 (for example 580) while leaving relief-open at 550 and pipe rupture at 600. Confirm the valve opens before rupture, visibly lifts, vents/hisses and closes again at 500. Restore the normal 500 kPa source cutoff afterward.
5. Confirm venting lowers actual network pressure and does not increase boiler water, HEAT or local Steam buffer.
6. Save/reload at low, mid and high pressure; gauge must reconcile to actual network pressure.
7. Remove/reconnect the Steam Pipe: disconnected gauge returns to zero and relief cannot vent a nonexistent connection.
8. Test all four boiler facings and inspect the needle/cap overlays for rotation, z-fighting and UV stretching.
9. Dedicated two-client test: both clients must see the same gauge band and open/closed valve state.
10. Re-run pipe rupture behavior above 600 kPa separately to ensure relief work did not disable the structural fault mechanism.
