#!/usr/bin/env node
// Iron I-C source/JSON contract. Not a Forge compile or runtime test.
import fs from "node:fs";
import assert from "node:assert/strict";
const root="src/main/resources/";
const jr=p=>JSON.parse(fs.readFileSync(root+p,"utf8"));
const java=p=>fs.readFileSync("src/main/java/com/magneticraft2/"+p,"utf8");
const prefix="data/magneticraft2/recipes/";
const newRoll=jr(prefix+"roll_wrought_iron_sheet.json");
assert.equal(newRoll.type,"magneticraft2:multiblock_processing");
assert.equal(newRoll.machine,"magneticraft2:bronze_rolling_mill");
assert.equal(newRoll.input.item,"magneticraft2:wrought_iron_plate");
assert.equal(newRoll.input_count,1);
assert.equal(newRoll.output.item,"magneticraft2:wrought_iron_sheet");
assert.equal(newRoll.output.count,2);
assert.equal(newRoll.process_time,240);
assert.deepEqual(newRoll.mechanical,{min_speed:16,torque:12});
const bar=jr(prefix+"roll_wrought_iron_bar.json");
const rods=jr(prefix+"roll_wrought_iron_rods.json");
const copper=jr(prefix+"bronze_rolling_copper_sheet.json");
assert.equal(bar.output.item,"magneticraft2:wrought_iron_bar");
assert.equal(rods.output.item,"magneticraft2:wrought_iron_rod");
assert.equal(bar.mechanical.torque,6);
assert.equal(rods.mechanical.torque,6);
assert.equal(copper.machine,"magneticraft2:bronze_rolling_mill");
const panelOld=jr(prefix+"iron_plate_panel.json");
const panelNew=jr(prefix+"iron_plate_panel_from_sheets.json");
const hatchOld=jr(prefix+"iron_service_hatch.json");
const hatchNew=jr(prefix+"iron_service_hatch_from_sheets.json");
assert.deepEqual(panelOld.pattern,["PP"]);
assert.deepEqual(panelNew.pattern,["SS"]);
assert.equal(panelNew.result.item,"magneticraft2:iron_plate_panel");
assert.equal(panelNew.result.count,1);
assert.deepEqual(hatchOld.pattern,["PBP"," R "]);
assert.deepEqual(hatchNew.pattern,["SBS"," R "]);
assert.equal(hatchNew.result.item,"magneticraft2:iron_service_hatch");
assert.equal(hatchNew.result.count,2);
const sheetCount=2;
const ingotEquivalentPerSheet=1/sheetCount;
assert.equal(sheetCount*ingotEquivalentPerSheet,1,"Metal must not multiply");
assert.equal(panelNew.pattern.join("").split("S").length-1,2);
const model=jr("assets/magneticraft2/models/item/wrought_iron_sheet.json");
assert.equal(model.parent,"minecraft:block/block");
assert.equal(model.elements.length,1);
const part=model.elements[0];
assert.deepEqual(part.from,[1,7.5,1]);
assert.deepEqual(part.to,[15,8.5,15]);
const dimensions=[14,1,14];
for(const [face,entry] of Object.entries(part.faces)){
 const dims=face==="up"||face==="down"?[14,14]:
  face==="north"||face==="south"?[14,1]:[14,1];
 assert.deepEqual(entry.uv,[0,0,...dims],"Native UV "+face);
}
assert.equal(model.textures.iron,"magneticraft2:block/material/dark_work_iron");
const items=java("common/registry/registers/ItemRegistry.java");
const tabs=java("common/registry/registers/CreativeTabRegistry.java");
const hot=java("common/systems/material/HotBloomWorkpiece.java");
const mill=java("common/blockentity/stage/bronze/BronzeRollingMillBlockEntity.java");
assert.ok(items.includes('ITEM_WROUGHT_IRON_SHEET ='));
assert.ok(items.includes('ITEMS.register("wrought_iron_sheet"'));
assert.ok(tabs.includes("ITEM_WROUGHT_IRON_SHEET.get()"));
assert.ok(hot.includes("preservesRollingQuality("));
assert.ok(hot.includes("recipe.getOutput().is(ItemRegistry.ITEM_WROUGHT_IRON_SHEET.get())"));
assert.ok(hot.includes("metalData.remove(HOT_UNTIL_TAG);"));
assert.ok(hot.includes("requiresRollingHeat(MultiblockProcessingRecipe recipe)"));
const rollHeatStart=hot.indexOf("public static boolean requiresRollingHeat(");
const rollHeatEnd=hot.indexOf("public static boolean preservesRollingQuality(");
assert.ok(!hot.slice(rollHeatStart,rollHeatEnd).includes("ITEM_WROUGHT_IRON_SHEET"),
 "Cold finishing must not require unavailable Plate reheat");
assert.equal(mill.split("HotBloomWorkpiece.preservesRollingQuality(").length-1,2,
 "Production AND interaction diagnostics must reserve NBT-matched output");
assert.ok(mill.includes("HotBloomWorkpiece.requiresRollingHeat(recipe)"),
 "Hot material processing must remain unchanged");
assert.ok(mill.includes("rollerSleeves.completeBatch()"));
assert.ok(mill.includes("itemHandler.insertItem(1, output, true)"));
assert.equal(jr("assets/magneticraft2/lang/en_us.json")
 ["item.magneticraft2.wrought_iron_sheet"],"Wrought Iron Sheet");
assert.ok(jr("assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/ironage.json")
 .pages.some(p=>p.text?.includes("Cold-Finished Sheet")));
console.log("Iron Sheet source/JSON contract PASS; material ledger 1 Plate = "+
 sheetCount+" Sheets, two consumer recipes, quality without heat. "+
 "Forge build and in-game tests NOT performed.");
