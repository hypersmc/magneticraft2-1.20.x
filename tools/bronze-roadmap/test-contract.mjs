#!/usr/bin/env node
// Current Bronze Age source/roadmap alignment, not a build or game test.
import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const doc=read('docs/bronze-age-production-plan.md');
const sub=read('docs/bronze-age-foundry-roadmap.md');
const kiln=read('docs/bronze-refractory-kiln-contract.md');
const registry=read('src/main/java/com/magneticraft2/common/registry/registers/BlockRegistry.java');
const recipe=JSON.parse(read('src/main/resources/data/magneticraft2/recipes/retorting_logs.json'));
const items=[
 'Stamp Mill','Rotary Ore Classifier','Ore Roasting Kiln','Water Settling Tank',
 'Charcoal Retort','Refractory Kiln','Mechanical Forge Hammer','Bronze Rolling Mill',
 'Mechanical Press','Precision Boring Machine','Mechanical Sawmill',
 'Foundry Sand Mixer','Industrial Air Blower','Mechanical Assembly Station',
 'Mechanical Ore Breaker','Bloomery Furnace'
];
for(const name of items){
  const count=doc.split('\n').filter(line=>line.startsWith('| '+name+' |')).length;
  assert.equal(count,1,'roadmap machine is missing or duplicated: '+name);
}
assert.equal(items.length,16);
assert.match(sub,/Full Bronze Age machinery reference/);
assert.match(doc,/Refractory Kiln \| \*\*Next agreed implementation target/);
assert.match(kiln,/5×4×5/);
const built=[
 ['BRONZE_STAMP_MILL','Stamp Mill'],
 ['ROTARY_ORE_CLASSIFIER','Rotary Ore Classifier'],
 ['WATER_SETTLING_TANK','Water Settling Tank'],
 ['CHARCOAL_RETORT','Charcoal Retort'],
 ['BRONZE_FOUNDRY','Bronze Foundry'],
 ['BRONZE_MECHANICAL_LATHE','Mechanical Lathe']
];
for(const [id,label]of built)
  assert.ok(registry.includes(id),label+' source status has no registered block');
assert.deepEqual(recipe.fluid_output,{fluid:'magneticraft2:wood_tar',amount:250});
assert.equal(recipe.output.item,'minecraft:charcoal');
assert.ok(!('byproduct' in recipe),'roadmap must not cite legacy Wood Tar item output');
console.log('Bronze roadmap: 16 machines and current source/refractory dependencies aligned');
