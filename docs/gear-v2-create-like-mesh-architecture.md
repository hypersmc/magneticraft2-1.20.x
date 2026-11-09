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
