# Charcoal Retort V3.25 — Copper capping on both side walls

**Draft PR:** #11 — `feature/bronze-age-foundry`. **Visual candidate:** in-game approval pending.

The user reported missing copper detailing directly above the recessed/raised upper right side wall and suggested the left side was probably missing it too. Model inspection confirmed neither side had a cut-copper cap at that elevation.

The fix adds a narrow continuous **cut-copper cap and an upper lip** on both upper (Y2) walls, over three cells front-to-rear. SOUTH-local pixel coordinates: right X15.15..16, left X0..0.85, Y12.45..13.35; lips at right X15.6..16, left X0..0.4, Y13.35..13.65. Middle/rear Z0..16; front corners stop at Z13 (right) or Z13.75 (left), avoiding the approved square iron mounts. This is **not** a return to the broad dark horizontal bands.

Only 24 Y2 models change (3 depth cells x 2 sides x 4 rotations). The accepted roof/chimney, front outward surfaces, recipes, service ports, multiblock layout and gameplay are unchanged; the left side's masonry is not otherwise redesigned.

The V3.23/V3.24 tests now inspect the top masonry course above the intentional copper overlap, and V3.25 adds exposed copper cap/lip checks for all 24 side cells. In-memory generation and face partitioning completed for four rotations, UVs retain proportional pixels, blocks stay within 0..16 and 0 sampled visible front-face changes were found.

**Still required:** actual Node command checks, `./gradlew build`, and in-game visual review. Inspect left side, right side and both three-quarter front corners for continuous copper without seam gaps or clipping.
