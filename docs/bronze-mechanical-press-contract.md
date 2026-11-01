# Bronze Mechanical Press — K1 implementation and Studio contract

**Milestone:** Bronze III Mechanical Press, **source candidate only**. The Forge Hammer has a user-verified plate process; the Copper Sheet name is confirmed, but the Rolling Mill's full powered rolling/animation remains unverified. Do not represent this Press as in-game tested before user evidence.

## Purpose and distinct gameplay role

The Mechanical Press is for forming flat, cold-workable sheet stock into **shaped components**, rather than repeating the Forge Hammer's ingot-to-plate operation or the Rolling Mill's thickness reduction. The first data-driven recipe is:

`4 magneticraft2:copper_sheet -> 4 magneticraft2:copper_panel` in **160 powered ticks** at a minimum **18 RPM and 8 torque**. Exact input/output counts are conserved. Forming uses a maintained **Bronze Press Die** (384 completed batches per installed die, one wear per successful batch).

Copper Panels have an immediate, additive downstream crafting sink: **4 Copper Panels + 2 Glass -> 1 Copper Fluid Tank**. The current **6 Copper Plates + 2 Glass -> 1 Copper Fluid Tank** remains unchanged. The difference represents the provisional construction advantage of shaped and stiffened panels, not a finalized physical mass accounting system. No automatic progression gate or forced rerouting of old Copper content.

The panel's name is **Copper Panel**, not “Pressed/Rolled Copper Panel,” reflecting the earlier user preference that item labels describe the product rather than the process. Registry ID is `magneticraft2:copper_panel`.

## Canonical 3 × 3 × 3 structure

SOUTH JSON rows, BACK -> FRONT, BOTTOM -> TOP:

```text
Y1:
SIS
USO
SCS

Y2:
PAP
PAP
PAP

Y3:
PPP
PAP
PPP
```

- `S`: Stone Bricks. `P`: Oak Planks. `A`: mandatory unobstructed AIR.
- `C` at (x=1,y=0,z=2): actual controller.
- `I` at (x=1,y=0,z=0): real rear Gear V2 Mechanical Input module.
- `U` at (x=0,y=0,z=1): physical LEFT input.
- `O` at (x=2,y=0,z=1): physical RIGHT Primary Output.
- Rotation for WEST/NORTH/EAST is generated from the exact SOUTH rows; visual-air anchors rotate with them.
- **Patchouli mirrors X relative to SOUTH JSON**, so the displayed base layer is `SIS / OSU / S0S`, not the raw JSON. This is a necessary documented convention; the Forge Hammer initially failed formation when its guide missed it.
- Real item/drive modules remain independent block entities and their block-local render meshes must be **empty**.

## Drive and visual contract

A horizontally driven rear Gear V2 shaft leads to a supported enclosed drive housing and cam/follower, which drives the single vertical die. The static frame has four stone feet, an oak load-bearing gantry, iron press table, cut-copper crosshead and actual die guides. The press ram (one cached baked model) follows the module's **real visual rotation phase** using a cyclic cam stroke and moves down to the bed; there are no invisible motors, free-spinning unpowered visuals, entity stand-ins or per-frame model rebuilding.

The geometry is anchored to the **same 3x3x3 footprint** and emitted through the established shared multiblock generator:
- `tools/multiblock-models/designs/bronze_mechanical_press.json` (editable box/texture design).
- `tools/multiblock-models/sources/bronze_mechanical_press.json` (derived source faces).
- `src/main/resources/data/magneticraft2/multiblock_voxels/bronze_mechanical_press.json` (collision/selection).
- Four generated block-local model sets in `assets/magneticraft2/models/multiblock/bronze_mechanical_press_{south,west,north,east}/`.
- One separately registered `bronze_mechanical_press_ram.json` used by the BER.
- `docs/reference/bronze-mechanical-press-studio.svg`: vector Studio schematic of the actual frame and layer positions.

The concept illustration shared with the user in chat is a **non-authoritative moodboard**; some numbers, external wheel placements, diagrams and potential recipes in concept art are illustrative only. In-game positions, kinetic limits and actual item flow are defined by code/JSON. Final materials, bearing details, cam linkage, texture art and visual wear remain future Studio tasks, not K1 acceptance claims.

## Gameplay/engine safety

- Server-only exact ticker `BlockEntityRegistry.BRONZE_MECHANICAL_PRESS_BE`. In the Forge Hammer the wrong copied BE registry caused a permanently non-ticking machine; this is tested explicitly here.
- Reuse Gear V2's **raw speed for load registration**, not overload-masked speed; only progress when the authoritative network's `MechanicalLoadState.supplied()` returns true. Source overload must stall processing. No new mechanical-power system or forced off/on load oscillation.
- Use the real Mechanical Input BE as the registered consumer position. Clear load on missing recipes, worn die, blocked output and block destruction.
- Enforce only item input/output recipes with positive mechanical parameters; reject unsupported fluid/byproduct content.
- Simulate output insertion before removing exact input count; commit item output once. No duplication or consumption on stalled or full-output ticks.
- Controller cannot expose `ITEM_HANDLER`; only the linked physical item modules provide automation.
- Persist `PressProgress`, `PressRecipe`, `PressActiveInput`, `PressDie`, `PressRunning`, base inventory and Voxel V2 data across reload. Input identity/NBT mismatch resets partial progress; chunks don't continue off-thread processing.
- Worn press tooling stops new batches and needs a real replacement `bronze_press_die` item. Initial die is installed (controller crafting also requires one). No artificial wear inside the generic Gear V2 shafts is added.

## Acceptance checklist — not yet run in Minecraft

1. Execute `node tools/multiblock-models/build-designs.mjs --check`, `node tools/multiblock-models/generate.mjs --check`, `node tools/bronze-mechanical-press/test-contract.mjs`, and **`./gradlew build`** when the repository/build environment is available. Source checks do not prove Java compilation.
2. Form the book's 3×3×3 structure for SOUTH, WEST, NORTH and EAST. Missing AIR spaces and reversed real U/O must prevent formation. Confirm correct controller facing, stable shape and accurate V2 collision.
3. Supply **3 Copper Sheets**: no progress. Add a fourth and sustained Gear V2 ≥18 RPM/≥8 torque: run **160 supplied ticks**, consuming exactly 4 Sheets, yielding exactly 4 Panels.
4. Craft one **Copper Fluid Tank** from four panels plus two glass; verify old six-plate recipe and existing Water Pipe pathways are unchanged.
5. Disconnect or overload Gear V2 midway: progress stops, no input lost and no spuriously completed item. Check load statuses, source torque/capacity and resumed operation.
6. Fill output port, check full-batch simulation and paused progress, empty output and resume. Test both player and automated item insertion/extraction via actual ports only.
7. Save/reload mid-batch with a nearly worn die; check saved work, no duplicate outputs, correct wear count and no missing model animation. Replace a worn die and confirm exact inventory consumption.
8. In-game inspect ram motion against the fixed press table from all sides. Confirm die has a guide/support, rear drive is plausible, no collision artifacts, UV stretching or major FPS regression. Repeat two-client testing separately.
9. Confirm former Forge Hammer and Ore Roasting Kiln still function. Verify the Rolling Mill's actual processing/animation before marking the 3-machine line runtime complete.

**Do not merge or close PR #11 without broader Bronze-age testing.**
