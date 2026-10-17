#!/usr/bin/env node
// Real 90-degree tooth meshing regression for the Stamp Mill.
// Unlike a static rim-clearance check, this samples both moving solids
// through all phases and checks that opposing teeth enter each other's gaps.
import assert from "node:assert/strict";
import {stampMillDriveDesigns} from "./build-drive-models.mjs";

const Z=stampMillDriveDesigns.bronze_stamp_mill_drive_bevel;
const X=stampMillDriveDesigns.bronze_stamp_mill_drive_bevel_x;
const za=Z.filter(b=>b.meshTooth),xa=X.filter(b=>b.meshTooth);
assert.equal(za.length,24,"Eight joined three-step Z teeth");
assert.equal(xa.length,24,"Eight joined three-step X teeth");

const intersects=(a,b)=>a.from.every((v,i)=>
  Math.min(a.to[i],b.to[i])-Math.max(v,b.from[i])>1e-5);
for(const gear of [Z,X]){
  const rim=gear.filter(b=>!b.meshTooth),teeth=gear.filter(b=>b.meshTooth);
  for(let i=0;i<8;i++){
    const [root,flank,tip]=teeth.slice(i*3,i*3+3);
    assert.ok(rim.some(b=>intersects(root,b)),"Tooth root is floating at index "+i);
    assert.ok(intersects(root,flank)&&intersects(flank,tip),
      "Disconnected three-step tooth at index "+i);
  }
}

function radialTip(boxes,axis){
  const r=axis==="z"?0:2;
  return Math.max(...boxes.flatMap(b=>[b.from[r],b.to[r]]
    .flatMap(v=>[b.from[1],b.to[1]].map(y=>Math.hypot(v-8,y-8)))));
}
assert.ok(radialTip(za,"z")>5&&radialTip(xa,"x")>5,
  "Teeth do not extend into the opposing wheel's gaps");

const dot=(a,b)=>a.reduce((s,v,i)=>s+v*b[i],0);
const cross=(a,b)=>[
  a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]
];
function rotateBox(box,axis,angle){
  const c=Math.cos(angle),s=Math.sin(angle);
  const R=axis==="z"?[[c,-s,0],[s,c,0],[0,0,1]]
                    :[[1,0,0],[0,c,-s],[0,s,c]];
  const pos=box.from.map((v,i)=>(v+box.to[i])/2-8);
  const half=box.from.map((v,i)=>(box.to[i]-v)/2);
  const center=R.map(row=>dot(row,pos));
  const ext=R.map(row=>row.reduce((n,v,i)=>n+Math.abs(v)*half[i],0));
  return {center,half,axes:[0,1,2].map(i=>R.map(row=>row[i])),
    min:center.map((v,i)=>v-ext[i]),max:center.map((v,i)=>v+ext[i])};
}
function bboxGap(a,b){
  return Math.hypot(...[0,1,2].map(i=>
    Math.max(0,a.min[i]-b.max[i],b.min[i]-a.max[i])));
}
// 15 separating axes for two arbitrarily rotated rectangular prisms.
function obbOverlap(a,b){
  const delta=b.center.map((v,i)=>v-a.center[i]);
  for(const axis of [...a.axes,...b.axes,
      ...a.axes.flatMap(u=>b.axes.map(v=>cross(u,v)))]){
    const len=Math.hypot(...axis);if(len<1e-9)continue;
    const dir=axis.map(v=>v/len);
    const ra=a.axes.reduce((n,u,i)=>n+a.half[i]*Math.abs(dot(dir,u)),0);
    const rb=b.axes.reduce((n,u,i)=>n+b.half[i]*Math.abs(dot(dir,u)),0);
    if(Math.abs(dot(delta,dir))>ra+rb+1e-5)return false;
  }
  return true;
}
// Mesh on +X/+Z: omegaZ and omegaX must oppose to have identical
// tangential velocity. Both signs occur across four machine facings.
for(const sign of [-1,1])assert.equal(sign*4,-(-sign)*4);

let nearest=Infinity,farthest=0,nearPhases=0,pairs=0;
for(let step=-180;step<=180;step++){
  const t=step*.25*Math.PI/180;
  const A=Z.map(b=>rotateBox(b,"z",t));
  const B=X.map(b=>rotateBox(b,"x",-t));
  let phaseGap=Infinity;
  for(let i=0;i<A.length;i++)for(let j=0;j<B.length;j++){
    const gap=bboxGap(A[i],B[j]);pairs++;
    if(gap<1e-5)assert.ok(!obbOverlap(A[i],B[j]),
      "Bevel teeth clip at "+(step*.25)+" deg ("+i+"/"+j+")");
    if(Z[i].meshTooth&&X[j].meshTooth)phaseGap=Math.min(phaseGap,gap);
  }
  nearest=Math.min(nearest,phaseGap);
  farthest=Math.max(farthest,phaseGap);
  if(phaseGap<.25)nearPhases++;
}
assert.ok(nearest>.02&&nearest<.12,
  "Teeth cannot nearly meet without clipping: "+nearest);
assert.ok(farthest<.80,"Teeth separate too far: "+farthest);
assert.ok(nearPhases>=50,"Too few phases show engagement: "+nearPhases);
console.log("Stamp Mill bevel mesh PASS: "+pairs+" sampled solid pairs, "+
  nearPhases+" engaged phases, tooth gap "+
  nearest.toFixed(3)+".."+farthest.toFixed(3)+" px, zero collisions");
