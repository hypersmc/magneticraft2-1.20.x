#!/usr/bin/env node
// Non-build, non-runtime source and JSON fixture for first Iron walkways.
import fs from 'node:fs';
import assert from 'node:assert/strict';
const R='src/main/resources/',J='src/main/java/com/magneticraft2/';
const j=p=>JSON.parse(fs.readFileSync(R+p,'utf8'));
const c=p=>fs.readFileSync(J+p,'utf8');
const ids=['iron_floor_grating','iron_catwalk_railing'];
const block=c('common/registry/registers/BlockRegistry.java');
const item=c('common/registry/registers/ItemRegistry.java');
const tab=c('common/registry/registers/CreativeTabRegistry.java');
const book=j('assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/ironage.json');
const tags=j('data/minecraft/tags/blocks/mineable/pickaxe.json');
const lang=j('assets/magneticraft2/lang/en_us.json');
let count=0;
const checkCuboids=path=>{
 const model=j(path);
 assert.equal(model.textures.iron,'magneticraft2:block/material/dark_work_iron');
 for(const e of model.elements){
  const [x,y,z]=e.to.map((v,i)=>v-e.from[i]);
  assert.ok([x,y,z].every(q=>q>0));
  assert.ok(e.from.every(q=>q>=0)&&e.to.every(q=>q<=16));
  for(const [side,f] of Object.entries(e.faces)){
   const d=side==='up'||side==='down'?[x,z]:side==='east'||side==='west'?[z,y]:[x,y];
   assert.deepEqual([f.uv[2]-f.uv[0],f.uv[3]-f.uv[1]],d,path+' '+side);
   count++;
  }
 }
 return model;
};
for(const id of ids){
 const U=id.toUpperCase();
 assert.ok(block.includes('BLOCKS.register("'+id+'"'));
 assert.ok(item.includes('ITEM_'+U+' = fromBlock('+U+')'));
 assert.ok(tab.includes('ITEM_'+U+'.get()'));
 assert.ok(tags.values.includes('magneticraft2:'+id));
 assert.ok(lang['block.magneticraft2.'+id]);
 assert.ok(book.pages.some(p=>p.recipe==='magneticraft2:'+id));
 assert.equal(j('data/magneticraft2/loot_tables/blocks/'+id+'.json').pools[0].entries[0].name,'magneticraft2:'+id);
 const recipe=j('data/magneticraft2/recipes/'+id+'.json');
 assert.equal(recipe.type,'minecraft:crafting_shaped');
 assert.equal(recipe.result.count,4);
 assert.equal(recipe.result.item,'magneticraft2:'+id);
}
const source=c('common/block/stage/iron/IronFloorGratingBlock.java');
assert.ok(source.includes('implements SimpleWaterloggedBlock'));
const dims=[...source.matchAll(/Block\.box\(([^)]+)\)/g)].map(m=>m[1].split(',').map(x=>Number(x.trim())));
const deck=checkCuboids('assets/magneticraft2/models/block/iron_floor_grating.json');
assert.equal(deck.elements.length,19);
assert.deepEqual(dims,deck.elements.map(x=>[...x.from,...x.to]),'Collision precisely matches 19 physical bars');
const g=j('assets/magneticraft2/blockstates/iron_floor_grating.json').variants;
assert.equal(Object.keys(g).length,2);
assert.deepEqual(Object.keys(g),['waterlogged=false','waterlogged=true']);
const fence=j('data/minecraft/tags/blocks/fences.json');
assert.equal(fence.replace,false);
assert.ok(fence.values.includes('magneticraft2:iron_catwalk_railing'));
const multipart=j('assets/magneticraft2/blockstates/iron_catwalk_railing.json').multipart;
assert.equal(multipart.length,5);
for(const dir of ['north','east','south','west'])assert.ok(multipart.some(x=>x.when?.[dir]==='true'));
assert.equal(checkCuboids('assets/magneticraft2/models/block/iron_catwalk_railing_post.json').elements.length,1);
assert.equal(checkCuboids('assets/magneticraft2/models/block/iron_catwalk_railing_side.json').elements.length,2);
assert.equal(checkCuboids('assets/magneticraft2/models/block/iron_catwalk_railing_inventory.json').elements.length,5);
const gr=j('data/magneticraft2/recipes/iron_floor_grating.json');
assert.equal(gr.pattern.join('').split('P').length-1,2);
assert.equal(gr.pattern.join('').split('B').length-1,2);
const ra=j('data/magneticraft2/recipes/iron_catwalk_railing.json');
assert.equal(ra.pattern.join('').split('R').length-1,4);
assert.equal(ra.pattern.join('').split('B').length-1,2);
console.log('Iron Catwalk source/model/JSON contract PASS; '+count+' native-UV faces, 19 collision bars (NOT a Forge build or in-game test).');
