package com.fafik77.concatenate.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.*;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.DataCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;


public class ConcatStr {
    private static final SimpleCommandExceptionType MERGE_FAILED_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.data.merge.failed"));
    private static final DynamicCommandExceptionType MODIFY_EXPECTED_OBJECT_EXCEPTION = new DynamicCommandExceptionType(nbt -> Text.translatable("commands.data.modify.expected_object", nbt));
    private static final DynamicCommandExceptionType MODIFY_EXPECTED_VALUE_EXCEPTION = new DynamicCommandExceptionType(nbt -> Text.translatable("commands.data.modify.expected_value", nbt));
    public static final List<Function<String, DataCommand.ObjectType>> OBJECT_TYPE_FACTORIES = ImmutableList.of(EntityDataObject.TYPE_FACTORY, BlockDataObject.TYPE_FACTORY, StorageDataObject.TYPE_FACTORY);
    public static final List<DataCommand.ObjectType> TARGET_OBJECT_TYPES = OBJECT_TYPE_FACTORIES.stream().map(factory -> (DataCommand.ObjectType)factory.apply("target")).collect(ImmutableList.toImmutableList());
    public static final List<DataCommand.ObjectType> SOURCE_OBJECT_TYPES = OBJECT_TYPE_FACTORIES.stream().map(factory -> (DataCommand.ObjectType)factory.apply("source")).collect(ImmutableList.toImmutableList());

    ///copied part of the Vanilla code responsible for adding the /data command
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        LiteralArgumentBuilder literalArgumentBuilder = (LiteralArgumentBuilder)CommandManager.literal("data").requires(source -> source.hasPermissionLevel(2));

