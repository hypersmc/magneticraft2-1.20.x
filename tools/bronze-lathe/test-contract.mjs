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
   JSON.parse(fs.readFileSync(filename));
  }
 }
 const normalized=[counts.S,counts.L,counts.I,counts.P,counts.H,counts.A];
 if(baseline)assert.deepEqual(normalized,baseline);
 else baseline=normalized;
}
const voxel=JSON.parse(fs.readFileSync(root+"/data/magneticraft2/multiblock_voxels/bronze_mechanical_lathe.json"));
assert.equal(voxel.format,1);
assert.equal(voxel.collision.length,22);
for(const box of voxel.collision)assert.ok(box[3]>box[0]&&box[4]>box[1]&&box[5]>box[2]);
for(const file of ["bronze_mechanical_lathe","bronze_lathe_cutter"]){
 const recipe=JSON.parse(fs.readFileSync(root+"/data/magneticraft2/recipes/"+file+".json"));
 assert.ok(!JSON.stringify(recipe).includes("minecraft:iron_ingot"),
         "Bronze machinery must not require Iron Age resources: "+file);
}
const book=JSON.parse(fs.readFileSync(root+"/assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json"));
assert.ok(book.pages.some(p=>p.type==="patchouli:multiblock"&&p.name==="Mechanical Lathe"));
console.log("Bronze Lathe structural contract: 4 orientations, 88 part models, 22 voxel cells, recipes and Patchouli PASS");
