# Industrial Air Blower K1 — Bronze IV contract

**Status: committed source candidate, NOT verified in Minecraft or via Gradle.** Keep PR #11 draft; never infer furnace or Bloomery functionality from an animation.

## Purpose and bounded behavior
- Use the existing Gear V2 mechanical input (real rear Mechanical Input Module), with **minimum raw shaft speed 16 RPM** and **torque demand 5**.
- Produce **1 integer pressure/air unit per 5 consecutive supplied ticks**, with a buffer of **16 units**. No production when stalled, overloaded or at capacity. Load releases when the buffer is full.
- Transfer **at most 1 air unit per server tick** from the **front face of the controller** to the directly adjacent PRESSURE receiver, or expose a directional send-only PRESSURE capability on that front face for compatible integrations. Other faces and the unformed machine do not expose air. External callers cannot inject pressure into the output capability. Never mint free air in the client.
- Buffer and partial stroke are persisted by block entity NBT. Running state must revalidate on the next server tick. Dismantling releases Gear V2 demand; stored compressed air is lost when physically broken.
- **Existing consumer:** `BellowsMultiblockModuleEntity`, which already accepts `CapabilityPressure.PRESSURE` for the Primitive Furnace. Set the bellows-module block immediately in front of the outlet to test this. Existing Copper Mechanical Bellows uses the same protocol. This is *not yet a combustion-air consumer for the Refractory Kiln, Ore Roasting Kiln, Forge or future Bloomery*; those need a receiver contract, physical connection and furnace gating before they can claim support.
- No new electrical system, air-block entities, fake pipes, HEAT source, Forge fluid, fuel or wear-item system. K1's fan is non-consumable. Existing PRESSURE code has legacy comments about future removal; review migration before building any broad air network.

## Multiblock
SOUTH blueprint, from rear to front, bottom to top:

```
Y0: SIS    Y1: PAP    Y2: PPP
    SSS        PAP        PAP
    SCS        PAP        PPP
```

**S** stone bricks; **I** real mechanical module; **C** controller and front air outlet; **P** oak planks; **A** AIR. Dimensions: **3×3×3 (W×H×D)**. Patchouli mirrors X and replaces C with 0. There are **three** Y1 AIR cells (rear, middle, front); Y2 has **one** more. These 4 cells become V2 render-air anchors on formation (not extra placed blocks). Fan axis runs along local Z centered at model pixel (8,24,-16), with visible guarded shaft support and a separate rear low-input coupling centered (8,8,-24). Connected hardware is physically grounded; no floating gears.

## Source and illustration
Canonical editable geometry JSON: `tools/multiblock-models/designs/bronze_industrial_air_blower.json`. Static V2 model sources and four sets of block-host meshes are generated from that design. Separate baked rotor and coupling models turn in a client-only BER; no per-frame voxel generation. Edit design JSON and rerun the existing V2 generator for final Studio changes. Additional orthographic study: `docs/reference/bronze-industrial-air-blower-studio.svg`.

The **visual concept poster** is only art direction. Any generated scene/text showing pipes, direct Bloomery/Roaster hookup, wear parts, 4×2×3 dimensions, volumetric liters/sec or speed/torque figures other than the contract is **non-canonical and unimplemented**.

## Acceptance & regression
1. Execute `node tools/multiblock-models/build-designs.mjs --check`, `node tools/multiblock-models/generate.mjs --check`, `node tools/bronze-industrial-air-blower/test-contract.mjs`, then `./gradlew build` on an environment with the repo and dependencies. Record actual outputs.
2. Form all four orientations from Patchouli. Verify rear Mechanical Input module and four AIR cells. Ensure 3×3×3 rotates exactly, no missing-part textures or stretched UVs.
3. Connect Gear V2 and compare raw RPM 15.9 vs 16+, torque 4.9 vs 5+, source overloaded and source missing. Check no power-free generation or source load oscillation; fan turns in correct direction.
4. Without a receiver, buffer fills 0→16 then demand stops. Disconnect/reconnect at partial fill and verify exactly one per five powered ticks without rate exploits.
5. Place an already-formed Primitive Furnace Bellows module directly touching the FRONT controller. Confirm its pressure fills, stops when full, and pumping air is actually consumed by the primitive furnace. No side/rear/external pressure injection.
6. Save/reload with buffered air and partial stroke; validate finite values, no air duplication, no stale running. Break formed structure and verify no phantom mechanical load.
7. Test multiple blower sources sharing one source network; profile FPS around several, and complete a real server plus two-client test. Verify preexisting Mechanical Bellows, furnace recipes, ore roasting, Sawmill and Mixer are unaffected.

## K1 limitations
Air volume is counted in integer pressure units inherited from the current capability. This **does not** define real-world pressure, volume, standard flow rates, compressor temperature, or air duct routing. No actual Refractory Kiln / Bloomery integration has been implemented. Do not introduce HEAT or ore processing bonuses until the receiver architecture and progression gating have been designed and tested.
