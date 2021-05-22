import mods.mekanism.api.ingredient.ItemStackIngredient;
import mods.mekanism.api.ingredient.ChemicalStackIngredient.GasStackIngredient;

//Adds a Dissolution Recipe that uses 100 mB of Sulfuric Acid (1 mB per tick) to convert Salt into 10 mB of Hydrogen Chloride.

// <recipetype:mekanism:dissolution>.addRecipe(arg0 as string, arg1 as ItemStackIngredient, arg2 as GasStackIngredient, arg3 as ICrTChemicalStack)

<recipetype:mekanism:dissolution>.addRecipe("salt_to_hydrogen_chloride", ItemStackIngredient.from(<item:mekanism:salt>), GasStackIngredient.from(<gas:mekanism:sulfuric_acid>), <gas:mekanism:hydrogen_chloride> * 10);
//Alternate implementations of the above recipe are shown commented below. These implementations make use of implicit casting to allow easier calling:
// <recipetype:mekanism:dissolution>.addRecipe("salt_to_hydrogen_chloride", <item:mekanism:salt>, GasStackIngredient.from(<gas:mekanism:sulfuric_acid>), <gas:mekanism:hydrogen_chloride> * 10);
// <recipetype:mekanism:dissolution>.addRecipe("salt_to_hydrogen_chloride", ItemStackIngredient.from(<item:mekanism:salt>), <gas:mekanism:sulfuric_acid>, <gas:mekanism:hydrogen_chloride> * 10);
// <recipetype:mekanism:dissolution>.addRecipe("salt_to_hydrogen_chloride", <item:mekanism:salt>, <gas:mekanism:sulfuric_acid>, <gas:mekanism:hydrogen_chloride> * 10);


/*
 * Removes two Dissolution Recipes:
 * 1) The recipe for producing Hydrofluoric Acid from Fluorite.
 * 2) The recipe for producing Dirty Lead Slurry from Lead Ore.
*/

// <recipetype:mekanism:dissolution>.removeByName(name as string)

<recipetype:mekanism:dissolution>.removeByName("mekanism:processing/uranium/hydrofluoric_acid");
<recipetype:mekanism:dissolution>.removeByName("mekanism:processing/lead/slurry/dirty");