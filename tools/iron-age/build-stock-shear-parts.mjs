#!/usr/bin/env node
// Magneticraft2 Iron Stock Shear: 18 editable SOUTH models -> 54 rotated
// directional parts. The existing V2 model/voxel runtime is unchanged.
// Usage: node tools/iron-age/build-stock-shear-parts.mjs --check
//        node tools/iron-age/build-stock-shear-parts.mjs --write
import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';

const root=process.cwd();
const base='src/main/resources/assets/magneticraft2/models/multiblock';
const id='iron_stock_shear';
const inputDir=path.join(root,base,id+'_south');
const args=new Set(process.argv.slice(2));
assert.ok(args.has('--check')!==args.has('--write'),
 'Exactly one of --check or --write is required');
const dirs=['south','west','north','east'];
const faces=['north','south','east','west','up','down'];
const xy=(x,z,d)=>d==='south'?[x,z]:d==='north'?[-x,-z]:
 d==='east'?[z,-x]:[-z,x];
const rotate=(from,to,d)=>{
 const [x,y,z]=from,[a,b,c]=to;
 if(d==='south')return [from,to];
 if(d==='north')return [[16-a,y,16-c],[16-x,b,16-z]];
 if(d==='east')return [[z,y,16-a],[c,b,16-x]];
 return [[16-c,y,x],[16-z,b,a]];
};
const generated=new Map();
const originals=fs.readdirSync(inputDir)
 .filter(n=>/^part_-?\d+_-?\d+_-?\d+\.json$/.test(n)).sort();
assert.equal(originals.length,18,'3x3x2 shear needs 18 South part models');
for(const name of originals){
 const match=name.match(/^part_(-?\d+)_(-?\d+)_(-?\d+)\.json$/);
 const [dx,dy,dz]=match.slice(1).map(Number);
 const canonical=JSON.parse(fs.readFileSync(path.join(inputDir,name),'utf8'));
 assert.equal(canonical.parent,'minecraft:block/block');
 for(const dir of dirs){
  const [ox,oz]=xy(dx,dz,dir);
  const elements=canonical.elements.map(element=>{
   const [from,to]=rotate(element.from,element.to,dir);
   const [w,h,t]=to.map((v,i)=>v-from[i]);
   assert.ok([w,h,t].every(n=>n>0),'Degenerate cuboid '+name);
   const materials=new Set(Object.values(element.faces).map(x=>x.texture));
   assert.equal(materials.size,1,'Mixed-material cuboid '+name);
   const material=[...materials][0];
   assert.ok(canonical.textures[material.slice(1)],'Missing material '+name);
   const result={from,to,faces:{}};
   for(const f of faces){
    const [u,v]=f==='up'||f==='down'?[w,t]:
     f==='east'||f==='west'?[t,h]:[w,h];
    result.faces[f]={texture:material,uv:[0,0,u,v]};
   }
   return result;
  });
  const model={
   parent:canonical.parent,
   ambientocclusion:canonical.ambientocclusion,
   textures:canonical.textures,
   elements
  };
  const file=path.join(root,base,id+'_'+dir,
   'part_'+ox+'_'+dy+'_'+oz+'.json');
  assert.ok(!generated.has(file),'Duplicate transformed part '+file);
  generated.set(file,JSON.stringify(model,null,2)+'\n');
 }
}
assert.equal(generated.size,72,'All four rotations must have 18 parts');
let written=0;
for(const [file,expected]of generated){
 if(args.has('--check')){
  assert.ok(fs.existsSync(file),'Missing generated file '+file);
  // JSON formatting is not geometry. Compare every parsed field exactly,
  // retaining material, UV, dimensions and orientation checks for compact files.
  assert.deepEqual(JSON.parse(fs.readFileSync(file,'utf8')),JSON.parse(expected),
   'Model part stale: '+path.relative(root,file));
 } else {
  fs.mkdirSync(path.dirname(file),{recursive:true});
  fs.writeFileSync(file,expected);
  written++;
 }
}
console.log('Iron Stock Shear '+(args.has('--check')?'PASS: 72 exact model definitions':
 'wrote '+written+' directional model parts')+
 ' (not a Gradle build or in-game visual test)');
