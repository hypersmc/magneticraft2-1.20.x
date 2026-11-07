#!/usr/bin/env node
// Static model/source/recipe contract; not a Gradle build or runtime test.
import assert from "node:assert/strict";
import fs from "node:fs";
const R="src/main/resources/",J="src/main/java/com/magneticraft2/";
const json=p=>JSON.parse(fs.readFileSync(R+p,"utf8"));
const java=p=>fs.readFileSync(J+p,"utf8");
const id="iron_forge_hearth",item="magneticraft2:"+id;
const block=java("common/block/stage/iron/IronForgeHearthBlock.java");
const be=java("common/blockentity/stage/iron/IronForgeHearthBlockEntity.java");
assert.ok(block.includes("import com.magneticraft2.common.blockentity.stage.iron.IronForgeHearthBlockEntity;"));
assert.ok(block.includes("IRON_FORGE_HEARTH_BE"));
assert.ok(be.includes("getInputCount() == 4"));
assert.ok(be.includes("recipe.getOutput().getCount() == 4"));
assert.ok(be.includes("HotBloomWorkpiece.heatInKiln(candidate"));
assert.ok(be.includes("!candidate.is(recipe.getOutput().getItem())"));
assert.ok(be.includes("HearthHeatCapacity"));
assert.ok(be.includes("if (!player.getInventory().add(returned) && !returned.isEmpty())"));
for(const [file,symbol] of [["common/registry/registers/BlockRegistry.java",'BLOCKS.register("iron_forge_hearth"'],["common/registry/registers/BlockEntityRegistry.java",'BLOCK_ENTITIES.register("iron_forge_hearth"'],["common/registry/registers/ItemRegistry.java","ITEM_IRON_FORGE_HEARTH"]]) assert.ok(java(file).includes(symbol));
assert.ok(java("common/blockentity/stage/copper/MultiblockItemPortBlockEntity.java").includes("instanceof IronForgeHearthBlockEntity hearth"));
const hot=java("common/systems/material/HotBloomWorkpiece.java");
for(const recipe of ["hearth_reheat_wrought_iron_ingots","hearth_reheat_wrought_iron_bars"])assert.ok(hot.includes(recipe));
assert.ok(json("data/minecraft/tags/blocks/mineable/pickaxe.json").values.includes(item));
const crafting=json("data/magneticraft2/recipes/iron_forge_hearth.json");
assert.equal(crafting.result.item,item);
assert.deepEqual(crafting.pattern,["PBP","CFC","PBP"]);
for(const [suffix,source] of [["ingots","wrought_iron_ingot"],["bars","wrought_iron_bar"]]){
const rec=json("data/magneticraft2/recipes/hearth_reheat_wrought_iron_"+suffix+".json");
assert.equal(rec.type,"magneticraft2:multiblock_processing");
assert.equal(rec.machine,item);
assert.equal(rec.input.item,"magneticraft2:"+source);
assert.equal(rec.output.item,"magneticraft2:"+source);
assert.equal(rec.input_count,4);assert.equal(rec.output.count,4);
assert.equal(rec.process_time,360);
}
const phys=json("data/magneticraft2/multiblock_voxels/"+id+".json");
assert.equal(phys.format,1);
assert.equal(phys.unit,"model_pixels");
assert.equal(phys.collision.length,17);
const orientations=["south","west","north","east"];
const canonical=[["FFF","BFB","UCO"],["BBB","BAB","BBB"]];
const expectedCounts={F:4,B:10,U:1,O:1,C:1,A:1};
for(const facing of orientations){
const bp=json("data/magneticraft2/multiblocks/"+id+"_"+facing+".json");
assert.deepEqual(bp.structure.dimensions,[3,2,3]);
assert.equal(bp.settings.voxel_profile,item);
const layers=[bp.structure.layout.layer1,bp.structure.layout.layer2];
const cells=layers.flat(2);const counts=Object.fromEntries([...new Set(cells)].map(ch=>[ch,cells.filter(x=>x===ch).length]));
assert.deepEqual(counts,expectedCounts);
assert.equal(bp.structure.blocks.A,"minecraft:air");
let controller;
for(let y=0;y<2;y++)for(let z=0;z<3;z++)for(let x=0;x<3;x++)if(layers[y][z][x]==="C")controller=[x,y,z];
for(let y=0;y<2;y++)for(let z=0;z<3;z++)for(let x=0;x<3;x++){
 const ch=layers[y][z][x];if(!["B","F"].includes(ch))continue;
 const [dx,dy,dz]=[x-controller[0],y-controller[1],z-controller[2]];
 const model=json("assets/magneticraft2/models/multiblock/"+id+"_"+facing+"/part_"+dx+"_"+dy+"_"+dz+".json");
 assert.equal(model.parent,"minecraft:block/cube_all");
 assert.ok(model.textures.all.startsWith("magneticraft2:block/refractory_kiln/"));
 }
}
const book=json("assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/ironage.json");
assert.ok(book.pages.some(p=>p.type==="patchouli:multiblock"&&p.name==="Iron Forge Hearth"));
assert.ok(book.pages.some(p=>p.recipe===item));
const model=json("assets/magneticraft2/models/block/iron_forge_hearth_controller.json");
let faces=0;
for(const e of model.elements){
const [w,h,d]=e.to.map((x,i)=>x-e.from[i]);
for(const [face,f] of Object.entries(e.faces)){
const dims=["up","down"].includes(face)?[w,d]:["east","west"].includes(face)?[d,h]:[w,h];
assert.deepEqual([f.uv[2]-f.uv[0],f.uv[3]-f.uv[1]],dims);
faces++;
}}
console.log("Iron Forge Hearth JSON/source contract PASS, "+faces+" 1:1 controller UV faces. Full Forge build and in-game behavior NOT checked.");
