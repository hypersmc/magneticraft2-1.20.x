# Bronze → Iron: Wrought Iron consolidation and acceptance

**Status (2026-11-02): initial gameplay source candidate only.** The player has used the Bloomery in Minecraft, but the new hot-bloom → Wrought Iron line has **not yet been compiled or tested in-game**, and Bronze Age is **not closed**. This implementation reuses the existing Refractory Kiln and Mechanical Forge Hammer; no new machine block, ore or power system is introduced.

## First complete working route

```text
Magnetite Ore / Deepslate Magnetite
   -> existing Bronze Stamp Mill
   -> existing Mechanical Ore Washer (consumes water, makes dirty water)
   -> existing Mechanical Sifter
   -> Magnetite Concentrate
   -> Bloomery + Calcite flux + actual Charcoal + HEAT + continuous AIR
   -> Iron Bloom + guaranteed Slag
   -> existing Refractory Kiln + fuel + HEAT
   -> same Iron Bloom (now carrying temporary `magneticraft2:hot_until` NBT)
   -> powered Mechanical Forge Hammer, with replaceable die
   -> Wrought Iron Ingot
```

**Hot stock is an item state, not an additional item or an automatic furnace-to-iron shortcut.**
The Refractory Kiln's `reheat_iron_bloom` datapack recipe consumes 240 heat-supplied ticks and one Iron Bloom, returning one Iron Bloom with a **2400-tick (120-second) forging window** stamped from server world game time at completion. The kiln still needs real fuel and its original HEAT threshold/cost. It can reheat a cooled bloom again, consuming real time/heat. NBT values other than temporary forging heat are preserved.

The Forge Hammer's `consolidate_iron_bloom` recipe consumes one still-hot bloom over 160 **supplied** ticks at minimum 20 RPM and 6 torque, producing one Wrought Iron Ingot. If heat expires or input changes, progress resets, no material is consumed, the mechanical load is released and the player sees a clear diagnostic. A worn die and blocked output stall the process normally. On success, the input's other NBT is forwarded but the time-limited heat marker is removed. The old 4 Copper Ingots → 4 Copper Plates recipe remains unchanged, and cold Copper work has no fabricated HEAT requirement.

The output is one ingot-equivalent per bloom. The Bloomery already emits guaranteed Slag; further realistic slag removal on consolidation would require a genuine new output port/recipe design and is not being invented in this minimal age-transition slice. Reheating can produce separate differently-stamped item stacks that do not combine; collect completed workpieces promptly. The 120-second window is a provisional gameplay balance, **not** real thermodynamic modelling.

## Progression and compatibility

- This is a **Bronze IV exit implementation**, not an Iron Age machinery implementation. The resulting Wrought Iron Ingot is the material handoff for future Iron shafts, tools and components, whose designs have not been approved.
- Keep all 31 ore-family decisions intact. Hematite/Limonite remain conditional alternatives. Uraninite is reserved for the Nuclear Age. No additional ore processing is required to close Bronze.
- Keep the existing `iron_bloom` item ID; only its Item class gains a tooltip displaying hot/cold status. No separate hot item ID, per-tick item entity, free smelting result or cold inventory heat exploit. World game time persists across saves; reload must not restart the heat window.
- Existing forging die wear, port inventory logic, Mechanical Power System load accounting, renderers, JSON formation layouts, model assets and all Copper/Tin recipes remain unchanged.
- The new wrought ingot texture is provisional 16×16 nearest-neighbor pixel art. Prioritize functionality and recipe correctness over additional visual redesign.

## Bronze drivetrain status: still a decision and test blocker

The current `GearNode` exposes individual `maxTorque` and the network already enforces overload and ratios. However, on this branch, standalone shafts/gears remain registered as **wooden** components; the proposed stronger **Bronze transmission components** have not been introduced as blocks, so we cannot truthfully mark the agreed drivetrain upgrade finished. Precision Bronze Gear/Bearing item production exists, but that is not equivalent to physically stronger installed transmission. Do not silently set every wooden part's `maxTorque` higher, or create duplicate gears without approved ratios, visuals and material/wear rules.

**Before closing Bronze:** decide the minimal Bronze drivetrain strength upgrade and implement/test it if it is mandatory for Iron-age machinery. Avoid blocking Wrought Iron production behind a yet-unimplemented drive upgrade if a correctly engineered existing wheel/gear network can supply the Forge Hammer.

## In-game acceptance checklist (all unchecked until tested)

- [ ] JDK 17 `./gradlew build`, recipe/JEI load and book validation; run `node tools/bronze-forge-hammer/test-contract.mjs` and `node tools/bronze-to-iron/test-contract.mjs`.
- [ ] Follow the full Copper → Tin/Bronze → Magnetite concentrate → Bloomery route with survival-obtainable tools/resources, real water, air, charcoal and Calcite. No creative-only ingredients.
- [ ] Obtain Iron Bloom and Slag at the distinct Bloomery output ports. Break/reform the Bloomery and ensure its physical AIR tuyere and all four facings behave.
- [ ] Input one ordinary cold Iron Bloom into the Forge Hammer: zero work, no mechanical load, explicit `bloom_cold` status.
- [ ] Fire one bloom in a fueled Refractory Kiln. Confirm real fuel consumption/HEAT, a single returned bloom with **hot tooltip**, and no free ingredient duplication.
- [ ] Deliver the hot bloom to a powered Forge Hammer. Confirm real RPM/torque demand, a real die cycle, 160 supplied ticks, and exactly one Wrought Iron Ingot with no hot marker.
- [ ] Expire a bloom in inventory, in a chest, in a port, and across save/reload; all must become unforgeable without server-side reheating. Cold → Kiln → hot must work again.
- [ ] Interrupt power, deliberately overload gears, remove the input, block the output, wear out the die, reload at 80/160 progress, then finish. No ghost load, extra products, lost blooms or free work.
- [ ] Test automation from the Kiln's genuine output module to the Forge Hammer input, both direct and with belts/arm; verify that heat timestamps survive exact stack movement.
- [ ] Verify copper Forge Hammer recipe, Refractory Kiln's refractory batch, ore Washer/Sifter, existing Foundry and Bloomery air/heat behavior still function.
- [ ] Dedicated server with two clients, plus actual in-game frame/render checks. Static JSON/source inspection does not substitute for tests.
- [ ] Final Bronze drivetrain and remaining source-implemented machine audit. Only then mark Bronze complete and start Iron content.

**Current gate:** Wrought Iron K1 source authored, full runtime/QA pending. Do not close PR #11 or claim Bronze is release-ready yet.
