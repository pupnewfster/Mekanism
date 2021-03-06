package mekanism.common.tile.machine;

import javax.annotation.Nonnull;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.recipe.lookup.cache.InputRecipeCache.SingleItem;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.prefab.TileEntityElectricMachine;

public class TileEntityCrusher extends TileEntityElectricMachine {

    public TileEntityCrusher() {
        super(MekanismBlocks.CRUSHER, 200);
    }

    @Nonnull
    @Override
    public MekanismRecipeType<ItemStackToItemStackRecipe, SingleItem<ItemStackToItemStackRecipe>> getRecipeType() {
        return MekanismRecipeType.CRUSHING;
    }
}