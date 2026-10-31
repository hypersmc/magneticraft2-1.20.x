# Charcoal Retort rear Fluid Output: reused formed-port model

## In-game observation
Wood Tar is successfully produced and extracted as a Forge fluid, but the new rear Fluid Output showed its generic freestanding copper block model in an oversized gap. It did not use the previously accepted Retort rear-wall connector geometry.

## Cause
The Retort's former item byproduct module already selected the orientation-specific `charcoal_retort_<facing>/port_byproduct` baked model via `MultiBlockProperties.MODEL_NAME`. The replacement `multiblock_fluid_output` BE lacked that ModelData override and its blockstate still referenced the generic standalone model, so the special Retort insert was never selected.

## Correction
- The fluid output blockstate now uses a selector with the original `multiblock_fluid_output.json` as its base model. Unformed ports and fluid modules on other multiblocks retain their original visual.
- The existing real `MultiblockFluidOutputBlockEntity` selects `charcoal_retort_<facing>/port_byproduct` **only** if activated/formed and linked to a Charcoal Retort controller. The model is already registered for all four orientations by `Clientsetup.onRegisterAdditionalModels`.
- The client invalidates model data when an activation/update packet is received.
- No fluid handler changes, port positions, block properties, JSON multiblock patterns, actual Retort mesh geometry, recipes or fluid extraction rules change. Do not duplicate the shared rear model.

## Verification and tests
The Wood Tar static test contract now checks selector and base-model references, formed Retort gating, packet model-data refresh, all four Retort rear-port model files and their texture aliases. This is a **static candidate**. Execute `node tools/wood-tar/test-contract.mjs`, `./gradlew build`, and inspect the actual formed/new/legacy Retort port in Minecraft.

Check (1) newly formed Retort rear wall, (2) all four orientations, (3) unformed generic Fluid Output, (4) another machine using Fluid Output, (5) break/reform and world reload, and (6) transfer of Wood Tar to pipes and tanks. Any incorrect front/rear rotation or visible gaps must be treated as an incomplete visual fix.
