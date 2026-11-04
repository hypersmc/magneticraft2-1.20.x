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

When a physical section or endpoint breaks, adjacent run halves remain
recoverable and a loaded opposite endpoint's obsolete visual diagonal port
is cleared. No stack is consumed until the complete placement succeeds.
World saves store all ports in blockstates, requiring no dynamic tick work.

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
- [ ] Break a middle section or an endpoint: no fluid movement across the gap,
      no ghost links, no duplicate drops; restore and reconnect.
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

Breaking a pipe while the opposite endpoint's chunk is unloaded may leave a
stale visual half-port there until the player reworks the run; fluid
traversal still refuses missing segments and never forces chunk loading.
The original Copper pipe has only one optional diagonal branch per block.
No unrestricted angles or separate pipe fluid simulation are introduced.
