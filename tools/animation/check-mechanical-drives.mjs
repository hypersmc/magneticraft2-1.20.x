#!/usr/bin/env node
/**
 * Numeric model/pose and resource checks for the four newly opted-in
 * mechanical machines (the Lathe and Sifter have their own fixtures).
 *
 * These compare current JSON to the ORIGINAL Java transform constants; they
 * do not replace a Forge build or visual testing of connected drive shafts.
 */
import assert from "node:assert/strict";
import { readFileSync, existsSync } from "node:fs";

const asset = name => JSON.parse(readFileSync(
    new URL(`../../src/main/resources/assets/magneticraft2/animations/${name}.json`,
        import.meta.url), "utf8"));
const grinders = asset("primitive_grinder");
const washer = asset("mechanical_ore_washer");
const classifier = asset("rotary_ore_classifier");
const mill = asset("bronze_stamp_mill");
const near = (a,b) => assert.ok(Math.abs(a-b) < 1e-8, `Expected ${b}, got ${a}`);
const vec = (a,b) => {
    assert.equal(a.length,3);
    a.forEach((v,i)=>near(v,b[i]));
};
const point=[0.16,0.88,0.35];
const facings=[0,90,180,-90];
const angles=[-540,-180,-45,0,27,90,180,360,720];

function turn(p,axis,deg) {
    const rad=deg*Math.PI/180,cos=Math.cos(rad),sin=Math.sin(rad);
    const [x,y,z]=p;
    switch(axis) {
        case "x": return [x,y*cos-z*sin,y*sin+z*cos];
        case "y": return [x*cos+z*sin,y,-x*sin+z*cos];
        case "z": return [x*cos-y*sin,x*sin+y*cos,z];
        default: throw Error("Unknown axis "+axis);
    }
}
function world(p,yaw) {
    const adjusted=p.map((v,i)=>v-((i===0 || i===2) ? 0.5 : 0));
    const rotated=turn(adjusted,"y",yaw);
    return rotated.map((v,i)=>v+((i===0 || i===2) ? 0.5 : 0));
}
const poses=Object.create(null);
let modelChecks=0,poseChecks=0,checkedParts=0;
function verifyPart(scene,name,expected) {
    const component=scene.components[name];
    assert.ok(component,`Missing animated part ${name}`);
    assert.equal(component.model,expected.model,`${name} model`);
    vec(component.model_offset,expected.offset);
    vec(component.pivot,expected.pivot);
    const path = new URL(
        `../../src/main/resources/assets/magneticraft2/models/${component.model.split(":")[1]}.json`,
        import.meta.url);
    assert.ok(existsSync(path),`Missing model: ${component.model}`);
    modelChecks++;
    for(const v of Object.values(component.model_variants?.models||{})) {
        const path = new URL(
            `../../src/main/resources/assets/magneticraft2/models/${v.split(":")[1]}.json`,
            import.meta.url);
        assert.ok(existsSync(path),`Missing worn model: ${v}`);
        modelChecks++;
    }
    const tracks=Object.values(scene.animations).flatMap(a=>a.tracks)
        .filter(t=>t.target===name);
    assert.equal(tracks.length,expected.axis?1:0,`${name}: incorrect track count`);
    const track=tracks[0];
    if(track) {
        assert.equal(track.axis,expected.axis);
        assert.equal(track.scale,expected.scale);
        assert.equal(scene.inputs[track.input].module,expected.module);
        assert.equal(scene.inputs[track.input].source,"mechanical.visual_angle_degrees");
    }
    // Explicit legacy placement: T(origin) R(axis,theta) T(-pivot).
    // JSON placement: T(offset) T(pivot) R(axis,theta) T(-pivot).
    const origin=expected.origin ?? expected.offset.map((v,i)=>v+expected.pivot[i]);
    for (const angle of angles) {
        for (const yaw of facings) {
            const theta=angle*(track?.scale??0);
            const local=point.map((v,i)=>v-component.pivot[i]);
            const previous=point.map((v,i)=>v-expected.pivot[i]);
            const jsonP=turn(local,track?.axis??"x",theta)
                .map((v,i)=>v+component.pivot[i]+component.model_offset[i]);
            const oldP=turn(previous,expected.axis??"x",theta)
                .map((v,i)=>v+origin[i]);
            vec(world(jsonP,yaw),world(oldP,yaw));
            poseChecks++;
        }
    }
    checkedParts++;
}
function model(prefix,suffix) {return `magneticraft2:multiblock/${prefix}${suffix}`;}
function standard(modelName,origin,pivot,axis,scale,module) {
    return {model:modelName,origin,pivot,offset:origin.map((v,i)=>v-pivot[i]),
        axis,scale,module};
}

