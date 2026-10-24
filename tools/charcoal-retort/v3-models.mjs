/**
 * Charcoal Retort V3 visual shell, preserving every real 3x4x3 block position.
 * Port models are temporary neighbors while the V2 face partitioner runs.
 * finalizeRetortV3() moves them back to models selected by the REAL port BEs.
 * No gameplay or per-frame rendering work is done in this generator.
 */
export function applyRetortV3(files) {
 const a="src/main/resources/assets/magneticraft2/",d="src/main/resources/data/magneticraft2/";
 const dirs=["south","west","north","east"];
 const rows=[["SSS","SSS","SCS"],["BYB","BAB","UBO"],["BBB","BAB","BBB"],["PPP","PAP","PPP"]];
 const textures={
  stone:"magneticraft2:block/settling_tank/foundation",metal:"magneticraft2:block/settling_tank/riveted_iron",
  join:"magneticraft2:block/settling_tank/bronze_joint",bronze:"magneticraft2:block/settling_tank/bronze",
  brick:"magneticraft2:block/charcoal_retort/refractory",roof:"magneticraft2:block/charcoal_retort/roof",
  soot:"magneticraft2:block/charcoal_retort/soot",vent:"magneticraft2:block/charcoal_retort/grille",
  off:"magneticraft2:block/charcoal_retort/hatch_off",on:"magneticraft2:block/charcoal_retort/hatch_on",
  wood:"minecraft:block/stripped_spruce_log",charcoal:"minecraft:block/coal_block",
  particle:"magneticraft2:block/charcoal_retort/refractory"
 };
 const shapes=new Map(),portShapes=new Map();
 const cells=[];for(let y=0;y<4;y++)for(let z=0;z<3;z++)for(let x=0;x<3;x++)cells.push({x:x-1,y,z:z-2,k:rows[y][z][x]});
 const cellKey=c=>[c.x,c.y,c.z].join(",");
 function add(map,c,mat,from,to){
  if(from.some((v,i)=>v<0||to[i]>16||to[i]<=v))throw Error("V3 model exceeded block bounds");
  if(!map.has(cellKey(c)))map.set(cellKey(c),[]);
  map.get(cellKey(c)).push({mat,from,to});
 }
 const solid=(c,mat,from,to)=>add(shapes,c,mat,from,to);
 const port=(c,mat,from,to)=>add(portShapes,c,mat,from,to);
 for(const c of cells){
  const {x,y,z,k}=c;
  if(k==="S"){
   solid(c,"stone",[0,0,0],[16,16,16]);
   if(z===0){
    solid(c,"metal",[0,1,14],[16,3,16]);
    solid(c,"metal",[0,12,14],[16,14,16]);
    if(x!==0)solid(c,"join",[2,14,14],[14,16,16]);
   } else if(x!==0&&z===-1){
    const xa=x===-1?0:14;
    solid(c,"metal",[xa,0,0],[xa+2,12,16]);
   }
  } else if(k==="C"){
   // Functional firebox at the controller: inset and visibly lit only while burning.
   solid(c,"stone",[0,0,0],[16,16,11]);
   solid(c,"metal",[2,2,11],[14,14,13]);
   solid(c,"soot",[3,3,13],[13,13,14]);
   solid(c,"off",[4,4,14],[12,12,15]);
   solid(c,"metal",[0,0,13],[3,16,16]);solid(c,"metal",[13,0,13],[16,16,16]);
   solid(c,"bronze",[3,13,13],[13,16,16]);solid(c,"bronze",[3,0,13],[13,3,16]);
   solid(c,"join",[6,1,15],[10,4,16]);
  } else if(k==="B"){
   const front=z===0,side=x!==0,rear=z===-2;
   // The important front openings are hollow/recessed masonry, not painted
   // faces on a full cube. The shoulder/corner cells also step backward.
   if(front&&x===0&&(y===1||y===2)){
    const opening=y===1?{lo:2,hi:14,bottom:2,top:15}:{lo:3,hi:13,bottom:6,top:12};
    solid(c,"brick",[0,0,0],[16,16,8]);
    solid(c,"brick",[0,0,8],[opening.lo,16,16]);
    solid(c,"brick",[opening.hi,0,8],[16,16,16]);
    solid(c,"brick",[opening.lo,0,8],[opening.hi,opening.bottom,16]);
    solid(c,"brick",[opening.lo,opening.top,8],[opening.hi,16,16]);
   }else if(front){
    solid(c,"brick",[0,0,0],[16,16,13]);
   }else if(side){
    // The two side walls are actually recessed behind their outer ribs.
    solid(c,"brick",[x===-1?1.5:0,0,0],[x===1?14.5:16,16,16]);
   }else{
    solid(c,"brick",[0,0,0],[16,16,16]);
   }
   if(side){
    // Actual corner buttresses instead of two layers of free-floating copper bands.
    const xa=x===-1?0:13;
    solid(c,"metal",[xa,0,0],[xa+3,3,16]);
    solid(c,"join",[xa,13,0],[xa+3,16,16]);
    if(front){
     solid(c,"bronze",[xa,3,13],[xa+3,15,16]);
     solid(c,"metal",[xa,0,14],[xa+3,3,16]);
    }else if(rear){
     solid(c,"metal",[xa,3,0],[xa+3,14,2]);
    }
   }
   if(front&&x===0&&y===1){
    // Dominant sealed door with copper jambs, side hinges and dark central grate.
    // A deep, wide opening. Dark recess sits behind discrete iron grating;
    // it is not another solid texture covering the whole opening.
    solid(c,"soot",[2.5,2.5,8],[13.5,14.5,10]);
    solid(c,"metal",[3,3,10],[13,13.5,11]);
    solid(c,"soot",[3.5,3.5,11],[12.5,12.5,12]);
    for(const xx of [4,7,10])solid(c,"metal",[xx,4,12],[xx+1,12,14]);
    solid(c,"metal",[3,3,12],[13,4,14]);
    solid(c,"metal",[3,12,12],[13,13,14]);
    solid(c,"bronze",[1,1,12],[3.5,15,16]);
    solid(c,"bronze",[12.5,1,12],[15,15,16]);
    solid(c,"bronze",[3.5,13.5,12],[12.5,16,16]);
    solid(c,"metal",[3.5,1,12],[12.5,3,16]);
    solid(c,"join",[11.5,7,15],[14,9,16]);
   } else if(front&&x===0&&y===2){
    // Small upper vent deliberately subordinate to the one-block door below.
    solid(c,"soot",[3,6,9],[13,12,11]);
    solid(c,"metal",[4,7,11],[12,11,12]);
    for(const xx of [4,7,10])solid(c,"metal",[xx,7,12],[xx+1,11,14]);
    solid(c,"bronze",[3,12,12],[13,14,16]);
    solid(c,"metal",[3,5,13],[13,6,16]);
    solid(c,"join",[2,5,13],[4,13,16]);
    solid(c,"join",[12,5,13],[14,13,16]);
   }
   if(side&&z===-1){
    if(x===-1)solid(c,"metal",[0,3,0],[2,5,16]);
    else solid(c,"metal",[14,3,0],[16,5,16]);
   }
  } else if(k==="P"){
   // ROOF-ONLY PASS: the two dark lines are GROOVES CUT INTO copper,
   // not raised iron rails. The same local-X trench spans all 3 Z cells
   // in each outer roof column, for every rotated machine orientation.
   // The 3x3 JSON still contains eight roof blocks plus chimney AIR.
   solid(c,"bronze",[0,0,0],[16,3,16]);
   solid(c,"roof",[0,3,0],[16,8,16]);  // supporting copper slab
   if(x===-1){
    // Left seam, cut 1.25px below the 10px copper walking surface.
    solid(c,"roof",[0,8,0],[12,10,16]);
    solid(c,"roof",[14,8,0],[16,10,16]);
    solid(c,"metal",[12,8,0],[14,8.75,16]);
   }else if(x===1){
    // Mirrored right seam. Copper vertical walls remain visible.
    solid(c,"roof",[0,8,0],[2,10,16]);
    solid(c,"roof",[4,8,0],[16,10,16]);
    solid(c,"metal",[2,8,0],[4,8.75,16]);
   }else{
    solid(c,"roof",[0,8,0],[16,10,16]);
   }
  } else if(k==="A"&&y===3){
   // V3.6: a CONTINUOUS COPPER ROOF around the center air throat.
   // All four model segments sit flush with adjacent Y=3 copper roof at
   // local Y=10. This is not a raised shelf/collar: the center A cell
   // simply has its own roof surface around the 6x6 open flue.
   // These vertical copper inner-wall faces also cover the otherwise
   // untextured-looking gap from the roof's underside to the chimney.
   for(const [f,t] of [
     [[0,0,0],[5,10,16]],  [[11,0,0],[16,10,16]],
     [[5,0,0],[11,10,5]], [[5,0,11],[11,10,16]]
   ])solid(c,"roof",f,t);
   // V3.5 chimney-only cleanup: block-local, INTEGER-pixel copper.
   // The copper walls begin directly at roof Y=10, without the extra
   // floating lower collar. All exposed undersides get normal face UVs.
   // Two real horizontal vent rows cut all four walls (Y=11..12,
   // Y=13..14); the middle 6x6 shaft stays open.
   const courses=[[10,11],[12,13],[14,15]];
   const gaps=[[11,12],[13,14]];
   // Continuous structural corners: 3px-long solid posts at front/back.
   for(const [xa,xb] of [[3,6],[10,13]]){
     for(const [za,zb] of [[3,5],[11,13]]){
       solid(c,"roof",[xa,10,za],[xb,15,zb]);
     }
   }
   // The two long front/back walls. The center of each window is AIR,
   // with short copper end posts retained on either side.
   for(const [za,zb] of [[3,5],[11,13]]){
     for(const [ya,yb] of courses)
       solid(c,"roof",[6,ya,za],[10,yb,zb]);
   }
   // Side walls match the front/back construction and have matching
   // two-row slots. No solid wall is hidden behind the apertures.
   for(const [xa,xb] of [[3,5],[11,13]]){
     for(const [ya,yb] of courses)
       solid(c,"roof",[xa,ya,5],[xb,yb,11]);
   }
   // Very small remaining wall returns at the ends of each vent row.
   // These are part of the chimney body, not decorative floating strips.
   for(const [ya,yb] of gaps){
     for(const [za,zb] of [[3,5],[11,13]]){
       solid(c,"roof",[5,ya,za],[6,yb,zb]);
       solid(c,"roof",[10,ya,za],[11,yb,zb]);
     }
     for(const [xa,xb] of [[3,5],[11,13]]){
       solid(c,"roof",[xa,ya,5],[xb,yb,6]);
       solid(c,"roof",[xa,ya,10],[xb,yb,11]);
     }
   }
   // A single 1px-thick copper top lip (Y=15..16), with a proper
   // downward face exposed on its overhang. No dark band beneath it.
   for(const [f,t] of [
     [[2,15,2],[5,16,14]], [[11,15,2],[14,16,14]],
     [[5,15,2],[11,16,5]], [[5,15,11],[11,16,14]]
   ])solid(c,"roof",f,t);
  } else if(["U","O","Y"].includes(k)){
   // These are not fillers. The real item port BE selects this baked model.
   // Left log hopper vs right charcoal drawer vs rear tar port remain distinct.
   const rear=k==="Y",input=k==="U";
   // Not the generic wooden bins: inset feed/collection chambers in
   // real port blocks, framed by the same kiln-facing metal/bronze.
   port(c,"metal",[0,0,0],[16,4,16]);
   port(c,"stone",[0,4,0],[16,6,12]);
   port(c,"soot",[2.5,6,6],[13.5,12,12]);
   port(c,input?"wood":k==="O"?"charcoal":"soot",[4,7,9],[12,9.5,12]);
   port(c,"bronze",[1,5,11],[3.5,14,16]);
   port(c,"bronze",[12.5,5,11],[15,14,16]);
   port(c,"bronze",[3.5,12.5,11],[12.5,15,16]);
   port(c,"metal",[3.5,5,12],[12.5,6.5,16]);
   if(rear){
    // Rear tar drain is a recessed metal fitting, not a third front hopper.
    port(c,"metal",[4,7,0],[12,12,3]);
    port(c,"soot",[5,8,0],[11,11,1]);
    port(c,"join",[6,9,0],[10,10,2]);
   }else{
    port(c,"join",[4,5,14],[12,6.5,16]);
   }
  }
 }
 const rotate=(x,z,dir)=>dir==="south"?[x,z]:dir==="west"?[-z,x]:dir==="north"?[-x,-z]:[z,-x];
 const rotatePixel=(x,z,dir)=>dir==="south"?[x,z]:dir==="west"?[16-z,x]:dir==="north"?[16-x,16-z]:[z,16-x];
 const uv=(f,t)=>({
  north:[16-t[0],16-t[1],16-f[0],16-f[1]],
  south:[f[0],16-t[1],t[0],16-f[1]],
  east:[16-t[2],16-t[1],16-f[2],16-f[1]],
  west:[f[2],16-t[1],t[2],16-f[1]],
  up:[f[0],f[2],t[0],t[2]],
  down:[f[0],16-t[2],t[0],16-f[2]]
 });
 function makeModel(parts,dir){
  return {parent:"minecraft:block/block",ambientocclusion:false,textures,
   elements:parts.map(box=>{
    const corners=[[box.from[0],box.from[2]],[box.from[0],box.to[2]],
      [box.to[0],box.from[2]],[box.to[0],box.to[2]]].map(([x,z])=>rotatePixel(x,z,dir));
    const f=[Math.min(...corners.map(p=>p[0])),box.from[1],Math.min(...corners.map(p=>p[1]))],
          t=[Math.max(...corners.map(p=>p[0])),box.to[1],Math.max(...corners.map(p=>p[1]))];
    return {from:f,to:t,faces:Object.fromEntries(Object.entries(uv(f,t)).map(([s,u])=>[s,{texture:"#"+box.mat,uv:u}]))};
   })
  };
 }
 const json=o=>JSON.stringify(o)+"\n";
 const ports=[];
 for(const dir of dirs)for(const c of cells){
  const [x,z]=rotate(c.x,c.z,dir);
  let parts=shapes.get(cellKey(c));
  const byPort=portShapes.get(cellKey(c));
  if(byPort)parts=byPort;
  if(!parts)continue;
  const path=a+"models/multiblock/charcoal_retort_"+dir+"/part_"+x+"_"+c.y+"_"+z+".json";
  const m=makeModel(parts,dir);
  // Keep only this layer's texture change. The other three layers use
  // their old shared material map and generated JSON byte-for-byte.
  if(c.y===3)m.textures={...m.textures,roof:"minecraft:block/cut_copper"};
  files.set(path,json(m));
  if(byPort){
   ports.push({dir,role:c.k==="U"?"input":c.k==="O"?"primary":"byproduct",path});
  }
  if(c.k==="C"){
   const lit=JSON.parse(JSON.stringify(m));
   for(const part of lit.elements)for(const face of Object.values(part.faces)){
    if(face.texture==="#off")face.texture="#on";
   }
   files.set(path.replace(".json","_lit.json"),json(lit));
   if(dir==="south"){
    files.set(a+"models/block/charcoal_retort_controller.json",json(m));
    files.set(a+"models/block/charcoal_retort_controller_lit.json",json(lit));
   }
  }
 }
 const physical=[];
 for(const c of cells){
  if(["S","B","P","C"].includes(c.k)){
   physical.push([c.x*16,c.y*16,c.z*16,(c.x+1)*16,c.y*16+(c.k==="P"?10:16),(c.z+1)*16]);
  }else if(c.k==="A"&&c.y===3){
   // Match the contiguous 10px roof seat, keep the 6x6 shaft empty.
   // Above the copper slab, four coarse chimney walls occupy y=10..16.
   for(const [f,t]of [
    [[0,0,0],[5,10,16]], [[11,0,0],[16,10,16]],
    [[5,0,0],[11,10,5]], [[5,0,11],[11,10,16]],
    [[3,10,3],[5,16,13]], [[11,10,3],[13,16,13]],
    [[5,10,3],[11,16,5]], [[5,10,11],[11,16,13]]
   ])physical.push([...f.map((v,i)=>v+[c.x,c.y,c.z][i]*16),...t.map((v,i)=>v+[c.x,c.y,c.z][i]*16)]);
  }
 }
 files.set(d+"multiblock_voxels/charcoal_retort.json",json({
  format:1,unit:"model_pixels",collision:physical,selection:physical,zones:{}
 }));
 return {ports,modelCells:shapes.size,physicalBoxes:physical.length};
}
export function finalizeRetortV3(files,ports){
 for(const {dir,role,path}of ports){
  const target="src/main/resources/assets/magneticraft2/models/multiblock/charcoal_retort_"+
   dir+"/port_"+role+".json";
  const model=files.get(path);
  if(!model)throw Error("Missing baked formed-state real port "+path);
  files.set(target,model);
  files.delete(path); // critical: the real block, not a V2 filler, renders this model
 }
}
/**
 * Shade exposed INNER wall faces without inserting coplanar texture plates.
 * This runs AFTER the V2 geometry partitioner so its mixed-material faces
 * cannot be mistaken for overlapping solid cuboids during generation.
 * The 6x6 chimney aperture is symmetric under all horizontal rotations.
 */
