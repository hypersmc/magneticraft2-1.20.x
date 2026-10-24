#!/usr/bin/env node
import fs from "node:fs";
import assert from "node:assert/strict";

const root="src/main/resources";
const dirs=["south","west","north","east"];
let baseline=null;
for(const direction of dirs){
 const id="bronze_mechanical_lathe_"+direction;
 const bp=JSON.parse(fs.readFileSync(root+"/data/magneticraft2/multiblocks/"+id+".json"));
 const [width,height,depth]=bp.structure.dimensions;
 assert.equal(height,2);
 assert.equal(width*depth,15);
 const layer=bp.structure.layout;
 const counts={},positions={};
 for(let y=0;y<height;y++)for(let z=0;z<depth;z++)for(let x=0;x<width;x++){
  const type=layer["layer"+(y+1)][z][x];
  counts[type]=(counts[type]??0)+1;
  (positions[type]??=[]).push([x,y,z]);
 }
 assert.equal(counts.L,1);
 assert.equal(counts.I,1);
 assert.equal(counts.H,2);
 assert.equal(counts.P,5);
 assert.equal(counts.A,8);
 assert.equal(counts.S,13);
 assert.deepEqual(bp.structure.blocks.I,"magneticraft2:mechanical_input_module");
 assert.equal(bp.settings.voxel_profile,"magneticraft2:bronze_mechanical_lathe");
 const [cx,cy,cz]=positions.L[0];
 const [ix,iy,iz]=positions.I[0];
 assert.equal(iy,0);
 const distance=Math.abs(ix-cx)+Math.abs(iz-cz);
 assert.equal(distance,2,"Gear V2 input must remain two cells behind controller");
 for(const [type,coords] of Object.entries(positions)){
  if(type==="A")continue;
  for(const [x,y,z] of coords){
   const filename=root+"/assets/magneticraft2/models/multiblock/"+id+"/part_"+(x-cx)+"_"+y+"_"+(z-cz)+".json";
   assert.ok(fs.existsSync(filename),filename);
   const part=JSON.parse(fs.readFileSync(filename));
   // The real module is not a filler; no static Geometry V2 parts can
   // render there. It is owned exclusively by the formed input BER.
   if(type==="I")assert.equal(part.elements.length,0,
     "Input module model part must be empty: "+filename);
  }
 }
 const normalized=[counts.S,counts.L,counts.I,counts.P,counts.H,counts.A];
 if(baseline)assert.deepEqual(normalized,baseline);
 else baseline=normalized;
}
const voxel=JSON.parse(fs.readFileSync(root+"/data/magneticraft2/multiblock_voxels/bronze_mechanical_lathe.json"));
assert.equal(voxel.format,1);
const design=JSON.parse(fs.readFileSync("tools/multiblock-models/designs/bronze_mechanical_lathe.json"));
assert.ok(voxel.collision.length>=1 && voxel.collision.length<=512);
assert.deepEqual(voxel.collision,design.voxels.collision);
assert.deepEqual(voxel.selection,design.voxels.selection);
const moduleBounds={from:[0,0,-32],to:[16,16,-16]};
assert.ok(design.boxes.every(box=>
    !box.from.every((a,k)=>Math.min(box.to[k],moduleBounds.to[k])
                         -Math.max(a,moduleBounds.from[k])>1e-5)),
    "V2 static solids must not occupy the non-filler input module cell");
