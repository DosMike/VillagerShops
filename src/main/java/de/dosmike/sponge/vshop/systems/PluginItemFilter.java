package de.dosmike.sponge.vshop.systems;

import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.Slot;

import java.util.Optional;

public interface PluginItemFilter {

	/**
	 * Used to filter {@link Slot}s out of an {@link Inventory}. Don't forget to check basic properties like
	 * {@link ItemType} here!
	 *
	 * @return true if item represents an item of this filter-type
	 */
	boolean isItem(ItemStack item);

	/**
	 * Can be used to enforce the use of this PluginItemFilter for items.
	 *
	 * @return true if this filter should automatically set in place, if an added item matched isItem()
	 */
	@SuppressWarnings("SameReturnValue")
	default boolean enforce() {
		return false;
	}

	/**
	 * If you want to block this item from being resold by players or admin shops you can use this
	 * to prevent the item from being added to a shop (or sold, if you change this later)
	 *
	 * @param shopType the type of shop this item is currently listed in
	 * @return true if the item can be added to this shop
	 */
	@SuppressWarnings({"SameReturnValue"})
	default boolean supportShopType(ShopType shopType) {
		return true;
	}

	/**
	 * VillagerShops will try to give this {@link ItemStack} to the player.
	 * While it does not increase the stack size it may reduce the stack size
	 * to accommodate to player inventory space and balance.
	 * This should not be less, because by the time this is called, the player balance was already touched!
	 *
	 * @param amount   the amount of items requested by the purchase transaction (is checked).
	 * @param shopType the type of shop this item is currently listed in
	 * @return the ItemStack that the user will buy
	 */
	ItemStack supply(int amount, ShopType shopType);

	/**
	 * can override the maximum amount of items that can be bought at once.
	 * default is evaluated using <code>supply(1).getType().getMaxStackQuantity()</code>
	 *
	 * @return a custom maximum stack quantity
	 */
	default int getMaxStackSize() {
		ItemStack stack = supply(1, ShopType.AdminShop);
		int stackSize = stack.getType().getMaxStackQuantity();
		consume(stack, ShopType.AdminShop);
		return stackSize;
	}

	/**
	 * This is a callback for your plugin in case you want to do something
	 * when a player sells your items. maybe the item has callbacks by NBT
	 * that could be destroyed to save memory.
	 * By the time this method is called you might assume the item was already
	 * removed from the player inventory.
	 *
	 * @param item     the ItemStack the user just sold.
	 * @param shopType the type of shop this item is currently listed in
	 */
	default void consume(ItemStack item, ShopType shopType) {
	}

	/**
	 * In case you want to manipulate the item displayed (custom lore, add enchantments for glow, etc., ...)
	 * you can do so here. VillagerShops will use this Item if possible.
	 *
	 * @param shopType the type of shop this item is currently listed in
	 * @return The display override or empty() if the default should be used.
	 */
	Optional<ItemStackSnapshot> getDisplayItem(ShopType shopType);

}
