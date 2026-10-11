#!/usr/bin/env node
// Generated asset builder: source models may exceed vanilla BlockElement bounds.
// Keep the canonical source outside assets/models so Minecraft never loads it directly.
import fs from "node:fs";
import path from "node:path";
const FACE=["north","east","south","west","up","down"], EPS=1e-6;
function rot(x,z,n){for(let i=0;i<n;i++){const tmp=x;x=-z;z=tmp;}return [x,z];}

function rotBox(box,n){
 const p=[[box[0],box[2]],[box[0],box[5]],[box[3],box[2]],[box[3],box[5]]].map(([x,z])=>{
   const [xx,zz]=rot(x-8,z-8,n);return[xx+8,zz+8];
 });
 return [Math.min(...p.map(q=>q[0])),box[1],Math.min(...p.map(q=>q[1])),Math.max(...p.map(q=>q[0])),box[4],Math.max(...p.map(q=>q[1]))];
}

function rotFace(f,n){return f==="up"||f==="down"?f:FACE[(FACE.indexOf(f)+n)%4];}

function uvAxes(f){return f==="north"||f==="south"?[0,1]:f==="east"||f==="west"?[2,1]:[0,2];}

function faceNormal(f){return {north:[2,-1],east:[0,1],south:[2,1],west:[0,-1],up:[1,1],down:[1,-1]}[f];}

function generate(id,model,variants){
 const output=new Map(),stats={};
 for(const [n,dir] of ["south","west","north","east"].entries()){
  const bp=variants[dir];if(!bp)continue;
  const st=bp.structure,[w,h,d]=st.dimensions,controllers=[],occupied=[];
  for(let y=0;y<h;y++)for(let z=0;z<d;z++)for(let x=0;x<w;x++){
   const k=st.layout[`layer${y+1}`]?.[z]?.[x],name=st.blocks[k];
   if(!name)throw Error(`Undefined block ${k} at ${x},${y},${z}`);
   if(name===`magneticraft2:${id}`)controllers.push([x,y,z]);
   if(name!=="minecraft:air")occupied.push([x,y,z]);
  }
  if(controllers.length!==1)throw Error(`Expected one controller in ${id}_${dir}`);
  const [cx,cy,cz]=controllers[0],hosts=occupied.map(([x,y,z])=>[x-cx,y-cy,z-cz]);
  const pieces=new Map(hosts.map(a=>[a.join("_"),[]]));
  let total=0,borrowed=0;
  for(const el of model.elements){
   if(el.rotation)throw Error(`Animated or rotated element requires a separate component: ${JSON.stringify(el.rotation)}`);
   const bounds=rotBox([...el.from,...el.to],n);
   const [x0,y0,z0,x1,y1,z1]=bounds;
   if(Math.min(x1-x0,y1-y0,z1-z0)<=EPS)throw Error("Zero-volume source cuboid");
   for(let iy=Math.floor(y0/16);iy<Math.ceil((y1-EPS)/16);iy++)
   for(let iz=Math.floor(z0/16);iz<Math.ceil((z1-EPS)/16);iz++)
   for(let ix=Math.floor(x0/16);ix<Math.ceil((x1-EPS)/16);ix++){
    const cell=[ix,iy,iz];
    const b=[Math.max(x0,ix*16),Math.max(y0,iy*16),Math.max(z0,iz*16),
      Math.min(x1,(ix+1)*16),Math.min(y1,(iy+1)*16),Math.min(z1,(iz+1)*16)];
    if(Math.min(b[3]-b[0],b[4]-b[1],b[5]-b[2])<=EPS)continue;
    const candidates=hosts.filter(a=>b.every((v,i)=>v-a[i%3]*16>=-16-EPS&&v-a[i%3]*16<=32+EPS))
      .sort((a,c)=>a.reduce((s,v,i)=>s+Math.abs(v-cell[i]),0)-c.reduce((s,v,i)=>s+Math.abs(v-cell[i]),0)
        ||a.join("_").localeCompare(c.join("_")));
    if(!candidates.length)throw Error(`No nearby structure host for geometry ${cell} in ${id}_${dir}`);
    const anchor=candidates[0],faces={};
    for(const [oldFace,info] of Object.entries(el.faces||{})){
      const f=rotFace(oldFace,n),[axis,sign]=faceNormal(f);
      if(Math.abs((sign<0?b[axis]:b[axis+3])-(sign<0?bounds[axis]:bounds[axis+3]))>EPS)continue;
      const [uAxis,vAxis]=uvAxes(f),dx=b[uAxis+3]-b[uAxis],dy=b[vAxis+3]-b[vAxis];
      let [u0,v0,u1,v1]=info.uv||[];
      if([u0,v0,u1,v1].some(v=>typeof v!=="number"))throw Error("Missing numeric UV");
      if((oldFace==="up"||oldFace==="down")&&(n%2===1))[u0,v0,u1,v1]=[v0,u0,v1,u1];
      if(Math.abs((u1-u0)-(bounds[uAxis+3]-bounds[uAxis]))>EPS ||
         Math.abs((v1-v0)-(bounds[vAxis+3]-bounds[vAxis]))>EPS)
          throw Error(`Source UV distortion at ${id}_${dir}:${oldFace} -> ${f}`);
      const wrap=x=>((x%16)+16)%16;
      const u=wrap(u0+b[uAxis]-bounds[uAxis]),v=wrap(v0+b[vAxis]-bounds[vAxis]);
      if(u+dx>16+EPS||v+dy>16+EPS)
          throw Error(`${id}_${dir}:${f} texture crosses an atlas tile boundary; split source cuboid.`);
      faces[f]={...info,uv:[u,v,Math.min(16,u+dx),Math.min(16,v+dy)].map(q=>+q.toFixed(5))};
    }
    if(!Object.keys(faces).length)continue;
    const rebased=b.map((value,i)=>+(value-anchor[i%3]*16).toFixed(5));
    if(rebased.some(v=>v < -16-EPS||v > 32+EPS))throw Error("Out-of-range model part");
    pieces.get(anchor.join("_")).push({from:rebased.slice(0,3),to:rebased.slice(3),faces});
    total++;
    if(cell.some((v,i)=>v!==anchor[i]))borrowed++;
   }
  }
  if(!pieces.get("0_0_0")?.length)throw Error("Controller must have visible geometry");
  for(const [key,elements] of pieces){
   const out={parent:"minecraft:block/block",ambientocclusion:false,textures:model.textures,elements};
   output.set(`src/main/resources/assets/magneticraft2/models/multiblock/${id}_${dir}/part_${key}.json`,
      JSON.stringify(out,null,2)+"\n");
  }
  stats[dir]={pieces:total,partModels:pieces.size,borrowedSegments:borrowed};
 }
 return {files:output,stats};
}

