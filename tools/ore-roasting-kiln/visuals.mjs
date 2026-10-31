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

function aperturePanel(add,x,y0,y1,z0,z1,material,depth=14.0){
 const face=faceSide(x);
 const a=x===2?[depth,y0,z0]:[16-depth-.18,y0,z0];
 const b=x===2?[depth+.18,y1,z1]:[16-depth,y1,z1];
 add(a,b,material,[face]);
}
function arch(cell,add){
 const {x,y,z}=cell;
 // A single arch spans two adjacent Z cells. The earlier implementation
 // reversed the outer jamb positions and put pillars on the SHARED seam.
 const frontHalf=rowStarts.has(z); // z=-1,-4,-7, outer edge at local Z=16
 const backHalf=rowEnds.has(z);    // z=-2,-5,-8, outer edge at local Z=0
 if(!frontHalf&&!backHalf)throw Error('Non-arch roasting segment '+z);

 // An opaque full-cell fire-chamber backplate prevents sky/terrain
 // leaking around the grilles. Recess is inboard of actual masonry.
 aperturePanel(add,x,0,16,0,16,'soot',13.0);

 if(y===1){
  aperturePanel(add,x,3,16,0,16,'ember',13.65);
  aperturePanel(add,x,0,3,0,16,'refractory',15.25);
  aperturePanel(add,x,3,4,0,16,'iron',15.4);
 }else if(y===2){
  aperturePanel(add,x,0,16,0,16,'ember',13.65);
 }else if(y===3){
  // The whole curved crown belongs to ONE two-cell-wide opening.
  // Narrowed shoulders appear only at the true outer corners.
  aperturePanel(add,x,0,9,0,16,'ember',13.65);
  aperturePanel(add,x,12,16,0,16,'brick',15.4);
  aperturePanel(add,x,10,12,0,16,'refractory',15.55);
  if(frontHalf){
   aperturePanel(add,x,0,11,13,16,'brick',15.5);
   aperturePanel(add,x,7,12,9,13,'refractory',15.5);
  }
  if(backHalf){
   aperturePanel(add,x,0,11,0,3,'brick',15.5);
   aperturePanel(add,x,7,12,3,7,'refractory',15.5);
  }
  // Split keystone spans the shared seam without creating a centre pillar.
  if(frontHalf)aperturePanel(add,x,9,13,0,4,'refractory',15.7);
  if(backHalf)aperturePanel(add,x,9,13,12,16,'refractory',15.7);
 }
 if(y===1||y===2){
  // Full-height OUTER jambs only. Do not fill the middle seam.
  if(frontHalf)aperturePanel(add,x,0,16,13,16,'brick',15.5);
  if(backHalf)aperturePanel(add,x,0,16,0,3,'brick',15.5);

  // Real iron grille bars tie the lower sill to the stone arch crown.
  for(const zz of [5,11])
   aperturePanel(add,x,4,16,zz,zz+1,'grate',15.7);
 }
}

/**
 * PILOT: one 2x3-block roasting aperture (right side, nearest front).
 * The remaining five apertures deliberately retain their old appearance
 * until this bay passes straight-on and grazing-angle inspection.
 * Cuboids use existing 16px textures and have physical return faces.
 */
export function isPilotBay(c){
 return c.x===2&&c.y>=1&&c.y<=3&&(c.z===-2||c.z===-1);
}
function solidBayBox(add,from,to,mat){
 add(from,to,mat,['north','south','east','west','up','down']);
}
function pilotRoastingBay(c,add){
 if(!isPilotBay(c))throw Error('Not the chosen pilot bay');
 const backHalf=c.z===-2;
 // Exterior +X=16. Six-pixel-deep cavity; no paper-thin overlays.
 solidBayBox(add,[0,0,0],[9,16,16],'brick');
 solidBayBox(add,[9,0,0],[10,16,16],'soot');
 solidBayBox(add,[10,0,0],[10.5,c.y===3?12:16,16],'ember');
 // The jambs are on the OUTER ends, not on the shared Z seam.
 if(backHalf)solidBayBox(add,[9,0,0],[16,16,3],'brick');
 else solidBayBox(add,[9,0,13],[16,16,16],'brick');
 if(c.y===1){
  // Connected to the actual stone-brick foundation below.
  solidBayBox(add,[10,0,0],[16,3,16],'refractory');
  solidBayBox(add,[10,3,0],[16,4,16],'iron');
 }
 if(c.y===3){
  // Substantial stepped upper arch, with side shoulder and broad crown.
  solidBayBox(add,[9,12,0],[16,16,16],'brick');
  if(backHalf)solidBayBox(add,[9,8,3],[16,12,7],'refractory');
  else solidBayBox(add,[9,8,9],[16,12,13],'refractory');
 }
 // 3px-deep bars tie into the lower sill and the upper masonry crown.
 const low=c.y===1?4:0,high=c.y===3?12:16;
 for(const zBar of (backHalf?[7,13]:[2,8]))
  solidBayBox(add,[11.5,low,zBar],[14.5,high,zBar+1.25],'iron');
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
  const window=side&&openings.has(z),pilot=isPilotBay(c);
  for(const face of exposed){
   if(pilot)continue; // pilot masonry supplies all solid return faces
   if(window&&face===faceSide(x)){
    if(x===2)add([0,0,0],[12.25,16,16],'brick',[face]);
    else add([3.75,0,0],[16,16,16],'brick',[face]);
   }else add([0,0,0],[16,16,16],face==='up'?'refractory':'brick',[face]);
  }
  if(pilot)pilotRoastingBay(c,add);
  else if(window)arch(c,add);
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
 // One integrated full-block metal inset in the real foundation port.
 add([0,0,0],[16,16,15],'stone',['north','south','up','down','east','west']);
 add([1,1,15.05],[15,15,15.3],'iron',['south']);
 add([2.5,2.5,15.35],[13.5,13.5,15.55],'soot',['south']);
 add([4,5,15.6],[12,11,15.85],role==='input'?'input':'output',['south']);
 add([2,13.5,15.55],[14,14.7,15.85],'iron',['south']);
 add([2,1.3,15.55],[14,2.5,15.85],'iron',['south']);
 return blank(E);
}
