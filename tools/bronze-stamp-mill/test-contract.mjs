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

// The upper crankshaft is animated and must NOT also be baked into V2.
assert.ok(!design.boxes.some(b=>b.name==="mount_upper_drive_shaft"),
  "Old fixed crank axle would z-fight with the spinning drive shaft");

// The outside belt line is kept clear of the tray, right-hand uprights and
// all four full-block module cells. The belt rides a tangent 4.5 pixels from
// each pulley center, with one uninterrupted lower/upper flight.
const driveBeltEnvelope={from:[46.55,8,-14.52],to:[47.85,38.5,-5.48]};
const lowerShaftEnvelope={from:[8,7.05,-10.95],to:[47.2,8.95,-9.05]};
const lowerPulleyEnvelope={from:[46.3875,3.95,-14.05],to:[47.9475,12.05,-5.95]};
const upperPulleyEnvelope={from:[46.3875,34.45,-14.05],to:[47.9475,42.55,-5.95]};
const intersects=(a,b)=>a.from.every((v,i)=>
  Math.min(a.to[i],b.to[i])-Math.max(v,b.from[i])>1e-5);
for(const part of design.boxes){
  assert.ok(!intersects(driveBeltEnvelope,part),
    "Vertical drive belt cuts through static machinery: "+part.name);
  assert.ok(!intersects(lowerShaftEnvelope,part),
    "Lower crossshaft cuts through the workshop: "+part.name);
  for(const [name,envelope]of [["lower pulley",lowerPulleyEnvelope],
                                 ["upper pulley",upperPulleyEnvelope]]){
    assert.ok(!intersects(envelope,part),
      name+" intersects structural timber or an adjacent module: "+part.name);
  }
}

const powerModels=["drive_shaft","drive_shaft_start","drive_shaft_tip",
  "drive_input_shaft","drive_input_spindle","drive_input_coupler",
  "drive_lower_spindle",
  "drive_bevel","drive_bevel_x","drive_pulley","drive_belt_straight",
  "drive_belt_upper","drive_belt_wrap",
  "drive_gearbox_frame","drive_cross_bearing","drive_side_lower_mount"];
for(const id of powerModels){
  const model=read(asset(id));
  assert.ok(model.elements?.length>0,"Missing powertrain model "+id);
  for(const element of model.elements){
    assert.ok(element.from.every((v,i)=>v>=-1e-5&&v<element.to[i]&&element.to[i]<=16+1e-5),
      id+": invalid moving model cuboid bounds");
    for(const [face,data]of Object.entries(element.faces)){
      const axes={north:[0,1],south:[0,1],east:[2,1],west:[2,1],up:[0,2],down:[0,2]}[face];
      assert.ok(axes&&model.textures[data.texture.slice(1)],
        id+": unresolved texture "+data.texture);
      assert.ok(data.uv?.length===4&&data.uv.every(n=>n>=-1e-5&&n<=16+1e-5),
        id+": invalid native UV tile");
      assert.ok(Math.abs(data.uv[2]-data.uv[0]-
          (element.to[axes[0]]-element.from[axes[0]]))<1e-4&&
        Math.abs(data.uv[3]-data.uv[1]-
          (element.to[axes[1]]-element.from[axes[1]]))<1e-4,
        id+": stretched texture on "+face);
    }
  }
}

// Genuine bevel-tooth interaction is validated using rotating cuboids.
// Bounding the whole radial envelope INSIDE the other wheel's plane was
// the bug: that made meshing geometrically impossible. The separate
// full-angle mesh regression now enforces actual interleaving and no clipping.
const incomingSign=-1,outputSign=1,contactRadius=4.0;
assert.equal(incomingSign*contactRadius,-outputSign*contactRadius,
  "Perpendicular bevels must have matching tooth tangent velocities");

// Gearbox straps must connect to the actual underside tray girders and
// the right-side mount must meet the existing right crossbrace.
const rearGirder=design.boxes.find(b=>b.name==="mount_rear_undertray_girder");
const frontGirder=design.boxes.find(b=>b.name==="mount_front_undertray_girder");
const rightBrace=design.boxes.find(b=>b.name==="mount_right_foundation_crossbrace");
assert.ok(rearGirder&&frontGirder&&rightBrace);
const rearMount={from:[14.2,2.3,-15.9],to:[15.6,16,-14.6]};
const frontMount={from:[14.2,2.3,-5.8],to:[15.6,16,-4.5]};
const middleMount={from:[24.15,14.9,-16.3],to:[25.85,16,-3.7]};
const outerMount={from:[45,10.65,-11.6],to:[46.1,13.6,-8.4]};
assert.ok(touchesFace(rearMount,rearGirder)&&touchesFace(frontMount,frontGirder),
  "Lower input gearbox must hang from BOTH crushing tray girders");
