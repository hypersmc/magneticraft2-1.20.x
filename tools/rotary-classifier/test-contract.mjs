/** Regression contract for the 3×3×5 Rotary Ore Classifier. Node 18+, no npm deps. */
import assert from 'node:assert/strict';
import {existsSync,readFileSync,readdirSync} from 'node:fs';
import {dirname,join,resolve} from 'node:path';
import {fileURLToPath} from 'node:url';
import {execFileSync} from 'node:child_process';

const root=resolve(dirname(fileURLToPath(import.meta.url)),'../..');
const text=p=>readFileSync(join(root,p),'utf8');
const json=p=>JSON.parse(text(p));
const exists=p=>assert.ok(existsSync(join(root,p)),'Missing '+p);
const resources='src/main/resources/';
const art=resources+'assets/magneticraft2/';
const data=resources+'data/magneticraft2/';
const java='src/main/java/com/magneticraft2/';
const name='rotary_ore_classifier';
const d=json('tools/rotary-classifier/frame-design.json');
assert.equal(d.schema,5);
assert.deepEqual(d.dimensions,[3,3,5]);
assert.equal(d.layers.length,3);
assert.ok(d.layers.every(l=>l.length===5&&l.every(row=>row.length===3)));
assert.equal(d.drum.axis,'z');
assert.deepEqual(d.drum.segments,[-2,-1,0]);
assert.equal(d.drum.centerY,24);
assert.ok(d.drum.facetOffset>=10.5,'Screen radius must not regress to the tiny drum');
assert.ok(d.drum.inletInset>=2&&d.drum.inletInset<4,'Rear mesh needs clearance from fixed support braces');
const posts=d.boxes.filter(b=>b.name.endsWith('_timber_post'));
assert.equal(posts.length,4,'The accepted compact design has only four primary timber posts');
assert.equal(d.supportContract.frontOpenEnd,true,'The front discharge must remain open');
assert.equal(d.supportContract.driveBearingOwnedByModule,true,
 'The real Gear V2 input already owns the rear copper bearing');
for(const oldBearing of ['rear_bearing_w','rear_bearing_e','rear_bearing_top','rear_bearing_bottom','rear_bearing_trim','rear_bearing_mount_w','rear_bearing_mount_e'])
 assert.ok(!d.boxes.some(b=>b.name===oldBearing),
  'Do not reintroduce duplicate bearing/crossbar over the screen: '+oldBearing);
assert.ok(d.boxes.every(b=>!b.name.includes('roof')),'No giant framing canopy');
assert.ok(d.boxes.some(b=>b.name==='tray_underpan'),'No fines collection pan');
assert.ok(!d.boxes.some(b=>/^(ore_hopper_|hopper_)/.test(b.name)), 'Fake elevated hopper obscures real input');
assert.ok(d.boxes.some(b=>b.name==='feed_coupler_outer'), 'Actual input must connect to the drum via a small coupling');
assert.ok(!d.boxes.some(b=>/^(fine_exit_|fine_tray_wedge$)/.test(b.name)), 'Remove misleading fake fines holder');
assert.ok(!d.boxes.some(b=>/^(coarse_exit_|coarse_rear_scoop$)/.test(b.name)), 'Remove misleading fake coarse holder');

const indexes=new Map(d.boxes.map((b,i)=>[b.name,i]));
assert.equal(indexes.size,d.boxes.length,'Duplicate geometry identifiers');
const joined=(a,b)=>a.from.every((v,i)=>v<=b.to[i]+0.001&&a.to[i]+0.001>=b.from[i]);
const roots=d.supportContract.roots.map(name=>indexes.get(name));
assert.equal(roots.length,4);assert.ok(roots.every(i=>i!==undefined),'Foundation root missing');
const visited=new Set(roots),queue=[...roots];
for(let k=0;k<queue.length;k++)for(let i=0;i<d.boxes.length;i++){
 if(!visited.has(i)&&joined(d.boxes[queue[k]],d.boxes[i])){visited.add(i);queue.push(i);}
}
assert.equal(visited.size,d.boxes.length,'Floating geometry: '+
 d.boxes.filter((b,i)=>!visited.has(i)).map(b=>b.name).join(','));
