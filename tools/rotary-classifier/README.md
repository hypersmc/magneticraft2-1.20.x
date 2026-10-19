# Rotary Ore Classifier — compact 3×3×5 redesign

After screenshots rejected the 5×3×5 scaffolding prototype, the authoritative model was rebuilt as an **open ~22 px-diameter rotating screen**, a bronze/stone lower collection trough and **four** load-bearing timber posts.

`frame-design.json` (schema 2) contains the canonical SOUTH 3×3×5 blueprint and one global (model-pixel) box list shared by all four static directions. It also contains the screen radius, wear variants and coarse/fines discharge details. `build-static.mjs` regenerates **136 direct V2 cell models**, **four rotated multiblock structures**, **six animated drum model JSON files**, the Voxel V2 profile and controller blockstate/item models. It removes obsolete old static templates and the tiny old two-model drum.

Do not hand-edit generated assets. Only edit `frame-design.json` and rebuild. The source-contract test checks dimensions, four facings, port clearance, 16px UV coverage, large open drum, Patchouli Visualize and processing chain.

```bash
node tools/rotary-classifier/build-static.mjs
node tools/rotary-classifier/build-static.mjs --check
node tools/rotary-classifier/test-contract.mjs
node tools/texture-art/build-kit.mjs --check
./gradlew build
```

**World migration:** Break the old 5×3×5 test machine *before* updating. The new formation blueprint is incompatible with the original test scaffold. Real Minecraft build and animation tests are pending.
