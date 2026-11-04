# Bronze Foundry — structure and casting contract

**Status:** PR #11 has formed structure, Rendering V2, Voxel V2, and a **first functional heating/melting prototype**. Fuel, persistent HEAT energy, bronze charge and conserved crucible bronze are implemented. **Manual ladle pouring and Basin cooling are now implemented**, and the Foundry still requires in-game testing.

## First build

A 3 wide × 2 high × 3 deep JSON multiblock. The bottom-front centre is the controller and tapping/front access side.

```text
Floor (y=0)        Upper layer (y=1)
B B B              B B B
B B B              B _ B
B C B              B _ B

B = minecraft:bricks, C = magneticraft2:bronze_foundry
_ = air, front toward bottom
```

**15 Bricks blocks + 1 Controller + 2 air.** There are four genuinely rotated JSON layouts, a model for each facing, and a physical collision shell that leaves the upper chamber and front opening empty. The furnace now has a **recessed lower firebox**, a stepped, visibly empty refractory crucible above, shorter upper walls with corner buttresses, bronze reinforcement, and a physically joined front tapping channel with a valve. Visual geometry is generated from named source volumes and tiled surface panels. Model coordinates and UVs are clipped into legal block-local bounds without stretching or compression.

Recipe: 5 Bricks blocks, 2 Copper Plates, 1 Furnace, 1 Bronze Ingot make the controller. The remaining 15 Bricks blocks are placed in-world. The pre-existing Copper/Tin/Bronze ingot loop remains intact.

## Longer-term casting and manufacturing contract (milestone history)

1. **Heat and charge.** Foundry melts already-made Bronze Ingots, not Bronze Alloy Charge and not ore. Use the existing Magneticraft HEAT capability/storage; track fuel energy entering a thermal mass, cooling losses and a melt-readiness threshold. Heating must cost fuel and time, and survive chunk reload without duplicating charge. Plan forced-air upgrades around Copper mechanical Bellows. Final temperatures, heat capacity, fuel efficiency and melting speed are not yet approved: make them configurable rather than inventing real temperatures.
2. **Visible states.** Discrete, visible fuel chamber and bronze charge zones. Empty crucible is dark; heated molten metal has a distinct fill level and glow. On stalls the metal remains in one recorded state and never consumes a second ingot accidentally.
3. **Dedicated front tap.** Molten Bronze is a measured, persistent amount, NOT a normal placeable bucket fluid. Begin with a refractory pouring ladle/crucible, then add a Casting Basin receiver. Verify donor/receiver state and capacity before a single atomic transfer. Ordinary water pipes cannot carry molten metal by default.
4. **Casting Basin.** Consumes one of the new Gear/Bearing Green Sand Molds for a valid pour. The *wooden* patterns remain reusable at the separate Mold Table. Reject incorrect molds, quantities and insufficient molten stock without deleting either input. Cool into **rough bronze castings**, not precise finished gears.
5. **Mechanical Lathe.** Gear V2 powers precision finishing of the rough castings; torque/load and tool wear matter. Bronze parts carry greater mechanical loads but never generate extra power. Preserve material-quality metadata through the full path, with actual tier rules undecided.
6. **Safety/wear.** Refractory liner/lid/service parts should be replaceable and visibly worn, not a universal durability bar. Cooling/solidification and safe manual metal handling need explicit acceptance tests.

### Test the structure prototype

- Each of the four Patchouli orientations forms, even if the controller was placed in a different direction.
- Visual front/tap matches the actual controller for every orientation.
- Upper chamber and access opening stay hollow (no invisible whole-cube collision).
- Textures are neither missing nor stretched; corner bracing and spout visibly connect.
- Chunk reload and client multiplayer restore formed geometry, and breaking formed/unformed structure does not leave invisible fillers or dupe blocks.
- Only the Foundry *shell* exists at this stage: it must not silently convert items into castings.

The Foundry heating, manual ladle transfer and first sand-mold Basin cooling path have now been implemented. The next manufacturing milestone is the powered Mechanical Lathe; the earlier steps below remain as the design record.


