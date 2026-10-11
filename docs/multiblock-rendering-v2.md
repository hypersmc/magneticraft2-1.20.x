# Multiblock Rendering V2

**Status:** First implementation in draft Bronze Age PR #11. The Bronze Foundry uses this path. Older machines remain on their existing render paths.

## Why the change

Minecraft 1.20.1 block model elements must lie within -16..32 model pixels; a single controller-anchored 3x3+ machine can exceed this and makes larger machines disappear when the controller chunk is culled. V2 represents one formed machine as **static baked models attached to the already-existing controller and filler cells**.

- The multiblock's datapack JSON remains authoritative for structure/formation.
- A single **south-facing canonical source model**, under `tools/multiblock-models/sources/`, defines the complete static geometry. The source is intentionally NOT in the Minecraft models resource folder (its large coordinates are not vanilla-safe).
- `node tools/multiblock-models/generate.mjs` clips source elements into block-local geometry, corrects UV dimensions without stretching, rotates all four horizontal facings, and emits ordinary Minecraft models as `assets/magneticraft2/models/multiblock/<machine>_<facing>/part_<dx>_<dy>_<dz>.json`.
- Generated models obey vanilla's -16..32 element bounds and each face's UVs stay in the 0..16 atlas tile.
- The generator rejects **same-facing coplanar faces** in the canonical model before exporting any parts. This detects actual depth fighting early, including overlaid copper trim or repeated brick surfaces. To create a decorative trim, place its visible face slightly proud of the wall and remove buried faces instead of stacking two faces on the same plane.
- The Foundry prototype's four copper corner braces now project 0.5 model pixels beyond the brick corners and top, while hidden rear faces on the firebox trim and overlapping spout faces are removed. Its collision remains the original brick shell; the small protrusion is decorative.
- **Collision in required-air structure positions:** a JSON multiblock can opt in to `settings.render_air_anchors` with an array of controller-relative integer offsets `[[dx,dy,dz], ...]`. These cells must be **AIR before formation**, as validated by the ordinary structure blueprint; on formation, the machine replaces only the explicitly named cells with a regular non-occluding filler that stores a block-local collision snapshot and its V2 model ID. The saved original blueprint remains AIR, so dismantling restores each proxy back to AIR without dropping fake items. Other machines are unaffected. This is important for the Foundry's upper crucible/tap cells, where rendered surfaces otherwise had no corresponding block for physical collision.
- The formed controller blockstate uses its own `part_0_0_0`. Each filler has the same static model selector, with a synchronized/persisted `render_part` key in its existing block entity. Chunk baking renders each filler normally. If its key is empty, the filler stays **invisible** (compatibility with older machines).
- V2 computes each filler’s **local collision shape once** at formation and saves it in its NBT. Reloading a filler chunk without the master chunk loaded preserves physical collision and the static visual.
- The generic `MultiblockRenderParts.assign` helper is reusable by the next machine; do not hardcode Foundry logic into the filler or the model selector.
- Missing optional named models in the older geometry loader no longer trigger an unchecked `Optional.get()`.

## Editable machine geometry

The Foundry is now modeled as 99 **named solid volumes** in `tools/multiblock-models/designs/bronze_foundry.json`. These are the editable source, not vanilla item/block models. `build-designs.mjs` extracts only surfaces exposed to air, removes covered faces and coplanar material overlaps, and creates world-aligned UVs. The V2 generator then splits those faces across per-block chunk renderers, preserving the same canonical south-facing model for every orientation.

The geometry uses only texture-correct world-space UV tiles, avoiding both texture stretching and tiny UV-offset hacks. The lower furnace is physically recessed, the upper crucible is open, and the front channel visibly connects to the crucible. Hidden interior faces do not consume vertices.

`--check` detects stale source/controller assets, and the separate V2 `--check` detects stale block-local assets. Both checks are useful after any model adjustment.

## Usage / development

