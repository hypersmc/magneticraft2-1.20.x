# Magneticraft2 — Visual Identity and Texture Transition

Status: **design baseline / staged implementation plan**, agreed direction for the active Forge 1.20.1 project (2026-10-19). This document does **not** claim that the texture set, its example paths, or the proposed Rotary Ore Classifier already exist. Review the first actual artwork in Minecraft before declaring the palette final.

Related plans: [Bronze production](bronze-age-production-plan.md), [Rendering V2](multiblock-rendering-v2.md), [Voxel V2](multiblock-voxels-v2.md), and [ore processing by age](ore-processing-by-age.md).

## Goal

Give Magneticraft2 an original, recognizable Minecraft-scale industrial look *as its ages advance*, without retroactively making primitive machines look mass-produced or replacing familiar world materials just to be different. **Vanilla textures may remain where they are honest vanilla materials** (ordinary stone, logs, dirt, etc.). A mod-specific functional part should gradually acquire a mod-specific texture. By Iron/Steel and beyond, the exterior of our machines should predominantly use our own assets.

Use an **incremental material library**, not one-off retextures of every individual machine. New materials become shared vocabulary across machinery, and legacy machines migrate only when their behavior and silhouette can be regression-tested.

## Existing implementation audit (Bronze PR #11, 2026-10-19)

- The repository already contains substantial mod-owned art, notably dozens of registered ore textures, item textures, conveyor/belt/pipe textures, and some multiblock textures. **Do not discard or globally replace these.** Some are unfinished or historical assets; ownership alone is not a sign of final quality.
- The current `tools/multiblock-models/designs/bronze_foundry.json` still resolves `brick`, `refractory`, `soot`, `iron` and `bronze` to vanilla `minecraft:block/...` surfaces (including `cut_copper` used for bronze).
- `bronze_mechanical_lathe.json` uses vanilla spruce, cut copper, iron, stone and blackstone; `bronze_stamp_mill.json` uses vanilla stone bricks, stripped spruce, cut copper, iron and blackstone. Moving parts such as `bronze_stamp_mill_hammer.json` also reference vanilla textures.
- This is why the **Bronze core material set** is a useful first target. The existing model generator explicitly preserves tile density / UV scaling and validates coplanar faces. Do not bypass it by hand-editing generated V2 `part_*.json` files.
- Current ore-processing and age data are authoritative for progression. **Changing texture identity must not change ore registry IDs, ore/ingot quality, recipes, wear logic, formation, or power.**

## Non-negotiable visual language

1. **Minecraft pixel-art language first:** sharp nearest-neighbor pixels, restrained texture noise, readable from a few blocks away. No smooth/PBR gradients, photo textures, fake realistic lighting baked into every face, or unrelated Hytale-like ornament.
2. **Physically legible construction:** wood actually supports the load, metal collars meet bearings/shafts, gears engage, chutes connect. Do not fix floating parts by recoloring them; correct geometry is separate.
3. **Material honesty:** copper is warm red-orange, bronze is more subdued brown-gold, wrought iron and steel are distinct, and refractory bricks look ceramic rather than metal. **Never use copper textures for bronze simply because Minecraft provides copper.**
4. **Texture density and UVs:** start with `16x16` repeatable pixel textures for one standard 16-model-pixel tile; build larger graphics only where a larger surface needs one. Repeat/tile long beams and drums, never stretch a single 16px tile over a many-block surface. Do not compress texture content into tiny faces. Validate every orientation using the V2 model generator.
5. **Static detail in baked models; motion in BER:** do not pay a per-frame renderer cost for a static pattern. Retain visibility and collision separation from Voxel V2.
6. **Visual feedback belongs to the part:** a worn classifier screen should show a damaged screen; worn bearings/shoes/belts should look worn at those components. Do not use a generic machine-wide durability tint.
7. **Functional markings:** physical item input/output, water direction, drive direction, maintenance state and active/blocked states must be readable where relevant. Accent colors convey a purpose and must not be the sole signal.
8. **Minimal churn:** retain existing paths/models unless intentionally migrated. Prefer adding new shared material textures and changing the authored machine material map in a dedicated, reversible commit.

