#!/usr/bin/env node
// Source/JSON fixture only; not a Forge build or a runtime test.
import fs from 'node:fs';
import assert from 'node:assert/strict';
const root='src/main/';
const json=p=>JSON.parse(fs.readFileSync(root+'resources/'+p,'utf8'));
const java=p=>fs.readFileSync(root+'java/com/magneticraft2/'+p,'utf8');
const id='riveted_iron_casing', item='magneticraft2:'+id;
assert.ok(java('common/registry/registers/BlockRegistry.java').includes('BLOCKS.register("'+id+'"'));
assert.ok(java('common/registry/registers/ItemRegistry.java').includes('ITEM_RIVETED_IRON_CASING = fromBlock(RIVETED_IRON_CASING)'));
assert.ok(java('common/registry/registers/CreativeTabRegistry.java').includes('ITEM_RIVETED_IRON_CASING.get()'));
const recipe=json('data/magneticraft2/recipes/'+id+'.json');
assert.equal(recipe.type,'minecraft:crafting_shaped');
assert.deepEqual(recipe.pattern,['PPP',' F ','PPP']);
assert.equal(recipe.key.F.item,'magneticraft2:riveted_iron_frame');
assert.equal(recipe.key.P.item,'magneticraft2:wrought_iron_plate');
assert.equal(recipe.result.item,item);
assert.equal(recipe.result.count,1);
assert.equal(recipe.pattern.join('').split('P').length-1,6);
const m=json('assets/magneticraft2/models/block/'+id+'.json');
assert.equal(m.textures.iron,'magneticraft2:block/material/dark_work_iron');
assert.equal(m.elements.length,22);
let faces=0;
for(const e of m.elements){
 const [w,h,d]=e.to.map((v,i)=>v-e.from[i]);
 assert.ok([w,h,d].every(x=>x>0));
 assert.ok(e.from.every(x=>x>=0)&&e.to.every(x=>x<=16));
 for(const [face,f] of Object.entries(e.faces)){
  const dims=face==='up'||face==='down'?[w,d]:face==='east'||face==='west'?[d,h]:[w,h];
  assert.deepEqual([f.uv[2]-f.uv[0],f.uv[3]-f.uv[1]],dims);
  faces++;
 }
}
assert.equal(json('assets/magneticraft2/blockstates/'+id+'.json').variants[''].model,'magneticraft2:block/'+id);
assert.equal(json('assets/magneticraft2/models/item/'+id+'.json').parent,'magneticraft2:block/'+id);
assert.equal(json('data/magneticraft2/loot_tables/blocks/'+id+'.json').pools[0].entries[0].name,item);
assert.ok(json('data/minecraft/tags/blocks/mineable/pickaxe.json').values.includes(item));
assert.equal(json('assets/magneticraft2/lang/en_us.json')['block.magneticraft2.'+id],'Riveted Iron Casing');
assert.ok(json('assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/ironage.json').pages.some(p=>p.recipe===item));
console.log('Iron Riveted Casing source/JSON fixture PASS: '+m.elements.length+' elements, '+faces+' native UV faces. No Forge build or in-game verification.');
