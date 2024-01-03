package com.fafik77.concatenate.mixin;

import com.fafik77.concatenate.command.PlayersStorageDataObject;
import com.google.common.collect.ImmutableList;
import net.minecraft.server.command.DataCommand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Mixin(DataCommand.class)
public class DataCommandMixin {

	/** we need to add our object to vanilla code (not only to DataCommand, also to ExecuteCommand )
	 * big thanks to Bawnorton (bawnorton) on Fabric community for help. (2024-01-03)
	 */
	@Shadow @Final @Mutable
	public static List<Function<String, DataCommand.ObjectType>> OBJECT_TYPE_FACTORIES;
	@Shadow @Final @Mutable
	public static List<DataCommand.ObjectType> TARGET_OBJECT_TYPES;
	@Shadow @Final @Mutable
	public static List<DataCommand.ObjectType> SOURCE_OBJECT_TYPES;

	static{
		List <Function<String, DataCommand.ObjectType>> Mutable_OBJECT_TYPE_FACTORIES = new ArrayList<>();
		Mutable_OBJECT_TYPE_FACTORIES.addAll(DataCommand.OBJECT_TYPE_FACTORIES);
		Mutable_OBJECT_TYPE_FACTORIES.add(PlayersStorageDataObject.TYPE_FACTORY);
		OBJECT_TYPE_FACTORIES= ImmutableList.copyOf(Mutable_OBJECT_TYPE_FACTORIES);

		List<DataCommand.ObjectType> Mutable_TARGET_OBJECT_TYPES = new ArrayList<>();
		Mutable_TARGET_OBJECT_TYPES.addAll(DataCommand.TARGET_OBJECT_TYPES);
		Mutable_TARGET_OBJECT_TYPES.add(Mutable_OBJECT_TYPE_FACTORIES.get(Mutable_OBJECT_TYPE_FACTORIES.size()-1).apply("target"));
		TARGET_OBJECT_TYPES= ImmutableList.copyOf(Mutable_TARGET_OBJECT_TYPES);

		List<DataCommand.ObjectType> Mutable_SOURCE_OBJECT_TYPES = new ArrayList<>();
		Mutable_SOURCE_OBJECT_TYPES.addAll(DataCommand.SOURCE_OBJECT_TYPES);
		Mutable_SOURCE_OBJECT_TYPES.add(Mutable_OBJECT_TYPE_FACTORIES.get(Mutable_OBJECT_TYPE_FACTORIES.size()-1).apply("source"));
		SOURCE_OBJECT_TYPES= ImmutableList.copyOf(Mutable_SOURCE_OBJECT_TYPES);
	}

}
