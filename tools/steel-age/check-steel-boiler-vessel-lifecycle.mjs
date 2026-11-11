import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

// Source/resource contract ONLY. Never report this checker as proof of in-world
// drops, save/reload, performance, multiclient consistency or successful Gradle.
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const text = p => readFileSync(path.join(root, p), "utf8");
const json = p => JSON.parse(text(p));
const data = "src/main/resources/data/magneticraft2/";
const java = "src/main/java/com/magneticraft2/common/";
const names = ["south", "north", "east", "west"];
const structure = Object.fromEntries(names.map(n => [
  n, json(data + "multiblocks/steel_boiler_vessel_" + n + ".json").structure
]));
const canonical = structure.south.layout;
const rotate = (x,z,facing) => ({
  south:[x,z], north:[2-x,4-z], east:[z,2-x], west:[4-z,x]
})[facing];
const expected = {S:24, J:2, P:16, F:10, C:1, A:7};
const positions = {};

for (const facing of names) {
  const s = structure[facing];
  const width = facing === "east" || facing === "west" ? 5 : 3;
  const depth = facing === "east" || facing === "west" ? 3 : 5;
  assert.deepEqual(s.dimensions, [width,4,depth], facing + " dimensions");
  const counts = Object.fromEntries(Object.keys(expected).map(k=>[k,0]));
  const cells = {};
  for(let y=0;y<4;y++)for(let z=0;z<5;z++)for(let x=0;x<3;x++){
    const type=canonical["layer"+(y+1)][z][x];
    const [X,Z]=rotate(x,z,facing);
    const cell=s.layout["layer"+(y+1)][Z][X];
    assert.equal(cell,type,"rotated blueprint cell "+[facing,x,y,z]);
    counts[cell]++;
    assert(s.blocks[cell],"unknown symbol "+cell);
    (cells[cell]??=[]).push([X,y,Z]);
  }
  assert.deepEqual(counts,expected,"material/air conservation "+facing);
  assert.equal(cells.C.length,1,"one controller "+facing);
  positions[facing]=cells.C[0];
}
assert.deepEqual(positions,{south:[1,0,4],north:[1,0,0],east:[4,0,1],west:[0,0,1]});

// Capture the original world states before converting parts to placeholders;
// the destructive lifecycle must still skip original AIR, persist the map, and
// clear it after a teardown. These assertions guard a shared system only;
// no shared controller or filler changes are made by Steel IV-C.
const core=text(java+"systems/Multiblocking/core/MultiblockController.java");
assert(core.includes("blockMap.put(currentPos, currentState)"),"original state snapshot");
assert(core.includes("BlockRegistry.multiblockfiller.get().defaultBlockState()"),"filler replacement");
assert(core.includes("if (block != Blocks.AIR)"),"empty air may not drop an item");
assert(core.includes("blockMap.clear();"),"teardown state cleared");
assert(core.includes('tag.put("blockMap", blockMapList)'),"persist original block map");
assert(core.includes('tag.getList("blockMap", CompoundTag.TAG_COMPOUND)'),"restore original block map");
assert(core.includes("BlockState.CODEC.encodeStart"),"store actual block properties");
assert(core.includes("BlockState.CODEC.parse"),"restore original block properties");
const block=text(java+"block/stage/steel/SteelBoilerVesselBlock.java");
const vessel=text(java+"blockentity/stage/steel/SteelBoilerVesselBlockEntity.java");
const filler=text(java+"block/general/Multiblockfiller.java");
assert(block.includes("vessel.onDestroy(world)"),"breaking controller delegates teardown");
assert(filler.includes("multiblockController.onDestroy(level)"),"breaking filler delegates teardown");
assert(vessel.includes("saveMultiblockData(tag,blueprintName,formed,replacementModel)"),
       "controller saves multiblock");
assert(vessel.includes("loadMultiblockData(tag)"),"controller reloads multiblock");
assert(vessel.includes("MultiblockVoxelNbt.write"),"controller saves voxel collision");
assert(vessel.includes("MultiblockVoxelNbt.read"),"controller reloads voxel collision");
assert(vessel.includes("MultiblockRenderParts.assign("),"static mesh assignment");
assert(!vessel.includes("serverTick"),"no ticking boiler during Steel IV-C");
for(const token of ["pressurecape(){return false;","fluidcape(){return false;",
                    "heatcape(){return false;","wattcape(){return false;"])
  assert(vessel.includes(token),"no operational fluid/heat/power capability: "+token);

console.log("PASS Steel IV-C lifecycle static contract: all four directions, 53 real blocks, seven air cells, source persistence/teardown guards.");
console.log("NOT VERIFIED: Forge compilation, formed-world drops exactly once, save/reload, chunk boundaries, player/creative/multiplayer tests.");
