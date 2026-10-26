# Magneticraft2 JSON animation scene graph (Forge 1.20.1)

**Development:** `feature/json-driven-animation`, child of Bronze PR #11.
**Status:** source implementation; user validated the first gold rotor and reversed
rotation in Minecraft. The multi-part scene graph extension still needs a new
Forge build and in-game verification. Schema is version 1 with optional fields
added without breaking the original single-rotor resource pack.

## What the JSON owns — and what it does not

Resource-pack JSON controls the **visual** model ID, transform, parent joint,
render layer, visibility condition, and typed movement relative to real machine
state. Gear V2 owns shaft phase and stall/overload physics. Server-side
multiblock construction, recipes, collision/selection voxel profiles and load
mechanics do not read these animation resources.

An animation lives at:
`assets/<namespace>/animations/<machine>.json`. The containing model must
still be a valid baked model, e.g.
`assets/<namespace>/models/animated/lathe_shaft.json`.

The Java machine opts into an animation resource ID and provides a frame with
named module phases and machine-state flags. **The Bronze Lathe currently
opts in to `magneticraft2:bronze_mechanical_lathe`.** New arbitrary machines
still require a client renderer binding; a resource pack cannot create a
gameplay machine just by adding JSON.

## Schema

```json
{
  "format": "magneticraft2:visual_animation",
  "format_version": 1,
  "coordinate_system": {
    "units": "blocks",
    "origin": "controller",
    "reference_facing": "south"
  },
  "components": {
    "gearbox_joint": {
      "model_offset": [0, 0, -2],
      "pivot": [0, 0, 0]
    },
    "input_rotor": {
      "model": "magneticraft2:multiblock/bronze_mechanical_lathe_input_rotor",
      "parent": "gearbox_joint",
      "model_offset": [0, 0, 0],
      "pivot": [0.5, 0.5, 0.5],
      "render_layer": "cutout"
    },
    "running_mark": {
      "model": "magneticraft2:animated/running_mark",
      "parent": "input_rotor",
      "model_offset": [0, 0, 0],
      "pivot": [0, 0, 0],
      "visible_when": "running"
    }
  },
  "inputs": {
    "drive_angle": {
      "source": "mechanical.visual_angle_degrees",
      "module": "mechanical_input"
    }
  },
  "animations": {
    "shaft_spin": {
      "type": "driven",
      "tracks": [
        {
          "target": "input_rotor",
          "property": "rotation",
          "axis": "z",
          "input": "drive_angle",
          "scale": 1.0,
          "when": "always"
        }
      ]
    }
  }
}
```

The example `running_mark` inherits the parent's rotation and does not
need a separate track. The actual sample resource pack is under
`docs/examples/json-animation-lathe-pack`: it changes the rotor to gold,
reverses its rotation, and adds a **running-only red marker** as a child.

### Hierarchy / coordinates

All geometry uses controller-relative **block units** and canonical SOUTH
authoring. For a root, `model_offset` is relative to the controller; for a
child, its `model_offset` is relative to the parent **after the parent's
animation transform**. A node's local matrix is:

`T(model_offset) · T(pivot) · R(axis, angle × scale) · T(-pivot)`

Parent matrices multiply their children's local matrices. Thus every child
inherits all parent rotations, including when the child has no track.
The machine's FACING yaw is applied **once** by the existing Lathe renderer,
outside the scene graph. Do not compensate again in JSON.

A component can omit `model` to act as a transform-only grouping joint.
All components require `model_offset` and `pivot`. Java validates parent
references, cycles and hierarchy depth before publication. Depth is capped
at 32, with a maximum of 256 components and 512 tracks.

### Typed input and state selection

Supported input source in this milestone:
`mechanical.visual_angle_degrees` — a real, interpolated, Gear V2 supplied
angle keyed by module name. The Lathe currently binds:
- `mechanical_input`: gearbox input shaft phase.
- `spindle`: clutched spindle phase, frozen when machining is not running.

Supported state conditions: `always`, `running`, `stopped`,
`overloaded`, `worn`.
- `visible_when` on a component determines whether its whole subtree is
  rendered. Parent visibility is inherited.
- `when` on a rotation track selects the condition for that track. If the
  same target has multiple tracks, the matching priority is:
  **overloaded → worn → running → stopped → always**. Duplicate
  target+condition tracks are rejected.
- For the Lathe, running/stopped comes from its synchronized machining state,
  overload from Gear V2's client overload flag, and worn from installed cutter
  damage of at least 75%. Flags may coexist: a worn cutter can still run.
- The input shaft may visually rotate while the Lathe is **stopped** (idle but
  mechanically powered). Running does not mean Gear V2 is spinning.
- Unsupported states, motion types and arbitrary expressions are rejected;
  JSON cannot run Java code or author server logic.

### Compatibility, baking and reload

The original single-rotor JSON remains accepted. If a resource pack does not
declare `input_output_rotor`, the Lathe still draws that part with its existing
Java renderer. A resource pack can add **new components** without adding
individual Java draw calls, provided their models can be baked.

Before drawing a scene the generic renderer verifies all *visible* component
models and bound inputs. If one model is missing, it aborts the entire JSON
scene and the existing Lathe rotor fallback takes over, avoiding half-rendered
machines. Models are registered from the CURRENT pack stack during Forge's
model registration event and JSON is compiled on reload, not per frame.

Resources for GeckoLib and other mods inside `assets/*/animations` are ignored
unless they explicitly declare the Magneticraft2 format or legacy structural
fields. A malformed own-namespace definition reports an error instead of
silently disappearing.

The current Lathe's render bounds are unchanged. Resource packs cannot
expand the machine's render bounds automatically; oversized models may cull.

## Validation and acceptance

Run the portable JSON pose fixture checks:

```bash
node tools/animation/check-scene-fixtures.mjs
```

They check component references, axes, parent transform semantics, default
and inverse rotation, output shaft rotation, and an inherited child. They do
**not** compile Minecraft/Forge Java.

Before merging PR #12, run `./gradlew build` and verify in Minecraft:

1. Default Lathe SOUTH/NORTH/EAST/WEST: input/output rotors at the same
   physical shaft connections, no floating gears or texture distortion.
2. Confirm a spinning Gear V2 source, unplug/stop, reverse speed, overload,
   and unload. The two rotors must follow real drive phase and never spin
   from an independent JSON timer.
3. Enable the provided resource pack and press F3+T: gold input rotor,
   reversed input direction, normal output direction, and red child marker
   appearing only while actually machining.
4. Set the pack's input `scale: 0`: its shaft visually freezes while the
   physical network and output shaft keep turning.
5. Disable/re-enable pack and reload; ensure all baked models switch back
   without pink/missing geometry.
6. Try invalid parent, cycle, bad model ID, invalid axis and missing driver:
   log one actionable error and use the Java fallback.
7. Confirm the V2 static mesh, ore washer, sifter, other Bronze machines and
   block collision shapes are unchanged; compare FPS with multiple Lathes.

The initial single-rotor gold model and reversed rotation have **already been
observed by the developer**, but the new hierarchy + running-only marker
haven't yet been visually accepted.

## Studio link

Native Qt/C++ Studio exporter lives separately on
`Magneticraft2-Studio:feature/json-animation-export` (PR #3), based on
`native-linux`. It emits this schema from explicitly saved model and motion
bindings. It does **not** yet preview poses or generate animated mesh JSON.
The next Studio milestone should add a native preview evaluator with the same
golden pose fixtures and visual pivot editing.

No source build or new in-game verification has run for the multi-part
extension in this environment. Keep both PRs as draft pending local tests.
