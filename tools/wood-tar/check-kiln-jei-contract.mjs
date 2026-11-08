// Static JEI integration assertions for optional Wood Tar fuel.
// This checks source wiring and lang/recipe identity, not Forge compilation.
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import {fileURLToPath} from "node:url";

const repo=path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const read=p=>fs.readFileSync(path.join(repo,p),"utf8");
const categoryPath="src/main/java/com/magneticraft2/compatibility/JEI/";
const plugin=read(categoryPath+"MagneticraftJeiPlugin.java");
const kilnRecipe=read(categoryPath+"MultiblockJeiRecipe.java");
const processing=read(categoryPath+"MultiblockProcessingJeiCategory.java");
const fuel=read(categoryPath+"KilnLiquidFuelJeiCategory.java");
const kiln=read("src/main/java/com/magneticraft2/common/blockentity/stage/bronze/RefractoryKilnBlockEntity.java");
const lang=JSON.parse(read("src/main/resources/assets/magneticraft2/lang/en_us.json"));
assert.match(kiln,/WOOD_TAR_IGNITION_MB = 50/);
assert.match(kiln,/WOOD_TAR_BURN_TICKS = 80/);
assert.match(kilnRecipe,/REFRACTORY_KILN\s*=\s*new ResourceLocation\("magneticraft2", "refractory_kiln"\)/);

assert.match(plugin,/new KilnLiquidFuelJeiCategory\(/);
assert.match(plugin,/KilnLiquidFuelJeiCategory\.TYPE,/);
assert.match(plugin,/RefractoryKilnBlockEntity\.WOOD_TAR_IGNITION_MB/);
assert.match(plugin,/RefractoryKilnBlockEntity\.WOOD_TAR_BURN_TICKS/);
assert.match(plugin,/new FluidStack\(FluidRegistry\.WOOD_TAR\.get\(\),/);
assert.match(plugin,/registerCatalyst\(registration, MultiblockJeiRecipe\.REFRACTORY_KILN\)/);
assert.match(plugin,/registration\.addRecipeCatalyst\(\s*new ItemStack\(BlockRegistry\.REFRACTORY_KILN\.get\(\)\),\s*KilnLiquidFuelJeiCategory\.TYPE/s);

assert.match(fuel,/implements IRecipeCategory<KilnLiquidFuelJeiCategory\.FuelRecipe>/);
assert.match(fuel,/RecipeIngredientRole\.INPUT, 10, 22/);
assert.match(fuel,/\.addFluidStack\(recipe\.fluid\(\)\.getFluid\(\), recipe\.fluid\(\)\.getAmount\(\)\)/);
assert.match(fuel,/RecipeIngredientRole\.CATALYST/);
assert.doesNotMatch(fuel,/RecipeIngredientRole\.OUTPUT/);
assert.match(fuel,/refractory_kiln_wood_tar_fuel/);
assert.match(fuel,/kiln_tar_layout_tooltip/);

assert.match(processing,/if \(MultiblockJeiRecipe\.REFRACTORY_KILN\.equals\(recipe\.getMachine\(\)\)\)/);
assert.match(processing,/jei\.magneticraft2\.kiln_tar_hint/);
assert.match(processing,/jei\.magneticraft2\.kiln_tar_rate/);
assert.match(processing,/RefractoryKilnBlockEntity\.WOOD_TAR_IGNITION_MB/);
assert.match(processing,/RefractoryKilnBlockEntity\.WOOD_TAR_BURN_TICKS/);

for(const key of ["kiln_liquid_fuel","kiln_tar_hint","kiln_tar_rate",
                  "kiln_fuel_amount","kiln_fuel_ticks",
                  "kiln_tar_layout_short","kiln_tar_layout_tooltip"])
  assert.ok(lang["jei.magneticraft2."+key]);
assert.equal(lang["jei.magneticraft2.kiln_tar_rate"],"%s mB = %s burn ticks");
console.log("PASS: Wood Tar fluid JEI uses, kiln catalyst, optional-fuel hints and live burn constants");
