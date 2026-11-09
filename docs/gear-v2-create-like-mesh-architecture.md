# Gear V2 — Create-like gear placement and mechanical consistency

**2026-11-09 · implemented code contract · PR #19, Forge 1.20.1**  
**Status:** source changes committed; compilation, runtime placement, animated mesh and regression testing remain unverified. This is an explicit gameplay connection-rule change from the earlier Steel III prototype.

## Approved connectivity (not an additional Mechanical Power System)

| Physical arrangement | Drive behavior | Signed speed |
| --- | --- | --- |
| Medium 8T ↔ Medium 8T | Same-axis, one block apart in the rotation plane | -1 |
| Medium 8T ↔ Large 16T | Same-axis, diagonal (1,1) in the rotation plane | -1/2 towards Large, -2 towards Medium |
| Large 16T ↔ Large 16T | **Perpendicular rotation axes**, offset ±1 block on each of the two axes and zero on third | 1:1, cross-axis sign by actual quadrant |
| Large 16T ↔ Large 16T | Parallel axes, two blocks apart | **No mechanical link**, though placement is allowed |
| Large ↔ Medium ↔ Large | Each diagonal medium/large contact is valid; large gears are linked only through medium | Two reversing contacts, net 1:1 and same direction |
| Gear ↔ Shaft | Same axle face, adjacent | 1:1 |
| Existing Gearbox/Custom Gearbox/Belts/Water Wheel | Preserve existing specific port and rim connection semantics | Unchanged |

Only actual `LargeGearBlockEntity_wood` instances and their Iron/Steel subclasses can form the new crossed-large link. A large Water Wheel rim or a generic machine GearNode cannot impersonate a crossed 16T gear.

**Perpendicular geometry:** X/Y, X/Z and Y/Z axis pairs all work. The pair centre delta is (+/-1) on each participating axis, and zero on the remaining axis. Reversing the pair must preserve the relative sign; sign is negative if the two axial offset signs differ and positive if they agree. The existing Gear V2 shaft reference axes and client rotation conventions are used.

## Loop consistency and intentional stalls

At each server network evaluation, traverse the cached physical gear connections and establish relative *signed* RPM for each connected component. Every edge—including one returning to an already visited node—must agree with the previously established rate. Two active sources in one component must also agree on their signed source RPM after their gearing ratios. If not, mark **the entire component overloaded/stalled**; do not choose one arbitrary path, permit invalid motor operation, or silently drive competing sources.

The check is deterministic and server-authoritative. It reuses the existing Gear V2 graph and contains no new block entities. A blocked source preserves its actual RPM through the invalid configuration so removing the offending connection permits recovery rather than permanently erasing source state. Do not persist transient topology-conflict flags as permanent broken components. Keep torque capacity, mechanical load and flywheel accounting unchanged.

A triangle of three crossed Large Gears on X/Y/Z axes imposes contradictory signed rotation (one negative parity edge) and must **stall**, as Create-like gear geometry also requires. The three-gear layout with two parallel Large Gears and a Medium diagonal bridge is consistent and should **run**.

## Important save compatibility change

No block/BE IDs or NBT shapes are changed. Existing placements remain placeable and loaded, including adjacent parallel Large Gears. However **a factory that relied on two parallel Large Gears at distance two to transmit power will no longer do so**; the player must add the proper Medium bridge, use a gearbox/shaft alternative, or reorient the Large pair perpendicular. This deliberate mechanical behavior change is documented instead of silently migrating or deleting gears.

## Rendering and model boundaries

Do **not** shift gear centres, alter axle placement, apply renderer offsets or stretch UVs to fake connectivity. The shared Iron/Steel 8/16-tooth model prototype remains a visual candidate and must be independently judged for all supported pair types, including new crossed Large Gears. The old effort to make parallel Large-to-Large teeth interlock is superseded by this design. MGC2 Studio owns final geometry.

## Source verification and acceptance

