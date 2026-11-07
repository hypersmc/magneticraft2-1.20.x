#!/usr/bin/env node
// Rebuild four oriented Hearth render-air models and Voxel V2 profile.
// Canonical geometry lives in the SOUTH AIR model; structural colliders
// are indexed below. No Forge code changes, no per-tick renderer.
import fs from "node:fs";
import path from "node:path";
import assert from "node:assert/strict";
const mode=process.argv.slice(2);
assert.deepEqual(mode.length,1,"Exactly one argument required");
assert.ok(["--check","--write"].includes(mode[0]),"Use --check or --write");
const root=path.join(process.cwd(),"src/main/resources");
const id="iron_forge_hearth";
const rj=p=>JSON.parse(fs.readFileSync(path.join(root,p),"utf8"));
const canonical=rj("assets/magneticraft2/models/multiblock/"+
 id+"_south/part_0_1_-1.json");
assert.equal(canonical.elements.length,15,"Chamber component count changed");
const physical=[0,2,3,4,5,11,12,13,14]; // 9 bearing parts, charcoal decorative
const dirs=["south","west","north","east"];
const offsets={south:[0,1,-1],west:[1,1,0],north:[0,1,1],east:[-1,1,0]};
const faces=["north","south","east","west","up","down"];
const rotate=(from,to,d)=>{
 const [x,y,z]=from,[a,b,c]=to;
 if(d==="south")return [from,to];
 if(d==="north")return [[16-a,y,16-c],[16-x,b,16-z]];
 if(d==="east")return [[z,y,16-a],[c,b,16-x]];
 return [[16-c,y,x],[16-z,b,a]];
};
const modelPart=(part,d)=>{
 const [from,to]=rotate(part.from,part.to,d);
 const [w,h,t]=to.map((n,i)=>n-from[i]);
 assert.ok([w,h,t].every(n=>n>0));
 assert.ok(from.every(n=>n>=0)&&to.every(n=>n<=16));
 const dims={north:[w,h],south:[w,h],east:[t,h],west:[t,h],up:[w,t],down:[w,t]};
 const material=part.faces.north.texture;
 assert.ok(Object.values(part.faces).every(v=>v.texture===material),"One texture per cuboid");
 return {from,to,faces:Object.fromEntries(faces.map(side=>
  [side,{texture:material,uv:[0,0,...dims[side]]}]))};
};
const results=new Map();
const fmt=o=>JSON.stringify(o,null,2)+"\n";
let south;
for(const d of dirs){
 const bpPath="data/magneticraft2/multiblocks/"+id+"_"+d+".json";
 const bp=rj(bpPath),layers=[bp.structure.layout.layer1,bp.structure.layout.layer2];
 assert.deepEqual(bp.structure.dimensions,[3,2,3]);
 let air=null,controller=null;
 for(let y=0;y<2;y++)for(let z=0;z<3;z++)for(let x=0;x<3;x++){
  const s=layers[y][z][x];
  if(s==="C"){assert.equal(controller,null);controller=[x,y,z];}
  if(s==="A"){assert.equal(air,null);air=[x,y,z];}
 }
 assert.ok(air&&controller,"Structure must include exactly one air and controller");
 const offset=air.map((v,k)=>v-controller[k]);
 assert.deepEqual(offset,offsets[d],"Unexpected AIR offset");
 bp.settings.render_air_anchors=[offset];
 results.set(bpPath,fmt(bp));
 const oriented={parent:canonical.parent,ambientocclusion:canonical.ambientocclusion,
  textures:canonical.textures,elements:canonical.elements.map(e=>modelPart(e,d))};
 results.set("assets/magneticraft2/models/multiblock/"+id+"_"+d+
  "/part_"+offset.join("_")+".json",fmt(oriented));
 if(d==="south")south={layers,controller,offset};
}
const occupied=[];
for(let y=0;y<2;y++)for(let z=0;z<3;z++)for(let x=0;x<3;x++){
 if(south.layers[y][z][x]==="A")continue;
 const dx=x-south.controller[0],dz=z-south.controller[2];
 occupied.push([16*dx,16*y,16*dz,16*(dx+1),16*(y+1),16*(dz+1)]);
}
for(const i of physical){
 const e=canonical.elements[i];
 occupied.push([...e.from.map((v,k)=>v+south.offset[k]*16),
                ...e.to.map((v,k)=>v+south.offset[k]*16)]);
}
assert.equal(occupied.length,26);
results.set("data/magneticraft2/multiblock_voxels/"+id+".json",
 fmt({format:1,unit:"model_pixels",collision:occupied,selection:occupied}));
for(const [p,source] of results){
 const dest=path.join(root,p);
 if(mode[0]==="--check"){
  assert.ok(fs.existsSync(dest),"Missing generated resource "+p);
  assert.deepEqual(JSON.parse(fs.readFileSync(dest,"utf8")),JSON.parse(source),
    "Stale output "+p);
 }else{
  fs.mkdirSync(path.dirname(dest),{recursive:true});
  fs.writeFileSync(dest,source);
 }
}
console.log("Iron Forge Hearth AIR chamber "+(mode[0]==="--check"?"PASS":"written")+
 ": four blockstate anchors, four models, 26 voxel boxes. "+
 "No Forge compilation or in-game testing.");
