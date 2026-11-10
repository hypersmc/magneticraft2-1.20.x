# Steam 0-A V2: real front firebox assembly

**2026-11-10 | Magneticraft2 | Forge 1.20.1**

**Status: implementation specification, not implemented gameplay.** This pass adds documentation, a design manifest and an executable coordinate/ledger check only. It does not change Java, models, recipes, world saves or the accepted boiler. PR #19 remains draft and unmerged.

Baseline inspected: `e8a6664a568ddb52cd3fd28db40fa9901d06bf3a`, branch `feature/steel-age-foundations`.

## 1. Decision and scope

Keep the accepted 3-wide x 4-high x 5-long Steel Boiler Vessel and all 53 original structural blocks. Replace the *visual role* of the one-block front fitting with a real **3-wide x 3-high x 2-deep firebox**, plus a three-block chimney. Each additional occupied block position requires a paid-for, placed block; one item must not generate a large structure.

The existing Firebox Mount becomes the rear heat-interface component of the new assembly, not its whole visible furnace. Existing water, Steam and safety fittings retain their mounting addresses and item IDs. The existing Flue Chimney item is reused for three real stack sections. Only two new craftable block IDs are proposed: a refractory-lined casing and a stoking-door/front controller.

This is construction and visual architecture only. No combustion, pressure, Steam production, working safety device or fabricated gauge reading is enabled. The approved illustration is a silhouette/material reference, not proof that a Minecraft model works.

## 2. Source-grounded constraints

The inspected implementation has one fixed external cell per fitting in `BoilerServiceFittingBlock`, bounded cuboid geometry and no per-fitting block entity. A one-cell firebox cannot provide the volume shown in the approved reference. `SteelBoilerVessel` still has the original 53-block construction and seven reserved air cells.

The existing `MultiblockController` validates rectangular layouts, replaces member blocks with fillers, persists original states and destructively returns the original block items on teardown. It assumes its controller is inside its own layout. Therefore the new firebox needs its **own small controller and its own original-part ledger**, linked to the existing vessel. Do not run a second structure over the original drum or include the drum's fillers in the firebox ledger.

`IronServiceHatchBlock` already supplies a manual iron hatch. Reuse it as a manufacturing ingredient and visual reference, rather than inventing an unrelated door technology. The existing four-input Mechanical Assembly Station remains the production machine.

Relevant baseline files:
- `src/main/java/com/magneticraft2/common/block/stage/steam/BoilerServiceFittingBlock.java`
- `src/main/java/com/magneticraft2/common/item/stage/steam/BoilerServiceFittingItem.java`
- `src/main/java/com/magneticraft2/common/systems/Multiblocking/core/MultiblockController.java`
- `src/main/java/com/magneticraft2/common/block/stage/iron/IronServiceHatchBlock.java`
- `src/main/resources/data/magneticraft2/multiblocks/steel_boiler_vessel_south.json`

## 3. Exact coordinates

The existing vessel controller is `(0,0,0)`. The canonical vessel faces SOUTH. `+X` is world east, `+Y` is up, `+Z` is outward through the front. The drum extends backward in negative Z. These are controller-relative coordinates, not player-facing left/right instructions.

| Group | Position/range |
| --- | --- |
| Unchanged vessel bounding grid | X=-1..1, Y=0..3, Z=-4..0 |
| New firebox | X=-1..1, Y=0..2, Z=1..2 |
| Rear heat interface: existing Firebox Mount | (0,1,1) |
| New stoking-door/controller | (0,1,2) |
| Chimney sections, bottom to top | (-1,3,1), (-1,4,1), (-1,5,1) |
| Existing water inlet | (2,2,-2) |
| Existing Steam dome/outlet | (0,4,-2) |
| Existing safety/instrument fitting | (0,4,-1) |
| Clear approach in front of the door | (0,1,3), (0,2,3) |

The vessel remains 3x4x5. The firebox extends it two blocks forward. The complete installation, including the side fitting and chimney, occupies an envelope of **4 wide x 6 high x 7 long**; this is not a new filled rectangular multiblock. Leave an additional access aisle in front.

Rotate an offset `(x,y,z)` around the original controller:

```text
SOUTH: ( x, y,  z)
WEST:  (-z, y,  x)
NORTH: (-x, y, -z)
EAST:  ( z, y, -x)
```

Use this transform once for blueprints, part roles, physical contacts, placement and tests. Generate the four directional resources from the canonical source; do not hand-maintain a second inverse convention.

## 4. Exact firebox layers and material ledger

For each layer below, rows run from the vessel outward (Z=1, then Z=2). Characters run west to east (X=-1,0,1). This is explanatory grid notation, NOT Patchouli's array order.

```text
Y=2: roof              Y=1: furnace             Y=0: base
L L L                  L I L                   F F F
L L L                  L D L                   F F F
```

`F` = existing Iron Machine Foundation. `L` = new Boiler Firebox Lining block. `I` = existing Boiler Firebox Mount, retained at its current position. `D` = new Boiler Firebox Front, containing the visible stoking door and the firebox's non-operational controller.