```bash
node tools/multiblock-models/build-designs.mjs --self-test
node tools/multiblock-models/build-designs.mjs --check
node tools/multiblock-models/generate.mjs --self-test
node tools/multiblock-models/generate.mjs --check

# When editing designs/bronze_foundry.json:
node tools/multiblock-models/build-designs.mjs
node tools/multiblock-models/generate.mjs
```

Legacy direct models inside sources/ remain supported for machines that are not built from the new design-file approach.

## Original V2 generator usage

```bash
node tools/multiblock-models/generate.mjs --self-test
node tools/multiblock-models/generate.mjs --check
# After changing source geometry:
node tools/multiblock-models/generate.mjs
```

The generator discovers any canonical `tools/multiblock-models/sources/<machine>.json` file and expects matching `data/magneticraft2/multiblocks/<machine>_{south,west,north,east}.json` structures. It regenerates every occupied host's model and deletes stale generated part files; `--check` detects both missing/changed and obsolete part files. This allows adding a second machine without modifying the generator itself.

The included virtual stress case splits a **9x5x9 (405-cell)** structure with a 9-block-wide, 9-block-deep continuous surface. It validates atlas UVs and vanilla element coordinate bounds. This is a **generator test**, not evidence that 405 block entities have been benchmarked in-game.

## Design limits and future improvements

1. **Static cuboids only in this initial exporter.** Rotated cuboids with Minecraft element rotations must be authored as local static components (or supported in a future clipping implementation), while animated shafts, gears, piston arms and molten surfaces use separately bounded, cached block entity renderers. A dynamically changing part must never force the entire static Foundry to rebuild.
2. **Geometry in an AIR structure cell:** V2 attaches it to the nearest occupied controller/filler within the vanilla -16..32 local coordinate envelope. If no host is close enough, generation fails with a useful message; a render-anchor solution will be needed for very large hollow interiors. This approach may overdraw across a nearby chunk boundary by up to one block; don't mistake it for independent air-cell culling.
3. **Size versus block entities:** Static visuals can span many chunk meshes, but the existing controller still creates a block entity for every filler. Large production buildings should eventually replace unnecessary per-cell BEs with grouped/chunk-local metadata. Benchmark memory, save time and rebuild cost before promising thousands of fillers.
4. **Dismantling across unloaded chunks:** Current multiblock lifecycle largely comes from the pre-V2 controller. Delayed dismantling for enormous multiblocks and safe restoration of parts in not-yet-loaded chunks still need dedicated implementation and tests.
5. **Lighting/interaction/protection:** This first pass reuses existing physical shapes and click forwarding. Validate occlusion, skylight, dynamic part bounds, hitboxes, reloading, and chunk edges in-game before migrating other machines.

## Air collision-anchor regression tests

- Re-form an existing Foundry once after installing this change (previously formed structures did not place the new anchors).
- Walk onto the pouring channel while looking at it, then rotate your view away and repeat; physics must remain unchanged.
- Confirm both upper air cells become non-occluding V2 filler blocks **after** formation, with partial/hollow collision matching the crucible and front tapping assembly.
- Save/reload with the controller chunk temporarily unloaded and confirm the local collision remains stable.
- Break either the controller or an anchored component; the two positions should return to AIR with no fake item drops.
- Form each of four facings. Nearby unrelated AIR cells must remain AIR, and all older multiblocks must keep working.

## Acceptance checklist for the Foundry prototype

- Build from Patchouli with all four placements; controller and filler models join without gaps.
- No oversized model errors from `bronze_foundry_<facing>.json` or any `part_*.json` models.
- Verify textures and UVs (including the front tapping lip); no stretched/compressed faces.
- Test a chunk border: unload the **controller’s chunk**, leave another part's chunk loaded; static parts and local collision remain.
- Confirm forming/unforming, break/drops, save/load, server-to-client sync, collision, selection and particle lighting.
- Verify older Washer and Sifter multiblocks retain their visuals and the original invisible fillers.
- Do not claim the Foundry is operational: molten bronze, heating and transfer aren't built yet.

V2 is intentionally an incremental renderer migration, not a wholesale rewrite of the JSON multiblock lifecycle.
