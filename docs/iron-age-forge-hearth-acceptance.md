# Iron IV-A — Iron Forge Hearth (first fueled batch reheater)

**Stage (2026-11-08):** Player-confirmed functional in-game. Complete Gradle build, all-orientation rendering, save/reload and other detailed regression cases are not independently verified.

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
  and one required upper AIR **before formation**. Forming adds a cached V2
  render proxy with the charcoal firebed and stock rests; no additional
  player-placed block or new gas/air/pressure system is involved.
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

## AIR-anchor interior — verified source contract, in-game tests outstanding

The top-center cell remains `minecraft:air` in the four JSON construction
blueprints. It is **not a resource input**. At formation the existing
`MultiblockRenderParts.assign` validates that exact original AIR cell,
places the standard multiblockfiller, and assigns the pre-baked chamber part
`part_<dx>_1_<dz>.json`. This is exactly the mechanism previously used
for the Iron Stock Shear. On teardown, the AIR proxy clears and the original non-AIR structure blocks drop as items.

The new formed model uses 15 block-local, native-UV cuboids. It includes a
refractory tray directly on the Iron Foundation underneath, dark charcoal
and five coal chunks, four short wrought-iron retaining edges, two wall-
anchored iron support brackets and two stock-rest bars. The bracket ends
touch the surrounding brick blocks. There are no floating components,
full-block fake collision boxes, added machine entities, new registry IDs or
per-frame renderer allocations. The 9 load-bearing shapes have matching
Voxel V2 collision and selection; decorative coal intentionally does not.
The firebed is deliberately UNLIT while idle. It does not fake flames or
new HEAT processing rules; status remains in the existing controller.

Orientation offsets (controller-relative, model cells): SOUTH `[0,1,-1]`,
WEST `[1,1,0]`, NORTH `[0,1,1]`, EAST `[-1,1,0]`.

### Follow-up in-game regression tests
1. Build each facing using **real AIR** in the upper center, and form the
   machine. Confirm the charcoal tray and stock rests appear automatically.
2. Place a block in that required AIR: formation must fail. Remove it:
   formation must work. No separate air-input blocks/items are needed.
3. Check attachment to brick walls and lower foundation, all 4 camera
   orientations, hitboxes and proper native UV density (no stretching).
4. Save/reload and press F3+T; confirm the chamber stays rendered with
   correct collision and no missing-model placeholders or duplicate parts.
5. Unform/break the multiblock: the proxy disappears, the chamber position
   restores AIR and no phantom drops/collision remain.
6. Repeat 4→4 ingot/bar heating, output blockage, fuel, HEAT and Kiln
   regression checks; none of those gameplay contracts should change.

Model regeneration: `node tools/iron-age/build-forge-hearth-chamber.mjs --check`
(or `--write` when intentionally regenerating resources). The source
is the SOUTH chamber file plus the four authoritative JSON blueprints.
Compilation and game visuals are still unverified until tested.

## Open formed brick rim — geometry correction from player screenshots

The screenshot shows an enclosed, nearly full-height 3×3 brick platform,
which conceals the formed Hearth's AIR-anchor charcoal and stock rails.
The source cause is the eight unchanged `cube_all` upper bricks and their
matching **full-height invisible collision shapes**. This is a geometry defect,
not a new texture requirement.

**Construction is unchanged:** eight ordinary Bricks plus top-center AIR,
3×3×2. Once formed, the original V2 filler renderer now supplies:

- back corners 8/16 high and back-center 9/16 + 1/16 iron coping;
- side mid-bricks 7/16 high + 1/16 iron coping on the inner edges;
- front corners 5/16 high;
- front center a real OPEN workpiece mouth: 2/16 low sill, two brick
  cheeks 5/16 high, and two slender wrought-iron edge strips 6/16 high.

The 15 existing AIR-anchor chamber cuboids remain supported by the
foundation and masonry; they are no longer hidden by full-height bricks.
All 32 upper-brick facing models are new block-local baked part geometry
with native 1:1 UVs rather than `cube_all` slab stretching. The server Voxel
V2 collider is regenerated from these exact parts: 9 lower cells + 15
rim parts + 9 solid chamber support parts = **33** collision/selection
boxes. No imaginary full-height brick wall remains when formed.

Because V2 saves formed shape/model snapshots, **DISMANTLE and REBUILD**
previously formed Hearths to refresh the visible model and physical hitbox.
The existing teardown drops the original non-AIR block items rather than
restoring the construction blocks in-place.

### Focused in-game checklist
1. Assemble the original 3×3×2 from eight normal upper bricks and one
   required AIR. Form all four facings; Patchouli layout is unchanged.
2. Visually check the low back/side rim, front loading opening, charcoal
   bed and genuinely supported iron stock rails from multiple angles.
3. Test both selection and collision: no full-height invisible walls;
   no walking through rendered brick or metal support pieces.
4. Dismantle/break and confirm the original brick ITEMS drop and the
   entire structure becomes AIR; rebuild the multiblock from those drops.
5. Reload world, F3+T, adjacent chunk views, texture mapping and FPS.
6. Repeat four-to-four ingot/bar HEAT batch and original Kiln regression.

**Stage:** source/model correction; JDK17 Gradle build, Minecraft rendering,
physics and regressions not yet verified. Keep PR #18 draft.

## Player-confirmed functionality — 2026-11-08

The player explicitly reported that **the Iron Forge Hearth already works and has been tested in-game**. Its primary functionality should be treated as accepted, not as an untested or broken source prototype. Do not repeat general Hearth implementation/debug work without a specific observed failure.

This statement does not individually establish that all four orientations, brick/model collision, old-world migration, F3+T reloads, exact HEAT/fuel balance, save/reload and FPS checks passed. Those remain separate verification items. A full JDK 17 `./gradlew build` has not been documented in this development session. PR #18 remains draft and unmerged.
