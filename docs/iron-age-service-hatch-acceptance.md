# Iron Service Hatch — source acceptance checklist (Iron II-D)

**Target:** `feature/iron-age-foundations`, draft PR #18.
**Stage:** source implementation; not yet compiled or in-game tested.

## Purpose and source contract

A useful factory maintenance opening for Iron catwalks, grating floors
and service walls. It is not a machine UI, simulated pressure seal,
advanced pipe component or additional tile entity.

- Registry ID: `magneticraft2:iron_service_hatch`. Uses an ordinary
  Forge 1.20.1 `TrapDoorBlock` subclass with a hand-openable
  `BlockSetType` using the real Iron set's sound events. Vanilla
  provides facing, top/bottom mounting, redstone interaction, normal
  waterlogging and collision, with no custom ticking or network sync.
- Closed geometry is exactly a 16×3×16 slab; when open, it is exactly
  16×16×3 on the standard hinged side. The blockstate has 16 render
  variants (four facings, two halves, two open states), leaving powered
  and waterlogged as nonvisual built-in states.
- Models use the existing dark wrought-iron material and a small
  bronze-machined hinge band. The plate is made from five non-overlapping
  cuboids, preserving the actual face texel density (no stretched UVs).
  The item uses the closed-bottom model.
- Crafting requires **2 Wrought Iron Plates + 1 Wrought Iron Bar +
  1 Wrought Iron Rod**, yielding **2 Hatches**. Early Iron materials,
  no Stock Shear rivets, new metal or extra machine.
- Standard survival drops, pickaxe and stone-tool tier tags and the
  vanilla `minecraft:trapdoors` block tag keep existing mechanics.
  Registered in the existing Iron construction creative category;
  Patchouli explains use and crafting.

## In-game regression checklist

1. Craft from existing Iron materials in Survival, verify JEI and the
   Patchouli entry, then break in Survival with/without the correct
   pickaxe tier. Check normal drops, silk behavior and no duplication.
2. Place as **floor bottom-half** and **ceiling top-half**. Rotate through
   north/south/east/west, especially the hinge edge and consistent
   player selection/collision; check all 16 model variants.
3. Open/close manually without redstone. Confirm metallic sounds,
   selection/collision, climb/access path and no rendering lag.
4. Activate from lever/redstone on each direction, including
   changes while the hatch was already opened manually. Verify
   vanilla redstone updates; no need to create a custom block entity.
5. Place into water, verify source-waterlogging, fluid flow/ticks,
   open/close and redstone while waterlogged; reload the chunk.
6. Inspect native texture UV scale from below/above, side and in
   both open/closed configurations. Ensure no floating hinge parts,
   coplanar artifacts, strange culling or stretched textures.
7. Confirm the adjacent Iron Floor Grating and Catwalk Railing
   still connect/collide correctly, and the Iron Flywheel/Forge Hearth
   behavior and existing vanilla trapdoors are unchanged.

## Build and verification

Run from a **complete checkout** with JDK17:
`node tools/iron-age/check-iron-service-hatch-contract.mjs`
`./gradlew build`

The fixture validates source registries, state completeness, textures,
UVs, recipe and tags. It is not compilation nor game acceptance.

**Open:** Forge compilation, actual game placement, redstone and
waterlogged runtime checks, visual screenshots, survival drops.
The user has deferred multiplayer-specific work until after Steam Age.
PR #18 stays draft and unmerged.

## Player feedback and later sheet-stock alternative

Player reported **“the hatch works”** on 2026-11-07. Record core
hatch functionality as **player-reported PASS**, not as individual
measured/automated redstone, waterlogging, drop or four-orientation tests.

The subsequent Iron I-C sheet-stock slice adds an **optional**
2 Sheet + 1 Bar + 1 Rod → 2 Hatch crafting recipe. It retains
the original 2 Plate + 1 Bar + 1 Rod → 2 Hatch recipe and does not
change the registered hatch block, collision, redstone or waterlogging.
New recipe and source verification are separate from the player's
earlier acceptance of hatch behavior.
