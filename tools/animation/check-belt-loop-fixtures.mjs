#!/usr/bin/env node
/**
 * Numerical and structural regression checks for typed UV-tiled open belts.
 * Run: node tools/animation/check-belt-loop-fixtures.mjs
 *
 * Does not execute Forge classes or replace in-game UV/FPS verification.
 */
import assert from "node:assert/strict";
import { readFileSync, existsSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { join } from "node:path";

const root = fileURLToPath(new URL("../../", import.meta.url));
const parse = p => JSON.parse(readFileSync(join(root,p),"utf8"));
const base = parse("src/main/resources/assets/magneticraft2/animations/bronze_stamp_mill.json");
const demo = parse("docs/examples/json-belt-loop-pack/assets/magneticraft2/animations/bronze_stamp_mill.json");
const oldScene = parse("docs/examples/json-animation-multiblock-pack/assets/magneticraft2/animations/bronze_stamp_mill.json");
const stampPack = parse("docs/examples/json-stamp-mill-cam-pack/assets/magneticraft2/animations/bronze_stamp_mill.json");
const b = base.belt_loops?.drive_belt;
const test = demo.belt_loops?.drive_belt;
const near = (a,c,message) => assert.ok(Number.isFinite(a) && Math.abs(a-c)<1e-8,
    `${message}: ${a} != ${c}`);
assert.ok(b && test && !oldScene.belt_loops && !stampPack.belt_loops);
assert.equal(b.type,"open_belt");
assert.equal(b.input,"output_phase");
assert.equal(base.inputs[b.input].source,"mechanical.visual_angle_degrees");
assert.equal(b.rotation_axis,"x");
assert.equal(b.texture,"magneticraft2:textures/block/bronze_stamp_mill_drive_belt.png");
assert.ok(existsSync(join(root,"src/main/resources/assets/magneticraft2",b.texture.split(":")[1])));
assert.equal(test.texture,"minecraft:textures/block/light_blue_wool.png");
assert.equal(test.speed_scale,-1);
assert.equal(test.half_width,0.0625);
assert.equal(test.texture_repeat,1);

const expectedR=(4.08+5.45)/32;
const expectedW=(8.72-7.13)/32;
const expectedT=(5.45-4.08)/32;
near(b.start_radius,expectedR,"Existing center radius");
near(b.end_radius,expectedR,"Existing end radius");
near(b.drive_radius,expectedR,"Original shaft travel radius");
near(b.half_width,expectedW,"Original leather width");
near(b.half_thickness,expectedT,"Original leather thickness");
near(b.texture_repeat,1,"Original native texture repeat");
assert.deepEqual(b.start_center,[47.2/16,8/16,-10/16]);
assert.deepEqual(b.end_center,[47.2/16,38.5/16,-10/16]);
assert.equal(b.speed_scale,1);
assert.equal(b.texture_offset,0);

// The original BeltPath helper adds its internal 0.030 block clearance.
// The animation parser subtracts that clearance before calling it, to
// preserve the exact original centerline radius instead of expanding it.
const clearance=0.03;
near((b.start_radius-clearance)+clearance,expectedR,"No double clearance");

// Original renderer used distanceForDegrees(theta)=theta*PI/180*radius.
const dist=(belt,degrees)=>degrees*Math.PI/180*belt.drive_radius*
    (belt.speed_scale??1)+(belt.texture_offset??0);
const positive=(value,period)=>((value%period)+period)%period;
function trackWrapped(stream) {
 let prev=null, total=0, result=[];
 for(const angle of stream) {
   if(prev===null)total=angle;
   else {
     let delta=(angle-prev)%360;
     if(delta>180)delta-=360;
     else if(delta< -180)delta+=360;
     total+=delta;
   }
   prev=angle;
   result.push(total);
 }
 return result;
}
const forward=[350,355,359,1,7,30,89,181,270,355,3,8];
const backward=[8,3,355,270,181,89,30,7,1,359,355,350];
const forwardUnwrapped=trackWrapped(forward);
const backwardUnwrapped=trackWrapped(backward);
for(let i=1;i<forward.length;i++) assert.ok(forwardUnwrapped[i]>=forwardUnwrapped[i-1]);
for(let i=1;i<backward.length;i++) assert.ok(backwardUnwrapped[i]<=backwardUnwrapped[i-1]);
near(forwardUnwrapped[3],361,"Continuity across 359->1");
near(backwardUnwrapped[2],-5,"Continuity across 3->355 reverse");
for(const degrees of [...forwardUnwrapped,...backwardUnwrapped]) {
 near(dist(b,degrees),degrees*Math.PI/180*expectedR,"Original surface distance");
 assert.ok(Number.isFinite(positive(dist(b,degrees),b.texture_repeat)));
}

// A physical belt section is sliced exactly at UV tile boundaries.
// No physical section maps one whole 16x16 texture across a long run.
function clippedTiles(start,length,repeat) {
 let cursor=0, tiles=[];
 while(cursor<length-1e-6) {
   let phase=positive(start+cursor,repeat);
   if(repeat-phase<1e-6)phase=0;
   let step=Math.min(length-cursor,repeat-phase);
   assert.ok(step>0,"UV splitter stopped progressing");
   tiles.push({length:step,u0:phase/repeat,u1:(phase+step)/repeat});
   cursor+=step;
   assert.ok(tiles.length<512,"UV slice count runaway");
 }
 near(tiles.reduce((total,t)=>total+t.length,0),length,"Tile length conservation");
 for(const tile of tiles) {
   assert.ok(tile.u0>=-1e-8 && tile.u0<=1+1e-8,"tile U0 outside [0,1]");
   assert.ok(tile.u1>=-1e-8 && tile.u1<=1+1e-8,"tile U1 outside [0,1]");
   assert.ok(tile.u1>=tile.u0,"UV must advance along each physical strip");
   near((tile.u1-tile.u0)*repeat,tile.length,"Physical UV density");
 }
 return tiles.length;
}
let samples=0,tiles=0;
const lengths=[0.024,0.5,1.90625,3.75,5.91125];
for(let angle=-720;angle<=720;angle+=3) {
 for(const facing of [0,90,180,-90]) {
   const degrees=angle+facing;
   const surface=dist(b,degrees);
   const surfaceTest=dist(test,degrees);
   near(surfaceTest, -surface+test.texture_offset,
       "Pack reverse travel obeys texture_offset");
   const vWidth=b.half_width/b.texture_repeat;
   const vThickness=b.half_thickness/b.texture_repeat;
   assert.ok(vWidth>0&&vWidth<=0.5&&vThickness>0&&vThickness<=0.5);
   for(const length of lengths)tiles+=clippedTiles(surface,length,b.texture_repeat);
   samples++;
 }
}
assert.equal(samples,481*4);
assert.ok(tiles>samples,"Long belt sections must be split into tiled parts");
console.log(`Belt-loop fixture: ${samples} angle/facing samples, ${tiles} `+
    "tile segments; wrap/reverse, default geometry, exact UV density, " +
    "legacy pack fallback, and blue/reversed resource-pack override passed.");
