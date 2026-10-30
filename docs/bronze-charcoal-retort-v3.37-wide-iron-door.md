# Charcoal Retort V3.37 — Wide iron furnace face

**Magneticraft2 / Forge 1.20.1**  
**Draft PR:** #11, branch `feature/bronze-age-foundry`  
**Status:** Model candidate pending Minecraft visual acceptance.

## Feedback addressed
The user's front screenshot of V3.36 showed a tall, narrow open grate surrounded by too much copper. The supplied reference instead has a large square dark-iron furnace door, three comparatively short deep vertical openings, and copper as structural support at the *edges* only.

## Changes
The Y1 front-center block `part_0_1_0.json` is redesigned; the generator `tools/charcoal-retort/v3-models.mjs` retains its existing Y2 stepped hood, all machine logic, and Y0 controller.

- Rebuild the 16px lower-center with a **broad 11.8px-wide, four-rail dark iron bezel** (X2.1–13.9; Y2.2–13.3; outer depth Z15.96).
- Enclose a recessed iron panel, rather than using three long exposed iron bars. **Three short dark slits** are X4.05–5.55, 7.25–8.75, 10.45–11.95 at Y5.15–10.05, recessed to Z15.60 behind the bezel. The surrounding solid iron is deeper at Z15.47.
- Preserve a small right-hand latch and a dark transition beneath the approved Y2 copper crown.
- Narrow both copper support columns to X0.75–2.1 and X13.9–15.25, Y1.4–15.5, front at Z15.9. The original structural brick backing is *selectively recessed in this one Y1 center cell* to make those copper pillars truly visible; the Y2 brick chamber is not redesigned.
- Do not add a new horizontal copper sill or constant orange flame texture. The real lit state currently belongs to the Y0 controller; an **active-only** glowing Y1 door requires separately wiring that state into its rendering. This revision solves door proportions/shape, not controller-driven Y1 glow.

## Scope and regression checks
The geometry redesign is confined to one logical Y1 block, regenerated for SOUTH/WEST/NORTH/EAST. Re-culling shared hidden faces also regenerates the four Y0 controller meshes (plus their `_lit` variants) and four neighboring Y2 models, 16 JSON files total. All four model rotations were inspected with visible surface/material probes, and no exposed Y0/Y2 front faces changed in dense static sample comparisons. All generated cuboids stay inside 0..16 and face UVs are proportional; aliased textures resolve. New V3.37 tests supersede V3.36 narrow-grille expectations.

**Not run here:** local `./gradlew build`, standalone Node CLI tests and in-game screenshot verification. Test the wider door from straight in front and from a shallow diagonal; verify three dark slots, slimmer copper framing, and an unchanged upper stepped apron. Compare the layout to the reference without expecting orange glow before active-only rendering is implemented.
