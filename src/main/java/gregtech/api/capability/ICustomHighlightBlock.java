package gregtech.api.capability;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Collection;

public interface ICustomHighlightBlock {

    Collection<AxisAlignedBB> getSelectedBoundingBoxes(IBlockAccess world, BlockPos blockPos, IBlockState blockState);

}
