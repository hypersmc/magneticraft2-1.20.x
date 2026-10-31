function modelBox(from,to,material,sides,dir){
 const pix=(x,z)=>dir==="south"?[x,z]:dir==="west"?[16-z,x]:dir==="north"?[16-x,16-z]:[z,16-x];
 const faceMap={south:{north:"north",south:"south",east:"east",west:"west"},west:{north:"east",south:"west",east:"north",west:"south"},north:{north:"south",south:"north",east:"west",west:"east"},east:{north:"west",south:"east",east:"south",west:"north"}};
 const corners=[[from[0],from[2]],[from[0],to[2]],[to[0],from[2]],[to[0],to[2]]].map(([x,z])=>pix(x,z));
 const f=[Math.min(...corners.map(v=>v[0])),from[1],Math.min(...corners.map(v=>v[1]))];
 const t=[Math.max(...corners.map(v=>v[0])),to[1],Math.max(...corners.map(v=>v[1]))];
 if(f.some((v,i)=>v<-1e-7||t[i]>16+1e-7||t[i]-v<0.01))throw Error("Outside model cell: "+JSON.stringify({from,to,f,t}));
 const uv=(side)=>side==="north"?[16-t[0],16-t[1],16-f[0],16-f[1]]:side==="south"?[f[0],16-t[1],t[0],16-f[1]]:side==="east"?[16-t[2],16-t[1],16-f[2],16-f[1]]:side==="west"?[f[2],16-t[1],t[2],16-f[1]]:side==="up"?[f[0],f[2],t[0],t[2]]:[f[0],16-t[2],t[0],16-f[2]];
 const faces={};
 for(const side of sides){
  const world=side==="up"||side==="down"?side:faceMap[dir][side];
  faces[world]={texture:"#"+material,uv:uv(world)};
 }
 return {from:f,to:t,faces};
}

const tex=Object.fromEntries(['brick','foundation','refractory','roof','iron',
 'bronze','soot','ember','grate','input','output'].map(k=>[k,
 'magneticraft2:block/ore_roasting_kiln/'+k]));
tex.particle=tex.brick;
const solid=new Set(['S','B','C','U','O']);
const delta={north:[0,0,-1],south:[0,0,1],east:[1,0,0],
 west:[-1,0,0],up:[0,1,0],down:[0,-1,0]};
