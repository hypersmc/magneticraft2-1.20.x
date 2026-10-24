# Charcoal Retort V3.29 — Center copper apron and iron drip-edge

**Magneticraft2 / Forge 1.20.1** · Draft PR #11 · Branch `feature/bronze-age-foundry` · **Visual candidate**

## User reference and issue
After V3.28, the front center above the lower hatch is missing the small shaped copper apron visible in the user's close-up reference: two dark iron inset accents directly under the copper profile, and a narrow dark iron horizontal band along the bottom. The existing seven-segment copper arch already provides the upper contour and must be preserved.

## Isolated generator change
The only source edit is in `tools/charcoal-retort/v3-models.mjs`, within the **front-center, upper Y2** `front&&x===0` branch, immediately after the original `archSegments` loop.

In canonical SOUTH 16px model coordinates:
- Local dark iron underside: X3.2..12.8, Y0.10..0.62, Z15.45..16.
- Projecting copper bottom sill: X3.35..12.65, Y0.62..1.02, Z15.55..16.
- Two dark iron accents recessed 0.12px from the outside: X5.2..6.4 and X9.6..10.8, Y1.02..1.58, Z15.35..15.88.
- Six small copper cheek sections between and outside those accents, Y1.02..1.58, reaching Z16.
- Two short central copper-apron steps X6.8..9.2, Y1.58..1.94 and Y1.94..2.10, leading into the already accepted rising arch.

The existing center vent/grille, raised masonry, copper arch segments, front side columns and dark mounting feet are untouched. The hood is a static model detail, with no functionality changes or genuine passage through the machine.

## Diff / static verification
Only four rotated versions of `part_0_2_0.json` are regenerated; no left/right, roof, Y1 hatch, formation, collision, machine behavior or recipe files change. In-memory generation/partitioning passes. Ten intended surface sample points per orientation, 256 unchanged upper/masonry sample points per orientation and all face UV/bounds checks pass. Regression assertions were added to the Retort contract.

**Pending:** run actual Node generator/test commands, `./gradlew build`, and Minecraft screenshot review. Verify from front and a shallow diagonal angle that the apron projects as expected, the two iron accents remain recessed, the thin dark lower line is continuous, and the Y1 hatch top stays readable.
