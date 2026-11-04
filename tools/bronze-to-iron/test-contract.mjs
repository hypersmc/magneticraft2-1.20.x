#!/usr/bin/env node
// Source/resource regression only, not a Forge build or in-game acceptance.
import fs from "node:fs";
import assert from "node:assert/strict";
const root="src/main/resources/";
const java="src/main/java/com/magneticraft2/common/";
const get=n=>JSON.parse(fs.readFileSync(root+"data/magneticraft2/recipes/"+n+".json","utf8"));
const source=n=>fs.readFileSync(java+n,"utf8");
const reheat=get("reheat_iron_bloom");
const hammer=get("consolidate_iron_bloom");
const bloom=get("bloomery_magnetite_reduction");
assert.equal(reheat.type,"magneticraft2:multiblock_processing");
assert.equal(reheat.machine,"magneticraft2:refractory_kiln");
assert.deepEqual(reheat.input,{item:"magneticraft2:iron_bloom"});
assert.equal(reheat.input_count,1);
assert.deepEqual(reheat.output,{item:"magneticraft2:iron_bloom",count:1});
assert.equal(reheat.process_time,240);
assert.equal(hammer.machine,"magneticraft2:bronze_forge_hammer");
assert.equal(hammer.input.item,"magneticraft2:iron_bloom");
assert.equal(hammer.input_count,1);
assert.deepEqual(hammer.output,{item:"magneticraft2:wrought_iron_ingot",count:1});
assert.equal(hammer.process_time,160);
assert.ok(hammer.mechanical.min_speed>=20 && hammer.mechanical.torque>=6);
assert.equal(bloom.output.item,"magneticraft2:iron_bloom");
assert.equal(bloom.slag.item,"magneticraft2:slagitem");
const heat=source("systems/material/HotBloomWorkpiece.java");
assert.ok(heat.includes("Tag.TAG_LONG"));
assert.ok(heat.includes("HOT_WINDOW_TICKS = 2400"));
assert.ok(heat.includes("tag.getLong(HOT_UNTIL_TAG) > world.getGameTime()"));
assert.ok(heat.includes("world.getGameTime() + HOT_WINDOW_TICKS"));
assert.ok(heat.includes("metalData.remove(HOT_UNTIL_TAG)"));
const kiln=source("blockentity/stage/bronze/RefractoryKilnBlockEntity.java");
assert.ok(kiln.includes("HotBloomWorkpiece.heatInKiln(candidate, product, level)"));
assert.ok(kiln.indexOf("HotBloomWorkpiece.heatInKiln")<
          kiln.indexOf("itemHandler.insertItem(1, product, true)"));
const forge=source("blockentity/stage/bronze/BronzeForgeHammerBlockEntity.java");
assert.ok(forge.includes("!HotBloomWorkpiece.isHot(inputStack, level)"));
assert.ok(forge.includes("HotBloomWorkpiece.consolidate(inputStack, recipe.getOutput())"));
assert.ok(forge.includes('diagnostic = "bloom_cold"'));
assert.ok(forge.includes("forgingDie.completeBatch()"));
assert.ok(forge.includes("manager.getMechanicalLoadState(level, worldPosition).supplied()"));
const item=source("item/stage/bronze/IronBloomItem.java");
assert.ok(item.includes("HotBloomWorkpiece.isHot(stack, level)"));
assert.ok(item.includes("HotBloomWorkpiece.remainingTicks(stack, level)"));
const items=source("registry/registers/ItemRegistry.java");
assert.ok(items.includes('ITEMS.register("iron_bloom", IronBloomItem::new)'));
assert.ok(items.includes('ITEMS.register("wrought_iron_ingot"'));
const book=JSON.parse(fs.readFileSync(root+"assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json","utf8"));
assert.ok(book.pages.some(p=>p.text?.includes("Refractory Kiln")&&p.text?.includes("Wrought Iron")));
const lang=JSON.parse(fs.readFileSync(root+"assets/magneticraft2/lang/en_us.json","utf8"));
for(const key of ["item.magneticraft2.wrought_iron_ingot",
 "tooltip.magneticraft2.iron_bloom.hot","tooltip.magneticraft2.iron_bloom.cold",
 "message.magneticraft2.bronze_forge_hammer.reason.bloom_cold"])
 assert.ok(lang[key],"Missing translation "+key);
assert.ok(fs.existsSync(root+"assets/magneticraft2/textures/item/wrought_iron_ingot.png"));
assert.equal(get("bronze_forging_copper_plate").output.item,"magneticraft2:copper_plate");
assert.equal(get("firing_refractory_batch").output.item,"magneticraft2:fired_refractory_ceramic");
console.log("Bronze-to-Iron source/JSON contract PASS (NOT a Forge build).");
