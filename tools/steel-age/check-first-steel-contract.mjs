// Contract smoke-test for three physically distinct Steel multiblocks,
// recipe balance, port placement, renderer palette and JEI/Forge wiring.
// Not a substitute for a Java compile or actual Minecraft process testing.
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import {fileURLToPath} from "node:url";
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),"../..");
const read=p=>fs.readFileSync(path.join(root,p),"utf8");
const json=p=>JSON.parse(read(p));
const base="src/main/resources/",data=base+"data/magneticraft2/",
  art=base+"assets/magneticraft2/";
const machines={
 steel_coke_oven:{dims:[3,4,3],input:1,air:false,slag:false},
 steel_blast_furnace:{dims:[5,5,5],input:3,air:true,slag:true},
 steel_refining_furnace:{dims:[5,5,5],input:1,air:true,slag:true}
};
const process={
 carbonize_coal_to_coke:"steel_coke_oven",
 reduce_magnetite_to_pig_iron:"steel_blast_furnace",
 decarburize_pig_iron_to_steel:"steel_refining_furnace"
};
const book=json(art+"patchouli_books/world_of_magneticraft/en_us/entries/steelage.json");
const toBook=(blueprint,dims)=>Array.from({length:dims[1]},(_,i)=>
 blueprint.structure.layout["layer"+(dims[1]-i)]
  .map(row=>[...row].reverse().map(c=>c==="A"?"_":c==="C"?"0":c).join("")));
for(const [id,info] of Object.entries(machines)){
 const south=json(data+"multiblocks/"+id+"_south.json");
 assert.deepEqual(south.structure.dimensions,info.dims);
 assert.equal(south.structure.blocks.C,"magneticraft2:"+id);
 assert.equal(south.structure.blocks.U,"magneticraft2:multiblock_item_input");
 assert.equal(south.structure.blocks.O,"magneticraft2:multiblock_item_primary_output");
 assert.equal(south.structure.blocks.F,"magneticraft2:iron_machine_foundation");
 for(const dir of ["south","north","east","west"]){
  const bp=json(data+"multiblocks/"+id+"_"+dir+".json");
  assert.equal(bp.name,id+"_"+dir);
  assert.deepEqual(bp.structure.dimensions,info.dims);
  assert.equal(bp.settings.voxel_profile,"magneticraft2:"+id);
  const count=(c)=>Object.values(bp.structure.layout).flat(2).filter(x=>x===c).length;
  assert.equal(count("C"),1);assert.equal(count("U"),1);assert.equal(count("O"),1);
  assert.equal(count("Y"),info.slag?1:0);
  const allowed=new Set(Object.keys(bp.structure.blocks));
  assert(Object.values(bp.structure.layout).flat(2).every(x=>allowed.has(x)));
  json(art+"blockstates/"+id+".json");
  json(art+"models/multiblock/"+id+"_"+dir+".json");
 }
 const page=book.pages.find(p=>p.type==="patchouli:multiblock"&&p.name===
  (id==="steel_coke_oven"?"Steel Coke Oven":id==="steel_blast_furnace"?"Steel Blast Furnace":"Steel Refining Furnace"));
 assert(page);
 assert.deepEqual(page.multiblock.pattern,toBook(south,info.dims));
 const profile=json(data+"multiblock_voxels/"+id+".json");
 assert.equal(profile.unit,"model_pixels");
 assert.deepEqual(profile.collision,profile.selection);
 const occupied=Object.values(south.structure.layout).flat(2).filter(x=>x!=="A").length;
 assert.equal(profile.collision.length,occupied+(info.air?
  (id==="steel_refining_furnace"?9:5):0));
 assert.deepEqual(south.settings.render_air_anchors,info.air?[[0,1,0]]:[]);
 assert(profile.collision.every(b=>b.every(n=>Number.isInteger(n))));
 const craft=json(data+"recipes/"+id+".json");
 assert.equal(craft.result.item,"magneticraft2:"+id);
}
for(const [name,machine]of Object.entries(process)){
 const r=json(data+"recipes/"+name+".json");
 assert.equal(r.type,"magneticraft2:steel_process");
 assert.equal(r.machine,"magneticraft2:"+machine);
 assert.equal(r.requires_air,machines[machine].air);
 assert.equal(r.inputs.length,machines[machine].input);
 assert.equal(Boolean(r.slag),machines[machine].slag);
}
const coke=json(data+"recipes/carbonize_coal_to_coke.json");
const blast=json(data+"recipes/reduce_magnetite_to_pig_iron.json");
const refine=json(data+"recipes/decarburize_pig_iron_to_steel.json");
assert.deepEqual(coke.inputs,[{item:"minecraft:coal",count:4}]);
assert.equal(coke.output.item,"magneticraft2:metallurgical_coke");
assert.equal(coke.output.count,4);
assert.equal(blast.inputs[1].item,"magneticraft2:metallurgical_coke");
assert.equal(blast.inputs[2].item,"minecraft:calcite");
assert.equal(blast.output.item,"magneticraft2:pig_iron");
assert.equal(refine.inputs[0].item,"magneticraft2:pig_iron");
assert.equal(refine.output.item,"magneticraft2:refined_steel_ingot");
assert.equal(blast.output.count,refine.inputs[0].count);
assert.equal(refine.output.count,refine.inputs[0].count);
const die=json(data+"recipes/steel_forge_die.json");
assert.equal(die.result.item,"magneticraft2:steel_forge_die");
const hammer=read("src/main/java/com/magneticraft2/common/blockentity/stage/bronze/BronzeForgeHammerBlockEntity.java");
assert.match(hammer,/ITEM_STEEL_FORGE_DIE::get, 1536/);
for(const item of ["metallurgical_coke","pig_iron","refined_steel_ingot","steel_forge_die"])
 json(art+"models/item/"+item+".json");
