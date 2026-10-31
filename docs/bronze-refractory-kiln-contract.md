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

## K1 integrated visual model — 2026-10-31

The unlit kiln now has generated block-local refractory masonry, recessed dark-iron charge/firebox doors, a low roof cornice and a real open flue rim. The upper central front cell has changed from required AIR to required Bricks so the hatch is supported by a real formed block, **not an invisible AIR proxy**. The two *existing* real item ports receive kiln-specific baked block models through their existing `MultiblockItemPortBlockEntity` model-data mechanism; their capabilities and coordinates are unchanged. Custom 16×16 textures already shipped with Magneticraft2 (refractory brick, retort hatch, riveted iron, bronze joints) are reused with pixel-proportional UV, no enlarged stretched tiles. Geometry stays within each owning block. The K1 hatch is visual and closed: opening/fuel/charging gameplay belongs to K2. Four facings, Voxel V2 snapshots and Patchouli pattern are regenerated from the same canonical source. Build/in-game acceptance is still required.

## K1 original texture kit — 2026-10-31

The first formed-kiln screenshot showed Retort hatches, Retort grilles and Settling Tank metalwork reused on the Kiln. Those machine-specific texture aliases are now removed. K1 owns **ten newly drawn, 16×16 pixel tiles** under `textures/block/refractory_kiln/`: masonry, plinth, roof, iron, bronze, soot, grille, charging door and separate input/output markings. Only the established generic `material/refractory_brick` ceramic lining is shared. This preserves material consistency without copying another machine's visual language.

Pixel art is stored as deterministic source in `tools/refractory-kiln/texture-art.mjs`; regenerate and check it using `node tools/refractory-kiln/build-textures.mjs --write` and `--check`, respectively. Rebuild models via `node tools/refractory-kiln/generate.mjs --write` and validate with `node tools/refractory-kiln/test-contract.mjs`. These are static tests; a Forge build and in-game camera/UV inspection are still outstanding.

## K1 final readability pass (2026-10-31)

This is the final deliberately **simplified** visual pass after the in-game video showed an overly busy, hard-to-read facade. The physical JSON structure, controller, true ports, Voxel V2 collision, roof-flue AIR and gameplay are **unchanged**. The upper front's three real masonry cells now visually form one broad dark-iron charging door; the controller is the single recessed firebox below. The middle front remains brickwork with only two small real item-port faces. Side walls lose repeated metal straps, top trim is reduced to a low ceramic collar around the real flue, and small decorative rivets/grilles are removed. Existing original Kiln textures are reused, with 1:1 face pixel UV. The door remains visually closed and non-interactive until an explicit K2 gameplay design. Judge the design from front/side/roof in Minecraft; if it still fails, defer artwork and move on, without new redesign loops.

## K2 — first functional firing path (2026-10-31)

The K1 visual model is frozen as requested. K2 uses the existing `magneticraft2:multiblock_processing` JSON recipe type, real left input/right output modules, and Magneticraft2 HEAT storage. No new renderer, fake ports or external energy system was introduced.

A provisional craftable preparation batch makes the first loop playable: **one clay ball + one sand → one Unfired Refractory Batch**. A data-pack recipe fires **one Unfired Refractory Batch → one Fired Refractory Ceramic** in 400 active processing ticks. No final flux identity or clay/sand/flux ratio has been agreed, and this is not intended as a permanent metallurgy balance. Modpacks can override these recipes. Final refractory block casting and use in later machines remain separate milestones.

Fuel is added by right-clicking the formed controller with a normal Forge solid smelting fuel (fuels with return containers are rejected); empty-hand right-click reports status, and sneak-use with empty hand retrieves queued, unburned fuel. The fuel queue is limited to 16 items and saved separately. HEAT is generated while a fuel item is burning, is gradually lost while idle, and is consumed only on active firing ticks. All energy values are Magneticraft HEAT units, not physical Celsius. The adjustable Common config defaults are 32,000 HEAT capacity, 24 HEAT per burn tick, 1 passive loss per tick, 9,000 firing threshold and 6 HEAT per firing tick.

A full output slot prevents ignition of further fuel and pauses progress without consuming the batch. Fuel already ignited continues burning. Successful completion extracts the exact recipe input count and inserts output in the same server tick after simulating output capacity. Progress and active recipe identity survive save/reload, and a changed recipe resets progress. Controller teardown drops queued fuel while the existing inventory drops input/output. The item ports retain exclusive automation control, and the controller-level ITEM_HANDLER bypass remains disabled.

This implementation is **not yet build-, client-, or multiplayer-verified**. Required tests: `node tools/refractory-kiln/build-textures.mjs --check`, `node tools/refractory-kiln/generate.mjs --check`, `node tools/refractory-kiln/test-contract.mjs`, `./gradlew build`; then in-game formation in four orientations, fuel handling, recipes, HEAT cooling, blocked output, port interaction, chunk unload/reload and break/reform. No K2 completion or PR merge claim until those pass.

K2 follow-up: The controller also persists a one-item identity/NBT snapshot of the in-flight input. Replacing an ingredient with another item or a different material-quality tag under the same recipe ID resets partial firing progress rather than inheriting already-paid HEAT work. Stack count changes alone do not reset progress.

## K2 item artwork correction (2026-10-31)

The Unfired Refractory Batch initially reused `minecraft:item/clay_ball`,
and Fired Refractory Ceramic reused `magneticraft2:block/material/refractory_brick`.
Both were placeholders, and in the creative inventory they were not recognizable
as a prepared sandy-clay batch and a fired ceramic product. These are now
original 16×16 RGBA item icons with transparent backgrounds. The raw batch is
an irregular brown sand-speckled compact, and the fired ceramic is a neatly
shaped light refractory brick. The model paths are
`magneticraft2:item/unfired_refractory_batch` and
`magneticraft2:item/fired_refractory_ceramic`.

The exact pixels and palettes live in `tools/refractory-kiln/texture-art.mjs`;
`node tools/refractory-kiln/build-textures.mjs --write` reproduces both new
item PNGs and all pre-existing block tiles. `--check` and the
`test-contract.mjs` assertions verify resource existence, dimensions and
distinct icons. No recipes, machine geometry, processing or saved data change.
