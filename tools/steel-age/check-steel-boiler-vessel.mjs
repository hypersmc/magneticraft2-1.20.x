import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import {fileURLToPath} from "node:url";
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),"../..");
const get=p=>JSON.parse(fs.readFileSync(path.join(root,p),"utf8"));
const load=p=>fs.readFileSync(path.join(root,p),"utf8");
const D="src/main/resources/data/magneticraft2/",A="src/main/resources/assets/magneticraft2/";
const roles=["bottom_left","bottom","bottom_right","left","right","top_left","top","top_right"];
const dirs=["south","north","east","west"];
const south=get(D+"multiblocks/steel_boiler_vessel_south.json");
const counts={};
Object.values(south.structure.layout).forEach(rows=>rows.forEach(row=>row.forEach(c=>counts[c]=(counts[c]||0)+1)));
assert.deepEqual([counts.S,counts.P,counts.J,counts.F,counts.C,counts.A],[24,16,2,10,1,7]);
assert.deepEqual(south.structure.dimensions,[3,4,5]);
const rot=(x,z,d)=>d==="south"?[x,z]:d==="north"?[2-x,4-z]:d==="east"?[z,2-x]:[4-z,x];
for(const dir of dirs){
 const bp=get(D+"multiblocks/steel_boiler_vessel_"+dir+".json");
 const w=dir==="east"||dir==="west"?5:3,depth=dir==="east"||dir==="west"?3:5;
 assert.deepEqual(bp.structure.dimensions,[w,4,depth]);
 assert.equal(bp.settings.voxel_profile,"magneticraft2:steel_boiler_vessel");
 for(let y=0;y<4;y++)for(let z=0;z<5;z++)for(let x=0;x<3;x++){
   const [X,Z]=rot(x,z,dir);
   assert.equal(bp.structure.layout["layer"+(y+1)][Z][X],south.structure.layout["layer"+(y+1)][z][x]);
 }
 for(const role of roles)for(const prefix of ["shell_","rim_front_","rim_back_"]){
   const m=get(A+"models/multiblock/boiler_vessel/"+prefix+role+"_"+dir+".json");
   assert(m.elements.length>0);
   for(const e of m.elements){
     const [dx,dy,dz]=e.to.map((v,i)=>v-e.from[i]);
     assert(e.from.every((v,i)=>v>=0&&e.to[i]<=16&&e.to[i]>v));
     const sizes={north:[dx,dy],south:[dx,dy],east:[dz,dy],west:[dz,dy],up:[dx,dz],down:[dx,dz]};
     for(const [face,s] of Object.entries(sizes))assert.deepEqual(e.faces[face].uv,[0,0,...s]);
   }
 }
 for(const end of ["front","back"])get(A+"models/multiblock/boiler_vessel/cap_"+end+"_"+dir+".json");
 get(A+"models/multiblock/steel_boiler_vessel_"+dir+".json");
}
const voxel=get(D+"multiblock_voxels/steel_boiler_vessel.json");
assert.equal(voxel.format,1);assert.equal(voxel.unit,"model_pixels");
assert(voxel.collision.length>50&&voxel.collision.length<=512);
for(const b of voxel.collision)assert(b.every((v,i)=>Number.isFinite(v)&&(i<3?b[i+3]>v:true)));
const book=get(A+"patchouli_books/world_of_magneticraft/en_us/entries/steelage.json");
const page=book.pages.find(p=>p.name==="Steel Boiler Vessel");
assert(page&&page.enable_visualize&&page.multiblock);
const display = page.multiblock.pattern;
assert(display.length===4 && display.every(layer=>layer.length===3 && layer.every(row=>row.length===5)),
       "Patchouli pattern must be [Y top-down][X west-east][Z north-south]");
const expected = {
    S: (x,y)=> y===1?"abc"[x]:y===2?(x===0?"d":"e"):"fgh"[x],
    J: z=>z===0?"K":"J", P:()=>"P", F:()=>"F", C:()=>"0", A:()=>" "
};
for(let y=0;y<4;y++)for(let z=0;z<5;z++)for(let x=0;x<3;x++){
    const cell=south.structure.layout["layer"+(y+1)][z][x];
    assert.equal(display[3-y][x][z],expected[cell](x,y,z),
                 "Patchouli cell order/role wrong at "+[x,y,z]);
}

assert(book.pages.some(p=>p.recipe==="magneticraft2:steel_boiler_vessel"));
const cls=load("src/main/java/com/magneticraft2/common/blockentity/stage/steel/SteelBoilerVesselBlockEntity.java");
assert(!cls.includes("serverTick")&&!cls.includes("SteamNetworkManager"));
const registered=load("src/main/java/com/magneticraft2/common/registry/registers/BlockRegistry.java");
assert(registered.includes('BLOCKS.register("steel_boiler_vessel"'));
assert.equal(get(D+"recipes/steel_boiler_vessel.json").result.item,"magneticraft2:steel_boiler_vessel");
console.log("PASS Steel IV-C JSON/mass ledger/rotations/UV/collision/Patchouli structural contract.");
console.log("Static only. Full Forge build and Minecraft acceptance NOT performed.");
