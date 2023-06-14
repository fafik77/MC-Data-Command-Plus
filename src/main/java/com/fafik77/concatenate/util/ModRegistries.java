package com.fafik77.concatenate.util;

import com.fafik77.concatenate.command.ConcatStr;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class ModRegistries {
    public static void registerModStuffs() {
        registerCommands();
    }



    private static void registerCommands() {
        CommandRegistrationCallback.EVENT.register(ConcatStr::register);
    }

}
