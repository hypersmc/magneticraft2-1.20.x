import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const [createSmall, createLarge] = process.argv.slice(2);
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const modelsDir = path.join(root, "src/main/resources/assets/magneticraft2/models/block");
const round = value => Math.round(value * 1000) / 1000;
function read(file) { return JSON.parse(fs.readFileSync(file, "utf8")); }
function bounds(model) {
  if (!model.elements?.length) throw new Error("Model has no geometry");
  let radial = 0, xExtent = 0, zExtent = 0;
  for (const e of model.elements) {
    const angle = (e.rotation?.axis === "y" ? e.rotation.angle : 0) * Math.PI / 180;
    const cx = e.rotation?.origin?.[0] ?? 8, cz = e.rotation?.origin?.[2] ?? 8;
    const c = Math.cos(angle), sin = Math.sin(angle);
    for (const x of [e.from[0], e.to[0]]) for (const z of [e.from[2], e.to[2]]) {
      const u = x - cx, v = z - cz;
      const px = cx + c * u + sin * v;
      const pz = cz - sin * u + c * v;
      radial = Math.max(radial, Math.hypot(px - 8, pz - 8));
      xExtent = Math.max(xExtent, Math.abs(px - 8));
      zExtent = Math.max(zExtent, Math.abs(pz - 8));
    }
  }
  return { radial: round(radial), xReach: round(xExtent), zReach: round(zExtent) };
}
const measurements = {};
function maybe(name, file) {
  if (file && fs.existsSync(file)) measurements[name] = bounds(read(file));
}
maybe("Create small", createSmall);
maybe("Create large", createLarge);
maybe("Magneticraft medium", path.join(modelsDir, "wrought_iron_gear_medium.json"));
maybe("Magneticraft large", path.join(modelsDir, "wrought_iron_gear_large.json"));
for (const [name, value] of Object.entries(measurements))
  console.log(name + ": " + JSON.stringify(value));
console.log("\nCurrent Magneticraft2 same-plane mesh distances (model pixels):");
console.log("8T + 8T: 16");
console.log("16T + 16T: 32");
console.log("8T + 16T diagonal: " + round(16 * Math.SQRT2));
console.log("Equal-module pitch radii 8/16 require mixed spacing 24; discrepancy: " +
  round(24 - 16 * Math.SQRT2));
if (measurements["Create large"])
  console.log("Create large/large radial-envelope gap at 32 px: " +
    round(32 - 2 * measurements["Create large"].radial));
if (measurements["Magneticraft large"])
  console.log("MGC2 large/large radial-envelope gap at 32 px: " +
    round(32 - 2 * measurements["Magneticraft large"].radial));
console.log("Note: envelope overlap does not ensure meshing; angular tooth width and phase matter.");
console.log("Create's normal large/large pairing is perpendicular, not parallel side-by-side.");
