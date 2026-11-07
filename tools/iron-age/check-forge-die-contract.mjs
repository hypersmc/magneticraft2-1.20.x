#!/usr/bin/env node
// Static source/resource checks. NOT a ForgeGradle compile or in-game test.
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const load = p => fs.readFileSync(path.join(root,p),'utf8');
const json = p => JSON.parse(load(p));
const java = p => load('src/main/java/com/magneticraft2/' + p);
const recipe = json('src/main/resources/data/magneticraft2/recipes/wrought_iron_forge_die.json');
const model = json('src/main/resources/assets/magneticraft2/models/item/wrought_iron_forge_die.json');
const lang = json('src/main/resources/assets/magneticraft2/lang/en_us.json');
const page = json('src/main/resources/assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/ironage.json');

assert.equal(recipe.type,'minecraft:crafting_shaped');
assert.deepEqual(recipe.pattern,['III',' I ',' P ']);
assert.equal(recipe.key.I.item,'magneticraft2:wrought_iron_ingot');
assert.equal(recipe.key.P.item,'minecraft:polished_blackstone');
assert.equal(recipe.result.item,'magneticraft2:wrought_iron_forge_die');
assert.equal(recipe.result.count,1);
assert.equal(recipe.pattern.join('').split('I').length-1,4);

assert.equal(lang['item.magneticraft2.wrought_iron_forge_die'],'Wrought Iron Forging Die');
assert.equal(page.icon,'magneticraft2:wrought_iron_ingot');
assert.ok(page.pages.some(p=>p.recipe==='magneticraft2:wrought_iron_forge_die'));

const text = java('common/systems/maintenance/ReplaceableMachinePart.java');
assert.match(text,/public ReplaceableMachinePart\(Supplier<\? extends Item> replacement,\s+int maximumCycles\)/);
assert.match(text,/this\(replacement, maximumCycles, new Alternative\[0\]\)/);
assert.match(text,/getMaximumCycles\(\)/);
assert.match(text,/installedVariant = 0/);
assert.match(text,/if \(installedVariant != 0\)/);
assert.match(text,/tag\.putInt\(REMAINING, remainingCycles\)/);
assert.match(text,/stored\.equals\(ForgeRegistries\.ITEMS\.getKey/);
assert.match(text,/Math\.max\(0, Math\.min\(getMaximumCycles\(\), tag\.getInt\(REMAINING\)\)\)/);
assert.match(text,/if \(requested == installedVariant && remainingCycles == getMaximumCycles\(\)\)/);
assert.match(text,/held\.shrink\(1\)/);
assert.match(text,/remainingCycles--/);
assert.match(text,/Math\.max\(1, maximumCycles\)/);

const hammer = java('common/blockentity/stage/bronze/BronzeForgeHammerBlockEntity.java');
assert.match(hammer,/ITEM_BRONZE_FORGE_DIE::get, 384/);
assert.match(hammer,/ITEM_WROUGHT_IRON_FORGE_DIE::get, 768/);
assert.match(hammer,/forgingDie\.completeBatch\(\)/);
assert.match(hammer,/forgingDie\.handleInteraction\(player, hand, this::sync\)/);
assert.match(hammer,/forgingDie\.save\(\)/);
assert.match(hammer,/forgingDie\.load\(tag\.getCompound\("ForgeDie"\)\)/);

const registry = java('common/registry/registers/ItemRegistry.java');
assert.match(registry,/ITEM_WROUGHT_IRON_FORGE_DIE/);
assert.match(registry,/ITEMS\.register\("wrought_iron_forge_die"/);
assert.match(java('common/registry/registers/CreativeTabRegistry.java'),
  /entries\.accept\(ItemRegistry\.ITEM_WROUGHT_IRON_FORGE_DIE\.get\(\)\)/);

assert.equal(model.parent,'minecraft:block/block');
assert.ok(model.elements.length>=2);
const textureRefs = new Set(Object.keys(model.textures).map(s=>'#'+s));
for(const e of model.elements){
  assert.equal(e.from.length,3); assert.equal(e.to.length,3);
  for(let axis=0;axis<3;axis++) {
    assert.ok(e.from[axis]>=0 && e.from[axis]<e.to[axis] && e.to[axis]<=16);
  }
  for(const [dir,face] of Object.entries(e.faces)){
    assert.ok(textureRefs.has(face.texture),dir+' unknown material');
    const expected=(dir==='up'||dir==='down')
      ?[e.to[0]-e.from[0],e.to[2]-e.from[2]]
      :(dir==='east'||dir==='west')
        ?[e.to[2]-e.from[2],e.to[1]-e.from[1]]
        :[e.to[0]-e.from[0],e.to[1]-e.from[1]];
    const actual=[face.uv[2]-face.uv[0],face.uv[3]-face.uv[1]];
    assert.deepEqual(actual,expected,dir+' stretched/compressed UV');
  }
}
console.log('PASS: Iron Forge Die recipe, registries, item UVs, book and legacy maintenance contract');
