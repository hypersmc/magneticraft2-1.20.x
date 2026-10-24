#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";
import {applyRetortV3,finalizeRetortV3,textureRetortChimneyInteriors} from "./v3-models.mjs";
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

/**
 * Repair malformed V2 retort geometry at asset-build time.
 * Later decorative pieces carve their volumes out of underlying solids.
 * Surface quads are emitted only where actually visible, including at
 * adjacent V2 filler boundaries. This does NOT move construction blocks.
 */
function repairRetortMeshes(files){
 const EPS=1e-6;
 const base="src/main/resources/assets/magneticraft2/";
 const modelPart=/^models\/multiblock\/charcoal_retort_(south|west|north|east)\/part_(-?\d+)_(-?\d+)_(-?\d+)(_lit)?\.json$/;
 const controller=/^models\/block\/charcoal_retort_controller(?:_lit)?\.json$/;
 const sides={
  north:[2,0,1,-1],south:[2,0,1,1],west:[0,2,1,-1],
  east:[0,2,1,1],down:[1,0,2,-1],up:[1,0,2,1]
 };
 const near=(a,b)=>Math.abs(a-b)<EPS;
 function cut3(a,b){
  const f=a.from,t=a.to;
  const l=f.map((v,i)=>Math.max(v,b.from[i])),h=t.map((v,i)=>Math.min(v,b.to[i]));
  if(l.some((v,i)=>h[i]-v<=EPS))return[a];
  const out=[];
  function append(p,q){if(p.every((v,i)=>q[i]-v>EPS))out.push({...a,from:p,to:q});}
  append([f[0],f[1],f[2]],[l[0],t[1],t[2]]);
  append([h[0],f[1],f[2]],[t[0],t[1],t[2]]);
  append([l[0],f[1],f[2]],[h[0],l[1],t[2]]);
  append([l[0],h[1],f[2]],[h[0],t[1],t[2]]);
  append([l[0],l[1],f[2]],[h[0],h[1],l[2]]);
  append([l[0],l[1],h[2]],[h[0],h[1],t[2]]);
  return out;
 }
 function cut2(r,b){
  const x=Math.max(r[0],b[0]),y=Math.max(r[1],b[1]),
        u=Math.min(r[2],b[2]),v=Math.min(r[3],b[3]);
  if(u-x<=EPS||v-y<=EPS)return[r];
  const out=[];
  if(x-r[0]>EPS)out.push([r[0],r[1],x,r[3]]);
  if(r[2]-u>EPS)out.push([u,r[1],r[2],r[3]]);
  if(y-r[1]>EPS)out.push([x,r[1],u,y]);
  if(r[3]-v>EPS)out.push([x,v,u,r[3]]);
  return out;
 }
 function uvs(f,t){
  return {
   north:[16-t[0],16-t[1],16-f[0],16-f[1]],
   south:[f[0],16-t[1],t[0],16-f[1]],
   east:[16-t[2],16-t[1],16-f[2],16-f[1]],
   west:[f[2],16-t[1],t[2],16-f[1]],
   up:[f[0],f[2],t[0],t[2]],
   down:[f[0],16-t[2],t[0],16-f[2]]
  };
 }
 function resolve(boxes){
  let solids=[];
  for(const e of boxes){
   const materials=[...new Set(Object.values(e.faces).map(f=>f.texture))];
   if(materials.length!==1)throw Error("Retort box contains mixed textures");
   const next={from:e.from,to:e.to,material:materials[0]};
   solids=solids.flatMap(old=>cut3(old,next));
   solids.push(next);
  }
  for(let i=0;i<solids.length;i++)for(let j=i+1;j<solids.length;j++){
   const a=solids[i],b=solids[j];
   if(a.from.every((v,k)=>Math.min(a.to[k],b.to[k])-Math.max(v,b.from[k])>EPS))
    throw Error("Retort boxes still intersect after geometry repair");
  }
  return solids;
 }
 const all=new Map(),ordinary=new Map();
 for(const [path,content]of files){
  if(!path.startsWith(base))continue;
  const tail=path.slice(base.length),m=modelPart.exec(tail);
  if(!m&&!controller.test(tail))continue;
  const model=JSON.parse(content);
  const cell=m?[+m[2],+m[3],+m[4]]:null;
  const entry={path,model,cell,dir:m?.[1]||null,boxes:resolve(model.elements)};
  all.set(path,entry);
  if(m&&!m[5])ordinary.set(m[1]+":"+cell.join(","),entry);
 }
 let totalQuads=0;
 for(const entry of all.values()){
  const candidates=[...entry.boxes];
  if(entry.cell)for(const shift of [[1,0,0],[-1,0,0],[0,1,0],[0,-1,0],[0,0,1],[0,0,-1]]){
   const coord=entry.cell.map((v,i)=>v+shift[i]);
   const neighbor=ordinary.get(entry.dir+":"+coord.join(","));
   if(!neighbor)continue;
   for(const b of neighbor.boxes)candidates.push({
    from:b.from.map((v,i)=>v+shift[i]*16),
    to:b.to.map((v,i)=>v+shift[i]*16)
   });
  }
  const elements=[];
  for(const box of entry.boxes){
   const whole={};
   for(const [side,[axis,u,v,direction]] of Object.entries(sides)){
    const plane=direction===1?box.to[axis]:box.from[axis];
    const original=[box.from[u],box.from[v],box.to[u],box.to[v]];
    let visible=[original];
    for(const other of candidates){
     if(other===box)continue;
     if(!near(direction===1?other.from[axis]:other.to[axis],plane))continue;
     const coverage=[other.from[u],other.from[v],other.to[u],other.to[v]];
     visible=visible.flatMap(r=>cut2(r,coverage));
     if(!visible.length)break;
    }
    if(visible.length===1&&visible[0].every((z,i)=>near(z,original[i]))){
     whole[side]={texture:box.material,uv:uvs(box.from,box.to)[side]};
     totalQuads++;
    } else for(const rect of visible){
     const f=[...box.from],t=[...box.to];
     f[u]=rect[0];t[u]=rect[2];f[v]=rect[1];t[v]=rect[3];
     elements.push({from:f,to:t,faces:{[side]:{
      texture:box.material,uv:uvs(f,t)[side]
     }}});
     totalQuads++;
    }
   }
   if(Object.keys(whole).length)elements.push({from:box.from,to:box.to,faces:whole});
  }
  if(!elements.length)throw Error("Retort part has no visible faces "+entry.path);
  for(const e of elements)if(e.from.some((v,i)=>v<-EPS||e.to[i]>16+EPS||e.to[i]-v<=EPS))
   throw Error("Retort part protrudes into adjacent real block "+entry.path);
  entry.model.elements=elements;
  files.set(entry.path,JSON.stringify(entry.model)+"\n");
 }
 return {modelCount:all.size,visibleQuads:totalQuads};
}

const {files,stats}=makeAssets();
repairRetortMeshes(files);
const v3=applyRetortV3(files);
repairRetortMeshes(files);
textureRetortChimneyInteriors(files);
finalizeRetortV3(files,v3.ports);
const check=process.argv.includes("--check");
let errors=0;
for(const [relative,source] of files){
 const full=path.join(process.cwd(),relative);
 if(check){if(!fs.existsSync(full)||fs.readFileSync(full,"utf8")!==source){console.error("Mismatch: "+relative);errors++;}}
 else{fs.mkdirSync(path.dirname(full),{recursive:true});fs.writeFileSync(full,source);}
}
console.log("Charcoal Retort generated assets:",stats);
if(errors)process.exit(1);
