# Steel IV-C — Boiler vessel assembly design contract

**Magneticraft2 · Forge 1.20.1 · 2026-11-09**  
**Branch:** `feature/steel-age-foundations` · PR #19 (draft).  
**Stage:** source-implemented inert JSON multiblock vessel; Forge compilation, in-game formation, visuals and conservation are NOT verified.

## 1. Why IV-C exists and what was accepted

The player has now produced all three Steel IV-B workshop outputs in Minecraft: Boiler End Plate, Boiler Reinforcement Flange and Pressure-Rated Pipe Coupling. Steel IV-A's Heavy Steel Plate -> Boiler Shell Section route was also player-reported working. This verifies the basic production paths, **not** tooling restrictions, provenance, wear, save/reload, or a full Forge build.

The player mistook Heavy Steel Plates for ordinary Refined Steel Plates because the placeholder item models look alike. MGC2 Studio must give Refined Steel Plate, Heavy Steel Plate, curved Shell Section, End Plate, Flange, Coupling, Boring Bit and hot/cold stock distinct readable inventory silhouettes; proper visual identity is a gameplay acceptance criterion, not optional cosmetic work. Do not stretch textures or substitute renderer offsets for real geometry.

**IV-C objective:** establish how individually manufactured Steel pieces become a coherent, physically buildable boiler *vessel* in the world. Steel IV-C makes construction possible; it does **not** boil water, generate Steam, create pressure, consume fuel, or drive an engine. Live boiler operation belongs to Steam Age.

## 2. Repository evidence and constraints

- The Steel IV-B products `boiler_shell_section`, `boiler_end_plate`, `boiler_reinforcement_flange`, and `pressure_pipe_coupling` are **items**, not currently placeable structural boiler blocks. A conventional JSON `multiblocks/*.json` layout matches world blocks; simply naming one of these item IDs inside a layout does not place a real block or consume the corresponding items.
- Existing `bronze_mechanical_assembly_station` is a real 5x3x5 formed machine with a `mechanical_assembly` multi-input recipe type, four input slots and a true output. Reuse it where assembly actually needs several distinct manufactured items. Do not rewrite single-input `multiblock_processing` or add a duplicate assembly machine.
- Steel furnace structures use the existing directional JSON multiblock loader, physical port modules, optional voxel profiles and formed-model conventions. A boiler proposal must provide four directionally correct structures, selection/collision, realistic ports and a matching Patchouli visualize page.
- The STEAM medium, `SteamPipeBlock`, `SteamNetworkManager`, typed `IPressurizedGasHandler`, and sparse conserved steam/rupture foundation already exist. Boiler operation must later attach to them; no second STEAM graph, new pressure quantity, AIR substitution, or per-pipe block entity.
- Current pressure equations, pipe limits and safety setpoints remain provisional. Do **not** encode supposedly final ratings during this structural milestone.

## 3. Proposed vessel design — subject to actual model and placement proof

**Concept:** a horizontal, cylindrical riveted/bolted boiler drum on a believable iron/steel support cradle. The drum has a separately distinguishable curved shell, one machined end closure at either end, circumferential reinforcement flanges at the closure joints, and explicitly visible and supported pipe couplings. No floating gearbox, decorative-only valve, unexplained exposed hollow cube or arbitrary texture stretching.

A **3 wide x 3 high drum cross-section** with a **3–5 block axial length** is a layout study starting point, *not* an authorized final blueprint. The actual JSON cell count, core air cavities, end shape, mounting clearance and exact material counts must be fixed **after** auditing what the existing item models represent (one section, quadrant, ring or entire course) and testing the chosen native-UV 3D geometry. Avoid making 24 four-ingot shells mandatory merely because 24 outer cells appear in a cube grid.

The first Steam plant should use one standardized vessel rather than introducing multiple boiler sizes before the basic pressure and heat balance works. The physical **water connection should be low**, Steam takeoff **high**, and a distinct **safety/pressure-service location** must be reachable from outside. These positions are geometric reservations, not functioning ports in IV-C. There must be a supported external firebox/heat interface in the future Steam implementation, not heat mysteriously entering a sealed vessel from air.

### Structural representation decision gate

Before writing formation JSON, choose one of the following based on the existing block registry and assembly contract:

