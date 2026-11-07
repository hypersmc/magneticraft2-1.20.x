# Iron II-C — Wrought Iron Large Gear

Scope: PR #18 draft. A genuine 16-tooth Gear V2 large wheel using existing large-gear
placement/ratios/network and the Copper/Bronze workshop, NOT a new lathe or power source.

## Production
- 4 Wrought Iron Bars + 4 Plates + 1 Riveted Iron Frame -> 1 heavy rough
  **Assembled Large Gear Blank**, in a crafting table. This is assembled
  hot-worked stock, not a magically forged finished tooth profile.
- Existing Bronze Mechanical Lathe -> 1 Machined Large Iron Gear, 160 supplied ticks,
  12 RPM, 8 torque, 3 cutter durability. Medium Iron remains 2, Bronze remains 1.
- Machined ring + 3 Wrought Iron Plates + 1 Wrought Iron Shaft -> Iron Large Gear.
- This uses already established Shear, Forge Hammer and Iron shaft production. No
  new input sources, artificial metal multiplication or material quality claims for
  ordinary crafting (the final crafting table cannot transfer custom NBT).

## Power contract
- 16 teeth, 24 max-transmitted torque. Same meshing ratio and rotation direction as
  Wood Large Gear, which remains 16 max; Iron Medium Gear/gearbox remain 16, wooden
  medium gear/gearbox remain 8, source Water Wheel <=16, existing mechanical input
  <=16. A 24-rated part does not increase source power and does not defeat weak links.
- Block states and Gear V2 ticker/renderer stay shared with Wood Large Gear; distinct
  iron block and block entity use new registered IDs. Existing save data unchanged.
- Static baked model has 16 physical rooted teeth and 2-pixel full-length shaft,
  exact native texel UV mapping; textures are temporary until MGC2 Studio.

## Verification boundary
Offline geometry/resource checks can confirm model cuboids, UVs, registered IDs and
valid recipe counts. A full Gradle build, runtime model baking, Forge/Bronze recipe
regressions, saved-world tests, Gear V2 overload/phase correctness and visual FPS
still require execution in a full checkout and Minecraft.

## In-game acceptance
1. Assemble the rough blank from survival hot-worked stock and a real frame.
2. Insert it in EXISTING Bronze Lathe; test 160 ticks, exact output, 3 wear, quality.
3. Test Medium Iron gear and Bronze gear/bearing with their original cutter costs.
4. Craft and place Iron Large Gear on X/Y/Z, rotate, check shaft contacts and ratios
   against 8-tooth gear, as well as save/reload of rotating phase.
5. Verify torque bottlenecks 8/16 and no phantom torque, stop/restart, overload.
6. Check static/rotating model, collider, textures, teeth, drop, tooltip, creative,
   JEI and Patchouli; inspect above/below and benchmark large connected networks.
7. Keep PR draft/unmerged. Multiplayer work deferred until after Steam Age.
