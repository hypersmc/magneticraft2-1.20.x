#!/usr/bin/env node
/**
 * Dependency-free contracts for resource-pack machine visual manifests.
 *
 * This tests JSON structure and the declared Java input contracts of all
 * six mechanical multiblocks. It does NOT execute Minecraft/Forge Java;
 * full build, resource reload and gameplay testing are still required.
 */
import assert from "node:assert/strict";
import {readFileSync, existsSync} from "node:fs";
import {fileURLToPath} from "node:url";
import {join} from "node:path";

const root=fileURLToPath(new URL("../../",import.meta.url));
const assets=join(root,"src/main/resources/assets/magneticraft2");
const parse=file=>JSON.parse(readFileSync(file,"utf8"));
const contracts=[
 {id:"bronze_mechanical_lathe",angles:["mechanical_input","spindle"],integers:[]},
 {id:"mechanical_sifter",angles:["mechanical_input"],integers:["sifter.wear_stage"]},
 {id:"primitive_grinder",angles:["mechanical_input"],integers:[]},
 {id:"mechanical_ore_washer",angles:["trommel"],integers:[]},
 {id:"bronze_stamp_mill",angles:["mechanical_input","output_shaft"],integers:[]},
 {id:"rotary_ore_classifier",angles:["drum"],integers:["classifier.wear_stage"]}
];
const machinePath=id=>join(assets,"machine_visuals",id+".json");
const animationPath=id=>{
 const [namespace,name]=id.split(":");
 return join(root,"src/main/resources/assets",namespace,"animations",name+".json");
};
function verifyManifest(manifest,scene,real) {
 assert.equal(manifest.format,"magneticraft2:machine_visual");
 assert.equal(manifest.format_version,1);
 assert.match(manifest.animation,/^[a-z0-9_.-]+:[a-z0-9_./-]+$/);
 assert.equal(scene.format,"magneticraft2:visual_animation");
 assert.equal(scene.format_version,1);
 assert.deepEqual(scene.coordinate_system,{
   units:"blocks",origin:"controller",reference_facing:"south"
 });
 for(const [alias,source] of Object.entries(manifest.angle_channels)) {
   assert.match(alias,/^[a-z][a-z0-9_]*$/);
   assert.ok(real.angles.includes(source),
     `Unexpected machine angle source ${source} for ${real.id}`);
 }
 for(const [alias,source] of Object.entries(manifest.integer_channels)) {
   assert.match(alias,/^[a-z][a-z0-9_.]*$/);
   assert.ok(real.integers.includes(source),
     `Unexpected machine integer source ${source} for ${real.id}`);
 }
 for(const input of Object.values(scene.inputs)) {
   assert.equal(input.source,"mechanical.visual_angle_degrees");
   assert.ok(Object.hasOwn(manifest.angle_channels,input.module),
     `Scene asks for unbound angle alias ${input.module}`);
 }
 for(const [name,part] of Object.entries(scene.components)) {
   if(part.parent)assert.ok(scene.components[part.parent],
     `Component ${name} has unknown parent`);
   if(part.model_variants) {
     assert.ok(Object.hasOwn(manifest.integer_channels,part.model_variants.source),
       `Scene asks for unbound wear alias ${part.model_variants.source}`);
   }
 }
 return Object.keys(scene.components).length;
}

let components=0;
for(const real of contracts) {
 const manifest=parse(machinePath(real.id));
 assert.equal(manifest.animation,`magneticraft2:${real.id}`);
 const scene=parse(animationPath(manifest.animation));
 components+=verifyManifest(manifest,scene,real);
}

// A pack selects a DIFFERENT animation ID and aliases "rebranded_shaft" to
// a real mechanical input. The scene is not under the default machine name.
const pack=join(root,"docs/examples/json-machine-visual-binding-pack/assets/magneticraft2");
const override=parse(join(pack,"machine_visuals/primitive_grinder.json"));
const custom=parse(join(pack,"animations/demo/primitive_grinder_restyle.json"));
assert.notEqual(override.animation,"magneticraft2:primitive_grinder");
const numCustom=verifyManifest(override,custom,contracts[2]);
assert.equal(override.angle_channels.rebranded_shaft,"mechanical_input");
assert.equal(custom.inputs.custom_drive.module,"rebranded_shaft");
assert.equal(custom.animations.custom_powertrain.tracks.find(
 t=>t.target==="grinding_rotor").scale,0);
assert.ok(existsSync(join(pack,"models/animated/restyled_grinder_rotor.json")));

// The following invalid input would have NO counterpart in the Java frame.
// An untrusted JSON definition is not allowed to invent "steam_pressure".
const invented=structuredClone(override);
invented.angle_channels.rebranded_shaft="steam_pressure";
assert.throws(()=>verifyManifest(invented,custom,contracts[2]),
 /Unexpected machine angle source/);

// A second alias can share a real Java source; source remapping doesn't
// create additional mechanical shafts, signals, or game-state mutations.
const alias=structuredClone(override);
alias.angle_channels.another_drive="mechanical_input";
verifyManifest(alias,custom,contracts[2]);

console.log(`Machine visual bindings: ${contracts.length} built-in manifests, `+
 `${components} default components, ${numCustom} custom Grinder components, `+
 "scene redirect/alias/unknown-input checks passed.");
