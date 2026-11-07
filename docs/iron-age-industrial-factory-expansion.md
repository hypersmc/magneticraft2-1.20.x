# Iron Age — industrial factory construction and additional multiblocks

**Magneticraft2 / Forge 1.20.1** · **Design expansion: 2026-11-07**  
**Related:** [Full Iron Age content plan](iron-age-full-content-plan.md), [Iron Age roadmap](iron-age-roadmap.md), draft PR #18.  
**Decision status:** Proposed scope, **not registered blocks, recipes, finished models or permission to implement the entire catalog**. Continue one tested feature slice at a time. Iron 0A's Wrought Iron Forging Die is the only player-accepted Iron Age feature at this point.

## 1. Why Iron needs a distinct factory identity

The first Iron plan had two new multiblocks and only two ordinary structural blocks (Frame and Casing). That is too little for an age meant to build real industrial facilities. Iron should introduce:
1. **Mechanical infrastructure:** correct iron gears, shafts, gearbox and finite-inertia flywheel for genuinely heavier networks.
2. **Specialized manufacturing:** Stock Shear, Forge Hearth and a carefully differentiated Mechanical Draw Bench.
3. **Factory construction:** a consistent wrought-iron building family for machine halls, platforms, service access and visibly supported multiblocks.

Decorative building blocks are useful in Minecraft *as building blocks*. They do not need imaginary torque, vibration, stress or pollution mechanics to justify existing. Do not create tile entities or per-tick logic for a normal wall, stair, walkway or grate.

**Total proposed core catalog:** **16 registered blocks = 3 distinct JSON multiblocks + 5 mechanical/transmission blocks + 8 structural/factory-building blocks.** The Draw Bench is a design-gated proposed third multiblock: it joins implementation only once a downstream demand for precise drawn stock is established. The Mechanical Hoist/Gantry is deliberately **conditional**, outside this total. These numbers describe a *proposed content register*, not an accepted implementation quota.

## 2. Factory construction family — coherent Iron palette

All six additions below are **proposed core construction blocks**. Frame and Casing were already in the earlier Iron plan; this brings the structural/building family to eight.

| Candidate block ID | Geometry / player use | Industrial use / source material | Notes and scope |
| --- | --- | --- | --- |
| `riveted_iron_frame` (already proposed) | Full structural cage/frame block | Supports the Iron heavy-machine multiblocks; real frame stock + fitted joints | Prefer solid support connection, no magical load bonuses |
| `riveted_iron_casing` (already proposed) | Enclosed machinery casing | Heavy factory enclosures, new machine structure | Different silhouette from decorative panel |
| `iron_structural_beam` | Full-block or inset I-beam profile with X/Y/Z axes | Columns, spans, overhead supports, Iron machine frames | Use believable connections/joint plates; no floating ends |
| `iron_floor_grating` | Thin or inset metal mesh/walkway floor | Elevated factory platforms, catwalk decking, service access | Use **cutout**, not stacked translucent panes; no fake item pipe or fluid transport |
| `iron_plate_panel` | Thin, facing-oriented wrought-metal wall/cover | Machine bay walls, maintenance enclosures, player construction | Not a second full `riveted_iron_casing` block; distinct shape and recipe |
| `iron_machine_foundation` | Full structural foundation block | Base for new heavy machines and factory floors | Can be a multiblock ingredient; **do not require vibration physics** without a separate decision |
| `iron_catwalk_railing` | Narrow-edge railing with safe collision | Visible guardrails for iron grating and platforms | Prefer standard blockstate neighbor connection, no per-tick scan |
| `iron_service_hatch` | Hinged floor/wall-access panel, normal placement/redstone behavior | Maintenance openings in industrial buildings | Reuse vanilla trapdoor mechanics where suitable; no special server machine interface without a need |

**Visual family contract:** beaten dark wrought-iron faces, restrained bronze/copper fasteners where real, visible reinforcing ribs and mechanically plausible weld/rivet locations. Keep Minecraft 16×16 pixel clarity, nearest-neighbor sampling, and face-aligned UV dimensions. Connecting beams, railings, grating and panels must not display glaring disconnected ends, floating bolts or stretched textures. Prefer the same existing dark-work-iron texture language and shared geometry conventions; inspect current assets before creating new ones.

