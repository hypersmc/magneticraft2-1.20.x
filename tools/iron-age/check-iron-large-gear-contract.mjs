#!/usr/bin/env node
// Static source and resource fixture; NOT a Java/Forge build.
import fs from 'node:fs';
import assert from 'node:assert/strict';
const root='src/main/',j=p=>JSON.parse(fs.readFileSync(root+'resources/'+p,'utf8'));
const s=p=>fs.readFileSync(root+'java/com/magneticraft2/'+p,'utf8');
const id='wrought_iron_gear_large',item='magneticraft2:'+id;
const block=s('common/block/stage/iron/WroughtIronLargeGearBlock.java');
const be=s('common/blockentity/stage/iron/WroughtIronLargeGearBlockEntity.java');
assert.ok(block.includes('extends LargeGearBlock_wood'));
assert.ok(be.includes('extends LargeGearBlockEntity_wood'));
assert.ok(be.includes('return 24.0F'));
assert.ok(s('common/registry/registers/BlockRegistry.java').includes('BLOCKS.register("'+id+'"'));
assert.ok(s('common/registry/registers/BlockEntityRegistry.java').includes('BLOCK_ENTITIES.register("'+id+'"'));
assert.ok(s('client/Clientsetup.java').includes('GEAR_LARGE_BE_IRON'));
const lathe=s('common/blockentity/stage/bronze/BronzeMechanicalLatheBlockEntity.java');
assert.ok(lathe.includes('ITEM_IRON_GEAR_BLANK_LARGE'));
assert.ok(lathe.includes('ITEM_PRECISION_IRON_GEAR_LARGE'));
assert.ok(lathe.includes('cutterCost'));
const blank=j('data/magneticraft2/recipes/iron_gear_blank_large.json');
assert.deepEqual(blank.pattern,['PBP','BFB','PBP']);
assert.equal(blank.key.F.item,'magneticraft2:riveted_iron_frame');
const machine=j('data/magneticraft2/recipes/lathe_precision_iron_gear_large.json');
assert.equal(machine.process_time,160);
assert.equal(machine.mechanical.min_speed,12);
assert.equal(machine.mechanical.torque,8);
assert.equal(machine.input.item,'magneticraft2:iron_gear_blank_large');
assert.equal(machine.output.item,'magneticraft2:precision_iron_gear_large');
const finish=j('data/magneticraft2/recipes/wrought_iron_gear_large.json');
assert.equal(finish.result.item,item);
assert.equal(finish.key.S.item,'magneticraft2:wrought_iron_shaft');
const state=j('assets/magneticraft2/blockstates/'+id+'.json').variants;
assert.equal(Object.keys(state).length,7);
assert.ok(Object.values(state).filter(v=>v.model==='magneticraft2:block/'+id).length===6);
let n=0;let rotated=0;
for(const file of ['models/block/wrought_iron_gear_large.json','models/item/iron_gear_blank_large.json','models/item/precision_iron_gear_large.json']){
const model=j('assets/magneticraft2/'+file);
assert.equal(model.textures.iron,'magneticraft2:block/material/dark_work_iron');
for(const el of model.elements){
 const [w,h,d]=el.to.map((v,i)=>v-el.from[i]);
 assert.ok([w,h,d].every(x=>x>0));
 if(el.rotation){assert.equal(el.rotation.axis,'y');assert.ok([-45,-22.5,22.5,45].includes(el.rotation.angle));rotated++;}
 for(const [face,uv] of Object.entries(el.faces)){
  const expected=face==='up'||face==='down'?[w,d]:(face==='east'||face==='west')?[d,h]:[w,h];
  assert.deepEqual([uv.uv[2]-uv.uv[0],uv.uv[3]-uv.uv[1]],expected);
  n++;
 }
}
}
const gear=j('assets/magneticraft2/models/block/'+id+'.json');
assert.equal(gear.elements.filter(e=>e.rotation).length,12);
assert.ok(gear.elements.some(e=>e.from.join(',')==='7,0,7'&&e.to.join(',')==='9,16,9'));
const old=j('data/magneticraft2/recipes/lathe_precision_iron_gear_medium.json');
assert.equal(old.process_time,160);
assert.equal(old.output.item,'magneticraft2:precision_iron_gear_medium');
assert.equal(j('data/magneticraft2/recipes/gear_large_wood.json').result.item,'magneticraft2:gear_large_wood');
console.log('Large Gear static fixture PASS: '+n+' native UV faces, '+rotated+' rotated elements. No Forge build or in-game tests.');
