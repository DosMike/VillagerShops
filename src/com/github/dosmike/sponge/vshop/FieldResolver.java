package com.github.dosmike.sponge.vshop;

import java.lang.reflect.Field;

public class FieldResolver {
	
	/** following the sponge code-style with static fields being all upper-case */
	public static <T extends Object> Object getFinalStaticByName(Class<T> clazz, String field) {
		try {
			Field result = clazz.getField(field.toUpperCase());
			return result.get(null);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/** following the sponge code-style with static fields being all upper-case
	 * Hoping static fields keep order (appearance wise or something) - do not serialize with! */
	public static <T extends Object> Object getFinalStaticByIndex(Class<T> clazz, int field) {
		try {
			Field[] result = clazz.getFields();
			return result[field].get(null);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}