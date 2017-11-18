package de.dosmike.sponge.vshop;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;

public class FieldResolver {
	
	/** following the sponge code-style with static fields being all upper-case */
	static <T extends Object> Object getFinalStaticByName(Class<T> clazz, String field) {
		try {
			Field result = clazz.getField(field.toUpperCase());
			return result.get(null);
		} catch (Exception e) {
//			e.printStackTrace();
			return null;
		}
	}
	
	/** following the sponge code-style with static fields being all upper-case
	 * Hoping static fields keep order (appearance wise or something) - do not serialize with! */
	static <T extends Object> Object getFinalStaticByIndex(Class<T> clazz, int field) {
		try {
			Field[] result = clazz.getFields();
			return result[field].get(null);
		} catch (Exception e) {
//			e.printStackTrace();
			return null;
		}
	}
	
	public static <T extends Object> Object getFinalStaticAuto(Class<T> clazz, String value) {
		return StringUtils.isNumeric(value) 
			? FieldResolver.getFinalStaticByIndex(clazz, Integer.parseInt(value))
			: FieldResolver.getFinalStaticByName(clazz, value);
	}

	/** getting item type API independent */
	static ItemType getType(ItemStack item) {
		Method m=null;
		try {
			m = item.getClass().getMethod("getType");
		} catch (Exception e) {
			try {
				m = item.getClass().getMethod("getItem");
			} catch (Exception e1) {
				throw new RuntimeException("Unable to get ItemStack.getType() Method");
			}
		}
		if (!m.getReturnType().equals(ItemType.class)) throw new RuntimeException("Unable to get ItemStack.getType() Method");
		try {
			return (ItemType) m.invoke(item);
		} catch (Exception e) {
			throw new RuntimeException("Unable to invoke ItemStack.getType() Method");
		}
	}
	static ItemType emptyHandItem() {
		ItemType air = (ItemType) getFinalStaticByName(ItemTypes.class, "air");
		if (air == null) air = (ItemType) getFinalStaticByName(ItemTypes.class, "none");
		return air;
	}
	static boolean itemStackEmpty(ItemStackSnapshot item) {
		try {
			return (boolean) ItemStackSnapshot.class.getMethod("isEmpty").invoke(item);
		} catch (Exception e) {
			try {
				return (int)ItemStackSnapshot.class.getMethod("getCount").invoke(item) ==0;
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		return true;
	}
}
