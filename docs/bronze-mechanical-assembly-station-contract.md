# Bronze III — Mechanical Assembly Station: K0 functional contract

**Status:** Design / dependency contract only. No new block, renderer, registry, machine logic, crafting or recipe serializer has been implemented in this milestone. Draft PR #11 remains unverified for a full Gradle build. 
**Roadmap:** [Bronze industrial progression](bronze-age-production-plan.md). This is the next *planned* Bronze III multiblock after the existing source-implemented machines. The Industrial Air Blower is frozen for now; Steam Age work is deferred.

## Why the station belongs in Bronze

The Mechanical Assembly Station is a **powered, repeatable way to combine several manufactured components into existing functional devices**. It is not a furnace, a stamp/press, a new source of rotational power, or a way to mint parts. Its distinct purpose is to allow Bronze workshops to turn prepared components into assembled machinery **through the established item belts/transfer arms and Mechanical Power System**, while manual crafting stays available.

The existing mod already has Pattern Maker, Mold Table, Foundry, Casting Basin, Lathe, Precision Boring Machine, Press, Sawmill and smaller components. Assembly makes those production paths useful at scale instead of making those machines obsolete. Material quality is preserved where supported; assembly must not silently upgrade low-quality components. No unreleased ore, mystery adhesive, fictional ingot grade or decorative gated intermediate is needed.

## First candidate real product — existing Mechanical Water Pump

The current shaped-crafting variant `recipes/mechanical_water_pump_bronze_bushing.json` consumes:

- **2** planks (vanilla `minecraft:planks` item tag)
- **1** `magneticraft2:bronze_bushing`
- **1** `magneticraft2:shaft_wood`
- **1** `magneticraft2:pulley_small_wood`

and produces **1** `magneticraft2:mechanical_water_pump`.

**Proposed first Assembly recipe:** exactly these same items and counts -> one Water Pump, driven by actual mechanical load over processing time. No free extra output. The manual crafting recipe **remains** for players who prefer it. A powered assembly line adds automation and repeatability, not mandatory gating. The Water Pump is a real machine already in the mod and provides an immediately testable downstream use. This recipe is a **candidate K1 implementation**, not yet an agreed numerical duration/torque value. Once the serializer is ready, author it as a datapack recipe, not embedded Java constants.

A second candidate after the first loop works is the existing Mechanical Transfer Arm (3 Copper Plates, 1 Chest, 2 Wooden Shafts, 1 Medium Wooden Gear). Do **not** add that recipe or further products in K1 just for quantity. Both examples are grounded in existing vanilla-shaped crafting recipes.

## Required multi-input recipe support — avoid accidental rewrites

The existing `magneticraft2:multiblock_processing` serializer and `MultiblockRecipeHandler.findRecipe` match **one input item/count** and an optional fluid. They cannot truthfully implement an assembly with 4 distinct inputs.

Before K1, implement a compatible reusable contract for a list of required `Ingredient + positive count` entries. Choose the smallest maintainable approach **after verifying all existing JEI, network-serializer and controller callers**:

- Preferred if safe: add a clearly distinct opt-in multi-input form to the existing shared recipe format, preserving every old one-input JSON and network behavior. The Assembly controller uses a new explicit multi-inventory matcher instead of silently assuming slot 0.
- Otherwise: introduce a dedicated `multiblock_assembly` recipe type with correct server/client sync and JEI display, **without copying** or breaking the existing processing parser.

Never reinterpret the old `input` field as a list, silently alter recipes for the Kiln/Press/Washer, or conflate four ingredients into a fictional preassembled single item solely to avoid extending recipe support.

Recipe contracts must define deterministic matching if several recipes share ingredients; positive counts and bounded ingredient-list sizes; tag support; save/reload recipe identity; and clear errors for missing items, invalid numbers or ambiguous recipe definitions. The input/output ports must expose correct automation capabilities, not phantom decorative inventories.

