# Steam boiler — manual fuel queue contract

**Date:** 2026-11-10  
**Branch:** `feature/steel-age-foundations`, draft PR #19  
**Status:** source implemented; Forge compilation and Minecraft runtime verification pending.

## Scope

The formed Firebox V2 now accepts more than one manual fuel item without adding a GUI, inventory BlockEntity, belt automation or a second combustion system.

The authoritative queue remains owned by the existing vessel `BoilerWarmup` state. This preserves the current one-owner model for water, HEAT, current burn state, local Steam and boiler pressure.

## Player interaction

- Right-click the formed firebox with a supported solid furnace-fuel stack.
- Up to **16 queued fuel items total** may be stored.
- One interaction loads as many items from the held stack as fit.
- Mixed supported solid fuels are allowed.
- Metallurgical Coke keeps its existing explicit 3200-tick burn value.
- Container fuels remain rejected.
- Creative-mode insertion does not consume the player's held stack.

The status line reports:

```text
fuel <current ticks> ticks + <queued>/16 queued
```

## Consumption rules

Queued items are real `ItemStack` records, not converted immediately into anonymous burn ticks.

The next queued item is consumed only when all of these are true:

1. no current fuel item is burning;
2. the firebox/vessel/chimney/water interlocks are valid;
3. the HEAT buffer has enough room for at least one full configured burn tick.

This prevents the queue from consuming new fuel while the boiler is nearly HEAT-saturated merely to throw most of that item's first burn tick away through overflow.

If an interlock fails, queued fuel waits unconsumed.

A current partially burned item is never refunded when extinguished or when a required combustion interlock is broken.

## Persistence

Boiler save version **5** adds a `FuelQueue` list.

Versions 1–4 remain accepted and load with an empty queue.

Queue restoration validates:

- every saved stack is non-empty;
- every saved stack is still a supported solid fuel;
- total queued count is at most 16.

An invalid queue is rejected and cleared **without discarding otherwise-valid water/HEAT/Steam state**.

## Teardown and firebox removal

Unburned queued fuel remains owned inventory and must not disappear.

- Whole-boiler dismantling returns all queued unburned items as world drops.
- Removing/unlinking the formed firebox returns all queued unburned items.
- The currently burning item's remaining chemical-energy budget is discarded, not converted back into an item.
- Queue clearing is claim-once, preventing duplicate returns if firebox and vessel teardown callbacks both run.

## Not included

This slice does **not** add:

- hopper capability;
- Mechanical Transfer Arm fuel insertion;
- conveyor input;
- automatic fuel selection;
- fuel extraction UI;
- ash/byproduct handling.

Those are later automation/combustion refinements and should not be added until the manual boiler loop is verified.

## Verification performed

22 source/resource assertions passed against the committed branch for:

- queue capacity;
- ItemStack ownership;
- stack insertion;
- automatic next-item start;
- HEAT-headroom gate;
- save version 5;
- queue persistence;
- queue teardown drops;
- local boiler-pressure gauge/relief;
- pressure-driven network transfer.

GitHub currently reports no attached CI statuses for this commit. A full Forge build has **not** been verified in the current environment.

## Required in-game regression

1. Build with JDK 17.
2. Form the complete boiler/firebox and provide water/chimney interlocks.
3. Hold a stack of 16 coal and right-click once. Confirm the status shows one current item and the remainder queued (or the equivalent queue state after the next server tick).
4. Let one fuel item expire. Confirm the next item starts automatically without another click.
5. Add a different valid fuel type while already burning. Confirm it queues and is later consumed with its own burn value.
6. Fill the HEAT buffer near capacity. Confirm the next queued item waits rather than being consumed immediately into overflow.
7. Remove water or invalidate the chimney. Confirm the current burn stops according to existing rules and queued unburned fuel remains.
8. Save/reload with multiple queued items. Confirm exact queued count and continuation.
9. Remove the firebox. Confirm all queued **unburned** items return exactly once and the partially burned current fuel does not.
10. Dismantle the entire boiler with queued fuel and repeat the exactly-once recovery check.
11. Re-test disconnected boiler pressure/gauge/relief: pressure must exist with no Steam Pipe attached.
12. Connect a Steam network and verify local boiler kPa and network kPa remain distinct while pressure-driven transfer occurs.


## Runtime confirmation — 2026-11-10

Player in-game testing confirmed the implemented behavior works in the current single-player test world. This confirms runtime behavior for the tested manual fuel queue and the corrected boiler-local pressure/gauge/relief path. It does **not** replace the outstanding full Forge build record, dedicated two-client verification, chunk-boundary coverage, or all teardown/save-regression cases documented above.
