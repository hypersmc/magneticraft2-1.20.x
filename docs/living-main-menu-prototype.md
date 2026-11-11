# Living Main Menu — real-world prototype (Forge 1.20.1)

**Branch:** `feature/living-main-menu` (branched from Steel Age PR #19).
**Status:** source implementation only; no Forge compilation or in-game verification has been completed.

## Requirement

The background **must be a real running Minecraft level**. Never replace it with video, panorama, a model-only reconstruction, or a fake client-side machine simulation. The actual integrated-server simulation must tick Magneticraft2 machinery using existing power, recipes, block entities, and visual renderers.

## Initial architecture

- The opt-in client setting `Living Main Menu (experimental)` defaults to `false`.
- When enabled and the player is on the vanilla title screen, look for `saves/magneticraft2_menu_source/level.dat`. If absent, use the ordinary menu.
- Make a disposable copy in `saves/_magneticraft2_living_menu_runtime`, then invoke Minecraft 1.20.1's normal `WorldOpenFlows.loadLevel`. The original source save is never opened for ticking by the menu.
- Once connected to that exact world on the real integrated server, display `LiveTitleScreen`. It reuses vanilla title-screen widgets but does **not** draw the panorama. The world is rendered by the actual Minecraft client renderer.
- `LiveTitleScreen.isPauseScreen()` returns false so server-side machinery keeps ticking.
- On vanilla menu-button navigation, disconnect the temporary integrated server. A stale `LiveTitleScreen` parent returns to a fresh normal `TitleScreen`.
- Loading failures disable further attempts for the current session, without preventing use of the vanilla menu.

This proof-of-concept uses one user-built real scene. It does not yet bundle a demo save or rotate scenes. There is no new machinery or duplicate mechanical-network implementation.

## How to stage the scene

1. With the latest Magneticraft2 content, create a **small** singleplayer world whose **save directory** is exactly `magneticraft2_menu_source`.
2. Build and prove a genuine machine chain works in ordinary gameplay. Position the player at the desired menu camera view and Save & Quit. Its real fuel, inputs, outputs and mechanical wear still apply.
3. Set `Living Main Menu (experimental) = true` under the Magneticraft2 client config in `config/magneticraft2-client.toml`, then restart Minecraft.
4. A new disposable save named `_magneticraft2_living_menu_runtime` is created and loaded. Vanilla menu buttons are displayed on top of actual gameplay.
5. The runtime is reset from source on subsequent launches. Do not edit it. If you disable the feature or remove the source save, normal title behavior returns.

## Save safety

- Only the exact reserved runtime and staging folders with a generation-marker file may be removed. An unmarked existing folder is *never* overwritten.
- Never follow a source symlink. Do not clone `session.lock`.
- Limit the prototype source to 128 MiB and 10,000 files. The copy executes on the client thread: scene saves should be intentionally tiny.
- The temporary runtime folder currently appears in the world list; a later iteration should move it out of the visible save list if feasible.

## Known limitations

- **Unverified:** Java 17 `./gradlew build`, actual loading, rendering, camera pose, server ticking, memory/FPS, game integration and multiplayer compatibility. Do not mark this PR merge-ready based on static checks.
- Custom title-screen render injections from other mods might not be shown, since the title panorama's regular render method is intentionally bypassed.
- The standard integrated server has nontrivial startup/memory overhead. Measure it before enabling by default.
- The menu source is local to each client, not a bundled reference scene. A curated demonstrator will be a follow-up only after the architecture is proven.

## In-game acceptance and regression checklist

- [ ] Java 17 `./gradlew build` passes.
- [ ] Without config or without a prepared source, the original title screen operates normally.
- [ ] With source prepared, confirm the actual integrated server, original saved camera and live ClientLevel rendering.
- [ ] Watch real machine inventories and power transfer over 20–60 seconds; verify fuel/input consumption, output production, maintenance wear and 20-tick simulation.
- [ ] Validate graphics after a resource reload and from several camera positions; check for missing textures and geometry issues.
- [ ] Singleplayer, Multiplayer, Options, Mods and Quit work by mouse and keyboard, with no lingering menu world; returning from these menus must not show a stale blank screen.
- [ ] Check that user saves and the source save are unchanged; generated world resets safely after restarts, crashes and errors.
- [ ] Validate reserved-name collision, corrupt source save, symlink rejection and copy-size limits.
- [ ] Benchmark FPS, CPU, memory, startup time and behavior in modded installations.
- [ ] Revisit navigation after exiting a normal singleplayer world or multiplayer server.

This is a **prototype**, not a verified implementation or complete feature.
