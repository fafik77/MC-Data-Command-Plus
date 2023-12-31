package com.fafik77.concatenate.mixin;

import com.fafik77.concatenate.util.singletons;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
	@Inject(at = @At("HEAD"), method = "onPlayerConnect")
	private void onPlayerConnect(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
		singletons.playersStorage.loadPlayerData(player);
	}
	@Inject(at = @At("TAIL"), method = "remove")
	private void onPlayerDisconnect(ServerPlayerEntity player, CallbackInfo info) {
		//singletons.playersStorage.savePlayerData(player); //dont call 2 times
		singletons.playersStorage.removePlayerFromList(player);
	}
	@Inject(at = @At("HEAD"), method = "savePlayerData")
	private void onSavePlayerData(ServerPlayerEntity player, CallbackInfo info) {
		singletons.playersStorage.savePlayerData(player);
	}
}
