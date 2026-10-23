/** Compact 3x3x5 Rotary Ore Classifier authoritative JSON exporter. Node 18+; no dependencies. */
import {readFileSync,writeFileSync,existsSync,mkdirSync,readdirSync,rmSync} from 'node:fs';
import {join,dirname,resolve} from 'node:path';
import {fileURLToPath} from 'node:url';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'../..');
const source=JSON.parse(readFileSync(join(root,'tools/rotary-classifier/frame-design.json'),'utf8'));
function generate(design){
 const A="src/main/resources/assets/magneticraft2/",D="src/main/resources/data/magneticraft2/",files=new Map();
 const F=["south","west","north","east"],round=n=>Math.round(n*10000)/10000;
 const write=(p,j)=>{if(files.has(p))throw Error("Duplicate "+p);files.set(p,JSON.stringify(j,null,2)+"\n")};
 const cells=[];for(let y=0;y<3;y++)for(let z=-3;z<=1;z++)for(let x=-1;x<=1;x++)cells.push({x,y,z,key:design.layers[y][z+3][x+1]});
 const air=new Set(design.anchors.map(p=>p.join(","))),hosts=cells.filter(c=>c.key==="A"?air.has([c.x,c.y,c.z].join(",")):!"UIOY".includes(c.key));
 if(hosts.length!==34)throw Error("Wrong render host count "+hosts.length);
 const turn=(x,z,f)=>f==="south"?[x,z]:f==="west"?[16-z,x]:f==="north"?[16-x,16-z]:[z,16-x];
 const offset=(x,z,f)=>f==="south"?[x,z]:f==="west"?[-z,x]:f==="north"?[-x,-z]:[z,-x];
 const boxRot=(b,f)=>{const q=[[b.from[0],b.from[2]],[b.from[0],b.to[2]],[b.to[0],b.from[2]],[b.to[0],b.to[2]]].map(p=>turn(...p,f));return{...b,from:[Math.min(...q.map(p=>p[0])),b.from[1],Math.min(...q.map(p=>p[1]))],to:[Math.max(...q.map(p=>p[0])),b.to[1],Math.max(...q.map(p=>p[1]))]}};
 function staticModel(boxes,c){
  const min=[c.x*16,c.y*16,c.z*16],max=min.map(v=>v+16),els=[];
  for(const b of boxes){
   const lo=b.from.map((v,i)=>Math.max(v,min[i])),hi=b.to.map((v,i)=>Math.min(v,max[i]));
   if(hi.some((v,i)=>v-lo[i]<1e-4))continue;
   const from=lo.map((v,i)=>round(v-min[i])),to=hi.map((v,i)=>round(v-min[i])),faces={};
   for(const [s,axis,end] of [["west",0,false],["east",0,true],["down",1,false],["up",1,true],["north",2,false],["south",2,true]]){
    const v=end?hi[axis]:lo[axis],outer=end?b.to[axis]:b.from[axis];if(Math.abs(v-outer)>1e-4)continue;
    const uv=s==="up"||s==="down"?[from[0],from[2],to[0],to[2]]:s==="north"||s==="south"?[from[0],16-to[1],to[0],16-from[1]]:[from[2],16-to[1],to[2],16-from[1]];
    if(uv[2]-uv[0]>16.0001||uv[3]-uv[1]>16.0001)throw Error("UV stretch "+b.name);
    faces[s]={texture:"#"+b.material,uv:uv.map(round)};
   }
   if(Object.keys(faces).length)els.push({from,to,faces});
  }
  if(els.length>128)throw Error("Static model too complex: "+els.length);
  return {parent:"minecraft:block/block",ambientocclusion:false,textures:design.textures,elements:els};
 }
 const element=(mat,from,to,angle)=>{
  const sizes=to.map((v,i)=>v-from[i]);if(sizes.some(v=>v<=0||v>16.0001))throw Error("Drum component >16px or inverted: "+mat+" "+from+" "+to);
  const faces={};for(const side of ["north","south","east","west","up","down"]){
   const width=side==="north"||side==="south"?sizes[0]:side==="east"||side==="west"?sizes[2]:sizes[0];
   const height=side==="up"||side==="down"?sizes[2]:sizes[1];
   faces[side]={texture:"#"+mat,uv:[0,0,round(width),round(height)]};
  }
  const result={from:from.map(round),to:to.map(round),faces};
  // A missing rotation is represented by no field at all. Serializing an
  // explicit rotation.angle:null breaks BlockElement deserialization in 1.20.1.
  if (angle !== null && angle !== undefined) {
   if (!Number.isFinite(angle) || ![-45,-22.5,0,22.5,45].includes(angle))
    throw Error("Invalid vanilla model rotation angle: "+angle);
   result.rotation={origin:[8,8,8],axis:"z",angle,rescale:false};
  }
  return result;
 };
 function drum(section,worn){
  const cfg=design.drum,es=[],ext=cfg.facetWidth/2,R=cfg.facetOffset,T=cfg.skinThickness/2;
  function eight(mat,z0,z1,delta){
   const cases=[
    [[8-ext-delta,8+R-T-delta,z0],[8+ext+delta,8+R+T+delta,z1],null],
    [[8-ext-delta,8-R-T-delta,z0],[8+ext+delta,8-R+T+delta,z1],null],
    [[8-R-T-delta,8-ext-delta,z0],[8-R+T+delta,8+ext+delta,z1],null],
    [[8+R-T-delta,8-ext-delta,z0],[8+R+T+delta,8+ext+delta,z1],null]
   ];
   for(const [a,b,angle] of cases)es.push(element(mat,a,b,angle));
   for(const angle of [-45,45]){
    es.push(element(mat,cases[0][0],cases[0][1],angle));
    es.push(element(mat,cases[1][0],cases[1][1],angle));
   }
  }
  eight("mesh",0,16,0);
  eight("bearing",section==="front"?14.6:0.3,section==="front"?15.8:1.3,0.22);
  if(section==="middle")eight("bronze",7.7,8.55,0.10);
  es.push(element("iron",[6.9,6.9,0],[9.1,9.1,16]));
  if(section!=="front"){
   es.push(element("bronze",[7.35,-2,0.7],[8.65,8,1.4]));
   es.push(element("bronze",[7.35,8,0.7],[8.65,18,1.4]));
   es.push(element("bronze",[-2,7.35,0.7],[8,8.65,1.4]));
   es.push(element("bronze",[8,7.35,0.7],[18,8.65,1.4]));
   es.push(element("bearing",[6.3,6.3,0.2],[9.7,9.7,2]));
  }
  if(es.length>48)throw Error("Too many drum elements "+es.length);
  return{parent:"minecraft:block/block",ambientocclusion:false,
   textures:{mesh:"magneticraft2:block/component/screen_mesh_bronze"+(worn?"_worn":""),
    bronze:"magneticraft2:block/material/bronze_cast",bearing:"magneticraft2:block/material/bronze_machined",
    iron:"magneticraft2:block/material/dark_work_iron",particle:"magneticraft2:block/material/bronze_cast"},elements:es};
 }
 for(const face of F){
  const rotated=cells.map(c=>{const [x,z]=offset(c.x,c.z,face);return {...c,x,z}});
  const minX=Math.min(...rotated.map(c=>c.x)),maxX=Math.max(...rotated.map(c=>c.x)),
    minZ=Math.min(...rotated.map(c=>c.z)),maxZ=Math.max(...rotated.map(c=>c.z));
  const lookup=new Map(rotated.map(c=>[[c.x,c.y,c.z].join(","),c.key]));
  const layers={};for(let y=0;y<3;y++){const rows=[];for(let z=minZ;z<=maxZ;z++){const row=[];for(let x=minX;x<=maxX;x++)row.push(lookup.get([x,y,z].join(",")));rows.push(row)}layers["layer"+(y+1)]=rows}
  write(D+`multiblocks/rotary_ore_classifier_${face}.json`,{name:`rotary_ore_classifier_${face}`,
   structure:{dimensions:[maxX-minX+1,3,maxZ-minZ+1],layout:layers,blocks:design.blocks},
   settings:{replacewhenformed:`multiblock/rotary_ore_classifier_${face}`,inventory:true,power:false,fuel:false,fluid:false,inputs:[],outputs:[],
    voxel_profile:"magneticraft2:rotary_ore_classifier",render_air_anchors:design.anchors.map(([x,y,z])=>{const [a,b]=offset(x,z,face);return[a,y,b]})}});
  const boxes=design.boxes.map(b=>boxRot(b,face));
  for(const c of hosts){const [x,z]=offset(c.x,c.z,face);write(A+`models/multiblock/rotary_ore_classifier_${face}/part_${x}_${c.y}_${z}.json`,
    staticModel(boxes,{...c,x,z}));}
 }
 const physics=design.boxes.filter(b=>b.collision!==false).concat(design.collisionExtras).map(b=>b.from.concat(b.to).map(round));
 if(physics.length>512)throw Error("Voxel count");
 write(D+"multiblock_voxels/rotary_ore_classifier.json",{format:1,unit:"model_pixels",collision:physics,selection:physics,
  zones:{screen:[[-4,13,-32,20,35.5,16]]}});
 const block={variants:{}};for(const face of F){
  const y={south:0,west:90,north:180,east:270}[face];
  block.variants[`facing=${face},is_formed=true`]={model:`magneticraft2:multiblock/rotary_ore_classifier_${face}/part_0_0_0`};
  block.variants[`facing=${face},is_formed=false`]={model:"magneticraft2:block/rotary_ore_classifier_controller",y};
 }
 write(A+"blockstates/rotary_ore_classifier.json",block);
 write(A+"models/block/rotary_ore_classifier_controller.json",{parent:"minecraft:block/cube_all",textures:{all:"magneticraft2:block/material/bronze_cast"}});
 write(A+"models/item/rotary_ore_classifier.json",{parent:"magneticraft2:block/rotary_ore_classifier_controller"});
 for(const part of ["rear","middle","front"])for(const worn of [false,true])
  write(A+`models/multiblock/rotary_ore_classifier_drum_${part}${worn?"_worn":""}.json`,drum(part,worn));
 return [...files.entries()];
}
const files=generate(source),wanted=new Set(files.map(([p])=>p)),check=process.argv.includes('--check');
const modelBase='src/main/resources/assets/magneticraft2/models/multiblock/';
for(const face of ['south','west','north','east']){
 const dir=join(root,modelBase+'rotary_ore_classifier_'+face);
 if(!existsSync(dir))continue;
 for(const filename of readdirSync(dir))if(filename.endsWith('.json')){
  const p=modelBase+'rotary_ore_classifier_'+face+'/'+filename;
  if(!wanted.has(p)){
   if(check)throw Error('Obsolete generated classifier frame: '+p);
   rmSync(join(dir,filename));
  }
 }
}
for(const filename of ['rotary_ore_classifier_drum.json','rotary_ore_classifier_drum_worn.json']){
 const path=join(root,modelBase+filename);
 if(existsSync(path)){
  if(check)throw Error('Old narrow drum still present: '+filename);
  rmSync(path);
 }
}
for(const [p,body] of files){const path=join(root,p);
 if(check){if(!existsSync(path)||readFileSync(path,'utf8')!==body)throw Error('Generated file changed: '+p);}
 else{mkdirSync(dirname(path),{recursive:true});writeFileSync(path,body);}
}
console.log((check?'Verified':'Generated')+' '+files.length+' classifier files; '+source.dimensions.join('x')+' frame, four orientations');
