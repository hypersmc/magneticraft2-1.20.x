# Charcoal Retort V3 — block-by-block visual design specification

**Status:** proposed visual direction following the 2026-10-24 concept image and user gameplay video `2026-10-24_14-04-52.mp4`. **No V3 models, gameplay code, blueprints, or collision geometry changed by this document.** V2 is not visually accepted. The goal is to obtain agreement on a model that resembles the concept rather than implementing another generic brick/copper box.

> **User clarification (2026-10-24):** the existing **3×4×3 real block layout is correct and must not be moved**. The observed fault is rendered models/textures intersecting or visually landing inside other models. This is a **geometry/face-placement repair**, not a request to change construction footprint or relocate modules. Resolve V2 generator overlap and port-model fit **before** any optional aesthetic redesign.

**Confirmed V2 geometry defect:** `tools/charcoal-retort/build.mjs` draws the front stone foundation using `[0,0,0]..[16,12,16]`, then draws front metal band `[0,1,15.4]..[16,3,16]` through its volume in the same block cell. Similar front bands and corners create competing geometry. Correct by subtracting/decomposing opaque solids into non-intersecting volumes, omitting concealed/copied faces (including coplanar faces), and checking the actual module model geometry. A simple arbitrary depth offset only hides the error and can introduce new clipping. Preserve the working `U/O/Y` modules and their real positions.

**Required first proof:** screenshot/video before/after of the identical SOUTH build, with arrows' lower front band, left/right port transitions and upper front vent inspected, then verify WEST/NORTH/EAST. Do not infer that a nicer texture pack fixes geometry.

## Non-negotiable constraints

- Forge 1.20.1; the JSON-formed machine remains **3 wide x 4 high x 3 deep** in four orientations. No changes to the actual construction recipe, Patchouli formation pattern, formation controller, material process, or output quantities.
- Keep the three genuine item-port block entities and their capabilities/selection: input at front-left, charcoal output front-right, tar output at rear centre. Do **not** turn them into fake baked storage props.
- Preserve `AIR` centre throat cells and the actual chimney AIR proxy; only the already-designated roof AIR cell hosts a filler. Nothing may obstruct formation or the three working ports.
- No z-fighting, no visible parts embedded into other blocks, no texture stretching/compression, no double facing rotation, no huge per-tick renderer work.
- Model in **16x16 Minecraft pixel proportions**. A detail belongs to one cell unless a dedicated controller/port renderer is explicitly validated. Keep collision distinct from visual trim.

## Fixed coordinate / structure contract

SOUTH-facing **controller origin = (0,0,0)**. X negative = left when viewing the front from the south; Z negative = toward the rear; Y increases upward. Each grid character is one *real construction block*, not decorative geometry. The book currently reverses row columns for its Patchouli projection and must continue to be tested against this physical arrangement.

```text
                REAR (z=-2)             FRONT (z=0)

Y=3 roof       P P P       P A P       P P P
Y=2 chamber    B B B       B A B       B B B
Y=1 service    B Y B       B A B       U B O
Y=0 plinth     S S S       S S S       S C S
```

More explicitly (each row rear to front):

```text
Y=3   PPP / PAP / PPP
Y=2   BBB / BAB / BBB
Y=1   BYB / BAB / UBO
Y=0   SSS / SSS / SCS
```

`S` stone bricks; `C` Charcoal Retort controller; `B` brick; `P` cut copper; `A` required air; `U` *real* log Input; `O` *real* charcoal Primary Output; `Y` *real* Wood Tar Byproduct Output. Coordinates of special cells: `C=(0,0,0)`, `U=(-1,1,0)`, `O=(1,1,0)`, `Y=(0,1,-2)`, `AIR=(0,1,-1),(0,2,-1),(0,3,-1)`.

## Design objective / silhouette

The target reference is a **heavy masonry kiln with a copper protective roof**, not a copper box decorated with brick textures. It should have a readable dark plinth; 2-block-high reddish refractory kiln body; one **central, recessed service door**; a restrained **high ventilation grille**; two integrated but clearly different real front ports; an open, strengthened copper chimney. From a distance the eye should find **main kiln / hearth / roof**, not crisscrossing trim.

The static art must make sense with the fire **off**; when lit, only the firebox slit/window glows and light smoke appears above the roof. No pretend gear/shaft or pressure system. Approximate visual weight: 55–60% masonry, 20% dark stone/iron plinth, 15–20% copper roof, under 10% additional trim, excluding real port faces.

