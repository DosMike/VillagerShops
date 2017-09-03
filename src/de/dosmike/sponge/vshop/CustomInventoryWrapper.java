package de.dosmike.sponge.vshop;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryProperty;
import org.spongepowered.api.item.inventory.ItemStack;

/** for some reason this class is no longer in the API, preventing any access i desire
 * the problem: inv is instance of CusotmInventory and thus can't be casted into GridInventory or similar */
public class CustomInventoryWrapper {
	static Class<? extends Inventory> cinv;
	Inventory holder;
	static Method setItemStack;
	static Method getItemStack;
	static Method getProperties;
	
	static {
		try {
			cinv = (Class<? extends Inventory>) Class.forName("org.spongepowered.common.item.inventory.custom.CustomInventory");
			
			//sig scanning
			for (Method m : cinv.getDeclaredMethods()) {
				Class<?>[] args = m.getParameterTypes();
				if (m.getReturnType().equals(Void.TYPE)) {
					if (args.length== 2 && args[0].equals(Integer.TYPE) && args[1].getName().equals("net.minecraft.item.ItemStack")) {
						if (setItemStack == null) {
							setItemStack = m;
							setItemStack.setAccessible(true);
						} else {
							VillagerShops.w("Found multiple Signatures for setItemStack!");
						}
					}
				}
				
				if (m.getName().equalsIgnoreCase("getProperties")) {
					if (args.length== 0) {
						if (getProperties == null) {
							getProperties = m;
							getProperties.setAccessible(true);
						} else {
							VillagerShops.w("Found multiple Signatures for getProperties!");
						}
					}
				}
				
				// ItemStack getItemStack(int) shares sig with ItemStack removeItemStack(int)
				
				//sig dump
				/*
				String[] sargs= new String[args.length];
				for (int i=0;i<args.length;i++) sargs[i] = args[i].getName(); 
				VillagerShops.l("%s %s(%s)", 
						m.getReturnType().getName(),
						m.getName(),
						StringUtils.join(sargs, ", ")
						);*/
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static boolean isInstanceOf(Inventory inv) {
		return cinv.isInstance(inv);
	}
	
	/** just public in case someone has a use for this */
	public CustomInventoryWrapper(Inventory inventory) {
		holder = inventory;
	}
	
	public void setItemStack(int column, int row, ItemStack item) {
		//assuming fix width of 9
		try {
			int num = row*9+column;
			
			setItemStack.invoke(holder, num, item);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Deprecated
	public void getItemStack(int column, int row) {
		//assuming fix width of 9
//		try {
//			Method getStackInSlot = cinv.getMethod("getStackInSlot", Integer.TYPE);
//			getStackInSlot.setAccessible(true);
//			getStackInSlot.invoke(holder, row*9+column);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}
	
	public Inventory getInventory() {
		return holder;
	}
	
	public Map<String, InventoryProperty<?, ?>> getProperties() {
		try {
			return (Map<String, InventoryProperty<?, ?>>) getProperties.invoke(holder);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new HashMap<>();
	}
}
