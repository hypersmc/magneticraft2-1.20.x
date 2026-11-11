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
const tests = [
  ['real integrated-server world loading', /createWorldOpenFlows\(\)\.loadLevel\(/.test(controller)],
  ['exact active server save check', /getSingleplayerServer\(\)/.test(controller) && /getWorldPath\(LevelResource\.ROOT\)/.test(controller)],
  ['client-only Forge subscriber', /value = Dist\.CLIENT/.test(controller) && /Bus\.FORGE/.test(controller)],
  ['original scene copied', /Files\.walkFileTree\(source/.test(copy) && /Files\.copy\(file/.test(copy)],
  ['only generated save may be deleted', /MARKER/.test(copy) && /Refusing to overwrite non-generated menu world/.test(copy)],
  ['symlinks rejected', /attributes\.isSymbolicLink\(\)/.test(copy)],
  ['no panorama or fake renderer', !/super\.render\(/.test(title) && /this\.renderables/.test(title)],
  ['integrated server not paused', /boolean isPauseScreen\(\)\s*\{[\s\S]*?return false;/.test(title)],
  ['defer world unload until client tick', /if \(pendingUnloadDestination != null\)/.test(controller) && /minecraft\.clearLevel\(destination\)/.test(controller) && /pendingUnloadDestination = destination;/.test(controller)],
  ['intercept gameplay screen BEFORE it opens', /event\.getCurrentScreen\(\) instanceof LiveTitleScreen/.test(controller) && /event\.setCanceled\(true\)/.test(controller)],
  ['quit unloads real menu world before stopping client', /if \(quitRequested\)/.test(controller) && /minecraft\.clearLevel\(\);/.test(controller) && /minecraft\.stop\(\);/.test(controller)],
  ['quit button input handles mouse and keyboard', /LivingMenuController\.requestQuit\(\)/.test(title) && /menu\.quit/.test(title)],
  ['keep demo world alive across non-gameplay submenus', /destination instanceof SelectWorldScreen/.test(controller) && /destination instanceof JoinMultiplayerScreen/.test(controller) && /destination instanceof RealmsMainScreen/.test(controller)],
  ['return-to-title is gated before reopening demo world', /if \(navigatingAway\)/.test(controller) && /minecraft\.level == null && minecraft\.screen instanceof TitleScreen/.test(controller)],
  ['restore live title when returning from options', /event\.setNewScreen\(new LiveTitleScreen\(\)\)/.test(controller)],
  ['config opt-in default off', /livingMainMenu = builder[\s\S]*?\.define\("Living Main Menu \(experimental\)", false\)/.test(config)],
  ['HUD and Forge overlay suppression scoped to live menu', /onGameplayHud\(RenderGuiEvent\.Pre/.test(controller) && /onGuiOverlay\(RenderGuiOverlayEvent\.Pre/.test(controller) && /isLiveMenu\(\)/.test(controller)],
  ['hand and block selection suppression', /onPlayerHand\(RenderHandEvent/.test(controller) && /onBlockHighlight\(RenderHighlightEvent\.Block/.test(controller)],
  ['real client viewpoint slowly pans', /player\.setYRot\(minecraft\.player\.getYRot\(\) \+ CAMERA_TURN_DEGREES_PER_TICK\)/.test(controller)],
  ['loading screens are visually covered only', /onLoadingScreenRender\(ScreenEvent\.Render\.Pre/.test(controller) && /menuDuringLoading\.render/.test(controller) && /event\.setCanceled\(true\)/.test(controller)],
  ['loading panorama is frozen without stopping real world load', /menuDuringLoading\.render\(event\.getGuiGraphics\(\), -1000, -1000, 0\.0F\)/.test(controller)],
];
let failures = 0;
for (const [name, okay] of tests) {
  console.log((okay ? 'PASS' : 'FAIL') + ' ' + name);
  if (!okay) failures++;
}
console.log((tests.length - failures) + '/' + tests.length + ' static checks passed (not compilation or gameplay verification)');
if (failures) process.exitCode = 1;
