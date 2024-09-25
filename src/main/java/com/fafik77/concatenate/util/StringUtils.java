package com.fafik77.concatenate.util;

public class StringUtils {
	public static String escapeQuote(String s){
		return s.replace("\\", "\\\\")
				.replace("\"", "\\\"");
	}
}
