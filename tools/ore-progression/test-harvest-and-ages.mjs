#!/usr/bin/env node
/**
 * Regression contract for Magneticraft2's registered ore catalog.
 * Age means earliest useful PROCESSING unlock, not ore spawn/mining tier.
 * This keeps the Stone -> Copper -> Bronze metallurgy handoff collectible.
 */
import fs from "node:fs";
import assert from "node:assert/strict";

const read=path=>JSON.parse(fs.readFileSync(path,"utf8"));
const java="src/main/java/com/magneticraft2/common/";
const data="src/main/resources/data/";
const blockSource=fs.readFileSync(java+"registry/registers/BlockRegistry.java","utf8");
const itemSource=fs.readFileSync(java+"registry/registers/ItemRegistry.java","utf8");
const ores=[...blockSource.matchAll(
 /RegistryObject<\w+>\s+([A-Z0-9_]+_ORE)\s*=\s*BLOCKS\.register\("([^"]+)"/g
)].map(([_,symbol,id])=>({symbol,id}));
const oreItems=new Set([...itemSource.matchAll(/fromBlockOre\(([A-Z0-9_]+)\)/g)]
 .map(([,sym])=>sym));
assert.equal(ores.length,42,
 "Registered ore count changed: update block drops AND the age matrix");
assert.equal(new Set(ores.map(o=>o.id)).size,ores.length);
assert.ok(ores.every(o=>oreItems.has(o.symbol)),
 "Every ore block must have an obtainable BlockItem");
const pickaxe=read(data+"minecraft/tags/blocks/mineable/pickaxe.json").values;
const tier=read(data+"minecraft/tags/blocks/needs_stone_tool.json").values;
assert.equal(new Set(pickaxe).size,pickaxe.length,"Pickaxe tags duplicated");
assert.equal(new Set(tier).size,tier.length,"Harvest-tier tags duplicated");

const ageFile=read("docs/ore-processing-by-age.json");
assert.equal(ageFile.schema_version,1);
assert.ok(ageFile.meaning.includes("processing/extraction unlock".split("/")[0])||
          ageFile.meaning.includes("processing"),
 "Age map must be a processing unlock, not a mining restriction");
const expectedAges=["Stone","Copper","Bronze","Iron","Steel",
 "Steam","Gasoline","Electric Generator","Nuclear"];
assert.deepEqual(ageFile.ages.map(a=>a.age),expectedAges);
assert.ok(ageFile.ages.every(a=>a.state==="planned"||a.state==="implemented"));
const base=ore=>ore.replace(/_deepslate_ore$/,"").replace(/_ore$/,"");
const groups=new Set(ores.map(o=>base(o.id)));
const mapOres=ageFile.ages.flatMap(a=>a.ores);
assert.equal(groups.size,31,"Unexpected new mineral family");
assert.equal(mapOres.length,31,"Exactly 31 families must be assigned");
assert.equal(new Set(mapOres).size,31,"A mineral cannot belong to two ages");
assert.deepEqual([...mapOres].sort(),[...groups].sort(),
 "All registered minerals must be assigned, with no invented ore");
assert.deepEqual(ageFile.ages.find(a=>a.age==="Copper").ores,
 ["chalcocite","cassiterite","quartz"],
 "Established Copper chain must not be moved into Bronze");
assert.deepEqual(ageFile.ages.find(a=>a.age==="Bronze").ores,
 ["magnetite","hematite","limonite","galena"],
 "Bloomery ore-preparation families must stay in Bronze design stage");
assert.equal(ageFile.ages.filter(a=>a.state==="implemented").length,1,
 "Only the Copper ore processing families are currently implemented");
assert.equal(ageFile.ages.find(a=>a.age==="Gasoline").ores.length,0,
 "Do not invent a compulsory mineral merely for the Gasoline Age");

