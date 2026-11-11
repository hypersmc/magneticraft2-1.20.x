# Steam 0-B1: manual water and fuel/HEAT warm-up

Baseline: `9f163f556364ed7ab3a54da91bcbcd5778122976`, Steel draft PR #19.
This is a narrow runtime slice, NOT a working pressurized steam plant and NOT
completion of all Steam 0-B requirements. Vessel/firebox block IDs, original
material ledgers, models, UVs and footprints are unchanged.

## Player interaction

1. Form the existing vessel and its V2 front firebox. Install the water inlet
   and all THREE chimney sections; leave air above the final chimney.
2. Right-click the physical WATER INLET with a water bucket/container. The
   default capacity is 8,000 mB. An empty container drains water from that
   inlet. Other fluids and tagged water are refused. Full/empty/refused
   interactions do not fall through into placing water/lava in the world.
3. Right-click the FIREBOX with one solid furnace fuel item. The server
   consumes one item after all checks pass. It does not queue more fuel or
   automatically ignite another item. Container fuels (including lava buckets)
   are refused; Metallurgical Coke uses the existing 3,200-tick hearth burn.
4. Empty-hand use on the firebox, vessel/controller/filler, or inlet reports
   water, HEAT and remaining fuel ticks. Sneak + empty hand on the FIREBOX
   extinguishes the current charge without returning the consumed fuel.
5. Smoke is emitted at the actual chimney outlet, sparingly, ONLY while a real
   fuel tick is consumed through a complete unobstructed flue. No idle smoke.

Manual containers deliberately come first. The inlet remains a non-ticking
block with NO automatic Forge fluid capability. The existing pump and water
pipe network are UNCHANGED and CANNOT feed it yet. The shared `IFluidHandler`
implementation is a private vessel-owned interface used by `FluidUtil` for
manual interaction, not an invisible fill capability exposed on the drum.
A subsequent inlet-capability slice must delegate to this SAME state, not
introduce another tank or mass owner.

## Ownership, units and persistence

`BoilerWarmup` is owned solely by `SteelBoilerVesselBlockEntity`. The firebox
routes user interaction to its identity-checked linked vessel. Existing
`BoilerFireboxInstallation.linkedFirebox` checks positions and UUIDs.
The water/heat/fuel ledger is persisted in a versioned `BoilerWarmup` NBT
compound alongside, not instead of, existing assembly/recovery metadata.
Older saves without this compound start empty, cold and unlit. Nothing starts
burning automatically when an old structure is loaded.

HEAT means Magneticraft2's abstract thermal budget: NOT Celsius, Kelvin,
pressure, steam mass or a claim of real-world thermodynamic calibration.
The supplied `data/magneticraft2/boiler_warmup/default.json` is data-pack
configurable and uses provisional warm-up values:

| Parameter | Initial value |
|---|---:|
| Water capacity | 8,000 mB |
| Stored HEAT capacity | 36,000 |
| HEAT per solid-fuel burn tick | 36 |
| Passive HEAT loss per loaded vessel tick | 2 |

The HEAT gain/loss/capacity copy the existing Iron Forge Hearth defaults as
an explicit prototype balance choice. They do not establish a boiling point,
steam conversion ratio, final boiler rating, or valve threshold. Config data
is strictly validated; missing/invalid data disables new filling/ignition.
Existing water remains drainable; invalid reloads extinguish remaining fuel
without creating energy. Capacity reductions reject further intake rather
than clipping already-paid-for water or HEAT. A burning item's HEAT/tick rate
is fixed at ignition and saved, so a data reload cannot change its fuel budget.

## Accounting rules

The dependency-free Java state used by the runtime enforces:

- `water admitted = water stored + water drained + water spilled`.
- `generated HEAT = stored HEAT + passive/flue loss + drained-water HEAT + spilled HEAT`.
- `accepted fuel energy = remaining burn ticks * saved HEAT/tick + generated HEAT + extinguished/discarded chemical budget`.

SIMULATE does not change any field. Drain removes the proportional integer
share of stored HEAT, with the last water removing the exact remainder.
Returned vanilla water is not a new hot-fluid product: its accounted HEAT
leaves this prototype, rather than surviving in the vessel for free refills.
A lit charge continues consuming its finite fuel budget at the HEAT limit;
excess energy is accounted as flue loss, not paused for unlimited later use.

No new ignition is allowed when dry, already burning, at the HEAT limit,
missing its inlet/firebox/three-section chimney, obstructed at its exhaust,
or dependent on unloaded chunks. Known broken components extinguish the
remaining charge. Unloaded dependencies PAUSE fuel, without forcing chunks
or performing offline catch-up. The still-loaded vessel may cool meanwhile.
All mutations and one bounded tick per vessel happen on the server thread.
Dormant cold vessels return before looking up world components. Geometry
and chunk addresses are cached; there is no network search or per-pipe ticker.

Dismantling the vessel explicitly spills water, dissipates stored HEAT and
extinguishes remaining fuel before original block ownership is claimed. These
losses are logged once; no buckets or consumed fuel appear in block drops.
Breaking only the firebox extinguishes fuel and leaves the vessel water/HEAT
available for manual draining/cooling. Chunk unload is NOT dismantling.
The existing 53/18/3-block recovery systems continue to own structural items.

## Verification and limitations

Executed here: `javac --release 17` of the REAL `BoilerWarmupState` and its test,
then execution with 402,913 assertions. Tests include full/empty/negative
transfers, pure simulation, reload/continuation, cap changes, exact integer
water/heat accounting, extinguishing, repeat teardown, corrupt snapshots and
128,000 randomized operations. Run with:

```sh
sh tools/steel-age/check-steam0b-warmup.sh
```

This is NOT Forge compilation or an in-game test. The working container could
not resolve GitHub to obtain a full checkout; Forge dependencies and a full
JDK17 Forge build were not available. Minecraft interaction, smoke, performance,
client synchronization, protection plugins and two-client tests remain unverified.

Still absent: automatic water-pipe intake, an external HEAT capability,
queued/automated fuel, hot-water fluid, temperature/enthalpy model, phase change,
STEAM output, pressure, operating gauge/relief and engine power. The in-world
gauge remains non-functional. The warm-up action-bar messages are currently
English-only prototype text; full localization is not yet implemented.

## Required in-game regression

- Build with `./gradlew build --no-daemon` on JDK17, then use a disposable world.
- Empty vessel + coal, incomplete chimney + coal, blocked exhaust + coal:
  reject before consuming the item. Normal unrelated item placement still works.
- Fill/drain with buckets at the WATER inlet; verify exact 1,000 mB changes,
  eight-bucket capacity, full/empty rejection, no lava acceptance or placed fluids.
- Light one coal/coke at the FIREBOX; confirm one item consumed, ticking HEAT,
  finite fuel, real chimney smoke and no steam or pressure capability.
- Right-click while already burning: do not consume more fuel. Sneak-empty
  extinguish; remove a chimney while burning; drain the last water while burning.
  In each case verify no free fuel refund and appropriate stop behavior.
- Save/reload mid-burn: preserve remaining fuel, water, HEAT, and accounting;
  no extra bucket/coal, no offline catch-up. Repeat across chunk boundaries.
- Dismantle the warm vessel and firebox separately; confirm unchanged structural
  item counts, no water/fuel in block drops, no ghost state and no duplicate loss.
- Test all four facings, survival vs creative, two clients and protected regions.
- Re-run the established Firebox V2 and Steel fabrication/formation tests.
