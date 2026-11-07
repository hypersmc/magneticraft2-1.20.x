# Iron machine mechanical input — Tier-specific appearance

This is a visual correction for formed Iron multiblocks, not a new
Mechanical Input block or a power system. Existing Iron Stock Shear
construction, saves and all existing Copper/Bronze input mechanics remain.

## Root cause
The shared module BER hardcoded the oak shaft and copper bearing at ALL
technological ages. A formed Iron Shear therefore showed a visually
misleading wooden drive even though the rest of its machinery was iron.

## Implementation
- MultiblockController already writes controller coordinates into each
  actual module before activation. The module caches the BlockPos from
  its existing NBT, without new saved fields or per-frame allocations.
- BER checks a data-pack block tag `magneticraft2:iron_input_visual_controllers`
  only for *formed* modules. Currently it lists `iron_stock_shear`.
  Future Iron controller blocks can be added without modifying old
  module IDs, save files or Gear V2 code.
- For these machines only, use the existing 2x2 Iron Shaft JSON model and
  new 8-cuboid forged-iron housing with a bronze supported inner journal.
  Reuse the original four housing members to guarantee alignment.
- Both materials use 1:1 native-density UVs. All six facing directions
  use the existing axis rotation and Gear V2 angle.
- Fix a pre-existing visual overlap: unformed+rotating blockstate now
  leaves rendering to the BER as intended, rather than duplicating its
  static copper housing. Unformed+stopped static model remains unchanged.
- The module remains at **16 torque** and has the same data/sync/formed
  behavior. No new block item or multiblock blueprint requirements.

## Single-player acceptance
1. Run `node tools/iron-age/check-iron-input-visual-contract.mjs`
   and JDK17 `./gradlew build` in a full checkout.
2. Construct fresh Metal Shear; inspect the input before/after formation
   and after save/reload with the 2px Iron Shaft connected externally.
3. Check all NORTH/SOUTH/EAST/WEST Shear orientations and free rotation;
   inspect bearing->shaft->machine coupling and both sides of the real port.
4. Stop power, resume it, toggle reload with F3+T; reject missing models,
   stale atlas quads, coplanar z-fighting and unexplained floating parts.
5. Check older Copper/Bronze machines retain copper/wood housing before,
   during and after formation; verify rotating unformed shaft rendering.
6. Test network load remains 16 and original wood 8-/iron 16-torque
   bottlenecks stay unchanged. Verify saving, re-forming and old worlds.
7. Multiplayer acceptance still deferred until after Steam Age.
No build, runtime, FPS or resource-pack reload claim is made here.
