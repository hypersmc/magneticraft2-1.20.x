#!/usr/bin/env node
// Authored visual-only side belt and frame for the Bronze Stamp Mill.
// Keeps the signed Gear V2 transmission, both pulley centers, and all
// 5x3x4 blueprint/voxel geometry unchanged.
import fs from "node:fs";
import path from "node:path";
import {fileURLToPath} from "node:url";

const box=(material,from,to)=>({material,from:from.map(n=>+n.toFixed(5)),to:to.map(n=>+n.toFixed(5))});
// The leather rides just outside the actual 4.05px copper pulley rim.
// Near side: radial distance 4.08..5.05px rather than a .54px wire.
const run=length=>[
 box("belt",[7.25,0,2.95],[8.7,length,3.92]),
 box("belt",[7.25,0,12.08],[8.7,length,13.05])
];
function wrap(){
 const y=[8,8.72,9.44,10.16,10.88,11.6,12.3,13.05],inner=4.12,outer=5.05,parts=[];
 for(let i=0;i<y.length-1;i++){
  const low=y[i],high=y[i+1],dy=low-8;
  const ro=Math.sqrt(Math.max(0,outer*outer-dy*dy));
  const ri=dy>=inner?0:Math.sqrt(inner*inner-dy*dy);
  if(ri===0)parts.push(box("belt",[7.25,low,8-ro],[8.7,high,8+ro]));
  else{
   parts.push(box("belt",[7.25,low,8-ro],[8.7,high,8-ri]));
   parts.push(box("belt",[7.25,low,8+ri],[8.7,high,8+ro]));
  }
 }
 return parts;
}
// Dark uprights and bronze ties behind the leather, not decorative spokes.
// Lower brace meets the existing side bearing; upper brace meets the upper
// timber bridge. The strap and pulleys stay in front of these braces.
function guard(upper){
 const low=upper?0:6,high=upper?10.5:16;
 const parts=[
  box("dark",[6.8,low,1.75],[7.04,high,2.35]),
  box("dark",[6.8,low,13.65],[7.04,high,14.25])
 ];
 for(const [y0,y1]of upper?[[4.5,5.15],[10,10.5]]:[[6,6.7],[10.7,11.35]])
  parts.push(box("bronze",[6.79,y0,1.75],[7.05,y1,14.25]));
 parts.push(upper?
  box("wood",[6.6,10.42,7],[7.06,11,9]):
  box("wood",[6.9,4.5,7],[7.06,6.4,9]));
 return parts;
}
export const stampBeltSolids={
 bronze_stamp_mill_drive_belt_straight:run(16),
 bronze_stamp_mill_drive_belt_upper:run(14.5),
 bronze_stamp_mill_drive_belt_wrap:wrap(),
 bronze_stamp_mill_drive_belt_guard_lower:guard(false),
 bronze_stamp_mill_drive_belt_guard_upper:guard(true)
};
// Same face culling and unit-scale UVs as the current Stamp Mill drive
// builder, adding only the dedicated opaque leather texture key.
const compile=function compile(boxes){
const S={north:[2,-1,0,1],south:[2,1,0,1],west:[0,-1,2,1],east:[0,1,2,1],up:[1,1,0,2],down:[1,-1,0,2]},eps=1e-6,
textures={bronze:"minecraft:block/cut_copper",iron:"minecraft:block/iron_block",dark:"minecraft:block/polished_blackstone",wood:"minecraft:block/stripped_spruce_log",belt:"magneticraft2:block/bronze_stamp_mill_drive_belt",particle:"minecraft:block/cut_copper"},
uniq=a=>[...new Set(a.map(v=>+v.toFixed(5)))].sort((a,b)=>a-b);
for(const b of boxes)if(!textures[b.material]||b.from.some((v,i)=>v<0||v>=b.to[i]||b.to[i]>16))throw Error("Invalid geometry: "+JSON.stringify(b));
const patches=[];
for(let i=0;i<boxes.length;i++){const A=boxes[i],f=A.from,t=A.to;
 for(const [face,[axis,sign,u,v]]of Object.entries(S)){
  const plane=sign>0?t[axis]:f[axis],rel=[];
  for(let j=0;j<boxes.length;j++){if(i===j)continue;const B=boxes[j],o=B.from,p=B.to;
   if(Math.max(f[u],o[u])+eps>=Math.min(t[u],p[u])||Math.max(f[v],o[v])+eps>=Math.min(t[v],p[v]))continue;
   const q=plane+sign*.0001;if((o[axis]<q&&p[axis]>q)||(j>i&&Math.abs((sign>0?p[axis]:o[axis])-plane)<eps))rel.push({j,B});
  }
  const us=uniq([f[u],t[u],...rel.flatMap(x=>[Math.max(f[u],x.B.from[u]),Math.min(t[u],x.B.to[u])])]),vs=uniq([f[v],t[v],...rel.flatMap(x=>[Math.max(f[v],x.B.from[v]),Math.min(t[v],x.B.to[v])])]);
  for(let a=0;a<us.length-1;a++)for(let b=0;b<vs.length-1;b++){
   const[u0,u1,v0,v1]=[us[a],us[a+1],vs[b],vs[b+1]];
   if(u1-u0<eps||v1-v0<eps)continue;
   const mx=(u0+u1)/2,my=(v0+v1)/2,q=plane+sign*.0001;
   if(rel.some(({j,B})=>mx>B.from[u]+eps&&mx<B.to[u]-eps&&my>B.from[v]+eps&&my<B.to[v]-eps&&((B.from[axis]<q&&B.to[axis]>q)||(j>i&&Math.abs((sign>0?B.to[axis]:B.from[axis])-plane)<eps))))continue;
   patches.push({face,plane,material:A.material,u0,u1,v0,v1});
  }
 }
}
const groups=new Map();for(const p of patches){const k=`${p.face}:${p.plane.toFixed(5)}:${p.material}`;if(!groups.has(k))groups.set(k,[]);groups.get(k).push({...p})}
let panels=[];for(const g of groups.values()){let change=true;while(change){change=false;loop:for(let i=0;i<g.length;i++)for(let j=i+1;j<g.length;j++){
 const a=g[i],b=g[j];let m=null;
 if(Math.abs(a.v0-b.v0)<eps&&Math.abs(a.v1-b.v1)<eps&&(Math.abs(a.u1-b.u0)<eps||Math.abs(b.u1-a.u0)<eps))m={...a,u0:Math.min(a.u0,b.u0),u1:Math.max(a.u1,b.u1)};
 if(Math.abs(a.u0-b.u0)<eps&&Math.abs(a.u1-b.u1)<eps&&(Math.abs(a.v1-b.v0)<eps||Math.abs(b.v1-a.v0)<eps))m={...a,v0:Math.min(a.v0,b.v0),v1:Math.max(a.v1,b.v1)};
 if(m){g[i]=m;g.splice(j,1);change=true;break loop;}}}panels.push(...g)}
const elements=panels.map(p=>{const[axis,sign,u,v]=S[p.face],from=[0,0,0],to=[0,0,0];from[u]=p.u0;to[u]=p.u1;from[v]=p.v0;to[v]=p.v1;if(sign>0){from[axis]=p.plane-.0625;to[axis]=p.plane}else{from[axis]=p.plane;to[axis]=p.plane+.0625}return {from,to,faces:{[p.face]:{texture:"#"+p.material,uv:[p.u0,p.v0,p.u1,p.v1]}}}});
return {parent:"minecraft:block/block",ambientocclusion:false,textures,elements};
};
export function buildStampBeltModels(){
 const output={};
 for(const [name,solids]of Object.entries(stampBeltSolids)){
  const model=compile(solids);
  model.elements=model.elements.filter(e=>{
   const face=Object.keys(e.faces)[0];
   if(name.endsWith("_belt_straight")||name.endsWith("_belt_upper"))
    return face!=="up"&&face!=="down";
   if(name.endsWith("_guard_lower"))return face!=="up";
   if(name.endsWith("_guard_upper"))return face!=="down";
   return true;
  });
  output[name]=model;
 }
 return output;
}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)){
 let failures=0;
 for(const [name,model]of Object.entries(buildStampBeltModels())){
  const p=path.join("src/main/resources/assets/magneticraft2/models/multiblock",name+".json");
  const text=JSON.stringify(model,null,2)+"\n";
  if(process.argv.includes("--check")){
   if(!fs.existsSync(p)||fs.readFileSync(p,"utf8")!==text){
    console.error("Stale belt or support model:",p);
    failures++;
   }
  }else{
   fs.mkdirSync(path.dirname(p),{recursive:true});
   fs.writeFileSync(p,text,"utf8");
  }
  console.log(name+": "+model.elements.length+" visible panels");
 }
 if(failures)process.exitCode=1;
}
