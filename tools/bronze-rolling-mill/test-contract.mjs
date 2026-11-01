#!/usr/bin/env node
/**
 * Bronze Rolling Mill: authoritative source/resource integration contract.
 * Static checks only: still requires Forge compilation & in-game tests.
 */
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import {fileURLToPath} from "node:url";
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),"../..");
const read=p=>fs.readFileSync(path.join(root,p),"utf8");
const json=p=>JSON.parse(read(p));
const id="bronze_rolling_mill",mid="magneticraft2:"+id;
const recipes="src/main/resources/data/magneticraft2/recipes/";
const recipe=json(recipes+"bronze_rolling_copper_sheet.json");
assert.equal(recipe.machine,mid);
assert.equal(recipe.type,"magneticraft2:multiblock_processing");
assert.deepEqual(recipe.input,{item:"magneticraft2:copper_plate"});
assert.equal(recipe.input_count,4);
assert.deepEqual(recipe.output,{item:"magneticraft2:copper_sheet",count:4});
assert.equal(recipe.process_time,180);
assert.deepEqual(recipe.mechanical,{min_speed:16,torque:6});
assert.ok(!recipe.byproduct&&!recipe.fluid_input&&!recipe.fluid_output);
const oldPipe=json(recipes+"water_pipe.json");
const newPipe=json(recipes+"water_pipe_rolled_sheet.json");
assert.deepEqual(oldPipe.pattern,["CCC","   ","CCC"]);
assert.deepEqual(oldPipe.key.C,{item:"magneticraft2:copper_plate"});
assert.deepEqual(oldPipe.result,{item:"magneticraft2:water_pipe",count:8});
assert.deepEqual(newPipe.key.C,{item:"magneticraft2:copper_sheet"});
assert.deepEqual(newPipe.result,oldPipe.result);
assert.equal(newPipe.pattern.join("").split("C").length-1,4);
assert.deepEqual(json(recipes+"bronze_rolling_mill.json").result,{item:mid});
assert.deepEqual(json(recipes+"bronze_roller_sleeves.json").result,
 {item:"magneticraft2:bronze_roller_sleeves"});

