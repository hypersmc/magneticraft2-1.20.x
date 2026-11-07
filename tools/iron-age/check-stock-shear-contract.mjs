#!/usr/bin/env node
// Magneticraft2 Iron III-A source + datapack contract. NOT a Gradle or game test.
// Run from repository root: node tools/iron-age/check-stock-shear-contract.mjs
import fs from 'node:fs';
import assert from 'node:assert/strict';

const root='src/main/';
const java=p=>fs.readFileSync(root+'java/com/magneticraft2/'+p,'utf8');
const json=p=>JSON.parse(fs.readFileSync(root+'resources/'+p,'utf8'));
const recipe=p=>json('data/magneticraft2/recipes/'+p+'.json');
const block='common/block/stage/iron/IronStockShearBlock.java';
const controller='common/blockentity/stage/iron/IronStockShearBlockEntity.java';
const renderer='client/render/blocks/stage/iron/IronStockShearRenderer.java';
const b=java(block),be=java(controller),r=java(renderer);
assert.ok(b.includes('class IronStockShearBlock'));
assert.ok(b.includes('IRON_STOCK_SHEAR_BE.get()'));
assert.ok(b.includes('getLocalVoxelCollision()'));
assert.ok(b.includes('getLocalVoxelSelection()'));
assert.ok(be.includes('MultiblockRecipeHandler.findRecipe(level, MACHINE,'));
assert.ok(be.includes('new ReplaceableMachinePart(ItemRegistry.ITEM_IRON_SHEAR_BLADE::get, 512)'));
assert.ok(be.includes('shearBlade.completeBatch()'));
assert.ok(be.includes('manager.setMechanicalLoad('));
assert.ok(be.includes('manager.getMechanicalLoadState(level, worldPosition).supplied()'));
assert.ok(be.includes('node.getSpeed() >= recipe.getMinSpeed()'));
assert.ok(be.includes('removeMechanicalLoad(level, worldPosition)'));
assert.ok(be.includes('itemHandler.insertItem(1, output, true)'));
assert.ok(be.includes('itemHandler.extractItem(0, recipe.getInputCount(), false)'));
assert.ok(be.includes('HotBloomWorkpiece.consolidate(inputStack, recipe.getOutput())'));
assert.ok(be.includes('activeOutput = ItemStack.EMPTY;'));
assert.ok(be.includes('boolean inputOrRecipeChanged'));
assert.ok(be.includes('progress = 0;'));
assert.ok(be.includes('PressureCanReceive()')&&be.includes('PressureCanSend()'));
assert.ok(!be.includes('ShearureCan')&&!be.includes('Bronze III'));
const consume=be.indexOf('itemHandler.extractItem(0, recipe.getInputCount(), false)');
const insert=be.indexOf('itemHandler.insertItem(1, output, false)');
const wear=be.indexOf('shearBlade.completeBatch()');
assert.ok(consume>0&&insert>consume&&wear>insert,
 'Wear must occur only after real output insertion succeeds');
assert.ok(be.indexOf('itemHandler.insertItem(1, output, true)')<consume,
 'Reserve output before ingredient consumption');
const output=be.indexOf('if (inputOrRecipeChanged || activeOutput.isEmpty())');
assert.ok(output>=0,'Derived output cache must not reset saved progress');
assert.ok(r.includes('multiblock/iron_stock_shear_blade'));
assert.ok(r.includes('BlockRegistry.IRON_STOCK_SHEAR.get().defaultBlockState()'));
assert.ok(r.includes('input.getVisualRotationDegrees(partial)'));
assert.ok(r.includes('IronShearMotion.sliderY(pinX, pinY)'));
assert.ok(r.includes('IronShearMotion.linkDegrees(pinX, pinY, sliderY)'));
assert.ok(!r.includes('pose.scale('), 'No scaled rod or stretched moving models');
assert.ok(!r.includes('BRONZE_MECHANICAL_PRESS'));
const itemPort=java('common/blockentity/stage/copper/MultiblockItemPortBlockEntity.java');
assert.ok(itemPort.includes('controller instanceof IronStockShearBlockEntity shear'));
assert.ok(itemPort.includes('return shear.acceptsRecipeInput(stack)'));
const reg=java('common/registry/registers/BlockRegistry.java');
const beReg=java('common/registry/registers/BlockEntityRegistry.java');
const ir=java('common/registry/registers/ItemRegistry.java');
const tab=java('common/registry/registers/CreativeTabRegistry.java');
const client=java('client/Clientsetup.java');
for(const name of ['iron_stock_shear','riveted_iron_frame'])
 assert.ok(reg.includes('BLOCKS.register("'+name+'"'));