assert.ok(touchesFace(middleMount,rearGirder)&&touchesFace(middleMount,frontGirder),
  "Intermediate bearing must hang from the load frame");
assert.ok(touchesFace(outerMount,rightBrace),
  "Right-side pulley bearing must attach to the foundation brace");

// Render ownership and real input-module boundary:
// MechanicalInputModuleBlockEntityRenderer already draws both the wooden
// rotor AND copper bearing when FORMED. The Stamp Mill must never draw its
// old duplicate full-cell housing over the real module.
const millRenderer=fs.readFileSync(
  "src/main/java/com/magneticraft2/client/render/blocks/stage/bronze/BronzeStampMillRenderer.java","utf8");
const moduleRenderer=fs.readFileSync(
  "src/main/java/com/magneticraft2/client/render/blocks/stage/copper/MechanicalInputModuleBlockEntityRenderer.java","utf8");
assert.ok(moduleRenderer.includes("if (!formed && !rotating)")&&
          moduleRenderer.includes("SHAFT_MODEL")&&moduleRenderer.includes("HOUSING_MODEL"),
  "Shared formed input module must continue to own and render its shaft and bearing");
assert.ok(!millRenderer.includes("getModel(MODULE)")&&
          !millRenderer.includes("new ResourceLocation(\"magneticraft2\",\"multiblock/bronze_stamp_mill_input_frame\")"),
  "Stamp Mill must not re-render the already visible functional module");

// The previously visible decorative input frame must not disappear when
// replacing its inner bearing with the reusable module's own renderer.
const originalFrame=read(asset("input_frame"));
const frameTrim=read(asset("input_frame_detail"));
assert.equal(originalFrame.elements.length,63,"The accepted input frame is the source of truth");
assert.equal(frameTrim.elements.length,51,"All outer frame detail and clipped faces must be preserved");
assert.equal(frameTrim.elements.filter(e=>Object.values(e.faces)[0].texture==="#bronze").length,16,
  "Lost copper corner uprights from the original input frame");
assert.equal(frameTrim.elements.filter(e=>Object.values(e.faces)[0].texture==="#dark").length,35,
  "Lost dark-metal crosspieces from the original input frame");
assert.equal(frameTrim.elements.filter(e=>originalFrame.elements.slice(0,43)
  .some(old=>JSON.stringify(old)===JSON.stringify(e))).length,39,
  "Undisturbed original input-frame faces should be preserved exactly");
assert.ok(millRenderer.includes("INPUT_FRAME_DETAIL")&&
          millRenderer.includes("pose.translate(0D,0D,-2D)"),
  "The formed Stamp Mill must render its outer input decoration at its original cell");
const housingModel=read(root+"/assets/magneticraft2/models/block/mechanical_input_module_housing.json");
const woodModel=read(root+"/assets/magneticraft2/models/block/shaft_wood.json");
// The shared module BER rotates canonical +Y housing/shaft models 90 degrees
// about +X to the Stamp Mill's local +Z input axis.
const orientToInputZ=e=>({
  from:[e.from[0],16-e.to[2],e.from[1]],
  to:[e.to[0],16-e.from[2],e.to[1]]
});
const moduleParts=[...housingModel.elements.map(orientToInputZ),
                   ...woodModel.elements.map(orientToInputZ)];
