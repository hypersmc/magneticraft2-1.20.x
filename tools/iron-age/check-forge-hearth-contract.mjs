#!/usr/bin/env node
// Static model/source/recipe contract; not a Gradle build or runtime test.
import assert from "node:assert/strict";
import fs from "node:fs";
const R="src/main/resources/",J="src/main/java/com/magneticraft2/";
const json=p=>JSON.parse(fs.readFileSync(R+p,"utf8"));
const java=p=>fs.readFileSync(J+p,"utf8");
const id="iron_forge_hearth",item="magneticraft2:"+id;
const block=java("common/block/stage/iron/IronForgeHearthBlock.java");
const be=java("common/blockentity/stage/iron/IronForgeHearthBlockEntity.java");
assert.ok(block.includes("import com.magneticraft2.common.blockentity.stage.iron.IronForgeHearthBlockEntity;"));
assert.ok(block.includes("IRON_FORGE_HEARTH_BE"));
assert.ok(be.includes("getInputCount() == 4"));
assert.ok(be.includes("recipe.getOutput().getCount() == 4"));
assert.ok(be.includes("HotBloomWorkpiece.heatInKiln(candidate"));
assert.ok(be.includes("!candidate.is(recipe.getOutput().getItem())"));
assert.ok(be.includes("HearthHeatCapacity"));
assert.ok(be.includes("if (!player.getInventory().add(returned) && !returned.isEmpty())"));
for(const [file,symbol] of [["common/registry/registers/BlockRegistry.java",'BLOCKS.register("iron_forge_hearth"'],["common/registry/registers/BlockEntityRegistry.java",'BLOCK_ENTITIES.register("iron_forge_hearth"'],["common/registry/registers/ItemRegistry.java","ITEM_IRON_FORGE_HEARTH"]]) assert.ok(java(file).includes(symbol));
assert.ok(java("common/blockentity/stage/copper/MultiblockItemPortBlockEntity.java").includes("instanceof IronForgeHearthBlockEntity hearth"));
const hot=java("common/systems/material/HotBloomWorkpiece.java");
for(const recipe of ["hearth_reheat_wrought_iron_ingots","hearth_reheat_wrought_iron_bars"])assert.ok(hot.includes(recipe));
assert.ok(json("data/minecraft/tags/blocks/mineable/pickaxe.json").values.includes(item));
const crafting=json("data/magneticraft2/recipes/iron_forge_hearth.json");
assert.equal(crafting.result.item,item);
assert.deepEqual(crafting.pattern,["PBP","CFC","PBP"]);
for(const [suffix,source] of [["ingots","wrought_iron_ingot"],["bars","wrought_iron_bar"]]){
const rec=json("data/magneticraft2/recipes/hearth_reheat_wrought_iron_"+suffix+".json");
assert.equal(rec.type,"magneticraft2:multiblock_processing");
assert.equal(rec.machine,item);
assert.equal(rec.input.item,"magneticraft2:"+source);
assert.equal(rec.output.item,"magneticraft2:"+source);
assert.equal(rec.input_count,4);assert.equal(rec.output.count,4);
assert.equal(rec.process_time,360);
}
const phys=json("data/magneticraft2/multiblock_voxels/"+id+".json");
assert.equal(phys.format,1);
assert.equal(phys.unit,"model_pixels");
assert.equal(phys.collision.length,33);
const orientations=["south","west","north","east"];
const canonical=[["FFF","BFB","UCO"],["BBB","BAB","BBB"]];
const expectedCounts={F:4,B:10,U:1,O:1,C:1,A:1};
for(const facing of orientations){
const bp=json("data/magneticraft2/multiblocks/"+id+"_"+facing+".json");
assert.deepEqual(bp.structure.dimensions,[3,2,3]);
assert.equal(bp.settings.voxel_profile,item);
const layers=[bp.structure.layout.layer1,bp.structure.layout.layer2];
const cells=layers.flat(2);const counts=Object.fromEntries([...new Set(cells)].map(ch=>[ch,cells.filter(x=>x===ch).length]));
assert.deepEqual(counts,expectedCounts);
assert.equal(bp.structure.blocks.A,"minecraft:air");
let controller;
for(let y=0;y<2;y++)for(let z=0;z<3;z++)for(let x=0;x<3;x++)if(layers[y][z][x]==="C")controller=[x,y,z];
for(let y=0;y<2;y++)for(let z=0;z<3;z++)for(let x=0;x<3;x++){
 const ch=layers[y][z][x];if(!["B","F"].includes(ch))continue;
 const [dx,dy,dz]=[x-controller[0],y-controller[1],z-controller[2]];
 const model=json("assets/magneticraft2/models/multiblock/"+id+"_"+facing+"/part_"+dx+"_"+dy+"_"+dz+".json");
 if(y===1&&ch==="B"){
  assert.equal(model.parent,"minecraft:block/block");
  assert.equal(model.textures.brick,"magneticraft2:block/refractory_kiln/brick");
  assert.ok(model.elements.length>=1);
  for(const e of model.elements){
   assert.ok(e.to[1]<=11,"The formed rim must stay below the visible stock rests");
   const [w,h,d]=e.to.map((v,i)=>v-e.from[i]);
   assert.ok([w,h,d].every(v=>v>0));
   for(const [face,f] of Object.entries(e.faces)){
    const dims=["up","down"].includes(face)?[w,d]:
       ["east","west"].includes(face)?[d,h]:[w,h];
    assert.deepEqual([f.uv[2]-f.uv[0],f.uv[3]-f.uv[1]],dims);
   }
  }
 }else{
  assert.equal(model.parent,"minecraft:block/cube_all");
  assert.ok(model.textures.all.startsWith("magneticraft2:block/refractory_kiln/"));
 }
 }
}
const book=json("assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/ironage.json");
assert.ok(book.pages.some(p=>p.type==="patchouli:multiblock"&&p.name==="Iron Forge Hearth"));
assert.ok(book.pages.some(p=>p.recipe===item));
const model=json("assets/magneticraft2/models/block/iron_forge_hearth_controller.json");
let faces=0;
for(const e of model.elements){
const [w,h,d]=e.to.map((x,i)=>x-e.from[i]);
for(const [face,f] of Object.entries(e.faces)){
const dims=["up","down"].includes(face)?[w,d]:["east","west"].includes(face)?[d,h]:[w,h];
assert.deepEqual([f.uv[2]-f.uv[0],f.uv[3]-f.uv[1]],dims);
faces++;
}}
// A in the blueprint is a required EMPTY block only while constructing.
// After formation it is the standard saved/rendered V2 filler proxy.
// Every formed SOUTH brick collider comes from its matching baked model.
// The floor and the AIR chamber keep their original positions.
let rimIndex=9;
const southBp=json("data/magneticraft2/multiblocks/"+id+"_south.json");
for(let z=0;z<3;z++)for(let x=0;x<3;x++){
 if(southBp.structure.layout.layer2[z][x]!=="B")continue;
 const model=json("assets/magneticraft2/models/multiblock/"+id+
  "_south/part_"+(x-1)+"_1_"+(z-2)+".json");
 for(const elem of model.elements){
  const translated=[...elem.from.map((v,k)=>v+[x-1,1,z-2][k]*16),
   ...elem.to.map((v,k)=>v+[x-1,1,z-2][k]*16)];
  assert.deepEqual(phys.collision[rimIndex++],translated,
   "No invisible full-height former brick collision");
 }
}
assert.equal(rimIndex,24,"8 original top bricks become 15 low shaped pieces");
const frontMouth=json("assets/magneticraft2/models/multiblock/"+id+
 "_south/part_0_1_0.json");
