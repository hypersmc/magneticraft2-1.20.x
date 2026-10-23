# Rotary Ore Classifier — Bronze II acceptance contract

**Status:** Compact **3×3×5 design**, with a 2026-10-23 port-visibility correction after in-game screenshots showed noninteractive duplicate hoppers and output holders. Source belongs to draft PR #11 (Forge 1.20.1). Minecraft screenshots confirmed the previous geometry ran but showed false noninteractive holders; **the new port-exposure fix has not yet been retested in game or verified by `./gradlew build`**.

## Functional scope — intentionally preserved

The classifier is an optional mechanically-driven sizing stage following Stamp Mill/Grinder, not a replacement for Copper Ore Washer or Sifter. For each of Chalcocite, Cassiterite and Quartz:
- `4 × crushed_<ore>` → `3 × classified_<ore>` **fine fraction** + `1 × crushed_<ore>` **coarse return**, 180 ticks, Gear V2 ≥24 RPM / ≥4 torque. Material is conserved, no tailings are invented.
- `3 × classified_<ore>` + 600 mB Water → `3 × washed_<ore>` + 600 mB Dirty Water in the existing Washer, 240 ticks; the Sifter continues to extract the mineral product.
- Ordinary crushed-ore washing recipes, all preexisting Bronze Stamp Mill, Copper Sifter, quality and worldgen mechanics remain unchanged. Separate primary/fines and byproduct/coarse physical ports still map to controller slots 1 and 2.
- The bronze screen is a persistent, replaceable **384-batch** maintenance item. A stopped/driveless/blocked operation must not consume input, leak output or wear the screen. The Wear V2 art swaps the screen mesh, not the surrounding frame.

## 3×3×5 construction blueprint

**Canonical SOUTH**. Controller at (0,0,0). Rows are rear z=-3 → front z=+1; characters west x=-1 → east x=+1, bottom Y=0 → top Y=2:

```text
Layer Y=0:
SSS   rear stone cradle
SAS
SAS
SCS   controller at middle
OAY   front ports: fines west / coarse east

Layer Y=1:
UIA   real Item Input at (-1,1,-3), Gear V2 at (0,1,-3)
PAP   rear timber bearings
AAA
PAP   front timber bearings
AAA   AIR: no false bins/chutes over the real output ports

Layer Y=2:
AAA   AIR: input must remain accessible from above
AAA
AAA
AAA
AAA
```

`S` Stone Bricks, `P` Oak Planks, `C` Classifier Controller, `A` **required empty AIR**, `U` true Item Input module, `I` true Gear V2 Mechanical Input module, `O` Primary Item Output and `Y` Byproduct Item Output.

**Fourteen selected AIR cells** become V2 filler render/collision proxies after formation, including the real occupied space around the wide drum; all others remain AIR. East/west blueprints are **5×3×3** after rotation; north/south remain **3×3×5**. Both the local model origin and Voxel V2 rotations use the controller's 8-pixel X/Z pivot. Patchouli `multiblock` Visualize follows the exact canonical structure, transposed into Patchouli's X-column format.

**Development-world warning:** Unform/break either the first 5×3×5 scaffold or the 3×3×5 model with an upper Cut Copper block before loading this port-exposed revision. Existing formed machine blocks and saved proxy positions cannot safely be migrated by merely retexturing; the source must not silently rewrite the player's test-world multiblock NBT.

## Appearance and engineering acceptance

- **The screen is the silhouette:** an uncovered octagonal **~22 px diameter**, three-block-long rotating wire-mesh drum; normal/worn `screen_mesh_bronze` cutout tiles contain actual alpha openings. At normal Minecraft FOV it must be obvious that ore is being classified in the drum.
- **Only four timber support posts**, two stations (rear/front), connected to real stone cradles and dark-iron lower girders. No five-wide timber perimeter, wood roof, disconnected bearing collar, invisible support, or repeated fence-look.
- **Only real ports are interaction points:** U at (-1,1,-3) is the input, O at (-1,0,1) the fines outlet, Y at (1,0,1) the coarse outlet. There are **no additional hoppers, catch bins, item trays or independent inventories** in decorative V2 geometry. A short machined-metal coupling meets the REAL input module's front face; the low bronze pan underneath the screen is an internal separating part, not an output holder. The mesh discharges into the actual item-port inventories. Keep the block AND the full cell immediately above each U/O/Y module free of solid/decorative geometry.
- **Connected drive:** the real input at (0,1,-3) has its own Gear V2 moving shaft; static rear bronze bearing surrounds the axis just *outside* its cell, and the first rotating segment starts immediately at z=-2. The module's shaft axis must follow the length of the drum. No fake drives through wood supports, floating couplers or conflicting rotation axes.
- **No UV stretching:** editable box-based design `tools/rotary-classifier/frame-design.json` builds block-local baked V2 faces with native 16-pixel texel density. Split faces across cells must **not** gain visible artificial end caps. Thin wear liners have separate physical thickness. The drum's 16px screen panels use vanilla-valid ±45° rotated elements.
- **Only drum moves in BER:** front/rear/middle normal/worn models are explicit registrations; fixed frame, small input coupling and internal collection pan are baked into V2 models, not redrawn per frame. Two small visible ore fragments follow real processing progress inside the cutout screen and stop when power/processing stops; these are render-only and cannot duplicate items. Waiting inputs are previewed ON the actual elevated input module, and finished items are shown ON their respective real ground-level output modules. Client rendering uses `RenderType.cutout()` and the real module's Gear V2 angle.
- The voxel profile follows original structural boxes plus a simplified rotating-drum physical envelope, clipped to each valid controller/filler cell; all AIR proxy cells are defined in the JSON blueprint. Port modules own their block models/capabilities. No static or collision geometry may penetrate U/I/O/Y modules.
- **No change to ore fractions, water, age assignment, Gear V2 computation, damage, maintenance logic or world generation** in this visual revision.

