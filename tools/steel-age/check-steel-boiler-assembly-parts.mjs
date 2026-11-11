import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),"../..");
const read=p=>fs.readFileSync(path.join(root,p),"utf8");
const json=p=>JSON.parse(read(p));
const res="src/main/resources/", data=res+"data/magneticraft2/", assets=res+"assets/magneticraft2/";
const classes="src/main/java/com/magneticraft2/common/registry/registers/";
const blocks=read(classes+"BlockRegistry.java"), items=read(classes+"ItemRegistry.java"),creative=read(classes+"CreativeTabRegistry.java");
const modelClass=read("src/main/java/com/magneticraft2/common/block/stage/steel/SteelBoilerStructuralBlock.java");
const bit=read(classes+"BlockRegistry.java");
const pickaxe=json(res+"data/minecraft/tags/blocks/mineable/pickaxe.json");
const lang=json(assets+"lang/en_us.json");
const book=json(assets+"patchouli_books/world_of_magneticraft/en_us/entries/steelage.json");
const expected={
 boiler_shell_panel:[["boiler_shell_section",1],["refined_steel_rod",1],["iron_rivet",4],["riveted_iron_frame",1]],
 boiler_end_joint:[["boiler_end_plate",1],["boiler_reinforcement_flange",1],["pressure_pipe_coupling",1],["iron_rivet",4]]
};
for(const [id,parts] of Object.entries(expected)){
 const symbol=id.toUpperCase();
 assert(blocks.includes("BLOCKS.register(\""+id+"\""),"block not registered "+id);
 assert(items.includes("ITEM_"+symbol+" = fromBlock("),"block item not registered "+id);
 assert(creative.includes("ItemRegistry.ITEM_"+symbol+".get()"),"creative missing "+id);
 assert(pickaxe.values.includes("magneticraft2:"+id),"pickaxe missing "+id);
 assert(lang["block.magneticraft2."+id],"translation missing "+id);
 const recipe=json(data+"recipes/assembly_"+id+".json");
 assert.equal(recipe.type,"magneticraft2:mechanical_assembly");
 assert.equal(recipe.ingredients.length,4);
 recipe.ingredients.forEach((v,i)=>{assert.equal(v.ingredient.item,"magneticraft2:"+parts[i][0]);assert.equal(v.count,parts[i][1]);});
 assert.equal(recipe.result.item,"magneticraft2:"+id);
 assert.equal(recipe.result.count,1);
 assert.equal(recipe.process_time,320);assert.equal(recipe.mechanical.min_speed,18);
 assert.equal(recipe.mechanical.torque,10);
 const state=json(assets+"blockstates/"+id+".json");
 for(const dir of ["north","east","south","west"])assert.equal(state.variants["facing="+dir].model,"magneticraft2:block/"+id);
 const itemModel=json(assets+"models/item/"+id+".json");
 assert.equal(itemModel.parent,"magneticraft2:block/"+id);
 const blockModel=json(assets+"models/block/"+id+".json");
 assert(blockModel.elements.length>1);
 for(const e of blockModel.elements){
  assert(e.from.every((v,i)=>Number.isFinite(v)&&v>=0&&v<e.to[i]&&e.to[i]<=16));
  const dim=e.to.map((v,i)=>v-e.from[i]);
  const sides={north:[dim[0],dim[1]],south:[dim[0],dim[1]],east:[dim[2],dim[1]],west:[dim[2],dim[1]],up:[dim[0],dim[2]],down:[dim[0],dim[2]]};
  for(const [side,size] of Object.entries(sides))assert.deepEqual(e.faces[side].uv,[0,0,...size],"UV stretching "+id+" "+side);
 }
 const loot=json(data+"loot_tables/blocks/"+id+".json");
 assert.equal(loot.pools[0].entries[0].name,"magneticraft2:"+id);
}
assert(modelClass.includes("getCollisionShape")&&modelClass.includes("getShape"),"physical shape missing");
assert(modelClass.includes("HorizontalDirectionalBlock"),"directional placement missing");
assert(book.pages.some(p=>p.text?.includes("Steel IV-C: Physical Boiler Pieces")));
assert(!modelClass.includes("SteamNetworkManager")&&!modelClass.includes("serverTick"),"Steam must remain deferred");
console.log("PASS Steel IV-C partial source contract: two four-slot subassemblies, registries, orientation, UV, loot, book and pickaxe.");
console.log("NOT a Forge compile, real vessel multiblock, or in-game acceptance.");
