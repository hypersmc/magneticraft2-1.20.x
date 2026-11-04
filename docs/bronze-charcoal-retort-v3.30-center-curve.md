# Charcoal Retort V3.30 — Enlarged center copper curve

**Minecraft Forge 1.20.1** · Draft PR #11 · `feature/bronze-age-foundry` · **Visual candidate**

## User request
The V3.29 front-center copper arch/apron still looks small, with very little of the reference's two dark recessed iron details visible. User permits enlarging the curve, but explicitly limits work to **only that one block**. This means the canonical upper front-center Y2 cell `part_0_2_0.json`, generated for each of SOUTH, WEST, NORTH, EAST.

## Geometry revisions (center cell only)
Seven symmetrical copper arch segments, previously center rise bottom/top Y2.1..3.85, now Y3.25..4.55. Side segment heights rise gradually: 1.25..3.0, 1.85..3.55, 2.55..4.1, 3.25..4.55, then mirrored. The original 7-piece stepping and native 1:1 texture scale are maintained.

The two recessed iron pockets are enlarged from X5.2..6.4 and 9.6..10.8 / Y1.02..1.58 to X4.6..6.4 and 9.6..11.4 / Y1.02..2.55. Their outermost face is Z15.78 (0.22px behind the surrounding Z16 copper). Repositioned copper cheeks no longer obstruct these slots. A solid broad copper central pier X6.7..9.3 rises to Y3.25 and joins the new arch. The narrow original dark drip strip at Y0.1..0.62 and the copper lower sill at Y0.62..1.02 remain unchanged.

## Strict scope and checks
The generator source changes only within the `front&&x===0` branch of `tools/charcoal-retort/v3-models.mjs`. Exactly **four generated JSON files** change: `charcoal_retort_{south,west,north,east}/part_0_2_0.json`. No Y1 hatch, side or corner block, roof, chimney, I/O, formation JSON, collision, textures, recipes, or behavior is edited.

In-memory generation, four-orientation front-surface probes, all model boundaries and all face UVs passed. The larger two metal recesses, raised copper center, lower iron strip, upper masonry and five-slot grille all passed sampled appearance assertions, now recorded in `tools/charcoal-retort/test-contract.mjs`. This is a candidate awaiting Minecraft screenshots.

**Not run:** full local Node build/test commands or `./gradlew build`. The actual in-game look is not yet visually accepted. Test directly front-on and from a shallow diagonal: both dark recessed pockets should be more noticeable and symmetrical, with a clearly raised copper hood and intact central hatch/upper grille.
