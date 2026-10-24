#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";
// Retort shell V2: deterministic block-local models and Voxel V2 profile.
// The JSON formation layout, module blocks and machine recipes are unchanged.
function makeAssets() {
 const files=new Map(),root="src/main/resources/",a=root+"assets/magneticraft2/",d=root+"data/magneticraft2/";
 const id="charcoal_retort",dirs=["south","west","north","east"];
 const map=[["SSS","SSS","SCS"],["BYB","BAB","UBO"],["BBB","BAB","BBB"],["PPP","PAP","PPP"]];
 const tex={stone:"magneticraft2:block/settling_tank/foundation",metal:"magneticraft2:block/settling_tank/riveted_iron",join:"magneticraft2:block/settling_tank/bronze_joint",bronze:"magneticraft2:block/settling_tank/bronze",brick:"magneticraft2:block/charcoal_retort/refractory",roof:"magneticraft2:block/charcoal_retort/roof",soot:"magneticraft2:block/charcoal_retort/soot",vent:"magneticraft2:block/charcoal_retort/grille",off:"magneticraft2:block/charcoal_retort/hatch_off",on:"magneticraft2:block/charcoal_retort/hatch_on",particle:"magneticraft2:block/charcoal_retort/refractory"};
 const cells=[];for(let y=0;y<4;y++)for(let zi=0;zi<3;zi++)for(let xi=0;xi<3;xi++)cells.push({x:xi-1,y,z:zi-2,k:map[y][zi][xi]});
 const shapes=new Map(),collision=[];const key=c=>[c.x,c.y,c.z].join();
 function add(c,m,f,t,solid=false){
  if(f.some((v,i)=>v<0||t[i]>16||v>=t[i]))throw Error("invalid box "+key(c));
  if(!shapes.has(key(c)))shapes.set(key(c),[]);
  shapes.get(key(c)).push({m,f,t});
  if(solid)collision.push([...f.map((v,i)=>v+[c.x,c.y,c.z][i]*16),...t.map((v,i)=>v+[c.x,c.y,c.z][i]*16)]);
 }
 function hitbox(c,f,t){collision.push([...f.map((v,i)=>v+[c.x,c.y,c.z][i]*16),...t.map((v,i)=>v+[c.x,c.y,c.z][i]*16)]);}
 for(const c of cells){
  const {x,y,z,k}=c;
  if(k==="S"){
   add(c,"stone",[0,0,0],[16,12,16]);add(c,"metal",[0,12,0],[16,15,16]);add(c,"join",[0,15,0],[16,16,16]);
   if(z===0){add(c,"metal",[0,1,15.4],[16,3,16]);if(x!==0)add(c,"join",[0,8,15.4],[16,10,16]);}
   hitbox(c,[0,0,0],[16,16,16]);
  }else if(k==="C"){
   add(c,"stone",[0,0,0],[16,15,10]);
   add(c,"stone",[0,0,10],[2,15,16]);add(c,"stone",[14,0,10],[16,15,16]);
   add(c,"metal",[2,0,10],[14,3,16]);add(c,"metal",[2,12,10],[14,15,16]);
   add(c,"soot",[3,3,10],[13,12,11]);add(c,"off",[3,3,11],[13,12,12]);
   add(c,"join",[2,3,12],[3,12,15.7]);add(c,"join",[13,3,12],[14,12,15.7]);
   add(c,"join",[6,1,15.5],[10,3,16]);add(c,"bronze",[7,0,15.6],[9,2,16]);add(c,"join",[0,15,0],[16,16,16]);
   hitbox(c,[0,0,0],[16,16,12]);
  }else if(k==="B"&&y===1&&x===0&&z===0){
   // Real inset stove face instead of a flush cube with bricks painted on.
   add(c,"brick",[0,0,0],[16,16,9]);
   add(c,"brick",[0,0,9],[3,16,16]);add(c,"brick",[13,0,9],[16,16,16]);
   add(c,"brick",[3,0,9],[13,3,16]);add(c,"brick",[3,13,9],[13,16,16]);
   add(c,"soot",[3,3,9],[13,13,11]);add(c,"metal",[3,3,11],[13,13,12]);add(c,"vent",[4,4,12],[12,12,13]);
   add(c,"join",[2.5,2.5,13],[4,13.5,15.5]);add(c,"join",[12,2.5,13],[13.5,13.5,15.5]);
   add(c,"join",[4,12.5,13],[12,14,15.5]);add(c,"join",[4,2,13],[12,3.5,15.5]);add(c,"bronze",[6,13.5,14],[10,15,15.5]);
   hitbox(c,[0,0,0],[16,16,12.5]);
  }else if(k==="B"&&y===2&&x===0&&z===0){
   // Narrow flue inspection grille above the central sealed door.
   add(c,"brick",[0,0,0],[16,16,12]);
   add(c,"brick",[0,0,12],[16,5,16]);add(c,"brick",[0,12,12],[16,16,16]);
   add(c,"brick",[0,5,12],[2,12,16]);add(c,"brick",[14,5,12],[16,12,16]);
   add(c,"soot",[2,5,12],[14,12,13]);add(c,"vent",[3,6,13],[13,11,14]);
   add(c,"join",[2,5,14],[3,12,16]);add(c,"join",[13,5,14],[14,12,16]);
   add(c,"bronze",[3,11,14],[13,12,16]);add(c,"metal",[3,4,14],[13,5,16]);
   hitbox(c,[0,0,0],[16,16,15]);
  }else if(k==="B"){
   const w=x===-1,e=x===1,n=z===-2,s=z===0;
   add(c,"brick",[w?.65:0,0,n?.65:0],[e?15.35:16,16,s?15.35:16]);
   if(w){add(c,"join",[0,0,0],[.65,2,16]);add(c,"join",[0,14,0],[.65,16,16]);if(z!==-1)add(c,"bronze",[0,2,0],[.65,14,2.25]);}
   if(e){add(c,"join",[15.35,0,0],[16,2,16]);add(c,"join",[15.35,14,0],[16,16,16]);if(z!==-1)add(c,"bronze",[15.35,2,0],[16,14,2.25]);}
   if(n){add(c,"join",[0,0,0],[16,2,.65]);add(c,"join",[0,14,0],[16,16,.65]);if(x!==0)add(c,"bronze",[0,2,0],[2.25,14,.65]);}
   if(s){add(c,"metal",[0,0,15.35],[16,2,16]);add(c,"bronze",[0,13.5,15.35],[16,16,16]);if(x!==0)add(c,"join",[0,2,15.35],[2.25,13.5,16]);}
   if(y===1&&z===-1){if(w)add(c,"metal",[0,10,2.25],[.65,11.5,16]);if(e)add(c,"metal",[15.35,10,2.25],[16,11.5,16]);}
   hitbox(c,[0,0,0],[16,16,16]);
  }else if(k==="P"){
   add(c,"brick",[0,0,0],[16,4,16]);add(c,"roof",[0,4,0],[16,11,16]);
   const w=x===-1,e=x===1,n=z===-2,s=z===0;
   if(w)add(c,"join",[0,11,0],[3,14,16]);
   if(e)add(c,"join",[13,11,0],[16,14,16]);
   if(n)add(c,"bronze",[w?3:0,11,0],[e?13:16,14,3]);
   if(s)add(c,"bronze",[w?3:0,11,13],[e?13:16,14,16]);
   if(x===0){add(c,"metal",[1,11,3],[2,12,13]);add(c,"metal",[14,11,3],[15,12,13]);}
   hitbox(c,[0,0,0],[16,12,16]);
  }else if(k==="A"&&y===3&&x===0&&z===-1){
   for(const [f,t] of [
    [[3,0,3],[5,16,13]],[[11,0,3],[13,16,13]],
    [[5,0,3],[11,16,5]],[[5,0,11],[11,16,13]]])add(c,"bronze",f,t,true);
   for(const [f,t] of [
    [[2,0,2],[3,3,14]],[[13,0,2],[14,3,14]],
    [[3,0,2],[13,3,3]],[[3,0,13],[13,3,14]]])add(c,"join",f,t);
   // Braced, hollow exhaust hood above the roof.
   for(const [f,t] of [
    [[3,13,3],[5,16,13]],[[11,13,3],[13,16,13]],
    [[5,13,3],[11,16,5]],[[5,13,11],[11,16,13]]])add(c,"metal",f,t);
  }
 }
 const rotation=(x,z,dir)=>dir==="south"?[x,z]:dir==="west"?[-z,x]:dir==="north"?[-x,-z]:[z,-x];
 const pixel=(x,z,dir)=>dir==="south"?[x,z]:dir==="west"?[16-z,x]:dir==="north"?[16-x,16-z]:[z,16-x];
 const uvs=(f,t)=>({north:[16-t[0],16-t[1],16-f[0],16-f[1]],south:[f[0],16-t[1],t[0],16-f[1]],east:[16-t[2],16-t[1],16-f[2],16-f[1]],west:[f[2],16-t[1],t[2],16-f[1]],up:[f[0],f[2],t[0],t[2]],down:[f[0],16-t[2],t[0],16-f[2]]});
 const json=o=>JSON.stringify(o)+"\n";
 files.set(d+"multiblock_voxels/"+id+".json",json({format:1,unit:"model_pixels",collision,selection:collision,zones:{}}));
 const states={variants:{}};
 for(const dir of dirs){
  const rotated=cells.map(c=>({...c,position:rotation(c.x,c.z,dir)}));
  for(const c of rotated){
   const sections=shapes.get(key(c));if(!sections?.length)continue;
   const [rx,rz]=c.position;
   const elements=sections.map(box=>{
    const corners=[[box.f[0],box.f[2]],[box.f[0],box.t[2]],[box.t[0],box.f[2]],[box.t[0],box.t[2]]].map(([x,z])=>pixel(x,z,dir));
    const f=[Math.min(...corners.map(v=>v[0])),box.f[1],Math.min(...corners.map(v=>v[1]))],
          t=[Math.max(...corners.map(v=>v[0])),box.t[1],Math.max(...corners.map(v=>v[1]))];
    if(f.some((v,i)=>v<0||t[i]>16||v>=t[i]))throw Error("invalid rotated element "+dir+" "+key(c));
    return {from:f,to:t,faces:Object.fromEntries(Object.entries(uvs(f,t)).map(([side,uv])=>[side,{texture:"#"+box.m,uv}]))};
   });
   const model={parent:"minecraft:block/block",ambientocclusion:false,textures:tex,elements};
   const modelpath=a+"models/multiblock/"+id+"_"+dir+"/part_"+rx+"_"+c.y+"_"+rz+".json";
   files.set(modelpath,json(model));
   if(c.k==="C"){
    const lit={...model,elements:model.elements.map(e=>({...e,faces:Object.fromEntries(Object.entries(e.faces).map(([side,val])=>[side,{...val,texture:val.texture==="#off"?"#on":val.texture}]))}))};
    files.set(modelpath.replace(".json","_lit.json"),json(lit));
    if(dir==="south"){
     files.set(a+"models/block/charcoal_retort_controller.json",json(model));
     files.set(a+"models/block/charcoal_retort_controller_lit.json",json(lit));
    }
   }
  }
  const yaw={south:0,west:90,north:180,east:270}[dir];
  for(const lit of [false,true]){
   const unfinished={model:"magneticraft2:block/charcoal_retort_controller"+(lit?"_lit":"")};
   if(yaw)unfinished.y=yaw;
   states.variants[`facing=${dir},is_formed=false,lit=${lit}`]=unfinished;
   states.variants[`facing=${dir},is_formed=true,lit=${lit}`]={model:`magneticraft2:multiblock/charcoal_retort_${dir}/part_0_0_0${lit?"_lit":""}`};
  }
 }
 files.set(a+"blockstates/"+id+".json",json(states));
 files.set(a+"models/item/"+id+".json",json({parent:"magneticraft2:block/charcoal_retort_controller"}));
 if(collision.length>100)throw Error("Excessive collision");
 if(cells.filter(c=>c.k==="A"&&shapes.has(key(c))).length!==1)throw Error("AIR proxies changed");
 for(const c of cells)if("UOY".includes(c.k)&&shapes.has(key(c)))throw Error("Real port blocked: "+key(c));
 return {files,stats:{files:files.size,models:[...files.keys()].filter(s=>s.includes("/models/multiblock/")).length,collision:collision.length}};
}
const {files,stats}=makeAssets();
const check=process.argv.includes("--check");
let errors=0;
for(const [relative,source] of files){
 const full=path.join(process.cwd(),relative);
 if(check){if(!fs.existsSync(full)||fs.readFileSync(full,"utf8")!==source){console.error("Mismatch: "+relative);errors++;}}
 else{fs.mkdirSync(path.dirname(full),{recursive:true});fs.writeFileSync(full,source);}
}
console.log("Charcoal Retort generated assets:",stats);
if(errors)process.exit(1);
