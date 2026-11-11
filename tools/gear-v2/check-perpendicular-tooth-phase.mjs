// Geometry regression using actual Large Gear cuboids and their blockstate axes.
// This is NOT a Minecraft runtime render or multiplayer simulation.
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),"../..");
const source=fs.readFileSync(path.join(root,"src/main/java/com/magneticraft2/common/systems/GEAR/GearNetworkManager.java"),"utf8");
const model=JSON.parse(fs.readFileSync(path.join(root,"src/main/resources/assets/magneticraft2/models/block/wrought_iron_gear_large.json"),"utf8"));
assert.equal(model.elements.length,66);
const at=source.indexOf("private float calculatePerpendicularMeshPhaseDegrees(");
assert(at>=0,"crossed phase method missing");
const method=source.slice(at,source.indexOf("private float calculateMeshedPhaseDegrees(",at));
assert(method.includes("toothPitch * 0.5F"),"missing half-pitch offset");
assert(method.includes("turnSign * current.getMeshPhaseDegrees()"),"upstream phase lost");
assert.equal((source.match(/calculatePerpendicularMeshPhaseDegrees\(/g)||[]).length,4,"three call sites required");
const axes=["X","Y","Z"],rad=Math.PI/180;
const dot=(a,b)=>a[0]*b[0]+a[1]*b[1]+a[2]*b[2];
const cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];
function rotate(axis,degrees,[x,y,z]){
 const a=degrees*rad,s=Math.sin(a),c=Math.cos(a);
 return axis==="X"?[x,c*y-s*z,s*y+c*z]
   :axis==="Y"?[c*x+s*z,y,-s*x+c*z]
   :[c*x-s*y,s*x+c*y,z];
}
function box(e,axis,angle,offset){
 let centre=e.from.map((v,j)=>(v+e.to[j])/2-8);
 let basis=[[1,0,0],[0,1,0],[0,0,1]];
 const half=e.from.map((v,j)=>(e.to[j]-v)/2);
 function turn(a,d){centre=rotate(a,d,centre);basis=basis.map(v=>rotate(a,d,v));}
 if(e.rotation)turn("Y",e.rotation.angle);
 if(axis==="X"){turn("X",90);turn("Y",90);}
 else if(axis==="Z")turn("X",90);
 turn(axis,angle);
 return{centre:centre.map((v,j)=>v+offset[j]),basis,half,
   bound:[0,1,2].map(j=>basis.reduce((s,b,k)=>s+Math.abs(b[j])*half[k],0))};
}
function intersects(a,b){
 const d=b.centre.map((v,j)=>v-a.centre[j]);
 for(let i=0;i<3;i++)if(Math.abs(d[i])>=a.bound[i]+b.bound[i]+1e-5)return false;
 const dirs=[...a.basis,...b.basis];
 for(const x of a.basis)for(const y of b.basis)dirs.push(cross(x,y));
 for(const v of dirs){
   if(dot(v,v)<1e-8)continue;
   const ra=a.basis.reduce((s,w,i)=>s+Math.abs(dot(v,w))*a.half[i],0);
   const rb=b.basis.reduce((s,w,i)=>s+Math.abs(dot(v,w))*b.half[i],0);
   if(Math.abs(dot(v,d))>=ra+rb+1e-5)return false;
 }
 return true;
}
function collisions(a,b){
 let count=0;
 for(const x of a)for(const y of b)if(intersects(x,y))count++;
 return count;
}
let cases=0,oldCollisions=0;
for(const firstAxis of axes)for(const secondAxis of axes){
 if(firstAxis===secondAxis)continue;
 for(const a of [-1,1])for(const b of [-1,1]){
   const offset=[0,0,0];
   offset[axes.indexOf(firstAxis)]=16*a;
   offset[axes.indexOf(secondAxis)]=16*b;
   const sign=a===b?1:-1;
   let oldWorst=0;
   for(const inheritedPhase of [0,5.625,11.25]){
     for(let frame=0;frame<8;frame++){
       const t=22.5*frame/8;
       const first=model.elements.map(e=>box(e,firstAxis,t+inheritedPhase,[0,0,0]));
       const second=model.elements.map(e=>box(e,secondAxis,sign*t+11.25+sign*inheritedPhase,offset));
       assert.equal(collisions(first,second),0,
         "crossed teeth collide for "+firstAxis+"/"+secondAxis+" offset "+a+","+b+" frame "+frame);
       if(inheritedPhase===0){
         const old=model.elements.map(e=>box(e,secondAxis,sign*t,offset));
         oldWorst=Math.max(oldWorst,collisions(first,old));
       }
     }
   }
   assert(oldWorst>0,"old zero phase no longer reproduces collision");
   oldCollisions+=oldWorst;
   cases++;
 }
}
assert.equal(cases,24);
console.log("PASS 24 crossed gear orientation/quadrant cases, 3 source phases, 8 motion steps");
console.log("Half-tooth crossed phase: zero cuboid intersections; zero-phase baseline collision sum: "+oldCollisions);
console.log("Build, visual and incompatible-loop tests remain outstanding.");
