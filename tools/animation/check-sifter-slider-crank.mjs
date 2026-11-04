#!/usr/bin/env node
// Dependency-free structural and exact-geometry fixtures for the Sifter JSON.
// These tests do not substitute for the ForgeGradle build or live Minecraft.
import assert from "node:assert/strict";
import {readFileSync} from "node:fs";

const read = relative => JSON.parse(readFileSync(new URL(relative,import.meta.url),"utf8"));
const normal = read("../../src/main/resources/assets/magneticraft2/animations/mechanical_sifter.json");
const override = read("../../docs/examples/json-animation-sifter-pack/assets/magneticraft2/animations/mechanical_sifter.json");
const almost = (actual, expected, msg, eps=1e-8) =>
    assert.ok(Number.isFinite(actual) && Math.abs(actual-expected) < eps,
        `${msg}: ${actual} != ${expected}`);

function verifyDefinition(data) {
    assert.equal(data.format, "magneticraft2:visual_animation");
    assert.equal(data.format_version, 1);
    assert.equal(data.coordinate_system.reference_facing, "south");
    const parts = data.components;
    const mechanism = data.mechanisms.sieve_drive;
    assert.equal(mechanism.type, "slider_crank");
    assert.equal(mechanism.rotation_axis, "x");
    assert.equal(mechanism.slide_axis, "z");
    assert.equal(mechanism.input, "drive_angle");
    assert.equal(data.inputs.drive_angle.source, "mechanical.visual_angle_degrees");
    assert.equal(data.inputs.drive_angle.module, "mechanical_input");
    assert.ok(mechanism.radius > 0 && mechanism.rod_length > mechanism.radius);
    const roles = Object.values(mechanism.parts);
    assert.equal(new Set(roles).size, 4, "Mechanism roles cannot overlap");
    for(const role of roles) {
        assert.ok(parts[role], `Missing component ${role}`);
        assert.equal(parts[role].parent, undefined,
            `Role ${role} must be a root in mechanism space`);
    }
    assert.equal(parts.upper_tray.parent,"sieve_carriage");
    assert.equal(parts.slide_shoes.parent,"sieve_carriage");
    assert.equal(parts.upper_tray.static_rotation.axis,"x");
    assert.equal(parts.upper_tray.static_rotation.degrees,10);
    for(let i=1;i<=5;i++) {
        assert.equal(parts.upper_tray.model_variants.models[i],
            `magneticraft2:multiblock/mechanical_sifter_upper_tray_wear_${i}`);
    }
    return mechanism;
}

// Mirrors the documented exact X-rotary/Z-slider closure and checks that
// the rod ALWAYS meets the follower, without stretching or disconnections.
function sample(config, degrees) {
    const phase=(degrees+(config.phase_degrees??0))*Math.PI/180;
    const [x,y,z]=config.center;
    const r=config.radius, l=config.rod_length;
    const pinY=y+r*Math.cos(phase), pinZ=z+r*Math.sin(phase);
    const rodAngle=Math.asin((pinY-y)/l);
    const rodEndY=pinY-l*Math.sin(rodAngle);
    const rodEndZ=pinZ+l*Math.cos(rodAngle);
    const stroke=r*Math.sin(phase)+Math.sqrt(l*l-(r*Math.cos(phase))**2)-l;
    const followerY=y,followerZ=z+l+stroke;
    almost(rodEndY,followerY,"Connecting rod / follower vertical closure");
    almost(rodEndZ,followerZ,"Connecting rod / follower horizontal closure");
    almost(Math.hypot(rodEndY-pinY,rodEndZ-pinZ),l,"Rigid rod length");
    almost(stroke,r*Math.sin(phase)+Math.sqrt(l*l-(r*Math.cos(phase))**2)-l,
        "Legacy Sifter displacement parity");
    return {x,pinY,pinZ,rodEndY,rodEndZ,stroke};
}

const base=verifyDefinition(normal), pack=verifyDefinition(override);
almost(base.radius,.12,"Original Sifter crank radius");
almost(base.rod_length,7/16,"Original Sifter connecting rod length");
almost(pack.radius,base.radius,"Test pack must not disconnect the pin");
almost(pack.rod_length,base.rod_length,"Test pack must not stretch the rod");
almost(pack.phase_degrees,90,"Resource pack rotation phase override");

for(let degrees=-720;degrees<=720;degrees+=3) {
    const a=sample(base,degrees);
    const b=sample(pack,degrees);
    const shifted=sample(base,degrees+90);
    almost(b.pinY,shifted.pinY,"Phase-shifted crank vertical pin");
    almost(b.pinZ,shifted.pinZ,"Phase-shifted crank longitudinal pin");
    almost(b.stroke,shifted.stroke,"Phase-shifted slider carriage");
    assert.ok(Math.abs(a.stroke)<=.121);
}
almost(sample(base,90).stroke,.12,"Peak forward stroke");
almost(sample(base,270).stroke,-.12,"Peak reverse stroke");

console.log("Sifter slider-crank: scene, wear models, 481 rigid poses and phase override passed");
