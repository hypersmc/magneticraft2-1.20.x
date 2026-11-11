# Living Main Menu — real-world prototype (Forge 1.20.1)

**Branch:** `feature/living-main-menu` (branched from Steel Age PR #19).
**Status:** The user's screenshot verifies a real client world with Magneticraft2 blocks behind the title menu. Java compilation succeeded in the user's Forge launch log; actual machine processing, UI hiding, pan motion and loading transition remain unverified.

## Requirement

The background **must be a real running Minecraft level**. Never replace it with video, panorama, a model-only reconstruction, or a fake client-side machine simulation. The actual integrated-server simulation must tick Magneticraft2 machinery using existing power, recipes, block entities, and visual renderers.

## Initial architecture

- The opt-in client setting `Living Main Menu (experimental)` defaults to `false`.
- When enabled and the player is on the vanilla title screen, look for `saves/magneticraft2_menu_source/level.dat`. If absent, use the ordinary menu.
- Make a disposable copy in `saves/_magneticraft2_living_menu_runtime`, then invoke Minecraft 1.20.1's normal `WorldOpenFlows.loadLevel`. The original source save is never opened for ticking by the menu.
- Once connected to that exact world on the real integrated server, display `LiveTitleScreen`. It reuses vanilla title-screen widgets but does **not** draw the panorama. The world is rendered by the actual Minecraft client renderer.
- `LiveTitleScreen.isPauseScreen()` returns false so server-side machinery keeps ticking.
- Options/Mods and other non-gameplay screens **keep the demo world alive**; Back returns immediately to `LiveTitleScreen` without reloading.
- Quit Game is intercepted for mouse and keyboard activation; the demo integrated world is unloaded before the Minecraft client is stopped.
- Singleplayer/Multiplayer/Realms transitions are now cancelled in the Forge screen-opening event BEFORE the target screen opens. The next client tick closes the demo server and opens the selection screen, preventing an actual world join from racing the demo world.
- After returning from game/server selection, the controller waits for a genuinely disconnected title state before restarting the menu demo on a subsequent tick. Returning via an old `LiveTitleScreen` parent falls back to the vanilla title until a new demo is ready.
- Loading failures disable further attempts for the current session, without preventing use of the vanilla menu.
- Only while the live title is visible, Forge client rendering events suppress gameplay HUD, Forge overlays (including The One Probe), first-person hands and block outlines. No global HUD settings are changed.
- The real client player's view rotates at 1.5 degrees per second at the saved world location. This is a smooth in-place camera *pan*, not a physical orbit around a point.
- While the normal integrated server loads, a render-only interception preserves the existing title-screen image in place of world-loading progress screens. The actual loading screens remain alive internally for normal tick/packet handling. Loading time is not eliminated.

This proof-of-concept uses one user-built real scene. It does not yet bundle a demo save or rotate *between* scenes. There is no new machinery or duplicate mechanical-network implementation.

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

- **Observed:** The user's 2026-11-11 screenshot displays actual Magneticraft2 blocks behind the custom title, and their launch log reports Java 17 `compileJava UP-TO-DATE` with the Forge event subscriber registered. These observations do not prove complete `./gradlew build` or a running machine process.
- **Unverified:** Full `./gradlew build`, server-side machine processing, new deferred world unload / menu return behavior, frozen panorama, memory/FPS and multiplayer compatibility. The user previously confirmed HUD hiding, view rotation and loading concealment worked.
- Custom title-screen render injections from other mods might not be shown, since the title panorama's regular render method is intentionally bypassed.
- The standard integrated server has nontrivial startup/memory overhead. Measure it before enabling by default.
- The menu source is local to each client, not a bundled reference scene. A curated demonstrator will be a follow-up only after the architecture is proven.

## In-game acceptance and regression checklist

- [ ] Java 17 `./gradlew build` passes.
- [ ] Without config or without a prepared source, the original title screen operates normally.
- [x] The user screenshot displays the actual saved world and Magneticraft2 blocks behind the title menu (2026-11-11).
- [ ] Verify the hotbar, first-person hand, TOP HUD and selection outlines disappear while the live title is showing, and reappear normally in gameplay.
- [ ] Verify 1.5-degree/second smooth camera pan in the disposable scene (stationary viewpoint, not orbit).
- [ ] Verify no normal world-loading screen *is drawn* after the title; server loading still happens and genuine errors remain visible.
- [ ] Watch real machine inventories and power transfer over 20–60 seconds; verify fuel/input consumption, output production, maintenance wear and 20-tick simulation.
- [ ] Validate graphics after a resource reload and from several camera positions; check for missing textures and geometry issues.
- [ ] Options and Mods remain responsive, do not unload the menu world, and return instantly to the same live menu.
- [ ] Singleplayer/Multiplayer/Realms unload the demo **before** the selection menu is opened; actual singleplayer and multiplayer gameplay must connect without a stuck 100% screen.
- [ ] Quit Game works on the living menu (mouse and keyboard) directly and after visiting Options/Mods.
- [ ] Repeatedly enter/exit menus at least 10 times; after loading a normal world and quitting, the live menu can start again.
- [ ] Confirm the vanilla panorama stays visually stationary throughout the hidden load transition; no world-loading text is drawn.
- [ ] Quit and external modded screen navigation do not leave a stale demo world or change real saves.
- [ ] Check that user saves and the source save are unchanged; generated world resets safely after restarts, crashes and errors.
- [ ] Validate reserved-name collision, corrupt source save, symlink rejection and copy-size limits.
- [ ] Benchmark FPS, CPU, memory, startup time and behavior in modded installations.
- [ ] Revisit navigation after exiting a normal singleplayer world or multiplayer server.

This is a **prototype**, not a verified implementation or complete feature.
