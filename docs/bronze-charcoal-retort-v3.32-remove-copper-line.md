# Charcoal Retort V3.32 — Remove stray copper line below center crown

**Forge 1.20.1** · Draft PR #11 · `feature/bronze-age-foundry` · Visual review pending.

## User correction
In V3.31 the front-center block still displays a continuous horizontal copper line underneath the stepped copper crown and recessed dark iron. The user explicitly confirmed that this copper line does not belong there.

## Exact correction
Remove the single `bronze` cuboid `[3.35,0.62,15.55]..[12.65,1.02,16]` from the `front&&x===0` branch in `tools/charcoal-retort/v3-models.mjs`. The entire strip is exposed at Y0.62..1.02, and its removal reveals existing recessed brick at Z14. No new replacement copper is added.

Preserve the nearby **dark iron drip-edge** `[3.2,0.1,15.45]..[12.8,0.62,16]`, all seven stepped copper crown segments, exposed iron backing, raised refractory masonry, upper center grille, roof, neighboring cells, and lower Y1 hatch.

## Verification and scope
Only the four direction-specific copies of **the same upper front-center block**, `charcoal_retort_{south,west,north,east}/part_0_2_0.json`, are regenerated. Static model/UV checks passed; sampled 10 points per orientation confirm the continuous copper strip disappeared. Dark iron band, the stepped upper copper crown, and visible upper masonry/grille were unchanged. Updated regression checks explicitly reject the stray copper strip.

**Not run:** external Node test/build-check commands, `./gradlew build`, or Minecraft. Inspect the center above the lower hatch from straight ahead, checking that there is no red copper stripe underneath the dark iron and that the stepped copper crown remains.
