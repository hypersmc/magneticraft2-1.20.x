#!/usr/bin/env node
// Wood Tar fluid asset and compatibility contract (Node 18+).
// Run: node tools/wood-tar/test-contract.mjs
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../');
const get = p => fs.readFileSync(path.join(root, p), 'utf8');
const json = p => JSON.parse(get(p));
const resource = 'src/main/resources/';
const assets = resource + 'assets/magneticraft2/';
const data = resource + 'data/magneticraft2/';
const recipe = json(data + 'recipes/retorting_logs.json');
assert.equal(recipe.machine, 'magneticraft2:charcoal_retort');
assert.deepEqual(recipe.fluid_output, {fluid:'magneticraft2:wood_tar', amount:250});
assert.ok(!('byproduct' in recipe), 'Retort must no longer generate a Wood Tar item');
assert.ok(!('byproduct_chance' in recipe), 'Old item byproduct chance must be removed');
assert.equal(recipe.output.item, 'minecraft:charcoal');
assert.equal(recipe.output.count, 4);
const rows = ['south','west','north','east'];
for (const dir of rows) {
  const current = json(data + 'multiblocks/charcoal_retort_' + dir + '.json');
  const legacy = json(data + 'multiblocks/charcoal_retort_legacy_' + dir + '.json');
  assert.equal(current.structure.blocks.Y, 'magneticraft2:multiblock_fluid_output');
  assert.equal(legacy.structure.blocks.Y, 'magneticraft2:multiblock_item_byproduct_output');
  assert.equal(legacy.name, 'charcoal_retort_legacy_' + dir);
  assert.equal(current.settings.fluid, true);
  assert.deepEqual(current.structure.layout, legacy.structure.layout);
  assert.equal(current.settings.replacewhenformed, legacy.settings.replacewhenformed);
}
const lang = json(assets + 'lang/en_us.json');
assert.ok(lang['fluid.magneticraft2.wood_tar']);
assert.ok(lang['item.magneticraft2.wood_tar_bucket']);
assert.equal((lang['message.magneticraft2.charcoal_retort.status'].match(/%s/g)||[]).length,8);
assert.equal(json(assets+'models/item/wood_tar_bucket.json').textures.layer0,
 'magneticraft2:item/wood_tar_bucket');
const fluids = get('src/main/java/com/magneticraft2/common/registry/registers/FluidRegistry.java');
assert.ok(fluids.includes('FLOWING_WOOD_TAR'));
assert.ok(fluids.includes('WOOD_TAR_BUCKET'));
const retort = get('src/main/java/com/magneticraft2/common/blockentity/stage/bronze/CharcoalRetortBlockEntity.java');
for(const required of ['RetortTarTank','WOOD_TAR_CAPACITY','TarOutputHandler','FluidAction.SIMULATE','FluidAction.EXECUTE']){
  assert.ok(retort.includes(required),'Retort lacks '+required);
}
assert.ok(retort.includes('int fill(FluidStack resource, FluidAction action) { return 0; }'),
  'Retort tar output must be drain-only');
assert.ok(retort.includes('getStackInSlot(2)'), 'Retort legacy item stack recovery removed');
const compatibility = get('src/main/java/com/magneticraft2/common/blockentity/stage/copper/MultiblockItemPortBlockEntity.java');
assert.ok(compatibility.includes('getPortMode() == MultiblockItemPortBlock.PortMode.BYPRODUCT_OUTPUT'));
const readPng = filename => {
 const p=path.join(root,assets+'textures/'+filename);
 const buf=fs.readFileSync(p);
 assert.equal(buf.toString('hex',0,8),'89504e470d0a1a0a',p+' is not PNG');
 return {width:buf.readUInt32BE(16),height:buf.readUInt32BE(20)};
};
for(const kind of ['still','flow']){
  const png=readPng('block/wood_tar_'+kind+'.png');
  assert.deepEqual(png,{width:16,height:64},kind+' must contain four 16px frames');
  const meta=json(assets+'textures/block/wood_tar_'+kind+'.png.mcmeta');
  assert.ok(meta.animation.frametime>=10,'Tar should animate slowly');
}
assert.deepEqual(readPng('item/wood_tar_bucket.png'),{width:16,height:16});
console.log('Wood Tar contract: recipe, ports, legacy forms, tank, palette and sprites OK');
