# Charcoal Retort — Legacy fluid NBT crash fix

**Forge 1.20.1 · Draft PR #11**  

## Reproduced from the user's stack trace
Loading a Retort via a client chunk packet failed in `FluidStorages.deserializeNBT` on `(FluidStack) nbt.get("fluidtype")`: the legacy implementation persisted `fluidtype` as `StringTag`, so it cannot be cast to `FluidStack`.

The recent Wood Tar conversion accidentally enabled the unrelated inherited fluid serialization by returning `true` from `CharcoalRetortBlockEntity.fluidcape()`.

## Scoped correction
The Retort now returns `false` for **inherited** `fluidcape()`. It *still* exposes its real custom `ForgeCapabilities.FLUID_HANDLER` via its existing `getCapability` override, backed by `tarTank` (8,000 mB, output-only). All storage is preserved through the existing `RetortTarTank` CompoundTag, independent of the old `fluidamount` / `fluidtype` keys. Loading legacy NBT with those two old keys no longer tries to cast them.

This is not a cast workaround or disabling Wood Tar. The obsolete `FluidStorages` instance should never have been enabled on the Retort in the first place.

## Boundary
The inherited `FluidStorages` codec is itself defective and should be redesigned separately for any *other* machine that truly uses it. Avoid altering other machines' persisted data under the Retort-specific bug fix.

## Verification
Static source assertions added to `tools/wood-tar/test-contract.mjs` for dedicated NBT save/load, disabled inherited storage, and retained custom fluid capability. Still required: `node tools/wood-tar/test-contract.mjs`, `./gradlew build`, rejoin/reload the affected world, and test 250 mB production and draining to pipes/tank. No in-game or Forge compilation result is claimed.