        for (DataCommand.ObjectType objectType : TARGET_OBJECT_TYPES) {
            literalArgumentBuilder.then(
                    addModifyArgument((builder, modifier) -> (
                        (ArgumentBuilder)(
                            (ArgumentBuilder)(
                                (ArgumentBuilder)(
                                    (ArgumentBuilder)builder
                                        .then(CommandManager.literal("insert").then((ArgumentBuilder<ServerCommandSource, ?>)CommandManager.argument("index", IntegerArgumentType.integer()).then(modifier.create((context, sourceNbt, path, elements) -> path.insert(IntegerArgumentType.getInteger(context, "index"), sourceNbt, elements)))))
                                )
                                .then(CommandManager.literal("prepend").then(modifier.create((context, sourceNbt, path, elements) -> path.insert(0, sourceNbt, elements))))
                            )
                            .then(CommandManager.literal("append").then(modifier.create((context, sourceNbt, path, elements) -> path.insert(-1, sourceNbt, elements))))
                        )
                        .then(CommandManager.literal("set").then(modifier.create((context, sourceNbt, path, elements) -> path.put(sourceNbt, (NbtElement)Iterables.getLast(elements)))))
                    )
                    .then(CommandManager.literal("merge").then(modifier.create((context, element, path, elements) -> {
                NbtCompound nbtCompound = new NbtCompound();
                for (NbtElement nbtElement : elements) {
                    if (NbtPathArgumentType.NbtPath.isTooDeep(nbtElement, 0)) {
                        throw NbtPathArgumentType.TOO_DEEP_EXCEPTION.create();
                    }
                    if (nbtElement instanceof NbtCompound) {
                        NbtCompound nbtCompound2 = (NbtCompound)nbtElement;
                        nbtCompound.copyFrom(nbtCompound2);
                        continue;
                    }
                    throw MODIFY_EXPECTED_OBJECT_EXCEPTION.create(nbtElement);
                }
                List<NbtElement> collection = path.getOrInit(element, NbtCompound::new);
                int i = 0;
                for (NbtElement nbtElement2 : collection) {
                    if (!(nbtElement2 instanceof NbtCompound)) {
                        throw MODIFY_EXPECTED_OBJECT_EXCEPTION.create(nbtElement2);
                    }
                    NbtCompound nbtCompound3 = (NbtCompound)nbtElement2;
                    NbtCompound nbtCompound4 = nbtCompound3.copy();
                    nbtCompound3.copyFrom(nbtCompound);
                    i += nbtCompound4.equals(nbtCompound3) ? 0 : 1;
                }
                return i;
            })))) );
        }
        dispatcher.register(literalArgumentBuilder);
    }

    private static String asString(NbtElement nbt) throws CommandSyntaxException {
        if (nbt.getNbtType().isImmutable()) {
            return nbt.asString();
        }
        throw MODIFY_EXPECTED_VALUE_EXCEPTION.create(nbt);
    }

    private static List<NbtElement> mapValues(List<NbtElement> list, Processor processor) throws CommandSyntaxException {
        ArrayList<NbtElement> list2 = new ArrayList<NbtElement>(list.size());
        for (NbtElement nbtElement : list) {
            String string = asString(nbtElement);
            list2.add(NbtString.of(processor.process(string)));
        }
        return list2;
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

    private static ArgumentBuilder<ServerCommandSource, ?> addModifyArgument(BiConsumer<ArgumentBuilder<ServerCommandSource, ?>, ModifyArgumentCreator> subArgumentAdder) {
        LiteralArgumentBuilder<ServerCommandSource> literalArgumentBuilder = CommandManager.literal("modify");
        for (DataCommand.ObjectType objectType : TARGET_OBJECT_TYPES) {
            objectType.addArgumentsToBuilder(literalArgumentBuilder, builder -> {
                RequiredArgumentBuilder<ServerCommandSource, NbtPathArgumentType.NbtPath> argumentBuilder = CommandManager.argument("targetPath", NbtPathArgumentType.nbtPath());
                for (DataCommand.ObjectType objectType2 : SOURCE_OBJECT_TYPES) {
                    subArgumentAdder.accept(argumentBuilder, operation -> objectType2.addArgumentsToBuilder(CommandManager.literal("concat"), builder2 -> ((ArgumentBuilder)builder2.executes(context -> executeModify(context, objectType, operation, getValues(context, objectType2)))).then( (RequiredArgumentBuilder)CommandManager.argument("sourcePath", NbtPathArgumentType.nbtPath()).executes(context -> executeModify(context, objectType, operation, getValuesByPath(context, objectType2)))
                        .then(CommandManager.argument("separator", StringArgumentType.string()).executes(context -> executeModify(context, objectType, operation, getValuesByPath(context, objectType2) )))
                    )));

                }
                return builder.then(argumentBuilder);
            });
        }
        return literalArgumentBuilder;
    }

    private static class NbtToStr {
        NbtToStr() {}
        String separator = "";
        // concat result
        protected StringBuilder concatOut = new StringBuilder();
        // elements concatenated
        protected int concatCount= 0;

        public void setSeparator(final String separator){this.separator = separator;}
        public int getConcatCount(){return concatCount;}
        public String getResult(){return concatOut.toString();}

        public void concat(final String str) {
            if(concatCount!=0){concatOut.append(separator);} ++concatCount;
            concatOut.append(str);
        }
        public void concat(final NbtElement nbtElement) {
            byte NT = nbtElement.getType();
            switch (NT){
                //Lists: 1 general && 3 specified
                case(NbtElement.LIST_TYPE): {
                    //List can store many different things, recurse through its content
                    final NbtList ElementAsList = (NbtList)nbtElement;
                    for(int i=0; i!= ElementAsList.size(); ++i ){
                        concat( ElementAsList.get(i) );
                    }
                    break;
                }
                case(NbtElement.BYTE_ARRAY_TYPE): {
                    byte[] arr = ((NbtByteArray) nbtElement).getByteArray();
                    for(int i=0; i!=arr.length; ++i){if(concatCount!=0){concatOut.append(separator);}; ++concatCount; concatOut.append(arr[i]);}
                    break;
                }
                case(NbtElement.INT_ARRAY_TYPE): {
                    int[] arr = ((NbtIntArray) nbtElement).getIntArray();
                    for(int i=0; i!=arr.length; ++i){if(concatCount!=0){concatOut.append(separator);}; ++concatCount; concatOut.append(arr[i]);}
                    break;
                }
                case(NbtElement.LONG_ARRAY_TYPE):
                {
                    long[] arr = ((NbtLongArray) nbtElement).getLongArray();
                    for(int i=0; i!=arr.length; ++i){if(concatCount!=0){concatOut.append(separator);}; ++concatCount; concatOut.append(arr[i]);}
                    break;
                }
                //simple single data
                case(NbtElement.BYTE_TYPE):
                case(NbtElement.SHORT_TYPE):
                case(NbtElement.INT_TYPE):
                case(NbtElement.LONG_TYPE):
                case(NbtElement.DOUBLE_TYPE):
                case(NbtElement.FLOAT_TYPE):
                case(NbtElement.STRING_TYPE): {
                    if(concatCount!=0){concatOut.append(separator);} ++concatCount;
                    concatOut.append(nbtElement.asString());
                    break;
                }
                default: {
                    //unknown type = do nothing
                    break;
                }
            }
        }
    }
    // modify concat (handled here)
    private static int executeModify(CommandContext<ServerCommandSource> context, DataCommand.ObjectType objectType, ModifyOperation modifier, List<NbtElement> elements) throws CommandSyntaxException {
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
        if( elements.size() == 1 && elements.get(0).getType() == NbtElement.STRING_TYPE )
            nbtToStr.concat( nbtCompound.getString(nbtPath.toString()) );
        //concat all elements to string
        while(iteratorElem.hasNext()) {
            nbtElement = (NbtElement) iteratorElem.next();
            nbtToStr.concat(nbtElement);
        }

        nbtCompound.putString(nbtPath.toString(), nbtToStr.getResult());

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