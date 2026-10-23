#!/usr/bin/env node
/** Water Settling Tank structural and process regression, Node 18+ no npm deps. */
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { execFileSync } from "node:child_process";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const read = p => fs.readFileSync(path.join(root, p), "utf8");
const json = p => JSON.parse(read(p));
const assets = "src/main/resources/assets/magneticraft2/";
const data = "src/main/resources/data/magneticraft2/";
const source = "src/main/java/com/magneticraft2/";
const id = "water_settling_tank";
const rows = [
  ["SSSSS","SSSSS","SSSSS","SSSSS","SSCSS"],
  ["SSISS","SAAAS","SAAAS","SAAAO","SPSSS"]
];
const parts = [];
for (let y=0;y<2;y++) for(let zi=0;zi<5;zi++) for(let xi=0;xi<5;xi++)
  parts.push({x:xi-2,y,z:zi-4,k:rows[y][zi][xi]});
const where = k => parts.filter(p=>p.k===k);
assert.equal(where("C").length, 1);
assert.equal(where("I").length, 1);
assert.equal(where("P").length, 1);
assert.equal(where("O").length, 1);
assert.deepEqual(where("C").map(p=>[p.x,p.y,p.z]),[[0,0,0]]);
assert.deepEqual(where("I").map(p=>[p.x,p.y,p.z]),[[0,1,-4]]);
assert.deepEqual(where("P").map(p=>[p.x,p.y,p.z]),[[-1,1,0]]);
assert.deepEqual(where("O").map(p=>[p.x,p.y,p.z]),[[2,1,-1]]);
assert.equal(where("S").length,37,
 "Basin foundation must support all three raised port modules");
assert.ok(parts.every(c=>c.y<=1),"Upper frame must be entirely removed");
const nonModule = p=>!"IPO".includes(p.k);
const rot=(x,z,f)=>f==="south"?[x,z]:f==="west"?[-z,x]:f==="north"?[-x,-z]:[z,-x];
// The inlet is a REAL port with its own tank-only overlay model. Its
// neighboring AIR cell is empty; only the existing six weir/lip hosts
// are placed as Voxel V2 fillers during formation.
const anchors=[
 [-1,1,-2],[0,1,-2],[1,1,-2],
 [-1,1,-1],[0,1,-1],[1,1,-1]
];
const sdir=["south","west","north","east"];
const expectedIds={
 C:"magneticraft2:water_settling_tank",
 I:"magneticraft2:multiblock_fluid_input",
 P:"magneticraft2:multiblock_item_primary_output",
 O:"magneticraft2:multiblock_fluid_output",
 S:"minecraft:stone_bricks",A:"minecraft:air"
};
const modelPaths=new Set();
for (const f of sdir) {
 const bp=json(data+"multiblocks/"+id+"_"+f+".json");
 assert.equal(bp.name,id+"_"+f);
 assert.deepEqual(bp.structure.dimensions,[5,2,5]);
 assert.deepEqual(bp.structure.blocks,expectedIds);
 assert.equal(bp.settings.voxel_profile,"magneticraft2:"+id);
 assert.equal(bp.settings.inventory,true);
 assert.equal(bp.settings.fluid,true);
 assert.equal(bp.settings.power,false);
 assert.equal(bp.settings.fuel,false);
 assert.deepEqual(bp.settings.render_air_anchors.map(a=>a.join(",")).sort(),
  anchors.map(([x,y,z])=>{const [rx,rz]=rot(x,z,f);return [rx,y,rz].join(",")}).sort(),
  f+": baffle and spill lip must anchor ONLY valid AIR cells");
 const rotated=parts.map(c=>({...c,rot:rot(c.x,c.z,f)}));
 const minX=Math.min(...rotated.map(c=>c.rot[0]));
 const minZ=Math.min(...rotated.map(c=>c.rot[1]));
 const covered=new Set();
 for(const cell of rotated) {
  const [x,z]=cell.rot;
  assert.equal(bp.structure.layout["layer"+(cell.y+1)][z-minZ][x-minX],cell.k,
    f+": wrong blueprint cell "+[x,cell.y,z]);
  const key=[x,cell.y,z].join(",");
  assert.ok(!covered.has(key),f+": duplicate rotated cell "+key);
  covered.add(key);
  if(!nonModule(cell) || cell.k==="A" &&
      !anchors.some(a=>a.join(",")===[cell.x,cell.y,cell.z].join(",")))
   continue;
  const rel="models/multiblock/"+id+"_"+f+"/part_"+x+"_"+cell.y+"_"+z+".json";
  modelPaths.add(rel);
  const model=json(assets+rel);
  assert.equal(model.parent,"minecraft:block/block");
  assert.equal(model.ambientocclusion,false);
  assert.ok(Array.isArray(model.elements) && model.elements.length>0,
    f+": real model host missing geometry "+rel);
  for (const el of model.elements) {
   assert.ok(el.from.every((v,i)=>Number.isFinite(v) && v>=0 &&
           Number.isFinite(el.to[i]) && el.to[i]<=16 && v<el.to[i]),
       "Voxel filler models must stay block-local: "+rel);
   for(const [side,view] of Object.entries(el.faces)) {
    const spanU=side==="east"||side==="west"
      ?el.to[2]-el.from[2]:el.to[0]-el.from[0];
    const spanV=side==="up"||side==="down"
      ?el.to[2]-el.from[2]:el.to[1]-el.from[1];
    assert.ok(Math.abs(view.uv[2]-view.uv[0]-spanU)<0.001 &&
       Math.abs(view.uv[3]-view.uv[1]-spanV)<0.001,
      "Texture UV stretched or compressed "+rel+" "+side);
   }
  }
 }
}
assert.equal(modelPaths.size,176,
 "Four facings each need 44 V2 model parts; port inserts aren't fillers");
