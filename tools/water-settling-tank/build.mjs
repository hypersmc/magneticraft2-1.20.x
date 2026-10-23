#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";
function makeAssets(){
 const files=new Map();
 const a="src/main/resources/assets/magneticraft2/",d="src/main/resources/data/magneticraft2/",id="water_settling_tank";
 const rows=[["SSISS","SSSSS","SSSSS","SSSSS","SPCSO"],["SSSSS","SAAAS","SAAAS","SAAAS","SAAAS"]];
 const types={S:"minecraft:stone_bricks",A:"minecraft:air",
 I:"magneticraft2:multiblock_fluid_input",P:"magneticraft2:multiblock_item_primary_output",
 C:"magneticraft2:water_settling_tank",O:"magneticraft2:multiblock_fluid_output"};
 const tex=Object.fromEntries(["refractory","lining","bronze","riveted_iron","grate","sludge"].map(t=>[t,"magneticraft2:block/settling_tank/"+t]));
 tex.particle=tex.refractory;
 const cells=[];for(let y=0;y<2;y++)for(let zi=0;zi<5;zi++)for(let xi=0;xi<5;xi++)cells.push({x:xi-2,y,z:zi-4,k:rows[y][zi][xi]});
 const required={C:[0,0,0],I:[0,0,-4],P:[-1,0,0],O:[2,0,0]};
 for(const [letter,xyz] of Object.entries(required)){
  const found=cells.filter(c=>c.k===letter);
  if(found.length!==1||[found[0].x,found[0].y,found[0].z].join()!==xyz.join())throw Error("Invalid real port "+letter);
 }
 const shapes=new Map(),voxels=[];
 const add=(c,material,from,to)=>{
  if(from.some((v,i)=>v<0||to[i]>16||v>=to[i]))throw Error("Non-local model element");
  const key=[c.x,c.y,c.z].join(",");
  if(!shapes.has(key))shapes.set(key,[]);
  shapes.get(key).push({material,from,to});
  voxels.push([...from.map((v,i)=>v+[c.x,c.y,c.z][i]*16),...to.map((v,i)=>v+[c.x,c.y,c.z][i]*16)]);
 };
 for(const c of cells) {
  const {x,y,z,k}=c;
  if(y===0 && (k==="S"||k==="C")){
   // Flat, readable masonry foundation with waterproof ceramic floor.
   // Keeping the base close to Minecraft blocks avoids false wood beams.
   add(c,"refractory",[0,0,0],[16,13,16]);
   add(c,"lining",[0,13,0],[16,16,16]);
   if((x===-1||x===0)&&z===-3)
    add(c,"sludge",[3,15,3],[12,16,12]); // Subtle mineral stain, not free items.
   if(x===1&&z===-1)
    add(c,"grate",[3,15,4],[13,16,12]); // Low drain guard near clean outlet.
   if(k==="C")
    add(c,"riveted_iron",[3,13,14],[13,16,16]); // Controller's real fascia.
  }else if(y===1&&k==="S"){
   // A 12-pixel LOW outer wall; no overhead frame, posts, or catwalk.
   if(z===-4){
    add(c,"refractory",[0,0,0],[16,10,5]);
    add(c,"lining",[0,1,5],[16,10,6]);
    add(c,"bronze",[0,10,0],[16,12,7]);
    if(x===0){
     // Real rear fluid input is directly below this restrained inlet pipe.
     add(c,"riveted_iron",[6,0,8],[10,9,12]);
     add(c,"riveted_iron",[5,8,9],[11,12,16]);
     add(c,"bronze",[4,8,11],[12,11,16]);
    }
   }else if(x===-2){
    add(c,"refractory",[11,0,0],[16,10,16]);
    add(c,"lining",[10,2,0],[11,10,16]);
    add(c,"bronze",[9,10,0],[16,12,16]);
   }else if(x===2){
    add(c,"refractory",[0,0,0],[5,10,16]);
    add(c,"lining",[5,2,0],[6,10,16]);
    add(c,"bronze",[0,10,0],[7,12,16]);
    if(z===0){
     // The downpipe ends above the exposed actual east-edge output module.
     add(c,"riveted_iron",[1,0,5],[5,10,11]);
     add(c,"bronze",[1,8,3],[9,11,13]);
    }
   }else throw Error("Unrecognized perimeter "+[x,y,z]);
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
 if(renderAnchors.length!==6)throw Error("Expected 6 actual AIR weir/lip hosts, got "+renderAnchors.length);
 if(voxels.length>512)throw Error("Voxel collision too complex: "+voxels.length);
 // Ensure no decorative or collision volume overlaps genuine port/module cells.
 for(const letter of ["I","P","O"]){
  const c=cells.find(v=>v.k===letter);
  for(const b of voxels) if([0,1,2].every(i=>b[i]<(i===0?c.x+1:i===1?c.y+1:c.z+1)*16 &&
         b[i+3]>(i===0?c.x:i===1?c.y:c.z)*16))throw Error("Overlapping real "+letter+" module");
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
    if(from.some((v,i)=>v<0||to[i]>16||v>=to[i]))throw Error("Invalid facing geometry "+facing+" "+[c.x,c.y,c.z]);
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
