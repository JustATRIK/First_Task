package com.example.examplemod.gui.slots;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public class OnlyBlockSlot extends Slot {
    public OnlyBlockSlot(IInventory p_i1824_1_, int p_i1824_2_, int p_i1824_3_, int p_i1824_4_) {
        super(p_i1824_1_, p_i1824_2_, p_i1824_3_, p_i1824_4_);
    }

    public boolean isItemValid(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return true;
        }
        return itemStack.getItem() instanceof ItemBlock;
    }
}
