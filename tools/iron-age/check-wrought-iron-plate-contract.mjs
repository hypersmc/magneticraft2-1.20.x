#!/usr/bin/env node
// Source/JSON assertions ONLY; not a Forge build or gameplay test.
import fs from 'node:fs';
import assert from 'node:assert/strict';
const src='src/main/java/com/magneticraft2/common/';
const res='src/main/resources/';
const java=p=>fs.readFileSync(src+p,'utf8');
const json=p=>JSON.parse(fs.readFileSync(res+p,'utf8'));
const recipe=n=>json('data/magneticraft2/recipes/'+n+'.json');
const k=recipe('reheat_wrought_iron_ingot'), f=recipe('forge_wrought_iron_plate');
assert.equal(k.machine,'magneticraft2:refractory_kiln');
assert.deepEqual(k.input,{item:'magneticraft2:wrought_iron_ingot'});
assert.deepEqual(k.output,{item:'magneticraft2:wrought_iron_ingot',count:1});
assert.equal(k.input_count,1);assert.equal(k.process_time,240);
assert.equal(f.machine,'magneticraft2:bronze_forge_hammer');
assert.deepEqual(f.input,{item:'magneticraft2:wrought_iron_ingot'});
assert.deepEqual(f.output,{item:'magneticraft2:wrought_iron_plate',count:1});
assert.equal(f.input_count,1);assert.equal(f.process_time,160);
assert.deepEqual(f.mechanical,{min_speed:20,torque:6});
assert.equal(recipe('reheat_iron_bloom').input.item,'magneticraft2:iron_bloom');
assert.equal(recipe('consolidate_iron_bloom').output.item,'magneticraft2:wrought_iron_ingot');
assert.equal(recipe('bronze_forging_copper_plate').input.item,'minecraft:copper_ingot');
const heat=java('systems/material/HotBloomWorkpiece.java');
for(const needle of ['REHEAT_WROUGHT_INGOT_RECIPE','ITEM_WROUGHT_IRON_INGOT.get()','ITEM_WROUGHT_IRON_PLATE.get()','HOT_WINDOW_TICKS = 2400','tag.getLong(HOT_UNTIL_TAG) > world.getGameTime()','metalData.remove(HOT_UNTIL_TAG)','result.setTag(metalData)'])
 assert.ok(heat.includes(needle),'Heat contract: '+needle);
const kiln=java('blockentity/stage/bronze/RefractoryKilnBlockEntity.java');
assert.ok(kiln.includes('HotBloomWorkpiece.isReheatRecipe(recipe)'));
assert.ok(kiln.includes('HotBloomWorkpiece.heatInKiln(candidate, product, level)'));
assert.ok(kiln.indexOf('HotBloomWorkpiece.heatInKiln') < kiln.indexOf('itemHandler.insertItem(1, product, true)'));
const hammer=java('blockentity/stage/bronze/BronzeForgeHammerBlockEntity.java');
for(const needle of ['HotBloomWorkpiece.requiresHeat(recipe)','!HotBloomWorkpiece.isHot(inputStack, level)','HotBloomWorkpiece.consolidate(inputStack, recipe.getOutput())','forgingDie.completeBatch()'])
 assert.ok(hammer.includes(needle),'Forge contract: '+needle);
const reg=java('registry/registers/ItemRegistry.java');
assert.ok(reg.includes('ITEMS.register("wrought_iron_ingot", WroughtIronIngotItem::new)'));
assert.ok(reg.includes('ITEMS.register("wrought_iron_plate"'));
assert.ok(java('registry/registers/CreativeTabRegistry.java').includes('entries.accept(ItemRegistry.ITEM_WROUGHT_IRON_PLATE.get())'));
const item=java('item/stage/iron/WroughtIronIngotItem.java');
assert.ok(item.includes('HotBloomWorkpiece.isHot(stack, level)'));
assert.ok(item.includes('HotBloomWorkpiece.remainingTicks(stack, level)'));
const lang=json('assets/magneticraft2/lang/en_us.json');
for(const key of ['item.magneticraft2.wrought_iron_plate','tooltip.magneticraft2.wrought_iron_ingot.hot','tooltip.magneticraft2.wrought_iron_ingot.cold','message.magneticraft2.bronze_forge_hammer.reason.bloom_cold'])
 assert.ok(lang[key],key);
const book=json('assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/ironage.json');
assert.ok(book.pages.some(p=>p.item==='magneticraft2:wrought_iron_plate'));
const model=json('assets/magneticraft2/models/item/wrought_iron_plate.json');
assert.equal(model.textures.iron,'magneticraft2:block/material/dark_work_iron');
for(const e of model.elements) {
 const [x,y,z]=e.to.map((v,i)=>v-e.from[i]);
 for(let i=0;i<3;i++)assert.ok(e.from[i]>=0&&e.to[i]<=16&&e.from[i]<e.to[i]);
 for(const [side,face] of Object.entries(e.faces)){
  assert.equal(face.texture,'#iron');
  const wanted=(side==='up'||side==='down')?[x,z]:(side==='east'||side==='west')?[z,y]:[x,y];
  assert.deepEqual([face.uv[2]-face.uv[0],face.uv[3]-face.uv[1]],wanted,side+' texel density');
 }
}
console.log('Iron 0B static source/resource contract PASS (not a Gradle build).');
