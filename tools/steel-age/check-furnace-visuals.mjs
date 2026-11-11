// Verify the active Steel V2 models are present and do not contain
// overlapping cuboids or non-native UVs. Run after any Studio/model edit.
import fs from "node:fs";
import path from "node:path";
import assert from "node:assert/strict";
import {fileURLToPath} from "node:url";
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),"../..");
const base="src/main/resources/assets/magneticraft2/models/multiblock/steel_process/";
const dirs=["south","north","east","west"];
const oriented=[
 "front_corner","side_panel","coke_lower","coke_upper","coke_cap",
 "coke_roof_corner","blast_jamb_left","blast_jamb_right","blast_hearth",
 "blast_lintel","blast_ledge","shaft_column","blast_shaft_vent",
 "blast_shaft_flue","refine_wing_left","refine_wing_right",
 "refine_hearth","refine_vent","refine_roof_cornice",
 "refine_hood_vent","stack_inner"
];
const shared=["stack_corner","shaft_wall","foundation","refractory","roof"];
const side=(name)=>name==="north"||name==="south"?"z":
 name==="east"||name==="west"?"x":"y";
const overlap=(a,b)=>a.from.every((v,i)=>
 Math.max(v,b.from[i])<Math.min(a.to[i],b.to[i]));
let count=0;
for(const role of [...oriented,...shared]){
 const facings=oriented.includes(role)?dirs:[""];
 for(const facing of facings){
  const id=role+(facing?"_"+facing:"");
  const j=JSON.parse(fs.readFileSync(path.join(root,base+id+".json"),"utf8"));
  assert(j.parent&&j.textures,"Missing model metadata "+id);
  if(!j.elements)continue; // legacy foundation/refractory/roof use cube parents
  assert(j.elements.length>0&&j.elements.length<=32,"Model part budget "+id);
  for(let a=0;a<j.elements.length;a++){
   const e=j.elements[a];
   for(let i=0;i<3;i++){
    assert(Number.isInteger(e.from[i])&&Number.isInteger(e.to[i]),
      "Non-native pixel model "+id);
    assert(e.from[i]>=0&&e.to[i]<=16&&e.from[i]<e.to[i],
      "Model cube outside local cell "+id);
   }
   for(const [dir,face] of Object.entries(e.faces)){
    assert(j.textures[face.texture.slice(1)],"Unknown texture in "+id);
    const width=dir==="east"||dir==="west"?
      e.to[2]-e.from[2]:e.to[0]-e.from[0];
    const height=dir==="up"||dir==="down"?
      e.to[2]-e.from[2]:e.to[1]-e.from[1];
    assert.deepEqual(face.uv,[0,0,width,height],
      "Stretched UV in "+id+"/"+dir);
   }
   for(let b=a+1;b<j.elements.length;b++)
    assert(!overlap(e,j.elements[b]),
      "Overlapping solid cubes in "+id+": "+a+"+"+b);
  }
  count++;
 }
}
for(const machine of ["steel_blast_furnace","steel_refining_furnace"]){
 const baseData="src/main/resources/data/magneticraft2/";
 for(const dir of dirs){
  const bp=JSON.parse(fs.readFileSync(path.join(root,
   baseData+"multiblocks/"+machine+"_"+dir+".json"),"utf8"));
  assert.deepEqual(bp.settings.render_air_anchors,[[0,1,0]],
   "Hearth proxy must sit immediately above controller");
 }
 const vox=JSON.parse(fs.readFileSync(path.join(root,
  baseData+"multiblock_voxels/"+machine+".json"),"utf8"));
 assert.deepEqual(vox.collision,vox.selection);
 const occupied=Object.values(JSON.parse(fs.readFileSync(path.join(root,
   baseData+"multiblocks/"+machine+"_south.json"),"utf8"))
   .structure.layout).flat(2).filter(x=>x!=="A").length;
 assert.equal(vox.collision.length,occupied+
  (machine==="steel_refining_furnace"?9:5),
  "Hearth collision lost or original brick collision changed");
}
console.log("PASS Steel furnace models: "+count+
 " baked models, native UVs, no overlapping solids, 8 anchored formations");
