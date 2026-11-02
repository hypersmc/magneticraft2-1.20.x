#!/usr/bin/env node
// Static contract checks only. This does NOT replace ./gradlew build or in-game tests.
import fs from "node:fs";
import path from "node:path";
import assert from "node:assert/strict";
const root=process.cwd();
const json=p=>JSON.parse(fs.readFileSync(path.join(root,p),"utf8"));
const source=p=>fs.readFileSync(path.join(root,p),"utf8");
const dir="src/main/resources/data/magneticraft2/";
const baseBlueprint=json(dir+"multiblocks/bloomery_furnace_south.json");
const layers=Array.from({length:5},(_,y)=>baseBlueprint.structure.layout["layer"+(y+1)]);
const faces=["south","west","north","east"];
function rotate(x,z,n){for(let i=0;i<n;i++){const nextX=4-z;z=x;x=nextX;}return[x,z];}
let occupied=0;
for(let n=0;n<faces.length;n++){
 const d=faces[n],blueprint=json(dir+"multiblocks/bloomery_furnace_"+d+".json");
 assert.deepEqual(blueprint.structure.dimensions,[5,5,5]);
 assert.equal(blueprint.settings.voxel_profile,"magneticraft2:bloomery_furnace");
 const counts={C:0,U:0,O:0,Y:0};
 for(let y=0;y<5;y++)for(let z=0;z<5;z++)for(let x=0;x<5;x++){
   const [xx,zz]=rotate(x,z,n);
   const symbol=blueprint.structure.layout["layer"+(y+1)][zz][xx];
   assert.equal(symbol,layers[y][z][x],d+" rotated structure "+x+","+y+","+z);
   if(symbol in counts)counts[symbol]++;
 }
 for(const key of Object.keys(counts))assert.equal(counts[key],1,d+" must have exactly one "+key);
 assert.equal(blueprint.structure.layout.layer5[2][2],"A");
 const states=json("src/main/resources/assets/magneticraft2/blockstates/bloomery_furnace.json");
 assert.ok(states.variants["facing="+d+",is_formed=true"]);
 assert.ok(states.variants["facing="+d+",is_formed=false"]);
 json("src/main/resources/assets/magneticraft2/models/multiblock/bloomery_furnace_"+d+".json");
}
const voxel=json(dir+"multiblock_voxels/bloomery_furnace.json");
for(let y=0;y<5;y++)for(let z=0;z<5;z++)for(let x=0;x<5;x++)
 if(layers[y][z][x]!=="A"){
  occupied++;
  const box=[(x-2)*16,y*16,(z-4)*16,(x-1)*16,(y+1)*16,(z-3)*16];
  assert.ok(voxel.collision.some(b=>JSON.stringify(b)===JSON.stringify(box)),
          "missing collision voxel "+x+","+y+","+z);
 }
assert.equal(voxel.collision.length,occupied);
const book=json("src/main/resources/assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json");
const shown=book.pages.find(page=>page.type==="patchouli:multiblock" && page.name==="Bloomery Furnace");
assert.ok(shown,"Bloomery construction page missing");
for(let y=0;y<5;y++)for(let z=0;z<5;z++){
 const expected=layers[4-y][z].map(c=>c==="C"?"0":c==="A"?"_":c).join("");
 assert.equal(shown.multiblock.pattern[y][z],expected,"book/build mismatch");
}
const recipe=json(dir+"recipes/bloomery_magnetite_reduction.json");
assert.equal(recipe.type,"magneticraft2:bloomery_reduction");
assert.equal(recipe.input.item,"magneticraft2:magnetite_concentrate");
assert.equal(recipe.input_count,4);
assert.equal(recipe.flux.item,"minecraft:calcite");
assert.equal(recipe.flux_count,1);
assert.equal(recipe.output.item,"magneticraft2:iron_bloom");
assert.equal(recipe.slag.item,"magneticraft2:slagitem");
for(const ore of ["magnetite_ore","magnetite_deepslate_ore"]){
 const r=json(dir+"recipes/bronze_stamping_"+ore+".json");
 assert.equal(r.input.item,"magneticraft2:"+ore);
 assert.equal(r.output.item,"magneticraft2:crushed_magnetite");
 assert.equal(r.input_count,r.output.count);
}
const wash=json(dir+"recipes/mechanical_washing_magnetite.json");
assert.equal(wash.fluid_input.fluid,"minecraft:water");
assert.equal(wash.fluid_output.fluid,"magneticraft2:dirty_water");
const sift=json(dir+"recipes/mechanical_sifting_magnetite.json");
assert.equal(sift.input.item,"magneticraft2:washed_magnetite");
assert.equal(sift.output.item,"magneticraft2:magnetite_concentrate");
// Formation geometry evidence: for the north-facing layout, the first
// block one step behind the byproduct port is required Stone Bricks.
// This avoids treating a visually recessed module as a blueprint AIR cell.
const north=json(dir+"multiblocks/bloomery_furnace_north.json");
assert.equal(north.structure.layout.layer1[0][0],"Y");
assert.equal(north.structure.layout.layer1[1][0],"S");
const diagnosticsBlock=source("src/main/java/com/magneticraft2/common/block/stage/bronze/BloomeryFurnaceBlock.java");
assert.ok(diagnosticsBlock.includes("tank.reportFormationFailure(player)"));
const be=source("src/main/java/com/magneticraft2/common/blockentity/stage/bronze/BloomeryFurnaceBlockEntity.java");
assert.ok(be.includes("public void reportFormationFailure(Player player)"));
assert.ok(be.includes("Closest layout is "));

for(const required of [
 "if (level == null || !formed) return;",
 "held.is(Items.CHARCOAL)",
 "heat>=IGNITION_THRESHOLD+HEAT_COST",
 "pressureHandler.extractPressure(1,false)",
 "BloomeryRecipe.SLAG_SLOT,slag,true",
 "BloomeryRecipe.FLUX_SLOT,recipe.fluxCount(),false",
 "side == matchedFacing",
 "saveMultiblockData",
 "BloomeryAirStep"
]) assert.ok(be.includes(required),"missing Bloomery guard: "+required);
assert.ok(!be.includes("SteamNetworkManager"));
const port=source("src/main/java/com/magneticraft2/common/blockentity/stage/copper/MultiblockItemPortBlockEntity.java");
assert.ok(port.includes("controller instanceof BloomeryFurnaceBlockEntity"));
assert.ok(port.includes("slot == 1 ? 3"));
console.log("Bloomery K1 static contract: PASS ("+faces.length+" facings, "+occupied+" voxel cells, first Magnetite route).");
console.log("NOT TESTED: Forge compilation, recipe registry sync, game rendering, saved worlds, dual clients.");