for(const [part,mount] of [
 ['rear_guide_roller_w','bronze_roller_w_rear'],
 ['rear_guide_roller_e','bronze_roller_e_rear'],
 ['rear_feed_support_w','w_rear_timber_post'],
 ['rear_feed_support_w','feed_coupler_outer'],
 ['front_guide_roller_w','bronze_roller_w_front'],
 ['front_guide_roller_e','bronze_roller_e_front'],
 ['fine_outlet_guide_plate','tray_west_lip'],
 ['coarse_outlet_guide_plate','tray_east_lip']
]){
 assert.ok(indexes.has(part)&&indexes.has(mount),part+' missing');
 assert.ok(joined(d.boxes[indexes.get(part)],d.boxes[indexes.get(mount)]),
  part+' not anchored to '+mount);
}
// All roller support sites must meet a bronze rim and remain grounded.
for(const site of [
 {end:'rear',z0:-32+d.drum.inletInset+0.1,z1:-32+d.drum.inletInset+1.3},
 {end:'front',z0:14.6,z1:15.8}
]){
 const left=d.boxes[indexes.get(site.end+'_guide_roller_w')];
 const right=d.boxes[indexes.get(site.end+'_guide_roller_e')];
 assert.ok(left&&right,site.end+' missing support rollers');
 assert.ok(Math.abs(left.to[0]-(8-d.drum.facetOffset-d.drum.skinThickness/2))<0.1);
 assert.ok(Math.abs(right.from[0]-(8+d.drum.facetOffset+d.drum.skinThickness/2))<0.1);
 for(const roller of [left,right]){
  assert.ok(roller.from[2]<=site.z0+0.15&&roller.to[2]>=site.z1-0.15,
   site.end+' roller misses the outer rotating rim');
 }
}
// The central axial connection is already the formed Mechanical Input
// module. Fixed radial braces must never intrude across that opening.
const driveMouth={from:[0,16,-32],to:[16,32,-32+d.drum.inletInset]};
const overlap=(a,b)=>a.from.every((v,i)=>v<b.to[i]-1e-4&&a.to[i]>b.from[i]+1e-4);
for(const box of d.boxes)assert.ok(!overlap(box,driveMouth),
 'Stationary '+box.name+' intrudes across Gear V2 central shaft opening');
for(const name of d.supportContract.frontOutputGuides){
 const guide=d.boxes[indexes.get(name)];
 assert.ok(guide&&guide.to[2]<=16&&guide.to[2]>=15.5,
  'Internal guide must terminate at, not occupy, the real output block: '+name);
}


const cells=[];
for(let y=0;y<3;y++)for(let z=-3;z<=1;z++)for(let x=-1;x<=1;x++)cells.push({x,y,z,key:d.layers[y][z+3][x+1]});
const moduleIds={C:'magneticraft2:rotary_ore_classifier',
 I:'magneticraft2:mechanical_input_module',
 U:'magneticraft2:multiblock_item_input',
 O:'magneticraft2:multiblock_item_primary_output',
 Y:'magneticraft2:multiblock_item_byproduct_output'};
for(const [k,id] of Object.entries(moduleIds)){
 assert.equal(cells.filter(c=>c.key===k).length,1,'Exactly one '+k);
 assert.equal(d.blocks[k],id);
}
const where=k=>{const c=cells.find(c=>c.key===k);return[c.x,c.y,c.z]};
assert.deepEqual(where('C'),[0,0,0]);
assert.deepEqual(where('I'),[0,1,-3]);
assert.deepEqual(where('U'),[-1,1,-3]);
assert.deepEqual(where('O'),[-1,0,1]);
assert.deepEqual(where('Y'),[1,0,1]);
const contains=(b,c)=>b.to[0]>c.x*16+1e-4&&b.from[0]<(c.x+1)*16-1e-4
 &&b.to[1]>c.y*16+1e-4&&b.from[1]<(c.y+1)*16-1e-4
 &&b.to[2]>c.z*16+1e-4&&b.from[2]<(c.z+1)*16-1e-4;
