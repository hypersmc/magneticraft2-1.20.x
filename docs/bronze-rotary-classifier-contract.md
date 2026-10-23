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
- **Connected drive:** the real Mechanical Input module at (0,1,-3) ALREADY renders the copper bearing and Gear V2 shaft, aligned along the drum's Z axis. Do not duplicate this bearing with another stationary ring or dark crossbars across the inlet. The rear rotating spindle meets that module at the cell boundary, while independently mounted bronze rollers support the rotating rim from outside. The separate left-rear item feed throat stays clear of the central drivetrain.
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

## 2026-10-23 — Rear/front supported-geometry revision (V4, superseded by V5)

**Problem observed in real client screenshots:** after removing decorative faux item holders, the rear small bronze intake and bearing assembly still appeared to float, and the front mesh ended in a shaft without a visible supporting bearing/roller. Static geometry auditing found exactly two disconnected components: `rear_bearing_*` and `feed_coupler_*`. The front rollers also stopped short of the drum's front hoop.

**Geometry solution (schema 4; purely visual):**

- Rear static axle collar is attached to each rear timber post with a slim dark-iron mount. The feed throat is now a short *hollow* four-sided bronze sleeve with a support brace reaching the same left rear post, **not** a fake inventory block.
- The rotating rear screen begins after a 2.1-model-pixel inlet clearance, keeping its mesh/hoop out of the stationary brace. The working bearings remain distinguishable from rotating cage parts.
- The open front drum loses only its unsupported front axial rod. Two fixed small bronze guide rollers sit on the existing grounded front support rollers and nearly touch the actual rotating outer rim. The opening itself remains clear for coarse return.
- Low-profile fines/coarse outlet guide plates and ribs are mechanically connected to the *internal* pan, ending **before the actual U/O/Y port block boundaries**, without independent containers, click targets, item capability handlers or changes to input/output module positions. The real item port blocks remain the only item interaction targets.
- All 59 authored static boxes form **one connected ground-supported component**, not separate floating islands. Source generation and regression tests fail if bearings/intake are detached again. The fixed V2 models and voxel boxes update across all four facings; **the 3×3×5 formation blueprint and Patchouli pattern stay exactly the same**. There are still 28 static model cells per facing and six normal/worn rotating drum models.

**Checks from source generation:** prior generator re-created all 126 previous generated files by exact Git blob hash; V4 changes 33 generated files (28 frame parts, four drum variants and one voxel profile), with no changed blueprints, invalid rotation angles, invalid model bounds or stretched UVs. The isolated model generator ran successfully with all geometry/source invariants satisfied. **The repository's Node contract script, Gradle build, actual connected automation, and live in-game appearance have not been reverified in this environment**. In-game, inspect the rear hub support and the open front rim/rollers from the same two screenshot angles and all four facings; watch for overlap during drum rotation.

## 2026-10-23 — V5 final rear-bearing design correction

**Screenshots:** V4 eliminated two floating subassemblies, but in doing so added long dark-iron rear-bearing brackets that visually pierced the open screening drum. The actual Mechanical Input module's copper housing was already the proper rear bearing. This is a **duplicate-geometry problem**, not a new texture or additional port issue.

- Remove all five static `rear_bearing_*` model boxes and both long `rear_bearing_mount_*` iron bars. Preserve the real `mechanical_input_module` as the sole central rear drive/bearing.
- Replace the unnecessary crossbars with two compact **external** bronze-machined rear roller contacts (`rear_guide_roller_w/e`). Like the front rollers, these rest on the original grounded side rollers and nearly meet the rotating octagon's outermost facet; neither crosses the drum's interior.
- Retain the four grounded timber posts, the open-ended 3-segment screen, the small supported input coupling and the short internal fines/coarse guides that stop before the true interactive output modules.
- Source schema **5** verifies all remaining fixed components are ground-connected, *both* ends' roller contacts meet their mesh hoop, and a 16×16-pixel central Gear V2 rear entry volume is free of any static part. Rendered UVs remain block-local, and no new materials, recipes or multiblock cells are introduced.
- Existing 2.1-pixel rear screen inset prevents the left-hand item-feed sleeve from entering the rotating mesh while exposing the radial spokes and real central drive shaft.

**Verification status:** source-level model generation and exact resource diff checks can run without a Minecraft client. A full `node tools/rotary-classifier/test-contract.mjs`, `./gradlew build`, all facings, moving/idle screen and in-world visual alignment must still be tested. Do **not** mark the model accepted solely from static connectivity or source generation.

## 2026-10-23 — Animated material handling and actual-port previews

**Rendering contract (no recipe or inventory changes):**

