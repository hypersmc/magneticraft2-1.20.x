#!/usr/bin/env node
// Formed Stamp Mill strike faces, cosmetic only. No blueprint or voxel edit.
import fs from "node:fs";
import path from "node:path";
import {fileURLToPath} from "node:url";
export const stampStrikeBoxes=[
  {
    "material": "dark",
    "from": [
      2.7,
      8.05,
      4.65
    ],
    "to": [
      13.3,
      8.25,
      11.35
    ]
  },
  {
    "material": "iron",
    "from": [
      3.18,
      8.2,
      5.05
    ],
    "to": [
      12.82,
      8.47,
      10.95
    ]
  },
  {
    "material": "bronze",
    "from": [
      2.72,
      8.24,
      4.75
    ],
    "to": [
      3.22,
      8.58,
      5.35
    ]
  },
  {
    "material": "bronze",
    "from": [
      12.78,
      8.24,
      4.75
    ],
    "to": [
      13.28,
      8.58,
      5.35
    ]
  },
  {
    "material": "bronze",
    "from": [
      2.72,
      8.24,
      10.65
    ],
    "to": [
      3.22,
      8.58,
      11.25
    ]
  },
  {
    "material": "bronze",
    "from": [
      12.78,
      8.24,
      10.65
    ],
    "to": [
      13.28,
      8.58,
      11.25
    ]
  },
  {
    "material": "dark",
    "from": [
      4.35,
      8.45,
      6.94
    ],
    "to": [
      11.65,
      8.53,
      7.18
    ]
  },
  {
    "material": "dark",
    "from": [
      4.35,
      8.45,
      8.82
    ],
    "to": [
      11.65,
      8.53,
      9.06
    ]
  }
];
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
export function buildStrikePlate(){return compile(stampStrikeBoxes);}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)){
 const name="src/main/resources/assets/magneticraft2/models/multiblock/bronze_stamp_mill_strike_plate.json";
 const expected=JSON.stringify(buildStrikePlate(),null,2)+"\n";
 if(process.argv.includes("--check")){
  if(!fs.existsSync(name)||fs.readFileSync(name,"utf8")!==expected){
   console.error("Stamp Mill strike plate is stale:",name);
   process.exitCode=1;
  }
 }else{
  fs.mkdirSync(path.dirname(name),{recursive:true});
  fs.writeFileSync(name,expected,"utf8");
 }
 console.log("Stamp Mill strike faces:",buildStrikePlate().elements.length);
}
