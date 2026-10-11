# Bronze Age: Foundry and Precision Workshop

Status: development design. The previous Copper Age is the entry point.

**In PR #11:** first Foundry *structure/visual prototype*: 3×2×3 JSON layouts for all facings, new controller, static baked models, collision and Patchouli Visualize. It does **not** yet burn fuel, melt metal or pour castings. See `docs/bronze-foundry-structure-and-casting-contract.md`.

## Identity and boundaries

The Bronze Age is **casting and precision manufacturing**, not an automatic
increase in energy production. Bronze components make heavier, better-aligned
mechanical systems feasible; they do not create free torque or speed.

All actual **multiblock machines remain JSON-defined** and previewable in
Patchouli. Normal single-block workstations are fine where they make sense.
Favor inspectable, physical processes, in-world items and simple interactions
over mandatory GUIs.

### Entry (already implemented)

Cassiterite -> Crushed Cassiterite -> Washed Cassiterite ->
Tin Concentrate -> Tin Ingot; 3 Copper + 1 Tin -> Bronze Charge;
Primitive Furnace + Coal -> 4 Bronze Ingots + Slag.

### Proposed production path (implement by working slices)

1. **Pattern Maker:** work timber into reusable wooden patterns for specific
   parts (gear, bearing and later other castings).
2. **Mold Table:** combine a reusable pattern, sand and binder into an
   actual consumable casting mold. The pattern stays installed and reusable.
3. **Foundry:** heat bronze into a measured molten charge using real fuel/heat.
   Molten metal must be handled physically and cooled safely. Model visibly
   reflects its state.
4. **Casting Basin:** receive a matching mold and a molten charge; produce a
   rough casting after cooling. Wrong mold/material cannot yield a part.
5. **Mechanical Lathe:** machine rough castings into precise mechanical
   components using Gear V2 torque/speed, producing shavings as a byproduct.
6. **Bronze drivetrain upgrade:** bearings, gears and housings raise
   transmission and load limits without increasing generated power.
7. **Bloomery:** processed iron ore + heat/fuel + forced air ->
   iron bloom; working the bloom yields first usable **wrought iron**.

**Bronze Age exit milestone:** the first wrought iron item, after a real
Bloomery and mechanical/hand finishing chain. The bloom and its refining
must not be skipped by a plain crafting recipe.

## Still undecided — do not hard-code prematurely

- Quality tier names/thresholds, including existing vanilla Copper quality
  and the mod's lower quality Copper, and how casting/machining propagates it.
- Full ore-to-iron mineral routes and exact smelting/fuel ratios.
- Bronze melting temperatures and other heat system balance.
- Pattern/mold lifetime and the recoverability of used mold material.
- Numerical torque/speed capacities of Bronze drive parts.

Parts need separate, visible replaceable-component wear as established in
Copper (not a generic machine durability bar), but the new parts and their
exact lifespan are decisions for each machine as implemented.

## Development dependencies

- Copper processing/maintenance PR #10 and JEI multiblock PR #9 are both
  merged into `master`. Bronze PR #11 is rebased directly onto that history.
- Keep the released Discord Copper build unchanged and avoid merging
  unfinished Bronze development into `master`.
- Verify JEI compatibility with the Bronze machine recipes as they are added.
- Create age-specific bug fixes separately if public testers report issues.
- Mark steps as implemented only after source and gameplay checks.

## First milestone

Deliver reusable wooden patterns and real sand molds with a physical
no-GUI workflow, then build the Foundry/Casting Basin around those item
contracts. Never treat an unimplemented downstream process as complete.
