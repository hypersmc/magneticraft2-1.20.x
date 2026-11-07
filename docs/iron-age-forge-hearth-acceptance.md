# Iron IV-A — Iron Forge Hearth (first fueled batch reheater)

**Stage:** source implementation on draft PR #18, not built or game-accepted.

## Why it exists

The Bronze Refractory Kiln already fires ceramic charges and reheats
individual Iron Blooms, Wrought Iron Ingots and Bars. The Hearth does
**not** replace that work. The smaller open firebox performs a distinct
workshop function: **four matching ingots or four matching bars per batch**,
at a cost of 360 supplied HEAT processing ticks versus 4 × 240 ticks
on the Kiln. The player pays for the Iron infrastructure and must collect
a full batch. Individual ingots, bars, and Blooms continue through Kiln.

## First balance contract (provisional HEAT, not Celsius)
- 3×3×2 real JSON multiblock; one controller, one genuine item input,
  one genuine primary output, 4 iron foundation blocks, 10 brick blocks,
  and one required upper AIR. Model/voxel snapshots generated from one layout.
- Controller recipe: 4 Wrought Iron Plates, 2 Iron Structural Beams,
  2 Fired Refractory Ceramics, 1 Riveted Iron Frame. No recipe loops.
- Two JSON processing recipes: input count 4, output count 4,
  `wrought_iron_ingot` or `wrought_iron_bar`, 360 ticks.
- Same single-stack quality NBT copied to all four outputs and original
  `magneticraft2:hot_until` timestamp set at batch completion.
  Forge Hammer and Rolling Mill retain their existing heat checks.
- Fuel is real vanilla solid smelting fuel, no remainder-container loss,
  max 16 queued *matching* fuel items; controller interaction handles fuel.
  Natural draft only. No AIR network, bellows or external HEAT IO claimed.
- Config defaults: 36,000 capacity; 36 HEAT per burn tick, 2 passive loss;
  12,000 threshold; 16 HEAT per active batch tick. HEAT is persistent
  machine storage, ignition is demand-triggered, burning fuel is not
  returned as queued fuel.
- Output reservation before ignition/each supplied tick and before
  input removal; wrong item or mismatched quality resets progress.
  Blocked output pauses progress and new ignition while already ignited
  fuel continues burning. Active progress, NBT identity and queued fuel
  survive chunk save/load. Destroying the machine returns queued unburned
  fuel and uses existing controller inventory drop behavior.
- Same JSON processing type and item-port forwarding as other machines;
  one source of truth for registered parts, voxel occupied cells and
  Patchouli layout, without changing existing Kiln internals.

## In-game acceptance checklist (single-player)
1. Run `node tools/iron-age/check-forge-hearth-contract.mjs`, then
   JDK17 `./gradlew build` in a complete checkout and save the results.
2. Build and form all four orientations; check book `Visualize`, input/
   output location, top AIR opening, no invisible collision and controller
   hitbox/loot. Inspect front, side, rear, and elevated camera positions.
3. Queue charcoal, inspect HEAT state, run 4 ingots through the real input,
   extract 4 hot ingots; repeat for 4 bars. Test item quality preservation,
   heat expiry and forged/rolled downstream processing within hot window.
4. Try 1–3 inputs; verify no processing or fuel ignition without 4.
   Try wrong stock, incompatible quality input tags, full output, no fuel,
   power loss (not applicable), fuel runout, cold restart and no
   multiplication or missing input on interrupted batches.
5. Save/reload midway with queued fuel, partially burnt fuel, HEAT and
   progress. Check progress preservation and changed-item reset.
6. Sneak-use empty-handed to retrieve queued unused fuel with no
   duplicated items, including nearly full inventory. Break while queued
   and while processing, verify standard drops and no double drops.
7. Confirm original Kiln single-item reheat and ceramic processing,
   Bloomery, Forge Hammer, Rolling Mill, all item modules, data-pack recipe
   overrides, JEI and book remain functional.
8. Visual/texture finishing belongs to MGC2 Studio; reject missing
   models, texture stretching, floating support or invalid block outlines.
9. Multiplayer tests remain deferred until after Steam Age.

**Verification:** No Forge compile or in-game test performed by this source
commit. Leave PR #18 draft and unmerged.

## Studio concept reference

`docs/reference/iron-forge-hearth-studio.svg` is the accepted **source-based
concept layout**, not a model or runtime screenshot. The earlier generated
mockups depicting mechanical gears, forced bellows, chimneys, degrees Celsius,
steel refining or RPM requirements are inaccurate and rejected. This sheet
illustrates exactly the 3×3×2 SOUTH blueprint, its real ports and air chamber,
its 4-input/4-hot-output HEAT route, controller fuel and blocked state. All
final art is still assigned to MGC2 Studio. Patchouli is authoritative.
