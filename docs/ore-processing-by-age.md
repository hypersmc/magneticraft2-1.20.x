# Magneticraft2 — ore processing unlocks by age

Status: **reviewed progression contract**, not a claim that all ores are processed or automatically age-gated.
Authoritative machine-readable mineral assignment: [ore-processing-by-age.json](ore-processing-by-age.json).

| Age | Registered ore families (normal registry names) | Processing state |
| --- | --- | --- |
| Stone | `kaolinite_ore`, `lignite_ore` | Planned |
| Copper | `chalcocite_ore`, `cassiterite_ore`, `quartz_ore` | Active (Copper machines) |
| Bronze | `magnetite_ore`, `hematite_ore`, `limonite_ore`, `galena_ore` | Planned |
| Iron | `graphite_ore`, `anthracite_ore`, `sphalerite_ore`, `silver_ore` | Planned |
| Steel | `manganese_ore`, `chromite_ore`, `garnierite_ore`, `kimberlite_ore` | Planned |
| Steam | `sulfur_ore`, `saltpeter_ore`, `apatite_ore`, `cinnabar_ore` | Planned |
| Gasoline | — | Planned |
| Electric Generator | `bauxite_ore`, `cryolite_ore`, `silicium_ore`, `titanite_ore`, `wolframite_ore`, `tantalite_ore`, `cobaltite_ore`, `platinium_ore` | Planned |
| Nuclear | `uraninite_ore`, `osmiridium_ore` | Planned |

**Inventory:** 31 mineral families and 42 already registered ore blocks including Deepslate variants. A `*_deepslate_ore` block belongs to its normal ore family's age. The names above are the actual `magneticraft2:` registry IDs, **not** localized display names. This file does not change block IDs.

## Age contracts

### Stone

Kaolinite and Lignite exist as world ores, but do not yet have a usable ceramic/fuel processing chain. Do not call them implemented.

### Copper

**Already implemented:** Chalcocite has direct/crushed/concentrate furnace yields of 1/2/3 Copper; Cassiterite produces Tin via washing and sifting; Quartz and its Deepslate variant can be ground, washed and sifted into Nether Quartz. The Primitive Grinder is the first ore breaker and remains available beside the Bronze Stamp Mill.

### Bronze

**Preparation unlock, not iron ingots:** Magnetite/Hematite/Limonite should feed ore-dressing and eventually the Bloomery; Galena/Lead remains an optional Bronze-era chemistry/metal route. No iron concentrate, roasted ores, bloom or Lead items exist yet. Do not add dead-end Stamp Mill recipes today. First bloom/forged wrought iron is the Bronze → Iron milestone.

### Iron

Keep Graphite/Anthracite/Zinc/Silver chemistry for machines that can actually use these materials. Bloomery requires charcoal rather than an early-locked Coal replacement.

### Steel

Manganese, Chromite, Garnierite and Kimberlite should support alloying/industrial abrasives; this age needs smelters and steel recipes before outputs are made craftable.

### Steam

Sulfur, Saltpeter, Apatite and Cinnabar are feedstocks for a dedicated chemical process, not furnace-to-ingot conversions.

### Gasoline

An extraction/refinery age for oil/fuels; it may reuse existing materials such as sulfur. It does not require a new mineral ore simply to have an age-exclusive drop.

### Electric Generator

Electric processing/energy-intensive separation allows Bauxite + Cryolite, Silicium (silicon), Titanite, Wolframite, Tantalite, Cobaltite and Platinium (platinum). *Silicium* and *Platinium* are existing registry names; preserve their IDs for world/save compatibility.

### Nuclear

Uraninite is a uranium-bearing mineral; Osmiridium is an existing legacy registry ore. Decide its physically meaningful processing/end use before enabling recipes.

## What is already implemented and must remain compatible

- Stone ends with Chalcocite Ore + Coal in the Primitive Furnace → Copper Ingot + Slag, not a hidden late iron dependency.
- Copper uses **Chalcocite, Cassiterite, Quartz** (including Deepslate Quartz), with separate Grinder, Ore Washer and Sifter roles.
- Bronze Stamp Mill currently crushes Chalcocite, Cassiterite, both Quartz ores, Cobblestone and Cobbled Deepslate. The Quartz result continues through Washer/Sifter; the Gravel result can be sifted into Sand/Flint. No recipes for the other minerals exist yet.
- Tin Concentrate + Coal → Tin; 3 Copper + 1 Tin → Bronze Charge; Bronze Charge + Coal → 4 Bronze Ingots. Preserve these established counts.
- The planned Bloomery + air/charcoal/flux + wrought iron finishing is the **end-of-Bronze milestone**. Vanilla Iron/Gold compat recipes remain untouched until an explicit compatibility decision.
- Keep output item/NBT quality propagation a **separate** tracked feature. Mod `copper_ingot_low` and vanilla Copper are not to be silently interchanged in furnace recipes; current 1/2/3-Copper output balances stay unchanged.

## The fix applied before the next multiblock

All **42 registered ore blocks** now have a self-drop block loot table, a pickaxe mining tag and the current Stone-tool harvest baseline. Previously only Chalcocite had all three; the others could be mined without returning ore to the player. The Bronze Stamp Mill and Mechanical Lathe controllers, both requiring a correct tool, now have the corresponding pickaxe/Stone-tool tags too.

This is a **harvestability repair**, not a new technology unlock: all 31 minerals may physically be found/collected as before, but only the implemented ore processing chains turn them into useful products. Keeping the interim Stone-tool harvest baseline also avoids a paradox where the player needs an Iron pickaxe to mine ore for the *first* iron bloom. Decide later metal-ore hardness/tool gates only with a tested handoff.

## Deferred deliberately, rather than introducing dead ends

- Define proper iron mineral chemistry (magnetite/hematite/limonite) before adding Crushed/Washed/Concentrate variants or Stamp Mill recipes. Distinguish roasting/flux from mechanical washing.
- Define Galena lead and Sphalerite zinc paths separately. No direct ore → metal ingot via Stamp Mill.
- Specify the first Bloomery and the charcoal/refractory/forced-air prerequisites before restricting vanilla recipes.
- Revisit ore-to-ingot **quality**, item wear and processing impurity calculations as one consistent NBT/recipe system.
- Keep the existing Stamp Mill input/crushing animation bugs on their planned fix list; this sweep does **not** modify its renderer or recipes.

## Pre-next-multiblock acceptance

The processing matrix and all registered ore harvestability must be checked against current item/block registration. Existing six Bronze Stamp Mill recipes and the Copper 1/2/3 yield hierarchy must still parse and use their original items. The Copper/Bronze guide pages should describe current functionality rather than prototype tasks. Run `node tools/ore-progression/test-harvest-and-ages.mjs`, `node tools/bronze-stamp-mill/test-contract.mjs`, then `./gradlew build` and mine Cassiterite/Quartz and the two Bronze machine controllers in Minecraft before merging.
