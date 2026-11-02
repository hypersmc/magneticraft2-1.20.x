# Magneticraft2 — useful ore processing by age

**Status: design policy and source audit (2026-11-02), not new game mechanics.** The authoritative 31-family inventory and proposed processing ages are in [ore-processing-by-age.json](ore-processing-by-age.json). This revision is deliberately conservative: **no new ore blocks, crafting recipes, age-gates or world generation changes**.

## Governing rule

**Technology + useful consumer + progression fit are all required.** A mineral's presence in world generation never implies a useful processing recipe. The age below means *earliest candidate usefulness*, not automatically available content. `source_active` means a datapack/code route exists, not that full survival, multiplayer and regression testing have passed; `conditional` is a future candidate **without** a promised recipe; `deferred` has **no assigned processing age** until a specific use is justified. Every registered ore can still be mined using the existing rules.

**31 mineral families / 42 registered ore blocks** are covered, including Deepslate variants. `*_deepslate_ore` inherits its normal family's status. Preserve legacy `silicium` and `platinium` IDs and all existing saves.

## Stone, Copper and Bronze: deliberately minimal

| Age | Candidates with an actual role | Not required |
| --- | --- | --- |
| **Stone** | No new ore-specific input is required; vanilla clay and fuel already support current progression | Kaolinite and Lignite remain registered but **deferred** |
| **Copper** | Chalcocite, Cassiterite, Quartz — **existing source recipes stay unchanged** | No additional copper mineral merely for historical variety |
| **Bronze** | Magnetite **existing source path**; Hematite and Limonite **conditional alternatives** only with genuinely useful preparation and Bloomery recipes | Galena/Lead is **deferred**; no dead-end Lead Ingot |

**Bloomery evidence:** Magnetite can be crushed in the Stamp Mill, washed, sifted to concentrate, and reduced in the existing Bloomery recipe with **Calcite flux + charcoal + HEAT + AIR → Iron Bloom + Slag**. The user has formed and used the Bloomery. The complete survival ore chain, hot consolidation into usable Wrought Iron, build, and multiplayer regressions remain separate tests. Do not mark them complete. Wrought Iron is the Bronze-to-Iron milestone; existing vanilla iron recipes are not silently removed. Calcite is a **vanilla flux resource**, not one of the 31 custom ore families.

**Hematite and Limonite:** both can provide iron and therefore have a believable Bronze output, but recipes should only be introduced if the ore preparation differs meaningfully (for instance, conditioning a hydrated Limonite feed) or provides useful exploration variation. Do not require universal roasting or identical crushing→washing→sifting for every material. The optional Ore Breaker is not made mandatory.

## Full mineral decision register

Every line below is a **planning decision, not an implemented recipe** unless marked "Existing source route." Process details and yields must be verified with actual machine capabilities before implementation.

### Stone

No mandatory ore-specific processing; vanilla clay and ordinary fuels already cover the first technologies.

No custom mineral is required to unlock this age.

### Copper

Preserve proven Copper, Tin and Quartz recipes and material quality conventions.

| Mineral ID | Status | First useful product or purpose | Condition before a recipe is allowed |
| --- | --- | --- | --- |
| `chalcocite` | Existing source route | Existing copper concentrate / copper ingots | Preserve the Grinder, Washer, Sifter and Primitive Furnace 1/2/3-copper yield tiers; the Roasting Kiln is a separate optional improvement. |
| `cassiterite` | Existing source route | Existing tin concentrate / tin ingots | Preserve existing copper-to-bronze tin chain, including processing water and slag accounting. |
| `quartz` | Existing source route | Existing quartz product | Preserve existing crushing, washing and sifting; later silicon chemistry must not erase the current route. |

### Bronze

Magnetite is the K1 iron-bloom baseline; Hematite/Limonite require demonstrably distinct preparation and downstream Bloomery use.

| Mineral ID | Status | First useful product or purpose | Condition before a recipe is allowed |
| --- | --- | --- | --- |
| `magnetite` | Existing source route | Iron Bloom from the existing Bloomery | Existing Stamp Mill → Washer → Sifter → concentrate; Calcite flux, charcoal, HEAT and real AIR are mandatory. Wrought Iron consolidation is still missing. |
| `hematite` | Conditional only | Alternative iron-bloom feed | Approve an ore-specific concentration method and compare its yield and availability with Magnetite. Use the same Bloomery only when mechanically justified. |
| `limonite` | Conditional only | Alternative iron-bloom feed after drying/roasting when beneficial | Prove the hydrated iron-bearing feed benefits from existing drying or Ore Roasting Kiln, and provide a real Bloomery recipe. |

### Iron

