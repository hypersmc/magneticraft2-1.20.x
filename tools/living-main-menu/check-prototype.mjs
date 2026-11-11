#!/usr/bin/env node
// Static contract checks only, NOT Java compilation or Minecraft runtime testing.
import fs from 'node:fs';
import path from 'node:path';
const root = path.resolve(import.meta.dirname, '../..');
const read = p => fs.readFileSync(path.join(root, p), 'utf8');
const controller = read('src/main/java/com/magneticraft2/client/menu/LivingMenuController.java');
const copy = read('src/main/java/com/magneticraft2/client/menu/LivingMenuWorldCopy.java');
const title = read('src/main/java/com/magneticraft2/client/menu/LiveTitleScreen.java');
const config = read('src/main/java/com/magneticraft2/common/utils/Magneticraft2ConfigClient.java');
const scene = read('src/main/java/com/magneticraft2/client/menu/LivingMenuScene.java');
const sceneExample = JSON.parse(read('docs/examples/living-main-menu/scene.json'));
const tests = [
  ['real integrated-server world loading', /createWorldOpenFlows\(\)\.loadLevel\(/.test(controller)],
  ['exact active server save check', /getSingleplayerServer\(\)/.test(controller) && /getWorldPath\(LevelResource\.ROOT\)/.test(controller)],
  ['client-only Forge subscriber', /value = Dist\.CLIENT/.test(controller) && /Bus\.FORGE/.test(controller)],
  ['original scene copied', /Files\.walkFileTree\(source/.test(copy) && /Files\.copy\(file/.test(copy)],
  ['only generated save may be deleted', /MARKER/.test(copy) && /Refusing to overwrite non-generated menu world/.test(copy)],
  ['symlinks rejected', /attributes\.isSymbolicLink\(\)/.test(copy)],
  ['no panorama or fake renderer', !/super\.render\(/.test(title) && /this\.renderables/.test(title)],
  ['integrated server not paused', /boolean isPauseScreen\(\)\s*\{[\s\S]*?return false;/.test(title)],
  ['exit is deferred until the next tick before opening selection', /if \(requestedNavigation != null\)/.test(controller) && /minecraft\.clearLevel\(new TitleScreen\(\)\)/.test(controller)],
  ['wait for integrated-server stop and released session lock', /if \(awaitingWorldRelease != null\)/.test(controller) && /minecraft\.getSingleplayerServer\(\) == null/.test(controller) && /isRuntimeSaveUnlocked\(/.test(controller)],
  ['save lock check detects overlapping locks', /FileChannel\.open\(lockPath/.test(controller) && /OverlappingFileLockException/.test(controller)],
  ['selection constructed AFTER lock release', controller.indexOf('isRuntimeSaveUnlocked(minecraft.getLevelSource().getBaseDir())') < controller.indexOf('new SelectWorldScreen(parent)')],
  ['generated runtime cleaned only after unlock', /LivingMenuWorldCopy\.cleanupRuntime\(/.test(controller) && /deleteGeneratedDirectoryIfPresent\(savesDirectory\.resolve\(RUNTIME_ID\)\)/.test(copy)],
  ['intercept gameplay screen BEFORE it opens', /event\.getCurrentScreen\(\) instanceof LiveTitleScreen/.test(controller) && /event\.setCanceled\(true\)/.test(controller)],
  ['quit waits for the same safe unload path', /destination == NavigationTarget\.QUIT/.test(controller) && /minecraft\.stop\(\);/.test(controller)],
  ['gameplay and quit buttons intercepted BEFORE vanilla callbacks', /LivingMenuController\.requestNavigation\(target\)/.test(title) && /menu\.singleplayer/.test(title) && /menu\.multiplayer/.test(title) && /menu\.online/.test(title) && /menu\.quit/.test(title) && /super\.mouseClicked/.test(title)],
  ['keep demo world alive across non-gameplay submenus', /destination instanceof SelectWorldScreen/.test(controller) && /destination instanceof JoinMultiplayerScreen/.test(controller) && /destination instanceof RealmsMainScreen/.test(controller)],
  ['return-to-title is gated before reopening demo world', /if \(navigatingAway\)/.test(controller) && /minecraft\.level == null && minecraft\.screen instanceof TitleScreen/.test(controller)],
  ['restore live title when returning from options', /event\.setNewScreen\(new LiveTitleScreen\(\)\)/.test(controller)],
  ['config opt-in default off', /livingMainMenu = builder[\s\S]*?\.define\("Living Main Menu \(experimental\)", false\)/.test(config)],
  ['HUD and Forge overlay suppression scoped to live menu', /onGameplayHud\(RenderGuiEvent\.Pre/.test(controller) && /onGuiOverlay\(RenderGuiOverlayEvent\.Pre/.test(controller) && /isLiveMenu\(\)/.test(controller)],
  ['hand and block selection suppression', /onPlayerHand\(RenderHandEvent/.test(controller) && /onBlockHighlight\(RenderHighlightEvent\.Block/.test(controller)],
  ['real client viewpoint slowly pans at configured speed', /selectedScene\.degreesPerSecond \/ 20\.0F/.test(controller)],
  ['loading screens are visually covered only', /onLoadingScreenRender\(ScreenEvent\.Render\.Pre/.test(controller) && /menuDuringLoading\.render/.test(controller) && /event\.setCanceled\(true\)/.test(controller)],
  ['loading panorama is frozen without stopping real world load', /menuDuringLoading\.render\(event\.getGuiGraphics\(\), -1000, -1000, 0\.0F\)/.test(controller)],
  ['real scene lookup with legacy save fallback', /LivingMenuScene\.select\(/.test(controller) && /LivingMenuWorldCopy\.sourceExists\(legacy\)/.test(scene)],
  ['per-scene camera rotation speed applied', /selectedScene\.degreesPerSecond \/ 20\.0F/.test(controller) && /selectedScene\.rotate/.test(controller)],
  ['real server world time and gamerules applied', /level\.setDayTime/.test(scene) && /RULE_DAYLIGHT/.test(scene) && /RULE_RANDOMTICKING/.test(scene)],
  ['source size and stored chunk limits enforced', /scene\.maxWorldMiB/.test(copy) && /scene\.maxStoredChunks/.test(copy) && /countStoredChunks\(file\)/.test(copy)],
  ['no source enumeration on every tick', /if \(sceneLookupDone\)/.test(controller)],
  ['example manifest valid and enabled', sceneExample.schemaVersion === 1 && sceneExample.enabled === true && sceneExample.camera.rotation.degreesPerSecond === 1.5],
  ['actual scene folder automatically created', /Files\.createDirectory\(base\)/.test(scene) && /Files\.createDirectory\(root\)/.test(scene)],
];
let failures = 0;
for (const [name, okay] of tests) {
  console.log((okay ? 'PASS' : 'FAIL') + ' ' + name);
  if (!okay) failures++;
}
console.log((tests.length - failures) + '/' + tests.length + ' static checks passed (not compilation or gameplay verification)');
if (failures) process.exitCode = 1;
