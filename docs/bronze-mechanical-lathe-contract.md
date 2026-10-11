# Bronze Mechanical Lathe — implementation contract

Status: **design and structure contract only**. This file is the starting point for the next Bronze Age implementation. It does not claim the Lathe is registered, formable or functional yet.

## Design

A **5 wide × 2 tall × 3 deep** workshop machine, with the spindle along the five-block width. The working assembly is larger than the Foundry on purpose: do not compress the workpiece, tool carriage and tailstock into one block or scatter floating decorations.

- Left: bronze and iron headstock, mechanical drive pulley, bearing housing, turning spindle.
- Center: visible rough gear/bearing casting held on a real arbor; cutting tool advances into the spinning workpiece.
- Right: adjustable tailstock, bronze supports and a locking screw.
- Front: accessible tool carriage and replacement cutting tool.
- Rear: mechanically connected drive input and guarding; do not put an invisible power receiver behind decorative brickwork.
- Body: timber/stone base and bronze structural details, textured in native 16-pixel tiling without compressed or stretched UVs.

The controller should sit at the **lower-front middle**; all rotations are authored from a canonical south-facing design and exported to four matching JSON blueprints. The machine has a low, approachable profile with a hollow upper working area, not a solid 5×2×3 brick box.

## Proposed footprint (not a live blueprint yet)

South is the service/front side, north is the rear. The two layers shown below are north-to-south rows.

```text
     WEST -------------------- EAST
y=0 (floor, rear -> front)
     F F F F F
     F F F F F
     F F C F F

y=1 (working deck, rear -> front)
     B B B B B
     H . . . T
     B . . . B

F = foundation (candidate: stone bricks or polished andesite)
B = machine base or guard (candidate: wooden or bronze-framed housing)
C = Mechanical Lathe controller
H = headstock/drive assembly
T = tailstock assembly
. = intentionally empty space (no fake full-cube collision)
```

**Materials and exact recipes remain to be implemented and balanced** against the existing Copper and Bronze ages. In particular, headstock and tailstock parts should be crafted and reused as actual components if this can be accomplished without inventing bootstrap loops (e.g. requiring precision bronze bearings to build the only machine that can make them).

## Manufacturing

1. Load one **Rough Bronze Gear Casting** or **Rough Bronze Bearing Casting**. Only one workpiece is held.
2. Install a suitable replaceable cutting tool. The tool must be visible in the machine, and its wear must survive saves, block break and reload.
3. Drive the headstock from an actual Gear V2 mechanical connection. Require a positive minimum RPM and sufficient torque/load headroom before work progresses.
4. Animate the mounted workpiece rotating with the power network and advance the tool along a defined cutting path. When stopped or overloaded, the animation and work must stop rather than magically complete.
5. Finish as a **Precision Bronze Gear** or **Precision Bronze Bearing**. Preserve all original ingot-quality metadata on the output.
6. If stopped, unloaded, dismantled or broken, retain/recover the original rough casting or finished output exactly once; do not duplicate materials through partial progress.
7. The tool loses durability only for actually completed cutting work, not from sitting idle in a powered machine.

## Gear V2 integration

Use the existing `GearNetworkManager.getMechanicalLoadState` and the established mechanical-input-module contracts used by the Copper Washer/Sifter. Do **not** invent a new RPM or Watt simulation, and do not assume Watt alone represents torque. The Lathe is a load and does not generate mechanical power.

First balance values must remain adjustable until live testing. Distinguish stall due to speed from stall due to insufficient torque; show machine condition in-world without requiring a GUI.

## Rendering, collision and documentation

- Static multiblock: Rendering V2 authored volumes -> generated block-local model parts, all four facings.
- Dynamic: spindle/arbor rotation, tool carriage travel, actual input/output workpiece, physically linked to model bearings.
- Voxel V2: honest collision on low base, physical headstock and tailstock; air remains accessible above the work zone.
- No per-tick model regeneration, atlas allocations or unbounded particles. Avoid texture stretching.
- Patchouli: four-direction build layout, crafting requirements, drive side, using/replacing the cutting tool and input/output handling.
- JEI: both rough casting -> precision component processes, with machine as a recipe catalyst.

## Mandatory acceptance tests before calling it implemented

- Four orientation builds from Patchouli; no mirrored headstock/tailstock; correct actual shaft connection in each direction.
- Both gear and bearing casting cycles, plus wrong input, missing tool, broken tool, under-speed, overload and idle.
- No manufacturing from a lone crank if the measured network cannot supply the load; stop/resume without resetting.
- Quality NBT survives casting -> machining.
- Dismantle/reload/network detach mid-cycle; material/tool conservation and no ghost/duplicated parts.
- Check mechanical load propagation and cross-chunk formation (5-wide machine), client FPS and dedicated server.
- Existing Copper machine performance, Foundry and Casting Basin must remain unchanged.
