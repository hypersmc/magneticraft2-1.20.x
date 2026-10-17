#!/usr/bin/env node
/**
 * Offline asset and source-contract checks for the first GUI-less casting loop.
 * This does NOT run Forge/Gradle or prove Java gameplay correctness.
 * Run: node tools/bronze-casting/test-contract.mjs
 */
import fs from "node:fs";
import assert from "node:assert/strict";
const root=process.cwd();
const file=p=>fs.readFileSync(root+"/"+p,"utf8");
const json=p=>JSON.parse(file(p));
const pre="src/main/resources/";
const asset=pre+"assets/magneticraft2/";
const data=pre+"data/magneticraft2/";
const java="src/main/java/com/magneticraft2/";

const names=["bronze_pouring_ladle","bronze_casting_basin","rough_bronze_gear","rough_bronze_bearing"];
const items=file(java+"common/registry/registers/ItemRegistry.java");
const blocks=file(java+"common/registry/registers/BlockRegistry.java");
const entities=file(java+"common/registry/registers/BlockEntityRegistry.java");
const client=file(java+"client/Clientsetup.java");
const foundry=file(java+"common/blockentity/stage/bronze/BronzeFoundryThermalCore.java");
const ladle=file(java+"common/item/stage/bronze/BronzePouringLadleItem.java");
const basin=file(java+"common/blockentity/stage/bronze/BronzeCastingBasinBlockEntity.java");
const config=file(java+"common/utils/Magneticraft2ConfigCommon.java");

for(const name of names)assert(items.includes('"'+name+'"'),"Missing registered item "+name);
assert(blocks.includes('BLOCKS.register("bronze_casting_basin"'));
assert(entities.includes('BLOCK_ENTITIES.register("bronze_casting_basin"'));
assert(client.includes("BronzeCastingBasinRenderer::new"));
assert(client.includes('new ResourceLocation(MOD_ID, "filled")'));

for(const p of ["bronze_pouring_ladle","bronze_casting_basin"]){
 const recipe=json(data+"recipes/"+p+".json");
 assert.equal(recipe.type,"minecraft:crafting_shaped");
 assert.equal(recipe.result.item,"magneticraft2:"+p);
}
for(const p of names){
 const model=json(asset+"models/item/"+p+".json");
 assert(model.parent,"Missing item model parent "+p);
}
const ladleModel=json(asset+"models/item/bronze_pouring_ladle.json");
assert(ladleModel.overrides.some(x=>x.predicate?.["magneticraft2:filled"]===1.0));
json(asset+"models/item/bronze_pouring_ladle_filled.json");
const blockstate=json(asset+"blockstates/bronze_casting_basin.json");
for(const dir of ["north","south","east","west"])assert(blockstate.variants["facing="+dir]);
const loot=json(data+"loot_tables/blocks/bronze_casting_basin.json");
assert(loot.pools[0].entries[0].name==="magneticraft2:bronze_casting_basin");

const models=[
 "models/block/bronze_casting_basin.json",
 "models/item/bronze_pouring_ladle.json",
 "models/item/bronze_pouring_ladle_filled.json",
 "models/item/rough_bronze_gear.json",
 "models/item/rough_bronze_bearing.json"
];
for(const path of models){
 const model=json(asset+path);
 for(const el of model.elements||[]){
  // Vanilla 1.20.1 models allow -16..32 model pixels. The Basin's
  // visible front fitting projects 0.6px past the nominal 16px block.
  for(const v of [...el.from,...el.to])assert(v>=-16&&v<=32,path+": element out of bounds");
  const [x,y,z]=el.to.map((v,i)=>v-el.from[i]);
  for(const [side,f] of Object.entries(el.faces)){
   const dims=side==="up"||side==="down"?[x,z]:
      side==="east"||side==="west"?[z,y]:[x,y];
   assert(f.uv.every(v=>v>=0&&v<=16),path+": invalid UV");
   assert(Math.abs((f.uv[2]-f.uv[0])-dims[0])<1e-5
      &&Math.abs((f.uv[3]-f.uv[1])-dims[1])<1e-5,path+": stretched/compressed UV "+side);
  }
 }
}
// Guard the conservation design in the actual Java source. Gameplay and NBT
// still need to be validated in a Minecraft server, not this script.
assert(foundry.includes("BronzePouringLadleItem.fill(held, original, world)"));
assert(foundry.includes("crucibleUnits--;"));
assert(foundry.includes("if (crucibleUnits == 0)"));
assert(ladle.includes("getGameTime() - info.getLong(LOADED_AT)"));
assert(ladle.includes("world.dimension().location().toString()"));
assert(ladle.includes("ItemStack metal = storedIngot(ladle).copy();"));
assert(ladle.includes("clear(ladle);"));
assert(basin.includes("BronzePouringLadleItem.isHot(held,level)"));
assert(basin.includes("BronzePouringLadleItem.takeHot(held,level)"));
assert(basin.includes("mold=ItemStack.EMPTY;"));
assert(basin.includes("created.setTag(basin.sourceBronze.getTag().copy());"));
assert(basin.includes("basin.sourceBronze=ItemStack.EMPTY;"));
assert(basin.includes("if (!c.isEmpty()) Block.popResource(level,worldPosition,c);"));
assert(config.includes("FoundryLadleHotTicks"));
assert(config.includes("FoundryCastingCoolTicks"));
const guide=json(asset+"patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json");
for(const id of ["bronze_pouring_ladle","bronze_casting_basin"])
 assert(guide.pages.some(p=>p.recipe==="magneticraft2:"+id),"Missing guidebook recipe "+id);
console.log("Bronze casting asset/source contract: PASS");
console.log("Separate tests needed: ./gradlew compileJava, in-game pour/cool/quality/break/reload.");
