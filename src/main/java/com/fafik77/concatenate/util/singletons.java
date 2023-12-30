package com.fafik77.concatenate.util;

import com.fafik77.concatenate.command.PlayersStorage;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** One Class to rule them all (like 'MinecraftServer' only one)
 */
public class singletons {
	public static final Logger data_LOGGER = LoggerFactory.getLogger("Data Command +");
	public static PlayersStorage playersStorage;
}
