#!/usr/bin/env node
// Source/JSON contract only: neither Forge compilation nor runtime behavior.
import fs from 'node:fs';
import assert from 'node:assert/strict';
const R='src/main/resources/',J='src/main/java/com/magneticraft2/';
const j=p=>JSON.parse(fs.readFileSync(R+p,'utf8'));
const s=p=>fs.readFileSync(J+p,'utf8');
const axes=['south','west','north','east'];
let models=0,faces=0;
for(const dir of axes){
 const b=j('data/magneticraft2/multiblocks/iron_flywheel_'+dir+'.json');
 const dims=b.structure.dimensions,centre=dims.map(x=>Math.floor(x/2));
 assert.deepEqual(dims,dir==='south'||dir==='north'?[3,3,1]:[1,3,3]);
 let cells=[];
 for(let y=0;y<3;y++)for(let z=0;z<dims[2];z++)for(let x=0;x<dims[0];x++){
  let ch=b.structure.layout['layer'+(y+1)][z][x];
  cells.push({ch,rel:[x-centre[0],y-centre[1],z-centre[2]]});
 }
 assert.equal(cells.filter(v=>v.ch==='F').length,2);
 assert.equal(cells.filter(v=>v.ch==='A').length,6);
 assert.deepEqual(cells.filter(v=>v.ch==='C').map(v=>v.rel),[[0,0,0]]);
 assert.deepEqual(cells.filter(v=>v.ch==='A').map(v=>v.rel.join(',')).sort(),
   b.settings.render_air_anchors.map(v=>v.join(',')).sort());
 for(const cell of cells.filter(c=>c.ch!=='C')){
  const p='assets/magneticraft2/models/multiblock/iron_flywheel_'+dir+
   '/part_'+cell.rel.join('_')+'.json';
  const m=j(p);models++;assert.equal(m.parent,'minecraft:block/block');
  for(const e of m.elements){
   let [w,h,d]=e.to.map((v,i)=>v-e.from[i]);
   assert.ok([w,h,d].every(n=>n>0));
   assert.ok(e.from.every(n=>n>=0)&&e.to.every(n=>n<=16));
   for(const [face,f]of Object.entries(e.faces)){
    const uv=face==='up'||face==='down'?[w,d]:
      face==='east'||face==='west'?[d,h]:[w,h];
    assert.deepEqual(f.uv,[0,0,...uv]);faces++;
   }
  }
 }
}
assert.equal(models,32);
const v=j('data/magneticraft2/multiblock_voxels/iron_flywheel.json');
assert.equal(v.collision.length,37);
const center=j('assets/magneticraft2/models/block/iron_flywheel_controller.json');
assert.equal(center.elements.length,2,"No duplicate baked moving axle");
assert.deepEqual(center.elements[0].from,[5,5,11]);
assert.deepEqual(center.elements[0].to,[11,11,15]);
assert.ok(!v.collision.some(b=>JSON.stringify(b)==='[7,7,0,9,9,16]'));
assert.ok(!v.collision.some(b=>JSON.stringify(b)==='[0,7,11,16,9,15]'));
assert.deepEqual(v.collision,v.selection);
const r=j('data/magneticraft2/recipes/iron_flywheel.json');
assert.equal(r.result.item,'magneticraft2:iron_flywheel');
assert.deepEqual(r.pattern,['PBP','FGS','PBP']);
const be=s('common/blockentity/stage/iron/WroughtIronFlywheelBlockEntity.java');
const wood=s('common/blockentity/stage/copper/FlywheelBlockEntity_wood.java');
assert.ok(be.includes('implements MultiblockVoxelHost'));
assert.ok(be.includes('getInertiaTorque(){return 12F;}'));
assert.ok(be.includes('getCoastDecayScale(){return 0.40F;}'));
assert.ok(be.includes('MultiblockRenderParts.assign'));
assert.ok(be.includes('restoreAssembly()'));
assert.ok(be.includes('for(var old:ctrl.getBlockMap().entrySet())'));
assert.ok(be.includes('collision=null;'));
assert.ok(be.includes('level.setBlock(old.getKey(),old.getValue(),Block.UPDATE_ALL)'));
assert.ok(wood.includes('getInertiaTorque(){return INERTIA_TORQUE;}'));
assert.ok(s('client/render/blocks/stage/iron/WroughtIronFlywheelRenderer.java')
  .includes('FlywheelMesh.drawBox(stack,iron,light,0.92D,0.125D,0.125D,true)'));
assert.ok(s('common/block/general/Multiblockfiller.java').includes('flywheel.dismantleWheel()'));
assert.ok(j('data/minecraft/tags/blocks/mineable/pickaxe.json').values.includes('magneticraft2:iron_flywheel'));
assert.ok(j('assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/ironage.json')
  .pages.some(p=>p.name==='Wrought Iron Flywheel'));
console.log('Iron Flywheel static contract PASS: '+models+' models, '+faces+
 ' native UV faces, 37 V2 collision boxes. Build and game tests not run.');
