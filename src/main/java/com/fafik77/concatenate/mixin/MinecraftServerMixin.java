package com.fafik77.concatenate.mixin;

import com.fafik77.concatenate.command.PlayersStorage;
import com.fafik77.concatenate.util.singletons;
import com.mojang.datafixers.DataFixer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
	@Shadow public abstract DataFixer getDataFixer();
	@Shadow @Final protected LevelStorage.Session session;

	@Inject(at = @At("HEAD"), method = "loadWorld")
	private void init(CallbackInfo info) {
		singletons.playersStorage = new PlayersStorage(session, getDataFixer());
	}
}