for(const panel of frameTrim.elements){
  for(const solid of moduleParts){
    assert.ok(!panel.from.every((v,i)=>
      Math.min(panel.to[i],solid.to[i])-Math.max(v,solid.from[i])>1e-5),
      "Decorative frame intersects the real input shaft or bearing");
  }
  const nearestX=Math.max(panel.from[0],Math.min(8,panel.to[0]));
  const nearestY=Math.max(panel.from[1],Math.min(8,panel.to[1]));
  assert.ok(Math.hypot(nearestX-8,nearestY-8)>Math.sqrt(18)+.01,
    "Frame decoration clips the 6x6 wooden rotor when it turns 45 degrees");
  const face=Object.keys(panel.faces)[0],data=panel.faces[face];
  const axes={north:[0,1],south:[0,1],east:[2,1],west:[2,1],up:[0,2],down:[0,2]}[face];
  assert.ok(axes&&frameTrim.textures[data.texture.slice(1)],
    "Decorative frame has unresolved texture");
  assert.ok(data.uv.every(v=>v>=0&&v<=16)&&
    Math.abs(data.uv[2]-data.uv[0]-(panel.to[axes[0]]-panel.from[axes[0]]))<1e-5&&
    Math.abs(data.uv[3]-data.uv[1]-(panel.to[axes[1]]-panel.from[axes[1]]))<1e-5,
    "Decorative frame UV tiles must not be stretched");
}

// The steel extension, its copper transition ring, and the gearbox begin
// entirely OUTSIDE the rear module's real z=-32..-16 cell.
const steelExtension=read(asset("drive_input_shaft"));
const steelStart=Math.min(...steelExtension.elements.map(e=>e.from[2]))-26;
assert.ok(steelStart>=-16-1e-5,
  "Stamp Mill must not duplicate the formed module's wooden rotor");
const coupling=read(asset("drive_input_coupler"));
const couplerStart=Math.min(...coupling.elements.map(e=>e.from[2]))-24;
assert.ok(couplerStart>-16,
  "Copper coupler may not render over the module's wood end cap");
// Extending steel must fit through the coupler's open square bore.
const steelBoxes=steelExtension.elements.map(e=>({
  from:e.from.map((v,i)=>v+[0,0,-26][i]),
  to:e.to.map((v,i)=>v+[0,0,-26][i])
}));
const couplingBoxes=coupling.elements.map(e=>({
  from:e.from.map((v,i)=>v+[0,0,-24][i]),
  to:e.to.map((v,i)=>v+[0,0,-24][i])
}));
for(const c of couplingBoxes)for(const b of steelBoxes){
  assert.ok(!c.from.every((v,i)=>Math.min(c.to[i],b.to[i])-Math.max(v,b.from[i])>1e-5),
    "Copper coupling clips the actual steel drive spindle");
}

// No duplicate caps on the seams of rotating contiguous shafts.
const internalCaps={
  drive_input_shaft:["north","south"],
  drive_input_spindle:["north"],
  drive_lower_spindle:["east"],
  drive_shaft:["west","east"],
  drive_shaft_start:["east"],
  drive_shaft_tip:["west"]
};
for(const [id,caps]of Object.entries(internalCaps)){
  const model=read(asset(id));
  for(const panel of model.elements)for(const cap of caps)
    assert.ok(!Object.hasOwn(panel.faces,cap),
      id+" must not draw a coplanar internal shaft-end face: "+cap);
}

// The main pitch rims are still compact, but the actual TOOTH FLANKS
// must cross the old 4.06px exclusion radius to engage the other gear.
// The imported source model and full-phase SAT test confirm this safely.
for(const id of ["drive_bevel","drive_bevel_x"]){
  const model=read(asset(id));
  assert.ok(model.elements.length>250,"Missing extended bevel tooth panels");
  assert.ok(model.elements.some(e=>Object.values(e.faces)
    .some(face=>face.texture==="#bronze")),
    id+": bronze tooth surfaces missing");
}
const driveBuilder=fs.readFileSync("tools/bronze-stamp-mill/build-drive-models.mjs","utf8");
assert.ok(driveBuilder.includes("octRing")&&driveBuilder.includes("removeInternalCaps"),
  "Powertrain generator must preserve rounded pitch rims and capless splices");

// Each animated model contains *only visible face panels*. This prevents
// z-fighting where full cuboids previously drew matching internal faces.
const faceAxes={north:[2,-1,0,1],south:[2,1,0,1],
                east:[0,1,2,1],west:[0,-1,2,1],
                up:[1,1,0,2],down:[1,-1,0,2]};
