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
let partModels=0,recesses=0,faceCount=0;
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
  if(!p.endsWith('/part_0_0_0.json')&&
   m.elements.some(e=>Object.values(e.faces).some(f=>f.texture==='#soot'))&&
   m.elements.some(e=>Object.values(e.faces).some(f=>f.texture==='#grate')))
  recesses++;
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
// Side grilles now occupy the middle wall layer (48 models); the front
// charging door retains 24 layers. Lower courses must be solid masonry.
assert.equal(recesses,72);
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
  const bars=m.elements.filter(e=>Object.values(e.faces)
    .some(f=>f.texture==='#grate'));
  assert.equal(bars.length,y===2?1:0,
   'Roasting bay should have one short guard bar per middle-layer half '+
     x+','+y+','+z);
 }
const custom=[...materialTextures].filter(x=>x.startsWith('magneticraft2:block/ore_roasting_kiln/'));
assert.deepEqual(custom,[],'Machine-specific custom textures on baked body');
for(const name of [
 'minecraft:block/bricks','minecraft:block/stone_bricks','minecraft:block/magma',
 'minecraft:block/black_concrete','minecraft:block/polished_deepslate',
 'minecraft:block/polished_blackstone'])
 assert.ok(materialTextures.has(name),'Missing intended vanilla texture '+name);

/** Side-wall screenshot regressions: continuous lower masonry without
 * ground-facing holes, three short and wide furnace throats, heavy piers,
 * two short guards per full bay and the unchanged roof header. */
const at=(x,y,z)=>assets.get(base+
 'assets/magneticraft2/models/multiblock/ore_roasting_kiln_south/part_'+x+'_'+y+'_'+z+'.json');
const contains=(m,from,to,texture)=>m.elements.some(e=>
 e.from.every((n,i)=>Math.abs(n-from[i])<.001)&&
 e.to.every((n,i)=>Math.abs(n-to[i])<.001)&&
 Object.values(e.faces).some(f=>f.texture==='#'+texture));
// Lower/middle pier must read as masonry, not a floor-to-roof timber slat.
for(const x of [-2,2])for(const z of [-3,-6])
 for(const y of [1,2]){
  const pier=at(x,y,z);
  assert.equal(pier.elements.length,1,'Extra geometry on brick pier '+x+','+y+','+z);
  assert.ok(!pier.elements.some(e=>Object.values(e.faces)
    .some(f=>f.texture==='#timberEnd')),
   'Full-height timber divider returned '+x+','+y+','+z);
 }
assert.ok(contains(at(2,2,-3),[0,0,0],[16,16,16],'brick'),
 'Heavy brick pier must occupy the full lower/middle wall');
assert.ok(contains(at(2,3,-3),[12,12,0],[16,16,16],'timber'),
 'Continuous roof-bearing timber header missing');
assert.ok(contains(at(2,3,-3),[12,7,5],[16,10,11],'timberEnd')&&
          contains(at(2,3,-3),[12,10,5],[16,12,11],'iron'),
 'Short upper timber corbel and iron collar must meet the continuous header');
assert.ok(contains(at(2,3,-3),[12,0,5],[16,7,11],'brick'),
 'The brick pier must support its corbel from underneath');
assert.ok(contains(at(2,2,0),[12,0,12],[16,16,16],'timberEnd'),
 'Front corner post not confined to a real 4x4 footprint');
assert.ok(!contains(at(2,2,0),[12,0,0],[16,16,16],'timberEnd'),
 'Full-width timber panel has returned at the corner');
assert.ok(contains(at(2,3,0),[0,12,12],[12,16,16],'timber'),
 'Front gable crossbeam missing at the corner');
assert.ok(contains(at(0,3,0),[0,12,12],[16,16,16],'timber'),
 'Crossbeam must span the middle of the front gable');
assert.ok(contains(at(2,1,-1),[10,0,0],[16,16,16],'brick'),
 'Lower roasting wall must be solid all the way across each bay');
assert.ok(contains(at(2,2,-1),[10,0,0],[16,4,14],'brick')&&
          contains(at(2,2,-1),[10,4,0],[16,5,14],'refractory'),
 'The lower firebed should stand on masonry with a thin stone sill');
assert.ok(contains(at(2,3,-1),[14,2,13],[16,3,16],'arch')&&
          contains(at(2,3,-1),[14,3,11],[16,4,16],'arch')&&
          contains(at(2,3,-1),[14,4,9],[16,5,16],'arch')&&
          contains(at(2,3,-1),[14,5,7],[16,6,16],'arch')&&
          contains(at(2,3,-1),[14,6,0],[16,8,16],'arch'),
 'A lower segmental arch must spread across the 2-block-wide bay');
for(const z of [-8,-7,-5,-4,-2,-1])for(const y of [1,2,3]){
 const bay=at(2,y,z),bars=bay.elements.filter(e=>
   Object.values(e.faces).some(f=>f.texture==='#grate'));
 assert.equal(bars.length,y===2?1:0,
  'Only the middle furnace layer should carry short protective bars '+y+','+z);
}
assert.ok(contains(at(2,2,-1),[13,5,7],[15,14,9],'grate'),
 'Short guards must stay below the arch instead of looking like window bars');
assert.ok(contains(at(2,3,-1),[12,12,0],[16,16,16],'timber'),
 'The arched roasting chamber lacks its continuous timber header');
