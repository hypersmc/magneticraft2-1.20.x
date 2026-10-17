# Bronze Stamp Mill — implementation contract

Status: **First Bronze II prototype on PR #11**. Source and structural checks only; no local Gradle or client run yet. See [the Bronze Age production plan](bronze-age-production-plan.md) for the full accepted progression.

## Industrial purpose

This machine converts **two raw Chalcocite or Cassiterite Ore into two crushed ore items in 115 ticks** with Gear V2 input (minimum 24 RPM, 6 torque). Each batch can independently produce one Gravel tailing at 20%. The existing Mechanical Grinder stays available as a low-power, single-item alternative; Washer and Sifter remain the next required stages. We deliberately do **not** bypass either machine or increase metal yield without material accounting.

## 5×3×3 multiblock

The south blueprint uses a five-wide, three-deep, three-high structure; west/east orientations rotate to 3×3×5. All are in `src/main/resources/data/magneticraft2/multiblocks/bronze_stamp_mill_{south,west,north,east}.json`.

South blueprint viewed in top-down rows rear-to-front; bottom layers listed first:

```text
Y=0  S S I S S    S S S S S    S S C S S
Y=1  P P P P P    B _ _ _ B    B U Y O B
Y=2  B P P P B    B _ _ _ B    B P P P B
```

Legend: `S` Stone Bricks, `P` Oak Planks, `B` Cut Copper, `C` Stamp Mill Controller, `I` Mechanical Input Module, `U` Item Input, `Y` Byproduct Output, `O` Primary Output, `_` required air. The controller occupies bottom front center. Use the in-game Patchouli **Visualize** page for authoritative placement.

### What is rendered where

- `tools/multiblock-models/designs/bronze_stamp_mill.json` is the authoring source with 60 structural cuboids and the Voxel V2 definition.
- `tools/multiblock-models/sources/bronze_stamp_mill.json` is the occlusion-culled canonical source.
- 42 block-local render hosts per facing are generated from that source. The Gear V2 module and all three item-port modules remain real BEs and have **empty** static V2 part models, not filler geometry.
- Three upper AIR cells are opt-in V2 render/voxel proxies; they are explicitly passed from `getRenderAirAnchors()` to `MultiblockRenderParts.assign()`. Do not treat every AIR cell as a proxy.
- `BronzeStampMillRenderer` is reserved for animated components: three 120-degree-offset stamp heads, drive cams, feed item and the formed input housing. Input module geometry is deliberately not baked over its BE cell.
- `bronze_stamp_mill_hammer_worn` is a separate wear-marked head model selected from the saved maintenance wear state. Wear is counted only on successful batches.

**Do not manually edit the 168 directional V2 part files**. Run `node tools/multiblock-models/build-designs.mjs`, then `node tools/multiblock-models/generate.mjs` after changing the source, inspect diffs and confirm `--check`. The generator touches other machines too, including the accepted Bronze Lathe; review and isolate unrelated changes before committing.

## Resource, power and state rules

- Port #0 accepts only machine-specific recipe inputs (via `MultiblockItemPortBlockEntity.acceptsControllerInput`); primary #1 and tailings #2 are machine outputs.
- A batch chooses its tailing once and stores this choice in NBT. If a pipe, belt or inventory blocks any reserved result, production **pauses without consuming input** and resumes when space is free. Material is removed at successful completion only.
- Network load is requested from the real Gear V2 input; if speed or torque is insufficient, the process stalls without advancing its counter. Load is released during idle, broken-maintenance and destruction.
- Replaceable stamp shoe: 384 successful batches by default. Controller right-click with a fresh shoe repairs it; sneak-empty-hand inspects condition. One part services the whole three-stamp mechanism for this first release; finer per-head wear is a later design candidate.
- Input, output, byproduct, recipe ID, pending tailing choice, remaining wear and processing progress are persisted in BE NBT. Machine power can fail and recover without item duplication.
- Recipes are datapack-editable `magneticraft2:multiblock_processing` and are automatically visible in existing generic Multiblock Processing JEI category with the Stamp Mill catalyst.
- Working machine uses its three visible hammer-head assemblies. Gear rotation is based on actual input speed; it must not be animated by a world-time-only fake motor.

## Local acceptance tests

From the repo root:

```bash
./gradlew build
node tools/bronze-stamp-mill/test-contract.mjs
node tools/multiblock-models/build-designs.mjs --check
node tools/multiblock-models/generate.mjs --check
```

If generation checks report mismatches, **do not blindly commit regenerated Lathe/Foundry files**; inspect them.

In-game:

1. Form all four rotated structures, confirming Guidebook preview, controller interaction and empty AIR spaces. Check idle/operating collision and no fake blocks across actual item/module ports.
2. Power Gear V2 at 24+ RPM, 6+ torque. Feed two Chalcocite Ore and verify exactly two Crushed Chalcocite after 115 ticks; repeat with Cassiterite.
3. Inspect visual input bearing, all three synchronized-offset stamps, trough, mounted inventory, and performance looking directly at the machine.
4. Cut power mid-batch, restart and chunk reload; confirm progress and pending Gravel roll are not lost or rerolled.
5. Fill either output port; confirm no input is consumed before result storage is possible, and production resumes after clearing it.
6. Wear out shoe over repeated successful batches; confirm the visible worn head, failure to run, status message and restoration with a replacement.
7. Pipe and belt items to/from the three ports; verify invalid ore never enters and neither output can be used as input.
8. Process the resulting Crushed Chalcocite/Cassiterite in the existing Washer and Sifter, confirming no duplicate yield and correct dirty-water output.

## Not yet implemented

The Rotary Ore Classifier, ore roasting, settling-water recycling, charcoal retort, refractory kiln, iron bloomery and other Bronze II-IV candidates remain roadmap work. The fully automated crusher -> Washer -> Sifter transfer needs in-game proof, not an assertion based only on code.

**No local `./gradlew build` or Minecraft test was performed when this first prototype was committed.**
