# Bronze Foundry Sand Mixer K1 — production and rendering contract

**Status:** Source candidate on draft PR #11. This document defines intended behavior; Java compilation, model loading, full Gear V2 load tests, multiplayer, and real-world gameplay are not certified.

## Why the machine exists

The existing **Bronze Casting Basin** consumes a Green Sand Mold on pouring, and the **Bronze Mold Table** uses one sand (Sand or Red Sand) + one Clay Ball per mold. Without recovery, casting requires new sand and binder each time. The K1 mixer reduces material consumption without making fresh resources irrelevant.

| Step | Inputs | Outputs |
|---|---|---|
| Mold Table, fresh | Reusable wooden pattern + **1 Sand/Red Sand + 1 Clay Ball** | 1 Green Sand Mold |
| Casting Basin, successful full cooling | 1 matching Green Sand Mold + exactly 1 molten bronze equivalent | 1 rough bronze casting (unchanged) + **1 Spent Molding Sand** (new) |
| Prepare reclamation feed | **4 Spent Molding Sand + 1 Clay Ball** | 1 Sand Reclamation Blend |
| Powered sand mixer | **1 Sand Reclamation Blend** | **3 Reclaimed Molding Sand** |
| Mold Table, reclaimed | Reusable wooden pattern + **1 Reclaimed Molding Sand**, no additional clay | 1 Green Sand Mold |

Material recovery **loses 1 sand-equivalent per 4 castings** (75% recovery); one clay binder is shared across the three recovered uses. Repeated recycling cannot create extra sand. The mixed feed is **manually crafted first in K1** to reuse the established single-item-input port and datapack recipe format; this is not yet a separate automated clay input. The 200-tick Gear V2 mixing process is the only step producing reclaimed molding sand.

**Backward compatibility:** Original Sand/Red Sand + Clay Ball Mold Table workflow stays unchanged. Existing green sand molds, rough casting items and quality NBT stay unchanged. Casting source NBT and saved old basins remain loadable. An old saved basin output does not retroactively create a recovered-sand drop if its saved `RecoverableSand` flag is absent.

## Structure, power, animation

**Size:** 3 wide × 2 high × 3 deep. SOUTH blueprint (rows rear → front):

```
Top Y1:     PPP   Bottom Y0: SIS
            PAP              USO
            PPP              SCS
```

P=Oak Planks, A=required AIR, S=Stone Bricks, I=existing Gear V2 Mechanical Input Module, U=existing Item Input, O=existing Item Primary Output, C=controller.

The **Patchouli** preview mirrors SOUTH X, displaying **SIS / OSU / S0S** bottom up. Rotate the exact JSON and air render anchor to other directions; do not confuse display-side U and O with raw JSON coordinates.

The physical rear input is 2 blocks behind the controller; the torque requirement is **4**, speed minimum **12 RPM**, process length **200 powered ticks**. One pre-installed set of **Bronze Mixing Paddles** wears after **512 completed batches**. No wear on idle/pause or partial work; missing/stalled network/output full releases demand. Expose item capability only through the U/O modules; controller itself refuses direct item capability.

The reference model is an open copper/bronze-lined trough on a stone/timber base, with visible Z-axis paddle and rear supported gearcase. Static formed V2 parts use native 16-pixel tiled textures, plus an AIR render anchor for the open center. The cached BER animates only the paddles and rear shaft coupling, and renders the one current feed blend as an item. There are **no** per-tick geometry creations, world item entities for the mounted workpiece, fake power nodes, or new Gear V2 simulation.

**Editable Studio source:** `tools/multiblock-models/designs/bronze_foundry_sand_mixer.json`. Additional orthographic guidance: `docs/reference/bronze-foundry-sand-mixer-studio.svg`. The separate concept infographic is a *visual direction*, not proof of its precise blueprint or in-game geometry.

## Required acceptance

1. `node tools/multiblock-models/build-designs.mjs --check`, `node tools/multiblock-models/generate.mjs --check`, `node tools/bronze-foundry-sand-mixer/test-contract.mjs`, then `./gradlew build`.
2. Form all 4 orientations using Patchouli. Check correct real I/U/O modules, required AIR anchor, open trough collision, no missing-model textures, and controller ticker.
3. Original Mold Table must still form a mold from sand + clay + pattern in four presses. Put one cooled mold and bronze-equivalent through Casting Basin; collect *exactly one* rough casting + *one* spent sand, with original quality unchanged.
4. Break/reload **before cooling completes**, **after cooling before collecting**, and **after collecting**. No double-spent refund, lost rough casting, duplicated ingots or lost NBT. Mid-pour dismantling must NOT yield reclaimed sand.
5. Craft four spent + one clay into one feed blend; insert via real U; connect real Gear V2 module and deliver 12 RPM / 4 torque for 200 supplied ticks; collect exactly **three** reclaimed sand via O.
6. Use each reclaimed sand at the same old Mold Table *without* extra clay. Repeat reclamation with mixed fresh/recovered molds and verify progressive loss, no infinite mass loop.
7. Test input wrong item, no paddles/worn paddles, output full, network overload/speed stall; progress, wear, and item NBT survive world reload; do not consume on unsupplied load. Test item automation, separate dedicated server and two clients.
8. Visual acceptance from every direction: paddles mesh and shaft align and are supported; UVs not stretched; no Z-fighting with mixing trough or hidden module occlusion; profile FPS with multiple machines.
9. Regression: current Pattern Maker, Casting Basin direct pour, Foundry, Lathe, Boring Machine, Sawmill, JEI and mechanical module system.

## Known K1 limitations

Prepared binder-feed is crafted manually, not pumped by a second material port. Reclaimed sand color does not retain vanilla Sand versus Red Sand provenance. No HEAT, fluid binder, humidity, individual mineral contaminant grade, or explicit fine-tailings byproduct item is modeled (the 25% loss is consumed in the recipe). Machine numbers are preliminary and datapack-editable. No performance or visual claims until measured/inspected in Minecraft.

**Poster fidelity note:** the generated decorative concept infographic is a style reference only. It is not an exact reproduction of the JSON layout, recipes or physical I/O. In particular, K1 consumes **one pre-crafted blend** through one left item port, uses **4 torque**, and yields **exactly 3 reclaimed sand**. The only two structure layers are bottom Y0 and top Y1; there is **no third layer or top binder port**. Use the blueprint/contract above for all exact numbers and locations.
