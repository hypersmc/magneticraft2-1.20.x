#!/usr/bin/env node
/** K2 source/resource contract checks. This is NOT a Forge or gameplay test. */
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const json=p=>JSON.parse(read(p));
const src='src/main/';
const roasted='magneticraft2:roasted_copper_concentrate';
const input='magneticraft2:copper_concentrate';
const r=json(src+'resources/data/magneticraft2/recipes/roasting_copper_concentrate.json');
assert.equal(r.type,'magneticraft2:multiblock_processing');
assert.equal(r.machine,'magneticraft2:ore_roasting_kiln');
assert.deepEqual(r.input,{item:input});
assert.equal(r.input_count,4);
assert.deepEqual(r.output,{item:roasted,count:4});
assert.equal(r.process_time,600);
assert.ok(!r.byproduct&&!r.fluid_input&&!r.fluid_output&&!r.mechanical);

const roastSmelt=json(src+'resources/data/magneticraft2/recipes/primitive_furnace_roasted_copper_concentrate.json');
const original=json(src+'resources/data/magneticraft2/recipes/primitive_furnace_copper_concentrate.json');
assert.equal(roastSmelt.type,'magneticraft2:primitive_furnace_multiblock');
assert.deepEqual(roastSmelt.input1,{item:roasted});
assert.deepEqual(roastSmelt.input2,{item:'minecraft:coal'});
assert.deepEqual(roastSmelt.output1,{item:'minecraft:copper_ingot',count:4});
assert.deepEqual(roastSmelt.output2,{item:'magneticraft2:slagitem',count:1});
assert.deepEqual(original.input1,{item:input});
assert.deepEqual(original.output1,{item:'minecraft:copper_ingot',count:3},
 'Old Copper Age processing must not change');

const base=read(src+'java/com/magneticraft2/common/blockentity/stage/bronze/OreRoastingKilnBlockEntity.java');
const block=read(src+'java/com/magneticraft2/common/block/stage/bronze/OreRoastingKilnBlock.java');
const itemReg=read(src+'java/com/magneticraft2/common/registry/registers/ItemRegistry.java');
const creative=read(src+'java/com/magneticraft2/common/registry/registers/CreativeTabRegistry.java');
const config=read(src+'java/com/magneticraft2/common/utils/Magneticraft2ConfigCommon.java');
const jei=read(src+'java/com/magneticraft2/compatibility/JEI/MagneticraftJeiPlugin.java');
const jeiModel=read(src+'java/com/magneticraft2/compatibility/JEI/MultiblockJeiRecipe.java');
assert.match(block,/OreRoastingKilnBlockEntity::serverTick/);
assert.match(block,/level\.isClientSide \? null/);
assert.match(base,/if \(level == null \|\| !formed\) return;/);
assert.match(base,/MultiblockRecipeHandler\.findRecipe\(level, MACHINE/);
assert.match(base,/MultiblockRecipeHandler\.acceptsInput\(level, MACHINE/);
assert.match(base,/recipe\.getInputCount\(\)/);
assert.match(base,/itemHandler\.insertItem\(1, product, true\)\.isEmpty\(\)/);
assert.match(base,/itemHandler\.extractItem\(0, recipe\.getInputCount\(\), false\)/);
assert.match(base,/itemHandler\.insertItem\(1, product, false\)/);
assert.match(base,/ForgeHooks\.getBurnTime\(stack, RecipeType\.SMELTING\)/);
assert.match(base,/getCraftingRemainingItem\(\)/);
assert.match(base,/burnRemaining > 0/);
assert.match(base,/heatHandler\.setHeat\(heat\)/);
assert.match(base,/recipe\.getByproduct\(\)\.isEmpty\(\)/);
assert.match(base,/recipe\.getFluidInput\(\)\.isEmpty\(\)/);
assert.match(base,/recipe\.getFluidOutput\(\)\.isEmpty\(\)/);
assert.match(base,/recipe\.getMinSpeed\(\) == 0F/);
assert.match(base,/recipe\.getTorque\(\) == 0F/);
for(const n of ['RoasterFuel','RoasterBurnTotal','RoasterBurnRemaining','RoasterProgress',
                'RoasterRecipe','RoasterActiveInput']) assert.ok(base.includes(n),'Missing persisted '+n);
assert.match(base,/if \(cap == ForgeCapabilities\.ITEM_HANDLER\) return LazyOptional\.empty\(\)/);
assert.match(base,/linkFormedController\(worldPosition\)/);
assert.match(base,/MultiblockRenderParts\.assign/);
for(const name of ['RoasterHeatCapacity','RoasterHeatPerFuelTick','RoasterPassiveHeatLoss',
                    'RoasterRoastingThreshold','RoasterHeatPerProcessingTick']){
 assert.ok(config.includes(name),'Missing config '+name);
 assert.ok(base.includes(name),'Missing HEAT usage '+name);
}
assert.match(itemReg,/ITEM_ROASTED_COPPER_CONCENTRATE/);
assert.match(creative,/ITEM_ROASTED_COPPER_CONCENTRATE/);
assert.match(jeiModel,/ORE_ROASTING_KILN/);
assert.match(jei,/registerCatalyst\(registration, MultiblockJeiRecipe\.ORE_ROASTING_KILN\)/);
const model=json(src+'resources/assets/magneticraft2/models/item/roasted_copper_concentrate.json');
assert.ok(Object.values(model.textures).includes('minecraft:block/red_terracotta'));
const book=json(src+'resources/assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json');
const page=book.pages.find(p=>p.type==='patchouli:multiblock'&&p.name==='Ore Roasting Kiln');
assert.equal(page.multiblock.pattern.length,8,'5x8x9 structure must stay eight layers');
assert.ok(book.pages.some(p=>p.type==='text'&&p.text.includes('4 Copper Concentrate')));
const lang=json(src+'resources/assets/magneticraft2/lang/en_us.json');
assert.ok(lang['item.magneticraft2.roasted_copper_concentrate']);
assert.ok(lang['message.magneticraft2.ore_roasting_kiln.status']);
assert.ok(!lang['message.magneticraft2.ore_roasting_kiln.prototype']);
console.log('Ore Roasting Kiln K2: recipe wiring, ports, HEAT, saved state, JEI, book and old Copper route passed (static only).');
