import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

/**
 * Steel IV-C Visual Contract:
 * - All placed/formed variants use the same geometry in Voxel V2 and models
 * - Distinct circular sections in Patchouli; actual air is required at the bore
 * - UV coverage follows cuboid dimensions (no stretching/compression)
 * - No overlapping cuboid volumes in any baked section
 * - No unsafe new Steam/pressure functionality
 * Run: node tools/steel-age/check-steel-boiler-vessel-visuals.mjs
 */
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const json = p => JSON.parse(fs.readFileSync(path.join(root, p), "utf8"));
const java = p => fs.readFileSync(path.join(root, p), "utf8");
const D = "src/main/resources/data/magneticraft2/";
const A = "src/main/resources/assets/magneticraft2/";
const J = "src/main/java/com/magneticraft2/common/";
const roles = ["bottom_left", "bottom", "bottom_right", "left", "right",
               "top_left", "top", "top_right"];
const directions = ["south", "west", "north", "east"];
const body = json(D + "multiblocks/steel_boiler_vessel_south.json").structure.layout;
const voxel = json(D + "multiblock_voxels/steel_boiler_vessel.json");
const worldBoxes = [];
const modelCache = new Map();

function model(name) {
    if (!modelCache.has(name))
        modelCache.set(name, json(A + "models/multiblock/boiler_vessel/" + name + ".json"));
    return modelCache.get(name);
}
function volumeOverlaps(a, b) {
    return [0, 1, 2].every(i => Math.min(a[3+i], b[3+i]) > Math.max(a[i], b[i]));
}
function boxes(name, role, facing) {
    const id = name + (role ? role + "_" : "") + facing;
    const m = model(id);
    assert(m.elements.length > 0, "Empty model: " + id);
    const result = m.elements.map(e => [...e.from, ...e.to]);
    for (const [i, e] of m.elements.entries()) {
        const b = result[i];
        assert(b.slice(0,3).every((v,d) => Number.isInteger(v) && v >= 0
                  && b[d+3] <= 16 && b[d+3] > v), "Invalid cuboid " + id);
        const dx = b[3]-b[0], dy = b[4]-b[1], dz = b[5]-b[2];
        const sizes = {north:[dx,dy],south:[dx,dy],east:[dz,dy],
                       west:[dz,dy],up:[dx,dz],down:[dx,dz]};
        for (const [face, faceData] of Object.entries(e.faces))
            assert.deepEqual(faceData.uv, [0,0,...sizes[face]],
                             "Texture UV distortion in " + id + " face " + face);
    }
    for (let i = 0; i < result.length; i++)
        for (let j = i + 1; j < result.length; j++)
            assert(!volumeOverlaps(result[i], result[j]),
                   "Overlapping cuboids in " + id + " elements " + i + " " + j);
    return result;
}
function relative(box, x, y, z) {
    return [box[0] + 16*(x-1), box[1] + 16*y, box[2] + 16*(z-4),
            box[3] + 16*(x-1), box[4] + 16*y, box[5] + 16*(z-4)];
}
function roleFor(x,y) {
    if (y === 1) return roles[x];
    if (y === 2 && x !== 1) return x===0 ? "left" : "right";
    if (y === 3) return roles[5+x];
    throw new Error("No shell role at " + x + "," + y);
}
const ledger = {};
for (let y=0; y<4; y++) for (let z=0; z<5; z++) for (let x=0; x<3; x++) {
    const c=body["layer"+(y+1)][z][x];
    ledger[c]=(ledger[c]||0)+1;
    if (c==="A") continue;
    if (c==="F" || c==="C") {
        worldBoxes.push(relative([0,0,0,16,16,16],x,y,z));
        continue;
    }
    const kind = c==="S" ? ["shell_",roleFor(x,y)]
        : c==="P" ? ["rim_"+(z===4?"front":"back")+"_",roleFor(x,y)]
        : c==="J" ? ["cap_"+(z===4?"front":"back")+"_",""]
        : null;
    assert(kind, "Unexpected south blueprint cell "+c);
    for(const box of boxes(kind[0],kind[1],"south"))
        worldBoxes.push(relative(box,x,y,z));
}
assert.deepEqual([ledger.S,ledger.P,ledger.J,ledger.F,ledger.C,ledger.A],
                 [24,16,2,10,1,7], "Material ledger changed");
