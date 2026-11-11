import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import {fileURLToPath} from "node:url";

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const read=p=>fs.readFileSync(path.join(root,p),"utf8");
const json=p=>JSON.parse(read(p));
const R="src/main/resources/",J="src/main/java/com/magneticraft2/common/";
const recipes=R+"data/magneticraft2/recipes/";
const models=R+"assets/magneticraft2/models/item/";
const itemRegistry=read(J+"registry/registers/ItemRegistry.java");
const creative=read(J+"registry/registers/CreativeTabRegistry.java");
const hotStock=read(J+"systems/material/HotBloomWorkpiece.java");
const forge=read(J+"blockentity/stage/bronze/BronzeForgeHammerBlockEntity.java");
const press=read(J+"blockentity/stage/bronze/BronzeMechanicalPressBlockEntity.java");
const book=json(R+"assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/steelage.json");
const lang=json(R+"assets/magneticraft2/lang/en_us.json");

const items={
 heavy_steel_plate:"refined_steel_plate",
 boiler_shell_section:"refined_steel_plate",
 steel_press_die:"steel_forge_die"
};
for(const [item,parent] of Object.entries(items)){
 assert(itemRegistry.includes('ITEMS.register("'+item+'"'),"unregistered item: "+item);
 assert(creative.includes("entries.accept(ItemRegistry.ITEM_"+item.toUpperCase()+".get())"),
  "missing creative item "+item);
 assert(lang["item.magneticraft2."+item],"missing item name "+item);
 assert.equal(json(models+item+".json").parent,"magneticraft2:item/"+parent,
  "missing valid placeholder parent "+item);
}
const heavy=json(recipes+"forge_heavy_steel_plate.json");
const shell=json(recipes+"press_boiler_shell_section.json");
const die=json(recipes+"steel_press_die.json");
const ordinaryPlate=json(recipes+"forge_refined_steel_plate.json");
const barReheat=json(recipes+"hearth_reheat_refined_steel_bars.json");

for(const [name,r]of [["heavy",heavy],["shell",shell]]){
 assert.equal(r.type,"magneticraft2:multiblock_processing",name+" recipe type");
 assert(r.process_time>=1&&r.process_time<=2400,"reasonable production time");
 assert(r.mechanical.min_speed>0&&r.mechanical.torque>0&&r.mechanical.torque<=16,
   "must work with existing maximum 16-torque source before Steam");
}
assert.equal(heavy.machine,"magneticraft2:bronze_forge_hammer");
assert.equal(heavy.input.item,"magneticraft2:refined_steel_bar");
assert.equal(heavy.input_count,4);
assert.equal(heavy.output.item,"magneticraft2:heavy_steel_plate");
assert.equal(heavy.output.count,1);
assert.equal(heavy.mechanical.min_speed,20);
assert.equal(heavy.mechanical.torque,12);
assert.notEqual(ordinaryPlate.input.item,heavy.input.item,
  "existing Steel plate and new heavy plate must be unambiguous to the same Hammer");
assert.equal(barReheat.machine,"magneticraft2:iron_forge_hearth");
assert.equal(barReheat.input.item,"magneticraft2:refined_steel_bar");
assert.equal(barReheat.input_count,4);
assert.equal(barReheat.output.count,4);
assert.equal(shell.machine,"magneticraft2:bronze_mechanical_press");
assert.equal(shell.input.item,"magneticraft2:heavy_steel_plate");
assert.equal(shell.input_count,1);
assert.equal(shell.output.item,"magneticraft2:boiler_shell_section");
assert.equal(shell.output.count,1);
assert.equal(shell.mechanical.min_speed,18);
assert.equal(shell.mechanical.torque,14);
assert.equal(die.type,"minecraft:crafting_shaped");
assert.equal(die.result.item,"magneticraft2:steel_press_die");
assert.equal(die.pattern.join("").split("P").length-1,4);
assert.equal(die.key.P.item,"magneticraft2:refined_steel_plate");
assert.equal(die.key.D.item,"magneticraft2:bronze_press_die");

// Forge has only one hot Steel Bars recipe, preserves quality on the heavy stock.
assert(hotStock.includes("ITEM_HEAVY_STEEL_PLATE.get()"),"Heavy Plate not in Steel Forge Die gate");
assert(hotStock.includes("ITEM_REFINED_STEEL_BAR.get()"),"Bars not approved as hot stock");
assert(hotStock.includes("|| requiresSteelDie(recipe);"),"Heavy Forge must require hot steel");
assert(forge.includes("HotBloomWorkpiece.isHot(inputStack, level)"),
  "Forge must check server-owned heat each supplied tick");
assert(forge.includes("HotBloomWorkpiece.consolidate(inputStack, recipe.getOutput())"),
  "Heavy Plate must inherit source quality and lose temporary hot state");
assert(forge.includes("toolingSupports(recipe)"),"Steel Die gate on Forge");
// Pressure forming is a real powered processing operation with a persisted service part.
assert(press.includes("ITEM_STEEL_PRESS_DIE::get, 1536"),"Press steel tooling tier");
assert(press.includes("pressDie.isInstalled(ItemRegistry.ITEM_STEEL_PRESS_DIE.get())"),
  "installed press die must be explicitly validated");
assert(press.includes("!toolingSupports(recipe)"),"press must enforce tool tier every tick");
assert(press.includes("!toolingSupports(candidate)"),"press must explain incorrect die");
assert(press.includes("HotBloomWorkpiece.consolidate(input, recipe.getOutput())"),
  "Boiler Shell must inherit material quality and lose temporary heat");
assert(press.includes("outputForRecipe(candidate, inputStack), true"),
  "Press diagnostics must reserve NBT-aware output");
assert(press.includes("ItemStack output = outputForRecipe(recipe, inputStack);"),
  "Press runtime must reserve NBT-aware output");
assert(press.includes('tag.put("PressDie", pressDie.save())') &&
       press.includes('pressDie.load(tag.getCompound("PressDie"))'),
  "installed Press tier and wear must survive reload");
assert(press.includes("pressDie.completeBatch()"),"Press tool wears on success");
assert(book.pages.some(p=>p.recipe==="magneticraft2:steel_press_die"),
  "Patchouli must show Steel Press Die");
assert(book.pages.some(p=>p.text?.includes("Steel IV-A: Heavy Plate")),
  "Patchouli must explain actual four-bar forge step");
assert(book.pages.some(p=>p.text?.includes("Curving a Boiler Shell")),
  "Patchouli must explain press upgrade");
assert(lang["message.magneticraft2.bronze_mechanical_press.reason.steel_die_required"],
  "Missing wrong-tool feedback");
console.log("PASS Steel IV-A static contract: mass ledger, hot bars, forge/tool gates,");
console.log("press quality, output reservation, NBT wear, JSON recipes, models and Patchouli");
console.log("Not a Forge build or in-game/multiplayer test.");
