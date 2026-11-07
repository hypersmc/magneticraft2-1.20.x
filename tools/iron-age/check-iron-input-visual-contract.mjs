#!/usr/bin/env node
// Source/JSON fixture only. No claims of Forge compilation or game testing.
import assert from 'node:assert/strict';
import fs from 'node:fs';
const base='src/main/';
const j=p=>JSON.parse(fs.readFileSync(base+'resources/'+p,'utf8'));
const s=p=>fs.readFileSync(base+'java/com/magneticraft2/'+p,'utf8');
const tag=j('data/magneticraft2/tags/blocks/iron_input_visual_controllers.json');
assert.equal(tag.replace,false);
assert.deepEqual(tag.values,['magneticraft2:iron_stock_shear']);
const module=s('common/blockentity/stage/copper/MechanicalInputModuleBlockEntity.java');
assert.ok(module.includes('getLinkedControllerPosition()'));
assert.ok(module.includes('linkedControllerPosition = new BlockPos(controllerX, controllerY, controllerZ)'));
assert.ok(module.includes('return 16.0F'));
const ber=s('client/render/blocks/stage/copper/MechanicalInputModuleBlockEntityRenderer.java');
assert.ok(ber.includes('IRON_VISUAL_CONTROLLERS'));
assert.ok(ber.includes('getLinkedControllerPosition()'));
assert.ok(ber.includes('ironVisual ? IRON_SHAFT_MODEL : SHAFT_MODEL'));
const state=j('assets/magneticraft2/blockstates/mechanical_input_module.json').variants;
assert.equal(Object.keys(state).length,24);
let stopped=0,dynamic=0;
for(const [k,v] of Object.entries(state)){
 if(k.includes('formed=false')&&k.includes('rotating=false')){
  assert.equal(v.model,'magneticraft2:block/mechanical_input_module');stopped++;
 }else{
  assert.equal(v.model,'magneticraft2:block/invis');dynamic++;
 }
}
assert.equal(stopped,6);assert.equal(dynamic,18);
const iron=j('assets/magneticraft2/models/block/mechanical_input_module_housing_iron.json');
const copper=j('assets/magneticraft2/models/block/mechanical_input_module_housing.json');
assert.equal(iron.elements.length,8);
for(let i=0;i<4;i++){
 assert.deepEqual(iron.elements[i].from,copper.elements[i].from);
 assert.deepEqual(iron.elements[i].to,copper.elements[i].to);
}
let faces=0;
for(const e of iron.elements){
 const [w,h,d]=e.to.map((v,i)=>v-e.from[i]);
 assert.ok([w,h,d].every(x=>x>0));
 for(const [face,f] of Object.entries(e.faces)){
  const want=['up','down'].includes(face)?[w,d]:['east','west'].includes(face)?[d,h]:[w,h];
  assert.deepEqual([f.uv[2]-f.uv[0],f.uv[3]-f.uv[1]],want);faces++;
 }
}
const book=j('assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/ironage.json');
assert.ok(book.pages.some(p=>p.text?.includes('Iron machine drive bearing')));
console.log('Iron mechanical input visual fixture PASS: '+faces+' UV-safe faces; copper/bronze module untouched. No Forge build.');
