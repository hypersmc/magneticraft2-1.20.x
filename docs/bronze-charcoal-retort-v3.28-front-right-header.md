# Charcoal Retort V3.28 — Matched right upper copper header

**Magneticraft2 / Forge 1.20.1** · Draft PR #11 · Branch `feature/bronze-age-foundry` · **Visual candidate**

## Defect
The front-right upper copper header below the roof did not match the accepted front-left header in height, thickness and lower stepped lip, visible in the user's October 24 front screenshot.

## Isolated model-generator fix
In `tools/charcoal-retort/v3-models.mjs`, change only the Y2 `front&&x===1` copper lintel:
- Prior right beam: [0,14.1,14.5] to [13,16,16].
- New main beam: [0,12.75,14.5] to [13,16,16], **mirror of accepted left** [3,12.75,14.5] to [16,16,16].
- New lower lip: [0,12.25,15] to [12.25,12.75,16], **mirror of accepted left** [3.75,12.25,15] to [16,12.75,16].

The later-layered right recessed square iron fixing, bracket perimeter, copper shoulder and iron foot are untouched. The four-vent panel spread across three side-wall cells remains unchanged.

## Validation and scope
Generator and face-partition pass completed in memory for four rotations. Eight model JSONs changed: four front-right Y2 blocks plus four neighboring center Y2 models (hidden-face re-partition only). The left front models are byte-identical; **168 mirrored left/right front header samples** match, including **112 copper material/depth samples** at four Y values, and right iron-mount samples match prior output. No visible right-side copper vents or center grille/front appearance changed in sampled comparisons. All modified cuboids stay within 0..16 with proportional UVs.

Some left-reference surfaces in WEST/EAST are already occluded or partitioned differently at Y12.5 and Y15.5; the regression contract compares actual visible surfaces there rather than falsely requiring exposed copper. Roof and chimney geometry, formation JSON, collision, ports, recipes and logic unchanged.

**Not run in this session:** actual Node CLI checks, `./gradlew build`, Minecraft. For approval compare front-on and front-right screenshots: matching main copper header height, mirrored stepped lip and unchanged right iron boss.