1. **Preferred:** introduce the minimum placeable *structural module block(s)* made from the already fabricated Steel IV-A/B items via the existing Mechanical Assembly Station or a justified manual construction recipe. Each module must have a documented input/output material ledger and an unmistakable visual purpose. Then form the vessel with real module blocks through existing JSON multiblock formation.
2. **Alternative only if existing infrastructure supports it:** attach real inventory-based part installation to a separately placeable structural frame **without** bypassing block matching, duplication/drop accounting, or saved-world compatibility. Document the actual code path and prove it before use.

Do not silently treat an inventory item as a block; do not replace the parts with an unexplained single shapeless full-boiler crafting recipe; do not spawn a visually complete vessel from an empty controller.

### Preliminary interfaces

| Position / feature | Steel IV-C role | Deferred Steam Age role |
| --- | --- | --- |
| Two axial end closures | Real, separately recognizable caps and supported flanges | Pressure boundary and accessible servicing |
| Curved shell and cradle | Physical formed structure, accurate collision and selection | Pressure/heat boundary, measured vessel capacity |
| Low water connection | Reserved connection location; no fluid movement yet | Actual liquid-water port, conserved intake |
| High steam takeoff | Reserved connection location; no STEAM capability yet | Typed STEAM output into existing steam pipes |
| Pressure/safety service pad | Accessible attachment geometry, not fake functioning equipment | Gauge and spring relief, setpoints and failure response |
| Firebox/heat interface | Reserved externally supported access | Real fuel/HEAT input and thermal model |

**No fake functionality:** IV-C status may say `vessel formed; Steam systems not installed`. It must not display a fabricated kPa value, silently accept water/steam, advertise working safety valves, or produce energy.

## 4. Material/accounting and progression invariants

- A Shell Section is made from **one Heavy Steel Plate**, itself **four Steel Bars / four ingot-equivalents**. An End Plate likewise represents **four ingot-equivalents**, a Reinforcement Flange **two**, and a Coupling **one**. Count these before choosing module recipes, and document exactly where every required unit goes.
- Do not assume a vanilla shaped recipe propagates input quality/provenance NBT. If assembled structure quality matters, use an explicit server-authoritative transfer/aggregation rule, with real persistence and save compatibility, or state clearly that the property is currently not retained and keep critical quality-sensitive operations gated.
- Assembly/reassembly, full outputs, part breakage, unforming, save/reload and chunk boundary changes must not create or erase stock. Define whether the completed vessel yields modules or constituent parts; do not allow *both* to drop for the same consumed materials.
- All necessary Steel IV assembly remains available using pre-Steam workshop power (16 maximum water-wheel source torque) and real existing machinery. A Steel shaft or gear increases transmission capacity but never generates energy.
- Existing steel/copper/iron machine recipes, controller behavior, registrations, item IDs, JEI and Patchouli content must remain compatible.

## 5. IV-C implementation sequence and test gates

1. **Audit** the existing Assembly Station multi-ingredient matcher/slots, current boiler item representations, multiblock structure loader, block registry, controller formation, model/voxel generator and real item-port ownership. Record which abstraction is being reused and why.
2. **Resolve module mapping**: a drawn cross-section, justified exact block dimensions, part counts per module/vessel, ingredient-to-placeable-block recipes, orientation and end-cap/flange placement. MGC2 Studio should deliver silhouettes that can be distinguished at normal Minecraft inventory scale and physically supported in the world.
3. **Implement only approved structure**: minimum registrations and recipes; four directional JSON blueprints, construction instructions and real geometry/collision. No simulated boiler processing or new pressure mechanics in IV-C.
4. **Validate**: run all Steel IV-A/B contract checks, `./gradlew build` on JDK 17, and appropriate JSON/recipe/structure contract checks. No source-only pass is a compilation or runtime pass.
5. **In-game regression checklist:** craft and place each structural module using actual steel stock; form from all four horizontal facings; confirm missing/wrong component rejects; inspect shell/end-plate/flange/coupling geometry and port access from multiple camera angles; verify formation/unformation, block breaking and drop conservation; save/reload and cross chunks; check creative/survival JEI/Patchouli parity, existing Steel IV Press/Boring recipes, and Steel I–III machinery. Measure performance if formed geometry is large.

### Completion boundary

**IV-C design accepted** means the exact geometry, module counts, construction route and item/quality accounting are agreed and documented.  
**IV-C implemented** means real placeable assembly and a JSON-formed vessel exist.  
**IV-C verified** requires build and actual Minecraft formation/conservation/regression tests.  
**IV-C complete** additionally requires accepted collision, textures, UVs, instructions and maintainability. **None of these implementation/verification/completion gates is claimed by this design document.**