verifyPart(grinders,"input_axle",standard(
    model("primitive_grinder_","axle"),[0,.5,.5],[0,.5,.5],"x",1,"mechanical_input"));
verifyPart(grinders,"grinding_rotor",standard(
    model("primitive_grinder_","rotor"),[.5,0,.5],[.5,0,.5],"y",-1,"mechanical_input"));
verifyPart(washer,"drum",standard(
    model("mechanical_ore_washer_","rotor"),[.5,.90625,.5],[.5,.90625,.5],"z",1,"trommel"));

for(const [i,position] of ["rear","middle","front"].entries()){
    const name="drum_"+position;
    verifyPart(classifier,name,standard(
        model("rotary_ore_classifier_drum_",position),[.5,1.5,i-1.5],
        [.5,.5,.5],"z",1,"drum"));
    const variants=classifier.components[name].model_variants;
    assert.equal(variants?.source,"classifier.wear_stage");
    for(let stage=2;stage<=5;stage++){
        assert.equal(variants.models[stage],
            model("rotary_ore_classifier_drum_",position+"_worn"));
    }
}

const ix=8/16,iy=8/16,lz=-10/16,uy=38.5/16,px=47.2/16;
const stampRefs=[
    ["gearbox_frame","drive_gearbox_frame",ix,iy,lz],
    ["cross_bearing","drive_cross_bearing",25/16,iy,lz],
    ["side_lower_mount","drive_side_lower_mount",px,iy,lz],
    ["input_coupler","drive_input_coupler",ix,iy,-1,"z","mechanical_input"],
    ["input_shaft","drive_input_shaft",ix,iy,-18/16,"z","mechanical_input"],
    ["input_spindle","drive_input_spindle",ix,iy,lz,"z","mechanical_input"],
    ["input_bevel","drive_bevel",ix,iy,lz,"z","mechanical_input"],
    ["output_bevel","drive_bevel_x",ix,iy,lz,"x","output_shaft"],
    ["lower_spindle","drive_lower_spindle",1,iy,lz,"x","output_shaft"],
    ["lower_drive_shaft","drive_shaft",2,iy,lz,"x","output_shaft"],
    ["lower_shaft_tip","drive_shaft_tip",40/16,iy,lz,"x","output_shaft"],
    ["lower_pulley","drive_pulley",px,iy,lz,"x","output_shaft"],
    ["upper_pulley","drive_pulley",px,uy,lz,"x","output_shaft"],
    ["upper_shaft_start","drive_shaft_start",-1,uy,lz,"x","output_shaft"],
    ["upper_shaft_zero","drive_shaft",0,uy,lz,"x","output_shaft"],
    ["upper_shaft_middle","drive_shaft",1,uy,lz,"x","output_shaft"],
    ["upper_shaft_end","drive_shaft",2,uy,lz,"x","output_shaft"],
    ["upper_shaft_tip","drive_shaft_tip",2.5,uy,lz,"x","output_shaft"]
];
for(const [id,file,x,y,z,axis,module] of stampRefs) {
    verifyPart(mill,id,standard(
        model("bronze_stamp_mill_",file),[x,y,z],[.5,.5,.5],
        axis,axis?1:undefined,module));
}
for (const scene of [grinders,washer,classifier,mill]) {
    assert.equal(scene.format,"magneticraft2:visual_animation");
    assert.equal(scene.format_version,1);
    assert.deepEqual(scene.coordinate_system,{
        units:"blocks",origin:"controller",reference_facing:"south"});
}
assert.equal(checkedParts,24); // 2 grinder +1 washer +3 classifier +18 mill
console.log(`Mechanical drives: ${checkedParts} parts, ${modelChecks} model files, `+
    `${poseChecks} facing/phase poses match legacy Java constants.`);