| Component | Count | Treatment |
| --- | ---: | --- |
| Original vessel parts | 53 | Unchanged |
| Iron Machine Foundation | 6 | Existing block, additional front base |
| Boiler Firebox Lining | 10 | New refractory-lined structural block |
| Boiler Firebox Mount | 1 | Reuse existing item/recipe as rear interface |
| Boiler Firebox Front | 1 | New door/controller |
| Boiler Flue Chimney | 3 | Existing item, three paid-for stack cells |
| Water inlet, Steam outlet, safety assembly | 3 | Existing fittings, one each |
| **Total physical blocks** | **77** | **53 original + 24 external** |

All seven original vessel air cells remain unoccupied. There are no new required full-air cells in the firebox grid; its internal chamber and duct openings are real empty portions of the formed component shapes, not concealed solid collision. The roof includes the chimney collector at its rear-west corner.

Proposed new four-slot assembly recipes (balance candidates, not yet registered):

| Output | Slot 1 | Slot 2 | Slot 3 | Slot 4 |
| --- | --- | --- | --- | --- |
| 1 Boiler Firebox Lining | 1 Riveted Iron Casing | 2 Fired Refractory Ceramics | 2 Refined Steel Plates | 4 Iron Rivets |
| 1 Boiler Firebox Front | 1 Iron Service Hatch | 1 Iron Plate Panel | 1 Refined Steel Rod | 2 Iron Rivets |

Both use the existing Mechanical Assembly Station: 18 RPM, 10 torque, 320 powered ticks. Existing mount and chimney recipes stay unchanged. No new Steam-powered prerequisite. Pressure-bearing material quality remains a separate unresolved operating gate.

## 5. Model specification and heat-path identity

The front extension must read as a furnace before details are added. It occupies most of its three-block-wide footprint, has a dark refractory-lined body, a central framed stoking door, and an ash-cleanout recess beneath it. The outer casing continues across neighboring cells; it must not look like 18 unrelated decorative cubes.

The ash-cleanout recess is part of the formed base/front geometry; it does not claim an implemented ash inventory or opening animation. The chimney is a visible hollow three-cell stack on the roof, not a particle plume or one tiny pipe on the front door. Each chimney section uses its own real local collision and selection shape.

Conceptual later hot-gas routing is: stoking chamber -> sealed heat-exchange passage at the rear interface -> return-flue collector -> roof chimney. Combustion exhaust is separate from the water/Steam space and from the top Steam takeoff. A chimney directly bypassing the heated interface is not an acceptable shortcut. This routing is a game-machine design proposal, not a validated real-world boiler design or implemented thermal simulation.

Keep the accepted drum contour. Rework the existing two top fittings as a broad steam-dome base and a neighboring instrument/relief mounting deck, with compatible flanges and a clear shared visual composition. Each remains independently paid for. Do not draw an operating gauge needle or safety-state indication before those systems exist; use an explicitly uncommissioned/blank indication.

Retain the side inlet address for save compatibility. Improve its shell saddle and lower-half connection within that cell; remove any decorative support foot that ends in empty air. There is a crucial difference between adjacent block coordinates and actual model contact: measure the existing shell surface and author a visible mating collar where necessary. A collar entering the existing shell cell must be a correctly owned, clipped V2 part variant with matching collision, not hidden geometry, a stretched face or an arbitrary item offset. The base drum itself is not rescaled.

Author each assembly in one canonical coordinate system, split it into owned local parts, and derive model and voxel outputs together. Runtime cell-local geometry must stay within that cell. Native pixel density, readable inventory silhouettes and honest bounds take precedence over a photorealistic copy of the reference. No new entity renderer or per-frame model construction.

## 6. Construction and interaction

1. Form the existing vessel normally. Existing saves must still load and function as inert vessels.
2. Keep or install the existing Firebox Mount at `(0,1,1)` using its established boiler-click interaction.
3. Place the six foundation blocks, ten lined casing blocks and the new front/controller manually using a separate Patchouli firebox guide. The guide highlights real materials, not already-formed filler blocks. No one-item whole-firebox spawn.
4. Right-click the new front/controller. Validate all 18 cells, the exact link to the formed vessel, matching orientation and loaded footprint before replacing anything. Form only the new extension.
5. Install the chimney using three individual items. Clicking the linked firebox or vessel with a chimney section installs the next empty section, bottom-up, and consumes one item. The original fixed-address chimney rule must be replaced for V2; do not attempt to stack all three at the old address.
6. Retain the existing water, Steam and safety fitting installation. A click on a formed extension filler resolves its firebox controller, then its validated vessel link. It must not be rejected merely because the immediate filler controller is not a Steel Boiler Vessel.

Messages must distinguish missing materials, unformed/wrong vessel, blocked target, missing lower chimney section, unloaded target and full stack. Failure consumes nothing and mutates nothing. Placement permissions and reach/protection checks apply to the actual destination, not only the clicked block; do not assume `BlockItem.place` alone proves every protection integration.

