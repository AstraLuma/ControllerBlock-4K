package com.astro73.controllerblock4k;

public enum BlockProtectMode {
	PROTECT, REMOVE, NONE;
	
	public static BlockProtectMode fromConfig(String value) {
		return BlockProtectMode.valueOf(value.toUpperCase());
	}
}