for(const b of d.boxes.concat(d.collisionExtras)){
 assert.equal(b.from.length,3);assert.equal(b.to.length,3);
 for(let a=0;a<3;a++)assert.ok(b.to[a]>b.from[a],b.name+' has inverted dimensions');
 assert.ok(b.from[0]>=-16&&b.to[0]<=32&&b.from[1]>=0&&b.to[1]<=48&&b.from[2]>=-48&&b.to[2]<=32,b.name+' extends outside 3x3x5 physical footprint');
 for(const c of cells.filter(c=>'UIOY'.includes(c.key)))assert.ok(!contains(b,c),b.name+' overlaps functional module '+c.key+' at '+[c.x,c.y,c.z]);
 for(const c of cells.filter(c=>'UOY'.includes(c.key)))assert.ok(!contains(b,{...c,y:c.y+1}),
    b.name+' blocks the space directly above actual interactive port '+c.key);
}
const anchors=new Set(d.anchors.map(a=>a.join(',')));
assert.equal(anchors.size,d.anchors.length);
for(const a of d.anchors){
 const c=cells.find(c=>[c.x,c.y,c.z].join(',')===a.join(','));
 assert.equal(c?.key,'A','V2 render anchor must be required AIR');
}
const calculated=new Set();
for(const c of cells.filter(c=>c.key==='A'))if(d.boxes.concat(d.collisionExtras).some(b=>contains(b,c)))calculated.add([c.x,c.y,c.z].join(','));
assert.deepEqual([...anchors].sort(),[...calculated].sort(),'AIR proxies must match only geometry-covered air cells');
const rot=(x,z,f)=>f==='south'?[x,z]:f==='west'?[-z,x]:f==='north'?[-x,-z]:[z,-x];
const faceNames=['south','west','north','east'];
const fixedHosts=cells.filter(c=>c.key==='A'?anchors.has([c.x,c.y,c.z].join(',')):!'UIOY'.includes(c.key));
assert.equal(fixedHosts.length,28);
for(const face of faceNames){
 const bp=json(data+'multiblocks/'+name+'_'+face+'.json');
 assert.equal(bp.name,name+'_'+face);
 const sideways=face==='west'||face==='east';
 assert.deepEqual(bp.structure.dimensions,sideways?[5,3,3]:[3,3,5]);
 const transformed=cells.map(c=>{const[x,z]=rot(c.x,c.z,face);return{...c,x,z}});
 const minX=Math.min(...transformed.map(c=>c.x)),minZ=Math.min(...transformed.map(c=>c.z));
 for(const c of transformed){
  assert.equal(bp.structure.layout['layer'+(c.y+1)][c.z-minZ][c.x-minX],c.key,'Bad '+face+' rotation '+[c.x,c.y,c.z]);
 }
 for(const [k,id] of Object.entries(moduleIds))assert.equal(bp.structure.blocks[k],id);
 assert.equal(bp.settings.voxel_profile,'magneticraft2:rotary_ore_classifier');
 const expected=d.anchors.map(([x,y,z])=>{const [rx,rz]=rot(x,z,face);return [rx,y,rz].join(',')}).sort();
 assert.deepEqual(bp.settings.render_air_anchors.map(v=>v.join(',')).sort(),expected,'Incorrect AIR filler anchors for '+face);
 const partDir=art+'models/multiblock/'+name+'_'+face+'/';
 const actualParts=readdirSync(join(root,partDir)).filter(p=>p.endsWith('.json'));
 assert.equal(actualParts.length,fixedHosts.length,'No unused scaffold models permitted for '+face);
 for(const c of fixedHosts){
  const [x,z]=rot(c.x,c.z,face);
  const filename=partDir+'part_'+x+'_'+c.y+'_'+z+'.json';
  const model=json(filename);
  assert.equal(model.parent,'minecraft:block/block');
  assert.ok(Array.isArray(model.elements),'No indirect template references');
  assert.equal(model.textures.bronze,'magneticraft2:block/material/bronze_cast');
  assert.equal(model.textures.timber,'magneticraft2:block/material/machine_timber');
  assert.ok(model.elements.length<128,'Fixed per-cell model too expensive');
  for(const el of model.elements){
   for(let axis=0;axis<3;axis++){
    assert.ok(el.from[axis]>=-1e-4&&el.to[axis]<=16.0001&&el.to[axis]>el.from[axis],
     'Invalid block-local element '+filename);
   }
   for(const [side,view] of Object.entries(el.faces)){
    const v=view.uv;
    assert.ok(v.length===4&&v.every(Number.isFinite));
    const spanX=side==='north'||side==='south'?el.to[0]-el.from[0]:side==='east'||side==='west'?el.to[2]-el.from[2]:el.to[0]-el.from[0];
    const spanY=side==='up'||side==='down'?el.to[2]-el.from[2]:el.to[1]-el.from[1];
    assert.ok(Math.abs(v[2]-v[0]-spanX)<0.01&&Math.abs(v[3]-v[1]-spanY)<0.01,
     'Texture must not be stretched or compressed: '+filename+' '+side);
   }
  }
 }
}
const vox=json(data+'multiblock_voxels/'+name+'.json');
assert.equal(vox.format,1);
assert.equal(vox.unit,'model_pixels');
assert.equal(vox.collision.length,d.boxes.filter(b=>b.collision!==false).length+d.collisionExtras.length);
assert.ok(vox.collision.length<512);
const blocks=json(art+'blockstates/'+name+'.json');
assert.equal(Object.keys(blocks.variants).length,8);

