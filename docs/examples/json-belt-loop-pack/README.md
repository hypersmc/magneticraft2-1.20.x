# JSON belt-loop proof resource pack (Stamp Mill)

Requires Magneticraft2 Forge 1.20.1 on the branch
`feature/json-driven-belt-loops`, stacked over PR #12.

Zip the **contents of this folder** so `pack.mcmeta` is at the ZIP
root; place the ZIP in your Minecraft development instance's
`resourcepacks/` folder, select it and press **F3+T**.

### Expected visual change

The Stamp Mill's original leather loop should become a **light blue**
loop using Minecraft's built-in wool texture. The strip is slightly
wider than the original, and the texture deliberately travels **in the
opposite direction** of the bronze pulley rotation. That reverse
motion is intentional for this proof pack (`speed_scale: -1`) and
does NOT change the physical machine's power direction.

The three cams, three hammers, powertrain shafts, ore visuals, wear,
recipes and formed V2 geometry are otherwise unchanged.

### Things to look for

1. Does the texture move continuously over BOTH straight runs and
   around each pulley, with no full-texture stretching on short faces?
2. Is there any visible jump when the shaft passes 0°/360°?
3. Does reversing the real shaft also reverse the moving texture?
4. Does disabling the pack and pressing F3+T restore the original
   brown leather, original strip thickness and forward UV movement?
5. Does an older Stamp Mill scene override *without* `belt_loops`
   still render its original working belt (compatibility fallback)?
6. All four facings and a few Stamp Mills running at once: any new
   culling, FPS or incorrect local rotation issues?

If a custom pack accidentally names a nonexistent belt PNG, that
**entire scene should be rejected on resource reload**, leaving the
original Java Stamp Mill and belt renderer intact.

### What it proves (and what it doesn't)

This tests the new typed `belt_loops` JSON:
`texture`, `start_center`, `end_center`,
`rotation_axis`, `start_radius`, `end_radius`,
`drive_radius`, `half_width`, `half_thickness`,
`texture_repeat`, `speed_scale` and `texture_offset`.

The actual server's BeltPath, collision and Gear V2 behavior are
unchanged. This is visual-only and is **not yet** a general machine
creation or physically functional conveyor API.

The old `BronzeStampMillBeltRenderer` remains as a safe fallback;
the Ore Washer is not migrated in this pilot.
See `docs/json-animation-belt-loops.md` for the full spec.
