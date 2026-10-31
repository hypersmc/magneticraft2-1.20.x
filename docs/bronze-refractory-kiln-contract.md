# Bronze Refractory Kiln — V1 implementation contract

**Scope:** 16-machine Bronze Age roadmap · preliminary footprint **5×4×5** · planned follow-on to the Charcoal Retort.
**Status as of 2026-10-31:** K1 structural source prototype authored; **not yet compiled or tested in Minecraft**, with firing/HEAT/ceramics reserved for K2.

## Why this machine exists

The Refractory Kiln produces **heat-resistant ceramics/linings**, creating the material basis for the selective Ore Roasting Kiln, improved thermal containment, and ultimately the high-temperature Bloomery. It is *not* a duplicate smelter for vanilla ores, a second Bronze Foundry, or a machine added just to raise a crafting gate.

The original 16-machine table specifies **clay/sand/flux → refractory ceramics**, with improved fuel as a dependency. Preserve that goal. The registered item catalog already includes ceramic pots and existing sand/clay inputs, and existing machine assets include `textures/block/material/refractory_brick.png`; **there is currently no registered refractory-brick item, prepared refractory batch, or Refractory Kiln controller**.

## Material flow — proposed, not silently approved

1. **Prepare a refractory mixture** from clay and granular mineral material. Flux is an original roadmap concept, but no exact mineral, ratio or effect has been agreed. Avoid pretending the existing ore system already outputs such a flux.
2. **Dry/fire the prepared batch** in the Kiln. Real fuel provides HEAT; the batch progresses only at a sufficient temperature-equivalent energy state. When idle, the thermal mass loses HEAT at a limited rate; output is blocked if full.
3. **Produce fired refractory ceramics** as a traceable item (and later block/lining as needed) with no free multiplication or direct furnace shortcut. Cooling/retrieval and part wear are intentionally separate acceptance milestones.
4. **Consume refractories later** in equipment that actually needs them. The initial Foundry already runs with existing materials; do not silently revise its recipes or retrofit old saves just to make the Kiln mandatory.

The current generic `magneticraft2:multiblock_processing` recipe supports **one item ingredient plus optional one fluid input**, not arbitrary multi-input clay/sand/flux blending. V1 should either use a *pre-mixed batch item* produced through an explicitly defined preparation step or add a carefully validated multi-ingredient recipe extension. Do not introduce a private kiln-only JSON parser without justification.

## Structural proposal — not a finished blueprint

- **Footprint:** 5 wide × 4 high × 5 deep. The controller is accessible from the front at floor level. Four rotated, datapack-driven blueprints must share one canonical layout; no hardcoded formation shortcuts.
- **Lower zone:** fueled firebox and HEAT storage, with visible intake and sufficient physical support. Reuse the existing Magneticraft2 HEAT capability/storage patterns, but do not copy Foundry molten-bronze semantics.
- **Middle zone:** insulating masonry and a discrete accessible firing chamber, with real input/output modules and a clear load/unload path. Keep required AIR genuinely hollow (with V2 anchors if needed), not solid invisible cube collision.
- **Upper zone:** exhaust/flue and inspectable hot masonry, with no unsupported floating roof or transparent effects masking machinery. The 5×4×5 envelope may be revised only by an explicit design decision, not silently during implementation.
- **Fuel:** initially ordinary solid burnable fuel; charcoal from the Retort is a natural supplier. Whether Wood Tar becomes usable as a Kiln fuel is a *separate* balance/transfer decision and must not accidentally treat fluid Wood Tar as an item.
- **I/O:** only real, accessible modules. Do not render decorative fake ports. Recipes, Patchouli Visualize and JEI must match the exact final physical structure.

## Implementation milestones

**K0 — Contract and constraints (this document):** Confirm source-of-truth machine list, material identity, HEAT constraints, proposed 5×4×5 envelope, proper input/output ownership and tests. Nothing is called implemented.

**K1 — Structural prototype:** Register controller/block entity and item/model/loot/recipe; author four validated JSON multiblocks, a model and Voxel V2 collision/selection, actual usable port blocks, Patchouli Visualize. Formation and teardown work in every orientation. No simulated firing until the core is ready.

**K2 — Working thermal/material loop:** Implement prepared-batch and fired ceramic materials, recipe + JEI integration, fuel queue, server HEAT, heat loss, reserved outputs, progress persistence, blocked operation, and deterministic breaking/salvage. Use config-backed provisional HEAT numbers rather than inventing real Celsius. All gameplay stays server-authoritative.

**K3 — Verification:** Run `./gradlew build`, JSON/source regression tests and actual single-player and multi-client tests. Validate chunk reload, blocked output, incorrect materials, fuel accounting, thermal pause/cool, broken machine/port cleanup, JEI/book parity and no duplicate outputs. Visually inspect all facings; defer decorative finishing but not broken/missing textures or structural errors.

## Preserved future decisions

- Exact kiln charge recipe(s), clay/sand/flux proportions and byproducts.
- Fuel efficiencies, firing temperature-equivalent thresholds and processing time.
- Required ceramic quality levels and the future interaction with the established ingot/material quality system.
- Replaceable insulation/liner wear life and explicit visual wear signal.
- Whether the Ore Roasting Kiln is independent or shares refractory parts (must remain a distinct role per roadmap).

**Do not use this document to claim implementation, compilation, or in-game acceptance.**

## K1 front-wall revision (2026-10-31)

The first in-game screenshot exposed an overly large, two-block-high front opening. The K1 revision keeps the **5×4×5** envelope, controller, item ports, hollow internal chamber and roof flue, but replaces the front opening with a masonry wall: lower front `BUBOB` (real item ports in bricks) and upper front `BBABB` (one-cell loading aperture). All four blueprint rotations, block-local faces, Voxel V2 occupancy, Patchouli Visualize and static structural assertions must be regenerated together. K1 still has no fuel, HEAT or production recipes; the aperture is an unclosed prototype opening, not a simulated hatch or firebox.

## K1 source implementation candidate (2026-10-31)

A structural-only implementation has now been authored: 5×4×5, 4 JSON facings, a complete front controller, two real front item modules, hollow chamber, open roof flue, Voxel V2 block-local snapshots, vanilla masonry prototype textures, Patchouli Visualize and a prototype controller recipe. Input is intentionally rejected until K2. No HEAT simulation, refractory products, wear, fuel, or production recipe is claimed. Model aesthetics are provisional. Compilation and in-game formation/regression remain unverified.
