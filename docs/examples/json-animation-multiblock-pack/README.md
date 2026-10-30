# Magneticraft2 animation coverage test pack (Forge 1.20.1)

This is a **visual-only** client resource pack for PR #12. It proves the
four newly migrated mechanical multiblocks use the resource-pack-selected
model IDs rather than their previous hardcoded Java renderer references.

**Install:** Zip the **contents** of this directory (put `pack.mcmeta`
at the ZIP root), place the ZIP under `run/resourcepacks/` for the dev
instance, select it from Minecraft's resource pack menu, then press F3+T.

The pack deliberately leaves every mechanical track's original scale/axis
unchanged: it changes models only, not the Gear V2 simulation.

| Machine | Expected visible change | Mechanics unchanged |
| --- | --- | --- |
| Primitive Grinder | Grinding rotor changes from stone to gold | Input axle still drives +X; rotor still drives -Y |
| Mechanical Ore Washer | Some copper on the drum becomes gold | Belt and pulleys remain phase-locked, water/ore simulation unchanged |
| Bronze Stamp Mill | Steel on input drive shaft becomes gold | Two shaft phases, cams, stamps and leather belt stay synchronized |
| Rotary Ore Classifier | Middle drum section uses a gold mesh texture | Three drum pieces still rotate together and worn variants still activate |

The original models are inherited with `"parent": "magneticraft2:..."`;
no geometry is stretched or regenerated. The animation JSON resources select
these new model IDs under `magneticraft2:animated/*` (outside the old
`models/multiblock` discovery path), so this also tests pre-bake
registration of resource-pack-provided models.

### Important limitations

- The 4 additional machines are **new code on PR #12**. They need a real
  Forge build, F3+T reload and in-game checks before merging.
- **Only selected animated geometry has been migrated.** The Grinder's items,
  Washer's belt/fluid/items, Stamp Mill's hammer/cam motion/belt and the
  Classifier's ore contact simulation remain specialized Java visuals.
- Static formed models still use the existing legacy or V2 path. Nothing in
  this pack edits formation/recipe/collision JSON or mechanical power.
- The classifier's worn drum models intentionally override the base model at
  wear stages 2+; its gold middle drum is only expected in lower wear stages.
- Known Sifter frame clipping has been deferred from the animation PR.
- Do not enable this test pack alongside another pack overriding these same
  animation IDs without understanding pack precedence.

### Regressions to test

1. Form all four machines in supported orientations, at least one example of
   each facing for the Washer/Stamp Mill/Classifier.
2. Run with Gear V2 input; stop, reverse and resume. Rotation direction and
   connection points must be identical with and without this resource pack.
3. Disable/re-enable the pack, pressing F3+T. The gold details should toggle,
   while the machine's mechanical simulation and item/fluid behavior persist.
4. Make a malformed JSON copy (bad axis, unknown model or invalid version)
   and confirm the corresponding old Java rotating models render instead of
   disappearing or becoming a missing-model cube.
5. Inspect worn and fresh mesh stages. Confirm no stale baked model remains
   after replacement, resource reload or world rejoin.

Full machine inventory: `docs/json-animation-multiblock-coverage.md`.
Numerical pivot checks: `node tools/animation/check-mechanical-drives.mjs`.
