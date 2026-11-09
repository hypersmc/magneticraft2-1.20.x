import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),"../..");
const read=p=>fs.readFileSync(path.join(root,p),"utf8");
const json=p=>JSON.parse(read(p));
const B="src/main/resources/assets/magneticraft2/models/block/";
const I="src/main/resources/assets/magneticraft2/models/item/";
const specs=[
 ["gear_medium_wood",41,16],
 ["gear_large_wood",66,16],
 ["gear_large_wood_with_handle",68,12]
];
for(const [name,count,shaftHeight] of specs){
 const model=json(B+name+".json"),item=json(I+name+".json");
 assert.equal(model.parent,"minecraft:block/block");
 assert.equal(model.elements.length,count,"elements: "+name);
 assert.equal(item.parent,"magneticraft2:block/"+name,"item follows placed model");
 assert(item.display?.gui&&item.display?.firstperson_righthand,"existing GUI/hand display");
 assert(!model.format_version,"do not ship post-1.20.1 Blockbench format metadata");
 for(const [key,texture] of Object.entries({
  wood_side:"minecraft:block/oak_log",wood_end:"minecraft:block/oak_log_top",
  stone:"minecraft:block/cobblestone",leather:"minecraft:item/leather"
 }))assert.equal(model.textures[key],texture);
 const hasStoneAxle=model.elements.some(e=>e.from.join(",")==="6,0,6"
   &&e.to.join(",")==="10,"+shaftHeight+",10"
   &&Object.values(e.faces).every(f=>f.texture==="#stone"));
 assert(hasStoneAxle,"original 4x4 stone axle "+name);
 for(const [i,e] of model.elements.entries()){
   if(e.rotation) {
     assert.equal(e.rotation.axis,"y","rotation axis: "+name+" "+i);
     assert([-45,-22.5,0,22.5,45].includes(e.rotation.angle),
       "unsupported Minecraft 1.20.1 rotation: "+name+" "+i);
   }
   const dim=e.to.map((v,j)=>v-e.from[j]);
   assert(dim.every(v=>v>0),"invalid element "+name+" "+i);
   for(const [side,face]of Object.entries(e.faces)){
     assert(model.textures[face.texture.slice(1)],"missing texture "+name+" "+i);
     const size=side==="east"||side==="west"?[dim[2],dim[1]]:
       side==="north"||side==="south"?[dim[0],dim[1]]:[dim[0],dim[2]];
     assert(face.uv&&size.every((v,j)=>Math.abs(face.uv[j+2]-face.uv[j]-v)<.001),
       "stretched face "+name+" "+i+" "+side);
     assert(face.uv.every(v=>v>=-0.001&&v<=16.001),"UV exceeds texture "+name+" "+i);
   }
 }
}
const handled=json(B+"gear_large_wood_with_handle.json");
assert(handled.elements.slice(-2).some(e=>Object.values(e.faces).some(f=>f.texture==="#leather")));
const speed=read("src/main/java/com/magneticraft2/common/blockentity/stage/copper/LargeGearWithHandleBlockEntity_wood.java");
assert(speed.includes("private static final float HANDLE_RPM = 40.0F"));
assert(speed.includes("setSource(true, HANDLE_RPM, getDefaultSourceTorque());"));
assert(speed.includes("activeTicks = 100;"),"preserve click duration");
assert(speed.includes("return 8.0F;"),"preserve manual torque");
const network=read("src/main/java/com/magneticraft2/common/systems/GEAR/GearNetworkManager.java");
assert(network.includes("isPhysicalLargeGear(currentBlockEntity)"));
assert(network.includes("isPhysicalLargeGear(neighborGearBlockEntity)"));
assert(network.includes("entity instanceof LargeGearWithHandleBlockEntity_wood"));
assert(network.includes("entity instanceof LargeGearBlockEntity_wood"));
console.log("PASS three Wooden Gear models, native UV, handle, item inheritance, 40 RPM and crossed-gear eligibility");
