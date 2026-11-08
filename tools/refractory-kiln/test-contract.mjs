#!/usr/bin/env node
/**
 * K1 structural contract. Static checks cannot replace Gradle/in-game tests.
 * Run: node tools/refractory-kiln/test-contract.mjs
 */
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {makeAssets,run} from './generate.mjs';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const r=p=>fs.readFileSync(path.join(root,p),'utf8');
const j=p=>JSON.parse(r(p));
const base='src/main/resources/';
const resource=base+'assets/magneticraft2/';
const data=base+'data/magneticraft2/';
const files=makeAssets();
const artRoot=resource+'textures/block/refractory_kiln/';
const expectedTiles=['brick','plinth','roof','iron','bronze','soot','grate','door','input','output'];
for(const tile of expectedTiles){
 const png=fs.readFileSync(path.join(root,artRoot+tile+'.png'));
 assert.deepEqual([...png.subarray(0,8)],[137,80,78,71,13,10,26,10],
  'Malformed kiln PNG '+tile);
 assert.equal(png.readUInt32BE(16),16,'Kiln texture width must be 16: '+tile);
 assert.equal(png.readUInt32BE(20),16,'Kiln texture height must be 16: '+tile);
}


const itemSprites=['unfired_refractory_batch','fired_refractory_ceramic'];
for(const item of itemSprites){
 const itemModel=j(resource+'models/item/'+item+'.json');
 assert.equal(itemModel.parent,'minecraft:item/generated');
 assert.equal(itemModel.textures.layer0,'magneticraft2:item/'+item,
  'Refractory item uses placeholder texture: '+item);
 const png=fs.readFileSync(path.join(root,resource+'textures/item/'+item+'.png'));
 assert.deepEqual([...png.subarray(0,8)],[137,80,78,71,13,10,26,10],
  'Invalid item PNG: '+item);
 assert.equal(png.readUInt32BE(16),16);
 assert.equal(png.readUInt32BE(20),16);
}
assert.notEqual(
 fs.readFileSync(path.join(root,resource+'textures/item/'+itemSprites[0]+'.png')).toString('base64'),
 fs.readFileSync(path.join(root,resource+'textures/item/'+itemSprites[1]+'.png')).toString('base64'),
 'Unfired and fired ceramic must have distinct textures'
);

assert.equal(run('--check'),331,'unexpected count or stale files');
const dirs=['south','west','north','east'];
const solids=['B','S','C'];
const canonical=j(data+'multiblocks/refractory_kiln_south.json');
assert.deepEqual(canonical.structure.dimensions,[5,4,5]);
assert.equal(Object.values(canonical.structure.layout).flat(2).filter(v=>v==='C').length,1);
const cellRows=Object.values(canonical.structure.layout);
assert.deepEqual(cellRows[1][4],['B','U','B','O','B']);
assert.deepEqual(cellRows[2][4],['B','B','B','B','B']);
assert.deepEqual(cellRows[3][2],['B','B','A','B','B']);
const southCells=new Map();
for(let y=0;y<4;y++)for(let z=-4;z<=0;z++)for(let x=-2;x<=2;x++)
 southCells.set(x+','+y+','+z,canonical.structure.layout['layer'+(y+1)][z+4][x+2]);
