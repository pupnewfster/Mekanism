package mekanism.common.tile.factory;

import java.util.function.LongSupplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.api.IContentsListener;
import mekanism.api.Upgrade;
import mekanism.api.annotations.NonNull;
import mekanism.api.chemical.ChemicalTankBuilder;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.math.MathUtils;
import mekanism.api.providers.IBlockProvider;
import mekanism.api.recipes.ItemStackGasToItemStackRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.api.recipes.cache.ItemStackGasToItemStackCachedRecipe;
import mekanism.api.recipes.inputs.ILongInputHandler;
import mekanism.api.recipes.inputs.InputHelper;
import mekanism.common.Mekanism;
import mekanism.common.capabilities.holder.chemical.ChemicalTankHelper;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.integration.computer.ComputerException;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerChemicalTankWrapper;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.slot.chemical.GasInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.recipe.lookup.IDoubleRecipeLookupHandler.ItemChemicalRecipeLookupHandler;
import mekanism.common.recipe.lookup.cache.InputRecipeCache.ItemChemical;
import mekanism.common.tile.interfaces.IHasDumpButton;
import mekanism.common.tile.prefab.TileEntityAdvancedElectricMachine;
import mekanism.common.upgrade.AdvancedMachineUpgradeData;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.InventoryUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.StatUtils;
import net.minecraft.item.ItemStack;

