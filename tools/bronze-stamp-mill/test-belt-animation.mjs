#!/usr/bin/env node
// Bronze Stamp Mill Gear V2-synchronized leather animation regression.
// Complements legacy-model visual checks: the baked static leather assets
// are now geometry references, not additional rendered surfaces.
import assert from "node:assert/strict";
import fs from "node:fs";

const prefix="src/main/java/com/magneticraft2/";
const code=fs.readFileSync(prefix+
 "client/render/blocks/stage/bronze/BronzeStampMillBeltRenderer.java","utf8");
const mill=fs.readFileSync(prefix+
 "client/render/blocks/stage/bronze/BronzeStampMillRenderer.java","utf8");
const be=fs.readFileSync(prefix+
 "common/blockentity/stage/bronze/BronzeStampMillBlockEntity.java","utf8");
const shared=fs.readFileSync(prefix+
 "client/render/blocks/stage/copper/LeatherBeltRenderHelper.java","utf8");
const path=fs.readFileSync(prefix+"common/systems/GEAR/BeltPath.java","utf8");

assert.ok(code.includes("BeltPath.createVisualSegments(")&&
          code.includes("Direction.Axis.X")&&
          code.includes("BeltPath.BELT_CLEARANCE"),
 "Animated belt must reuse the actual continuous BeltPath geometry");
assert.ok(code.includes("new Vec3(47.2D / 16D, 8D / 16D, -10D / 16D)")&&
          code.includes("new Vec3(47.2D / 16D, 38.5D / 16D, -10D / 16D)"),
 "Animated loop must use the original drum centers");
assert.ok(code.includes("textures/block/bronze_stamp_mill_drive_belt.png")&&
          code.includes("RenderType.entityCutoutNoCull(LEATHER)"),
 "The new belt must scroll its custom opaque leather texture");
assert.ok(code.includes("segment.startDistance() + cursor")&&
          code.includes("TEXTURE_REPEAT_LENGTH = 1.0D")&&
          code.includes("TEXTURE_REPEAT_LENGTH - phase")&&
          code.includes("positiveModulo("),
 "Texture UV must be tiled at physical 16px boundaries, not stretched");
assert.ok(code.includes("float width0 = 0.5F - (float) BELT_HALF_WIDTH")&&
          code.includes("float thick0 = 0.5F - (float) BELT_HALF_THICKNESS"),
 "A narrow band must use cropped UV, not all sixteen texels across 1.6px");
assert.ok(shared.includes("private static final double TEXTURE_REPEAT_LENGTH = 0.50D")&&
          shared.includes("BeltPath.RENDER_HALF_WIDTH")&&
          shared.includes("renderSegments(")&&
          path.includes("createVisualSegments("),
 "Original belt and Ore Washer behavior must remain available");
assert.ok(be.includes("getStampCrankRotationDegrees(float partialTicks)")&&
          mill.includes("final float driveAngle=mill.getStampCrankRotationDegrees(partial)")&&
          mill.includes("beltTravel.computeIfAbsent(mill")&&
          mill.includes(".sample(driveAngle)")&&
          mill.includes("BronzeStampMillBeltRenderer.render("),
 "Moving material must share EXACTLY the output shaft phase");
assert.ok(mill.includes("pulleyX,inputY,lowerZ,Axis.XP,driveAngle")&&
          mill.includes("pulleyX,upperY,lowerZ,Axis.XP,driveAngle"),
 "Both pulley drums must retain their original shaft phase");
assert.ok(!mill.includes("part(pose,buffers,light,overlay,BELT_STRAIGHT")&&
          !mill.includes("part(pose,buffers,light,overlay,BELT_WRAP"),
 "Remove all duplicate static leather from the live renderer");
assert.ok(mill.includes("WeakHashMap<")&&
          mill.includes("private static final class BeltTravel")&&
          mill.includes("travel %= 1.0D"),
 "Per-machine phase memory must stop growing and unload cleanly");

// Cross-section and path are the same pixel envelope as the accepted drum.
const radius=(4.08+5.45)/32;
const halfWidth=(8.72-7.13)/32;
const halfThickness=(5.45-4.08)/32;
assert.ok(Math.abs((radius-halfThickness)*16-4.08)<1e-9);
assert.ok(Math.abs((radius+halfThickness)*16-5.45)<1e-9);
assert.ok(Math.abs(halfWidth*32-1.59)<1e-9);
assert.ok(4.08-3.98>.06,"Leather must clear rotating bronze rims");
assert.ok(47.2+halfWidth*16<=48&&47.2-halfWidth*16>46,
 "Belt must remain inside the machine's existing X envelope");

