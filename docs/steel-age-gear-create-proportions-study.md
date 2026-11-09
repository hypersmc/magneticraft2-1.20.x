# Create cogwheel reference study — Magneticraft2 Steel III

**2026-11-09 · reference comparison and model contract; no model fix in this commit**

The player supplied two Create Blockbench JSONs (`cogwheel (1).json` and `large_cogwheel (1).json`) to investigate the persistent visible gap between Magneticraft2's side-by-side Steel Large Gears. These files are third-party reference material; do **not** commit or redistribute Create's source JSONs/textures. This study records measured dimensions only.

## Measured reference — raw uploaded Create JSON geometry

| Property | Create small cogwheel | Create large cogwheel |
| --- | ---: | ---: |
| Nominal primary gear-bar span | 18 model pixels (from -1 to 17) | 30 model pixels (from -7 to 23) |
| Maximum outward axis reach | 9 model pixels | 15 model pixels |
| Maximum radial corner reach | about 9.124 pixels | about 15.075 pixels |
| Main gear-bar thickness | 3 pixels (Y 6.5 to 9.5) | 2.95 pixels (Y 6.525 to 9.475) |
| Axle cross section | 4 × 4 pixels | 4 × 4 pixels |
| Inner gear-case core span | 12 × 12 (2 to 14) | 20 × 20 (-2 to 18) |
| Radial bar arrangement | 4 full-width bars, including ±45°, giving 8 tips | 8 full-width bars including ±22.5°/45°, giving 16 tips |
| Texture-space declaration | 32 × 32 | 32 × 32 |
| Parent | `create:block/large_wheels` | `create:block/large_wheels` |

These are geometry measurements, **not proof of mechanical tooth contact**. The actual toothed silhouette also depends on Create's referenced 32×32 textures and resource parent; those textures were not part of the uploads. The JSONs contain NeoForge/Blockbench metadata and cannot simply replace Magneticraft2's Forge 1.20.1 assets.

## Existing Magneticraft2 measurements and gameplay constraints

- Current `wrought_iron_gear_medium`: 8 teeth; outer radial corner extent ≈ **8.30 px**; axle **2×2 px**. Steel Medium inherits this model.
- Current `wrought_iron_gear_large`: 16 teeth; radial corner extent ≈ **16.29 px** following the earlier extension to 16.25 axial tip reach; axle **2×2 px**. Steel Large inherits this model.
- Gear V2 `GearPlacementValidator` permits **medium/medium parallel = 16 px centre spacing**, **large/large parallel = 32 px**, and **medium/large diagonal = 16√2 ≈22.627 px**. Existing support for parallel large/large is *intentional* and must not be removed without an explicit progression/gameplay decision.
- **Important mismatch with Create:** Create's documented large/large external mesh is **perpendicular axes**, not side-by-side parallel large/large. Its 30-pixel diameter at 32-pixel parallel spacing would leave at least **≈1.85 pixels radial-envelope gap** before tooth phase is considered. Create is therefore **not** a solved model for the player-reported Magneticraft2 arrangement. See [Create's official cogwheel usage wiki](https://github.com/Creators-of-Create/Create/wiki/Cogwheel).
- A strictly consistent same-module pitch with 8 and 16 teeth and radii 8/16 px requires mixed centre distance **24 px**. The existing diagonal cell spacing is only **22.627 px**, a **1.373-pixel compression**. Exact equal-module involute meshing across *all three* allowed same-plane pair arrangements is not geometrically achievable with the current integer-grid centres. We can pursue a visually believable low-poly approximation, but must not claim mathematical precision or silently rewrite the placement rule.
- The previous radius-only Steel/Iron large-tooth extension is **not an accepted visual fix**. The player's later screenshot still shows the two large gears failing to interlock. Radial-envelope overlap alone does not imply contact between correctly phased tooth faces.

## What to adopt from Create; what NOT to copy

**Adopt as proportion guides (not copied assets):**
- One consistent eight-tip medium gear and sixteen-tip large gear design vocabulary; coordinate tooth angular spacing with Gear V2's 8/16 tooth-count and phase calculations.
- Compared core size and axial thickness: a medium gear around 12-pixel core span and a larger ring around 20-pixel core span can guide MGC2 Studio's own distinctive Industrial Iron/Steel silhouettes.
- A coherent, intentionally drawn root/rim-to-tooth transition rather than extending isolated outer boxes until they barely reach.
- Main gear thickness near 3 pixels **only if the existing shaft, bearing, support and animated collision paths permit it**; the old 2×2 Metal drivetrain axle remains a deliberate Magneticraft2 connection standard. Do not silently replace it with Create's 4×4 axle.

**Do not adopt literally:**
- The Create large gear's 15-pixel reach for our parallel large/large connection: it is *smaller* than the already failing Magneticraft2 profile.
- Create's block models, textures, atlases, artistic details, or NeoForge data as shipped Magneticraft2 resources. Use **original models** and native UV mapping; model texturing belongs to MGC2 Studio.
- A renderer translation or hidden/overlapping rim that fakes connection but misaligns the gear centre/shaft.
- A gameplay rule deleting parallel large/large connectivity just because Create doesn't support it.

## Coordinated model-redesign specification (next asset implementation)

1. **Work on medium and large as a pair**, not another isolated large-tooth extension. Keep centre `[8,8,8]`, 8/16 phase pattern, existing 2×2 supported axle, gear/shaft bearings and mechanical ratios.
2. Construct an **original low-poly tooth profile** with deliberate root, flank, tip and gap. Target appropriate tangential width and small rotational clearance as well as radial reach. Merely making the outer bounding radius >16 pixels is insufficient, as demonstrated by the player screenshot.
3. Validate all three *existing* parallel-axis connections before shipping: medium/medium at 16 px, medium/large diagonal at 22.627 px, large/large at 32 px. A candidate that visually fixes large/large but causes medium/large intersections **fails acceptance**.
4. Evaluate the full tooth-mesh period with the real `GearNetworkManager.calculateMeshedPhaseDegrees` and `lockDrivenGearRotations` rules. Test both traversal directions, shaft-driven phase, start/stop and mixed Iron/Steel materials. Check model-space overlaps/signed clearances over dozens of angular steps, then actually verify in-game.
5. Preserve UV consistency: Magneticraft2's textures use its existing material files and 16-pixel block-model conventions. Never stretch a texture to fit a larger tooth. Model parent inheritance must remain stable and asset reloads must resolve correct textures.
6. If **no single profile** can plausibly satisfy every supported grid pairing, return with an explicit proposed choice (specialized tooth profile/material, placement-rule revision with save compatibility implications, or relaxed visual engineering tolerance), rather than pushing offsets or disabling validation.
7. MGC2 Studio owns final Steel/Iron model appearance. The mod code stays authoritative for torque, RPM, mesh connections and collision. A model redesign is not player-accepted until the gap is visibly gone, with no side/diagonal clipping.

## Reproduce the measurements

The repository contains `tools/steel-age/compare-create-gear-proportions.mjs`; run it from a repository checkout with the **user-supplied** Create JSONs (they are deliberately not stored here):

```bash
node tools/steel-age/compare-create-gear-proportions.mjs \
  "/local/path/cogwheel (1).json" \
  "/local/path/large_cogwheel (1).json"
```

The inspector prints planar radial extrema for the uploaded models and the current Magneticraft2 models. Its radial extent is only a necessary first diagnostic, **not a full animated tooth collision test**.

**Acceptance status:** Steel III implementation works according to player tests but the side-by-side large gear visual gap is **still open**. No mesh or model resources were changed in this reference-study commit.