assert.ok(beReg.includes('BLOCK_ENTITIES.register("iron_stock_shear"'));
for(const name of ['IRON_STOCK_SHEAR','IRON_SHEAR_BLADE','IRON_RIVET','RIVETED_IRON_FRAME']){
 assert.ok(ir.includes('ITEM_'+name));
 assert.ok(tab.includes('ItemRegistry.ITEM_'+name+'.get()'));
}
assert.ok(client.includes('IRON_STOCK_SHEAR_BE.get(), IronStockShearRenderer::new'));
assert.ok(client.includes('"multiblock/iron_stock_shear_blade"'));
const jei=java('compatibility/JEI/MagneticraftJeiPlugin.java');
const jeiId=java('compatibility/JEI/MultiblockJeiRecipe.java');
assert.ok(jei.includes('MultiblockJeiRecipe.IRON_STOCK_SHEAR'));
assert.ok(jeiId.includes('new ResourceLocation("magneticraft2", "iron_stock_shear")'));
const stock=recipe('shear_iron_rivets');
assert.equal(stock.type,'magneticraft2:multiblock_processing');
assert.equal(stock.machine,'magneticraft2:iron_stock_shear');
assert.deepEqual(stock.input,{item:'magneticraft2:wrought_iron_rod'});
assert.equal(stock.input_count,1);
assert.deepEqual(stock.output,{item:'magneticraft2:iron_rivet_blank',count:4});
assert.equal(stock.process_time,180);
assert.deepEqual(stock.mechanical,{min_speed:16,torque:12});
const heading=recipe('forge_iron_rivets');
assert.equal(heading.machine,'magneticraft2:bronze_forge_hammer');
assert.deepEqual(heading.input,{item:'magneticraft2:iron_rivet_blank'});
assert.equal(heading.input_count,4);
assert.deepEqual(heading.output,{item:'magneticraft2:iron_rivet',count:4});
assert.equal(heading.process_time,120);
assert.deepEqual(heading.mechanical,{min_speed:20,torque:6});
assert.ok(ir.includes('ITEM_IRON_RIVET_BLANK'));
assert.ok(tab.includes('ItemRegistry.ITEM_IRON_RIVET_BLANK.get()'));
assert.ok(client.includes('IronStockShearRenderer.registerModels(event)'));
const hammer=java('common/blockentity/stage/bronze/BronzeForgeHammerBlockEntity.java');
assert.equal(hammer.split('inputStack.is(ItemRegistry.ITEM_IRON_RIVET_BLANK.get())').length-1,2,
 'Heading reservation and diagnostics must both preserve material NBT');
const frame=recipe('riveted_iron_frame');
assert.equal(frame.type,'minecraft:crafting_shaped');
assert.equal(frame.result.item,'magneticraft2:riveted_iron_frame');
assert.deepEqual(frame.pattern,['RBR','B B','RBR']);
assert.equal(frame.key.R.item,'magneticraft2:iron_rivet');
assert.equal(frame.key.B.item,'magneticraft2:wrought_iron_bar');
const blade=recipe('iron_shear_blade');
assert.equal(blade.result.item,'magneticraft2:iron_shear_blade');
assert.equal(blade.key.P.item,'magneticraft2:wrought_iron_plate');
const controllerRecipe=recipe('iron_stock_shear');
assert.equal(controllerRecipe.result.item,'magneticraft2:iron_stock_shear');
assert.ok(!Object.values(controllerRecipe.key).some(v=>v.item==='magneticraft2:iron_rivet'),
 'Shear construction must not depend on its own output');
