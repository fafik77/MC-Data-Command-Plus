package com.fafik77.concatenate.util;

import com.fafik77.concatenate.command.DataCmdp;
import com.fafik77.concatenate.command.LootInventory;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class ModRegistries {
    public static void registerModStuffs() {
        registerCommands();
    }


	/** Register Data commands+ */
    private static void registerCommands() {
	    singletons.data_LOGGER.info("registering extended /Data command");
        CommandRegistrationCallback.EVENT.register(DataCmdp::register);
        CommandRegistrationCallback.EVENT.register(LootInventory::register);
    }

}