// All three REAL service modules now rest on a full 16-pixel
// load-bearing lower block. This must hold for every facing and must NOT
// be simulated by a detached cosmetic downpipe in an upper wall.
for(const c of [where("I")[0],where("P")[0],where("O")[0]]){
 assert.equal(c.y,1,"The service port must sit in the upper perimeter wall");
 const lower=json(assets+"models/multiblock/"+id+"_south/part_"+
            c.x+"_0_"+c.z+".json");
 assert.ok(lower.elements.some(e=>e.from[1]===0 && e.to[1]===14
                  && e.faces.up.texture==="#foundation"),
       "Real port needs a solid refractory base: "+c.k);
 assert.ok(lower.elements.some(e=>e.from[1]===14 && e.to[1]===16
                  && e.faces.up.texture==="#bronze_joint"),
       "Real port needs a flush bronze bearing plate: "+c.k);
 const missing=assets+"models/multiblock/"+id+"_south/part_"+
                   c.x+"_1_"+c.z+".json";
 assert.ok(!fs.existsSync(path.join(root,missing)),
       "The genuine module must not be overdrawn by a Voxel V2 filler model: "+c.k);
}
// Shared fluid ports auto-orient by greatest controller-relative axis.
// The clean port must be on the outside east edge; x=1,z=0 would point
// EAST INTO THE CORNER WALL and could not accept external pipe access.
const exposedClean=where("O")[0];
assert.ok(exposedClean.x===2 && exposedClean.z===-1,
 "The real clean Fluid Output must face exposed exterior on EAST edge");
const autoFacing=(v)=>{
 const {x,y,z}=v;
 if(Math.abs(x)>=Math.abs(z)&&Math.abs(x)>=Math.abs(y)&&x!==0)
  return x>0?"EAST":"WEST";
 if(Math.abs(z)>=Math.abs(y)&&z!==0)
  return z>0?"SOUTH":"NORTH";
 return y>0?"UP":"DOWN";
};
assert.equal(autoFacing(where("I")[0]),"NORTH",
 "The shared rear Fluid Input must automatically face the exposed north side");
assert.equal(autoFacing(exposedClean),"EAST",
 "The shared clean Fluid Output must automatically face the exposed east side");
// The actual REAL port block geometry must remain unchanged. Two purpose-
// baked static inserts fill the left/right margins INSIDE those very cells;
// nothing is placed in the AIR block in front of the inlet.
const portInsertNames=["input","output"];
const portInner=[
 ["input","north",where("I")[0],"multiblock_fluid_input"],
 ["output","east",where("O")[0],"multiblock_fluid_output"]
];
const orientNorth=(x,z,f)=>f==="north"?[x,z]:[16-z,x];
const rotateBox=(e,f)=>{
 const points=[[e.from[0],e.from[2]],[e.from[0],e.to[2]],
               [e.to[0],e.from[2]],[e.to[0],e.to[2]]]
               .map(([x,z])=>orientNorth(x,z,f));
 return [Math.min(...points.map(p=>p[0])),e.from[1],
         Math.min(...points.map(p=>p[1])),
         Math.max(...points.map(p=>p[0])),e.to[1],
         Math.max(...points.map(p=>p[1]))];
};
const intersects=(a,b)=>[0,1,2].every(i=>
 a[i]<b[i+3]-1e-6 && a[i+3]>b[i]+1e-6);