- Run `node tools/gear-v2/check-create-like-gear-mesh.mjs` for standalone mathematical rule and source-invariant checks.
- Run `./gradlew build` with JDK 17. There is no recorded successful build in this implementation turn.
- In-game regression: 8T/8T adjacent on X/Y/Z planes; 8T/16T diagonals in all quadrants; 16T/16T crossed in all axis pairs and quadrants; both placement orders; cross-direction signs.
- Confirm two parallel Large Gears at distance two **do not** drive each other. Add a Medium at the correct diagonal and confirm both Large Gears rotate in the **same direction**.
- Place three crossed Large Gears forming a contradictory loop: verify **all stop**, an attached machine receives no work, and removal of one gear restores motion without replacing the source.
- Attach two compatible sources, then incompatible speed/opposite direction sources; verify the latter stalls without generating energy.
- Verify existing water-wheel rim to gear, shaft, gearbox, custom gearbox, belts, clutch, brake, overload disconnect and flywheel continue working. Test save/reload and chunk boundaries.
- Visually inspect alignment, gears' tooth phase, cog intersections, gearbox and shaft geometry and lighting in all three planes, stationary and moving, from multiple camera angles; record FPS before/after.

**Engineering status:** implemented in source, acceptance pending. Multiplayer-specific tests remain deferred per project milestone, but all logic is server owned.

## Perpendicular Large Gear tooth-phase correction — 2026-11-09

The player screenshot shows crossed Large Gears rotating with tooth-to-tooth interference. The first version assigned `meshPhaseDegrees(0)` for every `PERPENDICULAR_GEAR_MESH` connection. For our 16T models, the driven gear needs **half a tooth pitch (180/16 = 11.25 degrees)** relative to the driver.

`GearNetworkManager.calculatePerpendicularMeshPhaseDegrees()` now calculates `(halfPitch + turnSign * upstreamMeshPhase) mod toothPitch`. This correction is used in powered propagation, server-authoritative rotation locking, and passive alignment. The upstream phase matters when a crossed gear is itself driven through an 8T/16T mesh. No model assets, shaft positions, render offsets, gear ratios, saved NBT or torque graph are changed.

The actual 66-element Large Iron/Steel block model was checked as oriented 3D cuboids, using representative Forge blockstate axes (X-east, Y-down, Z-north) with animation rotation. At 24 axis/direction cases and 8 angular samples per tooth pitch, the **11.25-degree relative offset gave no sampled cuboid overlaps**; the old zero-phase relation produced up to three overlapping tooth cuboid pairs in the original test. A repeatable regression checker using the real model and three inherited input phases is `tools/gear-v2/check-perpendicular-tooth-phase.mjs`.

**Important:** a closed X/Y/Z triangle of *three* crossed Large Gears is kinematically contradictory. No continuously moving tooth alignment is possible across all three connections. The network is expected to stall; test a single perpendicular pair to validate the visual phase, then separately verify that the triangle stalls and recovers when broken. Any remaining static tooth intersections in a contradictory, stopped assembly are not evidence that a valid two-gear mesh is misphased.

**Not verified:** JDK 17 `./gradlew build`, all block-facing variants, game-render visuals, multiplayer behavior, performance or actual player acceptance.

## Wooden gear missing-model and manual-drive cleanup — 2026-11-09

The handle-less Wooden Large Gear had **15 unsupported Blockbench rotations** in its `gear_large_wood.json` resource (including a non-Forge `{x,y,z}` rotation object and values beyond ±45°). The model fails Minecraft 1.20.1's baked-model rotation validation, appearing as missing black/magenta geometry. The Wooden Medium, handle-less Large and handled Large models now reuse the proven **original Magneticraft2 8T/16T metal gear cuboid profiles**, retextured with `minecraft:block/oak_log`, `oak_log_top`, `cobblestone` and the retained handle's `minecraft:item/leather`. All three have proportional face UVs, supported rotation values, a genuine 4×4 stone axle and the original handle-arm/handgrip geometry where applicable.

The inventory Item models now inherit the corresponding validated block geometry, while preserving each item's existing in-hand/GUI transformation, so held models no longer differ mechanically from the placed silhouette. The old invalid Large Gear JSON is not redistributed or referenced.

