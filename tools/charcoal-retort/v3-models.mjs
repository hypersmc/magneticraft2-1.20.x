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
   solid(c,"brick",[0,0,0],[16,16,16]);
   const front=z===0,side=x!==0,rear=z===-2;
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
    solid(c,"soot",[2,2,12],[14,15,14]);
    solid(c,"metal",[3,3,14],[13,14,15]);
    solid(c,"vent",[4,4,15],[12,12,16]);
    solid(c,"bronze",[0,1,13],[3,15,16]);
    solid(c,"bronze",[13,1,13],[16,15,16]);
    solid(c,"bronze",[3,13,13],[13,16,16]);
    solid(c,"metal",[3,1,13],[13,3,16]);
    solid(c,"join",[11,7,15],[14,10,16]);
   } else if(front&&x===0&&y===2){
    // Small upper vent deliberately subordinate to the one-block door below.
    solid(c,"metal",[2,5,14],[14,12,16]);
    solid(c,"soot",[3,6,13],[13,11,14]);
    solid(c,"vent",[4,7,15],[12,10,16]);
    solid(c,"bronze",[2,12,14],[14,14,16]);
    solid(c,"join",[1,4,14],[3,13,16]);
    solid(c,"join",[13,4,14],[15,13,16]);
   }
   if(side&&z===-1){
    if(x===-1)solid(c,"metal",[0,3,0],[2,5,16]);
    else solid(c,"metal",[14,3,0],[16,5,16]);
   }
  } else if(k==="P"){
   // The roof reads as a substantial separate copper cap; not flat brick cladding.
   solid(c,"brick",[0,0,0],[16,4,16]);
   solid(c,"bronze",[0,4,0],[16,8,16]);
   solid(c,"roof",[0,8,0],[16,11,16]);
   if(x!==0){
    const xa=x===-1?0:13;
    solid(c,"roof",[xa,11,0],[xa+3,14,16]);
    solid(c,"metal",[xa,4,0],[xa+2,6,16]);
   }
   if(z===0||z===-2){
    const za=z===-2?0:13;
    solid(c,"bronze",[x===-1?3:0,11,za],[x===1?13:16,14,za+3]);
    if(x===0)solid(c,"metal",[0,4,za],[16,6,za+1]);
   }
   if(x===0&&z!==-1)solid(c,"metal",[7.2,11,3],[8.8,12,13]);
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
   solid(c,"soot",[5,2,5],[11,3,11]);
  } else if(["U","O","Y"].includes(k)){
   // These are not fillers. The real item port BE selects this baked model.
   // Left log hopper vs right charcoal drawer vs rear tar port remain distinct.
   const rear=k==="Y",input=k==="U";
   port(c,"metal",[0,0,0],[16,3,16]);
   port(c,"stone",[0,3,0],[16,5,16]);
   port(c,"soot",[2,5,2],[14,8,14]);
   port(c,input?"wood":k==="O"?"charcoal":"soot",[3,7,3],[13,11,13]);
   port(c,"bronze",[0,5,0],[3,15,16]);
   port(c,"bronze",[13,5,0],[16,15,16]);
   port(c,"bronze",[3,12,0],[13,15,3]);
   port(c,"bronze",[3,12,13],[13,15,16]);
   port(c,"join",[3,5,rear?0:13],[13,7,rear?3:16]);
   if(rear){
    port(c,"metal",[5,7,0],[11,12,3]);
    port(c,"soot",[6,8,0],[10,11,1]);
   }else{
    port(c,"metal",[4,0,14],[12,3,16]);
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