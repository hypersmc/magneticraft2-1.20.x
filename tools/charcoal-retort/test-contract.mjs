import fs from 'node:fs';
import assert from 'node:assert/strict';

const root = new URL('../../', import.meta.url);
const read = path => fs.readFileSync(new URL(path, root), 'utf8');
const json = path => JSON.parse(read(path));
const resource = 'src/main/resources/';
const dirs = ['south','west','north','east'];

const canonicalCounts = {S:8,C:1,B:13,Y:1,U:1,O:1,A:3,P:8};
for(const dir of dirs) {
  const path = resource + 'data/magneticraft2/multiblocks/charcoal_retort_' + dir + '.json';
  const def = json(path);
  assert.deepEqual(def.structure.dimensions,[3,4,3]);
  const layers = Array.from({length:4},(_,i)=>def.structure.layout['layer'+(i+1)]);
  assert(layers.every(layer => layer.length===3 && layer.every(row=>row.length===3)));
  const symbols = layers.flat(2);
  const counts = Object.fromEntries(Object.keys(canonicalCounts).map(k=>[k,symbols.filter(s=>s===k).length]));
  assert.deepEqual(counts,canonicalCounts,dir);
  assert.deepEqual(def.settings.render_air_anchors.length,1);
  assert.equal(def.settings.voxel_profile,'magneticraft2:charcoal_retort');
  assert.equal(def.settings.render_air_anchors[0][1],3);
}
const voxels=json(resource+'data/magneticraft2/multiblock_voxels/charcoal_retort.json');
assert.equal(voxels.format,1);
assert(voxels.collision.length>=30);
assert.deepEqual(voxels.collision,voxels.selection);

const recipe=json(resource+'data/magneticraft2/recipes/retorting_logs.json');
assert.equal(recipe.machine,'magneticraft2:charcoal_retort');
assert.equal(recipe.input.tag,'minecraft:logs_that_burn');
assert.equal(recipe.input_count,4);
assert.equal(recipe.output.item,'minecraft:charcoal');
assert.equal(recipe.output.count,4);
assert.equal(recipe.byproduct.item,'magneticraft2:wood_tar');
assert.equal(recipe.byproduct.count,1);
assert.equal(recipe.byproduct_chance,1);
assert.equal(recipe.process_time,720);
const guide=json(resource+'assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json');
assert(guide.pages.some(p=>p.type==='patchouli:multiblock'&&p.name==='Charcoal Retort'));
const buildPage = guide.pages.find(p=>p.type==='patchouli:multiblock'&&p.name==='Charcoal Retort');
// The oriented Patchouli projection reverses row columns relative to the
// SOUTH JSON layout. Match actual materials and modules in every layer.
const south = json(resource+'data/magneticraft2/multiblocks/charcoal_retort_south.json');
const asSymbol = ch => ch === '0' ? 'C' : ch === '_' ? 'A' : ch;
for(let y=0;y<4;y++) {
  const projected = buildPage.multiblock.pattern[3-y]
      .map(row => [...row].reverse().map(asSymbol));
  assert.deepEqual(projected, south.structure.layout['layer'+(y+1)],
      'Patchouli formation mismatch in physical layer '+y);
}
assert.equal(buildPage.multiblock.pattern[2][2], 'OBU');