const rotation=(x,z,d)=>d==='south'?[x,z]:d==='west'?[-z,x]:d==='north'?[-x,-z]:[z,-x];
const vox=j(data+'multiblock_voxels/refractory_kiln.json');
assert.equal(vox.collision.length,79);
assert.deepEqual(vox.collision,vox.selection);
const pv=new Set(vox.collision.map(box=>[box[0]/16,box[1]/16,box[2]/16].join(',')));
assert.equal(pv.size,79);
for(const [key,type] of southCells)assert.equal(pv.has(key),solids.includes(type),'Voxel collision occupancy differs at '+key);
for(const d of dirs){
 const name='refractory_kiln_'+d;
 const bp=j(data+'multiblocks/'+name+'.json');
 assert.equal(bp.name,name);
 assert.deepEqual(bp.structure.dimensions,[5,4,5]);
 assert.equal(bp.settings.voxel_profile,'magneticraft2:refractory_kiln');
 assert.deepEqual(bp.settings.render_air_anchors,[]);
 const rotated=new Map();
 for(const [key,type] of southCells){
  const [x,y,z]=key.split(',').map(Number),[rx,rz]=rotation(x,z,d);
  rotated.set(rx+','+y+','+rz,type);
 }
 const positions=[...rotated.keys()].map(s=>s.split(',').map(Number));
 const minX=Math.min(...positions.map(p=>p[0]));
 const minZ=Math.min(...positions.map(p=>p[2]));
 let modelCount=0,faceCount=0;
 for(let y=0;y<4;y++)for(let iz=0;iz<5;iz++)for(let ix=0;ix<5;ix++){
  const x=ix+minX,z=iz+minZ;
  const key=x+','+y+','+z;
  const expected=rotated.get(key);
  const found=bp.structure.layout['layer'+(y+1)][iz][ix];
  assert.equal(found,expected,d+': rotation mismatch at '+key);
  const file=resource+'models/multiblock/'+name+'/part_'+x+'_'+y+'_'+z+'.json';
  if(!solids.includes(expected)){
   assert.ok(!fs.existsSync(path.join(root,file)),d+': module/AIR has fake model '+key);
   continue;
  }
  const model=j(file);
  assert.equal(model.parent,'minecraft:block/block');
  for(const id of Object.values(model.textures)){
   assert.ok(id.startsWith('magneticraft2:block/refractory_kiln/') ||
    id==='magneticraft2:block/material/refractory_brick',
    'Kiln model borrows a machine-specific texture: '+file+' -> '+id);
  }
  assert.ok(model.elements.length>0 && model.elements.length<=20,'Overly busy K1 model: '+file);
  for(const el of model.elements) {
   for(let axis=0;axis<3;axis++) {
    assert.ok(el.from[axis]>=0 && el.to[axis]<=16 && el.to[axis]>el.from[axis],
      'Out of cell geometry '+file);
   }
   for(const [direction,face] of Object.entries(el.faces)){
    assert.ok(['north','south','east','west','up','down'].includes(direction));
    assert.ok(model.textures[face.texture.slice(1)],'Missing texture '+file);
    const axes=direction==='north'||direction==='south'?[0,1]:
      direction==='east'||direction==='west'?[2,1]:[0,2];
    const du=face.uv[2]-face.uv[0],dv=face.uv[3]-face.uv[1];
    assert.ok(Math.abs(du-(el.to[axes[0]]-el.from[axes[0]]))<.001,
      'Stretched UV width '+file);
    assert.ok(Math.abs(dv-(el.to[axes[1]]-el.from[axes[1]]))<.001,
      'Stretched UV height '+file);
    faceCount++;
   }
  }
  modelCount++;
 }
 assert.equal(modelCount,79,d+': wrong solid model count');
 // Controller model is present, and Item I/O is physically separate.
 const control=positions.find(p=>rotated.get(p.join(','))==='C');
 assert.ok(control);
 const [cx,cy,cz]=control;
 assert.equal(cy,0);
 assert.equal(bp.structure.blocks.U,'magneticraft2:multiblock_item_input');
 assert.equal(bp.structure.blocks.O,'magneticraft2:multiblock_item_primary_output');
 assert.equal(bp.settings.inventory,true);
 assert.equal(bp.settings.power,false);
 assert.equal(bp.settings.fuel,false);
 console.log(d+': '+modelCount+' cell models, '+faceCount+' visible faces');
}
const book=j(resource+'patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json');
const liquidPage=book.pages.find(p=>p.type==='patchouli:multiblock'&&p.name==='Refractory Kiln');
const page=book.pages.find(p=>p.type==='patchouli:multiblock'&&p.name==='Refractory Kiln — Solid Fuel');
assert.ok(liquidPage,'First Kiln book Visualize must include the liquid inlet');
assert.ok(book.pages.indexOf(liquidPage)<book.pages.indexOf(page));
assert.equal(liquidPage.multiblock.mapping.F,'magneticraft2:multiblock_fluid_input');
assert.equal(liquidPage.multiblock.pattern[2][3],'B___F');
assert.ok(page,'Missing Kiln book Visualize');
assert.equal(page.multiblock.pattern.length,4);
assert.equal(page.multiblock.pattern[1][4],'BBBBB','Front hatch must be a real wall cell');
for(let y=0;y<4;y++){
 const pageLayer=page.multiblock.pattern[3-y];
 for(let iz=0;iz<5;iz++){
  const row=canonical.structure.layout['layer'+(y+1)][iz];
  const reversed=[...row].reverse().map(x=>x==='C'?'0':x==='A'?'_':x).join('');
  assert.equal(pageLayer[iz],reversed,'Patchouli projection drift at '+y+'/'+iz);
 }
}
// Patchouli reserves '_' for empty cells; every other symbol needs a mapping.
for (const page of book.pages.filter(p=>p.type==='patchouli:multiblock')) {
 const {pattern,mapping}=page.multiblock;
 for (const layer of pattern) for (const row of layer) for (const symbol of row) {
  if (symbol==='_' || symbol===' ') continue;
  assert.ok(Object.hasOwn(mapping,symbol),
   'Unmapped Patchouli multiblock symbol '+symbol+' on page '+page.name);
 }
}
for(const dir of dirs)for(const role of ['input','primary']){
 const file=resource+'models/multiblock/refractory_kiln_'+dir+'/port_'+role+'.json';
 const model=j(file);
 assert.equal(model.parent,'minecraft:block/block');
 assert.equal(model.elements.length,7,'Ports must stay small and uncluttered: '+file);
 assert.equal(model.textures.input,'magneticraft2:block/refractory_kiln/input');
 assert.equal(model.textures.output,'magneticraft2:block/refractory_kiln/output');
 assert.ok(model.elements.some(e=>Object.values(e.faces).some(f=>f.texture==='#'+
  (role==='input'?'input':'output'))),'Port-specific marking absent: '+file);
}
// One large upper charging door, one low firebox and no secondary Y=1 control panel.
for(const dir of dirs){
 const bp=j(data+'multiblocks/refractory_kiln_'+dir+'.json');
 const models=Object.values(bp.structure.layout).flat(2);
 assert.equal(models.filter(c=>c==='C').length,1);
 const door=j(resource+'models/multiblock/refractory_kiln_'+dir+'/part_0_2_0.json');
 assert.ok(door.elements.some(e=>Object.values(e.faces).some(f=>f.texture==='#iron')),
  'Main charging door has no dark-iron face: '+dir);
}
const unformed=j(resource+'models/block/refractory_kiln_controller.json');
assert.equal(unformed.parent,'minecraft:block/block');
const unformedFaces=new Set(unformed.elements.flatMap(e=>Object.keys(e.faces)));
for(const face of ['north','east','south','west','up','down'])
 assert.ok(unformedFaces.has(face),'Standalone controller is invisible from '+face);