- The REAL item input is at local (-1,1,-3) and its physical model tops out at 15/16; a queued raw item should appear at local (-0.5, **1.97**, -2.5), not hovering over a former fake hopper.
- Primary/fine and secondary/coarse outputs remain local (-1,0,1)/(1,0,1); their actual port model tops out at 10/16. Existing **committed** primary fines use the one built-in classified-ore tray at local (-0.5, **0.890**, 1.5); byproduct chunks use one or two pieces at local (1.5, **0.620**, 1.5). These are different render anchors because their item model GROUND transforms are different. An output item must never render as an already-produced result unless it exists in the server-owned output slot.
- Workpiece travel follows the **server-synced batch progress**: first 20% slides from physical input via the rear feed neck into the drum, middle portion tumbles up to **two** small crushed ore models along the lower bronze screen, small fines visibly sift toward the internal collection pan, final 20% advances the coarse return toward the open discharge end. The resulting finished items spawn visually only on the real ports when the controller actually inserts them at batch completion.
- When a batch stalls due to output backpressure, inadequate Gear V2 power or worn-out screen, its **partially completed workpiece remains inside the drum** rather than jumping back to the input. Stalled progress and all particulate effects remain static/absent. Stored queue material may still appear on the actual input module if a full later batch remains.
- Only Gear V2 may move the drum. Client workpiece movement is grounded in the existing Gear V2 phase and progress interpolation, not a free-running world-time animation.
- Restrained *dry* effects: falling gravel particles and muted brown/grey dust below the mesh (one client particle opportunity every 10 ticks), plus low-volume gravel rattling every 40 ticks. Effects exist only while the server says the machine is processing, have an 18-block nearby-player check, and use the same SOUTH/WEST/NORTH/EAST coordinate transformation as the drum renderer. They create **no physical item entities, byproducts, fluids or extra ore** and never alter recipe probabilities or wear.
- The renderer resolves the input's item model once per frame, limits itself to at most two normal tumbling fragments (plus feed/fines/coarse previews), and caches the active batch input count per recipe to avoid repeated datapack scans. A client tick—never the frame renderer—spawns particles and plays sound, keeping higher FPS from raising particle rates.

**In-game verification:** test initial idle storage, input with 4 versus 8 ore, active feed, screened fines, coarse discharge, both actual finished output trays, automatic extraction/pickup, shutdown during a batch, blocked primary and secondary outputs independently, a worn screen, four facing rotations, positive/negative Gear V2 rotation, and view-distance FPS with many machines. Expect zero idle effects and no visual output item before the server inserts one. `node tools/rotary-classifier/test-contract.mjs`, `node tools/rotary-classifier/build-static.mjs --check`, `node tools/bronze-stamp-mill/test-contract.mjs`, and `./gradlew build` still need to be run against the complete checkout before merging PR #11.

### Ore readability / scale follow-up (2026-10-23)

Screenshots from the open drum showed that the visible crushed ore fragments were much too small for a machine this size. The three 3D crushed-ore item models already apply an intrinsic ground scale of 0.5, so extra multipliers of 0.22/0.12 shrank chunks to a tiny fraction of a block. The new client BER uses scale constants of **0.82** for the main rolling chunks (two instances maximum), **0.38** for finer particles falling toward the pan, **0.72** for the feeding/coarse workpiece and **0.68** for items visible on the REAL input and output modules. **Superseded:** the interim Y≈1.50 anchor used an incorrect Y=2.00 drum centre, leaving the enlarged chunks at the real shaft height. The actual shaft rotates at Y=1.50 and the lower inner screen is near Y=0.84; the following correction resolves this. The coarse-discharge trajectory is limited to remain inside the open hoop. The two port-stack display pieces are spaced across the actual tray top to avoid model overlap.

This is strictly a **visual** change: no new items, item entities, particles, sounds, recipes, ore quantities, fluid mechanics or texture files. In-game, verify that larger pieces look convincingly sized and do not intersect the octagonal mesh in any cardinal orientation while rotating, and that output stacks stay within their real 1-block module surfaces. Keep `MAX_DRUM_FRAGMENTS=2` and the per-frame model lookup/particle budgets unchanged. Source checks verify ratios and approximate cylindrical clearances; they are not a substitute for `./gradlew build` or client screenshots.

### 2026-10-23 — Actual drum pivot and material-bed correction

In-game evidence exposed another rendering problem after ore was enlarged: ore material passed through the central axle instead of sitting against the lower screen. This was introduced by the previous ore-scale correction, which assumed the model rotates around Y=2.00. **The true drum centre is Y=1.50**: the renderer's `pose.translate(0.5,1.5,dz+0.5)` already centres its 8px local model pivot. Adding another +0.5 was wrong.

- Keep large tumbling chunks at client render scale **0.82**; their *model origin* now sits at **Y=0.96**, with small Gear V2-driven bouncing. Crushed ore itself includes an intrinsic 0.5-scale GROUND transform, a +3px GROUND translation and element rotations, so rendered rock geometry appears slightly above that origin.
- The screen inner lower surface is roughly Y=0.84, the shaft centre Y=1.50 and the shaft underside Y≈1.43. The new material bed is near the lower mesh and below the moving shaft. Fine fragments descend 0.46 blocks toward the pan at Y≈0.47 without passing through it.
- Keep the coarse chunk inside the front rim, ending at local Z=0.70 before any actual output item appears in the true physical port. Actual recipe outputs remain server-owned; the in-drum contents are decorative only.
- The regression contract derives conservative radial and vertical envelopes from all three registered crushed-ore item models, their JSON element rotations and GROUND display transforms, and compares against the actual drum radius, axial shaft, internal fines pan and front rim. The test must not reintroduce the erroneous extra +0.5 Y offset.

