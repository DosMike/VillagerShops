package de.dosmike.sponge.vshop.integrations.protection;

import java.util.NoSuchElementException;

public enum  ClaimAccessLevel {

    // ordinal is access level 'power'!
    IGNORED, CONTAINER, BUILDER, OWNER;

    public static ClaimAccessLevel fromString(String name) {
        if (name == null) return IGNORED;
        name = name.toLowerCase();
        if (name.startsWith("disable") || name.equals("off") || name.equals("false")) return IGNORED;
        if (name.equals("container") || name.equals("inventory")) return CONTAINER;
        if (name.startsWith("build") || name.startsWith("block")) return BUILDER;
        if (name.equals("owner") || name.startsWith("maintain")) return OWNER;
        throw new NoSuchElementException("Can not defer Claim Access Level from: "+name.toUpperCase());
    }

}
