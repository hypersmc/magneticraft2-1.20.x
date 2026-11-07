#!/usr/bin/env node
// Iron Flywheel split bearings: regenerate four facing models and V2 shapes
// from single canonical SOUTH definition. --check or --write.
import fs from "node:fs";
import assert from "node:assert/strict";
import path from "node:path";
const args=process.argv.slice(2);assert.equal(args.length,1);
assert.ok(["--check","--write"].includes(args[0]));
const root="src/main/resources/",json=x=>JSON.stringify(x,null,2)+"\n";
const iron=(from,to)=>({from,to,mat:"iron"});
const bronze=(from,to)=>({from,to,mat:"bearing"});
const rings=z=>[iron([4,10,z],[12,12,z+3]),iron([4,4,z],[12,6,z+3]),
 bronze([4,6,z],[6,10,z+3]),bronze([10,6,z],[12,10,z+3])];
const parts={
  "-1_-1_0": [
    {
      "from": [
        1,
        0,
        0
      ],
      "to": [
        15,
        3,
        7
      ],
      "mat": "iron"
    },
    {
      "from": [
        5,
        3,
        2
      ],
      "to": [
        11,
        16,
        5
      ],
      "mat": "iron"
    }
  ],
  "1_-1_0": [
    {
      "from": [
        1,
        0,
        9
      ],
      "to": [
        15,
        3,
        16
      ],
      "mat": "iron"
    },
    {
      "from": [
        5,
        3,
        11
      ],
      "to": [
        11,
        16,
        14
      ],
      "mat": "iron"
    }
  ],
  "-1_0_0": [
    {
      "from": [
        5,
        0,
        2
      ],
      "to": [
        11,
        8,
        5
      ],
      "mat": "iron"
    },
    {
      "from": [
        8,
        6,
        2
      ],
      "to": [
        16,
        8,
        5
      ],
      "mat": "iron"
    }
  ],
  "1_0_0": [
    {
      "from": [
        5,
        0,
        11
      ],
      "to": [
        11,
        8,
        14
      ],
      "mat": "iron"
    },
    {
      "from": [
        0,
        6,
        11
      ],
      "to": [
        8,
        8,
        14
      ],
      "mat": "iron"
    }
  ],
  "0_0_0": [
    {
      "from": [
        4,
        10,
        2
      ],
      "to": [
        12,
        12,
        5
      ],
      "mat": "iron"
    },
    {
      "from": [
        4,
        4,
        2
      ],
      "to": [
        12,
        6,
        5
      ],
      "mat": "iron"
    },
    {
      "from": [
        4,
        6,
        2
      ],
      "to": [
        6,
        10,
        5
      ],
      "mat": "bearing"
    },
    {
      "from": [
        10,
        6,
        2
      ],
      "to": [
        12,
        10,
        5
      ],
      "mat": "bearing"
    },
    {
      "from": [
        4,
        10,
        11
      ],
      "to": [
        12,
        12,
        14
      ],
      "mat": "iron"
    },
    {
      "from": [
        4,
        4,
        11
      ],
      "to": [
        12,
        6,
        14
      ],
      "mat": "iron"
    },
    {
      "from": [
        4,
        6,
        11
      ],
      "to": [
        6,
        10,
        14
      ],
      "mat": "bearing"
    },
    {
      "from": [
        10,
        6,
        11
      ],
      "to": [
        12,
        10,
        14
      ],
      "mat": "bearing"
    },
    {
      "from": [
        0,
        6,
        2
      ],
      "to": [
        4,
        8,
        5
      ],
      "mat": "iron"
    },
    {
      "from": [
        12,
        6,
        11
      ],
      "to": [
        16,
        8,
        14
      ],
      "mat": "iron"
    }
  ],
  "0_-1_0": [],
  "-1_1_0": [],
  "0_1_0": [],
  "1_1_0": []
};
const dirs=["south","west","north","east"];
const rotate=(f,t,d)=>{const [x,y,z]=f,[X,Y,Z]=t;return d==="south"?[f,t]:
 d==="west"?[[16-Z,y,x],[16-z,Y,X]]:
 d==="north"?[[16-X,y,16-Z],[16-x,Y,16-z]]:[[z,y,16-X],[Z,Y,16-x]];};
const model=(pieces,d)=>({parent:"minecraft:block/block",ambientocclusion:false,
 textures:{iron:"magneticraft2:block/material/dark_work_iron",
 bearing:"magneticraft2:block/material/bronze_machined",
 particle:"magneticraft2:block/material/dark_work_iron"},
 elements:pieces.map(p=>{
 const [from,to]=rotate(p.from,p.to,d),[w,h,t]=to.map((n,i)=>n-from[i]);
 assert.ok([w,h,t].every(v=>v>0));
 assert.ok(from.every(v=>v>=0)&&to.every(v=>v<=16));
 return {from,to,faces:Object.fromEntries(Object.entries({
 north:[w,h],south:[w,h],east:[t,h],west:[t,h],up:[w,t],down:[w,t]})
 .map(([face,uv])=>[face,{texture:"#"+p.mat,uv:[0,0,...uv]}]))};
 })});
const files=new Map();
for(const [key,pieces] of Object.entries(parts)){
 if(key==="0_0_0")continue;
 const [x,y,z]=key.split("_").map(Number);
 for(const d of dirs){
 const [rx,rz]=d==="south"?[x,z]:d==="west"?[-z,x]:
 d==="north"?[-x,-z]:[z,-x];
 files.set("assets/magneticraft2/models/multiblock/iron_flywheel_"+
  d+"/part_"+rx+"_"+y+"_"+rz+".json",json(model(pieces,d)));
 }
}
files.set("assets/magneticraft2/models/block/iron_flywheel_controller.json",
 json(model(parts["0_0_0"],"south")));
const current=JSON.parse(fs.readFileSync(path.join(root,
 "data/magneticraft2/multiblock_voxels/iron_flywheel.json"),"utf8"));
const moving=current.collision.slice(-24);
assert.equal(moving.length,24);
const staticBoxes=[];
for(const [key,pieces] of Object.entries(parts)){
 const offset=key.split("_").map(Number);
 for(const p of pieces)staticBoxes.push(
  [...p.from.map((n,i)=>n+16*offset[i]),...p.to.map((n,i)=>n+16*offset[i])]);
}
assert.equal(staticBoxes.length,18);
const overlaps=(a,b)=>a.slice(0,3).every((n,i)=>
 n<b[i+3]-1e-9&&b[i]<a[i+3]-1e-9);
assert.ok(!staticBoxes.some(a=>moving.some(b=>overlaps(a,b))),
 "Static support intersects swept rim");
const boxes=[...staticBoxes,[7,7,0,9,9,16],...moving];
assert.equal(boxes.length,43);
files.set("data/magneticraft2/multiblock_voxels/iron_flywheel.json",
 json({format:1,unit:"model_pixels",collision:boxes,selection:boxes}));
for(const [name,expected] of files){
 const dest=path.join(root,name);
 if(args[0]==="--check"){
 assert.ok(fs.existsSync(dest),"Missing generated file "+name);
 assert.deepEqual(JSON.parse(fs.readFileSync(dest,"utf8")),
  JSON.parse(expected),"Stale generated file "+name);
 }else{
 fs.mkdirSync(path.dirname(dest),{recursive:true});
 fs.writeFileSync(dest,expected);
 }
}
console.log("Flywheel bearing contract "+args[0]+
 ": 32 directional models, 2 connected bearings, 43 physical boxes; no Java build.");
