package gregtech.api.block.machines;

import codechicken.lib.render.particle.CustomParticleHandler;
import gregtech.api.GregTechAPI;
import gregtech.api.capability.ICustomHighlightBlock;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.render.MetaTileEntityRenderer;
import gregtech.api.unification.stack.SimpleItemStack;
import gregtech.api.util.GTUtility;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.*;

@SuppressWarnings("deprecation")
public class BlockMachine extends Block implements ITileEntityProvider, ICustomHighlightBlock {

    public BlockMachine() {
        super(Material.IRON);
        setCreativeTab(GregTechAPI.TAB_GREGTECH);
        setSoundType(SoundType.METAL);
        setHardness(6.0f);
        setResistance(6.0f);
        setUnlocalizedName("unnamed");
        setHarvestLevel("wrench", 1);
    }

    public static MetaTileEntity getMetaTileEntity(IBlockAccess blockAccess, BlockPos pos) {
        MetaTileEntityHolder holder = (MetaTileEntityHolder) blockAccess.getTileEntity(pos);
        return holder == null ? null : holder.getMetaTileEntity();
    }

    private Collection<AxisAlignedBB> getBoundingBoxes(IBlockAccess blockAccess, BlockPos pos) {
        MetaTileEntity metaTileEntity = getMetaTileEntity(blockAccess, pos);
        if(metaTileEntity == null)
            return Collections.emptyList();
        return metaTileEntity.getCollisionBoxes();
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        MetaTileEntity metaTileEntity = getMetaTileEntity(world, pos);
        if(metaTileEntity == null)
            return ItemStack.EMPTY;
        return new ItemStack(Item.getItemFromBlock(this), 1,
            GregTechAPI.META_TILE_ENTITY_REGISTRY.getIdByObjectName(metaTileEntity.metaTileEntityId));
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean isActualState) {
        for(AxisAlignedBB axisAlignedBB : getBoundingBoxes(worldIn, pos)) {
            AxisAlignedBB offsetBox = axisAlignedBB.offset(pos);
            if (entityBox.intersects(offsetBox)) {
                collidingBoxes.add(offsetBox);
            }
        }
    }

    @Override
    public Collection<AxisAlignedBB> getSelectedBoundingBoxes(IBlockAccess world, BlockPos blockPos, IBlockState blockState) {
        ArrayList<AxisAlignedBB> selection = new ArrayList<>();
        for(AxisAlignedBB axisAlignedBB : getBoundingBoxes(world, blockPos)) {
            selection.add(axisAlignedBB.offset(blockPos));
        }
        return selection;
    }

    @Nullable
    @Override
    public RayTraceResult collisionRayTrace(IBlockState blockState, World worldIn, BlockPos pos, Vec3d start, Vec3d end) {
        for(AxisAlignedBB boundingBox : getBoundingBoxes(worldIn, pos)) {
            Vec3d vec3d = start.subtract(pos.getX(), pos.getY(), pos.getZ());
            Vec3d vec3d1 = end.subtract(pos.getX(), pos.getY(), pos.getZ());
            RayTraceResult raytraceresult = boundingBox.calculateIntercept(vec3d, vec3d1);
            if(raytraceresult != null) {
                return new RayTraceResult(raytraceresult.hitVec.addVector(pos.getX(), pos.getY(), pos.getZ()), raytraceresult.sideHit, pos);
            }
        }
        return null;
    }

    @Override
    public boolean rotateBlock(World world, BlockPos pos, EnumFacing axis) {
        MetaTileEntity metaTileEntity = getMetaTileEntity(world, pos);
        if(metaTileEntity == null ||
            !metaTileEntity.isValidFrontFacing(axis) ||
            metaTileEntity.getFrontFacing() == axis)
            return false;
        metaTileEntity.setFrontFacing(axis);
        return true;
    }

    @Nullable
    @Override
    public EnumFacing[] getValidRotations(World world, BlockPos pos) {
        MetaTileEntity metaTileEntity = getMetaTileEntity(world, pos);
        if(metaTileEntity == null) return null;
        return Arrays.stream(EnumFacing.VALUES)
            .filter(metaTileEntity::isValidFrontFacing)
            .toArray(EnumFacing[]::new);
    }

    @Override
    public boolean recolorBlock(World world, BlockPos pos, EnumFacing side, EnumDyeColor color) {
        MetaTileEntity metaTileEntity = getMetaTileEntity(world, pos);
        if(metaTileEntity == null ||
            metaTileEntity.getPaintingColor() == color.colorValue)
            return false;
        metaTileEntity.setPaintingColor(color.colorValue);
        return true;
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        MetaTileEntityHolder holder = (MetaTileEntityHolder) worldIn.getTileEntity(pos);
        MetaTileEntity sampleMetaTileEntity = GregTechAPI.META_TILE_ENTITY_REGISTRY.getObjectById(stack.getItemDamage());
        if(holder != null && sampleMetaTileEntity != null) {
            MetaTileEntity metaTileEntity = holder.setMetaTileEntity(sampleMetaTileEntity);
            if(stack.hasTagCompound()) {
                metaTileEntity.initFromItemStackData(stack.getTagCompound());
            }
            EnumFacing placeFacing = placer.getHorizontalFacing().getOpposite();
            if(metaTileEntity.isValidFrontFacing(placeFacing)) {
                metaTileEntity.setFrontFacing(placeFacing);
            }
        }
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        MetaTileEntity metaTileEntity = getMetaTileEntity(worldIn, pos);
        if(metaTileEntity != null) {
            NonNullList<ItemStack> inventoryContents = NonNullList.create();
            metaTileEntity.clearMachineInventory(inventoryContents);
            for(ItemStack itemStack : inventoryContents) {
                Block.spawnAsEntity(worldIn, pos, itemStack);
            }
            metaTileEntity.onRemoval();
        }
        super.breakBlock(worldIn, pos, state);
    }



    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        MetaTileEntity metaTileEntity = tileEntities.get() == null ? getMetaTileEntity(world, pos) : tileEntities.get();
        if(metaTileEntity == null) return;

