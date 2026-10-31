#!/usr/bin/env node
/** Static generation/regression checks for the 5x8x9 Ore Roasting Kiln. */
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {makeAssets,typeAt,run} from './generate.mjs';
import {makeRoasterPngs} from './texture-art.mjs';
import {modelBox,isRoastingBay} from './visuals.mjs';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const base='src/main/resources/';
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const get=p=>JSON.parse(read(p));
const assets=makeAssets();
assert.equal(assets.size,748);
assert.equal(run('--check'),748);
for(const [name,png] of Object.entries(makeRoasterPngs())){
 const file=path.join(root,base+'assets/magneticraft2/textures/block/ore_roasting_kiln/'+name+'.png');
 assert.deepEqual(fs.readFileSync(file),Buffer.from(png),'Stale 16x16 pixel art '+name);
}
const dirs=['south','west','north','east'];
const materialTextures=new Set();
const expected={
 south:{north:'north',south:'south',west:'west',east:'east'},
 north:{north:'south',south:'north',west:'east',east:'west'},
 west:{north:'east',south:'west',west:'north',east:'south'},
 east:{north:'west',south:'east',west:'south',east:'north'}
};
for(const [dir,faces] of Object.entries(expected))
 for(const [side,mapped] of Object.entries(faces))
  assert.deepEqual(Object.keys(modelBox([1,1,1],[6,6,6],'brick',[side],dir).faces),[mapped],
  'Incorrect rotated face normal '+dir+'/'+side);
const sides={
 north:{axis:2,edge:'from',other:[0,1]},
 south:{axis:2,edge:'to',other:[0,1]},
 west:{axis:0,edge:'from',other:[2,1]},
 east:{axis:0,edge:'to',other:[2,1]},
 up:{axis:1,edge:'to',other:[0,2]},
 down:{axis:1,edge:'from',other:[0,2]}
};
let partModels=0,arches=0,faceCount=0;
for(const d of dirs){
 const bp=get(base+'data/magneticraft2/multiblocks/ore_roasting_kiln_'+d+'.json');
 assert.deepEqual(bp.structure.dimensions,
  d==='south'||d==='north'?[5,8,9]:[9,8,5]);
 assert.equal(Object.values(bp.structure.layout).flat(2).length,360);
 for(const role of ['C','U','O'])
  assert.equal(Object.values(bp.structure.layout).flat(2).filter(x=>x===role).length,1);
 for(const name of ['layer6','layer7','layer8']){
  assert.equal(bp.structure.layout[name].flat().filter(x=>x==='B').length,8);
  assert.equal(bp.structure.layout[name].flat().filter(x=>x==='A').length,37);
 }
 assert.equal(bp.structure.layout.layer5.flat().filter(x=>x==='A').length,1);
 assert.equal(bp.settings.voxel_profile,'magneticraft2:ore_roasting_kiln');
 const prefix=base+'assets/magneticraft2/models/multiblock/ore_roasting_kiln_'+d+'/';
 for(const [p,m] of assets){
  if(!p.startsWith(prefix)||!p.includes('/part_'))continue;
  partModels++;
  if(m.elements.some(e=>Object.values(e.faces).some(f=>f.texture==='#soot')))arches++;
  assert.ok(m.elements.length>0&&m.elements.length<40,'Element budget '+p);
  const planes=[];
  for(const e of m.elements){
   for(let i=0;i<3;i++)assert.ok(e.from[i]>=0&&e.to[i]<=16&&e.from[i]<e.to[i],
    'Invalid local box '+p);
   for(const [face,f] of Object.entries(e.faces)){
    faceCount++;
    const name=f.texture.slice(1);
    assert.ok(m.textures[name],'Missing texture alias '+p+'/'+name);
    materialTextures.add(m.textures[name]);
    const cfg=sides[face],axis=cfg.other;
    assert.ok(Math.abs(f.uv[2]-f.uv[0]-(e.to[axis[0]]-e.from[axis[0]]))<.001,
     'Stretched U '+p);
    assert.ok(Math.abs(f.uv[3]-f.uv[1]-(e.to[axis[1]]-e.from[axis[1]]))<.001,
     'Stretched V '+p);
    planes.push({face,depth:e[cfg.edge][cfg.axis],
      x:[e.from[axis[0]],e.to[axis[0]]],
      y:[e.from[axis[1]],e.to[axis[1]]]});
   }
  }
  // Z-fighting regression: no two same-normal face rectangles may overlap
  // on the same depth plane within one baked block model.
  for(let a=0;a<planes.length;a++)for(let b=a+1;b<planes.length;b++){
   const x=planes[a],y=planes[b];
   if(x.face!==y.face||Math.abs(x.depth-y.depth)>.00001)continue;
   const dx=Math.min(x.x[1],y.x[1])-Math.max(x.x[0],y.x[0]);
   const dy=Math.min(x.y[1],y.y[1])-Math.max(x.y[0],y.y[0]);
   assert.ok(dx<=.001||dy<=.001,'Coplanar fighting '+p+' '+x.face);
  }
 }
}
assert.equal(partModels,732);
assert.equal(arches,144);
assert.ok(faceCount>8000);
assert.equal(typeAt(0,4,-7),'A');
for(let y=5;y<=7;y++){
 assert.equal(typeAt(0,y,-7),'A');
 assert.equal(typeAt(1,y,-7),'B');
}
for(const x of [-2,2])for(const y of [1,2,3])
 for(const z of [-8,-7,-5,-4,-2,-1]){
  const m=assets.get(base+'assets/magneticraft2/models/multiblock/ore_roasting_kiln_south/part_'+x+'_'+y+'_'+z+'.json');
  assert.ok(isRoastingBay({x,y,z}));
  assert.ok(m.elements.some(e=>Object.values(e.faces).some(f=>f.texture==='#soot')),
   'Bay missing solid dark back '+x+','+y+','+z);
  assert.ok(m.elements.some(e=>Object.values(e.faces).some(f=>f.texture==='#grate')),
   'Bay missing thick metal grille '+x+','+y+','+z);
 }
