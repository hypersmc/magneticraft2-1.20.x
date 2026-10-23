#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";
function makeAssets(){
 const files=new Map(),a="src/main/resources/assets/magneticraft2/",d="src/main/resources/data/magneticraft2/",id="water_settling_tank";
 const rows=[["SSISS","SSSSS","SSSSS","SSSSS","SPCSO"],["SSSSS","SAAAS","SAAAS","SAAAS","SAAAS"],["WAAAW","AAAAA","AAAAA","AAAAA","WAAAW"]];
 const types={S:"minecraft:stone_bricks",W:"minecraft:oak_planks",A:"minecraft:air",I:"magneticraft2:multiblock_fluid_input",P:"magneticraft2:multiblock_item_primary_output",C:"magneticraft2:water_settling_tank",O:"magneticraft2:multiblock_fluid_output"};
 const tex={stone:"minecraft:block/stone_bricks",bronze:"magneticraft2:block/material/bronze_cast",wood:"magneticraft2:block/material/machine_timber",iron:"minecraft:block/iron_block",particle:"minecraft:block/stone_bricks"};
 const cells=[];
 for(let y=0;y<3;y++)for(let rz=0;rz<5;rz++)for(let rx=0;rx<5;rx++)cells.push({x:rx-2,y,z:rz-4,c:rows[y][rz][rx]});
 for(const [k,expected] of Object.entries({C:[0,0,0],I:[0,0,-4],P:[-1,0,0],O:[2,0,0]})){
   const entries=cells.filter(c=>c.c===k);if(entries.length!==1||[entries[0].x,entries[0].y,entries[0].z].join()!=expected.join())throw Error("Missing or displaced "+k);
 }
 const boxes=new Map(),global=[];
 const add=(c,mat,lo,hi)=>{
  if(lo.some((v,i)=>v<0||hi[i]>16||hi[i]<=v))throw Error("illegal local box");
  const key=[c.x,c.y,c.z].join();if(!boxes.has(key))boxes.set(key,[]);
  boxes.get(key).push({mat,lo,hi});
  global.push([...lo.map((v,i)=>v+[c.x,c.y,c.z][i]*16),...hi.map((v,i)=>v+[c.x,c.y,c.z][i]*16)]);
 };
 for(const c of cells) {
  const {x,y,z}=c;
  if(y===0&&c.c==="S"){
   // Full-height support under the side walls. The previous 12px footing
   // left a visible unsupported 4px gap below all upper reservoir walls.
   add(c,"stone",[0,0,0],[16,14,16]);
   add(c,"bronze",[0,14,0],[16,16,16]);
  }else if(y===0&&c.c==="C"){
   add(c,"stone",[0,0,0],[16,13,16]);
   add(c,"bronze",[1,13,1],[15,16,15]);
  }else if(y===1&&c.c==="S"){
   if(z===-4) {add(c,"stone",[0,0,0],[16,14,4]);add(c,"bronze",[0,14,0],[16,16,4]);}
   else if(x===-2){add(c,"stone",[12,0,0],[16,14,16]);add(c,"bronze",[12,14,0],[16,16,16]);}
   else if(x===2){add(c,"stone",[0,0,0],[4,14,16]);add(c,"bronze",[0,14,0],[4,16,16]);}
   else throw Error("bad wall");
  }else if(y===2&&c.c==="W"){
   const xx=x===-2?[12,16]:[0,4],zz=z===-4?[0,4]:[12,16];
   add(c,"wood",[xx[0],0,zz[0]],[xx[1],13,zz[1]]);
   add(c,"bronze",[xx[0],13,zz[0]],[xx[1],15,zz[1]]);
  }else if(y===1&&c.c==="A"&&Math.abs(x)<=1&&(z===-2||z===-1)){
   const lo=z===-2?12:14,top=z===-2?9:14;
   add(c,"stone",[0,0,lo],[16,top,16]);
   add(c,"bronze",[0,top,lo],[16,top+2,16]);
  }
 }
 const hosts=cells.filter(c=>c.c!=="A"||boxes.has([c.x,c.y,c.z].join()));
 const anchors=cells.filter(c=>c.c==="A"&&boxes.has([c.x,c.y,c.z].join())).map(c=>[c.x,c.y,c.z]);
 if(anchors.length!==6)throw Error("Expected six real baffle/front-lip AIR anchors");
 files.set(d+"multiblock_voxels/"+id+".json",JSON.stringify({format:1,unit:"model_pixels",collision:global,selection:global,zones:{}},null,2)+"\n");
 const rotate=(x,z,f)=>f==="south"?[x,z]:f==="west"?[-z,x]:f==="north"?[-x,-z]:[z,-x];
 const point=(x,z,f)=>f==="south"?[x,z]:f==="west"?[16-z,x]:f==="north"?[16-x,16-z]:[z,16-x];
 const faces=(lo,hi,mat)=>{
   const dx=hi[0]-lo[0],dy=hi[1]-lo[1],dz=hi[2]-lo[2];
   const p=(x,y)=>({texture:"#"+mat,uv:[0,0,x,y]});
   return {north:p(dx,dy),south:p(dx,dy),east:p(dz,dy),west:p(dz,dy),up:p(dx,dz),down:p(dx,dz)};
 };
 const states={variants:{}};
 for(const f of ["south","west","north","east"]) {
  const rc=cells.map(c=>({...c,r:rotate(c.x,c.z,f)})),minX=Math.min(...rc.map(c=>c.r[0])),minZ=Math.min(...rc.map(c=>c.r[1]));
  const l=Array.from({length:3},()=>Array.from({length:5},()=>Array(5).fill("")));
  for(const c of rc)l[c.y][c.r[1]-minZ][c.r[0]-minX]=c.c;
  const bp={name:id+"_"+f,structure:{dimensions:[5,3,5],layout:Object.fromEntries(l.map((a,y)=>["layer"+(y+1),a])),blocks:types},
   settings:{replacewhenformed:"multiblock/"+id+"_"+f,inventory:true,power:false,fuel:false,fluid:true,inputs:[],outputs:[],
    render_air_anchors:anchors.map(([x,y,z])=>{const [rx,rz]=rotate(x,z,f);return [rx,y,rz]}),voxel_profile:"magneticraft2:"+id}};
  files.set(d+"multiblocks/"+id+"_"+f+".json",JSON.stringify(bp,null,2)+"\n");
  for(const c of hosts){
   if("IPO".includes(c.c))continue;
   const key=[c.x,c.y,c.z].join(),elements=[];
   for(const b of boxes.get(key)||[]){
    const lo=b.lo.map((v,i)=>v+[c.x,c.y,c.z][i]*16),hi=b.hi.map((v,i)=>v+[c.x,c.y,c.z][i]*16);
    const corners=[[lo[0],lo[2]],[lo[0],hi[2]],[hi[0],lo[2]],[hi[0],hi[2]]].map(q=>point(q[0],q[1],f));
    const [rx,rz]=rotate(c.x,c.z,f);
    const from=[Math.min(...corners.map(p=>p[0]))-rx*16,b.lo[1],Math.min(...corners.map(p=>p[1]))-rz*16];
    const to=[Math.max(...corners.map(p=>p[0]))-rx*16,b.hi[1],Math.max(...corners.map(p=>p[1]))-rz*16];
    if(from.some((v,i)=>v<-.001||to[i]>16.001||v>=to[i]))throw Error("Bad rotated box at "+key+" / "+f);
    elements.push({from,to,faces:faces(from,to,b.mat)});
   }
   if(!elements.length)throw Error("Non-module host missing static model "+key);
   const [rx,rz]=rotate(c.x,c.z,f);
   files.set(a+"models/multiblock/"+id+"_"+f+"/part_"+rx+"_"+c.y+"_"+rz+".json",
     JSON.stringify({parent:"minecraft:block/block",ambientocclusion:false,textures:tex,elements},null,2)+"\n");
  }
  const yaw={south:0,west:90,north:180,east:270}[f];
  states.variants["facing="+f+",is_formed=false"]={model:"magneticraft2:block/"+id+"_controller",y:yaw};
  states.variants["facing="+f+",is_formed=true"]={model:"magneticraft2:multiblock/"+id+"_"+f+"/part_0_0_0"};
 }
 files.set(a+"blockstates/"+id+".json",JSON.stringify(states,null,2)+"\n");
 files.set(a+"models/block/"+id+"_controller.json",JSON.stringify({parent:"minecraft:block/cube_all",textures:{all:"magneticraft2:block/material/bronze_cast"}},null,2)+"\n");
 files.set(a+"models/item/"+id+".json",JSON.stringify({parent:"magneticraft2:block/"+id+"_controller"},null,2)+"\n");
 const lumps=[[2,5,3,7,8,8],[7,4,5,13,9,12],[4,5,10,10,7,14]];
 files.set(a+"models/item/settled_sediment.json",JSON.stringify({
  parent:"minecraft:block/block",textures:{mud:"minecraft:block/mud",grit:"minecraft:block/gravel",particle:"minecraft:block/mud"},
  elements:lumps.map((b,i)=>{const lo=b.slice(0,3),hi=b.slice(3);return {from:lo,to:hi,faces:faces(lo,hi,i===1?"grit":"mud")}}),
  display:{ground:{scale:[0.7,0.7,0.7]},gui:{rotation:[25,225,0],scale:[0.85,0.85,0.85]}}
 },null,2)+"\n");
 const crafting={
  type:"minecraft:crafting_shaped",pattern:["SBS","PCP","SFS"],
  key:{S:{item:"minecraft:stone_bricks"},B:{item:"magneticraft2:bronze_ingot"},P:{item:"magneticraft2:copper_plate"},
       C:{item:"magneticraft2:copper_fluid_tank"},F:{item:"magneticraft2:multiblock_fluid_output"}},
  result:{item:"magneticraft2:"+id}};
 files.set(d+"recipes/"+id+".json",JSON.stringify(crafting,null,2)+"\n");
 files.set(d+"recipes/settling_dirty_water.json",JSON.stringify({
  type:"magneticraft2:multiblock_processing",machine:"magneticraft2:"+id,
  fluid_input:{fluid:"magneticraft2:dirty_water",amount:600},
  fluid_output:{fluid:"minecraft:water",amount:480},
  output:{item:"magneticraft2:settled_sediment",count:1},process_time:240
 },null,2)+"\n");
 files.set(d+"recipes/settled_sediment_to_clay.json",JSON.stringify({
  type:"minecraft:crafting_shaped",pattern:["SS","SS"],key:{S:{item:"magneticraft2:settled_sediment"}},
  result:{item:"minecraft:clay_ball",count:1}
 },null,2)+"\n");
 files.set(d+"loot_tables/blocks/"+id+".json",JSON.stringify({type:"minecraft:block",pools:[{rolls:1,
  entries:[{type:"minecraft:item",name:"magneticraft2:"+id}],conditions:[{condition:"minecraft:survives_explosion"}]}]},null,2)+"\n");
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
  fs.mkdirSync(path.dirname(file),{recursive:true});fs.writeFileSync(file,content);
 }
}
console.log("Water Settling Tank source-generated assets:",files.size);
if(mismatches)process.exit(1);