## Age-by-age art direction

| Age | Construction identity | Texture transition |
| --- | --- | --- |
| **Stone** | Raw wood, earth, mud/clay, fire, stone tools, handmade joints | Remain largely compatible with vanilla's natural palette; original ash, clay, ceramic and handmade-tool details when useful |
| **Copper** | Open workshop, visible timber braces, hammered copper, exposed gears, leather drive parts | Introduce `worked_copper`, `machine_timber`, copper bearing/shaft faces, tool marks and simple IO hardware |
| **Bronze** | Cast and machined metal components, regularized frames, refractory fireboxes, driven drums, chutes and maintenance access | **First coherent machine-wide original material kit**. Pilot on the Rotary Ore Classifier; reuse on Foundry, Lathe and Stamp Mill after review |
| **Iron** | Wrought iron, forge scale, rivets, charcoal heat, heavier mechanical joints | Original forged iron, scale/dark metal and refractory variants; keep the bronze machines recognizably earlier |
| **Steel** | Precision machined steel, heavy plate, faster/stronger industrial equipment | Own steel grades, machined edges, fasteners, guards, wear surfaces; fewer exposed timber structures |
| **Steam** | Boilers, pressure vessels, pipework, valves, cast housings, condensate | Original pressure-rated materials, insulation, brass/bronze fittings and functional gauges |
| **Gasoline** | Engines, oil/fuel handling, cast engine bodies, rubber hoses, exhaust | Original engine alloy, oil-stained service surfaces, hoses, seals and restrained functional labels |
| **Electric Generator** | Power conductors, electrical enclosures, insulation, transformers/motors, controls | Original insulation classes, copper windings, cable families, electrical hazard/connection indicators |
| **Nuclear** | Shielding, sealed serviceable assemblies, containment, monitoring and heat management | Original reactor/shielding/containment surfaces and *meaningful* warning/status patterns, not decorative glowing everything |

These are **visual design targets**, not age gates or claims that all corresponding machines/materials have been implemented. Bronze marks the *start* of sustained original-machine artwork; the journey continues rather than flipping to a completely different art style overnight.

## Shared material library — proposed names and starter colors

The following hex colors are **art-direction anchors, not final approved pixels or a runtime configuration**. A texture is not just a fill color: design its grain, specular highlights, tooling, contrast and shadow in hand-authored pixel art.

| Shared family | Proposed texture name | Starter color anchor | Material / use |
| --- | --- | --- | --- |
| Worked copper | `worked_copper` | `#B76A49` | Copper-age forged/hammered plates and structural mechanical surfaces |
| Cast bronze | `bronze_cast` | `#A77B49` | Foundry cast housings, large drum collars and machine body parts |
| Machined bronze | `bronze_machined` | `#C39757` | Precision bearings, turned edges, lathe/chuck parts; cooler contrast than bright gold |
| Worn bronze | `bronze_worn` | `#786044` | Visible localized wear; **not** a one-texture substitute for per-component damage |
| Machine timber | `machine_timber` | `#74573E` | Supported structural timber for Copper/Bronze machinery |
| Timber end grain | `machine_timber_end` | `#997453` | Cross-cut beams and real end-grain joints |
| Refractory brick | `refractory_brick` | `#A38A70` | Kilns, Foundry and high-heat ceramic liners |
| Dark work iron | `dark_work_iron` | `#50575A` | Functional steel/iron stand-ins until Iron/Steel families are finalized |
| Chute liner | `chute_liner` | `#6A5D50` | Abrasion/contact surface separate from outer frame |
| Leather drive | reuse/revise current `leather_belt` where appropriate | `#68452D` | Flat drive belts and pulleys; don't invent a parallel material system |
| Screen mesh | `screen_mesh_bronze` | `#B18A55` | Classifier's actual sieve/screen; intentionally readable holes |
| Worn screen mesh | `screen_mesh_bronze_worn` | `#8E7151` | Specific damaged screen, with visible tear/bent wires rather than overall darkening |
| Patina accent | future `copper_patina` | `#4B817C` | Optional oxidized copper *only where exposure makes sense* |