**Structural vs decorative interaction:** Frame, Casing and Foundation may appear as JSON multiblock construction ingredients. Beam, Grating, Panel, Railing and Hatch should also be useful just for player buildings and should be survival-craftable. They must not all require an advanced powered shear/Draw Bench to become available; a basic structural starter palette must be possible using already produced wrought plate/bar via the Bronze workshop.

### Optional construction variants (not required for first Iron completion)

- Matching iron ladder, platform/stair/slab, iron service door, corner beam joint, diagonal brace, overhead support, maintenance access cover, drain grate or windowed panel.
- Variants should be added only if their geometry or gameplay genuinely improves building, and should share blockstate/model families where this is maintainable. Do not create many individual items to inflate content quantity.
- A hatch or railing with actual collision is different from an animated maintenance opening on an already-formed machine; do not confuse the two.

### Crafting and progression

- **Early Iron construction:** Forge/Roll existing Wrought Iron into Plate/Bar, then craft basic Beam, Panel, Foundation and Grating with believable material counts. The player should be able to build a visible iron workshop **before** the first new heavy multiblock.
- **Fabricated Iron construction:** Cut stock and rivets from the Stock Shear (when developed), then manufacture Riveted Frame and Casing for **heavier** multiblocks. Make sure the **Stock Shear does not require rivets it can only produce itself**.
- **Service-access family:** Railing and Hatch derive from sheets/frames when available; no advanced gate simply for decorative industrial building.
- Maintain an **ingot-equivalent** mass ledger (not necessarily equal item counts), quality metadata and material cost. Do not generate more iron by repeating a reversible plate → sheet → rivet chain.
- In-game collision, waterlogging where appropriate, rotation/facing, break/drops, light/occlusion, survival recipes, creative-tab categories, JEI and Patchouli are part of acceptance.

## 3. Three distinct Iron multiblocks

### A. Iron Stock Shear — proposed core (already in the full plan)

**Role:** Forceful shear/punch cutting of heavy bar or plate, including meaningful fastener/blank output. Unlike the Bronze Press, it performs cutting rather than forming, and unlike the Lathe it does not turn a cylindrical workpiece. Candidate 3×3×2 footprint **not approved**.

- Physical input/output, replaceable shear blade, real Gear V2 source, eccentric with supported bearings, progress-driven workpiece and cutting stroke.
- Test a genuine **greater-than-8-torque** load supplied via a properly configured large water wheel and strengthened Iron gear/gearbox path. Candidate values need in-game validation.
- No output when unpowered or jammed; no loss on blocked destination; no wear without a successful completed batch.

### B. Iron Forge Hearth — proposed core (already in the full plan)

**Role:** Higher-throughput hot-stock preparation; charcoal, HEAT and actual AIR with safe multiple-item access, compatible with existing kiln/hot-workpiece semantics. Candidate size 3×3×2 or 3×3×3 **not approved**.

- Reuse existing Refractory Kiln/HEAT/AIR technology and avoid new permanent hot item IDs.
- **Existing Kiln remains sufficient** to build the first Iron components and to use Forge Hammer, preventing a circular unlock.
- No reduction to pig iron, no Steel conversion, no Steam boiler; save/reload must preserve real heat expiry.

### C. Mechanical Draw Bench — proposed *third* core multiblock, consumer-gated

**Why it exists:** Unlike a Rolling Mill (rolls stock between rollers), a draw bench **pulls** workable stock through a supported die to make repeatable, dimensionally consistent bars/rods or wire. Its unique outputs must be used in real structural fasteners, accurate coupling pins, shaft keys and potentially later-era cable/mesh preparation. A few basic rough rods can still be produced on the older workshop; the bench adds **precision**, not an artificial prerequisite for all iron processing.

**First player-facing operation (design candidate):** work an appropriately prepared/hot rough wrought-iron rod into a **calibrated rod** at the cost of real drawing die wear, power and a valid material balance; a calibrated rod then becomes accurate pins, reliable fasteners or machined coupling parts. Do not ship an item called `calibrated_rod` until the real consumers are defined in recipes.

**Proposed construction:** long, narrow JSON multiblock (footprint TBD after actual models and available port rules); visible aligned infeed, supported draw die, Gear V2 winch/chain or roller-drive grip, output end and serviceable frame. Die/stock must be mechanically connected, not floating. Use existing mechanical module + item IO; **no item entities or per-tick mesh allocations**.

**Rule for activation:** identify at least two useful output consumers, demonstrate it does not merely duplicate the Rolling Mill or Lathe, and prove honest drawing heat/power/size limits. Otherwise it stays a conditional design candidate, not a blocker to completing Iron Age.

