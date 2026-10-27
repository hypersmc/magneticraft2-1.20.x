# Machine visual manifest resource-pack test — Primitive Grinder

For Minecraft Forge **1.20.1**, on Magneticraft2 PR #12's
`feature/json-driven-animation` branch.

This test exercises a NEW capability beyond overriding the existing
`animations/primitive_grinder.json` file:

1. `assets/magneticraft2/machine_visuals/primitive_grinder.json` **redirects**
   the machine to `magneticraft2:demo/primitive_grinder_restyle`.
2. That completely separate animation scene declares an input alias
   `rebranded_shaft` rather than the built-in `mechanical_input`.
3. The manifest maps `rebranded_shaft` back to the **actual** Java
   `mechanical_input` angle. No power system changes.
4. The new scene renders the existing axle with its usual direction and
   selects a new `magneticraft2:animated/restyled_grinder_rotor` model.
5. The alternate rotor is gold and intentionally **visually stationary**
   (`scale: 0`). This intentionally demonstrates that an animation pack
   can change the visual movement without changing the server's actual work.

## Install

Zip the **contents** of this folder with `pack.mcmeta` at the archive root;
copy the ZIP into `run/resourcepacks/`, enable it under Minecraft resource
packs, and press **F3+T**. Form/power a Primitive Grinder in the test world.

**Without the pack:** the original grinder model and both rotating parts.

**With the pack:** the axle still rotates, the grinding rotor changes to gold
and visually freezes. **This does not stop the real grinding recipe**.

Disable the pack and press F3+T again: both rotor appearance and rotation
should revert to the built-in animation.

## Error/fallback test

Try changing the manifest's `angle_channels` value from
`"rebranded_shaft": "mechanical_input"` to
`"rebranded_shaft": "steam_pressure"`.

The Grinder's Java renderer does **not** expose a `steam_pressure` angle.
Expected result: a single warning for an unavailable Java input and the old
Java grinder-rotor rendering. **No new power or item input is created.**

Or change `animation` to an unknown resource location. The renderer should
fall back to the original Java models without drawing a missing-model cube.

## Limits

- This is a client resource-pack **visual** extension, not a new gameplay
  machine, datapack machine definition or new physical power port.
- Only names/aliases for *existing typed, synchronized Java inputs* can be
  selected. The engine currently supports mechanical visual angles, typed
  integer model variants, state flags, rigid X/Z slider-crank and static model
  components. New physics/driver types require Java code.
- The machine's original static formed model remains part of the old
  multiblock rendering system and can still be replaced by ordinary model
  resources or the Rendering V2 generator. It is not automatically rebuilt
  from this animation scene.
- Test this pack by itself: it can conflict with other packs replacing the
  same machine-visual manifest.
- No full Forge build or live Minecraft verification of this specific new
  manifest-redirection behavior has been performed yet.

Run the structural manifest tests from the repo root:

```bash
node tools/animation/check-machine-visual-bindings.mjs
```