For the first textures, use **original, tileable 16x16 PNG artwork**, a single consistent 16-pixel density, and no per-machine color tint or blanket recoloring. Texture transparency is for actual holes/cutouts, not a workaround for incorrectly placed faces.

### Canonical asset paths (proposal; don't reference until PNGs exist)

```text
src/main/resources/assets/magneticraft2/textures/block/material/worked_copper.png
src/main/resources/assets/magneticraft2/textures/block/material/bronze_cast.png
src/main/resources/assets/magneticraft2/textures/block/material/bronze_machined.png
src/main/resources/assets/magneticraft2/textures/block/material/machine_timber.png
src/main/resources/assets/magneticraft2/textures/block/material/machine_timber_end.png
src/main/resources/assets/magneticraft2/textures/block/material/refractory_brick.png
src/main/resources/assets/magneticraft2/textures/block/material/dark_work_iron.png
src/main/resources/assets/magneticraft2/textures/block/component/chute_liner.png
src/main/resources/assets/magneticraft2/textures/block/component/screen_mesh_bronze.png
src/main/resources/assets/magneticraft2/textures/block/component/screen_mesh_bronze_worn.png
```

After assets exist, a machine's authored texture aliases can select them without changing geometry, e.g.:

```json
{
  "textures": {
    "wood": "magneticraft2:block/material/machine_timber",
    "bronze": "magneticraft2:block/material/bronze_cast",
    "iron": "magneticraft2:block/material/dark_work_iron",
    "particle": "magneticraft2:block/material/bronze_cast"
  }
}
```

The snippet shows a **possible alias-map migration**, not the complete design for a specific machine. Do **not** rename any existing model material keys indiscriminately, overwrite generated models, or change universal module textures globally.

## First practical pilot: Bronze Rotary Ore Classifier

This is the **next planned multiblock** in Bronze II, not an implemented block in this document. The [production plan](bronze-age-production-plan.md) proposes a **5x3x5** footprint, subject to geometry and processing decisions. It must classify fractions rather than duplicate the Washer's cleaning or Sifter's existing washed-material recipes.

**Art/animation contract to establish before modeling:**

- Open timber and bronze support frame, anchored at its base; shafts, bearings and pulleys demonstrably connect to Gear V2 input.
- Visible inclined/rotating perforated drum, with screen material repeated at constant texel density and a readable interior. Avoid creating a giant opaque solid drum that conceals ore processing.
- Feed hopper attached to the drum inlet, separate lower chutes for classified fractions and tailings, with actual 1x1 item modules unobstructed.
- Distinct `bronze_cast` housing versus `bronze_machined` bearing finish, `machine_timber` supports, `chute_liner` wear surface, `screen_mesh_bronze` / `screen_mesh_bronze_worn` as the first component-specific state.
- Screen wear is **visual/art direction only until controller-side maintenance, persistence and operation rules are specified**. Do not create a screen-consumption recipe or drop/wear mechanic solely to exercise textures.
- Geometry, material routing and actual recipes are **separate work**; approve meaningful coarse/fine/tailings item contracts before implementation. Preserve existing Grinder -> Washer -> Sifter recipes, including their yields and dirty-water backpressure.

### Pilot order (small reviewable commits)

