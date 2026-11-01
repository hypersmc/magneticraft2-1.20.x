#!/usr/bin/env node
/**
 * Mechanical Forge Hammer source/resource contract. Static only; not a
 * replacement for Gradle compilation or 1.20.1 Minecraft multiplayer tests.
 */
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import {fileURLToPath} from "node:url";
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),"../..");
const read=p=>fs.readFileSync(path.join(root,p),"utf8");
const json=p=>JSON.parse(read(p));
const id="bronze_forge_hammer";
const recipes="src/main/resources/data/magneticraft2/recipes/";
const recipe=json(recipes+"bronze_forging_copper_plate.json");
assert.equal(recipe.type,"magneticraft2:multiblock_processing");
assert.equal(recipe.machine,"magneticraft2:"+id);
assert.deepEqual(recipe.input,{item:"minecraft:copper_ingot"});
assert.equal(recipe.input_count,4);
assert.deepEqual(recipe.output,{item:"magneticraft2:copper_plate",count:4});
assert.equal(recipe.process_time,160);
assert.equal(recipe.mechanical.min_speed,20);
assert.equal(recipe.mechanical.torque,5);
assert.ok(!recipe.byproduct&&!recipe.fluid_input&&!recipe.fluid_output);

const controller=json(recipes+id+".json");
const die=json(recipes+"bronze_forge_die.json");
assert.equal(controller.result.item,"magneticraft2:"+id);
assert.ok(controller.key.C.item==="magneticraft2:precision_bronze_bearing");
assert.equal(die.result.item,"magneticraft2:bronze_forge_die");

const design=json("tools/multiblock-models/designs/"+id+".json");
const source=json("tools/multiblock-models/sources/"+id+".json");
const voxel=json("src/main/resources/data/magneticraft2/multiblock_voxels/"+id+".json");
assert.equal(design.schema,1);
assert.equal(voxel.format,1);
assert.deepEqual(voxel.collision,design.voxels.collision);
assert.deepEqual(voxel.selection,design.voxels.selection);
assert.ok(voxel.collision.length>0&&voxel.collision.length<=512);
assert.ok(design.boxes.some(b=>b.name==="anvil_strike_surface"));
assert.ok(!design.boxes.some(b=>b.name.includes("hammer_head")),
 "Moving hammer must not be baked into static geometry");

