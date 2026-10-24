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

// V3.8 — restore missing upper-chamber detailing and correct material roles. Do not make model edits to
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
      // Front cells use vanilla masonry. Side/rear structural
      // backing keeps Retort refractory; raised right-side face bricks
      // use a separate sideBrick texture key in V3.23.
      const frontBrick=(z===2&&dir==='south')||
        (z===0&&dir==='north')||
        (x===0&&dir==='west')||
        (x===2&&dir==='east');
      const expectedBrick=frontBrick?
        'minecraft:block/bricks':
        'magneticraft2:block/charcoal_retort/refractory';
      assert.equal(chamber.textures.brick,expectedBrick,
        'V3.10 front brick material mismatch: '+dir+' x='+x+' z='+z);
      assert.equal(chamber.textures.bronze,'minecraft:block/cut_copper',
        'Y2 copper details must match the accepted roof, not appear wooden: '+dir);
      assert.equal(chamber.textures.join,
        'magneticraft2:block/settling_tank/riveted_iron',
        'Y2 corner fasteners must have iron material: '+dir);
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
  e.faces.south?.texture==='#bronze' && e.from[1]===11.3 &&
  e.to[1]===14.1 && Math.abs(e.to[0]-e.from[0]-0.55)<0.00001);
assert.equal(ventDividers.length,4,
  'Copper upper grille must have four integrated mullions and five dark slots');
assert(upperFront.elements.some(e=>
  e.faces.south?.texture==='#soot' && Math.abs(e.to[2]-13.75)<0.00001),
  'Upper vent must have actual recessed soot backing, not rods in empty air');
for(const x of [-1,1]){
  const shoulder=southModel('part_'+x+'_2_0');
  assert(shoulder.elements.some(e=>
    Object.values(e.faces).some(f=>f.texture==='#join')),
    'Front Y2 corner should retain its dark riveted iron bracket');
  assert(shoulder.elements.some(e=>e.faces.south?.texture==='#bronze' &&
    e.from[1]>=3 && e.to[1]<=15),
    'Upper front shoulder must have real copper pilaster geometry');
}
// V3.7 keeps Y1 and Y3 geometry as-is; this is verified by asset-delta
// comparison during commit staging, while these assertions pin Y2 intent.

// V3.9: two roof groove lines must CONTINUE DOWN THE FRONT COPPER LIP.
// The previous roof pass had dark channels on the horizontal copper
// surface but stopped at the vertical face. Also verify the Y=2 front
// brick shoulders are actually visible at the face rather than terminating
// three pixels behind their copper ornamentation.
const v39Rotate=(x,z,dir)=>dir==='south'?[x,z]:
  dir==='west'?[-z,x]:dir==='north'?[-x,-z]:[z,-x];
