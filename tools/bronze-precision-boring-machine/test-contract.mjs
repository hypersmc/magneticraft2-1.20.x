#!/usr/bin/env node
/**
 * Precision Boring Machine K1: source, resource and geometry contracts.
 * No Java compiler or Minecraft runtime is invoked here.
 */
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import {fileURLToPath} from "node:url";

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),"../..");
const read=p=>fs.readFileSync(path.join(root,p),"utf8");
const json=p=>JSON.parse(read(p));
const id="bronze_precision_boring_machine",machine="magneticraft2:"+id;
const recipes="src/main/resources/data/magneticraft2/recipes/";
const process=json(recipes+"bronze_boring_bushing.json");
assert.equal(process.type,"magneticraft2:multiblock_processing");
assert.equal(process.machine,machine);
assert.deepEqual(process.input,{item:"magneticraft2:rough_bronze_bearing"});
assert.equal(process.input_count,1);
assert.deepEqual(process.output,{item:"magneticraft2:bronze_bushing",count:1});
assert.equal(process.process_time,200);
assert.deepEqual(process.mechanical,{min_speed:20,torque:6});
assert.ok(!process.byproduct&&!process.fluid_input&&!process.fluid_output);
const existingPump=json(recipes+"mechanical_water_pump.json");
const alternatePump=json(recipes+"mechanical_water_pump_bronze_bushing.json");
assert.deepEqual(existingPump.pattern,["PCP","PSP","PGP"],
 "Do not change original Copper Age pump recipe");
assert.equal(existingPump.key.C.item,"magneticraft2:copper_plate");
assert.deepEqual(alternatePump.pattern,["PBP"," S "," G "]);
assert.equal(alternatePump.key.B.item,"magneticraft2:bronze_bushing");
assert.equal(alternatePump.key.S.item,"magneticraft2:shaft_wood");
assert.equal(alternatePump.key.G.item,"magneticraft2:pulley_small_wood");
assert.deepEqual(alternatePump.result,existingPump.result);
assert.equal(alternatePump.pattern.join("").split("P").length-1,2);
assert.deepEqual(json(recipes+id+".json").result,{item:machine});
assert.equal(json(recipes+"abrasive_boring_bit.json").result.item,
 "magneticraft2:abrasive_boring_bit");
const itemModel=json("src/main/resources/assets/magneticraft2/models/item/bronze_bushing.json");
assert.ok(itemModel.elements.length>=4,"Bushing must have 3D hollow ring geometry");
assert.ok(itemModel.elements.every(el=>!(el.from[0]<10&&el.to[0]>6&&
  el.from[2]<10&&el.to[2]>6)),"Bushing model has solid geometry across its bore");

const pkg="src/main/java/com/magneticraft2/";
const block=read(pkg+"common/block/stage/bronze/BronzePrecisionBoringMachineBlock.java");
const be=read(pkg+"common/blockentity/stage/bronze/BronzePrecisionBoringMachineBlockEntity.java");
const registry=read(pkg+"common/registry/registers/BlockEntityRegistry.java");
const blockReg=read(pkg+"common/registry/registers/BlockRegistry.java");
const itemReg=read(pkg+"common/registry/registers/ItemRegistry.java");
const tab=read(pkg+"common/registry/registers/CreativeTabRegistry.java");
const ports=read(pkg+"common/blockentity/stage/copper/MultiblockItemPortBlockEntity.java");
const client=read(pkg+"client/Clientsetup.java");
const renderer=read(pkg+"client/render/blocks/stage/bronze/BronzePrecisionBoringMachineRenderer.java");
const jei=read(pkg+"compatibility/JEI/MagneticraftJeiPlugin.java");
const jeiTypes=read(pkg+"compatibility/JEI/MultiblockJeiRecipe.java");

