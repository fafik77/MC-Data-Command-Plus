package com.fafik77.concatenate.command;

import com.fafik77.concatenate.command.concat.NbtToStr;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.*;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.command.argument.NbtElementArgumentType;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.DataCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;


/** Data Command Plus includes:
 * PlayersStorageDataObject on 2023-12-30,
 * Concat on 2023-06-14,
 * */
public class DataCmdp {
	private static final SimpleCommandExceptionType MERGE_FAILED_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.data.merge.failed"));
	private static final DynamicCommandExceptionType GET_INVALID_EXCEPTION = new DynamicCommandExceptionType((path) -> Text.translatable("commands.data.get.invalid", new Object[]{path}));
	private static final DynamicCommandExceptionType GET_UNKNOWN_EXCEPTION = new DynamicCommandExceptionType((path) -> Text.translatable("commands.data.get.unknown", new Object[]{path}));
	private static final SimpleCommandExceptionType GET_MULTIPLE_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.data.get.multiple"));
	private static final DynamicCommandExceptionType MODIFY_EXPECTED_OBJECT_EXCEPTION = new DynamicCommandExceptionType((nbt) -> Text.translatable("commands.data.modify.expected_object", new Object[]{nbt}));
	private static final DynamicCommandExceptionType MODIFY_EXPECTED_VALUE_EXCEPTION = new DynamicCommandExceptionType((nbt) -> Text.translatable("commands.data.modify.expected_value", new Object[]{nbt}));
	private static final Dynamic2CommandExceptionType MODIFY_INVALID_SUBSTRING_EXCEPTION = new Dynamic2CommandExceptionType((startIndex, endIndex) -> Text.translatable("commands.data.modify.invalid_substring", new Object[]{startIndex, endIndex}));

	/** includes PlayersStorageDataObject.TYPE_FACTORY */
	public static final List<Function<String, DataCommand.ObjectType>> OBJECT_TYPE_FACTORIES = ImmutableList.of(EntityDataObject.TYPE_FACTORY, BlockDataObject.TYPE_FACTORY, StorageDataObject.TYPE_FACTORY, PlayersStorageDataObject.TYPE_FACTORY);
	public static final List<DataCommand.ObjectType> TARGET_OBJECT_TYPES = OBJECT_TYPE_FACTORIES.stream().map(factory -> factory.apply("target")).collect(ImmutableList.toImmutableList());
	public static final List<DataCommand.ObjectType> SOURCE_OBJECT_TYPES = OBJECT_TYPE_FACTORIES.stream().map(factory -> factory.apply("source")).collect(ImmutableList.toImmutableList());

	/** copied part of the Vanilla code responsible for adding the /data command (decompiled 1.20.4 by FernFlower decompiler) */
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
		LiteralArgumentBuilder literalArgumentBuilder = (LiteralArgumentBuilder)CommandManager.literal("data").requires(source -> source.hasPermissionLevel(2));
		Iterator var2 = TARGET_OBJECT_TYPES.iterator();