for(const id of ["hammer","hammer_worn","cam"]){
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
// The formed module housing is owned by MechanicalInputModuleBlockEntityRenderer;
// the Stamp Mill's former input_frame model is no longer a rendered surface.
const movingAtRest=[...[-8,8,24].flatMap(station=>[
  ...surfaces(parkedHammers,[station-8,24,-18]),
  // Cranks occupy this position at zero phase; test conservative clearance.
  ...surfaces(driveCams,[station-8,30.5,-18])])];
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
assert.ok(be.includes("getStampInputRotationDegrees(float partialTicks)")&&
          be.includes("facing == Direction.NORTH || facing == Direction.WEST")&&
          be.includes("return -getStampInputRotationDegrees(partialTicks)")&&
          be.includes("getStampCrankRotationDegrees(partialTicks)+120D*hammerIndex"),
  "Input module, gearbox output and hammer lift phases must agree in all facings");
const client=fs.readFileSync(javaDir+"client/Clientsetup.java","utf8");
for(const id of ["hammer","hammer_worn","input_frame","cam"])
  assert.ok(client.includes('"multiblock/bronze_stamp_mill_'+id+'"'),"Unregistered animated model "+id);
for(const id of powerModels)
  assert.ok(client.includes('"multiblock/bronze_stamp_mill_'+id+'"'),
    "Missing baked model registration for "+id);
assert.ok(client.includes("BRONZE_STAMP_MILL_BE"));
const renderer=fs.readFileSync(javaDir+"client/render/blocks/stage/bronze/BronzeStampMillRenderer.java","utf8");
assert.ok(renderer.includes("int[] stations={-8,8,24}")&&
          renderer.includes("24D/16D+mill.getHammerLift(partial,i)"),
  "Stamp heads must track the 3 service channels above the hoppers");
assert.ok(renderer.includes("INPUT_SHAFT")&&renderer.includes("BEVEL_X")&&
          renderer.includes("BELT_UPPER")&&renderer.includes("BELT_WRAP"),
  "The complete Gear V2 powertrain must be rendered");
assert.ok(renderer.includes("driveAngle+120F*i")&&
          renderer.includes("upperY,lowerZ,Axis.XP,driveAngle")&&
          !renderer.includes("Axis.XP,shaftAngle"),
  "All cams, the upper shaft, and both pulleys must share the bevel output phase");
assert.ok(renderer.includes("pulleyX,upperY,lowerZ,null,0F"),
  "The upper belt half-wrap must remain stationary around its rotating pulley");
assert.ok(renderer.includes("mill.getStampInputRotationDegrees(partial)")&&
          renderer.includes("mill.getStampCrankRotationDegrees(partial)")&&
          renderer.includes("BEVEL_Z,")&&renderer.includes("BEVEL_X,")&&
          renderer.includes("inputX,inputY,lowerZ,Axis.ZP,inputAngle")&&
          renderer.includes("inputX,inputY,lowerZ,Axis.XP,driveAngle"),
  "Both bevels need a shared pivot and opposite signed phases");
assert.ok(renderer.includes("LOWER_SPINDLE,")&&
          renderer.includes("16D/16D,inputY,lowerZ,Axis.XP,driveAngle")&&
          !renderer.includes("for(int x=16;x<=32;x+=16)"),
  "The X output shaft must terminate at the pinion and use the bevel output phase");
assert.ok(renderer.includes("GEARBOX_FRAME,")&&
          renderer.includes("CROSS_BEARING,")&&
          renderer.includes("SIDE_LOWER_MOUNT,"),
  "All drive bearings must render from mounted stationary models");

const jei=fs.readFileSync(javaDir+"compatibility/JEI/MagneticraftJeiPlugin.java","utf8");
assert.ok(jei.includes("MultiblockJeiRecipe.BRONZE_STAMP_MILL"));
// Fail if someone edited any bevel model by hand without updating its
// authored source. This also catches stale shaft cap geometry.
import {execFileSync} from "node:child_process";
execFileSync(process.execPath,["tools/bronze-stamp-mill/build-drive-models.mjs","--check"],{stdio:"pipe"});
execFileSync(process.execPath,["tools/bronze-stamp-mill/build-input-frame-detail.mjs","--check"],{stdio:"pipe"});
execFileSync(process.execPath,["tools/bronze-stamp-mill/test-bevel-mesh.mjs"],{stdio:"pipe"});
execFileSync(process.execPath,["tools/bronze-stamp-mill/build-strike-plate.mjs","--check"],{stdio:"pipe"});
execFileSync(process.execPath,["tools/bronze-stamp-mill/test-visual-details.mjs"],{stdio:"pipe"});
console.log("Bronze Stamp Mill contract PASS: four orientations, anchored static frame, culled moving models, clear port volumes, ore recipes and book");
