package com.example.examplemod.block.tiles;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.utils.BlocksMinerFakePlayer;
import com.example.examplemod.utils.energy.ModEnergyStorage;
import com.example.examplemod.utils.packets.EnergyAndProgressSyncPacket;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.lwjgl.Sys;

import javax.annotation.Nullable;
import java.util.UUID;

public class BlocksMinerTileEntity extends TileEntity implements ITickable, ISidedInventory {
    public static final Capability<IEnergyStorage> ENERGY_HANDLER = CapabilityEnergy.ENERGY;
    final static int REAL_ENERGY_CONSUMING = 10;
    int energyConsuming = 0;
    public NonNullList<ItemStack> items = NonNullList.withSize(19, ItemStack.EMPTY);
    boolean startedMining;
    public FakePlayer fakePlayer;
    BlockPos targetBlockPos;
    BlockPos offest;
    EnumFacing enumFacing;
    public float curBlockDamageMP = 0;
    private net.minecraftforge.common.capabilities.CapabilityDispatcher capabilities;
    private final ModEnergyStorage energyStorage = new ModEnergyStorage(20000, 200, REAL_ENERGY_CONSUMING) {
        @Override
        public void onEnergyChanged() {
            ExampleMod.NETWORK.sendToAll(new EnergyAndProgressSyncPacket(pos, energy, curBlockDamageMP));
        }
    };

    public BlocksMinerTileEntity() {
        capabilities = net.minecraftforge.event.ForgeEventFactory.gatherCapabilities(this);
    }

