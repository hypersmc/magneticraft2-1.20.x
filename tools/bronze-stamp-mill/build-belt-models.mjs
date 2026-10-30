#!/usr/bin/env node
// Flat leather transmission for the Bronze Stamp Mill, built as one
// continuous loop from straight bands and two supported half-wraps.
// Static blueprint, shafts, speed and accepted Lathe remain untouched.
import fs from "node:fs";
import path from "node:path";
import {fileURLToPath} from "node:url";
export const stampBeltSolids=(function newBeltGeometry(){
 const r=n=>+n.toFixed(5),box=(material,from,to)=>({material,from:from.map(r),to:to.map(r)});
 const INNER=4.08,OUTER=5.45,X0=7.13,X1=8.72;
 const run=height=>[
   box("belt",[X0,0,8-OUTER],[X1,height,8-INNER]),
   box("belt",[X0,0,8+INNER],[X1,height,8+OUTER])
 ];
 // One semicircular leather band over each drum. The lower copy is
 // rotated 180 degrees by the renderer. Facets are intentionally
 // coarse enough to look like Minecraft, not stretched rods.
 const cap=()=>{
  const cuts=[8,8.70,9.40,10.10,10.80,11.50,12.20,12.90,13.45],out=[];
  for(let i=0;i<cuts.length-1;i++){
   const y0=cuts[i],y1=cuts[i+1];
   const shell=Math.max(.24,Math.sqrt(Math.max(0,OUTER*OUTER-(y1-8)**2)));
   const hole=y0-8>=INNER?0:Math.sqrt(Math.max(0,INNER*INNER-(y0-8)**2));
   if(shell<hole+.06)throw Error("A leather half-wrap disconnected at "+y0);
   if(hole<=.05)out.push(box("belt",[X0,y0,8-shell],[X1,y1,8+shell]));
   else{
    out.push(box("belt",[X0,y0,8-shell],[X1,y1,8-hole]));
    out.push(box("belt",[X0,y0,8+hole],[X1,y1,8+shell]));
   }
  }
  return out;
 };
 // Solid copper-rimmed spruce drive drum. It replaces the disconnected
 // looking exposed spoke/sprocket wheel, NOT its axle, pivot or phase.
 const drum=()=>{
  const out=[],cuts=[4.06,4.8,5.6,6.7,8,9.3,10.4,11.2,11.94];
  for(let i=0;i<cuts.length-1;i++){
   const y0=cuts[i],y1=cuts[i+1];
   const radialFar=Math.max(Math.abs(y0-8),Math.abs(y1-8));
   const radialNear=y0<=8&&y1>=8?0:Math.min(Math.abs(y0-8),Math.abs(y1-8));
   const outer=Math.sqrt(Math.max(0,3.98**2-radialFar**2));
   const wood=Math.sqrt(Math.max(0,3.42**2-radialFar**2));
   const inner=radialNear>=3.20?0:Math.sqrt(Math.max(0,3.20**2-radialNear**2));
   if(wood>.05)out.push(box("wood",[7.34,y0,8-wood],[8.40,y1,8+wood]));
   if(outer>.10){
    if(inner<.04)out.push(box("bronze",[7.22,y0,8-outer],[8.58,y1,8+outer]));
    else if(inner<outer-.05){
     out.push(box("bronze",[7.22,y0,8-outer],[8.58,y1,8-inner]));
     out.push(box("bronze",[7.22,y0,8+inner],[8.58,y1,8+outer]));
    }
   }
  }
  out.push(box("iron",[8.4,7.11,7.11],[8.65,8.89,8.89]));
  out.push(box("bronze",[8.6,7.48,7.48],[8.74,8.52,8.52]));
  for(const [y,z] of [[4.55,8],[11.45,8],[8,4.55],[8,11.45]]){
   out.push(box("bronze",[8.48,y-.20,z-.20],[8.68,y+.20,z+.20]));
  }
  return out;
 };
 return{
  bronze_stamp_mill_drive_belt_straight:run(16),
  bronze_stamp_mill_drive_belt_upper:run(14.5),
  bronze_stamp_mill_drive_belt_wrap:cap(),
  bronze_stamp_mill_drive_pulley:drum()
 };
})();
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
  const out={};
  for(const [name,parts]of Object.entries(stampBeltSolids)){
    const model=compile(parts);
    model.elements=model.elements.filter(e=>{
      const face=Object.keys(e.faces)[0];
      if(name.endsWith("_belt_straight")||name.endsWith("_belt_upper"))
        return face!=="up"&&face!=="down";
      if(name.endsWith("_belt_wrap"))return face!=="down";
      return true;
    });
    out[name]=model;
  }
  return out;
}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)){
  let bad=0;
  for(const [name,model]of Object.entries(buildStampBeltModels())){
    const file=path.join("src/main/resources/assets/magneticraft2/models/multiblock",name+".json");
    const expected=JSON.stringify(model,null,2)+"\n";
    if(process.argv.includes("--check")){
      if(!fs.existsSync(file)||fs.readFileSync(file,"utf8")!==expected){console.error("Outdated belt/pulley:",name);bad++;}
    }else{fs.mkdirSync(path.dirname(file),{recursive:true});fs.writeFileSync(file,expected,"utf8");}
    console.log(name+": "+model.elements.length+" visible panels");
  }
  if(bad)process.exitCode=1;
}
