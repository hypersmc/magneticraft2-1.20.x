#!/usr/bin/env node
// Forge 1.20.1 Iron II-A source/resource contract. Not a Gradle or in-game test.
import fs from 'node:fs';
import assert from 'node:assert/strict';

const src='src/main/java/com/magneticraft2/';
const res='src/main/resources/';
const java=p=>fs.readFileSync(src+p,'utf8');
const json=p=>JSON.parse(fs.readFileSync(res+p,'utf8'));
const rec=n=>json('data/magneticraft2/recipes/'+n+'.json');
const forge=rec('forge_iron_gear_blank_medium');
const lathe=rec('lathe_precision_iron_gear_medium');
const assembly=rec('wrought_iron_gear_medium');

assert.deepEqual({
 machine:forge.machine,input:forge.input.item,output:forge.output.item,
 count:forge.output.count,inputCount:forge.input_count,
 time:forge.process_time,mechanical:forge.mechanical
},{
 machine:'magneticraft2:bronze_forge_hammer',
 input:'magneticraft2:wrought_iron_bar',
 output:'magneticraft2:iron_gear_blank_medium',
 count:1,inputCount:1,time:180,mechanical:{min_speed:20,torque:6}
});
assert.deepEqual({
 machine:lathe.machine,input:lathe.input.item,output:lathe.output.item,
 count:lathe.output.count,inputCount:lathe.input_count,
 time:lathe.process_time,mechanical:lathe.mechanical
},{
 machine:'magneticraft2:bronze_mechanical_lathe',
 input:'magneticraft2:iron_gear_blank_medium',
 output:'magneticraft2:precision_iron_gear_medium',
 count:1,inputCount:1,time:160,mechanical:{min_speed:12,torque:5}
});
assert.equal(assembly.type,'minecraft:crafting_shaped');
assert.equal(assembly.result.item,'magneticraft2:wrought_iron_gear_medium');
assert.equal(assembly.pattern.join('').split('P').length-1,3);
assert.equal(assembly.pattern.join('').split('B').length-1,1);
assert.equal(assembly.pattern.join('').split('G').length-1,1);
assert.equal(assembly.key.P.item,'magneticraft2:wrought_iron_plate');
assert.equal(assembly.key.B.item,'magneticraft2:wrought_iron_bar');
assert.equal(assembly.key.G.item,'magneticraft2:precision_iron_gear_medium');
assert.equal(rec('forge_wrought_iron_plate').machine,'magneticraft2:bronze_forge_hammer');
assert.equal(rec('bronze_rolling_copper_sheet').input.item,'magneticraft2:copper_plate');

