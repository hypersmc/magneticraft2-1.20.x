#!/usr/bin/env node
// Bronze Stamp Mill visible belt + three stamp impact plate contract.
// All checks are against immutable authored/canonical geometry; no Minecraft
// classes are needed to run this script.
import fs from "node:fs";
import assert from "node:assert/strict";
import {inflateSync} from "node:zlib";
import {stampStrikeBoxes,buildStrikePlate} from "./build-strike-plate.mjs";

const read=p=>JSON.parse(fs.readFileSync(p,"utf8"));
const base="src/main/resources/";
const asset=id=>base+"assets/magneticraft2/models/multiblock/bronze_stamp_mill_"+id+".json";
const strike=read(asset("strike_plate"));
const authored=buildStrikePlate();
assert.equal(JSON.stringify(strike,null,2)+"\n",JSON.stringify(authored,null,2)+"\n",
 "New strike-plate asset differs from the authored generator");
assert.equal(stampStrikeBoxes.length,8);
assert.ok(strike.elements.length<=70&&strike.elements.length>=30,
 "Strike-plate mesh exceeds the safe visual-detail face budget");
const dirs={north:[2,-1,0,1],south:[2,1,0,1],east:[0,1,2,1],
 west:[0,-1,2,1],up:[1,1,0,2],down:[1,-1,0,2]};
const verifyModel=(model,id)=>{
 const planes=new Map();
 for(const e of model.elements){
  assert.ok(e.from.every((v,i)=>v>=0&&v<e.to[i]&&e.to[i]<=16),id+" panel bounds invalid");
  for(const [face,tex] of Object.entries(e.faces)){
   const [ax,sign,u,v]=dirs[face],plane=sign>0?e.to[ax]:e.from[ax];
   assert.ok(model.textures[tex.texture.slice(1)],id+" unresolved texture "+tex.texture);
   assert.ok(tex.uv?.length===4&&tex.uv.every(v=>v>=0&&v<=16),
     id+" texture UV out of range");
   assert.ok(Math.abs(tex.uv[2]-tex.uv[0]-(e.to[u]-e.from[u]))<1e-4&&
             Math.abs(tex.uv[3]-tex.uv[1]-(e.to[v]-e.from[v]))<1e-4,
             id+" compressed or stretched texture on "+face);
   const key=face+":"+plane.toFixed(5);
   for(const previous of planes.get(key)||[]){
    assert.ok(Math.min(e.to[u],previous.to[u])-Math.max(e.from[u],previous.from[u])<=1e-5||
              Math.min(e.to[v],previous.to[v])-Math.max(e.from[v],previous.from[v])<=1e-5,
      id+" has duplicate coplanar faces");
   }
   planes.set(key,[...(planes.get(key)||[]),e]);
  }
 }
};
verifyModel(strike,"strike plate");
const shell=read("tools/multiblock-models/designs/bronze_stamp_mill.json");
const overlaps=(a,b)=>a.from.every((v,i)=>
 Math.min(a.to[i],b.to[i])-Math.max(v,b.from[i])>1e-5);
for(const x of [-8,8,24])for(const b of stampStrikeBoxes){
 const point=[x,23,-10];
 const located={from:b.from.map((v,i)=>v+point[i]-8),
                to:b.to.map((v,i)=>v+point[i]-8)};
 assert.ok(located.from[1]>23&&located.to[1]<24,
  "Strike faces must stand above the trough and below a fully lowered shoe");
 for(const staticPart of shell.boxes){
  assert.ok(!overlaps(located,staticPart),
   "New plate intersects V2 static structure: "+staticPart.name);
 }
 // Front item modules begin at z=16 and the input module at z=-32.
 assert.ok(located.from[2]>-16&&located.to[2]<-4,
  "Plate must remain inside the crushing trough, not a module block");
}
const lower=read(asset("drive_belt_straight"));
const upper=read(asset("drive_belt_upper"));
const wrap=read(asset("drive_belt_wrap"));
for(const [name,model]of [["lower",lower],["upper",upper],["wrap",wrap]]){
 assert.equal(model.textures.belt,"magneticraft2:block/bronze_stamp_mill_drive_belt",
  name+" belt is not using the Stamp Mill's opaque leather material");
 verifyModel(model,name+" leather belt");
}
assert.equal(lower.elements.length,8,"Lower flight should contain only outer faces");
assert.equal(upper.elements.length,8,"Upper flight should contain only outer faces");
for(const panel of [...lower.elements,...upper.elements])
 assert.ok(!Object.keys(panel.faces).some(face=>face==="up"||face==="down"),
  "End caps inside the continuous belt cause seam flicker");