Candidate carbon/lubrication, high-grade fuel and brass processing only if consumers exist.

| Mineral ID | Status | First useful product or purpose | Condition before a recipe is allowed |
| --- | --- | --- | --- |
| `graphite` | Conditional only | Dry lubricant or carbon/refractory ingredient | Only unlock with a named Iron-era bearing, hot-working, crucible or heat-resistant component that consumes it. |
| `anthracite` | Conditional only | Higher-performance industrial carbon fuel | Define a measurable difference in heat/cleanliness/duration from charcoal and coal in a machine that needs it. |
| `sphalerite` | Conditional only | Zinc feedstock for useful brass components | Add a proper zinc extraction route, identify brass consumers and prevent ordinary furnace ore-to-brass shortcuts. |

### Steel

Candidate alloying and industrial abrasion, gated by actual Steel production.

| Mineral ID | Status | First useful product or purpose | Condition before a recipe is allowed |
| --- | --- | --- | --- |
| `manganese` | Conditional only | Manganese-bearing alloying feed | Require an actual alloy/furnace recipe with improved or distinct steel-component use. |
| `garnierite` | Conditional only | Nickel-bearing alloy feed | Only use after nickel metallurgy and a named alloy or corrosion-resistant component are designed. |
| `kimberlite` | Conditional only | Industrial diamond recovery for durable cutting/abrasive tools | Define recovery yields and a genuinely wear-resistant cutting/grinding tool; never convert all Kimberlite directly into diamonds. |

### Steam

Sulfur chemistry only if a named industrial chemical consumer exists.

| Mineral ID | Status | First useful product or purpose | Condition before a recipe is allowed |
| --- | --- | --- | --- |
| `sulfur` | Conditional only | Sulfur chemistry used by industry | Require a specific Steam-era chemical product and machine consumer, such as controlled acid processing or elastomer treatment; not a bare Furnace recipe. |

### Gasoline

Petroleum extraction/refining is not a mandatory mineral-ore unlock.

No custom mineral is required to unlock this age.

### Electric Generator

Electrical contacts, specialty metal reduction and aluminum electrolysis require functioning electric technology.

| Mineral ID | Status | First useful product or purpose | Condition before a recipe is allowed |
| --- | --- | --- | --- |
| `silver` | Conditional only | Electrical contacts or specialist conductive parts | Only unlock with an actual electrical component; preserve vanilla materials compatibility and do not force Silver into basic iron forging. |
| `chromite` | Conditional only | Chromium-bearing corrosion/heat-resistant alloy feed | Define a sufficiently advanced reduction process and actual corrosion-resistant component; revisit Steel placement only if its furnaces legitimately support it. |
| `bauxite` | Conditional only | Aluminum feedstock | Require real electrically powered aluminum extraction and meaningful aluminum components. |
| `cryolite` | Conditional only | Aluminum electrolysis bath constituent | Link explicitly to Bauxite refining; allow a carefully balanced non-ore supply alternative if necessary to prevent mining bottlenecks. |
| `wolframite` | Conditional only | Tungsten feed for cutting tools or high-temperature components | Require appropriate advanced reduction and useful wear-resistant, heat-resistant tooling. |
| `tantalite` | Conditional only | Tantalum for advanced electrical components | Require an electronics/capacitor consumer and a realistic specialty-metal process. |
| `cobaltite` | Conditional only | Cobalt-bearing specialty-alloy or catalyst feed | Require a named advanced alloy, component or chemical consumer; avoid direct ore-to-pure-cobalt recipes. |
| `platinium` | Conditional only | Platinum-group catalyst or electrical contact | Require a named catalyst or specialist component; retain existing registry spelling for compatibility. |

### Nuclear

Real uranium fuel cycle rather than a symbolic ore smelting recipe.

| Mineral ID | Status | First useful product or purpose | Condition before a recipe is allowed |
| --- | --- | --- | --- |
| `uraninite` | Conditional only | Uranium-bearing feed for nuclear fuel fabrication | Require a dedicated fuel cycle, radiation containment and actual reactor consumption; no furnace-to-fuel-rod shortcut. |

### Deferred — deliberately not assigned a processing age

These blocks remain registered and mineable, but **none may receive a new processing recipe solely to fill an age**. A future proposal must name the machine, intermediate chemistry, useful output, and downstream consumer before moving the ore into an age.

