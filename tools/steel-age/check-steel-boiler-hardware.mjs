import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const read=p=>fs.readFileSync(path.join(root,p),"utf8");
const json=p=>JSON.parse(read(p));
const java="src/main/java/com/magneticraft2/common/";
const data="src/main/resources/data/magneticraft2/recipes/";
const assets="src/main/resources/assets/magneticraft2/";
const registry=read(java+"registry/registers/ItemRegistry.java");
const creative=read(java+"registry/registers/CreativeTabRegistry.java");
const press=read(java+"blockentity/stage/bronze/BronzeMechanicalPressBlockEntity.java");
const boring=read(java+"blockentity/stage/bronze/BronzePrecisionBoringMachineBlockEntity.java");
const maintenance=read(java+"systems/maintenance/ReplaceableMachinePart.java");
const book=json(assets+"patchouli_books/world_of_magneticraft/en_us/entries/steelage.json");
const lang=json(assets+"lang/en_us.json");

const ids=["boiler_end_plate","boiler_reinforcement_flange","pressure_pipe_coupling","steel_boring_bit"];
const parents=["heavy_steel_plate","iron_bearing_housing","steel_shaft","abrasive_boring_bit"];
for(let i=0;i<ids.length;i++){
 const id=ids[i];
 assert(registry.includes('ITEMS.register("'+id+'"'),"Missing registry: "+id);
 assert(creative.includes("entries.accept(ItemRegistry.ITEM_"+id.toUpperCase()+".get())"),
   "Creative tab missing: "+id);
 assert(lang["item.magneticraft2."+id],"Translation missing: "+id);
 const item=json(assets+"models/item/"+id+".json");
 assert.equal(item.parent,"magneticraft2:item/"+parents[i],"Placeholder parent: "+id);
 json(assets+"models/item/"+parents[i]+".json"); // parent must actually exist
}
const end=json(data+"bore_boiler_end_plate.json");
const flange=json(data+"press_boiler_reinforcement_flange.json");
const coupling=json(data+"bore_pressure_pipe_coupling.json");
const bit=json(data+"steel_boring_bit.json");
const olderShell=json(data+"press_boiler_shell_section.json");
const oldCopper=json(data+"bronze_pressing_copper_panel.json");
const oldIron=json(data+"bore_iron_bearing_housing.json");
const oldHolder=json(data+"bore_precision_iron_toolholder.json");
const heavy=json(data+"forge_heavy_steel_plate.json");
function process(recipe,machine,input,n,output,outCount,rpm,torque,ticks){
 assert.equal(recipe.type,"magneticraft2:multiblock_processing");
 assert.equal(recipe.machine,"magneticraft2:"+machine);
 assert.equal(recipe.input.item,"magneticraft2:"+input);
 assert.equal(recipe.input_count,n);
 assert.equal(recipe.output.item,"magneticraft2:"+output);
 assert.equal(recipe.output.count,outCount);
 assert.equal(recipe.mechanical.min_speed,rpm);
 assert.equal(recipe.mechanical.torque,torque);
 assert.equal(recipe.process_time,ticks);
 assert(recipe.mechanical.torque<=16,"Existing 16-torque mechanical input must support "+output);
}
process(end,"bronze_precision_boring_machine","heavy_steel_plate",1,
 "boiler_end_plate",1,20,12,300);
process(flange,"bronze_mechanical_press","refined_steel_plate",2,
 "boiler_reinforcement_flange",1,18,14,240);
process(coupling,"bronze_precision_boring_machine","refined_steel_bar",1,
 "pressure_pipe_coupling",1,20,12,260);
assert.equal(heavy.input_count,4);
assert.equal(heavy.output.count,1);
assert.equal(heavy.output.item,"magneticraft2:heavy_steel_plate");
assert.equal(olderShell.input.item,"magneticraft2:heavy_steel_plate");
assert.equal(oldCopper.input.item,"magneticraft2:copper_sheet");
assert.equal(oldIron.input.item,"magneticraft2:iron_gear_blank_medium");
assert.equal(oldHolder.input.item,"magneticraft2:wrought_iron_rod");
// Single-input recipe lookup uses the machine ID + item type, not the tool.
// Therefore no duplicate machine+item input pair may be introduced.
const recipes=[end,flange,coupling,olderShell,oldCopper,oldIron,oldHolder,heavy];
const keys=recipes.map(r=>r.machine+"/"+r.input.item);
assert.equal(new Set(keys).size,keys.length,"Ambiguous input recipe on same machine");
assert.equal(bit.type,"minecraft:crafting_shaped");
assert.equal(bit.result.item,"magneticraft2:steel_boring_bit");
assert.equal(bit.pattern.join("").split("P").length-1,3);
assert.equal(bit.key.P.item,"magneticraft2:refined_steel_plate");
assert.equal(bit.key.B.item,"magneticraft2:abrasive_boring_bit");
assert.equal(bit.key.R.item,"magneticraft2:refined_steel_rod");
assert(boring.includes("ITEM_STEEL_BORING_BIT::get, 1536"));
assert(boring.includes("boringBit.isInstalled(ItemRegistry.ITEM_STEEL_BORING_BIT.get())"));
assert(boring.includes("!toolingSupports(recipe)") && boring.includes("!toolingSupports(candidate)"),
 "Both runtime and diagnostics must enforce steel bit");
assert(boring.includes("HotBloomWorkpiece.consolidate(input, recipe.getOutput())"),
 "Machined Steel keeps quality without stale hot tag");
assert(boring.includes("if (input.hasTag()) result.setTag(input.getTag().copy())"),
 "Legacy quality transfer must stay intact");
assert(press.includes("ITEM_BOILER_REINFORCEMENT_FLANGE.get()"));
assert(press.includes("ITEM_BOILER_SHELL_SECTION.get()"));
assert(press.includes("pressDie.isInstalled(ItemRegistry.ITEM_STEEL_PRESS_DIE.get())"));
assert(press.includes("outputForRecipe(recipe, inputStack)"));
assert(press.includes("HotBloomWorkpiece.consolidate(input, recipe.getOutput())"));
assert(boring.includes('boringBit.load(tag.getCompound("BoreBit"))') &&
       boring.includes('tag.put("BoreBit", boringBit.save())'),"Boring tool NBT must survive reload");
assert(maintenance.includes("ForgeRegistries.ITEMS.getKey(itemAt(installedVariant))") &&
       maintenance.includes("tag.contains(INSTALLED, Tag.TAG_STRING)"),
       "Tool variants must use stable item ID NBT");
assert(boring.includes("boringBit.completeBatch()"));
assert(press.includes("pressDie.completeBatch()"));
assert(book.pages.some(p=>p.recipe==="magneticraft2:steel_boring_bit"));
assert(book.pages.some(p=>p.text?.includes("Steel IV-B: Boiler End Plates")));
assert(book.pages.some(p=>p.text?.includes("Boiler Reinforcement Flanges")));
assert(book.pages.some(p=>p.text?.includes("Pressure-Rated Pipe Couplings")));
assert(lang["message.magneticraft2.bronze_precision_boring_machine.reason.steel_bit_required"]);
console.log("PASS Steel IV-B static contract: all 4 items, 4 recipes, non-ambiguous machine inputs,");
console.log("power limits, quality preservation, old recipes, stable tool NBT, book and assets.");
console.log("Not a Forge build, real in-game test, or pressure-system verification.");