const states = json(resource+'assets/magneticraft2/blockstates/charcoal_retort.json');
const textureRoles = json(resource+'assets/magneticraft2/models/block/charcoal_retort_controller.json').textures;
for(const role of ['stone','metal','join','bronze','brick','roof','soot','vent','off','on']) {
  assert(textureRoles[role], 'Missing V2 material '+role);
}
const sourceBlueprint = json(resource+'data/magneticraft2/multiblocks/charcoal_retort_south.json');
const rot=(x,z,d)=>d==='south'?[x,z]:d==='west'?[-z,x]:d==='north'?[-x,-z]:[z,-x];
for(const dir of dirs){
  const lit = states.variants[`facing=${dir},is_formed=true,lit=true`];
  const cold = states.variants[`facing=${dir},is_formed=true,lit=false`];
  assert(lit && cold);
  assert(!Object.hasOwn(cold, 'y'), dir+' has double Y rotation');
  assert(!Object.hasOwn(lit, 'y'), dir+' has double Y rotation');
  const layout=sourceBlueprint.structure.layout;
  for(let y=0;y<4;y++)for(let z=0;z<3;z++)for(let x=0;x<3;x++){
    const k=layout['layer'+(y+1)][z][x];
    const [rx,rz]=rot(x-1,z-2,dir);
    const rel=resource+'assets/magneticraft2/models/multiblock/charcoal_retort_'+dir+'/part_'+rx+'_'+y+'_'+rz+'.json';
    if(['U','O','Y'].includes(k)){
      assert(!fs.existsSync(new URL(rel, root)), 'V2 fake model hides working '+k+' module');
      continue;
    }
    if(k==='A'&&y!==3)continue;
    assert(fs.existsSync(new URL(rel, root)), 'V2 missing part '+rel);
    const m=json(rel);
    assert(Array.isArray(m.elements)&&m.elements.length>0,'Empty V2 part '+rel);
    for(const e of m.elements){
      for(let i=0;i<3;i++) assert(e.from[i]>=0&&e.to[i]<=16&&e.from[i]<e.to[i],
        'V2 model outside local cell: '+rel);
    }
  }
}
assert(fs.existsSync(new URL('tools/charcoal-retort/build.mjs', root)),
  'Retort generator must be checked for drift with --check');


// The generator's output is the committed model contract: check mesh surfaces,
// not just box counts. Front trim must no longer draw coplanar intersecting
// quads and no Retort part may render inside a real item-port cell.
const faceAxes={
 north:[2,0,1],south:[2,0,1],
 west:[0,2,1],east:[0,2,1],
 down:[1,0,2],up:[1,0,2]
};
for(const dir of dirs){
 const blueprint=json(resource+'data/magneticraft2/multiblocks/charcoal_retort_'+dir+'.json');
 const flat=[];
 for(let y=0;y<4;y++)for(let z=0;z<3;z++)for(let x=0;x<3;x++){
  const key=blueprint.structure.layout['layer'+(y+1)][z][x];
  if(['U','O','Y','A'].includes(key)&&!(key==='A'&&y===3))continue;
  const absolute='src/main/resources/assets/magneticraft2/models/multiblock/charcoal_retort_'+dir+'/';
  // The per-facing offset is determined relative to the actual controller.
  const p=blueprint.structure.layout.layer1;
  const origin={x:0,z:0};
  for(let zz=0;zz<3;zz++)for(let xx=0;xx<3;xx++)if(p[zz][xx]==='C'){
   origin.x=xx;origin.z=zz;
  }
  const rel='part_'+(x-origin.x)+'_'+y+'_'+(z-origin.z)+'.json';
  const file=absolute+rel;
  assert(fs.existsSync(new URL(file,root)),'Missing Retort V2 model '+file);
  const body=json(file);
  for(const e of body.elements){
    for(let i=0;i<3;i++)assert(e.from[i]>=0&&e.to[i]<=16&&e.to[i]>e.from[i],
     'Retort element leaked into adjacent block: '+file);
    for(const [side,spec] of Object.entries(e.faces)){
     const [axis,u,v]=faceAxes[side];
     const coord=['east','up','south'].includes(side)?e.to[axis]:e.from[axis];
     flat.push({plane:axis+':'+(coord+([x-origin.x,y,z-origin.z][axis])*16),
       rect:[e.from[u]+([x-origin.x,y,z-origin.z][u])*16,
             e.from[v]+([x-origin.x,y,z-origin.z][v])*16,
             e.to[u]+([x-origin.x,y,z-origin.z][u])*16,
             e.to[v]+([x-origin.x,y,z-origin.z][v])*16]});
    }
  }
 }
 const groups=new Map();
 for(const face of flat){if(!groups.has(face.plane))groups.set(face.plane,[]);groups.get(face.plane).push(face.rect);}
 for(const [plane,faces] of groups)for(let i=0;i<faces.length;i++)for(let j=i+1;j<faces.length;j++){
  const a=faces[i],b=faces[j];
  const sharedX=Math.min(a[2],b[2])-Math.max(a[0],b[0]);
  const sharedY=Math.min(a[3],b[3])-Math.max(a[1],b[1]);
  assert(!(sharedX>0.00001&&sharedY>0.00001),'Coplanar overlapping Retort faces at '+dir+': '+plane);
 }
}


