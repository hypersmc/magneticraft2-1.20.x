#!/usr/bin/env node
/** Build or verify original K1 PNG assets. Node 18+, no dependencies. */
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {makeKilnPngs,makeKilnItemPngs} from './texture-art.mjs';
const resources=path.resolve(path.dirname(fileURLToPath(import.meta.url)),
 '../../src/main/resources/assets/magneticraft2/textures');
const targets=[
 [path.join(resources,'block/refractory_kiln'),makeKilnPngs()],
 [path.join(resources,'item'),makeKilnItemPngs()]
];
const mode=process.argv[2];
if(!['--write','--check'].includes(mode))throw Error('Expected --write or --check');
let count=0;
for(const [root,images] of targets)for(const [name,bytes] of Object.entries(images)){
 const p=path.join(root,name+'.png'),expected=Buffer.from(bytes);
 if(mode==='--check'){
  if(!fs.existsSync(p)||!fs.readFileSync(p).equals(expected))throw Error('Stale/missing K1 texture '+p);
 }else{
  fs.mkdirSync(path.dirname(p),{recursive:true});
  fs.writeFileSync(p,expected);
 }
 count++;
}
console.log('Refractory Kiln: '+count+' original 16x16 block and item textures '+mode);
