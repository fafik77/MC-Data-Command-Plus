package com.fafik77.concatenate.command;

import com.fafik77.concatenate.util.singletons;
import com.mojang.datafixers.DataFixer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.Util;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class PlayersStorage {
	@Nullable
	public final PlayersStorageMgr playersStorageMgr;
	public final File playerDataDir;
	protected final DataFixer dataFixer;

	/** init Mod data on server side (access singletons::)
	 */
	public PlayersStorage(LevelStorage.Session session, DataFixer dataFixer) {
		this.playersStorageMgr= new PlayersStorageMgr();
		this.playerDataDir = session.getDirectory(WorldSavePath.PLAYERDATA).toFile();
			this.playerDataDir.mkdirs();
		this.dataFixer = dataFixer;
	}

	/** returns file name for player "_pds" Player Data Storage */
	public static String getPlayerFilePds(String playerUuid){
		return playerUuid + "_pds";
	}

	public void savePlayerData(PlayerEntity player) {
		try {
			final String PlayerFile= getPlayerFilePds(player.getUuidAsString());
			NbtCompound nbtCompound = playersStorageMgr.get(player.getUuid());
			NbtCompound nbtCompound_data = new NbtCompound();
			nbtCompound_data.put("data", nbtCompound);

			File file = File.createTempFile(PlayerFile + "-", ".dat", this.playerDataDir);
			NbtIo.writeCompressed(nbtCompound_data, file);
			File file2 = new File(this.playerDataDir, PlayerFile + ".dat");
			File file3 = new File(this.playerDataDir, PlayerFile + ".dat_old");
			Util.backupAndReplace(file2, file, file3);

		} catch (Exception exception) {
			singletons.data_LOGGER.warn("Failed to save player_pds data for {}", player.getName().getString());
		}
	}
	public void loadPlayerData(PlayerEntity player) {
		final String PlayerFile= getPlayerFilePds(player.getUuidAsString());
		NbtCompound nbtCompound = null;
		try {
			File file = new File(this.playerDataDir, PlayerFile + ".dat");
			if (file.exists() && file.isFile()) {
				nbtCompound = NbtIo.readCompressed(file);
			}
		} catch (Exception exception) {
			singletons.data_LOGGER.warn("Failed to load player_pds data for {}", player.getName().getString());
		}
		if (nbtCompound != null) {
			playersStorageMgr.loadPlayerData(player.getUuid(),nbtCompound.getCompound("data"));
		}
	}
	public void removePlayerFromList(PlayerEntity player) {
		playersStorageMgr.removeFromList(player);
	}
}
