#!/usr/bin/env node
/** Static K1 contract: no Java compiler, Gradle or actual Minecraft runtime. */
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import {fileURLToPath} from "node:url";
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),"../..");
const read=p=>fs.readFileSync(path.join(root,p),"utf8");
const json=p=>JSON.parse(read(p));
const pkg="src/main/java/com/magneticraft2/",assets="src/main/resources/assets/magneticraft2/",data="src/main/resources/data/magneticraft2/";
const id="bronze_industrial_air_blower",machine="magneticraft2:"+id;
const block=read(pkg+"common/block/stage/bronze/BronzeIndustrialAirBlowerBlock.java");
const be=read(pkg+"common/blockentity/stage/bronze/BronzeIndustrialAirBlowerBlockEntity.java");
const bellows=read(pkg+"common/blockentity/stage/stone/BellowsMultiblockModuleEntity.java");
const regs=read(pkg+"common/registry/registers/BlockEntityRegistry.java");
const blocks=read(pkg+"common/registry/registers/BlockRegistry.java");
const items=read(pkg+"common/registry/registers/ItemRegistry.java");
const client=read(pkg+"client/Clientsetup.java");
const renderer=read(pkg+"client/render/blocks/stage/bronze/BronzeIndustrialAirBlowerRenderer.java");
const recipe=json(data+"recipes/"+id+".json");
assert.equal(recipe.result.item,machine);
assert.equal(recipe.key.G.item,"magneticraft2:precision_bronze_gear");
assert.equal(recipe.key.I.item,"magneticraft2:bronze_bushing");
assert.match(block,/BlockEntityRegistry\.BRONZE_INDUSTRIAL_AIR_BLOWER_BE\.get\(\),\s*BronzeIndustrialAirBlowerBlockEntity::serverTick/);
assert.match(blocks,/BronzeIndustrialAirBlowerBlock::new/);
assert.match(regs,/BlockEntityType\.Builder\.of\(BronzeIndustrialAirBlowerBlockEntity::new,\s*BlockRegistry\.BRONZE_INDUSTRIAL_AIR_BLOWER\.get\(\)/);
assert.match(items,/ITEM_BRONZE_INDUSTRIAL_AIR_BLOWER/);
assert.match(client,/BronzeIndustrialAirBlowerRenderer::new/);
for(const s of ["bronze_industrial_air_blower_rotor","bronze_industrial_air_blower_coupling"])
 assert.ok(client.includes(s),"Missing baked renderer model "+s);
assert.match(renderer,/Axis\.ZP\.rotationDegrees\(angle\)/);
assert.match(be,/public static final int AIR_CAPACITY = 16/);
assert.match(be,/public static final int TRANSFER_RATE = 1/);
assert.match(be,/public static final int PUMP_INTERVAL = 5/);
assert.match(be,/public static final float MIN_RPM = 16\.0F/);
assert.match(be,/public static final float TORQUE_DEMAND = 5\.0F/);
assert.match(be,/GearNetworkManager\.getInstance\(\)/);
assert.match(be,/node\.getSpeed\(\) >= MIN_RPM/);
assert.match(be,/node\.getTorque\(\) \+ EPS >= TORQUE_DEMAND/);
assert.doesNotMatch(be,/canAttempt[^;]*!node\.isOverloaded\(\)/);
assert.match(be,/getMechanicalLoadState\(level, worldPosition\)\.supplied\(\)/);
assert.match(be,/if \(cap == CapabilityPressure\.PRESSURE\)/);
assert.match(be,/return formed && side == matchedFacing \? output\.cast\(\) : LazyOptional\.empty\(\)/);
assert.match(be,/int receivePressure\(int units, boolean simulate\) \{ return 0; \}/);
assert.ok(be.includes("private int drainAir(int units, boolean simulate)"));
assert.ok(be.includes("lastOutputTick == tick ? outputThisTick : 0"));
assert.ok(be.includes("pressureHandler.extractPressure(permitted, simulate)"));
assert.ok(be.includes("int offered = drainAir(TRANSFER_RATE, true)"));
assert.ok(be.includes("receiver.receivePressure(offered, false)"));
assert.ok(be.includes("drainAir(Math.min(accepted, offered), false)"));
assert.match(be,/getCapability\(CapabilityPressure\.PRESSURE, matchedFacing\.getOpposite\(\)\)/);
assert.match(be,/neighbor instanceof BellowsMultiblockModuleEntity bellows && !bellows\.isFormedModule\(\)/);
assert.match(be,/pressureHandler\.receivePressure\(1, false\)/);
assert.match(be,/saveMultiblockData\(tag, blueprintName, formed, replacementModel\)/);
assert.match(be,/tag\.putInt\("AirPumpProgress", pumpProgress\)/);
assert.match(be,/pressureHandler\.setPressure\(Math\.max\(0, Math\.min\(AIR_CAPACITY/);
assert.match(be,/if \(cap == ForgeCapabilities\.ITEM_HANDLER\) return LazyOptional\.empty\(\)/);
assert.match(bellows,/CapabilityPressure\.PRESSURE/);
assert.match(bellows,/public boolean isFormedModule\(\)/);
const design=json("tools/multiblock-models/designs/"+id+".json");
const voxels=json(data+"multiblock_voxels/"+id+".json");
assert.equal(design.schema,1);
assert.equal(voxels.format,1);
assert.deepEqual(voxels.collision,design.voxels.collision);
assert.deepEqual(voxels.selection,design.voxels.selection);
const book=json(assets+"patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json");
const guide=book.pages.find(p=>p.name==="Industrial Air Blower");
assert.ok(guide&&guide.multiblock.pattern.length===3);
const UV={north:[0,1],south:[0,1],east:[2,1],west:[2,1],up:[0,2],down:[0,2]};
let models=0,faces=0;
function check(m,file){
 assert.ok(m.textures,"No textures: "+file);
 for(const el of m.elements){
  assert.ok(el.from.every((v,i)=>v>=-16.001&&el.to[i]<=32.001&&v<el.to[i]),"Invalid cube bounds "+file);
  for(const[side,face]of Object.entries(el.faces)){
   const[u,v]=UV[side],uv=face.uv;
   assert.ok(uv&&uv.every(x=>x>=-0.001&&x<=16.001),"UV outside tile "+file);
   assert.ok(Math.abs(uv[2]-uv[0]-(el.to[u]-el.from[u]))<0.001
     &&Math.abs(uv[3]-uv[1]-(el.to[v]-el.from[v]))<0.001,"Texture stretch "+file);
   assert.ok(m.textures[face.texture.slice(1)],"Texture alias missing "+file);
   faces++;
  }
 }
}
for(const dir of ["south","west","north","east"]){
 const bp=json(data+"multiblocks/"+id+"_"+dir+".json");
 assert.deepEqual(bp.structure.dimensions,[3,3,3]);
 assert.equal(bp.settings.voxel_profile,machine);
 const counts={},hosts=[];let controller;
 for(let y=0;y<3;y++)for(let z=0;z<3;z++)for(let x=0;x<3;x++){
  const symbol=bp.structure.layout["layer"+(y+1)][z][x];
  assert.ok(bp.structure.blocks[symbol],"Unknown block symbol");
  counts[symbol]=(counts[symbol]||0)+1;
  if(symbol==="C")controller=[x,y,z];
  if(symbol!=="A")hosts.push([x,y,z,symbol]);
 }
 assert.equal(counts.C,1);
 assert.equal(counts.I,1);
 assert.equal(counts.A,4);
 assert.equal(bp.settings.render_air_anchors.length,4);
 for(const rel of bp.settings.render_air_anchors){
  const a=rel.map((v,i)=>v+controller[i]);
  assert.equal(bp.structure.layout["layer"+(a[1]+1)][a[2]][a[0]],"A");
  hosts.push([...a,"A"]);
 }
 assert.equal(hosts.length,27);
 for(const [x,y,z,symbol]of hosts){
  const key=[x-controller[0],y-controller[1],z-controller[2]].join("_");
  const file=assets+"models/multiblock/"+id+"_"+dir+"/part_"+key+".json";
  const model=json(file);
  if(symbol==="I")assert.equal(model.elements.length,0,"Real Gear V2 module occluded "+file);
  if(symbol==="C")assert.ok(model.elements.length,"Controller model missing");
  check(model,file);models++;
 }
 if(dir==="south"){
  assert.deepEqual(bp.structure.layout.layer1.map(r=>r.join("")),["SIS","SSS","SCS"]);
  const mirrored=Array.from({length:3},(_,n)=>bp.structure.layout["layer"+(3-n)]
    .map(row=>[...row].reverse().map(s=>s==="C"?"0":s==="A"?"_":s).join("")));
  assert.deepEqual(guide.multiblock.pattern,mirrored,"Patchouli != actual SOUTH structure");
 }
}
assert.equal(models,108);
for(const component of ["rotor","coupling"])check(json(assets+"models/multiblock/"+id+"_"+component+".json"),component);
const lang=json(assets+"lang/en_us.json");
assert.equal(lang["block.magneticraft2."+id],"Industrial Air Blower");
assert.match(read("tools/multiblock-models/generate.mjs"),/id==="bronze_industrial_air_blower"/);
for(const tag of ["mineable/pickaxe","needs_stone_tool"])
 assert.ok(json("src/main/resources/data/minecraft/tags/blocks/"+tag+".json").values.includes(machine));
console.log("Industrial Air Blower K1 static contract PASS: "+models+" hosts, "+faces+" native UV faces; no compile or gameplay claim.");
