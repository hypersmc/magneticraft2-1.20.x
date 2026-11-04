# Charcoal Retort V3.38 — Integrated cast-iron lower furnace

**Magneticraft2 / Forge 1.20.1** | Draft PR #11 | `feature/bronze-age-foundry`

## Visual problem
V3.37 looked like a flat grate placed inside a copper-bordered opening instead of a heavy furnace door connected to the retort structure. This is a redesign of the ONE logical front-center Y1 block, not another overlay of disconnected geometry.

## Design and source changes
The `front&&x===0&&y===1` branch in `tools/charcoal-retort/v3-models.mjs` now builds a complete sealed cast-iron furnace face:
- Copper uprights at X1.05–2.95 and 13.05–14.95, Y1.10–16, share the accepted upper Y2 copper rib X-ranges. Iron feet seat the lower ends.
- One deep soot/refractory backing and a recessed dark iron subframe.
- A physically divided iron panel (Z14.63–15.08) around THREE open visual slots, at X4.20–5.65 / 7.25–8.75 / 10.35–11.80, Y5.15–10.05. The visible soot slot backing ends at Z14.62; it is 0.46 model pixels behind the panel and 1.30 behind the raised bezel. No solid iron front plane covers the openings.
- A wide, four-rail heavy iron bezel, X2.10–13.90 and Y2.25–13.25, outer depth Z15.92, with smaller recessed grey bevels.
- Dark iron socket beneath the already-approved stepped copper crown; the rejected horizontal copper sill does NOT return.
- Existing small right-hand joint, existing textures, no extra translucent parts.

Y1 is an unlit static model: controller-driven active-only flames in the door are NOT implemented. The Y0 controller's existing OFF/LIT model behavior is preserved.

## Safety and scope
Only four Y1 center directional model JSONs have their geometry redesigned. Four Y2 center and eight Y0 controller OFF/LIT models are regenerated from hidden-face partitioning, so 16 model JSON files change in the diff. Dense static front-view surface checks confirm existing Y2 and Y0 faces remain unchanged. All modified cuboids stay inside each block, their UVs remain 1:1 with face size, and face texture aliases are defined. The new V3.38 regression contract tests the three openings, front iron dividers, heavy bezel and two visible copper pillars.

### In-game verification checklist
- Inspect straight ahead and at a shallow diagonal with furnace OFF: wide iron door, 3 *actually recessed* dark slits, no floating bars or copper slab
- Inspect ON: lower controller fire remains correctly state-dependent; Y1 slits remain dark for now
- Check the Y1-to-Y2 copper/iron connection, Y0-to-Y1 transition, all four orientations, both side ports, and upper/rear walls
- Run `node tools/charcoal-retort/build.mjs --check`, `node tools/charcoal-retort/test-contract.mjs`, and `./gradlew build` in a local checkout

**Not yet verified:** local Node CLI test run, Forge build, in-game rendering or visual acceptance. Static geometry checks are not an in-game test.