for(const resource of ["input_frame","input_rotor","input_output_rotor","spindle","arbor","chuck","carriage","cross_slide"]){
 const name=root+"/assets/magneticraft2/models/multiblock/bronze_mechanical_lathe_"+resource+".json";
 const model=JSON.parse(fs.readFileSync(name));
 assert.ok(model.elements?.length>0,name+" needs nonempty geometry");
 for(const el of model.elements)for(const face of Object.values(el.faces||{})){
   if(face.uv)assert.ok(face.uv.every(u=>u>=0&&u<=16),name+" must use native tiled UVs");
 }
}
const clientSetup=fs.readFileSync("src/main/java/com/magneticraft2/client/Clientsetup.java","utf8");
for(const resource of ["input_frame","input_rotor","input_output_rotor","spindle","arbor"]){
 assert.ok(clientSetup.includes('"multiblock/bronze_mechanical_lathe_'+resource+'"'),
     resource+" must be registered for BER baking");
}
for(const box of voxel.collision)assert.ok(box[3]>box[0]&&box[4]>box[1]&&box[5]>box[2]);
for(const file of ["bronze_mechanical_lathe","bronze_lathe_cutter"]){
 const recipe=JSON.parse(fs.readFileSync(root+"/data/magneticraft2/recipes/"+file+".json"));
 assert.ok(!JSON.stringify(recipe).includes("minecraft:iron_ingot"),
         "Bronze machinery must not require Iron Age resources: "+file);
}
const book=JSON.parse(fs.readFileSync(root+"/assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json"));
assert.ok(book.pages.some(p=>p.type==="patchouli:multiblock"&&p.name==="Mechanical Lathe"));

const spindle=JSON.parse(fs.readFileSync(root+"/assets/magneticraft2/models/multiblock/bronze_mechanical_lathe_spindle.json"));
const shaftSpan=[Math.min(...spindle.elements.map(e=>e.from[0])),Math.max(...spindle.elements.map(e=>e.to[0]))];
assert.ok(shaftSpan[0]<=-1.5 && shaftSpan[1]>=20.3,
  "Spindle must bridge left bearing and pass through mounted bronze workpiece");
const arbor=JSON.parse(fs.readFileSync(root+"/assets/magneticraft2/models/multiblock/bronze_mechanical_lathe_arbor.json"));
assert.equal(arbor.elements.length,3,"Workpiece-specific arbor must only contain collars");
const renderer=fs.readFileSync("src/main/java/com/magneticraft2/client/render/blocks/stage/bronze/BronzeMechanicalLatheRenderer.java","utf8");
assert.ok(renderer.includes("getModel(SPINDLE)") && renderer.includes("getModel(ARBOR)"));
const latheBE=fs.readFileSync("src/main/java/com/magneticraft2/common/blockentity/stage/bronze/BronzeMechanicalLatheBlockEntity.java","utf8");
assert.ok(latheBE.includes("public float getInputRotation(")&&latheBE.includes("if(running){"),
           "Headstock visual clutch must be separate from powered gearbox rotation");
const planes={north:[2,-1,0,1],south:[2,1,0,1],east:[0,1,2,1],west:[0,-1,2,1],up:[1,1,0,2],down:[1,-1,0,2]};
for(const id of ["input_frame","input_rotor","input_output_rotor"]){
  const model=JSON.parse(fs.readFileSync(root+"/assets/magneticraft2/models/multiblock/bronze_mechanical_lathe_"+id+".json"));
  const planesSeen=new Map();
  for(const el of model.elements)for(const face of Object.keys(el.faces)){
    const [axis,sign,u,v]=planes[face],plane=sign>0?el.to[axis]:el.from[axis],key=face+":"+plane.toFixed(5);
    const prev=planesSeen.get(key)||[];
    for(const old of prev){
      const du=Math.min(el.to[u],old.to[u])-Math.max(el.from[u],old.from[u]);
      const dv=Math.min(el.to[v],old.to[v])-Math.max(el.from[v],old.from[v]);
      assert.ok(du<=1e-5||dv<=1e-5,id+": overlapping "+face+" faces remain at "+plane);
    }
    prev.push(el);planesSeen.set(key,prev);
  }
}

console.log("Bronze Lathe structural contract: 4 orientations, 88 part models, "+
    voxel.collision.length+" authored voxel boxes, formed gearbox and arbor assets PASS");
