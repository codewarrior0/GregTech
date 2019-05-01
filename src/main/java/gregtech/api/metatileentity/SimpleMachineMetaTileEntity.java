package gregtech.api.metatileentity;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregtech.api.capability.impl.EnergyContainerHandler;
import gregtech.api.capability.impl.FluidHandlerProxy;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.ItemHandlerProxy;
import gregtech.api.cover.ICoverable;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.DischargerSlotWidget;
import gregtech.api.gui.widgets.ImageWidget;
import gregtech.api.gui.widgets.LabelWidget;
import gregtech.api.gui.widgets.ToggleButtonWidget;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.render.OrientedOverlayRenderer;
import gregtech.api.render.Textures;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public class SimpleMachineMetaTileEntity extends WorkableTieredMetaTileEntity {

    private boolean hasFrontFacing;

    private ItemStackHandler chargerInventory;
    private EnumFacing outputFacing;

    private boolean autoOutputItems;
    private boolean autoOutputFluids;
    private boolean allowInputFromOutputSide;

    protected IItemHandler outputItemInventory;
    protected IFluidHandler outputFluidInventory;

    public SimpleMachineMetaTileEntity(ResourceLocation metaTileEntityId, RecipeMap<?> recipeMap, OrientedOverlayRenderer renderer, int tier) {
        this(metaTileEntityId, recipeMap, renderer, tier, true);
    }

    public SimpleMachineMetaTileEntity(ResourceLocation metaTileEntityId, RecipeMap<?> recipeMap, OrientedOverlayRenderer renderer, int tier, boolean hasFrontFacing) {
        super(metaTileEntityId, recipeMap, renderer, tier);
        this.hasFrontFacing = hasFrontFacing;
        this.chargerInventory = new ItemStackHandler(1) {
            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new SimpleMachineMetaTileEntity(metaTileEntityId, workable.recipeMap, renderer, getTier(), hasFrontFacing);
    }

    @Override
    protected void initializeInventory() {
        super.initializeInventory();
        this.outputItemInventory = new ItemHandlerProxy(new ItemStackHandler(0), exportItems);
        this.outputFluidInventory = new FluidHandlerProxy(new FluidTankList(false), exportFluids);
    }

    @Override
    public boolean hasFrontFacing() {
        return hasFrontFacing;
    }

    @Override
    public boolean onWrenchClick(EntityPlayer playerIn, EnumHand hand, EnumFacing facing, CuboidRayTraceResult hitResult) {
        if (!playerIn.isSneaking()) {
            EnumFacing currentOutputSide = getOutputFacing();
            if (currentOutputSide == facing || getFrontFacing() == facing) {
                return false;
            }
            if(!getWorld().isRemote) {
                setOutputFacing(facing);
            }
            return true;
        }
        return super.onWrenchClick(playerIn, hand, facing, hitResult);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        if (outputFacing != null) {
            Textures.PIPE_OUT_OVERLAY.renderSided(outputFacing, renderState, translation, pipeline);
            if (autoOutputItems) {
                Textures.ITEM_OUTPUT_OVERLAY.renderSided(outputFacing, renderState, translation, pipeline);
            }
            if (autoOutputFluids) {
                Textures.FLUID_OUTPUT_OVERLAY.renderSided(outputFacing, renderState, translation, pipeline);
            }
        }
    }

    @Override
    public void update() {
        super.update();
        if (!getWorld().isRemote) {
            ((EnergyContainerHandler) this.energyContainer).dischargeOrRechargeEnergyContainers(chargerInventory, 0);
            if (getTimer() % 5 == 0) {
                EnumFacing outputFacing = getOutputFacing();
                if (autoOutputFluids) {
                    pushFluidsIntoNearbyHandlers(outputFacing);
                }
                if (autoOutputItems) {
                    pushItemsIntoNearbyHandlers(outputFacing);
                }
            }
        }
    }

    @Override
    public boolean onScrewdriverClick(EntityPlayer playerIn, EnumHand hand, EnumFacing facing, CuboidRayTraceResult hitResult) {
        EnumFacing coverSide = ICoverable.traceCoverSide(hitResult);
        if(coverSide == getOutputFacing()) {
            if(!getWorld().isRemote) {
                if(allowInputFromOutputSide) {
                    setAllowInputFromOutputSide(false);
                    playerIn.sendMessage(new TextComponentTranslation("gregtech.machine.basic.input_from_output_side.disallow"));
                } else {
                    setAllowInputFromOutputSide(true);
                    playerIn.sendMessage(new TextComponentTranslation("gregtech.machine.basic.input_from_output_side.allow"));
                }
            }
            return true;
        }
        return super.onScrewdriverClick(playerIn, hand, facing, hitResult);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            IFluidHandler fluidHandler = (side == getOutputFacing() && !allowInputFromOutputSide) ? outputFluidInventory : fluidInventory;
            if(fluidHandler.getTankProperties().length > 0) {
                return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(fluidHandler);
            }
            return null;
        } else if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            IItemHandler itemHandler = (side == getOutputFacing() && !allowInputFromOutputSide) ? outputItemInventory : itemInventory;
            if(itemHandler.getSlots() > 0) {
                return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(itemHandler);
            }
            return null;
        }
        return super.getCapability(capability, side);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setTag("ChargerInventory", chargerInventory.serializeNBT());
        data.setInteger("OutputFacing", getOutputFacing().getIndex());
        data.setBoolean("AutoOutputItems", autoOutputItems);
        data.setBoolean("AutoOutputFluids", autoOutputFluids);
        data.setBoolean("AllowInputFromOutputSide", allowInputFromOutputSide);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.chargerInventory.deserializeNBT(data.getCompoundTag("ChargerInventory"));
        this.outputFacing = EnumFacing.VALUES[data.getInteger("OutputFacing")];
        this.autoOutputItems = data.getBoolean("AutoOutputItems");
        this.autoOutputFluids = data.getBoolean("AutoOutputFluids");
        this.allowInputFromOutputSide = data.getBoolean("AllowInputFromOutputSide");
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeByte(getOutputFacing().getIndex());
        buf.writeBoolean(autoOutputItems);
        buf.writeBoolean(autoOutputFluids);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.outputFacing = EnumFacing.VALUES[buf.readByte()];
        this.autoOutputItems = buf.readBoolean();
        this.autoOutputFluids = buf.readBoolean();
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == 100) {
            this.outputFacing = EnumFacing.VALUES[buf.readByte()];
            getHolder().scheduleChunkForRenderUpdate();
        } else if (dataId == 101) {
            this.autoOutputItems = buf.readBoolean();
            getHolder().scheduleChunkForRenderUpdate();
        } else if (dataId == 102) {
            this.autoOutputFluids = buf.readBoolean();
            getHolder().scheduleChunkForRenderUpdate();
        }
    }

    @Override
    public boolean isValidFrontFacing(EnumFacing facing) {
        //use direct outputFacing field instead of getter method because otherwise
        //it will just return SOUTH for null output facing
        return super.isValidFrontFacing(facing) && facing != outputFacing;
    }

    public void setOutputFacing(EnumFacing outputFacing) {
        this.outputFacing = outputFacing;
        if (!getWorld().isRemote) {
            writeCustomData(100, buf -> buf.writeByte(outputFacing.getIndex()));
            markDirty();
        }
    }

    public void setAutoOutputItems(boolean autoOutputItems) {
        this.autoOutputItems = autoOutputItems;
        if (!getWorld().isRemote) {
            writeCustomData(101, buf -> buf.writeBoolean(autoOutputItems));
            markDirty();
        }
    }

    public void setAutoOutputFluids(boolean autoOutputFluids) {
        this.autoOutputFluids = autoOutputFluids;
        if (!getWorld().isRemote) {
            writeCustomData(102, buf -> buf.writeBoolean(autoOutputFluids));
            markDirty();
        }
    }

    public void setAllowInputFromOutputSide(boolean allowInputFromOutputSide) {
        this.allowInputFromOutputSide = allowInputFromOutputSide;
        if(!getWorld().isRemote) {
            markDirty();
        }
    }

    @Override
    public void setFrontFacing(EnumFacing frontFacing) {
        super.setFrontFacing(frontFacing);
        if (this.outputFacing == null) {
            //set initial output facing as opposite to front
            setOutputFacing(frontFacing.getOpposite());
        }
    }

    public EnumFacing getOutputFacing() {
        return outputFacing == null ? EnumFacing.SOUTH : outputFacing;
    }

    public boolean isAutoOutputItems() {
        return autoOutputItems;
    }

    public boolean isAutoOutputFluids() {
        return autoOutputFluids;
    }

    public boolean isAllowInputFromOutputSide() {
        return allowInputFromOutputSide;
    }

    @Override
    public void clearMachineInventory(NonNullList<ItemStack> itemBuffer) {
        super.clearMachineInventory(itemBuffer);
        clearInventory(itemBuffer, chargerInventory);
    }

    protected ModularUI.Builder createGuiTemplate(EntityPlayer player) {
        ModularUI.Builder builder = workable.recipeMap.createUITemplate(workable::getProgressPercent, importItems, exportItems, importFluids, exportFluids)
            .widget(new LabelWidget(5, 5, getMetaFullName()))
            .widget(new DischargerSlotWidget(chargerInventory, 0, 79, 62)
                .setBackgroundTexture(GuiTextures.SLOT, GuiTextures.CHARGER_OVERLAY))
            .widget(new ImageWidget(79, 42, 18, 18, GuiTextures.INDICATOR_NO_ENERGY)
                .setPredicate(workable::isHasNotEnoughEnergy))
            .bindPlayerInventory(player.inventory);

        int leftButtonStartX = 7;
        int rightButtonStartX = 176 - 7 - 20;
        if(workable.recipeMap instanceof RecipeMapWithConfigButton) {
            leftButtonStartX += ((RecipeMapWithConfigButton) workable.recipeMap).getLeftButtonOffset();
            rightButtonStartX -= ((RecipeMapWithConfigButton) workable.recipeMap).getRightButtonOffset();
        }

        if (exportItems.getSlots() > 0) {
            builder.widget(new ToggleButtonWidget(leftButtonStartX, 62, 18, 18,
                GuiTextures.BUTTON_ITEM_OUTPUT, this::isAutoOutputItems, this::setAutoOutputItems));
            leftButtonStartX += 18;
        }
        if (exportFluids.getTanks() > 0) {
            builder.widget(new ToggleButtonWidget(leftButtonStartX, 62, 18, 18,
                GuiTextures.BUTTON_FLUID_OUTPUT, this::isAutoOutputFluids, this::setAutoOutputFluids));
        }

        builder.widget(new ToggleButtonWidget(rightButtonStartX, 60, 20, 20,
            GuiTextures.BUTTON_OVERCLOCK, workable::isAllowOverclocking, workable::setAllowOverclocking)
            .setTooltipText("gregtech.gui.overclock"));

        return builder;
    }

    @Override
    protected ModularUI createUI(EntityPlayer entityPlayer) {
        return createGuiTemplate(entityPlayer).build(getHolder(), entityPlayer);
    }

    public interface RecipeMapWithConfigButton {
        int getLeftButtonOffset();
        int getRightButtonOffset();
    }
}
