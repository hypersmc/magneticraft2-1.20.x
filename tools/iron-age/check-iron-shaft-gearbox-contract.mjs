#!/usr/bin/env node
// Iron II-B source/JSON/UV contract, not a Forge build or in-game test.
// Run from repository root with Node 18+.
import fs from 'node:fs';
import assert from 'node:assert/strict';

const javaRoot='src/main/java/com/magneticraft2/';
const resRoot='src/main/resources/';
const java=path=>fs.readFileSync(javaRoot+path,'utf8');
const json=path=>JSON.parse(fs.readFileSync(resRoot+path,'utf8'));
const ids=['wrought_iron_shaft','wrought_iron_gearbox'];
const woodShaft=java('common/blockentity/stage/copper/ShaftBlockEntity_wood.java');
const woodBox=java('common/blockentity/stage/copper/GearboxBlockEntity_wood.java');
const ironShaft=java('common/blockentity/stage/iron/WroughtIronShaftBlockEntity.java');
const ironBox=java('common/blockentity/stage/iron/WroughtIronGearboxBlockEntity.java');

assert.match(woodShaft,/getGearMaxTorque\(\)\s*\{\s*return 16\.0F;/);
assert.match(woodBox,/getGearMaxTorque\(\)\s*\{\s*return 8\.0F;/);
assert.match(ironShaft,/getGearMaxTorque\(\)\s*\{\s*return 24\.0F;/);
assert.match(ironBox,/getGearMaxTorque\(\)\s*\{\s*return 16\.0F;/);
assert.match(ironShaft,/extends ShaftBlockEntity_wood/);
assert.match(ironBox,/extends GearboxBlockEntity_wood/);
assert.match(ironShaft,/SHAFT_BE_IRON\.get\(\)/);
assert.match(ironBox,/GEARBOX_BE_IRON\.get\(\)/);

const woodShaftBlock=java('common/block/stage/copper/ShaftBlock_wood.java');
const woodBoxBlock=java('common/block/stage/copper/GearboxBlock_wood.java');
const ironShaftBlock=java('common/block/stage/iron/WroughtIronShaftBlock.java');
const ironBoxBlock=java('common/block/stage/iron/WroughtIronGearboxBlock.java');
assert.match(woodShaftBlock,/protected ShaftBlock_wood\(BlockBehaviour\.Properties properties\)/);
assert.match(woodBoxBlock,/protected GearboxBlock_wood\(BlockBehaviour\.Properties properties\)/);
assert.match(woodBoxBlock,/protected static <E extends BlockEntity, A extends BlockEntity>/);
assert.match(ironShaftBlock,/extends ShaftBlock_wood/);
assert.match(ironBoxBlock,/extends GearboxBlock_wood/);
assert.match(ironShaftBlock,/SHAFT_BE_IRON\.get\(\)/);
assert.match(ironBoxBlock,/GEARBOX_BE_IRON\.get\(\)/);
assert.match(ironShaftBlock,/ShaftBlockEntity_wood::serverTick/);
assert.match(ironBoxBlock,/GearboxBlockEntity_wood::serverTick/);
assert.match(woodShaftBlock,/return validateGearPlacement\(context, placementState\)/);
assert.match(woodBoxBlock,/nextPerpendicularDirection/);
assert.match(woodBoxBlock,/gearbox\.updateGearNetwork\(\)/);
assert.match(woodBoxBlock,/GearNetworkManager\.getInstance\(\)\.removeGear/);
assert.match(woodBox,/getRightAngleDirectionSign/);
assert.match(woodBox,/isPrimaryPort/);
assert.match(woodBox,/acceptsPort/);
assert.match(woodBox,/supportsExternalGearMesh\(\)/);

// Gear V2 must still discover both iron subclasses through the existing
// generic GearBlockEntity graph and existing Wood gearbox port checks.
const manager=java('common/systems/GEAR/GearNetworkManager.java');
assert.match(manager,/instanceof GearboxBlockEntity_wood gearbox/);
assert.match(manager,/gearBlockEntity\.getGearMaxTorque\(\)/);
assert.match(manager,/Math\.min\(availableTorque, neighbor\.getMaxTorque\(\)\)/);
assert.match(manager,/gearbox\.acceptsPort\(gearboxPort\)/);

const blockRegistry=java('common/registry/registers/BlockRegistry.java');
const beRegistry=java('common/registry/registers/BlockEntityRegistry.java');
const itemRegistry=java('common/registry/registers/ItemRegistry.java');
const creative=java('common/registry/registers/CreativeTabRegistry.java');
const client=java('client/Clientsetup.java');
for(const id of ids){
 assert.ok(blockRegistry.includes('BLOCKS.register("'+id+'"'),id+' block');
 assert.ok(beRegistry.includes('BLOCK_ENTITIES.register("'+id+'"'),id+' BE');
 const registryItem='ITEM_'+id.toUpperCase();
 assert.ok(itemRegistry.includes(registryItem+' = fromBlock'),id+' BlockItem');
 assert.ok(creative.includes(registryItem+'.get()'),id+' creative tab');
}
assert.match(client,/SHAFT_BE_IRON\.get\(\), WroughtIronShaftRenderer::new/);
assert.match(client,/GEARBOX_BE_IRON\.get\(\), WroughtIronGearboxRenderer::new/);
const ironShaftRenderer=java('client/render/blocks/stage/iron/WroughtIronShaftRenderer.java');
const ironBoxRenderer=java('client/render/blocks/stage/iron/WroughtIronGearboxRenderer.java');
const sharedBoxRenderer=java('client/render/blocks/stage/copper/GearboxBlockEntity_woodRenderer.java');
assert.match(ironShaftRenderer,/shared\.render\(entity, partialTicks/);
assert.match(ironBoxRenderer,/shared\.render\(entity, partialTicks/);
assert.match(ironBoxRenderer,/context, IRON, IRON, BRONZE/);
assert.match(ironBoxRenderer,/0\.125D, 0\.125D/);
assert.match(sharedBoxRenderer,/portAxleThickness/);
assert.match(sharedBoxRenderer,/portCouplingThickness/);
assert.match(sharedBoxRenderer,/0\.470D,\s*portAxleThickness/);
assert.match(sharedBoxRenderer,/0\.090D,\s*portCouplingThickness/);
assert.match(ironShaftBlock,/SHAPE_Y = Block\.box\(7, 0, 7, 9, 16, 9\)/);
assert.match(ironShaftBlock,/SHAPE_X = Block\.box\(0, 7, 7, 16, 9, 9\)/);
assert.match(ironShaftBlock,/SHAPE_Z = Block\.box\(7, 7, 0, 9, 9, 16\)/);
assert.match(ironShaftBlock,/getCollisionShape/);
assert.match(ironShaftBlock,/return getShape\(state, level, pos, context\)/);
assert.match(sharedBoxRenderer,/this\(context, OAK, SHAFT, SUPPORT, false, 0\.105D, 0\.375D\)/);
assert.match(sharedBoxRenderer,/this\(context, gearTexture, shaftTexture, supportTexture, true, 0\.105D, 0\.375D\)/);
assert.match(sharedBoxRenderer,/proportionalUv \? \(float\) sizeX : texelWindow\(sizeX\)/);
assert.match(sharedBoxRenderer,/renderBevelGear/);
assert.match(sharedBoxRenderer,/renderPortShaft/);
assert.match(sharedBoxRenderer,/renderPortSupport/);
assert.ok(sharedBoxRenderer.includes('proportionalUv ? consumer : support'));
assert.match(sharedBoxRenderer,/VertexConsumer structural/);
assert.match(sharedBoxRenderer,/VertexConsumer bearing/);

const book=json('assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/ironage.json');
const pickaxe=json('data/minecraft/tags/blocks/mineable/pickaxe.json');
const lang=json('assets/magneticraft2/lang/en_us.json');
let faceCount=0;
for(const id of ids){
 const model=json('assets/magneticraft2/models/block/'+id+'.json');
 const item=json('assets/magneticraft2/models/item/'+id+'.json');
 const blockstate=json('assets/magneticraft2/blockstates/'+id+'.json');
 const loot=json('data/magneticraft2/loot_tables/blocks/'+id+'.json');
 const recipe=json('data/magneticraft2/recipes/'+id+'.json');
 assert.equal(item.parent,'magneticraft2:block/'+id);
 assert.equal(loot.pools[0].entries[0].name,'magneticraft2:'+id);
 assert.equal(recipe.type,'minecraft:crafting_shaped');
 assert.equal(recipe.result.item,'magneticraft2:'+id);
 assert.equal(recipe.result.count,1);
 assert.ok(recipe.pattern.every(row=>row.length===recipe.pattern[0].length));
 assert.ok(pickaxe.values.includes('magneticraft2:'+id));
 assert.ok(lang['block.magneticraft2.'+id]);
 assert.ok(lang['item.magneticraft2.'+id]);
 assert.ok(book.pages.some(p=>p.recipe==='magneticraft2:'+id));
 for(const element of model.elements){
  const [x,y,z]=element.to.map((v,i)=>v-element.from[i]);
  assert.ok([x,y,z].every(v=>v>0));
  for(const [side,face] of Object.entries(element.faces)){
   const [w,h]=side==='up'||side==='down'?[x,z]:
    side==='east'||side==='west'?[z,y]:[x,y];
   assert.deepEqual(
    [face.uv[2]-face.uv[0],face.uv[3]-face.uv[1]],
    [w,h],id+' stretched '+side);
   assert.ok(model.textures[face.texture.substring(1)]);
   faceCount++;
  }
 }
 if(id==='wrought_iron_shaft'){
  assert.equal(Object.keys(blockstate.variants).length,7);
  assert.equal(blockstate.variants['rotating=true'].model,
   'magneticraft2:block/invis');
 } else {
  assert.equal(blockstate.multipart[0].apply.model,
   'magneticraft2:block/wrought_iron_gearbox');
 }
}
// The first in-game screenshot showed wood-looking exposed shaft ends
// and an overwhelmingly bronze/wood-looking interior. Keep bronze
// confined to the actual bearing journal, not shafts or frame supports.
const shaftVisual=json('assets/magneticraft2/models/block/wrought_iron_shaft.json');
const shaftElement=shaftVisual.elements[0];
assert.deepEqual(shaftElement.from,[7,0,7]);
assert.deepEqual(shaftElement.to,[9,16,9]);
assert.equal(shaftElement.to[0]-shaftElement.from[0],2);
const gearVisual=json('assets/magneticraft2/models/block/wrought_iron_gear_medium.json');
const fullGearAxles=gearVisual.elements.filter(e=>e.from[1]===0 && e.to[1]===16);
assert.equal(fullGearAxles.length,1);
assert.deepEqual(fullGearAxles[0].from,[7,0,7]);
assert.deepEqual(fullGearAxles[0].to,[9,16,9]);
const gearboxVisual=json('assets/magneticraft2/models/block/wrought_iron_gearbox.json');
const ironTex='magneticraft2:block/material/dark_work_iron';
const bronzeTex='magneticraft2:block/material/bronze_machined';
assert.equal(shaftVisual.textures.iron,ironTex);
assert.equal(shaftVisual.textures.end,ironTex,'shaft ends must be iron, not bronze/wood-like');
assert.equal(gearboxVisual.textures.frame,ironTex);
assert.equal(gearboxVisual.textures.base,ironTex,'structural floor must be iron');
assert.ok(!Object.values(shaftVisual.textures).includes(bronzeTex),
 'iron shaft unexpectedly has wooden-looking bronze caps');
assert.match(sharedBoxRenderer,/renderPortSupport\(\s*stack,\s*proportionalUv \? consumer : support,\s*support/);
assert.match(sharedBoxRenderer,/drawBox\([\s\S]*?structural,/);
assert.match(sharedBoxRenderer,/drawBox\([\s\S]*?bearing,/);
const shaftRecipe=json('data/magneticraft2/recipes/wrought_iron_shaft.json');
const boxRecipe=json('data/magneticraft2/recipes/wrought_iron_gearbox.json');
assert.deepEqual(shaftRecipe.pattern,[' B ',' R ',' B ']);
assert.equal(shaftRecipe.key.B.item,'magneticraft2:precision_bronze_bearing');
assert.equal(shaftRecipe.key.R.item,'magneticraft2:wrought_iron_bar');
assert.deepEqual(boxRecipe.pattern,['PGP','SBS','PGP']);
assert.equal(boxRecipe.key.G.item,'magneticraft2:precision_iron_gear_medium');
assert.equal(boxRecipe.key.S.item,'magneticraft2:wrought_iron_shaft');
assert.equal(boxRecipe.key.B.item,'magneticraft2:precision_bronze_bearing');
assert.equal(boxRecipe.key.P.item,'magneticraft2:wrought_iron_plate');
console.log('Iron II-B shaft/gearbox Gear V2 and native-UV contract PASS; faces '+faceCount+' (NOT a Forge build or in-game test).');
