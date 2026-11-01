export function modelBox(from,to,material,sides,dir){
 const pix=(x,z)=>dir==="south"?[x,z]:dir==="west"?[16-z,x]:dir==="north"?[16-x,16-z]:[z,16-x];
 const faceMap={south:{north:"north",south:"south",east:"east",west:"west"},west:{north:"east",south:"west",east:"south",west:"north"},north:{north:"south",south:"north",east:"west",west:"east"},east:{north:"west",south:"east",east:"north",west:"south"}};
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


/**
 * Ore Roasting Kiln — reference-matched, baked block-local facade.
 *
 * The large, three-wall-layer arches, actual timber header, angular slate
 * roof, brick chimney and loadbearing supports form the silhouette.
 *
 * Use existing vanilla tile textures wherever visually appropriate.
 * Original 16x16 art is reserved for grilles, fire bed, soot, iron and ports.
 * No model element leaves its owning block; no per-frame renderer.
 */
const textures={
 stone:'minecraft:block/stone_bricks',
 brick:'minecraft:block/bricks',
 roof:'minecraft:block/stone_bricks',
 roofTrim:'minecraft:block/deepslate_tiles',
 timber:'minecraft:block/spruce_planks',
 timberEnd:'minecraft:block/stripped_spruce_log',
 refractory:'minecraft:block/smooth_stone',
 iron:'minecraft:block/polished_deepslate',
 soot:'minecraft:block/black_concrete',
 ember:'minecraft:block/magma',
 grate:'minecraft:block/polished_blackstone',
 bronze:'minecraft:block/cut_copper',
 input:'magneticraft2:block/ore_roasting_kiln/input',
 output:'magneticraft2:block/ore_roasting_kiln/output',
 particle:'minecraft:block/bricks'
};
const offsets={north:[0,0,-1],south:[0,0,1],east:[1,0,0],
               west:[-1,0,0],up:[0,1,0],down:[0,-1,0]};
const solids=new Set(['S','B','C','U','O']);
const openings=new Set([-1,-2,-4,-5,-7,-8]);
const rowStarts=new Set([-1,-4,-7]);
const chimneyFoot=(x,z)=>Math.abs(x)<=1&&z>=-8&&z<=-6&&!(x===0&&z===-7);
const blank=(E)=>({parent:'minecraft:block/block',ambientocclusion:false,textures,elements:E});
export function roofTop(x,z){
 if(Math.abs(x)>2)throw Error('Outside roof '+x);
 // 4px-thick courses made within the existing roof cell only.
 // The top of the middle ridge is 15px, not an arbitrary full brick cap.
 return [15,11,6][Math.abs(x)];
}
export function roofStep(globalPixel) {
 // Across a 5-block roof, a stepped approximation to two pitched slopes.
 const dx=Math.abs(globalPixel-40);
 return Math.max(2,15-Math.floor(dx*13/40));
}
function addRoof(E,add,c){
 // Uniform stone-tile courses with a restrained dark gable fascia; no black bands.
 const front=c.z===0,rear=c.z===-8;
 for(let strip=0;strip<4;strip++){
  const from=strip*4,to=from+4,global=(c.x+2)*16+from;
  const h=roofStep(global+2),prev=global===0?0:roofStep(global-2),
        next=global===76?0:roofStep(global+6);
  const start=rear?2:0,end=front?14:16;
  add([from,0,start],[to,h,end],'roof',['up']);
  if(h>prev)add([from,prev,start],[to,h,end],'roof',['west']);
  if(h>next)add([from,next,start],[to,h,end],'roof',['east']);
  if(front||rear){
   const a=rear?0:14,b=rear?2:16;
   add([from,0,a],[to,h,b],'roofTrim',['up',front?'south':'north']);
   if(h>prev)add([from,prev,a],[to,h,b],'roofTrim',['west']);
   if(h>next)add([from,next,a],[to,h,b],'roofTrim',['east']);
  }
 }
}
function chimneyBounds(c){
 return {x0:c.x===-1?12:0,x1:c.x===1?4:16,
         z0:c.z===-8?12:0,z1:c.z===-6?4:16};
}
function addChimneyBase(c,add){
 // The flue emerges directly from the slate: no oversized solid plinth.
 const {x0,x1,z0,z1}=chimneyBounds(c);
 for(let a=x0;a<x1;a+=4){
  const b=Math.min(a+4,x1);
  const h=roofStep((c.x+2)*16+(a+b)/2);
  if(h<16)add([a,h,z0],[b,16,z1],'brick',['north','south','east','west']);
 }
}
/**
 * Six matching arched, recessed furnace windows. The old version overlaid
 * paper-thin textured planes and coplanar 3D pieces. Each wall cell now
 * contains a CLOSED back wall, a real inset, solid outer jambs, a masonry
 * sill / crown, and full-thickness grille bars. All volumes are disjoint
 * where they share an outward-facing plane: no decorated sheet over a block
 * face and no accidental central mullion.
 *
 * x is outward on the RIGHT side, and mirrored for the LEFT side. All
 * coordinates are native 16px and existing Minecraft textures are used.
 */
export function isRoastingBay(c){
 return Math.abs(c.x)===2 && openings.has(c.z) && c.y>=1 && c.y<=3;
}
function mirrorBoxX(from,to,left){
 return left?[[16-to[0],from[1],from[2]],[16-from[0],to[1],to[2]]]:
             [from,to];
}
function box6(add,from,to,mat,left){
 const [a,b]=mirrorBoxX(from,to,left);
 add(a,b,mat,['north','south','east','west','up','down']);
}
function volumeArch(c,add){
 if(!isRoastingBay(c))throw Error('Not a roasting bay');
 const left=c.x===-2,start=rowStarts.has(c.z),outerA=start?13:0,
       outerB=start?16:3,insideA=start?0:3,insideB=start?13:16;
 const p=(a,b,m)=>box6(add,a,b,m,left);
 p([0,0,0],[9,16,16],'brick');
 p([9,0,0],[10,16,16],'soot');
 if(c.y<3){
  p([10,0,outerA],[16,16,outerB],c.z===-8?'timberEnd':'brick');
  if(c.y===1){
   p([10,0,insideA],[16,5,insideB],'refractory');
   p([10,5,insideA],[11,12,insideB],'ember');
  }else p([10,0,insideA],[11,5,insideB],'ember');
 }else{
  // Each 2-block bay has stepped brick voussoirs and an actual arched void.
  // The continuous timber header *rests on* the brick crown.
  for(const [low,high,width] of [[0,4,3],[4,7,5],
                                   [7,9,7],[9,11,9],[11,12,16]]){
   const a=start?(width===16?0:16-width):0,b=start?16:width;
   p([10,low,a],[16,high,b],'brick');
  }
  p([12,12,0],[16,16,16],'timber');
 }
 const low=c.y===1?5:0,high=c.y===3?9:16,zBar=start?4:11;
 p([12,low,zBar],[14,high,zBar+1.5],'grate');
 if(c.y===2)p([11,6,insideA],[12,7,insideB],'iron');
}
function sidePier(c,add){
 const p=(a,b,m)=>box6(add,a,b,m,c.x===-2),y=c.y,top=y===3?12:16;
 p([0,0,0],[12,top,16],'brick');
 p([12,0,0],[16,top,6],'brick');
 p([12,0,10],[16,top,16],'brick');
 p([12,0,6],[16,y===3?9:16,10],'timberEnd');
 if(y===3){
  p([12,9,6],[16,12,10],'iron');
  p([12,12,0],[16,16,16],'timber');
 }
}
function frontCorner(c,add){
 const p=(a,b,m)=>box6(add,a,b,m,c.x===-2),y=c.y,top=y===3?12:16;
 p([0,0,0],[12,top,16],'brick');
 p([12,0,0],[16,top,12],'brick');
 p([12,0,12],[16,y===3?9:16,16],'timberEnd');
 if(y===3){
  p([12,9,12],[16,12,16],'iron');
  p([12,12,0],[16,16,16],'timber');
  p([0,12,12],[12,16,16],'timber');
 }
}
function frontDoorWall(c,add){
 const y=c.y,x=c.x,top=y===3?12:16;
 const [a,b]=x===-1?[8,16]:x===1?[0,8]:[0,16];
 add([0,0,0],[16,top,14],'brick',['north','south','east','west']);
 if(y===3){
  add([0,0,14],[16,12,16],'brick',['south']);
  add([0,12,12],[16,16,16],'timber',
      ['south','north','up','down','east','west']);
  return;
 }
 if(a>0)add([0,0,14],[a,16,16],'brick',['south','up','down']);
 if(b<16)add([b,0,14],[16,16,16],'brick',['south','up','down']);
 const low=y===1?4:0,high=y===2?12:16;
 if(low>0)add([a,0,14],[b,low,16],'refractory',['south','up']);
 if(high<16)add([a,high,14],[b,16,16],'brick',['south','down']);
 // 32px-wide framed front charging door, rather than a hidden 1-block strip.
 add([a,low,14],[b,high,15.1],'soot',['south']);
 add([a,low,15.1],[b,high,15.55],'iron',['south']);
 if(x===-1)add([a,low,15.55],[a+2,high,16],'grate',['south']);
 if(x===1)add([b-2,low,15.55],[b,high,16],'grate',['south']);
 const rail=y===1?6:9;
 add([a,rail,15.55],[b,rail+2,15.9],'grate',['south']);
 if(y===1&&x===0)add([4,8,15.6],[12,11,15.95],'ember',['south']);
}
function rearWall(c,add){
 const y=c.y;
 if(y===3){
  add([0,0,0],[16,12,16],'brick',['north','south','east','west']);
  add([0,12,0],[16,16,4],'timber',
      ['north','south','east','west','up','down']);
  add([0,12,4],[16,16,16],'brick',['north','south','east','west']);
 }else add([0,0,0],[16,16,16],'brick',['north','south','east','west']);
}
export function roastVisual(c,d,lookup){
 const {x,y,z,k}=c,E=[];
 const add=(a,b,material,faces)=>E.push(modelBox(a,b,material,faces,d));
 const exposed=Object.keys(offsets).filter(face=>{
  const off=offsets[face];
  const v=lookup.get([x+off[0],y+off[1],z+off[2]].join(','));
  return !v||!solids.has(v.k);
 });
 if(y===0){
  for(const face of exposed)add([0,0,0],[16,16,16],'stone',[face]);
  if(z===0&&x===0&&k==='C'){
   add([2,2,14.1],[14,13,14.4],'soot',['south']);
   add([3,3,14.5],[13,12,14.85],'grate',['south']);
   for(const a of [[2,2,14.9,4,14,15.9],[12,2,14.9,14,14,15.9]])
    add(a.slice(0,3),a.slice(3),'iron',['south']);
   add([4,12,15],[12,14,15.8],'iron',['south']);
  }
 }else if(y>=1&&y<=3){
  if(Math.abs(x)===2){
   if(z===0)frontCorner(c,add);
   else if(isRoastingBay(c))volumeArch(c,add);
   else sidePier(c,add);
  }else if(z===0)frontDoorWall(c,add);
  else if(z===-8)rearWall(c,add);
 }else if(y===4){
  addRoof(E,add,c);
  if(chimneyFoot(x,z))addChimneyBase(c,add);
 }else if(y>=5&&y<=7){
  const {x0,x1,z0,z1}=chimneyBounds(c);
  const exterior=y===7?13:16;
  add([x0,0,z0],[x1,exterior,z1],'brick',
      ['north','south','east','west','up','down']);
  if(y===7){
   const xa=x===-1?10:0,xb=x===1?6:16;
   const za=z===-8?10:0,zb=z===-6?6:16;
   add([xa,13,za],[xb,16,zb],'roofTrim',
       ['north','south','east','west','up','down']);
  }
 }
 return blank(E);
}
export function roastPort(role,d){
 if(role!=='input'&&role!=='primary')throw Error('Unknown item port '+role);
 const E=[],add=(a,b,m,f)=>E.push(modelBox(a,b,m,f,d));
 // One integrated full-block metal inset in the real foundation port.
 add([0,0,0],[16,16,15],'stone',['north','south','up','down','east','west']);
 add([1,1,15.05],[15,15,15.3],'iron',['south']);
 add([2.5,2.5,15.35],[13.5,13.5,15.55],'soot',['south']);
 add([4,5,15.6],[12,11,15.85],role==='input'?'input':'output',['south']);
 add([2,13.5,15.55],[14,14.7,15.85],'iron',['south']);
 add([2,1.3,15.55],[14,2.5,15.85],'iron',['south']);
 return blank(E);
}
