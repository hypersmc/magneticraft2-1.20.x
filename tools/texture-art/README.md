# Magneticraft2 original texture kit v1

Ten original, independently drawn 16x16 Minecraft-compatible RGBA PNG tiles for Copper/Bronze machinery.

- `texture-kit-v1.json`: **editable exact pixel source**. Each texture has a palette and 16 rows of 16 symbols. A character maps to a color; `.` means fully transparent.
- `build-kit.mjs`: dependency-free Node 18+ PNG exporter / regression check.
- Output: `src/main/resources/assets/magneticraft2/textures/block/{material,component}/`.

Run from the repository root:

```bash
node tools/texture-art/build-kit.mjs --check
# To regenerate intentionally after editing the JSON:
node tools/texture-art/build-kit.mjs
```

**Important:** These textures are **assets only**; none of the existing multiblock material aliases has been changed by this kit. Models will not use them until referenced as `magneticraft2:block/material/<name>` or `magneticraft2:block/component/<name>`.

The two screen mesh textures contain actual alpha cutouts; future screen geometry must use Minecraft/Forge **cutout** rendering where required. Do not apply them to a solid render layer and expect real openings.

This is a prototype palette; evaluate inside the game, in all facings, before replacing a whole machine's textures. Respect V2 UV tiling; never stretch a 16-pixel tile across several blocks. No vanilla texture pixels are copied.
