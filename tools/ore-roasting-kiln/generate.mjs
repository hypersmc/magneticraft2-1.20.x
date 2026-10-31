#!/usr/bin/env node
/**
 * One canonical 5x6x9 SOUTH blueprint: rotate four ways, bake block-local
 * models, Voxel V2 roof and chimney, and generate Patchouli-compatible layout.
 */
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {roastVisual,roastPort,roofStep} from './visuals.mjs';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const W=5,H=6,D=9,dirs=['south','west','north','east'];
const materials={S:'minecraft:stone_bricks',B:'minecraft:bricks',
 C:'magneticraft2:ore_roasting_kiln',U:'magneticraft2:multiblock_item_input',
 O:'magneticraft2:multiblock_item_primary_output',A:'minecraft:air'};
const owned=new Set(['S','B','C','U','O']);
const chimneyFoot=(x,z)=>Math.abs(x)<=1&&z>=-8&&z<=-6&&!(x===0&&z===-7);
const key=(x,y,z)=>x+','+y+','+z;
const rotate=(x,z,d)=>d==='south'?[x,z]:d==='west'?[-z,x]:
 d==='north'?[-x,-z]:[z,-x];
const res=p=>'src/main/resources/'+p;
const fmt=o=>JSON.stringify(o,null,2)+'\n';
export function typeAt(x,y,z){
 if(x < -2||x > 2||z < -8||z > 0||y < 0||y >= H)
  throw Error('Out of bounds '+key(x,y,z));
 if(y===0){
  if(z===0&&x===0)return 'C';
  if(z===0&&x===-2)return 'U';
  if(z===0&&x===2)return 'O';
  return 'S';
 }
 if(y>=1&&y<=3)
  return Math.abs(x)===2||z===0||z===-8?'B':'A';
 if(y===4)return x===0&&z===-7?'A':'B';
 if(y===5)return Math.abs(x)<=1&&z>=-8&&z<=-6&&
  !(x===0&&z===-7)?'B':'A';
 throw Error('Invalid layer '+y);
}
export function makeAssets(){
 const assets=new Map(),canon=[];
 for(let y=0;y<H;y++)for(let z=-8;z<=0;z++)for(let x=-2;x<=2;x++)
  canon.push({x,y,z,k:typeAt(x,y,z)});
 const lookup=new Map(canon.map(c=>[key(c.x,c.y,c.z),c]));
 const vox=[];
 for(const c of canon){
  if(!owned.has(c.k)||c.y===4)continue;
  vox.push([c.x*16,c.y*16,c.z*16,(c.x+1)*16,(c.y+1)*16,(c.z+1)*16]);
 }
 // The roof is one continuous slope along its length, interrupted ONLY
 // by the single real chimney AIR shaft. Merge adjacent Z roof cells into
 // long run boxes: ~24 rectangles instead of 176 small roof boxes.
 for(let x=-2;x<=2;x++){
  let start=null;
  for(let z=-8;z<=1;z++){
   const present=z<=0&&typeAt(x,4,z)!=='A'&&!chimneyFoot(x,z);
   if(present&&start===null)start=z;
   if(!present&&start!==null){
    for(let strip=0;strip<4;strip++){
     const px=strip*4,h=roofStep((x+2)*16+px+2);
     vox.push([x*16+px,64,start*16,x*16+px+4,64+h,z*16]);
    }
    start=null;
   }
  }
 }
 // Eight loadbearing masonry feet fill the gap between the sloping roof
 // and the brick ring above; no missing or duplicate collision volume.
 for(let x=-1;x<=1;x++)for(let z=-8;z<=-6;z++){
  if(chimneyFoot(x,z))vox.push([x*16,64,z*16,(x+1)*16,80,(z+1)*16]);
 }
 if(vox.length>512)throw Error('Too many voxel boxes: '+vox.length);
 assets.set(res('data/magneticraft2/multiblock_voxels/ore_roasting_kiln.json'),
  {format:1,unit:'model_pixels',collision:vox,selection:vox,zones:{}});
 for(const d of dirs){
  const rotated=canon.map(c=>{const [rx,rz]=rotate(c.x,c.z,d);return {...c,rx,rz};});
  const byPos=new Map(rotated.map(c=>[key(c.rx,c.y,c.rz),c]));
  const left=Math.min(...rotated.map(c=>c.rx)),right=Math.max(...rotated.map(c=>c.rx));
  const back=Math.min(...rotated.map(c=>c.rz)),front=Math.max(...rotated.map(c=>c.rz));
  const layout={};
  for(let y=0;y<H;y++){
   const rows=[];
   for(let z=back;z<=front;z++){
    const row=[];
    for(let x=left;x<=right;x++){
     const cell=byPos.get(key(x,y,z));
     if(!cell)throw Error('Missing rotated cell '+d+':'+key(x,y,z));
     row.push(cell.k);
    }
    rows.push(row);
   }
   layout['layer'+(y+1)]=rows;
  }
  assets.set(res('data/magneticraft2/multiblocks/ore_roasting_kiln_'+d+'.json'),{
   name:'ore_roasting_kiln_'+d,
   structure:{dimensions:[right-left+1,H,front-back+1],layout,blocks:materials},
   settings:{replacewhenformed:'multiblock/ore_roasting_kiln_'+d,
    inventory:true,power:false,fuel:false,fluid:false,inputs:[],outputs:[],
    render_air_anchors:[],voxel_profile:'magneticraft2:ore_roasting_kiln'}
  });
  for(const c of rotated.filter(c=>['S','B','C'].includes(c.k)))
   assets.set(res('assets/magneticraft2/models/multiblock/ore_roasting_kiln_'+d+
     '/part_'+c.rx+'_'+c.y+'_'+c.rz+'.json'),roastVisual(c,d,lookup));
  for(const role of ['input','primary'])
   assets.set(res('assets/magneticraft2/models/multiblock/ore_roasting_kiln_'+d+
     '/port_'+role+'.json'),roastPort(role,d));
 }
 const controller=canon.find(c=>c.k==='C');
 assets.set(res('assets/magneticraft2/models/block/ore_roasting_kiln_controller.json'),
  roastVisual({...controller,rx:0,rz:0},'south',new Map()));
 const variants={},angles={south:0,west:90,north:180,east:270};
 for(const d of dirs){
  const raw={model:'magneticraft2:block/ore_roasting_kiln_controller'};
  if(angles[d])raw.y=angles[d];
  variants['facing='+d+',is_formed=false']=raw;
  variants['facing='+d+',is_formed=true']={
   model:'magneticraft2:multiblock/ore_roasting_kiln_'+d+'/part_0_0_0'};
 }
 assets.set(res('assets/magneticraft2/blockstates/ore_roasting_kiln.json'),{variants});
 assets.set(res('assets/magneticraft2/models/item/ore_roasting_kiln.json'),{
  parent:'magneticraft2:block/ore_roasting_kiln_controller'});
 return assets;
}
export function run(mode){
 if(!['--write','--check'].includes(mode))throw Error('Expected --write or --check');
 const assets=makeAssets();
 for(const [rel,model] of assets){
  const file=path.join(root,rel);
  if(mode==='--write'){
   fs.mkdirSync(path.dirname(file),{recursive:true});
   fs.writeFileSync(file,fmt(model));
  }else if(!fs.existsSync(file)||fs.readFileSync(file,'utf8')!==fmt(model))
   throw Error('Missing/stale ore-roasting asset '+rel);
 }
 if(mode==='--check'){
  for(const d of dirs){
   const prefix=res('assets/magneticraft2/models/multiblock/ore_roasting_kiln_'+d+'/');
   const folder=path.join(root,prefix);
   const known=new Set([...assets.keys()].filter(p=>p.startsWith(prefix)).map(p=>path.basename(p)));
   for(const file of fs.readdirSync(folder))if(file.endsWith('.json')&&!known.has(file))
    throw Error('Obsolete model '+d+'/'+file);
  }
 }
 return assets.size;
}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url))
 console.log('Ore Roasting Kiln: '+run(process.argv[2])+' generated assets');