for(const [kind,facing,cell,moduleId] of portInner){
 const overlay=json(assets+"models/multiblock/"+id+"_"+kind+"_port_insert.json");
 const copper=json(assets+"models/block/"+moduleId+".json");
 assert.equal(overlay.elements.length,8,kind+" should have eight simple frame pieces");
 assert.equal(copper.elements.length,9,
   "Shared port model unexpectedly changed: "+moduleId);
 assert.equal(overlay.parent,"minecraft:block/block");
 assert.equal(overlay.ambientocclusion,false);
 const overlayBoxes=overlay.elements.map(e=>[...e.from,...e.to]);
 const copperBoxes=copper.elements.map(e=>rotateBox(e,facing));
 for(const b of overlayBoxes)for(const c of copperBoxes)
  assert.ok(!intersects(b,c),
   kind+" tank insert overlaps the real copper port geometry: "+b+" / "+c);
 // Each port has two material-consistent cheeks extending from the bottom
 // to the adjacent basin wall's bronze rim (Y=10..12).
 const left=overlayBoxes.some(b=>
   (facing==="north"?b[0]===0&&b[3]===2:b[2]===0&&b[5]===2)
   &&b[1]===0&&b[4]===10);
 const right=overlayBoxes.some(b=>
   (facing==="north"?b[0]===14&&b[3]===16:b[2]===14&&b[5]===16)
   &&b[1]===0&&b[4]===10);
 assert.ok(left&&right,kind+" must have two walls INSIDE the actual port block");
 // The upper header and lower sill occupy only the inboard half of
 // the port, leaving its outward pipe aperture entirely free.
 assert.ok(overlayBoxes.some(b=>b[1]===0&&b[4]===2 &&
   (facing==="north"?b[2]>=8:b[3]<=8)),
    kind+" bottom support fails to meet its existing copper frame");
 assert.ok(overlayBoxes.some(b=>b[1]===14&&b[4]===16 &&
   (facing==="north"?b[2]>=8:b[3]<=8)),
    kind+" top lintel fails to meet its existing copper frame");
 for(const el of overlay.elements){
  assert.ok(el.from.every((v,i)=>v>=0&&el.to[i]<=16&&v<el.to[i]),
       kind+" insert may not extend outside the real port block");
  for(const [direction,face] of Object.entries(el.faces)){
   const u=(direction==="east"||direction==="west") ?
     el.to[2]-el.from[2]:el.to[0]-el.from[0];
   const v=(direction==="up"||direction==="down") ?
     el.to[2]-el.from[2]:el.to[1]-el.from[1];
   assert.ok(face.uv[2]-face.uv[0]===u && face.uv[3]-face.uv[1]===v,
     kind+" insert stretches textures");
  }
 }
}
assert.ok(!fs.existsSync(path.join(root,assets+
 "models/multiblock/"+id+"_south/part_0_1_-3.json")),
 "Remove the previous AIR-cell throat: inlet walls belong INSIDE the real port");
assert.ok(!anchors.some(a=>a.join(",")==="0,1,-3"),
 "Do not create an air-filler to draw or collide inside the input");
const clientRegistrations=read(source+"client/Clientsetup.java");
const tankRenderer=read(source+
 "client/render/blocks/stage/bronze/WaterSettlingTankRenderer.java");
for(const kind of portInsertNames){
 assert.ok(clientRegistrations.includes(
    "multiblock/water_settling_tank_"+kind+"_port_insert"),
    kind+" overlay must be baked as an additional Forge model");
 assert.ok(tankRenderer.includes(
    "multiblock/water_settling_tank_"+kind+"_port_insert"),
    kind+" overlay must be drawn only by Settling Tank BER");
}
for(const at of ["INPUT_PORT_INSERT, 0D, 1D, -4D",
                 "OUTPUT_PORT_INSERT, 2D, 1D, -1D"])
 assert.ok(tankRenderer.includes(at),
 "Overlay must be rendered at the exact occupied REAL module position: "+at);
assert.ok(tankRenderer.includes("getModelRenderer().renderModel"),
 "Use pre-baked model quads rather than per-frame primitive generation");

// The original open rear corners were unconnected: the back wall stopped
// at Z=7/16 while the two side rails started one block farther along.
// Each corner must now have a true return to the side wall.
const cornerL=json(assets+"models/multiblock/"+id+"_south/part_-2_1_-4.json");
const cornerR=json(assets+"models/multiblock/"+id+"_south/part_2_1_-4.json");
assert.ok(cornerL.elements.some(e=>e.faces.up.texture==="#bronze_joint" &&
                        e.from[0]===9 && e.from[2]===7 && e.to[2]===16),
  "Left rear rim does not meet its side wall");
assert.ok(cornerR.elements.some(e=>e.faces.up.texture==="#bronze_joint" &&
                        e.to[0]===7 && e.from[2]===7 && e.to[2]===16),
  "Right rear rim does not meet its side wall");
