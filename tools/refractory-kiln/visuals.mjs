/**
 * Refractory Kiln K1: static, block-local facade and heat-resistant masonry.
 * No per-frame work, oversized cuboids or fake port proxies.
 * Geometry and UV are rotated from canonical SOUTH local coordinates.
 */
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
function kilnTextures(){
 return {
  // Shared material texture is legitimate; all machine-specific surfaces are original.
  stone:"magneticraft2:block/refractory_kiln/plinth",
  masonry:"magneticraft2:block/refractory_kiln/brick",
  ceramic:"magneticraft2:block/material/refractory_brick",
  iron:"magneticraft2:block/refractory_kiln/iron",
  band:"magneticraft2:block/refractory_kiln/iron",
  bronze:"magneticraft2:block/refractory_kiln/bronze",
  soot:"magneticraft2:block/refractory_kiln/soot",
  grate:"magneticraft2:block/refractory_kiln/grate",
  door:"magneticraft2:block/refractory_kiln/door",
  roof:"magneticraft2:block/refractory_kiln/roof",
  input:"magneticraft2:block/refractory_kiln/input",
  output:"magneticraft2:block/refractory_kiln/output",
  particle:"magneticraft2:block/refractory_kiln/brick"
 };
}
/**
 * K1 readability pass. All geometry is static, block-local, and visually
 * subordinate to the central charge door. The heat/firebox simulation is K2.
 * This does NOT create a second controller or fake item ports.
 */
export function kilnVisual(c,dir,lookup){
 const {x,y,z,k}=c, elements=[];
 const add=(from,to,material,faces)=>elements.push(modelBox(from,to,material,faces,dir));
 const offset={
  north:[0,0,-1],south:[0,0,1],east:[1,0,0],
  west:[-1,0,0],up:[0,1,0],down:[0,-1,0]
 };
 const occupied=new Set(['B','S','C','U','O']);
 const exposed=Object.entries(offset).filter(([,v])=>{
  const neighbor=lookup.get([x+v[0],y+v[1],z+v[2]].join(','));
  return !neighbor||!occupied.has(neighbor.k);
 }).map(([face])=>face);
 const front=z===0,foundation=y===0,roof=y===3;
 const chargeDoor=front&&y===2&&Math.abs(x)<=1;
 const firebox=front&&foundation&&x===0&&k==='C';
 const chimneyRim=roof&&(
  (Math.abs(x)===1&&z===-2)||
  (x===0&&(z===-3||z===-1))
 );
 const frontDepth=chargeDoor?13:firebox?12:front&&foundation?15:16;
 const top=chimneyRim?15:16;
 // Brick and stone are the structure itself. Side walls are left deliberately
 // plain; inner-facing surfaces use the common refractory ceramic material.
 for(const face of exposed){
  const outside=face==='south'&&front||face==='north'&&z===-4||
   face==='west'&&x===-2||face==='east'&&x===2;
  const material=foundation?'stone':outside||face==='up'?'masonry':'ceramic';
  add([0,0,0],[16,top,frontDepth],material,[face]);
 }
 if(firebox){
  // ONE low firebox. The former second 'instrument panel' is now brickwork.
  add([2,2.5,12.1],[14,12.5,12.8],'soot',['south']);
  add([3,3.5,12.9],[13,11.5,13.3],'grate',['south']);
  add([1,2,13.4],[3,13,15.5],'iron',['south','east']);
  add([13,2,13.4],[15,13,15.5],'iron',['south','west']);
  add([3,11.5,13.4],[13,13.5,15.5],'iron',['south','down']);
  add([3,2,13.4],[13,4,15.5],'iron',['south','up']);
 }else if(chargeDoor){
  // One broad door, formed from three adjoining REAL brick cells.
  // Local z<=16; each cell contributes a panel without covering its neighbours.
  const left=x===-1,right=x===1;
  add([0,2,13.1],[16,14,13.5],'soot',['south']);
  add([0,2.5,13.6],[16,13.5,14.7],'iron',['south']);
  add([0,13.5,14.8],[16,15.2,15.7],'iron',['south','down']);
  add([0,.8,14.8],[16,2.5,15.7],'iron',['south','up']);
  if(left){
   add([0,2.5,14.8],[2.4,13.5,15.7],'iron',['south','east']);
  }
  if(right){
   add([13.6,2.5,14.8],[16,13.5,15.7],'iron',['south','west']);
  }
  if(x===0){
   // A single identifiable latch. No repeated decorative control panels.
   add([7.4,3,14.8],[8.6,13,15.5],'soot',['south']);
   add([9,6.5,15.55],[13,8.2,16],'bronze',['south']);
  }
 }else if(chimneyRim){
  // Low, connected 3-pixel brick collar around the ACTUAL AIR flue.
  // It stays within existing roof blocks and does not invent floating air parts.
  if(x===-1)add([13,15,0],[16,16,16],'ceramic',['up','east']);
  if(x===1)add([0,15,0],[3,16,16],'ceramic',['up','west']);
  if(z===-3&&x===0)add([0,15,13],[16,16,16],'ceramic',['up','south']);
  if(z===-1&&x===0)add([0,15,0],[16,16,3],'ceramic',['up','north']);
 }
 return {parent:'minecraft:block/block',ambientocclusion:false,
  textures:kilnTextures(),elements};
}

export function kilnPort(role,dir){
 if(role!=='input'&&role!=='primary')throw Error('Unsupported kiln port '+role);
 const elements=[];
 const add=(from,to,material,faces)=>elements.push(modelBox(from,to,material,faces,dir));
 // Same real block and capability as before, now visually a small flush inset.
 // The main charging door remains much larger than either port.
 add([0,0,0],[16,16,15],'masonry',['south','up','down','east','west','north']);
 add([3,3,15.1],[13,13,15.35],'soot',['south']);
 add([4.5,4.5,15.45],[11.5,11.5,15.65],
  role==='input'?'input':'output',['south']);
 add([3,12,15.4],[13,13.25,15.8],'iron',['south']);
 add([3,2.75,15.4],[13,4,15.8],'iron',['south']);
 add([2.75,4,15.4],[4,12,15.8],'iron',['south']);
 add([12,4,15.4],[13.25,12,15.8],'iron',['south']);
 return {parent:'minecraft:block/block',ambientocclusion:false,
  textures:kilnTextures(),elements};
}
