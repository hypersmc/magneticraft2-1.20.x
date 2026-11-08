// Static resource/source contract — cannot replace a Forge build or in-game test.
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import {fileURLToPath} from "node:url";
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),"../..");
const read=p=>fs.readFileSync(path.join(root,p),"utf8");
const json=p=>JSON.parse(read(p));
const asset="src/main/resources/",db=asset+"data/magneticraft2/multiblocks/";
const kiln=read("src/main/java/com/magneticraft2/common/blockentity/stage/bronze/RefractoryKilnBlockEntity.java");
const port=read("src/main/java/com/magneticraft2/common/blockentity/stage/copper/MultiblockFluidInputBlockEntity.java");
const recipe=json(asset+"data/magneticraft2/recipes/retorting_logs.json");
assert.deepEqual(recipe.fluid_output,{fluid:"magneticraft2:wood_tar",amount:250});
for(const [test,re,label] of [
 [kiln,/WOOD_TAR_IGNITION_MB = 50/,"50mB"],
 [kiln,/WOOD_TAR_BURN_TICKS = 80/,"80ticks"],
 [kiln,/WOOD_TAR_CAPACITY_MB = 4000/,"capacity"],
 [kiln,/KilnWoodTarFuelTank/,"saved tank"],
 [kiln,/consumed\.getAmount\(\) != WOOD_TAR_IGNITION_MB/,"exact debit"],
 [kiln,/controller\.getBlockMap\(\)\.get\(portPos\)/,"original formed module"],
 [kiln,/worldPosition\.equals\(port\.getControllerPos\(\)\)/,"module link"],
 [kiln,/cap == ForgeCapabilities\.ITEM_HANDLER \|\| cap == ForgeCapabilities\.FLUID_HANDLER/,"no controller bypass"],
 [port,/kiln\.getTarFuelPortHandler\(worldPosition\)/,"port delegates to kiln"]
])assert.match(test,re,label);
const xy={south:[0,3],north:[4,1],east:[3,4],west:[1,0]};
// Shared module picks the axis with greatest absolute delta; break
// ties in favor of X. All four ports must actually face OUT of the wall.
const controllers={south:[2,4],north:[2,0],east:[4,2],west:[0,2]};
const outward={south:"west",north:"east",east:"south",west:"north"};
const facing=(dx,dz)=>Math.abs(dx)>=Math.abs(dz)&&dx!==0
  ?dx>0?"east":"west":dz>0?"south":"north";
for(const [dir,[x,z]] of Object.entries(xy)){
 const [cx,cz]=controllers[dir];
 assert.equal(facing(x-cx,z-cz),outward[dir],"port must face its actual exterior wall");
 const old=json(db+"refractory_kiln_"+dir+".json");
 const newer=json(db+"refractory_kiln_tarfuel_"+dir+".json");
 assert.equal(newer.name,"refractory_kiln_tarfuel_"+dir);
 assert.equal(old.structure.layout.layer2[z][x],"B");
 assert.equal(newer.structure.layout.layer2[z][x],"F");
 assert.equal(newer.structure.blocks.F,"magneticraft2:multiblock_fluid_input");
 assert.deepEqual(old.structure.dimensions,newer.structure.dimensions);
 assert.equal(old.settings.voxel_profile,newer.settings.voxel_profile);
 assert.equal(old.settings.replacewhenformed,newer.settings.replacewhenformed);
 let diff=0;
 for(let layer=1;layer<=4;layer++)for(let zz=0;zz<5;zz++)for(let xx=0;xx<5;xx++){
   const a=old.structure.layout["layer"+layer][zz][xx];
   const b=newer.structure.layout["layer"+layer][zz][xx];
   if(a!==b){diff++;assert.deepEqual([layer,zz,xx,a,b],[2,z,x,"B","F"]);}
 }
 assert.equal(diff,1,"exactly one wall-brick swap");
}
const book=json(asset+"assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json");
const original=book.pages.find(p=>p.type==="patchouli:multiblock"&&p.name==="Refractory Kiln");
const alt=book.pages.find(p=>p.type==="patchouli:multiblock"&&p.name==="Refractory Kiln — Liquid Inlet");
assert(original&&alt);
assert.equal(original.multiblock.pattern[2][2],"B___B");
assert.equal(alt.multiblock.pattern[2][2],"B___B");
assert.equal(alt.multiblock.pattern[2][3],"B___F");
assert.equal(alt.multiblock.mapping.F,"magneticraft2:multiblock_fluid_input");
const lang=json(asset+"assets/magneticraft2/lang/en_us.json");
assert(lang["message.magneticraft2.refractory_kiln.status"].includes("Wood Tar: %s/%s mB"));
console.log("PASS: four additive liquid Kilns, unchanged originals, private port, persisted exact-debit Wood Tar heat");
