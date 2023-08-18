package com.example.examplemod.block.tiles;

import com.example.examplemod.gui.BlocksMinerContainer;
import com.example.examplemod.gui.BlocksMinerGui;
import com.example.examplemod.utils.BlocksMinerFakePlayer;
import com.example.examplemod.utils.IGuiTile;
import com.example.examplemod.utils.energy.ModEnergyStorage;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    private int targetSlot = -1;
    private int clientEnergyConsuming = -1;
    private int clientStoredEnergy = -1;
    private float clientCurBlockDamageMP = -1;
    private IBlockState targetBlockState = Blocks.AIR.getDefaultState();
    private BlockPos freePosition;

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
            fakePlayer = BlocksMinerFakePlayer.initFakePlayer((WorldServer) world, UUID.randomUUID(), "blocks_miner");
            //Если получилось создать игрока
            if (fakePlayer != null) {
                fakePlayer.get().setPosition(pos.getX(), pos.getY(), pos.getZ());
                setFakePlayerMainHandItem(items.get(0));
            }
            return;
        }
        else if (freePosition != fakePlayer.get().getPosition()){
            freePosition = fakePlayer.get().getPosition();
        }
        //Ретерн, если нет блоков для копания
        if (getFirstFullStackInRange(1, 7) == -1 && targetBlockState == null && !startedMining) {
            curBlockDamageMP = 0;
            energyConsuming = 0;
            return;
        }
        //Настраиваем нужный слот
        if (!startedMining) {
            targetSlot = getFirstFullStackInRangeAsBlock(1, 7);

            if (targetSlot == -1) return;
            if (!canCastItemToBlock(getStackInSlot(targetSlot))) return;
            startedMining = true;
            //Ставим блок в измерении
            setTargetBlockState();
        }
        //Добываем блок
        if ((curBlockDamageMP += getRealBlockHardness()) >= 1.0F) {
            startedMining = false;
            curBlockDamageMP = 0;
            energyConsuming = 0;
            try {
                mineBlock();
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
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
        compound.setFloat("progress", curBlockDamageMP);
        compound.setBoolean("startedMining", startedMining);
        Block targetBlock = targetBlockState.getBlock();
        ItemStack targetBlockAsIS = new ItemStack(Item.getItemFromBlock(targetBlock));
        NBTTagCompound newNbt = new NBTTagCompound();
        targetBlockAsIS.writeToNBT(newNbt);
        compound.setTag("targetBlock", newNbt);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        energyStorage.setEnergy(compound.getInteger("energy"));
        curBlockDamageMP = compound.getFloat("progress");
        startedMining = compound.getBoolean("startedMining");
        try {
            ItemStackHelper.loadAllItems(compound, items);
            ItemStack targetBlockAsIS = new ItemStack((NBTTagCompound) compound.getTag("targetBlock"));
            targetBlockState = ((ItemBlock) targetBlockAsIS.getItem()).getBlock().getStateFromMeta(targetBlockAsIS.getMetadata());
        } catch (Exception ignored) {}
    }

    @Override
    public int getSizeInventory() {
        return items.size();
    }

    @Override
    public ItemStack getStackInSlot(final int index) {
        return items.get(index);
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
        items.set(index, stack);
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
        items = NonNullList.withSize(getSizeInventory(), ItemStack.EMPTY);
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
        return energyConsuming;
    }

    public int getStoredEnergy(){
        return energyStorage.getEnergyStored();
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
        return itemStack.getItem() instanceof ItemBlock;
    }

    @Override
    public Container createContainer(EntityPlayer player) {
        return new BlocksMinerContainer(player, this);
    }

    @Override
    public GuiContainer createGui(EntityPlayer player) {
        return new BlocksMinerGui(new BlocksMinerContainer(player, this), this);
    }

    private void setTargetBlockState() {
        ItemStack itemStack = getStackInSlot(targetSlot);
        targetBlockState = ((ItemBlock)itemStack.getItem()).getBlock().getStateFromMeta(getStackInSlot(targetSlot).getMetadata());
        itemStack.setCount(itemStack.getCount() - 1);
        setInventorySlotContents(targetSlot, itemStack);
    }

    private void mineBlock() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (!fakePlayer.get().canHarvestBlock(targetBlockState)) return;
        List<ItemStack> itemsToDrop = new ArrayList<>();
        itemsToDrop.add(ItemStack.EMPTY);
        boolean hasSilkTouch = EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, getStackInSlot(0)) != 0 ? true: false;
        if (hasSilkTouch) {
            Method getSilkTouchDrop = Block.class.getDeclaredMethod("getSilkTouchDrop", IBlockState.class);
            getSilkTouchDrop.setAccessible(true);
            ItemStack itemStack = (ItemStack) getSilkTouchDrop.invoke(targetBlockState.getBlock(), targetBlockState);
            itemsToDrop.add(itemStack != null ? itemStack: ItemStack.EMPTY);
        }
        else {
            Method getDropMethod = Block.class.getDeclaredMethod("getDrops" , IBlockAccess.class, BlockPos.class, IBlockState.class, int.class);
            getDropMethod.setAccessible(true);
            itemsToDrop = (List<ItemStack>)getDropMethod.invoke(targetBlockState.getBlock(), fakePlayer.get().world, freePosition, targetBlockState, EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, getStackInSlot(0)));
        }
        getStackInSlot(0).damageItem(1, fakePlayer.get());
        net.minecraftforge.event.ForgeEventFactory.fireBlockHarvesting(itemsToDrop, fakePlayer.get().world, pos, targetBlockState, EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, getStackInSlot(0)), 1.0f, hasSilkTouch, fakePlayer.get());
        targetBlockState = Blocks.AIR.getDefaultState();
        for (ItemStack item: itemsToDrop) {
            addItemStackToInventoryInRange(item, 7, 19);
        }
    }
    
    private float getRealBlockHardness() {
        float blockHardness = targetBlockState.getBlockHardness(world, new BlockPos(0, 0, 0));
        float miningSpeed = fakePlayer.get().inventory.getDestroySpeed(targetBlockState);
        if (miningSpeed > 1.0F) {
            int efficiencyModifier = EnchantmentHelper.getEfficiencyModifier(fakePlayer.get());
            ItemStack itemstack = getStackInSlot(0);
            if (efficiencyModifier > 0 && !itemstack.isEmpty()) {
                miningSpeed += (float)(Math.pow(efficiencyModifier, 2) + 1);
            }
        }
        return fakePlayer.get().canHarvestBlock(targetBlockState) ? miningSpeed / 30f / blockHardness : miningSpeed / 100f / blockHardness;
    }

    public ItemStack getTargetBlockAsIS() {
        Block targetBlock = targetBlockState.getBlock();
        return new ItemStack(Item.getItemFromBlock(targetBlock));
    }
}