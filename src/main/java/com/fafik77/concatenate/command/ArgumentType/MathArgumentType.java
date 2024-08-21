package com.fafik77.concatenate.command.ArgumentType;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class MathArgumentType implements ArgumentType<String> {
	//see ArgumentTypes

	private static final Collection<String> EXAMPLES = Arrays.asList("=", ">", "<");
	private static final SimpleCommandExceptionType INVALID_OPERATION = new SimpleCommandExceptionType(Text.translatable("arguments.operation.invalid"));

	public static MathArgumentType operation(){
		return new MathArgumentType();
	}
	public static String getOperation(CommandContext<ServerCommandSource> context, String name) {
		return context.getArgument(name, String.class);
	}


	@Override
	public String parse(StringReader reader) throws CommandSyntaxException {
		if (!reader.canRead()) {
			throw INVALID_OPERATION.createWithContext(reader);
		} else {
			int i = reader.getCursor();

			while (reader.canRead() && reader.peek() != ' ') {
				reader.skip();
			}

			return reader.getString().substring(i, reader.getCursor());
		}
	}


	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return CommandSource.suggestMatching(new String[]{"=", "+=", "-=", "*=", "/=", "%=", "^=", "<", ">"}, builder);
	}

	@Override
	public Collection<String> getExamples()  { return EXAMPLES; }

}
