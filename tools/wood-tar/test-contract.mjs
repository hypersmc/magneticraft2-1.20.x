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
// V3.40: the generic fluid module selects the existing Retort rear
// insert only when formed as part of that Retort. The base model must
// remain available for unformed ports and all other multiblocks.
const fluidStates=json(assets+'blockstates/multiblock_fluid_output.json');
for(const facing of ['north','south','east','west','up','down']){
  assert.equal(fluidStates.variants['facing='+facing]?.model,
    'magneticraft2:block/multiblock_fluid_output_selector',
    'Fluid Output is not connected to its model-data selector: '+facing);
}
const fluidSelector=json(assets+'models/block/multiblock_fluid_output_selector.json');
assert.equal(fluidSelector.loader,'magneticraft2:multiblock');
assert.equal(fluidSelector.baseModel,'magneticraft2:models/block/multiblock_fluid_output.json');
const fluidPort=get('src/main/java/com/magneticraft2/common/blockentity/stage/copper/MultiblockFluidOutputBlockEntity.java');
assert.ok(fluidPort.includes('controller instanceof CharcoalRetortBlockEntity'),
  'Only formed Charcoal Retorts may select the rear insert');
assert.ok(fluidPort.includes('if (!formedModule || level == null)'),
  'Unformed Fluid Outputs must preserve the original block model');
assert.ok(fluidPort.includes('"/port_byproduct"'),
  'Reuse the established Retort rear-port art');
assert.ok(fluidPort.includes('requestModelDataUpdate()'),
  'Client must refresh the selector when the module forms');
for(const dir of rows){
  const custom=json(assets+'models/multiblock/charcoal_retort_'+dir+'/port_byproduct.json');
  assert.ok(custom.elements.length>0,'Missing existing rear port insert '+dir);
  for(const element of custom.elements){
    for(const face of Object.values(element.faces)){
      if(face.texture?.startsWith('#'))assert.ok(custom.textures[face.texture.slice(1)],
        'Unbound rear port texture '+dir+'/'+face.texture);
    }
  }
}

// Prevent the unused legacy BaseBlockEntityMagneticraft2 fluid NBT handler
// from being reenabled on Retort controllers. The real Wood Tar tank has
// independent save/load keys and its own output-only fluid capability.
assert.ok(retort.includes('boolean fluidcape() { return false; }'),
  'Retort must not instantiate the legacy FluidStorages handler');
assert.ok(retort.includes('private final FluidTank tarTank'),
  'Retort must retain its real Wood Tar fluid tank');
assert.ok(retort.includes('tag.put("RetortTarTank", tarTank.writeToNBT(new CompoundTag()))'),
  'Retort must persist the real tank via its own NBT compound');
assert.ok(retort.includes('tarTank.readFromNBT(tag.getCompound("RetortTarTank"))'),
  'Retort must load the real tank independently of legacy fields');
assert.ok(retort.includes('if (capability == ForgeCapabilities.FLUID_HANDLER)') &&
          retort.includes('return formed ? fluidCapability.cast() : LazyOptional.empty()'),
  'Retort must still expose its drain-only fluid capability');

console.log('Wood Tar contract: recipe, ports, legacy forms, tank, palette and sprites OK');