Keep the existing vessel Visualize page. Add a firebox construction page and an installation-order page. Generate their patterns from the same coordinate manifest, explicitly converting to Patchouli's top-down Y/X/Z ordering. Use JEI or the established machine-recipe presentation for mechanical assembly; do not present these as vanilla crafting-table recipes.

## 7. Ownership, teardown and persistence

There are two non-overlapping structural owners. The existing vessel owns its unchanged 53 original blocks and seven air checks. The new front/controller owns the 18 original firebox block states, its vessel link and its three chimney positions. Water/Steam/safety remain vessel attachments. The new firebox must not keep a second copy of the drum's inventory or pressure state.

Reuse the existing JSON loader, original-state persistence, V2 fillers and baked-part assignment. Do not independently invent another multiblock engine. Add the boiler-specific coordination around those existing mechanisms; the generic destructive controller currently is not an atomic linked-assembly transaction.

Required survival behavior with suitable tools:

| Action on a fully installed V2 plant | Return/remain contract |
| --- | --- |
| Break firebox wall or its front/controller | Return 18 firebox parts + 3 stack parts once; original vessel remains formed; three other fittings remain |
| Break top chimney section | Return 1; two lower sections remain |
| Break bottom chimney section | Return that section and the two above; no floating stack |
| Break an original vessel part | Coordinated teardown returns all 77 block items once |
| Repeat callback or save/reload around teardown | No second return from an already consumed owner ledger |
| Break in creative | Remove the affected group without creating survival duplicate drops |

Before destructive changes, claim a server-side dismantling guard and snapshot which owner returns each item. Suppress the vanilla destroyed-cell drop when the same cell is being returned by assembly teardown. Do not manually spawn its item and also allow normal loot to spawn it. One coherent update must include the original front/controller itself.

For player-initiated work, validate the linked loaded footprint before starting. Reject/defer the operation if it cannot be completed without loading chunks. For unavoidable removals such as explosions, persist narrowly scoped pending cleanup keyed by assembly identity and exact member positions; unloaded members are reconciled when loaded, without touching unrelated replacement blocks. This requires runtime tests and is not solved by `hasChunkAt(...); continue`, which leaves orphans. No global ticking scan or forced chunk load.

Persist an assembly schema version, owner identity, facing, vessel/controller link, original block-state ledger, formed/dismantling status and unresolved cleanup. Scope new ownership metadata to V2 assemblies and maintain old saves without requiring all existing fillers to migrate.

## 8. Compatibility and implementation boundary

Do not remove or repurpose existing registry IDs. The already crafted Firebox Mount is reused, not discarded. Existing chimney items become stack sections; recover the old chimney and install it in the new stack. The old chimney cell `(0,2,1)` lies in the proposed V2 roof, so it must be mined first. Formation must report this conflict and must never overwrite/delete it. Two additional chimney items are required.

The three other fittings keep their current coordinates. No automatic relocation of user blocks on update. A V1 vessel with its old small fittings remains an inert legacy installation until the user builds V2. Only a verified V2 installation can later qualify for operational boiler code.

Expected focused implementation:
- New `boiler_firebox_lining` ordinary block and `BoilerFireboxFrontBlock`/`BoilerFireboxBlockEntity` under the existing Steam packages. The front/controller is at `(0,1,2)`; the old mount remains a normal component.
- Four `boiler_firebox_<facing>.json` structures, common voxel definition, cell-local baked models, two assembly recipes, loot/tags/names/JEI/book entries.
- Extend fitting installation and boiler-specific teardown to recognize the new controller link, relocate the V2 chimney rule and preserve V1 handling.
- Use the existing generic V2 part naming/assignment path where possible. Implement required base-block and base-entity abstract methods before presenting the change for testing.
- No pressure algorithm, water tank, fuel tick or Steam engine in this pass.

## 9. Verification gates

The accompanying design checker was executed locally with Node. It verifies the proposed 77-block ledger, all four coordinate rotations, absence of overlap with the entire old vessel grid including air, connected extension/stack cells, support-cell adjacency, access cells and the explicit old-chimney migration conflict. It does **not** verify model surface contact, Forge compilation, block placement or drops.

Implementation must first produce a compile-tested, plain structural prototype at the specified size. Inspect its front, side and three-quarter silhouette beside the accepted drum before adding detailed gauges, rivets or new mechanics. Then test real placement, model/collision parity, all orientations, negative formation, protected/obstructed placement, save/reload, cross-chunk behavior, exact-once teardown in survival/creative, and dedicated two-client consistency. Record actual results rather than merely adding source-string assertions.

Run `./gradlew build --no-daemon` with JDK 17 when source implementation exists. No build was run for this design-only change. Local network access to raw.githubusercontent.com failed DNS resolution during this session; GitHub connector reads/writes worked. This limitation is not evidence of compilation failure or success.

**Completion boundary:** the specification and grid check are complete; the V2 firebox itself is not implemented or visually verified. The next code task is the real 3x3x2 front structure, not another cosmetic pass on the one-cell mount.