assert.equal(frontMouth.elements.length,5,"Front heating mouth must be open");
assert.ok(frontMouth.elements.some(e=>e.to[1]===2));

const airOffsets={south:[0,1,-1],west:[1,1,0],north:[0,1,1],east:[-1,1,0]};
const chamberSouth=json("assets/magneticraft2/models/multiblock/"+id+"_south/part_0_1_-1.json");
assert.equal(chamberSouth.parent,"minecraft:block/block");
assert.equal(chamberSouth.elements.length,15);
assert.equal(chamberSouth.textures.coal,"minecraft:block/coal_block");
const physical=[0,2,3,4,5,11,12,13,14]; // all structural; coal is decorative.
assert.equal(phys.collision.length,9+15+physical.length);
assert.deepEqual(phys.selection,phys.collision);
for(let i=0;i<physical.length;i++){
 const shape=chamberSouth.elements[physical[i]];
 assert.deepEqual(phys.collision[24+i],[
  ...shape.from.map((v,k)=>v+airOffsets.south[k]*16),
  ...shape.to.map((v,k)=>v+airOffsets.south[k]*16)
 ],"V2 selection/collision must match visual load-bearing hearth part");
}
const rotate=(from,to,d)=>{
 const [x,y,z]=from,[a,b,c]=to;
 if(d==="south")return [from,to];
 if(d==="north")return [[16-a,y,16-c],[16-x,b,16-z]];
 if(d==="east")return [[z,y,16-a],[c,b,16-x]];
 return [[16-c,y,x],[16-z,b,a]];
};
for(const facing of orientations){
 const bp=json("data/magneticraft2/multiblocks/"+id+"_"+facing+".json");
 assert.deepEqual(bp.settings.render_air_anchors,[airOffsets[facing]]);
 const m=json("assets/magneticraft2/models/multiblock/"+id+"_"+
   facing+"/part_"+airOffsets[facing].join("_")+".json");
 assert.deepEqual(m.textures,chamberSouth.textures);
 assert.equal(m.elements.length,15);
 for(let i=0;i<m.elements.length;i++){
  const e=m.elements[i],s=chamberSouth.elements[i];
  assert.deepEqual([e.from,e.to],rotate(s.from,s.to,facing),
    "Directional part geometry mismatch "+facing+" element "+i);
  const [w,h,d]=e.to.map((n,k)=>n-e.from[k]);
  for(const [face,uv] of Object.entries(e.faces)){
   const dims=["up","down"].includes(face)?[w,d]:
    ["east","west"].includes(face)?[d,h]:[w,h];
   assert.deepEqual([uv.uv[2]-uv.uv[0],uv.uv[3]-uv.uv[1]],dims,
    "Native UV mismatch "+facing+" "+face);
  }
 }
}
assert.ok(book.pages.some(p=>p.text?.includes("Why AIR in the middle")));
console.log("Iron Forge Hearth AIR anchor contract PASS: four directions, 15 model "+
 "parts each, 9 model-matched structural collision components; player leaves AIR.");

console.log("Iron Forge Hearth JSON/source contract PASS, "+faces+" 1:1 controller UV faces. Full Forge build and in-game behavior NOT checked.");