export function textureRetortChimneyInteriors(files) {
 const prefix="src/main/resources/assets/magneticraft2/models/multiblock/";
 let changed=0;
 for(const [dir,offset] of Object.entries({
   south:"0_3_-1",north:"0_3_1",west:"1_3_0",east:"-1_3_0"
 })){
   const path=prefix+"charcoal_retort_"+dir+"/part_"+offset+".json";
   const model=JSON.parse(files.get(path)||"null");
   if(!model||!Array.isArray(model.elements))throw Error("No chimney mesh for "+dir);
   let shaded=0;
   for(const element of model.elements){
     const f=element.from,t=element.to;
     // Assign soot exactly to *visible* inward faces on the shaft wall.
     // Existing face UVs remain in 1-model-pixel-per-texel scale.
     const inside=[
       ["east",t[0]===5,f[2]>=5&&t[2]<=11],
       ["west",f[0]===11,f[2]>=5&&t[2]<=11],
       ["south",t[2]===5,f[0]>=5&&t[0]<=11],
       ["north",f[2]===11,f[0]>=5&&t[0]<=11]
     ];
     for(const [side,onPlane,inSpan] of inside){
       if(onPlane&&inSpan&&f[1]>=0&&t[1]<=15&&element.faces[side]){
         element.faces[side].texture="#roof";
         shaded++;
       }
     }
   }
   if(shaded===0)throw Error("Chimney missing inner copper wall faces: "+dir);
   files.set(path,JSON.stringify(model)+"\n");
   changed++;
 }
 return changed;
}
