package de.dosmike.sponge.vshop.integrations.protection;

import java.util.NoSuchElementException;

public enum ProtectionAccessLevel {

	// ordinal is access level 'power'!
	IGNORED, CONTAINER, MEMBER, MODERATIVE;

	public static ProtectionAccessLevel fromString(String name) {
		if (name == null) return IGNORED;
		name = name.toLowerCase();
		if (name.startsWith("disable") || name.equals("off") || name.equals("false")) return IGNORED;
		if (name.equals("container") || name.equals("inventory") || name.contains("chest")) return CONTAINER;
		if (name.startsWith("build") || name.startsWith("block") || name.equals("member")) return MEMBER;
		if (name.equals("owner") || name.startsWith("maintain") || name.equals("leader") || name.startsWith("mod"))
			return MODERATIVE;
		throw new NoSuchElementException("Can not defer Claim Access Level from: " + name.toUpperCase());
	}

}
