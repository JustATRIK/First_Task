package com.example.examplemod.block.tiles;

import com.example.examplemod.dimensions.BlocksMinerVoidDimension;
import com.example.examplemod.dimensions.ModDimensions;
import com.example.examplemod.gui.BlocksMinerContainer;
import com.example.examplemod.gui.BlocksMinerGui;
import com.example.examplemod.utils.BlocksMinerFakePlayer;
import com.example.examplemod.utils.BlocksMinerUtils;
import com.example.examplemod.utils.IGuiTile;
import com.example.examplemod.utils.energy.ModEnergyStorage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockDropper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import org.lwjgl.Sys;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.UUID;

public class BlocksMinerTileEntity extends TileEntity implements ITickable, ISidedInventory, IGuiTile {
    //Публичные поля
    final static int REAL_ENERGY_CONSUMING = 10;
    public int energyConsuming = 0;
    public NonNullList<ItemStack> items = NonNullList.withSize(19, ItemStack.EMPTY);
    public float curBlockDamageMP = 0;
    public WeakReference<BlocksMinerFakePlayer> fakePlayer;
    //Приватные поля
    private final net.minecraftforge.common.capabilities.CapabilityDispatcher capabilities;
    private final ModEnergyStorage energyStorage = new ModEnergyStorage(20000, 200, REAL_ENERGY_CONSUMING);
    private boolean startedMining;
    private int targetSlot;
    private int clientEnergyConsuming = -1;
    private int clientStoredEnergy = -1;
    private float clientCurBlockDamageMP = -1;
    private IBlockState targetBlock;
    private BlockPos freePosition = new BlockPos(0, 0, 0);

    public BlocksMinerTileEntity() {
        capabilities = net.minecraftforge.event.ForgeEventFactory.gatherCapabilities(this);
    }

    @Override
    public void update() {
        if (!(world instanceof WorldServer)) return;
        energyStorage.receiveEnergy(1000, false);
        //Ретерн, если нехвтает энергии для копания
        if (getStoredEnergy() < energyConsuming) {
            energyConsuming = 0;
            return;
        }
        //Создать фейк-плейера, если его еще нет
        if (fakePlayer == null) {
            fakePlayer = BlocksMinerFakePlayer.initFakePlayer(DimensionManager.getWorld(2), UUID.randomUUID(), "blocks_miner", world.provider.getDimension(), pos);
            //Если получилось создать игрока
            if (fakePlayer != null) {
                if (DimensionManager.getWorld(2).getBlockState(freePosition).getBlock() != Blocks.AIR) freePosition = BlocksMinerUtils.getValidPosForSpawn(pos, (WorldServer) fakePlayer.get().world);
                fakePlayer.get().setPosition(freePosition.getX(), freePosition.getY(), freePosition.getZ());
            }
            return;
        }
        else if (freePosition != fakePlayer.get().getPosition()){
            freePosition = fakePlayer.get().getPosition();
        }
        //Ретерн, если нет блоков для копания
        if (getFirstFullStackInRange(1, 7) == -1 && fakePlayer.get().world.getBlockState(freePosition).getBlock() == Blocks.AIR) {
            curBlockDamageMP = 0;
            energyConsuming = 0;
            return;
        }
        //Настраиваем нужный слот
        if (!startedMining) {
            targetSlot = getFirstFullStackInRangeAsBlock(1, 7);
            startedMining = true;
            //Ставим блок в измерении
            placeBlock();
        }
        targetBlock = fakePlayer.get().world.getBlockState(freePosition);
        //Добываем блок
        if ((curBlockDamageMP += targetBlock.getPlayerRelativeBlockHardness(fakePlayer.get(), fakePlayer.get().world, freePosition) * 5) >= 1.0F) {
            startedMining = false;
            curBlockDamageMP = 0;
            energyConsuming = 0;
            fakePlayer.get().interactionManager.tryHarvestBlock(freePosition);
            return;
        }
        energyConsuming = REAL_ENERGY_CONSUMING;
        energyStorage.extractEnergy(energyConsuming, false);
    }

    @Override
    @Nullable
    public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable net.minecraft.util.EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY){
            return CapabilityEnergy.ENERGY.cast(energyStorage);
        }
        return capabilities == null ? null : capabilities.getCapability(capability, facing);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    private void setFakePlayerMainHandItem(ItemStack itemStack) {
        fakePlayer.get().setHeldItem(EnumHand.MAIN_HAND, itemStack);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound = ItemStackHelper.saveAllItems(compound, items);
        compound.setInteger("energy", energyStorage.getEnergyStored());
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        ItemStackHelper.loadAllItems(compound, items);
        energyStorage.setEnergy(compound.getInteger("energy"));
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
        addItemStackToInventoryInRange(itemStackIn, 1, items.size());
    }

    public void addItemStackToInventoryInRange(ItemStack itemStackIn, int startIndex, int endIndex) {
        int toDrop = 0;
        boolean flag = false;
        for (int i = startIndex; i < endIndex; i++) {
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
            int firstEmptyStack = getFirstEmptyStackInRange(startIndex, endIndex);
            if (firstEmptyStack != -1) {
                setInventorySlotContents(firstEmptyStack, itemStackIn);
            }
            else {
                Block.spawnAsEntity(world, pos.add(0, 0.5, 0), itemStackIn);
            }
        }
    }

    public int getFirstEmptyStack() {
        return getFirstFullStackInRange(1, items.size());
    }

    public int getFirstFullStackInRangeAsBlock(int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex; i++) {
            if (!items.get(i).isEmpty() && canCastItemToBlock(items.get(i))) {
                return i;
            }
        }
        return -1;
    }

    public int getFirstEmptyStackInRange(int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex; i++) {
            if (items.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    public int getFirstFullStack() {
        return getFirstFullStackInRange(1, items.size());
    }

    public int getFirstFullStackInRange(int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex; i++) {
            if (!items.get(i).isEmpty()) {
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

    public int getEnergyConsuming(){
        return this.energyConsuming;
    }

    public int getStoredEnergy(){
        return this.energyStorage.getEnergyStored();
    }

    public int getClientIntData(int ID){
        switch (ID){
            case 0: return this.clientStoredEnergy;
            case 1: return this.clientEnergyConsuming;
        }
        return -1;
    }

    public void setClientIntData(int ID, int value){
        switch (ID){
            case 0: this.clientStoredEnergy = value;
            case 1: this.clientEnergyConsuming = value;
        }
    }

    public float getClientFloatData(int ID){
        switch (ID){
            case 0: return this.clientCurBlockDamageMP;
        }
        return -1;
    }

    public void setClientFloatData(int ID, float value){
        switch (ID){
            case 0: this.clientCurBlockDamageMP = value;
        }
    }

    private boolean canCastItemToBlock(ItemStack itemStack){
        try {
            ItemBlock i = (ItemBlock) itemStack.getItem();
            return true;
        } catch (Exception e){
            return false;
        }
    }

    @Override
    public Container createContainer(EntityPlayer player) {
        return new BlocksMinerContainer(player, this);
    }

    @Override
    public GuiContainer createGui(EntityPlayer player) {
        return new BlocksMinerGui(new BlocksMinerContainer(player, this), this);
    }

    private void placeBlock(){
        setFakePlayerMainHandItem(items.get(targetSlot));
        fakePlayer.get().interactionManager.processRightClickBlock(fakePlayer.get(), fakePlayer.get().world, items.get(targetSlot), EnumHand.MAIN_HAND, freePosition, EnumFacing.UP, freePosition.getX(), freePosition.getY() - 1, freePosition.getZ());
        setFakePlayerMainHandItem(items.get(0));
    }
}


