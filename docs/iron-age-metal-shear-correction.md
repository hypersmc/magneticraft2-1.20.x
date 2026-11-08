# Iron III-A: Mechanical Metal Shear correction

Source implementation for draft PR #18, based on
47a058e940cc88e8aa1979d4961f4f9fe047437e.
This document supersedes the original direct rod-to-finished-rivet description.

## Scope

The display name is Mechanical Metal Shear. Existing block, block-entity,
recipe and maintenance IDs remain unchanged, including iron_stock_shear and
shear_iron_rivets. The 3x3x2 construction layout and real ports are unchanged.
The existing formed machine should be unformed and reformed during testing,
so cached voxel state cannot hide the new static model/collision profile.
Back up the test world first; do not delete world data to refresh it.

The shear now cuts 1 Wrought Iron Rod into 4 Iron Rivet Blanks, retaining its
16 RPM / 12 torque / 180 supplied ticks and 512-batch blade life. The existing
Forge Hammer cold-heads 4 matching blanks into 4 finished Iron Rivets at
20 RPM / 6 torque / 120 supplied ticks, consuming one existing die batch.
Small-blank cold heading is the chosen gameplay abstraction; this does not
remove heat requirements from blooms, ingots, plates or gear blanks.

Material NBT follows rod -> blank -> rivet. Both the Hammer's output reservation
and its diagnostic check use the same NBT-aware result for blanks. The original
cold Copper output handling is unchanged. Existing finished rivets and frame
recipes remain valid. Empty an old finished-rivet output slot before trying to
collect new blanks there; different item types must not merge.

## Mechanism and rendering

Reuse the existing baked JSON models, V2 static parts and Gear V2 input angle.
No new renderer framework, entity, mechanical network or runtime model builder.
Only the lower/upper central static cells change (and their four rotations).
The static voxel profile follows the changed geometry.

The drive meets the shared input shaft at the real rear module face. Equal
sheaves drive a supported upper shaft and crank. A constant ten-pixel connecting
rod drives the guided blade past a fixed edge, over an open cutting slot. The
feed rod uses its actual Z-aligned item model, with no stretching or scale hack.
All axes/facings compensate for the shared module's positive-world-axis angle.

The unloaded mechanism can free-cycle with its input; server-authoritative
load/progress/output/wear still decide whether any material is processed. It
must freeze when the actual network stops, rather than snapping to a rest pose
on every completed batch. Animation illustrates the mechanism; it is not an
individual-piece simulation or a promise of one item per visible revolution.

All new geometry uses existing textures. Final material/texture authoring remains
MGC2 Studio work. Moving components are rigid transforms of baked models.
The directional model checker compares parsed model definitions, not whitespace;
all geometry, UVs, material and orientation validation is retained.

## Verification boundary

Executed locally: actual pure-Java linkage class compiled with --release 17;
1,441 linkage poses; 768 model-face UV/dimension checks; eight changed static
parts bounded to their cells; sampled moving-vs-static clearance inspection
(73 poses and 43,508 depth-overlapping pairs), and rod/blank/rivet recipe counts.
The voxel source was reconstructed from fetched content and checked against
its original GitHub blob hash before applying its patch.

NOT executed: a full JDK 17 ForgeGradle build, the full repository contract suite,
Minecraft model baking, resource reload, live save migration, gameplay, visual
acceptance, FPS measurement, or multiplayer. No merge-ready claim is made.
A complete checkout could not be obtained in the assistant container because
GitHub DNS/network access failed. Authenticated GitHub file/tree actions are
used to publish the source changes; they do not constitute build verification.

## Run in a full checkout

    node tools/iron-age/check-metal-shear-geometry.mjs
    node tools/iron-age/build-stock-shear-parts.mjs --check
    node tools/iron-age/check-stock-shear-contract.mjs
    ./gradlew build

Compile the independent Java linkage test without Minecraft dependencies:

    mkdir -p build/shear-motion-test
    javac --release 17 -d build/shear-motion-test \
      src/main/java/com/magneticraft2/client/render/blocks/stage/iron/IronShearMotion.java \
      tools/iron-age/tests/ShearMotionTest.java
    java -cp build/shear-motion-test ShearMotionTest

## Single-player acceptance

- Confirm the book preview still forms the same SOUTH/NORTH/EAST/WEST machine;
  inspect the new feed bed, shaft coupling, blade guides and static collisions.
- Follow shaft -> belt -> crank -> link -> blade from both front and rear.
  Inspect slow motion, every facing, camera angles, F3+T and stopped power.
  Reject visible gaps, clipping, belt/rim artifacts or missing models.
- Cut one rod into four blanks; head four matching blanks into four rivets.
  Check JEI, book, material NBT and unchanged frame crafting.
- Verify 8-torque wood failure versus a genuinely supplied Iron path; stop power,
  fill output, wear/replace the blade, save/reload and check no duplication.
- Re-test Copper plates and hot Iron/bloom Hammer routes, die wear and old saves.

PR #18 remains draft. Multiplayer remains deferred until after Steam Age.