const pixelSource=json('tools/texture-art/texture-kit-v1.json');
for(const material of ['bronze_cast','bronze_machined','machine_timber','machine_timber_end','dark_work_iron']){
 const p=art+'textures/block/material/'+material+'.png';exists(p);
 const png=readFileSync(join(root,p));assert.equal(png.readUInt32BE(16),16);assert.equal(png.readUInt32BE(20),16);
}
for(const wear of ['','_worn']){
 const tile=pixelSource.textures['screen_mesh_bronze'+wear];
 assert.ok(tile.pixels.some(r=>r.includes('.')),'Screen holes must use true transparency');
 const png=readFileSync(join(root,art+'textures/block/component/screen_mesh_bronze'+wear+'.png'));
 assert.equal(png.readUInt32BE(16),16);assert.equal(png.readUInt32BE(20),16);
}
for(const section of ['rear','middle','front'])for(const wear of ['','_worn']){
 const p=art+'models/multiblock/'+name+'_drum_'+section+wear+'.json';
 const model=json(p);
 assert.equal(model.textures.mesh,'magneticraft2:block/component/screen_mesh_bronze'+wear);
 const mesh=model.elements.filter(e=>Object.values(e.faces).some(f=>f.texture==='#mesh'));
 assert.equal(mesh.length,8,'Use eight individual screen facets');
 if(section==='rear')assert.ok(mesh.every(e=>e.from[2]>=d.drum.inletInset-0.01),
  'Rear drum mesh must not extend through fixed bearing mounts');
 assert.ok(mesh.some(e=>e.from[0]<0)||mesh.some(e=>e.to[0]>16),'Screen must visibly extend past the original tiny drum');
 assert.ok(mesh.some(e=>e.to[1]>18),'Drum silhouette must reach the upper visual level');
 assert.ok(model.elements.length<49,'Do not rebuild an expensive high-poly drum');
 const hasCentralAxle=model.elements.some(e=>Object.values(e.faces).some(f=>f.texture==='#iron'));
 if(section==='front')assert.equal(hasCentralAxle,false,'Open front must not show an unsupported spindle');
 else assert.equal(hasCentralAxle,true,'Rear and middle section retain their power shaft');
 for(const e of model.elements){
  for(let i=0;i<3;i++)assert.ok(e.from[i]>=-16&&e.to[i]<=32&&e.to[i]>e.from[i],'Invalid vanilla model element '+p);
  // Minecraft rejects rotation.angle:null even though it is valid JSON.
  // Keep this check explicit: older source tests did not run before the bad model shipped.
  if (Object.hasOwn(e,'rotation')) {
   assert.ok(e.rotation && Number.isFinite(e.rotation.angle),
    'rotation.angle must be a finite number, never null: '+p);
   assert.ok([-45,45].includes(e.rotation.angle) && e.rotation.axis==='z',
    'Only explicitly rotated screen diagonals have an angle: '+p);
  }
  for(const f of Object.values(e.faces))assert.ok(f.uv[2]-f.uv[0]<=16&&f.uv[3]-f.uv[1]<=16);
 }
}
for(const old of [art+'models/multiblock/'+name+'_drum.json',art+'models/multiblock/'+name+'_drum_worn.json'])assert.ok(!existsSync(join(root,old)),'Obsolete tiny model still present');

