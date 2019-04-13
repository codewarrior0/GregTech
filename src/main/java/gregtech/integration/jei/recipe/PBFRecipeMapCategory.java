package gregtech.integration.jei.recipe;

import gregtech.api.GTValues;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.gui.BlankUIHolder;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.SlotWidget;
import gregtech.api.gui.widgets.TankWidget;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.recipes.RecipeMaps;
import gregtech.api.recipes.recipes.PrimitiveBlastFurnaceRecipe;
import gregtech.common.metatileentities.MetaTileEntities;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;

import static gregtech.common.metatileentities.MetaTileEntities.PRIMITIVE_BLAST_FURNACE;

public class PBFRecipeMapCategory implements IRecipeCategory<PBFRecipeWrapper> {

    private final ModularUI modularUI;
    private ItemStackHandler importItems, exportItems;
    private final IDrawable backgroundDrawable;

    public PBFRecipeMapCategory(IGuiHelper guiHelper) {
        this.modularUI = PRIMITIVE_BLAST_FURNACE.createJeiUITemplate(
            (importItems = new ItemStackHandler(1)),
            (exportItems = new ItemStackHandler(1))
            )
            .build(new BlankUIHolder(), Minecraft.getMinecraft().player);
        this.modularUI.initWidgets();
        this.backgroundDrawable = guiHelper.createBlankDrawable(modularUI.getWidth(), modularUI.getHeight() * 2 / 3);
    }

    @Override
    public String getUid() {
        return getInstanceUid();
    }

    public static String getInstanceUid() {
        return GTValues.MODID + ":" + PRIMITIVE_BLAST_FURNACE.getMetaName();
    }

    @Override
    public String getTitle() {
        return I18n.format(PRIMITIVE_BLAST_FURNACE.getMetaFullName());
    }

    @Override
    public String getModName() {
        return GTValues.MODID;
    }

    @Override
    public IDrawable getBackground() {
        return backgroundDrawable;
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, PBFRecipeWrapper recipeWrapper, IIngredients ingredients) {
        IGuiItemStackGroup itemStackGroup = recipeLayout.getItemStacks();
        IGuiFluidStackGroup fluidStackGroup = recipeLayout.getFluidStacks();
        for (Widget uiWidget : modularUI.guiWidgets.values()) {

            if (uiWidget instanceof SlotWidget) {
                SlotWidget slotWidget = (SlotWidget) uiWidget;
                if (slotWidget.getHandle().getItemHandler() == importItems) {
                    //this is input item stack slot widget, so add it to item group
                    itemStackGroup.init(slotWidget.getHandle().getSlotIndex(), true, slotWidget.getXPosition() - 1, slotWidget.getYPosition() - 1);
                } else if (slotWidget.getHandle().getItemHandler() == exportItems) {
                    //this is output item stack slot widget, so add it to item group
                    itemStackGroup.init(importItems.getSlots() + slotWidget.getHandle().getSlotIndex(), false, slotWidget.getXPosition() - 1, slotWidget.getYPosition() - 1);
                }

            }
        }
        itemStackGroup.addTooltipCallback(recipeWrapper::addTooltip);
        fluidStackGroup.addTooltipCallback(recipeWrapper::addTooltip);
        itemStackGroup.set(ingredients);
        fluidStackGroup.set(ingredients);
    }

    @Override
    public void drawExtras(Minecraft minecraft) {
        for (Widget widget : modularUI.guiWidgets.values()) {
            widget.drawInBackground(0, 0);
            widget.drawInForeground(0, 0);
        }
    }
}
