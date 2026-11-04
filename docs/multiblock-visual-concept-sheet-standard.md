# Multiblock visual concept sheet — required project deliverable

**Decision (2026-11-01):** Every new substantial Magneticraft2 multiblock should receive a **single polished, visually rich reference infographic**, in the style of the user's **Ore Roasting Kiln** example. This is required alongside the working implementation, JSON blueprint, Patchouli guide, and editable geometry/Studio reference. It is **not** satisfied by a lone Blockbench model, bare orthographic SVG, isolated render, or text-only design contract.

## Requested concept-sheet composition

1. Magneticraft2 identity, machine name and technological Age.
2. One dominant **Minecraft-style 3D hero illustration** with recognizable materials, believable mechanical support and actual machine proportions.
3. An accurate **multiblock structure overview**: dimensions (width × height × depth), block/material legend, important modules and layer patterns based on the actual JSON blueprint. If the image is illustrative rather than an exact block-for-block diagram, label it **concept only**, and direct the builder to Patchouli's canonical layout.
4. Small **front, side, rear and internal-mechanism views** of the same machine design (not four unrelated designs).
5. **Input → process → output** examples, with real recipes and byproducts; distinguish provisional future recipes from implemented ones.
6. The **operation sequence** and ports for power, items, fluids, HEAT or pressure where they actually exist.
7. **Visual states** such as idle, operating and output-blocked, appropriate to the actual machine.
8. A compact specification panel with real footprint, processing time, power demand (RPM/torque or other relevant units), wear-part lifetime and output count. Clearly mark proposals versus implemented data.

## Accuracy and modeling rules

- Use the existing branch's current source, actual blueprint and machine contract as source of truth. **Never copy numerical values from an unrelated reference poster**, invent gear connectivity, materials, chimneys, heat effects, processing ratios or extra mechanical ports.
- One coherent machine across every view. Gear, blade, shaft, belt and bearing axes must be plausibly connected, centered, and supported. Avoid floating mechanisms.
- Preserve Magneticraft2's Minecraft aesthetic and block proportions. Concept art should communicate how to build a future Studio replacement, not silently redefine the functional multiblock.
- Separate **visual concept** from an **editable 3D Studio sample model**. Provide both when requested; their purposes are different. The concept sheet is a high-quality visual target, not necessarily an importable 3D geometry source.
- Validate wording and numeric labels against implemented code; when a detail is unknown, omit it or explicitly label it conceptual.
- Store the image asset/reference with the machine's design materials where practical, and document its status; do not imply that a generated concept sheet has passed visual, build or game testing.

## Existing example and Sawmill milestone

The user supplied a full-page **Ore Roasting Kiln** infographic on 2026-11-01 as the exact desired **presentation format**. The Mechanical Sawmill K1 concept infographic generated afterward illustrates the presentation approach, but is **visual concept art**, not a verification of the detailed K1 model or layout. Its extra decorative geometry should not be assumed implemented.

**User in-game feedback (2026-11-01):** “the mb works” for the Mechanical Sawmill K1. Record that the **multiblock formation** has user confirmation. Do not infer that powered cutting, blade wear, item automation, save/reload or visual model quality have been verified from that single statement. PR #11 remains draft until further acceptance.
