#!/usr/bin/env node
// Iron 0C/0D static source/resource assertions. Not a Gradle compile or gameplay run.
import fs from 'node:fs';
import assert from 'node:assert/strict';
const src='src/main/java/com/magneticraft2/common/';
const resources='src/main/resources/';
const java=p=>fs.readFileSync(src+p,'utf8');
const json=p=>JSON.parse(fs.readFileSync(resources+p,'utf8'));
const recipe=n=>json('data/magneticraft2/recipes/'+n+'.json');
const ids=['iron_structural_beam','iron_plate_panel','iron_machine_foundation'];
const rolling=[recipe('roll_wrought_iron_bar'),recipe('roll_wrought_iron_rods')];
const heat=recipe('reheat_wrought_iron_bar');

assert.equal(heat.machine,'magneticraft2:refractory_kiln');
assert.deepEqual(heat.input,{item:'magneticraft2:wrought_iron_bar'});
assert.deepEqual(heat.output,{item:'magneticraft2:wrought_iron_bar',count:1});
assert.equal(heat.process_time,240);
assert.deepEqual(rolling.map(x=>x.input.item),[
 'magneticraft2:wrought_iron_ingot','magneticraft2:wrought_iron_bar']);
assert.deepEqual(rolling.map(x=>x.output.item),[
 'magneticraft2:wrought_iron_bar','magneticraft2:wrought_iron_rod']);
assert.deepEqual(rolling.map(x=>x.output.count),[1,4]);
assert.deepEqual(rolling.map(x=>x.process_time),[180,200]);
for(const r of rolling) {
 assert.equal(r.machine,'magneticraft2:bronze_rolling_mill');
 assert.equal(r.input_count,1);
 assert.deepEqual(r.mechanical,{min_speed:16,torque:6});
}
assert.equal(recipe('bronze_rolling_copper_sheet').input.item,'magneticraft2:copper_plate');
assert.equal(recipe('reheat_wrought_iron_ingot').machine,'magneticraft2:refractory_kiln');
assert.equal(recipe('forge_wrought_iron_plate').machine,'magneticraft2:bronze_forge_hammer');
const heatJava=java('systems/material/HotBloomWorkpiece.java');
for(const token of ['REHEAT_WROUGHT_BAR_RECIPE','ITEM_WROUGHT_IRON_BAR.get()',
 'ITEM_WROUGHT_IRON_ROD.get()','requiresRollingHeat','metalData.remove(HOT_UNTIL_TAG)',
 'tag.getLong(HOT_UNTIL_TAG) > world.getGameTime()'])
 assert.ok(heatJava.includes(token),token);
const mill=java('blockentity/stage/bronze/BronzeRollingMillBlockEntity.java');
for(const token of ['HotBloomWorkpiece.requiresRollingHeat(recipe)',
 '!HotBloomWorkpiece.isHot(inputStack, level)',
 'HotBloomWorkpiece.consolidate(inputStack, recipe.getOutput())',
 'rollerSleeves.completeBatch()','diagnostic = "iron_cold"'])
 assert.ok(mill.includes(token),token);
assert.ok(mill.indexOf('!HotBloomWorkpiece.isHot(inputStack, level)') <
 mill.indexOf('progress++'),'Cold stock can still advance');
const registry=java('registry/registers/ItemRegistry.java');
const blocks=java('registry/registers/BlockRegistry.java');
const tabs=java('registry/registers/CreativeTabRegistry.java');
for(const id of ['wrought_iron_bar','wrought_iron_rod',...ids]) {
 assert.ok(registry.includes('"'+id+'"')||registry.includes('ITEM_'+id.toUpperCase()+' = fromBlock'),id);
}
assert.ok(java('item/stage/iron/WroughtIronBarItem.java')
 .includes('HotBloomWorkpiece.remainingTicks(stack, level)'));
assert.ok(java('block/stage/iron/IronStructuralBeamBlock.java')
 .includes('getCollisionShape'));
assert.ok(java('block/stage/iron/IronPlatePanelBlock.java')
 .includes('getCollisionShape'));
