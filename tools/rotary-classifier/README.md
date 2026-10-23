# Rotary Ore Classifier — compact 3×3×5 redesign

After screenshots rejected the 5×3×5 scaffolding prototype, the authoritative model was rebuilt as an **open ~22 px-diameter rotating screen**, a bronze/stone lower collection trough and **four** load-bearing timber posts.

`frame-design.json` (schema 4) contains the canonical SOUTH 3×3×5 blueprint and one global (model-pixel) box list shared by all four static directions. It also contains the screen radius and wear variants. Fake receptacle meshes have deliberately been removed: real input and both outputs are the ONLY places to interact or attach automation. `build-static.mjs` regenerates **112 direct V2 cell models**, **four rotated multiblock structures**, **six animated drum model JSON files**, the Voxel V2 profile and controller blockstate/item models. It removes obsolete old static templates and the tiny old two-model drum.

Do not hand-edit generated assets. Only edit `frame-design.json` and rebuild. The generator rejects model/collision obstruction of U/O/Y modules or the entire cell above them, and rejects any old false hopper/output-tray shapes. The source-contract test checks dimensions, four facings, exposed module targets, no fake bins, 16px UV coverage, **grounded geometry connectivity**, rear-bearings/feed supports, a front rim on roller guides, an unobstructed open discharge mouth, Patchouli Visualize and the unchanged processing chain.

```bash
node tools/rotary-classifier/build-static.mjs
node tools/rotary-classifier/build-static.mjs --check
node tools/rotary-classifier/test-contract.mjs
node tools/texture-art/build-kit.mjs --check
./gradlew build
```

**World migration:** Break the old 5×3×5 or previous 3×3×5 machine (with the Cut Copper cap over Input) *before* updating. The new formation blueprint is incompatible with the original test scaffold. Real Minecraft build and animation tests are pending.

## Visual support pass — 2026-10-23

The rear copper bearing and intake were detached from the four-post frame; the old front rim and shaft stopped just beyond the front roller cradles. Source schema 4 now adds slim iron mounts joining each rear axle bearing to its timber post, braces the actual feed neck from the left rear post, and replaces the former solid tongue with a short **open-sided hollow sleeve**. The rear rotating mesh begins 2.1 model pixels after the stationary bearing brackets to avoid clipping. The front axle is removed **only from the front drum section**, leaving the discharge end open, with two bronze rollers that actually meet the moving outer hoop. Very short *attached* fine/coarse runoff guides terminate just before the original interactive item output blocks; they own no inventory or block collision.

All fixed source model boxes must be connected through touching geometry to the stone rails and all real input/output cells (and the full cell above each item port) remain empty of V2 decorations. No formation layout or recipes change in this pass. Run `node tools/rotary-classifier/build-static.mjs --check`, `node tools/rotary-classifier/test-contract.mjs`, `./gradlew build` and check all camera angles in-game before considering the visuals accepted.
