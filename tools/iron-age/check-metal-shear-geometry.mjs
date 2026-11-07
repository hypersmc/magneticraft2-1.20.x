#!/usr/bin/env node
// Offline model/kinematic checks, not Forge compilation or in-game acceptance.
import fs from 'node:fs';
import assert from 'node:assert/strict';
const root='src/main/resources/';
const read=p=>JSON.parse(fs.readFileSync(root+p,'utf8'));
const dir='assets/magneticraft2/models/multiblock/';
const names=['belt','pulley','coupling','shaft','crank','link','blade'];
const models=Object.fromEntries(names.map(n=>[n,read(dir+'iron_stock_shear_'+n+'.json')]));
const voxels=read('data/magneticraft2/multiblock_voxels/iron_stock_shear.json');
assert.deepEqual(voxels.collision,voxels.selection);
assert.equal(voxels.collision.length,60);
const rot=(x,y,ox,oy,degrees)=>{
 const a=degrees*Math.PI/180,c=Math.cos(a),s=Math.sin(a);
 return [(x-ox)*c-(y-oy)*s+ox,(x-ox)*s+(y-oy)*c+oy];
};
function polygon(e,pos,origin,degrees){
 const [x,y,z]=e.from,[a,b,c]=e.to;
 let points=[[x,y],[a,y],[a,b],[x,b]];
 if(e.rotation){
  assert.equal(e.rotation.axis,'z');
  points=points.map(([u,v])=>rot(u,v,...e.rotation.origin.slice(0,2),e.rotation.angle));
 }
 points=points.map(([u,v])=>rot(u-origin[0],v-origin[1],0,0,degrees))
  .map(([u,v])=>[u+pos[0],v+pos[1]]);
 return {points,z0:z-origin[2]+pos[2],z1:c-origin[2]+pos[2]};
}
function intersects(a,b){
 if(Math.min(a.z1,b.z1)-Math.max(a.z0,b.z0)<=1e-5)return false;
 for(const points of [a.points,b.points])for(let i=0;i<points.length;i++){
  const p=points[i],q=points[(i+1)%points.length];
  const axis=[-(q[1]-p[1]),q[0]-p[0]];
  const project=poly=>poly.points.map(v=>v[0]*axis[0]+v[1]*axis[1]);
  const pa=project(a),pb=project(b);
  if(Math.min(Math.max(...pa),Math.max(...pb))-
     Math.max(Math.min(...pa),Math.min(...pb))<=1e-5)return false;
 }
 return true;
}
const fixed=voxels.collision.map(([x,y,z,a,b,c])=>({points:[[x,y],[a,y],[a,b],[x,b]],z0:z,z1:c}));
let relevant=0;
for(let angle=0;angle<=360;angle+=5){
 const rad=angle*Math.PI/180,px=8+3*Math.sin(rad),py=24-3*Math.cos(rad);
 const sy=py-Math.sqrt(100-(px-8)**2),link=Math.atan2(px-8,sy-py)*180/Math.PI;
 assert.ok(Math.abs(Math.hypot(px-8,py-sy)-10)<1e-10);
 const poses=[['belt',[8,8,-15],[8,8,8],0],
  ['pulley',[8,8,-15],[8,8,8],angle],['pulley',[8,24,-15],[8,8,8],angle],
  ['coupling',[8,8,-16],[8,8,0],angle],['shaft',[8,24,-15],[8,8,0],angle],
  ['crank',[8,24,-8.5],[8,8,8],angle],['link',[px,py,-7.5],[8,0,8],link],
  ['blade',[8,sy,-7.5],[8,0,8],0]];
 const belt=models.belt.elements.map(e=>polygon(e,[8,8,-15],[8,8,8],0));
 for(const [name,pos,origin,deg]of poses)for(const e of models[name].elements){
  const poly=polygon(e,pos,origin,deg);
  for(let i=0;i<fixed.length;i++){
   if(Math.min(poly.z1,fixed[i].z1)-Math.max(poly.z0,fixed[i].z0)>1e-5)relevant++;
   assert.ok(!intersects(poly,fixed[i]),`${angle}: ${name} intersects static box ${i}`);
  }
  if(name==='pulley')for(const segment of belt)
   assert.ok(!intersects(poly,segment),`${angle}: pulley intersects belt`);
 }
}
let faces=0;
const validate=(m,bounded)=>{
 for(const e of m.elements){
  const [w,h,d]=e.to.map((n,i)=>n-e.from[i]);
  assert.ok(Math.min(w,h,d)>0);
  if(bounded)assert.ok(e.from.every(n=>n>=0)&&e.to.every(n=>n<=16));
  if(e.rotation)assert.ok([-45,-22.5,0,22.5,45].includes(e.rotation.angle));
  for(const [side,f]of Object.entries(e.faces)){
   const [u,v]=side==='up'||side==='down'?[w,d]:side==='east'||side==='west'?[d,h]:[w,h];
   assert.ok(Math.abs(f.uv[2]-f.uv[0]-u)<1e-5&&Math.abs(f.uv[3]-f.uv[1]-v)<1e-5);
   assert.ok(f.uv.every(n=>n>=0&&n<=16));
   assert.ok(m.textures[f.texture.slice(1)]);faces++;
  }
 }
};
for(const m of Object.values(models))validate(m,false);
for(const [d,x,z]of [['south',0,-1],['north',0,1],['west',1,0],['east',-1,0]])
 for(const y of [0,1])validate(read(dir+`iron_stock_shear_${d}/part_${x}_${y}_${z}.json`),true);
validate(read('assets/magneticraft2/models/item/iron_rivet_blank.json'),false);
const cut=read('data/magneticraft2/recipes/shear_iron_rivets.json');
const head=read('data/magneticraft2/recipes/forge_iron_rivets.json');
assert.equal(cut.output.item,head.input.item);
assert.equal(cut.output.count,head.input_count);
assert.equal(cut.output.count,4);assert.equal(head.output.count,4);
assert.equal(head.output.item,'magneticraft2:iron_rivet');
console.log(`PASS: 73 sampled poses, ${relevant} depth-overlapping moving/static pairs, belt clearance, ${faces} native-UV faces and 1 rod -> 4 blanks -> 4 rivets. NOT a Forge/game test.`);