const custom=[...materialTextures].filter(x=>x.startsWith('magneticraft2:block/ore_roasting_kiln/'));
assert.deepEqual(custom,[],'Machine-specific custom textures on baked body');
for(const name of [
 'minecraft:block/bricks','minecraft:block/stone_bricks','minecraft:block/magma',
 'minecraft:block/black_concrete','minecraft:block/polished_deepslate',
 'minecraft:block/iron_block'])
 assert.ok(materialTextures.has(name),'Missing intended vanilla texture '+name);
const voxel=get(base+'data/magneticraft2/multiblock_voxels/ore_roasting_kiln.json');
assert.equal(voxel.collision.length,169);
assert.deepEqual(voxel.selection,voxel.collision);
assert.equal(voxel.collision.filter(v=>v[1]===64&&v[4]===80).length,8);
const book=get(base+'assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json');
const page=book.pages.find(p=>p.name==='Ore Roasting Kiln');
assert.equal(page.multiblock.pattern.length,8);
const south=get(base+'data/magneticraft2/multiblocks/ore_roasting_kiln_south.json');
for(let y=0;y<8;y++){
 const expected=south.structure.layout['layer'+(8-y)]
  .map(row=>row.map(c=>c==='A'?'_':c==='C'?'0':c).reverse().join(''));
 assert.deepEqual(page.multiblock.pattern[y],expected,'Patchouli mismatch '+y);
}
const entity=read('src/main/java/com/magneticraft2/common/blockentity/stage/bronze/OreRoastingKilnBlockEntity.java');
const block=read('src/main/java/com/magneticraft2/common/block/stage/bronze/OreRoastingKilnBlock.java');
assert.ok(entity.includes('port.linkFormedController(worldPosition)'));
assert.ok(entity.includes('acceptsRecipeInput(ItemStack stack) { return false; }'));
assert.ok(!block.includes('::serverTick'));
console.log('Ore Roasting Kiln 5x8x9: geometry, 4 facings, coplanarity, book and port contract passed (static only).');
