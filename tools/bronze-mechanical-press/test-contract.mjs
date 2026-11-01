#!/usr/bin/env node
/**
 * Bronze Mechanical Press K1 source + assets contract.
 * This does NOT substitute for Forge 1.20.1 Gradle or in-game tests.
 */
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import {fileURLToPath} from "node:url";
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),"../..");
const read=p=>fs.readFileSync(path.join(root,p),"utf8");
const json=p=>JSON.parse(read(p));
const id="bronze_mechanical_press",machine="magneticraft2:"+id;
const recipes="src/main/resources/data/magneticraft2/recipes/";
const pressing=json(recipes+"bronze_pressing_copper_panel.json");
assert.equal(pressing.type,"magneticraft2:multiblock_processing");
assert.equal(pressing.machine,machine);
assert.deepEqual(pressing.input,{item:"magneticraft2:copper_sheet"});
assert.equal(pressing.input_count,4);
assert.deepEqual(pressing.output,{item:"magneticraft2:copper_panel",count:4});
assert.equal(pressing.process_time,160);
assert.deepEqual(pressing.mechanical,{min_speed:18,torque:8});
assert.ok(!pressing.byproduct&&!pressing.fluid_input&&!pressing.fluid_output);
const oldTank=json(recipes+"copper_fluid_tank.json");
const newTank=json(recipes+"copper_fluid_tank_pressed_panel.json");
assert.deepEqual(oldTank.pattern,["CCC","G G","CCC"]);
assert.equal(oldTank.key.C.item,"magneticraft2:copper_plate");
assert.equal(oldTank.result.item,"magneticraft2:copper_fluid_tank");
assert.deepEqual(newTank.pattern,["P P","G G","P P"]);
assert.equal(newTank.key.P.item,"magneticraft2:copper_panel");
assert.equal(newTank.key.G.item,"minecraft:glass");
assert.deepEqual(newTank.result,oldTank.result);
assert.deepEqual(json(recipes+id+".json").result,{item:machine});
assert.deepEqual(json(recipes+"bronze_press_die.json").result,{item:"magneticraft2:bronze_press_die"});