Steam 0 later separately requires water/fuel/heat conservation, typed steam delivery, thermal/pressure calibration, protection, persistence and mechanical engine load verification. Keep PR #19 draft and unmerged.

## 6. First implementation slice — placeable panel and end joint (2026-11-09)

**Source implemented, NOT built or Minecraft tested.** Rather than retyping the original Steel IV-A/B inventory outputs as BlockItems (which would alter existing item behavior and risk losing quality NBT on placement), two **new** structurally distinct, inert world blocks are assembled in the already existing Bronze Mechanical Assembly Station:

| Result | Four actual Assembly Station input slots | Processing |
| --- | --- | --- |
| `boiler_shell_panel` (placeable) | 1 `boiler_shell_section`, 1 `refined_steel_rod`, 4 `iron_rivet`, 1 `riveted_iron_frame` | 18 RPM, 10 torque, 320 powered ticks |
| `boiler_end_joint` (placeable) | 1 `boiler_end_plate`, 1 `boiler_reinforcement_flange`, 1 `pressure_pipe_coupling`, 4 `iron_rivet` | 18 RPM, 10 torque, 320 powered ticks |

These are **physical subassemblies**, not a shapeless whole-boiler crafting recipe. Their existing sources and Steel process recipes are unchanged; the Assembly Station's existing four-slot ingredient count, mechanical load, blocking and output reservation are reused. The controller remains independent of Steam functionality.

The block states are horizontal-facing and have actual stepped steel shape/selection/collision with proportional UV per face. Native dark worked-iron and bronze-machined material textures are reused with **no stretched faces**. These small baked geometry candidates require MGC2 Studio's in-game model inspection; a recognizable shape is not proof of mechanical realism. Creative entries, translations, pickaxe tag, single-item loot, and Patchouli instructions are added.

**Important quality restriction:** the Assembly Station recipe currently outputs a new untagged BlockItem and does not aggregate Steel input provenance/quality. The placed structural block stores no quality metadata. It is therefore *not authorized as a pressure-bearing functional boiler part* until the quality/pressure interface and actual formed controller are implemented. This is a deliberate bounded limitation, not a claim that quality is preserved across assembly.

**Next structural gate, NOT implemented:** choose exact number/layout of shell panels, end joints and supporting blocks after inspecting actual geometry; design the correct 3D cross-section and all four JSON orientations, real controller/filler lifecycle, material return on breaking/unforming, native V2 collision and final visual model. Do not invent a layout solely to force a full IV-C label onto these first two working-item candidates. Existing Steel I–IV and Assembly Station regression must pass before taking the next step.


## 7. Steel IV-C formed drum implementation candidate — 2026-11-09

A horizontal **3 wide x 4 high x 5 long** vessel has been source-implemented using the unchanged JSON structure loader, controller/filler persistence, once-baked filler models and Voxel V2. All four directional structures and a common SOUTH-authored voxel profile are included. The controller is at the lower front center. One row of bottom foundation rail supports either side. Three central vessel courses are **hollow** (air), never hidden full-block collision. This is a static vessel, not a functioning boiler.

Exact survival build: **24** `boiler_shell_panel`, **2** `boiler_end_joint`, **16** existing `iron_plate_panel`, **10** `iron_machine_foundation`, and **1** `steel_boiler_vessel` controller. The shell items alone account for 96 ingot-equivalents of Steel (plus rods, iron rivets and frames); the two closures contain 14 Steel ingot-equivalents. This is deliberate substantial pressure plant fabrication, though game balance is pending review. The existing four-slot Assembly Station prepares the two structural items; no new standalone assembly machine.

The outer profile is an intentionally **stepped octagonal drum** rather than square 3×3 masonry. Baked static parts use proportional native UVs; the same geometry supplies per-block collision and selection. MGC2 Studio remains responsible for further artwork quality and end-ring joint inspection. There are NO decorative Steam ports or fabricated gauges. The occupied end-ring blocks are genuine Iron Plate Panels. Steam connections, valve/gauge positions, pressure and conservation remain deferred to Steam Age; assembled BlockItems still lose material provenance NBT and are explicitly unqualified for live pressure use.

