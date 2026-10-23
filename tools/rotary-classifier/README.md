# Rotary Ore Classifier — compact 3×3×5 redesign

After screenshots rejected the 5×3×5 scaffolding prototype, the authoritative model was rebuilt as an **open ~22 px-diameter rotating screen**, a bronze/stone lower collection trough and **four** load-bearing timber posts.

`frame-design.json` (schema 5) contains the canonical SOUTH 3×3×5 blueprint and one global (model-pixel) box list shared by all four static directions. It also contains the screen radius and wear variants. Fake receptacle meshes have deliberately been removed: real input and both outputs are the ONLY places to interact or attach automation. `build-static.mjs` regenerates **112 direct V2 cell models**, **four rotated multiblock structures**, **six animated drum model JSON files**, the Voxel V2 profile and controller blockstate/item models. It removes obsolete old static templates and the tiny old two-model drum.

Do not hand-edit generated assets. Only edit `frame-design.json` and rebuild. The generator rejects model/collision obstruction of U/O/Y modules or the entire cell above them, and rejects any old false hopper/output-tray shapes. The source-contract test checks dimensions, four facings, exposed module targets, no fake bins, 16px UV coverage, **grounded geometry connectivity**, the built-in Gear V2 drive housing, exterior roller supports at both ends, an unobstructed open discharge mouth, Patchouli Visualize and the unchanged processing chain.

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

## V5 — remove redundant rear-bearing frame (2026-10-23)

A follow-up Minecraft screenshot showed **long dark stationary metal arms across the exposed rear drum mouth** despite the V4 support pass. The source audit explains the conflict: the genuine `mechanical_input_module` already renders and rotates its own copper bearing and axial shaft in exactly the rear-center cell. The V4 model also constructed a second smaller fixed rear bearing and used two long iron struts to attach it to the posts. This duplicate bearing was unnecessary and visually interfered with the rotating cage.

V5 removes **all five** static `rear_bearing_*` pieces and **both** long `rear_bearing_mount_*` struts. Instead, two bronze-machined roller contacts ride on the **outside** of the rear octagonal hoop and connect to the existing grounded rear rollers; the front remains similarly roller-supported with an open discharge mouth. The short supported metal feed throat still meets the real left-rear Item Input. The rear screen retains its 2.1-pixel clearance for the feed throat and visible rotating hub. The center drive opening is explicitly kept free of static geometry.

No change to the 3×3×5 formation blueprint, real port locations, automation, recipes, materials, screen maintenance or Mechanical Input module Java. Generator and regression tests reject duplicate internal stationary bearings, clipping into the axial drive opening and any roller that fails to meet the outer rim. Run the current checks and inspect both ends in Minecraft before accepting the visual work.

## Material-flow animation (2026-10-23)

The new client-side classifier visuals use the *real* Item Input and both extract-only Item Output modules as their only stable item previews. Input top = Y 1.97; output tops = Y 0.665 relative to the formed controller. Running ore feeds through the inlet, tumbles (bounded to two small in-drum fragments), sheds intermittent fines onto the internal pan and exposes the coarse fraction near the open outlet. Final products are only shown once stored in slots 1 and 2. Pausing with full outputs or insufficient mechanical power freezes server-synced batch progress and keeps the in-drum workpiece in place. A lightweight client ticker emits at most one dust/gravel particle opportunity per 10 ticks and one muted gravel rattle per 40 ticks, only nearby and only while truly processing. Particle coordinates rotate with all four multiblock facings. Do not add particle creation to the BER or new server-side items. Avoid a per-frame recipe lookup; the active input count is cached for preview purposes.
