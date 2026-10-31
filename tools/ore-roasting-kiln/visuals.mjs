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
 refractory:'magneticraft2:block/material/refractory_brick',
 iron:'magneticraft2:block/ore_roasting_kiln/iron',
 soot:'magneticraft2:block/ore_roasting_kiln/soot',
 ember:'magneticraft2:block/ore_roasting_kiln/ember',
 grate:'magneticraft2:block/ore_roasting_kiln/grate',
 bronze:'magneticraft2:block/ore_roasting_kiln/bronze',
 input:'magneticraft2:block/ore_roasting_kiln/input',
 output:'magneticraft2:block/ore_roasting_kiln/output',
 particle:'minecraft:block/bricks'
};
const offsets={north:[0,0,-1],south:[0,0,1],east:[1,0,0],
               west:[-1,0,0],up:[0,1,0],down:[0,-1,0]};
const solids=new Set(['S','B','C','U','O']);
const openings=new Set([-1,-2,-4,-5,-7,-8]);
const ribs=new Set([0,-3,-6,-8]);
const rowEnds=new Set([-2,-5,-8]);
const rowStarts=new Set([-1,-4,-7]);
const faceSide=x=>x===2?'east':'west';
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
 const {x,z}=c;
 for(let strip=0;strip<4;strip++){
  const from=strip*4,to=from+4,gx=(x+2)*16+from;
  const h=roofStep(gx+2);
  const prev=roofStep(gx-2);
  const mat=(z===0||z===-8||z===-3||z===-6)?'roofTrim':'roof';
  add([from,0,0],[to,h,16],mat,['up']);
  // Faces of the true upward/downward height difference only.
  // Never emit a zero-volume element when adjacent strips match.
  if(strip>0&&h>prev)
   add([from-.35,prev,0],[from,h,16],mat,['west']);
  else if(strip>0&&h<prev)
   add([from,h,0],[from+.35,prev,16],mat,['east']);
 }
 // Solid gable at the front and back follows the exact step roof profile.
 if(z===0||z===-8){
  for(let s=0;s<4;s++){
   const a=s*4,b=a+4,gy=roofStep((x+2)*16+a+2);
   add([a,0,0],[b,gy,16],z===0?'refractory':'brick',
       [z===0?'south':'north']);
  }
 }
 // Material bearing line under the eaves, made of existing wall blocks.
 if(Math.abs(x)===2){
  add([0,0,0],[16,2,16],'roofTrim',[faceSide(x)]);
 }
 // Ridge ridge-cap, physically sits on the centre course.
 if(x===0){
  add([5,14,0],[11,16,16],'roofTrim',['up']);
 }
 // Roof cross-ribs are continuous across the five sloping courses.
 if(z===-2||z===-5){
  const y=roofTop(x,z)-1;
  add([0,y,6],[16,y+1.0,10],'iron',['up','north','south']);
 }
}
function aperturePanel(add,x,y0,y1,z0,z1,material,depth=14.0){
 const face=faceSide(x);
 const a=x===2?[depth,y0,z0]:[16-depth-.18,y0,z0];
 const b=x===2?[depth+.18,y1,z1]:[16-depth,y1,z1];
 add(a,b,material,[face]);
}
function arch(cell,add){
 const {x,y,z}=cell;
 const side=faceSide(x);
 // Two tiles per arch; 3 arches repeated down each long wall.
 const first=rowStarts.has(z),last=rowEnds.has(z);
 const base0=first?2:0,base1=last?14:16;
 if(y===1||y===2){
  // Recess is 3 pixels from the masonry facade. Glow occupies the interior.
  aperturePanel(add,x,0,16,base0,base1,'soot',12.4);
  aperturePanel(add,x,y===1?4:0,15,base0+1,base1-1,'ember',12.75);
  if(y===1){
   aperturePanel(add,x,0,3,base0,base1,'refractory',15.4);
   aperturePanel(add,x,3,4,base0,base1,'iron',15.6);
  }
  // Thick masonry edge is visible around the two-cell arch.
  if(first)aperturePanel(add,x,0,16,0,2.6,'brick',15.45);
  if(last)aperturePanel(add,x,0,16,13.4,16,'brick',15.45);
  // Three closely spaced grille bars. They attach to the lower sill and arch.
  for(const zbar of [4,9,14]){
   if(zbar<base0+2||zbar>base1-2)continue;
   aperturePanel(add,x,4,16,zbar,zbar+.9,'grate',15.6);
  }
 }
 if(y===3){
  // Upper curved shoulder; two blocks across, split into stepped voussoirs.
  aperturePanel(add,x,0,7,base0,base1,'soot',12.4);
  aperturePanel(add,x,0,5,base0+2,base1-2,'ember',12.75);
  aperturePanel(add,x,7,11,base0,base1,'refractory',15.1);
  aperturePanel(add,x,11,16,base0,base1,'brick',15.5);
  if(first){
   aperturePanel(add,x,4,11,0,4,'brick',15.55);
   aperturePanel(add,x,7,13,4,7,'refractory',15.4);
  }
  if(last){
   aperturePanel(add,x,4,11,12,16,'brick',15.55);
   aperturePanel(add,x,7,13,9,12,'refractory',15.4);
  }
  aperturePanel(add,x,5,8,7,9,'refractory',15.75);
 }
}
export function roastVisual(c,d,lookup){
 const {x,y,z,k}=c,E=[];
 const add=(a,b,material,faces)=>E.push(modelBox(a,b,material,faces,d));
 const exposed=Object.keys(offsets).filter(face=>{
  const off=offsets[face];
  const v=lookup.get([x+off[0],y+off[1],z+off[2]].join(','));
  return !v||!solids.has(v.k);
 });
 const side=Math.abs(x)===2,front=z===0,back=z===-8;
 if(y===0){
  // Vanilla stone-brick foundation, visible all around.
  for(const face of exposed)add([0,0,0],[16,16,16],'stone',[face]);
  if(front&&x===0&&k==='C'){
   add([2,2,14.1],[14,13,14.4],'soot',['south']);
   add([3,3,14.5],[13,12,14.85],'grate',['south']);
   for(const a of [[2,2,14.9,4,14,15.9],[12,2,14.9,14,14,15.9]])
    add(a.slice(0,3),a.slice(3),'iron',['south']);
   add([4,12,15],[12,14,15.8],'iron',['south']);
  }
 }else if(y>=1&&y<=3){
  // Three true wall layers: side openings occupy roughly 2.5 blocks in
  // height rather than the old one-block-high miniature window slots.
  const window=side&&openings.has(z);
  for(const face of exposed){
   if(window&&face===faceSide(x)){
    // Don't hide the recessed arch beneath a full exterior brick face.
    if(x===2)add([0,0,0],[12.25,16,16],'brick',[face]);
    else add([3.75,0,0],[16,16,16],'brick',[face]);
   }else add([0,0,0],[16,16,16],face==='up'?'refractory':'brick',[face]);
  }
  if(window)arch(c,add);
  // Separate loadbearing iron ribs and timber ties, based on wall piers.
  if(side&&(ribs.has(z)||front||back)){
   const f=faceSide(x);
   const a=x===2?[15.2,0,0]:[0,0,0];
   const b=x===2?[16,16,3]:[.8,16,3];
   add(a,b,'iron',[f,'north','south']);
  }
  // Continuous oak-colored support timber below roof, on actual brickwork.
  if(side&&y===3){
   const face=faceSide(x);
   add(x===2?[15.1,12,0]:[.2,12,0],
       x===2?[15.9,15,16]:[.9,15,16],'timber',[face,'up']);
  }
  if(front&&x===0){
   if(y===1||y===2){
    add([2,0,13.5],[14,16,13.8],'soot',['south']);
    add([3,0,13.9],[13,16,15.0],'iron',['south']);
    if(y===1)add([8,5,15.1],[12,8,15.8],'bronze',['south']);
   }else{
    add([2,0,14.1],[14,6,14.4],'iron',['south']);
    add([1,6,14.4],[15,12,14.9],'refractory',['south']);
   }
  }
  if(front&&Math.abs(x)===1&&y===3)
   add([0,5,15],[16,7,15.7],'refractory',['south']);
 }else if(y===4){
  // A continuous actual pitched stone roof, not a single flat tile with
  // decorative strips. The central AIR chimney throat is left untouched.
  addRoof(E,add,c);
 }else if(y===5){
  // Thick three-by-three BRICK chimney ring built from real world blocks.
  // The centre (x=0,z=-7) is AIR, so there is a genuine open flue.
  for(const face of exposed)add([0,0,0],[16,16,16],'brick',[face]);
  if(exposed.includes('up')){
   add([0,14,0],[16,16,16],'roofTrim',['up']);
  }
  // Tall vertical corner ties: only on chimney's actual corners.
  if(Math.abs(x)===1&&(z===-8||z===-6)){
   for(const face of ['north','south','east','west'])
    if(exposed.includes(face)){
     const a=face==='west'?[0,0,0]:face==='east'?[15.2,0,0]:
       face==='north'?[0,0,0]:[0,0,15.2];
     const b=face==='west'?[.8,16,16]:face==='east'?[16,16,16]:
       face==='north'?[16,16,.8]:[16,16,16];
     add(a,b,'iron',[face]);
    }
  }
 }
 return blank(E);
}
export function roastPort(role,d){
 if(role!=='input'&&role!=='primary')throw Error('Unknown item port '+role);
 const E=[],add=(a,b,m,f)=>E.push(modelBox(a,b,m,f,d));
 add([0,0,0],[16,16,15],'stone',['north','south','up','down','east','west']);
 add([2,3,15.1],[14,13,15.35],'soot',['south']);
 add([4,4,15.4],[12,12,15.65],role==='input'?'input':'output',['south']);
 add([2,12.5,15.5],[14,14,15.8],'iron',['south']);
 add([2,2,15.5],[14,3.5,15.8],'iron',['south']);
 return blank(E);
}