const p="src/main/java/com/magneticraft2/";
const block=read(p+"common/block/stage/bronze/BronzeMechanicalPressBlock.java");
const be=read(p+"common/blockentity/stage/bronze/BronzeMechanicalPressBlockEntity.java");
const registry=read(p+"common/registry/registers/BlockEntityRegistry.java");
const blockReg=read(p+"common/registry/registers/BlockRegistry.java");
const itemReg=read(p+"common/registry/registers/ItemRegistry.java");
const port=read(p+"common/blockentity/stage/copper/MultiblockItemPortBlockEntity.java");
const client=read(p+"client/Clientsetup.java");
const renderer=read(p+"client/render/blocks/stage/bronze/BronzeMechanicalPressRenderer.java");
const jei=read(p+"compatibility/JEI/MagneticraftJeiPlugin.java");
const jeiModel=read(p+"compatibility/JEI/MultiblockJeiRecipe.java");
const tab=read(p+"common/registry/registers/CreativeTabRegistry.java");
assert.match(block,/createTickerHelper\(type,\s*BlockEntityRegistry\.BRONZE_MECHANICAL_PRESS_BE\.get\(\),\s*BronzeMechanicalPressBlockEntity::serverTick\)/);
assert.doesNotMatch(block,/createTickerHelper\(type,\s*BlockEntityRegistry\.BRONZE_(ROLLING_MILL|FORGE_HAMMER|STAMP_MILL)_BE\.get\(\)/);
assert.match(registry,/BlockEntityType\.Builder\.of\(BronzeMechanicalPressBlockEntity::new,\s*BlockRegistry\.BRONZE_MECHANICAL_PRESS\.get\(\)\)/);
assert.match(blockReg,/BronzeMechanicalPressBlock::new/);
for(const item of ["BRONZE_MECHANICAL_PRESS","BRONZE_PRESS_DIE","COPPER_PANEL"]){
 assert.ok(itemReg.includes("ITEM_"+item),"Missing item "+item);
 assert.ok(tab.includes("ITEM_"+item),"Missing creative tab "+item);
}
assert.match(port,/controller instanceof BronzeMechanicalPressBlockEntity press/);
assert.match(port,/press.acceptsRecipeInput\(stack\)/);
assert.match(jei,/MultiblockJeiRecipe.BRONZE_MECHANICAL_PRESS/);
assert.match(jeiModel,/BRONZE_MECHANICAL_PRESS/);
assert.match(client,/BronzeMechanicalPressRenderer::new/);
assert.match(client,/multiblock\/bronze_mechanical_press_ram/);
assert.match(renderer,/press\.getPressStroke\(partial\)/);
assert.match(be,/float getPressStroke\(float partialTicks\)/);
assert.match(be,/input\.getVisualRotationDegrees\(partialTicks\)/);
assert.doesNotMatch(be,/getRollProgress\(\)/,"No recipe search per rendered frame");
assert.match(be,/boolean canAttempt = node != null && node\.getSpeed\(\) >= recipe\.getMinSpeed\(\)/);
assert.match(be,/manager\.getMechanicalLoadState\(level, worldPosition\)\.supplied\(\)/);
for(const token of ["itemHandler.insertItem(1, output, true)",
 "itemHandler.insertItem(1, output, false)",
 "itemHandler.extractItem(0, recipe.getInputCount(), false)",
 "pressDie.canOperate()","pressDie.completeBatch()",
 "removeMechanicalLoad","MultiblockRenderParts.assign",
 "getMechanicalInput()","linkFormedController",
 "PressProgress","PressRecipe","PressActiveInput","PressDie","PressRunning"]){
 assert.ok(be.includes(token),"Missing press K1 control "+token);
}
assert.match(be,/if \(cap == ForgeCapabilities.ITEM_HANDLER\) return LazyOptional.empty\(\)/);

const design=json("tools/multiblock-models/designs/"+id+".json");
const source=json("tools/multiblock-models/sources/"+id+".json");
const voxel=json("src/main/resources/data/magneticraft2/multiblock_voxels/"+id+".json");
assert.equal(design.schema,1);
assert.equal(voxel.format,1);
assert.deepEqual(voxel.collision,design.voxels.collision);
assert.deepEqual(voxel.selection,design.voxels.selection);
assert.ok(design.boxes.some(b=>b.name==="front_controller_chassis"));
assert.ok(design.boxes.some(b=>b.name==="press_anvil_surface"));
assert.ok(design.boxes.some(b=>b.name==="rear_drive_casing"));
assert.ok(!design.boxes.some(b=>b.name==="moving_ram"),
 "Moving ram must be separate from the static Voxel model");

const book=json("src/main/resources/assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json");
const guide=book.pages.find(pg=>pg.name==="Mechanical Press");
assert.ok(guide&&guide.multiblock.pattern.length===3);
const axes={north:[0,1],south:[0,1],east:[2,1],west:[2,1],up:[0,2],down:[0,2]};
let hosts=0,faces=0;
function checkFaces(model,where){
 for(const el of model.elements){
  assert.ok(el.from.every((v,i)=>v>=-16.0001&&el.to[i]<=32.0001&&v<el.to[i]),
   "Model outside block-local bounds "+where);
  for(const [face,uv] of Object.entries(el.faces)){
   const [u,v]=axes[face];
   assert.ok(uv.uv.every(n=>n>=-0.0001&&n<=16.0001),"UV outside tile "+where);
   assert.ok(Math.abs(uv.uv[2]-uv.uv[0]-(el.to[u]-el.from[u]))<0.0002
         &&Math.abs(uv.uv[3]-uv.uv[1]-(el.to[v]-el.from[v]))<0.0002,
     "Texture stretched or compressed: "+where+" "+face);
   assert.ok(model.textures[uv.texture.slice(1)],"Missing texture alias "+where);
   faces++;
  }
 }
}
for(const dir of ["south","west","north","east"]){
 const blueprint=json("src/main/resources/data/magneticraft2/multiblocks/"+id+"_"+dir+".json");
 assert.deepEqual(blueprint.structure.dimensions,[3,3,3]);
 assert.equal(blueprint.settings.voxel_profile,machine);
 const c={},pos={};
 for(let y=0;y<3;y++)for(let z=0;z<3;z++)for(let x=0;x<3;x++){
  const s=blueprint.structure.layout["layer"+(y+1)][z][x];
  c[s]=(c[s]||0)+1;if(s==="C")pos.center=[x,y,z];
  assert.ok(blueprint.structure.blocks[s],"Unknown blueprint symbol "+s);
 }
 for(const s of ["C","I","U","O"])assert.equal(c[s],1,dir+" missing "+s);
 assert.equal(pos.center[1],0);
 assert.equal(blueprint.settings.render_air_anchors.length,2);
 const [cx,cy,cz]=pos.center,parts=new Set();
 for(let y=0;y<3;y++)for(let z=0;z<3;z++)for(let x=0;x<3;x++){
  const s=blueprint.structure.layout["layer"+(y+1)][z][x];
  if(s!=="A")parts.add([x-cx,y,z-cz].join("_"));
 }
 for(const [dx,dy,dz] of blueprint.settings.render_air_anchors){
  assert.equal(blueprint.structure.layout["layer"+(dy+cy+1)][cz+dz][cx+dx],
   "A","Render-air anchor is not AIR "+dir);
  parts.add([dx,dy,dz].join("_"));
 }
 assert.equal(parts.size,25,"Wrong per-facing render model count "+dir);
 for(const key of parts){
  const model=json("src/main/resources/assets/magneticraft2/models/multiblock/"+id+
     "_"+dir+"/part_"+key+".json");
  const [dx,y,dz]=key.split("_").map(Number);
  const symbol=blueprint.structure.layout["layer"+(y+1)][cz+dz][cx+dx];
  if(["I","U","O"].includes(symbol))
   assert.equal(model.elements.length,0,"Decorative mesh over real module "+dir+"/"+key);
  if(key==="0_0_0")
   assert.ok(model.elements.length>0,"Missing formed controller model "+dir);
  checkFaces(model,dir+"/"+key);
  hosts++;
 }
 if(dir==="south"){
  assert.deepEqual(blueprint.structure.layout.layer1.map(row=>row.join("")),
   ["SIS","USO","SCS"]);
  const mirror=Array.from({length:3},(_,i)=>blueprint.structure.layout["layer"+(3-i)]
    .map(row=>[...row].reverse().map(s=>s==="C"?"0":s==="A"?"_":s).join("")));
  assert.deepEqual(guide.multiblock.pattern,mirror,
   "Patchouli must mirror exact SOUTH JSON, preventing swapped ports");
 }
}
assert.equal(hosts,100);
const ram=json("src/main/resources/assets/magneticraft2/models/multiblock/"+id+"_ram.json");
assert.ok(ram.elements.length>=3);
checkFaces(ram,"dynamic ram");
const panel=json("src/main/resources/assets/magneticraft2/models/item/copper_panel.json");
assert.ok(panel.elements.length>=4,"Copper Panel should have real flanged sheet geometry");
checkFaces(panel,"Copper Panel");
const lang=json("src/main/resources/assets/magneticraft2/lang/en_us.json");
assert.equal(lang["item.magneticraft2.copper_panel"],"Copper Panel");
for(const k of ["block.magneticraft2."+id,
 "message.magneticraft2.bronze_mechanical_press.status",
 "message.magneticraft2.bronze_mechanical_press.source",
 "message.magneticraft2.bronze_mechanical_press.reason.die_worn"]){
 assert.ok(lang[k],"Missing localization "+k);
}
for(const tag of ["mineable/pickaxe","needs_stone_tool"])
 assert.ok(json("src/main/resources/data/minecraft/tags/blocks/"+tag+".json").values.includes(machine));
const svg=read("docs/reference/bronze-mechanical-press-studio.svg");
assert.ok(svg.includes("MAGNETICRAFT2")&&svg.includes("3×3×3"));
console.log("Bronze Mechanical Press K1 static contract PASS: "+hosts+
 " block-local parts; "+faces+" UV-verified model faces; 4 exact facings;"+
 " ticker/Gear V2/load/maintenance/ports/book/JEI/recipes/reference.");