for(const dir of dirs)for(const role of ['input','primary','byproduct']){
  const file=resource+'assets/magneticraft2/models/multiblock/charcoal_retort_'+dir+'/port_'+role+'.json';
  const model=json(file);
  assert(model.elements.length>0,'Empty V3 real port '+file);
  assert(model.elements.every(e=>e.from.every((v,i)=>v>=0&&e.to[i]<=16&&e.to[i]>v)),
    'Port model overflows its REAL block cell: '+file);
  const selector=role==='input'?'input':role==='primary'?'primary_output':'byproduct_output';
  const state=json(resource+'assets/magneticraft2/blockstates/multiblock_item_'+selector+'.json');
  assert(state.variants[''].model.endsWith('_selector'),'Shared port must use fallback selector');
  const dynamic=json(resource+'assets/magneticraft2/models/block/multiblock_item_'+selector+'_selector.json');
  assert.equal(dynamic.loader,'magneticraft2:multiblock');
  assert.equal(dynamic.baseModel,'magneticraft2:models/block/multiblock_item_'+selector+'.json');
}
const portSource=read('src/main/java/com/magneticraft2/common/blockentity/stage/copper/MultiblockItemPortBlockEntity.java');
assert(portSource.includes('linkRetortController('));
assert(portSource.includes('getModelData()') && portSource.includes('CharcoalRetortBlockEntity'));
const clientSetup=read('src/main/java/com/magneticraft2/client/Clientsetup.java');
assert(clientSetup.includes('charcoal_retort_')&&clientSetup.includes('port_'));
const retortGenerator=read('tools/charcoal-retort/build.mjs');
assert(retortGenerator.includes('finalizeRetortV3(files,v3.ports)'));
const sourceV3=read('tools/charcoal-retort/v3-models.mjs');
assert(sourceV3.includes('portShapes')&&sourceV3.includes('files.delete(path)'));
// V3.1 proportion regression: a real chimney above the roof edge,
 // and a large DEEP front hatch instead of a grille painted on a full cube.
const southModel = name => json(resource+'assets/magneticraft2/models/multiblock/charcoal_retort_south/'+name+'.json');
const roofEdge = southModel('part_1_3_0');
const chimney = southModel('part_0_3_-1');
const highest = model => Math.max(...model.elements.map(e=>e.to[1]));
assert(highest(chimney)-highest(roofEdge)>=4,
  'Retort chimney rim must stand clearly above roof coping');
