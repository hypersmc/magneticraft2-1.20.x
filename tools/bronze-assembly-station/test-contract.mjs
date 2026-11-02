#!/usr/bin/env node
// Static K1 contract: run from repository root with Node 18+.
// This is NOT a Forge build or in-game rendering/automation test.
import fs from "node:fs";
import path from "node:path";
const root=process.cwd(), id="bronze_mechanical_assembly_station";
const read=p=>JSON.parse(fs.readFileSync(path.join(root,p),"utf8"));
const assert=(test,message)=>{if(!test)throw Error(message)};
const dirs=["south","west","north","east"];
const models="src/main/resources/assets/magneticraft2/models/multiblock/";
const blueprints="src/main/resources/data/magneticraft2/multiblocks/";
const pattern=read("src/main/resources/assets/magneticraft2/blockstates/"+id+".json");
const vox=read("src/main/resources/data/magneticraft2/multiblock_voxels/"+id+".json");
assert(vox.format===1&&vox.unit==="model_pixels","Invalid voxel profile");
assert(vox.collision.length>0&&vox.collision.length<=512,"Invalid voxel geometry");
const modules=["magneticraft2:mechanical_input_module",
"magneticraft2:multiblock_item_input","magneticraft2:multiblock_item_primary_output"];
for(const dir of dirs){
 const bp=read(blueprints+id+"_"+dir+".json"),s=bp.structure;
 assert(JSON.stringify(s.dimensions)==="[5,3,5]",dir+": footprint changed");
 assert(bp.settings.voxel_profile==="magneticraft2:"+id,dir+": missing V2");
 const flat=Object.values(s.layout).flat(2);
 assert(flat.filter(x=>s.blocks[x]=== "magneticraft2:"+id).length===1,dir+": controller");
 for(const name of modules)assert(flat.filter(x=>s.blocks[x]===name).length===1,
    dir+": missing/repeated actual module "+name);
 assert(bp.settings.render_air_anchors.length===1,dir+": expected one supported fixture anchor");
 const layer=s.layout.layer1,idx=[];
 for(let z=0;z<5;z++)for(let x=0;x<5;x++)if(s.blocks[layer[z][x]]==="magneticraft2:"+id)idx.push([x,z]);
 assert(idx.length===1,dir+": missing controller");
 const [cx,cz]=idx[0];
 let count=0;
 for(let y=0;y<3;y++)for(let z=0;z<5;z++)for(let x=0;x<5;x++){
  const k=s.layout["layer"+(y+1)][z][x],block=s.blocks[k];
  const anchor=bp.settings.render_air_anchors.some(a=>a[0]===x-cx&&a[1]===y&&a[2]===z-cz);
  if(block==="minecraft:air"&&!anchor)continue;
  if(modules.includes(block))continue;
  const rel=[x-cx,y,z-cz].join("_"),file=models+id+"_"+dir+"/part_"+rel+".json";
  const m=read(file); count++;
  assert(m.ambientocclusion===false&&m.elements.length>0,dir+": missing model "+file);
  for(const el of m.elements){
   for(let axis=0;axis<3;axis++)assert(el.from[axis]>=0&&el.to[axis]<=16
    &&el.to[axis]>el.from[axis],dir+": out-of-cell geometry "+file);
   for(const [face,data] of Object.entries(el.faces)){
    const axes=(face==="north"||face==="south")?[0,1]:
      (face==="east"||face==="west")?[2,1]:[0,2];
    assert(Math.abs(data.uv[2]-data.uv[0]-(el.to[axes[0]]-el.from[axes[0]]))<1e-6
      &&Math.abs(data.uv[3]-data.uv[1]-(el.to[axes[1]]-el.from[axes[1]]))<1e-6,
     dir+": stretched texture "+file);
   }
  }
 }
 assert(count===44,dir+": wrong model host count: "+count);
 assert(pattern.variants["facing="+dir+",is_formed=true"].model.endsWith("/part_0_0_0"),
     dir+": wrong formed controller model");
}
const assembly=read("src/main/resources/data/magneticraft2/recipes/assembly_mechanical_water_pump.json");
assert(assembly.type==="magneticraft2:mechanical_assembly", "Wrong serializer");
assert(assembly.ingredients.length===4,"Wrong number of parts");
assert(assembly.ingredients.map(x=>x.count).join(",")==="2,1,1,1","Wrong mass balance");
assert(assembly.result.item==="magneticraft2:mechanical_water_pump","Wrong first product");
const manual=read("src/main/resources/data/magneticraft2/recipes/mechanical_water_pump_bronze_bushing.json");
assert(manual.result.item===assembly.result.item,"Manual alternative removed");
const book=read("src/main/resources/assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json");
const construction = book.pages.find(p=>
 p.type==="patchouli:multiblock" && p.name==="Mechanical Assembly Station");
assert(construction,"Patchouli build guide missing");
// Patchouli patterns are TOP-to-BOTTOM, while JSON layer1 is the foundation.
// Check every construction cell against all four rotated server blueprints.
const schematic = construction.multiblock;
assert(schematic.pattern.length===3 && schematic.pattern.every(layer=>
 layer.length===5 && layer.every(row=>row.length===5)),
 "Patchouli dimensions don't match 5x3x5 server multiblock");
const canonical=(x,y,z)=>{
 const symbol=schematic.pattern[2-y][z][x];
 return symbol==="_"?"minecraft:air":schematic.mapping[symbol];
};
for(let n=0;n<dirs.length;n++){
 const dir=dirs[n],server=read(blueprints+id+"_"+dir+".json").structure;
 for(let y=0;y<3;y++)for(let z=0;z<5;z++)for(let x=0;x<5;x++){
  let xx=x,zz=z;
  for(let turn=0;turn<n;turn++){const oldX=xx;xx=4-zz;zz=oldX;}
  const bookBlock=canonical(x,y,z);
  const key=server.layout["layer"+(y+1)][zz][xx];
  const serverBlock=server.blocks[key];
  assert(bookBlock===serverBlock,
   "Patchouli mismatch "+dir+" layer="+(y+1)+" x="+xx+" z="+zz+
   ": expected "+serverBlock+", book shows "+bookBlock);
 }
}
console.log("PASS Bronze Assembly K1: 4 layouts, 176 part models, voxel shapes, UV, recipe and guide");
