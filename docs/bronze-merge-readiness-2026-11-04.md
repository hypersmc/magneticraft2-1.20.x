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

## Follow-up user confirmations and scheduling decisions — 2026-11-04

These supersede *planning assumptions* in the earlier review below. Historical
checklists remain as test inventories; unchecked items are not automatically
current implementation blockers or promises to finish before Steam.

1. **MGC2 Studio is the intended authoring workflow for unfinished machine
   models and animations.** The user expects to create these themselves when
   Studio meets their requirements. First confirm Studio can animate both
   actual machine parts **and carried/processed ItemStacks or item visuals**
   driven by machine state/progress, including transport, visibility,
   positioning and output changes. Do not build throwaway Java animation
   workarounds or mark the Studio item-motion feature complete without a test.
   Visual finishing is a planned follow-up dependent on Studio acceptance.
2. **Iron Bloom reheating verified in game:** the player heated an Iron Bloom
   in the Refractory Kiln, saw it become *hot*, and observed a live countdown.
   Together with their previously confirmed Wrought Iron output, this is
   positive functional evidence for the normal hot-working path. It does
   not establish that expiry, subsequent reheating, NBT across storage/reload,
   die wear, stalls or automation edge cases were specifically tested.
3. **Direct 45-degree Copper pipe teardown verified in game:** breaking the
   installed diagonal assembly works, per the player. The player now wants
   the same diagonal connection behavior for **other existing pipe families**,
   across **every supported 45-degree plane/orientation** rather than a
   single demonstration direction. Bronze AIR ducts and Steam pipes need
   their own existing network/pressure/rupture semantics preserved. Do
   not claim these variants have been implemented already.
4. **The mod launches successfully in the user's environment.** This is
   actual evidence the client game and its required code/assets can load
   and be used. The specific JDK 17 `./gradlew build` task has not been
   shown to have passed. Record *game launch/functional runtime: confirmed*
   separately from *packaging/full Gradle task: not separately evidenced*.
   Do not erase real gameplay verification simply because CI is absent.
5. **Multiplayer testing is deliberately scheduled after Steam Age
   development** by user decision. It is *deferred*, not a current Bronze
   implementation blocker or evidence of a passed multi-client test.
   Reassess regression coverage at that stage.

This update does not authorize implementing Steam, making new Bronze
drivetrain parts, redesigning animation infrastructure or merging PR #11.
Its draft status is retained pending an explicit completion/merge decision.

## Merge blockers / gates

1. **Build evidence is partial, not absent.** The user successfully
   launched and played the mod with the current changes. The separate
   JDK 17 `./gradlew build` task and packaging/test suite have not
   been independently evidenced. Run and record that task before
   claiming *full build/package verification*; it does not negate
   the confirmed functioning client game.
2. **Model/animation finishing is dependent on MGC2 Studio maturity.**
   The player intends to do this work using Studio when its authoring
   workflow is acceptable. Verify that Studio represents/animates both
   machine parts and actual moving items before declaring the workflow
   ready. Preserve existing working machine logic and do not treat
   unfinished art as a request for immediate speculative Java renderers.
   Ultimately inspect all facings, UVs, supports, motion, collisions and
   performance before visual completion is claimed.
3. **Normal hot-bloom production is user-verified.** Refractory Kiln
   heating produced the expected hot Iron Bloom with a visible live timer,
   and the normal chain produced Wrought Iron. Targeted regressions still
   include actual expiry, cooling across saves, reheating, fuel/power
   interruption, die wear, output backpressure, item/arm automation, NBT
   persistence and mass accounting; do not re-label the proven normal
   path as untested.
4. **Broader Bronze machine acceptance.** Verify the 16-machine roadmap
   at actual K1/K2 criteria: working inputs/outputs, stall and resume,
   wear/service, JSON formation/JEI/Patchouli, crafting and four facings.
   Some machines have previous individual player confirmations, but not
   blanket regressions or completed visuals.
5. **Copper and pipe regressions.** The item/drive belt and arm motion
   fixes, direct diagonal Copper pipe placement preview and diagonal
   **assembly breaking** have now been reported working in-game. Targeted
   material drops, liquid transport, legacy elbows, obstructed corners and
   unloaded-chunk cleanup still need separate evidence where applicable.
   Extend the requested diagonal functionality to AIR/STEAM only by
   separately preserving their existing transport networks.
6. **Save/resource checks remain; multiplayer is scheduled for Steam.**
   Test persistence, chunk boundaries, water/AIR/STEAM separation and
   performance where the current changes require it. The user elected
   to do dedicated-server/two-client verification **after Steam Age**.
   Do not claim multiplayer passed or use it as an immediate Bronze
   development stop.
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

- Record the confirmed client game launch and Wrought Iron gameplay path;
  obtain separate full Gradle/contract packaging evidence when available.
- Finish/verify MGC2 Studio's machinery **and moving-item animation**
  capabilities before scheduling the user's model and animation authoring.
- Exercise targeted Wrought Iron expiry/stall/reload edge cases as needed,
  without repeating the already-proven normal path.
- Finish water diagonal corner/chunk cases and design equivalent
  all-orientation 45-degree connections for the other pipe families.
- Keep the Bronze drivetrain decision open until actual requirements exist.
- Revisit multiplayer **after Steam Age**, as requested.
- Reconcile JEI/Patchouli/visuals and PR readiness after those milestones;
  do not merge PR #11 automatically.

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
