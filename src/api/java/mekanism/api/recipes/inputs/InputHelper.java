package mekanism.api.recipes.inputs;

import javax.annotation.Nonnull;
import mekanism.api.annotations.NonNull;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTank;
import mekanism.api.infuse.InfusionStack;
import mekanism.api.infuse.InfusionTank;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;

public class InputHelper {

    //TODO: 1.14, evaluate using the IItemHandler variant instead
    public static IInputHandler<@NonNull ItemStack> getInputHandler(@Nonnull NonNullList<ItemStack> inventory, int slot) {
        return new IInputHandler<@NonNull ItemStack>() {

            @Override
            public @NonNull ItemStack getInput() {
                return inventory.get(slot);
            }

            @Override
            public @NonNull ItemStack getRecipeInput(InputIngredient<@NonNull ItemStack> recipeIngredient) {
                ItemStack input = getInput();
                if (input.isEmpty()) {
                    //All recipes currently require that we have an input. If we don't then return that we failed
                    return ItemStack.EMPTY;
                }
                return recipeIngredient.getMatchingInstance(input);
            }

            @Override
            public void use(@NonNull ItemStack recipeInput, int operations) {
                if (!recipeInput.isEmpty()) {
                    //TODO: Should this be done in some other way than shrink, such as via an IItemHandler, 1.14
                    //TODO: If this would make the stack empty, we should just set the inventory slot to empty instead of using shrink
                    getInput().shrink(recipeInput.getCount() * operations);
                }
            }

            @Override
            public int operationsCanSupport(InputIngredient<@NonNull ItemStack> recipeIngredient, int currentMax) {
                if (currentMax == 0) {
                    //Short circuit that if we already can't perform any operations, just return
                    return 0;
                }
                ItemStack recipeInput = getRecipeInput(recipeIngredient);
                if (recipeInput.isEmpty()) {
                    //If the input is empty that means there is no ingredient that matches
                    return 0;
                }
                return Math.min(getInput().getCount() / recipeInput.getCount(), currentMax);
            }
        };
    }

    public static IInputHandler<@NonNull ItemStack> getInputHandler(@Nonnull IItemHandler inventory, int slot) {
        return new IInputHandler<@NonNull ItemStack>() {

            @Override
            public @NonNull ItemStack getInput() {
                return inventory.getStackInSlot(slot);
            }

            @Override
            public @NonNull ItemStack getRecipeInput(InputIngredient<@NonNull ItemStack> recipeIngredient) {
                ItemStack input = getInput();
                if (input.isEmpty()) {
                    //All recipes currently require that we have an input. If we don't then return that we failed
                    return ItemStack.EMPTY;
                }
                return recipeIngredient.getMatchingInstance(input);
            }

            @Override
            public void use(@NonNull ItemStack recipeInput, int operations) {
                if (!recipeInput.isEmpty()) {
                    //TODO: Should we check if it failed
                    inventory.extractItem(slot, recipeInput.getCount() * operations, false);
                }
            }

            @Override
            public int operationsCanSupport(InputIngredient<@NonNull ItemStack> recipeIngredient, int currentMax) {
                if (currentMax == 0) {
                    //Short circuit that if we already can't perform any operations, just return
                    return 0;
                }
                ItemStack recipeInput = getRecipeInput(recipeIngredient);
                if (recipeInput.isEmpty()) {
                    //If the input is empty that means there is no ingredient that matches
                    return 0;
                }
                return Math.min(getInput().getCount() / recipeInput.getCount(), currentMax);
            }
        };
    }

    public static IInputHandler<@NonNull GasStack> getInputHandler(@Nonnull GasTank gasTank) {
        return new IInputHandler<@NonNull GasStack>() {

            @Override
            public @NonNull GasStack getInput() {
                return gasTank.getGas();
            }

            @Override
            public @NonNull GasStack getRecipeInput(InputIngredient<@NonNull GasStack> recipeIngredient) {
                GasStack input = getInput();
                //TODO: Make this be an is empty check, and return empty instead of null
                if (input.isEmpty()) {
                    //All recipes currently require that we have an input. If we don't then return that we failed
                    return GasStack.EMPTY;
                }
                return recipeIngredient.getMatchingInstance(input);
            }

            @Override
            public void use(@NonNull GasStack recipeInput, int operations) {
                if (recipeInput.isEmpty()) {
                    //Something went wrong, this if should never really be true if we got to finishProcessing
                    return;
                }
                GasStack inputGas = getInput();
                if (inputGas.isEmpty()) {
                    //Something went wrong, this if should never really be true if we got to finishProcessing
                    return;
                }
                gasTank.draw(recipeInput.getAmount() * operations, true);
            }

            @Override
            public int operationsCanSupport(InputIngredient<@NonNull GasStack> recipeIngredient, int currentMax) {
                if (currentMax == 0) {
                    //Short circuit that if we already can't perform any operations, just return
                    return 0;
                }
                GasStack recipeInput = getRecipeInput(recipeIngredient);
                //Test to make sure we can even perform a single operation. This is akin to !recipe.test(inputGas)
                if (recipeInput.isEmpty()) {
                    //If the input is empty that means there is no ingredient that matches
                    return 0;
                }
                return Math.min(getInput().getAmount() / recipeInput.getAmount(), currentMax);
            }
        };
    }

