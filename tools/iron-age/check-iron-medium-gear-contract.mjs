#!/usr/bin/env node
// Forge 1.20.1 Iron II-A source/resource contract. Not a Gradle or in-game test.
import fs from 'node:fs';
import assert from 'node:assert/strict';

const src='src/main/java/com/magneticraft2/';
const res='src/main/resources/';
const java=p=>fs.readFileSync(src+p,'utf8');
const json=p=>JSON.parse(fs.readFileSync(res+p,'utf8'));
const rec=n=>json('data/magneticraft2/recipes/'+n+'.json');
const forge=rec('forge_iron_gear_blank_medium');
const lathe=rec('lathe_precision_iron_gear_medium');
const assembly=rec('wrought_iron_gear_medium');

assert.deepEqual({
 machine:forge.machine,input:forge.input.item,output:forge.output.item,
 count:forge.output.count,inputCount:forge.input_count,
 time:forge.process_time,mechanical:forge.mechanical
},{
 machine:'magneticraft2:bronze_forge_hammer',
 input:'magneticraft2:wrought_iron_bar',
 output:'magneticraft2:iron_gear_blank_medium',
 count:1,inputCount:1,time:180,mechanical:{min_speed:20,torque:6}
});
assert.deepEqual({
 machine:lathe.machine,input:lathe.input.item,output:lathe.output.item,
 count:lathe.output.count,inputCount:lathe.input_count,
 time:lathe.process_time,mechanical:lathe.mechanical
},{
 machine:'magneticraft2:bronze_mechanical_lathe',
 input:'magneticraft2:iron_gear_blank_medium',
 output:'magneticraft2:precision_iron_gear_medium',
 count:1,inputCount:1,time:160,mechanical:{min_speed:12,torque:5}
});
assert.equal(assembly.type,'minecraft:crafting_shaped');
assert.equal(assembly.result.item,'magneticraft2:wrought_iron_gear_medium');
assert.equal(assembly.pattern.join('').split('P').length-1,3);
assert.equal(assembly.pattern.join('').split('B').length-1,1);
assert.equal(assembly.pattern.join('').split('G').length-1,1);
assert.equal(assembly.key.P.item,'magneticraft2:wrought_iron_plate');
assert.equal(assembly.key.B.item,'magneticraft2:wrought_iron_bar');
assert.equal(assembly.key.G.item,'magneticraft2:precision_iron_gear_medium');
assert.equal(rec('forge_wrought_iron_plate').machine,'magneticraft2:bronze_forge_hammer');
assert.equal(rec('bronze_rolling_copper_sheet').input.item,'magneticraft2:copper_plate');