const renderer=text(java+'client/render/blocks/stage/bronze/RotaryOreClassifierRenderer.java');
for(const namePart of ['drum_rear','drum_middle','drum_front','WORN_DRUM','RenderType.cutout()','getDrumRotationDegrees','drawScreeningOre','getVisualProcessProgress'])assert.ok(renderer.includes(namePart),'Renderer missing '+namePart);
// Check the REAL port surface heights rather than former fake hoppers.
const inputPortModel=json(art+'models/block/multiblock_item_input.json');
const outputPortModel=json(art+'models/block/multiblock_item_output.json');
assert.equal(Math.max(...inputPortModel.elements.map(e=>e.to[1])),15);
assert.equal(Math.max(...outputPortModel.elements.map(e=>e.to[1])),10);
assert.ok(renderer.includes('INPUT_X = -0.5D;')&&
          renderer.includes('INPUT_Y = 1.97D;')&&
          renderer.includes('INPUT_Z = -2.5D;'),
 'Real elevated input model is topped at 1 + 15/16 blocks');
assert.ok(renderer.includes('FINE_X = -0.5D;')&&
          renderer.includes('COARSE_X = 1.5D;')&&
          renderer.includes('OUTPUT_Y = 0.665D;')&&
          renderer.includes('OUTPUT_Z = 1.5D;'),
 'Two output trays really top out at 10/16 blocks');
assert.ok(renderer.includes('drawPortStack('),'Only real port surfaces show stored items');
assert.ok(renderer.includes('classifier.hasVisibleWorkpiece()'),
 'A stalled batch must remain visible in the drum');
assert.ok(renderer.includes('input.getCount() > classifier.getVisualBatchInputCount()'),
 'Only remaining queued ore should stay on the input while one batch runs');
assert.ok(renderer.includes('MAX_DRUM_FRAGMENTS = 2;'),
 'Keep the number of BER item draws bounded for FPS');
assert.ok(renderer.includes('classifier.isProcessing() ? angle : 0F'),
 'Stalled material must not tumble freely');
assert.ok(renderer.includes('getVisualProcessProgress(partialTicks)'),
 'Workpiece flow must use server-synced batch progress');
for(const fragment of ['progress < 0.20F','progress >= 0.36F','progress >= 0.80F',
                       '0.85D * smoothstep(fall)','drawScreeningOre',
                       'ItemDisplayContext.GROUND']){
 assert.ok(renderer.includes(fragment),'Missing meaningful screen/process phase: '+fragment);
}
assert.ok(!renderer.includes('getProcessingPrimaryPreview()')&&
          !renderer.includes('getProcessingByproductPreview()'),
 'Do not show uncommitted recipe outputs on item ports');
