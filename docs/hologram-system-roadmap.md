# Magneticraft2 — Holographic Recording System: Completion Plan

Status: **in development** on draft PR #16 (`feature/ancient-holographic-recordings`), Forge 1.20.1.
This document records the boundary between finishing the **runtime** and building
a **personal Hologram Recording Builder**. Do not merge PR #16 merely because
Archive 017 and Archive 042 can play through.

## What we are building

A small, rare, advanced projector artifact from a forgotten civilization.
Each physical projector has **one permanent archive ID** and plays a
noninteractive, full-sized, walk-through engineering recording. A recording
can teach through operation, load, failure, repair and recovery. Archive
collectibles may eventually be discovered long before a player can build
the demonstrated machines.

Keep the server authoritative for playback lifespan, world interaction and
artifact return; keep models, material, particles, audio and animation
client-only. There must be **no extra ghost machines**, no live machine
power or block interactions, no editor GUI attached to the artifact.

## Runtime completion checklist

These are acceptance criteria, **not claims of completed testing**.

### A. Core projection and artifact lifecycle

- [x] Stable physical projector model, one immutable recording per item
- [x] Server-latched duration and start time; synced to client rendering
- [x] Save/reload persistence of recording, duration, visual bounds and start time
- [x] Early break and normal playback end share a one-shot item return path
- [x] Original ItemStack tag preserved on return; its archive ID remains canonical
- [x] Network update tag does not transmit full collectible item NBT
- [ ] Manual repeated early-break and natural-end tests in survival, creative,
      multiplayer, /reload and save/reload with renamed and custom-tagged items
- [ ] Validate safe behavior around block replacement, pistons, explosions,
      unload/reload, clock discontinuities and deliberately malformed item data
- [ ] Verify no archive switching, duplicate drops or stuck/eternal projectors

### B. World-space renderer and resource correctness

- [x] Existing PR #12 animation and baked models for hologram visuals
- [x] Shader-enhanced but fallback-capable translucent, depth-tested material
- [x] Enlarged block entity frustum bounds with per-recording radius
- [x] Recording-defined emitter-to-scene light field and wider placement
      offsets; 33 bounded translucent quads and synchronized raster pulse,
      with no new entities/real light sources
- [x] Beam, baked scene and procedural shader/fallback render layers all
      retain LEQUAL depth testing and use COLOR_WRITE (no depth-buffer writes)
- [ ] Confirm in-game that projection light no longer hides gears, objects
      or terrain behind it, both with shader_effect true and false
- [ ] In-game visual approval of projection field, especially emitter
      attachment, directional alignment, wall occlusion, readability and FPS
- [x] One-time missing-model/missing-atlas diagnostics and scoped fallback
- [x] Existing Copper Age machine models for normal components in Archive 042
- [ ] Confirm all four facings, world depth occlusion, thin-part visibility,
      shader/fallback rendering and resource reload in Minecraft
- [x] The real Mechanical Brake's drum, leather shoes and copper clamp arms
      now share the exact live renderer geometry with holograms, using
      resource-pack-authored procedural attachment points and timing
- [ ] Verify the shared brake procedural geometry renders correctly with
      holographic transparency and its real wood/leather/copper textures
      in-game, without clipping or floating when the models animate
- [ ] Extend the same opt-in shared-visual approach to other dynamically
      rendered Magneticraft2 parts only when a recording needs them
- [ ] Fix remaining depth sorting / transparent surface overdraw if profiling
      and screenshots show it makes the mechanism illegible
- [ ] Verify render-radius AABB covers the whole scene at every facing
- [ ] Performance profile 1, 4, 8 and 16 projectors, viewing and not viewing
      them, with frame-time numbers; establish a sane configuration ceiling

### C. JSON data format and validation

- [x] Datapack server catalog for ID, archive label, title, duration and radius
- [x] Resource-pack client format for appearance, film channels, model variants,
      reconstruction fades, bounded positional visual events and one-shot sounds
- [x] Shared immutable PR #12 scene definition and generic model registration
- [x] Cross-resource validation catches missing mechanical channels, unused
      visual tracks, invalid model-variant stages, missing scene definitions,
      and invalid procedural attachment references/stages
