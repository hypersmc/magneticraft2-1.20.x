# Bronze Rolling Mill — K1 functional/Studio contract

**Stage:** source candidate, not compiled or Minecraft-verified. PR #11 stays draft.

## Functional role and material progression

The first Bronze III mill is a **powered COLD copper sheet finishing machine**,
not a fantasy unheated bronze smelter. The preceding **Mechanical Forge Hammer**
has been confirmed by the user to produce real Copper Plates. The mill uses
that output: **4 `magneticraft2:copper_plate` -> 4
`magneticraft2:copper_sheet`**, 180 supplied ticks, >=16 Gear V2 RPM,
>=6 torque (values are JSON-editable). Sheet stock has an immediate
downstream use: 4 Sheets -> 8 Water Pipes. The existing 6 Copper Plates ->
8 Pipes crafting recipe remains available to players without this mill.
The saved inventory, real input/output ports and material count all remain
conserved. Fewer copper plates per pipe is provisional efficiency from
forming thinner stock; it is not final mass-balance or quality bookkeeping.

**NOT in K1:** heated bronze/iron billets, rod stock, roll gap selection,
different sheet thickness grades, lubricant, temper and metallurgical
quality attributes. These need a proper heat source, useful downstream
parts and an agreed material/quality contract before adding them. A 3D
animation by itself does not authorize extra recipes.

## Actual 5 wide × 3 tall × 3 deep multiblock

The **server-side SOUTH** base (Y1), rows from BACK to FRONT:

```text
SSISS
USSSO
SSCSS
```

- `S` stone bricks, `P` oak planks, `A` true required AIR.
- `I` **rear center (local x=2,z=0)** = real Gear V2 Mechanical Input.
- `U` **left mid-side (x=0,z=1)** = real Item Input.
- `O` **right mid-side (x=4,z=1)** = real Primary Output.
- `C` **front center (x=2,z=2)** = the controller.
- Y2: `PAAAP` on all three rows; Y3:
  `PPPPP / PAAAP / PPPPP`. AIR must stay AIR when forming.
- West, North and East use exact JSON rotations of this SOUTH geometry;
  renderer anchor offsets rotate as well.
- Patchouli's visual rendering convention mirrors the SOUTH X axis.
  Consequently its displayed Y1 rows are `SSISS / OSSSU / SS0SS`.
  The visual preview and the actual JSON are *intentionally different*
  coordinate presentations, not different port semantics. This mistake
  previously blocked Forge Hammer formation. A contract regression check
  must always compare all mirrored book rows with the actual blueprint.

## Mechanical path and model strategy

The real rear input faces the LOWER roller's Z-axis shaft. Two horizontal
rolls are vertically stacked, **their axes parallel to the rear input shaft**.
A compact housed timing train mechanically reverses the upper roller
instead of requiring a new standalone Gear V2 node or a decorative,
physically impossible external gearbox. Feed travels left -> right,
perpendicular to both axes, through the roller nip. The metal is thin
cold-workable copper, not the future reheated Bronze/Steel billet.

The static **editable source model** is
`tools/multiblock-models/designs/bronze_rolling_mill.json`; it is the
dimension- and UV-authoritative base for future Magneticraft2 Studio work.
`tools/multiblock-models/sources/bronze_rolling_mill.json` is derived;
the regular generator produces block-local JSON models in all four
facing directories. Static model/voxel profiles are separate from the
two cached, counter-rotating baked-roller models rendered by the BER.
Only existing 16px-aligned vanilla block textures are used, with native
density UVs (no stretching). **Real** I/O and mechanical modules must
never receive baked decorative faces. The server's Voxel V2 collision
includes the static supports and conservative enclosed roller sweep.

### Studio visual reference
- Editable exact-layout reference: `docs/reference/bronze-rolling-mill-studio.svg`.
- Single concept render was also generated in chat as a **non-authoritative
  visual mood board**, and must not override dimensions/port locations,
  operating numbers, or actual stock types; some concept-panel numbers and
  external-gear layouts are speculative.
- The current frame is a coherent but intentionally provisional Minecraft
  model: stone base, structural wood posts, supported bronze crosshead,
  enclosed timing gearing, lower/upper rollers, left feed and right exit.
- Later Studio art may improve surface details, the roll bearings, guard
  rails, realistic roll spacing and timing cover, but must keep all real
  ports connected and preserve genuine texture density. Do not confuse an
  image with a usable 3D source/import file.

## Gameplay, power and persistence

Use server-only ticker registered to **the exact**
`BlockEntityRegistry.BRONZE_ROLLING_MILL_BE` (not Forge Hammer or Stamp Mill).
This has already caused an in-game dead-machine regression and must be
verified in source before every new controller.

The Gear V2 network supplies the requested torque. The controller
registers its demand with the real Mechanical Input module, qualifies
load activation against raw RPM to avoid overload load-chattering, then
progresses **only** when the manager reports `supplied()`. It unregisters
load when no valid recipe, broken rollers, full output or destruction.
It never consumes items when the network is stalled.

The existing generic multiblock recipe handler matches this specific
machine ID, input identity/count, torque/speed and output. Unsupported
fluid/byproduct recipes are refused rather than silently discarded.
The output must pass a simulated full-stack insertion before consuming
the exact 4 input items, then be inserted once. Replacing input item/tags
resets an incompatible partial batch.

The replaceable `bronze_roller_sleeves` item represents a serviceable
PAIR of roll surfaces. The first pre-installed pair lasts **512 completed
batches**, loses exactly one unit per completed batch, and is replaced
at the controller outside normal item automation. Sneak/empty-hand
shows wear; controller normal interaction shows input, progress,
actual/required RPM, torque, upstream load and fault. NBT persists
`MillProgress`, `MillRecipe`, `MillActiveInput`, `MillRollers`,
`MillRunning` plus the existing inventory and Voxel V2 structure.

## Regression checklist (NOT YET PASSED IN MINECRAFT)

1. Run `node tools/multiblock-models/build-designs.mjs --check`,
   `node tools/multiblock-models/generate.mjs --check`,
   `node tools/bronze-rolling-mill/test-contract.mjs` and
   `./gradlew build`. A structural test does NOT replace a real compile.
2. Build and form all four JSON orientations from the Patchouli book.
   Confirm one real rear Gear V2 input and one left/right pair of true
   input/output blocks. Incomplete AIR or swapped ports must reject.
3. Input 3 Copper Plates: no progress; insert the 4th and supply >=16 RPM
   and >=6 torque. Verify **180 supplied ticks -> exactly 4 Copper Sheets**,
   and no duplicate output. The original Forge Hammer remains usable.
4. The new alternative recipe makes **8 Water Pipes from 4 Sheets**.
   The original recipe from **6 Plates** still makes the same pipes.
5. Remove or overload the drive mid-batch: no progress and no consumption.
   Check upstream load registration/clearing, clutch state and no oscillation.
   Clear output blocking and resume paused batch without dropping inputs.
6. Save/reload with partial progress and almost-worn roller sleeves.
   Check NBT integrity, linked modules, visual rotation and collision.
7. Wear through a complete service cycle; verify worn status, one sleeve
   item consumption on replacement, and exactly one wear per output batch.
8. Test breakage/drops, automatic item ports, and separate two-client
   synchronization and persistence. Observe FPS and UVs from several
   positions; no floating shafts, duplicate gears or depth artifacts.
9. Regression: existing Forge Hammer still advances under power and
   Refractory/Ore Roasting Kilns are unchanged. Do not mark PR merge-ready
   based on one machine demonstration.
