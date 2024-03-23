package com.fafik77.concatenate.command;

import com.fafik77.concatenate.mixin.AbstractHorseEntityMixin;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.ItemSlotArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.VehicleInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/** Data Command Plus includes:
 * Loot Inventory on 2024-03-17
 */
public class LootInventory {
	static final Dynamic3CommandExceptionType NOT_A_CONTAINER_TARGET_EXCEPTION = new Dynamic3CommandExceptionType((x, y, z) -> {
		return Text.stringifiedTranslatable("commands.item.target.not_a_container", new Object[]{x, y, z});
	});
	static final DynamicCommandExceptionType NO_SUCH_SLOT_TARGET_EXCEPTION = new DynamicCommandExceptionType((slot) -> {
		return Text.stringifiedTranslatable("commands.item.target.no_such_slot", new Object[]{slot});
	});

	private static final DynamicCommandExceptionType NO_INVENTORY_EXCEPTION = new DynamicCommandExceptionType((entityName) -> {
		return Text.stringifiedTranslatable("commands.loot.inventory.no_inventory", new Object[]{entityName});
	});

	public LootInventory(){
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment ignored) {
		dispatcher.register((LiteralArgumentBuilder)addTargetArguments((LiteralArgumentBuilder) CommandManager.literal("loot").requires((source) -> {
			return source.hasPermissionLevel(2);
		}), (builder, constructor) -> {
			return builder.then(CommandManager.literal("inventory").then(CommandManager.argument("target", EntityArgumentType.entity()).executes((context) -> {
				return executeLootInventory(context, EntityArgumentType.getEntity(context, "target"), constructor);
			})));
		}));
	}

	private static <T extends ArgumentBuilder<ServerCommandSource, T>> ArgumentBuilder addTargetArguments(T rootArgument, SourceConstructor sourceConstructor) {
		return rootArgument.then(((LiteralArgumentBuilder)CommandManager.literal("replace").then(CommandManager.literal("entity").then(CommandManager.argument("entities", EntityArgumentType.entities()).then(sourceConstructor.construct(CommandManager.argument("slot", ItemSlotArgumentType.itemSlot()), (context, stacks, messageSender) -> {
			return executeReplace(EntityArgumentType.getEntities(context, "entities"), ItemSlotArgumentType.getItemSlot(context, "slot"), stacks.size(), stacks, messageSender);
		}).then(sourceConstructor.construct(CommandManager.argument("count", IntegerArgumentType.integer(0)), (context, stacks, messageSender) -> {
			return executeReplace(EntityArgumentType.getEntities(context, "entities"), ItemSlotArgumentType.getItemSlot(context, "slot"), IntegerArgumentType.getInteger(context, "count"), stacks, messageSender);
		})))))).then(CommandManager.literal("block").then(CommandManager.argument("targetPos", BlockPosArgumentType.blockPos()).then(sourceConstructor.construct(CommandManager.argument("slot", ItemSlotArgumentType.itemSlot()), (context, stacks, messageSender) -> {
			return executeBlock((ServerCommandSource)context.getSource(), BlockPosArgumentType.getLoadedBlockPos(context, "targetPos"), ItemSlotArgumentType.getItemSlot(context, "slot"), stacks.size(), stacks, messageSender);
		}).then(sourceConstructor.construct(CommandManager.argument("count", IntegerArgumentType.integer(0)), (context, stacks, messageSender) -> {
			return executeBlock((ServerCommandSource)context.getSource(), BlockPosArgumentType.getLoadedBlockPos(context, "targetPos"), IntegerArgumentType.getInteger(context, "slot"), IntegerArgumentType.getInteger(context, "count"), stacks, messageSender);
		})))))).then(CommandManager.literal("insert").then(sourceConstructor.construct(CommandManager.argument("targetPos", BlockPosArgumentType.blockPos()), (context, stacks, messageSender) -> {
			return executeInsert((ServerCommandSource)context.getSource(), BlockPosArgumentType.getLoadedBlockPos(context, "targetPos"), stacks, messageSender);
		}))).then(CommandManager.literal("give").then(sourceConstructor.construct(CommandManager.argument("players", EntityArgumentType.players()), (context, stacks, messageSender) -> {
			return executeGive(EntityArgumentType.getPlayers(context, "players"), stacks, messageSender);
		}))).then(CommandManager.literal("spawn").then(sourceConstructor.construct(CommandManager.argument("targetPos", Vec3ArgumentType.vec3()), (context, stacks, messageSender) -> {
			return executeSpawn((ServerCommandSource)context.getSource(), Vec3ArgumentType.getVec3(context, "targetPos"), stacks, messageSender);
		})));
	}

