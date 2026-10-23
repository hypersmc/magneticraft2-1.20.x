#!/usr/bin/env node
/** Water Settling Tank structural and process regression, Node 18+ no npm deps. */
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { execFileSync } from "node:child_process";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const read = p => fs.readFileSync(path.join(root, p), "utf8");
const json = p => JSON.parse(read(p));
const assets = "src/main/resources/assets/magneticraft2/";
const data = "src/main/resources/data/magneticraft2/";
const source = "src/main/java/com/magneticraft2/";
const id = "water_settling_tank";
const rows = [
  ["SSISS","SSSSS","SSSSS","SSSSS","SPCSO"],
  ["SSSSS","SAAAS","SAAAS","SAAAS","SAAAS"]
];
const parts = [];
for (let y=0;y<2;y++) for(let zi=0;zi<5;zi++) for(let xi=0;xi<5;xi++)
  parts.push({x:xi-2,y,z:zi-4,k:rows[y][zi][xi]});
const where = k => parts.filter(p=>p.k===k);
assert.equal(where("C").length, 1);
assert.equal(where("I").length, 1);
assert.equal(where("P").length, 1);
assert.equal(where("O").length, 1);
assert.deepEqual(where("C").map(p=>[p.x,p.y,p.z]),[[0,0,0]]);
assert.deepEqual(where("I").map(p=>[p.x,p.y,p.z]),[[0,0,-4]]);
assert.deepEqual(where("P").map(p=>[p.x,p.y,p.z]),[[-1,0,0]]);
assert.deepEqual(where("O").map(p=>[p.x,p.y,p.z]),[[2,0,0]]);
assert.equal(where("S").length,34,"Low basin needs 21 Stone Bricks in foundation and 13 in rim");
assert.ok(parts.every(c=>c.y<=1),"Upper frame must be entirely removed");
const nonModule = p=>!"IPO".includes(p.k);
const rot=(x,z,f)=>f==="south"?[x,z]:f==="west"?[-z,x]:f==="north"?[-x,-z]:[z,-x];
// Only six low, continuous air-hosted pieces: transverse overflow
// across z=-2 and short front retaining lip at z=-1.
const anchors=[
 [-1,1,-2],[0,1,-2],[1,1,-2],
 [-1,1,-1],[0,1,-1],[1,1,-1]
];
const sdir=["south","west","north","east"];
const expectedIds={
 C:"magneticraft2:water_settling_tank",
 I:"magneticraft2:multiblock_fluid_input",
 P:"magneticraft2:multiblock_item_primary_output",
 O:"magneticraft2:multiblock_fluid_output",
 S:"minecraft:stone_bricks",A:"minecraft:air"
};
const modelPaths=new Set();
for (const f of sdir) {
 const bp=json(data+"multiblocks/"+id+"_"+f+".json");
 assert.equal(bp.name,id+"_"+f);
 assert.deepEqual(bp.structure.dimensions,[5,2,5]);
 assert.deepEqual(bp.structure.blocks,expectedIds);
 assert.equal(bp.settings.voxel_profile,"magneticraft2:"+id);
 assert.equal(bp.settings.inventory,true);
 assert.equal(bp.settings.fluid,true);
 assert.equal(bp.settings.power,false);
 assert.equal(bp.settings.fuel,false);
 assert.deepEqual(bp.settings.render_air_anchors.map(a=>a.join(",")).sort(),
  anchors.map(([x,y,z])=>{const [rx,rz]=rot(x,z,f);return [rx,y,rz].join(",")}).sort(),
  f+": baffle and spill lip must anchor ONLY valid AIR cells");
 const rotated=parts.map(c=>({...c,rot:rot(c.x,c.z,f)}));
 const minX=Math.min(...rotated.map(c=>c.rot[0]));
 const minZ=Math.min(...rotated.map(c=>c.rot[1]));
 const covered=new Set();
 for(const cell of rotated) {
  const [x,z]=cell.rot;
  assert.equal(bp.structure.layout["layer"+(cell.y+1)][z-minZ][x-minX],cell.k,
    f+": wrong blueprint cell "+[x,cell.y,z]);
  const key=[x,cell.y,z].join(",");
  assert.ok(!covered.has(key),f+": duplicate rotated cell "+key);
  covered.add(key);
  if(!nonModule(cell) || cell.k==="A" &&
      !anchors.some(a=>a.join(",")===[cell.x,cell.y,cell.z].join(",")))
   continue;
  const rel="models/multiblock/"+id+"_"+f+"/part_"+x+"_"+cell.y+"_"+z+".json";
  modelPaths.add(rel);
  const model=json(assets+rel);
  assert.equal(model.parent,"minecraft:block/block");
  assert.equal(model.ambientocclusion,false);
  assert.ok(Array.isArray(model.elements) && model.elements.length>0,
    f+": real model host missing geometry "+rel);
  for (const el of model.elements) {
   assert.ok(el.from.every((v,i)=>Number.isFinite(v)&&v>=0&&
        Number.isFinite(el.to[i])&&el.to[i]<=16&&v<el.to[i]),
        "Illegal vanilla model box "+rel);
   for(const [side,view] of Object.entries(el.faces)) {
    const spanU=side==="east"||side==="west"
      ?el.to[2]-el.from[2]:el.to[0]-el.from[0];
    const spanV=side==="up"||side==="down"
      ?el.to[2]-el.from[2]:el.to[1]-el.from[1];
    assert.ok(Math.abs(view.uv[2]-view.uv[0]-spanU)<0.001 &&
       Math.abs(view.uv[3]-view.uv[1]-spanV)<0.001,
      "Texture UV stretched or compressed "+rel+" "+side);
   }
  }
 }
}
assert.equal(modelPaths.size,164,"Four rotations need 41 low-basin static models each");
// Physical pipe-to-port continuity: the rear riser begins at the top of
// the real ground-floor fluid input instead of hovering overhead.
const inletRiser=json(assets+"models/multiblock/"+id+"_south/part_0_1_-4.json");
assert.ok(inletRiser.elements.some(e=>e.from[1]===0&&e.to[1]===9
       &&e.from[0]===6&&e.from[2]===8),
       "Dirty inlet riser no longer joins the real Fluid Input below");