const ybounds=model=>[
 Math.min(...model.elements.map(e=>e.from[1])),
 Math.max(...model.elements.map(e=>e.to[1]))
];
assert.deepEqual(ybounds(lower),[0,16]);
assert.deepEqual(ybounds(upper),[0,14.5]);
// Lower pulley centre=8; upper centre=38.5; straight runs join at 24.
assert.deepEqual([8+ybounds(lower)[0],8+ybounds(lower)[1],
                  24+ybounds(upper)[0],24+ybounds(upper)[1]],
 [8,24,24,38.5],"Belt flights must meet BOTH pulleys, not start 8px late");
const png=fs.readFileSync(base+"assets/magneticraft2/textures/block/bronze_stamp_mill_drive_belt.png");
assert.equal(png.subarray(0,8).toString("hex"),"89504e470d0a1a0a");
assert.equal(png.readUInt32BE(16),16);
assert.equal(png.readUInt32BE(20),16);
assert.equal(png[24],8,"Leather texture must use 8-bit channels");
assert.equal(png[25],6,"Leather texture must be opaque RGBA pixel art");
const chunks=[];let off=8;
while(off<png.length){
 const len=png.readUInt32BE(off),name=png.toString("ascii",off+4,off+8);
 if(name==="IDAT")chunks.push(png.subarray(off+8,off+8+len));
 off+=12+len;
}
const raw=inflateSync(Buffer.concat(chunks));
let index=0,previous=Buffer.alloc(64);
for(let y=0;y<16;y++){
 const filter=raw[index++],row=Buffer.alloc(64);
 assert.ok(filter>=0&&filter<=4,"Unsupported PNG row filter");
 for(let x=0;x<64;x++){
  const a=x>=4?row[x-4]:0,b=previous[x],c=x>=4?previous[x-4]:0;
  const prediction=filter===0?0:filter===1?a:filter===2?b:filter===3?Math.floor((a+b)/2):
   (Math.abs(b-c)<=Math.abs(a+b-2*c)&&Math.abs(b-c)<=Math.abs(a-c)?a:
    Math.abs(a-c)<=Math.abs(b-c)&&Math.abs(a-c)<=Math.abs(a+b-2*c)?b:c);
  row[x]=(raw[index++]+prediction)&255;
 }
 for(let x=3;x<64;x+=4)assert.equal(row[x],255,
  "Transparent belt pixel at row "+y+" column "+Math.floor(x/4));
 previous=row;
}
const client=fs.readFileSync("src/main/java/com/magneticraft2/client/Clientsetup.java","utf8");
const renderer=fs.readFileSync("src/main/java/com/magneticraft2/client/render/blocks/stage/bronze/BronzeStampMillRenderer.java","utf8");
assert.ok(client.includes('"multiblock/bronze_stamp_mill_strike_plate"'),
 "New visual strike model not registered to bake");
assert.ok(renderer.includes("STRIKE_PLATE")&&renderer.includes("stations[i]/16D,23D/16D,-10D/16D,null,0F")&&
          renderer.includes("int[] stations={-8,8,24}"),
 "Three wear plates must be centered beneath the existing hammer stations");
assert.ok(renderer.includes("pulleyX,8D/16D,lowerZ,null,0F")&&
          renderer.includes("pulleyX,24D/16D,lowerZ,null,0F"),
 "Belt flights have slipped off their actual pulley tangents");
console.log("Stamp Mill visuals PASS: 3 strike plates, "+strike.elements.length+
 " authored faces each; 2 connected leather flights, opaque 16x16 texture");
