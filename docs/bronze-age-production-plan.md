# Bronze Age industrial progression (Magneticraft2 1.20.1)

Status: **approved design direction**, not a claim that the proposed machines are implemented. Commit new content incrementally and preserve Copper-era functionality. Target progression: **Copper workshop -> Bronze industrial factory -> first iron bloom -> Iron Age**.

## Non-negotiable design rules

1. Every formed machine has a concrete purpose: new material transformation, meaningful throughput/quality upgrade, or a connection between established processes. Avoid adding machines solely for item-gating.
2. The existing Mechanical Grinder, Ore Washer and Sifter remain useful. The Stamp Mill is a higher-throughput ore breaker, not a mandatory replacement for the grinder. The Washer concentrates crushed ore with water, and the Sifter classifies washed material. Keep these independent, and do not duplicate their conversions.
3. Use the established JSON multiblock and V2 render/voxel pipelines with physical module blocks for Gear V2 input, item and fluid ports; form from a proper Patchouli multiblock guide with four orientations. Models/UVs must be optimized, native-tiled, not stretched, and avoid overlapping faces. BER only for moving parts.
4. Gear V2 demand must be consumed while processing, released when idle/stalled/broken, and never freeze an upstream machine on failure. Use a consistent load/state protocol. Reserve recipe outputs before consuming input; save unfinished work and don't duplicate items on pause/chunk reload.
5. Recipes use `magneticraft2:multiblock_processing` with machine-specific `machine` IDs, JEI catalyst, book instructions, and datapack-editable operations. Distinguish item/fluid processes from purely visual approximations. Never change the existing Copper chain silently.
6. Respect agreed ingot/material quality levels, including Minecraft Copper quality and lower-grade mod-derived Copper. Add traceable input quality propagation and ore impurity later; don't add multiple incompatible quality systems. Design wear parts as visible, replaceable components whose remaining life persists and wears per completed operation.
7. Preserve mass/material accounting and provide uses or recycling routes for dirty water, tailings, slag, casting sand and metal chips. No infinite yield loops from recycling. Explicitly define input/output for every recipe.
8. Keep Stone/Copper tools useful as fallbacks, with Bronze unlocking scale/precision and not instant free multipliers. Iron Age upgrades machine components and process capability instead of replacing every machine.
9. Treat 3x3x3 only as a convenience, not a footprint cap. Use bigger multiblocks when their purpose and internal mechanical motion warrant it.
10. Do not call work compiled without `./gradlew build`. Test models, blueprints, collision, all facings, JEI, recipe conservation and integrated automation in-game.

## Release sequence and dependencies

| Phase | Role | Production milestones and first verification |
|---|---|---|
| **Bronze I — Casting and machine tools** | *Implemented in PR #11; Minecraft/Gradle validation pending* | Complete and stabilize Pattern Maker -> Mold Table -> Foundry -> Casting Basin -> precision Lathe; bronze bearings/gears, input modules, visible tooling and wear. Prove an uninterrupted and a stalled machining batch; compare ladle and direct-pour losses. |
| **Bronze II — Ore dressing and industrial fuel** | *Stamp Mill implemented in PR #11; remaining machines planned* | Stamp Mill (ore breaker) -> existing Washer -> existing Sifter. Follow with rotary classifier, settling/recycled-water tank, ore roasting kiln and charcoal retort. First test line: 64 ore into product + tailings with real machine power and water. |
| **Bronze III — Manufacturing and networks** | *Planned* | Refractory kiln, forge hammer, rolling mill, mechanical press, boring machine, sawmill, mold-sand mixer, assembly station. Introduce larger line shafts, mechanical item distribution, lubrication, maintenance and meaningful quality differences. |
| **Bronze IV — Iron preparation** | *Planned* | Iron ore concentration, controlled reduction/smelting, flux and charcoal, high-temperature refractories, mechanical air blast, bloomery furnace. First iron bloom completes Bronze's core progression and opens Iron Age. |

## Multiblock candidates (footprints preliminary)

