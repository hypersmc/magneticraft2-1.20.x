#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";
function makeAssets(){
 const files=new Map();
 const a="src/main/resources/assets/magneticraft2/",d="src/main/resources/data/magneticraft2/",id="water_settling_tank";
 const rows=[["SSSSS","SSSSS","SSSSS","SSSSS","SSCSS"],["SSISS","SAAAS","SAAAS","SAAAO","SPSSS"]];
 const types={S:"minecraft:stone_bricks",A:"minecraft:air",
 I:"magneticraft2:multiblock_fluid_input",P:"magneticraft2:multiblock_item_primary_output",
 C:"magneticraft2:water_settling_tank",O:"magneticraft2:multiblock_fluid_output"};
 const tex=Object.fromEntries(["refractory","lining","bronze","riveted_iron","grate","sludge"].map(t=>[t,"magneticraft2:block/settling_tank/"+t]));
 tex.particle=tex.refractory;
 const cells=[];for(let y=0;y<2;y++)for(let zi=0;zi<5;zi++)for(let xi=0;xi<5;xi++)cells.push({x:xi-2,y,z:zi-4,k:rows[y][zi][xi]});
 // Real working modules belong in accessible walls with a solid base BELOW
 // each one. The old floor-level ports looked sunken, disconnected, and
 // punched conspicuous holes through the floor.
 const required={C:[0,0,0],I:[0,1,-4],P:[-1,1,0],O:[2,1,-1]};
 for(const [letter,xyz] of Object.entries(required)){
  const found=cells.filter(c=>c.k===letter);
  if(found.length!==1||[found[0].x,found[0].y,found[0].z].join()!==xyz.join())throw Error("Invalid real port "+letter);
 }
 const shapes=new Map(),voxels=[];
 const isInletCollar=c=>c.x===0&&c.y===1&&c.z===-3;
 const add=(c,material,from,to)=>{
  // Only this AIR-hosted inlet collar may extend up to half a block into
  // the adjoining actual Fluid Input. All other parts remain block-local.
  if(from.some((v,i)=>v<(i===2&&isInletCollar(c)?-8:0)||to[i]>16||v>=to[i]))
   throw Error("Invalid local element "+[c.x,c.y,c.z]+": "+[from,to]);
  const key=[c.x,c.y,c.z].join(",");
  if(!shapes.has(key))shapes.set(key,[]);
  shapes.get(key).push({material,from,to});
  voxels.push([...from.map((v,i)=>v+[c.x,c.y,c.z][i]*16),...to.map((v,i)=>v+[c.x,c.y,c.z][i]*16)]);
 };
 for(const c of cells) {
  const {x,y,z,k}=c;
  if(y===0 && (k==="S"||k==="C")){
   // One uninterrupted dark structural floor per cell. The former ceramic
   // top slab, "sludge" decal, grate and fascia all ended at Y=16 with
   // overlapping TOP FACES, producing severe in-game Z-fighting.
   // No water is rendered when the tanks are empty, so the dark dry floor
   // now reads differently from the translucent water surface.
   // Dedicated bronze footplates visibly seat all three real working
   // ports ON the masonry, rather than floating in cut-out ground cells.
   if((x===0&&z===-4)||(x===-1&&z===0)||(x===2&&z===-1)){
    add(c,"refractory",[0,0,0],[16,14,16]);
    add(c,"bronze",[0,14,0],[16,16,16]);
   }else{
    add(c,"refractory",[0,0,0],[16,16,16]);
   }
  }else if(y===1&&k==="S"){
   // Each upper wall segment occupies ONLY its own block-local cell.
   // In corner cells the rear/front return and side wall join without
   // geometric intersections, missing wedges, or duplicate surfaces.
   if(z===-4){
    add(c,"refractory",[0,0,0],[16,10,5]);
    if(x===-2){
     // Left rear L joint, continuous right up to the inside wall.
     add(c,"refractory",[11,0,5],[16,10,16]);
     add(c,"lining",[0,1,5],[11,10,6]);
     add(c,"lining",[10,2,6],[11,10,16]);
     add(c,"bronze",[0,10,0],[16,12,7]);
     add(c,"bronze",[9,10,7],[16,12,16]);
    }else if(x===2){
     // Right rear L joint; no dark missing triangle behind copper rim.
     add(c,"refractory",[0,0,5],[5,10,16]);
     add(c,"lining",[5,1,5],[16,10,6]);
     add(c,"lining",[5,2,6],[6,10,16]);
     add(c,"bronze",[0,10,0],[16,12,7]);
     add(c,"bronze",[0,10,7],[7,12,16]);
    }else{
     add(c,"lining",[0,1,5],[16,10,6]);
     add(c,"bronze",[0,10,0],[16,12,7]);
    }
    // Both rear-wall returns now meet the copper inlet's frame through
    // a dedicated AIR-hosted throat. The I cell remains the REAL module.
    if(x===-1){
     add(c,"refractory",[13,0,6],[15,10,16]);
     add(c,"lining",[15,2,6],[16,10,16]);
     add(c,"bronze",[13,10,7],[16,12,16]);
    }else if(x===1){
     add(c,"refractory",[1,0,6],[3,10,16]);
     add(c,"lining",[0,2,6],[1,10,16]);
     add(c,"bronze",[0,10,7],[3,12,16]);
    }
   }else if(z===0 && (x===-2 || x===2)){
    // Front corners connect side rails to the short front wall; neither
    // rail terminates mid-air. Leave true module blocks unobstructed.
    if(x===-2){
     add(c,"refractory",[11,0,0],[16,10,16]);
     add(c,"refractory",[0,0,11],[11,10,16]);
     add(c,"lining",[10,2,0],[11,10,10]);
     add(c,"lining",[0,2,10],[11,10,11]);
     add(c,"bronze",[9,10,0],[16,12,16]);
     add(c,"bronze",[0,10,9],[9,12,16]);
    }else{
     add(c,"refractory",[0,0,0],[5,10,16]);
     add(c,"refractory",[5,0,11],[16,10,16]);
     add(c,"lining",[5,2,0],[6,10,10]);
     add(c,"lining",[5,2,10],[16,10,11]);
     add(c,"bronze",[0,10,0],[7,12,16]);
     add(c,"bronze",[7,10,9],[16,12,16]);
     // Simple raised end-cap matching the opposite end of the copper
     // output module in adjacent side-wall cell (2,1,-1).
     add(c,"riveted_iron",[0,12,0],[7,14,2]);
    }
   }else if(x===-2){
    add(c,"refractory",[11,0,0],[16,10,16]);
    add(c,"lining",[10,2,0],[11,10,16]);
    add(c,"bronze",[9,10,0],[16,12,16]);
   }else if(x===2){
    add(c,"refractory",[0,0,0],[5,10,16]);
    add(c,"lining",[5,2,0],[6,10,16]);
    add(c,"bronze",[0,10,0],[7,12,16]);
    if(z===-2) add(c,"riveted_iron",[0,12,14],[7,14,16]);
   }else if(z===0){
    // Continuous front facade between actual sediment/clean-water ports.
    add(c,"refractory",[0,0,11],[16,10,16]);
    add(c,"lining",[0,2,10],[16,10,11]);
    add(c,"bronze",[0,10,9],[16,12,16]);
   }else throw Error("Unrecognized perimeter "+[x,y,z]);
  }else if(y===1 && k==="A" && x===0 && z===-3){
   // The actual fluid input's inner copper ring is X=2..14,Y=2..14,
   // Z=8..12. In this adjoining AIR block Z=-8 touches the copper at
   // its real depth; the middle stays fully open for water to enter.
   add(c,"refractory",[0,0,-8],[2,12,2]);
   add(c,"refractory",[14,0,-8],[16,12,2]);
   add(c,"bronze",[0,12,-8],[2,14,2]);
   add(c,"bronze",[14,12,-8],[16,14,2]);
   add(c,"riveted_iron",[2,0,-8],[14,2,2]);
   add(c,"bronze",[2,14,-8],[14,16,2]);
   // Inboard detailing begins only AFTER the actual module's ring
   // ends, avoiding intersecting or repainting the shared copper port.
   add(c,"riveted_iron",[2,2,-4],[3,14,2]);
   add(c,"riveted_iron",[13,2,-4],[14,14,2]);
   add(c,"riveted_iron",[3,13,-4],[13,14,2]);
   add(c,"lining",[3,2,-4],[13,3,2]);
  }else if(y===1 && k==="A" && z===-2 && Math.abs(x)<=1){
   // A single low overflow weir across the WIDTH; behind it is the
   // deeper dirty settling area, in front a short clear-water channel.
   add(c,"refractory",[0,0,13],[16,6,16]);
   add(c,"lining",[0,6,13],[16,8,16]);
   add(c,"bronze",[0,8,12],[16,9,16]);
  }else if(y===1 && k==="A" && z===-1 && Math.abs(x)<=1){
   // Front retaining lip stays BEHIND the three real service modules.
   add(c,"refractory",[0,0,14],[16,9,16]);
   add(c,"lining",[0,9,14],[16,10,16]);
   add(c,"bronze",[0,10,13],[16,11,16]);
  }
 }
 const renderAnchors=cells.filter(c=>c.k==="A"&&shapes.has([c.x,c.y,c.z].join(",")))
  .map(c=>[c.x,c.y,c.z]);
 if(renderAnchors.length!==7)throw Error("Expected inlet throat + six weir/lip AIR hosts, got "+renderAnchors.length);
 if(voxels.length>512)throw Error("Voxel collision too complex: "+voxels.length);
 // Ensure no decorative or collision volume overlaps genuine port/module cells.
 for(const letter of ["I","P","O"]){
  const c=cells.find(v=>v.k===letter);
  for(const b of voxels){
   const overlapsCell=[0,1,2].every(i=>
    b[i]<([c.x,c.y,c.z][i]+1)*16&&b[i+3]>[c.x,c.y,c.z][i]*16);
   if(!overlapsCell)continue;
   // The actual north-facing inlet model has a 2px opening along every
   // edge, and its inboard ring terminates at its own local Z=12.
   // Allow ONLY the collar's non-intersecting empty margins there.
   // The module's center and all other genuine port cells stay clear.
   if(letter==="I"&&b[2]>=c.z*16+8 &&
      (b[3]<=c.x*16+2 || b[0]>=c.x*16+14
       || b[4]<=c.y*16+2 || b[1]>=c.y*16+14
       || b[2]>=c.z*16+12))continue;
   throw Error("Overlapping real "+letter+" module");
  }
 }
 files.set(d+"multiblock_voxels/"+id+".json",JSON.stringify({format:1,unit:"model_pixels",collision:voxels,selection:voxels,zones:{}},null,2)+"\n");
 const dirs=["south","west","north","east"];
 const rotate=(x,z,f)=>f==="south"?[x,z]:f==="west"?[-z,x]:f==="north"?[-x,-z]:[z,-x];
 const rotatePoint=(x,z,f)=>f==="south"?[x,z]:f==="west"?[16-z,x]:f==="north"?[16-x,16-z]:[z,16-x];
 const faces=(from,to,mat)=>{
  const dx=to[0]-from[0],dy=to[1]-from[1],dz=to[2]-from[2];
  const f=(u,v)=>({texture:"#"+mat,uv:[0,0,u,v]});
  return {north:f(dx,dy),south:f(dx,dy),east:f(dz,dy),west:f(dz,dy),up:f(dx,dz),down:f(dx,dz)};
 };
 const blockstates={variants:{}};
 for(const facing of dirs){
  const rotated=cells.map(c=>({...c,rot:rotate(c.x,c.z,facing)}));
  const x0=Math.min(...rotated.map(c=>c.rot[0])),z0=Math.min(...rotated.map(c=>c.rot[1]));
  const layout=Array.from({length:2},()=>Array.from({length:5},()=>Array(5).fill("")));
  for(const c of rotated)layout[c.y][c.rot[1]-z0][c.rot[0]-x0]=c.k;
  const blueprint={name:id+"_"+facing,structure:{
    dimensions:[5,2,5],layout:Object.fromEntries(layout.map((v,i)=>["layer"+(i+1),v])),blocks:types
   },settings:{replacewhenformed:"multiblock/"+id+"_"+facing,inventory:true,power:false,fuel:false,fluid:true,inputs:[],outputs:[],
    render_air_anchors:renderAnchors.map(([x,y,z])=>{const [X,Z]=rotate(x,z,facing);return [X,y,Z]}),voxel_profile:"magneticraft2:"+id}};
  files.set(d+"multiblocks/"+id+"_"+facing+".json",JSON.stringify(blueprint,null,2)+"\n");
  for(const c of cells){
   const local=shapes.get([c.x,c.y,c.z].join(","));
   if(!local?.length)continue;
   const [rx,rz]=rotate(c.x,c.z,facing),elements=[];
   for(const box of local){
    const pMin=box.from.map((v,i)=>v+[c.x,c.y,c.z][i]*16);
    const pMax=box.to.map((v,i)=>v+[c.x,c.y,c.z][i]*16);
    const corners=[[pMin[0],pMin[2]],[pMin[0],pMax[2]],[pMax[0],pMin[2]],[pMax[0],pMax[2]]]
      .map(([x,z])=>rotatePoint(x,z,facing));
    const from=[Math.min(...corners.map(p=>p[0]))-rx*16,box.from[1],
                Math.min(...corners.map(p=>p[1]))-rz*16];
    const to=[Math.max(...corners.map(p=>p[0]))-rx*16,box.to[1],
              Math.max(...corners.map(p=>p[1]))-rz*16];
    const collar=isInletCollar(c);
    if(from.some((v,i)=>{
     const horizontal=i===0||i===2;
     return v<(collar&&horizontal?-8:0)
       ||to[i]>(collar&&horizontal?24:16)||v>=to[i];
    }))throw Error("Invalid facing geometry "+facing+" "+[c.x,c.y,c.z]);
    elements.push({from,to,faces:faces(from,to,box.material)});
   }
   files.set(a+"models/multiblock/"+id+"_"+facing+"/part_"+rx+"_"+c.y+"_"+rz+".json",
     JSON.stringify({parent:"minecraft:block/block",ambientocclusion:false,textures:tex,elements},null,2)+"\n");
  }
  const yaw={south:0,west:90,north:180,east:270}[facing];
  blockstates.variants["facing="+facing+",is_formed=false"]={model:"magneticraft2:block/"+id+"_controller",y:yaw};
  blockstates.variants["facing="+facing+",is_formed=true"]={model:"magneticraft2:multiblock/"+id+"_"+facing+"/part_0_0_0"};
 }
 files.set(a+"blockstates/"+id+".json",JSON.stringify(blockstates,null,2)+"\n");
 files.set(a+"models/block/"+id+"_controller.json",
  JSON.stringify({parent:"minecraft:block/cube_all",textures:{all:tex.riveted_iron,particle:tex.bronze}},null,2)+"\n");
 files.set(a+"models/item/"+id+".json",JSON.stringify({parent:"magneticraft2:block/"+id+"_controller"},null,2)+"\n");
 const lumps=[[2,5,3,7,8,8],[7,4,5,13,9,12],[4,5,10,10,7,14]];
 files.set(a+"models/item/settled_sediment.json",JSON.stringify({
  parent:"minecraft:block/block",textures:{mud:tex.sludge,grit:tex.refractory,particle:tex.sludge},
  elements:lumps.map((bbox,i)=>{const from=bbox.slice(0,3),to=bbox.slice(3);
    return {from,to,faces:faces(from,to,i===1?"grit":"mud")};}),
  display:{ground:{scale:[0.7,0.7,0.7]},gui:{rotation:[25,225,0],scale:[0.85,0.85,0.85]}}
 },null,2)+"\n");
 const recipes=[
 [id,{type:"minecraft:crafting_shaped",pattern:["SBS","PCP","SFS"],
  key:{S:{item:"minecraft:stone_bricks"},B:{item:"magneticraft2:bronze_ingot"},P:{item:"magneticraft2:copper_plate"},
   C:{item:"magneticraft2:copper_fluid_tank"},F:{item:"magneticraft2:multiblock_fluid_output"}},result:{item:"magneticraft2:"+id}}],
 ["settling_dirty_water",{type:"magneticraft2:multiblock_processing",machine:"magneticraft2:"+id,
  fluid_input:{fluid:"magneticraft2:dirty_water",amount:600},fluid_output:{fluid:"minecraft:water",amount:480},
  output:{item:"magneticraft2:settled_sediment",count:1},process_time:240}],
 ["settled_sediment_to_clay",{type:"minecraft:crafting_shaped",pattern:["SS","SS"],key:{S:{item:"magneticraft2:settled_sediment"}},
  result:{item:"minecraft:clay_ball",count:1}}]
 ];
 for(const [file,recipe] of recipes)files.set(d+"recipes/"+file+".json",JSON.stringify(recipe,null,2)+"\n");
 files.set(d+"loot_tables/blocks/"+id+".json",JSON.stringify({
  type:"minecraft:block",pools:[{rolls:1,entries:[{type:"minecraft:item",name:"magneticraft2:"+id}],
  conditions:[{condition:"minecraft:survives_explosion"}]}]
 },null,2)+"\n");
 return files;
}
const args=new Set(process.argv.slice(2));
const files=makeAssets();
let mismatches=0;
for(const [relative,content] of files){
 const file=path.join(process.cwd(),relative);
 if(args.has("--check")){
  if(!fs.existsSync(file)||fs.readFileSync(file,"utf8")!==content){
   console.error("Outdated: "+relative);mismatches++;
  }
 }else{
  fs.mkdirSync(path.dirname(file),{recursive:true});
  fs.writeFileSync(file,content);
 }
}
console.log("Water Settling Tank source-generated assets:",files.size);
if(mismatches)process.exit(1);