for(const x of [-2,2]){
 const model=json(assets+"models/multiblock/"+id+"_south/part_"+x+"_1_0.json");
 assert.ok(model.elements.some(e=>e.faces.up.texture==="#bronze_joint" &&
                      e.from[2]===9 && e.to[2]===16),
      "Front corner rim does not return to front facade: "+x);
}
for(const facing of sdir){
 const dir=path.join(root,assets+"models/multiblock/"+id+"_"+facing);
 const models=fs.readdirSync(dir).filter(n=>/^part_-?\d+_-?\d+_-?\d+\.json$/.test(n));
 assert.equal(models.length,44,
   "Old third-layer posts/bridge models must be deleted: "+facing);
 assert.ok(models.every(file=>!/^part_-?\d+_2_/.test(file)),
   "No obsolete overhead geometry may remain after redesign: "+facing);
}
for(const f of sdir) {
 const state=json(assets+"blockstates/"+id+".json");
 assert.ok(state.variants["facing="+f+",is_formed=true"].model.endsWith(
  "/"+id+"_"+f+"/part_0_0_0"));
 const pageController=json(assets+"models/item/"+id+".json");
 assert.equal(pageController.parent,"magneticraft2:block/"+id+"_controller");
}
const vox=json(data+"multiblock_voxels/"+id+".json");
assert.equal(vox.format,1);
assert.equal(vox.unit,"model_pixels");
assert.ok(vox.collision.length>40 && vox.collision.length<=512);
const inside=(b,cell)=>[0,1,2].every(i=>b[i]<16*(cell[i]+1)-1e-4
  &&b[i+3]>16*cell[i]+1e-4);
// Real port cells continue to own their vanilla block collision and hit
// selection. No V2 collision geometry is permitted inside any REAL port.
for(const box of vox.collision)for(const type of ["I","P","O"]){
 const port=where(type)[0];
 assert.ok(!inside(box,[port.x,port.y,port.z]),
   "V2 filler collision cannot occupy a genuine "+type+" port cell");
}

assert.ok(vox.collision.some(b=>b[1]===0&&b[4]===16),
 "The visibly dry basin floor must be solid and continuous");
for(const point of [[-1,1,-2],[0,1,-2],[1,1,-2],[-1,1,-1],[0,1,-1],[1,1,-1]]){
 assert.ok(vox.collision.some(box=>inside(box,point)),
    "Missing low overflow weir or rear-of-port retaining lip: "+point);
}
assert.ok(vox.collision.every(box=>box[4] <= 32),
 "Tall posts, walkways or third-level collision geometry remain");

/**
 * V4.1 geometry regression: the previous renderer drew panels on the exact
 * top of the dry ceramic floor (and mutually overlapping inlet fittings).
 * Same-facing coplanar faces MUST NEVER share positive rendered area.
 * Touching walls/floor along opposing interior faces is perfectly valid.
 */
const intersections=[];
const coplanar=[];
for(let i=0;i<vox.collision.length;i++) {
 const a=vox.collision[i];
 for(let j=i+1;j<vox.collision.length;j++) {
  const b=vox.collision[j];
  const delta=[0,1,2].map(k=>Math.min(a[k+3],b[k+3])-Math.max(a[k],b[k]));
  if(delta.every(n=>n>0))
    intersections.push({i,j,volume:delta.reduce((x,y)=>x*y,1)});
  for(let axis=0;axis<3;axis++){
   const sides=[0,1,2].filter(k=>k!==axis);
   if(sides.every(k=>delta[k]>0) &&
       (a[axis]===b[axis] || a[axis+3]===b[axis+3]))
    coplanar.push({i,j,axis});
  }
 }
}
assert.deepEqual(intersections,[],
 "Overlapping cuboid volumes cause overlapping opaque faces and depth artifacts");
assert.deepEqual(coplanar,[],
 "Same-facing, same-plane surfaces are Z-fighting inside the generated model");

for(const dx of [-1,0,1])for(const dz of [-3,-2,-1]){
 const floor=json(assets+"models/multiblock/"+id+"_south/part_"+
        dx+"_0_"+dz+".json");
 assert.ok(floor.elements.some(el=>el.from[1]===0&&el.to[1]===16 &&
           el.faces.up.texture==="#basin_floor" &&
           el.faces.north.texture==="#foundation"),
   "Dry basin floor must have a sealed ceramic TOP on structural stone SIDES");
 assert.ok(!floor.elements.some(el=>el.faces.up.texture==="#sludge"),
   "Do not Z-fight with permanent floating sediment decals");
}
// Connected fluid footprints are checked from the actual renderer constants
// below, including concave pocket coverage, outlet approach and containment.


const recipe=json(data+"recipes/settling_dirty_water.json");
assert.equal(recipe.type,"magneticraft2:multiblock_processing");
assert.equal(recipe.machine,"magneticraft2:"+id);
assert.ok(!Object.hasOwn(recipe,"input"),"Passive fluid-only recipe must not invent item inputs");
assert.equal(recipe.fluid_input.fluid,"magneticraft2:dirty_water");
assert.equal(recipe.fluid_input.amount,600);
assert.equal(recipe.fluid_output.fluid,"minecraft:water");
assert.equal(recipe.fluid_output.amount,480);
assert.ok(recipe.fluid_output.amount<recipe.fluid_input.amount,
 "Water recovery must not form an infinite loop");
