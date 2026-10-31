#!/usr/bin/env node
/**
 * Refractory Kiln K1: single canonical SOUTH blueprint -> four rotated
 * datapack structures, Voxel V2 profile, and block-local visible-only
 * Minecraft models. Avoid editing generated files directly.
 * Usage: node tools/refractory-kiln/generate.mjs --check|--write
 */
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const W=5,H=4,D=5;
const dirs=['south','west','north','east'];
const materials={
 S:'minecraft:stone_bricks',B:'minecraft:bricks',
 C:'magneticraft2:refractory_kiln',
 U:'magneticraft2:multiblock_item_input',
 O:'magneticraft2:multiblock_item_primary_output',
 A:'minecraft:air'
};
const exposedDirs=['north','south','east','west','up','down'];
const offset={north:[0,0,-1],south:[0,0,1],east:[1,0,0],west:[-1,0,0],up:[0,1,0],down:[0,-1,0]};
const facing={
 south:{north:'north',south:'south',east:'east',west:'west'},
 west:{north:'east',south:'west',east:'north',west:'south'},
 north:{north:'south',south:'north',east:'west',west:'east'},
 east:{north:'west',south:'east',east:'south',west:'north'}
};
const solid=new Set(['B','S','C']);
const out=(x,y,z)=>{
 if(y===0)return x===0&&z===0?'C':'S';
 if(y===1){
  if(z===-4)return 'B';
  if(z===0)return x===-1?'U':x===1?'O':Math.abs(x)===2?'B':'A';
  return Math.abs(x)===2?'B':'A';
 }
 if(y===2)return z===-4||Math.abs(x)===2?'B':'A';
 if(y===3)return x===0&&z===-2?'A':'B';
 throw new Error('Out-of-bounds kiln Y: '+y);
};
const rot=(x,z,d)=>d==='south'?[x,z]:d==='west'?[-z,x]:d==='north'?[-x,-z]:[z,-x];
const write=p=>'src/main/resources/'+p;
const text=obj=>JSON.stringify(obj,null,2)+'\n';
const cellName=(x,y,z)=>x+','+y+','+z;
export function makeAssets(){
 const assets=new Map();
 const canonical=[];
 for(let y=0;y<H;y++)for(let z=-4;z<=0;z++)for(let x=-2;x<=2;x++)
  canonical.push({x,y,z,k:out(x,y,z)});
 const lookup=new Map(canonical.map(c=>[cellName(c.x,c.y,c.z),c]));
 const vox=canonical.filter(c=>solid.has(c.k)).map(c=>
  [c.x*16,c.y*16,c.z*16,(c.x+1)*16,(c.y+1)*16,(c.z+1)*16]);
 assets.set(write('data/magneticraft2/multiblock_voxels/refractory_kiln.json'),
  {format:1,unit:'model_pixels',collision:vox,selection:vox,zones:{}});
 for(const d of dirs){
  const rotated=canonical.map(c=>{
   const [rx,rz]=rot(c.x,c.z,d);
   return {...c,rx,rz};
  });
  const byPos=new Map(rotated.map(c=>[cellName(c.rx,c.y,c.rz),c]));
  const loX=Math.min(...rotated.map(c=>c.rx));
  const loZ=Math.min(...rotated.map(c=>c.rz));
  const layout={};
  for(let y=0;y<H;y++){
   const rows=[];
   for(let z=loZ;z<loZ+D;z++){
    const row=[];
    for(let x=loX;x<loX+W;x++){
     const c=byPos.get(cellName(x,y,z));
     if(!c)throw new Error('Rotation contains a hole '+d+' '+x+'/'+y+'/'+z);
     row.push(c.k);
    }
    rows.push(row);
   }
   layout['layer'+(y+1)]=rows;
  }
  assets.set(write('data/magneticraft2/multiblocks/refractory_kiln_'+d+'.json'),{
   name:'refractory_kiln_'+d,
   structure:{dimensions:[W,H,D],layout,blocks:materials},
   settings:{
    replacewhenformed:'multiblock/refractory_kiln_'+d,
    inventory:true,power:false,fuel:false,fluid:false,
    inputs:[],outputs:[],render_air_anchors:[],
    voxel_profile:'magneticraft2:refractory_kiln'
   }
  });
  for(const c of rotated.filter(c=>solid.has(c.k))){
   const faces={};
   for(const f of exposedDirs){
    const [dx,dy,dz]=offset[f];
    const n=lookup.get(cellName(c.x+dx,c.y+dy,c.z+dz));
    if(n&&(solid.has(n.k)||n.k==='U'||n.k==='O'))continue;
    const world=f==='up'||f==='down'?f:facing[d][f];
    const material=c.k==='S'?'stone':c.k==='B'?'brick':
      world===facing[d].south?'front':'stone';
    faces[world]={texture:'#'+material,uv:[0,0,16,16]};
   }
   assets.set(write('assets/magneticraft2/models/multiblock/refractory_kiln_'+d+
     '/part_'+c.rx+'_'+c.y+'_'+c.rz+'.json'),{
    parent:'minecraft:block/block',
    textures:{
     stone:'minecraft:block/stone_bricks',
     brick:'minecraft:block/bricks',
     front:'minecraft:block/furnace_front',
     particle:c.k==='B'?'minecraft:block/bricks':'minecraft:block/stone_bricks'
    },
    elements:[{from:[0,0,0],to:[16,16,16],faces}]
   });
  }
 }
 const variants={},angle={south:0,west:90,north:180,east:270};
 for(const d of dirs){
  const unfinished={model:'magneticraft2:block/refractory_kiln_controller'};
  if(angle[d])unfinished.y=angle[d];
  variants['facing='+d+',is_formed=false']=unfinished;
  variants['facing='+d+',is_formed=true']={
   model:'magneticraft2:multiblock/refractory_kiln_'+d+'/part_0_0_0'
  };
 }
 assets.set(write('assets/magneticraft2/blockstates/refractory_kiln.json'),{variants});
 return assets;
}
export function run(mode){
 const assets=makeAssets();
 if(!['--write','--check'].includes(mode))
  throw new Error('Expected --write or --check');
 let checked=0;
 for(const [relative,expected] of assets){
  const target=path.join(root,relative);
  if(mode==='--write'){
   fs.mkdirSync(path.dirname(target),{recursive:true});
   fs.writeFileSync(target,text(expected));
  }else{
   if(!fs.existsSync(target))throw new Error('Missing generated file: '+relative);
   const current=JSON.parse(fs.readFileSync(target,'utf8'));
   if(text(current)!==text(expected))
    throw new Error('Stale generated model or blueprint: '+relative);
  }
  checked++;
 }
 // Don't quietly leave obsolete generated part models from prior iterations.
 if(mode==='--check'){
  for(const d of dirs){
   const folder=path.join(root,write('assets/magneticraft2/models/multiblock/refractory_kiln_'+d));
   const declared=new Set([...assets.keys()].filter(p=>p.startsWith(write('assets/magneticraft2/models/multiblock/refractory_kiln_'+d+'/'))).map(p=>path.basename(p)));
   for(const name of fs.readdirSync(folder))if(name.endsWith('.json')&&!declared.has(name))
    throw new Error('Obsolete generated model: '+d+'/'+name);
  }
 }
 return checked;
}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)){
 const count=run(process.argv[2]);
 console.log('Refractory Kiln K1 assets: '+count+' '+process.argv[2]);
}