//Compressing, injecting, purifying
public class TileEntityItemStackGasToItemStackFactory extends TileEntityItemToItemFactory<ItemStackGasToItemStackRecipe> implements IHasDumpButton,
      ItemChemicalRecipeLookupHandler<Gas, GasStack, ItemStackGasToItemStackRecipe> {

    private final ILongInputHandler<@NonNull GasStack> gasInputHandler;

    private double secondaryEnergyPerTickMultiplier = 1;
    @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getChemicalItem")
    private GasInventorySlot extraSlot;
    @WrappingComputerMethod(wrapper = ComputerChemicalTankWrapper.class, methodNames = {"getChemical", "getChemicalCapacity", "getChemicalNeeded",
                                                                                        "getChemicalFilledPercentage"})
    private IGasTank gasTank;

    public TileEntityItemStackGasToItemStackFactory(IBlockProvider blockProvider) {
        super(blockProvider);
        gasInputHandler = InputHelper.getInputHandler(gasTank);
        configComponent.addSupported(TransmissionType.GAS);
        configComponent.setupInputConfig(TransmissionType.GAS, gasTank);
    }

    @Nonnull
    @Override
    public IChemicalTankHolder<Gas, GasStack, IGasTank> getInitialGasTanks() {
        ChemicalTankHelper<Gas, GasStack, IGasTank> builder = ChemicalTankHelper.forSideGasWithConfig(this::getDirection, this::getConfig);
        //If the tank's contents change make sure to call our extended content listener that also marks sorting as being needed
        // as maybe the valid recipes have changed and we need to sort again and have all recipes know they may need to be rechecked
        // if they are not still valid
        builder.addTank(gasTank = ChemicalTankBuilder.GAS.input(TileEntityAdvancedElectricMachine.MAX_GAS * tier.processes, this::containsRecipeB,
              this::onContentsChangedUpdateSortingAndCache));
        return builder.build();
    }

    @Override
    protected void addSlots(InventorySlotHelper builder, IContentsListener updateSortingListener) {
        super.addSlots(builder, updateSortingListener);
        //Note: We care about the gas tank not the slot when it comes to recipes and updating sorting
        builder.addSlot(extraSlot = GasInventorySlot.fillOrConvert(gasTank, this::getLevel, this, 7, 57));
    }

    public IGasTank getGasTank() {
        return gasTank;
    }

    @Nullable
    @Override
    protected GasInventorySlot getExtraSlot() {
        return extraSlot;
    }

    @Override
    public boolean isValidInputItem(@Nonnull ItemStack stack) {
        return containsRecipeA(stack);
    }

    @Override
    protected int getNeededInput(ItemStackGasToItemStackRecipe recipe, ItemStack inputStack) {
        return MathUtils.clampToInt(recipe.getItemInput().getNeededAmount(inputStack));
    }

    @Override
    protected boolean isCachedRecipeValid(@Nullable CachedRecipe<ItemStackGasToItemStackRecipe> cached, @Nonnull ItemStack stack) {
        if (cached != null) {
            ItemStackGasToItemStackRecipe cachedRecipe = cached.getRecipe();
            return cachedRecipe.getItemInput().testType(stack) && (gasTank.isEmpty() || cachedRecipe.getChemicalInput().testType(gasTank.getType()));
        }
        return false;
    }

    @Override
    protected ItemStackGasToItemStackRecipe findRecipe(int process, @Nonnull ItemStack fallbackInput, @Nonnull IInventorySlot outputSlot,
          @Nullable IInventorySlot secondaryOutputSlot) {
        GasStack stored = gasTank.getStack();
        ItemStack output = outputSlot.getStack();
        //TODO: Give it something that is not empty when we don't have a stored gas stack for getting the output?
        return getRecipeType().getInputCache().findTypeBasedRecipe(level, fallbackInput, stored,
              recipe -> InventoryUtils.areItemsStackable(recipe.getOutput(fallbackInput, stored), output));
    }

    @Override
    protected void handleSecondaryFuel() {
        extraSlot.fillTankOrConvert();
    }

    @Nonnull
    @Override
    public MekanismRecipeType<ItemStackGasToItemStackRecipe, ItemChemical<Gas, GasStack, ItemStackGasToItemStackRecipe>> getRecipeType() {
        switch (type) {
            case INJECTING:
                return MekanismRecipeType.INJECTING;
            case PURIFYING:
                return MekanismRecipeType.PURIFYING;
            case COMPRESSING:
            default:
                //TODO: Make it so that it throws an error if it is not one of the three types
                return MekanismRecipeType.COMPRESSING;
        }
    }

    @Nullable
    @Override
    public ItemStackGasToItemStackRecipe getRecipe(int cacheIndex) {
        return findFirstRecipe(inputHandlers[cacheIndex], gasInputHandler);
    }

    @Nonnull
    @Override
    public CachedRecipe<ItemStackGasToItemStackRecipe> createNewCachedRecipe(@Nonnull ItemStackGasToItemStackRecipe recipe, int cacheIndex) {
        LongSupplier secondaryEnergyUsage;
        if (getSupportedUpgrade().contains(Upgrade.GAS)) {
            secondaryEnergyUsage = () -> StatUtils.inversePoisson(secondaryEnergyPerTickMultiplier);
        } else {
            secondaryEnergyUsage = () -> MathUtils.clampToLong(Math.ceil(secondaryEnergyPerTickMultiplier));
        }
        return new ItemStackGasToItemStackCachedRecipe<>(recipe, inputHandlers[cacheIndex], gasInputHandler, secondaryEnergyUsage, outputHandlers[cacheIndex])
              .setCanHolderFunction(() -> MekanismUtils.canFunction(this))
              .setActive(active -> setActiveState(active, cacheIndex))
              .setEnergyRequirements(energyContainer::getEnergyPerTick, energyContainer)
              .setRequiredTicks(this::getTicksRequired)
              .setOnFinish(() -> markDirty(false))
              .setOperatingTicksChanged(operatingTicks -> progress[cacheIndex] = operatingTicks);
    }

    @Override
    public boolean hasSecondaryResourceBar() {
        return true;
    }

    @Override
    public void recalculateUpgrades(Upgrade upgrade) {
        super.recalculateUpgrades(upgrade);
        if (upgrade == Upgrade.SPEED || upgrade == Upgrade.GAS && getSupportedUpgrade().contains(Upgrade.GAS)) {
            secondaryEnergyPerTickMultiplier = MekanismUtils.getGasPerTickMeanMultiplier(this);
        }
    }

    @Override
    public void parseUpgradeData(@Nonnull IUpgradeData upgradeData) {
        if (upgradeData instanceof AdvancedMachineUpgradeData) {
            //Generic factory upgrade data handling
            super.parseUpgradeData(upgradeData);
            AdvancedMachineUpgradeData data = (AdvancedMachineUpgradeData) upgradeData;
            //Copy the contents using NBT so that if it is not actually valid due to a reload we don't crash
            gasTank.deserializeNBT(data.stored.serializeNBT());
            extraSlot.deserializeNBT(data.gasSlot.serializeNBT());
        } else {
            Mekanism.logger.warn("Unhandled upgrade data.", new Throwable());
        }
    }

    @Nonnull
    @Override
    public AdvancedMachineUpgradeData getUpgradeData() {
        return new AdvancedMachineUpgradeData(redstone, getControlType(), getEnergyContainer(), progress, gasTank, extraSlot, energySlot, inputSlots, outputSlots,
              isSorting(), getComponents());
    }

    @Override
    public void dump() {
        gasTank.setEmpty();
    }

    //Methods relating to IComputerTile
    @ComputerMethod
    private void dumpChemical() throws ComputerException {
        validateSecurityIsPublic();
        dump();
    }
    //End methods IComputerTile
}