const furnace=read("src/main/java/com/magneticraft2/common/blockentity/stage/steel/SteelProcessBlockEntity.java");
assert.match(furnace,/getSideTuyereCapability/);
assert.match(furnace,/outputFits\(product,slag\)/);
assert.match(furnace,/pressureHandler\.extractPressure\(1,false\)/);
assert.match(furnace,/fuel\.shrink\(1\)/);
const registry=read("src/main/java/com/magneticraft2/common/registry/registers/RecipesRegistry.java");
assert.match(registry,/steel_process/);
const mod=read("src/main/java/com/magneticraft2/common/blockentity/stage/copper/MultiblockItemPortBlockEntity.java");
assert.match(mod,/controller instanceof SteelProcessBlockEntity/);
const jei=read("src/main/java/com/magneticraft2/compatibility/JEI/MagneticraftJeiPlugin.java");
assert.match(jei,/getAllRecipesFor\(SteelProcessRecipe.Type.INSTANCE\)/);
// Distinguish quantity changes from material-identity changes. Topping up
// inputs must not restart progress; amount changes must sync to the client.
assert.match(furnace,/boolean different=!ItemStack\.matches\(/);
assert.match(furnace,/different\|=!ItemStack\.matches\(/);
assert.equal((furnace.match(/lastInputs\[i\]=itemHandler\.getStackInSlot\(SteelProcessRecipe\.INPUT_SLOTS\[i\]\)\.copy\(\);/g)||[]).length,2);
assert.match(furnace,/ItemStack\.isSameItemSameTags\(now,activeInputs\[i\]\)/);
// Steel side AIR is an intake, never a public source of free AIR. The
// controller can internally burn one AIR charge per 5 supplied ticks.
assert.match(furnace,/if \(cap == CapabilityPressure\.PRESSURE\) return airInlet\.cast\(\);/);
assert.match(furnace,/private IPressureStorage newAirInlet\(\)/);
assert.match(furnace,/int extractPressure\(int units, boolean simulate\) \{/);
assert.match(furnace,/private LazyOptional<IPressureStorage> airInlet/);
assert.match(furnace,/airInlet\.invalidate\(\)/);
assert.match(furnace,/airInlet = LazyOptional\.of\(this::newAirInlet\)/);
console.log("PASS Steel first loop: validated 12 formations, materials, AIR, physical ports, tool, JEI and book");
