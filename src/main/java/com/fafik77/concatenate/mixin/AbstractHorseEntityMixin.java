package com.fafik77.concatenate.mixin;

import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.inventory.SimpleInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractHorseEntity.class)
public interface AbstractHorseEntityMixin {
	@Accessor("items")
	SimpleInventory getItems();
}
