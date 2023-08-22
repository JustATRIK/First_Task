package com.example.examplemod.block.tiles;

import com.example.examplemod.CommonProxy;
import com.example.examplemod.ExampleMod;
import com.example.examplemod.gui.BlocksMinerContainer;
import com.example.examplemod.gui.BlocksMinerGui;
import com.example.examplemod.utils.BlocksMinerFakePlayer;
import com.example.examplemod.utils.IGuiTile;
import com.example.examplemod.utils.energy.ModEnergyStorage;
import com.example.examplemod.utils.packets.EnergyAndProgressSyncPacket;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class BlocksMinerTileEntity extends TileEntity implements ITickable, ISidedInventory, IGuiTile {
    //Публичные поля
    final static int REAL_ENERGY_CONSUMING = 10;
    public int energyConsuming = 0;
    public int storedXP = 0;
    public NonNullList<ItemStack> items = NonNullList.withSize(20, ItemStack.EMPTY);
    public float curBlockDamageMP = 0;
    public WeakReference<BlocksMinerFakePlayer> fakePlayer;
    //Приватные поля
    private static final int MAX_EXP_STORED = 10000;
    private final net.minecraftforge.common.capabilities.CapabilityDispatcher capabilities;
    private final IItemHandler itemStackHandler = new SidedInvWrapper(this, EnumFacing.WEST);
    private final ModEnergyStorage energyStorage = new ModEnergyStorage(20000, 200, REAL_ENERGY_CONSUMING);
    private boolean startedMining;
    private int targetSlot = -1;
    private int clientEnergyConsuming = -1;
    private int clientStoredEnergy = -1;
    private int clientStoredXP = -1;
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
        if (fakePlayer == null) {
            fakePlayer = BlocksMinerFakePlayer.initFakePlayer((WorldServer) world, UUID.randomUUID(), "blocks_miner");
            if (fakePlayer != null) {
                fakePlayer.get().setPosition(pos.getX(), pos.getY(), pos.getZ());
                setFakePlayerMainHandItem(items.get(0));
            }
            return;
        }
        else if (freePosition != fakePlayer.get().getPosition()){
            freePosition = fakePlayer.get().getPosition();
        }
        if (!startedMining) {
            targetSlot = getFirstFullStackInRangeAsBlock(1, 7);
            if (targetSlot == -1) return;
            if (!(getStackInSlot(targetSlot).getItem() instanceof ItemBlock)) return;
            setTargetBlockState();
            if (!canMine()) return;
            ItemStack itemStack = getStackInSlot(targetSlot);
            itemStack.setCount(itemStack.getCount() - 1);
            setInventorySlotContents(targetSlot, itemStack);
            startedMining = true;
        }
        if ((curBlockDamageMP += getRealBlockHardness()) >= 1.0F) {
            startedMining = false;
            curBlockDamageMP = 0;
            energyConsuming = 0;
            List<ItemStack> itemsToDrop;
            try {
                itemsToDrop = getDrop();
                if (getStackInSlot(0).getMaxDamage() - getStackInSlot(0).getItemDamage() > 1) getStackInSlot(0).damageItem(1, fakePlayer.get());
                BlockPos eventPos = pos.add(0, -10000, 0);
                net.minecraftforge.event.ForgeEventFactory.fireBlockHarvesting(itemsToDrop, fakePlayer.get().world, eventPos, targetBlockState, EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, getStackInSlot(0)), 1.0f, EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, getStackInSlot(0)) != 0 ? true: false, fakePlayer.get());
                storedXP += targetBlockState.getBlock().getExpDrop(targetBlockState, world, eventPos, EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, getStackInSlot(0)));
                List<EntityXPOrb> xpOrbs = world.getEntitiesWithinAABB(EntityXPOrb.class, new AxisAlignedBB(eventPos.add(-1, -1, -1), eventPos.add(1, 1, 1)));
                for (EntityXPOrb xpOrb:xpOrbs) {
                    storedXP += xpOrb.xpValue;
                    world.removeEntity(xpOrb);
                }
                storedXP = Math.min(storedXP, MAX_EXP_STORED);
                targetBlockState = Blocks.AIR.getDefaultState();
                for (ItemStack item: itemsToDrop) {
                    addItemStackToInventoryInRange(item, 7, 19);
                }
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
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(energyStorage);
        }
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) itemStackHandler;
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY || capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
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
        compound.setInteger("storedXP", storedXP);
        compound.setFloat("progress", curBlockDamageMP);
        compound.setBoolean("startedMining", startedMining);
        Block targetBlock = targetBlockState.getBlock();
        ItemStack targetBlockAsIS = new ItemStack(targetBlock);
        NBTTagCompound newNbt = new NBTTagCompound();
        targetBlockAsIS.writeToNBT(newNbt);
        compound.setTag("targetBlock", newNbt);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        energyStorage.setEnergy(compound.getInteger("energy"));
        storedXP = compound.getInteger("storedXP");
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
        if (isItemValidForSlot(index, stack)) items.set(index, stack);
        if (!world.isRemote && index == 0 && fakePlayer.get() != null) setFakePlayerMainHandItem(stack);
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
        if (stack.isEmpty()) {
            return true;
        }
        if (!stack.getItem().getToolClasses(stack).isEmpty() && (index == 0 || index == 19)) {
            return true;
        }
        if (index > 6) {
            return true;
        }
        if (index == 0 && stack.getMaxDamage() - stack.getItemDamage() <= 1) {
            return false;
        }
        return index > 0 && index <= 6 && stack.getItem() instanceof ItemBlock;
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
        return new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19};
    }

    @Override
    public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction) {
        return isItemValidForSlot(index, itemStackIn);
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
        return index > 6;
    }

    public void addItemStackToInventory(ItemStack itemStackIn) {
        addItemStackToInventoryInRange(itemStackIn, 1, items.size());
    }

    public void addItemStackToInventoryInRange(ItemStack itemStackIn, int startIndex, int endIndex) {
        int toDrop = 1;
        for (int i = startIndex; i < endIndex; i++) {
            ItemStack itemStack = items.get(i);
            if (canMergeStacks(itemStack, itemStackIn)) {
                toDrop = Math.max(itemStack.getCount() + itemStackIn.getCount() - 64, 0);
                itemStack.setCount(itemStack.getCount() + itemStackIn.getCount() - toDrop);
                itemStackIn.setCount(toDrop);
            }
        }
        if (toDrop > 0) {
            int firstEmptyStack = getFirstEmptyStackInRange(startIndex, endIndex);
            if (firstEmptyStack != -1) {
                setInventorySlotContents(firstEmptyStack, itemStackIn);
            }
        }
    }

    private boolean canAddISToInventoryInRange(ItemStack itemStackIn, int startIndex, int endIndex) {
        int toDrop = 1;
        for (int i = startIndex; i < endIndex; i++) {
            ItemStack itemStack = items.get(i);
            if (canMergeStacks(itemStack, itemStackIn)) {
                toDrop = Math.max(itemStack.getCount() + itemStackIn.getCount() - 64, 0);
            }
        }
        if (toDrop > 0) {
            int firstEmptyStack = getFirstEmptyStackInRange(startIndex, endIndex);
            if (firstEmptyStack != -1) {
                return true;
            }
        }
        return toDrop == 0;
    }

    public int getFirstEmptyStack() {
        return getFirstFullStackInRange(1, items.size());
    }

    public int getFirstFullStackInRangeAsBlock(int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex; i++) {
            if (!items.get(i).isEmpty() && getStackInSlot(i).getItem() instanceof ItemBlock) {
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
            case 2: return this.clientStoredXP;
        }
        return -1;
    }

    public void setClientIntData(int ID, int value){
        switch (ID){
            case 0: this.clientStoredEnergy = value;
            case 1: this.clientEnergyConsuming = value;
            case 2: this.clientStoredXP = value;
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
    }

    private List<ItemStack> getDrop() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (!fakePlayer.get().canHarvestBlock(targetBlockState)) return Arrays.asList(ItemStack.EMPTY);
        List<ItemStack> itemsToDrop = new ArrayList<>(Arrays.asList(ItemStack.EMPTY));
        boolean hasSilkTouch = EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, getStackInSlot(0)) != 0 ? true: false;
        if (hasSilkTouch) {
            if (CommonProxy.GET_SILK_TOUCH_DROP == null) {
                CommonProxy.GET_SILK_TOUCH_DROP = Block.class.getMethod("getSilkTouchDrop", IBlockState.class);
                CommonProxy.GET_SILK_TOUCH_DROP.setAccessible(true);
            }
            ItemStack itemStack = (ItemStack) CommonProxy.GET_SILK_TOUCH_DROP.invoke(targetBlockState.getBlock(), targetBlockState);
            itemsToDrop.add(itemStack != null ? itemStack: ItemStack.EMPTY);
        }
        else {
            if (CommonProxy.GET_DROP_METHOD == null) {
                CommonProxy.GET_DROP_METHOD = Block.class.getMethod("getDrops" , IBlockAccess.class, BlockPos.class, IBlockState.class, int.class);
                CommonProxy.GET_DROP_METHOD.setAccessible(true);
            }
            itemsToDrop = (List<ItemStack>) CommonProxy.GET_DROP_METHOD.invoke(targetBlockState.getBlock(), fakePlayer.get().world, freePosition, targetBlockState, EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, getStackInSlot(0)));
        }
        return itemsToDrop;
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
        return miningSpeed / 30f / blockHardness;
    }

    public ItemStack getTargetBlockAsIS() {
        Block targetBlock = targetBlockState.getBlock();
        return new ItemStack(targetBlock);
    }

    private boolean canMine() {
        if (getStackInSlot(0).getMaxDamage() - getStackInSlot(0).getItemDamage() <= 1 && !(getStackInSlot(0).isEmpty())) {
            if (getStackInSlot(19) == ItemStack.EMPTY) {
                setInventorySlotContents(19, removeStackFromSlot(0));
            }
            return false;
        }
        if (targetBlockState.getBlock().getHarvestTool(targetBlockState) != null) {
            if (targetBlockState.getBlock().getHarvestLevel(targetBlockState) > getStackInSlot(0).getItem().getHarvestLevel(getStackInSlot(0), targetBlockState.getBlock().getHarvestTool(targetBlockState), fakePlayer.get(), targetBlockState) && !targetBlockState.getMaterial().isToolNotRequired()) {
                return false;
            }
        }
        if (getFirstFullStackInRange(1, 7) == -1 && targetBlockState == null && !startedMining) {
            curBlockDamageMP = 0;
            energyConsuming = 0;
            return false;
        }
        if (getStoredEnergy() < energyConsuming) {
            energyConsuming = 0;
            return false;
        }
        List<ItemStack> itemStacks;
        try {
            itemStacks = getDrop();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        for (ItemStack itemStack: itemStacks) {
            if (!canAddISToInventoryInRange(itemStack, 7, 19)) {
                return false;
            }
        }
        return true;
    }
}