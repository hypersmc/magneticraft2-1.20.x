/** Magneticraft2 1.20.1 Rotary Ore Classifier — source regression contract (Node 18+). */
import assert from 'node:assert/strict';
import {readFileSync,existsSync} from 'node:fs';
import {resolve,join,dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'../..');
const src=p=>readFileSync(join(root,p),'utf8');
const json=p=>JSON.parse(src(p));
const has=p=>assert.ok(existsSync(join(root,p)),'Missing: '+p);
const base='src/main/resources/';
const data=base+'data/magneticraft2/';
const assets=base+'assets/magneticraft2/';
const atlas=assets+'textures/block/';
const facings=['south','west','north','east'];
const canonical=[['SSSSS','SASAS','SASAS','SACAS','SOYSS'],['PUIBP','PAAAP','PAAAP','PAAAP','PAAAP'],['PBPBP','PAAAP','PAAAP','PAAAP','PBPBP']];
const offsets=[[0,0,0],[0,1,-3],[-1,1,-3],[-1,0,1],[0,0,1]];
const kinds=['C','I','U','O','Y'];
const rot=(x,z,face)=>face==='south'?[x,z]:face==='west'?[-z,x]:face==='north'?[-x,-z]:[z,-x];
const materialNames=['machine_timber','machine_timber_end','bronze_cast','bronze_machined','dark_work_iron','worked_copper','refractory_brick'];
for(const name of materialNames){
  const p=atlas+'material/'+name+'.png';has(p);
  const buffer=readFileSync(join(root,p));
  assert.equal(buffer.toString('hex',0,8),'89504e470d0a1a0a','PNG signature '+p);
  assert.equal(buffer.readUInt32BE(16),16,'Width '+p);
  assert.equal(buffer.readUInt32BE(20),16,'Height '+p);
}
for(const name of ['screen_mesh_bronze','screen_mesh_bronze_worn','chute_liner']){
  const p=atlas+'component/'+name+'.png';has(p);
  const b=readFileSync(join(root,p));
  assert.equal(b.readUInt32BE(16),16);assert.equal(b.readUInt32BE(20),16);
}
const kit=json('tools/texture-art/texture-kit-v1.json');
for(const name of ['screen_mesh_bronze','screen_mesh_bronze_worn']){
  assert.ok(kit.textures[name].pixels.join('').includes('.'),'Actual transparent pixels required for '+name);
}
const blueprintIds=['C','I','U','O','Y'];
const names={C:'magneticraft2:rotary_ore_classifier',I:'magneticraft2:mechanical_input_module',
 U:'magneticraft2:multiblock_item_input',O:'magneticraft2:multiblock_item_primary_output',
 Y:'magneticraft2:multiblock_item_byproduct_output'};
const originalCells=[];
for(let y=0;y<3;y++)for(let z=0;z<5;z++)for(let x=0;x<5;x++)
 originalCells.push({x:x-2,y,z:z-3,key:canonical[y][z][x]});
const anchorCells=[[-1,1,-2],[0,1,-2],[0,1,-1],[0,1,0],[-1,1,1],[0,1,1]];
for(const face of facings){
  const filename=data+'multiblocks/rotary_ore_classifier_'+face+'.json';
  const b=json(filename);
  assert.deepEqual(b.structure.dimensions,[5,3,5],filename);
  assert.equal(b.name,'rotary_ore_classifier_'+face);
  assert.equal(b.settings.voxel_profile,'magneticraft2:rotary_ore_classifier');
  for(const id of blueprintIds)assert.equal(b.structure.blocks[id],names[id]);
  for(const id of blueprintIds){
    const count=b.structure.layout.layer1.flat().join('')+
       b.structure.layout.layer2.flat().join('')+b.structure.layout.layer3.flat().join('');
    assert.equal([...count].filter(c=>c===id).length,1,'Exactly one '+id+' in '+face);
  }
  const cells=originalCells.map(c=>{const[x,z]=rot(c.x,c.z,face);return{...c,x,z};});
  const minX=Math.min(...cells.map(c=>c.x)),minZ=Math.min(...cells.map(c=>c.z));
  for(const c of cells){
    assert.equal(b.structure.layout['layer'+(c.y+1)][c.z-minZ][c.x-minX],c.key,'Mismatch '+face);
  }
  const want=anchorCells.map(([x,y,z])=>{const[v,w]=rot(x,z,face);return [v,y,w].join(',')}).sort();
  const actual=b.settings.render_air_anchors.map(p=>p.join(',')).sort();
  assert.deepEqual(actual,want,'Anchors '+face);
  for(const a of actual){
    const [x,y,z]=a.split(',').map(Number);
    const c=cells.find(c=>c.x===x&&c.y===y&&c.z===z);
    assert.equal(c?.key,'A','V2 render anchor must be required AIR');
  }
  // One fixed part per non-module, non-air-anchor cell and each anchored AIR cell.
  const anchors=new Set(actual);let parts=0;
  for(const c of cells){
    if(kinds.includes(c.key)&&c.key!=='C'&&c.key!=='S')continue;
    if(c.key==='A'&&!anchors.has([c.x,c.y,c.z].join(',')))continue;
    const p=assets+'models/multiblock/rotary_ore_classifier_'+face+'/part_'+c.x+'_'+c.y+'_'+c.z+'.json';
    has(p);const model=json(p);assert.ok(model.parent.startsWith('magneticraft2:multiblock/rotary_ore_classifier_'+face+'/static_'),p);parts++;
  }
  assert.equal(parts,50,'Exactly 50 V2 render hosts, '+face);
  for(const name of ['outer_base','base','central_base','controller_base','mid_side','mid_bronze','feed','drum_anchor','chute','top_cross','top_bearing','top_side']){
    const p=assets+'models/multiblock/rotary_ore_classifier_'+face+'/static_'+name+'.json';
    const model=json(p);assert.equal(model.textures.bronze,'magneticraft2:block/material/bronze_cast');
    for(const e of model.elements){
      for(let i=0;i<3;i++)assert.ok(e.from[i]>=0&&e.to[i]<=16&&e.to[i]>e.from[i],'out-of-bounds '+p);
      for(const v of Object.values(e.faces)){
        const [u1,v1,u2,v2]=v.uv;
        assert.ok(u2-u1<=16&&v2-v1<=16,'UV stretched '+p);
      }
    }
  }
}
const vox=json(data+'multiblock_voxels/rotary_ore_classifier.json');
assert.equal(vox.unit,'model_pixels');assert.equal(vox.format,1);
assert.ok(vox.collision.length>0&&vox.collision.length<=512);
const blockstates=json(assets+'blockstates/rotary_ore_classifier.json');
assert.equal(Object.keys(blockstates.variants).length,8);
for(const f of facings)assert.ok(blockstates.variants['facing='+f+',is_formed=true']);
for(const f of ['','_worn']){
  const m=json(assets+'models/multiblock/rotary_ore_classifier_drum'+f+'.json');
  assert.ok(m.elements.length>8&&m.elements.length<64);
  assert.equal(m.textures.screen,'magneticraft2:block/component/screen_mesh_bronze'+f);
}
const java='src/main/java/com/magneticraft2/';
const controller=src(java+'common/blockentity/stage/bronze/RotaryOreClassifierBlockEntity.java');
for(const v of ['setMechanicalLoad','removeMechanicalLoad','getMechanicalInput','getDrumRotationDegrees',
 'classifierScreen.completeBatch()','pendingByproduct','getByproductStack','onDestroy','MultiblockRenderParts.assign']){
  assert.ok(controller.includes(v),'Controller missing: '+v);
}
const client=src(java+'client/Clientsetup.java');
for(const v of ['ROTARY_ORE_CLASSIFIER_BE','rotary_ore_classifier_drum','rotary_ore_classifier_drum_worn'])
 assert.ok(client.includes(v),'Missing client registration '+v);
