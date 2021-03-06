package gregtech.common.tools;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class ToolJackHammer extends ToolDrillLV {

    @Override
    public int getToolDamagePerBlockBreak(ItemStack stack) {
        return 400;
    }

    @Override
    public int getToolDamagePerDropConversion(ItemStack stack) {
        return 400;
    }

    @Override
    public int getToolDamagePerContainerCraft(ItemStack stack) {
        return 3200;
    }

    @Override
    public int getToolDamagePerEntityAttack(ItemStack stack) {
        return 800;
    }

    @Override
    public int getBaseQuality(ItemStack stack) {
        return 1;
    }

    @Override
    public float getBaseDamage(ItemStack stack) {
        return 3.0F;
    }

    @Override
    public float getSpeedMultiplier(ItemStack stack) {
        return 12.0F;
    }

    @Override
    public float getMaxDurabilityMultiplier(ItemStack stack) {
        return 2.0F;
    }

    @Override
    public boolean isMinableBlock(IBlockState block, ItemStack stack) {
        String tool = block.getBlock().getHarvestTool(block);
        return tool != null && tool.equals("pickaxe") ||
            block.getMaterial() == Material.ROCK ||
            block.getMaterial() == Material.GLASS ||
            block.getMaterial() == Material.ICE ||
            block.getMaterial() == Material.PACKED_ICE;
    }

//    @Override
//    public int convertBlockDrops(World world, BlockPos blockPos, IBlockState blockState, EntityPlayer harvester, List<ItemStack> drops) {
//        int rConversions = 0;
//        Recipe tRecipe = RecipeMap.FORGE_HAMMER_RECIPES.findRecipe(null, true, 2147483647L, null, new ItemStack[]{getBlockStack(blockState)});
//        if (tRecipe == null || blockState.getBlock().hasTileEntity(blockState)) {
//            for (ItemStack tDrop : drops) {
//                tRecipe = RecipeMap.FORGE_HAMMER_RECIPES.findRecipe(null, true, 2147483647L, null, new ItemStack[]{GTUtility.copyAmount(1, tDrop)});
//                if (tRecipe != null) {
//                    ItemStack tHammeringOutput = tRecipe.getOutputs().get(0);
//                    if (tHammeringOutput != null) {
//                        rConversions += tDrop.stackSize;
//                        tDrop.stackSize *= tHammeringOutput.stackSize;
//                        tHammeringOutput.stackSize = tDrop.stackSize;
//                        GTUtility.setStack(tDrop, tHammeringOutput);
//                    }
//                }
//            }
//        } else {
//            drops.clear();
//            drops.add(tRecipe.getOutputs().get(0));
//            rConversions++;
//        }
//        return rConversions;
//    }

    @Override
    public void onToolCrafted(ItemStack stack, EntityPlayer player) {
        super.onToolCrafted(stack, player);
//        GregTechMod.achievements.issueAchievement(player, "hammertime"); // TODO ACHIEVEMENTS/ADVANCEMENTS
    }

//    @Override
//    public ITextComponent getDeathMessage(EntityLivingBase player, EntityLivingBase entity) {
//        return new TextComponentString(TextFormatting.RED + "")
//                .appendSibling(entity.getDisplayName())
//                .appendText(TextFormatting.WHITE + " has been jackhammered into pieces by " + TextFormatting.GREEN)
//                .appendSibling(player.getDisplayName());
//    }
}
