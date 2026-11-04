# Forge Hammer — real-item progress test pack

**This is an optional resource-pack test, not production artwork and not an
alternative recipe.** The mod's default workpiece appearance stays where it
was before the Java runtime extension.

## In-game verification

1. Copy this directory into Minecraft 1.20.1's `resourcepacks` folder
   (or zip **its contents**, with `pack.mcmeta` at the ZIP root).
2. Enable the pack, press **F3+T**, and use the existing **Bronze Forge Hammer**.
3. Place a genuine Iron Bloom (or Copper input) into the real item port;
   the scene must display that **actual item**, not a fake static ingot.
4. Apply real mechanical power. The test pack lifts the visible workpiece
   by **0.18 blocks** and rotates it by **25 degrees** at 50% of the
   synchronized production progress, then returns it to the original pose.
5. Stop/stall the machine and resume. Progress should freeze/resume with
   actual processing, not an independent animation clock. Remove the input:
   the item should disappear. Check all four facing directions.
6. Disable the pack and F3+T: the default scene returns the item to its
   previous original constant pose, with no changed crafting or geometry.
7. Break the JSON or remove a required manifest binding: the original
   Java workpiece renderer should take over without duplicate items.

This pack overrides **only**
`assets/magneticraft2/animations/bronze_forge_hammer.json`. It uses the
existing mod manifest and item model, so it needs **no custom PNG or
baked model**.

### Expected runtime limits

The client interpolates progress from the last server-synced Forge Hammer
snapshot while the machine is running. The server still controls actual
recipe progress, item consumption, die wear, power and output.
Only the Forge Hammer opts into item/progress visual channels in this
milestone. Further Bronze machine integrations should use the same
generic schema after inspecting their existing machine state and renderers.

Run `node tools/animation/check-item-progress-contract.mjs` in the mod
checkout for static contract checks. This is not a compilation/runtime test.
