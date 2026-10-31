#!/usr/bin/env node
/** Ore Roasting Kiln structural/visual contract: static checks, not runtime proof. */
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {makeAssets,typeAt,run} from './generate.mjs';
import {makeRoasterPngs} from './texture-art.mjs';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const base='src/main/resources/';
const get=p=>fs.readFileSync(path.join(root,p),'utf8');
const j=p=>JSON.parse(get(p));
const assets=makeAssets();
assert.equal(assets.size,684);
assert.equal(run('--check'),684);
for(const [name,png] of Object.entries(makeRoasterPngs())){
 const file=path.join(root,base+'assets/magneticraft2/textures/block/ore_roasting_kiln/'+name+'.png');
 assert.deepEqual(fs.readFileSync(file),Buffer.from(png));
}
const dirs=['south','west','north','east'];
const viz=get('tools/ore-roasting-kiln/visuals.mjs');
for(const tex of ['minecraft:block/bricks','minecraft:block/stone_bricks',
                  'minecraft:block/deepslate_tiles','minecraft:block/spruce_planks'])
 assert.ok(viz.includes(tex),'Missing reference material '+tex);
assert.ok(!viz.includes('gear_')&&!viz.includes('shaft_'),
  'Unrequested large external gear/shaft');
let countModels=0,arches=0;
for(const dir of dirs){
 const bp=j(base+'data/magneticraft2/multiblocks/ore_roasting_kiln_'+dir+'.json');
 assert.deepEqual(bp.structure.dimensions,
  dir==='south'||dir==='north'?[5,6,9]:[9,6,5]);
 assert.equal(Object.values(bp.structure.layout).flat(2).length,270);
 for(const role of ['C','U','O'])
  assert.equal(Object.values(bp.structure.layout).flat(2).filter(v=>v===role).length,1);
 assert.equal(bp.structure.layout.layer5.flat().filter(v=>v==='A').length,1);
 assert.equal(bp.structure.layout.layer6.flat().filter(v=>v==='B').length,8);
 assert.equal(bp.structure.layout.layer6.flat().filter(v=>v==='A').length,37);
 assert.equal(bp.settings.voxel_profile,'magneticraft2:ore_roasting_kiln');
 assert.deepEqual(bp.settings.render_air_anchors,[]);
 const flat=(y)=>bp.structure.layout['layer'+(y+1)];
 let controller,flue;
 for(let y=0;y<6;y++)for(let z=0;z<flat(y).length;z++)for(let x=0;x<flat(y)[z].length;x++){
  if(flat(y)[z][x]==='C')controller=[x,y,z];
  if(y===4&&flat(y)[z][x]==='A')flue=[x,y,z];
 }
 assert.ok(controller&&flue);
 const dx=flue[0]-controller[0],dz=flue[2]-controller[2];
 assert.equal(flue[1]-controller[1],4);
 assert.ok((Math.abs(dx)===7&&dz===0)||(Math.abs(dz)===7&&dx===0),
 'Flue should be 7 cells toward rear: '+dir);
 for(const [p,model] of assets){
  if(!p.includes('models/multiblock/ore_roasting_kiln_'+dir+'/'))continue;
  countModels++;
  assert.ok(model.elements.length>0&&model.elements.length<40,'Element budget '+p);
  if(p.includes('/part_')&&model.elements.some(e=>Object.values(e.faces).some(f=>f.texture==='#ember')))
   arches++;
  for(const el of model.elements){
   for(let a=0;a<3;a++)
    assert.ok(el.from[a]>=0&&el.to[a]<=16&&el.from[a]<el.to[a],p);
   for(const [face,f] of Object.entries(el.faces)){
    assert.ok(model.textures[f.texture.slice(1)],'Texture alias missing '+p);
    const axes=face==='north'||face==='south'?[0,1]:
      face==='east'||face==='west'?[2,1]:[0,2];
    assert.ok(Math.abs(f.uv[2]-f.uv[0]-(el.to[axes[0]]-el.from[axes[0]]))<.001,'Stretched U '+p);
    assert.ok(Math.abs(f.uv[3]-f.uv[1]-(el.to[axes[1]]-el.from[axes[1]]))<.001,'Stretched V '+p);
   }
  }
 }
}
assert.equal(countModels,676);
assert.equal(arches,144);
assert.equal(typeAt(0,4,-7),'A');
assert.equal(typeAt(0,5,-7),'A');
assert.equal(typeAt(1,5,-7),'B');
const vox=j(base+'data/magneticraft2/multiblock_voxels/ore_roasting_kiln.json');
assert.equal(vox.collision.length,301);
assert.deepEqual(vox.selection,vox.collision);
assert.equal(vox.collision.filter(v=>v[1]===64).length,176);
const book=j(base+'assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json');
const page=book.pages.find(p=>p.name==='Ore Roasting Kiln');
assert.equal(page.multiblock.pattern.length,6);
const south=j(base+'data/magneticraft2/multiblocks/ore_roasting_kiln_south.json');
for(let y=0;y<6;y++){
 const expected=south.structure.layout['layer'+(6-y)]
   .map(r=>r.map(c=>c==='A'?'_':c==='C'?'0':c).reverse().join(''));
 assert.deepEqual(page.multiblock.pattern[y],expected,'Patchouli layer mismatch '+y);
}
const entity=get('src/main/java/com/magneticraft2/common/blockentity/stage/bronze/OreRoastingKilnBlockEntity.java');
const block=get('src/main/java/com/magneticraft2/common/block/stage/bronze/OreRoastingKilnBlock.java');
assert.ok(entity.includes('port.linkFormedController(worldPosition)'));
assert.ok(entity.includes('acceptsRecipeInput(ItemStack stack) { return false; }'));
assert.ok(!block.includes('::serverTick'));
console.log('Ore Roasting Kiln 5x6x9 static source contracts passed; in-game not verified.');
