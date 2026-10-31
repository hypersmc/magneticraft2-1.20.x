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
 grate:'minecraft:block/iron_block',
 bronze:'minecraft:block/cut_copper',
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
 const {x,z}=c;
 const outerFront=z===0,outerRear=z===-8;
 const openSouth=x===0&&z===-8;
 const openNorth=x===0&&z===-6;
 for(let strip=0;strip<4;strip++){
  const from=strip*4,to=from+4,gx=(x+2)*16+from;
  const h=roofStep(gx+2);
  const prev=gx===0?0:roofStep(gx-2);
  const next=gx===76?0:roofStep(gx+6);
  const mat=outerFront||outerRear||z===-3||z===-6?'roofTrim':'roof';

  // A solid, connected stepped roof course, not a single unbacked top quad.
  // Model faces for each raised riser close every join between adjacent strips.
  add([from,0,0],[to,h,16],mat,['up']);
  if(h>prev)add([from,prev,0],[to,h,16],mat,['west']);
  if(h>next)add([from,next,0],[to,h,16],mat,['east']);

  // End gables and the sides of the real chimney opening. No open sky
  // should be visible through a roof edge, except the intentional flue.
  const ends=[];
  if(outerFront||openSouth)ends.push('south');
  if(outerRear||openNorth)ends.push('north');
  if(ends.length)add([from,0,0],[to,h,16],
    outerFront?'refractory':outerRear?'brick':mat,ends);

  // Every cross-rib sits DIRECTLY on the local roof course, not at
  // roofTop(x), which previously differed from the actual stepped slope.
  if((z===-2||z===-5)&&h<16)
   add([from,h,6],[to,Math.min(16,h+1),10],
       'iron',['up','north','south']);

  // Central ridge caps also follow the top of their owning strips.
  if(x===0&&h>=14&&h<16)
   add([from,h,0],[to,Math.min(16,h+1),16],'roofTrim',['up']);
 }
 // Continuous eaves remain connected to the masonry below.
 if(Math.abs(x)===2)
  add([0,0,0],[16,2,16],'roofTrim',[x===2?'east':'west']);
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
 if(!isRoastingBay(c))throw Error('Out-of-bounds roaster arch');
 const left=c.x===-2,backHalf=rowEnds.has(c.z);
 const outerA=backHalf?0:13,outerB=backHalf?3:16;
 const innerA=backHalf?3:0,innerB=backHalf?16:13;
 const p=(a,b,m)=>box6(add,a,b,m,left);
 // Back wall is nine pixels of structural brick. Its interior is a flat,
 // fully opaque dark wall; no daylight can shine between the grille bars.
 p([0,0,0],[9,16,16],'brick');
 p([9,0,0],[10,16,16],'soot');
 // The warmed ore bed is only low in the cavity, not a three-block-high
 // blanket of noisy custom "embers". The rest remains dark inside.
 if(c.y===1)p([10,4,innerA],[11,8,innerB],'ember');
 // These outer jambs touch the actual brick wall and are on the outside
 // of the TWO-block bay, not between its adjoining cells.
 p([10,0,outerA],[16,16,outerB],'brick');
 if(c.y===1){
  // Sill fills only the OPEN Z span, so it never overlaps the jamb.
  p([10,0,innerA],[16,3,innerB],'refractory');
  p([10,3,innerA],[16,4,innerB],'iron');
 }
 if(c.y===3){
  // Arch crown also ends where the jamb begins, with no coplanar
  // same-direction outer surface. Shoulder is set below the crown.
  p([10,12,innerA],[16,16,innerB],'brick');
  const shoulder=backHalf?[3,7]:[9,13];
  p([10,8,shoulder[0]],[16,12,shoulder[1]],'refractory');
 }
 // Real 3px-deep iron bars (vanilla iron texture) stand clear of the
 // brick and fire bed; they terminate at the sill and arch crown.
 const low=c.y===1?4:0,high=c.y===3?12:16;
 for(const z of (backHalf?[8,12]:[3.5,7]))
  p([12,low,z],[15,high,z+1.25],'grate');
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
  const bay=isRoastingBay(c);
  const pierRib=side&&!bay&&(ribs.has(z)||front||back);
  if(bay){
   // Every bay has a complete masonry recess; do not cover it with
   // the old exterior wall sheet.
   volumeArch(c,add);
  }else for(const face of exposed){
   if(pierRib&&face===faceSide(x)){
    // Brick exterior starts AFTER the 3px-wide iron pier: never
    // render brick and iron at the same outward-facing depth.
    add([0,0,3],[16,16,16],'brick',[face]);
   }else add([0,0,0],[16,16,16],face==='up'?'refractory':'brick',[face]);
  }
  // Only wall PIERS receive these external ribs. Arch outer jambs
  // already have their own frame and must not overlap another rib.
  if(pierRib){
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
  // All roof cells carry the pitched tiles; under the chimney, real brick
  // piers meet the upper chimney wall without an unsupported vertical gap.
  addRoof(E,add,c);
  if(chimneyFoot(x,z)){
   const wallFaces=[];
   for(const face of ['north','south','east','west']){
    const step=offsets[face];
    if(!chimneyFoot(x+step[0],z+step[2]))wallFaces.push(face);
   }
   if(wallFaces.length)add([0,0,0],[16,16,16],'brick',wallFaces);
  }
 }else if(y>=5&&y<=7){
  // Three real courses of brick above the pitched roof. The 3x3 ring
  // preserves a real 1x1 open AIR flue through every level.
  // The cap is a separate last course, not overlapped coplanar decals.
  for(const face of exposed){
   if(y===7){
    add([0,0,0],[16,13,16],'brick',[face]);
    add([0,13,0],[16,16,16],'roofTrim',[face]);
   }else add([0,0,0],[16,16,16],'brick',[face]);
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
