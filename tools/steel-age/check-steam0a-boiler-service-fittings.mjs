import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import {fileURLToPath} from "node:url";
const ROOT=path.resolve(path.dirname(fileURLToPath(import.meta.url)),"../..");
const read=p=>fs.readFileSync(path.join(ROOT,p),"utf8"),json=p=>JSON.parse(read(p));
const R="src/main/resources/",D=R+"data/magneticraft2/",A=R+"assets/magneticraft2/";
const J="src/main/java/com/magneticraft2/common/";
const roles=[{"name":"boiler_water_inlet","enum":"WATER","kind":"water","off":[2,2,-2],"recipe":[["pressure_pipe_coupling",1],["iron_plate_panel",1],["refined_steel_rod",1],["iron_rivet",2]]},{"name":"boiler_steam_outlet","enum":"STEAM","kind":"steam","off":[0,4,-2],"recipe":[["pressure_pipe_coupling",1],["boiler_reinforcement_flange",1],["refined_steel_plate",1],["iron_rivet",2]]},{"name":"boiler_firebox_mount","enum":"FIREBOX","kind":"firebox","off":[0,0,-2],"recipe":[["riveted_iron_casing",1],["fired_refractory_ceramic",1],["refined_steel_plate",1],["iron_rivet",4]]},{"name":"boiler_safety_mount","enum":"SAFETY","kind":"safety","off":[0,4,-1],"recipe":[["boiler_reinforcement_flange",1],["pressure_pipe_coupling",1],["refined_steel_rod",1],["iron_rivet",2]]}];
const reg=read(J+"registry/registers/BlockRegistry.java"),
  item=read(J+"registry/registers/ItemRegistry.java"),
  tab=read(J+"registry/registers/CreativeTabRegistry.java"),
  cls=read(J+"block/stage/steam/BoilerServiceFittingBlock.java");
const lang=json(A+"lang/en_us.json"),tag=json(R+"data/minecraft/tags/blocks/mineable/pickaxe.json");
assert(cls.includes("getStateForPlacement(")&&cls.includes("IS_FORMED")&&cls.includes("multiblockfiller"));
assert(!cls.includes("getCapability(")&&!cls.includes("SteamNetworkManager.")&&!cls.includes("serverTick("));
const facing=["south","east","north","west"],turn={south:0,east:1,north:2,west:3};
const rotate=(b,n)=>{let [x,y,z,X,Y,Z]=b;for(let k=0;k<n;k++)[x,X,z,Z]=[16-Z,16-z,x,X];return[x,y,z,X,Y,Z]};
for(const r of roles){
 const ID=r.name.toUpperCase();
 assert(reg.includes('BLOCKS.register("'+r.name+'"'));
 assert(item.includes("ITEM_"+ID+" = fromBlock("+ID+")"));
 assert(tab.includes("ITEM_"+ID+".get()"));
 assert(lang["block.magneticraft2."+r.name]);
 assert(tag.values.includes("magneticraft2:"+r.name));
 const recipe=json(D+"recipes/steam0a_assembly_"+r.name+".json");
 assert.equal(recipe.type,"magneticraft2:mechanical_assembly");
 assert.equal(recipe.ingredients.length,4);
 assert.deepEqual(recipe.ingredients.map(x=>[x.ingredient.item,x.count]),r.recipe.map(([id,n])=>["magneticraft2:"+id,n]));
 assert.equal(recipe.result.item,"magneticraft2:"+r.name);
 assert.equal(recipe.result.count,1);
 assert.equal(recipe.process_time,320);assert.equal(recipe.mechanical.min_speed,18);
 assert.equal(recipe.mechanical.torque,10);
 const state=json(A+"blockstates/"+r.name+".json");
 const base=json(A+"models/block/"+r.name+"_south.json");
 assert.equal(json(A+"models/item/"+r.name+".json").parent,"magneticraft2:block/"+r.name+"_south");
 for(const face of facing){
   const m=json(A+"models/block/"+r.name+"_"+face+".json");
   assert.equal(state.variants["facing="+face].model,"magneticraft2:block/"+r.name+"_"+face);
   assert.deepEqual(m.elements.map(e=>[...e.from,...e.to]),
       base.elements.map(e=>rotate([...e.from,...e.to],turn[face])));
   for(const e of m.elements){
     const [dx,dy,dz]=e.to.map((v,i)=>v-e.from[i]);
     assert(e.from.every((v,i)=>Number.isFinite(v)&&v>=0&&e.to[i]<=16&&e.to[i]>v));
     const size={north:[dx,dy],south:[dx,dy],east:[dz,dy],west:[dz,dy],up:[dx,dz],down:[dx,dz]};
     for(const [dir,s] of Object.entries(size)) assert.deepEqual(e.faces[dir].uv,[0,0,...s]);
   }
 }
 const loot=json(D+"loot_tables/blocks/"+r.name+".json");
 assert.equal(loot.pools[0].entries[0].name,"magneticraft2:"+r.name);
}
console.log("PASS Steam 0-A: 4 inert fittings, 4-way models/UV, orientation, assembly, drops and registrations.");
console.log("Static contract only. Gradle, Minecraft runtime, physical support and multiplayer unverified.");
