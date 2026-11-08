#!/usr/bin/env node
// Iron II-D Service Hatch source/JSON contract (NOT a Forge build).
import assert from "node:assert/strict";
import fs from "node:fs";
const R="src/main/resources/",J="src/main/java/com/magneticraft2/";
const read=p=>fs.readFileSync(p,"utf8");
const data=p=>JSON.parse(read(R+p));
const code=p=>read(J+p);
const id="iron_service_hatch";
const block=code("common/block/stage/iron/IronServiceHatchBlock.java");
assert.ok(block.includes("extends TrapDoorBlock"));
assert.ok(block.includes("BlockSetType.register("));
assert.ok(block.includes("BlockSetType.IRON.trapdoorOpen()"));
assert.ok(block.includes("BlockSetType.IRON.trapdoorClose()"));
assert.ok(block.includes("                    true,"),
 "Manual opening must remain enabled in the iron-sounding type");
assert.ok(!block.includes("BlockEntity")&&!block.includes("getTicker("));
const reg=code("common/registry/registers/BlockRegistry.java");
const items=code("common/registry/registers/ItemRegistry.java");
const creative=code("common/registry/registers/CreativeTabRegistry.java");
assert.ok(reg.includes('BLOCKS.register("iron_service_hatch", IronServiceHatchBlock::new)'));
assert.ok(items.includes("ITEM_IRON_SERVICE_HATCH = fromBlock(IRON_SERVICE_HATCH)"));
assert.ok(creative.includes("ItemRegistry.ITEM_IRON_SERVICE_HATCH.get()"));
const states=data("assets/magneticraft2/blockstates/"+id+".json").variants;
const yaw={north:0,east:90,south:180,west:270};
assert.equal(Object.keys(states).length,16);
for(const [facing,angle]of Object.entries(yaw))for(const half of ["bottom","top"])
for(const open of [false,true]){
 const key="facing="+facing+",half="+half+",open="+open;
 const current=states[key];
 assert.ok(current,"Missing trapdoor state: "+key);
 assert.equal(current.model,"magneticraft2:block/"+id+"_"+(open?"open":half));
 assert.equal(current.y??0,angle);
}
const models={};
let checkedFaces=0;
for(const kind of ["bottom","top","open"]){
 const m=data("assets/magneticraft2/models/block/"+id+"_"+kind+".json");
 models[kind]=m;
 assert.equal(m.parent,"minecraft:block/block");
 assert.equal(m.elements.length,5);
 assert.equal(m.textures.iron,"magneticraft2:block/material/dark_work_iron");
 assert.equal(m.textures.hinge,"magneticraft2:block/material/bronze_machined");
 for(const e of m.elements){
  const [w,h,d]=e.to.map((n,i)=>n-e.from[i]);
  assert.ok([w,h,d].every(n=>n>0));
  assert.ok(e.from.every(n=>n>=0)&&e.to.every(n=>n<=16));
  for(const [face,side]of Object.entries(e.faces)){
   assert.equal(["up","down","east","west","north","south"].includes(face),true);
   const dims=["up","down"].includes(face)?[w,d]:["east","west"].includes(face)?[d,h]:[w,h];
   assert.deepEqual(side.uv,[0,0,...dims],"Non-native UV "+kind+" "+face);
   checkedFaces++;
  }
 }
}
for(let i=0;i<5;i++){
 const b=models.bottom.elements[i],t=models.top.elements[i],o=models.open.elements[i];
 assert.deepEqual(t.from,[b.from[0],b.from[1]+13,b.from[2]]);
 assert.deepEqual(t.to,[b.to[0],b.to[1]+13,b.to[2]]);
 assert.deepEqual(o.from,[b.from[0],b.from[2],16-b.to[1]]);
 assert.deepEqual(o.to,[b.to[0],b.to[2],16-b.from[1]]);
}
assert.ok(models.bottom.elements.every(e=>e.to[1]<=3));
assert.ok(models.top.elements.every(e=>e.from[1]>=13));
assert.ok(models.open.elements.every(e=>e.from[2]>=13));
assert.equal(data("assets/magneticraft2/models/item/"+id+".json").parent,
 "magneticraft2:block/"+id+"_bottom");
const recipe=data("data/magneticraft2/recipes/"+id+".json");
assert.deepEqual(recipe.pattern,["PBP"," R "]);
assert.equal(recipe.result.count,2);
assert.equal(recipe.result.item,"magneticraft2:"+id);
assert.deepEqual(Object.keys(recipe.key).sort(),["B","P","R"]);
const loot=data("data/magneticraft2/loot_tables/blocks/"+id+".json");
assert.equal(loot.pools[0].entries[0].name,"magneticraft2:"+id);
for(const tag of ["mineable/pickaxe","needs_stone_tool","trapdoors"]){
 const path="data/minecraft/tags/blocks/"+tag+".json";
 assert.ok(data(path).values.includes("magneticraft2:"+id),"Missing tag "+tag);
}
assert.equal(data("assets/magneticraft2/lang/en_us.json")
  ["block.magneticraft2."+id],"Iron Service Hatch");
const pages=data("assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/ironage.json").pages;
assert.ok(pages.some(p=>p.recipe==="magneticraft2:"+id));
assert.ok(pages.some(p=>p.text?.includes("Iron Service Access")));
console.log("Iron Service Hatch source/JSON contract PASS: 16 model states, "+
 checkedFaces+" native UV faces, vanilla trapdoor behavior. "+
 "JDK17 Gradle build and in-game behavior NOT tested.");
