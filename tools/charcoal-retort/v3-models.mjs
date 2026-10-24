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
   // The roof reads as a substantial separate copper cap; not flat brick cladding.
   // Roof field sits noticeably below the chimney lip: visible height,
   // fewer flat planes, and a supported copper overhang/cap silhouette.
   solid(c,"brick",[0,0,0],[16,3,16]);
   solid(c,"bronze",[0,3,0],[16,5,16]);
   solid(c,"roof",[0,5,0],[16,8,16]);
   if(x!==0){
    const xa=x===-1?0:13;
    solid(c,"roof",[xa,8,0],[xa+3,12,16]);
    solid(c,"metal",[xa,3,0],[xa+2,5,16]);
   }
   if(z===0||z===-2){
    const za=z===-2?0:13;
    solid(c,"bronze",[x===-1?3:0,8,za],[x===1?13:16,12,za+3]);
    if(x===0)solid(c,"metal",[0,3,za],[16,5,za+1]);
   }
   if(x===0&&z!==-1)solid(c,"metal",[7.2,8,3],[8.8,9,13]);
  } else if(k==="A"&&y===3){
   // Hollow raised chimney with open central air shaft and distinct side vents.
   solid(c,"bronze",[2,2,2],[5,16,14]);
   solid(c,"bronze",[11,2,2],[14,16,14]);
   solid(c,"bronze",[5,2,2],[11,16,5]);
   solid(c,"bronze",[5,2,11],[11,16,14]);
   solid(c,"metal",[2,11,2],[5,13,14]);
   solid(c,"metal",[11,11,2],[14,13,14]);
   solid(c,"metal",[5,11,2],[11,13,5]);
   solid(c,"metal",[5,11,11],[11,13,14]);
   // Inner shaft remains open. Dark lining sits below the rim with no lid.
   solid(c,"soot",[5,1,5],[11,2,11]);
   for(const [f,t]of [
     [[2,14,2],[5,16,14]],[[11,14,2],[14,16,14]],
     [[5,14,2],[11,16,5]],[[5,14,11],[11,16,14]]
   ])solid(c,"metal",f,t);
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
   physical.push([c.x*16,c.y*16,c.z*16,(c.x+1)*16,c.y*16+(c.k==="P"?14:16),(c.z+1)*16]);
  }else if(c.k==="A"&&c.y===3){
   for(const [f,t]of [
    [[2,2,2],[5,16,14]],[[11,2,2],[14,16,14]],
    [[5,2,2],[11,16,5]],[[5,2,11],[11,16,14]]
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