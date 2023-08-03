package com.example.examplemod.gui;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.block.tiles.BlocksMinerTileEntity;
import com.example.examplemod.utils.packets.EnergyAndProgressSyncPacket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class BlocksMinerContainer extends Container {
    BlocksMinerTileEntity tileEntity;

    public BlocksMinerContainer(EntityPlayer player, BlocksMinerTileEntity tileEntity) {
        this.tileEntity = tileEntity;
        this.addSlotToContainer(new Slot(tileEntity, 0, 18 * 4 + 18 * 3 - 3, 18 * 2));
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 6; ++j) {
                this.addSlotToContainer(new Slot(tileEntity, i * 6 + 1 + j , j * 18 + 8, i * 18 + 18));
            }
        }
        initPlayerInventory(player);
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    private void initPlayerInventory(EntityPlayer player) {
        IInventory playerInventory = player.inventory;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlotToContainer(new Slot(playerInventory, j + i * 9 + 9, j * 18 + 8, i * 18 + 84));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlotToContainer(new Slot(playerInventory, i, i * 18 + 8, 4 + 54 + 84));
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            if (index < 19) {
                if (!this.mergeItemStack(itemstack1, 19, 18 + 4 * 9 + 1, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onSlotChanged();
            }
            else {
                if (index < 19 + 9 * 3){
                    if (!this.mergeItemStack(itemstack1, 0, 19, false)){
                        return ItemStack.EMPTY;
                    }
                } else if (index < 55 && !this.mergeItemStack(itemstack1, 19, 18 + 9 * 3, false)){
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
                tileEntity.setClientFloatData(0, tileEntity.curBlockDamageMP);
                ExampleMod.NETWORK.sendToAll(new EnergyAndProgressSyncPacket(tileEntity.getPos(), tileEntity.getStoredEnergy(), tileEntity.curBlockDamageMP, tileEntity.energyConsuming));
            }
        }
    }
}
