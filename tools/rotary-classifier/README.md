# Rotary Ore Classifier source and test contract

`frame-design.json` is the editable static-frame source; `build-static.mjs` rebuilds the 251 per-facing static V2 models (48 shared local templates, 200 block-local model wrappers, controller blockstate/model/item). **Do not edit generated part JSON by hand.**

`test-contract.mjs` checks all four 5×3×5 blueprints, rotated module locations and V2 render AIR anchors, 50 static model hosts per facing, valid material/UV coordinates, two explicit screen models with transparent original textures, recipe conservation and the Washer/Sifter handoff, machine registrations, mining tags, JEI catalyst and guidebook.

```bash
node tools/rotary-classifier/test-contract.mjs
node tools/rotary-classifier/build-static.mjs --check
node tools/texture-art/build-kit.mjs --check
./gradlew build
```

These source checks do **not** prove Minecraft/Forge compilation or in-game correctness; see `docs/bronze-rotary-classifier-contract.md` for the gameplay test matrix.
