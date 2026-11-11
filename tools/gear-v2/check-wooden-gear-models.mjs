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
   assert.equal(e.from.length,3,"from must have 3 coordinates: "+name+" "+i);
   assert.equal(e.to.length,3,"to must have 3 coordinates: "+name+" "+i);
   assert(e.from.every(Number.isFinite)&&e.to.every(Number.isFinite),
     "coordinates must be finite: "+name+" "+i);
   const dim=e.to.map((v,j)=>v-e.from[j]);
   assert(dim.every(v=>v>0),"invalid element "+name+" "+i);
   for(const [side,face]of Object.entries(e.faces)){
     assert(model.textures[face.texture.slice(1)],"missing texture "+name+" "+i);
     const size=side==="east"||side==="west"?[dim[2],dim[1]]:
       side==="north"||side==="south"?[dim[0],dim[1]]:[dim[0],dim[2]];
     assert(face.uv&&face.uv.length===4&&face.uv.every(Number.isFinite)&&size.every((v,j)=>Number.isFinite(v)&&Math.abs(face.uv[j+2]-face.uv[j]-v)<.001),
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


// 3D oriented-box sweep: the handled lever must not collide with a crossed
// Large Gear when the shared half-tooth phase is applied.
const models=["gear_large_wood","gear_large_wood_with_handle"].map(n=>json(B+n+".json"));
const axes=["X","Y","Z"],rad=Math.PI/180;
function rotate(axis,degrees,[x,y,z]){
 const a=degrees*rad,c=Math.cos(a),s=Math.sin(a);
 return axis==="X"?[x,c*y-s*z,s*y+c*z]
  :axis==="Y"?[c*x+s*z,y,-s*x+c*z]
  :[c*x-s*y,s*x+c*y,z];
}
const dot=(a,b)=>a[0]*b[0]+a[1]*b[1]+a[2]*b[2];
const cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];
function cube(el,axis,angle,offset){
 let center=el.from.map((v,i)=>(v+el.to[i])/2-8);
 let basis=[[1,0,0],[0,1,0],[0,0,1]];
 const half=el.from.map((v,i)=>(el.to[i]-v)/2);
 function turn(a,d){center=rotate(a,d,center);basis=basis.map(v=>rotate(a,d,v));}
 if(el.rotation)turn("Y",el.rotation.angle);
 // Matched to 1.20.1 blockstates: X-east x=90,y=90; Y-down identity; Z-north x=90.
 if(axis==="X"){turn("X",90);turn("Y",90);}
 else if(axis==="Z")turn("X",90);
 turn(axis,angle);
 return{center:center.map((v,i)=>v+offset[i]),basis,half,
   extent:[0,1,2].map(i=>basis.reduce((s,v,k)=>s+Math.abs(v[i])*half[k],0))};
}
function intersects(a,b){
 const d=b.center.map((v,i)=>v-a.center[i]);
 for(let i=0;i<3;i++)if(Math.abs(d[i])>=a.extent[i]+b.extent[i]-1e-5)return false;
 const tests=[...a.basis,...b.basis];
 for(const u of a.basis)for(const v of b.basis)tests.push(cross(u,v));
 for(const axis of tests){
  if(dot(axis,axis)<1e-7)continue;
  const ra=a.basis.reduce((s,v,i)=>s+Math.abs(dot(axis,v))*a.half[i],0);
  const rb=b.basis.reduce((s,v,i)=>s+Math.abs(dot(axis,v))*b.half[i],0);
  if(Math.abs(dot(axis,d))>=ra+rb-1e-5)return false;
 }
 return true;
}
let tested=0;
for(const source of models){
 for(const firstAxis of axes)for(const secondAxis of axes){
  if(firstAxis===secondAxis)continue;
  for(const a of [-1,1])for(const b of [-1,1]){
   const offset=[0,0,0];
   offset[axes.indexOf(firstAxis)]=16*a;
   offset[axes.indexOf(secondAxis)]=16*b;
   const sign=a===b?1:-1;
   for(let frame=0;frame<8;frame++){
    const angle=22.5*frame/8;
    const first=source.elements.map(el=>cube(el,firstAxis,angle,[0,0,0]));
    const other=models[0].elements.map(el=>cube(el,secondAxis,sign*angle+11.25,offset));
    for(const x of first)for(const y of other)
      assert(!intersects(x,y),
        "Crossed wooden gear collision at "+firstAxis+"/"+secondAxis+
        ", offset "+a+","+b+", frame "+frame);
    tested++;
   }
  }
 }
}
assert.equal(tested,384);
console.log("PASS 384 wooden crossed-gear swept samples (including handled lever)");
