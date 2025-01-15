package com.fafik77.concatenate.command;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.Iterator;

/*new on 2025-01-16*/
/** this command Deletes the entity (not a Player)
 * entities are removed not killed, no events are emitted, no loot is dropped
 */
public class DeleteKillEntity {
	public DeleteKillEntity(){}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment ignored) {
		dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("deletekill").requires((source) -> {
			return source.hasPermissionLevel(2);
		})).executes((context) -> {
			return execute((ServerCommandSource)context.getSource(), ImmutableList.of(((ServerCommandSource)context.getSource()).getEntityOrThrow()));
		})).then(CommandManager.argument("targets", EntityArgumentType.entities()).executes((context) -> {
			return execute((ServerCommandSource)context.getSource(), EntityArgumentType.getEntities(context, "targets"));
		})));
	}


	private static int execute(ServerCommandSource source, Collection<? extends Entity> targets) {
		Iterator var2 = targets.iterator();
		int deleted=0;
		while(var2.hasNext()) {
			Entity entity = (Entity)var2.next();
			if(entity instanceof PlayerEntity){
				continue;
			}
			++deleted;
			entity.discard();   //source.getWorld()
		}

		if (deleted == 1) {
			source.sendFeedback(() -> {
				return Text.translatable("commands.deletekill.success.single", new Object[]{((Entity)targets.iterator().next()).getDisplayName()});
			}, true);
		} else {
			int finalDeleted = deleted;
			source.sendFeedback(() -> {
				return Text.translatable("commands.deletekill.success.multiple", new Object[]{finalDeleted});
			}, true);
		}

		return deleted;
	}

}