## Rendering V2 migration (PR #11)

The original oversized Foundry shell was not loadable as one vanilla model; V2 now splits the one canonical south model into 16 controller/filler-local meshes per facing, baked into chunk geometry. Static visuals and local collision survive an unloaded controller chunk. See `docs/multiblock-rendering-v2.md` for the script, limitations and regression checklist. The original V2 model geometry was retained as this milestone evolved; Foundry heating and the manual Casting Basin have subsequently been implemented.

## Foundry visual redesign (PR #11)

The source-of-truth visual volumes are in `tools/multiblock-models/designs/bronze_foundry.json`. Recompile them using `node tools/multiblock-models/build-designs.mjs`, then regenerate the 64 block-local assets with `node tools/multiblock-models/generate.mjs`. Do not edit the generated files directly.

The raised crucible is open and dark when cold, and the tap is presently a static visual (no fuel or molten metal can flow). The large front firebox is hollow, with an inset refractory lining and grate. Upper brickwork forms low retaining walls instead of enclosing the entire second layer; the original 15 Bricks + Controller multiblock layout and crafting requirements have not changed.

The Foundry has a **hand-authored simplified collision outline** that now follows the open firebox and raised hollow crucible rather than its old solid brick bounding box. This was the pre-Voxel V2 outline; an independent voxel profile now supplies controller and filler collision and selection. Verify hitting the firebox, reaching the crucible from above, walking near the spout, all four facings, and dismantling after formation.

## V2 collision anchors for the open crucible/tap

The assembled model extends into the two required-AIR cells of the upper 3×2×3 structure. Before forming they stay AIR and do not change the construction instructions. The JSON `settings.render_air_anchors` now explicitly names those controller-relative locations; they are replaced with non-occluding render/collision proxy blocks **only after formation**. The proxies receive per-block cached collision and model IDs and restore to AIR on dismantle. Re-form Foundries built with older code to install the new anchors. The tap nose projects 7/16 of a block beyond the outer front face. Each V2 required-air proxy can preserve up to half a block of its real, intersected collision shape beyond its local cell, so that short projection can now provide support. This is not a general collision system for large overhangs beyond a structure; the full Voxel V2 design still needs dedicated anchors.

## Voxel V2 milestone

The Foundry's own physical definition has been extracted from its old `Block.box` implementation into the editable `voxels` data in `tools/multiblock-models/designs/bronze_foundry.json`. An exported server datapack JSON carries 22 collision boxes, separate selection geometry, and named regions for the firebox, crucible and tapping valve. Its block-local cached shapes are saved and synced on formation. Existing formed test machines must be rebuilt/reformed once to populate the new snapshots. See `docs/multiblock-voxels-v2.md`. At this earlier milestone neither heating nor pouring was present; both are now in the current implementation.


## First fuel/heat/bronze melt slice

The Foundry uses the existing Magneticraft HEAT storage/capability and Forge solid furnace-fuel burn durations. It does not create RF, Watt or mechanical power. The external HEAT capability is read-only at this stage; physical solid fuel is the heat source. There is no Forge FluidHandler for molten bronze.

- Right-click the **firebox** with a solid furnace fuel to queue one item; an active fuel burns its normal duration. Fuels with a container remainder (e.g. lava buckets) are deliberately rejected until container handling exists.
- Right-click the **crucible** with a Magneticraft Bronze Ingot to queue one charge. Mixed NBT/quality variants are rejected while another variant is queued or already stored in the crucible.
- With empty hands, click either opening for status; **sneak-right-click** to retrieve unburned queued fuel or unmelted queued ingots. The hot crucible does not expose an ordinary fluid/item capability. Bronze exits ONLY via an empty refractory ladle at the tapping zone.
- Fuel adds HEAT energy each server tick; the refractory mass leaks HEAT energy every loaded tick. Only when the threshold and per-ingot energy requirement are met does one solid ingot become one conserved crucible unit. Melting does **not** generate extra bronze.
- If HEAT falls below the separately configurable solidification threshold, the same bronze solidifies in place. Reheating to the higher melting threshold makes it liquid again without inserting or consuming an extra ingot; this phase persists across reloads.
- A named, server-owned crucible batch retains its originating Bronze Ingot's complete item NBT, including planned quality data. Every unit is an **ingot-equivalent**; its conversion to millibuckets is intentionally undecided.
- Breaking the prototype returns the remaining queue contents and crucible material as the original ingots, with NBT, so development tests cannot permanently lose metal. Burning fuel is not refunded. State clears *before* drops are spawned to prevent duplicate restoration.