assert.ok(!renderer.includes('addParticle('),'Particles cannot be emitted every render frame');
// The three crushed ores have a built-in half-scale GROUND transform.
// Very small secondary render multipliers used to make them microscopic.
// All ore must now be big enough to SEE while staying inside the 22px drum.
for(const ore of ['chalcocite','cassiterite','quartz']){
 const itemModel=json(art+'models/item/crushed_'+ore+'.json');
 assert.deepEqual(itemModel.display.ground.scale,[0.5,0.5,0.5],
  'Update this scale contract if the ore item model transform changes');
}
const visualScale=name=>{
 const rx=new RegExp('\\b'+name+'\\s*=\\s*([0-9]+(?:\\.[0-9]+)?)F;');
 const m=renderer.match(rx);
 assert.ok(m,'Missing classifier visual scale constant '+name);
 return Number(m[1]);
};
const drumSize=visualScale('DRUM_ITEM_SCALE');
const fineSize=visualScale('FINE_ITEM_SCALE');
const feedSize=visualScale('FEED_ITEM_SCALE');
const coarseSize=visualScale('COARSE_ITEM_SCALE');
const inputSize=visualScale('INPUT_ITEM_SCALE');
const outputSize=visualScale('OUTPUT_ITEM_SCALE');
assert.ok(drumSize>=0.75&&drumSize<=0.9,'Main pieces should be visually substantial without clipping');
assert.ok(fineSize>=0.3&&fineSize<drumSize*0.55,'Screened fragments should be smaller, not microscopic');
assert.ok(feedSize>=0.65&&coarseSize>=0.65,'Material must not shrink during intake/discharge');
assert.ok(inputSize>=0.6&&outputSize>=0.6,'Actual physical input/output displays should be legible');
const restY=Number(renderer.match(/SCREEN_ORE_REST_Y\s*=\s*([0-9.]+)D;/)?.[1]);
assert.ok(Number.isFinite(restY),'Missing roller-rest height constant');
// Drum centre: BER translates model to Y=1.5, local drum pivot at 8/16.
// Its inner radius is specified by frame-design. Validate the actual
// coarse and main chunk bounds with the crushed item model's 10px width.
const drumAxisY=1.5+0.5;
const innerRadius=(d.drum.facetOffset-d.drum.skinThickness/2)/16;
const groundScale=0.5;
const oreHalfWidth=(10/16)*groundScale/2;
assert.ok(restY>=1.4&&restY<=1.65,
 'Pieces must ride INSIDE lower screen, not below its underside');
const mainReach=Math.hypot(0.15,drumAxisY-restY)+drumSize*oreHalfWidth;
const coarseReach=Math.hypot(0.24,drumAxisY-(restY+0.02))+coarseSize*oreHalfWidth;
assert.ok(mainReach<innerRadius,'Large tumbling chunk now crosses the mesh');
assert.ok(coarseReach<innerRadius,'Larger coarse-return chunk crosses the outlet rim');
assert.ok(renderer.includes('DRUM_ITEM_SCALE - i * 0.055F')&&
          renderer.includes('FINE_ITEM_SCALE')&&
          renderer.includes('COARSE_ITEM_SCALE')&&
          renderer.includes('SCREEN_ORE_REST_Y + 0.045D'),
 'Use named visual sizes and the corrected lower-inside rolling path');
assert.ok(renderer.includes('x - 0.14D')&&renderer.includes('x + 0.14D'),
 'Multiple finished items need distinct positions on their real output trays');

const client=text(java+'client/Clientsetup.java');
assert.ok(client.includes('ROTARY_ORE_CLASSIFIER_BE')&&client.includes('rotary_ore_classifier_drum_'),'Six registered BER models required');
const controller=text(java+'common/blockentity/stage/bronze/RotaryOreClassifierBlockEntity.java');
for(const piece of ['getMechanicalInput','classifierScreen.completeBatch()','removeMechanicalLoad','pendingByproduct','MultiblockRenderParts.assign','getDrumRotationDegrees','hasVisibleWorkpiece','clientTick','worldParticleXZ','getVisualBatchInputCount'])
 assert.ok(controller.includes(piece),'Regressed processor: '+piece);
const block=text(java+'common/block/stage/bronze/RotaryOreClassifierBlock.java');
assert.ok(block.includes('RotaryOreClassifierBlockEntity::clientTick')&&
          block.includes('RotaryOreClassifierBlockEntity::serverTick'),
 'Register separate client particle/sound and server processing tickers');
assert.ok(controller.includes('!classifier.processing')&&
          controller.includes('!classifier.formed')&&
          controller.includes('getInputStack().isEmpty()'),
 'Client effects must stop when idle, unformed or out of feed');
assert.ok(controller.includes('getNearestPlayer(')&&
          controller.includes('18.0D, false'),
 'Cull particle/sound work beyond nearby players');
assert.ok(controller.includes('% 10L == 0L')&&
          controller.includes('% 40L == 0L'),
 'Particles/sounds must be time-budgeted and spread across machine positions');
assert.ok(controller.includes('ParticleTypes.FALLING_DUST')&&
          controller.includes('DustParticleOptions')&&
          controller.includes('SoundEvents.GRAVEL_STEP'),
 'Physical screening should shed low-rate grit/dust and soft gravel rattles');
const tickerBody=controller.slice(controller.indexOf('public static <E extends BlockEntity> void clientTick'),
                                  controller.indexOf('public static <E extends BlockEntity> void serverTick'));
