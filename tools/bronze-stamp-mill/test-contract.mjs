#!/usr/bin/env node
import fs from "node:fs";
import assert from "node:assert/strict";

const read=(path)=>JSON.parse(fs.readFileSync(path,"utf8"));
const root="src/main/resources";
const asset=name=>root+"/assets/magneticraft2/models/multiblock/bronze_stamp_mill_"+name+".json";
const dirs=["south","west","north","east"];
const design=read("tools/multiblock-models/designs/bronze_stamp_mill.json");
const vox=read(root+"/data/magneticraft2/multiblock_voxels/bronze_stamp_mill.json");
assert.equal(design.schema,1);
assert.equal(vox.format,1);
assert.equal(vox.collision.length,design.voxels.collision.length);
assert.deepEqual(vox.collision,design.voxels.collision);
assert.deepEqual(vox.selection,design.voxels.selection);
assert.ok(vox.collision.length<=512);
const moduleSpace={lo:[0,0,-32],hi:[16,16,-16]};
assert.ok(design.boxes.every(b=>!b.from.every((v,i)=>
  Math.min(b.to[i],moduleSpace.hi[i])-Math.max(v,moduleSpace.lo[i])>1e-5)),
  "Rendering V2 geometry may not occupy the actual non-filler input module cell");

let referenceCount=null;
for(const dir of dirs){
  const bp=read(root+"/data/magneticraft2/multiblocks/bronze_stamp_mill_"+dir+".json");
  const [w,h,d]=bp.structure.dimensions;
  assert.equal(h,3);
  assert.equal(w*d,15);
  const counts={},coords={},physical=[];
  for(let y=0;y<h;y++)for(let z=0;z<d;z++)for(let x=0;x<w;x++){
    const name=bp.structure.layout["layer"+(y+1)][z][x];
    const type=bp.structure.blocks[name];
    assert.ok(type,"Unknown blueprint cell type "+name);
    counts[name]=(counts[name]||0)+1;
    (coords[name]??=[]).push([x,y,z]);
    if(type!=="minecraft:air")physical.push([x,y,z]);
  }
  assert.equal(counts.C,1);assert.equal(counts.I,1);
  assert.equal(counts.U,1);assert.equal(counts.O,1);assert.equal(counts.Y,1);
  const expected=JSON.stringify(Object.entries(counts).sort());
  if(referenceCount)assert.equal(expected,referenceCount);
  else referenceCount=expected;
  const [cx,cy,cz]=coords.C[0],[ix,iy,iz]=coords.I[0];
  assert.equal(Math.abs(cx-ix)+Math.abs(cz-iz),2);
  assert.equal(cy,0);assert.equal(iy,0);
  assert.equal(bp.settings.voxel_profile,"magneticraft2:bronze_stamp_mill");
  const anchors=bp.settings.render_air_anchors;
  assert.equal(anchors.length,3);
  const actual=new Set(physical.map(([x,y,z])=>[x-cx,y-cy,z-cz].join("_")));
  for(const [x,y,z] of anchors){
    assert.equal(bp.structure.layout["layer"+(y+cy+1)][z+cz][x+cx],"A");
    const key=[x,y,z].join("_");
    assert.ok(!actual.has(key),"Duplicate air anchor "+key);
    actual.add(key);
  }
  const expectedPartCount=physical.length+anchors.length;
  assert.equal(actual.size,expectedPartCount);
  for(const key of actual){
    const model=read(root+"/assets/magneticraft2/models/multiblock/bronze_stamp_mill_"+dir+"/part_"+key+".json");
    for(const element of model.elements){
      for(const v of [...element.from,...element.to])assert.ok(v>=-16-1e-5&&v<=32+1e-5);
      for(const face of Object.values(element.faces||{})){
        assert.equal(face.uv.length,4);
        assert.ok(face.uv.every(n=>n>=-1e-5&&n<=16+1e-5),
          "No texture stretching: "+dir+"/"+key);
      }
    }
  }
  const module=[ix-cx,iy-cy,iz-cz].join("_");
  assert.equal(read(root+"/assets/magneticraft2/models/multiblock/bronze_stamp_mill_"+dir+"/part_"+module+".json").elements.length,0,
    "Mechanical module owns its BER model; no V2 faces are allowed there");
}
for(const ore of ["chalcocite","cassiterite"]){
  const p=read(root+"/data/magneticraft2/recipes/bronze_stamping_"+ore+".json");
  assert.equal(p.type,"magneticraft2:multiblock_processing");
  assert.equal(p.machine,"magneticraft2:bronze_stamp_mill");
  assert.equal(p.input.item,"magneticraft2:"+ore+"_ore");
  assert.equal(p.input_count,2);
  assert.equal(p.output.item,"magneticraft2:crushed_"+ore);
  assert.equal(p.output.count,2);
  assert.equal(p.mechanical.min_speed,24);
  assert.equal(p.mechanical.torque,6);
  assert.equal(p.process_time,115);
}
for(const id of ["hammer","input_frame","cam"]){
  const model=read(asset(id));
  assert.ok(model.elements?.length>0,"Missing animated part "+id);
}
const book=read(root+"/assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json");
assert.ok(book.pages.some(page=>page.type==="patchouli:multiblock"&&page.name==="Bronze Stamp Mill"));
assert.ok(book.pages.some(page=>page.recipe==="magneticraft2:bronze_stamp_mill"));
const javaDir="src/main/java/com/magneticraft2/";
const be=fs.readFileSync(javaDir+"common/blockentity/stage/bronze/BronzeStampMillBlockEntity.java","utf8");
assert.ok(be.includes('MultiblockRenderParts.assign(')&&be.includes('MultiblockRecipeHandler.findRecipe('));
assert.ok(be.includes("stampShoe.completeBatch()")&&be.includes("setMechanicalLoad("));
const client=fs.readFileSync(javaDir+"client/Clientsetup.java","utf8");
for(const id of ["hammer","input_frame","cam"])
  assert.ok(client.includes('"multiblock/bronze_stamp_mill_'+id+'"'),"Unregistered animated model "+id);
assert.ok(client.includes("BRONZE_STAMP_MILL_BE"));
const jei=fs.readFileSync(javaDir+"compatibility/JEI/MagneticraftJeiPlugin.java","utf8");
assert.ok(jei.includes("MultiblockJeiRecipe.BRONZE_STAMP_MILL"));
console.log("Bronze Stamp Mill contract PASS: 4 layouts, V2 parts/voxels, 2 ore recipes, JEI and book");
