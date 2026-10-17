# Bronze Stamp Mill — implementation contract

Status: **Bronze II prototype on PR #11**, pending Gradle build and in-game verification. See [Bronze industrial plan](bronze-age-production-plan.md) for progression.

## Purpose

2 Chalcocite Ore or 2 Cassiterite Ore -> 2 corresponding crushed ores (115 ticks; 24 RPM minimum; torque 6). Each batch independently has 20% chance of 1 Gravel tailing. Existing Grinder remains a low-power fallback; ore still proceeds to the existing Washer and Sifter.

## Full-size module integration: 5 x 3 x 4

**All I/O modules occupy complete Minecraft blocks and retain their original, reusable wooden/copper item-port models.** None are fillers or decorative geometry. To avoid floating hoppers and keep item access clear, all three ports now sit side-by-side at **ground level on an open front service shelf**, one block south of the controller. Each has a side/front face exposed to the world for players and transport automation.

The former 5x3x3 draft with three ports in the raised front wall is obsolete. A second draft buried the ports inside the stone-brick foundation; that was rejected because they could not be accessed. The authoritative layout is now 5 wide, 4 deep, 3 tall (rotated to 4 wide, 5 deep, 3 tall for east/west). The core Stamp Mill frame retains its original 5x3x3 section and adds a service shelf with a matching bronze/timber canopy and **three physical chutes** linking the crushing trough to the hoppers.

South blueprint, each row is 5 blocks west-to-east, rows appear rear-to-front, and layers are bottom-up:

```text
Y=0   S S I S S    S S S S S    S S C S S    S U Y O S
Y=1   P P P P P    B _ _ _ B    B _ _ _ B    B _ _ _ B
Y=2   B P P P B    B _ _ _ B    B P P P B    B P P P B
```

S = Stone Bricks; P = Oak Planks; B = Cut Copper; C = Stamp Mill Controller; I = Mechanical Input Module; U = Item Input; Y = Byproduct Output; O = Primary Output; _ = AIR. Build by Patchouli **Visualize**, which stores its dimensions transposed relative to the raw JSON. Do not copy JSON Z rows directly into Patchouli's X columns.

Blueprints: `src/main/resources/data/magneticraft2/multiblocks/bronze_stamp_mill_{south,west,north,east}.json`

### Rendering V2 and physical-port rules

- Authoring: `tools/multiblock-models/designs/bronze_stamp_mill.json`; canonical occlusion-culled source: `tools/multiblock-models/sources/bronze_stamp_mill.json`.
- Each facing now owns **57 render hosts**, including 9 explicitly selected AIR anchors that become V2 filler proxies on formation. The three item modules, Gear V2 module and the actual controller are NOT swapped for artificial port meshes; module render parts must remain EMPTY.
- Every module's complete local AABB is reserved for that module. Neither static V2 boxes nor local Voxel V2 collision/selection may overlap any of the four module spaces. The regression test asserts this for the model and both voxel profiles.
- Ground-level port models stay visible. Raised structural columns, front canopy and metal transport chutes sit around and ABOVE them; none use the port cell as an attachment/render host. No globally modified I/O models or blockstates.
- BER handles the independently moving stamp heads, 120-degree-phased cams, loaded ore and Gear V2 input shroud. Stamp heads are now centered over the three crushing channels at X -8, +8, +24 model pixels, with a resting shoe height of 24 pixels above controller ground.
- `bronze_stamp_mill_hammer_worn.json` visually indicates the shared shoe wear state; only successful batches decrement maintenance.

**Never manually edit the 228 generated V2 part files**. Regenerate from the authored design and isolate diffs to this machine:
```bash
node tools/multiblock-models/build-designs.mjs
node tools/multiblock-models/generate.mjs
```
Check output for other machines (especially the accepted Bronze Lathe) before committing. Do not accept stretched UVs or overlapping faces.

## Resources and state

- Port U connects to controller slot 0 (recipe-validated insertion).
- Port O connects to slot 1 (primary output; extraction only).
- Port Y connects to slot 2 (Gravel byproduct; extraction only).
- A batch reserves both outputs, picks the optional byproduct once, and does not consume ore or wear the shoe until success. Blocked products or inadequate torque/speed pause it.
- Replaceable Stamp Shoe lasts 384 successful batches. Maintenance and unfinished recipes are saved in NBT.
- Recipe type `magneticraft2:multiblock_processing`, machine `magneticraft2:bronze_stamp_mill`. Datapacks and JEI category/catalyst stay supported.
- Gear V2 power and maintenance logic remain unchanged by this visual-layout fix.

## Verification commands

```bash
./gradlew build
node tools/bronze-stamp-mill/test-contract.mjs
node tools/multiblock-models/build-designs.mjs --check
node tools/multiblock-models/generate.mjs --check
```

In-game checklist:

1. Dismantle the previous 5x3x3 version and rebuild this 5x3x4 blueprint. Test all orientations.
2. Confirm the controller forms the machine and the nine AIR proxy cells are placed without replacing ANY of the four actual modules.
3. Inspect the **grounded, front-accessible** three port models, the three separate chutes, canopy support and raised crushing tray. Check no invisible barriers, z-fighting, texture distortions or protruding modules.
4. Insert two raw Chalcocite or Cassiterite through port U; extract crushed output from O and any Gravel tailing from Y. Test item automation from outside the front service row.
5. Watch hammer shoes travel vertically and strike above the tray without clipping the modules.
6. Pause/restore Gear V2 power mid-batch, unload/reload chunks, fill either output, and verify no free items or lost progress.
7. Wear and replace the Stamp Shoe; confirm visual wear changes and correct job inhibition.
8. Continue crushed ore into Washer and Sifter, checking existing ore yields and fluid/byproducts.

**No `./gradlew build` or Minecraft runtime verification has been performed for this revision.**
