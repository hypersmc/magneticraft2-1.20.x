// Focused Iron Bearing Housing source-and-resource test, no Minecraft runtime.
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import {fileURLToPath} from "node:url";

const repo=path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const read=p=>fs.readFileSync(path.join(repo,p),"utf8");
const j=p=>JSON.parse(read(p));
const base="src/main/resources/";
const recipes=id=>j(base+"data/magneticraft2/recipes/"+id+".json");
const item=read("src/main/java/com/magneticraft2/common/registry/registers/ItemRegistry.java");
const tab=read("src/main/java/com/magneticraft2/common/registry/registers/CreativeTabRegistry.java");
const borer=read("src/main/java/com/magneticraft2/common/blockentity/stage/bronze/BronzePrecisionBoringMachineBlockEntity.java");
const hammer=read("src/main/java/com/magneticraft2/common/blockentity/stage/bronze/BronzeForgeHammerBlockEntity.java");
const heat=read("src/main/java/com/magneticraft2/common/systems/material/HotBloomWorkpiece.java");
const jei=read("src/main/java/com/magneticraft2/compatibility/JEI/MagneticraftJeiPlugin.java");
const lang=j(base+"assets/magneticraft2/lang/en_us.json");
const book=j(base+"assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/ironage.json");
const forged=recipes("forge_iron_gear_blank_medium");
const bore=recipes("bore_iron_bearing_housing");
const oldGearbox=recipes("wrought_iron_gearbox");
const newGearbox=recipes("wrought_iron_gearbox_bearing_housing");
const oldFly=recipes("iron_flywheel");
const newFly=recipes("iron_flywheel_bearing_housing");
const ingredientCount=(r,symbol)=>r.pattern.join("").split(symbol).length-1;

