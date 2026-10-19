# Rotary Ore Classifier — Bronze II acceptance contract

**Status:** Compact **3×3×5 redesign** staged for PR #11 in Forge 1.20.1 after rejecting the first overscaffolded, tiny-drum prototype from the user's screenshots on 2026-10-19. This is a source-level geometry revision; **no `./gradlew build` or Minecraft test has been run against the redesign**.

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
AAA   discharge chutes above output modules

Layer Y=2:
BAA   one bronze/copper build block supports the hopper
AAA
AAA
AAA
AAA
```

`S` Stone Bricks, `P` Oak Planks, `B` Cut Copper, `C` Classifier Controller, `A` **required empty AIR**, `U` true Item Input module, `I` true Gear V2 Mechanical Input module, `O` Primary Item Output and `Y` Byproduct Item Output.

**Nineteen selected AIR cells** become V2 filler render/collision proxies after formation, including the real occupied space around the wide drum; all others remain AIR. East/west blueprints are **5×3×3** after rotation; north/south remain **3×3×5**. Both the local model origin and Voxel V2 rotations use the controller's 8-pixel X/Z pivot. Patchouli `multiblock` Visualize follows the exact canonical structure, transposed into Patchouli's X-column format.

**Development-world warning:** Unform/break the *old* 5×3×5 prototype before loading the new revision. Existing formed machine blocks and saved proxy positions cannot safely be migrated by merely retexturing; the source must not silently rewrite the player's test-world multiblock NBT.

## Appearance and engineering acceptance

- **The screen is the silhouette:** an uncovered octagonal **~22 px diameter**, three-block-long rotating wire-mesh drum; normal/worn `screen_mesh_bronze` cutout tiles contain actual alpha openings. At normal Minecraft FOV it must be obvious that ore is being classified in the drum.
- **Only four timber support posts**, two stations (rear/front), connected to real stone cradles and dark-iron lower girders. No five-wide timber perimeter, wood roof, disconnected bearing collar, invisible support, or repeated fence-look.
- **Connected process path:** high rear feed hopper above the real Item Input → rear screen mouth → lower bronze fines collection tray → left-front fines output. Coarse ore exits the front of the drum via a separate right-front lined chute and the genuine Byproduct port. The physical modules must remain unobstructed by V2 model geometry.
- **Connected drive:** the real input at (0,1,-3) has its own Gear V2 moving shaft; static rear bronze bearing surrounds the axis just *outside* its cell, and the first rotating segment starts immediately at z=-2. The module's shaft axis must follow the length of the drum. No fake drives through wood supports, floating couplers or conflicting rotation axes.
- **No UV stretching:** editable box-based design `tools/rotary-classifier/frame-design.json` builds block-local baked V2 faces with native 16-pixel texel density. Split faces across cells must **not** gain visible artificial end caps. Thin wear liners have separate physical thickness. The drum's 16px screen panels use vanilla-valid ±45° rotated elements.
- **Only drum moves in BER:** front/rear/middle normal/worn models are explicit registrations; fixed frame, hopper, collection basin and discharge chutes are baked into V2 models, not redrawn per frame. Two small visible ore fragments follow real processing progress inside the cutout screen and stop when power/processing stops; these are render-only and cannot duplicate items. Client rendering uses `RenderType.cutout()` and the real module's Gear V2 angle.
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
2. Day and night / survival and spectator views: the big 3-block mesh drum dominates, no wood canopy, 4 timber posts grounded, hopper and fines trough physically attached, two real output chutes. Check all sides for missing block parts, texture distortion, thin-plane z-fighting and fake double faces.
3. Run Gear V2 in both signed directions then remove power. Real module shaft and drum must remain coaxial and rotate in lockstep; stopped state freezes. No invisible motion, no ghost rotor/shaft passing through module, no animation jumps.
4. Feed 4 Crushed Chalcocite, Cassiterite and Quartz through the real input module and visually locate inventory items at *the actual* port, fines outlet and coarse outlet. Check 3 fines + 1 coarse return, pressure/torque limits, screen installation and screen wear/replace after many cycles.
5. Block primary then coarse output independently: machine pauses safely without wasting material and resumes. Confirm save/reload and chunk boundary persistence, automation and manual extraction.
6. Send 3 fines to Washer (600 mB Water/Dirty Water) and then Sifter. Old normal Crusher/Stamp → Washer → Sifter flows should still work, with no extra material or altered base recipes.
7. Performance comparison with first prototype from same world/view distance, especially FPS looking at the animated drum and BER client allocation; check that static geometry remains chunk-baked.
8. Break machine and reclaim its inventory, modules and components without duplication. Check collision while walking on/off platform and targeting posts, chute and screen, and absence of missing model cubes.

**Validation status:** generated source, spatial assertions and Git tree comparison can be validated without Minecraft. Only `./gradlew build` and real in-game screenshots can establish compilation and visual/behavior acceptance.
