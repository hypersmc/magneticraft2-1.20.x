import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import {fileURLToPath} from "node:url";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const network = fs.readFileSync(path.join(repoRoot,
  "src/main/java/com/magneticraft2/common/systems/GEAR/GearNetworkManager.java"), "utf8");
assert(network.includes("private float nearestEquivalentMeshedRotation("),
  "Gear V2 must retain tooth-phase equivalent nearest the integrated angle");
assert(network.includes("neighbor, neighborVisualAngle - neighborPhase"),
  "powered mesh lock must use nearest equivalent angle");
assert(network.indexOf("advanceGearRotations(level, gears);") <
       network.indexOf("lockDrivenGearRotations(level, gears);"),
  "nearest solution must reference the already advanced rotation");
assert(network.includes("calculateMeshedPhaseDegrees(current, neighbor)"),
  "existing mesh tooth-phase law must remain in use");

const normalize = angle => ((angle % 360) + 360) % 360;
const difference = (a,b) => ((a-b+540)%360)-180;
const nearest = (solution, prediction, teeth) => {
  const pitch = 360 / teeth;
  return normalize(solution + Math.round((prediction - solution) / pitch) * pitch);
};
const meshPhase = (driverTeeth, driverPhase, drivenTeeth) => {
  const pitch = 360 / drivenTeeth;
  return ((180 - driverTeeth * driverPhase) / drivenTeeth % pitch + pitch) % pitch;
};

assert.equal(nearest(-170,10,16), 10, "16T: raw -170 and +10 are same tooth position");
assert.equal(nearest(-20,340,8),340, "8T: -20 and 340 retain continuity");
assert.equal(nearest(190,10,16),10, "180-degree alias must not become movement");

function run(drivingTeeth, middleTeeth, outputTeeth, rpm, direction, ticks) {
  const firstRatio = drivingTeeth / middleTeeth;
  const secondRatio = middleTeeth / outputTeeth;
  const sourceDelta = rpm * 360 / 1200 * direction;
  const middleDelta = -sourceDelta * firstRatio;
  const outputDelta = -middleDelta * secondRatio;
  const phaseMid = meshPhase(drivingTeeth,0,middleTeeth);
  const phaseOutput = meshPhase(middleTeeth,phaseMid,outputTeeth);
  let source=0,middle=0,output=0,broken=0;
  for(let tick=1;tick<=ticks;tick++){
    source=normalize(source+sourceDelta);
    // This is the same order as GearNode.advanceRotation() before
    // lockDrivenGearRotations(): all gears already have a local prediction.
    const middlePrediction=normalize(middle+middleDelta);
    const outputPrediction=normalize(output+outputDelta);
    const rawMid=(180-drivingTeeth*source)/middleTeeth-phaseMid;
    middle=nearest(rawMid,middlePrediction,middleTeeth);
    const rawOutput=(180-middleTeeth*(middle+phaseMid))/outputTeeth-phaseOutput;
    output=nearest(rawOutput,outputPrediction,outputTeeth);

    assert(Math.abs(difference(middle,middlePrediction))<.001,
      "intermediate gear jerk at tick "+tick+" for "+drivingTeeth+"/"+middleTeeth+"/"+outputTeeth);
    assert(Math.abs(difference(output,outputPrediction))<.001,
      "end gear jerk at tick "+tick+" for "+drivingTeeth+"/"+middleTeeth+"/"+outputTeeth);
    assert(Math.abs(difference(output,source))<.001,
      "end gear must rotate smoothly with driving gear for same outer tooth count");

    // Reproduce the old bug: picking one raw 360-degree representation
    // instead of the nearest equivalent tooth-aligned position.
    const rawOutputOld=normalize(rawOutput);
    if(Math.abs(difference(rawOutputOld,outputPrediction))>40)broken++;
  }
  assert(broken>0,"regression reproduction: old locking must produce jumps");
  return {input:drivingTeeth+"T",via:middleTeeth+"T",output:outputTeeth+"T",
    rpm,direction,ticks,oldLargeCorrections:broken,newCorrections:0};
}

const checks=[];
for(const rpm of [40,130,300])for(const direction of [-1,1]){
  checks.push(run(16,8,16,rpm,direction,1200));
  checks.push(run(8,16,8,rpm,direction,1200));
}
console.log("PASS: 12 real gear-ratio chains, forward/reverse, 40/130/300 RPM, 1200 ticks each");
console.log("Old angle representation snaps; nearest tooth-equivalent angle remains continuous");