const eastDownpipe=json(assets+"models/multiblock/"+id+"_south/part_2_1_0.json");
assert.ok(eastDownpipe.elements.some(e=>e.from[1]===0&&e.to[1]===10),
       "Recovered water downpipe must reach its east-edge real Fluid Output");
for(const facing of sdir){
 const dir=path.join(root,assets+"models/multiblock/"+id+"_"+facing);
 const models=fs.readdirSync(dir).filter(n=>/^part_-?\d+_-?\d+_-?\d+\.json$/.test(n));
 assert.equal(models.length,41,
   "Old third-layer posts/bridge models must be deleted: "+facing);
 assert.ok(models.every(file=>!/^part_-?\d+_2_/.test(file)),
   "No obsolete overhead geometry may remain after redesign: "+facing);
}
for(const f of sdir) {
 const state=json(assets+"blockstates/"+id+".json");
 assert.ok(state.variants["facing="+f+",is_formed=true"].model.endsWith(
  "/"+id+"_"+f+"/part_0_0_0"));
 const pageController=json(assets+"models/item/"+id+".json");
 assert.equal(pageController.parent,"magneticraft2:block/"+id+"_controller");
}
const vox=json(data+"multiblock_voxels/"+id+".json");
assert.equal(vox.format,1);
assert.equal(vox.unit,"model_pixels");
assert.ok(vox.collision.length>40 && vox.collision.length<=512);
const inside=(b,cell)=>[0,1,2].every(i=>b[i]<16*(cell[i]+1)-1e-4
  &&b[i+3]>16*cell[i]+1e-4);
for(const box of vox.collision)for(const name of ["I","P","O"]) {
 const c=where(name)[0];
 assert.ok(!inside(box,[c.x,c.y,c.z]),
   "Voxel solid illegally overlaps real module "+name);
}
assert.ok(vox.collision.some(b=>b[1]===0&&b[4]===13)
       &&vox.collision.some(b=>b[1]===13&&b[4]===16),
 "No floating liner or unsupported basin panels");
