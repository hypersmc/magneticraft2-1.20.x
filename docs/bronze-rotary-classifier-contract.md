# Rotary Ore Classifier — Bronze II integration contract

**Status:** Source implementation in draft PR #11, **unverified with `./gradlew build` or Minecraft runtime**. 1.20.1 Forge. This is a new JSON/V2 Bronze multiblock and the first user-facing adopter of the original Magneticraft2 material texture kit.

## Why it exists, without replacing Copper machines

The Bronze Stamp Mill remains an industrial *breaker*. The Crusher/Stamp output already contains usable `crushed_*` items. The Rotary Ore Classifier is an **optional, powered size-grading step** between Stamp Mill/Grinder and the existing Ore Washer, not a replacement for the Washer's water cleaning or the Sifter's concentrate production. A pack maker can skip it and use existing recipes.

Per ore family (Chalcocite, Cassiterite and Quartz):

1. **Size classification:** **four** `crushed_<ore>` -> **three** `classified_<ore>` (fine fraction) **plus one** unchanged `crushed_<ore>` (coarse return), **always**, 180 ticks, Gear V2 >=24 RPM, torque >=4. Coarse is a real secondary output in slot 2, not a probabilistic gravel gain. All four input units are accounted for.
2. **Optional optimized washing:** **three** `classified_<ore>` + **600 mB Water** -> **three** `washed_<ore>` + **600 mB Dirty Water**, 240 ticks, >=20 RPM, torque >=3. The existing plain `crushed_<ore>` Washer recipe remains unchanged (250 mB and 140 ticks per unit, so three plain washes use 750 mB and 420 ticks).
3. **Existing Sifter:** `washed_<ore>` proceeds as before to its respective copper/tin/quartz product, preserving all established final yields.

The coarse return is an existing crushed item and can go directly through the normal Washer or be accumulated for further classification. It never transforms into more material by itself. Ore quality NBT/yield changes are **not** part of this feature, and worn screens don't secretly alter final ingot quality.

## Construction — 5 wide × 3 high × 5 deep, all four facings

Canonical **SOUTH** blueprint with the controller at local offset (0,0,0); row order is back (-Z) to front (+Z), columns west(-X) to east(+X); layers bottom-to-top:

```text
Y=0   SSSSS / SASAS / SASAS / SACAS / SOYSS
Y=1   PUIBP / PAAAP / PAAAP / PAAAP / PAAAP
Y=2   PBPBP / PAAAP / PAAAP / PAAAP / PBPBP
```

`S` = Stone Bricks; `P` = Oak Planks; `B` = Cut Copper; `C` = Classifier Controller; `A` = AIR; `I` = real Gear V2 Mechanical Input Module; `U` = real Item Input; `O` = primary *fine* output; `Y` = secondary *coarse* output. The controller sits at front row z0; physically accessible output ports sit at z+1, ground-level front edge. The mechanical drive and item feed are elevated at rear z-3/y+1. Other orientations are independently rotated datapack JSON, not a forced player-facing setting.

Only six selected AIR locations become V2 render/collision proxies; all unrelated AIR must remain hollow. Never place an extra hidden port mesh inside the actual I/U/O/Y module cells. Guidebook `patchouli:multiblock` Visualize pattern transposes JSON rows into the Patchouli X-column convention.

## Models and motion

- The *stationary* load-bearing frame uses per-cell baked V2 models (no static BER rebuild) with custom `machine_timber`, `machine_timber_end`, `bronze_cast`, `bronze_machined`, `dark_work_iron` and `chute_liner` textures. Stone Bricks remain vanilla where they actually are the stone foundation.
- The rotating three-cell axial drum uses two explicit registered baked models, normal and worn, using `screen_mesh_bronze` or `screen_mesh_bronze_worn`. Both have true transparent screen openings and render with `RenderType.cutout()`. No opaque virtual drum casing is added.
- Drum phase comes from the **actual formed** Gear V2 input module, including reversal/stoppage, not a free-running world time animation. Models must remain inside Minecraft's -16..32 per-element bounds, with no UV stretches, impossible drives, interference with real port modules or unsupported floating frames.
- Screen condition is a **replaceable component**. The part tracks 384 completed batches, persists as maintenance NBT, changes the drum's appearance, and blocks processing when expired. No wear during pause/idle.

## Safety and persistence

- Machine is bound to `magneticraft2:multiblock_processing`, with machine ID `magneticraft2:rotary_ore_classifier` and current datapack recipes.
- Process input slot 0; output 1 for fines; output 2 for coarse. The real Item Input module enforces recipe-validated insertion; the two dedicated outputs are extract-only. Controller still accepts manual work and lets player retrieve finished outputs.
- A batch reserves **both** output capacities before work and registers mechanical load only when all output and power prerequisites are satisfied. If slots fill, torque/RPM disappear, a screen wears out, or a chunk unloads, the active batch pauses without duplicating, consuming, or rerolling outputs.
- Save/restore progress, active recipe, output decision, maintenance and voxel snapshots. A new block identity is created; no existing machine or registry IDs change. Breaking must drop stored inventory and safely disconnect its mechanical load.
- Recipes/outputs, crafting and tool tags, Patchouli guide, JEI processing category/catalyst, both models and all facing rotations are required; keep older Stamp Mill, Washer and Sifter untouched.

## Verification

```bash
node tools/rotary-classifier/test-contract.mjs
node tools/texture-art/build-kit.mjs --check
./gradlew build
```

Check in-game:

1. Build directly from Patchouli Visualize, including all 4 horizontal facings, and form by controller right-click. Verify all six AIR anchors become invisible V2 hosts except where attached models require surfaces.
2. Confirm genuine Mechanical Input and both complete item output modules remain visible, unobstructed, and accessible to hopper/belt/arm insertion or extraction. Never place generated geometry inside their cells.
3. Insert 4 crushed Chalcocite; apply at least 24 RPM/4 torque; verify rotating bronze mesh, live input/outputs and **3 screened fines + 1 crushed return**. Repeat with Cassiterite and Quartz.
4. Repeat each fine batch through Washer and Sifter; check **600 mB water in, 600 mB dirty out**, independent ports, correct mineral outputs and no Copper yield regression. Compare 3 original crushed ore washes (750 mB) and no output overflow.
5. Fill coarse output, then primary output; verify pause/resume and no phantom extra material. Cut Gear V2 mid-run; ensure no stalled source overload.
6. Stop/reverse mechanical power, inspect shaft and drum direction, restart. Test screen wear, replace part without tearing down the structure and check condition after server/chunk save/reload.
7. Inspect model in daylight/night, all 4 facings, from every side at normal FOV, including empty/filled/active states: missing textures, UV compression/stretching, z-fighting, opaque mesh, floating shafts, missing side braces, excessive FPS costs, block/module collision and vanished chunks.
8. Break and recover machine and its modules; ensure controller/loot, inventory, wear and physical proxies behave as intended. Test multiplayer and automated I/O.
9. Confirm JEI recipe/catalyst and that no original Copper/Stamp/Washer/Sifter datapack recipes have been changed.

**Not yet performed:** a Forge Gradle build, server/world startup, actual in-game animation and collision validation. Passing a source-contract test does not establish that the machine compiles or looks correct in Minecraft.