for(const id of ids) {
 assert.ok(blocks.includes('"'+id+'"'),id+' registry');
 assert.ok(tabs.includes('ITEM_'+id.toUpperCase()+'.get()'),id+' creative tab');
 const s=json('assets/magneticraft2/blockstates/'+id+'.json');
 const m=json('assets/magneticraft2/models/block/'+id+'.json');
 const item=json('assets/magneticraft2/models/item/'+id+'.json');
 const loot=json('data/magneticraft2/loot_tables/blocks/'+id+'.json');
 const r=recipe(id);
 assert.equal(item.parent,'magneticraft2:block/'+id);
 assert.equal(loot.pools[0].entries[0].name,'magneticraft2:'+id);
 assert.equal(r.result.item,'magneticraft2:'+id);
 assert.ok(Object.keys(s.variants).length >= 1);
 for(const e of m.elements) {
  const [x,y,z]=e.to.map((n,j)=>n-e.from[j]);
  assert.ok([x,y,z].every(n=>n>0));
  assert.ok(e.from.every(n=>n>=0)&&e.to.every(n=>n<=16));
  for(const [side,face] of Object.entries(e.faces)) {
   const [w,h]=side==='up'||side==='down'?[x,z]:
    side==='east'||side==='west'?[z,y]:[x,y];
   assert.deepEqual([face.uv[2]-face.uv[0],face.uv[3]-face.uv[1]],[w,h],
    id+' '+side+' UV stretching');
  }
 }
}
const axes=json('assets/magneticraft2/blockstates/iron_structural_beam.json').variants;
assert.deepEqual(Object.keys(axes).sort(),['axis=x','axis=y','axis=z']);
// The I-beam's X-axis collision previously had its cross-section rotated
// 90 degrees relative to the blockstate-rendered model. Verify geometry,
// not just that all three blockstate variants exist.
assert.equal(axes['axis=x'].x,90);
assert.equal(axes['axis=x'].y,90);
const beamModel=json('assets/magneticraft2/models/block/iron_structural_beam.json');
const beamSource=java('block/stage/iron/IronStructuralBeamBlock.java');
const xShape=beamSource.match(/private static final VoxelShape X = Shapes\.or\(([\s\S]*?)\)\.optimize\(\);/);
assert.ok(xShape,'X-axis collision boxes not found');
const boxBounds=[...xShape[1].matchAll(/Block\.box\(([^)]*)\)/g)]
  .map(m=>m[1].split(',').map(n=>Number(n.trim())));
assert.equal(boxBounds.length,beamModel.elements.length,'X collision cuboid count');
// Blockstate x=90, y=90 transform in block-centered coordinates:
// (x,y,z) -> (y, 16-z, 16-x); each rotated model cuboid must exactly
// match a collision cuboid, independent of box ordering.
const expectedX=beamModel.elements.map(e=>[
  e.from[1],16-e.to[2],16-e.to[0],
  e.to[1],16-e.from[2],16-e.from[0]
]);
const sortBoxes=boxes=>boxes.map(b=>b.join(',')).sort();
assert.deepEqual(sortBoxes(boxBounds),sortBoxes(expectedX),
  'X-axis beam collision/selection cuboids must match its rendered model');
const faces=json('assets/magneticraft2/blockstates/iron_plate_panel.json').variants;
assert.deepEqual(Object.keys(faces).sort(),['facing=east','facing=north','facing=south','facing=west']);
const tag=json('data/minecraft/tags/blocks/mineable/pickaxe.json');
for(const id of ids)assert.ok(tag.values.includes('magneticraft2:'+id));
const lang=json('assets/magneticraft2/lang/en_us.json');
for(const id of ['wrought_iron_bar','wrought_iron_rod']) assert.ok(lang['item.magneticraft2.'+id]);
for(const id of ids) assert.ok(lang['block.magneticraft2.'+id]);
const book=json('assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/ironage.json');
for(const id of ids)assert.ok(book.pages.some(p=>p.recipe==='magneticraft2:'+id));
const volume=item=>{const e=json('assets/magneticraft2/models/item/'+item+'.json').elements[0];
 return e.to.reduce((v,x,i)=>v*(x-e.from[i]),1)};
assert.equal(volume('wrought_iron_bar'),4*volume('wrought_iron_rod'),
 'Rod display proportions not aligned with a 1:4 stock split');
console.log('Iron 0C/0D source/JSON/UV contract PASS (NOT a Forge build)');
