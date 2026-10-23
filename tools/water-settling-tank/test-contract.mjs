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
assert.ok(inletRiser.elements.some(e=>e.from[1]===0&&e.to[1]===8
       &&e.from[0]===6&&e.from[2]===8),
       "Dirty inlet riser no longer joins the real Fluid Input below");
const eastDownpipe=json(assets+"models/multiblock/"+id+"_south/part_2_1_0.json");
assert.ok(eastDownpipe.elements.some(e=>e.from[1]===0&&e.to[1]===10
       &&e.from[0]===6&&e.from[2]===5),
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
assert.ok(vox.collision.some(b=>b[1]===0&&b[4]===16),
 "The visibly dry basin floor must be solid and continuous");
for(const point of [[-1,1,-2],[0,1,-2],[1,1,-2],[-1,1,-1],[0,1,-1],[1,1,-1]]){
 assert.ok(vox.collision.some(box=>inside(box,point)),
    "Missing low overflow weir or rear-of-port retaining lip: "+point);
}
assert.ok(vox.collision.every(box=>box[4] <= 32),
 "Tall posts, walkways or third-level collision geometry remain");

/**
 * V4.1 geometry regression: the previous renderer drew panels on the exact
 * top of the dry ceramic floor (and mutually overlapping inlet fittings).
 * Same-facing coplanar faces MUST NEVER share positive rendered area.
 * Touching walls/floor along opposing interior faces is perfectly valid.
 */
const intersections=[];
const coplanar=[];
for(let i=0;i<vox.collision.length;i++) {
 const a=vox.collision[i];
 for(let j=i+1;j<vox.collision.length;j++) {
  const b=vox.collision[j];
  const delta=[0,1,2].map(k=>Math.min(a[k+3],b[k+3])-Math.max(a[k],b[k]));
  if(delta.every(n=>n>0))
    intersections.push({i,j,volume:delta.reduce((x,y)=>x*y,1)});
  for(let axis=0;axis<3;axis++){
   const sides=[0,1,2].filter(k=>k!==axis);
   if(sides.every(k=>delta[k]>0) &&
       (a[axis]===b[axis] || a[axis+3]===b[axis+3]))
    coplanar.push({i,j,axis});
  }
 }
}
assert.deepEqual(intersections,[],
 "Overlapping cuboid volumes cause overlapping opaque faces and depth artifacts");
assert.deepEqual(coplanar,[],
 "Same-facing, same-plane surfaces are Z-fighting inside the generated model");

for(const dx of [-1,0,1])for(const dz of [-3,-2,-1]){
 const floor=json(assets+"models/multiblock/"+id+"_south/part_"+
        dx+"_0_"+dz+".json");
 assert.ok(floor.elements.some(el=>el.from[1]===0&&el.to[1]===16 &&
           el.faces.up.texture==="#refractory"),
   "Unfilled basin must show dark, dry refractory rather than a water-colored liner");
 assert.ok(!floor.elements.some(el=>el.faces.up.texture==="#sludge"),
   "Do not Z-fight with permanent floating sediment decals");
}
// Internal basin: left wall ends at X=-1, right wall starts at X=2.
// The low weir occupies Z [-1.1875,-1.0], while the front lip starts
// at Z=-0.125. A fluid surface MUST stay between these solids.
const fluidX=[-0.975,1.975],dirtyZ=[-3.53,-1.29],cleanZ=[-0.96,-0.24];
assert.ok(fluidX[0]>-1 && fluidX[1]<2,"Fluid crosses a physical side wall");
assert.ok(dirtyZ[0]>-3.625 && dirtyZ[1]<-1.1875,"Dirty water enters rear rim or weir");
assert.ok(cleanZ[0]>-1.0 && cleanZ[1]<-0.125,"Clean water clips the weir or front retaining lip");

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
assert.ok(1.045 + 0.495 > 1.50 &&
          1.045 + 0.495 < 1 + 9/16,
 "Full water must stay below the low transverse weir, not slice through it");
for (const s of [
  "tank.getDirtyFillRatio(),",
  "tank.getCleanFillRatio(),",
  "-0.975F, 1.975F, -3.53F, -1.29F",
  "-0.975F, 1.975F, -0.96F, -0.24F",
  "getDirtyAmount() > 0", "getCleanAmount() > 0",
  "float top = 1.045F + 0.495F",
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
console.log("PASS: Water Settling Tank V4.1 — zero overlapping solids or coplanar faces, clipped dry/clean fluid bounds, two-stage basin, 4 facings, 164 models, 6 anchors, recipes, book and generated textures");
