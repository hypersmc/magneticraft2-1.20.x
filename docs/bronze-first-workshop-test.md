# Bronze Age: first workshop slice (prototype)

Branch: `feature/bronze-age-foundry` (PR #11), rebased directly onto
`master` after Copper maintenance PR #10 and JEI support PR #9 merged.

This is **not** the complete Bronze Age. It starts the pattern/mold
manufacturing flow without pretending molten Bronze, a Foundry, a Casting
Basin, a Lathe, or wrought iron already exist.

## What is implemented

- **Bronze Pattern Maker**: single-block GUI-less wooden/bronze cutter.
  Crafted with an existing Bronze Ingot; uses the actual 3 Copper + 1 Tin
  -> Bronze Charge -> Bronze Ingots Copper Age progression.
  Sneak-right-click empty-handed switches the visible model between a
  gear pattern and a bearing pattern. Right-click with a plank to cut one
  reusable wooden pattern; it is delivered to the player's inventory.
- **Bronze Mold Table**: single-block GUI-less sand press. Insert a wooden
  gear/bearing pattern, one Sand (or Red Sand), then a Clay Ball by
  right-clicking with those items. Empty-hand right-click four times to
  press the selected Green Sand Mold.
  The wooden pattern survives **every** press, while sand and clay are
  consumed once per finished mold. The finished item is stored in the
  output slot until empty-hand collection. Sneak-right-click empty-handed
  recovers the wooden pattern or unused materials.
- The mold press saves slots and partial pressing progress, renders
  input/output items above its real tabletop, and drops its contents if
  broken. Both workbenches have open-leg collision instead of an invisible
  full-cube hitbox, 3D item models, recipes, mining tags, and Patchouli
  instructions.

## In-game verification (required before merging)

- [ ] Craft Pattern Maker after producing Bronze, not before.
- [ ] Rotate/select gear and bearing pattern on all four facings.
- [ ] Switching only changes the selected type; it does not consume planks.
- [ ] Plank right-click produces exactly one matching pattern, survival
      consumes exactly one plank, and creative remains non-consuming.
- [ ] Place Mold Table on each facing; rendered pattern, sand, clay and
      output sit visibly ON the physical tray (no hovering or clipping).
- [ ] One Sand (normal OR Red) + one Clay Ball + the correct pattern yields
      the expected mold after **four** presses.
- [ ] Wooden patterns remain installed and reusable between batches.
- [ ] Incorrect items cannot occupy input slots; the output cannot be
      overwritten with a mold of another type.
- [ ] With a full mold output stack, extra inputs are not consumed.
- [ ] Empty-hand collection and sneak-empty-hand recovery work.
- [ ] A half-pressed Mold Table resumes after chunk unload/world reload.
- [ ] Breaking the Mold Table returns all inventory items exactly once.
- [ ] Multiplayer clients see updated workpieces and completed mold output.
- [ ] Patchouli crafting pages and creative items load without missing
      model/texture warnings; mining tools and loot work correctly.
- [ ] Verify no impact on the existing Washer/Sifter or Bronze ingot loop.

## Remaining Bronze milestones

1. Design and build **JSON Foundry multiblock**, with heat/fuel behavior,
   measured bronze melt and visible molten metal.
2. Casting Basin with mold consumption and cooling that creates a
   **rough casting**, not a directly finished gear/bearing.
3. Gear V2-powered Mechanical Lathe that finishes castings with power
   constraints and physical workpiece feedback.
4. Load-bearing Bronze drivetrain upgrades, with replaceable wear parts.
5. Bloomery, iron blooms and wrought-iron finishing; this is the
   Bronze -> Iron progression milestone.

Exact metal quality propagation and heat temperatures have not been
approved; do not silently hard-code them.

## Source verification

Repository static checks confirm registration names, recipes, JSON model
variants, Patchouli references, saved mold inventory, and the rule that
patterns are reusable while sand/clay are consumed. **A full Forge 1.20.1
Gradle compile and an actual Minecraft test were not available in this
session and remain required** before the draft PR is merged.
