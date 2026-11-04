#!/usr/bin/env node
/**
 * Bronze Air Duct K1 contract. This checks repository invariants only;
 * it is NOT a Forge compile, gas-flow benchmark or in-game verification.
 */
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import {fileURLToPath} from "node:url";
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),"../..");
const read=p=>fs.readFileSync(path.join(root,p),"utf8"),json=p=>JSON.parse(read(p));
const code="src/main/java/com/magneticraft2/";
const assets="src/main/resources/assets/magneticraft2/";
const data="src/main/resources/data/magneticraft2/";
const duct=read(code+"common/block/stage/bronze/BronzeAirDuctBlock.java");
const graph=read(code+"common/systems/PRESSURE/AirDuctNetwork.java");
const blower=read(code+"common/blockentity/stage/bronze/BronzeIndustrialAirBlowerBlockEntity.java");
const roaster=read(code+"common/blockentity/stage/bronze/OreRoastingKilnBlockEntity.java");
const liquid=read(code+"common/systems/fluid/FluidPipeNetwork.java");
const blocks=read(code+"common/registry/registers/BlockRegistry.java");
const items=read(code+"common/registry/registers/ItemRegistry.java");
const creative=read(code+"common/registry/registers/CreativeTabRegistry.java");
assert.match(duct,/class BronzeAirDuctBlock extends Block/);
assert.doesNotMatch(duct,/extends WaterPipeBlock/);
assert.match(duct,/getCapability\(\s*CapabilityPressure\.PRESSURE, direction\.getOpposite\(\)\)/);
assert.doesNotMatch(duct,/ForgeCapabilities\.FLUID_HANDLER/);
assert.match(duct,/instanceof BronzeAirDuctBlock/);
assert.match(duct,/!bellows\.isFormedModule\(\)/);
for(const d of ["NORTH","EAST","SOUTH","WEST","UP","DOWN"])
 assert.ok(duct.includes("public static final BooleanProperty "+d),"Duct missing "+d);
assert.match(graph,/public static final int MAX_DUCTS = 128/);
assert.match(graph,/while \(!queue\.isEmpty\(\) && visited\.size\(\) < MAX_DUCTS\)/);
assert.match(graph,/!level\.hasChunkAt\(neighbour\)/);
assert.match(graph,/!level\.hasChunkAt\(firstDuct\)/);
assert.match(graph,/receiverPositions\.contains\(neighbour\)/);
assert.match(graph,/getCapability\(CapabilityPressure\.PRESSURE, inputFace\)/);
assert.doesNotMatch(graph,/ForgeCapabilities\.FLUID_HANDLER/);
assert.doesNotMatch(graph,/getCapability\(CapabilityPressure\.PRESSURE, null\)/);
assert.doesNotMatch(graph,/BlockEntityTicker/);
assert.match(graph,/!bellows\.isFormedModule\(\)/);
assert.match(blower,/AirDuctNetwork\.findReceivers\(level, next, worldPosition\)/);
assert.match(blower,/level\.getGameTime\(\) % PUMP_INTERVAL != 0/);
assert.match(blower,/Math\.floorMod\(nextReceiver \+ i, size\)/);
assert.match(blower,/nextReceiver = \(index \+ 1\) % size/);
assert.match(blower,/lastOutputTick == tick \? outputThisTick : 0/);
assert.match(blower,/getCapability\(CapabilityPressure\.PRESSURE, matchedFacing\.getOpposite\(\)\)/);
assert.match(roaster,/formed && side == matchedFacing/);
assert.match(roaster,/poweredAirTicks/);
assert.match(liquid,/BlockRegistry\.WATER_PIPE/);
assert.doesNotMatch(liquid,/BRONZE_AIR_DUCT/);
assert.match(blocks,/BRONZE_AIR_DUCT = BLOCKS\.register\("bronze_air_duct", BronzeAirDuctBlock::new\)/);
assert.match(items,/ITEM_BRONZE_AIR_DUCT = fromBlock\(BRONZE_AIR_DUCT\)/);
assert.match(creative,/ITEM_BRONZE_AIR_DUCT/);
const recipe=json(data+"recipes/bronze_air_duct.json");
assert.equal(recipe.result.item,"magneticraft2:bronze_air_duct");
assert.equal(recipe.result.count,8);
assert.equal(json(data+"loot_tables/blocks/bronze_air_duct.json").pools[0].entries[0].name,
 "magneticraft2:bronze_air_duct");
for(const tag of ["mineable/pickaxe","needs_stone_tool"])
 assert.ok(json("src/main/resources/data/minecraft/tags/blocks/"+tag+".json").values.includes("magneticraft2:bronze_air_duct"));
const state=json(assets+"blockstates/bronze_air_duct.json");
assert.equal(state.multipart.length,7);
const sides=["north","south","east","west","up","down"];
for(const side of sides)
 assert.ok(state.multipart.some(part=>part.when?.[side]==="true"
   &&part.apply.model==="magneticraft2:block/bronze_air_duct_end_"+side));
assert.equal(state.multipart[0].apply.model,"magneticraft2:block/bronze_air_duct_center");
const faces={north:[0,1],south:[0,1],east:[2,1],west:[2,1],up:[0,2],down:[0,2]};
for(const name of ["center",...sides.map(d=>"end_"+d)]){
 const model=json(assets+"models/block/bronze_air_duct_"+name+".json");
 for(const element of model.elements){
  for(const [side,face]of Object.entries(element.faces)){
   const [u,v]=faces[side];
   assert.equal(face.uv[2]-face.uv[0],element.to[u]-element.from[u],"Stretched X UV "+name);
   assert.equal(face.uv[3]-face.uv[1],element.to[v]-element.from[v],"Stretched Y UV "+name);
   assert.ok(face.uv.every(n=>n>=0&&n<=16),"Outside 16px UV tile "+name);
   assert.ok(model.textures[face.texture.slice(1)],"Missing texture alias "+name);
  }
 }
}
assert.equal(json(assets+"models/item/bronze_air_duct.json").parent,
 "magneticraft2:block/bronze_air_duct_center");
const book=json(assets+"patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json");
assert.ok(book.pages.some(page=>page.recipe==="magneticraft2:bronze_air_duct"));
assert.ok(book.pages.some(page=>page.text?.includes("Air Distribution K1")));
const lang=json(assets+"lang/en_us.json");
assert.equal(lang["block.magneticraft2.bronze_air_duct"],"Bronze Air Duct");
console.log("Bronze air duct static contract PASS: 7 native-UV models, no fluid coupling, bounded pressure graph, real directional ports and round-robin supply. NOT COMPILED.");
