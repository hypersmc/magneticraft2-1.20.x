#!/usr/bin/env node
// Bronze Stamp Mill visible belt + three stamp impact plate contract.
// All checks are against immutable authored/canonical geometry; no Minecraft
// classes are needed to run this script.
import fs from "node:fs";
import assert from "node:assert/strict";
import {inflateSync} from "node:zlib";
import {stampStrikeBoxes,buildStrikePlate} from "./build-strike-plate.mjs";
import {stampBeltSolids,buildStampBeltModels} from "./build-belt-models.mjs";


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
// The belt lives on two X-axis DRUMS, not sprockets, and must make an
// unbroken loop in ACTUAL renderer space (local baked model -0.5 blocks).
const lower=read(asset("drive_belt_straight"));
const upper=read(asset("drive_belt_upper"));
const wrap=read(asset("drive_belt_wrap"));
const pulley=read(asset("drive_pulley"));
const builtBelts=buildStampBeltModels();
const names=["bronze_stamp_mill_drive_belt_straight",
 "bronze_stamp_mill_drive_belt_upper",
 "bronze_stamp_mill_drive_belt_wrap",
 "bronze_stamp_mill_drive_pulley"];
assert.deepEqual(Object.keys(builtBelts),names,"Obsolete belt guard unexpectedly returned");
for(const id of names){
 const expected=builtBelts[id],actual=read(base+"assets/magneticraft2/models/multiblock/"+id+".json");
 assert.deepEqual(actual,expected,"Baked belt/drum does not match authored source: "+id);
 verifyModel(actual,id);
}
for(const [name,model]of [["lower",lower],["upper",upper],["wrap",wrap]])
 assert.equal(model.textures.belt,"magneticraft2:block/bronze_stamp_mill_drive_belt",
   name+" must use the dark opaque leather texture");
for(const id of ["drive_belt_guard_lower","drive_belt_guard_upper"])
 assert.ok(!fs.existsSync(asset(id)),"Do not resurrect exposed guard frame: "+id);
assert.equal(stampBeltSolids.bronze_stamp_mill_drive_belt_straight.length,2,
 "A flat endless belt should have exactly two straight flights");
assert.equal(stampBeltSolids.bronze_stamp_mill_drive_belt_upper.length,2);
assert.equal(lower.elements.length,8);
assert.equal(upper.elements.length,8);
assert.ok(wrap.elements.length>=50&&wrap.elements.length<=90,
 "Minecraft-faceted half-wrap lost, or has excessive geometry");
assert.ok(pulley.elements.length<=190,
 "Drum must cost LESS to render than the former spoked wheel");

// A smooth wooden drive drum with bronze outer contact, NOT an exposed
// sprocket-wheel trying to pull loose leather cords.
for(const metal of ["#iron","#bronze","#wood"])
 assert.ok(pulley.elements.some(e=>
   Object.values(e.faces).some(f=>f.texture===metal)),
   "Missing correct wood/bronze/iron drum material "+metal);
const beltWidth=stampBeltSolids.bronze_stamp_mill_drive_belt_straight[0].to[0]
 -stampBeltSolids.bronze_stamp_mill_drive_belt_straight[0].from[0];
const radialThickness=stampBeltSolids.bronze_stamp_mill_drive_belt_straight[0].to[2]
 -stampBeltSolids.bronze_stamp_mill_drive_belt_straight[0].from[2];
assert.ok(beltWidth>=1.55&&beltWidth<=1.7,"Leather ribbon must fit both drums");
assert.ok(radialThickness>=1.3&&radialThickness<=1.55,
 "Reject thin leather strings or giant tyre-sized drive bands");
assert.ok(![...lower.elements,...upper.elements].some(e=>
 Object.keys(e.faces).some(face=>face==="up"||face==="down")),
 "Shaft-wide flights may not draw coincident end caps at their junction");
assert.ok(!wrap.elements.some(e=>
 Object.keys(e.faces).includes("down")&&Math.abs(e.from[1]-8)<1e-5),
 "Half-wrap must not draw a coincident cap on its straight tangent");

// The crucial earlier bug: a render call uses
// pose.translate(center).translate(-0.5,-0.5,-0.5).
// Local belt y=0..16 at center=16 -> WORLD y=8..24.
// Local belt y=0..14.5 at center=32 -> WORLD y=24..38.5.
function transformed(box,[cx,cy,cz],turned=false){
 return {
  from:box.from.map((v,i)=>i===0?v+cx-8:turned?16-box.to[i]+[cx,cy,cz][i]-8:v+[cx,cy,cz][i]-8),
  to:box.to.map((v,i)=>i===0?v+cx-8:turned?16-box.from[i]+[cx,cy,cz][i]-8:v+[cx,cy,cz][i]-8)
 };
}
const transforms={
 bronze_stamp_mill_drive_belt_straight:[[[47.2,16,-10],false]],
 bronze_stamp_mill_drive_belt_upper:[[[47.2,32,-10],false]],
 bronze_stamp_mill_drive_belt_wrap:[[[47.2,8,-10],true],[[47.2,38.5,-10],false]],
 bronze_stamp_mill_drive_pulley:[[[47.2,8,-10],false],[[47.2,38.5,-10],false]]
};
const placed=name=>transforms[name].flatMap(([centre,turned])=>
 stampBeltSolids[name].map(b=>transformed(b,centre,turned)));
