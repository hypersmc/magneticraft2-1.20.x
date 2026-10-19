/** Rebuild/check all generated 5x3x5 classifier static V2 parts. Node 18+, no deps. */
import {readFileSync,writeFileSync,existsSync,mkdirSync} from 'node:fs';
import {join,dirname,resolve} from 'node:path';
import {fileURLToPath} from 'node:url';

const dir=dirname(fileURLToPath(import.meta.url));
const repo=resolve(dir,'../..');
const outputRoot=join(repo,'src/main/resources/assets/magneticraft2');
const design=JSON.parse(readFileSync(join(dir,'frame-design.json'),'utf8'));
const check=process.argv.includes('--check');
const facings=['south','west','north','east'];
if(design.schema!==1 || JSON.stringify(design.dimensions)!=='[5,3,5]'){
  throw Error('Unexpected frame schema/dimensions');
}
const anchorSet=new Set(design.anchors.map(p=>p.join(',')));
const rot=(x,z,f)=>f==='south'?[x,z]:f==='west'?[16-z,x]:f==='north'?[16-x,16-z]:[z,16-x];
const rotOffset=(x,z,f)=>f==='south'?[x,z]:f==='west'?[-z,x]:f==='north'?[-x,-z]:[z,-x];
const sideNames=['north','south','east','west','up','down'];
function templateFor(cell){
  const {x,y,z,key}=cell;
  if(key==='C')return 'controller_base';
  if(y===0&&key==='S')return Math.abs(x)===2?'outer_base':x===0?'central_base':'base';
  if(y===1&&key==='P')return Math.abs(x)===2?'mid_side':'mid_rear';
  if(y===1&&key==='B')return 'mid_bronze';
  if(y===2&&key==='B')return 'top_bearing';
  if(y===2&&key==='P')return z===-3||z===1?'top_cross':'top_side';
  if(key==='A'&&anchorSet.has([x,y,z].join(',')))
    return z===1?'chute':x===-1&&z===-2?'feed':'drum_anchor';
  if(key==='A'||'UIOY'.includes(key))return null;
  throw Error('Unknown cell '+JSON.stringify(cell));
}
const cells=[];
for(let y=0;y<3;y++)for(let z=-3;z<=1;z++)for(let x=-2;x<=2;x++){
  const key=design.layers[y][z+3][x+2],template=templateFor({x,y,z,key});
  if(template)cells.push({x,y,z,key,template});
}
if(cells.length!==50)throw Error('Expected 50 render hosts');
const used=[...new Set(cells.map(c=>c.template))];
if(used.length!==12)throw Error('Expected 12 template kinds');
function model(name,face){
  const elements=design.templates[name].map(b=>{
    const pts=[[b.from[0],b.from[2]],[b.from[0],b.to[2]],[b.to[0],b.from[2]],[b.to[0],b.to[2]]].map(([x,z])=>rot(x,z,face));
    const from=[Math.min(...pts.map(p=>p[0])),b.from[1],Math.min(...pts.map(p=>p[1]))];
    const to=[Math.max(...pts.map(p=>p[0])),b.to[1],Math.max(...pts.map(p=>p[1]))];
    if(from.some((v,i)=>v<0||to[i]>16||to[i]<=v))throw Error('Invalid model-local box: '+name);
    const faces={};
    for(const s of sideNames){
      const uv=s==='up'||s==='down'?[from[0],from[2],to[0],to[2]]:
        s==='north'||s==='south'?[from[0],16-to[1],to[0],16-from[1]]:
        [from[2],16-to[1],to[2],16-from[1]];
      if(uv[2]-uv[0]>16||uv[3]-uv[1]>16)throw Error('Stretched UV: '+name);
      faces[s]={texture:'#'+b.material,uv};
    }
    return {from,to,faces};
  });
  return {parent:'minecraft:block/block',ambientocclusion:false,textures:design.textures,elements};
}
let total=0;
function write(path,obj){
  const file=join(outputRoot,path),contents=JSON.stringify(obj,null,2)+'\n';
  if(check){if(!existsSync(file)||readFileSync(file,'utf8')!==contents)throw Error('Out-of-sync generated file: '+file);}
  else{mkdirSync(dirname(file),{recursive:true});writeFileSync(file,contents);}
  total++;
}
for(const face of facings){
  const prefix='models/multiblock/rotary_ore_classifier_'+face+'/';
  for(const name of used)write(prefix+'static_'+name+'.json',model(name,face));
  for(const c of cells){
    const [x,z]=rotOffset(c.x,c.z,face);
    write(prefix+'part_'+x+'_'+c.y+'_'+z+'.json',
      {parent:'magneticraft2:multiblock/rotary_ore_classifier_'+face+'/static_'+c.template});
  }
}
const states={variants:{}};
for(const face of facings){
  const y={south:0,west:90,north:180,east:270}[face];
  states.variants['facing='+face+',is_formed=true']={model:'magneticraft2:multiblock/rotary_ore_classifier_'+face+'/part_0_0_0'};
  states.variants['facing='+face+',is_formed=false']={model:'magneticraft2:block/rotary_ore_classifier_controller',y};
}
write('blockstates/rotary_ore_classifier.json',states);
write('models/block/rotary_ore_classifier_controller.json',{parent:'minecraft:block/cube_all',textures:{all:'magneticraft2:block/material/bronze_cast'}});
write('models/item/rotary_ore_classifier.json',{parent:'magneticraft2:block/rotary_ore_classifier_controller'});
if(total!==251)throw Error('Expected 251 generated files; got '+total);
console.log((check?'Verified':'Generated')+' '+total+' Rotary Ore Classifier V2 frame files');
