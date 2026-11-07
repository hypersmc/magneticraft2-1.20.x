# Precision Iron Tooling — bored holder and wrought-fitted Lathe Cutter

**Branch:** `feature/iron-age-foundations`, draft PR #18.
**Stage:** source implemented; not yet compiled or player-tested.

## Player-facing reason for this milestone

Iron gear production on the existing Bronze Mechanical Lathe now has
a maintainable Iron Age cutting-tool upgrade, rather than another
standalone metal item. This uses the already established Precision
Boring Machine and its real mechanical load:

1. Existing hot rolling produces **4 cold Wrought Iron Rods from one Bar**.
2. **1 Wrought Iron Rod -> 1 Precision Iron Toolholder** in the
   existing formed Bronze Precision Boring Machine (20 RPM,
   8 torque, 240 *supplied* ticks). The toolholder is a machined
   stock part, not a new ore, machine or metal grade.
3. Craft **1 Toolholder + 1 Bronze Bushing + 1 Flint** into
   **1 Wrought Iron Lathe Cutter**.
4. Install the cutter in the existing Bronze Mechanical Lathe. It
   has **128 durability**, compared with the original Bronze cutter's
   **64 durability**. Both remain eligible for Bronze bearings,
   Bronze gears and Medium/Large Iron gears.

The wrought-iron body gives a more rigid mount to the same flint edge.
It does not pretend wrought iron is hardened steel. Both tools retain
the existing machining-time/torque contract and cost **1** durability
for Bronze, **2** for the Medium Iron gear and **3** for the Large
Iron gear. The old Bronze cutter and recipe remain valid.

No extra machine or new power source is created. The Boring Machine
already handles recipe-driven input/output, true load, NBT-tagged
output reservation, saves and tool wear. No Boring Machine Java was
changed. The Lathe only accepts the new registered tool as an
alternative; its output and persistence logic are untouched.

The Boring Machine passes NBT from the wrought rod to the toolholder.
Vanilla crafting into the final cutter **does not carry material
quality NBT**. No material-grade dependent durability is claimed.

## In-game acceptance checklist (single-player)

1. Roll a real Wrought Iron Bar to four Rods; insert a Rod in the
   existing formed Bronze Precision Boring Machine. With a valid
   abrasive bit and >=20 RPM/8 torque, receive exactly **one**
   Precision Iron Toolholder after 240 supplied ticks.
2. Confirm a wooden 8-torque path can supply the new 8-torque
   recipe only if the entire source/network is adequate; lower
   source torque or insufficient RPM must stall without output.
3. Verify 1 Rod consumed, 1 Toolholder produced, exact quality
   NBT copied, boring bit wears once, and no work on blocked
   output, interrupted source, or absent/worn boring bit.
4. Craft the new cutter from Toolholder + Bronze Bushing + Flint;
   check JEI, Patchouli, tab, item labels, and both 3D item
   models for contact, correct UVs, dropped/held/GUI displays.
5. Remove any installed cutter and insert the new one in the
   *existing* Bronze Lathe. Confirm the item is accepted and its
   128 starting durability is retained through save/reload.
6. Machine original Bronze rough gear and rough bearing with
   Iron cutter: 1 damage/batch; machine Medium Iron blank:
   2 damage/batch; Large Iron blank: 3 damage/batch.
   Machining progress, output count and quality rules must be
   unchanged. Re-test with the original Bronze 64-durability tool.
7. Run cutter to exhaustion. Confirm it disappears once and
   never generates an extra item or a free machining batch.
   Break/unform the Lathe with installed cutter and unfinished
   work to verify correct single drops (no duplication).
8. Test a saved old Lathe with its Bronze cutter already installed;
   confirm legacy NBT is still readable and cutter replacement
   needs the slot cleared. Check visuals, FPS, rotations, and
   no disturbance to the existing Copper/Bronze production chain.

## Technical checks

```sh
node tools/iron-age/check-precision-tooling-contract.mjs
./gradlew build
```

The JSON/model static checks are not equivalent to running a
full ForgeGradle build or testing in-game. Keep PR #18 draft and
unmerged; multiplayer testing remains deferred until after Steam Age.
