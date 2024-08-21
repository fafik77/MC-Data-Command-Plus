package com.fafik77.concatenate.command;

import com.fafik77.concatenate.command.ArgumentType.MathArgumentType;
import com.fafik77.concatenate.command.concat.NbtToStr;
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
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.DataCommandObject;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.DataCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;


/** Data Command Plus includes:
 * Loot Inventory on 2024-03-17,
 * PlayersStorageDataObject on 2023-12-30,
 * Concat on 2023-06-14,
 * */
public class DataCmdp {
	private static final SimpleCommandExceptionType MERGE_FAILED_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.data.merge.failed"));
	private static final DynamicCommandExceptionType MODIFY_EXPECTED_OBJECT_EXCEPTION = new DynamicCommandExceptionType((nbt) -> Text.translatable("commands.data.modify.expected_object", new Object[]{nbt}));
	private static final DynamicCommandExceptionType GET_UNKNOWN_EXCEPTION = new DynamicCommandExceptionType((path) -> {return Text.stringifiedTranslatable("commands.data.get.unknown", new Object[]{path});});
	private static final SimpleCommandExceptionType GET_MULTIPLE_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.data.get.multiple"));
	private static final DynamicCommandExceptionType GET_INVALID_EXCEPTION = new DynamicCommandExceptionType(path -> Text.stringifiedTranslatable("commands.data.get.invalid", path));
	private static final SimpleCommandExceptionType INVALID_OPERATION = new SimpleCommandExceptionType(Text.translatable("arguments.operation.invalid"));
	private static final SimpleCommandExceptionType DIVISION_ZERO_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("arguments.operation.div0"));


	/** includes PlayersStorageDataObject.TYPE_FACTORY, & concat operation
	copied part of the Vanilla code responsible for adding the /data command (decompiled 1.20.4 by FernFlower decompiler) */
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
		LiteralArgumentBuilder literalArgumentBuilder = (LiteralArgumentBuilder)CommandManager.literal("data").requires(source -> source.hasPermissionLevel(2));

		(((literalArgumentBuilder))).then(addModifyArgument((builder, modifier) -> {
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
	/** Data Math on 2024-08-20*/
		Iterator var2 = DataCommand.TARGET_OBJECT_TYPES.iterator();
		while(var2.hasNext()) {
			DataCommand.ObjectType objectType = (DataCommand.ObjectType) var2.next();
			(literalArgumentBuilder).then(objectType.addArgumentsToBuilder(CommandManager.literal("modify"), (builder) -> {
				return builder.then(CommandManager.argument("targetPath", NbtPathArgumentType.nbtPath()).then(CommandManager.literal("operation").then(CommandManager.argument("operation", MathArgumentType.operation()).then(CommandManager.argument("value", DoubleArgumentType.doubleArg())
						.executes((context) -> {
							return executeDataMath(context, objectType.getObject(context), NbtPathArgumentType.getNbtPath(context, "targetPath"), MathArgumentType.getOperation(context, "operation"), DoubleArgumentType.getDouble(context, "value"));
				})))));
			}));
		}

		dispatcher.register(literalArgumentBuilder);
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


	/** Adds concat String option here */
	private static ArgumentBuilder<ServerCommandSource, ?> addModifyArgument(BiConsumer<ArgumentBuilder<ServerCommandSource, ?>, ModifyArgumentCreator> subArgumentAdder) {
		LiteralArgumentBuilder<ServerCommandSource> literalArgumentBuilder = CommandManager.literal("modify");
		Iterator var2 = DataCommand.TARGET_OBJECT_TYPES.iterator();

		while(var2.hasNext()) {
			DataCommand.ObjectType objectType = (DataCommand.ObjectType)var2.next();
			objectType.addArgumentsToBuilder(literalArgumentBuilder, (builder) -> {
				ArgumentBuilder<ServerCommandSource, ?> argumentBuilder = CommandManager.argument("targetPath", NbtPathArgumentType.nbtPath());
				Iterator var4 = DataCommand.SOURCE_OBJECT_TYPES.iterator();

				while(var4.hasNext()) {
					DataCommand.ObjectType objectType2 = (DataCommand.ObjectType)var4.next();

					/** Adds concat String option here */
					subArgumentAdder.accept(argumentBuilder, operation -> objectType2.addArgumentsToBuilder(CommandManager.literal("concat"), builder2 -> ((ArgumentBuilder)builder2.executes(context -> executeModifyConcat(context, objectType, operation, getValues(context, objectType2)))).then( (RequiredArgumentBuilder)CommandManager.argument("sourcePath", NbtPathArgumentType.nbtPath()).executes(context -> executeModifyConcat(context, objectType, operation, getValuesByPath(context, objectType2)))
							.then(CommandManager.argument("separator", StringArgumentType.string()).executes(context -> executeModifyConcat(context, objectType, operation, getValuesByPath(context, objectType2) )))
					)));
				}

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
		} catch (IllegalArgumentException c) { separator = ""; }
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

	/** Data Math (handled here) 2024-08-21*/
	private static int executeDataMath(CommandContext<ServerCommandSource> context, DataCommandObject object, NbtPathArgumentType.NbtPath path, String operation, double value) throws CommandSyntaxException {
		Collection<NbtElement> collection = path.get(object.getNbt());

		Iterator<NbtElement> iterator = collection.iterator();
		int resultCount = collection.size();
		if(0==resultCount) throw GET_UNKNOWN_EXCEPTION.create(path.toString());
		if(resultCount>1) throw GET_MULTIPLE_EXCEPTION.create();

	//	List<NbtElement> list = Collections.emptyList();

		while(iterator.hasNext()) { //ready for Multiple, but there is no easy way to sync it back
			NbtElement nbtElement = (NbtElement) iterator.next();
			if (nbtElement instanceof AbstractNbtNumber) {
				NbtElement nbtElementOut = switch (nbtElement) {
					case NbtDouble nbtDouble -> NbtDouble.of(operationOnDouble(nbtDouble.doubleValue(), operation, value));
					case NbtFloat nbtFloat -> NbtDouble.of(operationOnDouble(nbtFloat.doubleValue(), operation, value));

					case NbtInt nbtInt -> NbtInt.of( (int) operationOnLong(nbtInt.longValue(), operation, value));
					case NbtShort nbtShort -> NbtInt.of( (short) operationOnLong(nbtShort.longValue(), operation, value));
					case NbtByte nbtByte -> NbtByte.of( (byte) operationOnLong(nbtByte.longValue(), operation, value));
					case NbtLong nbtLong -> NbtLong.of(operationOnLong(nbtLong.longValue(), operation, value));
					default ->  //NaN
						throw GET_INVALID_EXCEPTION.create(path.toString());
				};
		//		list.addLast(nbtElementOut);
				path.put(object.getNbt(), nbtElementOut);
			}
		}
		return resultCount;
	}

	private static long operationOnLong(long a, String operation, double b) throws CommandSyntaxException {
		long long_b= (long) b; //we have native double input, cast here to long for most math operations
		return switch (operation){
			case "<>" -> long_b; //both do set to new
			case "=" -> long_b;
			case "+=" -> a + long_b;
			case "-=" -> a - long_b;
			case "*=" -> a * long_b;
			case "/=" -> {
				if (b == 0) {
					throw DIVISION_ZERO_EXCEPTION.create();
				} else {
					yield(a / long_b);
				}
			}
			case "%=" -> {
				if (b == 0) {
					throw DIVISION_ZERO_EXCEPTION.create();
				} else {
					yield a % long_b;
				}
			}
			case "^=" -> (long) Math.pow( (double) a,b); //except here where we expect 2 doubles
			case "<" -> Math.min(a,long_b);
			case ">" -> Math.max(a,long_b);
			default -> throw INVALID_OPERATION.create();
		};
	}
	private static Double operationOnDouble(Double a, String operation, Double b) throws CommandSyntaxException {
		return switch (operation){
			case "<>" -> b; //both do set to new
			case "=" -> b;
			case "+=" -> a + b;
			case "-=" -> a - b;
			case "*=" -> a * b;
			case "/=" -> {
				if (b == 0) {
					throw DIVISION_ZERO_EXCEPTION.create();
				} else {
					yield(a / b);
				}
			}
			case "%=" -> {
				if (b == 0) {
					throw DIVISION_ZERO_EXCEPTION.create();
				} else {
					yield a % b;
				}
			}
			case "^=" -> Math.pow(a,b);
			case "<" -> Math.min(a,b);
			case ">" -> Math.max(a,b);
			default -> throw INVALID_OPERATION.create();
		};
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