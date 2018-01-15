package de.dosmike.sponge.vshop;

import java.lang.reflect.Method;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;

public class FieldResolver {
	
	/** following the sponge code-style with static fields being all upper-case */
	static <T extends CatalogType> CatalogType getFinalStaticByName(Class<T> clazz, String field) {
		try {
			Collection<T> result = Sponge.getRegistry().getAllOf(clazz);
			VillagerShops.l("Entries for %s (%d):", clazz.getSimpleName(), result.size());
			
			//of course we perfer the ID, and we'll save that, but the use may enter a name
			CatalogType toReturn=null;
			for (CatalogType type : result) 
				if (type.getId().equalsIgnoreCase(field) || type.getName().equalsIgnoreCase(field)) {
					if (toReturn != null) return null; // field name was not unique
					else toReturn = type;
				}
			return toReturn;
		} catch (Exception e) {
//			e.printStackTrace();
			return null;
		}
	}
	
	/** following the sponge code-style with static fields being all upper-case
	 * Hoping static fields keep order (appearance wise or something) - do not serialize with! */
	static <T extends CatalogType> CatalogType getFinalStaticByIndex(Class<T> clazz, int field) {
		try {
			Collection<T> result = Sponge.getRegistry().getAllOf(clazz);
			if (field < 0 || field > result.size()) return null;
			return result.toArray(new CatalogType[result.size()])[field];
		} catch (Exception e) {
//			e.printStackTrace();
			return null;
		}
	}
	
	public static <T extends CatalogType> CatalogType getFinalStaticAuto(Class<T> clazz, String value) {
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
}
