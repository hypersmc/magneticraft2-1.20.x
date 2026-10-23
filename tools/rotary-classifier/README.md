# Rotary Ore Classifier — compact 3×3×5 redesign

After screenshots rejected the 5×3×5 scaffolding prototype, the authoritative model was rebuilt as an **open ~22 px-diameter rotating screen**, a bronze/stone lower collection trough and **four** load-bearing timber posts.

`frame-design.json` (schema 3) contains the canonical SOUTH 3×3×5 blueprint and one global (model-pixel) box list shared by all four static directions. It also contains the screen radius and wear variants. Fake receptacle meshes have deliberately been removed: real input and both outputs are the ONLY places to interact or attach automation. `build-static.mjs` regenerates **112 direct V2 cell models**, **four rotated multiblock structures**, **six animated drum model JSON files**, the Voxel V2 profile and controller blockstate/item models. It removes obsolete old static templates and the tiny old two-model drum.

Do not hand-edit generated assets. Only edit `frame-design.json` and rebuild. The generator rejects model/collision obstruction of U/O/Y modules or the entire cell above them, and rejects any old false hopper/output-tray shapes. The source-contract test checks dimensions, four facings, exposed module targets, no fake bins, 16px UV coverage, large open drum, Patchouli Visualize and processing chain.

```bash
node tools/rotary-classifier/build-static.mjs
node tools/rotary-classifier/build-static.mjs --check
node tools/rotary-classifier/test-contract.mjs
node tools/texture-art/build-kit.mjs --check
./gradlew build
```

**World migration:** Break the old 5×3×5 or previous 3×3×5 machine (with the Cut Copper cap over Input) *before* updating. The new formation blueprint is incompatible with the original test scaffold. Real Minecraft build and animation tests are pending.
