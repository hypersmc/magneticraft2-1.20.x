# Charcoal Retort V3.20 — Front Upper Header Connection

**Status:** Model implementation candidate in draft PR #11; Minecraft visual acceptance pending.

## Why this pass exists

V3.19 fixed the center vertical copper supports and the curved lower copper arch. The user then pointed out that the two neighboring front blocks are still visually disconnected: the upper copper beam stops within the center grille block rather than forming a single structural header across the three blocks.

## Structure and scope

- Minecraft Forge 1.20.1; Charcoal Retort south-local upper level `Y=2`.
- Keep existing blueprint, collision, gameplay, five grille slots, curved arch, left masonry, roof and chimney unchanged.
- Shared visual header height: **local Y=14.1..16**. Material: **cut copper**, with existing **dark iron brackets** left visible. Front depth **Z=16**.
- No dark full-width under-roof strip; this is an extension of the existing copper lintel, not a new decorative rail.

## SOUTH-local cuboid requirements

| Front block | Header geometry | Other changes |
|---|---|---|
| Left `part_-1_2_0` | Existing `[3,12.75,14.5]→[16,16,16]` | No authored geometry changes |
| Center `part_0_2_0` | Existing `[2.5,14.1,14.5]→[13.5,16,16]`, plus seam returns `[0,14.1,14.5]→[0.6,16,16]` and `[15.4,14.1,14.5]→[16,16,16]` | Preserve x0.6..2.5 and x13.5..15.4 iron brackets, five-slot grille, Y1-facing arch |
| Right `part_1_2_0` | New `[0,14.1,14.5]→[13,16,16]` | Split masonry behind the header at Y14.1 to avoid coplanar brick and copper front faces; outer riveted iron fitting remains |

## Acceptance tests

1. From a frontal camera, the copper structural header visibly runs left → center → right, with dark iron connectors rather than air gaps at both block seams.
2. Exactly **five** dark inset grille openings remain clear; the copper divider and iron end mounts are not overwritten.
3. The lower copper arch and vertical supports remain as in V3.19.
4. No repeated dark horizontal under-roof strip, texture stretching, floating geometry, coplanar z-fighting or visible roof changes.
5. All four orientations pass mesh geometry checks and 48px fascia continuity sampling.
6. Only accept the visual design after an in-game screenshot. Full `./gradlew build` remains outstanding for this model-only pass.

## Source of truth

- Generator: `tools/charcoal-retort/v3-models.mjs`
- Generated models: `src/main/resources/assets/magneticraft2/models/multiblock/charcoal_retort_*/part_*.json`
- Model checks: `tools/charcoal-retort/test-contract.mjs`
- Existing contract: `docs/bronze-charcoal-retort-contract.md`
