# Copper Age: expanded processing and machine maintenance

Development branch: `feature/copper-ore-processing-maintenance`

This is an **experimental** extension of the Copper Age that was merged to
`master` before the public test build. Keep Discord-reported bug fixes on
separate branches; this work must not alter the already distributed build.

## Ore processing

| Material | Grinder | Washer (250 mB water -> 250 mB dirty water) | Sifter | Usable product |
| --- | --- | --- | --- | --- |
| Cassiterite (existing) | Crushed Cassiterite | Washed Cassiterite | Tin Concentrate | Tin via Primitive Furnace (existing) |
| Chalcocite (expanded) | Crushed Chalcocite | Washed Chalcocite | Copper Concentrate | **3 Copper Ingots + Slag** via Primitive Furnace |
| Quartz / Deepslate Quartz | Crushed Quartz | Washed Quartz | **2 Nether Quartz** | Directly usable |

Current Chalcocite alternatives remain valid: direct furnace = 1 Copper
Ingot; crushed-ore furnace = 2 Copper Ingots. The extra Washer and Sifter
stages provide the better 3-ingot yield at higher infrastructure and time cost.

For the two new Washer recipes, Gravel is a **25%** chance byproduct; the
Sifter recipes have a **15%** chance of Flint. Byproducts remain independent
of the primary result. All processing recipes stay editable as datapack JSON.
The previous mandatory Cassiterite -> Tin Concentrate -> Bronze route remains
unchanged.

Other available ore blocks (such as Galena, Sphalerite, and Magnetite) will
be addressed after their age-specific metallurgy and the earlier Ore Process
design notes can be verified; no speculative early Iron/Steel smelting has
been introduced.

## Replaceable parts

Wear is tracked **per completed batch**, not every tick or while idle. Each
installed part has its own NBT counter independent of material slots:

| Machine | Part | Config default |
| --- | --- | --- |
| Mechanical Ore Washer | Existing Leather Belt item | 320 completed batches |
| Mechanical Sifter | New craftable Copper Sifter Mesh | 256 completed batches |

The counter survives chunk unloads and save/reload. Existing machines without
maintenance NBT begin with a fresh installed part. Changing the config
clamps saved counters to the new maximum.

### Wear visible on the actual machine

The wear counter also selects **six persistent visual stages**:

| Remaining life | Visual stage | Mechanical Ore Washer | Mechanical Sifter |
| --- | --- | --- | --- |
| 75-100% | 0 | Original intact leather belt | Original complete copper screen |
| 50-<75% | 1 | Faded leather, first worn edge | Local discoloration, tiny snapped wires |
| 25-<50% | 2 | Abrasion marks and frayed sides | Weathered wires and small holes |
| 10-<25% | 3 | Torn edges, dark stains | Larger ragged openings in the grid |
| >0-<10% | 4 | Heavily frayed but still continuous moving belt | Nearly half the grid torn away |
| 0% | 5 | Snapped, stationary belt and stopped driven drum | Mostly missing mesh; processing disabled |

Sifter wear uses baked meshes that keep the **original eight-piece moving
frame unchanged** and crop UV coordinates when splitting copper bars; worn
mesh models are registered explicitly for loading/reloading. The original
pristine `mechanical_sifter_upper_tray` model is not edited.

Washer damage is rendered directly on the **existing** mechanically moving
leather strip. Its original 0.5-block texture repeat remains intact, and UV
coordinates are cropped (never stretched or compressed) at the frayed edges.
**Until the belt reaches 0% life, its entire drive loop stays continuous: no
missing lengthwise sections while the belt is transmitting rotation.** At 0%,
the snapped model has true gaps and the driven pulley/drum stop.
No replacement-item sprites float over machinery.

When life reaches zero, processing pauses without consuming queued items
or water and removes its mechanical load. The Washer input pulley can still
turn with its upstream Gear V2 shaft, but its broken internal belt no longer
drives the downstream pulley/drum. To check a machine, **sneak-right-click
its formed controller with an empty hand**. To service it, **right-click the
controller normally while holding the correct part item**. The replacement
consumes one item in survival; an unworn part is not wasted.

The reusable `ReplaceableMachinePart` state machine allows adding multiple
component types to a later-age machine without affecting its recipe inventory.
Material-quality effects and salvage of broken parts remain design decisions,
not silently implemented rules.

## In-game regression checklist (must test before merge)

- [ ] Existing saved worlds load with a functional Washer and Sifter.
- [ ] JEI shows both new ore-processing paths, chance byproducts and catalysts,
      once the JEI branch/PR #9 is merged or included for testing.
- [ ] Both Quartz variants can be ground; normal and deepslate outputs agree.
- [ ] Chalcocite produces 1/2/3 Copper with direct/crushed/concentrated paths.
- [ ] Full output tanks/ports stop processing without deleting material.
- [ ] Test 0/75/50/25/10% thresholds with the lifetime settings reduced to
      20-40 cycles so all six stages can be observed without hours of work.
- [ ] Inspect frayed belt and broken mesh on all four machine facings, while
      animating, paused and fully broken. No floating sprites or missing models.
- [ ] Reload resources while models are damaged; worn meshes must stay baked.
- [ ] Confirm the moving belt texture does not stretch, distort or jump at seams.
- [ ] At 1-10% life (visual stage 4) the moving leather belt is badly torn,
      but has NO complete gaps anywhere around its moving loop.
- [ ] At 0% life (stage 5) real missing belt sections appear, but the belt
      itself and driven drum do not continue moving.
- [ ] Broken Washer stops the downstream pulley/drum but not the upstream shaft.
- [ ] Worn-out machinery consumes no items or fluid, then resumes after service
      with an intact model/texture.
- [ ] An invalid held item doesn't repair the machine or enter the input slot.
- [ ] Empty-hand inspection and held-item replacement work in Survival/Creative,
      multiplayer and after a save/reload.
- [ ] Large arrays of loaded machines do not develop rendering or tick lag.

### Build validation

Static checks cover JSON integrity, registry references, changed-file scope
and basic Java structure. A complete Forge 1.20.1 Gradle compilation and
Minecraft test **must still be run** before merging this PR.