// Prevent the previous Forge Hammer K1 error: mismatched BlockEntity ticker.
assert.match(block,/createTickerHelper\(type,\s*BlockEntityRegistry\.BRONZE_PRECISION_BORING_MACHINE_BE\.get\(\),\s*BronzePrecisionBoringMachineBlockEntity::serverTick\)/);
assert.doesNotMatch(block,/createTickerHelper\(type,\s*BlockEntityRegistry\.BRONZE_(MECHANICAL_PRESS|ROLLING_MILL|FORGE_HAMMER|STAMP_MILL)_BE\.get\(\)/);
assert.match(registry,/BlockEntityType\.Builder\.of\(BronzePrecisionBoringMachineBlockEntity::new,\s*BlockRegistry\.BRONZE_PRECISION_BORING_MACHINE\.get\(\)\)/);
assert.match(blockReg,/BronzePrecisionBoringMachineBlock::new/);
assert.match(block,/BronzePrecisionBoringMachineBlockEntity borer/);
assert.doesNotMatch(block,/BronzePrecisionBoringMachineBlockEntity mill/);
for(const name of ["BRONZE_PRECISION_BORING_MACHINE","ABRASIVE_BORING_BIT","BRONZE_BUSHING"]){
 assert.ok(itemReg.includes("ITEM_"+name),"Missing ItemRegistry "+name);
 assert.ok(tab.includes("ITEM_"+name),"Missing CreativeTab "+name);
}
assert.match(ports,/controller instanceof BronzePrecisionBoringMachineBlockEntity borer/);
assert.match(ports,/borer.acceptsRecipeInput\(stack\)/);
assert.match(jei,/MultiblockJeiRecipe.BRONZE_PRECISION_BORING_MACHINE/);
assert.match(jeiTypes,/BRONZE_PRECISION_BORING_MACHINE/);
assert.match(client,/BronzePrecisionBoringMachineRenderer::new/);
assert.match(client,/multiblock\/bronze_precision_boring_machine_coupling/);
assert.match(client,/multiblock\/bronze_precision_boring_machine_spindle/);
assert.match(renderer,/borer\.getBoringSpindleAngle\(partial\)/);
assert.match(renderer,/drawShaft\(pose, buffers, blocks, mc\.getModelManager\(\)\.getModel\(COUPLING\)/);
assert.match(renderer,/drawShaft\(pose, buffers, blocks, mc\.getModelManager\(\)\.getModel\(SPINDLE\)/);
assert.match(renderer,/Axis\.ZP\.rotationDegrees\(angle\)/);

assert.match(be,/boolean canAttempt = node != null && node\.getSpeed\(\) >= recipe\.getMinSpeed\(\)/);
assert.match(be,/manager\.getMechanicalLoadState\(level, worldPosition\)\.supplied\(\)/);
assert.match(be,/private static ItemStack taggedOutput\(MultiblockProcessingRecipe recipe, ItemStack input\)/);
assert.match(be,/result\.setTag\(input\.getTag\(\)\.copy\(\)\)/);
assert.match(be,/activeOutput = taggedOutput\(recipe, inputStack\)/);
assert.match(be,/ItemStack output = activeOutput/);
assert.match(be,/if \(!itemHandler\.insertItem\(1, output, true\)\.isEmpty\(\)\)/);
assert.match(be,/itemHandler\.extractItem\(0, recipe\.getInputCount\(\), false\)/);
assert.match(be,/itemHandler\.insertItem\(1, output, false\)/);
assert.match(be,/boringBit\.completeBatch\(\)/);
assert.match(be,/getMechanicalInput\(\)/);
assert.match(be,/getmodulePos\("mechanical_input"\)/);
assert.match(be,/MultiblockRenderParts\.assign/);
assert.match(be,/port\.linkFormedController\(worldPosition\)/);
assert.match(be,/if \(cap == ForgeCapabilities\.ITEM_HANDLER\) return LazyOptional\.empty\(\)/);
for(const n of ["BoreProgress","BoreRecipe","BoreActiveInput","BoreActiveOutput",
                "BoreBit","BoreRunning"]){
 assert.ok(be.includes('"'+n+'"'),"Missing persisted state "+n);
}
assert.match(be,/\|\| activeOutput\.isEmpty\(\)\)/);
assert.match(be,/new ReplaceableMachinePart\(ItemRegistry\.ITEM_ABRASIVE_BORING_BIT::get, 384\)/);
assert.ok(!be.includes("mill.tickBoring()"),"Copy-paste BE variable type mismatch");
assert.ok(be.includes("borer.tickBoring()"),"Ticker does not tick the correct machine");
// No per-tick recipe output NBT copying: the current tagged output is cached.
assert.equal(be.split("taggedOutput(recipe, inputStack)").length-1,1);

const design=json("tools/multiblock-models/designs/"+id+".json");
const faces=json("tools/multiblock-models/sources/"+id+".json");
const voxels=json("src/main/resources/data/magneticraft2/multiblock_voxels/"+id+".json");
assert.equal(design.schema,1);
assert.equal(voxels.format,1);
assert.deepEqual(voxels.collision,design.voxels.collision);
assert.deepEqual(voxels.selection,design.voxels.selection);
assert.ok(design.boxes.some(x=>x.name==="central_bed_stone"));
assert.ok(design.boxes.some(x=>x.name==="rear_gearbox_body"));
assert.ok(design.boxes.some(x=>x.name==="fixture_anvil"));
assert.ok(design.boxes.some(x=>x.name==="front_controller_shell"));
assert.ok(!design.boxes.some(x=>x.name==="spinning_gear"));

// This is the exact supported Forge/Patchouli rendering convention: book X
// coordinates are mirrored relative to the raw SOUTH JSON block coordinates.
const book=json("src/main/resources/assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json");
const guide=book.pages.find(p=>p.name==="Precision Boring Machine");
assert.ok(guide&&guide.multiblock.pattern.length===3);
const uvAxis={north:[0,1],south:[0,1],east:[2,1],west:[2,1],up:[0,2],down:[0,2]};
let models=0,quads=0;
function verifyFaces(model,where){
 for(const el of model.elements){
  assert.ok(el.from.every((v,i)=>v>=-16.0001&&el.to[i]<=32.0001&&v<el.to[i]),
   "Out-of-range block-local model element "+where);
  for(const [f,uv] of Object.entries(el.faces)){
   const [u,v]=uvAxis[f];
   assert.ok(uv.uv.every(a=>a>=-0.0001&&a<=16.0001),"UV not in native tile "+where);
   assert.ok(Math.abs((uv.uv[2]-uv.uv[0])-(el.to[u]-el.from[u]))<0.0002
        &&Math.abs((uv.uv[3]-uv.uv[1])-(el.to[v]-el.from[v]))<0.0002,
    "Texture stretched/compressed at "+where+" face "+f);
   assert.ok(model.textures[uv.texture.slice(1)],"Missing texture alias "+where);
   quads++;
  }
 }
}
for(const dir of ["south","west","north","east"]){
 const b=json("src/main/resources/data/magneticraft2/multiblocks/"+id+"_"+dir+".json");
 const [width,height,depth]=b.structure.dimensions;
 assert.equal(height,3);
 assert.deepEqual([width,depth].sort((a,b)=>a-b),[3,5]);
 assert.equal(b.settings.voxel_profile,machine);
 const counts={},pos={},hosts=[];
 for(let y=0;y<height;y++)for(let z=0;z<depth;z++)for(let x=0;x<width;x++){
  const symbol=b.structure.layout["layer"+(y+1)][z][x];
  assert.ok(b.structure.blocks[symbol],"Unknown blueprint symbol "+symbol);
  counts[symbol]=(counts[symbol]||0)+1;
  if(symbol==="C")pos.controller=[x,y,z];
  if(symbol!=="A")hosts.push([x,y,z]);
 }
 for(const type of ["C","I","U","O"])assert.equal(counts[type],1,dir+" missing "+type);
 const [cx,cy,cz]=pos.controller;assert.equal(cy,0);
 assert.equal(b.settings.render_air_anchors.length,2);
 for(const [dx,dy,dz] of b.settings.render_air_anchors){
  assert.equal(b.structure.layout["layer"+(dy+cy+1)][cz+dz][cx+dx],"A",
   dir+" non-AIR render anchor");
  hosts.push([cx+dx,cy+dy,cz+dz]);
 }
 assert.equal(hosts.length,35,dir+" unexpected render host count");
 const seen=new Set();
 for(const [x,y,z] of hosts){
  const key=[x-cx,y-cy,z-cz].join("_");
  assert.ok(!seen.has(key),"duplicate V2 model host "+dir+"/"+key);
  seen.add(key);
  const model=json("src/main/resources/assets/magneticraft2/models/multiblock/"+
   id+"_"+dir+"/part_"+key+".json");
  const sym=b.structure.layout["layer"+(y+1)][z][x];
  if(["I","U","O"].includes(sym))assert.equal(model.elements.length,0,
    dir+" decorative geometry intrudes into actual "+sym+" module");
  if(key==="0_0_0")assert.ok(model.elements.length>0,"Missing controller mesh "+dir);
  verifyFaces(model,dir+"/"+key);models++;
 }
 if(dir==="south"){
  assert.deepEqual(b.structure.layout.layer1.map(row=>row.join("")),
   ["SSISS","USSSO","SSCSS"]);
  const mirrored=Array.from({length:height},(_,n)=>
   b.structure.layout["layer"+(height-n)].map(row=>[...row].reverse()
     .map(ch=>ch==="C"?"0":ch==="A"?"_":ch).join("")));
  assert.deepEqual(guide.multiblock.pattern,mirrored,
    "Patchouli and JSON disagree: copied unmirrored ports caused the Forge Hammer bug");
 }
}
assert.equal(models,140);
for(const id of ["coupling","spindle"]){
 const m=json("src/main/resources/assets/magneticraft2/models/multiblock/"+
  "bronze_precision_boring_machine_"+id+".json");
 assert.ok(m.elements.length>=3);
 verifyFaces(m,"animated "+id);
}
verifyFaces(itemModel,"Bronze Bushing");
const lang=json("src/main/resources/assets/magneticraft2/lang/en_us.json");
assert.equal(lang["item.magneticraft2.bronze_bushing"],"Bronze Bushing");
for(const k of ["item.magneticraft2.abrasive_boring_bit",
 "block.magneticraft2."+id,"message.magneticraft2."+id+".source",
 "message.magneticraft2."+id+".reason.bit_worn"])
 assert.ok(lang[k],"Missing localization "+k);
for(const tag of ["mineable/pickaxe","needs_stone_tool"])
 assert.ok(json("src/main/resources/data/minecraft/tags/blocks/"+tag+".json").values.includes(machine));
const svg=read("docs/reference/bronze-precision-boring-machine-studio.svg");
assert.ok(svg.includes("MAGNETICRAFT2")&&svg.includes("5 wide"));
console.log("Precision Boring Machine static contract PASS: "+models+
 " model parts; "+quads+" UV-checked faces; 4 facings; exact ticker;"+
 " quality NBT, mechanical load, maintenance, JEI, ports and Studio source.");