const dirs=["south","west","north","east"];
const axes={north:[0,1],south:[0,1],east:[2,1],west:[2,1],up:[0,2],down:[0,2]};
let models=0,faces=0;
for(const direction of dirs){
 const bp=json("src/main/resources/data/magneticraft2/multiblocks/"+id+"_"+direction+".json");
 const [width,height,depth]=bp.structure.dimensions;
 assert.equal(height,4);
 assert.deepEqual([width,depth].sort((a,b)=>a-b),[3,5]);
 assert.equal(bp.settings.voxel_profile,"magneticraft2:"+id);
 const count={},coords={},hosts=[];
 for(let y=0;y<height;y++)for(let z=0;z<depth;z++)for(let x=0;x<width;x++){
  const symbol=bp.structure.layout["layer"+(y+1)][z][x];
  const type=bp.structure.blocks[symbol];
  assert.ok(type,"Unknown blueprint symbol "+symbol);
  count[symbol]=(count[symbol]||0)+1;
  (coords[symbol]??=[]).push([x,y,z]);
  if(type!=="minecraft:air")hosts.push([x,y,z]);
 }
 for(const symbol of ["C","I","U","O"])assert.equal(count[symbol],1,direction+":"+symbol);
 const [cx,cy,cz]=coords.C[0];
 assert.equal(cy,0);
 assert.equal(coords.I[0][1],0);
 assert.equal(coords.U[0][1],0);
 assert.equal(coords.O[0][1],0);
 assert.equal(bp.settings.render_air_anchors.length,3);
 for(const [x,y,z] of bp.settings.render_air_anchors){
  assert.equal(bp.structure.layout["layer"+(y+1)][cz+z][cx+x],"A",
   "AIR anchor must be part of the real required-air blueprint");
  hosts.push([cx+x,cy+y,cz+z]);
 }
 const seen=new Set();
 for(const [x,y,z] of hosts){
  const key=[x-cx,y-cy,z-cz].join("_");
  assert.ok(!seen.has(key),direction+" duplicate host "+key);
  seen.add(key);
  const model=json("src/main/resources/assets/magneticraft2/models/multiblock/"+
    id+"_"+direction+"/part_"+key+".json");
  models++;
  if(["I","U","O"].includes(bp.structure.layout["layer"+(y+1)][z][x]))
   assert.equal(model.elements.length,0,"Functional module cell has decorative render geometry "+direction+"/"+key);
  if(key==="0_0_0")assert.ok(model.elements.length>0,"Missing formed controller geometry");
  for(const el of model.elements){
   assert.ok(el.from.every((v,i)=>v>=-16&&v<el.to[i]&&el.to[i]<=32),
    "Invalid block-local coordinates "+direction+"/"+key);
   for(const [face,uv] of Object.entries(el.faces)){
    faces++;
    const [u,v]=axes[face];
    assert.ok(uv.uv.every(a=>a>=0&&a<=16),
     "Out-of-range UV "+direction+"/"+key);
    assert.ok(model.textures[uv.texture.slice(1)],
     "Unknown texture alias "+direction+"/"+key);
    assert.ok(Math.abs(uv.uv[2]-uv.uv[0]-(el.to[u]-el.from[u]))<.0001&&
              Math.abs(uv.uv[3]-uv.uv[1]-(el.to[v]-el.from[v]))<.0001,
      "Stretched or compressed UV "+direction+"/"+key);
   }
  }
 }
 assert.equal(seen.size,42,direction+" render-host count");
}
assert.equal(models,168);
const ram=json("src/main/resources/assets/magneticraft2/models/multiblock/bronze_forge_hammer_ram.json");
assert.ok(ram.elements.length>=3);
for(const element of ram.elements)for(const [face,f] of Object.entries(element.faces)){
 const [u,v]=axes[face];
 assert.ok(Math.abs(f.uv[2]-f.uv[0]-(element.to[u]-element.from[u]))<.0001 &&
           Math.abs(f.uv[3]-f.uv[1]-(element.to[v]-element.from[v]))<.0001,
  "Ram texture stretch "+face);
}
const be=read("src/main/java/com/magneticraft2/common/blockentity/stage/bronze/BronzeForgeHammerBlockEntity.java");
const b=read("src/main/java/com/magneticraft2/common/block/stage/bronze/BronzeForgeHammerBlock.java");
const ports=read("src/main/java/com/magneticraft2/common/blockentity/stage/copper/MultiblockItemPortBlockEntity.java");
const client=read("src/main/java/com/magneticraft2/client/Clientsetup.java");
const renderer=read("src/main/java/com/magneticraft2/client/render/blocks/stage/bronze/BronzeForgeHammerRenderer.java");
const jei=read("src/main/java/com/magneticraft2/compatibility/JEI/MagneticraftJeiPlugin.java");
const jeiModel=read("src/main/java/com/magneticraft2/compatibility/JEI/MultiblockJeiRecipe.java");
assert.match(b,/BronzeForgeHammerBlockEntity::serverTick/);
for(const token of ["getMechanicalLoadState","setMechanicalLoad","removeMechanicalLoad",
 "getEffectiveSpeed","getTorque","recipe.getInputCount()",
 "itemHandler.insertItem(1, output, true)","itemHandler.insertItem(1, output, false)",
 "itemHandler.extractItem(0, recipe.getInputCount(), false)","activeInput",
 "ForgeProgress","ForgeRecipe","ForgeDie","ForgeActiveInput","linkFormedController",
 "ReplaceableMachinePart","forgingDie.completeBatch()","MultiblockRenderParts.assign"]){
 assert.ok(be.includes(token),"Missing Forge Hammer contract: "+token);
}
assert.match(be,/if \(cap == ForgeCapabilities.ITEM_HANDLER\) return LazyOptional.empty\(\)/);
assert.match(ports,/controller instanceof BronzeForgeHammerBlockEntity hammer/);
assert.match(ports,/hammer.acceptsRecipeInput\(stack\)/);
assert.match(renderer,/getHammerStroke\(partial\)/);
assert.match(client,/BronzeForgeHammerRenderer::new/);
assert.match(client,/multiblock\/bronze_forge_hammer_ram/);
assert.match(jeiModel,/BRONZE_FORGE_HAMMER/);
assert.match(jei,/MultiblockJeiRecipe.BRONZE_FORGE_HAMMER/);
const book=json("src/main/resources/assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json");
const guide=book.pages.find(p=>p.name==="Mechanical Forge Hammer");
assert.ok(guide&&guide.multiblock.pattern.length===4);
assert.equal(guide.multiblock.mapping["0"],"magneticraft2:"+id);
/*
 * Patchouli's visual multiblock preview displays the horizontal X axis
 * mirrored relative to the server's SOUTH coordinate layout. Do NOT copy
 * the blueprint rows verbatim: it swaps functional input/output ports.
 * The existing in-game-tested Ore Roasting Kiln follows this convention.
 */
const south=json("src/main/resources/data/magneticraft2/multiblocks/"+id+"_south.json");
const bookRows=Array.from({length:south.structure.dimensions[1]},(_,topIndex)=>{
 const layer=south.structure.layout["layer"+(south.structure.dimensions[1]-topIndex)];
 return layer.map(row=>[...row].reverse().map(symbol=>
    symbol==="C"?"0":symbol==="A"?"_":symbol).join(""));
});
assert.deepEqual(guide.multiblock.pattern,bookRows,
 "Patchouli preview must mirror the SOUTH blueprint's X coordinates so I/O appears correctly");
assert.equal(south.structure.layout.layer1[2][1],"U");
assert.equal(south.structure.layout.layer1[2][3],"O");

const lang=json("src/main/resources/assets/magneticraft2/lang/en_us.json");
assert.ok(lang["block.magneticraft2."+id]&&lang["item.magneticraft2.bronze_forge_die"]);
for(const tag of ["mineable/pickaxe","needs_stone_tool"])assert.ok(
 json("src/main/resources/data/minecraft/tags/blocks/"+tag+".json").values.includes("magneticraft2:"+id));
console.log("Bronze Forge Hammer K1: 4 facings, "+models+" models, "+faces+
 " tiled faces, Gear V2, ports, die wear, NBT, recipes, JEI, book OK (static only).");