1. **Texture atlas / art review:** six primary test tiles — `machine_timber`, `bronze_cast`, `bronze_machined`, `dark_work_iron`, `screen_mesh_bronze`, `chute_liner`; compare beside vanilla oak/stone/copper and the existing Bronze machines. Add `screen_mesh_bronze_worn` only when worn-state legibility is evaluated.
2. **Classifier design:** decide real coarse/fine/tailings input/output contracts and stable physical port locations; draft one SOUTH-facing layout and working transmission geometry.
3. **Integration:** reference the approved original tiles from the classifier's authored material aliases, use V2-generated facings, native UV tiling and real separate controller/modules; make BER animate only the drum/drive/item visuals.
4. **Validation:** screenshots in all four facings and day/night, nearest-neighbor close-ups, survival formation and recovery, real recipes, output backpressure and Gear V2 pause/resume; benchmark render cost. Add book, JEI, crafting/loot and automation as required by the Bronze acceptance contract.
5. **Reuse:** after the shared kit is accepted, migrate **one existing** Bronze machine's authored material aliases (Foundry *or* Lathe *or* Stamp Mill) in a separate visual-only change; compare against its previous model. Do not retexture every multiblock at once.

## What to migrate, and when

| Priority | Targets | Rule |
| --- | --- | --- |
| **Now: first kit / Bronze classifier** | Bronze cast/machined, timber, dark iron, screen, chute | Establish shared textures; no ore yield or mechanical changes |
| **Next: Bronze consistency** | Foundry, Mechanical Lathe, Stamp Mill | Replace misleading vanilla `cut_copper`-as-bronze and other machine-material mappings *one machine at a time* |
| **Then: Copper identity** | Grinder, Ore Washer, Sifter, arm, gearboxes, bearings, pumps and pipes | Prefer shared Copper/Bronze library, preserve accepted models and public-test expectations |
| **As needed** | Existing ores, items, book pages | Audit actual quality/legibility before reworking; many already have custom graphics |
| **Future age unlocks** | Iron/Steel/Steam/Gasoline/Electric Generator/Nuclear | Add materials when a functioning machine needs them, not speculative art for every hypothetical block |

## Texture-specific acceptance and regression checks

- PNGs resolve to intended `magneticraft2:block/...` resource locations; no missing checkerboard or atlas errors. Verify their origin is independent original artwork, not copied/recolored game texture pixels.
- View tiles at 1x, 2x, 4x nearest-neighbor, then **on the actual blocks in game** beside vanilla surfaces. Readable silhouettes should survive ordinary viewing distance.
- Inspect every face on long beams, spinning drums, corners, module boundaries and 16px+ spans for **stretched/compressed UVs, swapped face orientation, seams, z-fighting and repeated faces**. Compare all four facings.
- Keep collision, formation JSON, structure requirements, machine speed/torque, inventory slots, item/fluid ports, recipes, loot and saved-world IDs untouched by texture-only commits.
- Ensure textures do not introduce render-per-frame work; generated V2 `part_*.json` files are rebuilt by the existing tools, **never hand-edited**.
- When touching a formed multiblock's geometry/voxel profile, re-form existing machines for testing; simple material-alias swaps should not silently force formation changes.
- Confirm the changed machine works in multiplayer/chunk reload and check runtime performance around animated parts.
- Run the model-design generator checks relevant to the changed machine and `./gradlew build` before claiming a compiled change. The final judgment on color, scale and machine readability comes from Minecraft screenshots, not JSON validation.

## Open art decisions — intentionally not hard-coded

- Exact colors and whether copper patinas naturally or through separate variants.
- Whether a particular machine's wood should be spruce, oak or an original treatment; material geometry and period must lead.
- Final ingot **quality-tier visuals**; current material quality mechanics are unfinished and separate from machine texture families.
- Exact classifier wear rate, replacement item and effects; user-required **visible replaceable components** must have real corresponding gameplay when implemented.
- Mineral-specific processing fractions/yields, water accounting and classifier-vs-sifter recipe split.

**Scope of this initial document:** visual identity, shared material taxonomy, first pilot and migration/testing procedure only. No PNGs or model/registry/recipe/worldgen changes are implied.