assert.deepEqual(voxel.collision, voxel.selection, "Collision and selection disagree");
assert(voxel.collision.length <= 512, "Voxel profile exceeds configured V2 bound");
const normalized = a => a.map(b=>b.join(",")).sort();
assert.deepEqual(normalized(worldBoxes), normalized(voxel.collision),
    "Formed visible geometry and Voxel V2 collision diverge");

function rotate(b,f) {
    let [x0,y0,z0,x1,y1,z1]=b;
    for(let i=0;i<directions.indexOf(f);i++)
        [x0,x1,z0,z1]=[16-z1,16-z0,x0,x1];
    return [x0,y0,z0,x1,y1,z1];
}
for(const facing of directions) {
    for(const role of roles) for(const prefix of ["shell_","rim_front_","rim_back_"]) {
        const south = boxes(prefix,role,"south").map(b=>rotate(b,facing).join(",")).sort();
        const oriented = boxes(prefix,role,facing).map(b=>b.join(",")).sort();
        assert.deepEqual(oriented,south,"Incorrect rotation: "+prefix+role+facing);
    }
    for(const end of ["front","back"]) {
        const south = boxes("cap_"+end+"_","","south").map(b=>rotate(b,facing).join(",")).sort();
        const oriented=boxes("cap_"+end+"_","",facing).map(b=>b.join(",")).sort();
        assert.deepEqual(oriented,south,"Incorrect endcap rotation "+end+facing);
    }
}
const states = json(A+"blockstates/boiler_shell_panel.json").variants;
const caps = json(A+"blockstates/boiler_end_joint.json").variants;
for(const facing of directions) for(let i=0;i<10;i++) {
    const key="facing="+facing+",segment="+i;
    assert(states[key],"Shell visual state missing "+key);
    assert(caps[key],"End visual state missing "+key);
}
const book=json(A+"patchouli_books/world_of_magneticraft/en_us/entries/steelage.json");
const page=book.pages.find(p=>p.name==="Steel Boiler Vessel");
assert(page,"Patchouli page missing");
const mapping=page.multiblock.mapping;
const expectedSymbols=["a","b","c","d","e","f","g","h"];
expectedSymbols.forEach((sym,i)=>
    assert.equal(mapping[sym],"magneticraft2:boiler_shell_panel[facing=south,segment="+i+"]"));
assert.equal(mapping.J,"magneticraft2:boiler_end_joint[facing=south,segment=8]");
assert.equal(mapping.K,"magneticraft2:boiler_end_joint[facing=south,segment=9]");
const parseBook=c=> c===" "? "A" : c==="0"?"C" : c==="F"?"F"
    : c==="P"?"P" : (c==="J"||c==="K")?"J" : expectedSymbols.includes(c)?"S":"?";
for(let y=0;y<4;y++) for(let z=0;z<5;z++) for(let x=0;x<3;x++) {
    const cell=page.multiblock.pattern[3-y][4-z][x];
    assert.equal(parseBook(cell),body["layer"+(y+1)][z][x],
                 "Book construction does not match actual vessel block at "+[x,y,z]);
}
const shell=java(J+"block/stage/steel/SteelBoilerStructuralBlock.java");
assert(shell.includes('IntegerProperty.create("segment", 0, 9)'));
assert(shell.includes("getStateForPlacement(") && shell.includes("COLLISION[segment][i]"));
const controller=java(J+"blockentity/stage/steel/SteelBoilerVesselBlockEntity.java");
assert(!controller.includes("SteamNetworkManager")&&!controller.includes("serverTick"));
assert.equal(json(A+"models/item/boiler_shell_panel.json").parent,
    "magneticraft2:multiblock/boiler_vessel/shell_bottom_left_south");
assert.equal(json(A+"models/item/boiler_end_joint.json").parent,
    "magneticraft2:multiblock/boiler_vessel/cap_front_south");
console.log("PASS Steel IV-C visuals: book/blueprint, 4 orientations, native UVs,");
console.log("no volume intersections, collision/model agreement and component icons.");
console.log("Static only: compilation and Minecraft rendering still require testing.");
