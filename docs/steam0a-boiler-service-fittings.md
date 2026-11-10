# Steam 0-A — Boiler service fitting implementation

**2026-11-10, draft PR #19. Source implemented, build and Minecraft runtime NOT verified.**

Four inert separate placeable parts attach to the **formed** Steel IV-C drum. No existing multiblock JSON, baked shell geometry, original 53-block BOM, controller storage or rendering code is modified. These attachment blocks are NOT original multiblock members; they are independently crafted/mined/returned and must be removed separately when the vessel is dismantled. Their placement requires the correct formed controller in the same loaded area and a real adjacent multiblock-filler support. No free-standing parts. They do not automatically detach when the boiler is broken; that is an explicit limitation to avoid silently destroying items.

| Fitting ID | Canonical SOUTH offset from vessel controller | Contact | Assembly Station recipe |
| --- | --- | --- | --- |
| `boiler_water_inlet` | (2, 2, -2) | Right-hand barrel wall | 1 pressure_pipe_coupling + 1 iron_plate_panel + 1 refined_steel_rod + 2 iron_rivet |
| `boiler_steam_outlet` | (0, 4, -2) | Top of middle barrel course | 1 pressure_pipe_coupling + 1 boiler_reinforcement_flange + 1 refined_steel_plate + 2 iron_rivet |
| `boiler_firebox_mount` | (0, 1, 1) | Lower front end-ring firebox hatch | 1 riveted_iron_casing + 1 fired_refractory_ceramic + 1 refined_steel_plate + 4 iron_rivet |
| `boiler_safety_mount` | (0, 4, -1) | Top of barrel, toward the front | 1 boiler_reinforcement_flange + 1 pressure_pipe_coupling + 1 refined_steel_rod + 2 iron_rivet |

**All four:** 18 RPM, 10 torque, 320 powered ticks in the existing four-input `mechanical_assembly` recipe. Materials can be produced before Steam (no Steam-dependent circular crafting). Each component has one registered block, BlockItem, creative entry, localization, unique baked 4-facing model, matching static precomputed collision geometry and exact-one-item loot. Visuals reuse existing Magneticraft2 worked iron and vanilla iron/deepslate textures without scaling face UVs. Fittings are visible mounting hardware, **not functioning valves/pipes/firebox**: water, Steam and heat capabilities are deliberately absent; Steam Pipes cannot connect to an outlet that advertises no STEAM capability.

**Installation order:** form boiler first, then install service fittings. At each permitted position the facing is inferred from the real formed controller, not arbitrary clicked side. SOUTH canonical geometry rotates 270° EAST, 180° NORTH, 90° WEST with the same positional mapping. All four fitting positions are OUTSIDE the entire 3×4×5 original blueprint, including its required-air cells. The firebox service hatch bolts to the lower front end ring, rather than occupying an original AIR cell that dismantling would clear. The safety service mount is a prepared valve/gauge pad only; it cannot relieve pressure and may not be mistaken for an installed operating valve.

**Critical acceptance:** do not allow water/Steam/heat flow or pressure until Steam 0-B/C actual persisted mass/thermal ledger, safety, per-face capability and quality/rating decisions exist. Do not invent kPa numbers.

**In-game regression checklist:**
- [ ] Build and form the existing Steel 3×4×5 boiler before attempting attachments.
- [ ] Produce each fitting through existing Mechanical Assembly Station. Check 4-slot consumption, output-blocking, correct 18 RPM/10 torque and tool behavior.
- [ ] Place at permitted water/steam/firebox/safety locations in SOUTH, NORTH, EAST and WEST; ensure visible bolted support and no floating or clipped interfaces from multiple views.
- [ ] Attempt arbitrary ground/air placement and placement on unformed boiler; both must be rejected, not place an unsupported block.
- [ ] Confirm existing water/steam pipes cannot mistakenly transmit water/STEAM through inert mounts. Check no heat, fuel, gauges or pressure simulation occurs.
- [ ] Mine each mount and recover one matching block item. Break/reform the boiler with mounts installed: its 53 original parts must still return exactly once and the fittings remain distinct owned blocks (remove them independently).
- [ ] Save/reload and cross-chunk boundary; verify retained facing, shape, mining and no ghost mounts. Test survival and creative, optionally dedicated server with two clients.
- [ ] Run Steel IV-A/B/C checks and `./gradlew build` on JDK 17. Visually inspect model UV/seams and performance.

**Known limitation:** The mounts are externally located stage-0-A blocks and not part of a Steam-capable operating multiblock yet. Pressurized quality tracking and true port capability are still deferred. The visually accepted boiler shell has not been touched. No successful Java compilation, runtime, or multiplayer tests are asserted in this implementation pass.