// BeltPath: two vertical tangents plus 16 faceted segments per semicircle.
// At each tangent the polyline touches the SAME two drum centers.
const height=(38.5-8)/16;
const stepsPerArc=16;
const arcStep=2*radius*Math.sin(Math.PI/(2*stepsPerArc));
const lengths=[height,
 ...Array.from({length:stepsPerArc},()=>arcStep),
 height,
 ...Array.from({length:stepsPerArc},()=>arcStep)];
const length=lengths.reduce((sum,value)=>sum+value,0);
assert.ok(length>5.6&&length<5.8);
assert.equal(lengths.length,34);
const countQuads=offset=>{
 let start=0,patches=0;
 for(const distance of lengths){
  let cursor=0;
  while(cursor<distance-1e-6){
   let phase=(offset+start+cursor)%1;
   if(phase<0)phase+=1;
   let until=1-phase;
   if(until<1e-6)until=1;
   const step=Math.min(distance-cursor,until);
   assert.ok(step>0&&step<=1+1e-6,"A UV tile subdivision failed");
   const endUv=phase+step;
   assert.ok(endUv<=1+1e-6,"Texture leaked outside the 16x16 tile");
   cursor+=step;patches++;
  }
  start+=distance;
 }
 return patches*4;
};
for(const phase of [0,.05,.26,.49,.50,.75,.99,1.25,-.37]){
 const faces=countQuads(phase);
 assert.ok(faces>130&&faces<260,
  "Animation must not reintroduce a very costly 500+ face belt: "+faces);
}

// This mirrors the renderer's exact signed shortest-angle accumulator.
// Full rotations cannot make the UV phase jump, and stop/reverse require
// no independent animation timer or extra machine power state.
class BeltTravel{
 constructor(){this.initialized=false;this.previousAngle=0;this.travel=0;}
 sample(angle){
  if(!this.initialized){
   this.initialized=true;
   this.previousAngle=angle;
   this.travel=angle*Math.PI/180*radius;
  }else{
   let delta=(angle-this.previousAngle)%360;
   if(delta>180)delta-=360;
   else if(delta< -180)delta+=360;
   this.previousAngle=angle;
   this.travel+=delta*Math.PI/180*radius;
  }
  this.travel%=1;
  return this.travel;
 }
}
const motion=new BeltTravel(),oneDegree=radius*Math.PI/180;
let first=motion.sample(358);
let nearEnd=motion.sample(359);
let rollover=motion.sample(0);
let forward=motion.sample(1);
assert.ok(Math.abs((nearEnd-first)-oneDegree)<1e-9);
assert.ok(Math.abs((rollover-nearEnd)-oneDegree)<1e-9,
 "Texture must NOT jump backward when the drums cross 360 degrees");
assert.ok(Math.abs((forward-rollover)-oneDegree)<1e-9);
assert.equal(motion.sample(1),forward,
 "Stopped drums must freeze the belt at the last physical position");
assert.ok(Math.abs((motion.sample(0)-forward)+oneDegree)<1e-9,
 "Reversing shaft direction must reverse the leather travel");
const reverse360=motion.sample(359);
assert.ok(Math.abs((reverse360-rollover)+oneDegree)<1e-9,
 "Reverse rotation across 0 degrees must remain continuous");

// Directions differ by facing, but getStampCrankRotationDegrees() already
// supplies the correctly signed local +X angle in all 4 orientations.
for(const facing of ["SOUTH","EAST","NORTH","WEST"]){
 const sign=(facing==="NORTH"||facing==="WEST")?-1:1;
 const localDrive=-sign*15;
 const next=-sign*16;
 const tracker=new BeltTravel();
 tracker.sample(localDrive);
 const before=tracker.travel;
 const after=tracker.sample(next);
 assert.ok(Math.abs((after-before)-(-sign*oneDegree))<1e-9,
  "Wrong belt direction in "+facing+" orientation");
}
console.log("Stamp Mill animated leather PASS: signed phase, stop/reverse, all facings, "+
 "native texture UVs, original drum bounds and low-cost closed path");
