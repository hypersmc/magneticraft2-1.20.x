# Refractory Kiln — optional liquid Wood Tar fuel

**2026-11-08 · Forge 1.20.1 · stage: source implemented, NOT compiled or tested in-game.**
**Branch:** feature/bronze-wood-tar-kiln-fuel (stacked on Iron PR #18, independent of Steel PR #19).

## Purpose and reuse

The existing Charcoal Retort produces **250 mB real Wood Tar fluid plus four Charcoal**
from four Logs. Copper Fluid Pipes/Tanks can transport and store this fluid, but
it previously had no consumer. This adds a meaningful heat use without creating
new machine or fluid registry IDs. Wood Tar **item** remains legacy compatibility
and is NOT converted from or to the liquid.

## Backward-compatible optional multiblock

The FOUR original `refractory_kiln_<facing>` 5x4x5 JSONs are untouched.
Additional `refractory_kiln_tarfuel_<facing>` layouts replace exactly
one SECOND-layer side-wall Brick with the existing real
`multiblock_fluid_input` module:

| Facing | Zero-based layer2 wall coordinate (x,z) |
| --- | --- |
| South | (0,3), west side |
| North | (4,1), east side |
| East | (3,4), south side |
| West | (1,0), north side |

The inlet is positioned one cell toward the front, rather than at the
dead-center of the wall, because the existing shared Fluid Input module
chooses its outward face from the controller-to-module offset. These
positions are exactly 90-degree rotations of each other, ensuring
WEST/EAST/SOUTH/NORTH outward-facing ports respectively without changing
generic module code.

Same input/output item modules, controller, render resource keys, 5x4x5
footprint, Voxel V2 profile, and recipes. The formed Fluid Input remains
a real module at its source world location rather than a filler model;
its own outward-facing fluid port and existing five-tick/250-mB passive
pull logic connect it to the Copper fluid network. Patchouli now puts the fluid-inlet variant FIRST in the main construction
Visualize, with the old solid-fuel-only variant following as a separate
page. The existing SOUTH mirrored column order is preserved.

The dedicated private Kiln fluid handler is passed **only to the formed
Fluid Input module** after verifying its controller link and membership
in the saved original multiblock block map. Neither inventory nor fluid
capabilities are exposed on the Kiln controller itself, and an ordinary
unformed Fluid Input cannot bypass the multiblock.

## Fuel rules

| Fuel | Burn duration | Heat behavior |
| --- | --- | --- |
| Forge-compatible solid fuel item | unchanged Forge item burn duration | existing configurable HEAT gain and loss |
| **50 mB Wood Tar fluid** | **80 burn ticks** | same existing configurable HEAT gain and loss |

The Retort's 250 mB yield provides exactly 400 kiln burn ticks if all
consumed. The new Kiln-only tank holds **4,000 mB** of Wood Tar and
accepts no other fluid. Incoming fluid is buffered without generating
HEAT. Only when the existing kiln already has a valid workpiece,
unblocked output, and heat needs topping up does it burn:
**solid fuel first**, otherwise debit exactly 50 mB from the tank
*before* starting 80 burn ticks. An already burning fuel batch
continues to burn while an output becomes blocked, just as before.
Partial remainders <50 mB cannot ignite. No free HEAT,
automatically minted fluid, new source or changed recipes.

The new tank is persisted in `KilnWoodTarFuelTank`; old saved Kilns
load with an empty tank and unchanged `KilnFuel`/`KilnBurnRemaining`.
As with other current process tanks, drain buffered Wood Tar into
external storage before dismantling; breaking the Kiln does not
mint filled buckets and any remainder is lost.

## In-game regression checklist

1. Original solid-only Kiln (all four facings): forms and makes both
   fired refractory ceramics and heated Iron workpieces unchanged.
2. New optional inlet blueprint (all four): forms precisely as
   Patchouli's *first* Kiln Visualize shows; no missing shell tiles,
   occluded fluid inlet, texture distortion or FPS regression.
3. Charcoal Retort four Logs + normal fuel → four Charcoal + 250 mB
   **liquid** Wood Tar. Pipe liquid through the real Retort outlet,
   Copper Fluid Tank/Pipe and new Kiln Fluid Input.
4. Verify exactly **50 mB used before each 80 burn ticks**; 250 mB
   fuels 400 ticks. Kiln HEAT threshold/gain/loss and recipe progress
   are unaffected.
5. When solid and Wood Tar coexist, solid starts first; it does not
   eat Wood Tar until solid supplies are exhausted.
6. Try water/dirty water and other liquids; all must be rejected.
   Direct controller capability fill/drain and module drain must fail.
7. Block the item output: no **new** fuel starts, progress stalls;
   already burning fuel finishes normally. Check no duplication.
8. Save/unload/reload with stored Wood Tar, a remainder <50 mB,
   and a partially completed 80-tick liquid-fuel burn; confirm exact
   persistence and no fuel reset.
9. Break/unform/reform, test attached copper pipes and standalone
   invalid modules. Confirm no stale links or material duplication.
10. Confirm retained original JEI recipes, original visual guide,
    old save compatibility and Wood Tar legacy Retort items.

Run `node tools/wood-tar/check-kiln-liquid-fuel-contract.mjs`
and full `./gradlew build` under JDK17 before gameplay acceptance.
Neither has been executed against the full source checkout here:
GitHub hostname resolution in the container is unavailable.
Multiplayer-specific tests remain deferred until after Steam Age.

## JEI discoverability — 2026-11-08

The existing `multiblock_processing` JEI display **does not** treat Wood Tar
as a processing ingredient because a single output can consume a variable
amount of fuel depending on retained HEAT, configuration and elapsed burn.
Instead, every actual Refractory Kiln processing recipe now shows a short
optional fuel hint and the live **50 mB = 80 burning ticks** setting.

A separate JEI `kiln_liquid_fuel` category registers a Wood Tar **fluid
INPUT** (not a Wood Tar Bucket or obsolete Wood Tar item). JEI Uses on the
fluid can discover the Kiln; the Refractory Kiln Controller is a recipe
catalyst in both processing and fuel categories. The fuel entry gives the
50 mB / 80 tick conversion and describes the **optional formed Fluid Input
blueprint** from Patchouli. No new physical JSON processing recipe,
crafting item output, heat bypass, fuel registration or simulation logic
is introduced. The quantities come from the actual Kiln BlockEntity
constants, so JEI cannot drift from the in-game 50/80 rate.

JEI acceptance: (1) press U on Wood Tar **fluid**, verify the Kiln fuel
usage; (2) open the Kiln controller's uses and verify the new fuel and
existing processing tabs; (3) inspect any real Kiln processing recipe,
verify the visible Wood Tar fuel hint and burn rate; (4) verify the old
solid-fuel recipes are unmodified and the original solid-only structure
is not described as accepting tar; (5) test localization, no category
clipping, and tooltip clarity at normal Minecraft GUI scale.

JEI source/static test: `node tools/wood-tar/check-kiln-jei-contract.mjs`.
Full JDK17 `./gradlew build` and Minecraft GUI verification remain open.

## JEI structure requirement — 2026-11-08 follow-up

In-game design clarification: the liquid-fuel variant **requires a real
`magneticraft2:multiblock_fluid_input` block** in its 5x4x5 formation;
merely having liquid Wood Tar in a tank near the solid-only Kiln is
insufficient. The new JEI fuel category now shows two nonconsumed
structural catalyst slots: the existing Refractory Kiln Controller and
the registered **Fluid Input block**, plus a Wood Tar fluid INPUT slot.
A visible "Required port" label and text instruct players to use the
alternate Patchouli multiblock blueprint. The existing processing
category now explicitly says **"Wood Tar needs Fluid Input build"**
rather than implying the original Kiln can burn liquid fuel.

The original solid-only structure, the four optional formations,
their models, resource locations, port connections, solid-fuel
operation, 50 mB / 80 burn-tick ratio and save data are all unchanged.
This is only JEI presentation and its resource tests.

## Corrected first construction guide — 2026-11-08

Player feedback identified that the original solid-only Kiln was still
the FIRST Patchouli multiblock visualization. The Wood Tar structure was
hidden two pages later, leading to the impression that the multiblock
had no Fluid Input. The FIRST Refractory Kiln visualization now shows
the real `multiblock_fluid_input` block in the second-layer side wall.
The original 5x4x5 solid-only structure remains available under the
"Refractory Kiln — Solid Fuel" visualization, for existing saves.
The crafting guide, Bronze III introduction, firing text and JEI help
now all refer to the correct first layout. Regression fixtures validate
both book patterns cell-by-cell against the corresponding SOUTH JSON
multiblock definitions after the established reversal/mirroring.

No source machine/formation JSONs or fuel mechanics changed. PR #20
remains stacked and unmerged, so test the actual PR #20 branch in-game
rather than PR #18 or #19.
