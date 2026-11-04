# Magneticraft2 PR #11 — Bronze Age merge-readiness review

**Review date:** 2026-11-04
**Target:** `master`
**Source:** `feature/bronze-age-foundry` (draft PR #11)
**Reviewed source HEAD:** `227a76c3d0881f8edf2f8797d4851fee164706e2`
**Decision:** **NOT MERGE-READY.** Documentation-only status update; no merge,
PR closure, base change, feature addition or ready-for-review transition.

This is an evidence ledger: a reported gameplay success is not a substitute
for a clean build, regression matrix or broad visual acceptance. GitHub
reported the branch as *mergeable* (no current Git conflict) before this
documentation-only commit. Mergeability is not test approval.

## 2026-11-04 player feedback and scope decisions

| User feedback | Recorded decision | Evidence level |
|---|---|---|
| Started with one Magnetite Ore, adding more of each required input when needed, and obtained Wrought Iron Ingot | **Main Bronze IV chain works in a real in-game run**; do not imply one ore is self-sufficient, or assume all full-chain survival recipe gates and stress tests passed | Player-reported in-game end-to-end test |
| Asked why a stronger Bronze drivetrain was next | Existing network handles ratios/torque, but installed gears and shafts remain wooden; **Bronze strength upgrade details and whether it gates Iron are TBD**. First Wrought Iron demonstrably doesn't require creating new Bronze drive blocks first | Code/documentation review + gameplay milestone |
| Machine functions work, models/animations need to be made | **Visual completion is open**, independently of machine operation. Prioritize models, native UVs, mechanically plausible connections, moving parts, JSON animation coverage and performance; retain working rendering and prior visual sign-offs | Player feedback, not machine-by-machine art sign-off |
| Other pipes need diagonals | **Future requirement** for Bronze Air Duct and Steam Pipe diagonal routes. Keep separate AIR vs STEAM vs water transport, including Steam rupture/load rules; not yet implemented | Requirement, scope/scheduling unresolved |
| Dedicated multiplayer / full release progression | **TBD / unverified**, not cancelled | Player feedback |
| Asked to proceed with documentation and PR merge review | Update these status documents and PR description; **do not merge** | Authorized scope |

## Merge blockers / gates

1. **No trusted build evidence for the current branch.** Run JDK 17
   `./gradlew build`; report the real output. No CI workflow run or
   commit status was reported for the reviewed HEAD, and no build
   was run during this documentation review.
2. **Model and animation acceptance remains open.** A functional machine
   may still have incomplete or placeholder visual assets. Inspect the
   current machine models, multiblock V2, JSON animation framework
   (including the changes already merged through PR #12), and actual
   in-game camera views before claiming completion. Test *each*
   affected multiblock orientation; no UV stretch, floating shafts,
   false ports, missing collision, depth artifacts or major FPS/TPS issues.
3. **Wrought Iron risk cases are unverified.** Re-test cold/hot bloom
   expiry, HEAT/fuel, die wear, Gear power/load, output backpressure,
   item/arm transfer, NBT preservation, save/reload, and ingredient/byproduct
   conservation. Main functional output success is confirmed.
4. **Broader Bronze machine acceptance.** Verify the 16-machine roadmap
   at actual K1/K2 criteria: working inputs/outputs, stall and resume,
   wear/service, JSON formation/JEI/Patchouli, crafting and four facings.
   Some machines have previous individual player confirmations, but not
   blanket regressions or completed visuals.
5. **Copper and pipe regressions.** The item/drive belt and arm movement
   fixes have player-reported success. Direct diagonal Copper Fluid Pipe
   placement preview also has player-reported success, but water flow,
   complete-run breaking/drops, legacy elbows, obstruction handling and
   unloaded-chunk teardown need explicit tests. Documented edge cases
   remain unresolved (see the pipe prototype contract).
6. **Save/multiplayer/resource checks.** Retest existing worlds, inventory
   persistence, chunk boundaries, full item-fluid-pressure separation,
   a dedicated server with two clients and real performance. Record
   actual evidence before passing.
7. **Bronze drivetrain decision.** Do not invent stronger component stats
   simply to tick a milestone. Review whether the existing power network
   already meets Bronze-to-Iron needs, then decide if reinforced Bronze
   installed shafts/gears/bearings should ship in Bronze or follow as a
   later, separately accepted development slice.
8. **Other pipe diagonals.** Bronze Air Duct and Steam Pipe support is an
   explicit product requirement, but **implementation and release age
   placement have not been agreed**. Record separately; do not claim
   feature parity or couple Steam's parked systems into Bronze by default.

## Next practical review sequence

- Compile HEAD, run existing contracts and collect real build evidence.
- Complete model/animation work on existing functional machines; make
  small focused reviewable patches instead of reworking whole renderers.
- Exercise Wrought Iron failure modes and the complete resource loop.
- Finish direct Copper pipe runtime and unload/break correctness.
- Run Bronze factory survival/regression and dedicated-server checks.
- Reconcile docs, creative/JEI/Patchouli, and only then re-review PR #11.

## PR scope and conflict notes

The PR body has historically described only Pattern Maker + Mold Table
and 29 files; this is obsolete. The current branch contains the broader
Bronze factory, Copper pipe/animation maintenance and many generated JSON
resources. Large file counts alone are not a blocker; inspect behavior,
resource integrity, data generation and collisions rather than assuming
all files are independently authored Java.

The GitHub metadata reported **mergeable=true** and **draft=true**
at review time. That does *not* establish compilation, runtime acceptance,
or permission to merge. Do not merge/close/change the base PR until asked.
