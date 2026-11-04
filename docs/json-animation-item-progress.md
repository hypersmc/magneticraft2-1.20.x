# Magneticraft2 visual animation v1 — real items and progress inputs

**Status:** Forge 1.20.1 Java source implementation on draft Bronze PR #11.
Not yet Gradle-compiled or verified in Minecraft since introduction.

This extension preserves existing `format_version: 1` scenes and
`machine_visuals` manifests. MGC2 Studio's editing UI and ZIP exporter are
**out of scope for this PR**, to be developed in a separate conversation.

## Real gameplay state, presentation-only controls

A machine's existing Java renderer may opt into these optional manifest
channels (all keys are stable machine-specific physical names):

```json
{
  "format": "magneticraft2:machine_visual",
  "format_version": 1,
  "animation": "magneticraft2:bronze_forge_hammer",
  "angle_channels": {},
  "integer_channels": {},
  "scalar_channels": { "work_progress": "process" },
  "item_channels": { "workpiece": "workpiece", "output": "output" }
}
```

- `scalar_channels`: Java supplies finite normalized processing progress
  in **[0,1]**. Never use world time as a substitute for real recipe time.
- `item_channels`: actual client-synchronized `ItemStack` values.
  `ItemStack.EMPTY` is a real empty slot and renders nothing; a missing
  channel fails the entire scene's preflight and invokes Java fallback.
- `angle_channels` and `integer_channels`: unchanged. The original
  constructors and old manifests continue working without new keys.

No JSON visual may create items, fabricate processing progress, change
server inventory, power, fluid or fluid/HEAT balance, or decide recipe output.
The server remains authoritative.

## Render a real ItemStack

```json
"workpiece": {
  "model_offset": [0.5, 1.53125, -0.5625],
  "pivot": [0, 0, 0],
  "item": {
    "input": "workpiece",
    "display_context": "none",
    "scale": 0.42,
    "rotation": [90, 0, 0]
  }
}
```

The `item.input` key is an alias from `item_channels`. Scale is uniform,
bounded to 1/16..4; display context is `none`, `fixed` or `ground`.
A component must not declare both an `item` and baked `model`. An empty
ItemStack is intentionally invisible. Item transforms share the scene's
parent-child hierarchy, model offset, pivot, parent orientation and
optional progress transforms. No item entities, special geometry or
per-frame ItemStack copies are introduced.

The source `ItemDisplayContext` draws the actual item model and its
NBT-dependent appearance. Real ports/slots are provided by the Java
machine, not inferred by the resource pack.

## Normalized progress-driven poses

```json
"inputs": {
  "progress": {
    "source": "machine.progress_normalized",
    "module": "work_progress"
  }
}
```

```json
{
  "target": "workpiece",
  "property": "transform_keyframes",
  "input": "progress",
  "when": "always",
  "keyframes": [
    {"time": 0, "position": [0,0,0], "rotation": [0,0,0]},
    {"time": 0.5, "position": [0,0.18,0], "rotation": [0,0,25]},
    {"time": 1, "position": [0,0,0], "rotation": [0,0,0]}
  ]
}
```

`time` is the fraction of the **real recipe's supplied progress**, not
seconds or a client-only animation clock. At least 2 and at most 32 keys
are permitted, strictly ascending from 0 to 1. Position axes are
bounded to ±16 blocks, keyframe rotations to ±3600 degrees. Linear
interpolation runs on the client without rebuilding static geometry.
`when` uses the existing RUNNING/STOPPED/OVERLOADED/WORN conditions.
Both baked models and item components can use this track type.

Existing phase rotation, sine/cosine translation, slider-crank, wear,
procedural belts and fallbacks keep their previous behavior.

## Forge Hammer integration

The Forge Hammer is the first **end-to-end opted-in machine**.

- Reads genuine `itemHandler` input and output slots; no visual-only
  item duplication. The original workpiece pose is unchanged by the
  default new scene.
- The Java machine synchronizes progress, active duration and RUNNING
  state. The client extrapolates from the last actual server packet
  only while RUNNING; power loss/stall stops the extrapolation when
  the server state arrives. Normalized progress does not affect real
  workpiece consumption, output or wear.
- Its accepted Gear V2 ram model and stroke are **not replaced**;
  JSON affects the workpiece only.
- If resource-pack scene/binding/model validation fails, the existing
  hardcoded workpiece render remains the fallback.
- `output` ItemStack is exposed for future Studio-authored output
  visuals. The default scene deliberately doesn't add a floating
  output icon, preserving existing machine appearance.
- Each machine must explicitly expose its real channels in its Java
  renderer before authoring a manifest. Existing other Bronze machine
  special-purpose item animations are not *automatically* migrated.

## Verification / acceptance before calling the milestone complete

- [ ] `node tools/animation/check-item-progress-contract.mjs`
- [ ] Existing animation validation scripts and JDK 17 `./gradlew build`
- [ ] Forge in-game default appearance: no duplicate or moving workpiece
  unexpectedly; no missing-texture/model warnings
- [ ] Example resource pack moves the actual item at real progress, pauses
  on stall, resumes without frame jumps, and disappears when slot empties
- [ ] F3+T enable/disable and missing scene/manifest fallback
- [ ] Machine facings N/E/S/W, save/reload and chunk unload
- [ ] Regression against existing six manifest opt-in machines and
  old resource packs
- [ ] FPS/TPS and network packet count; no per-tick sync spam
- [ ] Dedicated-server and two-client tests at the separately agreed
  post-Steam release-validation milestone

**Do not mark PR #11 merge-ready merely because Java source and these
fixtures exist.** This is a reusable authoring contract but Studio's
UI/export work remains separate.
