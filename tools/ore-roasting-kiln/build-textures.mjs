#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {makeRoasterPngs} from './texture-art.mjs';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),
 '../../src/main/resources/assets/magneticraft2/textures/block/ore_roasting_kiln');
const mode=process.argv[2];
if(!['--write','--check'].includes(mode))throw Error('Expected --write or --check');
let count=0;
for(const [name,data] of Object.entries(makeRoasterPngs())){
 const target=path.join(root,name+'.png'),expected=Buffer.from(data);
 if(mode==='--write'){
  fs.mkdirSync(root,{recursive:true});fs.writeFileSync(target,expected);
 }else if(!fs.existsSync(target)||!fs.readFileSync(target).equals(expected))
  throw Error('Missing/stale roasting kiln texture '+target);
 count++;
}
console.log('Ore Roasting Kiln: '+count+' 16x16 original tiles '+mode);
