import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import {fileURLToPath} from "node:url";
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),"../..");
const read=p=>fs.readFileSync(path.join(root,p),"utf8");
const json=p=>JSON.parse(read(p));
const data="src/main/resources/data/magneticraft2/recipes/";
const items="src/main/resources/assets/magneticraft2/models/item/";
const named=["refined_steel_plate","refined_steel_bar","refined_steel_rod","steel_roller_sleeves"];
const recipes={
 hearth_reheat_refined_steel_ingots:["iron_forge_hearth","refined_steel_ingot","refined_steel_ingot",4,4],
 hearth_reheat_refined_steel_bars:["iron_forge_hearth","refined_steel_bar","refined_steel_bar",4,4],
 forge_refined_steel_plate:["bronze_forge_hammer","refined_steel_ingot","refined_steel_plate",1,1],
 roll_refined_steel_bar:["bronze_rolling_mill","refined_steel_ingot","refined_steel_bar",1,1],
 roll_refined_steel_rods:["bronze_rolling_mill","refined_steel_bar","refined_steel_rod",1,4]
};
const reg=read("src/main/java/com/magneticraft2/common/registry/registers/ItemRegistry.java");
for(const item of named){
 assert(reg.includes('ITEMS.register("'+item+'"'),"missing item registry "+item);
 const model=json(items+item+".json");
 assert.equal(model.parent,"minecraft:block/block");
 assert(model.elements.length>0,"missing Steel II temporary item geometry");
 for(const e of model.elements)for(const [side,f]of Object.entries(e.faces)){
  const u=side==="east"||side==="west"?e.to[2]-e.from[2]:e.to[0]-e.from[0];
  const v=side==="up"||side==="down"?e.to[2]-e.from[2]:e.to[1]-e.from[1];
  assert.deepEqual(f.uv,[0,0,u,v],"stretched item UV: "+item+" "+side);
 }
}
for(const [id,[machine,input,output,inputCount,outputCount]]of Object.entries(recipes)){
 const r=json(data+id+".json");
 assert.equal(r.type,"magneticraft2:multiblock_processing");
 assert.equal(r.machine,"magneticraft2:"+machine);
 assert.equal(r.input.item,"magneticraft2:"+input);
 assert.equal(r.input_count,inputCount);
 assert.equal(r.output.item,"magneticraft2:"+output);
 assert.equal(r.output.count,outputCount);
 assert(r.process_time>0&&r.process_time<=72000);
 if(machine!=="iron_forge_hearth")assert(r.mechanical.min_speed>0&&r.mechanical.torque>0);
 else assert.equal(r.process_time,480);
}
const rollers=json(data+"steel_roller_sleeves.json");
assert.equal(rollers.result.item,"magneticraft2:steel_roller_sleeves");
assert.equal(rollers.pattern.join("").split("P").length-1,4);
assert.equal(rollers.key.S.item,"magneticraft2:wrought_iron_roller_sleeves");
const stock=read("src/main/java/com/magneticraft2/common/systems/material/HotBloomWorkpiece.java");
for(const term of ["isSteelReheatRecipe","requiresSteelDie","requiresSteelRollers","ITEM_REFINED_STEEL_BAR","ITEM_REFINED_STEEL_INGOT"])
 assert(stock.includes(term),"missing stock rule "+term);
const hearth=read("src/main/java/com/magneticraft2/common/blockentity/stage/iron/IronForgeHearthBlockEntity.java");
const hammer=read("src/main/java/com/magneticraft2/common/blockentity/stage/bronze/BronzeForgeHammerBlockEntity.java");
const mill=read("src/main/java/com/magneticraft2/common/blockentity/stage/bronze/BronzeRollingMillBlockEntity.java");
const maint=read("src/main/java/com/magneticraft2/common/systems/maintenance/ReplaceableMachinePart.java");
assert(hearth.includes("HearthSteelHeatingThreshold")&&hearth.includes("isSteelReheatRecipe"));
assert(hearth.includes("ITEM_METALLURGICAL_COKE"));
assert(hammer.includes("toolingSupports(recipe)")&&hammer.includes("ITEM_STEEL_FORGE_DIE"));
assert(mill.includes("toolingSupports(recipe)")&&mill.includes("ITEM_STEEL_ROLLER_SLEEVES::get, 2048"));
assert(maint.includes("boolean isInstalled(Item item)"));
const lang=json("src/main/resources/assets/magneticraft2/lang/en_us.json");
for(const item of named)assert(lang["item.magneticraft2."+item]);
const book=json("src/main/resources/assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/steelage.json");
assert(book.pages.some(p=>p.recipe==="magneticraft2:steel_roller_sleeves"));
console.log("PASS Steel II: 4 registered items/models, 6 recipes, heat and tooling gates, native item UV, book and language");
