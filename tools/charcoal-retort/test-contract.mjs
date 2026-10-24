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
assert.equal(profileRoof.collision.length,38,
 'Four flush copper chimney-seat segments replace four old suspended-wall boxes');
const roofCollision=profileRoof.collision.filter(box=>
  box[1]===48 && box[3]-box[0]===16 && box[5]-box[2]===16);
assert.equal(roofCollision.length,8,'Expected eight roof slabs');
assert(roofCollision.every(box=>box[4]===58),
  'Voxel roof height must match the 10px copper slab, not old raised bands');

// V3.5: discrete, integer-pixel vent walls, NO floating lower collar.
// Both full-cut vent rows and the 6x6 central shaft stay open in all facings;
// UV spans must match model-pixel face spans (no stretched textures).
const chimneyRotate=(x,z,dir)=>dir==='south'?[x,z]:
  dir==='west'?[16-z,x]:dir==='north'?[16-x,16-z]:[z,16-x];
const chimneyRotateBox=(box,dir)=>{
  const [x0,y0,z0,x1,y1,z1]=box;
  const corners=[[x0,z0],[x0,z1],[x1,z0],[x1,z1]]
      .map(([x,z])=>chimneyRotate(x,z,dir));
  return [Math.min(...corners.map(p=>p[0])),y0,
    Math.min(...corners.map(p=>p[1])),
    Math.max(...corners.map(p=>p[0])),y1,
    Math.max(...corners.map(p=>p[1]))];
};
const chimneyOverlaps=(e,b)=>e.from.every((v,i)=>
  Math.min(e.to[i],b[i+3])-Math.max(v,b[i])>0.00001);
