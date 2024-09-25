package com.fafik77.concatenate.command.concat;

import com.fafik77.concatenate.util.StringUtils;
import net.minecraft.nbt.*;
import org.jetbrains.annotations.NotNull;

/** converts any element to json string representation */
public class NbtToStr {
	public NbtToStr() {}
	String separator = "";
	/** concat result */
	protected StringBuilder concatOut = new StringBuilder();
	/** elements concatenated */
	protected int concatCount= 0;

	public void setSeparator(final String separator){this.separator = separator;}
	public int getConcatCount(){return concatCount;}
	public String getResult(){return concatOut.toString();}

	public void concat(final String str) {
		if(concatCount!=0){concatOut.append(separator);} ++concatCount;
		concatOut.append(str);
	}
	public void concat(final @NotNull NbtElement nbtElement) {
		concat(nbtElement, false);
	}
	public void concat(final @NotNull NbtElement nbtElement, final boolean noRecurse) {
		byte NT = nbtElement.getType();
		switch (NT){
			//Lists: 1 general && 3 specified
			case(NbtElement.LIST_TYPE): {
				if(noRecurse) break; //already recursed
				//List can store many different things, recurse through its content
				final NbtList ElementAsList = (NbtList)nbtElement;
				for(int i=0; i!= ElementAsList.size(); ++i ){
					concat( ElementAsList.get(i), true);
				}
				break;
			}
			case(NbtElement.BYTE_ARRAY_TYPE): {
				byte[] arr = ((NbtByteArray) nbtElement).getByteArray();
				for(int i=0; i!=arr.length; ++i){if(concatCount!=0){concatOut.append(separator);} ++concatCount; concatOut.append(arr[i]);}
				break;
			}
			case(NbtElement.INT_ARRAY_TYPE): {
				int[] arr = ((NbtIntArray) nbtElement).getIntArray();
				for(int i=0; i!=arr.length; ++i){if(concatCount!=0){concatOut.append(separator);} ++concatCount; concatOut.append(arr[i]);}
				break;
			}
			case(NbtElement.LONG_ARRAY_TYPE): {
				long[] arr = ((NbtLongArray) nbtElement).getLongArray();
				for(int i=0; i!=arr.length; ++i){if(concatCount!=0){concatOut.append(separator);} ++concatCount; concatOut.append(arr[i]);}
				break;
			}
			//simple single data
			case(NbtElement.BYTE_TYPE):
			case(NbtElement.SHORT_TYPE):
			case(NbtElement.INT_TYPE):
			case(NbtElement.LONG_TYPE):
			case(NbtElement.DOUBLE_TYPE):
			case(NbtElement.FLOAT_TYPE): {
				if(!noRecurse && concatCount!=0){concatOut.append(separator);} ++concatCount;
				concatOut.append(nbtElement.asString());
				break;
			}
			case(NbtElement.STRING_TYPE): {
				if(!noRecurse){
					if(concatCount!=0){concatOut.append(separator);} ++concatCount;
					concatOut.append(nbtElement.asString());
				}
				else {
					concatOut.append( "\""+ StringUtils.escapeQuote(nbtElement.asString())+ "\"" );
				}
				break;
			}
			// 2024-09-25 added handing for compounds
			case(NbtElement.COMPOUND_TYPE): {
				if(noRecurse) break; //already recursed
				//object can store many different things, recurse through its content
				final NbtCompound ElementAsObj = (NbtCompound)nbtElement;
				for (String key : ElementAsObj.getKeys()) {
					NbtElement val1 = ElementAsObj.get(key);
					if(val1==null) continue;
					if(concatCount!=0){concatOut.append(separator);} ++concatCount;
					concatOut.append( "\""+ StringUtils.escapeQuote(key)+ "\":" );
					concat( val1,true);
				}
				break;
			}
			default: {
				//COMPOUND_TYPE --not included, as it would have to recurse everything to spit out gibberish
				//unknown type = do nothing
				break;
			}
		}
	}
}
