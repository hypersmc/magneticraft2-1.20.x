import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import {fileURLToPath} from "node:url";
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),"../..");
const read=p=>fs.readFileSync(path.join(root,p),"utf8");
const json=p=>JSON.parse(read(p));
const java="src/main/java/com/magneticraft2/",res="src/main/resources/";
const blocks=read(java+"common/registry/registers/BlockRegistry.java");
const bes=read(java+"common/registry/registers/BlockEntityRegistry.java");
const items=read(java+"common/registry/registers/ItemRegistry.java");
const creative=read(java+"common/registry/registers/CreativeTabRegistry.java");
const client=read(java+"client/Clientsetup.java");
const book=json(res+"assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/steelage.json");
const lang=json(res+"assets/magneticraft2/lang/en_us.json");
const pick=json(res+"data/minecraft/tags/blocks/mineable/pickaxe.json");
const specs=[
 ["gear_medium","MediumGear","WroughtIronMediumGearBlock","MediumGearBlockEntity_wood",32],
 ["gear_large","LargeGear","WroughtIronLargeGearBlock","LargeGearBlockEntity_wood",48],
 ["shaft","Shaft","WroughtIronShaftBlock","ShaftBlockEntity_wood",48],
 ["gearbox","Gearbox","WroughtIronGearboxBlock","GearboxBlockEntity_wood",32]
];
for (const [n,suffix,ironBlock,copperBE,max] of specs) {
 const id="steel_"+n, reg="STEEL_"+n.toUpperCase();
 const block=read(java+"common/block/stage/steel/Steel"+suffix+"Block.java");
 const entity=read(java+"common/blockentity/stage/steel/Steel"+suffix+"BlockEntity.java");
 assert(block.includes("extends "+ironBlock),"inherit Iron placement: "+id);
 assert(entity.includes("extends "+copperBE),"reuse Gear V2 BE: "+id);
 assert(entity.includes("getGearMaxTorque() { return "+max+".0F; }"),"rating "+id);
 assert(blocks.includes('BLOCKS.register("'+id+'"'),"registered block "+id);
 assert(bes.includes('BLOCK_ENTITIES.register("'+id+'"'),"registered BE "+id);
 assert(items.includes("ITEM_"+reg+" = fromBlock("+reg+")"),"block item "+id);
 assert(creative.includes("ItemRegistry.ITEM_"+reg+".get()"),"creative tab "+id);
 assert(client.includes("BlockEntityRegistry."+reg+"_BE.get()"),"BER "+id);
 assert(pick.values.includes("magneticraft2:"+id),"mineable "+id);
 assert(lang["block.magneticraft2."+id],"translation "+id);
 assert(book.pages.some(p=>p.recipe==="magneticraft2:"+id),"Patchouli "+id);
 const state=json(res+"assets/magneticraft2/blockstates/"+id+".json");
 assert(JSON.stringify(state).includes("magneticraft2:block/"+id),"blockstate "+id);
 const b=json(res+"assets/magneticraft2/models/block/"+id+".json");
 const item=json(res+"assets/magneticraft2/models/item/"+id+".json");
 assert.equal(b.parent,"magneticraft2:block/wrought_iron_"+n);
 assert.equal(item.parent,"magneticraft2:item/wrought_iron_"+n);
 assert(!b.elements&&!item.elements,"no duplicate model geometry "+id);
 const loot=json(res+"data/magneticraft2/loot_tables/blocks/"+id+".json");
 assert(loot.pools.some(p=>p.entries.some(e=>e.name==="magneticraft2:"+id)),"drop "+id);
 const recipe=json(res+"data/magneticraft2/recipes/"+id+".json");
 assert.equal(recipe.type,"minecraft:crafting_shaped");
 assert.equal(recipe.result.item,"magneticraft2:"+id);
 for (const k of Object.keys(recipe.key)) assert(recipe.pattern.join("").includes(k),"unused recipe symbol "+id);
}
for(const s of ["MediumGear","LargeGear","Shaft","Gearbox"]) {
 const iron=read(java+"common/block/stage/iron/WroughtIron"+s+"Block.java");
 assert(iron.includes("public class WroughtIron"+s+"Block "), "Iron subclass hook "+s);
}
for(const name of ["REFINED_STEEL_PLATE","REFINED_STEEL_BAR","REFINED_STEEL_ROD","STEEL_ROLLER_SLEEVES"])
 assert(creative.includes("ItemRegistry.ITEM_"+name+".get()"),"Steel II creative "+name);
assert(!blocks.includes("SteelPowerGenerator"),"no new power source");
console.log("PASS Steel III: 4 block/BE/renderer recipes, drops, inherited models, 32/48 torque ratings, creative and book");
