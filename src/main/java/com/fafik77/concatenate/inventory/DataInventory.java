package com.fafik77.concatenate.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public class DataInventory implements ImplementedInventory {
	/*  see incoming changes 20.4 --> 20.5
		https://minecraft.wiki/w/Commands/loot
		https://minecraft.wiki/w/Commands/item
		https://minecraft.wiki/w/Item_modifier#History
	 */

	private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(256, ItemStack.EMPTY);
	public  DataInventory (){
	}
	/// design decisions, to save or not to save -on exit

	@Override
	public DefaultedList<ItemStack> getItems() {
		return this.inventory;
	}
}