	private static Inventory getBlockInventory(ServerCommandSource source, BlockPos pos) throws CommandSyntaxException {
		BlockEntity blockEntity = source.getWorld().getBlockEntity(pos);
		if (!(blockEntity instanceof Inventory)) {
			throw NOT_A_CONTAINER_TARGET_EXCEPTION.create(pos.getX(), pos.getY(), pos.getZ());
		} else {
			return (Inventory)blockEntity;
		}
	}
	/** the modified version of getBlockInventory that uses HopperEntity lookup of finding a chest*/
	private static Inventory getOutputInventory(ServerCommandSource source, BlockPos pos) throws CommandSyntaxException {
		BlockEntity blockEntity = source.getWorld().getBlockEntity(pos);
		BlockState blockState = source.getWorld().getBlockState(pos);
		Block block = blockState.getBlock();
		if (!(blockEntity instanceof Inventory)) {
			throw NOT_A_CONTAINER_TARGET_EXCEPTION.create(pos.getX(), pos.getY(), pos.getZ());
		} else {
			Inventory inventory= (Inventory)blockEntity;
			if (inventory instanceof ChestBlockEntity && block instanceof ChestBlock) {
				inventory = ChestBlock.getInventory((ChestBlock)block, blockState, source.getWorld(), pos, true);
			}
			return inventory;
		}
	}
	private static int executeInsert(ServerCommandSource source, BlockPos targetPos, List<ItemStack> stacks, FeedbackMessage messageSender) throws CommandSyntaxException {
		Inventory inventory = getOutputInventory(source, targetPos);
		List<ItemStack> list = Lists.newArrayListWithCapacity(stacks.size());
		Iterator var6 = stacks.iterator();

		while(var6.hasNext()) {
			ItemStack itemStack = (ItemStack)var6.next();
			if (insert(inventory, itemStack.copy())) {
				inventory.markDirty();
				list.add(itemStack);
			}
		}

		messageSender.accept(list);
		return list.size();
	}

	private static boolean insert(Inventory inventory, ItemStack stack) {
		boolean bl = false;

		for(int i = 0; i < inventory.size() && !stack.isEmpty(); ++i) {
			ItemStack itemStack = inventory.getStack(i);
			if (inventory.isValid(i, stack)) {
				if (itemStack.isEmpty()) {
					inventory.setStack(i, stack);
					bl = true;
					break;
				}

				if (itemsMatch(itemStack, stack)) {
					int j = stack.getMaxCount() - itemStack.getCount();
					int k = Math.min(stack.getCount(), j);
					stack.decrement(k);
					itemStack.increment(k);
					bl = true;
				}
			}
		}

		return bl;
	}

	private static int executeBlock(ServerCommandSource source, BlockPos targetPos, int slot, int stackCount, List<ItemStack> stacks, FeedbackMessage messageSender) throws CommandSyntaxException {
		Inventory inventory = getOutputInventory(source, targetPos);
		int i = inventory.size();
		if (slot >= 0 && slot < i) {
			List<ItemStack> list = Lists.newArrayListWithCapacity(stacks.size());

			for(int j = 0; j < Math.min(stackCount, i); ++j) {
				int k = slot + j;
				ItemStack itemStack = j < stacks.size() ? (ItemStack)stacks.get(j) : ItemStack.EMPTY;
				if (inventory.isValid(k, itemStack)) {
					inventory.setStack(k, itemStack);
					list.add(itemStack);
				}
			}

			messageSender.accept(list);
			return list.size();
		} else {
			throw NO_SUCH_SLOT_TARGET_EXCEPTION.create(slot);
		}
	}

	private static boolean itemsMatch(ItemStack first, ItemStack second) {
		return first.getCount() <= first.getMaxCount() && ItemStack.canCombine(first, second);
	}

	private static int executeGive(Collection<ServerPlayerEntity> players, List<ItemStack> stacks, FeedbackMessage messageSender) throws CommandSyntaxException {
		List<ItemStack> list = Lists.newArrayListWithCapacity(stacks.size());
		Iterator var4 = stacks.iterator();

		while(var4.hasNext()) {
			ItemStack itemStack = (ItemStack)var4.next();
			Iterator var6 = players.iterator();

			while(var6.hasNext()) {
				ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)var6.next();
				if (serverPlayerEntity.getInventory().insertStack(itemStack.copy())) {
					list.add(itemStack);
				}
			}
		}