export function roofTop(x,z){
 if(x===0&&(z===-8||z===-6))return 16;
 if(Math.abs(x)===1&&z===-7)return 16;
 return Math.abs(x)===2?7:Math.abs(x)===1?11:15;
}
export function roastVisual(c,d,lookup){
 const {x,y,z,k}=c,E=[];
 const add=(a,b,m,faces)=>E.push(modelBox(a,b,m,faces,d));
 const out=Object.keys(delta).filter(f=>{
  const t=delta[f],n=lookup.get([x+t[0],y+t[1],z+t[2]].join(','));
  return !n||!solid.has(n.k);
 });
 const side=Math.abs(x)===2,front=z===0;
 if(y===0){
  for(const f of out)add([0,0,0],[16,16,16],'foundation',[f]);
  if(front&&x===0&&k==='C'){
   add([2,2,15],[14,12,15.2],'soot',['south']);
   add([3,3,15.25],[13,11,15.5],'grate',['south']);
   add([2,1.8,15.6],[4,13,15.85],'iron',['south']);
   add([12,1.8,15.6],[14,13,15.85],'iron',['south']);
   add([4,11.5,15.6],[12,13.5,15.85],'iron',['south']);
  }
  // Full-height structural footplates at real plinth corners.
  if(side&&[-8,-5,-2,0].includes(z)){
   add(x===2?[15,0,12]:[0,0,12],x===2?[16,16,16]:[1,16,16],
       'iron',[x===2?'east':'west']);
  }
 }else if(y===1){
  const window=side&&[-2,-4,-6].includes(z),face=x===2?'east':'west';
  for(const f of out){
   if(window&&f===face){
    if(x===2)add([0,0,0],[15.1,16,16],'brick',['east']);
    else add([.9,0,0],[16,16,16],'brick',['west']);
   }else add([0,0,0],[16,16,16],f==='up'?'refractory':'brick',[f]);
  }
  if(window){
   // Three inset roasting arches per side; each is mechanically attached.
   const panel=(y1,y2,z1,z2,m)=>{
    const a=x===2?[15.2,y1,z1]:[.3,y1,z1];
    const b=x===2?[15.7,y2,z2]:[.8,y2,z2];
    add(a,b,m,[face]);
   };
   panel(2,13,2,14,'soot');
   panel(4,11,4,12,'ember');
   panel(2,4,2,14,'refractory');       // sill
   panel(4,10,2,4,'refractory');      // left jamb
   panel(4,10,12,14,'refractory');    // right jamb
   panel(10,12,4,12,'refractory');    // arch spring
   panel(12,14,6,10,'refractory');    // keystone
   for(const bar of [6,9])panel(4,11,bar,bar+1,'grate');
  }
  if(side&&[-8,-5,-1].includes(z)){
   add(x===2?[15,0,12]:[0,0,12],x===2?[16,16,15]:[1,16,15],
       'iron',[face]);
  }
  if(front&&x===0){
   add([1,1,15],[15,15,15.2],'soot',['south']);
   add([2,2,15.3],[14,14,15.7],'iron',['south']);
   add([7,2,15.75],[8,14,15.9],'soot',['south']);
   add([9,7,15.91],[12,9,16],'bronze',['south']);
  }
  if(front&&Math.abs(x)===1)
   add([0,12.5,15.05],[16,15.5,15.75],'refractory',['south']);
 }else if(y===2){
  const top=roofTop(x,z);
  add([0,0,0],[16,top,16],'roof',['up']);
  for(const f of ['north','south'])
   if(out.includes(f))add([0,0,0],[16,top,16],'roof',[f]);
  for(const f of ['east','west']){
   const nx=x+(f==='east'?1:-1),n=lookup.get([nx,y,z].join(','));
   const next=n&&n.k==='B'?roofTop(nx,z):0;
   if(next<top)add([0,next,0],[16,top,16],
      Math.abs(x)===2?'iron':'refractory',[f]);
  }
  if(out.includes('down'))add([0,0,0],[16,top,16],'refractory',['down']);
  if([-1,-4,-6].includes(z))
   add([0,top-1.4,6],[16,Math.min(16,top+.8),8],'iron',['up','north','south']);
  // Real open AIR stack at x0,z-7. Stone surrounds its perimeter.
  if((x===0&&(z===-8||z===-6))||(Math.abs(x)===1&&z===-7)){
   add([0,top-1,0],[16,top,16],'refractory',['up']);
   if(x===0&&z===-8)add([0,0,13],[16,16,16],'iron',['south']);
   if(x===0&&z===-6)add([0,0,0],[16,16,3],'iron',['north']);
  }
 }
 return {parent:'minecraft:block/block',ambientocclusion:false,textures:tex,elements:E};
}
export function roastPort(role,d){
 if(role!=='input'&&role!=='primary')throw Error('Unknown port '+role);
 const E=[],add=(a,b,m,f)=>E.push(modelBox(a,b,m,f,d));
 add([0,0,0],[16,16,15],'foundation',['north','south','up','down','east','west']);
 add([2,3,15.1],[14,13,15.4],'soot',['south']);
 add([4,4,15.45],[12,12,15.7],role==='input'?'input':'output',['south']);
 add([2,12.5,15.5],[14,14,15.8],'iron',['south']);
 add([2,2,15.5],[14,3.5,15.8],'iron',['south']);
 return {parent:'minecraft:block/block',ambientocclusion:false,textures:tex,elements:E};
}