**Hand drive:** `LargeGearWithHandleBlockEntity_wood` now uses a named **40 RPM** source speed instead of the prior 130 RPM. The existing 8 game-torque output and five-second (100-tick) click duration are unchanged. This balance value is provisional and requires user feedback; downstream 2:1 gear ratios continue to follow Gear V2.

**Perpendicular connectivity:** the handled 16T wooden gear is explicitly recognized as a physical Large Gear together with passive Wooden/Iron/Steel instances. Water Wheel rims remain excluded. The existing crossed half-tooth phase calculation applies to these connections; no special-case wooden direction offset or duplicate renderer path was introduced.

**Verification:** standalone resource/model check `node tools/gear-v2/check-wooden-gear-models.mjs`; check both unpowered world placement and rendered rotation, item GUI, the handle when idle/active, 8T↔8T, 8T↔16T, crossed 16T↔16T with and without the handled gear; check water-wheel and previous machine behavior. Inspect collision, UV clarity, phase throughout rotation, source speed and actual mechanical load. Forge compilation and in-game visual verification remain **unverified**.

### 2026-11-09 additional wooden geometry verification

After correcting malformed axle coordinate arrays in the generated wooden models, the 3D oriented-box sweep using the actual 66-element unhandled and 68-element handled wooden models found **zero intersections** for both variants across 24 crossed-axis orientation/quadrant cases and 8 rotation samples each (192 per variant). The stone axle and original projecting handle are included. This approximates cuboid contact under the representative blockstate axis transforms, not actual Forge lighting, UV sampling, client interpolation or a full revolution test. The static checker now rejects missing/invalid third coordinates, non-finite values and invalid UVs; the same 384 swept samples are covered in `tools/gear-v2/check-wooden-gear-models.mjs`. In-game confirmation and `./gradlew build` remain pending.

## Gear V2 rotation continuity — 2026-11-09

**Player video evidence:** a 16T manual right Large Gear drives an 8T Medium bridge, which drives a left 16T Large Gear. The left gear stutters/appears to reverse while the source turns steadily. The actual source shows that `lockDrivenGearRotations()` calculates a phase-correct absolute angle for the downstream gear and sets it directly after `GearNode` has normalized intermediate angles modulo 360°. A 16T→8T→16T path can make equivalent outer-gear solutions differ by **180°** across an angle wrap, which the client then treats as an actual correction.

**Minimal implementation:** retain `calculateMeshedPhaseDegrees()` and the established tooth-to-gap law. In the authoritative server's powered mesh lock, take the mathematical neighbor angle and select the solution that differs by a multiple of the neighbor's **full tooth pitch (360 / teeth)** and is closest to that neighbor's existing angle after `advanceGearRotations()`. Store the nearest equivalent in the existing normalized `RotationDegrees` value. This removes false wrap-around jumps without new per-tick collections, an unbounded rotation counter, new NBT or a second animation system. The engine's actual RPM, ratio, torque, source and direction remain unchanged. Idle and crossed-axis connections retain their prior mechanics.

The runnable mathematical regression `node tools/gear-v2/check-gear-angle-continuity.mjs` tests 16→8→16 and 8→16→8 tooth chains for 1200 ticks each at 40, 130 and 300 RPM in both directions. It asserts the predicted smooth movement at every tick and independently reproduces the old uncorrected angle alias. The test is **not** Forge compilation or a client-side visual test.

**In-game checks:** replay the original user arrangement (handled 16T → 8T bridge → passive 16T), watch the left spoke pattern through several complete revolutions, reverse the source orientation, stop/start manual power, then compare Wooden/Iron/Steel mixed-material chains. Confirm smooth client interpolation at normal and low FPS, no gear teeth clipping, proper overload and zero work when jammed, and save/reload with no phase snap. Also test the existing perpendicular 16T gear pair and belts/shaft/gearbox connections. PR #19 remains draft pending player acceptance and build proof.