assert.deepEqual(recipe.output,{item:"magneticraft2:settled_sediment",count:1});
assert.equal(recipe.process_time,240);
assert.ok(!Object.hasOwn(recipe,"mechanical"),
 "Passive settling does not require an arbitrary gearbox");
const clay=json(data+"recipes/settled_sediment_to_clay.json");
assert.deepEqual(clay.pattern,["SS","SS"]);
assert.equal(clay.key.S.item,"magneticraft2:settled_sediment");
assert.equal(clay.result.item,"minecraft:clay_ball");
assert.equal(clay.result.count,1);

const be=read(source+"common/blockentity/stage/bronze/WaterSettlingTankBlockEntity.java");
const block=read(source+"common/block/stage/bronze/WaterSettlingTankBlock.java");
const renderer=read(source+"client/render/blocks/stage/bronze/WaterSettlingTankRenderer.java");

// A fluid-volume regression based on the actual Java static definitions,
// not a second set of hand-copied water coordinates in this test.
const getFluidDefinition=name=>{
 const m=renderer.match(new RegExp(
  "private static final (?:PoolSection\\[\\]|PoolSection|float\\[\\]\\[\\])\\s+"+
  name+"\\s*=\\s*([\\s\\S]*?);"));
 assert.ok(m,"Missing fluid footprint definition: "+name);
 return m[1];
};
const fluidRects=name=>[...getFluidDefinition(name).matchAll(
 /new PoolSection\((-?[\d.]+)F,\s*(-?[\d.]+)F,\s*(-?[\d.]+)F,\s*(-?[\d.]+)F\)/g
)].map(match=>match.slice(1).map(Number));
const fluidOutline=name=>[...getFluidDefinition(name).matchAll(
 /\{\s*(-?[\d.]+)F,\s*(-?[\d.]+)F\s*\}/g
)].map(match=>match.slice(1).map(Number));
const dirtySections=fluidRects("DIRTY_SECTIONS");
const dirtyInlet=fluidRects("DIRTY_INLET")[0];
const cleanMain=fluidRects("CLEAN_MAIN")[0];
const cleanTongue=fluidRects("CLEAN_OUTLET")[0];
const dirtyContour=fluidOutline("DIRTY_PERIMETER");
const dirtyInletContour=fluidOutline("DIRTY_INLET_PERIMETER");
const cleanContour=fluidOutline("CLEAN_OUTLET_PERIMETER");
const shallowContour=fluidOutline("CLEAN_SHALLOW_PERIMETER");
assert.equal(dirtySections.length,3,
 "Dirty basin must have rear-left and rear-right pockets plus main reservoir");
assert.ok(dirtyInlet&&cleanMain&&cleanTongue,
 "Missing inlet throat or clean collection/outlet");
const dirtyMain=dirtySections[1],left=dirtySections[0],right=dirtySections[2];
assert.equal(left[3],dirtyMain[2],"Left rear water pocket must meet the main pool");
assert.equal(right[3],dirtyMain[2],"Right rear water pocket must meet the main pool");
assert.equal(left[0],dirtyMain[0],"Left pocket must meet the basin's left wall");
assert.equal(right[1],dirtyMain[1],"Right pocket must meet the basin's right wall");
assert.ok(left[2] < dirtyMain[2]-0.5 && right[2] < dirtyMain[2]-0.5,
 "Dirty water must actually reach both rear wall pockets, not stop at the inlet");
assert.ok(left[1] < 0 && right[0] > 1,
 "Keep the original rear side pockets outside the actual inlet walls");
assert.equal(dirtyInlet[3],dirtyMain[2],
 "New dirty inlet water must meet the main pool without a seam");
assert.ok(dirtyInlet[0]>0.125 && dirtyInlet[1]<0.875,
 "New dirty inlet water must fit BETWEEN the real input port's side walls");
assert.ok(dirtyInlet[2]>-3.25 && dirtyInlet[2]<-3.10,
 "Water under the inlet must approach but not overlap its copper ring");
assert.ok(dirtyInlet[0]>left[1] && dirtyInlet[1]<right[0],
 "Dirty inlet throat must occupy only the dry center gap between rear pockets");
assert.ok(renderer.includes("top > DIRTY_INLET_SILL + 0.005F"),
 "Fluid may only cover the real inlet's raised bottom sill once high enough");
assert.ok(renderer.includes("onInletThroat ? DIRTY_INLET_SILL"),
 "Dirty throat sides must start above the real inlet sill, not the basin floor");
assert.ok(renderer.includes("DIRTY_WITH_INLET")&&
          renderer.includes("DIRTY_INLET_PERIMETER"),
 "New dirty inlet must be rendered with its exterior contour and no inner seam");
assert.equal(cleanTongue[0],cleanMain[1],
 "The clean outlet must have a connected approach, not a floating water island");
