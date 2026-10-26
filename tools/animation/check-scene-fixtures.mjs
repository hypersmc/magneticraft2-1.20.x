#!/usr/bin/env node
// Portable, dependency-free fixture checks for the JSON side of the Forge
// animation bridge. This does NOT replace ./gradlew build or in-game tests.
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const read = path => JSON.parse(readFileSync(new URL(path, import.meta.url), 'utf8'));
const base = read('../../src/main/resources/assets/magneticraft2/animations/bronze_mechanical_lathe.json');
const pack = read('../../docs/examples/json-animation-lathe-pack/assets/magneticraft2/animations/bronze_mechanical_lathe.json');
const states = ['always', 'running', 'stopped', 'overloaded', 'worn'];
const nearly = (actual, expected) =>
    assert.ok(Math.abs(actual - expected) < 1e-5, `Expected ${expected}, got ${actual}`);
const xyz = (actual, expected) => actual.forEach((v,i) => nearly(v,expected[i]));

function check(scene) {
    assert.equal(scene.format, 'magneticraft2:visual_animation');
    assert.equal(scene.format_version, 1);
    assert.deepEqual(scene.coordinate_system, {
        units: 'blocks', origin: 'controller', reference_facing: 'south'
    });
    assert.ok(Object.keys(scene.components).length >= 2);
    const active = new Set();
    for (const [id, component] of Object.entries(scene.components)) {
        assert.match(id, /^[a-z][a-z0-9_]*$/);
        assert.equal(component.model_offset.length, 3);
        assert.equal(component.pivot.length, 3);
        for (const v of [...component.model_offset,...component.pivot]) {
            assert.ok(Number.isFinite(v) && Math.abs(v) <= 256);
        }
        assert.ok(states.includes(component.visible_when ?? 'always'));
        if (component.parent) assert.ok(scene.components[component.parent]);
        let walk = id, depth = 0;
        active.clear();
        while (walk) {
            assert.ok(!active.has(walk), 'Component parent cycle');
            assert.ok(++depth <= 32, 'Component too deeply nested');
            active.add(walk);
            walk = scene.components[walk].parent;
        }
    }
    const tracks = new Set();
    for (const animation of Object.values(scene.animations)) {
        assert.equal(animation.type, 'driven');
        for (const track of animation.tracks) {
            assert.ok(scene.components[track.target]);
            assert.ok(scene.inputs[track.input]);
            assert.ok(states.includes(track.when ?? 'always'));
            const key = track.target + '/' + (track.when ?? 'always');
            assert.ok(!tracks.has(key), 'Duplicate rotation condition: '+key);
            tracks.add(key);
        }
    }
}

// Poses are in controller-relative blocks and use right-handed XYZ rotation.
// Child transforms are applied before parents; this agrees with nested
// PoseStack transforms in the Java animated-component renderer.
function pose(scene, componentId, point, angleDegrees, state = 'stopped') {
    const tracks = Object.values(scene.animations).flatMap(a => a.tracks);
    let id = componentId, result = [...point];
    while (id) {
        const component = scene.components[id];
        const relevant = tracks.filter(t => t.target === id);
        const choice = relevant.find(t => t.when === state)
            ?? relevant.find(t => !t.when || t.when === 'always');
        if (choice) {
            const axis = choice.axis;
            const signedAxis = Array.isArray(axis) ? axis : {
                x:[1,0,0],'-x':[-1,0,0],y:[0,1,0],'-y':[0,-1,0],
                z:[0,0,1],'-z':[0,0,-1]
            }[axis];
            assert.ok(signedAxis && signedAxis.length === 3);
            const sign = signedAxis.find(v => v !== 0);
            const angle = angleDegrees * choice.scale * sign * Math.PI / 180;
            const c = Math.cos(angle), s = Math.sin(angle);
            const pivot = component.pivot;
            let [x,y,z] = result.map((v,i) => v - pivot[i]);
            if (signedAxis[0]) [y,z] = [y*c-z*s, y*s+z*c];
            if (signedAxis[1]) [x,z] = [x*c+z*s, -x*s+z*c];
            if (signedAxis[2]) [x,y] = [x*c-y*s, x*s+y*c];
            result = [x+pivot[0],y+pivot[1],z+pivot[2]];
        }
        result = result.map((v,i) => v + component.model_offset[i]);
        id = component.parent;
    }
    return result;
}

check(base);
check(pack);
assert.equal(base.components.input_rotor.parent, 'gearbox_joint');
assert.equal(base.components.input_output_rotor.parent, 'gearbox_joint');
assert.equal(pack.components.shaft_marker.parent, 'input_rotor');
assert.equal(pack.components.shaft_marker.visible_when, 'running');
xyz(pose(base,'input_rotor',[1,.5,.5],90),[.5,1,-1.5]);
xyz(pose(pack,'input_rotor',[1,.5,.5],90),[.5,0,-1.5]);
xyz(pose(base,'input_output_rotor',[.5,1,.5],90),[.5,.5,-2]);
xyz(pose(pack,'shaft_marker',[1,.5,.5],90,'running'),[.5,0,-1.5]);
console.log('Animation JSON hierarchy/state/pose fixtures passed');