## Block-local visual specification (SOUTH canonical)

All dimensions below are **local model pixels [0,16]**, with each baked element staying in its own cell. Distances are design targets, subject to UV/collision validation, not finished geometry coordinates.

### Y=0 — Stone plinth (all S, plus controller C)

- `(-1,0,-2),(0,0,-2),(1,0,-2)`: three continuous rear stone-footing cells; separate 16x16 stone courses with consistent mortar at seams. One strong foundation profile, not a copper-clad cube.
- `(-1,0,-1),(0,0,-1),(1,0,-1)`: solid foundation beneath the sealed chamber; dark stone with only a thin iron course at the *outer* sides. No internal faces drawn through adjacent foundation cells.
- `(-1,0,0)` and `(1,0,0)`: left/right front buttresses on the same stone level. A low lintel/footplate visually **supports** each port directly above rather than looking like a detached hopper.
- `C=(0,0,0)`: retort controller and **lower service/firebox**. Dark stone frame with a small inset black rectangular grate and one functional ember window. Target aperture roughly x=3..13, y=4..12, recessed z≈11..13 behind a forward sill at z≈15. No texture-plane overlays across the controller's front; show on/off with distinct model state. Keep the block clickable.

### Y=1 — Main sealed working chamber (bricks B, three real ports)

- `(-1,1,-2),(1,1,-2)`: rear masonry pillars around the **real tar port**. No decorative mask may hide or disable it; keep an obvious access gap behind the machine.
- `(-1,1,-1),(1,1,-1)`: full-thickness side chamber walls with deep refractory texture. Minimal external iron waist strap at one intentional height only; no repeating strips drawn across every cube seam.
- `(0,1,-1)=AIR`: unobstructed central throat; **no** hidden brick block or fake internal volume.
- `B=(0,1,0)`: **main front sealed retort hatch**, centered one block *above* the controller. Inset dark panel with thick masonry jambs and bronze/copper hinge+handle details. Aim for a ~10x11-pixel opening within this 16x16 local face. It must read as the focal point, not a tiny secondary grille or ordinary furnace face. Only one continuous bezel owns the visible plane.
- `U=(-1,1,0)`: **real log input**, visually a feed hopper/hatch connected to the buttress below. Its *formed-state model* is a block-local framing/skin of the real port, not an inserted fake inventory. Face should be visibly labelled by shape/contents as incoming logs.
- `O=(1,1,0)`: **real charcoal output**, visibly a small recovery drawer/tray, with a dark receptacle opening. Same real entity and ability to extract using item automation. Do not make both front ports identical yellow boxes; differentiate their silhouettes and material.
- `Y=(0,1,-2)`: rear tar collection port stays legible, with a restrained pipe/drain fitting **on the port itself**. No pipe crossing into an inaccessible fake tank or blocking access to its output.

### Y=2 — Refractory body and high vent

- `(-1,2,-2),(0,2,-2),(1,2,-2)`: heavy back masonry and a subtle black iron support at *actual corner cells*.
- `(-1,2,-1),(1,2,-1)`: paired side wall cells. Reuse the same tileable masonry with continuous joints from Y=1; avoid disconnected copper vertical poles.
- `(0,2,-1)=AIR`: leave genuinely empty.
- `(-1,2,0),(1,2,0)`: strong front chamber shoulders. One narrow structural cap line across the upper edge, not a waist-high stripe intersecting the hatch.
- `B=(0,2,0)`: **small upper ventilation grille**, distinctly different and *smaller* than the main door. Target a narrow slit row inset in brick (about x=4..12, y=6..10), darkened internally. It must look connected to the kiln, without overlapping coplanar brick or side buttresses.

### Y=3 — Eight copper roof construction blocks + one AIR chimney

- All eight `P` positions: layered copper roof cap over refractory edge; two pixel-scale height levels (e.g. base y=0..7, cap y=7..12), properly connected as one protective lid. Use seams/fasteners only where they serve a believable join; no railway-track stripes or flat full-height red copper cubes.
- Four corner `P` cells: slightly heavier edge returns and dark brackets, kept entirely within local cell bounds. Adjacent edges share a single *continuous design*, not coincident faces at the seam.
- `A=(0,3,-1)`: the **only** Voxel V2 roof AIR host. Four hollow chimney walls with a truly open central throat; dark inner soot, small copper lip and minimal side vent slits. No solid central cap or opaque proxy cube. Particle source must remain aligned with this rotated roof position.
- The outside top should reveal the chimney from a normal slightly elevated camera, not show a flush 3x3 copper carpet.