**Verification pending:** Full checkout Node test suite, Forge 1.20.1 `./gradlew build`, and in-game checks in each facing at multiple shaft angles. No machine inventory or processing behavior is changed by this fix.


## 2026-10-23 — V6 true rotating-screen contact (awaiting in-game approval)

In-game review of the old enlarged-rock animation revealed an underlying design error:
the rocks followed scripted X/Y sine waves and a progress-driven Z path while the
actual barrel rotated around them. Adjusting the ore rest height did not make
that motion physically connected to the machine.

**Replacement:** the BER now keeps one weakly referenced client-only motion state
per formed controller. At most two crushed-ore models simulate at 20 Hz against
the eight rotating screen facets (using the same Gear V2 sign/phase as the baked
drum), gravity, mesh friction and central-shaft exclusion. Model-centre-aware
rotation makes chunks *roll* instead of spinning around an unrelated Y-axis.
Modest axial transport advances only while running, rather than interpolating
the full 2.28-block path from recipe progress. One small fine chip may fall from
a real lower-screen contact toward the stationary internal pan; normal particle
and sound budgets remain unchanged.

This is **visual only**. The input throat feed remains staged for the first
20% of a batch; finished results are still shown solely when the server has
committed them to the real primary and byproduct ports. Stalled batches freeze,
new batches reset visual state, and no item entities, fluids, recipes, wear,
gear physics or multiblock geometry change. There is no full 3D granular solver.

**Verification:** standalone pure-Java contact/regression harness
\`tools/rotary-classifier/OreMotionContractTest.java\` checks octagonal and
shaft clearance, sign reversal, contact-triggered fines, axial transport,
stall freeze and batch reset; \`test-contract.mjs\` compiles and runs it as
part of the contract. Local standalone harness passed. Full repository Node
and Gradle builds and real-client rendering require checking in a full
working checkout. Test +/− mechanical direction, 24 RPM and higher, all
four facings, collisions at rim/shaft, speed changes, paused output, input
with multiple batches, and FPS with many machines before merging PR #11.


## 2026-10-23 — V7 anti-adhesion ore slip (awaiting in-game acceptance)

The first V6 client test video showed that after an initial tumble, crushed ore
seemed to attach to the side wall. A standalone 24 RPM reproduction confirmed
a persistent, nearly stationary X/Y position after the first lift despite ongoing
roll and Z travel. The old wall collision applied a 31% tangential velocity pull
every touching tick regardless of available normal pressure.

The updated client-only physical model caps mesh traction by the estimated
normal reaction (gravity plus contact impulse) and adds an angle-of-repose
hysteresis: a carried rock loses grip at ~50 degrees off the lower bed and
does not regrip until it has slid back below ~26 degrees. There is no
time-based bounce loop or scripted material path. Both gear directions use
the same contact and gravity; no inventory, recipes, construction, size,
textures, wear or Gear V2 logic changes.

**Regression:** OreMotionContractTest now requires at least three distinct
lift/slip/fall cycles and significant X/Y movement in EACH later 45-tick
window, with clockwise/counter-clockwise tests at 24 and 48 RPM. The original
V6 code fails the regression (zero repeat falls); V7 passes locally with the
same shaft/octagon contact checks. Full Node/Gradle builds and real-client
visual approval remain pending.


## 2026-10-23 — V8 primary output visibility (pending client screenshot)

In-game screenshot showed a full-sized crushed/byproduct stack on the right,
but the primary fines port looked empty. The port was not necessarily empty:
the classified ore item models have a .7 GROUND transform **without** the +3px
GROUND lift used by crushed ore. The shared BER origin Y=.665 placed even the
topmost part of a classified ore model at Y~.546, below the output module's
actual wooden top at Y=10/16=.625.

Fine/primary output now uses its own origin Y=.890 at render scale .82, putting
the built-in bronze-grit tray bottom around Y=.639 and the upper pellets near
Y=.747. Only **one** classified model is drawn per non-empty slot-1 inventory
because its JSON model already contains several pieces on one tray; rendering
two identical items would overlap their solid tray geometry. Coarse/byproduct
keeps one/two distinct crushed-ore pieces but its preview origin moves to
Y=.620, leaving their lowest geometry around Y=.641 rather than hovering.
All three Chalcocite/Cassiterite/Quartz variants share the same model bounds,
and the Node contract tests the true port surface against their actual display
transforms to guard against disappearing preview geometry.

These are only stored-slot visual previews. Empty ports remain empty; no
precommitted outputs, duplicate inventories, item entities, new decorations,
model-generation changes or multiblock reconstruction are introduced.
Test both ports filled after a batch and empty after player/automation
extraction, in all four facings. Source math is not in-game acceptance.
