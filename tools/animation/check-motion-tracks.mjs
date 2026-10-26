#!/usr/bin/env node
// Exact-phase/geometry regression fixtures for generic rotation and
// cam-driven linear translation tracks. Dependency-free, no Minecraft.
import assert from "node:assert/strict";
import {readFileSync, existsSync} from "node:fs";

const json = path => JSON.parse(readFileSync(new URL(path,import.meta.url),"utf8"));
const scene=json("../../src/main/resources/assets/magneticraft2/animations/bronze_stamp_mill.json");
const binding=json("../../src/main/resources/assets/magneticraft2/machine_visuals/bronze_stamp_mill.json");
assert.equal(scene.format,"magneticraft2:visual_animation");
assert.equal(binding.format,"magneticraft2:machine_visual");
assert.equal(binding.integer_channels["stamp_mill.wear_stage"],"stamp_mill.wear_stage");
assert.equal(binding.angle_channels.output_shaft,"output_shaft");
const tracks=Object.values(scene.animations).flatMap(a=>a.tracks);
const near=(a,b,why)=>assert.ok(Number.isFinite(a) && Math.abs(a-b)<1e-8,
  `${why}: ${a} != ${b}`);
const stagePositions=[-8,8,24];
const stationNames=["left","middle","right"];
const inputAngleSamples=[];
for(let degrees=-720;degrees<=720;degrees+=3)inputAngleSamples.push(degrees);
const rotation=(t,angle)=>angle*(t.scale??1)+(t.phase_degrees??0);
const wave=(t,angle)=>{
  const rad=rotation(t,angle)*Math.PI/180;
  return t.waveform==="cosine"?Math.cos(rad):Math.sin(rad);
};
const travel=(t,angle,isRunning)=> !t || (t.when==="running" && !isRunning)
    ? 0 : t.distance * ((t.bias??0)+(t.amplitude??1)*wave(t,angle));
let poses=0;
for(let i=0;i<3;i++) {
 const tag=stationNames[i],station=stagePositions[i]/16;
 const cam=scene.components["cam_"+tag],hammer=scene.components["hammer_"+tag];
 const plate=scene.components["plate_"+tag];
 assert.ok(cam && hammer && plate,"Missing one of three stamp stations");
 const camTrack=tracks.find(t=>t.target==="cam_"+tag && t.property==="rotation");
 const hammerTrack=tracks.find(t=>t.target==="hammer_"+tag && t.property==="translation");
 assert.ok(camTrack && hammerTrack);
 assert.equal(camTrack.axis,"x");
 assert.equal(hammerTrack.axis,"y");
 assert.equal(hammerTrack.waveform,"cosine");
 assert.equal(hammerTrack.when,"running");
 near(camTrack.phase_degrees,120*i,"Cam phase");
 near(hammerTrack.phase_degrees,120*i,"Hammer phase");
 near(hammerTrack.distance,.32,"Lift stroke");
 near(hammerTrack.amplitude,.5,"Cosine amplitude");
 near(hammerTrack.bias,.5,"Cosine bias");
 near(cam.model_offset[0]+cam.pivot[0],station,"Cam shaft X");
 near(cam.model_offset[1]+cam.pivot[1],38.5/16,"Cam shaft Y");
 near(cam.model_offset[2]+cam.pivot[2],-10/16,"Cam shaft Z");
 near(plate.model_offset[0]+plate.pivot[0],station,"Strike plate X");
 near(plate.model_offset[1]+plate.pivot[1],23/16,"Strike plate Y");
 near(plate.model_offset[2]+plate.pivot[2],-10/16,"Strike plate Z");
 near(hammer.model_offset[0],(stagePositions[i]-8)/16,"Hammer X");
 near(hammer.model_offset[1],24/16,"Hammer rest Y");
 near(hammer.model_offset[2],(-10-8)/16,"Hammer Z");
 assert.equal(hammer.model_variants.source,"stamp_mill.wear_stage");
 for (let stage=2;stage<=5;stage++) {
  assert.equal(hammer.model_variants.models[stage],
    "magneticraft2:multiblock/bronze_stamp_mill_hammer_worn");
 }
 // Old Java formula from BronzeStampMillBlockEntity#getHammerLift:
 // if(!processing)return 0; 0.32*(0.5+0.5*cos(angle+120*i))
 for (const angle of inputAngleSamples) {
   const camAngle=rotation(camTrack,angle);
   const running=travel(hammerTrack,angle,true);
   const idle=travel(hammerTrack,angle,false);
   const oldJava=.32*(.5+.5*Math.cos((angle+120*i)*Math.PI/180));
   near(camAngle,angle+120*i,"Gear V2 cam phase");
   near(running,oldJava,"Original Java hammer displacement");
   near(idle,0,"Original Java stopped hammer");
   assert.ok(running>-1e-10 && running<=.32000000001,
        "Travel must remain within rail bounds");
   // All four facings preserve one shared local cam/hammer pair before
   // the existing renderer rotates the complete machine once.
   for(const facing of [0,90,180,-90]) {
     const theta=facing*Math.PI/180;
     const localX=hammer.model_offset[0];
     const localZ=hammer.model_offset[2];
     const dx=localX-.5, dz=localZ-.5;
     const worldX=.5+dx*Math.cos(theta)+dz*Math.sin(theta);
     const worldZ=.5-dx*Math.sin(theta)+dz*Math.cos(theta);
     assert.ok(Number.isFinite(worldX)&&Number.isFinite(worldZ));
     near(hammer.model_offset[1]+running,1.5+oldJava,
          "Hammer's rotated orientation does not affect lift");
     poses++;
   }
 }
}
assert.equal(poses,3*inputAngleSamples.length*4);
console.log(`Stamp Mill JSON motion: 3 cams + 3 hammers; ${poses} `+
  "facing/angle poses match original lift and 120-degree phase offsets.");
