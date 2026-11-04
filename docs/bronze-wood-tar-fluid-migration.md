# Bronze Age — Wood Tar as a fluid (Forge 1.20.1)

## Fluid implementation
Wood Tar is registered as `magneticraft2:wood_tar` (source), `magneticraft2:flowing_wood_tar` and a world-placeable fluid block/bucket. Textures use dedicated dark brown 4-frame still/flowing animations; the bucket has a separate sprite. Density 1180, viscosity 3500. The copper fluid tank and passive pipe network already accept arbitrary Forge fluids.

## Retort gameplay
`retorting_logs.json`: four logs -> four charcoal plus **250 mB** fluid Wood Tar in 720 ticks, with separate solid furnace fuel. **250 mB is a provisional balancing value**, not a previously approved ratio. The Retort has an 8,000 mB persistent, server-owned, drain-only fluid tank; the cycle preflights both item output and tank space before using feed, then commits both after processing. The Retort never accepts liquid fuel through the tar outlet.

The rear Y module in all four normal Retort JSON multiblocks is now a real `multiblock_fluid_output`. The generic fluid module and Copper Fluid Tank share existing Forge IFluidHandler transfer semantics. JEI already shows `fluid_output` on the recipe.

## Save/formation compatibility
Old `magneticraft2:wood_tar` items are **not unregistered or deleted**. They remain usable as Retort fuel (200 ticks) and can be extracted from legacy Retort byproduct slot 2; new cycles no longer produce them. The former byproduct item-port multiblock patterns remain available as `charcoal_retort_legacy_<direction>` so an existing structure can reform. Its original port now forwards a drain-only fluid capability to its Retort; this permits a Copper Fluid Tank to pull liquid through existing pipe connections without replacing the module immediately. Rebuilding the rear byproduct block as a proper Fluid Output is recommended for new builds.

Retort NBT retains existing slot inventory and adds `RetortTarTank`. On load, an absent tank tag produces an empty tank. Already-placed formed multiblocks should be checked after upgrading; do not claim automatic structure migration. Retort fluid is currently lost when the machine is broken, as with existing process tanks.

## Verification required
Check four-direction multiblock formation (new and legacy), legacy slot-2 item recovery, fuel usage, blocked-output rollback, NBT after world reload, the rear fluid port's extraction, 250 mB transfers through pipes, copper tank filling, bucket placement and pickup, fluid texture animation, JEI fluid output and multiplayer synchronization. Run `./gradlew build` plus an in-game visual test. These are NOT proven by source inspection alone.