## Proposed physical scope (provisional 5 x 3 x 5)

**Footprint:** 5 wide × 3 high × 5 deep (canonical roadmap placeholder, subject to actual model layout tests). The station should show a substantial horizontal assembly bed, guided clamping fixture, visible machine-driven press/fastener action and an accessible front work surface. It should fit established Bronze wood/iron/bronze production machines without turning into a modern robotic factory.

Use existing JSON multiblock and V2 model/voxel generator conventions, supported by actual structural blocks. Four directional orientations must share a canonical structure. Use true physical Mechanical Power input, correctly aligned bearings, a protected operating region, and accessible **real** material inputs/output. Multiple item input modules are necessary; inspect `MultiblockItemPortBlockEntity` before adapting it, since its current generic input mapping targets controller slot 0. Do **not** pretend that repeating the current port block creates independent slots.

The visual design must not stretch textures, hide the real ports, float parts, intersect the gear/shaft network or obscure the workpiece. A separate editable Magneticraft2 Studio reference model/image belongs with the machine; this is a future visual deliverable, not fabricated by K0 documentation.

## K1 behavior contract — implement and prove before extending recipes

1. At least four *logically distinct* stored ingredient types with accessible item input modules, one true output interface, and no controller-wide capability bypass.
2. A server-authoritative mechanical load while working. No idle load or free production on a stopped/overloaded network. Reuse the existing Mechanical Power System APIs without new "Gear V2" user-facing terminology.
3. Output capacity is reserved **before** consuming ingredients. A full output pauses without waste or duplicate progress. The exact stack/count/NBT inputs are checked again at commit time; on successful operation, subtract exactly the required counts and insert exactly one output.
4. Process state, recipe ID, buffered ingredients, consumed tooling (if any), and progress persist across save/reload. Input replacement or loss invalidates a pending cycle safely. Breaking the controller or relevant modules drops each stored item exactly once.
5. No new generic machine-wear bar or maintenance chore. Any future replaceable tool must have a gameplay purpose, visible wear feedback, and per-completed-operation accounting; none is required for the first assembly recipe.
6. JEI ingredients and recipe catalyst, Patchouli construction/automation, crafting recipe, translations and all four orientation JSONs reflect what actually works. No mandatory GUI if in-world feedback is sufficient.

## Acceptance / regression — unverified

- [ ] Inspect current PR/branch, module slot ownership, existing recipe serde/JEI handling and item-belt interaction before code edits.
- [ ] Build a K1 structural design with exposed real ports and a believable mechanical drive; generate all four facings + Patchouli and voxel/model data together.
- [ ] Feed the exact 2 planks + bushing + shaft + pulley, supply enough RPM/torque, and receive exactly one real Water Pump; manual crafting still works.
- [ ] Reject 1 plank, missing bushing, wrong pulley, nonmatching input, output full and unpowered/stalled conditions; no consumption or free progress.
- [ ] Repeat with concurrent automated input, mixed planks tags, multiple recipes where applicable, save/reload, break/unform/reform and chunk boundaries; no item duplication.
- [ ] Check item ports from every facing, JEI synced recipe view, single-player/dedicated server/two clients, animation/collision/UV and performance around multiple stations.
- [ ] Run `./gradlew build` and relevant generator contract tests. Source-level review is not compilation or gameplay verification.
- [ ] Regress existing Pattern Maker, Lathe, Press, Refractory Kiln, Copper Water Pump, Mechanical Transfer Arm and all old single-input `multiblock_processing` recipes.

## Explicit boundaries

**Do not expand into Bloomery/Iron reduction, Steam Age, Electrical machines or another air-blower pass in this slice.** Only proceed with the assembly station once multi-input recipe and genuine input port ownership are solved. One working product plus stable ports is better than many decorative machines. Keep the Bronze Ore Roasting Kiln, existing metals/quality, Copper progression and the current Creative/Survival recipes intact.
