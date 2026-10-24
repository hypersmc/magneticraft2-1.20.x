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

const be=read('src/main/java/com/magneticraft2/common/blockentity/stage/bronze/CharcoalRetortBlockEntity.java');
assert(be.includes('insertItem(1, result, true)')&&be.includes('insertItem(2, tar, true)'));
assert(be.includes('RetortBurnRemaining')&&be.includes('RetortProgress'));
assert(be.includes('ForgeCapabilities.ITEM_HANDLER'));
console.log('Charcoal Retort source/data contracts passed');
