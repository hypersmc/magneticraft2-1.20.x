#!/usr/bin/env node
// Authored Stamp Mill drive-only geometry. No changes to accepted machines.
import fs from "node:fs";
import path from "node:path";
const designs=(function designs(){
const b=(material,from,to)=>({material,from:from.map(n=>+n.toFixed(5)),to:to.map(n=>+n.toFixed(5))});
function gear(axis,phase){
 const boxes=[],map=(a,u,v)=>axis==="z"?[u,v,a]:[a,v,u];
 const add=(m,al,ah,ul,uh,vl,vh)=>boxes.push(b(m,map(al,ul,vl),map(ah,uh,vh)));
 const ring=(m,lo,hi,r,h)=>{add(m,lo,hi,8-r,8+r,8+h,8+r);add(m,lo,hi,8-r,8+r,8-r,8-h);add(m,lo,hi,8-r,8-h,8-h,8+h);add(m,lo,hi,8+h,8+r,8-h,8+h)};
 ring("bronze",12.2,13.1,2.65,1.16);
 for(const u of [-1,1])for(const v of [-1,1])
  add("bronze",12.2,13.1,u<0?5.45:9.15,u<0?6.85:10.55,v<0?5.45:9.15,v<0?6.85:10.55);
 ring("dark",13.1,13.38,2.12,1.10);
 ring("bronze",10.6,12.2,1.35,.94);
 ring("iron",11.2,11.75,1.55,1.35);
 for(let i=0;i<8;i++){
  const a=(i*45+phase)*Math.PI/180,c=Math.cos(a),s=Math.sin(a);
  for(const[m,r0,r1,h]of [["bronze",2.48,3.56,.40],["iron",3.52,3.80,.20]])
   add(m,12.06,12.94,8+Math.min(r0*c,r1*c)-h,8+Math.max(r0*c,r1*c)+h,8+Math.min(r0*s,r1*s)-h,8+Math.max(r0*s,r1*s)+h);
 }
 return boxes;
}
function collar(out,x0,x1){
 out.push(b("bronze",[x0,5.3,5.3],[x1,7.02,10.7]),b("bronze",[x0,8.98,5.3],[x1,10.7,10.7]));
 out.push(b("bronze",[x0,7.02,5.3],[x1,8.98,7.02]),b("bronze",[x0,7.02,8.98],[x1,8.98,10.7]));
}
const gearbox=[
 b("wood",[2,0,2.2],[15.65,1.8,3.45]),b("wood",[2,0,12.4],[15.65,1.8,13.65])
];
for(const x of [2.3,14.2])for(const z of [2.2,12.4])gearbox.push(b("wood",[x,0,z],[x+1.4,3,z+1.25]));
for(const z of [2.1,12.2])gearbox.push(b("wood",[14.2,2.3,z],[15.6,16,z+1.3]));
gearbox.push(b("iron",[14.2,13.6,3.4],[15.6,15.8,12.2]));
gearbox.push(b("bronze",[14.2,11.35,2.1],[15.6,12.5,13.5]),b("bronze",[14.2,10,6.65],[15.6,11.35,9.35]));
collar(gearbox,14.2,15.6);
const cross=[];collar(cross,7.15,8.85);
for(const z of [1.7,12.25])cross.push(b("iron",[7.15,10.55,z],[8.85,15.8,z+2.05]));
cross.push(b("iron",[7.15,14.9,1.7],[8.85,16,14.3]),b("bronze",[7.15,10.55,5.7],[8.85,13,10.3]));
const side=[];collar(side,5.8,6.9);
side.push(b("iron",[5.8,10.65,6.4],[6.9,13.6,9.6]),b("bronze",[5.8,12.5,5.65],[6.9,13.6,10.35]));
return {
 bronze_stamp_mill_drive_bevel:gear("z",0),
 bronze_stamp_mill_drive_bevel_x:gear("x",22.5),
 bronze_stamp_mill_drive_input_spindle:[b("iron",[7.3,7.3,8],[8.7,8.7,14.2]),b("bronze",[7.7,8.7,8],[8.3,8.9,14.2])],
 bronze_stamp_mill_drive_lower_spindle:[b("iron",[2.5,7.3,7.3],[16,8.7,8.7]),b("bronze",[2.5,8.7,7.7],[16,8.9,8.3])],
 bronze_stamp_mill_drive_gearbox_frame:gearbox,
 bronze_stamp_mill_drive_cross_bearing:cross,
 bronze_stamp_mill_drive_side_lower_mount:side
};
})();
const compile=function compile(boxes){
const S={north:[2,-1,0,1],south:[2,1,0,1],west:[0,-1,2,1],east:[0,1,2,1],up:[1,1,0,2],down:[1,-1,0,2]},eps=1e-6,
textures={bronze:"minecraft:block/cut_copper",iron:"minecraft:block/iron_block",dark:"minecraft:block/polished_blackstone",wood:"minecraft:block/stripped_spruce_log",particle:"minecraft:block/cut_copper"},
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
const check=process.argv.includes("--check");
let errors=0;
for(const [name,boxes]of Object.entries(designs)){
 const model=compile(boxes),output=JSON.stringify(model,null,2)+"\n";
 const file=path.join(process.cwd(),"src/main/resources/assets/magneticraft2/models/multiblock",name+".json");
 if(check){if(!fs.existsSync(file)||fs.readFileSync(file,"utf8")!==output){console.error("Outdated drive model:",name);errors++;}}
 else {fs.mkdirSync(path.dirname(file),{recursive:true});fs.writeFileSync(file,output,"utf8");}
 console.log(name+": "+boxes.length+" solids -> "+model.elements.length+" visible panels");
}
if(errors)process.exit(1);