		while(var2.hasNext()) {
			DataCommand.ObjectType objectType = (DataCommand.ObjectType)var2.next();
			((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)literalArgumentBuilder.then(objectType.addArgumentsToBuilder(CommandManager.literal("merge"), (builder) -> {
				return builder.then(CommandManager.argument("nbt", NbtCompoundArgumentType.nbtCompound()).executes((context) -> {
					return executeMerge((ServerCommandSource)context.getSource(), objectType.getObject(context), NbtCompoundArgumentType.getNbtCompound(context, "nbt"));
				}));
			}))).then(objectType.addArgumentsToBuilder(CommandManager.literal("get"), (builder) -> {
				return builder.executes((context) -> {
					return executeGet((ServerCommandSource)context.getSource(), objectType.getObject(context));
				}).then(((RequiredArgumentBuilder)CommandManager.argument("path", NbtPathArgumentType.nbtPath()).executes((context) -> {
					return executeGet((ServerCommandSource)context.getSource(), objectType.getObject(context), NbtPathArgumentType.getNbtPath(context, "path"));
				})).then(CommandManager.argument("scale", DoubleArgumentType.doubleArg()).executes((context) -> {
					return executeGet((ServerCommandSource)context.getSource(), objectType.getObject(context), NbtPathArgumentType.getNbtPath(context, "path"), DoubleArgumentType.getDouble(context, "scale"));
				})));
			}))).then(objectType.addArgumentsToBuilder(CommandManager.literal("remove"), (builder) -> {
				return builder.then(CommandManager.argument("path", NbtPathArgumentType.nbtPath()).executes((context) -> {
					return executeRemove((ServerCommandSource)context.getSource(), objectType.getObject(context), NbtPathArgumentType.getNbtPath(context, "path"));
				}));
			}))).then(addModifyArgument((builder, modifier) -> {
				builder.then(CommandManager.literal("insert").then(CommandManager.argument("index", IntegerArgumentType.integer()).then(modifier.create((context, sourceNbt, path, elements) -> {
					return path.insert(IntegerArgumentType.getInteger(context, "index"), sourceNbt, elements);
				})))).then(CommandManager.literal("prepend").then(modifier.create((context, sourceNbt, path, elements) -> {
					return path.insert(0, sourceNbt, elements);
				}))).then(CommandManager.literal("append").then(modifier.create((context, sourceNbt, path, elements) -> {
					return path.insert(-1, sourceNbt, elements);
				}))).then(CommandManager.literal("set").then(modifier.create((context, sourceNbt, path, elements) -> {
					return path.put(sourceNbt, (NbtElement)Iterables.getLast(elements));
				}))).then(CommandManager.literal("merge").then(modifier.create((context, element, path, elements) -> {
					NbtCompound nbtCompound = new NbtCompound();
					Iterator var5 = elements.iterator();

					while(var5.hasNext()) {
						NbtElement nbtElement = (NbtElement)var5.next();
						if (NbtPathArgumentType.NbtPath.isTooDeep(nbtElement, 0)) {
							throw NbtPathArgumentType.TOO_DEEP_EXCEPTION.create();
						}

						if (!(nbtElement instanceof NbtCompound)) {
							throw MODIFY_EXPECTED_OBJECT_EXCEPTION.create(nbtElement);
						}

						NbtCompound nbtCompound2 = (NbtCompound)nbtElement;
						nbtCompound.copyFrom(nbtCompound2);
					}

					Collection<NbtElement> collection = path.getOrInit(element, NbtCompound::new);
					int i = 0;

					NbtCompound nbtCompound3;
					NbtCompound nbtCompound4;
					for(Iterator var13 = collection.iterator(); var13.hasNext(); i += nbtCompound4.equals(nbtCompound3) ? 0 : 1) {
						NbtElement nbtElement2 = (NbtElement)var13.next();
						if (!(nbtElement2 instanceof NbtCompound)) {
							throw MODIFY_EXPECTED_OBJECT_EXCEPTION.create(nbtElement2);
						}

						nbtCompound3 = (NbtCompound)nbtElement2;
						nbtCompound4 = nbtCompound3.copy();
						nbtCompound3.copyFrom(nbtCompound);
					}

					return i;
				})));
			}));
		}
		dispatcher.register(literalArgumentBuilder);
	}


	private static String asString(NbtElement nbt) throws CommandSyntaxException {
		if (nbt.getNbtType().isImmutable()) {
			return nbt.asString();
		} else {
			throw MODIFY_EXPECTED_VALUE_EXCEPTION.create(nbt);
		}
	}

	private static List<NbtElement> mapValues(List<NbtElement> list, Processor processor) throws CommandSyntaxException {
		List<NbtElement> list2 = new ArrayList(list.size());
		Iterator var3 = list.iterator();

		while(var3.hasNext()) {
			NbtElement nbtElement = (NbtElement)var3.next();
			String string = asString(nbtElement);
			list2.add(NbtString.of(processor.process(string)));
		}

		return list2;
	}
	private static String substringInternal(String string, int startIndex, int endIndex) throws CommandSyntaxException {
		if (startIndex >= 0 && endIndex <= string.length() && startIndex <= endIndex) {
			return string.substring(startIndex, endIndex);
		} else {
			throw MODIFY_INVALID_SUBSTRING_EXCEPTION.create(startIndex, endIndex);
		}
	}

	private static String substring(String string, int startIndex, int endIndex) throws CommandSyntaxException {
		int i = string.length();
		int j = getSubstringIndex(startIndex, i);
		int k = getSubstringIndex(endIndex, i);
		return substringInternal(string, j, k);
	}

	private static String substring(String string, int startIndex) throws CommandSyntaxException {
		int i = string.length();
		return substringInternal(string, getSubstringIndex(startIndex, i), i);
	}

	private static int getSubstringIndex(int index, int length) {
		return index >= 0 ? index : length + index;
	}

	private static List<NbtElement> getValues(CommandContext<ServerCommandSource> context, DataCommand.ObjectType objectType) throws CommandSyntaxException {
		DataCommandObject dataCommandObject = objectType.getObject(context);
		return Collections.singletonList(dataCommandObject.getNbt());
	}

	private static List<NbtElement> getValuesByPath(CommandContext<ServerCommandSource> context, DataCommand.ObjectType objectType) throws CommandSyntaxException {
		DataCommandObject dataCommandObject = objectType.getObject(context);
		NbtPathArgumentType.NbtPath nbtPath = NbtPathArgumentType.getNbtPath(context, "sourcePath");
		return nbtPath.get(dataCommandObject.getNbt());
	}

	private static int executeModify(CommandContext<ServerCommandSource> context, DataCommand.ObjectType objectType, ModifyOperation modifier, List<NbtElement> elements) throws CommandSyntaxException {
		DataCommandObject dataCommandObject = objectType.getObject(context);
		NbtPathArgumentType.NbtPath nbtPath = NbtPathArgumentType.getNbtPath(context, "targetPath");
		NbtCompound nbtCompound = dataCommandObject.getNbt();
		int i = modifier.modify(context, nbtCompound, nbtPath, elements);
		if (i == 0) {
			throw MERGE_FAILED_EXCEPTION.create();
		} else {
			dataCommandObject.setNbt(nbtCompound);
			((ServerCommandSource)context.getSource()).sendFeedback(() -> {
				return dataCommandObject.feedbackModify();
			}, true);
			return i;
		}
	}

	private static int executeRemove(ServerCommandSource source, DataCommandObject object, NbtPathArgumentType.NbtPath path) throws CommandSyntaxException {
		NbtCompound nbtCompound = object.getNbt();
		int i = path.remove(nbtCompound);
		if (i == 0) {
			throw MERGE_FAILED_EXCEPTION.create();
		} else {
			object.setNbt(nbtCompound);
			source.sendFeedback(() -> {
				return object.feedbackModify();
			}, true);
			return i;
		}
	}

	public static NbtElement getNbt(NbtPathArgumentType.NbtPath path, DataCommandObject object) throws CommandSyntaxException {
		Collection<NbtElement> collection = path.get(object.getNbt());
		Iterator<NbtElement> iterator = collection.iterator();
		NbtElement nbtElement = (NbtElement)iterator.next();
		if (iterator.hasNext()) {
			throw GET_MULTIPLE_EXCEPTION.create();
		} else {
			return nbtElement;
		}
	}

	private static int executeGet(ServerCommandSource source, DataCommandObject object, NbtPathArgumentType.NbtPath path) throws CommandSyntaxException {
		NbtElement nbtElement = getNbt(path, object);
		int i;
		if (nbtElement instanceof AbstractNbtNumber) {
			i = MathHelper.floor(((AbstractNbtNumber)nbtElement).doubleValue());
		} else if (nbtElement instanceof AbstractNbtList) {
			i = ((AbstractNbtList)nbtElement).size();
		} else if (nbtElement instanceof NbtCompound) {
			i = ((NbtCompound)nbtElement).getSize();
		} else {
			if (!(nbtElement instanceof NbtString)) {
				throw GET_UNKNOWN_EXCEPTION.create(path.toString());
			}

			i = nbtElement.asString().length();
		}

		source.sendFeedback(() -> {
			return object.feedbackQuery(nbtElement);
		}, false);
		return i;
	}

	private static int executeGet(ServerCommandSource source, DataCommandObject object, NbtPathArgumentType.NbtPath path, double scale) throws CommandSyntaxException {
		NbtElement nbtElement = getNbt(path, object);
		if (!(nbtElement instanceof AbstractNbtNumber)) {
			throw GET_INVALID_EXCEPTION.create(path.toString());
		} else {
			int i = MathHelper.floor(((AbstractNbtNumber)nbtElement).doubleValue() * scale);
			source.sendFeedback(() -> {
				return object.feedbackGet(path, scale, i);
			}, false);
			return i;
		}
	}

	private static int executeGet(ServerCommandSource source, DataCommandObject object) throws CommandSyntaxException {
		NbtCompound nbtCompound = object.getNbt();
		source.sendFeedback(() -> {
			return object.feedbackQuery(nbtCompound);
		}, false);
		return 1;
	}

	private static int executeMerge(ServerCommandSource source, DataCommandObject object, NbtCompound nbt) throws CommandSyntaxException {
		NbtCompound nbtCompound = object.getNbt();
		if (NbtPathArgumentType.NbtPath.isTooDeep(nbt, 0)) {
			throw NbtPathArgumentType.TOO_DEEP_EXCEPTION.create();
		} else {
			NbtCompound nbtCompound2 = nbtCompound.copy().copyFrom(nbt);
			if (nbtCompound.equals(nbtCompound2)) {
				throw MERGE_FAILED_EXCEPTION.create();
			} else {
				object.setNbt(nbtCompound2);
				source.sendFeedback(() -> {
					return object.feedbackModify();
				}, true);
				return 1;
			}
		}
	}




	/** Adds concat String option here */
	private static ArgumentBuilder<ServerCommandSource, ?> addModifyArgument(BiConsumer<ArgumentBuilder<ServerCommandSource, ?>, ModifyArgumentCreator> subArgumentAdder) {
		LiteralArgumentBuilder<ServerCommandSource> literalArgumentBuilder = CommandManager.literal("modify");
		Iterator var2 = TARGET_OBJECT_TYPES.iterator();

		while(var2.hasNext()) {
			DataCommand.ObjectType objectType = (DataCommand.ObjectType)var2.next();
			objectType.addArgumentsToBuilder(literalArgumentBuilder, (builder) -> {
				ArgumentBuilder<ServerCommandSource, ?> argumentBuilder = CommandManager.argument("targetPath", NbtPathArgumentType.nbtPath());
				Iterator var4 = SOURCE_OBJECT_TYPES.iterator();

				while(var4.hasNext()) {
					DataCommand.ObjectType objectType2 = (DataCommand.ObjectType)var4.next();

					/** Adds concat String option here */
					subArgumentAdder.accept(argumentBuilder, operation -> objectType2.addArgumentsToBuilder(CommandManager.literal("concat"), builder2 -> ((ArgumentBuilder)builder2.executes(context -> executeModifyConcat(context, objectType, operation, getValues(context, objectType2)))).then( (RequiredArgumentBuilder)CommandManager.argument("sourcePath", NbtPathArgumentType.nbtPath()).executes(context -> executeModifyConcat(context, objectType, operation, getValuesByPath(context, objectType2)))
							.then(CommandManager.argument("separator", StringArgumentType.string()).executes(context -> executeModifyConcat(context, objectType, operation, getValuesByPath(context, objectType2) )))
					)));

					subArgumentAdder.accept(argumentBuilder, (operation) -> {
						return objectType2.addArgumentsToBuilder(CommandManager.literal("from"), (builderx) -> {
							return builderx.executes((context) -> {
								return executeModify(context, objectType, operation, getValues(context, objectType2));
							}).then(CommandManager.argument("sourcePath", NbtPathArgumentType.nbtPath()).executes((context) -> {
								return executeModify(context, objectType, operation, getValuesByPath(context, objectType2));
							}));
						});
					});
					subArgumentAdder.accept(argumentBuilder, (operation) -> {
						return objectType2.addArgumentsToBuilder(CommandManager.literal("string"), (builderx) -> {
							return builderx.executes((context) -> {
								return executeModify(context, objectType, operation, mapValues(getValues(context, objectType2), (value) -> {
									return value;
								}));
							}).then(((RequiredArgumentBuilder)CommandManager.argument("sourcePath", NbtPathArgumentType.nbtPath()).executes((context) -> {
								return executeModify(context, objectType, operation, mapValues(getValuesByPath(context, objectType2), (value) -> {
									return value;
								}));
							})).then(((RequiredArgumentBuilder)CommandManager.argument("start", IntegerArgumentType.integer()).executes((context) -> {
								return executeModify(context, objectType, operation, mapValues(getValuesByPath(context, objectType2), (value) -> {
									return substring(value, IntegerArgumentType.getInteger(context, "start"));
								}));
							})).then(CommandManager.argument("end", IntegerArgumentType.integer()).executes((context) -> {
								return executeModify(context, objectType, operation, mapValues(getValuesByPath(context, objectType2), (value) -> {
									return substring(value, IntegerArgumentType.getInteger(context, "start"), IntegerArgumentType.getInteger(context, "end"));
								}));
							}))));
						});
					});
				}

				subArgumentAdder.accept(argumentBuilder, (modifier) -> {
					return CommandManager.literal("value").then(CommandManager.argument("value", NbtElementArgumentType.nbtElement()).executes((context) -> {
						List<NbtElement> list = Collections.singletonList(NbtElementArgumentType.getNbtElement(context, "value"));
						return executeModify(context, objectType, modifier, list);
					}));
				});
				return builder.then(argumentBuilder);
			});
		}

		return literalArgumentBuilder;
	}


	/** modify concat (handled here) */
	private static int executeModifyConcat(CommandContext<ServerCommandSource> context, DataCommand.ObjectType objectType, ModifyOperation modifier, List<NbtElement> elements) throws CommandSyntaxException {
		 //do not run on empty
		if( elements.isEmpty() ){ throw MERGE_FAILED_EXCEPTION.create(); }
		NbtToStr nbtToStr = new NbtToStr();
		//get required info to modify
		DataCommandObject dataCommandObject = objectType.getObject(context);
		NbtPathArgumentType.NbtPath nbtPath = NbtPathArgumentType.getNbtPath(context, "targetPath");
		@NotNull String separator = "";
		try {
			separator = StringArgumentType.getString(context, "separator");
		} catch (IllegalArgumentException c) { }
		nbtToStr.setSeparator(separator);

		NbtCompound nbtCompound = dataCommandObject.getNbt();
		Iterator iteratorElem = elements.iterator();
		NbtElement nbtElement;

		//append 2 strings (and only those two)
		if( elements.size() == 1 && elements.get(0).getType() == NbtElement.STRING_TYPE ) {
			//try to add previous string
			try {
				List<NbtElement> listFirstEl = nbtPath.get(nbtCompound);
				NbtElement startWithThis;
				if (listFirstEl.iterator().hasNext() && (startWithThis = (listFirstEl.iterator().next())).getType() == NbtElement.STRING_TYPE) {
					nbtToStr.concat(startWithThis);
				}
			} catch (CommandSyntaxException err) {}
		}
		//concat all elements to string
		while(iteratorElem.hasNext()) {
			nbtElement = (NbtElement) iteratorElem.next();
			nbtToStr.concat(nbtElement);
		}

		List<NbtElement> elementsLocal = new ArrayList<NbtElement>();
		elementsLocal.add(NbtString.of(nbtToStr.getResult()));
		int i = modifier.modify(context, nbtCompound, nbtPath, elementsLocal);

		dataCommandObject.setNbt(nbtCompound);
		context.getSource().sendFeedback(() -> dataCommandObject.feedbackModify(), false);
		return nbtToStr.getConcatCount();
	}



	@FunctionalInterface
	static interface Processor {
		public String process(String var1) throws CommandSyntaxException;
	}
	@FunctionalInterface
	static interface ModifyOperation {
		public int modify(CommandContext<ServerCommandSource> var1, NbtCompound var2, NbtPathArgumentType.NbtPath var3, List<NbtElement> var4) throws CommandSyntaxException;
	}
	@FunctionalInterface
	static interface ModifyArgumentCreator {
		public ArgumentBuilder<ServerCommandSource, ?> create(ModifyOperation var1);
	}
}