function stressTest(){
 const size=9,height=5,layer=(y)=>Array.from({length:size},(_,z)=>Array.from({length:size},(_,x)=>x===4&&z===4&&y===0?"C":"B"));
 const spec={structure:{dimensions:[size,height,size],
 layout:Object.fromEntries(Array.from({length:height},(_,y)=>[`layer${y+1}`,layer(y)])),
 blocks:{B:"minecraft:bricks",C:"magneticraft2:stress_multiblock"}}};
 const full={textures:{brick:"minecraft:block/bricks",particle:"minecraft:block/bricks"},
 elements:[{from:[-64,0,-64],to:[80,16,80],faces:{
 north:{uv:[0,0,144,16],texture:"#brick"},south:{uv:[0,0,144,16],texture:"#brick"},
 east:{uv:[0,0,144,16],texture:"#brick"},west:{uv:[0,0,144,16],texture:"#brick"},
 up:{uv:[0,0,144,144],texture:"#brick"},down:{uv:[0,0,144,144],texture:"#brick"}
 }}]};
 const built=generate("stress_multiblock",full,{south:spec});
 if(built.stats.south.partModels!==405)throw Error("Stress model must have 405 part hosts");
 for(const raw of built.files.values()){
  const contents=JSON.parse(raw);
  for(const el of contents.elements){
   if([...el.from,...el.to].some(v=>v<-16-EPS||v>32+EPS))throw Error("Oversize element in large structure");
   for(const face of Object.values(el.faces)){
    if(face.uv.some(v=>v < -EPS||v>16+EPS))throw Error("Out-of-range UV on large model");
   }
  }
 }
 return built.stats.south;
}
const args=new Set(process.argv.slice(2));
if(args.has("--self-test")){
 console.log("9x5x9 virtual structure:",JSON.stringify(stressTest()));
 process.exit(0);
}
const cwd=process.cwd();
const sourceDir=path.join(cwd,"tools/multiblock-models/sources");
const dirs=["south","west","north","east"];
const ids=fs.readdirSync(sourceDir).filter(name=>name.endsWith(".json"))
 .map(name=>name.slice(0,-5)).sort();
if(!ids.length)throw Error("No canonical multiblock models found");
let errors=0;
for(const id of ids){
 const src=JSON.parse(fs.readFileSync(path.join(sourceDir,id+".json"),"utf8"));
 const layouts=Object.fromEntries(dirs.map(dir=>{
  const structurePath=path.join(cwd,"src/main/resources/data/magneticraft2/multiblocks/"+id+"_"+dir+".json");
  if(!fs.existsSync(structurePath))throw Error("Missing directional multiblock JSON: "+structurePath);
  return [dir,JSON.parse(fs.readFileSync(structurePath,"utf8"))];
 }));
 const built=generate(id,src,layouts);
 const expected=new Set(built.files.keys());
 for(const dir of dirs){
  const partDir=path.join(cwd,"src/main/resources/assets/magneticraft2/models/multiblock/"+id+"_"+dir);
  if(!fs.existsSync(partDir))continue;
  for(const filename of fs.readdirSync(partDir)){
   if(!/^part_-?\d+_-?\d+_-?\d+\.json$/.test(filename))continue;
   const relative=path.relative(cwd,path.join(partDir,filename)).split(path.sep).join("/");
   if(expected.has(relative))continue;
   if(args.has("--check")){
    console.error("Stale generated model:",relative);errors++;
   }else{
    fs.unlinkSync(path.join(partDir,filename));
    console.log("Removed stale model:",relative);
   }
  }
 }
 for(const [relative,contents] of built.files){
  const filepath=path.join(cwd,relative);
  if(args.has("--check")){
   if(!fs.existsSync(filepath)||fs.readFileSync(filepath,"utf8")!==contents){
    console.error("Mismatch:",relative);errors++;
   }
  }else{
   fs.mkdirSync(path.dirname(filepath),{recursive:true});
   fs.writeFileSync(filepath,contents,"utf8");
  }
 }
 console.log("Generated "+id+":",JSON.stringify(built.stats));
}
if(errors)process.exit(1);
