#!/usr/bin/env node
/** Static K1 design contract; does NOT replace a Forge build or in-game review. */
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {makeAssets,run} from './generate.mjs';
import {makeRoasterPngs,roasterTiles} from './texture-art.mjs';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const json=p=>JSON.parse(read(p));
const base='src/main/resources/',assets=makeAssets();
assert.equal(assets.size,460);
assert.equal(run('--check'),460);
for(const [name,content] of Object.entries(makeRoasterPngs())){
 assert.ok(roasterTiles.includes(name));
 const file=path.join(root,base+'assets/magneticraft2/textures/block/ore_roasting_kiln/'+name+'.png');
 assert.ok(fs.existsSync(file),'Missing original art: '+name);
 assert.deepEqual(fs.readFileSync(file),Buffer.from(content),'Stale original art: '+name);
 assert.equal(Buffer.from(content).readUInt32BE(16),16);
 assert.equal(Buffer.from(content).readUInt32BE(20),16);
}
const dirs=['south','west','north','east'],expected=[5,3,9],rev=[9,3,5];
let models=0,arches=0;
for(const d of dirs){
 const blueprint=json(base+'data/magneticraft2/multiblocks/ore_roasting_kiln_'+d+'.json');
 assert.deepEqual(blueprint.structure.dimensions,
   d==='south'||d==='north'?expected:rev);
 assert.equal(Object.values(blueprint.structure.layout).flat(2).length,135);
 for(const role of ['C','U','O'])
  assert.equal(Object.values(blueprint.structure.layout).flat(2).filter(c=>c===role).length,1);
 assert.equal(blueprint.settings.voxel_profile,'magneticraft2:ore_roasting_kiln');
 assert.deepEqual(blueprint.settings.render_air_anchors,[]);
 for(const [p,o] of assets){
  if(!p.includes('models/multiblock/ore_roasting_kiln_'+d+'/'))continue;
  models++;
  assert.ok(o.elements.length>0&&o.elements.length<=24,'Detail budget '+p);
  for(const e of o.elements){
   for(let a=0;a<3;a++)
    assert.ok(e.from[a]>=0&&e.to[a]<=16&&e.from[a]<e.to[a],p);
   for(const [face,f] of Object.entries(e.faces)){
    assert.ok(o.textures[f.texture.slice(1)],'Missing texture alias '+p);
    const axes=face==='north'||face==='south'?[0,1]:
      face==='east'||face==='west'?[2,1]:[0,2];
    assert.ok(Math.abs(f.uv[2]-f.uv[0]-(e.to[axes[0]]-e.from[axes[0]]))<.001,
      'Stretched U '+p);
    assert.ok(Math.abs(f.uv[3]-f.uv[1]-(e.to[axes[1]]-e.from[axes[1]]))<.001,
      'Stretched V '+p);
   }
  }
  if(p.includes('/part_')&&o.elements.some(e=>
    Object.values(e.faces).some(f=>f.texture==='#ember')))arches++;
 }
}
assert.equal(models,452); // 4*(111 solid part models + two genuine port models)
assert.equal(arches,24);  // 3 vent arches per long side * 2 sides * 4 facings
const voxel=json(base+'data/magneticraft2/multiblock_voxels/ore_roasting_kiln.json');
assert.equal(voxel.collision.length,113);
assert.equal(voxel.selection.length,113);
assert.ok(voxel.collision.some(box=>box[1]===32&&box[4]===39),
 'Roof eaves should be only seven pixels high');
assert.ok(voxel.collision.some(box=>box[1]===32&&box[4]===47),
 'Ridge must remain in the 3-block envelope');
const book=json(base+'assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json');
const page=book.pages.find(p=>p.name==='Ore Roasting Kiln');
assert.ok(page&&page.enable_visualize&&page.multiblock.pattern.length===3);
assert.ok(page.multiblock.pattern.every(layer=>layer.length===9));
// Every orientation must have exactly one roof AIR cell and must locate
// it seven blocks behind the front controller, two blocks above it.
// A brick in that AIR cell must make formation fail (real exhaust opening).
for(const dir of dirs){
 const layout=json(base+'data/magneticraft2/multiblocks/ore_roasting_kiln_'+dir+'.json').structure.layout;
 const air=[];
 const controllers=[];
 for(let y=0;y<3;y++)for(let z=0;z<layout['layer'+(y+1)].length;z++)
  for(let x=0;x<layout['layer'+(y+1)][z].length;x++){
   const cell=layout['layer'+(y+1)][z][x];
   if(cell==='A'&&y===2)air.push([x,y,z]);
   if(cell==='C')controllers.push([x,y,z]);
  }
 assert.equal(air.length,1,'Exactly one roof exhaust AIR required: '+dir);
 assert.equal(controllers.length,1,'Exactly one controller required: '+dir);
 const [ax,ay,az]=air[0],[cx,cy,cz]=controllers[0];
 assert.equal(ay-cy,2,'Exhaust at wrong height: '+dir);
 const dx=ax-cx,dz=az-cz;
 assert.ok((Math.abs(dx)===7&&dz===0)||(Math.abs(dz)===7&&dx===0),
  'Exhaust must be seven blocks behind the controller: '+dir);
}
assert.match(page.text,/Leave that block completely EMPTY/);

const south=json(base+'data/magneticraft2/multiblocks/ore_roasting_kiln_south.json');
for(let y=0;y<3;y++){
 const expectedRows=south.structure.layout['layer'+(3-y)]
  .map(row=>row.map(c=>c==='A'?'_':c==='C'?'0':c).reverse().join(''));
 assert.deepEqual(page.multiblock.pattern[y],expectedRows,'Patchouli formation mismatch');
}
const block=read('src/main/java/com/magneticraft2/common/block/stage/bronze/OreRoastingKilnBlock.java');
const entity=read('src/main/java/com/magneticraft2/common/blockentity/stage/bronze/OreRoastingKilnBlockEntity.java');
const ports=read('src/main/java/com/magneticraft2/common/blockentity/stage/copper/MultiblockItemPortBlockEntity.java');
assert.ok(entity.includes('port.linkFormedController(worldPosition)'));
assert.ok(entity.includes('acceptsRecipeInput(ItemStack stack) { return false; }'));
assert.ok(entity.includes('MultiblockRenderParts.assign('));
assert.ok(!block.includes('::serverTick'),'Structural prototype must not tick');
assert.ok(ports.includes('instanceof OreRoastingKilnBlockEntity roaster'));
assert.ok(ports.includes('"ore_roasting_kiln_"'));
console.log('Ore Roasting Kiln static source checks passed; Forge/in-game NOT verified');