| Machine | Suggested footprint | Input -> output / distinct role | Dependency |
|---|---|---|---|
| Stamp Mill | 5x3x4 | Raw ore -> crushed fraction, optional waste; large inertia and hammer wear | Bronze bearings, Gear V2 |
| Rotary Ore Classifier | 5x3x5 | Classified coarse/fine fraction and tailings | Stamp Mill |
| Ore Roasting Kiln | 5x3x5 | Sulfide concentrate -> roasted feed plus exhaust/impurity | Refractory and fuel |
| Water Settling Tank | 5x3x5 | Dirty water -> reusable clean water + captured sediment, bounded efficiency | Ore Washer |
| Charcoal Retort | 3x4x3 | Wood -> charcoal plus recoverable byproduct/heat | Furnace, airflow |
| Refractory Kiln | 5x4x5 | Clay/sand/flux -> refractory ceramics | Improved fuel |
| Mechanical Forge Hammer | 5x4x3 | Hot metal blank -> forged part | Bronze power |
| Bronze Rolling Mill | 5x3x3 | Heated billet -> sheet/rod | Precision bearings |
| Mechanical Press | 3x3x3 | Sheet/blank -> stamped part | Rolling mill |
| Precision Boring Machine | 5x3x3 | Casting -> precision bore | Lathe toolmaking |
| Mechanical Sawmill | 5x3x3 | Timber -> accurately cut lumber/pattern stock | Gear V2 |
| Foundry Sand Mixer | 3x2x3 | Used sand + binder -> reusable molding sand, with loss | Foundry |
| Industrial Air Blower | 3x3x3 | Gear V2 -> pressurized process air | Forge/Bloomery |
| Mechanical Assembly Station | 5x3x5 | Prepared components -> composite machinery | Bronze III |
| Mechanical Ore Breaker | 3x3x3 | Oversized/irregular rock -> feed for Stamp Mill | Optional precrusher |
| Bloomery Furnace | 5x5x5 | Prepared iron ore + charcoal + flux + air -> bloom + slag | Bronze IV |

Footprints and intermediate chemical recipes are **design proposals**, not final build requirements. Real ores need individual paths; chalcocite, cassiterite, quartz and iron minerals must not all magically smelt identically.

## Initial, existing material chain

* Chalcocite Ore -> existing Grinder -> Crushed Chalcocite -> Washer -> Washed Chalcocite -> Sifter -> Copper Concentrate.
* Cassiterite Ore -> existing Grinder -> Crushed Cassiterite -> Washer -> Washed Cassiterite -> Sifter -> Tin Concentrate.
* Both existing chains are deliberately retained as the Bronze baseline. The new Stamp Mill should yield greater *throughput* or reliable size control rather than eliminating the processing steps.
* Bronze casting is already scaffolded by Foundry, mold production and Lathe. The next engineering job is to connect it to industrial ore dressing.

## Future iron handoff (not yet implemented)

Iron ore deposits -> selective breaking/classification -> concentration -> dewatering/optional roasting according to mineral -> refractory Bloomery + charcoal + suitable flux + Gear V2-blown air -> **iron bloom** -> hot forging/consolidation -> first usable iron. No ordinary furnace recipe should bypass this chain once the gating strategy has been tested and accepted. Existing vanilla iron recipes require a compatibility pass before gating; do **not** silently remove them.

## Acceptance definition for each multiblock

- Functional construction and clear non-GUI interactions, including valid and invalid builds for all orientations.
- A connected mechanical power path, working item/fluid port capability and clear machine status when blocked or worn.
- Persistence of internal inventory, partial progress, power-paused state, wear and pending byproducts; resources are conserved through unload/reload, break and automation.
- A V2 static model plus inexpensive synchronized moving components; no UV distortion, z-fighting, hidden-module gaps or floating workpieces.
- Crafting recipes, JEI recipes and catalyst, Patchouli crafting/multiblock chapters, translations and item/loot models.
- Structural/processing regression tests and verified `./gradlew build`, plus a repeatable in-game checklist.

## Work order

1. Commit roadmap and material-flow table.
2. **Stamp Mill source implemented:** six raw feedstocks, Gear V2 input, replaceable stamp shoe, physical item ports, JSON recipes and guide/JEI integration. In-game animation polish and a verified build are still outstanding.
3. **Still required before the next ore-processing multiblock:** verify ore -> crushed -> Washer -> Sifter end-to-end in-game, including block harvests, full output ports, dirty-water backpressure and automation. Do not label source-level checks as gameplay verification.
4. Implement bounded dirty-water reclamation, then selective metallurgy and Iron Age prerequisites.

## Pre-next-multiblock consistency sweep (2026-10-19)

The registered ore catalog and mining rewards are now fixed as part of PR #11:
all **42 ore blocks** have self-drop loot tables and Stone-pickaxe mining tags,
including Quartz and Cassiterite needed in earlier ages. The previously
untagged Stamp Mill/Lathe controllers are harvestable with Stone pickaxes too.
See [ore processing by age](ore-processing-by-age.md) for the 31-family matrix
and implementation states. This did not change ore worldgen or furnace yields.

The next substantial ore-processing multiblock should be chosen based on a
usable input/output chain, not just a new machine design. Current candidates
remain the Rotary Ore Classifier and Dirty Water Settling Tank; neither is
claimed implemented. Iron ore reduction still needs dedicated concentrates,
refractory technology, charcoal/flux and Bloomery design decisions. Do not
add dead-end iron crushed-items or smelting recipes before that path is agreed.
