# Iron IV-B — Wrought Iron Flywheel

**Player-confirmed working in-game (2026-11-08) on draft PR #18.** Full JDK 17 Gradle compilation and specific regression cases remain separately unverified.

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
  The 43-box V2 profile contains stationary bearing supports and a
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

## Correctness preflight: aligned central shaft and formation rollback

The SOUTH controller's first fixed shaft was incorrectly authored across
X even though the rotating wheel's shaft runs along Z. That duplicate
fixed shaft was subsequently removed. The rotating shaft is now drawn
ONLY by the Iron BER along its actual Z axis, with a simple stationary
collision envelope representing the space it occupies. The model's
stationary bearing rings are independent from the rotating axle.

The V2 assignment failure path now restores all originally validated
blocks in place from the controller's existing block map, including
all six required AIR positions, **without dropping construction items**.
Only successful formation commits the new formed flag and stored map.
This does not change ordinary intentional dismantling behavior.

In-game acceptance: attach shafts on each axial side and verify there is
no transverse/floating shaft; obstruct an AIR cell before forming and
confirm no block loss; repeat after saving/reloading. No build or runtime
claim has been made for this source correction.

### Single rendered axle
The stationary controller model does NOT draw the renderer-owned shaft.
Instead, it now draws TWO complete stationary bearing rings at opposite
sides of the rotating wheel's depth, each with a continuous iron support
arm attached to one of the two real foundation-supported uprights.
The dynamic shaft retains one physical collision envelope, but has
no stationary copy in the renderer.
Check both axle ends for z-fighting, correct support and 1:1 rotation.

## Split-bearing geometry correction — player screenshot evidence

Two supplied front/angled game screenshots showed a wooden-looking rim and
inward-facing iron uprights with unsupported arm stubs. Code inspection
confirms `WroughtIronFlywheelRenderer` binds only `dark_work_iron`
and `bronze_machined`, not any wood texture. A wood-colored wheel in
those screenshots may be a different nearby block entity or a rendering
selection problem; it is **not proven corrected** by changing supports.

The machine retains the existing **3×3×1** construction, controller,
registry IDs, recipes, Gear V2 behavior and all six air reservations.
No 3×3×3 expansion was necessary, because that would occupy the two
shaft-adjacent blocks and break existing axial connections.

A single SOUTH canonical model now defines 18 physically supported
stationary cuboids. The real foundation at left holds a forward-facing
upright/crossarm; the right foundation holds the rearward upright.
The left arm meets the FRONT bearing ring across the model-cell boundary;
the right arm meets the REAR bearing ring. Each four-piece bearing ring
has a central shaft clearance hole. The two rings straddle the wheel's
rotation plane, at local z=2–5 and z=11–14. The spinning wheel occupies
approximately z=6–10, so no static support enters the swept rim envelope.
All 32 filler models for the four horizontal directions are regenerated,
with native face-size UV mapping and no hidden geometry offsets.

Voxel V2 now has **43 physical shapes**: 18 genuine fixed-model supports,
1 exact-length collision envelope for the renderer-owned 0.92-block rotating shaft,
and the 24 unchanged swept-rim segments. No extra entities, physics
manager, power producer or per-tick rendering allocations were added.

Generator: `node tools/iron-age/build-iron-flywheel-bearings.mjs --check`
(use `--write` only when intentionally regenerating). Because the
formed V2 models and colliders are persisted, dismantle and rebuild
any already-formed Iron Flywheel to refresh it; the original blocks
drop as items rather than restoring in place.

### Targeted in-game visual/behavior regression
1. Build in a **clear test area separated from Water Wheels and other
   wooden flywheels**; confirm the Iron renderer displays the heavy
   dark metal rotor. If wood is still visible, inspect the targeted block
   ID and client renderer registration, rather than guessing textures.
2. Confirm the front and rear bearing rings actually surround the shaft,
   support arms touch both the ring and each foundation-supported post,
   and the rotating iron hub does not intersect either ring.
3. Check all four orientations, front/back and side angles, stopped and
   rotating, including the two external axial shaft connections.
4. Check actual wheel clearance/collision, save/reload, F3+T and FPS.
5. Confirm finite inertia, load/overload, and the unchanged Wooden
   Flywheel/Water Wheel visuals and mechanical behavior.

**Verification:** Source geometry and collision check only; this revision
still requires JDK17 Gradle compilation and in-game acceptance.

The former 0.92-block rotating shaft left a 0.04-block gap at either
connection to a full-length existing Wrought Iron Shaft. The rotating
shaft now spans exactly 1.00 block / 16 model pixels along the axle,
with matching 2×2-pixel cross-section and Voxel V2 collision from
z=0 to z=16. This makes both ends flush at their respective block
boundaries, with no overlapping extension into neighboring shafts.

### Formation-dependent controller model

The unformed controller now uses a compact, block-local forged shaft and
journal housing, with 1:1 UVs and an exact 6×6×6 collision core. The
completed multiblock instead uses the two separate supported bearing
rings. All four `facing` blockstate variants select the correct model
when the `is_formed` state changes. The item model also uses the compact
controller. This avoids rendering unsupported/floating bearing rings
before the foundations exist, without drawing a second stationary shaft
when the formed renderer animates its own axle.

### Both-side axial shaft gap (player screenshot follow-up)

The screenshot showed identical short air gaps between the formed
flywheel spindle and neighboring Wrought Iron Shafts on both axial
sides. Root cause in source: renderer drew a 0.92-block central axle,
while the existing 2×2-pixel Wrought Iron Shaft model occupies the full
block length. This was symmetrical by construction (0.04-block setback
at each face). The flywheel renderer now draws an exact 1.00-block
axle with the same 2×2-pixel cross-section, and the single axle V2
collision cuboid spans the corresponding 16 model pixels.

No adjustments to the bearing rings, frame offsets, 3×3×1 structure,
visual textures, Gear V2 connections, source torque, or old wooden
flywheel were made. This is a block-face alignment fix, not an
arbitrary overlap hiding the seam.

**In-game test:** dismantle and rebuild a formed Iron Flywheel to
refresh its V2 collision snapshot; test Wrought Iron Shaft connections
from BOTH axial faces with stopped and powered machinery, and check
all four facings. If a gap remains, inspect block-entity identity
and exact target geometry rather than extending past block boundaries.

## Player confirmation — 2026-11-08

The player expressly confirmed that **the Iron Flywheel is working in-game**.
Treat the principal Flywheel functionality as player-accepted rather than
an untested source-only prototype. Do not schedule further general flywheel
rework without a concrete reported issue.

This general confirmation is not a detailed test log: final individual
shaft-gap views on both sides, each orientation, measured inertia under
specific loads, older save migration, F3+T, teardown/drop checks, and FPS
measurements remain individually undocumented unless separately supplied.
No full JDK17 `./gradlew build` is recorded for this milestone.
PR #18 remains draft/unmerged and multiplayer work is deferred until
after Steam Age.