## Specific fix for V2 clashes and z-fighting

The user video shows protruding port components and dense overlapping bands. Distinguish **coplanar z-fighting** from genuine **interpenetrating geometry**; neither can be solved by blindly shifting every slab outward.

1. **One visible face owner.** Each face is either masonry, a recess wall, an actual port model, or an intentional non-overlapping decorative bar. Do not draw multiple faces in the same plane (including hidden solid-cube surfaces behind inset geometry).
2. **Avoid internal cube faces.** Remove faces concealed by neighboring parts if generating subdivided solids, and prefer face-culling/whole orthogonal solids where meaningful.
3. **No geometry in real port cells from ordinary filler assets** `U/O/Y`. A deliberately opted-in formed port model may be rendered *by that module* but must not duplicate the original port's visible surfaces.
4. **Port integration implementation path:** extend the existing `MultiblockItemPortBlockEntity`/render model data or a precisely-scoped rendering hook to choose a **Charcoal Retort formed-state model** only while the module belongs to this formed controller. Unformed and other machines retain existing generic port rendering. Do **not** just overlay an opaque box over the old port face: replace/suppress the conflicting visible quads and preserve access/selection/capabilities. An optional fallback is an external collar occupying only proven free material around the original aperture.
5. **Coordinate discipline:** all baked element coordinates stay within `0..16`, and never cover an adjacent module; >16 extrusion requires a separate explicit and collision-validated design, not a hidden "quick fix."
6. **UV discipline:** standard 16x16 pixellated textures with stable native orientation. Crop UVs to physical cuboid surfaces; do not stretch a full tile over a narrow bar or reset mortar lines on each tiny section.
7. **Model/voxel parity:** regenerate Voxel V2 collision from coarse *physical surfaces only*. Decorative engraving and slim trim should usually have no independent collision. No invisible full-cube collision in the chimney or recessed access panel.
8. **Four-way rotation:** use the SOUTH canonical geometry and rotate once into north/east/west parts; formed blockstates must not rotate those already-rotated parts a second time. Confirm real ports and front/back remain on the same side as the blueprint.
9. **No avoidable FPS costs:** pre-baked static model parts for stationary brick/roof/trim. No per-frame mesh regeneration; only state-driven fire/glow and low-frequency particles.

## Acceptance checklist before considering V3 visually accepted

- First compare **front three-quarter**, front orthogonal, both sides, back, and top images against the single agreed concept; each view should communicate the same machine.
- No texture tearing, z-fighting shimmer, double side-trim or pieces sitting inside/outside real module apertures while camera moves (including the user's close-up video test).
- Feed logs at the front-left **real port**, remove charcoal from front-right **real port**, remove tar at rear-centre **real port**. Verify all three can be targeted and automated.
- Main kiln door is larger/more legible than the high vent; copper roof/chimney are visually stronger than thin copper straps; dark foundation connects the whole build.
- `node tools/charcoal-retort/build.mjs --check`, `node tools/charcoal-retort/test-contract.mjs`, and `./gradlew build`. Do not claim any pass until actually run.
- Form in all four directions, test fire on/off, smoke originating inside the roof vent, breaks/drops, blocked outputs, reload/persistence and FPS with several machines in view.
- Unform and re-form pre-V3 instances to refresh any changed Voxel V2 snapshots; construction layout must stay compatible.
- Do **not** touch Bronze Retort recipes, energy/fuel balance, JEI behavior, or other machine renderer assets as part of the art-only V3 pass unless an independently explained defect is discovered.

## Implementation order after design agreement

1. Review the current generic port geometry, the real formed-state model/BE hooks and the Water Settling Tank's module insert approach. Establish face ownership, model layer and render bounding-box behavior.
2. Replace the monolithic **Y=1 / Y=2 refractory shell** and hatch/vent front with a simplified composition. Temporarily leave roof/ports visually generic.
3. Integrate U/O/Y through targeted **formed-state port models**; verify player and arm/hopper interactions and that the underlying entities remain untouched.
4. Add compact copper roof, open chimney, and short physically coherent details. Regenerate collision from authoritative shapes.
5. Introduce or rework a handful of 16x16 textures and inspect each face at normal Minecraft render distance, not just in an isolated concept.
6. Run contract/build checks, compare in-game images and footage with target, and iterate **before** marking visuals approved.

**This document is a design contract and task plan, not an assertion that V3 has been implemented or accepted.**
