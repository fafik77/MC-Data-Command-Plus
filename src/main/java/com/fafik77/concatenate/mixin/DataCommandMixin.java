package com.fafik77.concatenate.mixin;

import net.minecraft.server.command.DataCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DataCommand.class)
public class DataCommandMixin {

//	@Inject(at = @At("STORE"), method = "OBJECT_TYPE_FACTORIES")
//	private void OBJECT_TYPE_FACTORIES_insert(CallbackInfo info) {
//		// This code is injected into the start of ...()
//	}

}
