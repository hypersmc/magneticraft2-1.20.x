# Steel IV-C — In-game vessel acceptance and regression record

**Branch:** `feature/steel-age-foundations` (PR #19, draft)  
**Scope:** final verification of the *inert* Steel boiler vessel, **not** new Steam functionality.  
**2026-11-10 status:** Fabrication and formed appearance are player-reported working; Patchouli presentation accepted temporarily. Controller/formation in one player world is evidenced by an in-game image; other orientation, teardown, drops, reload, chunk boundaries and multi-client behavior are not yet verified. **Do not mark PR merge-ready.**

## Source verification / build gate

- `node tools/steel-age/check-steel-boiler-fabrication.mjs` — Steel IV-A
- `node tools/steel-age/check-steel-boiler-hardware.mjs` — Steel IV-B
- `node tools/steel-age/check-steel-boiler-assembly-parts.mjs` — Steel IV-C parts
- `node tools/steel-age/check-steel-boiler-vessel.mjs` — directional structure
- `node tools/steel-age/check-steel-boiler-vessel-visuals.mjs` — formed-model/collision/book contract
- `node tools/steel-age/check-steel-boiler-vessel-lifecycle.mjs` — original part ledger, save data and teardown **source invariants only**
- `./gradlew build --no-daemon` on JDK 17.

The execution environment for this review cannot resolve `github.com` from the build container and no local checkout is available, so Gradle and full repository Node checkers were **not executed here**. Direct connector inspection of all four live JSON blueprints found no orientation or ledger mismatch. Source inspection is not runtime proof.

## Exact physical bill of materials (one complete vessel)

| Original piece (must be returned once on teardown) | Count |
| --- | ---: |
| `boiler_shell_panel` | 24 |
| `boiler_end_joint` | 2 |
| `iron_plate_panel` | 16 |
| `iron_machine_foundation` | 10 |
| `steel_boiler_vessel` controller | 1 |
| **Total actual blocks** | **53** |
| Required interior air positions (no item, no filler drop) | 7 |

These are **block items**, not the raw Steel plates/rods used earlier to manufacture them. Dismantling should return modules, **not both modules and their ingredient items**.

## Reproducible Minecraft checks — use a disposable test world

1. **Baseline:** In survival, fill a separate chest with exactly 24 panels, 2 joints, 16 iron plate panels, 10 foundations and 1 controller. Take a screenshot of inventory counts. Use no spare matching blocks in inventory or near the machine.
2. **Place:** Put down controller first. Build the entire 3×4×5 footprint exactly as the current book shows, with seven actual air locations. Right-click controller once. Observe formed state and physical drum.
3. **All four directions:** Recreate at south, north, east and west (separate test structures, not just rotate an already formed machine). Confirm expected controller coordinates from the orientation JSON: south (1,0,4); north (1,0,0); east (4,0,1); west (0,0,1).
4. **Reject invalid builds:** Starting from a known-working structure, remove one Shell Panel, End Joint, Iron Plate Panel or Foundation **before formation** and attempt to form. It must reject each type; also place a solid block in one expected air position and confirm rejection.
5. **Save/reload:** Form, save and exit; reopen the world, confirm all baked parts and hitboxes render correctly; do not replace any existing block. Break a filler, then a separate fresh test with the controller, checking neither path duplicates or loses items.
6. **Exact-once dismantle:** In a clean formed test, mine one shell filler in survival and collect all drops. Totals should be exactly **24/2/16/10/1**. No `multiblock_filler` or air item may drop, and there should be no invisible collision. Repeat in another clean vessel by mining its controller instead.
7. **Chunk boundary:** Build a vessel crossing a chunk boundary. Form, unload and reload both sides, inspect baked geometry, collision and dismantle totals. This is a separate test from ordinary save/reload.
8. **Creative and multiplayer:** Check creative removal does not duplicate items or leave permanent filler/ghost blocks; on an actual dedicated server with two clients, verify identical formed appearance, interaction and post-dismantle state. Single-player integrated-server success does not establish multiplayer behavior.
9. **Regressions:** Re-run Steel IV-A and IV-B manufacturing, both four-input IV-C assembly recipes, original Iron/Copper Press and Boring recipes, and one unrelated JSON multiblock's formation/teardown. No changes to Steam/Pressure/HEAT simulation are expected.

## Results

- [x] Steel IV-A/IV-B basic fabrication demonstrated by player in previous work
- [x] IV-C two assembled components produced in-world by machines
- [x] One formed boiler visually inspected by player; appearance provisionally accepted
- [x] Patchouli preview acceptable for current milestone (remaining irregular end panels acknowledged)
- [x] Four directional JSON footprints + physical ledger checked by live GitHub inspection
- [ ] Gradle compilation verified on JDK 17
- [ ] All Node checkers executed on a complete checkout
- [ ] Invalid/missing-block formation tests
- [ ] Four in-game facings
- [ ] Exact-once survival drops via filler and via controller
- [ ] Save/reload with post-reload teardown
- [ ] Chunk-boundary persistence
- [ ] Creative removal and actual two-client multiplayer check

**Stop condition:** If any destructive test loses/duplicates materials or leaves ghost filler, do not proceed to Steam integration. Record exact test case, controller facing, world reload status, expected/actual drop counts and relevant logs. Fix the demonstrated root cause with the smallest change to the correct lifecycle class, then rerun the shared multiblock regressions.
