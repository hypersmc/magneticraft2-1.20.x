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
  ['clear level on menu navigation', /minecraft\.clearLevel\(destination\)/.test(controller)],
  ['config opt-in default off', /livingMainMenu = builder[\s\S]*?\.define\("Living Main Menu \(experimental\)", false\)/.test(config)],
];
let failures = 0;
for (const [name, okay] of tests) {
  console.log((okay ? 'PASS' : 'FAIL') + ' ' + name);
  if (!okay) failures++;
}
console.log((tests.length - failures) + '/' + tests.length + ' static checks passed (not compilation or gameplay verification)');
if (failures) process.exitCode = 1;