const door = southModel('part_0_1_0');
const recessedFaces=door.elements.filter(e=>e.faces.south?.texture==='#soot'&&e.to[2]<=12);
assert(recessedFaces.length>=3,'Main retort hatch lost its deep dark recess');
for(const role of ['input','primary']){
  const port=southModel('port_'+role);
  const expected=role==='input'?'#wood':'#charcoal';
  assert(port.elements.some(e=>Object.values(e.faces).some(f=>f.texture===expected)),
    'Retort '+role+' must have a distinct chamber interior');
}
// Roof-only redesign: prove it stays a continuous copper slab with two
// lengthwise side bands and a genuinely open middle chimney (no center lid).
for (const dir of dirs) {
  const layout=json(resource+'data/magneticraft2/multiblocks/charcoal_retort_'+dir+'.json').structure.layout;
  const ground=layout.layer1;
  let centerX=-1,centerZ=-1;
  for(let z=0;z<3;z++)for(let x=0;x<3;x++)if(ground[z][x]==='C'){centerX=x;centerZ=z;}
  let copperTiles=0,centerHole=0;
  for(let z=0;z<3;z++)for(let x=0;x<3;x++){
    const symbol=layout.layer4[z][x];
    const p=resource+'assets/magneticraft2/models/multiblock/charcoal_retort_'+dir+
      '/part_'+(x-centerX)+'_3_'+(z-centerZ)+'.json';
    const roof=json(p);
    assert.equal(roof.textures.roof,'minecraft:block/cut_copper', 'roof must use copper slab texture');
    assert(roof.elements.length>0);
    if(symbol==='P'){
      copperTiles++;
      assert(roof.elements.some(e=>Object.values(e.faces).some(f=>f.texture==='#roof')),
        'Copper roof slab missing '+p);
      assert(Math.max(...roof.elements.map(e=>e.to[1]))<=12,
        'roof caps cannot be tall full-block boxes '+p);
    }else{
      assert.equal(symbol,'A','only chimney AIR is permitted in the roof');
      centerHole++;
      const open=roof.elements.every(e=>
        !(Math.min(e.to[0],10.9)-Math.max(e.from[0],5.1)>0.00001 &&
          Math.min(e.to[2],10.9)-Math.max(e.from[2],5.1)>0.00001 &&
          Math.min(e.to[1],15.9)-Math.max(e.from[1],5.1)>0.00001));
      assert(open,'chimney must have a real hollow shaft '+p);
      assert(Math.max(...roof.elements.map(e=>e.to[1]))===16,'chimney rim must rise above roof');
    }
  }
  assert.equal(copperTiles,8,'eight solid copper slabs around opening');
  assert.equal(centerHole,1,'exactly one central chimney');
}
// V3.3 — both longitudinal lines are CUT INTO copper at height 8.75.
// Surface is at height 10, so the dark floor must never be a raised strap.
// Each orientation contains six grooved roof tiles (two 3-block lengths).
for (const dir of dirs) {
  const layout=json(resource+'data/magneticraft2/multiblocks/charcoal_retort_'+dir+'.json').structure.layout;
  let cx=0,cz=0;
  for(let z=0;z<3;z++)for(let x=0;x<3;x++){
    if(layout.layer1[z][x]==='C'){cx=x;cz=z;}
  }
  let grooves=0;
  for(let z=0;z<3;z++)for(let x=0;x<3;x++){
    if(layout.layer4[z][x]!=='P')continue;
    const path=resource+'assets/magneticraft2/models/multiblock/charcoal_retort_'+dir+
      '/part_'+(x-cx)+'_3_'+(z-cz)+'.json';
    const roof=json(path);
    const darkFloors=roof.elements.filter(e =>
      e.faces.up?.texture==='#metal' && Math.abs(e.to[1]-8.75)<0.00001);
    const expected=(dir==='south'||dir==='north') ? x!==1 : z!==1;
    assert.equal(darkFloors.length>0,expected,'Recessed side groove position '+path);
    assert(roof.elements.some(e=>e.faces.up?.texture==='#roof' &&
      Math.abs(e.to[1]-10)<0.00001),'Copper top must remain at 10px '+path);
    assert(Math.max(...roof.elements.map(e=>e.to[1]))===10,
      'Grooves must be inside the roof, not raised '+path);
    assert(!roof.elements.some(e=>e.faces.up?.texture==='#metal' &&
      e.to[1]>=10),'Raised metal strap detected '+path);
    if(darkFloors.length)grooves++;
  }
  assert.equal(grooves,6,'Two unbroken 3-block channels per facing');
}
const profileRoof=json(resource+'data/magneticraft2/multiblock_voxels/charcoal_retort.json');
assert.equal(profileRoof.collision.length,34,'Keep voxel component count');
const roofCollision=profileRoof.collision.filter(box=>
  box[1]===48 && box[3]-box[0]===16 && box[5]-box[2]===16);
assert.equal(roofCollision.length,8,'Expected eight roof slabs');
assert(roofCollision.every(box=>box[4]===58),
  'Voxel roof height must match the 10px copper slab, not old raised bands');
const be=read('src/main/java/com/magneticraft2/common/blockentity/stage/bronze/CharcoalRetortBlockEntity.java');
assert(be.includes('insertItem(1, result, true)')&&be.includes('insertItem(2, tar, true)'));
assert(be.includes('RetortBurnRemaining')&&be.includes('RetortProgress'));
assert(be.includes('ForgeCapabilities.ITEM_HANDLER'));
console.log('Charcoal Retort source/data contracts passed');