const id='iron_stock_shear';
const directions=['south','north','east','west'];
const south=json('data/magneticraft2/multiblocks/iron_stock_shear_south.json');
const positions=layout=>{
 // Model offsets are relative to this orientation's controller, not a
 // hard-coded SOUTH origin. Require exactly one controller before comparing.
 const cells=[];
 for(let y=0;y<2;y++)for(let z=0;z<3;z++)for(let x=0;x<3;x++){
  cells.push([layout['layer'+(y+1)][z][x],x,y,z]);
 }
 const controllers=cells.filter(([c])=>c==='C');
 assert.equal(controllers.length,1,'Exactly one controller per layout');
 const [,cx,cy,cz]=controllers[0];
 const p={};
 for(const [c,x,y,z]of cells)(p[c]??=[]).push([x-cx,y-cy,z-cz]);
 return p;
};
const orig=positions(south.structure.layout);
const rotate=(x,z,d)=>d==='south'?[x,z]:d==='north'?[-x,-z]:
 d==='east'?[z,-x]:[-z,x];
let parts=0,faces=0;
for(const d of directions){
 const structure=json('data/magneticraft2/multiblocks/'+id+'_'+d+'.json');
 assert.deepEqual(structure.structure.dimensions,[3,2,3]);
 assert.equal(structure.structure.blocks.C,'magneticraft2:iron_stock_shear');
 assert.equal(structure.structure.blocks.M,'magneticraft2:mechanical_input_module');
 assert.equal(structure.structure.blocks.U,'magneticraft2:multiblock_item_input');
 assert.equal(structure.structure.blocks.O,'magneticraft2:multiblock_item_primary_output');
 assert.equal(structure.structure.blocks.A,'minecraft:air');
 const where=positions(structure.structure.layout);
 for(const [key,points] of Object.entries(orig)){
  const expected=points.map(([x,y,z])=>[...rotate(x,z,d).slice(0,1),y,rotate(x,z,d)[1]].join(',')).sort();
  const actual=(where[key]||[]).map(x=>x.join(',')).sort();
  assert.deepEqual(actual,expected,d+' '+key+' layout rotation');
 }
 assert.equal(where.C.length,1);
 assert.equal(where.M.length,1);
 assert.equal(where.U.length,1);
 assert.equal(where.O.length,1);
 assert.equal(where.A.length,1);
 const [ax,ay,az]=where.A[0];
 assert.deepEqual(structure.settings.render_air_anchors,[[ax,ay,az]]);
 assert.equal(structure.settings.voxel_profile,'magneticraft2:iron_stock_shear');
 assert.equal(structure.settings.replacewhenformed,'multiblock/iron_stock_shear_'+d);
 for(const points of Object.values(where))for(const [x,y,z]of points){
  const m=json('assets/magneticraft2/models/multiblock/'+id+'_'+d+
   '/part_'+x+'_'+y+'_'+z+'.json');
  assert.equal(m.parent,'minecraft:block/block');
  for(const e of m.elements){
   const [w,h,t]=e.to.map((v,i)=>v-e.from[i]);
   assert.ok([w,h,t].every(v=>v>0),d+' box');
   assert.ok(e.from.every(v=>v>=0)&&e.to.every(v=>v<=16),d+' model bounds');
   for(const [side,f] of Object.entries(e.faces)){
    const [u,v]=side==='up'||side==='down'?[w,t]:
     side==='east'||side==='west'?[t,h]:[w,h];
    assert.deepEqual([f.uv[2]-f.uv[0],f.uv[3]-f.uv[1]],
     [u,v],d+' stretched UV');
    assert.ok(m.textures[f.texture.substring(1)]);
    faces++;
   }
  }
  parts++;
 }
}
assert.equal(parts,72,'4 facings times 18 model parts');
const voxel=json('data/magneticraft2/multiblock_voxels/iron_stock_shear.json');
assert.equal(voxel.format,1);
assert.equal(voxel.unit,'model_pixels');
// Compare real baked static cuboids rather than retaining a stale fixed count.
const expectedVoxels=[];
for(const points of Object.values(orig))for(const [x,y,z] of points){
 const m=json('assets/magneticraft2/models/multiblock/iron_stock_shear_south/'+
  'part_'+x+'_'+y+'_'+z+'.json');
 for(const e of m.elements)expectedVoxels.push([
  e.from[0]+16*x,e.from[1]+16*y,e.from[2]+16*z,
  e.to[0]+16*x,e.to[1]+16*y,e.to[2]+16*z]);
}
const sortedBoxes=boxes=>boxes.map(b=>JSON.stringify(b)).sort();
assert.deepEqual(sortedBoxes(voxel.collision),sortedBoxes(expectedVoxels));
assert.deepEqual(voxel.collision,voxel.selection);
for(const b of voxel.collision){
 assert.equal(b.length,6);
 for(let i=0;i<3;i++)assert.ok(b[i+3]>b[i]);
}
const blockstates=json('assets/magneticraft2/blockstates/iron_stock_shear.json');
assert.equal(Object.keys(blockstates.variants).length,8);
const book=json('assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/ironage.json');
assert.ok(book.pages.some(p=>p.type==='patchouli:multiblock'&&p.name==='Mechanical Metal Shear'));
for(const id of ['iron_stock_shear','iron_shear_blade','riveted_iron_frame'])
 assert.ok(book.pages.some(p=>p.recipe==='magneticraft2:'+id));