| Mineral ID | Status | Unresolved purpose | Reason to wait |
| --- | --- | --- | --- |
| `kaolinite` | Deferred | Revisit if kaolin ceramics add a meaningful upgrade over existing clay/sand and fired refractory production. | A Stone-age ceramic recipe would currently duplicate established inputs without adding gameplay. |
| `lignite` | Deferred | Revisit only if low-grade coal provides an actual resource/combustion tradeoff; never replace mandatory charcoal in the Bloomery. | Stone/Copper already have usable fuels; a redundant low-efficiency fuel is not necessary. |
| `galena` | Deferred | Revisit in later metalworking or chemical/electric production when lead or associated silver has a named sink. | A plausible Bronze extraction process is not sufficient reason to add dead-end Lead Ingots. |
| `saltpeter` | Deferred | Revisit alongside a later fertilizer or controlled industrial chemical chain. | Do not introduce a chemical ore because Steam Age has a chemistry label. |
| `apatite` | Deferred | Revisit when agriculture, fertilizer or specialty chemical production creates meaningful demand. | An early phosphate concentrate would currently be a dead-end item. |
| `cinnabar` | Deferred | Only reconsider with a distinct, justified process that accounts for hazardous mercury chemistry; do not make it a mandatory age gate. | No need to add Mercury extraction just because the ore exists. |
| `silicium` | Deferred | Design Silicon production from the existing Quartz/silica path first; decide what silicium_ore represents without renaming saved-world IDs. | Native elemental Silicon ore is not a credible natural source; existing Quartz already supplies silica. |
| `titanite` | Deferred | Only reconsider if an advanced titanium route has credible chemistry and useful outputs; do not treat Titanite as simple raw titanium. | Titanite is not an easy high-grade titanium ore and should not be mandatory solely to populate Electric Age. |
| `osmiridium` | Deferred | Define a distinct endgame use and appropriate processing before assigning it to Nuclear or an earlier age. | Its legacy endgame ore status is not in itself a reason for a recipe. |

## Changes relative to the older provisional matrix

- **Moved out of Stone:** Kaolinite and Lignite have no mandatory Stone/Copper/Bronze consumer. They are discoverable but unscheduled for useful processing.
- **Moved out of Bronze:** Galena has no established Lead use; no Bronze Lead production is promised.
- **Moved out of Iron:** Silver is provisionally relevant to **Electric Generator** contacts rather than a mandatory Iron recipe.
- **Moved out of Steel:** Chromite is provisionally relevant to **Electric Generator** alloy processing unless a supported earlier furnace and consumer are designed.
- **Moved out of Steam:** Saltpeter, Apatite and Cinnabar are unscheduled until chemistry and consumers are decided.
- **Moved out of Electric Generator:** Legacy Silicium and Titanite are unscheduled pending a defensible feedstock/use definition. Quartz remains the established silica source.
- **Moved out of Nuclear:** Osmiridium is unscheduled without an endgame component that actually requires it.
- The status of **Copper's three sources** and Bronze's **Magnetite source** is recorded separately from unimplemented ore candidates, instead of labelling entire ages completed.

These are **roadmap proposals**, not automatic gameplay locks. Review them as consumers become real. Do not rename registered blocks or convert existing inventories.

## Material and infrastructure constraints

- **Keep Stone/Copper working:** Preserve Chalcocite's 1/2/3 yield, Cassiterite-to-Tin, Quartz processing, and the approved 3 Copper + 1 Tin Bronze Charge / 4 Bronze Ingots sequence. Preserve the distinct vanilla versus low-quality mod Copper concept without inventing a new quality system.
- **Use existing machines first:** Grinder or Stamp Mill, Washer, Sifter, optional Ore Classifier, Roasting Kiln, Refractory Kiln and Bloomery have separate purposes. Air, water, fuel, HEAT and mechanical-power costs cannot be waived to make a generic recipe.
- **Avoid mass/quality exploits:** Track slags, dirty water, tailings, fuel and useful metal quantities. Additional separation should not create free mass. Wear/quality propagation is a later integrated design task, not a reason to add every ore now.
- **Keep world compatibility:** Do not change loot tables, tags, worldgen distribution, the 42 block IDs or existing item IDs in this roadmap pass. Mining is not equivalent to useful extraction.
- **No fake early-game gating:** Revisit harvest tiers only after the first Wrought Iron route works end to end without softlocking players.

## When to implement the next ore

A future ore-specific PR should identify: **(1)** earliest justified age, **(2)** input mineral and valid existing machine, **(3)** actual ore preparation/thermal/chemical steps, **(4)** required fuels/fluids/heat/power, **(5)** useful product with at least one downstream recipe, **(6)** slag/tailings/quality handling, and **(7)** JEI/Patchouli, datapack, save/reload and in-game tests. If these aren't identified, leave the ore mineable but unprocessed.

This review does **not** approve new Malachite, Limestone or Siderite ore blocks. A practical source of the existing Calcite flux should be evaluated separately from adding world deposits.
