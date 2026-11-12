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
const chunkLimits = read('src/main/java/com/magneticraft2/client/menu/LivingMenuChunkLimits.java');
const startupBudget = read('src/main/java/com/magneticraft2/client/menu/LivingMenuSpawnBudget.java');
const startupMixin = read('src/main/java/com/magneticraft2/mixin/client/LivingMenuSpawnRadiusMixin.java');
const mixinConfig = JSON.parse(read('src/main/resources/magneticraft2.mixins.json'));
const gradle = read('build.gradle');
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
  ['exit is deferred outside vanilla button dispatch', /if \(requestedNavigation != null\)/.test(controller) && /minecraft\.clearLevel\(new GenericDirtMessageScreen\(/.test(controller)],
  ['send actual level disconnect BEFORE clearLevel', /if \(minecraft\.level != null\)\s*\{\s*minecraft\.level\.disconnect\(\);\s*\}\s*[\s\S]*?minecraft\.clearLevel\(new GenericDirtMessageScreen\(/.test(controller)],
  ['destination preserved on teardown error', /Waiting for full server and save-lock release/.test(controller) && /tryFinishNavigation\(minecraft\)/.test(controller)],
  ['opening happens immediately when shutdown completes', /tryFinishNavigation\(minecraft\);\s*return;\s*}\s*if \(awaitingWorldRelease != null\)/.test(controller)],
  ['no intermediate clickable title screen', !/minecraft\.clearLevel\(new TitleScreen\(\)\)/.test(controller)],
  ['explicit error instead of silent fallback on shutdown timeout', /new AlertScreen\(/.test(controller) && /runtimeSaveLocked/.test(controller)],
  ['wait for integrated-server stop and released session lock', /if \(awaitingWorldRelease != null\)/.test(controller) && /minecraft\.getSingleplayerServer\(\) == null/.test(controller) && /isRuntimeSaveUnlocked\(/.test(controller)],
  ['save lock check detects overlapping locks', /FileChannel\.open\(lockPath/.test(controller) && /OverlappingFileLockException/.test(controller)],
  ['selection constructed AFTER lock release', /private static boolean tryFinishNavigation/.test(controller) && /!isRuntimeSaveUnlocked\(minecraft\.getLevelSource\(\)\.getBaseDir\(\)\)/.test(controller) && controller.indexOf('private static boolean tryFinishNavigation') < controller.indexOf('new SelectWorldScreen(parent)')],
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
  ['menu-only player render and simulation caps parsed', /renderDistanceChunks/.test(scene) && /simulationDistanceChunks/.test(scene) && /integer\(limits, "renderDistanceChunks", 2, 8, 4\)/.test(scene) && /integer\(limits, "simulationDistanceChunks", 2, 8, 5\)/.test(scene)],
  ['chunk caps are applied BEFORE the real integrated server loads', controller.indexOf('LivingMenuChunkLimits.begin(minecraft, scene)') < controller.indexOf('createWorldOpenFlows().loadLevel') && /options\.renderDistance\(\)\.set/.test(chunkLimits)],
  ['chunk caps are upper bounds, not a forced higher view distance', /Math\.min\(originalRender, maxRender\)/.test(chunkLimits) && /Math\.min\(originalSimulation, maxSimulation\)/.test(chunkLimits) && /Math\.max\(MIN_SIMULATION_DISTANCE, scene\.simulationDistanceChunks\)/.test(chunkLimits)],
  ['real player options restored on nav and error', /finally\s*\{[\s\S]*?LivingMenuChunkLimits\.restore\(minecraft\)/.test(controller) && /LivingMenuChunkLimits\.restore\(minecraft\)/.test(controller)],
  ['user settings restored while options or mods screen open', /LivingMenuChunkLimits\.suspend\(Minecraft\.getInstance\(\)\)/.test(controller) && /LivingMenuChunkLimits\.resume\(minecraft\)/.test(controller)],
  ['menu never saves temporary client distance preferences', !/options\.save\(\)/.test(chunkLimits) && !/options\.save\(\)/.test(controller)],
  ['resume only in client tick after OptionsScreen removed and save', controller.indexOf('LivingMenuChunkLimits.resume(minecraft)') < controller.indexOf('public static void onScreenOpening') && !controller.slice(controller.indexOf('public static void onScreenOpening')).includes('LivingMenuChunkLimits.resume(')],
  ['example has both chunk radius limits', sceneExample.limits.renderDistanceChunks === 4 && sceneExample.limits.simulationDistanceChunks === 5],
  ['vanilla simulation distance minimum explicitly enforced', /MIN_SIMULATION_DISTANCE = 5/.test(chunkLimits) && !/simulationDistanceChunks cannot exceed renderDistanceChunks/.test(scene)],
  ['startup override logs real Mixin application', /spawn region-ticket Mixin ACTIVE/.test(startupBudget) && /spawn completion-target Mixin ACTIVE/.test(startupBudget)],
  ['initial spawn radius parsed with explicit bounds', /integer\(limits, "initialSpawnRadiusChunks", 2, 8, 3\)/.test(scene) && /final int initialSpawnRadiusChunks/.test(scene)],
  ['default sample startup prepares 49 chunks instead of 441', sceneExample.limits.initialSpawnRadiusChunks === 3 && /2 \* budget\.radiusChunks\(\) \+ 1/.test(startupBudget)],
  ['startup budget armed before integrated server load', controller.indexOf('LivingMenuSpawnBudget.arm(expectedRuntimePath, scene.initialSpawnRadiusChunks)') < controller.indexOf('createWorldOpenFlows().loadLevel')],
  ['budget gated by integrated server and exact runtime path', /server instanceof IntegratedServer/.test(startupBudget) && /budget\.runtimePath\(\)\.equals\(serverWorld\)/.test(startupBudget)],
  ['budget gated by marked disposable save', /resolve\(LivingMenuWorldCopy\.RUNTIME_MARKER\)/.test(startupBudget) && /RUNTIME_MARKER = MARKER/.test(copy)],
  ['menu Mixin coupled spawn ticket and wait count', /@ModifyConstant\(method = "prepareLevels", constant = @Constant\(intValue = 11\), require = 1\)/.test(startupMixin) && /@ModifyConstant\(method = "prepareLevels", constant = @Constant\(intValue = 441\), require = 1\)/.test(startupMixin)],
  ['menu Mixin progress listener radius matches new spawn radius', /@ModifyConstant\(method = "loadLevel", constant = @Constant\(intValue = 11\), require = 1\)/.test(startupMixin)],
  ['vanilla fallback preserved when budget does not match', /if \(budget == null\) \{\s*return vanillaRadius;/.test(startupBudget) && /if \(budget == null\) \{\s*return vanillaCount;/.test(startupBudget)],
  ['client-only Mixin registration and refmap', mixinConfig.client.includes('client.LivingMenuSpawnRadiusMixin') && !mixinConfig.mixins && mixinConfig.refmap === 'magneticraft2.refmap.json' && /add sourceSets\.main, 'magneticraft2\.refmap\.json'/.test(gradle)],
  ['mixin loaded in production JAR and annotation processed', /"MixinConfigs"\s*:\s*"magneticraft2\.mixins\.json"/.test(gradle) && /annotationProcessor 'org\.spongepowered:mixin:0\.8\.5:processor'/.test(gradle)],
  ['budget stays armed on load timeout to avoid changing expected chunk count', !controller.slice(controller.indexOf('} else if (System.nanoTime() - loadStartedAt > LOAD_TIMEOUT_NANOS)'), controller.indexOf('if (active)')).includes('LivingMenuSpawnBudget.disarm()')],
  ['actual scene folder automatically created', /Files\.createDirectory\(base\)/.test(scene) && /Files\.createDirectory\(root\)/.test(scene)],
];
let failures = 0;
for (const [name, okay] of tests) {
  console.log((okay ? 'PASS' : 'FAIL') + ' ' + name);
  if (!okay) failures++;
}
console.log((tests.length - failures) + '/' + tests.length + ' static checks passed (not compilation or gameplay verification)');
if (failures) process.exitCode = 1;