assert.ok(cleanMain[3]>-0.145 && cleanMain[3]<-0.125,
 "Clean pool must nearly meet the front retaining wall without intersecting it");
assert.ok(cleanTongue[1]>2.22 && cleanTongue[1]<2.25,
 "Clean water must approach the copper output frame without intersecting it");
assert.ok(cleanTongue[2]>-0.875 && cleanTongue[2]<-0.85 &&
          cleanTongue[3]>-0.15 && cleanTongue[3]<-0.125,
 "Clean water must fill BOTH sides of the output throat without crossing its walls");
assert.ok(renderer.includes("top > CLEAN_OUTLET_SILL + 0.005F"),
 "Outlet approach should only form after liquid rises above its bottom sill");
assert.ok(renderer.includes("CLEAN_OUTLET_SILL : POOL_BOTTOM"),
 "Fluid outlet approach must use the higher sill to avoid the metal base");

const fluidSectionArea=box=>(box[1]-box[0])*(box[3]-box[2]);
const overlapArea=(a,b)=>Math.max(0,Math.min(a[1],b[1])-Math.max(a[0],b[0]))*
                       Math.max(0,Math.min(a[3],b[3])-Math.max(a[2],b[2]));
const signedContourArea=outline=>outline.reduce((area,p,i)=>{
 const next=outline[(i+1)%outline.length];
 return area+p[0]*next[1]-next[0]*p[1];
},0)/2;
const containsFluid=(box,x,z)=>x>box[0]&&x<box[1]&&z>box[2]&&z<box[3];
const containsOutline=(outline,x,z)=>{
 let yes=false;
 for(let i=0,j=outline.length-1;i<outline.length;j=i++){
  const a=outline[i],b=outline[j];
  if((a[1]>z)!==(b[1]>z) &&
     x<(b[0]-a[0])*(z-a[1])/(b[1]-a[1])+a[0]) yes=!yes;
 }
 return yes;
};
for(const [label,sections,outline] of [
 ["dirty-shallow",dirtySections,dirtyContour],
 ["dirty-with-inlet",[...dirtySections,dirtyInlet],dirtyInletContour],
 ["clean",[cleanMain,cleanTongue],cleanContour],
 ["clean-shallow",[cleanMain],shallowContour]
]){
 assert.ok(signedContourArea(outline)<0,
  label+" perimeter must be ordered with outward normals (-dz,+dx)");
 const area=Math.abs(signedContourArea(outline));
 const sum=sections.reduce((a,b)=>a+fluidSectionArea(b),0);
 assert.ok(Math.abs(area-sum)<1e-5,
  label+" has an overlap, top-surface hole or mismatch at joined regions");
 for(let i=0;i<sections.length;i++)for(let j=i+1;j<sections.length;j++)
  assert.ok(overlapArea(sections[i],sections[j])<1e-8,
   label+" sections must TOUCH only, never duplicate translucent top faces");
 for(let i=0;i<outline.length;i++){
  const a=outline[i],b=outline[(i+1)%outline.length];
  assert.ok((a[0]===b[0]) !== (a[1]===b[1]),
   label+" perimeter must use nonzero axis-aligned exterior edges only");
 }
 // Sample every rectangle-boundary cell; the contour must describe exactly
 // the union of the rectangular top tiles, with no fake interior side walls.
 const xs=[...new Set([...outline.map(v=>v[0]),
   ...sections.flatMap(b=>[b[0],b[1]])])].sort((a,b)=>a-b);
 const zs=[...new Set([...outline.map(v=>v[1]),
   ...sections.flatMap(b=>[b[2],b[3]])])].sort((a,b)=>a-b);
 for(let xi=0;xi<xs.length-1;xi++)for(let zi=0;zi<zs.length-1;zi++){
  const x=(xs[xi]+xs[xi+1])/2,z=(zs[zi]+zs[zi+1])/2;
  assert.equal(containsOutline(outline,x,z),
   sections.some(b=>containsFluid(b,x,z)),
   label+" exterior perimeter mismatches top footprint at "+[x,z]);
 }
}
// Check real MODEL/COLLISION clearance at maximum fill, where intersection
// is most likely. A smaller fill cannot cross a static solid if this passes.
const staticBoxes=vox.collision.map(c=>c.map(v=>v/16));
const volume=(b,baseY)=>[b[0],baseY,b[2],b[1],1.54,b[3]];
const sectionVolumes=[
 ...dirtySections.map(b=>["dirty",volume(b,1.014)]),
 ["dirty-inlet",volume(dirtyInlet,1.135)],
 ["clean",volume(cleanMain,1.014)],
 ["clean-output",volume(cleanTongue,1.135)]
];
for(const [name,water] of sectionVolumes)
 for(const solid of staticBoxes)
  assert.ok(!intersects(water,solid),
   name+" rendered fluid volume intersects actual V2 masonry: "+water);
