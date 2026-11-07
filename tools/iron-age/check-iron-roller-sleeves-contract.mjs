// Focused source/resource contract. Run in a complete repository checkout.
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import path from "node:path";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const read = p => readFileSync(path.join(ROOT, p), "utf8");
const mill = read("src/main/java/com/magneticraft2/common/blockentity/stage/bronze/BronzeRollingMillBlockEntity.java");
const registry = read("src/main/java/com/magneticraft2/common/registry/registers/ItemRegistry.java");
const tabs = read("src/main/java/com/magneticraft2/common/registry/registers/CreativeTabRegistry.java");
const maintenance = read("src/main/java/com/magneticraft2/common/systems/maintenance/ReplaceableMachinePart.java");
const recipe = JSON.parse(read("src/main/resources/data/magneticraft2/recipes/wrought_iron_roller_sleeves.json"));
const model = JSON.parse(read("src/main/resources/assets/magneticraft2/models/item/wrought_iron_roller_sleeves.json"));
const lang = JSON.parse(read("src/main/resources/assets/magneticraft2/lang/en_us.json"));
const book = JSON.parse(read("src/main/resources/assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/ironage.json"));

assert.match(mill, /new ReplaceableMachinePart\(ItemRegistry\.ITEM_BRONZE_ROLLER_SLEEVES::get, 512,\s*new ReplaceableMachinePart\.Alternative\(\s*ItemRegistry\.ITEM_WROUGHT_IRON_ROLLER_SLEEVES::get, 1024\)/);
assert.match(mill, /rollerSleeves\.load\(tag\.getCompound\("MillRollers"\)\)/);
assert.match(mill, /tag\.put\("MillRollers", rollerSleeves\.save\(\)\)/);
assert.match(mill, /rollerSleeves\.completeBatch\(\)/);
assert.match(maintenance, /private static final String INSTALLED = "InstalledPart"/);
assert.match(maintenance, /private static final String REMAINING = "RemainingCycles"/);
assert.match(maintenance, /tag\.contains\(INSTALLED, Tag\.TAG_STRING\)/);
assert.match(registry, /ITEM_WROUGHT_IRON_ROLLER_SLEEVES/);
assert.match(tabs, /entries\.accept\(ItemRegistry\.ITEM_WROUGHT_IRON_ROLLER_SLEEVES\.get\(\)\)/);
assert.equal(lang["item.magneticraft2.wrought_iron_roller_sleeves"], "Wrought Iron Roller Sleeves");
assert.equal(recipe.type, "minecraft:crafting_shaped");
assert.deepEqual(recipe.pattern, ["I I","BPB","I I"]);
assert.equal(recipe.key.I.item, "magneticraft2:wrought_iron_bar");
assert.equal(recipe.key.B.item, "magneticraft2:bronze_bushing");
assert.equal(recipe.key.P.item, "minecraft:polished_blackstone");
assert.equal(recipe.result.item, "magneticraft2:wrought_iron_roller_sleeves");
assert.equal(recipe.result.count, 1);
assert(book.pages.some(p=>p.recipe==="magneticraft2:wrought_iron_roller_sleeves"));
assert.equal(model.parent,"minecraft:block/block");
assert.equal(model.elements.length,16);
for(const element of model.elements) {
  const [x0,y0,z0]=element.from,[x1,y1,z1]=element.to;
  assert(x1>x0 && y1>y0 && z1>z0);
  const expected={north:[x1-x0,y1-y0],south:[x1-x0,y1-y0],
    east:[z1-z0,y1-y0],west:[z1-z0,y1-y0],
    up:[x1-x0,z1-z0],down:[x1-x0,z1-z0]};
  for(const [face,size] of Object.entries(expected)) {
    const current=element.faces[face];
    assert(current,"Missing face "+face);
    assert.deepEqual([current.uv[2]-current.uv[0],current.uv[3]-current.uv[1]],size,"Stretched texture on "+face);
  }
}
console.log("PASS: Wrought Iron Roller Sleeves source, recipe, save contract, book and native UVs");