const block=read("src/main/java/com/magneticraft2/common/block/stage/bronze/BronzeRollingMillBlock.java");
const be=read("src/main/java/com/magneticraft2/common/blockentity/stage/bronze/BronzeRollingMillBlockEntity.java");
const registry=read("src/main/java/com/magneticraft2/common/registry/registers/BlockEntityRegistry.java");
const blocks=read("src/main/java/com/magneticraft2/common/registry/registers/BlockRegistry.java");
const items=read("src/main/java/com/magneticraft2/common/registry/registers/ItemRegistry.java");
const ports=read("src/main/java/com/magneticraft2/common/blockentity/stage/copper/MultiblockItemPortBlockEntity.java");
const renderer=read("src/main/java/com/magneticraft2/client/render/blocks/stage/bronze/BronzeRollingMillRenderer.java");
const client=read("src/main/java/com/magneticraft2/client/Clientsetup.java");
const jei=read("src/main/java/com/magneticraft2/compatibility/JEI/MagneticraftJeiPlugin.java");
const jeiType=read("src/main/java/com/magneticraft2/compatibility/JEI/MultiblockJeiRecipe.java");
const creative=read("src/main/java/com/magneticraft2/common/registry/registers/CreativeTabRegistry.java");
assert.match(block,/createTickerHelper\(type,\s*BlockEntityRegistry\.BRONZE_ROLLING_MILL_BE\.get\(\),\s*BronzeRollingMillBlockEntity::serverTick\)/);
assert.doesNotMatch(block,/createTickerHelper\(type,\s*BlockEntityRegistry\.BRONZE_FORGE_HAMMER_BE\.get\(\)/);
assert.doesNotMatch(block,/createTickerHelper\(type,\s*BlockEntityRegistry\.BRONZE_STAMP_MILL_BE\.get\(\)/);
assert.match(registry,/BlockEntityType\.Builder\.of\(BronzeRollingMillBlockEntity::new,\s*BlockRegistry\.BRONZE_ROLLING_MILL\.get\(\)\)/);
assert.match(blocks,/BronzeRollingMillBlock::new/);
assert.match(items,/ITEM_BRONZE_ROLLING_MILL = fromBlock\(BRONZE_ROLLING_MILL\)/);
assert.match(items,/ITEM_BRONZE_ROLLER_SLEEVES/);
assert.match(items,/ITEM_COPPER_SHEET/);
assert.match(ports,/controller instanceof BronzeRollingMillBlockEntity mill/);
assert.match(ports,/mill\.acceptsRecipeInput\(stack\)/);
assert.match(jei,/MultiblockJeiRecipe\.BRONZE_ROLLING_MILL/);
assert.match(jeiType,/BRONZE_ROLLING_MILL/);
assert.match(client,/BronzeRollingMillRenderer::new/);
assert.match(client,/multiblock\/bronze_rolling_mill_roller/);
assert.match(creative,/ITEM_BRONZE_ROLLING_MILL/);
assert.match(renderer,/mill\.getRollerAngle\(partial\)/);
assert.match(renderer,/renderRoller\(pose, buffers, roller, dispatcher, state, angle, 0F/);
assert.match(renderer,/renderRoller\(pose, buffers, roller, dispatcher, state, -angle, 14F/);
assert.doesNotMatch(be,/getRollProgress\(\)/,
 "Never search the recipe manager on each client render frame");
assert.match(be,/boolean canAttempt = node != null && node\.getSpeed\(\) >= recipe\.getMinSpeed\(\)/);
assert.match(be,/manager\.getMechanicalLoadState\(level, worldPosition\)\.supplied\(\)/);
for(const token of ["itemHandler.insertItem(1, output, true)","itemHandler.insertItem(1, output, false)",
 "itemHandler.extractItem(0, recipe.getInputCount(), false)","removeMechanicalLoad",
 "rollerSleeves.canOperate()","rollerSleeves.completeBatch()","RollerAngle",
 "MultiblockRenderParts.assign","linkFormedController","getMechanicalInput()",
 "MillProgress","MillRecipe","MillActiveInput","MillRollers","MillRunning"]){
 assert.ok(be.includes(token),"Missing K1 controller requirement: "+token);
}
assert.match(be,/if \(cap == ForgeCapabilities\.ITEM_HANDLER\) return LazyOptional\.empty\(\)/);
for(const field of ["sourceEquivalentDemand()","totalSourceDemand()","sourceTorqueCapacity()"])
 assert.ok(be.includes("load."+field),"Missing upstream load diagnosis "+field);

const design=json("tools/multiblock-models/designs/"+id+".json");
const source=json("tools/multiblock-models/sources/"+id+".json");
const voxel=json("src/main/resources/data/magneticraft2/multiblock_voxels/"+id+".json");
assert.equal(design.schema,1);
assert.equal(voxel.format,1);
assert.deepEqual(voxel.collision,design.voxels.collision);
assert.deepEqual(voxel.selection,design.voxels.selection);
assert.ok(voxel.collision.length>=20&&voxel.collision.length<=512);
assert.ok(design.boxes.some(b=>b.name==="left_feed_tray"));
assert.ok(design.boxes.some(b=>b.name==="right_exit_tray"));
assert.ok(!design.boxes.some(b=>/roller_core|rotating_roller/.test(b.name)),
 "Rotating rolls belong exclusively in the two baked BER render calls");
const axes={north:[0,1],south:[0,1],east:[2,1],west:[2,1],up:[0,2],down:[0,2]};
const checkUV=(model,id)=>{
 let quads=0;
 for(const element of model.elements){
  for(const [face,uv] of Object.entries(element.faces)){
   const [u,v]=axes[face];
   assert.ok(uv.uv.every(a=>a>=-0.0001&&a<=16.0001),"UV out of bounds: "+id);
   assert.ok(Math.abs(uv.uv[2]-uv.uv[0]-(element.to[u]-element.from[u]))<0.0002
       &&Math.abs(uv.uv[3]-uv.uv[1]-(element.to[v]-element.from[v]))<0.0002,
      "STRETCHED TEXTURE in "+id+" "+face);
   assert.ok(model.textures[uv.texture.slice(1)],"Bad material alias "+id);
   assert.ok(element.from.every((a,i)=>a>=-16.0001&&element.to[i]<=32.0001&&a<element.to[i]),
     "Model element exceeds vanilla block-local bounds "+id);
   quads++;
  }
 }
 return quads;
};
const book=json("src/main/resources/assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json");
const guide=book.pages.find(p=>p.name==="Bronze Rolling Mill");
assert.ok(guide&&guide.multiblock.pattern.length===3);
const dirs=["south","west","north","east"];
let cells=0,faces=0;
for(const dir of dirs){
 const bp=json("src/main/resources/data/magneticraft2/multiblocks/"+id+"_"+dir+".json");
 const [width,height,depth]=bp.structure.dimensions;
 assert.equal(height,3);
 assert.deepEqual([width,depth].sort((a,b)=>a-b),[3,5]);
 assert.equal(bp.settings.voxel_profile,mid);
 const counts={},positions={},hosts=[];
 for(let y=0;y<height;y++)for(let z=0;z<depth;z++)for(let x=0;x<width;x++){
  const sym=bp.structure.layout["layer"+(y+1)][z][x];
  const blockType=bp.structure.blocks[sym];
  assert.ok(blockType,"Missing block mapping for "+sym);
  counts[sym]=(counts[sym]||0)+1;
  (positions[sym]??=[]).push([x,y,z]);
  if(blockType!=="minecraft:air") hosts.push([x,y,z]);
 }
 for(const sym of ["C","I","U","O"])assert.equal(counts[sym],1,dir+": "+sym);
 const [cx,cy,cz]=positions.C[0];
 assert.equal(cy,0);
 for(const sym of ["I","U","O"])assert.equal(positions[sym][0][1],0);
 for(const a of bp.settings.render_air_anchors){
  const [dx,dy,dz]=a,actual=bp.structure.layout["layer"+(dy+cy+1)][cz+dz][cx+dx];
  assert.equal(actual,"A","AIR anchor not actually empty "+dir);
  hosts.push([cx+dx,cy+dy,cz+dz]);
 }
 assert.equal(hosts.length,35,"unexpected number of block-local render hosts "+dir);
 const seen=new Set();
 for(const [x,y,z] of hosts){
  const k=[x-cx,y-cy,z-cz].join("_");
  assert.ok(!seen.has(k),"Duplicate host "+dir+"/"+k);
  seen.add(k);
  const part=json("src/main/resources/assets/magneticraft2/models/multiblock/"+
    id+"_"+dir+"/part_"+k+".json");
  if(["I","U","O"].includes(bp.structure.layout["layer"+(y+1)][z][x]))
    assert.equal(part.elements.length,0,"Functional module has decorative geometry "+dir+"/"+k);
  if(k==="0_0_0")assert.ok(part.elements.length>0,"Missing controller visual "+dir);
  faces+=checkUV(part,dir+"/"+k);cells++;
 }
 if(dir==="south"){
  assert.deepEqual(bp.structure.layout.layer1.map(x=>x.join("")),["SSISS","USSSO","SSCSS"]);
  const mirrored=Array.from({length:height},(_,i)=>
   bp.structure.layout["layer"+(height-i)].map(row=>[...row].reverse().map(sym=>
     sym==="C"?"0":sym==="A"?"_":sym).join("")));
  assert.deepEqual(guide.multiblock.pattern,mirrored,
   "Patchouli MUST mirror all SOUTH JSON rows (Forge Hammer regression)");
  assert.deepEqual(bp.settings.render_air_anchors,[[0,1,-1],[0,2,-1]]);
 }
}
assert.equal(cells,140);
const roller=json("src/main/resources/assets/magneticraft2/models/multiblock/"+id+"_roller.json");
assert.ok(roller.elements.length>=4);
const rollFaces=checkUV(roller,"animated roller");
const sheet=json("src/main/resources/assets/magneticraft2/models/item/copper_sheet.json");
assert.ok(sheet.elements[0].to[1]-sheet.elements[0].from[1]<
 json("src/main/resources/assets/magneticraft2/models/item/copper_plate.json").elements[0].to[1]-
 json("src/main/resources/assets/magneticraft2/models/item/copper_plate.json").elements[0].from[1],
 "Sheet item model should be visually thinner than forged plate");
const lang=json("src/main/resources/assets/magneticraft2/lang/en_us.json");
for(const k of ["block.magneticraft2."+id,"item.magneticraft2.copper_sheet",
 "item.magneticraft2.bronze_roller_sleeves",
 "message.magneticraft2.bronze_rolling_mill.status",
 "message.magneticraft2.bronze_rolling_mill.source",
 "message.magneticraft2.bronze_rolling_mill.reason.rollers_worn"]){
 assert.ok(lang[k],"Missing language key "+k);
}
for(const tag of ["mineable/pickaxe","needs_stone_tool"])
 assert.ok(json("src/main/resources/data/minecraft/tags/blocks/"+tag+".json").values.includes(mid));
const svg=read("docs/reference/bronze-rolling-mill-studio.svg");
assert.ok(svg.includes("MAGNETICRAFT2")&&svg.includes("5 × 3 × 3"));
console.log("Bronze Rolling Mill K1 static contract PASS: "+cells+" models; "+
 faces+" static faces; "+rollFaces+" roller faces; 4 blueprints; exact server ticker; "+
 "rolling, maintenance, I/O, JEI, book, alternate pipe recipe and Studio reference.");