const woodBE=java('common/blockentity/stage/copper/MediumGearBlockEntity_wood.java');
const ironBE=java('common/blockentity/stage/iron/WroughtIronMediumGearBlockEntity.java');
assert.match(woodBE,/getGearMaxTorque\(\)\s*\{\s*return 8\.0F;/);
assert.match(ironBE,/getGearMaxTorque\(\)\s*\{\s*return 16\.0F;/);
assert.match(ironBE,/extends MediumGearBlockEntity_wood/);
assert.match(ironBE,/GEAR_MEDIUM_BE_IRON\.get\(\)/);

const ironBlock=java('common/block/stage/iron/WroughtIronMediumGearBlock.java');
assert.match(ironBlock,/extends MediumGearBlock_wood/);
assert.match(ironBlock,/GEAR_MEDIUM_BE_IRON\.get\(\)/);
assert.match(ironBlock,/MediumGearBlockEntity_wood::serverTick/);
const beRegistry=java('common/registry/registers/BlockEntityRegistry.java');
assert.match(beRegistry,/GEAR_MEDIUM_BE_IRON/);
assert.match(beRegistry,/WroughtIronMediumGearBlockEntity::new/);
assert.match(java('common/registry/registers/BlockRegistry.java'),
 /BLOCKS\.register\("wrought_iron_gear_medium"/);
assert.match(java('common/registry/registers/ItemRegistry.java'),
 /ITEM_IRON_GEAR_BLANK_MEDIUM/);
assert.match(java('common/registry/registers/ItemRegistry.java'),
 /ITEM_PRECISION_IRON_GEAR_MEDIUM/);
assert.match(java('common/registry/registers/ItemRegistry.java'),
 /ITEM_WROUGHT_IRON_GEAR_MEDIUM/);
const client=java('client/Clientsetup.java');
assert.match(client,/GEAR_MEDIUM_BE_IRON\.get\(\), WroughtIronMediumGearRenderer::new/);
const renderer=java('client/render/blocks/stage/iron/WroughtIronMediumGearRenderer.java');
assert.match(renderer,/shared\.render\(entity, partialTicks, stack, bufferSource/);
const hot=java('common/systems/material/HotBloomWorkpiece.java');
assert.match(hot,/ITEM_IRON_GEAR_BLANK_MEDIUM\.get\(\)/);
assert.match(hot,/ITEM_WROUGHT_IRON_BAR\.get\(\)/);
assert.match(hot,/metalData\.remove\(HOT_UNTIL_TAG\)/);
const latheSource=java('common/blockentity/stage/bronze/BronzeMechanicalLatheBlockEntity.java');
assert.match(latheSource,/MultiblockRecipeHandler\.findRecipe/);
assert.match(latheSource,/ironRecipe\.getTorque\(\)/);
assert.match(latheSource,/ironRecipe\.getMinSpeed\(\)/);
assert.match(latheSource,/cachedIronGearRecipe = null/);
assert.match(latheSource,/ironRecipe == null \? 1 : 2/);
assert.match(latheSource,/finished\.setTag\(workpiece\.getTag\(\)\.copy\(\)\)/);
assert.match(latheSource,/ITEM_ROUGH_BRONZE_GEAR\.get\(\)/);
assert.match(latheSource,/ITEM_ROUGH_BRONZE_BEARING\.get\(\)/);
const model=json('assets/magneticraft2/models/block/wrought_iron_gear_medium.json');
assert.equal(model.parent,'minecraft:block/block');
assert.equal(model.textures.iron,'magneticraft2:block/material/dark_work_iron');
assert.equal(model.textures.collar,'magneticraft2:block/material/bronze_machined');
assert.ok(model.elements.length>=19);
let faceCount=0,rotatedTeeth=0;
for(const element of model.elements){
 const [x,y,z]=element.to.map((v,i)=>v-element.from[i]);
 assert.ok([x,y,z].every(n=>n>0));
 if(element.rotation){
  assert.equal(element.rotation.axis,'y');
  assert.ok(Math.abs(element.rotation.angle)===45);
  rotatedTeeth++;
 }
 for(const [side,face] of Object.entries(element.faces)){
  const [w,h]=side==='up'||side==='down'?[x,z]:
    side==='east'||side==='west'?[z,y]:[x,y];
  assert.deepEqual([face.uv[2]-face.uv[0],face.uv[3]-face.uv[1]],
   [w,h],'stretched '+side);
  assert.ok(model.textures[face.texture.substring(1)]);
  faceCount++;
 }
}
assert.equal(rotatedTeeth,4,'expected four 45-degree diagonal teeth');
assert.ok(faceCount>=114);
const variants=json('assets/magneticraft2/blockstates/wrought_iron_gear_medium.json').variants;
assert.equal(Object.keys(variants).length,7);
assert.equal(variants['powered=true'].model,'magneticraft2:block/invis');
for(const v of Object.values(variants)){
 assert.ok(['magneticraft2:block/wrought_iron_gear_medium','magneticraft2:block/invis']
  .includes(v.model));
}
const item=json('assets/magneticraft2/models/item/wrought_iron_gear_medium.json');
assert.equal(item.parent,'magneticraft2:block/wrought_iron_gear_medium');
const loot=json('data/magneticraft2/loot_tables/blocks/wrought_iron_gear_medium.json');
assert.equal(loot.pools[0].entries[0].name,'magneticraft2:wrought_iron_gear_medium');
const tags=json('data/minecraft/tags/blocks/mineable/pickaxe.json');
assert.ok(tags.values.includes('magneticraft2:wrought_iron_gear_medium'));
const localization=json('assets/magneticraft2/lang/en_us.json');
for(const key of [
 'item.magneticraft2.iron_gear_blank_medium',
 'item.magneticraft2.precision_iron_gear_medium',
 'block.magneticraft2.wrought_iron_gear_medium'])
 assert.ok(localization[key],key);
const book=json('assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/ironage.json');
assert.ok(book.pages.some(p=>p.recipe==='magneticraft2:wrought_iron_gear_medium'));
console.log('Iron II-A gear source/JSON/UV contract PASS (NOT a build or in-game test).');