for(const [x,a,b] of [[-1,8,16],[0,0,16],[1,0,8]]){
 assert.ok(contains(at(x,2,0),[a,0,15.1],[b,12,15.55],'iron'),
  'Front charging door is not framed across its full two-block width: '+x);
}
// All four facings get the same local side-wall geometry, with no positive
// overlap between solid boxes; only the original side part models change.
const rotateCell=(x,z,d)=>d==='south'?[x,z]:d==='west'?[-z,x]:
 d==='north'?[-x,-z]:[z,-x];
for(const d of dirs)for(const x of [-2,2])for(let z=-8;z<0;z++)
 for(let y=1;y<=3;y++){
  const [rx,rz]=rotateCell(x,z,d);
  const m=assets.get(base+'assets/magneticraft2/models/multiblock/ore_roasting_kiln_'+
    d+'/part_'+rx+'_'+y+'_'+rz+'.json');
  assert.ok(m,'Missing mirrored side geometry '+d+' '+x+','+y+','+z);
  for(let a=0;a<m.elements.length;a++)for(let b=a+1;b<m.elements.length;b++){
   const u=m.elements[a],v=m.elements[b];
   assert.ok(u.from.some((n,i)=>Math.min(u.to[i],v.to[i])-
       Math.max(n,v.from[i])<=.001),
    'Intersecting side-wall solids '+d+' '+x+','+y+','+z);
  }
 }
// The previous screenshot showed daylight under the outer jambs. Check
// EXPOSED FACE COVERAGE (not just collisions) across the entire bottom
// roasting-wall layer, in both mirrored sides and all four rotations.
const outward={
 south:{'2':'east','-2':'west'},west:{'2':'south','-2':'north'},
 north:{'2':'west','-2':'east'},east:{'2':'north','-2':'south'}
};
for(const d of dirs)for(const x of [-2,2])for(let z=-8;z<0;z++){
 const [rx,rz]=rotateCell(x,z,d);
 const m=assets.get(base+'assets/magneticraft2/models/multiblock/ore_roasting_kiln_'+
   d+'/part_'+rx+'_1_'+rz+'.json');
 const face=outward[d][String(x)],outer=(face==='east'||face==='south');
 const edge=outer?'to':'from',axis=(face==='east'||face==='west')?0:2;
 const across=axis===0?2:0;
 for(const u of [0.5,2.5,7.5,12.5,15.5])
  for(const y of [0.5,5.5,10.5,15.5]){
   const covered=m.elements.some(e=>e.faces[face]&&
    Math.abs(e[edge][axis]-(outer?16:0))<.001&&
    e.from[across]<=u&&u<e.to[across]&&
    e.from[1]<=y&&y<e.to[1]);
   assert.ok(covered,'Visible hole at kiln foot: '+d+' '+x+','+z+
     ' horizontal='+u+' y='+y);
  }
}
const middleRoof=at(0,4,-4);
assert.deepEqual(middleRoof.elements.filter(e=>e.faces.up?.texture==='#roof')
 .map(e=>e.to[0]-e.from[0]),[8,8],
 '4px corrugated roof stripes have returned');
assert.ok(middleRoof.elements.some(e=>
 Object.values(e.faces).some(f=>f.texture==='#roof')),
 'Stone-tile roof courses missing');
assert.ok(!middleRoof.elements.some(e=>
 Object.values(e.faces).some(f=>f.texture==='#roofTrim')),
 'Dark roof bands have returned');
const frontRoof=at(0,4,0);
assert.ok(frontRoof.elements.some(e=>e.from[2]===14&&e.to[2]===16&&
 Object.values(e.faces).some(f=>f.texture==='#roofTrim')),
 'Front gable roof fascia missing');
assert.ok(frontRoof.elements.some(e=>e.faces.south?.texture==='#brick'),
 'Dark full-height gable fascia has returned; brick masonry must be visible');
assert.ok(contains(at(0,4,-8),[0,14,12],[4,16,16],'iron'),
 'Narrow chimney flashing must sit on the actual 14px roof course');
assert.ok(!contains(at(0,4,-8),[0,0,0],[16,16,16],'brick'),
 'Solid 3x3 chimney pedestal has returned');
assert.ok(contains(at(-1,6,-7),[12,0,0],[16,11,16],'brick')&&
          contains(at(-1,6,-7),[12,11,0],[16,13,16],'iron')&&
          contains(at(-1,6,-7),[12,13,0],[16,16,16],'brick'),
 'The 4px upper flue wall must keep a mechanically joined iron belt');
assert.equal(typeAt(0,4,-7),'A','Flue must remain open through roof');
const chimneyBoxes=assets.get(base+'data/magneticraft2/multiblock_voxels/ore_roasting_kiln.json').collision;
assert.ok(chimneyBoxes.some(v=>v[1]===78&&v[4]===80&&v[5]-v[2]===4),
 'Chimney base collision must rise just above matching slate course');
const voxel=get(base+'data/magneticraft2/multiblock_voxels/ore_roasting_kiln.json');
assert.equal(voxel.collision.length,167);
assert.deepEqual(voxel.selection,voxel.collision);
assert.equal(voxel.collision.filter(v=>v[4]===80&&v[1]>64).length,14);
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
console.log('Ore Roasting Kiln 5x8x9: closed side-wall foot, low roasting throats, short guards, 4 facings, UVs, overlaps, roof, book and port contract passed (static only).');