for(const dir of dirs)for(const side of [-1,1]){
  const [rx,rz]=v39Rotate(side,0,dir);
  const roof=json(resource+
      'assets/magneticraft2/models/multiblock/charcoal_retort_'+dir+
      '/part_'+rx+'_3_'+rz+'.json');
  const ends=roof.elements.filter(e=>e.faces[dir]?.texture==='#metal'
        && e.from[1]===3 && e.to[1]>=8);
  assert(ends.length>=1,
      'Dark grooved roof band must continue down its front copper lip: '+dir+' '+side);
  const brick=json(resource+'assets/magneticraft2/models/multiblock/charcoal_retort_'
      +dir+'/part_'+rx+'_2_'+rz+'.json');
  const forwardWall=brick.elements.filter(e=>e.faces[dir]?.texture==='#brick'
       && e.to[1]>=10 && e.from[1]<=5);
  assert(forwardWall.length>=1,
      'Y2 front shoulders should have a continuous visible masonry field: '+dir+' '+side);
}
// V3.10: NO full-width dark metal strip immediately below the roof.
// That was a V3.9 addition not present in the user's reference.
for(const dir of dirs){
  const frontOffsets = {
    south:[[-1,0],[0,0],[1,0]],
    north:[[1,0],[0,0],[-1,0]],
    west:[[0,-1],[0,0],[0,1]],
    east:[[0,1],[0,0],[0,-1]]
  }[dir];
  for(const [x,z] of frontOffsets){
    const model=json(resource+
      'assets/magneticraft2/models/multiblock/charcoal_retort_'+dir+
      '/part_'+x+'_2_'+z+'.json');
    const unwanted=model.elements.some(e=>
      e.faces[dir]?.texture==='#metal'&&
      e.from[1]===14&&e.to[1]===15&&
      (e.to[0]-e.from[0]===16||e.to[2]-e.from[2]===16));
    assert(!unwanted,'Unwanted full-width metal roof-underlining: '+dir+'/'+x+'/'+z);
  }
}
// The roof geometry is unchanged. Four west/east roof models have
// equivalent-but-merged underside faces due to changed neighboring Y2
// culling; their visible top, groove and fascia faces are identical.
// Physical Y=1 modules and Y=3 chimney/roof grooves are independently
// preserved by the byte-for-byte diff scope check used when committing.
// V3.16: physically raised masonry courses with clearly recessed mortar.
// V3.15's accepted-in-principle L-shaped framing is preserved in place.
// Reject paper-thin relief, an empty cavity, and any right-hand U-post.
const v314LeftParts={south:'-1_2_0',west:'0_2_-1',north:'1_2_0',east:'0_2_1'};
for(const [dir,part] of Object.entries(v314LeftParts)){
  const panel=json(resource+
    'assets/magneticraft2/models/multiblock/charcoal_retort_'+dir+
    '/part_'+part+'.json');
  assert.equal(panel.textures.brick,'minecraft:block/bricks',
    'Front-left bricks must match reference masonry: '+dir);
  assert.equal(panel.textures.mortar,'minecraft:block/stone',
    'Mortar should be real visible material: '+dir);
  assert.equal(panel.textures.bronze,'minecraft:block/cut_copper',
    'Copper trim must match accepted roof: '+dir);
  const front=panel.elements.filter(e=>e.faces[dir]);
  const depth=e=>dir==='south'?e.to[2]:
    dir==='west'?16-e.from[0]:
    dir==='north'?16-e.from[2]:e.to[0];
  const material=(e)=>e.faces[dir].texture;
  const brick=front.filter(e=>material(e)==='#brick'&&
    depth(e)>=15.449&&depth(e)<=15.701);
  const mortar=front.filter(e=>material(e)==='#mortar'&&
    Math.abs(depth(e)-14.2)<0.00001);
  // The baked mesh partitioner can split individual course faces where
  // corner trim occludes them; demand at least one part of each row.
  for(const y of [3,7.05,11.1]){
    assert(brick.some(e=>Math.abs(e.from[1]-y)<0.00001),
      'Missing raised physical brick course at Y='+y+' for '+dir);
  }
  assert(brick.length>=7,'V3.14 insufficient individual masonry faces '+dir);
  assert(mortar.length>=5,'Mortar gaps hidden or missing '+dir);
  assert(brick.every(e=>depth(e)<=15.701),
    'Brick faces must remain behind the L-frame '+dir);
  const uniqueFaceDepths=[...new Set(brick.map(e=>+depth(e).toFixed(2)))].sort();
  assert.deepEqual(uniqueFaceDepths,[15.45,15.70],
    'V3.16 alternating masonry course relief must stay physical: '+dir);
  assert(brick.every(e=>depth(e)-14.2>=1.249),
    'V3.16 restore at least 1.25px depth above mortar '+dir);
  const has=(tex,test)=>front.some(e=>material(e)===tex&&test(e));
  assert(has('#bronze',e=>depth(e)===16&&e.to[1]>10),
    'Raised copper column missing '+dir);
  assert(has('#metal',e=>depth(e)===16&&e.from[1]>=12),
    'Upper iron bracket missing '+dir);
  assert(has('#metal',e=>depth(e)===16&&e.to[1]<=3),
    'Lower iron foot missing '+dir);
  assert(has('#join',e=>depth(e)===16),
    'Inset riveted plate missing '+dir);
  assert(has('#stone',e=>depth(e)===16),
    'Dark bracket center missing '+dir);
  // Native per-face UV spans (including narrow fractional brick joints).
  const faceAxes={north:[0,1],south:[0,1],
    east:[2,1],west:[2,1],up:[0,2],down:[0,2]};
  for(const element of panel.elements){
    assert(element.from.every((v,i)=>v>=0&&element.to[i]<=16&&
      element.to[i]>v),'V3.14 block-local geometry overflow '+dir);
    for(const [side,face] of Object.entries(element.faces)){
      assert(panel.textures[face.texture.slice(1)],
        'Missing V3.14 face texture '+dir+'/'+side);
      const ax=faceAxes[side];
      const expected=ax.map(i=>element.to[i]-element.from[i]);
      const actual=[Math.abs(face.uv[2]-face.uv[0]),
        Math.abs(face.uv[3]-face.uv[1])];
      assert(expected.every((v,i)=>Math.abs(v-actual[i])<0.00001),
        'Texture is stretched/compressed on '+dir+'/'+side);
    }
  }
}
// V3.15: the user approved the staggered masonry, NOT the surrounding
// U-shaped framing. This one-block pass must have a full copper header
// bridging the brick panel, and remove the *right* full-height copper
// post and its dark top/bottom caps. The left bracket and pillar remain.
const v315RightPost={south:[13,15,14,16],
  west:[0,2,13,15],north:[1,3,0,2],east:[14,16,1,3]};