**Unverified:** Forge build, asset baking/resource reload, correct rotation in all four directions, shell seams and collisions in game, formation, end-plate joints, save/reload, break/unform returns, part conservation, performance and multiplayer. Run the IV-C structural checker and `./gradlew build` under JDK17; carry out the in-world checklist before calling this a verified vessel.


## 8. Visual reconstruction after player screenshots (2026-11-09)

The player's in-game images demonstrated two **rejected** results: Patchouli showed a set of tall flat panels, and formed geometry exposed a disconnected square closure, rock/wood-like ring and visibly discontinuous shell. These screenshots are runtime evidence, not signs that the model is finished.

A focused replacement retains the registered vessel controller, its **3 × 4 × 5 JSON construction footprint** and the fixed 24 Shell Panel / 2 End Joint / 16 Iron Plate Panel / 10 Foundation bill of materials. It changes only component/formed visual-state geometry, the corresponding Voxel V2 collisions, item models and Patchouli construction display. The formed shell is generated from a **single pixel-stepped radial 48 × 48 cross-section**, hollow through the three middle axial courses and terminated by solid machined end discs with raised steel hubs. Foundation rail saddles physically touch the outer drum wall. Old large flat sheet cuboids and the bronze-colored/wood-like closure are no longer the formed vessel visual structure.

The **unformed** `boiler_shell_panel` and `boiler_end_joint` blocks now have a `segment=0..9` integer blockstate. Sections 0–7 correspond to the eight circumferential ring positions, 8/9 to front/back axial closures. No new craftable items are introduced. Their visual/selection models are once-baked and native-UV; their volume collision uses static initialization and not per-frame geometry. The existing JSON multiblock matcher still compares registered block IDs, not the preview-only per-part segment state. The position-specific states exist to make the Patchouli schematic display actual curved blocks rather than a repeating upright plate.

**Construction UX / compatibility:** Place the vessel Controller **first**. When a Shell Panel or End Joint is placed within its 3×4×5 region, its blockstate auto-selects the position-specific segment and facing from the nearby controller. In older test worlds, already placed pre-change pieces may need replacing to update their newly introduced segment states. Patchouli explicitly maps the correct states; its air cells now use the actual Patchouli space character rather than the wildcard underscore. Iron Plate Panels at both ends remain genuine Iron Plate Panel blocks in the construction preview rather than fabricated steel-looking replacements, so their unformed layout remains less visually smooth than the finished vessel. This is a remaining book-preview limitation and must be judged in-game, not called fully resolved by static checks.

**Regression script:** `node tools/steel-age/check-steel-boiler-vessel-visuals.mjs` checks the four baked facing variants, non-overlapping cuboids, proportional UV, registered part icons, book-vs-JSON symbols, and equality of formed part geometry with the authoritative collision profile. This is static verification only. It is NOT evidence of successful JDK17 compilation, Patchouli model baking, in-world formation, visibility, frame rate or saved-world teardown.

**In-game acceptance:** Verify the Controller-first Patchouli Visualize, the shell/end segment auto-selection, complete formed vessel in all four facings (especially front/back end-disc continuity), no floating supports or holes, no stretched/compressed textures, physical targeting/collision, item drops/unforming exactly once, save/reload and chunk boundary behavior; rerun the old IV-A/IV-B and assembly recipes. Steam, HEAT, water and pressure remain entirely deferred.


## 9. Patchouli visual axis correction (2026-11-10)

Player screenshots showed the rebuilt **formed** vessel looked good, while the Patchouli book still displayed spiky, misplaced panels. Root cause was the **Patchouli pattern array's axis ordering**, not the accepted formed geometry. The previous book serialized each layer as `[Z rows][X characters]`, effectively drawing the 3-wide by 5-long boiler as 5-wide by 3-long and placing curved segment roles on the wrong cells. Patchouli specifies `pattern[Y top-down][X west-east][Z north-south]`. Corrected the four arrays to three five-character rows per layer, maintaining the same 24 shell panels, 16 iron panels, two end joints, 10 foundations, controller and hollow air cells. End-cap letter K denotes back (z=0), J denotes front (z=4). No world JSON, recipes, renderer or formed geometry changed.

Both Steel IV-C structural and visual regression scripts now test the **actual Patchouli axis convention** and the exact per-cell component role, rather than repeating the old incorrect transposition. This does not establish in-game visual acceptance; open Patchouli again and verify the drum is longitudinal, not a forest of standing vertical plates. The in-world construction outline should still be player-tested for facing, air cells and formation.