for(const point of [[-1,1,-2],[0,1,-2],[1,1,-2],[-1,1,-1],[0,1,-1],[1,1,-1]]){
 assert.ok(vox.collision.some(box=>inside(box,point)),
    "Missing low overflow weir or rear-of-port retaining lip: "+point);
}
assert.ok(vox.collision.every(box=>box[4] <= 32),
 "Tall posts, walkways or third-level collision geometry remain");

const recipe=json(data+"recipes/settling_dirty_water.json");
assert.equal(recipe.type,"magneticraft2:multiblock_processing");
assert.equal(recipe.machine,"magneticraft2:"+id);
assert.ok(!Object.hasOwn(recipe,"input"),"Passive fluid-only recipe must not invent item inputs");
assert.equal(recipe.fluid_input.fluid,"magneticraft2:dirty_water");
assert.equal(recipe.fluid_input.amount,600);
assert.equal(recipe.fluid_output.fluid,"minecraft:water");
assert.equal(recipe.fluid_output.amount,480);
assert.ok(recipe.fluid_output.amount<recipe.fluid_input.amount,
 "Water recovery must not form an infinite loop");
assert.deepEqual(recipe.output,{item:"magneticraft2:settled_sediment",count:1});
assert.equal(recipe.process_time,240);
assert.ok(!Object.hasOwn(recipe,"mechanical"),
 "Passive settling does not require an arbitrary gearbox");
const clay=json(data+"recipes/settled_sediment_to_clay.json");
assert.deepEqual(clay.pattern,["SS","SS"]);
assert.equal(clay.key.S.item,"magneticraft2:settled_sediment");
assert.equal(clay.result.item,"minecraft:clay_ball");
assert.equal(clay.result.count,1);

const be=read(source+"common/blockentity/stage/bronze/WaterSettlingTankBlockEntity.java");
const block=read(source+"common/block/stage/bronze/WaterSettlingTankBlock.java");
const renderer=read(source+"client/render/blocks/stage/bronze/WaterSettlingTankRenderer.java");
const generic=read(source+"common/recipe/multiblock/MultiblockProcessingRecipe.java");
const expectedBE=[
"new FluidTank(DIRTY_CAPACITY", "new FluidTank(CLEAN_CAPACITY",
"FluidRegistry.DIRTY_WATER.get()", "Fluids.WATER",
"clean.fill(recovered, IFluidHandler.FluidAction.SIMULATE)",
"itemHandler.insertItem(1, sediment.copy(), true).isEmpty()",
"if (!ready)", "setProcessing(false)", "FluidStack drained = dirty.drain(",
"clean.fill(recovered, IFluidHandler.FluidAction.EXECUTE)",
"itemHandler.insertItem(1, sediment.copy(), false)",
"activeRecipeId = recipe.getId()", "processTime = 0",
"MultiblockRenderParts.assign", "MultiblockVoxelNbt.write",
"getFluidInTank(int tank)", "return formed ? clean.drain",
"capability == ForgeCapabilities.ITEM_HANDLER"
];
for(const check of expectedBE)assert.ok(be.includes(check),"Controller lost contract "+check);
assert.ok(be.indexOf("clean.fill(recovered, IFluidHandler.FluidAction.SIMULATE)")
          <be.indexOf("FluidStack drained = dirty.drain("),
  "Must reserve clean water output before consuming dirty water");
assert.ok(be.indexOf("itemHandler.insertItem(1, sediment.copy(), true)")
          <be.indexOf("FluidStack drained = dirty.drain("),
  "Must reserve sediment output before consuming dirty water");
assert.ok(block.includes("WaterSettlingTankBlockEntity::serverTick"));
assert.ok(!be.includes("GearNetworkManager") && !be.includes("getOrCreateGearNode"),
 "Passive treatment does not consume mechanical power");
assert.ok(generic.includes("Ingredient.EMPTY") &&
          generic.includes("input.isEmpty() && fluidInput.isEmpty()") &&
          generic.includes("if (input.isEmpty()) return false"),
 "Fluid-only recipe support must leave item recipe matching gated");
assert.ok(1.045 + 0.535 > 1 + 9/16 &&
          1.045 + 0.535 < 1 + 11/16,
 "Water should reach the low spill weir but remain behind the front retaining lip");
