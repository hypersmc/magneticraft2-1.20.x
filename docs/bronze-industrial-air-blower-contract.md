# Industrial Air Blower K1 — Bronze IV contract

**Status: committed source candidate, NOT verified in Minecraft or via Gradle.** Keep PR #11 draft; never infer furnace or Bloomery functionality from an animation.

## Purpose and bounded behavior
- Use the existing Gear V2 mechanical input (real rear Mechanical Input Module), with **minimum raw shaft speed 16 RPM** and **torque demand 5**.
- Produce **1 integer pressure/air unit per 5 consecutive supplied ticks**, with a buffer of **16 units**. No production when stalled, overloaded or at capacity. Load releases when the buffer is full.
- Transfer **at most 1 air unit per server tick** from the **front face of the controller** directly to an adjacent pressure receiver or through Bronze Air Ducts to a compatible receiver, or expose a directional send-only PRESSURE capability on that front face for compatible integrations. Other faces and the unformed machine do not expose air. External callers cannot inject pressure into the output capability. Never mint free air in the client.
- Buffer and partial stroke are persisted by block entity NBT. Running state must revalidate on the next server tick. Dismantling releases Gear V2 demand; stored compressed air is lost when physically broken.
- **Real industrial consumer:** The **Ore Roasting Kiln**, on the formed controller's outward front face, adjacent and facing the blower outlet, consumes 1 air per 5 heated production ticks for one bonus oxidation-progress tick (+20% process rate). Old Roaster recipes, HEAT and fuel behavior remain unchanged. The Primitive Furnace bellows module also accepts the same pressure. The future Bloomery does not yet receive air.
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

The **visual concept poster** is only art direction. Any generated scene/text showing pipes, direct **Bloomery** hookup, wear parts, 4×2×3 dimensions, volumetric liters/sec or other speed/torque figures is **non-canonical and unimplemented**. A direct Ore Roasting Kiln controller connection is now real in source but remains unverified in-game.

## Acceptance & regression
1. Execute `node tools/multiblock-models/build-designs.mjs --check`, `node tools/multiblock-models/generate.mjs --check`, `node tools/bronze-industrial-air-blower/test-contract.mjs`, then `./gradlew build` on an environment with the repo and dependencies. Record actual outputs.
2. Form all four orientations from Patchouli. Verify rear Mechanical Input module and four AIR cells. Ensure 3×3×3 rotates exactly, no missing-part textures or stretched UVs.
3. Connect Gear V2 and compare raw RPM 15.9 vs 16+, torque 4.9 vs 5+, source overloaded and source missing. Check no power-free generation or source load oscillation; fan turns in correct direction.
4. Without a receiver, buffer fills 0→16 then demand stops. Disconnect/reconnect at partial fill and verify exactly one per five powered ticks without rate exploits.
5. Place an already-formed Primitive Furnace Bellows module directly touching the FRONT controller. Confirm its pressure fills, stops when full, and pumping air is actually consumed by the primitive furnace. No side/rear/external pressure injection.
6. Save/reload with buffered air and partial stroke; validate finite values, no air duplication, no stale running. Break formed structure and verify no phantom mechanical load.
7. Test multiple blower sources sharing one source network; profile FPS around several, and complete a real server plus two-client test. Verify preexisting Mechanical Bellows, furnace recipes, ore roasting, Sawmill and Mixer are unaffected.

## K1 limitations
Air volume is counted in integer pressure units inherited from the current capability. This **does not** define real-world pressure, volume, standard flow rates, compressor temperature, or air duct routing. Roaster oxidation-progress assistance is implemented as an optional real receiver; Refractory Kiln and Bloomery integration still do not exist. Do not invent HEAT injection or ore-yield bonuses.

## In-game rotor overlap / industrial-air follow-up

The static `upper_gearbox_shell` (local x1..15 y19..32 z-17..-12) actually intersects the rotating impeller swept region (x0..16 y16..32 z-18..-14), consistent with the user's Z-fighting screenshot. The static gearbox has been regenerated behind the fan with explicit radial clearance, using the canonical V2 geometry pipeline; do not mask with render offsets. The Ore Roaster intake is the current controller FRONT (not a new module) and only boosts valid hot processing; it never supplies HEAT.

## Post-change verification target

The new source design rejects any static cuboid intersecting the rotor sweep (x0..16, y16..32, z-18..-14), and preserves the original 3×3×3 JSON layer layouts. New back gearcase sits behind the impeller, with a spindle bearing reaching the hub. Retest the user's exact screenshot camera perspective plus all four orientations. A code-level intersection test passing does not constitute visual verification. Roasting throughput should be checked over at least 100 heated ticks with and without a full air supply; ensure exactly one air unit consumed per five valid heated ticks and no consumption while blocked.

## Bronze Air Duct K1 — bounded transport

**Scope:** one new normal Bronze Air Duct block, six-way multipart renderer (native 16px UVs), mining/loot/crafting, book entry, no block entities. It deliberately **does not** inherit from WaterPipeBlock or connect to Water Pipes, so fluids/air never share a network.

The existing FluidPipeNetwork pattern was adopted: an active source pushes into a passive graph. From its real **FRONT** outlet the blower identifies the first duct. Its server tick searches a maximum of **128 loaded connected ducts** and collects only endpoints advertising the existing `CapabilityPressure.PRESSURE` on the **touching side**, with `canReceive() == true`. Forge unsided fallbacks are forbidden to respect block orientation. Receivers deduplicate by block position and are chosen round-robin on successful transfer. The blower's original one-unit-per-game-tick limit remains authoritative across direct transfer, ducts, and external extraction; production remains 1/5 supplied ticks. Search is at most once per 5 ticks while connected to a duct; no global ticker. Disconnecting/breaking ducts stops delivery without duplicating or storing air in pipe segments.

**Controller contract:** both the Blower outlet and Ore Roasting Kiln inlet remain on their formed controller **FRONT** face. You can now connect them using ducts instead of placing controllers adjacent. This avoids editing existing 3×3×3 and 5×8×9 multiblock layouts and keeps saved machines compatible. Future Bloomery furnace must define a real blast inlet location and air demand as part of *its own* JSON-driven multiblock contract.

**Unresolved design decisions (do not treat as implemented):** pressure drop versus duct length, varying tube diameter/material rating, realistic fan pressure curve, airflow/temperature coupling, branch demand prioritization, leakage and overpressure protection, and multi-source network pressure equilibrium. Do not claim a true gas simulation or declare the current optional Roaster +20% oxidization bonus the final behavior. We need an explicit Bloomery HEAT/reduction contract before it can gate iron bloom creation.

**Regression:** test isolated air vs water ducts; all six corner branches; invalid machine sides; broken/removed duct; unloaded chunks; two output receivers; multiple blowers on one duct graph; buffer fill/drain; 1 air per 5 supplied ticks and never more than 1 air/tick via mixed capability+pipe access; old direct connection; ore roasting with and without air and when cold/blocked; independent water and fluid pipe behavior; dedicated server and 2 clients. Verify blockstate update when machine forms/dismantles, no visual gaps/stretched UVs and FPS with large connected networks.