for(const {id} of ores){
 const name="magneticraft2:"+id;
 assert.ok(pickaxe.includes(name),"Ore cannot be broken with pickaxe: "+id);
 assert.ok(tier.includes(name),"Ore has no baseline stone-tool harvest: "+id);
 const drop=read(data+"magneticraft2/loot_tables/blocks/"+id+".json");
 assert.equal(drop.type,"minecraft:block");
 assert.equal(drop.pools.length,1,"One ore block must have one drop pool: "+id);
 const p=drop.pools[0];
 assert.equal(p.rolls,1,"Ore drops must not multiply on harvest: "+id);
 assert.equal(p.entries.length,1);
 assert.deepEqual(p.entries[0],{type:"minecraft:item",name});
 assert.ok(p.conditions.some(x=>x.condition==="minecraft:survives_explosion"),
  "Ore block should respect explosions like the original Chalcocite: "+id);
 const feature=read(data+"magneticraft2/worldgen/configured_feature/"+id+".json");
 assert.equal(feature.type,"minecraft:ore");
 assert.ok(feature.config.targets.some(t=>t.state?.Name===name),
  "Configured ore feature does not actually generate "+name);
 const assignment=ageFile.ages.find(a=>a.ores.includes(base(id)));
 assert.ok(assignment,"Deepslate ore lacks its normal type's processing age: "+id);
}
// Both accepted Bronze controllers explicitly require a correct tool.
// An item loot table alone is insufficient unless the pickaxe can harvest.
for(const machine of ["bronze_stamp_mill","bronze_mechanical_lathe"]){
 const name="magneticraft2:"+machine;
 assert.ok(pickaxe.includes(name)&&tier.includes(name),
  "Recoverable Bronze machinery is missing pickaxe/harvest tier tags: "+name);
 const path=java+"block/stage/bronze/"+
  (machine==="bronze_stamp_mill"?"BronzeStampMillBlock.java":"BronzeMechanicalLatheBlock.java");
 assert.ok(fs.readFileSync(path,"utf8").includes(".requiresCorrectToolForDrops()"),
  "Controller mining contract has unexpectedly changed: "+machine);
 const drop=read(data+"magneticraft2/loot_tables/blocks/"+machine+".json");
 assert.ok(drop.pools.some(p=>p.entries?.some(e=>e.name===name)),
  "Breaking the controller does not return it: "+machine);
}
// Current furnace/yield tiers must survive this harvestability repair.
const rec=n=>read(data+"magneticraft2/recipes/"+n+".json");
for(const [name,expected]of [
 ["primitive_furnace_chalcocite",1],
 ["primitive_furnace_crushed_chalcocite",2],
 ["primitive_furnace_copper_concentrate",3]]){
 const r=rec(name);
 assert.equal(r.output1.item,"minecraft:copper_ingot");
 assert.equal(r.output1.count,expected);
}
assert.equal(rec("primitive_furnace_tin_concentrate").output1.item,
 "magneticraft2:tin_ingot");
assert.equal(rec("primitive_furnace_bronze_charge").output1.count,4);
for(const mineral of ["chalcocite","cassiterite","quartz"]){
 assert.equal(rec("mechanical_washing_"+mineral).input.item,
  "magneticraft2:crushed_"+mineral);
 assert.equal(rec("mechanical_sifting_"+mineral).input.item,
  "magneticraft2:washed_"+mineral);
}
const stone=read("src/main/resources/assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/stoneage.json");
const copper=read("src/main/resources/assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/copperage.json");
const bronze=read("src/main/resources/assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json");
assert.ok(stone.pages[17].text.includes("Stone Pickaxe"));
assert.ok(copper.pages[7].text.includes("Cassiterite")&&
          copper.pages[7].text.includes("Washer")&&
          !copper.pages[7].text.includes("Next milestones"));
assert.ok(!bronze.pages[17].text.includes("still in progress"),
 "Guidebook must not regress to describing the accepted Lathe as unfinished");
console.log("Ore harvest/age PASS: 42 self-dropping ores, 31 age families, "+
 "correct tool tags, Bronze controllers and unchanged Copper yields");
