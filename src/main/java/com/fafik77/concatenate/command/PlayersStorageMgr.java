package com.fafik77.concatenate.command;

import com.google.common.collect.Maps;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public class PlayersStorageMgr {
	private final Map<UUID, NbtCompound> storages = Maps.newHashMap();

	public PlayersStorageMgr(){
	}

	public void removeFromList(@NotNull PlayerEntity player) {
		storages.remove(player.getUuid());
	}

	/** this will put nbt into Map under Uuid */
	public void loadPlayerData(@NotNull UUID playerUuid, NbtCompound nbt){
		storages.put(playerUuid, nbt);
	}

	public NbtCompound get(@NotNull UUID playerUuid) {
		return storages.getOrDefault(playerUuid, new NbtCompound());  //.clone() //might be necessary
	}

    public void set(@NotNull UUID playerUuid, NbtCompound nbt) {
	    storages.put(playerUuid,nbt);
    }


}
