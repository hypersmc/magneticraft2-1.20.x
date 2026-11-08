# Wrought Iron Roller Sleeves — focused Iron tooling upgrade

**Branch:** `feature/iron-age-foundations`, draft PR #18. **Stage:** source implemented; primary behavior player-confirmed in-game on 2026-11-08; build and full regression validation pending.

## Why this exists

The existing Bronze Rolling Mill now performs the 12-torque cold finishing
of a Wrought Iron Plate into two Wrought Iron Sheets. Its Bronze Roller
Sleeves already have finite maintenance life. This small upgrade reuses
the existing `ReplaceableMachinePart` alternative material contract
(from the previously accepted Forge Hammer die upgrade).

- Original Bronze Roller Sleeves: **512 completed batches**.
- New Wrought Iron Roller Sleeves: **1024 completed batches**.
- No difference in rolling speed, torque, output, quality, heat, power
  generation, recipe selection, or effective material quality.
- One pair costs **four existing Wrought Iron Bars**, **two Bronze
  Bushings** from the existing Precision Boring Machine and **one
  polished blackstone**. Existing Bronze sleeves remain craftable.
- No special replacement inventory slot or new machine registration.
  Worn parts are serviced by hand using existing GUI-less interaction.
- The item uses a native-texel UV 3D model with two separate hollow
  wrought-iron sleeve bodies and attached bronze outboard collars.

## Backward compatibility

`MillRollers.RemainingCycles` retains its old meaning. Old saved mills
without `InstalledPart` load as Bronze sleeves with their exact
remaining life. Only Iron-installed mills add stable
`MillRollers.InstalledPart = magneticraft2:wrought_iron_roller_sleeves`
when saved. An unrecognized installed item falls back to Bronze and
clamps remaining lifetime. The existing machine block, BE, renderer,
progress, inventory, recipe JSON and module positions are unchanged.

## In-game acceptance (single-player)

1. Craft the new sleeve pair in survival using 4 rolled Bars, 2 bored
   Bronze Bushings and polished blackstone; inspect JEI, creative tab,
   Patchouli, held/ground/inventory model and 1:1 UV (no stretching).
2. Install on a formed existing Rolling Mill. Confirm one consumed,
   installed tier and remaining life **1024/1024** via sneak-empty-hand;
   a second pristine same-tier use must not consume another item.
3. Complete one powered Iron Plate -> 2 Sheets at 12 torque and >=16 RPM,
   and one Copper Sheet batch. Confirm exactly one sleeve service cycle
   per successful batch and unchanged recipes/yields.
4. Block output, interrupt power, induce 8-torque wooden bottleneck,
   idle the machine and unload/reload mid-cycle. Confirm none of these
   consumes service life or duplicates material.
5. Swap an Iron pair to Bronze (**512/512**) and back (**1024/1024**).
   Confirm one matching replacement consumed on a real replacement and
   no item is refunded from the old installed tier.
6. Load a **pre-upgrade saved Mill** with partially worn Bronze sleeves,
   verifying its remaining cycle count is preserved; separately save/
   reload a partially worn Iron pair.
7. Complete Iron hot Ingot -> Bar and hot Bar -> 4 Rods; verify heat
   requirements, quality NBT, and Bronze-era Mill regressions.
8. Verify other use-right-click/controller diagnostics remain available,
   all four formed orientations and no change to roller visuals/FPS.

## Technical checks

```sh
node tools/iron-age/check-iron-roller-sleeves-contract.mjs
./gradlew build
```

No successful compilation or Minecraft testing is claimed by this
source-only addition. Keep PR #18 draft/unmerged; multiplayer testing
remains deferred until after Steam Age.

## Player in-game acceptance — 2026-11-08

The player replied **"yep!"** after receiving the Wrought Iron Roller
Sleeves in-game test checklist. Record the primary replacement upgrade as
**player-confirmed working in-game**. This is overall acceptance feedback,
not a separate report for each scenario: precise 1024/512 wear accounting,
pre-upgrade save migration, both-way swaps, sustained load/overload,
quality, reload, visual FPS and prior rolling recipes remain individually
unverified unless further test evidence is provided.

The committed Node fixture and JDK 17 `./gradlew build` have not been
run or documented here. Keep PR #18 draft/unmerged; multiplayer work is
still deferred until after Steam Age.