**Initial balance values are configurable**, not approved real-world temperatures. In Common config: heat capacity 32,000 units; fuel gain 32 HEAT/tick; passive loss 2 HEAT/tick; melt readiness 14,000 HEAT; solidification threshold 7,000 HEAT; melt cost 2,500 HEAT per ingot; capacity 8 ingot-equivalents. These are provisional test defaults to tune after the Basin is gameplay-verified and mechanical bellows are added.

The dynamic Foundry renderer draws the queued fuel and charge as physical items, and one textured 8×8-pixel crucible surface. The surface is dark when solidified and warm when melt-ready. There is no new animated gear or large per-frame rebuild.

**Limitations:** Manual ladle/Basin transfer IS implemented. Material waste/slag, actual degrees Celsius, refractory damage, gas handling and air injection from the Mechanical Bellows are NOT implemented. The Basin outputs rough castings; precision finishing still requires the planned Mechanical Lathe.

### Focused in-game test

1. Rebuild and restart the client, and **dismantle/reform** the Foundry if the latest voxel selection profiles were not installed.
2. Right-click the lower firebox with Coal; see fuel inside. Right-click the crucible with one Bronze Ingot; see the input near the cup.
3. Wait while the server advances fuel burn/HEAT. The ingot must disappear **once** and the crucible amount become 1. No 2× Bronze outcome is allowed.
4. Click the tap; it should report the amount but give no metal, even if other fluid pipes are adjacent.
5. Unload and reload while the fuel is burning: the HEAT, remaining fuel, remaining solid charge, quality NBT and crucible count must persist.
6. Let it cool below the lower threshold: the bronze remains stored and turns visually darker. Add fuel alone to reheat the existing crucible metal (no new ingot required), and verify the phase persists over save/reload.
7. Test Bronze Ingots with distinct NBT tags; they must not mix in a loaded crucible.
8. Break either controller or one of its formed proxies; each remaining item and ingot-equivalent must drop exactly once.
9. Repeat on a dedicated server and all four orientations. Recheck the Foundry voxel/collision and Washer/Sifter regression cases.

No Gradle compile or in-game test has been run in this workspace; these remain acceptance requirements.

## Direct outlet casting (PR #11)

Place the Casting Basin **one block immediately in front of the Foundry's lower
controller**, at the same floor height. The physical bronze tapping nose
extends over this block and points into the Basin. Install a valid Gear or
Bearing Green Sand Mold and melt at least one ingot-equivalent. Right-click the
Foundry tap with an **empty main hand** to transfer one hot bronze unit directly
into the Basin and start its normal cooling cycle.

The tapping visual now uses a thin ribbon recessed between the rails, a
narrow crossed falling stream at the lip, and a receiving molten surface that
expands across the Basin during the 14-tick pour effect. This replaces the
previous thick rectangular orange column. The 14-tick effect is visual, not
a time-based material transfer; the original unit is still transferred exactly
once on the logical server. Visual state is persisted/synced by the existing
block entities.

The tap's short-lived glowing ribbon and downward stream are visual only;
material ownership transfers atomically on the server. Direct pouring requires
a loaded receiver, a valid unused mold, sufficient molten bronze and the exact
physical outlet position. Invalid pours change neither side. Quality NBT is
preserved. A nonempty Refractory Pouring Ladle still follows the original
portable workflow. The Foundry structure recipe and orientation layout are
unchanged.

Regression: verify all four directions, absent/occupied/unloaded Basins,
missing molds, cold metal, multi-unit charges, one-unit conservation,
cooling/reload/dismantling and that manual ladle casting still works.