const v35OpenSlots=[
 [7,11.1,11.1,9,11.9,12.9],
 [7,13.1,11.1,9,13.9,12.9],
 [7,11.1,3.1,9,11.9,4.9],
 [7,13.1,3.1,9,13.9,4.9],
 [3.1,11.1,7,4.9,11.9,9],
 [3.1,13.1,7,4.9,13.9,9],
 [11.1,11.1,7,12.9,11.9,9],
 [11.1,13.1,7,12.9,13.9,9]
];
const chimneyFaceAxes={
 north:[0,1],south:[0,1],
 east:[2,1],west:[2,1],
 up:[0,2],down:[0,2]
};
for(const dir of dirs){
 const offset={south:'0_3_-1',north:'0_3_1',west:'1_3_0',east:'-1_3_0'}[dir];
 const model=json(resource+'assets/magneticraft2/models/multiblock/charcoal_retort_'+
    dir+'/part_'+offset+'.json');
 assert(Math.max(...model.elements.map(e=>e.to[1]))===16,'chimney rim at 16px');
 // The center AIR-host is now the missing 9th roof surface: four copper
 // quarters go from Y=0..10 and are flush with the eight roof slab tiles.
 // They are NOT a raised platform and do not cover the 6x6 chimney hole.
 assert(Math.min(...model.elements.map(e=>e.from[1]))===0,
   'Central roof cell must connect down to the actual roof layer: '+dir);
 assert(model.elements.some(e=>e.to[1]===10&&e.faces.up?.texture==='#roof'),
   'Central copper roof seat must finish flush at Y=10: '+dir);
 assert(model.elements.length<120,'Chimney model should avoid runaway mesh fragments');
 for(const [i,box] of [...v35OpenSlots,[6.1,0.1,6.1,9.9,15.9,9.9]].entries()){
   const rotated=chimneyRotateBox(box,dir);
   assert(!model.elements.some(e=>chimneyOverlaps(e,rotated)),
     'V3.5 chimney vent or central shaft blocked: '+dir+' region '+i);
 }

 // Regress the originally missing copper roof seat around the open shaft:
 // the center cell's top must have material at every outside corner.
 for(const [x,z] of [[1,1],[1,14],[14,1],[14,14]]){
   const r=chimneyRotateBox([x,9.8,z,x+0.5,10.1,z+0.5],dir);
   assert(model.elements.some(e=>chimneyOverlaps(e,r)),
     'Floating chimney: center roof corner has no copper support '+dir);
 }
 assert(model.elements.some(e=>e.faces.down?.texture==='#roof'),
   'Exposed top-rim and chimney underside must have copper texture');
 const inwardCopper=model.elements.some(e=>Object.entries(e.faces).some(([side,face])=>{
   const [x,y,z]=e.from,[X,Y,Z]=e.to;
   const inward= (side==='east'&&X===5&&z>=5&&Z<=11)||
     (side==='west'&&x===11&&z>=5&&Z<=11)||
     (side==='south'&&Z===5&&x>=5&&X<=11)||
     (side==='north'&&z===11&&x>=5&&X<=11);
   return inward&&face.texture==='#roof';
 }));
 assert(inwardCopper,'Chimney inner walls must have visible copper UV texture');
 for(const e of model.elements)for(const [side,face] of Object.entries(e.faces)){
   assert(model.textures[face.texture.replace(/^#/,'')],
     'Unresolved chimney face texture: '+dir+' '+side);
   assert(Array.isArray(face.uv)&&face.uv.length===4,
     'Missing UV for chimney face: '+dir+' '+side);
   const axes=chimneyFaceAxes[side];
   const expected=axes.map(axis=>e.to[axis]-e.from[axis]);
   const actual=[Math.abs(face.uv[2]-face.uv[0]),
                 Math.abs(face.uv[3]-face.uv[1])];
   assert(expected.every((v,i)=>Math.abs(v-actual[i])<0.00001),
     'UV scale is stretched/compressed on '+dir+' '+side);
   assert(face.uv.every(v=>v>=0&&v<=16),
     'UV samples outside the 16x16 texture: '+dir+' '+side);
 }
}
// V3.5 affects only the chimney model. The roof's long inset grooves
// continue to be tested above independently.

// V3.7 — upper refractory chamber only. Do not make model edits to
// accepted copper roof Y=3 or service-layer Y=1 when reviewing Y=2.
for(const dir of dirs){
  const def=json(resource+'data/magneticraft2/multiblocks/charcoal_retort_'+dir+'.json');
  const base=def.structure.layout.layer1, upper=def.structure.layout.layer3;
  let cx=-1,cz=-1;
  for(let z=0;z<3;z++)for(let x=0;x<3;x++)
    if(base[z][x]==='C'){cx=x;cz=z;}
  let masonryCount=0,airCount=0;
  for(let z=0;z<3;z++)for(let x=0;x<3;x++){
    const id=upper[z][x],modelPath=resource+
      'assets/magneticraft2/models/multiblock/charcoal_retort_'+dir+
      '/part_'+(x-cx)+'_2_'+(z-cz)+'.json';
    if(id==='A'){
      airCount++;
      assert(!fs.existsSync(new URL(modelPath,root)),
        'Y2 center must remain AIR and not hide the exhaust throat: '+dir);
    }else{
      assert.equal(id,'B','Y2 construction stays entirely masonry: '+dir);
      masonryCount++;
      const chamber=json(modelPath);
      assert.equal(chamber.textures.brick,'minecraft:block/bricks',
        'Y2 brickwork must use native Minecraft tile scale: '+dir);
      assert(chamber.elements.length>0,'Empty chamber model: '+modelPath);
      for(const e of chamber.elements){
        assert(e.from.every((v,i)=>v>=0&&e.to[i]<=16&&e.to[i]>v),
          'Chamber exceeds its block cell: '+modelPath);
      }
    }
  }
  assert.equal(masonryCount,8);
  assert.equal(airCount,1);
}
const upperFront = southModel('part_0_2_0');
const ventDividers=upperFront.elements.filter(e=>
  e.faces.south?.texture==='#metal' && e.from[0]>=5 &&
  e.to[0]<=12 && e.from[1]>=9);
assert(ventDividers.length>=4,
  'Upper chamber must have four grille bars with five open slots');
assert(upperFront.elements.some(e=>
  e.faces.south?.texture==='#soot' && e.to[2]<=10),
  'Upper vent must have dark recessed backing, not painted-on bars');
for(const x of [-1,1]){
  const shoulder=southModel('part_'+x+'_2_0');
  assert(shoulder.elements.some(e=>
    Object.values(e.faces).some(f=>f.texture==='#join')),
    'Front Y2 corner should retain its riveted iron bracket');
}
// V3.7 keeps Y1 and Y3 geometry as-is; this is verified by asset-delta
// comparison during commit staging, while these assertions pin Y2 intent.
const be=read('src/main/java/com/magneticraft2/common/blockentity/stage/bronze/CharcoalRetortBlockEntity.java');
assert(be.includes('insertItem(1, result, true)')&&be.includes('insertItem(2, tar, true)'));
assert(be.includes('RetortBurnRemaining')&&be.includes('RetortProgress'));
assert(be.includes('ForgeCapabilities.ITEM_HANDLER'));
console.log('Charcoal Retort source/data contracts passed');
