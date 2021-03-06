package mekanism.client.jei.machine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.recipes.GasToGasRecipe;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.GuiDynamicHorizontalRateBar;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.gui.element.gauge.GuiGauge;
import mekanism.client.jei.BaseRecipeCategory;
import mekanism.client.jei.MekanismJEI;
import mekanism.common.MekanismLang;
import mekanism.common.config.MekanismConfig;
import mekanism.common.lib.Color;
import mekanism.common.lib.Color.ColorFunction;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.registries.MekanismItems;
import mekanism.common.util.text.EnergyDisplay;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.ingredient.IGuiIngredientGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import net.minecraft.util.text.ITextComponent;

public class SPSRecipeCategory extends BaseRecipeCategory<GasToGasRecipe> {

    private final GuiGauge<?> input;
    private final GuiGauge<?> output;

    public SPSRecipeCategory(IGuiHelper helper) {
        super(helper, MekanismBlocks.SPS_CASING.getRegistryName(), MekanismLang.SPS.translate(), 3, 12, 168, 74);
        icon = helper.createDrawableIngredient(MekanismItems.ANTIMATTER_PELLET.getItemStack());
        addElement(new GuiInnerScreen(this, 26, 13, 122, 60, () -> {
            List<ITextComponent> list = new ArrayList<>();
            list.add(MekanismLang.STATUS.translate(MekanismLang.ACTIVE));
            list.add(MekanismLang.SPS_ENERGY_INPUT.translate(EnergyDisplay.of(
                  MekanismConfig.general.spsEnergyPerInput.get().multiply(MekanismConfig.general.spsInputPerAntimatter.get()))));
            list.add(MekanismLang.PROCESS_RATE_MB.translate(1.0));
            return list;
        }));
        input = addElement(GuiGasGauge.getDummy(GaugeType.STANDARD, this, 6, 13));
        output = addElement(GuiGasGauge.getDummy(GaugeType.STANDARD, this, 150, 13));
        addElement(new GuiDynamicHorizontalRateBar(this, getBarProgressTimer(), 6, 75, 160,
              ColorFunction.scale(Color.rgbi(60, 45, 74), Color.rgbi(100, 30, 170))));
    }

    @Nonnull
    @Override
    public Class<? extends GasToGasRecipe> getRecipeClass() {
        return GasToGasRecipe.class;
    }

    @Override
    public void setIngredients(GasToGasRecipe recipe, IIngredients ingredients) {
        ingredients.setInputLists(MekanismJEI.TYPE_GAS, Collections.singletonList(recipe.getInput().getRepresentations()));
        ingredients.setOutputLists(MekanismJEI.TYPE_GAS, Collections.singletonList(recipe.getOutputDefinition()));
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, GasToGasRecipe recipe, @Nonnull IIngredients ingredients) {
        IGuiIngredientGroup<GasStack> gasStacks = recipeLayout.getIngredientsGroup(MekanismJEI.TYPE_GAS);
        initChemical(gasStacks, 0, true, input, recipe.getInput().getRepresentations());
        initChemical(gasStacks, 2, false, output, recipe.getOutputDefinition());
    }
}