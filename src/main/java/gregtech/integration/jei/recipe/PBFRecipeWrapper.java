package gregtech.integration.jei.recipe;

import codechicken.lib.util.ItemNBTUtils;
import com.google.common.collect.ImmutableList;
import gnu.trove.map.TObjectIntMap;
import gregtech.api.recipes.CountableIngredient;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.recipes.RecipeMaps;
import gregtech.api.recipes.recipes.PrimitiveBlastFurnaceRecipe;
import gregtech.api.unification.OreDictUnifier;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PBFRecipeWrapper implements IRecipeWrapper {

    private final PrimitiveBlastFurnaceRecipe recipe;

    public PBFRecipeWrapper(PrimitiveBlastFurnaceRecipe recipe) {
        this.recipe = recipe;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        CountableIngredient ingredient = recipe.getInput();
        List<List<ItemStack>> matchingInputs = new ArrayList<>(1);
        List<ItemStack> ingredientValues = Arrays.stream(ingredient.getIngredient().getMatchingStacks())
            .map(ItemStack::copy)
            .sorted(OreDictUnifier.getItemStackComparator())
            .collect(Collectors.toList());
        ingredientValues.forEach(stack -> {
            if (ingredient.getCount() == 0) {
                ItemNBTUtils.setBoolean(stack, "not_consumed", true);
                stack.setCount(1);
            } else stack.setCount(ingredient.getCount());
        });
        matchingInputs.add(ingredientValues);
        ingredients.setInputLists(ItemStack.class, matchingInputs);

        List<ItemStack> recipeOutputs = ImmutableList.of(recipe.getOutput().copy());

        ingredients.setOutputs(ItemStack.class, recipeOutputs);

    }

    public void addTooltip(int slotIndex, boolean input, Object ingredient, List<String> tooltip) {
        NBTTagCompound tagCompound;
        if (ingredient instanceof ItemStack) {
            tagCompound = ((ItemStack) ingredient).getTagCompound();
        } else if (ingredient instanceof FluidStack) {
            tagCompound = ((FluidStack) ingredient).tag;
        } else {
            throw new IllegalArgumentException("Unknown ingredient type: " + ingredient.getClass());
        }
        if (tagCompound != null && tagCompound.hasKey("chance")) {
            String chanceString = Recipe.formatChanceValue(tagCompound.getInteger("chance"));
            tooltip.add(I18n.format("gregtech.recipe.chance", chanceString));
        } else if (tagCompound != null && tagCompound.hasKey("not_consumed")) {
            tooltip.add(I18n.format("gregtech.recipe.not_consumed"));
        }
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        minecraft.fontRenderer.drawString(I18n.format("gregtech.recipe.duration", recipe.getDuration() / 20f), 0, 70, 0x111111);
        minecraft.fontRenderer.drawString(I18n.format("gregtech.recipe.primitive_blast_fuel", recipe.getFuelAmount()), 0, 80, 0x111111);
    }
}