const woodBE=java('common/blockentity/stage/copper/MediumGearBlockEntity_wood.java');
const ironBE=java('common/blockentity/stage/iron/WroughtIronMediumGearBlockEntity.java');
assert.match(woodBE,/getGearMaxTorque\(\)\s*\{\s*return 8\.0F;/);
assert.match(ironBE,/getGearMaxTorque\(\)\s*\{\s*return 16\.0F;/);
assert.match(ironBE,/extends MediumGearBlockEntity_wood/);
assert.match(ironBE,/GEAR_MEDIUM_BE_IRON\.get\(\)/);

const ironBlock=java('common/block/stage/iron/WroughtIronMediumGearBlock.java');
assert.match(ironBlock,/extends MediumGearBlock_wood/);
assert.match(ironBlock,/GEAR_MEDIUM_BE_IRON\.get\(\)/);
assert.match(ironBlock,/MediumGearBlockEntity_wood::serverTick/);
const beRegistry=java('common/registry/registers/BlockEntityRegistry.java');
assert.match(beRegistry,/GEAR_MEDIUM_BE_IRON/);
assert.match(beRegistry,/WroughtIronMediumGearBlockEntity::new/);
assert.match(java('common/registry/registers/BlockRegistry.java'),
 /BLOCKS\.register\("wrought_iron_gear_medium"/);
assert.match(java('common/registry/registers/ItemRegistry.java'),
 /ITEM_IRON_GEAR_BLANK_MEDIUM/);
assert.match(java('common/registry/registers/ItemRegistry.java'),
 /ITEM_PRECISION_IRON_GEAR_MEDIUM/);
assert.match(java('common/registry/registers/ItemRegistry.java'),
 /ITEM_WROUGHT_IRON_GEAR_MEDIUM/);
const jei=java('compatibility/JEI/MagneticraftJeiPlugin.java');
const jeiModel=java('compatibility/JEI/MultiblockJeiRecipe.java');
assert.match(jei,/MultiblockJeiRecipe\.BRONZE_MECHANICAL_LATHE/);
assert.match(jeiModel,/new ResourceLocation\("magneticraft2", "bronze_mechanical_lathe"\)/);
const client=java('client/Clientsetup.java');
assert.match(client,/GEAR_MEDIUM_BE_IRON\.get\(\), WroughtIronMediumGearRenderer::new/);
const renderer=java('client/render/blocks/stage/iron/WroughtIronMediumGearRenderer.java');
assert.match(renderer,/shared\.render\(entity, partialTicks, stack, bufferSource/);
const hot=java('common/systems/material/HotBloomWorkpiece.java');
assert.match(hot,/ITEM_IRON_GEAR_BLANK_MEDIUM\.get\(\)/);
assert.match(hot,/ITEM_WROUGHT_IRON_BAR\.get\(\)/);
assert.match(hot,/metalData\.remove\(HOT_UNTIL_TAG\)/);
const latheSource=java('common/blockentity/stage/bronze/BronzeMechanicalLatheBlockEntity.java');
assert.match(latheSource,/MultiblockRecipeHandler\.findRecipe/);
assert.match(latheSource,/ironRecipe\.getTorque\(\)/);
assert.match(latheSource,/ironRecipe\.getMinSpeed\(\)/);
assert.match(latheSource,/cachedIronGearRecipe = null/);
assert.ok(latheSource.includes('boolean canAttempt=node.getSpeed()>=rpmRequired'));
assert.ok(!latheSource.includes('boolean canAttempt=node.getEffectiveSpeed()>=rpmRequired'));
assert.match(latheSource,/ironRecipe == null \? 1 : 2/);
assert.match(latheSource,/finished\.setTag\(workpiece\.getTag\(\)\.copy\(\)\)/);
assert.match(latheSource,/ITEM_ROUGH_BRONZE_GEAR\.get\(\)/);
assert.match(latheSource,/ITEM_ROUGH_BRONZE_BEARING\.get\(\)/);
const model=json('assets/magneticraft2/models/block/wrought_iron_gear_medium.json');
const precision=json('assets/magneticraft2/models/item/precision_iron_gear_medium.json');
assert.equal(model.parent,'minecraft:block/block');
assert.equal(model.textures.iron,'magneticraft2:block/material/dark_work_iron');
assert.equal(model.textures.collar,'magneticraft2:block/material/bronze_machined');
assert.equal(model.elements.length,43);
assert.equal(precision.elements.length,34);
assert.deepEqual(precision.elements,model.elements.slice(0,34),
 'precision part must share rim, spokes, teeth and hub');
const rim=model.elements.slice(0,18),spokes=model.elements.slice(18,22);
const teeth=model.elements.slice(22,30),hub=model.elements.slice(30,34);
const shaft=model.elements[34],collars=model.elements.slice(35);
assert.equal(teeth.length,8);
assert.equal(teeth.filter(e=>e.rotation).length,4);
assert.ok(teeth.filter(e=>e.rotation).every(e=>e.rotation.axis==='y'&&Math.abs(e.rotation.angle)===45));
assert.deepEqual(shaft.from,[7,0,7]);assert.deepEqual(shaft.to,[9,16,9]);
assert.equal(collars.length,8);
let faceCount=0;
for(const m of [model,precision])for(const e of m.elements){
 const [x,y,z]=e.to.map((v,i)=>v-e.from[i]);
 assert.ok([x,y,z].every(n=>n>0));
 for(const [side,f]of Object.entries(e.faces)){
  const [w,h]=side==='up'||side==='down'?[x,z]:side==='east'||side==='west'?[z,y]:[x,y];
  assert.deepEqual([f.uv[2]-f.uv[0],f.uv[3]-f.uv[1]],[w,h],'stretched '+side);
  assert.ok(m.textures[f.texture.substring(1)]);
  faceCount++;
 }
}
assert.equal(faceCount,462);
const areaXZ=(a,b)=>Math.max(0,Math.min(a.to[0],b.to[0])-Math.max(a.from[0],b.from[0]))
 *Math.max(0,Math.min(a.to[2],b.to[2])-Math.max(a.from[2],b.from[2]));
const touching=(a,b)=>[0,2].some(i=>
 (Math.abs(a.to[i]-b.from[i])<1e-8||Math.abs(b.to[i]-a.from[i])<1e-8)
 &&Math.min(a.to[2-i],b.to[2-i])>Math.max(a.from[2-i],b.from[2-i]));
for(let i=0;i<rim.length;i++)for(let j=i+1;j<rim.length;j++)
 assert.equal(areaXZ(rim[i],rim[j]),0,'rim coplanar top overlap');
const reached=new Set([0]);
for(let changed=true;changed;){changed=false;
 for(const i of [...reached])for(let j=0;j<rim.length;j++)
 if(!reached.has(j)&&touching(rim[i],rim[j])){reached.add(j);changed=true;}
}
assert.equal(reached.size,rim.length,'rim is not continuous');
const contains=(e,x,z)=>{
 if(e.rotation){const t=e.rotation.angle*Math.PI/180,dx=x-8,dz=z-8;
  x=8+Math.cos(t)*dx-Math.sin(t)*dz;
  z=8+Math.sin(t)*dx+Math.cos(t)*dz;
 }
 return x>=e.from[0]&&x<e.to[0]&&z>=e.from[2]&&z<e.to[2];
};
const contact=(parts,e)=>{
 const step=.125;let hits=0;
 for(let z=0;z<16;z+=step)for(let x=0;x<16;x+=step){
  const px=x+step/2,pz=z+step/2;
  if(contains(e,px,pz)&&parts.some(q=>contains(q,px,pz)))hits++;
 }
 return hits*step*step;
};
for(const [i,tooth] of teeth.entries())
 assert.ok(contact(rim,tooth)>1,'DETACHED tooth '+i+' after rotation');
for(const [i,spoke] of spokes.entries()){
 assert.ok(contact(rim,spoke)>1,'spoke '+i+' detached from rim');
 assert.ok(contact(hub,spoke)>1,'spoke '+i+' detached from hub');
}
assert.ok(json('assets/magneticraft2/models/block/gear_medium_wood.json').elements.length>=20);
const variants=json('assets/magneticraft2/blockstates/wrought_iron_gear_medium.json').variants;
assert.equal(Object.keys(variants).length,7);
assert.equal(variants['powered=true'].model,'magneticraft2:block/invis');
for(const v of Object.values(variants)){
 assert.ok(['magneticraft2:block/wrought_iron_gear_medium','magneticraft2:block/invis']
  .includes(v.model));
}
const item=json('assets/magneticraft2/models/item/wrought_iron_gear_medium.json');
assert.equal(item.parent,'magneticraft2:block/wrought_iron_gear_medium');
const loot=json('data/magneticraft2/loot_tables/blocks/wrought_iron_gear_medium.json');
assert.equal(loot.pools[0].entries[0].name,'magneticraft2:wrought_iron_gear_medium');
const tags=json('data/minecraft/tags/blocks/mineable/pickaxe.json');
assert.ok(tags.values.includes('magneticraft2:wrought_iron_gear_medium'));
const localization=json('assets/magneticraft2/lang/en_us.json');
for(const key of [
 'item.magneticraft2.iron_gear_blank_medium',
 'item.magneticraft2.precision_iron_gear_medium',
 'block.magneticraft2.wrought_iron_gear_medium'])
 assert.ok(localization[key],key);
const book=json('assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/ironage.json');
assert.ok(book.pages.some(p=>p.recipe==='magneticraft2:wrought_iron_gear_medium'));
console.log('Iron II-A gear source/JSON/UV contract PASS (NOT a build or in-game test).');
