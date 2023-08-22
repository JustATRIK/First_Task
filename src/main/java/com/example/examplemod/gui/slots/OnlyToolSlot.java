package com.example.examplemod.gui.slots;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;

public class OnlyToolSlot extends Slot {
    public OnlyToolSlot(IInventory p_i1824_1_, int p_i1824_2_, int p_i1824_3_, int p_i1824_4_) {
        super(p_i1824_1_, p_i1824_2_, p_i1824_3_, p_i1824_4_);
    }

    public boolean isItemValid(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        if (stack.getMaxDamage() - stack.getItemDamage() <= 1) {
            return false;
        }
        return !stack.getItem().getToolClasses(stack).isEmpty();
    }
}
