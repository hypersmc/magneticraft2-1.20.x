#!/usr/bin/env node
// Source/resource contracts only. This does not compile Java or test Minecraft gameplay.
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import {fileURLToPath} from "node:url";
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),"../..");
const read=(p)=>fs.readFileSync(path.join(root,p),"utf8");
const json=(p)=>JSON.parse(read(p));
const pkg="src/main/java/com/magneticraft2/";
const assets="src/main/resources/assets/magneticraft2/";
const data="src/main/resources/data/magneticraft2/";
const id="bronze_mechanical_sawmill",machine="magneticraft2:"+id;
const block=read(pkg+"common/block/stage/bronze/BronzeMechanicalSawmillBlock.java");
const entity=read(pkg+"common/blockentity/stage/bronze/BronzeMechanicalSawmillBlockEntity.java");
const ports=read(pkg+"common/blockentity/stage/copper/MultiblockItemPortBlockEntity.java");
const maker=read(pkg+"common/block/stage/bronze/BronzePatternMakerBlock.java");
const items=read(pkg+"common/registry/registers/ItemRegistry.java");
const blocks=read(pkg+"common/registry/registers/BlockRegistry.java");
const entities=read(pkg+"common/registry/registers/BlockEntityRegistry.java");
const client=read(pkg+"client/Clientsetup.java");
const renderer=read(pkg+"client/render/blocks/stage/bronze/BronzeMechanicalSawmillRenderer.java");
const jei=read(pkg+"compatibility/JEI/MagneticraftJeiPlugin.java");
const recipe=json(data+"recipes/bronze_sawmill_pattern_stock.json");
assert.equal(recipe.machine,machine);
assert.deepEqual(recipe.input,{tag:"minecraft:logs_that_burn"});
assert.equal(recipe.input_count,1);
assert.deepEqual(recipe.output,{item:"magneticraft2:sawn_pattern_stock",count:3});
assert.equal(recipe.process_time,160);
assert.deepEqual(recipe.mechanical,{min_speed:16,torque:4});
assert.deepEqual(json(data+"recipes/"+id+".json").result,{item:machine});
assert.equal(json(data+"recipes/bronze_saw_blade.json").result.item,"magneticraft2:bronze_saw_blade");
assert.match(block,/BlockEntityRegistry\.BRONZE_MECHANICAL_SAWMILL_BE\.get\(\),\s*BronzeMechanicalSawmillBlockEntity::serverTick/);
assert.match(entities,/BlockEntityType\.Builder\.of\(BronzeMechanicalSawmillBlockEntity::new,\s*BlockRegistry\.BRONZE_MECHANICAL_SAWMILL\.get\(\)/);
assert.match(blocks,/BronzeMechanicalSawmillBlock::new/);
assert.match(ports,/controller instanceof BronzeMechanicalSawmillBlockEntity sawmill/);
assert.match(ports,/sawmill\.acceptsRecipeInput\(stack\)/);
assert.match(entity,/manager\.getMechanicalLoadState\(level, worldPosition\)\.supplied\(\)/);
assert.match(entity,/new ReplaceableMachinePart\(ItemRegistry\.ITEM_BRONZE_SAW_BLADE::get, 256\)/);
assert.match(entity,/itemHandler\.insertItem\(1, output, true\)/);
assert.match(entity,/itemHandler\.extractItem\(0, recipe\.getInputCount\(\), false\)/);
assert.match(entity,/sawBlade\.completeBatch\(\)/);
assert.match(entity,/if \(cap == ForgeCapabilities\.ITEM_HANDLER\) return LazyOptional\.empty\(\)/);
for(const key of ["SawProgress","SawRecipe","SawActiveInput","SawActiveOutput","SawBlade","SawRunning"])
  assert.ok(entity.includes('"'+key+'"'),"Missing save state "+key);
for(const item of ["BRONZE_MECHANICAL_SAWMILL","BRONZE_SAW_BLADE","SAWN_PATTERN_STOCK"])
  assert.ok(items.includes("ITEM_"+item));
assert.match(maker,/held\.is\(ItemTags\.PLANKS\) \|\| held\.is\(ItemRegistry\.ITEM_SAWN_PATTERN_STOCK\.get\(\)\)/);
assert.match(maker,/held\.is\(ItemRegistry\.ITEM_SAWN_PATTERN_STOCK\.get\(\)\) \? 2 : 1/);
assert.match(jei,/MultiblockJeiRecipe\.BRONZE_MECHANICAL_SAWMILL/);
assert.match(client,/BronzeMechanicalSawmillRenderer::new/);
assert.match(client,/multiblock\/bronze_mechanical_sawmill_blade/);
assert.match(client,/multiblock\/bronze_mechanical_sawmill_coupling/);
assert.match(renderer,/sawmill\.getSawingSpindleAngle\(partial\)/);
assert.match(renderer,/Axis\.ZP\.rotationDegrees/);
for(const tag of ["mineable/pickaxe","needs_stone_tool"])
  assert.ok(json("src/main/resources/data/minecraft/tags/blocks/"+tag+".json").values.includes(machine));
const design=json("tools/multiblock-models/designs/"+id+".json");
const voxel=json(data+"multiblock_voxels/"+id+".json");
assert.equal(design.schema,1);
assert.deepEqual(design.voxels.collision,voxel.collision);
const guide=json(assets+"patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json")
  .pages.find(p=>p.name==="Mechanical Sawmill");
assert.ok(guide,"Missing Sawmill multiblock Patchouli entry");
const axis={north:[0,1],south:[0,1],east:[2,1],west:[2,1],up:[0,2],down:[0,2]};
let models=0,faces=0;
const checkUV=(m,file)=>{
  for(const el of m.elements||[]) {
    assert.ok(el.from.every((v,i)=>v>=-16.0001&&el.to[i]<=32.0001&&v<el.to[i]),"Model bounds "+file);
    for(const [face,info] of Object.entries(el.faces||{})){
      const [a,b]=axis[face],uv=info.uv;
      assert.ok(uv&&uv.every(n=>n>=-0.0001&&n<=16.0001),"UV outside tile "+file);
      assert.ok(Math.abs(uv[2]-uv[0]-(el.to[a]-el.from[a]))<0.0002
             &&Math.abs(uv[3]-uv[1]-(el.to[b]-el.from[b]))<0.0002,"Stretched UV "+file);
      assert.ok(m.textures[info.texture.slice(1)],"Undefined material "+file);
      faces++;
    }
  }
};
for(const dir of ["south","west","north","east"]){
 const bp=json(data+"multiblocks/"+id+"_"+dir+".json");
 const [w,h,d]=bp.structure.dimensions;
 assert.equal(h,3);
 assert.deepEqual([w,d].sort((a,b)=>a-b),[3,5]);
 assert.equal(bp.settings.voxel_profile,machine);
 let controller,counts={},hosts=[];
 for(let y=0;y<h;y++)for(let z=0;z<d;z++)for(let x=0;x<w;x++){
   const key=bp.structure.layout["layer"+(y+1)][z][x];
   assert.ok(bp.structure.blocks[key],dir+": undefined "+key);
   counts[key]=(counts[key]||0)+1;
   if(key==="C")controller=[x,y,z];
   if(key!=="A")hosts.push([x,y,z,key]);
 }
 for(const k of ["C","I","U","O"]) assert.equal(counts[k],1,dir+" missing "+k);
 assert.equal(hosts.length,33);
 for(const [x,y,z,k] of hosts){
   const rel=[x-controller[0],y-controller[1],z-controller[2]].join("_");
   const file=assets+"models/multiblock/"+id+"_"+dir+"/part_"+rel+".json";
   const model=json(file);
   if(["I","U","O"].includes(k))assert.equal(model.elements.length,0,"Module host occluded "+file);
   if(k==="C")assert.ok(model.elements.length,"Empty controller model");
   checkUV(model,file);
   models++;
 }
 if(dir==="south"){
   assert.deepEqual(bp.structure.layout.layer1.map(row=>row.join("")),["SSISS","USSSO","SSCSS"]);
   const mirrored=Array.from({length:h},(_,n)=>bp.structure.layout["layer"+(h-n)]
      .map(row=>[...row].reverse().map(c=>c==="C"?"0":c==="A"?"_":c).join("")));
   assert.deepEqual(guide.multiblock.pattern,mirrored,"Patchouli has inverted real ports");
 }
}
assert.equal(models,132);
for(const component of ["blade","coupling"])
 checkUV(json(assets+"models/multiblock/"+id+"_"+component+".json"),component);
checkUV(json(assets+"models/item/sawn_pattern_stock.json"),"pattern stock");
assert.equal(json(assets+"models/item/bronze_saw_blade.json").parent,
 "magneticraft2:multiblock/bronze_mechanical_sawmill_blade");
const lang=json(assets+"lang/en_us.json");
for(const k of ["item.magneticraft2.sawn_pattern_stock","item.magneticraft2.bronze_saw_blade",
 "block.magneticraft2."+id,"message.magneticraft2."+id+".source"])
 assert.ok(lang[k],k);
assert.ok(read("docs/reference/bronze-mechanical-sawmill-studio.svg").includes("Mechanical Sawmill"));
console.log("Sawmill K1 source contract PASS: "+models+" native-UV models, "+faces+
 " faces, four facings, real ports, power, blade maintenance, JEI and Pattern Maker.");
