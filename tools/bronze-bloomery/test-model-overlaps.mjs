#!/usr/bin/env node
// Static Minecraft block-model mesh sanity check for Bloomery V2.
// This is not a replacement for Forge compilation or in-world visual QA.
// Run from the repository root with Node 18+: node tools/bronze-bloomery/test-model-overlaps.mjs
import fs from "node:fs";
import path from "node:path";
import assert from "node:assert/strict";

const prefix = "src/main/resources/assets/magneticraft2/models/multiblock";
const facings = ["south", "west", "north", "east"];
const parts = [
  "foundation", "hearth", "tuyere",
  "lower_corner_left", "lower_corner_right",
  "lower_jamb_left", "lower_jamb_right", "lower_wall",
  "upper_corner_left", "upper_corner_right",
  "upper_jamb_left", "upper_jamb_right", "upper_wall", "upper_arch",
  "shoulder", "shoulder_front", "chimney", "chimney_front",
  "frame_post", "frame_corner", "frame_beam_x", "frame_beam_z",
  "port_input", "port_primary", "port_byproduct"
];

function load(file) {
  return JSON.parse(fs.readFileSync(file, "utf8"));
}
function volumeOverlap(a, b) {
  return Math.max(0, Math.min(a.to[0], b.to[0]) - Math.max(a.from[0], b.from[0]))
       * Math.max(0, Math.min(a.to[1], b.to[1]) - Math.max(a.from[1], b.from[1]))
       * Math.max(0, Math.min(a.to[2], b.to[2]) - Math.max(a.from[2], b.from[2]));
}
function rectArea(a, b) {
  return Math.max(0, Math.min(a[2], b[2]) - Math.max(a[0], b[0]))
       * Math.max(0, Math.min(a[3], b[3]) - Math.max(a[1], b[1]));
}
function surface(element, direction) {
  const f=element.from, t=element.to;
  switch(direction) {
    case "north": return [f[2],f[0],f[1],t[0],t[1]];
    case "south": return [t[2],f[0],f[1],t[0],t[1]];
    case "west": return [f[0],f[2],f[1],t[2],t[1]];
    case "east": return [t[0],f[2],f[1],t[2],t[1]];
    case "down": return [f[1],f[0],f[2],t[0],t[2]];
    case "up": return [t[1],f[0],f[2],t[0],t[2]];
    default: throw Error("bad direction " + direction);
  }
}

let modelCount=0, elementCount=0;
for(const facing of facings) {
  for(const name of ["",...parts]) {
    const file=path.join(prefix,name ? "bloomery_furnace_"+facing+"/"+name+".json"
                                     : "bloomery_furnace_"+facing+".json");
    const model=load(file);
    assert.ok(model.elements.length>0, "No geometry: "+file);
    assert.ok(model.elements.length<=256, "Too many elements: "+file);
    modelCount++;
    elementCount += model.elements.length;
    for(const [index,el] of model.elements.entries()) {
      assert.ok(el.from.every((x,i)=>Number.isFinite(x)&&Number.isFinite(el.to[i])
              && el.to[i]>x), "Invalid box "+file+" element "+index);
      assert.deepEqual(Object.keys(el.faces).sort(),
              ["down","east","north","south","up","west"],"Face list mismatch "+file);
      for(const [dir,face] of Object.entries(el.faces)) {
        const p=surface(el,dir);
        const expected=[p[3]-p[1],p[4]-p[2]];
        const uv=[face.uv[2]-face.uv[0],face.uv[3]-face.uv[1]];
        assert.deepEqual(uv,expected,
                "UV stretched or compressed: "+file+" element "+index+" "+dir);
        assert.ok(face.uv.every(Number.isFinite),"Invalid UV "+file);
      }
      for(let j=index+1;j<model.elements.length;j++) {
        const b=model.elements[j];
        assert.equal(volumeOverlap(el,b),0,
          "Overlapping model volumes (z-fighting risk): "+file+" "+index+"/"+j);
        for(const dir of ["north","south","east","west","up","down"]) {
          if(!el.faces[dir]||!b.faces[dir])continue;
          const aFace=surface(el,dir), bFace=surface(b,dir);
          if(aFace[0]!==bFace[0])continue;
          assert.equal(rectArea(aFace.slice(1),bFace.slice(1)),0,
            "Co-facing coplanar quads: "+file+" "+index+"/"+j+" "+dir);
        }
      }
    }
  }
}
// Physical facade relief stays within the block: no arbitrary epsilon or
// out-of-block offset, and no collision profile migration required.
for(const name of ["lower_corner_left","lower_jamb_left",
                    "upper_corner_left","upper_jamb_left","upper_arch"]) {
  const m=load(path.join(prefix,"bloomery_furnace_south/"+name+".json"));
  assert.ok(m.elements.some(e=>e.to[2]===14),"Expected inset masonry: "+name);
  assert.ok(m.elements.some(e=>e.to[2]===16),"Expected attached raised trim: "+name);
}
console.log("Bloomery models: PASS",modelCount,"models;",elementCount,
            "disjoint elements, no coplanar front faces, 1:1 UV scale.");
console.log("Build/runtime and camera-angle visual inspection remain REQUIRED.");
