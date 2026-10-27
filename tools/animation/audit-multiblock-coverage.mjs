#!/usr/bin/env node
/**
 * Magneticraft2 animation/formation compatibility inventory.
 *
 * Run on the Bronze-derived animation branch (including after the Sifter
 * child PR is merged): node tools/animation/audit-multiblock-coverage.mjs
 *
 * Validates every registered multiblock variant, palette, layer and selected
 * static model path (legacy single-model or Rendering V2 split-cell parts).
 * Also checks default JSON animation component references and identifies
 * moving multiblocks that have NOT yet been migrated to the JSON renderer.
 *
 * A missing animation definition is NOT an error for legacy renderers:
 * migrating them is a separate, deliberate task. Use
 * --require-mechanical-json to require at least one JSON visual entry point
 * for each mechanical family; it does not mean every component is migrated.
 */
import assert from "node:assert/strict";
import { existsSync, readFileSync, readdirSync, statSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";

const root = fileURLToPath(new URL("../../", import.meta.url));
const data = join(root, "src/main/resources/data/magneticraft2/multiblocks");
const assets = join(root, "src/main/resources/assets/magneticraft2");
const machines = [
    { name: "blueprintmaker",              orientations: "all", mode: "static" },
    { name: "bronze_foundry",              orientations: "all", mode: "specialized" },
    { name: "bronze_mechanical_lathe",     orientations: "all", mode: "mechanical",
      renderer: "bronze/BronzeMechanicalLatheRenderer.java" },
    { name: "bronze_stamp_mill",           orientations: "all", mode: "mechanical",
      renderer: "bronze/BronzeStampMillRenderer.java" },
    { name: "charcoal_retort",             orientations: "all", mode: "static" },
    { name: "mechanical_ore_washer",       orientations: "all", mode: "mechanical",
      renderer: "copper/MechanicalOreWasherBlockEntityRenderer.java" },
    { name: "mechanical_sifter",           orientations: "all", mode: "mechanical",
      renderer: "copper/MechanicalSifterBlockEntityRenderer.java" },
    { name: "primitive_furnace",           orientations: "all", mode: "specialized" },
    { name: "primitive_grinder",           orientations: ["any"], mode: "mechanical",
      renderer: "stone/PrimitiveGrinderBlockEntityRenderer.java" },
    { name: "primitive_storagecellar",     orientations: ["west"], mode: "static" },
    { name: "rotary_ore_classifier",       orientations: "all", mode: "mechanical",
      renderer: "bronze/RotaryOreClassifierRenderer.java" },
    { name: "water_settling_tank",         orientations: "all", mode: "specialized" },
];
// Stable visual component IDs are the machine/pack interface. Validate the
// expected defaults so a refactor cannot silently leave only a partial scene.
const expectedComponents = {
    bronze_mechanical_lathe: ["input_rotor","input_output_rotor"],
    mechanical_sifter: ["sieve_crank","sieve_rod","sieve_follower","sieve_carriage",
        "upper_tray","slide_shoes"],
    primitive_grinder: ["input_axle","grinding_rotor"],
    mechanical_ore_washer: ["drum"],
    bronze_stamp_mill: ["input_coupler","input_shaft","input_bevel",
        "output_bevel","lower_pulley","upper_pulley","upper_shaft_tip",
        "plate_left","plate_middle","plate_right",
        "cam_left","cam_middle","cam_right",
        "hammer_left","hammer_middle","hammer_right"],
    rotary_ore_classifier: ["drum_rear","drum_middle","drum_front"]
};
const horizontal = ["north", "east", "south", "west"];
const readJson = p => JSON.parse(readFileSync(p, "utf8"));
const fail = message => {throw new Error(message)};
const modelFile = id => {
    const [namespace, ...rest] = id.split(":");
    assert.equal(rest.length, 1, `Bad model ID ${id}`);
    return join(root, "src/main/resources/assets", namespace, "models", rest[0] + ".json");
};
const resourceId = value => value.includes(":") ? value : "magneticraft2:" + value;
const allFiles = readdirSync(data).filter(x => x.endsWith(".json")).sort();
const claimed = new Set();
let checkedVariants = 0, checkedModels = 0, checkedAnimatedComponents = 0;
let mechanicalCount = 0, mechanicalJsonCount = 0;
const report = [];

for (const machine of machines) {
    const orientations = machine.orientations === "all"
        ? horizontal : machine.orientations;
    const rendererPath = machine.renderer == null ? null
        : join(root,"src/main/java/com/magneticraft2/client/render/blocks/stage", machine.renderer);
    if (rendererPath != null && !existsSync(rendererPath)) {
        fail(`${machine.name}: configured renderer missing: ${rendererPath}`);
    }

    const anim = join(assets, "animations", machine.name + ".json");
    const hasAnimation = existsSync(anim);
    if (machine.mode === "mechanical") {
        mechanicalCount++;
        if (hasAnimation) mechanicalJsonCount++;
    }
    const visualManifestPath = join(assets, "machine_visuals", machine.name + ".json");
    if (hasAnimation) {
        assert.ok(existsSync(visualManifestPath),
            `${machine.name}: missing resource-pack machine visual manifest`);
    }
    if (hasAnimation && rendererPath != null &&
        !readFileSync(rendererPath, "utf8").includes("MachineVisualRenderer")) {
        fail(`${machine.name}: animation JSON exists but its renderer is not opted in`);
    }

    const modelRefs = new Set();
    for (const facing of orientations) {
        const file = `${machine.name}_${facing}.json`;
        const path = join(data,file);
        assert.ok(existsSync(path), `Missing multiblock variant ${file}`);
        claimed.add(file);
        const structure = readJson(path);
        const name = structure.name;
        assert.ok(name === file.slice(0,-5) ||
            (machine.name === "primitive_grinder" && name === "primitive_grinder"),
            `${file}: unexpected internal multiblock name ${name}`);

        const dims = structure.structure?.dimensions;
        assert.ok(Array.isArray(dims) && dims.length === 3
            && dims.every(n => Number.isInteger(n) && n >= 1 && n <= 128),
            `${file}: invalid 3D dimensions`);
        const palette = structure.structure.blocks;
        const layout = structure.structure.layout;
        assert.ok(palette && typeof palette === "object", `${file}: missing palette`);
        for (let y=0; y<dims[1]; ++y) {
            const key = "layer" + (y+1);
            const rows = layout?.[key];
            assert.ok(Array.isArray(rows) && rows.length === dims[2],
                `${file}: ${key} must contain ${dims[2]} rows`);
            for (let z=0; z<dims[2]; ++z) {
                assert.ok(Array.isArray(rows[z]) && rows[z].length === dims[0],
                    `${file}: ${key} row ${z} must contain ${dims[0]} cells`);
                for (const role of rows[z]) {
                    assert.ok(Object.hasOwn(palette,role),
                        `${file}: cell references unknown palette role ${role}`);
                }
            }
        }

        const selected = structure.settings?.replacewhenformed;
        assert.equal(typeof selected, "string", `${file}: missing formed model selection`);
        const modelId = resourceId(selected);
        const [namespace, modelPath] = modelId.split(":");
        assert.equal(namespace, "magneticraft2", `${file}: unexpected model namespace`);
        // V2 machines intentionally do NOT contain a single giant model JSON.
        // Their static elements are split into cell-local models by the V2
        // generator. Validate those without declaring a false missing model.
        const direct = modelFile(modelId);
        const v2Folder = join(root, "src/main/resources/assets", namespace, "models", modelPath);
        const v2Pieces = existsSync(v2Folder) && statSync(v2Folder).isDirectory()
            ? readdirSync(v2Folder).filter(x=>/^part_-?\d+_-?\d+_-?\d+\.json$/.test(x))
            : [];
        assert.ok(existsSync(direct) || v2Pieces.length > 0,
            `${file}: neither legacy model ${modelId} nor V2 split-cell models were found`);
        modelRefs.add(existsSync(direct) ? "legacy" : "V2");
        checkedVariants++;
        checkedModels++;
    }

    let animatedParts = 0;
    if (hasAnimation) {
        const manifest = readJson(visualManifestPath);
        assert.equal(manifest.format, "magneticraft2:machine_visual",
            `${machine.name}: unexpected visual manifest format`);
        assert.equal(manifest.format_version, 1);
        assert.equal(manifest.animation, "magneticraft2:" + machine.name,
            `${machine.name}: default machine visual points to a different scene`);
        assert.ok(manifest.angle_channels && manifest.integer_channels,
            `${machine.name}: missing typed input channel mappings`);
        const animation = readJson(anim);
        for (const input of Object.values(animation.inputs)) {
            assert.ok(Object.hasOwn(manifest.angle_channels,input.module),
                `${machine.name}: unknown angle alias ${input.module}`);
        }
        assert.equal(animation.format, "magneticraft2:visual_animation",
            `${machine.name}: wrong animation schema`);
        assert.equal(animation.format_version, 1,
            `${machine.name}: wrong animation schema version`);
        assert.deepEqual(animation.coordinate_system, {
            units: "blocks", origin: "controller", reference_facing: "south"
        }, `${machine.name}: incompatible coordinate system`);
        const parts = animation.components;
        assert.ok(parts && typeof parts==="object" && Object.keys(parts).length>0,
            `${machine.name}: empty animated scene`);
        for (const [id, belt] of Object.entries(animation.belt_loops??{})) {
            assert.ok(animation.inputs[belt.input],
                `${machine.name}: belt ${id} references unknown input`);
            assert.equal(belt.type,"open_belt");
            assert.ok(belt.texture.startsWith("magneticraft2:textures/")
                || belt.texture.startsWith("minecraft:textures/"),
                `${machine.name}: belt ${id} has unexpected texture ID`);
            if (belt.texture.startsWith("magneticraft2:")) {
                const texturePath = join(root,
                    "src/main/resources/assets/magneticraft2",
                    belt.texture.split(":")[1]);
                assert.ok(existsSync(texturePath),
                    `${machine.name}: belt ${id} missing texture ${texturePath}`);
            }
        }
        for (const required of expectedComponents[machine.name] ?? []) {
            assert.ok(Object.hasOwn(parts,required),
                `${machine.name}: missing required visual component ${required}`);
        }
        for (const [id, part] of Object.entries(parts)) {
            assert.ok(/^[a-z][a-z0-9_]*$/.test(id),
                `${machine.name}: invalid component ID ${id}`);
            if (part.parent) {
                assert.ok(Object.hasOwn(parts,part.parent),
                    `${machine.name}: ${id} has unknown parent ${part.parent}`);
            }
            for (const field of ["pivot","model_offset"]) {
                assert.ok(Array.isArray(part[field]) && part[field].length === 3 &&
                    part[field].every(n => Number.isFinite(n) && Math.abs(n)<=256),
                    `${machine.name}: ${id} invalid ${field}`);
            }
            if (part.model_variants) {
                assert.ok(Object.hasOwn(manifest.integer_channels,
                    part.model_variants.source),
                    `${machine.name}: ${id} unknown integer alias ${part.model_variants.source}`);
            }
            const refs=[part.model,...Object.values(part.model_variants?.models??{})].filter(Boolean);
            for (const model of refs) {
                if (model.startsWith("magneticraft2:")) {
                    assert.ok(existsSync(modelFile(model)),
                        `${machine.name}: ${id} missing animated model ${model}`);
                }
                checkedAnimatedComponents++;
            }
            animatedParts++;
        }
    }
    report.push({ machine: machine.name, mode: machine.mode,
        variants: orientations.length, staticModels: [...modelRefs].sort().join("+"),
        animation: hasAnimation ? `JSON (${animatedParts} parts)` : "legacy/none" });
}

const unclaimed = allFiles.filter(p=>!claimed.has(p));
assert.deepEqual(unclaimed, [], `Undocumented multiblock structures: ${unclaimed.join(", ")}`);
console.log("\nMagneticraft2 multiblock compatibility audit");
console.table(report);
console.log(`Validated ${machines.length} families, ${checkedVariants} formation JSON variants, `+
    `${checkedModels} static model selections and ${checkedAnimatedComponents} animated model references.`);
console.log(`JSON mechanical entry-point coverage: ${mechanicalJsonCount}/${mechanicalCount} families.`);
if (mechanicalJsonCount < mechanicalCount) {
    console.log("Unmigrated mechanical families retain their existing Java renderers.");
}
if (process.argv.includes("--require-mechanical-json")) {
    assert.equal(mechanicalJsonCount,mechanicalCount,
        "Mechanical JSON conversion is not yet complete");
}