## Manual bronze casting — first end-to-end loop (PR #11)

The first **functional casting** path is intentionally manual, GUI-less and physically constrained:

1. Use the Pattern Maker to make a reusable Wooden Gear or Bearing Pattern, and the Mold Table to press a **consumable Green Sand Mold** (one Sand and Clay Ball per mold).
2. Build a single-block **Bronze Casting Basin** using Bricks and a Copper Plate. Install one gear or bearing mold into the Basin by right-clicking with it. The Basin shows the inserted mold.
3. Heat Bronze Ingots in the JSON-formed Foundry until at least one stored unit is molten. Right-click the **tapping zone** with an **empty Refractory Pouring Ladle**. One bronze unit is moved into that exact held item, with the source ingot's NBT copied. No ordinary bucket or water pipe can extract molten bronze.
4. Pour the **hot filled ladle** into the Basin. It verifies installed mold, no current pour/output and ladle heat *before* consuming any contents. The player keeps an empty reusable ladle. One mold is consumed with the pour; a single quality-preserving bronze unit begins cooling.
5. After the configured cooling interval, the Basin holds **Rough Bronze Gear Casting** or **Rough Bronze Bearing Casting**, not a finished gear/bearing. Collect using an empty hand; the future Mechanical Lathe will perform precision finishing.

**Cold-ladle safety:** Each ladle holds just one unit. Its server-recorded timestamp + dimension are saved on the ItemStack and it remains pourable for the configurable duration (default 1200 ticks, ~60 seconds at 20 TPS). If it cools, sneak-right-click **in air** to recover the original Bronze Ingot with NBT; this empties the ladle. No molten metal survives indefinitely as a transferable inventory fluid.

**Failure/dismantling:** Reject a wrong or missing mold, a cold ladle, a busy Basin or an empty Foundry without consuming any source materials. Break an unpoured Basin to recover its green sand mold. Breaking during cooling returns the original tagged Bronze Ingot, not an impossible partly-finished item; the poured sand mold has been spent. Breaking after cooling drops the rough casting. Saved cooling state and liquid-phase/quality are persistent. Note that lagging/unloaded chunks pause the Basin cooling ticker; no offline auto-completion is implied.

**Balancing knobs** in Common config: `ladleHotTicks` (default 1200) and `castingCoolTicks` (default 200). These are provisional gameplay values, not physical-world cooling constants. Each green sand gear/bearing mold currently takes **one ingot-equivalent**; later metallurgy and wear can extend that contract rather than assuming every output costs a magic recipe.

**Rendering:** The ladle has empty/filled item models. The Basin renders an actual green sand mold, glowing bronze while cooling and a rough output item when ready. The Foundry's corrected renderer puts fuel behind the grate, uses upright crossed fire sprites, centers the solid charge and shows a near-wall liquid surface with four vertical sides. These are render changes only and do not alter the base multiblock layout.

### Casting regression checks

- Craft both new items and Basin; verify the recipes appear in JEI's ordinary crafting categories.
- Load/melt one ingot, fill an empty ladle from the Foundry's tapping zone; the crucible count must drop exactly 1.
- Attempt to refill a filled ladle, and attempt pouring into a Basin without a mold or one already cooling; check that ladle metal is not lost.
- Insert each mold type and pour once; check the correct rough item after cooling. The wooden pattern must remain usable at its own bench.
- Carry a filled ladle until it is cold; Basin rejects it. Sneak-use in air to return exactly one original tagged ingot.
- Test quality variants. A tagged ingot must emerge from the Basin as a single rough casting with identical source tags.
- Break Basin before pour, mid-cooling and after output; confirm exactly one appropriate recovery path and no duplicated metal.
- Save/reload Foundry, filled ladle and cooling Basin; verify material ownership stays in the correct single container.
- Try all four Foundry orientations and both Basin orientations, and check that static/cached Foundry geometry and Voxel V2 collision remain unaffected.

**Not implemented yet:** powered lathe finishing, refractory wear, automated ladle transport or a true fluid-safe industrial metal transport network. Manual casting is the intended Bronze Age entry point.