		messageSender.accept(list);
		return list.size();
	}

	private static void replace(Entity entity, List<ItemStack> stacks, int slot, int stackCount, List<ItemStack> addedStacks) {
		for(int i = 0; i < stackCount; ++i) {
			ItemStack itemStack = i < stacks.size() ? (ItemStack)stacks.get(i) : ItemStack.EMPTY;
			StackReference stackReference = entity.getStackReference(slot + i);
			if (stackReference != StackReference.EMPTY && stackReference.set(itemStack.copy())) {
				addedStacks.add(itemStack);
			}
		}

	}

	private static int executeReplace(Collection<? extends Entity> targets, int slot, int stackCount, List<ItemStack> stacks, FeedbackMessage messageSender) throws CommandSyntaxException {
		List<ItemStack> list = Lists.newArrayListWithCapacity(stacks.size());

		for (Entity entity : targets) {
			if (entity instanceof ServerPlayerEntity serverPlayerEntity) {
				replace(entity, stacks, slot, stackCount, list);
				serverPlayerEntity.currentScreenHandler.sendContentUpdates();
			} else {
				replace(entity, stacks, slot, stackCount, list);
			}
		}

		messageSender.accept(list);
		return list.size();
	}

	private static int executeSpawn(ServerCommandSource source, Vec3d pos, List<ItemStack> stacks, FeedbackMessage messageSender) throws CommandSyntaxException {
		ServerWorld serverWorld = source.getWorld();
		stacks.forEach((stack) -> {
			ItemEntity itemEntity = new ItemEntity(serverWorld, pos.x, pos.y, pos.z, stack.copy());
			itemEntity.setToDefaultPickupDelay();
			serverWorld.spawnEntity(itemEntity);
		});
		messageSender.accept(stacks);
		return stacks.size();
	}

	private static void sendDroppedFeedback(ServerCommandSource source, List<ItemStack> stacks) {
		if (stacks.size() == 1) {
			ItemStack itemStack = (ItemStack)stacks.get(0);
			source.sendFeedback(() -> {
				return Text.translatable("commands.drop.success.single", new Object[]{itemStack.getCount(), itemStack.toHoverableText()});
			}, false);
		} else {
			source.sendFeedback(() -> {
				return Text.translatable("commands.drop.success.multiple", new Object[]{stacks.size()});
			}, false);
		}

	}



	private static int executeLootInventory(CommandContext<ServerCommandSource> context, Entity target, Target constructor) throws CommandSyntaxException {
		ServerCommandSource serverCommandSource = (ServerCommandSource)context.getSource();
		List<ItemStack> list = null;
		if(target instanceof ItemEntity){   //item
			DefaultedList<ItemStack> items = DefaultedList.of();
			list= items;
			items.add( ((ItemEntity) target).getStack().copy() );
		}
		else if(target instanceof VehicleInventory){    //cart, boat with chest
			VehicleInventory MooovingInv= (VehicleInventory)target;
			DefaultedList<ItemStack> items = DefaultedList.of();
			list= items;
			MooovingInv.getInventory().forEach(itemStack -> items.add(itemStack.copy()));

		}
		else if(target instanceof PlayerEntity) {   //player
			DefaultedList<ItemStack> items = DefaultedList.of();
			list= items;
			/* ups by mistake I discovered Shadow Item technology (that's why we use for each copy)*/
			((PlayerEntity) target).getInventory().main.forEach(itemStack -> items.add(itemStack.copy()));
			((PlayerEntity) target).getInventory().armor.forEach(itemStack -> items.add(itemStack.copy()));
			((PlayerEntity) target).getInventory().offHand.forEach(itemStack -> items.add(itemStack.copy()));
		}
		else if(target instanceof MerchantEntity) { //villagers
			DefaultedList<ItemStack> items = DefaultedList.of();
			list= items;
			((MerchantEntity) target).getInventory().getHeldStacks().forEach(itemStack -> items.add(itemStack.copy()));
			target.getItemsEquipped().forEach(itemStack -> items.add(itemStack.copy()) );
		}
		else if(target instanceof AbstractHorseEntity) {    //horse, donkey, llama
			DefaultedList<ItemStack> items = DefaultedList.of();
			list= items;
			((AbstractHorseEntityMixin) target).getItems().getHeldStacks().forEach(itemStack -> items.add(itemStack.copy()));
		}


		else if(target instanceof LivingEntity) {   //any other living entity
			DefaultedList<ItemStack> items = DefaultedList.of();
			list= items;
			target.getItemsEquipped().forEach(itemStack -> items.add(itemStack.copy()) );
		}

		if( list==null || list.isEmpty()){
			throw NO_INVENTORY_EXCEPTION.create(target.getName());
		}

		return constructor.accept(context, list, (stacks) -> {
			sendDroppedFeedback(serverCommandSource, stacks);
		});

	}

	@FunctionalInterface
	private interface SourceConstructor {
		ArgumentBuilder<ServerCommandSource, ?> construct(ArgumentBuilder<ServerCommandSource, ?> builder, Target target);
	}

	@FunctionalInterface
	private interface Target {
		int accept(CommandContext<ServerCommandSource> context, List<ItemStack> items, FeedbackMessage messageSender) throws CommandSyntaxException;
	}

	@FunctionalInterface
	interface FeedbackMessage {
		void accept(List<ItemStack> items) throws CommandSyntaxException;
	}
}