for (const s of [
  "tank.getDirtyFillRatio(),",
  "tank.getCleanFillRatio(),",
  "-1.28F, 2.28F, -3.53F, -1.29F",
  "-1.28F, 2.28F, -1.10F, -0.24F",
  "getDirtyAmount() > 0", "getCleanAmount() > 0",
  "float top = 1.045F + 0.535F",
  "float sideAlpha = alpha * 0.62F",
  "for (int tz = (int)Math.floor(minZ)",
  "float u0 = u(sprite, x0-tx)",
  "pose.translate(-0.5D, 0.775D, 0.5D)",
  "getSedimentStack()", "RenderType.translucent()"
])assert.ok(renderer.includes(s),"Wrong fluid/port position in renderer: "+s);
for(const s of ["BlockRegistry.WATER_SETTLING_TANK","ITEM_WATER_SETTLING_TANK",
 "WATER_SETTLING_TANK_BE","WATER_SETTLING_TANK"]){
 const sources=[
  source+"common/registry/registers/BlockRegistry.java",
  source+"common/registry/registers/ItemRegistry.java",
  source+"common/registry/registers/BlockEntityRegistry.java",
  source+"client/Clientsetup.java"
 ];
 assert.ok(sources.some(p=>read(p).includes(s)), "Missing registration "+s);
}
assert.ok(read(source+"compatibility/JEI/MagneticraftJeiPlugin.java")
 .includes("MultiblockJeiRecipe.WATER_SETTLING_TANK"));
for(const tag of ["mineable/pickaxe","needs_stone_tool"]){
 const t=json("src/main/resources/data/minecraft/tags/blocks/"+tag+".json");
 assert.ok(t.values.includes("magneticraft2:"+id),"Tool harvest tag missing "+tag);
}
const book=json(assets+"patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json");
const page=book.pages.find(p=>p.name==="Water Settling Tank");
assert.ok(page?.enable_visualize);
const expectedPattern=[];
for(let y=1;y>=0;y--){
 const cols=[];
 for(let x=0;x<5;x++){
  let s="";
  for(let z=0;z<5;z++){
   const k=rows[y][z][x];
   s+=k==="C"?"0":k==="A"?"_":k;
  }
  cols.push(s);
 }
 expectedPattern.push(cols);
}
assert.deepEqual(page.multiblock.pattern,expectedPattern,
 "Patchouli projection must match all 50 exact multiblock cells");
assert.ok(book.pages.some(p=>p.recipe==="magneticraft2:"+id));
assert.ok(book.pages.some(p=>p.recipe==="magneticraft2:settled_sediment_to_clay"));
// New machine-specific 16x16 art is backed by real committed PNG assets.
const customTextures=["refractory","lining","bronze","riveted_iron","grate","sludge"];
const bytes=new Set();
for(const tex of customTextures){
 const rel=assets+"textures/block/settling_tank/"+tex+".png";
 const buf=fs.readFileSync(path.join(root,rel));
 assert.equal(buf.subarray(0,8).toString("hex"),"89504e470d0a1a0a",
     tex+" is not a real PNG");
 assert.equal(buf.readUInt32BE(16),16,tex+" width must be one Minecraft tile");
 assert.equal(buf.readUInt32BE(20),16,tex+" height must be one Minecraft tile");
 assert.equal(buf[24],8,tex+" must use 8-bit channels");
 assert.equal(buf[25],2,tex+" must be a painted, opaque RGB texture");
 bytes.add(buf.toString("base64"));
}
assert.equal(bytes.size,customTextures.length,"Textures must not be copied duplicates");
execFileSync(process.execPath,["tools/water-settling-tank/build-textures.mjs","--check"],
             {cwd:root,stdio:"inherit"});
const modelExample=json(assets+"models/multiblock/"+id+"_south/part_0_1_-2.json");
for(const mat of ["refractory","lining","bronze"])
 assert.ok(modelExample.textures[mat]?.includes("block/settling_tank/"),
     "Baffle does not use a new purposeful machine-specific tile: "+mat);
execFileSync(process.execPath,["tools/water-settling-tank/build.mjs","--check"],
             {cwd:root,stdio:"inherit"});
console.log("PASS: V4 low open settling tank, rear dirty/front clean pools, 4 facings, 6 air proxies, 164 local models, accessible real ports, original textures, conserved water, Patchouli projection and generators");