assert.match(item,/ITEM_IRON_BEARING_HOUSING\s*=\s*ITEMS\.register\("iron_bearing_housing"/);
assert.match(tab,/entries\.accept\(ItemRegistry\.ITEM_IRON_BEARING_HOUSING\.get\(\)\)/);
assert.equal(lang["item.magneticraft2.iron_bearing_housing"],"Machined Iron Bearing Housing");
assert(book.pages.some(p=>p.item==="magneticraft2:iron_bearing_housing"));
for(const id of ["wrought_iron_gearbox_bearing_housing","iron_flywheel_bearing_housing"])
  assert(book.pages.some(p=>p.recipe==="magneticraft2:"+id));

assert.equal(forged.input.item,"magneticraft2:wrought_iron_bar");
assert.equal(forged.output.item,"magneticraft2:iron_gear_blank_medium");
assert.match(heat,/recipe\.getOutput\(\)\.is\(ItemRegistry\.ITEM_IRON_GEAR_BLANK_MEDIUM\.get\(\)\)/);
assert.match(hammer,/HotBloomWorkpiece\.consolidate\(inputStack, recipe\.getOutput\(\)\)/);

assert.equal(bore.type,"magneticraft2:multiblock_processing");
assert.equal(bore.machine,"magneticraft2:bronze_precision_boring_machine");
assert.deepEqual(bore.input,{item:"magneticraft2:iron_gear_blank_medium"});
assert.equal(bore.input_count,1);
assert.deepEqual(bore.output,{item:"magneticraft2:iron_bearing_housing",count:1});
assert.equal(bore.process_time,240);
assert.deepEqual(bore.mechanical,{min_speed:20,torque:8});
assert.match(borer,/MultiblockRecipeHandler\.findRecipe\(level, MACHINE,/);
assert.match(borer,/if \(input\.hasTag\(\)\) result\.setTag\(input\.getTag\(\)\.copy\(\)\)/);
assert.match(borer,/boringBit\.completeBatch\(\)/);
assert.match(borer,/getMechanicalLoadState\(level, worldPosition\)\.supplied\(\)/);
assert.match(jei,/\.getAllRecipesFor\(\s*MultiblockProcessingRecipe/);
for(const [oldR,newR,expectedOutput] of [
 [oldGearbox,newGearbox,"magneticraft2:wrought_iron_gearbox"],
 [oldFly,newFly,"magneticraft2:iron_flywheel"]
]){
 assert.equal(oldR.type,"minecraft:crafting_shaped");
 assert.equal(newR.type,"minecraft:crafting_shaped");
 assert.equal(oldR.result.item,expectedOutput);
 assert.equal(newR.result.item,expectedOutput);
 assert.equal(newR.result.count,1);
 assert.equal(newR.key.H.item,"magneticraft2:iron_bearing_housing");
 assert.equal(newR.key.B.item,"magneticraft2:bronze_bushing");
 assert.equal(ingredientCount(newR,"H"),1);
 assert.equal(ingredientCount(newR,"B"),expectedOutput==="magneticraft2:wrought_iron_gearbox"?2:1);
 assert.equal(ingredientCount(oldR,"H"),0);
}
assert.deepEqual(oldGearbox.pattern,["PGP","SBS","PGP"]);
assert.deepEqual(newGearbox.pattern,["PGP","SHS","BGB"]);
assert.equal(ingredientCount(oldGearbox,"P"),4);
assert.equal(ingredientCount(newGearbox,"P"),2);
assert.equal(ingredientCount(newGearbox,"B"),2);
// Housing starts as a forged one-Bar Gear Blank (1 ingot-equivalent).
// Savings must be real, not a performance buff or 2-for-1 output.
assert.equal(oldGearbox.result.count,1);
assert.equal(newGearbox.result.count,1);
assert.equal(ingredientCount(oldGearbox,"P") -
  (ingredientCount(newGearbox,"P") + bore.input_count),1,
  "Housing path must save exactly one iron ingot-equivalent");
assert.equal(ingredientCount(newGearbox,"B") -
  ingredientCount(oldGearbox,"B"),1,
  "Housing alternate must use two separate bronze bushings");
assert.equal(ingredientCount(newGearbox,"G"),2);
assert.equal(ingredientCount(newGearbox,"S"),2);
assert.deepEqual(oldFly.pattern,["PBP","FGS","PBP"]);
assert.deepEqual(newFly.pattern,["PHP","FGS","PBP"]);
assert.equal(ingredientCount(oldFly,"B"),2);
assert.equal(ingredientCount(newFly,"P"),4);
assert.equal(ingredientCount(newFly,"F"),1);
assert.equal(ingredientCount(newFly,"G"),1);
assert.equal(ingredientCount(newFly,"S"),1);

const m=j(base+"assets/magneticraft2/models/item/iron_bearing_housing.json");
assert.equal(m.parent,"minecraft:block/block");
assert.equal(m.elements.length,6);
assert.equal(m.textures.iron,"magneticraft2:block/material/dark_work_iron");
for(const e of m.elements){
  const [x,y,z]=e.from,[a,b,c]=e.to;
  assert(a>x&&b>y&&c>z);
  assert(e.from.every(v=>v>=0)&&e.to.every(v=>v<=16));
  const dim={north:[a-x,b-y],south:[a-x,b-y],east:[c-z,b-y],
    west:[c-z,b-y],up:[a-x,c-z],down:[a-x,c-z]};
  for(const [f,[w,h]] of Object.entries(dim)){
    const face=e.faces[f];
    assert.equal(face.texture,"#iron");
    assert.deepEqual(face.uv,[0,0,w,h],"Native UV: "+f);
  }
}
// 4x4px central journal pocket traverses the full iron wall depth.
for(const e of m.elements){
  const overlaps=e.from[0]<10&&e.to[0]>6&&e.from[1]<10&&e.to[1]>6
    &&e.from[2]<11&&e.to[2]>5;
  assert(!overlaps,"Housing center must stay open for the bronze liner");
}
console.log("PASS: forged blank -> bored housing -> two optional iron machine assemblies; 1:1 UVs");