const extentY=name=>[
 Math.min(...placed(name).map(b=>b.from[1])),
 Math.max(...placed(name).map(b=>b.to[1]))
];
assert.deepEqual(extentY("bronze_stamp_mill_drive_belt_straight"),[8,24],
 "Lower flight must start at the actual LOWER pulley centre y=8");
assert.deepEqual(extentY("bronze_stamp_mill_drive_belt_upper"),[24,38.5],
 "Upper flight must end at upper pulley centre y=38.5");
const arcWorld=placed("bronze_stamp_mill_drive_belt_wrap");
assert.ok(Math.max(...arcWorld.filter(p=>p.to[1]<=8+1e-4).map(b=>b.to[1]))<=8+1e-4);
assert.ok(Math.min(...arcWorld.filter(p=>p.from[1]>=38.5-1e-4).map(b=>b.from[1]))>=38.5-1e-4);

// The two flat strips join without gaps, and each touches both rounded caps
// on its side at y=8 / y=38.5. The caps must not overlap the straight
// portions in volume (a common source of flickering).
for(const name of ["bronze_stamp_mill_drive_belt_straight","bronze_stamp_mill_drive_belt_upper"]){
 for(const straight of placed(name))for(const round of arcWorld)
  assert.ok(!overlaps(straight,round),
   "Leather half-wrap intersects a straight run (Z-fighting)");
}
const startCaps=stampBeltSolids.bronze_stamp_mill_drive_belt_wrap
 .filter(b=>Math.abs(b.from[1]-8)<1e-5);
assert.equal(startCaps.length,2);
for(const strap of stampBeltSolids.bronze_stamp_mill_drive_belt_straight)
 assert.ok(startCaps.some(cap=>
  Math.min(strap.to[2],cap.to[2])-Math.max(strap.from[2],cap.from[2])>1),
 "Leather loop is disconnected from its pulley tangent");

// Wheel rotates about +X. Radius envelope of ALL its cuboid corners must
// clear the inner radius of the stationary leather at every angle.
const sweepRadius=boxes=>Math.max(...boxes.flatMap(b=>
 [b.from[1],b.to[1]].flatMap(y=>
  [b.from[2],b.to[2]].map(z=>Math.hypot(y-8,z-8)))));
const radiusDrum=sweepRadius(stampBeltSolids.bronze_stamp_mill_drive_pulley);
const closestToCentre=box=>{
 const y=Math.max(box.from[1],Math.min(8,box.to[1]));
 const z=Math.max(box.from[2],Math.min(8,box.to[2]));
 return Math.hypot(y-8,z-8);
};
const clearance=Math.min(...stampBeltSolids.bronze_stamp_mill_drive_belt_wrap.map(closestToCentre))-radiusDrum;
assert.ok(clearance>.06&&clearance<.20,
 "Drum must rotate under leather without clipping or visibly floating: "+clearance);

// This extra geometry must NEVER touch the V2 machine, ports, or the
// existing bearing mounts. No new filler blocks were added.
let instancedFaces=0;
for(const name of names){
 instancedFaces+=builtBelts[name].elements.length*transforms[name].length;
 for(const b of placed(name)){
  for(const frame of shell.boxes)assert.ok(!overlaps(b,frame),
   "End drive visually clips the structural Stamp Mill frame: "+frame.name);
  assert.ok(b.to[0]<=48&&b.from[0]>46,
   "Drive must stay in the unchanged 5x3x4 footprint");
 }
}
assert.ok(instancedFaces<=525,
 "End drive rendered face budget regression: "+instancedFaces);
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
// In the renderer, the accepted static baked belt models are reference
// geometry only. Leather now moves via the BeltPath tessellated loop.
assert.ok(renderer.includes("BronzeStampMillBeltRenderer.render(")&&
          renderer.includes("beltTravel.computeIfAbsent(mill")&&
          !renderer.includes("part(pose,buffers,light,overlay,BELT_STRAIGHT")&&
          !renderer.includes("part(pose,buffers,light,overlay,BELT_UPPER")&&
          !renderer.includes("part(pose,buffers,light,overlay,BELT_WRAP"),
 "No duplicated static leather may cover the animated belt");
assert.ok(client.includes('"multiblock/bronze_stamp_mill_drive_pulley"')&&
          !client.includes("bronze_stamp_mill_drive_belt_guard")&&
          !renderer.includes("BELT_GUARD"),
 "Use the new solid drums and do not render the rejected external guard");
console.log("Stamp Mill visuals PASS: 3 strike plates, "+strike.elements.length+
 " authored faces each; closed flat leather loop on two solid bronze-rimmed drums");