assert.ok(tickerBody.includes('!classifier.processing')&&tickerBody.includes('progress < 0.18F'),
 'No particle emission for power loss/output blockage');
assert.ok(!tickerBody.includes('extractItem(')&&!tickerBody.includes('insertItem('),
 'Visuals must never mutate the real inventory');
for(const dir of ['case NORTH','case WEST','case EAST','default ->'])assert.ok(
 controller.includes(dir),'Client particle spawn must rotate with four blueprints: '+dir);
// The client effect transform must match the BER's SOUTH canonical rotated points.
const maps={
 south:([x,z])=>[x,z],
 north:([x,z])=>[1-x,1-z],
 west:([x,z])=>[1-z,x],
 east:([x,z])=>[z,1-x]
};
const inputPoint=[-0.5,-2.5];
assert.deepEqual(maps.south(inputPoint),[-0.5,-2.5]);
assert.deepEqual(maps.north(inputPoint),[1.5,3.5]);
assert.deepEqual(maps.west(inputPoint),[3.5,-0.5]);
assert.deepEqual(maps.east(inputPoint),[-2.5,1.5]);
assert.ok(text(java+'common/blockentity/stage/copper/MultiblockItemPortBlockEntity.java').includes('RotaryOreClassifierBlockEntity'));
assert.ok(text(java+'compatibility/JEI/MagneticraftJeiPlugin.java').includes('ROTARY_ORE_CLASSIFIER'));
for(const ore of ['chalcocite','cassiterite','quartz']){
 const proc=json(data+'recipes/rotary_classifying_'+ore+'.json');
 assert.equal(proc.machine,'magneticraft2:rotary_ore_classifier');
 assert.equal(proc.input.item,'magneticraft2:crushed_'+ore);
 assert.equal(proc.input_count,4);
 assert.equal(proc.output.item,'magneticraft2:classified_'+ore);
 assert.equal(proc.output.count,3);
 assert.equal(proc.byproduct.item,proc.input.item);
 assert.equal(proc.byproduct.count,1);
 assert.equal(proc.byproduct_chance,1);
 assert.equal(proc.input_count,proc.output.count+proc.byproduct.count);
 const washer=json(data+'recipes/mechanical_washing_classified_'+ore+'.json');
 assert.equal(washer.input.item,proc.output.item);
 assert.equal(washer.input_count,3);
 assert.equal(washer.output.item,'magneticraft2:washed_'+ore);
 assert.equal(washer.output.count,3);
 assert.equal(washer.fluid_input.amount,600);
 assert.equal(washer.fluid_output.amount,600);
 assert.ok(json(data+'recipes/mechanical_washing_'+ore+'.json').input.item==='magneticraft2:crushed_'+ore);
 assert.ok(json(data+'recipes/mechanical_sifting_'+ore+'.json').input.item==='magneticraft2:washed_'+ore);
}
const book=json(art+'patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json');
const page=book.pages.find(p=>p.name==='Rotary Ore Classifier');
assert.ok(page?.enable_visualize);
const pattern=[];
for(let y=2;y>=0;y--){
 const slice=[];for(let x=0;x<3;x++){
  let row='';for(let z=0;z<5;z++){const key=d.layers[y][z][x];row+=key==='A'?'_':key==='C'?'0':key;}
  slice.push(row);
 }
 pattern.push(slice);
}
assert.deepEqual(page.multiblock.pattern,pattern,'Patchouli must form EXACTLY the redesigned structure');
assert.ok(page.text.includes('3×3×5'));
for(const k of ['pickaxe','stone']){const path=k==='pickaxe'?resources+'data/minecraft/tags/blocks/mineable/pickaxe.json':resources+'data/minecraft/tags/blocks/needs_stone_tool.json';assert.ok(json(path).values.includes('magneticraft2:rotary_ore_classifier'));}
execFileSync(process.execPath,['tools/rotary-classifier/build-static.mjs','--check'],{cwd:root,stdio:'inherit'});
console.log('Rotary Classifier V5 animation contract passed: physical input/output surfaces, material transfer, screening/fines/coarse, paused batches, low-rate client effects, grounded frame and four facings.');
