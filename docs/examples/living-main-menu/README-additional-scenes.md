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