**Correctness correction (2026-11-10):** The first firebox proposal placed its block in one of the seven originally required-air positions within the formed multiblock. Existing controller teardown would clear that location and delete the fitting. The firebox mount has therefore been moved to the lower front end-ring, **outside** the full blueprint. EAST and WEST offsets/model rotations now follow the actual directional JSON layouts. The contract checker asserts that every fitting is outside the original block map and backed by a real formed filler for each facing. These source corrections remain unverified by compilation or Minecraft.


## Installation UX correction — 2026-11-10

Player testing reported that the fittings could not be installed. Source investigation found the concrete input blocker: `Multiblockfiller.use` and `SteelBoilerVesselBlock.use` consumed right-clicks with a held BlockItem, so the fitting item handler never reached its strict placement check. The item also gave no in-world guidance for its exact designated external cell.

The four fitting items now use a single focused `BoilerServiceFittingItem extends BlockItem`. **Hold one and right-click ANY existing formed boiler filler or the boiler Controller.** The item's custom use-on resolves the specific boiler controller through `Multiblockfiller_tile.getControllerPos()`, checks formed status, determines the authoritative controller facing, resolves the fitting's exact position, requires a loaded empty target, and invokes vanilla `BlockItem.place(BlockPlaceContext.at(...))` so normal placement validation, protection hooks, player collision, sound and inventory consumption remain applicable. The boiler Controller and shared filler block specifically yield PASS for these fitting items; other held items and other machines retain prior behavior. The fitting block still independently checks correct orientation and real support. Target occupied/unformed generates a player message. Ordinary direct precise placement remains available.

**New acceptance:** with each fitting selected, right-click the FRONT of the formed boiler, then a side filler, then the controller (when reachable); placement must succeed regardless of clicked section, at the correct single target with exactly one item deducted, in all four facings. A second attempt must report target obstructed and not consume an item. Unformed/other machines must not install it. Confirm a protected region still prevents unauthorized placement and no unauthorized chunk loads. The previously accepted drum/preview/recipes remain unchanged. This correction is not independently compiled or tested in Minecraft.


## Visual boiler identity reconstruction (2026-11-10)

The player's actual Steam 0-A screenshot showed an accepted cylindrical pressure vessel with disconnected little posts/fittings: visually a Steam receiver rather than a fired boiler. A new concept established the intended industrial silhouette (firebox with stoking door and ash cleanout, attached exhaust stack, reinforced top Steam takeoff, gauge plus relief body, and flanged feedwater line). **This is an implementation of the concept in conventional Minecraft baked block models, not a claim of pixel-identical reproduction of the concept illustration.**

- Keep the Steel IV-C 3x4x5 controller, boiler shell, static baked V2 parts, foundations and 53-piece formation contract exactly unchanged.
- Rebuild ALL FOUR existing fitting models/voxel shapes using shared box dimensions and non-stretched UV. Preserve block IDs, recipes, facings, inventory and world saves. The water inlet now has an integrated reinforced flange, pipe, hand valve and support foot; top Steam fitting has a larger steam dome and horizontal takeoff; upper safety fitting has a readable white gauge, red needle and copper-colored relief body (INERT); front firebox is a substantial stoking housing with an **unlit** grate, dark combustion chamber and ash drawer.
- Add one actually placeable `boiler_flue_chimney` block, assembled using 1 riveted Iron Casing + 1 Iron Plate Panel + 1 Refined Steel Plate + 4 Iron Rivets. Install as a vertical chimney **directly above** the new firebox at controller-relative SOUTH offset (0,+2,+1), only when the firebox exists in the correct orientation. It is composed of physical hollow iron shaft walls and bolted collars, no fake smoke or combustion. One existing boiler can carry all five fittings. The chimney is external to the original blueprint like the other fittings.
- Installation still works by right-clicking **any** formed boiler part with the held fitting. A player must install FIREBOX before CHIMNEY. The existing Steel vessel teardown does not own or delete these attached pieces; remove them separately. They are independent inert parts, not a new working boiler multiblock.
- No HEAT, Water, STEAM capabilities, combustion, pressure, fire, valve operation, true gauge reading, automatic exhaust animation or idle smoke are introduced. The exhaust pipe must remain non-emitting until actual combustion is modeled. The small front-mounted format is deliberate to avoid changing the accepted boiler body or adding oversized arbitrary geometry across chunks.

**Visual / in-game acceptance:** build on South, North, East, West; install Firebox before Flue; confirm chimney rests flush on the firebox roof and the firebox bolts into the end ring; inspect low water flange, top dome/steam takeoff and gauge/relief assembly from all camera angles, with no floating gaps, UV distortion, hidden faces or misoriented components. Check unobstructed vs obstructed attachment handling, correct one-item consumption, tool drops, reload and dismantling of 53 original vessel parts while fittings remain independent. Run `node tools/steel-age/check-steam0a-boiler-service-fittings.mjs` and `./gradlew build` with JDK17, and an actual dedicated two-client test when available. **No compilation or in-game verification has occurred in this pass.**
