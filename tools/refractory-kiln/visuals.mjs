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
export function kilnVisual(c,dir,lookup){
 const {x,y,z,k}=c, elements=[];
 const push=(a,b,m,faces)=>elements.push(modelBox(a,b,m,faces,dir));
 const off={north:[0,0,-1],south:[0,0,1],east:[1,0,0],west:[-1,0,0],up:[0,1,0],down:[0,-1,0]};
 const exposed=[];
 for(const [side,o] of Object.entries(off)){
  const n=lookup.get([x+o[0],y+o[1],z+o[2]].join(","));
  if(!n||!["B","S","C","U","O"].includes(n.k))exposed.push(side);
 }
 const front=z===0, left=x===-2,right=x===2,rear=z===-4, roof=y===3, foundation=y===0;
 const sideL=left&&exposed.includes("west"),sideR=right&&exposed.includes("east");
 const back=rear&&exposed.includes("north");
 // The masonry body has independently textured interior/exterior faces.
 // Recess only decorated exterior planes; never draw a decal coplanar with the core.
 let f=[sideL?1:0,0,back?1:0],t=[sideR?15:16,roof?14:16,front?(k==="C"?11:k==="B"&&x===0&&y===2?12:14.5):16];
 if(k==="C"){f=[0,0,0];t=[16,16,11];}
 for(const side of exposed){
  const mat=foundation?"stone":roof&&side==="up"?"roof":
    side==="north"||side==="south"||side==="east"||side==="west"?
      ((side==="north"&&z!==-4)||(side==="south"&&z!==0)||(side==="west"&&x!==-2)||(side==="east"&&x!==2)?"ceramic":"masonry"):
    "ceramic";
  push(f,t,mat,[side]);
 }
 if(foundation){
  if(front){
   // Stepped, iron-banded plinth. Controller has a real inset firebox face.
   push([0,0,14.5],[16,3,16],"stone",["south","up"]);
   push([0,13,14.5],[16,16,16],"band",["south","up"]);
   if(k==="C"){
    push([2,2,11.25],[14,12.5,13.3],"soot",["south"]);
    push([2.5,2.5,13.35],[13.5,12,14],"grate",["south"]);
    push([1,1.5,14],[3,13.5,15.8],"iron",["south","east"]);
    push([13,1.5,14],[15,13.5,15.8],"iron",["south","west"]);
    push([3,11.5,14],[13,13.4,15.8],"band",["south"]);
    push([3,1,14],[13,2.5,15.8],"band",["south"]);
    push([6.5,2,15.8],[9.5,3.25,16],"bronze",["south"]);
   }else{
    push([2,5,14.6],[14,11,15.15],"stone",["south"]);
    push([2,9.5,15.2],[14,10.5,15.8],"band",["south"]);
    push([2,5,15.2],[14,6,15.8],"band",["south"]);
   }
  }
 }else if(roof){
  // Roof tiles are real solid roof cells; central chimney stays hollow AIR.
  push([0,14.15,0],[16,15.2,16],"roof",["up"]);
  if(left)push([0,15.2,0],[2,16,16],"band",["up","west"]);
  if(right)push([14,15.2,0],[16,16,16],"band",["up","east"]);
  if(rear)push([left?2:0,15.2,0],[right?14:16,16,2],"band",["up","north"]);
  if(front)push([left?2:0,15.2,14],[right?14:16,16,16],"band",["up","south"]);
  // A low metal collar around the actual open chimney, confined to masonry cells.
  if(x===-1&&z===-2)push([13.75,15.2,3],[16,16,13],"bronze",["up","east"]);
  if(x===1&&z===-2)push([0,15.2,3],[2.25,16,13],"bronze",["up","west"]);
  if(x===0&&z===-3)push([3,15.2,13.75],[13,16,16],"bronze",["up","south"]);
  if(x===0&&z===-1)push([3,15.2,0],[13,16,2.25],"bronze",["up","north"]);
 }else if(front){
  if(x===0&&y===2){
   // A closed, dark-iron charge hatch baked onto actual front masonry.
   // It is visual-only in K1; no false inventory, collision or HEAT semantics.
   push([1.5,1.5,12.25],[14.5,14.5,13.1],"soot",["south"]);
   push([2.5,2.5,13.2],[13.5,13.5,14.4],"door",["south"]);
   push([1,1,14.45],[3,15,15.8],"iron",["south","east"]);
   push([13,1,14.45],[15,15,15.8],"iron",["south","west"]);
   push([3,13,14.45],[13,15,15.8],"band",["south","down"]);
   push([3,1,14.45],[13,3,15.8],"band",["south","up"]);
   push([6,6.7,14.5],[12,8.25,15.75],"bronze",["south"]);
   push([5,6.25,14.5],[6.5,8.75,15.75],"iron",["south"]);
   for(const px of [2,13])for(const py of [2,13])
    push([px-.45,py-.45,15.8],[px+.45,py+.45,16],"bronze",["south"]);
  }else if(x===0&&y===1){
   // Fixed firebox-instrument panel between the genuine item modules.
   push([1,1,14.6],[15,15,15.1],"iron",["south"]);
   push([2,11.5,15.2],[14,13.5,15.7],"band",["south"]);
   push([2,2,15.2],[14,4,15.7],"band",["south"]);
   for(const px of [4,8,12])push([px-1,6,15.2],[px,10,15.75],"grate",["south"]);
   push([7,4,15.75],[9,6,16],"bronze",["south"]);
  }else{
   // 3D staggered brick courses. The backing remains visible in mortar joints.
   push([.5,.5,14.6],[15.5,15.5,14.85],"masonry",["south"]);
   for(let j=0;j<4;j++){
    const low=1+j*3.65,high=Math.min(15,low+2.7);
    const split=j%2===0?7.4:9.2;
    push([.6,low,14.9],[split-.35,high,15.65],"masonry",["south","up"]);
    push([split+.25,low,14.9],[15.4,high,15.65],"masonry",["south","up"]);
   }
   // The lintel and vertical corner ribs visibly anchor the brickwork.
   if(y===2)push([0,13.9,15.7],[16,15.35,16],"band",["south"]);
   if(left||right){
    const xa=left?0:13.5;
    push([xa,1,15.7],[xa+2.5,14.8,16],"iron",["south"]);
    push([xa+.5,11.9,15.7],[xa+2,13.2,16],"bronze",["south"]);
    push([xa+.5,2.5,15.7],[xa+2,3.8,16],"bronze",["south"]);
   }
  }
 }else{
  // External wall hoops and buttress straps. No geometry extends into ports/air.
  if(sideL||sideR){
   const a=sideL?[0,0,0]:[15,0,0],b=sideL?[1,16,16]:[16,16,16];
   const facing=sideL?"west":"east";
   for(const zz of [2,13]){
    const fa=[a[0],2,zz],ta=[b[0],14,zz+1.2];
    push(fa,ta,"band",[facing]);
    push([a[0],11.6,zz],[b[0],13,zz+1.2],"bronze",[facing]);
   }
  }
  if(back){
   for(const xx of [2,13]){
    push([xx,2,0],[xx+1.2,14,1],"band",["north"]);
   }
  }
 }
 return {parent:"minecraft:block/block",ambientocclusion:false,textures:kilnTextures(),elements};
}
export function kilnPort(role,dir){
 const elements=[];
 const push=(a,b,m,sides)=>elements.push(modelBox(a,b,m,sides,dir));
 const output=role==="primary";
 // These models are selected only for the existing formed, real item ports.
 // Their BlockEntity, hitbox, capabilities and inventory remain unchanged.
 push([0,0,0],[16,16,13],"ceramic",["south","up","down","north","east","west"]);
 push([.5,.5,13.1],[15.5,15.5,14.1],"masonry",["south"]);
 push([2,3,14.2],[14,13,14.7],"soot",["south"]);
 push([3,5,14.75],[13,11.5,15.05],output?"output":"input",["south"]);
 push([0,0,14.8],[2,16,16],"iron",["south"]);
 push([14,0,14.8],[16,16,16],"iron",["south"]);
 push([2,13.5,14.8],[14,16,16],"band",["south"]);
 push([2,0,14.8],[14,2.5,16],"band",["south"]);
 if(output){
  // Projecting extraction lip, open-looking but not a fake item entity.
  push([3,3,14.9],[13,5,16],"band",["south","up"]);
  push([5,6.7,15.1],[11,8.2,15.85],"bronze",["south"]);
 }else{
  // Narrow input throat and feed guide.
  push([4,7,15.1],[12,9,15.9],"iron",["south"]);
  push([7,4,15.1],[9,7,15.9],"bronze",["south"]);
 }
 for(const px of [1,14])for(const py of [1.5,14.5])
  push([px-.45,py-.45,15.8],[px+.45,py+.45,16],"bronze",["south"]);
 return {parent:"minecraft:block/block",ambientocclusion:false,textures:kilnTextures(),elements};
}
