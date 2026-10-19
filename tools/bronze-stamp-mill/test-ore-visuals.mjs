#!/usr/bin/env node
// Bronze Stamp Mill: visible feed -> three stamp stages -> physical outputs.
// No inventory writes or new module voxels are permitted by this feature.
import assert from "node:assert/strict";
import fs from "node:fs";

const j="src/main/java/com/magneticraft2/";
const render=fs.readFileSync(j+
 "client/render/blocks/stage/bronze/BronzeStampMillRenderer.java","utf8");
const ore=fs.readFileSync(j+
 "client/render/blocks/stage/bronze/BronzeStampMillOreVisuals.java","utf8");
const controller=fs.readFileSync(j+
 "common/blockentity/stage/bronze/BronzeStampMillBlockEntity.java","utf8");
const port=fs.readFileSync(j+
 "common/blockentity/stage/copper/MultiblockItemPortBlockEntity.java","utf8");
const blueprint=JSON.parse(fs.readFileSync(
 "src/main/resources/data/magneticraft2/multiblocks/bronze_stamp_mill_south.json","utf8"));
const design=JSON.parse(fs.readFileSync(
 "tools/multiblock-models/designs/bronze_stamp_mill.json","utf8"));

assert.ok(render.includes("oreVisuals.render(mill,partial,pose,buffers,light,overlay)"));
assert.ok(!render.includes("pose.translate(.5D,24D/16D,-10D/16D)"),
 "Ore must not just appear in the middle of the Stamp Mill");
assert.ok(render.includes("BronzeStampMillBeltRenderer.render("),
 "Accepted animated leather transmission is preserved");
assert.ok(ore.includes("getInputStack()")&&ore.includes("getOutputStack()")&&
 ore.includes("getByproductStack()")&&
 ore.includes("getVisualProcessProgress(partial)")&&
 ore.includes("getProcessingPrimaryPreview()"),
 "Ore visuals must read physical inventory and real recipe progress");
assert.ok(controller.includes("syncInventoryVisualStateIfChanged()")&&
 controller.includes("sync();")&&
 controller.includes("ItemStack.isSameItemSameTags("),
 "Server must send inventory changes for automation AND player input");
assert.ok(port.includes("return controller.itemHandler")&&
 port.includes(".insertItem(")&&
 port.includes("boolean simulate"),
 "Forge automation must still insert through the real input capability");
assert.ok(!ore.includes("insertItem(")&&!ore.includes("extractItem(")&&
 !ore.includes("setStackInSlot(")&&!ore.includes("setMechanicalLoad("),
 "A visual feed must never duplicate, consume or move inventory items");
assert.ok(!ore.includes("Thread.sleep")&&!ore.includes("System.currentTimeMillis"),
 "Render timing must track Minecraft game ticks, not wall-clock timers");
assert.ok(ore.includes("MAX_FEED_ITEMS = 3")&&
 ore.includes("Math.min(incoming, 2)")&&
 ore.includes("state.feeding.removeIf("),
 "Automation input bursts must be capped and finished animations removed");
assert.ok(ore.includes("original;")&&
 !ore.includes("ItemStack stack = original.copy();"),
 "Prevent unnecessary ItemStack allocations every render frame");

const layout=blueprint.structure.layout.layer1;
assert.deepEqual(blueprint.structure.dimensions,[5,3,4]);
assert.equal(layout[3][1],"U");
assert.equal(layout[3][2],"Y");
assert.equal(layout[3][3],"O");
assert.equal(blueprint.structure.blocks.U,"magneticraft2:multiblock_item_input");
assert.equal(blueprint.structure.blocks.Y,
 "magneticraft2:multiblock_item_byproduct_output");
assert.equal(blueprint.structure.blocks.O,
 "magneticraft2:multiblock_item_primary_output");