assert.ok(src(java+'common/blockentity/stage/copper/MultiblockItemPortBlockEntity.java').includes('RotaryOreClassifierBlockEntity'),'Port must validate inputs');
assert.ok(src(java+'compatibility/JEI/MagneticraftJeiPlugin.java').includes('ROTARY_ORE_CLASSIFIER'),'JEI catalyst absent');
for(const ore of ['chalcocite','cassiterite','quartz']){
  const r=json(data+'recipes/rotary_classifying_'+ore+'.json');
  assert.equal(r.type,'magneticraft2:multiblock_processing');
  assert.equal(r.machine,'magneticraft2:rotary_ore_classifier');
  assert.equal(r.input.item,'magneticraft2:crushed_'+ore);
  assert.equal(r.input_count,4);
  assert.equal(r.output.item,'magneticraft2:classified_'+ore);
  assert.equal(r.output.count,3);
  assert.equal(r.byproduct.item,r.input.item);
  assert.equal(r.byproduct.count,1);
  assert.equal(r.byproduct_chance,1);
  assert.equal(r.input_count,r.output.count+r.byproduct.count);
  assert.equal(r.mechanical.min_speed,24);assert.equal(r.mechanical.torque,4);
  const w=json(data+'recipes/mechanical_washing_classified_'+ore+'.json');
  assert.equal(w.machine,'magneticraft2:mechanical_ore_washer');
  assert.equal(w.input_count,3);assert.equal(w.output.count,3);
  assert.equal(w.input.item,r.output.item);
  assert.equal(w.output.item,'magneticraft2:washed_'+ore);
  assert.equal(w.fluid_input.amount,600);assert.equal(w.fluid_output.amount,600);
  assert.equal(w.fluid_input.fluid,'minecraft:water');
  assert.equal(w.fluid_output.fluid,'magneticraft2:dirty_water');
  assert.equal(w.process_time,240);
  assert.ok(json(data+'recipes/mechanical_sifting_'+ore+'.json').input.item==='magneticraft2:washed_'+ore);
}
const book=json(assets+'patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json');
const page=book.pages.find(p=>p.name==='Rotary Ore Classifier');
assert.ok(page?.multiblock?.pattern?.length===3&&page.enable_visualize,'Missing guide Visualize');
assert.equal(book.pages.filter(p=>p.name==='Rotary Ore Classifier').length,1);
const lang=json(assets+'lang/en_us.json');
assert.ok(lang['block.magneticraft2.rotary_ore_classifier']);
for(const path of ['data/minecraft/tags/blocks/mineable/pickaxe.json','data/minecraft/tags/blocks/needs_stone_tool.json']){
 assert.ok(json(base+path).values.includes('magneticraft2:rotary_ore_classifier'),path);
}
console.log('Rotary Ore Classifier source contract passed: 4 facings, 50 hosts/facing, 3 conserved mineral lines, Washer/Sifter, Gear V2, V2 model/voxel, textures, maintenance, tags, JEI, book.');
