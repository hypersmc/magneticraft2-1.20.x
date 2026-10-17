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

// Nondecorative static geometry must have an actual support path to the
// ground-level sill. Boxes may overlap or share a full supporting face;
// touching at only an edge or point does not count as a structural joint.
const touchesFace=(a,b)=>{
  const overlaps=[0,1,2].map(i=>Math.min(a.to[i],b.to[i])-Math.max(a.from[i],b.from[i]));
  return overlaps.every(v=>v>=-1e-5)&&overlaps.filter(v=>v>1e-5).length>=2;
};
const foundation=design.boxes.find(b=>b.name==="front_sill");
assert.ok(foundation,"Stamp Mill needs an anchored front sill");
const supported=new Set([foundation.name]);
const frontier=[foundation];
while(frontier.length){
  const current=frontier.shift();
  for(const block of design.boxes){
    if(!supported.has(block.name)&&touchesFace(current,block)){
      supported.add(block.name);
      frontier.push(block);
    }
  }
}
assert.equal(supported.size,design.boxes.length,
    "Floating static parts: "+design.boxes.filter(b=>!supported.has(b.name)).map(b=>b.name).join(", "));
for(const id of ["trough_floor","mount_rear_undertray_girder",
                 "mount_front_undertray_girder","mount_upper_rear_guide_rail",
                 "mount_upper_front_guide_rail","mount_crank_pillow_16"]){
  assert.ok(supported.has(id),id+" must be bolted into the load-bearing frame");
}
for(const prefix of ["stamp_top_brace_","cam_bearing_"]){
  assert.ok(design.boxes.every(b=>!b.name.startsWith(prefix)),
    "Legacy intersecting cam brackets must not return: "+prefix);
}

const moduleSpace={lo:[0,0,-32],hi:[16,16,-16]};
assert.ok(design.boxes.every(b=>!b.from.every((v,i)=>
  Math.min(b.to[i],moduleSpace.hi[i])-Math.max(v,moduleSpace.lo[i])>1e-5)),
  "Rendering V2 geometry may not occupy the actual non-filler input module cell");

// Design geometry and BOTH voxel outlines must reserve all 4 complete
// non-filler block volumes, including I/O ports. They aren't decorative
// 3D blocks we can embed other cuboids through.
const southLayout=read(root+"/data/magneticraft2/multiblocks/bronze_stamp_mill_south.json");
const [SW,SH,SD]=southLayout.structure.dimensions;
const ports=[];
let control=null;
for(let y=0;y<SH;y++)for(let z=0;z<SD;z++)for(let x=0;x<SW;x++){
  const symbol=southLayout.structure.layout["layer"+(y+1)][z][x];
  if(symbol==="C")control=[x,y,z];
  if("IUYO".includes(symbol))ports.push({symbol,coord:[x,y,z]});
}
assert.ok(control&&ports.length===4);
for(const p of ports){
  const lo=p.coord.map((v,i)=>16*(v-control[i])),hi=lo.map(v=>v+16);
  for(const [listName,list] of [["model",design.boxes.map(b=>[...b.from,...b.to])],
     ["collision",vox.collision],["selection",vox.selection]]){
    for(const b of list){
      const overlap=lo.every((v,i)=>Math.min(b[i+3],hi[i])-Math.max(b[i],v)>1e-5);
      assert.ok(!overlap,listName+" invades full "+p.symbol+" module cell at "+p.coord);
    }
  }
}