- [x] Bounded `procedural_parts` whitelists the real mechanical brake
      component, axis, phase driver, visibility stage and clamping interval
- [ ] Test malformed, conflicting and incomplete packs: wrong IDs, missing
      resources, unsupported schema versions, invalid sound IDs, missing models
      and texture atlas sprites; errors must name the source and remain safe
- [ ] Decide how to handle a server-installed archive without corresponding
      client assets (currently logs and does not render)
- [ ] Test resource packs changing animation models while recording is active
- [ ] Provide stable versioned reference schemas and small/large examples with
      same rendering semantics as the game

### D. Audio, particles and playback synchronization

- [x] Audio and particles are client-only, sparse, bounded and based on the
      synced recording clock (not frame rate)
- [x] Late joins and frame/time skips do not burst-play historical cues
- [x] Optional scene-relative 3D sound cue positions added; audio and visual
      cues share the exact scene-to-world transform, with fallback sounds at
      the physical emitter for backward compatibility
- [ ] In-game verification that sound/event positions track the **actual
      recorded machinery** in all four horizontal facing directions
- [ ] Multiplayer test: mid-recording join, /reload, resource reload, client
      distance changes, chunk unload/reload, and different FPS
- [ ] Test pause/alt-tab, unusual server durations, missing sound events and
      Minimal particle settings
- [ ] Profile total particles and sounds when several projectors overlap

### E. Finished recording content

- [x] Archive 017: small 45-second mechanical transmission, overload and repair
- [x] Archive 042: larger 60-second multi-stage drive, native Copper Age models,
      overloading disconnect, warning sparks and repair
- [ ] In-game visual and performance approval of BOTH recordings following the
      latest cleanup (the second 042 screenshot still showed checkerboard)
- [ ] Confirm both demonstrate believable gear engagement, shafts, bearings,
      machine load and reconstruction; avoid unsupported fake game blocks
- [ ] Decide the minimum additional archives required before initial release
      (one successful demo is not a complete archival-content system)
- [ ] Player discovery: ruin placement, rarity, loot tables and progression rules
      that make archives discoverable without forcing a higher technological age
- [ ] Integrate JEI/book descriptions only after discovery and gameplay rules
      are settled

## Later — Hologram Recording Builder (not part of the current runtime pass)

**Decision:** build a dedicated authoring tool *after the runtime formats and
rendering are stable*. It is intended for the mod developer, not a projector
item UI or necessarily a player-facing editor.

Desired workflow:

1. Pick an archive ID, title, duration and intended physical scene bounds.
2. Place real existing Magneticraft2 models and, only when needed, separate
   hologram-only scenery. Browse the actual model library and preview UVs.
3. Arrange parts on a 1:1 Minecraft block grid with pivots, facings and
   parent/child transformations; inspect contacts/overlaps before exporting.
4. Build a timeline with machine phases, rotation inputs, state/variant changes,
   reconstruction fades, audio cues and bounded sparks/warnings.
5. Preview at arbitrary ticks, including in-progress load/failure/repair
   states. Camera rotation, occlusion and translucent overdraw need inspection.
6. Run the same cross-file validator as the runtime, including model presence,
   texture references, allowed keyframes, particle limits, and completion time.
7. Export paired **datapack + resource-pack JSON/assets** with no custom Java
   code required for each new recording. Future optional live preview can be
   evaluated separately rather than coupling the editor to gameplay.

**Preferred direction to explore:** add the Hologram Recording Builder as a
module inside the existing **MGC2 Studio**, reusing its established asset
workflow rather than launching an unrelated standalone program. This is a
suggestion, not a committed editor architecture or an implementation task yet.
Assess Studio's current capabilities and architecture *after* runtime and
recording schema stabilization. No framework, UI, packaging, plugin API or
editor workflow has been approved for implementation.

## Release gate

PR #16 remains draft until the code builds successfully, the source data
passes the validation checks, user-reviewed in-game rendering meets the mod's
aesthetic/mechanical standards, lifecycle and multiplayer tests pass, and
there are no unresolved substantial performance or missing-texture regressions.

Record each confirmed test here or in the PR. Distinguish static inspection
from a real successful `./gradlew build` or multiplayer test.
