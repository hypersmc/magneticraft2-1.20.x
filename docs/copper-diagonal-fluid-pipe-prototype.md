# Copper Fluid Pipes: direct 45-degree connections (prototype correction)

**Status: implemented in source, not compiled or tested in-game.**
**Branch:** `feature/bronze-age-foundry` (draft PR #11).

## Why the first attempt needed correction

The first implementation added a cardinal pipe fitting and then tilted the
middle pipe. That produced an unwanted elbow at *both* endpoints. The required
shape is instead a continuous, genuinely straight diagonal, from the centre
of the first existing Copper pipe to the centre of the second existing pipe.

## Placement

Hold existing **Copper Fluid Pipe** items and sneak-right-click an ordinary
Copper pipe to select the first endpoint. Sneak-right-click the second
ordinary pipe to connect them. Selecting the same pipe again cancels the
selection. Normal pipe placement is unchanged.

- Both ordinary endpoint pipes are reused in place. No new endpoint block.
- Their positions differ on **exactly two axes** by equal absolute amounts:
  a true 45-degree horizontal diagonal or slope.
- Length must be 2–8 grid steps. Only `length - 1` new, physically
  selectable middle pipe blocks are needed and consumed in survival.
- Each ordinary endpoint has one optional persisted diagonal port. This port
  renders a tilted half-length tube from its centre to the appropriate
  diagonal corner; the matching half continues in the first physical segment.
- The existing 45-degree baked intermediate models are reused. The run has
  **no new cardinal elbow** or extra off-line pipe block.
- If either endpoint already has a diagonal port, placement fails instead of
  silently replacing an old connection.

## World interaction and fluid transport

The straight line is represented by static blockstates, not ticking block
entities or collision entities. The normal Copper pipe models, recipes,
textures and standard connections remain intact.

`FluidPipeNetwork` traverses a WaterPipeBlock-to-DiagonalWaterPipeBlock
edge only if both sides specify reciprocal 45-degree ports. Middle segments
also require matching opposite ports. The graph continues to use the
existing server-authoritative pump/tank/port system and 256-block limit.

Every intermediate cell and **every** diagonal crossing between endpoints
must have a clear physical corridor. Both side cells around each corner
must be air, so thick tubes do not initially pass through stone walls.
Placing a new solid block into that corridor stops fluid transport (the
render model may still clip; see limitations).

Breaking **either ordinary endpoint** or **any intermediate diagonal
section** of a complete, loaded direct run tears down all other generated
sections and clears the diagonal port on each surviving ordinary endpoint.
Both regular endpoint pipes remain when an intermediate part breaks, because
they may also serve other fluid connections. Breaking an ordinary endpoint
never destroys the *other* ordinary endpoint.

Survival breaking drops exactly one normal Copper Fluid Pipe per removed
section through its existing loot table; creative breaking drops none.
The player-broken block itself is handled by normal Minecraft block breaking.
A synchronous cascade guard prevents recursive destruction and item dupes.
For non-player removal (e.g. an explosion), onRemove triggers the same bounded
teardown. Old bent-style diagonal pieces remain on their established behavior.

Every removal checks for a valid reciprocal direct line of at most eight grid
steps and never force-loads missing chunks or tears down unrelated networks.
No stack is consumed until the complete placement succeeds. World saves store
all ports in blockstates, requiring no dynamic tick work.

### Legacy links

Old bent diagonal runs from the first prototype use `section` and
`socket` blockstates on the same diagonal block ID. Their models and
cardinal fluid connection logic are retained, so existing worlds need no
destructive migration. Rebuild a previously placed bent run manually with
this direct mode if the straight geometry is desired.

## Regression and acceptance — unchecked

- [ ] JDK 17 `./gradlew build`; resource/models/loot/translation load.
- [ ] Place regular Copper pipes and use normal six-sided connections.
- [ ] Test direct XY (both slopes), YZ and XZ 45-degree links, including
      reversed selection direction, at 2 and 8 blocks.
- [ ] Verify from all camera angles: **no first/last elbow, no extra
      off-line blocks, no visible gap or texture stretching**.
- [ ] Confirm selectable physical middle blocks, accurate collision
      and a consistent rendered/physical tube diameter.
- [ ] Confirm powered pump -> ordinary pipe -> diagonal -> ordinary
      pipe -> tank/Washer transfers water normally.
- [ ] Test old elbow-style diagonal pipes from an older save.
- [ ] Reject non-45-degree links, 1- or 9-step spans, a used endpoint,
      insufficient item stacks, obstructed cells/corners and protected cells.
      All failed placements must leave the world/inventory unchanged.
- [ ] Break either end of a direct diagonal run: all generated sections
      immediately break, the surviving ordinary pipe remains and loses
      its diagonal arm, and survival item drops match the removed sections.
- [ ] Break any middle section: all other generated sections break and
      both ordinary endpoints remain connected to their original cardinal
      networks without a diagonal arm.
- [ ] Repeat with creative breaking: no generated-section item drops.
- [ ] Repeat using non-player block removal/explosion: no duplicate drops
      or recursion. Confirm ordinary straight pipes and old elbow-style
      diagonal links are unaffected.
- [ ] Verify interrupted/missing sections and unloaded adjacent chunks
      never cause unrelated pipe blocks to be broken.
- [ ] Save/reload, travel through chunks/dimensions, and test on a dedicated
      server with two clients.
- [ ] Verify no frame stutter/performance regressions around numerous pipes.

## Known prototype limitations

A fully occupied 45-degree thick tube touches side-cell space around a
corner. For now, the initial corridor must be empty and the fluid traversal
checks it again; the *side cells* do not contain separate invisible physical
block proxies. Placing blocks into those side cells after installation can
create a visible intersection and selection/collision mismatch. Improving
corner ownership is a separate review before calling the system complete.

Teardown requires a fully loaded reciprocal direct run. Breaking an endpoint
when the opposite end is in an **unloaded chunk** is not yet guaranteed to
remove all sections: the old detach fallback may only clear the visible
link. Persistent deferred cleanup across unloaded chunks remains a
completion requirement before we call this survival-safe in every situation.
The original Copper pipe has only one optional diagonal branch per block.
No unrestricted angles or separate pipe fluid simulation are introduced.

## Direct-pipe construction guide (client preview)

Sneak-right-click an ordinary Copper pipe with Copper Fluid Pipes in hand,
then aim at a second ordinary Copper pipe. The **existing belt survey
renderer** now draws a straight pair of rails with cross marks from the
first pipe centre directly to the candidate second pipe centre.
The two endpoints are outlined in the same colors as other belt guides:

- **Green:** valid route, all placement checks pass, enough pipes in hand.
- **Amber:** geometry and permissions pass, but fewer than span-minus-one
  pipe items are held.
- **Red:** unsupported angle/length, used endpoint or obstructed corridor.

The renderer calls `CopperFluidPipeItem.evaluateConnection`, the **same**
geometry/permissions validation the server placement calls. Client data may
still be temporarily stale due to synchronization, so the server remains
authoritative. No extra renderer, ticking block entities, fluid network
or packet traffic is introduced.

### Preview verification (pending)
- [ ] Valid 45-degree XY/YZ slopes and XZ horizontal links show green.
- [ ] Block the corridor; preview turns red without placing anything.
- [ ] Invalid angle, out-of-range and busy endpoints show red.
- [ ] Insufficient inventory gives amber; creative is green.
- [ ] Clicking the second endpoint agrees with preview for all cases.
- [ ] Main-hand/offhand selection, dimension changes and switching items.
- [ ] Existing Leather and Item Belt previews still render correctly.
- [ ] Confirm stable FPS while looking around with a pipe selected.
