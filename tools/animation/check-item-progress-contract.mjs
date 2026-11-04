#!/usr/bin/env node
// Structural/resource contract check, not a ForgeGradle compile or Minecraft test.
// Run from any working directory: node tools/animation/check-item-progress-contract.mjs
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const java = name => fs.readFileSync(
  path.join(root, 'src/main/java/com/magneticraft2/client/animation', name), 'utf8');
const scene = name => JSON.parse(fs.readFileSync(path.join(root,
  'src/main/resources/assets/magneticraft2/animations', name + '.json'), 'utf8'));
const manifest = name => JSON.parse(fs.readFileSync(path.join(root,
  'src/main/resources/assets/magneticraft2/machine_visuals', name + '.json'), 'utf8'));

function checkKeys(keys) {
  assert.ok(Array.isArray(keys) && keys.length >= 2 && keys.length <= 32);
  assert.equal(keys[0].time, 0);
  assert.equal(keys.at(-1).time, 1);
  let last = -1;
  for (const key of keys) {
    assert.ok(Number.isFinite(key.time) && key.time > last && key.time <= 1);
    assert.ok(Array.isArray(key.position) && key.position.length === 3);
    assert.ok(key.position.every(v => Number.isFinite(v) && Math.abs(v) <= 16));
    if (key.rotation) {
      assert.ok(key.rotation.length === 3 && key.rotation.every(
        v => Number.isFinite(v) && Math.abs(v) <= 3600));
    }
    last = key.time;
  }
  return true;
}

function checkVisualPair(m, s) {
  assert.equal(m.format, 'magneticraft2:machine_visual');
  assert.equal(s.format, 'magneticraft2:visual_animation');
  assert.equal(m.format_version, 1);
  assert.equal(s.format_version, 1);
  assert.equal(s.coordinate_system.reference_facing, 'south');
  const components = Object.values(s.components);
  assert.ok(components.length >= 1 && components.length <= 256);
  for (const c of components) {
    if (!c.item) continue;
    assert.ok(!c.model, 'live item must not duplicate a baked model');
    assert.ok(Object.hasOwn(m.item_channels, c.item.input));
    assert.ok(['none', 'ground', 'fixed'].includes(c.item.display_context ?? 'none'));
    assert.ok(c.item.scale >= 0.0625 && c.item.scale <= 4);
  }
  for (const anim of Object.values(s.animations)) {
    for (const track of anim.tracks) {
      if (track.property !== 'transform_keyframes') continue;
      assert.ok(Object.hasOwn(s.components, track.target));
      const input = s.inputs[track.input];
      assert.equal(input?.source, 'machine.progress_normalized');
      assert.ok(Object.hasOwn(m.scalar_channels, input.module));
      checkKeys(track.keyframes);
    }
  }
}
const m = manifest('bronze_forge_hammer');
const s = scene('bronze_forge_hammer');
checkVisualPair(m, s);
assert.equal(m.item_channels.workpiece, 'workpiece');
assert.equal(m.item_channels.output, 'output');
assert.equal(m.scalar_channels.work_progress, 'process');
assert.equal(s.components.workpiece.item.scale, 0.42);

// Check the same real input alias and normalized curve in the optional demo.
const demoPath = path.join(root, 'docs/examples/json-forge-hammer-progress-pack',
  'assets/magneticraft2/animations/bronze_forge_hammer.json');
const demo = JSON.parse(fs.readFileSync(demoPath, 'utf8'));
checkVisualPair(m, demo);
const keys = demo.animations.workpiece_progress.tracks[0].keyframes;
assert.equal(keys[1].time, 0.5);
assert.equal(keys[1].position[1], 0.18);

// Explicit malformed fixtures must fail the resource contract.
assert.throws(() => checkKeys([{time:0,position:[0,0,0]}, {time:0,position:[0,0,0]}]));
assert.throws(() => checkKeys([{time:0,position:[0,0,0]}, {time:1.2,position:[0,0,0]}]));
assert.throws(() => checkKeys([{time:0,position:[0,0,0]}, {time:1,position:[20,0,0]}]));
assert.throws(() => checkKeys([{time:0,position:[0,0,0]}, {time:1,position:[0,0,0],rotation:[0,4000,0]}]));

const animationDef = java('AnimationDefinition.java');
const frame = java('AnimationFrame.java');
const modelRenderer = java('AnimatedComponentRenderer.java');
const manifestRenderer = java('MachineVisualRenderer.java');
const forgeBlock = fs.readFileSync(path.join(root,
  'src/main/java/com/magneticraft2/common/blockentity/stage/bronze/BronzeForgeHammerBlockEntity.java'), 'utf8');
const forgeRenderer = fs.readFileSync(path.join(root,
  'src/main/java/com/magneticraft2/client/render/blocks/stage/bronze/BronzeForgeHammerRenderer.java'), 'utf8');
assert.match(animationDef, /case "transform_keyframes"/);
assert.match(animationDef, /machine\.progress_normalized/);
assert.match(animationDef, /ItemDisplayContext\.NONE/);
assert.match(frame, /Map<String, ItemStack> itemInputs/);
assert.match(frame, /Map<String, Float> scalarInputs/);
assert.match(modelRenderer, /DrivenProgress\.apply\(/);
assert.match(modelRenderer, /itemRenderer\.render\(stack, item\.display\(\)/);
assert.match(manifestRenderer, /visual\.itemChannels\(\)/);
assert.match(manifestRenderer, /visual\.scalarChannels\(\)/);
assert.match(forgeBlock, /ForgeVisualProcessTime/);
assert.match(forgeBlock, /clientVisualSnapshotTick/);
assert.match(forgeRenderer, /MachineVisualRenderer\.renderScene\(WORKPIECE_VISUAL/);
assert.match(forgeRenderer, /!jsonItem && !workpiece\.isEmpty\(\)/);

// Backward compatibility: old six machine scenes still have valid v1 manifests
// without scalar/item channels. Their authors are not required to add any.
for (const name of ['bronze_mechanical_lathe','bronze_stamp_mill',
  'mechanical_ore_washer','mechanical_sifter',
  'primitive_grinder','rotary_ore_classifier']) {
  const old = manifest(name);
  assert.equal(old.format_version, 1);
  assert.ok(old.angle_channels && old.integer_channels);
}
console.log('PASS: real item/progress scene, example resource pack, bounds, legacy manifests');
