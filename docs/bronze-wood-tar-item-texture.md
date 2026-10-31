# Bronze Age — Wood Tar item texture

Wood Tar is currently registered as an **item**, not a Forge fluid. The Charcoal Retort produces it as an item byproduct. This change deliberately replaces the former vanilla ink-sac item model placeholder without changing recipes, fluid systems, inventory handling, or multiblock functionality.

## Visual direction
- `textures/item/wood_tar.png`: 16×16 transparent PNG, nearest-neighbor pixel art
- Near-black warm-brown resin body with subdued amber highlights and uneven glossy patches
- Distinct from coal/charcoal and from the water-tinted dirty-water fluid
- `models/item/wood_tar.json`: `minecraft:item/generated` with `magneticraft2:item/wood_tar` layer0

No wood-tar still/flowing sprites or bucket are introduced: those would be premature without a registered Wood Tar fluid. If fluid behavior is agreed later, implement it as a separate gameplay feature and then build sprite/tint/pipe/tank assets that fit the chosen transport system.

## Verification
PNG header is 16×16, full RGBA with transparency; item model references the exact asset path. No Java code, recipe or block model is modified. In-game inventory/JEI rendering and full Gradle compilation remain unverified until tested in Minecraft.