assert.equal(j(resource+'models/item/refractory_kiln.json').parent,
 'magneticraft2:block/refractory_kiln_controller');
const be=r('src/main/java/com/magneticraft2/common/blockentity/stage/bronze/RefractoryKilnBlockEntity.java');
assert.ok(be.includes('boolean heatcape() { return true; }'),'K2 HEAT capability is disabled');
assert.ok(be.includes('boolean fluidcape() { return false; }'));
assert.ok(be.includes('MultiblockRecipeHandler.acceptsInput(level, MACHINE, stack)'),'Kiln still rejects all inputs');
assert.ok(be.includes('return super.getCapability(cap, side);'));
assert.ok(be.includes('if (cap == ForgeCapabilities.ITEM_HANDLER || cap == ForgeCapabilities.FLUID_HANDLER)'),
 'No controller-wide item-capability bypass of the locked K1 ports');
assert.ok(be.includes('MultiblockRenderParts.assign('));
assert.ok(be.includes('MultiblockVoxelProfiles.exists('));
assert.ok(!be.includes('BronzeFoundryThermalCore'));
const block=r('src/main/java/com/magneticraft2/common/block/stage/bronze/RefractoryKilnBlock.java');
assert.ok(block.includes('RefractoryKilnBlockEntity::serverTick'),'Kiln has no server ticker');
const port=r('src/main/java/com/magneticraft2/common/blockentity/stage/copper/MultiblockItemPortBlockEntity.java');
assert.ok(port.includes('RefractoryKilnBlockEntity kiln')&&port.includes('kiln.acceptsRecipeInput(stack)'));
assert.ok(port.includes('linkFormedController(BlockPos controller)'), 'No reusable item-port linkage');
assert.ok(be.includes('port.linkFormedController(worldPosition)'), 'Kiln does not link real port BEs');
assert.ok(port.includes('"refractory_kiln_"')&&port.includes('"/port_" + role'),
 'Kiln formed real ports lack their own local baked models');
