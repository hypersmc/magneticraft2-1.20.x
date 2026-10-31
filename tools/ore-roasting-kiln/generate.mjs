#!/usr/bin/env node
/** 5x3x9 structural layout, four facings and matching local voxel collision. */
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {roastVisual,roastPort,roofTop} from './visuals.mjs';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');

const W=5,H=3,D=9,dirs=['south','west','north','east'];
const materials={S:'minecraft:stone_bricks',B:'minecraft:bricks',
 C:'magneticraft2:ore_roasting_kiln',U:'magneticraft2:multiblock_item_input',
 O:'magneticraft2:multiblock_item_primary_output',A:'minecraft:air'};
const occupied=new Set(['S','B','C','U','O']);
const key=(x,y,z)=>x+','+y+','+z;
const rot=(x,z,d)=>d==='south'?[x,z]:d==='west'?[-z,x]:
 d==='north'?[-x,-z]:[z,-x];
const type=(x,y,z)=>{
 if(y===0)return z===0&&x===0?'C':z===0&&x===-2?'U':z===0&&x===2?'O':'S';
 if(y===1)return Math.abs(x)===2||z===0||z===-8?'B':'A';
 if(y===2)return x===0&&z===-7?'A':'B';
 throw Error('Invalid local y='+y);
};
const write=p=>'src/main/resources/'+p;
const fmt=m=>JSON.stringify(m,null,2)+'\n';

export 
function makeAssets(){
 const assets=new Map(),canon=[];
 for(let y=0;y<H;y++)for(let z=-8;z<=0;z++)for(let x=-2;x<=2;x++)
  canon.push({x,y,z,k:type(x,y,z)});
 const lookup=new Map(canon.map(c=>[key(c.x,c.y,c.z),c]));
 const vox=canon.filter(c=>occupied.has(c.k)).map(c=>[
  c.x*16,c.y*16,c.z*16,(c.x+1)*16,
  c.y*16+(c.y===2?roofTop(c.x,c.z):16),(c.z+1)*16
 ]);
 assets.set(write('data/magneticraft2/multiblock_voxels/ore_roasting_kiln.json'),
  {format:1,unit:'model_pixels',collision:vox,selection:vox,zones:{}});
 for(const d of dirs){
  const cells=canon.map(c=>{const [rx,rz]=rot(c.x,c.z,d);return {...c,rx,rz};});
  const locations=new Map(cells.map(c=>[key(c.rx,c.y,c.rz),c]));
  const left=Math.min(...cells.map(c=>c.rx)),right=Math.max(...cells.map(c=>c.rx));
  const back=Math.min(...cells.map(c=>c.rz)),front=Math.max(...cells.map(c=>c.rz));
  const layout={};
  for(let y=0;y<H;y++){
   const rows=[];
   for(let z=back;z<=front;z++){
    const row=[];
    for(let x=left;x<=right;x++){
     const c=locations.get(key(x,y,z));
     if(!c)throw Error('Rotation hole '+d+'/'+x+'/'+y+'/'+z);
     row.push(c.k);
    }
    rows.push(row);
   }
   layout['layer'+(y+1)]=rows;
  }
  assets.set(write('data/magneticraft2/multiblocks/ore_roasting_kiln_'+d+'.json'),{
   name:'ore_roasting_kiln_'+d,
   structure:{dimensions:[right-left+1,H,front-back+1],layout,blocks:materials},
   settings:{replacewhenformed:'multiblock/ore_roasting_kiln_'+d,
    inventory:true,power:false,fuel:false,fluid:false,inputs:[],outputs:[],
    render_air_anchors:[],voxel_profile:'magneticraft2:ore_roasting_kiln'}
  });
  for(const c of cells.filter(c=>['S','B','C'].includes(c.k)))
   assets.set(write('assets/magneticraft2/models/multiblock/ore_roasting_kiln_'+d+
     '/part_'+c.rx+'_'+c.y+'_'+c.rz+'.json'),roastVisual(c,d,lookup));
  for(const role of ['input','primary'])
   assets.set(write('assets/magneticraft2/models/multiblock/ore_roasting_kiln_'+d+
      '/port_'+role+'.json'),roastPort(role,d));
 }
 const controller=canon.find(c=>c.k==='C');
 assets.set(write('assets/magneticraft2/models/block/ore_roasting_kiln_controller.json'),
  roastVisual({...controller,rx:0,rz:0},'south',new Map()));
 const variants={},angles={south:0,west:90,north:180,east:270};
 for(const d of dirs){
  const unformed={model:'magneticraft2:block/ore_roasting_kiln_controller'};
  if(angles[d])unformed.y=angles[d];
  variants['facing='+d+',is_formed=false']=unformed;
  variants['facing='+d+',is_formed=true']={
   model:'magneticraft2:multiblock/ore_roasting_kiln_'+d+'/part_0_0_0'
  };
 }
 assets.set(write('assets/magneticraft2/blockstates/ore_roasting_kiln.json'),{variants});
 assets.set(write('assets/magneticraft2/models/item/ore_roasting_kiln.json'),{
  parent:'magneticraft2:block/ore_roasting_kiln_controller'});
 return assets;
}


export function run(mode){
 if(!['--write','--check'].includes(mode))throw Error('Expected --write or --check');
 const assets=makeAssets();
 for(const [relative,model] of assets){
  const destination=path.join(root,relative);
  if(mode==='--write'){
   fs.mkdirSync(path.dirname(destination),{recursive:true});
   fs.writeFileSync(destination,fmt(model));
  }else if(!fs.existsSync(destination)||fs.readFileSync(destination,'utf8')!==fmt(model))
   throw Error('Missing/stale Ore Roaster generated asset '+relative);
 }
 if(mode==='--check'){
  for(const d of dirs){
   const prefix=write('assets/magneticraft2/models/multiblock/ore_roasting_kiln_'+d+'/');
   const dir=path.join(root,prefix);
   const expected=new Set([...assets.keys()].filter(p=>p.startsWith(prefix)).map(p=>path.basename(p)));
   for(const f of fs.readdirSync(dir))if(f.endsWith('.json')&&!expected.has(f))
    throw Error('Obsolete Ore Roaster model '+f);
  }
 }
 return assets.size;
}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url))
 console.log('Ore Roasting Kiln: '+run(process.argv[2])+' generated assets');