const lang=json('assets/magneticraft2/lang/en_us.json');
for(const key of ['block.magneticraft2.iron_stock_shear','item.magneticraft2.iron_rivet',
 'block.magneticraft2.riveted_iron_frame'])
 assert.ok(lang[key],key);
const pick=json('data/minecraft/tags/blocks/mineable/pickaxe.json');
assert.ok(pick.values.includes('magneticraft2:iron_stock_shear'));
assert.ok(pick.values.includes('magneticraft2:riveted_iron_frame'));
const frameModel=json('assets/magneticraft2/models/block/riveted_iron_frame.json');
const frameCode=java('common/block/stage/iron/RivetedIronFrameBlock.java');
assert.equal(frameModel.elements.length,12);
for(const e of frameModel.elements)assert.ok(frameCode.includes(
 'Block.box('+[...e.from,...e.to].join(', ')+')'),'Frame hitbox mismatch');
// Bronze machinery source remains registered with its original IDs.
assert.ok(reg.includes('BLOCKS.register("bronze_mechanical_press"'));
assert.ok(beReg.includes('BLOCK_ENTITIES.register("bronze_mechanical_press"'));
assert.ok(java('common/blockentity/stage/bronze/BronzeMechanicalPressBlockEntity.java')
 .includes('ITEM_BRONZE_PRESS_DIE::get'));

 // High-load source-to-consumer integration: the Large Wheel exposes a
 // 24-tooth RIM but can also connect directly via its shaft-like axle.
 // Wheel-to-gear meshing and axle/shaft transmission are not equivalent.
 const wheel=java('common/blockentity/stage/copper/WaterWheelBlockEntity.java');
 const network=java('common/systems/GEAR/GearNetworkManager.java');
 const inputModule=java('common/blockentity/stage/copper/MechanicalInputModuleBlockEntity.java');
 const ironMedium=java('common/blockentity/stage/iron/WroughtIronMediumGearBlockEntity.java');
 const woodMedium=java('common/blockentity/stage/copper/MediumGearBlockEntity_wood.java');
 const ironLarge=java('common/blockentity/stage/iron/WroughtIronLargeGearBlockEntity.java');
 const ironBox=java('common/blockentity/stage/iron/WroughtIronGearboxBlockEntity.java');
 const ironShaft=java('common/blockentity/stage/iron/WroughtIronShaftBlockEntity.java');
 assert.ok(wheel.includes('return isLarge() ? 24 : 8;'),'Wheel rim uses 24/8 tooth topology');
 assert.ok(wheel.includes('return isLarge() ? 16.0F : 6.0F;'),'Wheel max torque');
 assert.ok(wheel.includes('return true;')&&wheel.includes('boolean isShaftLike()'),'Wheel supports axle connection');
 for(const text of [
   'float minRpm = isLarge() ? 18.0F : 12.0F;',
   'float maxRpm = isLarge() ? 24.0F : 16.0F;',
   'float minTorque = isLarge() ? 8.0F : 3.0F;',
   'float maxTorque = isLarge() ? 16.0F : 6.0F;',
   'float rpm = minRpm + (maxRpm - minRpm) * engagement;',
   'float torque = minTorque + (maxTorque - minTorque) * engagement;'
 ]) assert.ok(wheel.includes(text),'Wheel drive equation drifted: '+text);
 assert.ok(network.includes('current.getTeeth() / (float) neighbor.getTeeth()'),
  'Meshed gear RPM uses tooth ratio');
 assert.ok(network.includes('current.getTorque() / Math.max(TORQUE_EPSILON, ratio)'),
  'Torque must fall with RPM speed-up');
 assert.ok(network.includes('connection.kind() == ConnectionKind.SHAFT'),
  'Axial connection expected to be 1:1');
 assert.ok(ironMedium.includes('return 16.0F;'));
 assert.ok(woodMedium.includes('return 8.0F;'));
 assert.ok(ironLarge.includes('return 24.0F;'));
 assert.ok(ironBox.includes('return 16.0F;'));
 assert.ok(ironShaft.includes('return 24.0F;'));
 assert.ok(inputModule.includes('return 16.0F;'));
 const neededTorque=stock.mechanical.torque,neededRpm=stock.mechanical.min_speed;
 const atEngagement=e=>({torque:8+8*e,rpm:18+6*e});
 const atHalf=atEngagement(.5),atFull=atEngagement(1);
 assert.ok(atHalf.torque>=neededTorque&&atHalf.rpm>=neededRpm,
  'Large Wheel at 50% engagement can supply 1:1 shear load');
 assert.ok(atEngagement(.49).torque<neededTorque,
  'Below 50% engagement cannot supply 12 torque on 1:1 path');
 const meshed=(wheelTorque,drivenTeeth,capacity)=>
  Math.min(wheelTorque/(24/drivenTeeth),capacity);
 assert.ok(meshed(atFull.torque,8,16)<neededTorque,
  '24-tooth wheel to 8-tooth Iron gear loses too much torque');
 assert.ok(meshed(atFull.torque,16,24)<neededTorque,
  '24-tooth wheel to 16-tooth Iron gear also loses too much torque');
 assert.equal(meshed(atFull.torque,8,16),16/3);
 assert.equal(meshed(atFull.torque,16,24),32/3);
 assert.ok(Math.min(atFull.torque,8)<neededTorque,
  'Wood medium gear must still limit a genuinely 1:1 high-torque path');
 assert.ok(Math.min(atFull.torque,16)>=neededTorque,
  'Iron medium gear can carry 12 torque on a 1:1 path');
 assert.ok(atFull.rpm>=neededRpm);

console.log('Iron III-A Stock Shear source/JSON/UV contract PASS; '+parts+
 ' parts, '+faces+' native UV faces (NOT a Forge build or in-game test).');