        ItemStack itemStack = new ItemStack(Item.getItemFromBlock(this), 1,
            GregTechAPI.META_TILE_ENTITY_REGISTRY.getIdByObjectName(metaTileEntity.metaTileEntityId));
        NBTTagCompound tagCompound = new NBTTagCompound();
        metaTileEntity.writeItemStackData(tagCompound);
        //only set item tag if it's not empty, so newly created items will stack with dismantled
        if(!tagCompound.hasNoTags())
            itemStack.setTagCompound(tagCompound);
        drops.add(itemStack);
        metaTileEntity.getDrops(drops, harvesters.get());
    }

    private static final int DAMAGE_FOR_SCREWDRIVER_CLICK = 40;
    private static final int DAMAGE_FOR_WRENCH_CLICK = 60;

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        MetaTileEntity metaTileEntity = getMetaTileEntity(worldIn, pos);
        if(metaTileEntity == null) return false;
        ItemStack itemInHand = playerIn.getHeldItem(hand);
        if(!itemInHand.isEmpty()) {
            SimpleItemStack simpleItemStack = new SimpleItemStack(itemInHand);
            if(GregTechAPI.screwdriverList.contains(simpleItemStack)) {
                if(GTUtility.doDamageItem(itemInHand, DAMAGE_FOR_SCREWDRIVER_CLICK, true) &&
                    metaTileEntity.onScrewdriverClick(playerIn, hand, facing, hitX, hitY, hitZ)) {
                    GTUtility.doDamageItem(itemInHand, DAMAGE_FOR_SCREWDRIVER_CLICK, false);
                    return true;
                } else return false;
            } else if(GregTechAPI.wrenchList.contains(simpleItemStack)) {
                //System.out.println("We are here " + );
                if(GTUtility.doDamageItem(itemInHand, DAMAGE_FOR_WRENCH_CLICK, true) &&
                    metaTileEntity.onWrenchClick(playerIn, hand, facing, hitX, hitY, hitZ)) {
                    //System.out.println("And here we now");
                    GTUtility.doDamageItem(itemInHand, DAMAGE_FOR_WRENCH_CLICK, false);
                    return true;
                } else return false;
            }
        }
        return metaTileEntity.onRightClick(playerIn, hand, facing, hitX, hitY, hitZ);
    }

    @Override
    public void onBlockClicked(World worldIn, BlockPos pos, EntityPlayer playerIn) {
        MetaTileEntity metaTileEntity = getMetaTileEntity(worldIn, pos);
        if(metaTileEntity == null) return;
        metaTileEntity.onLeftClick(playerIn);
    }

    @Override
    public boolean canConnectRedstone(IBlockState state, IBlockAccess world, BlockPos pos, @Nullable EnumFacing side) {
        MetaTileEntity metaTileEntity = getMetaTileEntity(world, pos);
        return metaTileEntity != null && metaTileEntity.canConnectRedstone(side);
    }

    @Override
    public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        MetaTileEntity metaTileEntity = getMetaTileEntity(blockAccess, pos);
        return metaTileEntity == null ? 0 : metaTileEntity.getOutputRedstoneSignal(side);
    }

    @Override
    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos) {
        MetaTileEntity metaTileEntity = getMetaTileEntity(worldIn, pos);
        return metaTileEntity == null ? 0 : metaTileEntity.getComparatorValue();
    }

    protected ThreadLocal<MetaTileEntity> tileEntities = new ThreadLocal<>();

    @Override
    public void harvestBlock(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack stack) {
        tileEntities.set(te == null ? null : ((MetaTileEntityHolder) te).getMetaTileEntity());
        super.harvestBlock(worldIn, player, pos, state, te, stack);
        tileEntities.set(null);
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return false;
    }

    @Override
    public boolean isNormalCube(IBlockState state) {
        return true;
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public MetaTileEntityHolder createNewTileEntity(@Nullable World worldIn, int meta) {
        return new MetaTileEntityHolder();
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return MetaTileEntityRenderer.BLOCK_RENDER_TYPE;
    }

    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean addLandingEffects(IBlockState state, WorldServer worldObj, BlockPos blockPosition, IBlockState iblockstate, EntityLivingBase entity, int numberOfParticles) {
        return CustomParticleHandler.handleLandingEffects(worldObj, blockPosition, entity, numberOfParticles);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean addHitEffects(IBlockState state, World worldObj, RayTraceResult target, ParticleManager manager) {
        return CustomParticleHandler.handleHitEffects(state, worldObj, target, manager);
    }

    @Override
    public boolean addDestroyEffects(World world, BlockPos pos, ParticleManager manager) {
        return CustomParticleHandler.handleDestroyEffects(world, pos, manager);
    }

    @Override
    public boolean addRunningEffects(IBlockState state, World world, BlockPos pos, Entity entity) {
        return true;
    }


    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
        for (String metaTileEntityId : GregTechAPI.META_TILE_ENTITY_REGISTRY.getKeys()) {
            int metaId = GregTechAPI.META_TILE_ENTITY_REGISTRY.getIdByObjectName(metaTileEntityId);
            items.add(new ItemStack(this, 1, metaId));
        }
    }
}