const lanes=[-8,8,24];
for(let k=0;k<lanes.length;k++){
 const start=design.boxes.find(b=>b.name==="ore_chute_front_"+k);
 const back=design.boxes.find(b=>b.name==="ore_chute_back_"+k);
 assert.ok(start&&back,"Existing physical ore chute missing for lane "+k);
 assert.ok(start.from[0]<=lanes[k]&&start.to[0]>=lanes[k]&&
           back.from[0]<=lanes[k]&&back.to[0]>=lanes[k],
  "Visual ore lane is not supported by physical chute geometry "+k);
 assert.ok(start.to[2]>=17&&back.from[2]<=-4,
  "Chute must run from its front opening to the crushing bed");
}
const lerp=(from,to,t)=>from+(to-from)*Math.max(0,Math.min(1,t));
const segment=(a,b,t)=>a.map((v,i)=>lerp(v,b[i],t));
function incoming(t){
 if(t<.18)return segment([-8,16.2,24],[-8,19.8,17.8],t/.18);
 if(t<.8)return segment([-8,19.8,17.8],[-8,22,-3],(t-.18)/.62);
 return segment([-8,22,-3],[-8,24.1,-10],(t-.8)/.20);
}
function outgoing(x,t){
 if(t<.2)return segment([x,24.1,-10],[x,26.9,-4],t/.2);
 if(t<.75)return segment([x,26.9,-4],[x,20.1,17],(t-.2)/.55);
 return segment([x,20.1,17],[x,16.4,24],(t-.75)/.25);
}
const close=(actual,expected)=>actual.every((v,i)=>Math.abs(v-expected[i])<1e-7);
for(const [t,expected] of [[0,[-8,16.2,24]],[.18,[-8,19.8,17.8]],
 [.8,[-8,22,-3]],[1,[-8,24.1,-10]]]){
 assert.ok(close(incoming(t),expected),"Input feed waypoint drift at t="+t);
}
for(const portX of [8,24]){
 for(const [t,expected]of [[0,[portX,24.1,-10]],[.2,[portX,26.9,-4]],
  [.75,[portX,20.1,17]],[1,[portX,16.4,24]]]){
  assert.ok(close(outgoing(portX,t),expected),
   "Output chute waypoint drift at t="+t+" lane="+portX);
 }
}
// All sampled items stay over their respective *real* chute lane rather
// than teleporting across the machine or entering other port modules.
for(let i=0;i<=100;i++){
 const t=i/100,feed=incoming(t);
 assert.equal(feed[0],lanes[0],"Input visual left its physical chute lane");
 assert.ok(feed[2]>=-10-1e-7&&feed[2]<=24+1e-7);
 assert.ok(feed[1]>=16&&feed[1]<=25);
 for(const k of [1,2]){
  const output=outgoing(lanes[k],t);
  assert.equal(output[0],lanes[k]);
  assert.ok(output[2]>=-10-1e-7&&output[2]<=24+1e-7);
  assert.ok(output[1]>=16&&output[1]<=27);
 }
}
assert.ok(ore.includes("FEED_TICKS = 24.0D")&&
          ore.includes("DISCHARGE_TICKS = 20.0D"),
 "Feed and ejection must be animated, not instantaneous");
for(const snippet of ["if (t < 0.18D)","if (t < 0.80D)",
 "if (t < 0.20D)","if (t < 0.75D)",
 "new Vec3(ax+(bx-ax)*t,ay+(by-ay)*t,az+(bz-az)*t)"]){
 assert.ok(ore.includes(snippet),"Authored animation route changed: "+snippet);
}
for(const stage of ["progress < 0.34F","progress < 0.68F",
 "mill.getProcessingPrimaryPreview()","0.51F,0F","0.32F,32F","0.27F,-22F"]){
 assert.ok(ore.includes(stage),"Missing progressive crushing feedback "+stage);
}
assert.ok(ore.includes("mill.isProcessing()")&&
 ore.includes("LOW_HAMMER_THRESHOLD")&&
 ore.includes("now-state.lastDustTick[i] < 3.0D")&&
 ore.includes("for(int i=0;i<2;i++)")&&
 ore.includes("level.isClientSide")&&
 ore.includes("BlockParticleOption(ParticleTypes.BLOCK,stone)"),
 "Dust should only trigger at real active-ore hammer impacts, with a cap");
assert.ok(ore.includes("case WEST -> -90D;")&&
 ore.includes("case NORTH -> 180D;")&&
 ore.includes("case EAST -> 90D;")&&
 ore.includes("Math.cos(angle)*dx+Math.sin(angle)*dz"),
 "Crushing particles must follow all four multiblock facings");
for(const file of ["bronze_stamping_chalcocite.json",
                   "bronze_stamping_cassiterite.json"]){
 const data=JSON.parse(fs.readFileSync(
  "src/main/resources/data/magneticraft2/recipes/"+file,"utf8"));
 assert.equal(data.machine,"magneticraft2:bronze_stamp_mill");
 assert.equal(data.input_count,2);
}
console.log("Stamp Mill ore visuals PASS: physical port/chute alignment, "+
 "24-tick feed, 3 crushing stages, 20-tick discharge and capped dust");
