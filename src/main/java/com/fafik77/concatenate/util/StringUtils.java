package com.fafik77.concatenate.util;

public class StringUtils {
	public static String escapeQuote(String s){
		return s.replace("\\", "\\\\")
				.replace("\"", "\\\"");
	}
	public static String escape(String s){
		return s.replace("\\", "\\\\")
				.replace("\t", "\\t")
				.replace("\b", "\\b")
				.replace("\n", "\\n")
				.replace("\r", "\\r")
				.replace("\f", "\\f")
				.replace("\"", "\\\"");
	}
}