let referenceCount=null;
for(const dir of dirs){
  const bp=read(root+"/data/magneticraft2/multiblocks/bronze_stamp_mill_"+dir+".json");
  const [w,h,d]=bp.structure.dimensions;
  assert.equal(h,3);
  assert.equal(w*d,20,"Stamp Mill must have a 5x4 ground footprint (rotated per facing)");
  const counts={},coords={},physical=[];
  for(let y=0;y<h;y++)for(let z=0;z<d;z++)for(let x=0;x<w;x++){
    const name=bp.structure.layout["layer"+(y+1)][z][x];
    const type=bp.structure.blocks[name];
    assert.ok(type,"Unknown blueprint cell type "+name);
    counts[name]=(counts[name]||0)+1;
    (coords[name]??=[]).push([x,y,z]);
    if(type!=="minecraft:air")physical.push([x,y,z]);
  }
  assert.equal(counts.A,12,"Three stamp AIR cells plus nine service/drive AIR cells");
  assert.equal(counts.C,1);assert.equal(counts.I,1);
  assert.equal(counts.U,1);assert.equal(counts.O,1);assert.equal(counts.Y,1);
  const expected=JSON.stringify(Object.entries(counts).sort());
  if(referenceCount)assert.equal(expected,referenceCount);
  else referenceCount=expected;
  const [cx,cy,cz]=coords.C[0],[ix,iy,iz]=coords.I[0];
  assert.equal(Math.abs(cx-ix)+Math.abs(cz-iz),2);
  assert.equal(cy,0);assert.equal(iy,0);
  assert.equal(bp.settings.voxel_profile,"magneticraft2:bronze_stamp_mill");
  // Module blocks are grounded, exposed to the outside of the footprint
  // and NOT built as a row of hovering bins above the workshop frame.
  for(const moduleType of ["U","Y","O"]){
    const [mx,my,mz]=coords[moduleType][0];
    assert.equal(my,0,"Service hopper must sit on ground: "+dir);
    const exterior = mx===0 || mx===w-1 || mz===0 || mz===d-1;
    assert.ok(exterior,"Service hopper needs an exposed face for item transport: "+dir);
  }
  const anchors=bp.settings.render_air_anchors;
  assert.equal(anchors.length,9,"Three rows of upper-air cells serve stamp heads and service chutes");
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
  // All four real module cells keep their original block entities after
  // formation and are not converted into filler render hosts.
  for(const moduleType of ["I","U","Y","O"]){
    const [mx,my,mz]=coords[moduleType][0];
    const key=[mx-cx,my-cy,mz-cz].join("_");
    const part=read(root+"/assets/magneticraft2/models/multiblock/bronze_stamp_mill_"+dir+"/part_"+key+".json");
    assert.equal(part.elements.length,0,
      "Non-filler "+moduleType+" must not have static V2 faces in "+dir);
  }
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

// Each animated model contains *only visible face panels*. This prevents
// z-fighting where full cuboids previously drew matching internal faces.
const faceAxes={north:[2,-1,0,1],south:[2,1,0,1],
                east:[0,1,2,1],west:[0,-1,2,1],
                up:[1,1,0,2],down:[1,-1,0,2]};
for(const id of ["hammer","hammer_worn","input_frame","cam"]){
  const model=read(asset(id));
  assert.ok(model.elements?.length>0,"Missing animated part "+id);
  const panels=new Map();
  for(const e of model.elements){
    assert.ok(e.from.every((v,i)=>v>=-1e-5&&e.to[i]<=16+1e-5&&v<e.to[i]),
       "Invalid moving part element bounds: "+id);
    for(const [face,render] of Object.entries(e.faces)){
      const [axis,sign,u,v]=faceAxes[face];
      assert.ok(render.uv.every(x=>x>=-1e-5&&x<=16+1e-5),
          id+": UV outside native atlas tile");
      assert.ok(Math.abs(render.uv[2]-render.uv[0]-(e.to[u]-e.from[u]))<1e-4
          &&Math.abs(render.uv[3]-render.uv[1]-(e.to[v]-e.from[v]))<1e-4,
          id+": stretched texture detected on "+face);
      const plane=sign>0?e.to[axis]:e.from[axis],key=face+":"+plane.toFixed(5);
      const neighbors=panels.get(key)||[];
      for(const prior of neighbors){
        const dx=Math.min(e.to[u],prior.to[u])-Math.max(e.from[u],prior.from[u]);
        const dy=Math.min(e.to[v],prior.to[v])-Math.max(e.from[v],prior.from[v]);
        assert.ok(dx<=1e-5||dy<=1e-5,
           id+": duplicate coplanar "+face+" faces at "+plane);
      }
      neighbors.push(e);panels.set(key,neighbors);
    }
  }
}
// The resting stamp shoe is just above the crushing plate and its active
// body fits between the trough walls. The upper sliding yoke is excluded.
const hammer=read(asset("hammer"));
const lowerPanels=hammer.elements.filter(e=>e.from[1]<7.93);
const rearWall=design.boxes.find(b=>b.name==="trough_back");
const frontWall=design.boxes.find(b=>b.name==="trough_front");
const floor=design.boxes.find(b=>b.name==="trough_floor");
assert.ok(lowerPanels.length>0&&rearWall&&frontWall&&floor);
const lowZ=Math.min(...lowerPanels.map(e=>e.from[2]))-18;
const highZ=Math.max(...lowerPanels.map(e=>e.to[2]))-18;
assert.ok(lowZ>=rearWall.to[2]-1e-4&&highZ<=frontWall.from[2]+1e-4,
    "Stamp shoe collides with the fixed walls: "+lowZ+".."+highZ);
const hammerBottom=Math.min(...hammer.elements.map(e=>e.from[1]))+24;
assert.ok(hammerBottom>=floor.to[1]&&hammerBottom-floor.to[1]<=2,
    "Moving shoe must start immediately above the supported crushing floor");

// At rest, a moving stamp/cam/input-housing surface must not be exactly
// coplanar with a visible V2 static face. This previously caused flickering
// where the upper sliding yoke touched a crank-support mounting bracket.
// Animated lift introduces legitimate brief crossings, not parked overlaps.
const canonical=read("tools/multiblock-models/sources/bronze_stamp_mill.json");
function surfaces(model,offset){
  return model.elements.flatMap(el=>Object.keys(el.faces).map(face=>{
    const [axis,sign,u,v]=faceAxes[face];
    const from=el.from.map((x,i)=>x+offset[i]);
    const to=el.to.map((x,i)=>x+offset[i]);
    return{axis,plane:sign>0?to[axis]:from[axis],u,v,from,to};
  }));
}
const occupiedPlanes=new Map();
for(const face of surfaces(canonical,[0,0,0])){
  const k=face.axis+":"+face.plane.toFixed(5);
  const existing=occupiedPlanes.get(k)||[];
  existing.push(face);occupiedPlanes.set(k,existing);
}
const parkedHammers=read(asset("hammer")),driveCams=read(asset("cam"));
const inputFrame=read(asset("input_frame"));
const movingAtRest=[...[-8,8,24].flatMap(station=>[
  ...surfaces(parkedHammers,[station-8,24,-18]),
  // Cranks occupy this position at zero phase; test conservative clearance.
  ...surfaces(driveCams,[station-8,30.5,-18])]),
  ...surfaces(inputFrame,[0,0,-32])];
for(const face of movingAtRest){
  const k=face.axis+":"+face.plane.toFixed(5);
  for(const fixed of occupiedPlanes.get(k)||[]){
    const w=Math.min(face.to[face.u],fixed.to[face.u])
             -Math.max(face.from[face.u],fixed.from[face.u]);
    const h=Math.min(face.to[face.v],fixed.to[face.v])
             -Math.max(face.from[face.v],fixed.from[face.v]);
    assert.ok(w<=1e-5||h<=1e-5,
      "Parked animated model has coincident faces with V2 static parts at "+k);
  }
}

const book=read(root+"/assets/magneticraft2/patchouli_books/world_of_magneticraft/en_us/entries/bronzeage.json");
const guide=book.pages.find(page=>page.type==="patchouli:multiblock"&&page.name==="Bronze Stamp Mill");
assert.ok(guide,"Stamp Mill Patchouli visualize page missing");

// Patchouli's arrays are [y from top to bottom][x west to east][z north
// to south]. Our multiblock JSON is [y from bottom][z rows][x entries].
// Never just copy the blueprint rows into Patchouli: on 5x3 machines that
// transposes the shape into 3x5 and can reverse the item ports.
const south=read(root+"/data/magneticraft2/multiblocks/bronze_stamp_mill_south.json");
const [bookWidth,bookHeight,bookDepth]=south.structure.dimensions;
assert.deepEqual([bookWidth,bookHeight,bookDepth],[5,3,4]);
assert.equal(guide.multiblock.pattern.length,bookHeight);
for(let y=0;y<bookHeight;y++){
  const layer=guide.multiblock.pattern[bookHeight-1-y];
  assert.equal(layer.length,bookWidth,"Patchouli expects one string per X column");
  for(let x=0;x<bookWidth;x++){
    assert.equal(layer[x].length,bookDepth,"Patchouli strings encode Z positions");
    for(let z=0;z<bookDepth;z++){
      const char=layer[x][z];
      const actual=char==="_"?"minecraft:air":guide.multiblock.mapping[char];
      const expected=south.structure.blocks[south.structure.layout["layer"+(y+1)][z][x]];
      assert.equal(actual,expected,
        "Guidebook / JSON mismatch at x="+x+" y="+y+" z="+z);
    }
  }
}

assert.ok(book.pages.some(page=>page.recipe==="magneticraft2:bronze_stamp_mill"));
const javaDir="src/main/java/com/magneticraft2/";
const be=fs.readFileSync(javaDir+"common/blockentity/stage/bronze/BronzeStampMillBlockEntity.java","utf8");
assert.ok(be.includes('MultiblockRenderParts.assign(')&&be.includes('MultiblockRecipeHandler.findRecipe('));
assert.ok(be.includes("getRenderAirAnchors()")&&
          be.includes("matchedFacing,renderAirAnchors,voxelProfileName"),
    "AIR stamp proxies must be formed using the blueprint-specified anchors");
assert.ok(be.includes("MultiblockVoxelNbt.write(")&&
          be.includes("MultiblockVoxelNbt.read("),
    "Voxel V2 snapshot persistence must be symmetrical");
assert.ok(be.includes("stampShoe.completeBatch()")&&be.includes("setMechanicalLoad("));
const client=fs.readFileSync(javaDir+"client/Clientsetup.java","utf8");
for(const id of ["hammer","hammer_worn","input_frame","cam"])
  assert.ok(client.includes('"multiblock/bronze_stamp_mill_'+id+'"'),"Unregistered animated model "+id);
assert.ok(client.includes("BRONZE_STAMP_MILL_BE"));
const renderer=fs.readFileSync(javaDir+"client/render/blocks/stage/bronze/BronzeStampMillRenderer.java","utf8");
assert.ok(renderer.includes("int[] stations={-8,8,24}")&&
          renderer.includes("24D/16D+mill.getHammerLift(partial,i)"),
  "Stamp heads must track the 3 service channels above the hoppers");
const jei=fs.readFileSync(javaDir+"compatibility/JEI/MagneticraftJeiPlugin.java","utf8");
assert.ok(jei.includes("MultiblockJeiRecipe.BRONZE_STAMP_MILL"));
console.log("Bronze Stamp Mill contract PASS: four orientations, anchored static frame, culled moving models, clear port volumes, ore recipes and book");
