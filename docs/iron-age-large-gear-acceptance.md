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

## Player verification and physical meshing defect — 2026-11-07

The player explicitly answered **yes** to all four prior in-game checks:
large blank -> existing Bronze Lathe machining (including cutter wear),
Large Gear crafting/rotation and 8/16 compatibility, formed Iron Shear
bearing appearance, and older Copper/Bronze input appearance. Record
those as reported in-game successes; they do not prove every save/load,
collision, stress, FPS or all-orientations criterion above.

**Open visual/mechanical defect (player screenshot, four 16-tooth Iron Large
Gears in a 2x2 network):** Gear V2 deliberately allows 16-tooth gears
to form straight external meshes with axle centers separated by **2 blocks**
= 32 model pixels. The current large model has tooth tips at approximately
15.24px radius (including an outer tooth corner). Even a tip-to-tip contact
would leave at least **1.52px gap** at the approved 32px center distance;
tooth-to-root contact leaves more. The gear graph may treat a pair as
connected even though the models visually **do not mesh**. This is a known
correctness/acceptance issue, not an accepted final model.

The Iron Large Gear also currently specifies a broad in-plane square
collision shape, which does not match the visibly open annular body; test
and correct this as part of geometrical acceptance. Do **not** solve either
problem using a concealed renderer offset, fake contact, or stretched UVs.

**Constraint discovered during source-level experiments:** Gear V2 also
allows 8/16-tooth diagonal connections at sqrt(2) blocks (~22.63 model
pixels). Increasing Large Gear rim/tooth radius merely to close a 16/16
32px mesh increases intersections with existing Medium gears at the
allowed diagonal connection. The current model already has small
cross-component intersections in a sampled polygon test. Any correction
must preserve *both* legal meshing layouts across all phases, not simply
make the four-Large-Gear screenshot look close.

Next acceptance gate: prove visible tooth-to-gap engagement and no significant
intersection for Large/Large, Medium/Large and existing wooden geometries,
in stopped and powered states over a full tooth-pitch rotation; verify
shape against rendered ring and shaft and check both X/Y/Z axes. This
needs measured geometry QA and actual in-game views. No geometry or Gear V2
rule changed by this note, and this milestone is **NOT visually complete**.