assert.ok(j(data+'recipes/refractory_kiln.json').result.item==='magneticraft2:refractory_kiln');
const prepared=j(data+'recipes/unfired_refractory_batch.json');
assert.equal(prepared.type,'minecraft:crafting_shapeless');
assert.deepEqual(prepared.ingredients.map(i=>i.item),['minecraft:clay_ball','minecraft:sand']);
assert.equal(prepared.result.item,'magneticraft2:unfired_refractory_batch');
const firing=j(data+'recipes/firing_refractory_batch.json');
assert.equal(firing.type,'magneticraft2:multiblock_processing');
assert.equal(firing.machine,'magneticraft2:refractory_kiln');
assert.equal(firing.input.item,'magneticraft2:unfired_refractory_batch');
assert.equal(firing.output.item,'magneticraft2:fired_refractory_ceramic');
assert.equal(firing.process_time,400);
for(const item of ['unfired_refractory_batch','fired_refractory_ceramic'])
 assert.equal(j(resource+'models/item/'+item+'.json').parent,'minecraft:item/generated');
assert.ok(book.pages.length>=46,'Bronze Age Patchouli lost K2 pages');
assert.ok(book.pages.some(p=>p.type==='patchouli:crafting'&&p.recipe==='magneticraft2:unfired_refractory_batch'));
const cfg=r('src/main/java/com/magneticraft2/common/utils/Magneticraft2ConfigCommon.java');
for(const name of ['KilnHeatCapacity','KilnHeatPerFuelTick','KilnPassiveHeatLoss',
  'KilnFiringThreshold','KilnFiringHeatPerTick'])
 assert.ok(cfg.includes(name),'Missing HEAT configuration '+name);
for(const source of ['KilnFuel','KilnBurnRemaining','KilnProgress','KilnRecipe',
 'heatHandler.setHeat(heat)','ForgeHooks.getBurnTime',
 'itemHandler.insertItem(1, product, true)',
 'itemHandler.extractItem(0, recipe.getInputCount(), false)',
 'KilnActiveInput','ItemStack.isSameItemSameTags(activeInput, candidate)',
 'port.linkFormedController(worldPosition)'])
 assert.ok(be.includes(source),'K2 logic/persistence missing '+source);
assert.ok(be.includes('void onDestroy(Level world)'),'Queued fuel not recovered on break');
console.log('Refractory Kiln K2 static contract: recipes, ports, HEAT, persistence and book validated');