const realPortVolumes=[
 ["input",where("I")[0],json(assets+"models/block/multiblock_fluid_input.json"),"north",
  json(assets+"models/multiblock/"+id+"_input_port_insert.json")],
 ["output",where("O")[0],json(assets+"models/block/multiblock_fluid_output.json"),"east",
  json(assets+"models/multiblock/"+id+"_output_port_insert.json")]
];
for(const [name,cell,model,facing,insert] of realPortVolumes){
 const offset=[cell.x,cell.y,cell.z];
 const modelShapes=model.elements.map(e=>rotateBox(e,facing));
 const insertShapes=insert.elements.map(e=>[...e.from,...e.to]);
 for(const shape of [...modelShapes,...insertShapes]){
  const solid=shape.map((v,i)=>v/16+offset[i%3]);
  for(const [region,water] of sectionVolumes)assert.ok(!intersects(water,solid),
    region+" water cuts through the real "+name+" port or tank insert");
 }
}

const generic=read(source+"common/recipe/multiblock/MultiblockProcessingRecipe.java");
const expectedBE=[
"new FluidTank(DIRTY_CAPACITY", "new FluidTank(CLEAN_CAPACITY",
"FluidRegistry.DIRTY_WATER.get()", "Fluids.WATER",
"clean.fill(recovered, IFluidHandler.FluidAction.SIMULATE)",
"itemHandler.insertItem(1, sediment.copy(), true).isEmpty()",
"if (!ready)", "setProcessing(false)", "FluidStack drained = dirty.drain(",
"clean.fill(recovered, IFluidHandler.FluidAction.EXECUTE)",
"itemHandler.insertItem(1, sediment.copy(), false)",
"activeRecipeId = recipe.getId()", "processTime = 0",
"MultiblockRenderParts.assign", "MultiblockVoxelNbt.write",
"getFluidInTank(int tank)", "return formed ? clean.drain",
"capability == ForgeCapabilities.ITEM_HANDLER"
];
for(const check of expectedBE)assert.ok(be.includes(check),"Controller lost contract "+check);
assert.ok(be.indexOf("clean.fill(recovered, IFluidHandler.FluidAction.SIMULATE)")
          <be.indexOf("FluidStack drained = dirty.drain("),
  "Must reserve clean water output before consuming dirty water");
assert.ok(be.indexOf("itemHandler.insertItem(1, sediment.copy(), true)")
          <be.indexOf("FluidStack drained = dirty.drain("),
  "Must reserve sediment output before consuming dirty water");
assert.ok(block.includes("WaterSettlingTankBlockEntity::serverTick"));
assert.ok(!be.includes("GearNetworkManager") && !be.includes("getOrCreateGearNode"),
 "Passive treatment does not consume mechanical power");
assert.ok(generic.includes("Ingredient.EMPTY") &&
          generic.includes("input.isEmpty() && fluidInput.isEmpty()") &&
          generic.includes("if (input.isEmpty()) return false"),
 "Fluid-only recipe support must leave item recipe matching gated");
assert.ok(1.045 + 0.495 > 1.50 &&
          1.045 + 0.495 < 1 + 9/16,
 "Full water must stay below the low transverse weir, not slice through it");
for (const s of [
  "tank.getDirtyFillRatio(),",
  "tank.getCleanFillRatio(),",
  "DIRTY_SECTIONS", "DIRTY_PERIMETER",
  "DIRTY_INLET", "DIRTY_WITH_INLET", "DIRTY_INLET_PERIMETER",
  "CLEAN_MAIN", "CLEAN_OUTLET", "CLEAN_OUTLET_PERIMETER",
  "CLEAN_SHALLOW_PERIMETER", "renderPerimeter(",
  "renderPool(pose, buffers, light, new FluidStack(",
  "getDirtyAmount() > 0", "getCleanAmount() > 0",
  "float top = 1.045F + 0.495F",
  "float sideAlpha=alpha*0.62F",
  "for (int tileZ=(int)Math.floor(section.minZ())",
  "float u0=u(sprite,x0-tileX)",
  "pose.translate(-0.5D, 1.775D, 0.5D)",
  "getSedimentStack()", "RenderType.translucent()"
])assert.ok(renderer.includes(s),"Wrong fluid/port position in renderer: "+s);
for(const s of ["BlockRegistry.WATER_SETTLING_TANK","ITEM_WATER_SETTLING_TANK",
 "WATER_SETTLING_TANK_BE","WATER_SETTLING_TANK"]){
 const sources=[
  source+"common/registry/registers/BlockRegistry.java",
  source+"common/registry/registers/ItemRegistry.java",
  source+"common/registry/registers/BlockEntityRegistry.java",
  source+"client/Clientsetup.java"
 ];
 assert.ok(sources.some(p=>read(p).includes(s)), "Missing registration "+s);
}
assert.ok(read(source+"compatibility/JEI/MagneticraftJeiPlugin.java")
 .includes("MultiblockJeiRecipe.WATER_SETTLING_TANK"));