    @Override
    public void update() {
        if (world instanceof WorldServer) {
            if (energyStorage.getEnergyStored() < energyConsuming){
                energyConsuming = 0;
                return;
            }
            if (enumFacing != world.getBlockState(pos).getValue(BlockDirectional.FACING)) {
                enumFacing = world.getBlockState(pos).getValue(BlockDirectional.FACING);
                if (enumFacing == EnumFacing.UP) offest = new BlockPos(0, 1, 0);
                else if (enumFacing == EnumFacing.DOWN) offest = new BlockPos(0, -1, 0);
                else if (enumFacing == EnumFacing.NORTH) offest = new BlockPos(0, 0, -1);
                else if (enumFacing == EnumFacing.SOUTH) offest = new BlockPos(0, 0, 1);
                else if (enumFacing == EnumFacing.WEST) offest = new BlockPos(-1, 0, 0);
                else if (enumFacing == EnumFacing.EAST) offest = new BlockPos(1, 0, 0);
            }
            if (fakePlayer == null) {
                fakePlayer = BlocksMinerFakePlayer.initFakePlayer((WorldServer) world, UUID.randomUUID(), "blocks_miner");
                fakePlayer.setPosition(pos.getX(), pos.getY(), pos.getZ());
                setFakePlayerMainHandItem(items.get(0));
            }
            else {
                targetBlockPos = pos.add(offest);
                IBlockState targetBlock = world.getBlockState(targetBlockPos);
                if (targetBlock.getBlock() == Blocks.AIR) {
                    curBlockDamageMP = 0;
                    energyConsuming = 0;
                    world.sendBlockBreakProgress(fakePlayer.getEntityId(), targetBlockPos, (int) (curBlockDamageMP * 10.0F) - 1);
                    return;
                }
                if (!startedMining) {
                    fakePlayer.connection.sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ()), EnumFacing.DOWN));
                    startedMining = true;
                }
                if ((curBlockDamageMP += targetBlock.getPlayerRelativeBlockHardness(fakePlayer, world, targetBlockPos) * 5) >= 1.0F) {
                    fakePlayer.connection.sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, targetBlockPos, EnumFacing.UP));
                    fakePlayer.interactionManager.tryHarvestBlock(targetBlockPos);
                    startedMining = false;
                    curBlockDamageMP = 0.0F;
                }
                energyConsuming = REAL_ENERGY_CONSUMING;
                world.sendBlockBreakProgress(fakePlayer.getEntityId(), targetBlockPos, (int) (curBlockDamageMP * 10.0F) - 1);
                getEnergyStorage().extractEnergy(energyConsuming, false);
            }
        }
    }

    @Override
    @Nullable
    public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable net.minecraft.util.EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY && facing != enumFacing){
            return ENERGY_HANDLER.cast(energyStorage);
        }
        return capabilities == null ? null : capabilities.getCapability(capability, facing);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY && facing != enumFacing) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    private void setFakePlayerMainHandItem(ItemStack itemStack) {
        fakePlayer.setHeldItem(EnumHand.MAIN_HAND, itemStack);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound = ItemStackHelper.saveAllItems(compound, items);
        compound.setInteger("blocks_destroyer.energy", energyStorage.getEnergyStored());
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        ItemStackHelper.loadAllItems(compound, items);
        energyStorage.setEnergy(compound.getInteger("blocks_destroyer.energy"));
    }

    @Override
    public int getSizeInventory() {
        return this.items.size();
    }

    @Override
    public ItemStack getStackInSlot(final int s) {
        return (s >= this.getSizeInventory()) ? ItemStack.EMPTY : this.items.get(s);
    }

    @Override
    public String getName() {
        return "Blocks Miner Inventory";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentTranslation("examplemod.blocks_miner.gui.name");
    }

    @Override
    public ItemStack removeStackFromSlot(final int index) {
        final ItemStack stack = this.getStackInSlot(index);
        this.setInventorySlotContents(index, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public ItemStack decrStackSize(final int index, final int count) {
        ItemStack stack = this.getStackInSlot(index);
        if (!stack.isEmpty()) {
            if (stack.getCount() <= count) {
                this.setInventorySlotContents(index, ItemStack.EMPTY);
            } else {
                stack = stack.splitStack(count);
                if (stack.getCount() == 0) {
                    this.setInventorySlotContents(index, ItemStack.EMPTY);
                }
            }
        }
        return stack;
    }

    @Override
    public void setInventorySlotContents(final int index, final ItemStack stack) {
        if (index < this.items.size()) {
            this.items.set(index, stack);
        }
        if (!world.isRemote && index == 0) setFakePlayerMainHandItem(stack);
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUsableByPlayer(final EntityPlayer par1EntityPlayer) {
        return true;
    }

    @Override
    public void openInventory(final EntityPlayer player) {
        ExampleMod.NETWORK.sendToAll(new EnergyAndProgressSyncPacket(pos, energyStorage.getEnergyStored(), curBlockDamageMP));
    }

    @Override
    public void closeInventory(final EntityPlayer player) {
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return false;
    }

    @Override
    public int getField(final int id) {
        return 0;
    }

    @Override
    public void setField(final int id, final int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        return new int[0];
    }

    @Override
    public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction) {
        return index == 0;
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
        return index != 0;
    }

    public void addItemStackToInventory(ItemStack itemStackIn) {
        int toDrop = 0;
        boolean flag = false;
        for (int i = 1; i < items.size(); i++) {
            ItemStack itemStack = items.get(i);
            if (canMergeStacks(itemStack, itemStackIn)) {
                toDrop = Math.max(itemStack.getCount() + itemStackIn.getCount() - 64, 0);
                itemStack.setCount(itemStack.getCount() + itemStackIn.getCount() - toDrop);
                itemStackIn.setCount(toDrop);
                flag = true;
            }

        }
        if (!flag) {
            toDrop = itemStackIn.getCount();
        }
        if (toDrop > 0) {
            int firstEmptyStack = getFirstEmptyStack();
            if (firstEmptyStack != -1) {
                setInventorySlotContents(firstEmptyStack, itemStackIn);
            }
            else {
                Block.spawnAsEntity(world, pos.add(0, 0.5, 0), itemStackIn);
            }
        }
    }

    public int getFirstEmptyStack() {
        for (int i = 1; i < items.size(); i++) {
            if (items.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private boolean canMergeStacks(ItemStack stack1, ItemStack stack2) {
        return !stack1.isEmpty() && this.stackEqualExact(stack1, stack2) && stack1.isStackable() && stack1.getCount() < stack1.getMaxStackSize() && stack1.getCount() < this.getInventoryStackLimit();
    }

    private boolean stackEqualExact(ItemStack stack1, ItemStack stack2) {
        return stack1.getItem() == stack2.getItem() && (!stack1.getHasSubtypes() || stack1.getMetadata() == stack2.getMetadata()) && ItemStack.areItemStackTagsEqual(stack1, stack2);
    }

    public void setEnergyLevel(int energy) {
        this.energyStorage.setEnergy(energy);
    }

    public EnergyStorage getEnergyStorage(){
        return this.energyStorage;
    }

    public int getMaxExtract(){
        return this.energyConsuming;
    }
}

