package mekanism.api.recipes.cache;

import javax.annotation.ParametersAreNonnullByDefault;
import mekanism.api.annotations.NonNull;
import mekanism.api.recipes.SawmillRecipe;
import mekanism.api.recipes.SawmillRecipe.ChanceOutput;
import mekanism.api.recipes.inputs.IInputHandler;
import mekanism.api.recipes.outputs.IOutputHandler;
import mekanism.api.annotations.FieldsAreNonnullByDefault;
import net.minecraft.item.ItemStack;

@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
public class SawmillCachedRecipe extends CachedRecipe<SawmillRecipe> {

    private final IOutputHandler<@NonNull ChanceOutput> outputHandler;
    private final IInputHandler<@NonNull ItemStack> inputHandler;

    public SawmillCachedRecipe(SawmillRecipe recipe, IInputHandler<@NonNull ItemStack> inputHandler, IOutputHandler<@NonNull ChanceOutput> outputHandler) {
        super(recipe);
        this.inputHandler = inputHandler;
        this.outputHandler = outputHandler;
    }

    @Override
    protected int getOperationsThisTick(int currentMax) {
        currentMax = super.getOperationsThisTick(currentMax);
        if (currentMax == 0) {
            //If our parent checks show we can't operate then return so
            return 0;
        }
        //TODO: This input getting, is only really needed for getting the output
        ItemStack recipeItem = inputHandler.getRecipeInput(recipe.getInput());
        //Test to make sure we can even perform a single operation. This is akin to !recipe.test(inputItem)
        if (recipeItem.isEmpty()) {
            return 0;
        }

        //Calculate the current max based on the input
        currentMax = inputHandler.operationsCanSupport(recipe.getInput(), currentMax);

        //Calculate the max based on the space in the output
        return outputHandler.operationsRoomFor(recipe.getOutput(recipeItem), currentMax);
    }

    @Override
    public boolean isInputValid() {
        return recipe.test(inputHandler.getInput());
    }

    @Override
    protected void finishProcessing(int operations) {
        //TODO: Cache this stuff from when getOperationsThisTick was called?
        ItemStack recipeItem = inputHandler.getRecipeInput(recipe.getInput());
        if (recipeItem.isEmpty()) {
            //Something went wrong, this if should never really be true if we got to finishProcessing
            return;
        }
        inputHandler.use(recipeItem, operations);
        outputHandler.handleOutput(recipe.getOutput(recipeItem), operations);
    }
}