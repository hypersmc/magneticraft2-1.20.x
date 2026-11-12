# Additional Living Main Menu test scenes (Forge 1.20.1)

Two more **real Minecraft save worlds** accompany PR #21 as the downloadable archive
`Magneticraft2_LivingMenu_AdditionalScenes.zip` distributed with the task.

The source worlds are intentionally **not committed as binary save files** to the
repository. These manifests document their settings, and the downloadable ZIP
contains each corresponding `world/level.dat` and `world/region/*.mca`.

## Install

Extract the ZIP into the game directory (`run/` for Gradle `runClient`), **not**
into `saves/`. You should end up with:

```text
run/mgcmainmenu/scenes/
  copper_workshop/       # original Copper Mill, keep unchanged
    scene.json
    world/level.dat
  stone_camp/
    scene.json
    world/level.dat
    world/region/*.mca
  waterworks_quarry/
    scene.json
    world/level.dat
    world/region/*.mca
```

The two additional scenes contain only **16 pre-generated chunks each**.
Their manifest caps use render 3, simulation 5 and initial spawn radius 3.
Vanilla may generate additional chunks on the disposable runtime at startup.

- `stone_camp`: an independent Stone Age clearing with timber lodges, campfire,
  stone paths and landscaping. This tests scene switching and world appearance,
  **not functioning Magneticraft2 machinery**.
- `waterworks_quarry`: rearranged quarry/watercourse setting derived from the
  Copper Mill template, preserving the actual original 13 Magneticraft2
  mechanical block entities. Their in-game operation still needs visual testing.
- `copper_workshop`: the existing sample is preserved exactly as installed.

## Boot-time selection

On **each fresh Minecraft launch**, the loader sorts valid enabled scenes by
folder ID and chooses the one **after** the last *successfully joined* scene:

```text
copper_workshop -> stone_camp -> waterworks_quarry -> copper_workshop
```

If no prior state exists, choose the first valid scene alphabetically. The
last scene ID is recorded in `mgcmainmenu/.last-successful-scene`, which is a
small text file **outside** the real save and source scene worlds.
Reopening the title after Singleplayer within the **same running JVM** reuses
the current scene; there is **no automatic live cycling** that would repeatedly
close/start an integrated server. To force a scene, set `enabled: false`
on every other scene's `scene.json` and restart Minecraft.

The original `saves/magneticraft2_menu_source` fallback still works if the
scene library has no valid enabled scenes.

## Testing

1. Back up existing scene folders if you have customized them, then install.
2. Restart three times and confirm three different scene IDs in `latest.log`.
3. Visit Singleplayer and return to title in a single process: it must not
   advance to the next scene.
4. Verify each map's camera/time, source world remains unchanged, chunk caps,
   real mechanical wheel operation in Waterworks, and no unsafe shutdown hang.
5. Disable one scene and confirm it is skipped; invalidate a copied scene and
   confirm it is skipped without deleting any source worlds.

This is **sample/test content**. NBT/Anvil structure and block-entity counts
were statically checked, but Java compilation and actual Minecraft runtime
world visuals/navigation/animation have NOT been verified.

## Bronze Foundry: real test-world sample (2026-11-12)

A third additional sample, `bronze_foundry`, was created from the supplied *real* Forge 1.20.1 development save (`testworld.zip`), **not** by recoloring the Copper Mill. The binary sample is distributed separately as `Magneticraft2_Bronze_Foundry_LivingMenu.zip`, not committed into the source repository. Install by extracting into the `run/` game directory. It adds `mgcmainmenu/scenes/bronze_foundry/scene.json` and `world/`; the original source test world remains untouched.

The sample crops **35 pregenerated chunks** from the source's 9,661 chunks, preserving existing formed Bronze Age controllers and real machine block entities at their original positions. It adds a stone/copper paved yard, brick industrial backdrop and raised solid viewing platform. The menu camera stands at `(0.5, 66, -6.5)` facing north, with a 3-chunk view, 5-chunk simulation cap, 3-chunk menu-only initial spawn radius, and near-noon static lighting. The startup region covers the wheel and forge line. The existing mechanical arms, washer, sifters, waterwheel, gears, shafts, belts and Bronze foundry/forge hammer/rolling mill/press/boring machine/sawmill/mixer/blower/assembly station remain genuine game systems, not fake world-model representations. It also contains some earlier-age fixtures from the source test area.

**Static validation:** the resulting Anvil region headers, all 35 chunk NBT records and section palettes, all 33 saved formed multiblock controller block-map bounds, preserved mod blockstates and block-entity NBT, camera support and ZIP CRC pass programmatic checks. The archive contains **1,161 block entities** (including **759 multiblock fillers**), so **startup and FPS performance require actual measurement**. Cropped filler/port blocks linking to controllers outside the scene extent were removed, not moved. The user's original `Player` NBT, playerdata, stats and advancements are excluded from the sample. The development save's small Magneticraft2 test datapack is included because `level.dat` enables it, and **four Mekanism creative fluid tanks** are preserved where they support existing test rigs. No production game behavior or package compatibility is asserted without in-game testing.

To keep a **three-scene** round robin (`bronze_foundry`, `copper_workshop`, `stone_camp`), first back up and **disable** `mgcmainmenu/scenes/waterworks_quarry/scene.json` (`"enabled": false`). The ZIP never deletes or overwrites the old scene. If Waterworks remains enabled, the launcher will rotate through **four** valid scenes. No in-menu live switching was added.

Required in-game regression: launch the bronze scene, inspect arm/belt handoff and gear rotation, verify formed Bronze machines stay formed and work when actually supplied, check camera support and sightlines, measure startup duration/FPS with the same mods, confirm Singleplayer/Multiplayer/Quit navigation releases the disposable world, and verify normal-world settings remain unchanged. **Forge `./gradlew build` and runtime verification have not been performed here.**