## Reproducible asset build

```bash
node tools/rotary-classifier/build-static.mjs --check
node tools/rotary-classifier/test-contract.mjs
node tools/texture-art/build-kit.mjs --check
node tools/bronze-stamp-mill/test-contract.mjs
./gradlew build
```

The first script also checks for obsolete narrow-drum and former scaffold JSON files. To intentionally regenerate after editing the design source, omit `--check`. Do not hand-edit generated `part_*.json` or directional blueprints.

## What to test in Minecraft before approval

1. **Break old prototype first**, rebuild the new exact Patchouli layout in SOUTH/WEST/NORTH/EAST and form; check block substitutions, real U/I/O/Y modules and AIR proxies. Confirm dimensions rotate (3×3×5 vs 5×3×3).
2. Day and night / survival and spectator views: big 3-block mesh drum, four grounded timber posts, one internal fines pan and a small metal intake coupling; no standalone decorative hopper or fake output receptacles. Check all sides for missing block parts, texture distortion, thin-plane z-fighting and fake double faces.
3. Run Gear V2 in both signed directions then remove power. Real module shaft and drum must remain coaxial and rotate in lockstep; stopped state freezes. No invisible motion, no ghost rotor/shaft passing through module, no animation jumps.
4. Feed 4 Crushed Chalcocite, Cassiterite and Quartz through the real input module and visually locate inventory items at *the actual* port, fines outlet and coarse outlet. Check 3 fines + 1 coarse return, pressure/torque limits, screen installation and screen wear/replace after many cycles.
5. Block primary then coarse output independently: machine pauses safely without wasting material and resumes. Confirm save/reload and chunk boundary persistence, automation and manual extraction.
6. Send 3 fines to Washer (600 mB Water/Dirty Water) and then Sifter. Old normal Crusher/Stamp → Washer → Sifter flows should still work, with no extra material or altered base recipes.
7. Performance comparison with first prototype from same world/view distance, especially FPS looking at the animated drum and BER client allocation; check that static geometry remains chunk-baked.
8. Right-click the input/output blocks themselves, including the top of Input and Outputs: there must be no intercepted interaction on a decorative tray. Break the machine and reclaim its inventory, modules and components without duplication. Check collision while targeting posts, internal pan and screen; no missing model cubes.

**Validation status:** generated source, spatial assertions and Git tree comparison can be validated without Minecraft. Only `./gradlew build` and real in-game screenshots can establish compilation and visual/behavior acceptance.

## 2026-10-23 — Rear/front supported-geometry acceptance revision

**Problem observed in real client screenshots:** after removing decorative faux item holders, the rear small bronze intake and bearing assembly still appeared to float, and the front mesh ended in a shaft without a visible supporting bearing/roller. Static geometry auditing found exactly two disconnected components: `rear_bearing_*` and `feed_coupler_*`. The front rollers also stopped short of the drum's front hoop.

**Geometry solution (schema 4; purely visual):**

- Rear static axle collar is attached to each rear timber post with a slim dark-iron mount. The feed throat is now a short *hollow* four-sided bronze sleeve with a support brace reaching the same left rear post, **not** a fake inventory block.
- The rotating rear screen begins after a 2.1-model-pixel inlet clearance, keeping its mesh/hoop out of the stationary brace. The working bearings remain distinguishable from rotating cage parts.
- The open front drum loses only its unsupported front axial rod. Two fixed small bronze guide rollers sit on the existing grounded front support rollers and nearly touch the actual rotating outer rim. The opening itself remains clear for coarse return.
- Low-profile fines/coarse outlet guide plates and ribs are mechanically connected to the *internal* pan, ending **before the actual U/O/Y port block boundaries**, without independent containers, click targets, item capability handlers or changes to input/output module positions. The real item port blocks remain the only item interaction targets.
- All 59 authored static boxes form **one connected ground-supported component**, not separate floating islands. Source generation and regression tests fail if bearings/intake are detached again. The fixed V2 models and voxel boxes update across all four facings; **the 3×3×5 formation blueprint and Patchouli pattern stay exactly the same**. There are still 28 static model cells per facing and six normal/worn rotating drum models.

**Checks from source generation:** prior generator re-created all 126 previous generated files by exact Git blob hash; V4 changes 33 generated files (28 frame parts, four drum variants and one voxel profile), with no changed blueprints, invalid rotation angles, invalid model bounds or stretched UVs. The isolated model generator ran successfully with all geometry/source invariants satisfied. **The repository's Node contract script, Gradle build, actual connected automation, and live in-game appearance have not been reverified in this environment**. In-game, inspect the rear hub support and the open front rim/rollers from the same two screenshot angles and all four facings; watch for overlap during drum rotation.
