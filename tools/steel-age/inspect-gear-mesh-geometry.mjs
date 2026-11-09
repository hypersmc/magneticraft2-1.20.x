// Diagnostic only: reads current block model cuboids and mesh distances.
// Not an animated collision or Forge build verification.
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
function read(name) {
  return JSON.parse(fs.readFileSync(path.join(root,
    "src/main/resources/assets/magneticraft2/models/block", name + ".json"), "utf8"));
}
function outerRadius(model) {
  if (!Array.isArray(model.elements) || model.elements.length === 0)
    throw new Error("Missing visible model cuboids");
  let max = 0;
  for (const element of model.elements) {
    for (const x of [element.from[0], element.to[0]]) {
      for (const z of [element.from[2], element.to[2]]) {
        // Y rotations around (8, 8, 8) preserve this planar radial length.
        // A noncentral rotation axis would need its own transformed-corner test.
        max = Math.max(max, Math.hypot(x - 8, z - 8));
      }
    }
  }
  return max;
}
const small = outerRadius(read("wrought_iron_gear_medium"));
const large = outerRadius(read("wrought_iron_gear_large"));
const pixel = n => Math.round(n * 1000) / 1000;
const measurements = {
  medium_outer_corner_radius_px: pixel(small),
  large_outer_corner_radius_px: pixel(large),
  medium_medium_grid_center_px: 16,
  large_large_grid_center_px: 32,
  medium_large_diagonal_grid_center_px: pixel(16 * Math.sqrt(2)),
  radial_envelope_gap_medium_medium_px: pixel(16 - 2 * small),
  radial_envelope_gap_large_large_px: pixel(32 - 2 * large),
  radial_envelope_gap_medium_large_px: pixel(16 * Math.sqrt(2) - (small + large)),
  ideal_equal_module_mixed_pitch_center_px: 24,
  mixed_pitch_compression_vs_diagonal_px: pixel(24 - 16 * Math.sqrt(2)),
};
console.log(JSON.stringify(measurements, null, 2));
console.log("NOTE: signed radial envelopes are not actual tooth collisions. " +
  "A positive value indicates a guaranteed envelope gap; a negative value " +
  "indicates possible interference only. Verify moving gear teeth in-game.");
