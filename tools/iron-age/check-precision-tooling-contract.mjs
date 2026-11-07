// Iron precision tooling contract. Run from the full project checkout.
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import path from "node:path";

const ROOT=path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const read=p=>readFileSync(path.join(ROOT,p),"utf8");
const json=p=>JSON.parse(read(p));
const base="src/main/";
const lathe=read(base+"java/com/magneticraft2/common/blockentity/stage/bronze/BronzeMechanicalLatheBlockEntity.java");
const borer=read(base+"java/com/magneticraft2/common/blockentity/stage/bronze/BronzePrecisionBoringMachineBlockEntity.java");
const items=read(base+"java/com/magneticraft2/common/registry/registers/ItemRegistry.java");
const creative=read(base+"java/com/magneticraft2/common/registry/registers/CreativeTabRegistry.java");
const lang=json(base+"resources/assets/magneticraft2/lang/en_us.json");
const book=json(base+"resources/assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/ironage.json");
const recipe=json(base+"resources/data/magneticraft2/recipes/bore_precision_iron_toolholder.json");
const cutter=json(base+"resources/data/magneticraft2/recipes/iron_lathe_cutter.json");

assert.match(items,/ITEM_PRECISION_IRON_TOOLHOLDER\s*=\s*ITEMS\.register\("precision_iron_toolholder"/);
assert.match(items,/ITEM_IRON_LATHE_CUTTER\s*=\s*ITEMS\.register\("iron_lathe_cutter",\s*\(\)\s*->\s*new Item\(new Item\.Properties\(\)\.durability\(128\)\)\)/);
assert.match(items,/ITEM_LATHE_CUTTER\s*=\s*ITEMS\.register\("bronze_lathe_cutter",\s*\(\)\s*->\s*new Item\(new Item\.Properties\(\)\.durability\(64\)\)\)/);
assert.match(lathe,/held\.is\(ItemRegistry\.ITEM_LATHE_CUTTER\.get\(\)\)\s*\|\|\s*held\.is\(ItemRegistry\.ITEM_IRON_LATHE_CUTTER\.get\(\)\)/);
assert.match(lathe,/cutter\.getDamageValue\(\)\s*<\s*cutter\.getMaxDamage\(\)/);
assert.match(lathe,/int cutterCost = ironRecipe == null \? 1/);
assert.match(lathe,/ITEM_PRECISION_IRON_GEAR_LARGE\.get\(\)\) \? 3 : 2/);
assert.match(lathe,/tag\.put\("LatheCutter",cutter\.save\(new CompoundTag\(\)\)\)/);
assert.match(borer,/MultiblockRecipeHandler\.findRecipe\(level, MACHINE,/);
assert.match(borer,/ItemStack result = recipe\.getOutput\(\)/);
assert.match(borer,/if \(input\.hasTag\(\)\) result\.setTag\(input\.getTag\(\)\.copy\(\)\)/);
assert.match(borer,/boringBit\.completeBatch\(\)/);
assert.match(borer,/getMechanicalLoadState\(level, worldPosition\)\.supplied\(\)/);
assert.equal(recipe.type,"magneticraft2:multiblock_processing");
assert.equal(recipe.machine,"magneticraft2:bronze_precision_boring_machine");
assert.equal(recipe.input.item,"magneticraft2:wrought_iron_rod");
assert.equal(recipe.input_count,1);
assert.equal(recipe.output.item,"magneticraft2:precision_iron_toolholder");
assert.equal(recipe.output.count,1);
assert.equal(recipe.process_time,240);
assert.equal(recipe.mechanical.min_speed,20);
assert.equal(recipe.mechanical.torque,8);
assert.equal(cutter.type,"minecraft:crafting_shaped");
assert.deepEqual(cutter.pattern,[" F "," B "," H "]);
assert.equal(cutter.key.F.item,"minecraft:flint");
assert.equal(cutter.key.B.item,"magneticraft2:bronze_bushing");
assert.equal(cutter.key.H.item,"magneticraft2:precision_iron_toolholder");
assert.equal(cutter.result.item,"magneticraft2:iron_lathe_cutter");
for(const k of ["ITEM_PRECISION_IRON_TOOLHOLDER","ITEM_IRON_LATHE_CUTTER"])
  assert.match(creative,new RegExp("entries\\.accept\\(ItemRegistry\\."+k+"\\.get\\(\\)\\)"));
assert.equal(lang["item.magneticraft2.precision_iron_toolholder"],"Precision Wrought Iron Toolholder");
assert.equal(lang["item.magneticraft2.iron_lathe_cutter"],"Wrought Iron Lathe Cutter");
assert(book.pages.some(p=>p.item==="magneticraft2:precision_iron_toolholder"));
assert(book.pages.some(p=>p.recipe==="magneticraft2:iron_lathe_cutter"));

for(const name of ["precision_iron_toolholder","iron_lathe_cutter"]){
 const model=json(base+"resources/assets/magneticraft2/models/item/"+name+".json");
 assert.equal(model.parent,"minecraft:block/block");
 assert(model.elements.length>=4);
 assert.equal(model.textures.iron,"magneticraft2:block/material/dark_work_iron");
 for(const e of model.elements){
   const [x0,y0,z0]=e.from,[x1,y1,z1]=e.to;
   assert(x1>x0&&y1>y0&&z1>z0);
   const faceDimensions={north:[x1-x0,y1-y0],south:[x1-x0,y1-y0],
     east:[z1-z0,y1-y0],west:[z1-z0,y1-y0],
     up:[x1-x0,z1-z0],down:[x1-x0,z1-z0]};
   for(const [side,[w,h]] of Object.entries(faceDimensions)){
     const f=e.faces[side];
     assert(f,"Missing "+side+" face");
     assert.deepEqual([f.uv[2]-f.uv[0],f.uv[3]-f.uv[1]],[w,h],
       "Texture stretched on "+name+" "+side);
   }
 }
}
console.log("PASS: Iron precision tooling recipes, registries, maintenance, and 1:1 UV contracts");
