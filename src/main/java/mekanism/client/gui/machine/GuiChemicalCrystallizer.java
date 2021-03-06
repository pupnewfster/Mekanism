package mekanism.client.gui.machine;

import com.mojang.blaze3d.matrix.MatrixStack;
import java.lang.ref.WeakReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.api.chemical.merged.BoxedChemicalStack;
import mekanism.api.chemical.merged.MergedChemicalTank.Current;
import mekanism.api.recipes.ChemicalCrystallizerRecipe;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.custom.GuiCrystallizerScreen;
import mekanism.client.gui.element.custom.GuiCrystallizerScreen.IOreInfo;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiMergedChemicalTankGauge;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.tile.machine.TileEntityChemicalCrystallizer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public class GuiChemicalCrystallizer extends GuiConfigurableTile<TileEntityChemicalCrystallizer, MekanismTileContainer<TileEntityChemicalCrystallizer>> {

    public GuiChemicalCrystallizer(MekanismTileContainer<TileEntityChemicalCrystallizer> container, PlayerInventory inv, ITextComponent title) {
        super(container, inv, title);
        dynamicSlots = true;
        titleLabelY = 4;
    }

    @Override
    public void init() {
        super.init();
        addButton(new GuiCrystallizerScreen(this, 31, 13, new OreInfo()));
        addButton(new GuiVerticalPowerBar(this, tile.getEnergyContainer(), 157, 23));
        addButton(new GuiEnergyTab(tile.getEnergyContainer(), tile::getActive, this));
        addButton(new GuiMergedChemicalTankGauge<>(() -> tile.inputTank, () -> tile, GaugeType.STANDARD, this, 7, 4));
        addButton(new GuiProgress(tile::getScaledProgress, ProgressType.LARGE_RIGHT, this, 53, 61).jeiCategory(tile));
    }

    @Override
    protected void drawForegroundText(@Nonnull MatrixStack matrix, int mouseX, int mouseY) {
        renderTitleText(matrix);
        super.drawForegroundText(matrix, mouseX, mouseY);
    }

    private class OreInfo implements IOreInfo {

        private WeakReference<ChemicalCrystallizerRecipe> cachedRecipe;

        @Nonnull
        @Override
        public BoxedChemicalStack getInputChemical() {
            Current current = tile.inputTank.getCurrent();
            return current == Current.EMPTY ? BoxedChemicalStack.EMPTY : BoxedChemicalStack.box(tile.inputTank.getTankFromCurrent(current).getStack());
        }

        @Nullable
        @Override
        public ChemicalCrystallizerRecipe getRecipe() {
            BoxedChemicalStack input = getInputChemical();
            if (input.isEmpty()) {
                return null;
            }
            ChemicalCrystallizerRecipe recipe;
            if (cachedRecipe == null) {
                recipe = getRecipeAndCache();
            } else {
                recipe = cachedRecipe.get();
                if (recipe == null || !recipe.testType(input)) {
                    recipe = getRecipeAndCache();
                }
            }
            return recipe;
        }

        private ChemicalCrystallizerRecipe getRecipeAndCache() {
            ChemicalCrystallizerRecipe recipe = tile.getRecipe(0);
            if (recipe == null) {
                cachedRecipe = null;
            } else {
                cachedRecipe = new WeakReference<>(recipe);
            }
            return recipe;
        }
    }
}