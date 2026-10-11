# Bronze Mechanical Lathe — implementation contract

Status: **structure prototype implemented, source-level only**. The 5×2×3 controller is registered with four rotated JSON blueprints, per-cell baked placeholder models, Voxel V2 collision snapshots, crafting/loot/item resources and a creative tab entry. Formation is intended to be playable; the first Gear V2-driven machining cycle, cutter wear and precision items are implemented in source. Full authored static spindle/headstock geometry, proper Patchouli/JEI and live balance remain incomplete. Compilation/in-game formation have not been verified.

## Design

A **5 wide × 2 tall × 3 deep** workshop machine, with the spindle along the five-block width. The working assembly is larger than the Foundry on purpose: do not compress the workpiece, tool carriage and tailstock into one block or scatter floating decorations.

- Left: bronze and iron headstock, mechanical drive pulley, bearing housing, turning spindle.
- Center: visible rough gear/bearing casting held on a real arbor; cutting tool advances into the spinning workpiece.
- Right: adjustable tailstock, bronze supports and a locking screw.
- Front: accessible tool carriage and replacement cutting tool.
- Rear: mechanically connected drive input and guarding; do not put an invisible power receiver behind decorative brickwork.
- Body: timber/stone base and bronze structural details, textured in native 16-pixel tiling without compressed or stretched UVs.

The controller should sit at the **lower-front middle**; all rotations are authored from a canonical south-facing design and exported to four matching JSON blueprints. The machine has a low, approachable profile with a hollow upper working area, not a solid 5×2×3 brick box.

## First blueprint footprint (implemented prototype)

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

F = Stone Bricks
B = Oak Planks rear upper rail, Cut Copper at the two headstock/tailstock posts
C = Mechanical Lathe controller
H = Cut Copper placeholder post (future headstock assembly)
T = Cut Copper placeholder post (future tailstock assembly)
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

### Current prototype test
Build the lower 5×3 platform of Stone Bricks with the Mechanical Lathe controller at lower-front center. Place five Oak Planks along the back top row and one Cut Copper block at each extreme end of the center top row; leave the remaining upper cells air. Right-click the controller to form. Rotate the layout according to the four directional JSON files. Do not expect spinning or machining yet.

## Initial machining implementation (unverified)

The lower rear middle Stone Bricks block has been replaced with the existing
Gear V2 Mechanical Input Module. This is part of the multiblock blueprint:
place one on the rear-center lower row, correctly connected to the network.

Right-click a formed Lathe with one Rough Bronze Gear/Bearing Casting to mount it.
Right-click with a Bronze Lathe Cutter to install the tool. An empty-hand click
reports progress; sneak-empty-hand retrieves unfinished workpiece or cutter.
A finished component can be collected with an empty hand. One quality-tagged
rough casting becomes exactly one matching precision bronze part. Each
completed cut damages the cutter once. An unfinished casting returns on break.

The first fixed test values are 12 RPM minimum, 2.5 torque demand, and 160
ticks cutting; speed or insufficient load headroom stalls progress without
manufacturing an output. Gear V2's existing mechanical load manager provides
the actual network supply verdict. These are not final balance values.
Dynamic workpiece and cutter display is only a simple, inexpensive first pass,
not the final lathe machine model.

Gradle compile, world reload and all four construction directions still
require actual verification before this milestone can be called finished.

## Sculpted formed model (PR #11)

The first full-cube placeholders have been replaced by 52 authored cuboid volumes for the raised support feet and timber sills, two iron bed rails, bronze headstock with chuck, tailstock and quill, tool saddle and carriage, and front handwheel. Four directional sets of 22 block-local render-part models are committed. This is an intermediate Minecraft-style geometry pass and requires **visual verification in-game**; there are no expensive animated static parts. The mounted workpiece and cutting-tool items remain the only dynamic render elements.

**Generator note:** the authored volume source is `tools/multiblock-models/designs/bronze_mechanical_lathe.json`, but its full design-to-source generator has not yet been run on the committed shape. The 88 part files were generated directly from those volumes for this iteration. Regenerating all models with `build-designs.mjs` and `generate.mjs` may alter them; before doing so, verify consistency and resolve any generator differences rather than accepting unrelated changes blindly.