for(const tag of ["mineable/pickaxe","needs_stone_tool"]){
 const t=json("src/main/resources/data/minecraft/tags/blocks/"+tag+".json");
 assert.ok(t.values.includes("magneticraft2:"+id),"Tool harvest tag missing "+tag);
}
const book=json(assets+"patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json");
const page=book.pages.find(p=>p.name==="Water Settling Tank");
assert.ok(page?.enable_visualize);
const expectedPattern=[];
for(let y=1;y>=0;y--){
 const cols=[];
 for(let x=0;x<5;x++){
  let s="";
  for(let z=0;z<5;z++){
   const k=rows[y][z][x];
   s+=k==="C"?"0":k==="A"?"_":k;
  }
  cols.push(s);
 }
 expectedPattern.push(cols);
}
assert.deepEqual(page.multiblock.pattern,expectedPattern,
 "Patchouli projection must match all 50 exact multiblock cells");
assert.ok(book.pages.some(p=>p.recipe==="magneticraft2:"+id));
assert.ok(book.pages.some(p=>p.recipe==="magneticraft2:settled_sediment_to_clay"));
// New machine-specific 16x16 art is backed by real committed PNG assets.
const customTextures=[
 "foundation","refractory","lining","basin_floor",
 "bronze","bronze_joint","riveted_iron","grate","sludge"
];
const bytes=new Set();
for(const tex of customTextures){
 const rel=assets+"textures/block/settling_tank/"+tex+".png";
 const buf=fs.readFileSync(path.join(root,rel));
 assert.equal(buf.subarray(0,8).toString("hex"),"89504e470d0a1a0a",
     tex+" is not a real PNG");
 assert.equal(buf.readUInt32BE(16),16,tex+" width must be one Minecraft tile");
 assert.equal(buf.readUInt32BE(20),16,tex+" height must be one Minecraft tile");
 assert.equal(buf[24],8,tex+" must use 8-bit channels");
 assert.equal(buf[25],2,tex+" must be a painted, opaque RGB texture");
 bytes.add(buf.toString("base64"));
}
assert.equal(bytes.size,customTextures.length,"Textures must not be copied duplicates");
execFileSync(process.execPath,["tools/water-settling-tank/build-textures.mjs","--check"],
             {cwd:root,stdio:"inherit"});
// Materials are DIFFERENT ROLES, not merely nine aliases on one mesh.
const modelExample=json(assets+"models/multiblock/"+id+"_south/part_0_1_-2.json");
for(const mat of customTextures)
 assert.equal(modelExample.textures[mat],"magneticraft2:block/settling_tank/"+mat,
   "Missing registered V5 material "+mat);
assert.ok(modelExample.elements.some(e=>e.faces.up.texture==="#riveted_iron"),
 "Transverse overflow weir must look like WORKING METAL, not another bronze rail");
const retainingLip=json(assets+"models/multiblock/"+id+"_south/part_0_1_-1.json");
assert.ok(retainingLip.elements.some(e=>e.faces.up.texture==="#riveted_iron"),
 "Retaining lip must contrast with the bronze perimeter");
const straightRail=json(assets+"models/multiblock/"+id+"_south/part_-2_1_-3.json");
assert.ok(straightRail.elements.some(e=>e.faces.up.texture==="#bronze"),
 "Straight perimeter rails must use unadorned cast bronze");
for(const f of sdir){
 const at=rot(0,-2,f);
 const floor=json(assets+"models/multiblock/"+id+"_"+f+"/part_"+at[0]+"_0_"+at[1]+".json");
 assert.ok(floor.elements.some(e=>
      e.faces.up.texture==="#basin_floor" &&
      e.faces.east.texture==="#foundation"),
   "Waterproof floor TOP and load-bearing foundation SIDES must survive facing "+f);
}
for(const kind of portInsertNames){
 const insert=json(assets+"models/multiblock/"+id+"_"+kind+"_port_insert.json");
 assert.ok(insert.elements.some(e=>e.faces.up.texture==="#bronze_joint"),
  "Real fluid port "+kind+" should have DARK mechanical joining plates");
}
const textureGenerator=read("tools/water-settling-tank/build-textures.mjs");
assert.ok(textureGenerator.includes("const tiles = {") &&
          !textureGenerator.includes("Math.random("),
 "V5 textures must be reproducible hand-painted pixel grids");
execFileSync(process.execPath,["tools/water-settling-tank/build.mjs","--check"],
             {cwd:root,stdio:"inherit"});
console.log("PASS: V5 role-based art — nine unique 16x16 tiles, foundation vs ceramic floor per-face UVs, cast bronze and dark riveted joints, steel working surfaces, no geometry or fluid changes, 4 facings");
