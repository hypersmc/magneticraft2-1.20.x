#!/usr/bin/env node
// Self-contained Voxel V2 geometry contract tests. Run from repository root:
// node tools/multiblock-models/test-voxels.mjs
import fs from "node:fs";
import path from "node:path";
import assert from "node:assert/strict";

const root=process.cwd(),dirs=["south","west","north","east"];
const design=JSON.parse(fs.readFileSync(path.join(root,
  "tools/multiblock-models/designs/bronze_foundry.json"),"utf8"));
const profile=JSON.parse(fs.readFileSync(path.join(root,
  "src/main/resources/data/magneticraft2/multiblock_voxels/bronze_foundry.json"),"utf8"));
assert.equal(profile.format,1);
assert.equal(profile.unit,"model_pixels");
for(const key of ["collision","selection"])assert.deepEqual(profile[key],design.voxels[key]);
assert.deepEqual(profile.zones,design.voxels.zones);
assert(profile.collision.length>0&&profile.collision.length<=512);
assert(profile.selection.length>0&&profile.selection.length<=512);

function rotatePoint(point,direction){
 const [x,y,z]=point;
 switch(direction){
  case "south":return [x,y,z];
  case "west":return [16-z,y,x];
  case "north":return [16-x,y,16-z];
  case "east":return [z,y,16-x];
  default:throw Error("Not a horizontal direction");
 }
}
function rotateBox(box,dir){
 const [x0,y0,z0,x1,y1,z1]=box;
 const points=[[x0,y0,z0],[x0,y0,z1],[x1,y0,z0],[x1,y0,z1]]
   .map(p=>rotatePoint(p,dir));
 return [Math.min(...points.map(p=>p[0])),y0,Math.min(...points.map(p=>p[2])),
         Math.max(...points.map(p=>p[0])),y1,Math.max(...points.map(p=>p[2]))];
}
function contains(box,point){
 return point.every((p,i)=>p>box[i]&&p<box[i+3]);
}
function occupied(boxes,point){return boxes.some(b=>contains(b,point));}
function overlap(a,b){
 return [0,1,2].every(i=>Math.max(a[i],b[i])<Math.min(a[i+3],b[i+3]));
}
function relativeCells(blueprint){
 const {structure}=blueprint,[w,h,d]=structure.dimensions,controller=[],solid=[];
 for(let y=0;y<h;y++)for(let z=0;z<d;z++)for(let x=0;x<w;x++){
   const k=structure.layout["layer"+(y+1)][z][x],b=structure.blocks[k];
   if(b==="magneticraft2:bronze_foundry")controller.push([x,y,z]);
   if(b!=="minecraft:air")solid.push([x,y,z]);
 }
 assert.equal(controller.length,1);
 return {controller:controller[0],solid,dimensions:structure.dimensions};
}
const emptyCrucible=[8,25,-8],emptyFirebox=[8,8,14],tapSupport=[8,23,21];
const selectableHandle=[14.5,29,12.5];
assert(!occupied(profile.collision,emptyCrucible),"Crucible center cannot be solid");
assert(!occupied(profile.collision,emptyFirebox),"Firebox opening cannot be solid");
assert(occupied(profile.collision,tapSupport),"Tap nose must have real collision");
assert(!occupied(profile.collision,selectableHandle),"Valve handle is selection-only");
assert(occupied(profile.selection,selectableHandle),"Valve handle must be selectable");
assert(profile.zones.tap.some(b=>contains(b,tapSupport)));
assert(profile.zones.crucible.some(b=>contains(b,emptyCrucible)));
assert(profile.zones.firebox.some(b=>contains(b,emptyFirebox)));

const summary={};
for(const dir of dirs){
 const blueprint=JSON.parse(fs.readFileSync(path.join(root,
   "src/main/resources/data/magneticraft2/multiblocks/bronze_foundry_"+dir+".json"),"utf8"));
 assert.equal(blueprint.settings.voxel_profile,"magneticraft2:bronze_foundry");
 const geometry=relativeCells(blueprint);
 const [cx,cy,cz]=geometry.controller;
 const anchors=blueprint.settings.render_air_anchors;
 assert.equal(anchors.length,2);
 const anchorKeys=new Set();
 for(const anchor of anchors){
  assert(anchor.every(Number.isInteger));
  const [dx,dy,dz]=anchor, x=cx+dx,y=cy+dy,z=cz+dz;
  assert(x>=0&&x<3&&y>=0&&y<2&&z>=0&&z<3);
  assert.equal(blueprint.structure.blocks[
    blueprint.structure.layout["layer"+(y+1)][z][x]],"minecraft:air");
  assert(!anchorKeys.has(anchor.join(",")));
  anchorKeys.add(anchor.join(","));
 }
 const rotating=profile.collision.map(b=>rotateBox(b,dir));
 const selected=profile.selection.map(b=>rotateBox(b,dir));
 assert(!occupied(rotating,rotatePoint(emptyCrucible,dir)));
 assert(!occupied(rotating,rotatePoint(emptyFirebox,dir)));
 assert(occupied(rotating,rotatePoint(tapSupport,dir)));
 assert(!occupied(rotating,rotatePoint(selectableHandle,dir)));
 assert(occupied(selected,rotatePoint(selectableHandle,dir)));

 // The air proxy must catch the physical metal after the machine forms.
 for(const anchor of anchors){
  const [x,y,z]=anchor;
  const clip=[(x-.5)*16,y*16,(z-.5)*16,(x+1.5)*16,(y+1)*16,(z+1.5)*16];
  assert(rotating.some(b=>overlap(b,clip)),"Empty V2 air anchor: "+dir+":"+anchor);
 }
 const partFiles=fs.readdirSync(path.join(root,
   "src/main/resources/assets/magneticraft2/models/multiblock/bronze_foundry_"+dir))
   .filter(f=>/^part_-?\d+_-?\d+_-?\d+\.json$/.test(f));
 assert.equal(partFiles.length,18);
 summary[dir]={renderParts:partFiles.length,airAnchors:anchors.length,
   collisionBoxes:rotating.length,selectionBoxes:selected.length};
}
// A virtual 9x5x9 industrial machine. One canonical full-volume physical
// shape must intersect every occupied cell, without requiring any look
// direction, per-frame calculations, or a camera-dependent selection mesh.
const wide=[-64,0,-64,80,80,80];let checked=0;
for(let y=0;y<5;y++)for(let z=-4;z<=4;z++)for(let x=-4;x<=4;x++){
 const cell=[x*16,y*16,z*16,(x+1)*16,(y+1)*16,(z+1)*16];
 assert(overlap(wide,cell));checked++;
}
assert.equal(checked,405);
console.log("Voxel V2 passed:",JSON.stringify({foundry:summary,virtualLargeCells:checked}));
