# Iron factory catwalks — Iron construction slice

Added ordinary **Iron Floor Grating** and automatically connecting
**Iron Catwalk Railing** on draft PR #18. Neither is a multiblock.
Neither introduces a new simulation system or maintenance part.

## Material recipes
- Two Wrought Iron Bars + two Wrought Iron Plates -> **four** grating blocks.
- Two Wrought Iron Bars + four Wrought Iron Rods -> **four** railing blocks.
- Both use previously available Bronze workshop hot-rolling material stock;
  there is no rivet/machine circular dependency. Vanilla crafting does not
  transfer graded input NBT into ordinary building blocks, as with existing
  panels and frames.

## Technical behavior
- Grating is a waterloggable open lattice with 19 actual baked bars and
  matching precomputed selection/collision; no invisible full slab.
- Railing uses vanilla FenceBlock connected states and 1.5-block-tall fence
  collision. The minecraft:fences tag is extended without replacement to
  allow neighboring Iron Railings to connect to one another.
- Ordinary solid-support fence connections, waterloggable railings, pickaxe
  loot, default redstone behavior. All art is provisional for MGC2 Studio.
- Baked resource models preserve Minecraft's 1:1 model-pixel UV dimensions.
  No block entity, tick loop or dynamic renderer.

## Acceptance checklist — single-player
1. Run `node tools/iron-age/check-catwalk-contract.mjs` and JDK 17
   `./gradlew build` on a full checkout (record actual logs).
2. Craft in survival using hot-worked wrought iron stock; confirm both
   recipe yields, JEI and Patchouli pages.
3. Place 2x2 grating over an open drop; walk across, jump, inspect hitbox,
   edges, light, waterlogging and removal, top/bottom face rendering.
4. Place railings in a straight line, L-corner, T and cross intersections,
   next to iron beam/foundation; verify visually connected posts, collision
   and no odd interactions with vanilla fences.
5. Mine both with a pickaxe and check exactly one drop each. Verify
   creative tab and handheld item models and all orientations.
6. Recheck Steel/Age-boundary remains unchanged; existing Iron Frame,
   casing, shear, Forge Hammer and Bronze workshop recipes must still run.
7. Check stacked catwalk performance and visuals from above/below/at angles.

No build or in-game test result is claimed by source upload. Keep PR #18
draft and unmerged. Multiplayer testing is deferred until after Steam Age.
