#!/usr/bin/env node
// Bronze II Stamp Mill feedstock contract: preserve the accepted Copper
// throughput/yields and add only useful, registered materials with a
// complete already-implemented downstream processing route.
import fs from "node:fs";
import assert from "node:assert/strict";

const recipesDir="src/main/resources/data/magneticraft2/recipes/";
const path=name=>recipesDir+name+".json";
const read=name=>JSON.parse(fs.readFileSync(path(name),"utf8"));
const expected=[
 {name:"chalcocite",input:"magneticraft2:chalcocite_ore",output:"magneticraft2:crushed_chalcocite",tails:true,wasExisting:true},
 {name:"cassiterite",input:"magneticraft2:cassiterite_ore",output:"magneticraft2:crushed_cassiterite",tails:true,wasExisting:true},
 {name:"quartz",input:"magneticraft2:quartz_ore",output:"magneticraft2:crushed_quartz",tails:true},
 {name:"quartz_deepslate",input:"magneticraft2:quartz_deepslate_ore",output:"magneticraft2:crushed_quartz",tails:true},
 {name:"cobblestone",input:"minecraft:cobblestone",output:"minecraft:gravel",tails:false},
 {name:"cobbled_deepslate",input:"minecraft:cobbled_deepslate",output:"minecraft:gravel",tails:false}
];
const actualNames=fs.readdirSync(recipesDir)
 .filter(name=>/^bronze_stamping_[a-z_]+\.json$/.test(name))
 .map(name=>name.replace(/^bronze_stamping_|\.json$/g,"")).sort();
assert.deepEqual(actualNames,expected.map(e=>e.name).sort(),
 "The Bronze Stamp Mill ore list has changed; review its matching processing chains");
const blockRegister=fs.readFileSync(
 "src/main/java/com/magneticraft2/common/registry/registers/BlockRegistry.java","utf8");
const itemRegister=fs.readFileSync(
 "src/main/java/com/magneticraft2/common/registry/registers/ItemRegistry.java","utf8");
const bronzeGuide=JSON.parse(fs.readFileSync(
 "src/main/resources/assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json","utf8"));
const jei=fs.readFileSync(
 "src/main/java/com/magneticraft2/compatibility/JEI/MagneticraftJeiPlugin.java","utf8");
const controller=fs.readFileSync(
 "src/main/java/com/magneticraft2/common/blockentity/stage/bronze/BronzeStampMillBlockEntity.java","utf8");
const seen=new Set();
for(const e of expected){
 const r=read("bronze_stamping_"+e.name);
 assert.equal(r.type,"magneticraft2:multiblock_processing");
 assert.equal(r.machine,"magneticraft2:bronze_stamp_mill");
 assert.deepEqual(r.input,{item:e.input});
 assert.equal(r.input_count,2,"Stamp Mill stays batch-operated, not a single-ore grinder");
 assert.deepEqual(r.output,{item:e.output,count:2});
 assert.deepEqual(r.mechanical,{min_speed:24,torque:6});
 assert.equal(r.process_time,115);
 assert.ok(!seen.has(e.input),"Ambiguous duplicate recipe ingredient "+e.input);
 seen.add(e.input);
 assert.ok(!r.fluid_input&&!r.fluid_output&&!r.fuel&&!r.heat,
  "Coarse ore breaking cannot bypass washing, sifting, smelting or fuel");
 if(e.tails){
  assert.deepEqual(r.byproduct,{item:"minecraft:gravel",count:1});
  assert.equal(r.byproduct_chance,.2);
 }else{
  assert.ok(!("byproduct" in r)&&!("byproduct_chance" in r),
   "Stone -> Gravel must NOT also emit free additional Gravel");
 }
 if(e.input.startsWith("magneticraft2:")){
  const id=e.input.substring("magneticraft2:".length);
  assert.ok(blockRegister.includes('BLOCKS.register("'+id+'"'),
   "The input mineral is not registered: "+e.input);
 }
 if(e.output.startsWith("magneticraft2:")){
  const id=e.output.substring("magneticraft2:".length);
  assert.ok(itemRegister.includes('ITEMS.register("'+id+'"'),
   "There is no usable crushed intermediate: "+e.output);
 }
 assert.ok(!/ingot|concentrate|washed|powder/.test(r.output.item),
  "Stamp Mill must not create refined downstream results");
}
assert.ok(controller.includes("getActiveInputCount()")&&
 controller.includes("getMatchingRecipe()")&&
 controller.includes("canAccept(1, output)")&&
 controller.includes("pendingByproduct && !canAccept(2, byproduct)")&&
 controller.includes("insertOutput(1, output)")&&
 controller.includes("itemHandler.extractItem(0, recipe.getInputCount(), false)"),
 "Keep existing data-driven server recipe matching, output reservation and conservation");

