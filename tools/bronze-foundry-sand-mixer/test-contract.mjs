#!/usr/bin/env node
// Bronze III Foundry Sand Mixer K1 source contract, not Java compilation or gameplay.
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import {fileURLToPath} from "node:url";
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),"../..");
const read=p=>fs.readFileSync(path.join(root,p),"utf8"),json=p=>JSON.parse(read(p));
const pkg="src/main/java/com/magneticraft2/",assets="src/main/resources/assets/magneticraft2/",data="src/main/resources/data/magneticraft2/";
const id="bronze_foundry_sand_mixer",machine="magneticraft2:"+id;
const be=read(pkg+"common/blockentity/stage/bronze/BronzeFoundrySandMixerBlockEntity.java");
const block=read(pkg+"common/block/stage/bronze/BronzeFoundrySandMixerBlock.java");
const basin=read(pkg+"common/blockentity/stage/bronze/BronzeCastingBasinBlockEntity.java");
const table=read(pkg+"common/blockentity/stage/bronze/BronzeMoldTableBlockEntity.java");
const ports=read(pkg+"common/blockentity/stage/copper/MultiblockItemPortBlockEntity.java");
const registry=read(pkg+"common/registry/registers/BlockEntityRegistry.java");
const itemRegistry=read(pkg+"common/registry/registers/ItemRegistry.java");
const jei=read(pkg+"compatibility/JEI/MagneticraftJeiPlugin.java");
const client=read(pkg+"client/Clientsetup.java");
const recipe=json(data+"recipes/bronze_reclaim_molding_sand.json");
const blend=json(data+"recipes/sand_reclamation_blend.json");
assert.equal(recipe.machine,machine);
assert.deepEqual(recipe.input,{item:"magneticraft2:sand_reclamation_blend"});
assert.equal(recipe.input_count,1);
assert.deepEqual(recipe.output,{item:"magneticraft2:reclaimed_molding_sand",count:3});
assert.equal(recipe.process_time,200);
assert.deepEqual(recipe.mechanical,{min_speed:12,torque:4});
assert.equal(blend.type,"minecraft:crafting_shapeless");
assert.equal(blend.ingredients.length,5);
assert.equal(blend.ingredients.filter(i=>i.item==="magneticraft2:spent_molding_sand").length,4);
assert.equal(blend.ingredients.filter(i=>i.item==="minecraft:clay_ball").length,1);
assert.equal(blend.result.item,"magneticraft2:sand_reclamation_blend");
assert.equal(json(data+"recipes/"+id+".json").result.item,machine);
assert.equal(json(data+"recipes/bronze_mixer_paddles.json").result.item,"magneticraft2:bronze_mixer_paddles");
// The existing Basin still refunds original bronze on mid-cooling break, NOT spent sand.
assert.match(basin,/basin\.recoverableSand=true;/);
assert.match(basin,/if \(recoverableSand\) \{\s*give\(player,new ItemStack\(ItemRegistry\.ITEM_SPENT_MOLDING_SAND\.get\(\)\)\);/);
assert.match(basin,/if \(dropSpentSand\) Block\.popResource/);
assert.match(basin,/recoverableSand=tag\.getBoolean\("RecoverableSand"\)/);
assert.match(basin,/tag\.putBoolean\("RecoverableSand",recoverableSand\)/);
assert.match(basin,/if \(!c\.isEmpty\(\)\) Block\.popResource\(level,worldPosition,c\);/);
assert.match(table,/stack\.is\(Items\.SAND\) \|\| stack\.is\(Items\.RED_SAND\)/);
assert.match(table,/ITEM_RECLAIMED_MOLDING_SAND/);
assert.match(table,/if \(!recycled\) inventory\.extractItem\(2, 1, false\)/);
assert.match(table,/REQUIRED_PRESSES = 4/);
assert.match(block,/BlockEntityRegistry\.BRONZE_FOUNDRY_SAND_MIXER_BE\.get\(\),\s*BronzeFoundrySandMixerBlockEntity::serverTick/);
assert.match(registry,/BlockEntityType\.Builder\.of\(BronzeFoundrySandMixerBlockEntity::new,\s*BlockRegistry\.BRONZE_FOUNDRY_SAND_MIXER\.get\(\)/);
assert.match(be,/mixer\.tickMixing\(\)/);
assert.match(be,/new ReplaceableMachinePart\(ItemRegistry\.ITEM_BRONZE_MIXER_PADDLES::get, 512\)/);
assert.match(be,/manager\.getMechanicalLoadState\(level, worldPosition\)\.supplied\(\)/);
assert.match(be,/itemHandler\.insertItem\(1, output, true\)/);
assert.match(be,/itemHandler\.extractItem\(0, recipe\.getInputCount\(\), false\)/);
assert.match(be,/mixingPaddles\.completeBatch\(\)/);
assert.match(be,/if \(cap == ForgeCapabilities\.ITEM_HANDLER\) return LazyOptional\.empty\(\)/);
assert.match(ports,/controller instanceof BronzeFoundrySandMixerBlockEntity mixer/);
assert.match(ports,/mixer\.acceptsRecipeInput\(stack\)/);
for(const k of ["BRONZE_FOUNDRY_SAND_MIXER","BRONZE_MIXER_PADDLES","SPENT_MOLDING_SAND","SAND_RECLAMATION_BLEND","RECLAIMED_MOLDING_SAND"])
 assert.ok(itemRegistry.includes("ITEM_"+k),"Unregistered item "+k);
for(const k of ["MixProgress","MixRecipe","MixActiveInput","MixActiveOutput","MixPaddles","MixRunning"])
 assert.ok(be.includes('"'+k+'"'),"Unsaved state "+k);
assert.match(jei,/MultiblockJeiRecipe\.BRONZE_FOUNDRY_SAND_MIXER/);
assert.match(client,/BronzeFoundrySandMixerRenderer::new/);
for(const m of ["paddles","coupling"])
 assert.ok(client.includes("multiblock/"+id+"_"+m));
const design=json("tools/multiblock-models/designs/"+id+".json");
const voxels=json(data+"multiblock_voxels/"+id+".json");
assert.equal(design.schema,1);
assert.deepEqual(voxels.collision,design.voxels.collision);
assert.deepEqual(voxels.selection,design.voxels.selection);
const book=json(assets+"patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json");
const guide=book.pages.find(p=>p.name==="Foundry Sand Mixer");
assert.ok(guide&&guide.multiblock.pattern.length===2,"Missing Patchouli layout");
const UV={north:[0,1],south:[0,1],east:[2,1],west:[2,1],up:[0,2],down:[0,2]};
let models=0,quads=0;
function verifyModel(m,file){
 for(const el of m.elements){
  assert.ok(el.from.every((v,i)=>v>=-16.001&&el.to[i]<=32.001&&v<el.to[i]),"Out-of-bounds model "+file);
  for(const[face,info]of Object.entries(el.faces)){
   const[a,b]=UV[face],uv=info.uv;
   assert.ok(uv?.every(x=>x>=-0.001&&x<=16.001),"UV outside native tile "+file);
   assert.ok(Math.abs(uv[2]-uv[0]-(el.to[a]-el.from[a]))<0.001
     &&Math.abs(uv[3]-uv[1]-(el.to[b]-el.from[b]))<0.001,"Stretched texture "+file);
   assert.ok(m.textures[info.texture.slice(1)],"Missing texture "+file);
   quads++;
  }
 }
}
for(const dir of ["south","west","north","east"]){
 const bp=json(data+"multiblocks/"+id+"_"+dir+".json");
 assert.deepEqual(bp.structure.dimensions,[3,2,3]);
 assert.equal(bp.settings.voxel_profile,machine);
 const counts={},hosts=[];let controller;
 for(let y=0;y<2;y++)for(let z=0;z<3;z++)for(let x=0;x<3;x++){
  const key=bp.structure.layout["layer"+(y+1)][z][x];
  assert.ok(bp.structure.blocks[key],"Unknown blueprint symbol");
  counts[key]=(counts[key]||0)+1;
  if(key==="C")controller=[x,y,z];
  if(key!=="A")hosts.push([x,y,z,key]);
 }
 for(const symbol of ["C","I","U","O","A"])assert.equal(counts[symbol],1,dir+" missing "+symbol);
 assert.equal(bp.settings.render_air_anchors.length,1);
 const off=bp.settings.render_air_anchors[0],a=off.map((q,i)=>q+controller[i]);
 assert.equal(bp.structure.layout["layer"+(a[1]+1)][a[2]][a[0]],"A");
 hosts.push([...a,"A"]);
 assert.equal(hosts.length,18);
 for(const[x,y,z,symbol]of hosts){
  const rel=[x-controller[0],y-controller[1],z-controller[2]].join("_");
  const file=assets+"models/multiblock/"+id+"_"+dir+"/part_"+rel+".json";
  const m=json(file);verifyModel(m,file);
  if("IUO".includes(symbol))assert.equal(m.elements.length,0,"Real module occluded "+file);
  if(symbol==="C")assert.ok(m.elements.length,"No controller mesh "+file);
  models++;
 }
 if(dir==="south"){
  assert.deepEqual(bp.structure.layout.layer1.map(row=>row.join("")),["SIS","USO","SCS"]);
  const mirror=Array.from({length:2},(_,j)=>bp.structure.layout["layer"+(2-j)]
   .map(row=>[...row].reverse().map(k=>k==="C"?"0":k==="A"?"_":k).join("")));
  assert.deepEqual(guide.multiblock.pattern,mirror);
 }
}
assert.equal(models,72);
for(const kind of ["paddles","coupling"])verifyModel(json(assets+"models/multiblock/"+id+"_"+kind+".json"),kind);
const langs=json(assets+"lang/en_us.json");
for(const term of ["reclaimed_molding_sand","spent_molding_sand","sand_reclamation_blend","bronze_mixer_paddles"])
 assert.ok(langs["item.magneticraft2."+term],"Missing translation");
for(const tag of ["mineable/pickaxe","needs_stone_tool"])
 assert.ok(json("src/main/resources/data/minecraft/tags/blocks/"+tag+".json").values.includes(machine));
assert.match(read("tools/multiblock-models/generate.mjs"),/id==="bronze_foundry_sand_mixer"/);
console.log("Sand Mixer K1 static contract passed: "+models+" render-host models, "+quads+" native-UV faces, 4 facings, recovery chain, load and wear. NOT JAVA COMPILED.");