### D. Mechanical Hoist / Gantry — conditional *additional* multiblock

**Potential Iron Age identity:** Use legitimate iron beams and lifting equipment to do meaningful work in the world, not just make another recipe. A manually/gear-driven hoist would make Iron infrastructure visibly useful outside the crafting loop.

**Open gameplay question:** What does it lift—ordinary items, filled crates, portable machine parts, or actual placed world blocks? These are different mechanics. Begin with a bounded, fixed-position lift/transfer of compatible item cargo only if the existing belt/arm system does not already solve the same problem. *Moving world blocks/multiblocks* needs a separate large architecture/safety decision; do not infer permission from this plan.

**Safety/performance:** finite range/height, source-driven lifting speed and load, no item duplication or lost NBT, breaks/faults return contents, chunk-boundary protection, safe path obstruction checks, single source of truth, no moving item entity workarounds. If none of these can be implemented cleanly with meaningful player use, omit the machine.

**Hoist decision gate:** write a concrete in-world interaction demonstration before introducing any block ID, recipe or custom renderer. It remains outside the proposed 16-block core.

## 4. Phase-by-phase placement

| Phase | Factory and new machinery scope | Gate |
| --- | --- | --- |
| **Iron 0A** | Wrought Iron Forging Die — already player accepted | Existing Bronze → Iron path |
| **Iron 0B / Iron I** | Heat-safe Plate/Bar/Rod/Sheet through existing kiln/hammer/rolling/press machinery; early Beam, Panel, Foundation, Grating | Correct workpiece HEAT and quality; no circular manufacturing |
| **Iron II** | Actual Iron medium gear, shaft/gearbox, precision component processing; Railing and Hatch | Verify full Gear V2 8-torque bottleneck and real >8 delivered load |
| **Iron III-A** | Stock Shear, blade and rivets; Riveted Frame/Casing and later upgraded fabrications | Output consumers and high-load network tested |
| **Iron III-B** | Draw Bench and precision rods **only if outputs have genuine consumers** | Distinct from Rolling Mill/Lathe, drawn-stock material ledger |
| **Iron IV** | Forge Hearth and Iron Flywheel; optional Hoist feasibility study | Existing Kiln/Flywheel retain purpose; no Steam/Steel jump |
| **Steel handoff** | Real structural and machined Iron products useful in Steel facilities | Steel chemistry and furnaces decided in Steel Age itself |

**No automatic content quota:** The core catalog is a target for design review; accept or remove candidates based on whether they make Iron construction enjoyable, mechanically plausible, buildable and maintainable. If the Hoist or Draw Bench turns out redundant, do not build it just to match the row count.

## 5. Cross-system engineering and verification

- **Registries:** New general construction blocks should be simple blocks wherever possible. Reuse existing registry, item blocks, creative tabs, state/rotation conventions, loot, textures and Patchouli formats. Item models must follow original Minecraft UV density.
- **Multiblocks:** Current JSON structures, real item ports, Gear V2, maintenance and resource-pack scenes are authoritative. All four facings must form exactly as shown in Patchouli/JEI. No forced bespoke architecture.
- **Visuals:** Use MGC2 Studio when ready; real item movement comes from the shared Java animation runtime. Placeholder geometry must still look physically connected in-game.
- **Power:** No new power source or torque magic; inspect mixed wood/iron nodes, ratios, overload, inertia and mechanical load accounting. Don't silently change old limits.
- **Materials:** Reuse existing Wrought Iron input. Forge from real hot stock where appropriate, preserve quality metadata, record conservation and make wear actionable.
- **Pollution:** Future cross-age simulation; no arbitrary emissions per block introduced by this catalog.
- **Dedicated multiplayer:** **Every multiplayer-related feature/test is deferred until *after Steam Age***, by user decision; do not use it as an Iron acceptance gate. Maintain server-authoritative designs without performing multiplayer testing now.
- **Per-slice validation:** JDK 17 `./gradlew build`, block/recipe/JSON checks, survival crafting, render angle/texture QA, persistence, power, form/unform, wear, chunk unload and existing Bronze regressions. Source inspection is not a compilation result.
- **PR management:** Keep PR #18 draft, do not merge it or modify long-running PR #17 unless requested. Update the roadmap as users approve individual designs and finish them.

**No Java or resource implementation is authorized by this expansion alone.**