    //TODO: Generify this up to either IFluidTank or IFluidHandler?
    public static IInputHandler<@NonNull FluidStack> getInputHandler(@Nonnull FluidTank fluidTank) {
        return new IInputHandler<@NonNull FluidStack>() {

            @Override
            public @NonNull FluidStack getInput() {
                return fluidTank.getFluid();
            }

            @Override
            public @NonNull FluidStack getRecipeInput(InputIngredient<@NonNull FluidStack> recipeIngredient) {
                FluidStack input = getInput();
                if (input.isEmpty()) {
                    //All recipes currently require that we have an input. If we don't then return that we failed
                    return FluidStack.EMPTY;
                }
                return recipeIngredient.getMatchingInstance(input);
            }

            @Override
            public void use(@NonNull FluidStack recipeInput, int operations) {
                if (recipeInput.isEmpty()) {
                    //Something went wrong, this if should never really be true if we got to finishProcessing
                    return;
                }
                FluidStack inputFluid = getInput();
                if (inputFluid.isEmpty()) {
                    //Something went wrong, this if should never really be true if we got to finishProcessing
                    return;
                }
                //TODO: Pass this as a fluidstack?
                fluidTank.drain(recipeInput.getAmount() * operations, FluidAction.EXECUTE);
            }

            @Override
            public int operationsCanSupport(InputIngredient<@NonNull FluidStack> recipeIngredient, int currentMax) {
                if (currentMax == 0) {
                    //Short circuit that if we already can't perform any operations, just return
                    return 0;
                }
                FluidStack recipeInput = getRecipeInput(recipeIngredient);
                //Test to make sure we can even perform a single operation. This is akin to !recipe.test(inputGas)
                if (recipeInput.isEmpty()) {
                    //If the input is empty that means there is no ingredient that matches
                    return 0;
                }
                //TODO: Simulate the drain?
                return Math.min(getInput().getAmount() / recipeInput.getAmount(), currentMax);
            }
        };
    }

    public static IInputHandler<@NonNull InfusionStack> getInputHandler(@Nonnull InfusionTank infuseStorage) {
        return new IInputHandler<@NonNull InfusionStack>() {

            @Override
            public @NonNull InfusionStack getInput() {
                if (infuseStorage.isEmpty()) {
                    return InfusionStack.EMPTY;
                }
                return new InfusionStack(infuseStorage.getType(), infuseStorage.getAmount());
            }

            @Override
            public @NonNull InfusionStack getRecipeInput(InputIngredient<@NonNull InfusionStack> recipeIngredient) {
                InfusionStack input = getInput();
                //TODO: Make this be an is empty check, and return empty instead of null
                if (input.isEmpty()) {
                    //All recipes currently require that we have an input. If we don't then return that we failed
                    return InfusionStack.EMPTY;
                }
                return recipeIngredient.getMatchingInstance(input);
            }

            @Override
            public void use(@NonNull InfusionStack recipeInput, int operations) {
                if (infuseStorage.isEmpty()) {
                    //Something went wrong, this if should never really be true if we got to finishProcessing
                    return;
                }
                infuseStorage.subtract(new InfusionStack(recipeInput, recipeInput.getAmount() * operations));
            }

            @Override
            public int operationsCanSupport(InputIngredient<@NonNull InfusionStack> recipeIngredient, int currentMax) {
                if (currentMax == 0 || infuseStorage.isEmpty()) {
                    //Short circuit that if we already can't perform any operations, just return
                    return 0;
                }
                InfusionStack recipeInput = getRecipeInput(recipeIngredient);
                //Test to make sure we can even perform a single operation. This is akin to !recipe.test(inputGas)
                if (recipeInput.isEmpty()) {
                    //If the input is empty that means there is no ingredient that matches
                    return 0;
                }
                return Math.min(getInput().getAmount() / recipeInput.getAmount(), currentMax);
            }
        };
    }
}