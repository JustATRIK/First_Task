package com.example.examplemod.gui;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.block.tiles.BlocksMinerTileEntity;
import com.example.examplemod.gui.slots.OnlyBlockSlot;
import com.example.examplemod.gui.slots.OnlyTakeSlot;
import com.example.examplemod.gui.slots.OnlyToolSlot;
import com.example.examplemod.utils.packets.EnergyAndProgressSyncPacket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

public class BlocksMinerContainer extends Container {

    BlocksMinerTileEntity tileEntity;

    public BlocksMinerContainer(EntityPlayer player, BlocksMinerTileEntity tileEntity) {
        this.tileEntity = tileEntity;
        this.addSlotToContainer(new OnlyToolSlot(tileEntity, 0, 123, 38)); //1) index 2) xPos 3) yPos
        //слоты для входа
        for (int column = 0; column < 6; column++) {
            int index = 1 + column;
            int xPos = column * 18 + 8;
            int yPos = 16;
            this.addSlotToContainer(new OnlyBlockSlot(tileEntity, index, xPos, yPos));
        }
        //слоты для выхода
        for (int row = 1; row < 3; row++) {
            for (int column = 0; column < 6; column++) {
                int index = row * 6 + 1 + column;
                int xPos = column * 18 + 8;
                int yPos = row * 18 + 20;
                this.addSlotToContainer(new OnlyTakeSlot(tileEntity, index, xPos, yPos));
            }
        }
        this.addSlotToContainer(new OnlyTakeSlot(tileEntity, 19, 123, 56));
        initPlayerInventory(player);
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    private void initPlayerInventory(EntityPlayer player) {
        IInventory playerInventory = player.inventory;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int index = column + row * 9 + 9;
                int xPos = column * 18 + 8;
                int yPos = row * 18 + 84;
                this.addSlotToContainer(new Slot(playerInventory, index, xPos, yPos));
            }
        }
        for (int column = 0; column < 9; column++) {
            int xPos = column * 18 + 8;
            this.addSlotToContainer(new Slot(playerInventory, column, xPos, 142));
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            if (index < 20) {
                if (!mergeItemStack(itemstack1, 20, 20 + 4 * 9, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onSlotChanged();
            }
            else {
                if (index < 20 + 9 * 3 && !mergeItemStack(itemstack1, 0, 7, false)){
                    return ItemStack.EMPTY;
                }
                else if (!mergeItemStack(itemstack1, 0, 7, false)){
                    return ItemStack.EMPTY;
                }
            }
            if (itemstack1.getCount() == 0) {
                slot.putStack(ItemStack.EMPTY);
            }
            else {
                slot.onSlotChanged();
            }
            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(playerIn, itemstack1);
        }
        return itemstack;
    }


    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (!tileEntity.getWorld().isRemote) {
            if (tileEntity.getStoredEnergy() != tileEntity.getClientIntData(0) || tileEntity.energyConsuming != tileEntity.getClientIntData(1) || tileEntity.curBlockDamageMP != tileEntity.getClientFloatData(0)) {
                tileEntity.setClientIntData(0, tileEntity.getStoredEnergy());
                tileEntity.setClientIntData(1, tileEntity.energyConsuming);
                tileEntity.setClientIntData(2, tileEntity.storedXP);
                tileEntity.setClientFloatData(0, tileEntity.curBlockDamageMP);
            }
            ExampleMod.NETWORK.sendToAll(new EnergyAndProgressSyncPacket(tileEntity.getPos(), tileEntity.getStoredEnergy(), tileEntity.curBlockDamageMP, tileEntity.energyConsuming, tileEntity.storedXP));
        }
    }
}