// The existing lower-tier grinder remains usable while Bronze has better
// throughput: two ores in 115t, versus one ore in 160-180t.
for(const [grinderName,newName]of [
 ["quartz","quartz"],["quartz_deepslate","quartz_deepslate"]]){
 const grinder=read("primitive_grinder_"+grinderName);
 const stamp=read("bronze_stamping_"+newName);
 assert.equal(grinder.input.item,stamp.input.item);
 assert.equal(grinder.output.item,stamp.output.item);
 assert.equal(grinder.output.count,1);
 assert.ok(stamp.process_time/stamp.input_count < grinder.crushtime);
}
const wash=read("mechanical_washing_quartz");
const classify=read("mechanical_sifting_quartz");
const gravelSifter=read("mechanical_sifting_gravel");
assert.equal(wash.machine,"magneticraft2:mechanical_ore_washer");
assert.equal(wash.input.item,"magneticraft2:crushed_quartz");
assert.equal(wash.output.item,"magneticraft2:washed_quartz");
assert.equal(wash.fluid_input.fluid,"minecraft:water");
assert.equal(classify.machine,"magneticraft2:mechanical_sifter");
assert.equal(classify.input.item,"magneticraft2:washed_quartz");
assert.equal(classify.output.item,"minecraft:quartz");
assert.equal(gravelSifter.input.item,"minecraft:gravel");
assert.equal(gravelSifter.output.item,"minecraft:sand");
assert.equal(gravelSifter.byproduct.item,"minecraft:flint");
assert.equal(gravelSifter.byproduct_chance,.25);
for(const mineral of ["chalcocite","cassiterite"]){
 assert.equal(read("mechanical_washing_"+mineral).input.item,
  "magneticraft2:crushed_"+mineral);
 assert.equal(read("mechanical_sifting_"+mineral).input.item,
  "magneticraft2:washed_"+mineral);
}
// Iron ore and other later-age minerals have NO defined washed/sifted
// outputs yet. Do not silently invent dead-end items or skip metallurgy.
for(const blocked of ["magnetite","hematite","limonite","galena",
 "chromite","bauxite","silver","tantalite","uraninite"]){
 assert.ok(!seen.has("magneticraft2:"+blocked+"_ore"),
  "Late-age mineral without a tested refining chain added prematurely: "+blocked);
}
const guidePage=bronzeGuide.pages[27];
assert.ok(guidePage&&guidePage.type==="text"&&
 guidePage.text.includes("Quartz Ore")&&guidePage.text.includes("Deepslate Quartz Ore")&&
 guidePage.text.includes("Cobblestone")&&guidePage.text.includes("Cobbled Deepslate")&&
 guidePage.text.includes("115 ticks")&&guidePage.text.includes("Mechanical Ore Washer")===false&&
 guidePage.text.includes("Ore Washer")&&guidePage.text.includes("Sifter"),
 "Bronze book must explain the supported new feedstocks and their downstream machines");
assert.equal(bronzeGuide.extra_recipe_mappings["magneticraft2:quartz_ore"],27);
assert.equal(bronzeGuide.extra_recipe_mappings["magneticraft2:quartz_deepslate_ore"],27);
assert.ok(jei.includes("getAllRecipesFor(")&&
 jei.includes("MultiblockProcessingRecipe.Type.INSTANCE")&&
 jei.includes("MultiblockJeiRecipe::from")&&
 jei.includes("registration.addRecipes("),
 "JEI should discover ALL new machine-ID recipes from RecipeManager automatically");
console.log("Bronze Stamp Mill feedstocks PASS: six real recipes, "+
 "existing Grinder/Washer/Sifter outputs, conserved stone->gravel and JEI/book coverage");
