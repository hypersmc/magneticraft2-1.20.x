// Static and swept 2D model-space acceptance check for Iron/Steel gear pairs.
// No Forge, client rendering, UV-image, or multiplayer simulation is performed.
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const models="src/main/resources/assets/magneticraft2/models/block";
const load=name=>JSON.parse(fs.readFileSync(path.join(root,models,name+".json"),"utf8"));
const medium=load("wrought_iron_gear_medium");
const large=load("wrought_iron_gear_large");
assert.equal(medium.elements.length,41);
assert.equal(large.elements.length,66);
for (const [model,n] of [[medium,8],[large,16]]) {
  assert.equal(model.parent,"minecraft:block/block");
  assert.equal(model.textures.iron,"magneticraft2:block/material/dark_work_iron");
  assert.equal(model.textures.collar,"magneticraft2:block/material/bronze_machined");
  const count=n===8?8:16;
  assert.equal(model.elements.length,count+(n===8?4:8)+2*count+(n===8?13:10));
  assert(model.elements.some(e=>e.from[0]===7&&e.from[1]===0&&e.from[2]===7
    &&e.to[0]===9&&e.to[1]===16&&e.to[2]===9),"original centered axle");
  for (const e of model.elements) {
    const dim=e.to.map((v,j)=>v-e.from[j]);
    assert(dim.every(v=>v>0));
    if(e.rotation) {
      assert.equal(e.rotation.axis,"y");
      assert.deepEqual(e.rotation.origin,[8,8,8]);
      assert([-45,-22.5,0,22.5,45].includes(e.rotation.angle));
    }
    for (const [side,face] of Object.entries(e.faces)) {
      const xy=side==="east"||side==="west"?[dim[2],dim[1]]
        :side==="north"||side==="south"?[dim[0],dim[1]]:[dim[0],dim[2]];
      assert(face.uv,side+" face lacks UV");
      assert(Math.abs(face.uv[2]-face.uv[0]-xy[0])<.001
        &&Math.abs(face.uv[3]-face.uv[1]-xy[1])<.001,"stretched UV on "+side);
    }
  }
}
for(const [name,parent] of [
  ["steel_gear_medium","wrought_iron_gear_medium"],
  ["steel_gear_large","wrought_iron_gear_large"]])
  assert.equal(load(name).parent,"magneticraft2:block/"+parent);

// Project model cuboids into the actual XZ gear plane, rotate by the same
// 8/16-tooth phase law used in GearNetworkManager and sample a full tooth pitch.
function elementFootprint(e,id) {
  const rad=(e.rotation?.angle||0)*Math.PI/180,c=Math.cos(rad),s=Math.sin(rad);
  const f=e.from,t=e.to;
  return{id,low:f[1],high:t[1],poly:[[f[0],f[2]],[t[0],f[2]],
    [t[0],t[2]],[f[0],t[2]]].map(([x,z])=>{
      x-=8;z-=8;return[x*c+z*s,-x*s+z*c];
    })};
}
const sm=medium.elements.map(elementFootprint),lg=large.elements.map(elementFootprint);
function transform(input,degrees,dx,dz) {
  const rad=degrees*Math.PI/180,c=Math.cos(rad),s=Math.sin(rad);
  return input.map(a=>{
    const p=a.poly.map(([x,z])=>[x*c+z*s+dx,-x*s+z*c+dz]);
    return{...a,p,minX:Math.min(...p.map(v=>v[0])),maxX:Math.max(...p.map(v=>v[0])),
      minZ:Math.min(...p.map(v=>v[1])),maxZ:Math.max(...p.map(v=>v[1]))};
  });
}
const cross=(a,b,p)=>(b[0]-a[0])*(p[1]-a[1])-(b[1]-a[1])*(p[0]-a[0]);
function intersectionArea(a,b) {
  let shape=a;
  for(let j=0;j<4;j++){
    if(!shape.length)return 0;
    const v=b[j],w=b[(j+1)%4],out=[];
    for(let i=0;i<shape.length;i++){
      const p=shape[i],q=shape[(i+1)%shape.length],
        dp=cross(v,w,p),dq=cross(v,w,q),aInside=dp>=-1e-8,bInside=dq>=-1e-8;
      if(aInside)out.push(p);
      if(aInside!==bInside){
        const t=dp/(dp-dq);
        out.push([p[0]+(q[0]-p[0])*t,p[1]+(q[1]-p[1])*t]);
      }
    }
    shape=out;
  }
  let area=0;
  for(let i=0;i<shape.length;i++){
    const p=shape[i],q=shape[(i+1)%shape.length];
    area+=p[0]*q[1]-q[0]*p[1];
  }
  return Math.abs(area)*.5;
}
function pointSegmentSquared(p,a,b) {
  const dx=b[0]-a[0],dz=b[1]-a[1],len=dx*dx+dz*dz,
    t=len===0?0:Math.min(1,Math.max(0,((p[0]-a[0])*dx+(p[1]-a[1])*dz)/len)),
    ux=p[0]-(a[0]+dx*t),uz=p[1]-(a[1]+dz*t);
  return ux*ux+uz*uz;
}
function quadGap(a,b) {
  let best=Infinity;
  for(let i=0;i<4;i++)for(let j=0;j<4;j++){
    best=Math.min(best,pointSegmentSquared(a[i],b[j],b[(j+1)%4]));
    best=Math.min(best,pointSegmentSquared(b[j],a[i],a[(i+1)%4]));
  }
  return Math.sqrt(best);
}
function runPair(label,A,B,ta,tb,dx,dz,maxArea,maxGap){
  let worstArea=0,worstGap=0,overlapFrames=0;
  for(let frame=0;frame<144;frame++){
    const aAngle=360/ta*frame/144,bAngle=(180-ta*aAngle)/tb;
    const x=transform(A,aAngle,0,0),y=transform(B,bAngle,dx,dz);
    let area=0,bestDistance=Infinity;
    for(const p of x)for(const q of y){
      if(p.high<=q.low||q.high<=p.low)continue;
      const bx=Math.max(0,p.minX-q.maxX,q.minX-p.maxX),
            bz=Math.max(0,p.minZ-q.maxZ,q.minZ-p.maxZ);
      if(bx>1||bz>1)continue;
      const hit=intersectionArea(p.p,q.p);
      if(hit>1e-6)area+=hit;
      else bestDistance=Math.min(bestDistance,quadGap(p.p,q.p));
    }
    if(area>1e-6){overlapFrames++;bestDistance=0;}
    worstArea=Math.max(worstArea,area);
    worstGap=Math.max(worstGap,bestDistance);
  }
  assert(worstArea<=maxArea,label+" gear penetration: "+worstArea.toFixed(4));
  assert(worstGap<=maxGap,label+" visible gap: "+worstGap.toFixed(3));
  console.log(label+": worst projected overlap "+worstArea.toFixed(4)
    +" px^2, worst projected gap "+worstGap.toFixed(3)
    +" px, overlapping phases "+overlapFrames+"/144");
}
runPair("8T/8T",sm,sm,8,8,16,0,.001,.7);
runPair("16T/16T",lg,lg,16,16,32,0,.001,.7);
// The integer-grid diagonal spacing cannot perfectly satisfy both module
// pitch radii. Allow only a small model-space tooth-tip overlap (<.05 px^2),
// not substantial penetration. Real in-game visual inspection is still required.
runPair("16T/8T diagonal",lg,sm,16,8,16,16,.05,.3);
console.log("PASS coordinated metal gear model static/swept acceptance");
