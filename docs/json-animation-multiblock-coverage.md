# JSON animation — all-multiblock compatibility and migration audit

**Reference branches:** Bronze `feature/bronze-age-foundry` (PR #11),
JSON animation `feature/json-driven-animation` (PR #12), and Sifter
`feature/sifter-json-slider-crank` (PR #13, targeting #12).

**Date:** 2026-10-26

## Two different acceptance requirements

1. **Compatibility:** Every existing multiblock must still form, render,
   process items/fluids, respect wear, and dismantle correctly with the
   animation runtime installed. A machine with no animation JSON should keep
   using its existing renderer. This is mandatory before merging PR #12.
2. **Full visual moddability:** Every moving component can be changed via
   resource-pack model + animation JSON. That is a separate migration, and
   **is not completed** by PR #13. Don't imply all multiblocks have already
   been converted just because the Sifter and Lathe work.

## Complete inventory (12 families / 42 construction JSON variants)

| Machine | Construction facings | Render/motion approach | JSON animation status |
| --- | --- | --- | --- |
| Blueprint Maker | N, E, S, W | Legacy static model/structure | Not needed for mechanical motion |
| Bronze Foundry | N, E, S, W | Rendering V2 static cells; custom molten/heat/items | Custom dynamic renderer retained |
| Bronze Mechanical Lathe | N, E, S, W | Rendering V2; Gear V2 drivetrain, clutched spindle and carriage | **PR #12:** input and output gearbox shafts in JSON; other dynamic visuals still Java |
| Bronze Stamp Mill | N, E, S, W | Rendering V2; Gear V2 cams/stamps/belts and wear | Legacy dynamic; migration candidate |
| Charcoal Retort | N, E, S, W | Rendering V2 static cells | Existing behavior retained |
| Mechanical Ore Washer | N, E, S, W | Gear V2-driven drum/belt; fluids, item positions, mesh wear | Legacy dynamic; migration candidate |
| Mechanical Sifter | N, E, S, W | Gear V2 slider-crank; inclined sieve + six wear stages | **PR #13:** exact JSON mechanism, parented carriage and model variants |
| Primitive Furnace | N, E, S, W | Legacy model; heating and item render feedback | Existing specialized renderer retained |
| Primitive Grinder | Any | Gear V2 rotor/axle plus workpiece and output | Legacy dynamic; simplest next migration candidate |
| Primitive Storage Cellar | West only | Legacy formed storage structure | Existing behavior retained |
| Rotary Ore Classifier | N, E, S, W | Rendering V2; Gear V2 drum, ore simulation and mesh wear | Legacy dynamic; migrate drum only initially |
| Water Settling Tank | N, E, S, W | Rendering V2; fluid surfaces and transfer visuals | Specialized fluid renderer retained |

**Important:** A V2 formed machine intentionally has no giant
`models/multiblock/<machine>_<facing>.json` file. Static geometry is in
`models/multiblock/<machine>_<facing>/part_*.json`, distributed among
controller/filler cells. The automatic audit validates **both** forms.

## Automated compatibility checks

After PR #13 merges into the PR #12 branch, run:

```bash
node tools/animation/audit-multiblock-coverage.mjs
node tools/animation/check-scene-fixtures.mjs
node tools/animation/check-sifter-slider-crank.mjs
node tools/multiblock-models/build-designs.mjs --check
node tools/multiblock-models/generate.mjs --check
./gradlew build
```

The **first script is added directly to PR #12** and is safe to run before
PR #13 merges; it reports 1/6 mechanical JSON coverage today. After
merging #13, it should report 2/6.

It validates:
- Every expected multiblock family and facing is present (including
  `primitive_grinder_any` and `primitive_storagecellar_west`).
- Every JSON structure has consistent 3D dimensions, layer row lengths and
  defined palette roles.
- Every formation points to an available legacy formed model or has valid
  Rendering V2 per-cell model files.
- An opted-in animation renderer actually calls the generic scene renderer.
- Default animation resource-pack model references exist; nested parent
  references, pivots and canonical coordinate system are structurally valid.
- Coverage is visible: machines that still use existing Java animation
  are **reported**, not silently counted as migrated.

An optional `--require-mechanical-json` flag intentionally fails until
all six mechanical families actually use JSON; do not use that flag in
normal CI until the migration is completed.

**Automated asset checks cannot prove Minecraft formation, loading, UV
correctness, animation synchronization, collision or FPS.** These still
require in-game checks.

## Small in-game regression matrix

For **all 12 families**, form the structure using the current book/JEI
instructions, then unform it. Inspect the existing static V2/legacy model,
textures, collision, item/fluid ports and output behavior. Verify the
controller still accepts interaction and save/reload does not duplicate
filler blocks. For the ten families with all cardinal orientations, check
at least one rotation of each model layout and all four for the newly
converted Sifter and Lathe.

For the **six mechanically animated machines**, also confirm a real Gear V2
source drives only the intended moving components; idle/unpowered visuals
do not spin from a separate clock. Stop/reverse/overload tests were already
covered by much of the previous mechanical system but should get at least a
spot check at newly migrated renderer boundaries.

For **wear**, verify the visible stage matches the existing worn-part data.
The Sifter has six wear models, and the Washer, Stamp Mill and Classifier
have their own existing deterioration systems. This is NOT an invitation
to change server-side wear mechanics.

For **resource packs**, test F3+T with one pack that changes a model ID
outside the standard multiblock model folder, one pack that changes a
mechanical animation field, and a malformed animation resource that
triggers the old Java fallback. Existing static/filler models must not
disappear. No GeckoLib files should be mistaken for Magneticraft2 formats.

For **performance**, a JSON definition should be compiled once on reload,
models prebaked and poses evaluated per frame. Do not rebuild V2 static
geometry per frame or allocate unbounded render-instance state.

## What to migrate next (not automatically in this audit)

1. **Primitive Grinder:** rotor and axle are simple Gear V2 rotations.
2. **Mechanical Ore Washer:** drive pulley/drum first, retaining specialized
   water, item motion, mesh wear and repeating belt texture logic.
3. **Bronze Stamp Mill:** shafts/cams first. Stamp travel and belt UV
   animation should remain typed/procedural until modeled as proper
   constraints, not approximated with independent sine curves.
4. **Rotary Ore Classifier:** drum segments driven by the real shaft,
   while ore particle simulation and worn mesh stay specialized.
5. Complete **Lathe** spindle and carriage animation through typed
   additional inputs/tracks where fidelity is proven.
6. Add optional visual-only mappings to **Foundry, Settling Tank and
   Primitive Furnace** only where relevant; never force fluids/heat or
   item rendering into a rotation-only JSON schema.

The guiding rule is **data-driven visual models and typed movements, not
an arbitrary replacement of every specialized renderer**.

## Scope and verification status

The source inventory has been checked against the Bronze-derived GitHub
tree, including the per-cell static V2 directories. The 42 construction
files' dimensions, palette cells and layer shapes were individually
reviewed through the repository connector, with no mismatches detected.

The automated script is committed, but a full local execution of that
script, `./gradlew build`, and gameplay regression tests have **not**
been completed in this environment. Keep PR #12 draft until verified.