for(const [dir,id] of Object.entries(v314LeftParts)){
  const panel=json(resource+
    'assets/magneticraft2/models/multiblock/charcoal_retort_'+dir+
    '/part_'+id+'.json');
  const visible=panel.elements.filter(e=>e.faces[dir]);
  const depth=e=>dir==='south'?e.to[2]:
    dir==='north'?16-e.from[2]:
    dir==='west'?16-e.from[0]:e.to[0];
  const header=visible.filter(e=>e.faces[dir].texture==='#bronze'&&
    e.from[1]>=12.25 && depth(e)===16 &&
    (e.to[0]-e.from[0]>=10 || e.to[2]-e.from[2]>=10));
  assert(header.length>=1,'V3.15 must have a wide horizontal copper header '+dir);
  const [xmin,xmax,zmin,zmax]=v315RightPost[dir];
  const unwantedRight=visible.filter(e=>e.faces[dir].texture==='#bronze'&&
    e.from[1]<=3 && e.to[1]>=13 &&
    e.from[0]>=xmin && e.to[0]<=xmax &&
    e.from[2]>=zmin && e.to[2]<=zmax);
  assert.equal(unwantedRight.length,0,
    'V3.15 rejected symmetrical right-hand copper pillar returned '+dir);
  assert(visible.some(e=>e.faces[dir].texture==='#stone'&&
    e.from[1]>=13),'V3.15 left iron bracket boss disappeared '+dir);
  assert(visible.some(e=>e.faces[dir].texture==='#brick'&&
    e.from[1]>=7.05&&e.to[1]<=10.60),
    'V3.15 retained physical brickwork was lost '+dir);
}
// Only the one left cell is authored; adjacent Y2 center/Y3 roof
// files change at hidden touching faces due to the face partitioner.
// V3.17: user-marked TOP iron fixing and LOWER copper collar only.
// Do not flatten the inset or turn the copper collar back into a tall
// iron foot. Every direction is generated from the same left facade.
for(const [dir,part] of Object.entries(v314LeftParts)){
 const model=json(resource+
    'assets/magneticraft2/models/multiblock/charcoal_retort_'+dir+
    '/part_'+part+'.json');
 const f=model.elements.filter(e=>e.faces[dir]);
 const depth=e=>dir==='south'?e.to[2]:
   dir==='west'?16-e.from[0]:
   dir==='north'?16-e.from[2]:e.to[0];
 const has=(material,check)=>f.some(e=>
   e.faces[dir].texture===material && check(e));
 assert(has('#stone',e=>e.from[1]===13&&e.to[1]===15&&
   Math.abs(depth(e)-15.65)<0.00001),
   'V3.17 dark square bracket center must be physically recessed '+dir);
 assert(has('#metal',e=>e.from[1]===12&&e.to[1]===12.5&&
   depth(e)===16),
   'V3.17 upper dark iron bracket must retain raised border '+dir);
 assert(has('#join',e=>e.from[1]===12.5&&e.to[1]===15.5&&
   depth(e)===16),
   'V3.17 upper bracket inset riveted border missing '+dir);
 assert(has('#bronze',e=>e.from[1]===1.25&&e.to[1]===4.5&&
   depth(e)===16),
   'V3.17 lower-left copper shoulder missing '+dir);
 assert(has('#metal',e=>e.from[1]===0&&e.to[1]===1.25&&
   depth(e)===16),
   'V3.17 lower dark iron foot must remain BELOW the copper shoulder '+dir);
 // The brick courses remain physically raised and the L-frame stays.
 for(const level of [3,7.05,11.1])assert(
   has('#brick',e=>e.from[1]===level&&depth(e)>=15.45),
   'V3.17 brick relief regressed at '+level+' '+dir);
 assert(has('#bronze',e=>e.from[1]>=12.25&&e.to[1]===16&&
   depth(e)===16),
   'V3.17 copper L-header disappeared '+dir);
}
// V3.18 center upper-front block: actual five-slot faceplate, raised
// left/right copper supports and a low copper arch — NO free-floating
// bars in a roof cavity. Only this center physical cell is authored.
// Test every facing from the same outward camera-relative direction.
for(const dir of dirs){
  const center=json(resource+'assets/magneticraft2/models/multiblock/charcoal_retort_'
    +dir+'/part_0_2_0.json');
  const horizontal=(dir==='south'||dir==='north')?0:2;
  const normal=(dir==='south'||dir==='north')?2:0;
  const positive=(dir==='south'||dir==='east');
  const depth=e=>positive?e.to[normal]:16-e.from[normal];
  const front=center.elements.filter(e=>e.faces[dir]);
  const material=e=>e.faces[dir].texture;
  const mullions=front.filter(e=>material(e)==='#bronze' &&
    e.from[1]===11.3 && e.to[1]===14.1 &&
    Math.abs(e.to[horizontal]-e.from[horizontal]-0.55)<0.00001)
    .map(e=>[e.from[horizontal],e.to[horizontal]])
    .sort((a,b)=>a[0]-b[0]);
  assert.equal(mullions.length,4,
    'Center vent should have four copper mullions '+dir);
  const frontAt=(x,y)=>{
    const covering=front.filter(e=>e.from[horizontal]<x &&
      x<e.to[horizontal] && e.from[1]<y && y<e.to[1])
      .sort((a,b)=>depth(b)-depth(a));
    return covering[0] ? {
      texture:material(covering[0]),depth:depth(covering[0])
    }:null;
  };
  const slots=[mullions[0][0]-0.45,
    ...[0,1,2].map(i=>(mullions[i][1]+mullions[i+1][0])/2),
    mullions[3][1]+0.45];
  for(const x of slots){
    const hit=frontAt(x,12.7);
    assert(hit && hit.texture==='#soot' &&
      Math.abs(hit.depth-13.75)<0.00001,
      'Vent slot is blocked, empty or too deeply recessed: '+dir+'/'+x);
  }
  for(const [x0,x1] of mullions){
    const hit=frontAt((x0+x1)/2,12.7);
    assert(hit && hit.texture==='#bronze' && hit.depth===16,
      'Vent bar not integrated into front copper plate: '+dir+'/'+x0);
  }
  const ironBosses=front.filter(e=>material(e)==='#stone' &&
    e.from[1]===12.2 && Math.abs(depth(e)-15.65)<0.00001);
  assert.equal(ironBosses.length,2,'Both vent end fittings need dark insets '+dir);
  const pillars=front.filter(e=>material(e)==='#bronze' &&
    e.from[1]<=3.3 && e.to[1]>=7 && depth(e)===16);
  assert(pillars.length>=2,'Missing paired copper supports below vent '+dir);
  assert(front.some(e=>material(e)==='#bronze' &&
    e.from[1]<=2 && e.to[1]>=2 && depth(e)===16),
    'Low stepped copper arch missing '+dir);
  assert(center.elements.every(e=>e.from.every((v,i)=>
    v>=0&&e.to[i]<=16&&e.to[i]>v)),
    'One-block center facade overflow '+dir);
}
// V3.19 — the mounting plates must be physically connected to the
// vertical copper ribs (not 0.3 model pixels above them). The lower
// apron must be a shallow centered ARCH, with BOTH outlines rising
// toward the middle instead of four rectangular/corner shelves.
const archProfile=[
 [2.8,4,0.9,2.65],[4,5.2,1.25,3],
 [5.2,6.4,1.7,3.4],[6.4,9.6,2.1,3.85],
 [9.6,10.8,1.7,3.4],[10.8,12,1.25,3],
 [12,13.2,0.9,2.65]
];
for(const dir of dirs){
 const obj=json(resource+'assets/magneticraft2/models/multiblock/charcoal_retort_'
   +dir+'/part_0_2_0.json');
 const front=obj.elements.filter(e=>e.faces[dir]);
 const horizontal=(dir==='south'||dir==='north')?0:2;
 const normal=(dir==='south'||dir==='north')?2:0;
 const positive=dir==='south'||dir==='east';
 const depth=e=>positive?e.to[normal]:16-e.from[normal];
 const copper=front.filter(e=>e.faces[dir].texture==='#bronze' &&
   depth(e)===16);
 const brackets=front.filter(e=>e.faces[dir].texture==='#metal' &&
   e.from[1]===11.5&&e.to[1]===12.2&&depth(e)===16);
 // Inspect the VISIBLE seam, not the number of mesh rectangles:
 // wider V3.21 copper uprights are split around the curved apron by
 // automatic face partitioning, but remain one connected structure.
 const connectedAt=(x,y)=>{
   const faces=front.filter(e=>e.from[horizontal]<x &&
     x<e.to[horizontal] && e.from[1]<y && y<e.to[1])
     .sort((a,b)=>depth(b)-depth(a));
   return faces.length ? {texture:faces[0].faces[dir].texture,
     depth:depth(faces[0])}:null;
 };
 assert.equal(brackets.length,2,
   'V3.19 two upper iron fixing bottom rails required '+dir);
 for(const x of [1.6,14.4]){
   const lower=connectedAt(x,11.45);
   const upper=connectedAt(x,11.55);
   assert(lower&&lower.texture==='#bronze'&&lower.depth===16 &&
     upper&&upper.texture==='#metal'&&upper.depth===16,
     'V3.19 copper rib must touch its upper iron mounting '+dir+'/'+x);
 }
 const arch=copper.filter(e=>e.from[1]>=0.9-0.00001 &&
   e.to[1]<=3.85+0.00001 &&
   e.from[horizontal]>=2.8-0.00001 &&
   e.to[horizontal]<=13.2+0.00001 &&
   e.to[1]-e.from[1]>=1.4)
   .map(e=>[e.from[horizontal],e.to[horizontal],
     e.from[1],e.to[1]])
   .sort((a,b)=>a[0]-b[0]);
 assert.equal(arch.length,7,
   'V3.19 arch must have seven separate physical copper segments '+dir);
 for(let i=0;i<arch.length;i++){
   for(let j=0;j<4;j++)assert(
     Math.abs(arch[i][j]-archProfile[i][j])<0.00001,
     'V3.19 incorrect curved arch profile '+dir+' index='+i+' axis='+j);
   if(i>0)assert(Math.abs(arch[i][0]-arch[i-1][1])<0.00001,
     'V3.19 arch segments are detached '+dir+' index='+i);
 }
 // Two mirrored halves, both intrados and extrados rise to the crown.
 assert(arch[3][2]>arch[0][2] && arch[3][3]>arch[0][3],
   'V3.19 apron still flat rather than crowned '+dir);
 // The reference grille keeps the five V3.18 real recessed slots,
 // validated above, with no change to their copper mullions.
}
// V3.20 — FRONT CROSS-BLOCK HEADER. The copper/iron structural fascia
// must physically connect the left, center and right Y2 cells without
// a dark decorative under-roof strip or holes at block boundaries.
// The sample is inside faces/segments (not exactly on mesh edges).
const v320FrontParts={
  south:['-1_2_0','0_2_0','1_2_0'],
  west:['0_2_-1','0_2_0','0_2_1'],
  north:['1_2_0','0_2_0','-1_2_0'],
  east:['0_2_1','0_2_0','0_2_-1']
};
for(const [dir,ids] of Object.entries(v320FrontParts)){
  const horizontal=(dir==='south'||dir==='north')?0:2;
  const normal=(dir==='south'||dir==='north')?2:0;
  const positive=(dir==='south'||dir==='east');
  const reversed=(dir==='north'||dir==='west');
  const front=[];
  for(let blockIndex=0;blockIndex<3;blockIndex++){
    const part=json(resource+
      'assets/magneticraft2/models/multiblock/charcoal_retort_'+dir+
      '/part_'+ids[blockIndex]+'.json');
    for(const e of part.elements){
      if(!e.faces[dir])continue;
      const left=reversed?16-e.to[horizontal]:e.from[horizontal];
      const right=reversed?16-e.from[horizontal]:e.to[horizontal];
      front.push({left:blockIndex*16+left,right:blockIndex*16+right,
        y0:e.from[1],y1:e.to[1],
        depth:positive?e.to[normal]:16-e.from[normal],
        material:e.faces[dir].texture});
    }
  }
  const facingAt=(x,y)=>{
    const hits=front.filter(f=>f.left<x&&x<f.right&&f.y0<y&&y<f.y1)
      .sort((a,b)=>b.depth-a.depth);
    return hits[0]||null;
  };
  // A full 48px row of copper and intentionally interrupted iron
  // fittings, all exactly flush. No air or brick material at this height.
  for(let x=0.125;x<48;x+=0.25){
    const face=facingAt(x,15.7);
    assert(face&&face.depth===16&&
      (face.material==='#bronze'||face.material==='#metal'),
      'V3.20 broken copper/iron header at '+dir+' x='+x);
  }
  for(const x of [15.875,16.125,31.875,32.125]){
    const face=facingAt(x,15.7);
    assert(face&&face.depth===16,
      'V3.20 visible gap at an inter-block upper-facade seam '+dir+'/'+x);
  }
  // Check a point firmly INSIDE the third block: the mesh partitioner
  // may split its header into smaller visible rectangles around trim.
  const rightHeader=facingAt(40.125,15.7);
  assert(rightHeader&&rightHeader.material==='#bronze' &&
    rightHeader.depth===16,
    'V3.20 right brick block lacks continuous copper header '+dir);
}
// Roof top/grooves and left outward faces are also compared
// byte-for-byte by the generator asset-diff check before commit.
// V3.21: center-only 3D masonry and stronger structural proportions.
// Brick faces must physically rise ahead of gray mortar at two depths,
// and both wide copper pillars / end brackets must be visible without
// erasing the five V3.18 vent slots or V3.19 shallow curved arch.
for(const dir of dirs){
  const m=json(resource+'assets/magneticraft2/models/multiblock/charcoal_retort_'
    +dir+'/part_0_2_0.json');
  const h=(dir==='south'||dir==='north')?0:2;
  const n=(dir==='south'||dir==='north')?2:0;
  const positive=dir==='south'||dir==='east';
  const depth=e=>positive?e.to[n]:16-e.from[n];
  const f=m.elements.filter(e=>e.faces[dir]),mat=e=>e.faces[dir].texture;
  assert.equal(m.textures.mortar,'minecraft:block/stone',
    'Center mortar texture must exist only where authored '+dir);
  const course=f.filter(e=>mat(e)==='#brick'&&
    e.from[1]>=4.05&&e.to[1]<=10.25&&depth(e)>=15.45);
  const mortar=f.filter(e=>mat(e)==='#mortar'&&
    Math.abs(depth(e)-14.2)<0.00001);
  assert(course.length>=9,
    'V3.21 visible physical bricks disappeared '+dir);
  for(const y of [4.05,6.2,8.45]){
    assert(course.some(e=>Math.abs(e.from[1]-y)<0.00001),
      'V3.21 staggered row missing at Y='+y+' '+dir);
  }
  const relief=[...new Set(course.map(e=>+depth(e).toFixed(2)))].sort();
  assert.deepEqual(relief,[15.45,15.7],
    'V3.21 brick face relief should alternate '+dir);
  assert(mortar.length>=8,
    'V3.21 recessed mortar not visible between real bricks '+dir);
  const at=(x,y)=>{
    const hit=f.filter(e=>e.from[h]<x&&x<e.to[h]&&
      e.from[1]<y&&y<e.to[1])
      .sort((a,b)=>depth(b)-depth(a))[0];
    return hit&&{texture:mat(hit),depth:depth(hit)};
  };
  // These points fall outside the narrower V3.20 copper/iron fittings.
  for(const x of [1.2,2.88,13.12,14.8]){
    const upper=at(x,8);
    assert(upper&&upper.texture==='#bronze'&&upper.depth===16,
      'V3.21 copper upright not broad enough at '+dir+'/'+x);
  }
  for(const x of [1.2,2.9,13.1,14.8]){
    const base=at(x,0.6);
    assert(base&&base.texture==='#metal'&&base.depth===16,
      'V3.21 wide lower iron foot missing at '+dir+'/'+x);
  }
  for(const x of [0.35,2.65,13.35,15.65]){
    const bracket=at(x,12.4);
    assert(bracket&&bracket.texture==='#metal'&&bracket.depth===16,
      'V3.21 wide vent bracket missing at '+dir+'/'+x);
  }
  for(const x of [1.6,14.4]){
    assert(at(x,11.45)?.texture==='#bronze'&&
      at(x,11.55)?.texture==='#metal',
      'V3.21 widened support is disconnected from metal fitting '+dir);
  }
  // V3.18, V3.19 and V3.20 checks immediately above prove the grille,
  // seven arch segments and continuous 48px front header stay intact.
}
// V3.22 — SINGLE RIGHT UPPER FRONT BLOCK: raised bricks/mortar,
// a recessed top-right square iron fixing and a small lower-right
// copper shoulder over a dark iron foot. Inner edge remains masonry.
// The existing V3.20 three-block header must stay continuous.
const retortRight={
  south:'1_2_0',west:'0_2_1',north:'-1_2_0',east:'0_2_-1'
};
for(const [dir,id] of Object.entries(retortRight)){
 const m=json(resource+'assets/magneticraft2/models/multiblock/charcoal_retort_'+
   dir+'/part_'+id+'.json');
 const horizontal=(dir==='south'||dir==='north')?0:2;
 const normal=(dir==='south'||dir==='north')?2:0;
 const positive=dir==='south'||dir==='east';
 const mirrored=dir==='north'||dir==='east';
 const depth=e=>positive?e.to[normal]:16-e.from[normal];
 const front=m.elements.filter(e=>e.faces[dir]);
 const mat=e=>e.faces[dir].texture;
 assert.equal(m.textures.mortar,'minecraft:block/stone',
   'V3.22 right-hand masonry must have real mortar '+dir);
 const hit=(localX,y)=>{
   const x=mirrored?16-localX:localX;
   const f=front.filter(e=>e.from[horizontal]<x&&x<e.to[horizontal]&&
       e.from[1]<y&&y<e.to[1])
     .sort((a,b)=>depth(b)-depth(a))[0];
   return f&&{material:mat(f),depth:depth(f)};
 };
 const bricks=front.filter(e=>mat(e)==='#brick'&&
   e.from[1]>=3&&e.to[1]<=13.8&&depth(e)>=15.45);
 const depths=[...new Set(bricks.map(e=>+depth(e).toFixed(2)))].sort();
 assert.deepEqual(depths,[15.45,15.7],
   'V3.22 physical brick-course depth lost '+dir);
 for(const y of [3,7.05,11.1])
   assert(bricks.some(e=>e.from[1]-0.00001<=y&&e.to[1]>=y+0.1),
     'V3.22 missing staggered masonry course at '+y+'/'+dir);
 for(const [x,y] of [[4,6.8],[5.65,8.8],[7.55,12.2]]){
   const exposed=hit(x,y);
   assert(exposed&&exposed.material==='#mortar'&&
     Math.abs(exposed.depth-14.2)<0.00001,
     'V3.22 mortar line not recessed at '+dir+'/'+x+','+y);
 }
 assert(hit(4,7.6)?.material==='#brick'&&hit(4,7.6)?.depth>15.4,
   'V3.22 masonry is flat or hollow '+dir);
 assert(hit(14.8,9)?.material==='#bronze' &&
   hit(14.8,9)?.depth===16,
   'V3.22 copper outer support missing '+dir);
 const plate=hit(14,14);
 assert(plate&&plate.material==='#stone'&&
   Math.abs(plate.depth-15.65)<0.00001,
   'V3.22 upper right iron fitting lacks recessed boss '+dir);
 for(const x of [12.2,15.8]){
   const bossEdge=hit(x,14);
   assert(bossEdge&&bossEdge.material==='#metal'&&bossEdge.depth===16,
     'V3.22 upper plate lacks substantial iron perimeter '+dir+'/'+x);
 }
 const shoulder=hit(13.8,2),base=hit(14,0.6);
 assert(shoulder&&shoulder.material==='#bronze'&&shoulder.depth===16,
   'V3.22 lower-right copper shoulder missing '+dir);
 assert(base&&base.material==='#metal'&&base.depth===16,
   'V3.22 dark base must be below the copper shoulder '+dir);
 assert(hit(0.5,8)?.material==='#brick',
   'V3.22 unwanted inner framing instead of masonry '+dir);
 assert(hit(0.1,15.7)?.material==='#bronze'&&
   hit(0.1,15.7)?.depth===16,
   'V3.22 right-hand copper header disconnected at center seam '+dir);
 assert(m.elements.every(e=>e.from.every((v,i)=>
   v>=0&&e.to[i]<=16&&e.to[i]>v)),
   'V3.22 mesh escaped the physical right-hand block '+dir);
}
// Asset diff checks also verify that the center outward grille, all
// approved roof top/sides, formation/ports and collision are unchanged.
// V3.23 — RIGHT HAND SIDE Y2 brick wall, two cells deep.
// Screenshot reference: raised, staggered brickwork without wide
// horizontal dark iron strips. Only the right wall changes now;
// rear-corner metal/copper fittings and front corner remain anchored.
const v323RightSide={
  south:{side:'east',parts:['1_2_-2','1_2_-1']},
  west:{side:'south',parts:['2_2_1','1_2_1']},
  north:{side:'west',parts:['-1_2_2','-1_2_1']},
  east:{side:'north',parts:['-2_2_-1','-1_2_-1']}
};
for(const [orientation,cfg] of Object.entries(v323RightSide)){
  const face=cfg.side;
  const n=(face==='east'||face==='west')?0:2;
  const positive=face==='east'||face==='south';
  const h=n===0?2:0;
  const depth=e=>positive?e.to[n]:16-e.from[n];
  const details=[];
  let sidePieces=0,realBricks=0,recessedMortar=0,rearCorners=0;
  const yLevels=new Set(),brickDepths=new Set();
  for(const part of cfg.parts){
    const model=json(resource+'assets/magneticraft2/models/multiblock/charcoal_retort_'+
      orientation+'/part_'+part+'.json');
    assert.equal(model.textures.brick,'magneticraft2:block/charcoal_retort/refractory',
      'V3.23 structural backing/rear exterior must retain refractory '+orientation+'/'+part);
    assert.equal(model.textures.sideBrick,'minecraft:block/bricks',
      'V3.23 raised side faces must match upper front bricks '+orientation+'/'+part);
    assert.equal(model.textures.mortar,'minecraft:block/stone',
      'V3.23 side mortar backing missing '+orientation+'/'+part);
    const visible=model.elements.filter(e=>e.faces[face]);
    sidePieces++;
    for(const e of visible){
      const mat=e.faces[face].texture;
      if(mat==='#sideBrick'&&depth(e)>15.4){
        realBricks++;
        brickDepths.add(+depth(e).toFixed(2));
        for(const y of [0.25,3.25,6.3,9.35,13.7])
          if(e.from[1]-0.00001<=y&&e.to[1]>=y+0.1)yLevels.add(y);
      }
      if(mat==='#mortar'&&Math.abs(depth(e)-14.2)<0.00001)
        recessedMortar++;
      if(mat==='#bronze'&&depth(e)===16)rearCorners++;
      // The V3.22 rejected side wall had full-length top and bottom
      // steel rails. Narrow rear-edge caps are allowed; long rails
      // spanning half a block or more are not.
      if(mat==='#metal'){
        const span=e.to[h]-e.from[h];
        const upper=e.from[1]<16&&e.to[1]>13;
        const lower=e.from[1]<3&&e.to[1]>0;
        assert(!(span>=8&&(upper||lower)),
          'V3.23 full-width dark side-wall stripe returned: '+
          orientation+'/'+part);
      }
    }
  }
  assert.equal(sidePieces,2,'V3.23 right wall must have two upper side cells');
  assert(realBricks>=25,
    'V3.23 side brick faces still look like a flat wallpaper panel '+orientation);
  assert(recessedMortar>=8,
    'V3.23 exposed recessed mortar joints are missing '+orientation);
  assert.deepEqual([...brickDepths].sort((a,b)=>a-b),[15.45,15.7],
    'V3.23 individual brick depth relief must alternate '+orientation);
  assert.deepEqual([...yLevels].sort((a,b)=>a-b),[0.25,3.25,6.3,9.35,13.7],
    'V3.23 all five side masonry courses required '+orientation);
  assert(rearCorners>0,
    'V3.23 rear copper corner anchor removed '+orientation);
}

 // V3.24 — side-facing continuation of the front-right upper block.
 for(const [orientation,id] of Object.entries(retortRight)){
   const m=json(resource+'assets/magneticraft2/models/multiblock/charcoal_retort_'+
     orientation+'/part_'+id+'.json');
   const side=v323RightSide[orientation].side;
   const axis=(side==='east'||side==='west')?0:2;
   const horizontal=axis===0?2:0;
   const positive=(side==='east'||side==='south');
   const depth=e=>positive?e.to[axis]:16-e.from[axis];
   const visible=m.elements.filter(e=>e.faces[side]);
   const bricks=visible.filter(e=>e.faces[side].texture==='#sideBrick'&&depth(e)>15.4);
   assert.equal(m.textures.sideBrick,'minecraft:block/bricks',
     'V3.24 front-right side texture missing '+orientation);
   assert(bricks.length>=15,'V3.24 missing raised side bricks '+orientation);
   assert.deepEqual([...new Set(bricks.map(e=>+depth(e).toFixed(2)))].sort((a,b)=>a-b),
     [15.45,15.7],'V3.24 incorrect side masonry relief '+orientation);
   for(const y of [0.25,3.25,6.3,9.35,13.7])
     assert(bricks.some(e=>Math.abs(e.from[1]-y)<0.00001),
       'V3.24 missing masonry course '+orientation+'/'+y);
   assert(visible.filter(e=>e.faces[side].texture==='#mortar'&&
     Math.abs(depth(e)-14.2)<0.00001).length>=8,
     'V3.24 missing recessed side mortar '+orientation);
   for(const e of visible)if(e.faces[side].texture==='#metal'){
     const span=e.to[horizontal]-e.from[horizontal];
     assert(!(span>=8&&((e.from[1]<3&&e.to[1]>0)||
       (e.from[1]<16&&e.to[1]>13))),
       'V3.24 full-width dark side band returned '+orientation);
   }
 }

 // V3.25: continuous, exposed cut-copper cap and upper lip on both walls.
 const capRotate=(x,z,dir)=>dir==='south'?[x,z]:dir==='west'?[-z,x]:
   dir==='north'?[-x,-z]:[z,-x];
 const capSide=(x,dir)=>({
   south:x===-1?'west':'east',west:x===-1?'north':'south',
   north:x===-1?'east':'west',east:x===-1?'south':'north'
 })[dir];
 for(const dir of dirs)for(const x of [-1,1])for(const z of [-2,-1,0]){
   const [rx,rz]=capRotate(x,z,dir);
   const m=json(resource+'assets/magneticraft2/models/multiblock/charcoal_retort_'+
     dir+'/part_'+rx+'_2_'+rz+'.json');
   const face=capSide(x,dir),normal=['west','east'].includes(face)?0:2;
   const h=normal===0?2:0;
   const depth=e=>['east','south'].includes(face)?e.to[normal]:16-e.from[normal];
   const copperAt=y=>m.elements.some(e=>e.faces[face]?.texture==='#bronze'&&
     Math.abs(depth(e)-16)<0.00001&&e.from[h]<8&&e.to[h]>8&&
     e.from[1]<y&&e.to[1]>y);
   assert.equal(m.textures.bronze,'minecraft:block/cut_copper',
     'V3.25 incorrect side cap material '+dir+'/'+x+'/'+z);
   assert(copperAt(12.75)&&copperAt(13.5),
     'V3.25 missing side copper cap/lip '+dir+'/'+x+'/'+z);
 }
// Neighboring roof non-bottom faces, Y1 outer side surfaces and the
// front-right outward face are compared to previous assets before commit.
const be=read('src/main/java/com/magneticraft2/common/blockentity/stage/bronze/CharcoalRetortBlockEntity.java');
assert(be.includes('insertItem(1, result, true)')&&be.includes('insertItem(2, tar, true)'));
assert(be.includes('RetortBurnRemaining')&&be.includes('RetortProgress'));
assert(be.includes('ForgeCapabilities.ITEM_HANDLER'));
console.log('Charcoal Retort source/data contracts passed');
