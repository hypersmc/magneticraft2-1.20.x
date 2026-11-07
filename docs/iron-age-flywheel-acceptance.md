# Iron IV-B — Wrought Iron Flywheel

**Source milestone for draft PR #18. Not compiled or in-game tested.**

## 3×3 machine contract
- A 3-block-wide, 3-block-high, 1-block-deep *vertical* machine.
- One central `iron_flywheel` Gear V2 controller, two existing Iron
  Machine Foundations for feet, six AIR positions required before forming.
- Four authored horizontal JSON blueprints reserve AIR with the existing
  V2 render-air proxy system. No oversized one-block collision workaround.
- Reuses the existing Wooden Flywheel Gear V2 charging/coasting arbitration.
  Original Wood remains 6-torque coast with original decay and save tags.
- New provisional Iron coast: 12-torque discharge and 0.40x Wood's decay,
  with 24-torque axle structural rating. No additional source torque.
  The existing storage is an RPM-based approximation, not a physically
  complete kinetic-energy conservation model.
- One 2.4-block-diameter client-rendered rim: 24 segments, 8 spokes,
  iron weights and bronze bearing. Fixed supports are baked V2 models.
  Texture face coordinates are proportional to size (no stretching).
  The 38-box V2 profile contains stationary bearing supports and a
  conservative moving-rim swept envelope; never a solid invisible disk.
- Original part map, formed flag, stored inertia, collision and selection
  are saved. Breaking center or a filler uses the existing multi teardown
  and drops original non-AIR construction materials.

## Single-player regression checklist
1. Run `node tools/iron-age/check-iron-flywheel-contract.mjs` and JDK 17
   `./gradlew build` from a full checkout; record actual results.
2. Craft in Survival; form four directions. A missing foot or a blocked
   AIR clearance cell must prevent formation.
3. Verify supports, axle, wheel rim, eight spokes and native texture UVs
   from all angles. No floating parts, intersecting footings or missing
   filler model, especially around chunk boundaries.
4. Connect shafts only along the central horizontal axle; verify 1:1
   speed and correct phase/direction. Unformed controller must be passive.
5. Charge from real water-wheel power. Compare Wood and Iron coasting
   under idle, 4-, 8- and 12-torque load; test source interruptions,
   repeated starts, overload and eventual stop. No infinite energy.
6. Check original Wood flywheel, wooden gear 8-torque bottleneck, iron
   shaft 24 limit and 16-maximum water-wheel output remain correct.
7. Walk/target around the reserved wheel sweep and supports. The
   entire 3×3 clearance stays reserved, without an invisible full disk.
8. Save/reload with charge, destroy center and filler separately; no
   phantom collisions, stuck gear nodes, duplicate drops or missing
   block items. Rebuild from drops and retest.
9. Resource reload, creative/JEI/Patchouli, all axes, world boundaries,
   FPS with repeated flywheels and existing processing-machine regressions.

**Status:** static source/JSON checks only. Forge build, in-game visuals,
performance, actual loaded coasting, save and teardown behavior require
player verification. Textures remain provisional for MGC2 Studio.
Multiplayer deferred until after Steam. Keep PR #18 draft/unmerged.
