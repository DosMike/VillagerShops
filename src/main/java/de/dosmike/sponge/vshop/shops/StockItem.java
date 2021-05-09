package de.dosmike.sponge.vshop.shops;

import de.dosmike.sponge.vshop.Utilities;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.systems.GameDictHelper;
import de.dosmike.sponge.vshop.systems.ItemNBTCleaner;
import de.dosmike.sponge.vshop.systems.ShopType;
import de.dosmike.sponge.vshop.systems.pluginfilter.DummyItemFilter;
import de.dosmike.sponge.vshop.systems.pluginfilter.FilterResolutionException;
import de.dosmike.sponge.vshop.systems.pluginfilter.PluginItemFilter;
import de.dosmike.sponge.vshop.systems.pluginfilter.PluginItemServiceImpl;
import org.spongepowered.api.GameDictionary;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.*;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.service.economy.Currency;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class StockItem {
	private static final DataQuery dqStackSize = DataQuery.of("Count"); //not interesting for filtering
	private static final DataQuery dqDamageMeta = DataQuery.of("UnsafeDamage"); //used for variants up to mc 1.12.2
	private ItemStackSnapshot item;
	private FilterOptions nbtfilter = FilterOptions.NORMAL;
	private String filterNameExtra = null;
	private PluginItemFilter pluginFilter = null;
	private final Currency currency; //currency to use
	private Double sellprice = null, buyprice = null; //This IS the SINGLE ITEM price
	//caution: maxStock does not actually hold information about the size of the stock container!
	private int maxStock, stocked = 0;
	/**
	 * This is a cache for valid itemStackSnapshots that match
	 */
	private List<GameDictionary.Entry> oreDictEntries = new LinkedList<>();

	public StockItem(ItemStackSnapshot itemstack, Double sellfor, Double buyfor, Currency currency, int stockLimit) {
		item = itemstack.copy();
		//legacy load
		double quantity = itemstack.getQuantity();
		this.item = ItemStack.builder().fromSnapshot(this.item).quantity(1).build().createSnapshot();
		if (sellfor != null && sellfor >= 0) this.sellprice = sellfor / quantity;
		if (buyfor != null && buyfor >= 0) this.buyprice = buyfor / quantity;
		this.currency = currency;
		this.maxStock = stockLimit;
	}

	public StockItem(ItemStackSnapshot itemstack, Double sellfor, Double buyfor, Currency currency, int stockLimit, FilterOptions nbtfilter) {
		this(itemstack, sellfor, buyfor, currency, stockLimit);
		this.nbtfilter = nbtfilter;
	}

	/**
	 * forces filterOption to OREDICT
	 *
	 * @throws IllegalArgumentException if oredict entry not found
	 */
	public StockItem(String oreDictEntry, Double sellfor, Double buyfor, Currency currency, int stockLimit) {
		this.oreDictEntries = GameDictHelper.getAll(oreDictEntry);
		if (oreDictEntries.isEmpty())
			throw new IllegalArgumentException("No Game Dictionary entry for " + oreDictEntry);

		this.filterNameExtra = oreDictEntry;
		item = oreDictEntries.get(0).getTemplate().copy();
		if (sellfor != null && sellfor >= 0) this.sellprice = sellfor;
		if (buyfor != null && buyfor >= 0) this.buyprice = buyfor;
		this.currency = currency;
		this.maxStock = stockLimit;
		this.nbtfilter = FilterOptions.OREDICT;
	}

	/**
	 * forces filterOption to PLUGIN
	 */
	public StockItem(ItemStackSnapshot itemstack, PluginItemFilter pluginItemFilter, Double sellfor, Double buyfor, Currency currency, int stockLimit) {
		this.pluginFilter = pluginItemFilter;

		this.filterNameExtra = PluginItemServiceImpl.getFilterID(pluginItemFilter).get();

		item = itemstack.copy(); // item in this case is a fallback
		if (sellfor != null && sellfor >= 0) this.sellprice = sellfor;
		if (buyfor != null && buyfor >= 0) this.buyprice = buyfor;
		this.currency = currency;
		this.maxStock = stockLimit;
		this.nbtfilter = FilterOptions.PLUGIN;
	}

	public List<GameDictionary.Entry> getAllOreDictEntries() {
		return oreDictEntries;
	}
	private PluginItemFilter resolveFilter() throws FilterResolutionException {
		if (pluginFilter instanceof DummyItemFilter) {
			pluginFilter = ((DummyItemFilter) pluginFilter).get();
		}
		return pluginFilter;
	}

	/**
	 * Calls getDisplayItem on the pluginFilter if exists
	 *
	 * @return itemstack for display and value extraction. To actually create items for the user, use createItem()
	 */
	public ItemStackSnapshot getItem(ShopType shopType) throws FilterResolutionException {
		if (pluginFilter != null) {
			return resolveFilter().getDisplayItem(shopType).orElseGet(() -> item);
		} else {
			return item;
		}
	}

	/**
	 * @param amount   the exact amount of items this stack shall have
	 * @param shopType carry information on whether this item is listed in an admin shop
	 * @return a brand new item stack for the player with up to amount items
	 */
	public ItemStack createItem(int amount, ShopType shopType) throws FilterResolutionException {
		if (pluginFilter == null) {
			ItemStack amountCopy = item.createStack();
			amountCopy.setQuantity(amount);
			return amountCopy;
		} else {
			ItemStack amountCopy = resolveFilter().supply(amount, shopType);
			if (amountCopy == null) {
				VillagerShops.audit("The item %s did not hand any items to the player! ", filterNameExtra);
				VillagerShops.critical("The item %s did not hand any items to the player! ", filterNameExtra);
				VillagerShops.critical("This is probably a bug - Report to the corresponding Plugin author immediately!");
				return ItemStack.of(ItemTypes.AIR);
			} else if (amountCopy.getQuantity() != amount) {
				VillagerShops.audit("The item %s did not supply the correct amount and scammed the customer by %d items. (Trying to correct stack size)", filterNameExtra, Math.abs(amountCopy.getQuantity() - amount));
				VillagerShops.critical("The item %s did not supply the correct amount and scammed the customer by %d items. (Trying to correct stack size)", filterNameExtra, Math.abs(amountCopy.getQuantity() - amount));
				VillagerShops.critical("This is probably a bug - Report to the corresponding Plugin author immediately!");
				amountCopy.setQuantity(amount);
			}
			return amountCopy;
		}
	}

	public Double getBuyPrice() {
		return buyprice;
	}

	public void setBuyPrice(Double price) {
		buyprice = price;
	}

	public Double getSellPrice() {
		return sellprice;
	}

	public void setSellPrice(Double price) {
		sellprice = price;
	}

	public FilterOptions getNbtFilter() {
		return nbtfilter;
	}

	public Optional<String> getFilterNameExtra() {
		return Optional.ofNullable(filterNameExtra);
	}

	public Optional<PluginItemFilter> getPluginFilter() throws FilterResolutionException {
		return Optional.ofNullable(resolveFilter());
	}

	/**
	 * the currency this item is handled in
	 */
	public Currency getCurrency() {
		return currency;
	}

	public int getMaxStock() {
		return maxStock;
	}

	public void setMaxStock(int stockLimit) {
		maxStock = stockLimit;
	}

	/**
	 * this is probably broken for non stock-limited items!
	 */
	public int getStocked() {
		return maxStock > 0 ? stocked : item.getQuantity();
	}

	public void updateStock(Inventory container) throws FilterResolutionException {
		Inventory result = filterInventory(container);
		int count = 0;
		for (Inventory s : result.slots()) count += s.totalItems();
		stocked = Math.min(maxStock, count);
	}

	/**
	 * Custom toString { info }
	 */
	@Override
	public String toString() {
		return String.format("{ %s: %s, filter: %s, buyprice: %s, sellprice: %s, currency: %s, limit: %s }",
				filterNameExtra != null ? "filtername" : "item",
				filterNameExtra != null ? filterNameExtra : Utilities.toString(item),
				nbtfilter.toString(),
				buyprice != null ? buyprice.toString() : "N/A",
				sellprice != null ? sellprice.toString() : "N/A",
				currency.getId(),
				maxStock > 0 ? maxStock : "N/A"
		);
	}

	public Purchase.Result buy(Player player, ShopEntity shop, int amount) throws FilterResolutionException {
		if (!getPluginFilter().map(pif -> pif.supportShopType(ShopType.fromInstance(shop))).orElse(true))
			return Purchase.Result.INCOMPATIBLE_SHOPTYPE;
		try {
			return new Purchase(this, player, shop).buy(amount);
		} catch (Purchase.FailedPreconditionException e) {
			return e.getResult();
		}
	}

	public Purchase.Result sell(Player player, ShopEntity shop, int amount) throws FilterResolutionException {
		if (!getPluginFilter().map(pif -> pif.supportShopType(ShopType.fromInstance(shop))).orElse(true))
			return Purchase.Result.INCOMPATIBLE_SHOPTYPE;
		try {
			return new Purchase(this, player, shop).sell(amount);
		} catch (Purchase.FailedPreconditionException e) {
			return e.getResult();
		}
	}

	/**
	 * filter some inventory for the items represented by this StockItem
	 */
	public Inventory filterInventory(Inventory inv) throws FilterResolutionException {
		Inventory filtered;
		final ItemStack TEMPLATE = ItemNBTCleaner.filter(item.createStack());
		switch (nbtfilter) {
			case IGNORE_NBT: {
				filtered = inv.query(QueryOperationTypes.ITEM_TYPE.of(TEMPLATE.getType()));
				break;
			}
			case IGNORE_DAMAGE: {
				DataContainer j = TEMPLATE.toContainer()
						.remove(dqDamageMeta)
						.remove(dqStackSize);
				filtered = inv.query(QueryOperationTypes.ITEM_STACK_CUSTOM.of((i) ->
						ItemNBTCleaner.filter(i.toContainer())
								.remove(dqDamageMeta)
								.remove(dqStackSize)
								.equals(j)
				));
				break;
			}
			case OREDICT: {
				filtered = inv.query(QueryOperationTypes.ITEM_STACK_CUSTOM.of(i ->
						oreDictEntries.stream().anyMatch(e -> e.matches(i))
				));
				break;
			}
			case PLUGIN: {
				resolveFilter();
				filtered = inv.query(QueryOperationTypes.ITEM_STACK_CUSTOM.of(item ->
						pluginFilter.isItem(ItemNBTCleaner.filter(item)))
				);
				break;
			}
			case TYPE_ONLY: {
				ItemType type = item.getType();
				int meta = (TEMPLATE.supports(Keys.ITEM_DURABILITY) ? 0 : TEMPLATE.toContainer().getInt(dqDamageMeta).orElse(0));
				filtered = inv.query(QueryOperationTypes.ITEM_STACK_CUSTOM.of(i -> {
							if (!i.getType().equals(type)) return false;
							int imeta = (i.supports(Keys.ITEM_DURABILITY) ? 0 : TEMPLATE.toContainer().getInt(dqDamageMeta).orElse(0));
							return meta == imeta;
						}
				));
				break;
			}
			default: {
				filtered = inv.query(QueryOperationTypes.ITEM_STACK_CUSTOM.of(item -> ItemStackComparators.IGNORE_SIZE.compare(ItemNBTCleaner.filter(item), TEMPLATE) == 0));
//            filtered = inv.query(QueryOperationTypes.ITEM_STACK_IGNORE_QUANTITY.of(item));
				break;
			}
		}
		return filtered;
	}

	/**
	 * allows you to check whether some item is represented through this stock item
	 *
	 * @param other the ItemStack to test
	 * @return true if this stock item would trade the given stack
	 */
	public boolean test(ItemStack other) throws FilterResolutionException {
		final ItemStack TEMPLATE = ItemNBTCleaner.filter(item.createStack());
		switch (nbtfilter) {
			case IGNORE_NBT: {
				return other.getType().equals(TEMPLATE.getType());
			}
			case IGNORE_DAMAGE: {
				DataContainer j = TEMPLATE.toContainer()
						.remove(dqDamageMeta)
						.remove(dqStackSize);
				return ItemNBTCleaner.filter(other.toContainer())
						.remove(dqDamageMeta)
						.remove(dqStackSize)
						.equals(j);
			}
			case OREDICT: {
				return oreDictEntries.stream().anyMatch(e -> e.matches(other));
			}
			case PLUGIN: {
				return resolveFilter().isItem(ItemNBTCleaner.filter(other));
			}
			case TYPE_ONLY: {
				ItemType type = item.getType();
				int meta = (TEMPLATE.supports(Keys.ITEM_DURABILITY) ? 0 : TEMPLATE.toContainer().getInt(dqDamageMeta).orElse(0));
				if (!other.getType().equals(type)) return false;
				int othermeta = (other.supports(Keys.ITEM_DURABILITY) ? 0 : TEMPLATE.toContainer().getInt(dqDamageMeta).orElse(0));
				return meta == othermeta;
			}
			default: {
				return ItemStackComparators.IGNORE_SIZE.compare(ItemNBTCleaner.filter(other), TEMPLATE) == 0;
			}
		}
	}

	/**
	 * figure out how much of item the inventory can hold<br><br>
	 * A few months ago there Sponge asked what could be changes about the inventory API
	 * it is only now, that I realize a inventory.tryOffer(ItemStack) or inventory.capacityFor(ItemStack) would be nice
	 * count for each slot, how many of the item it could accept
	 */
	public int invSpace(Inventory inventory) throws FilterResolutionException {
		int maxStack = getPluginFilter().map(PluginItemFilter::getMaxStackSize).orElse(item.createStack().getMaxStackQuantity());
		int space = 0, c;
		Inventory result = filterInventory(inventory);
		for (Inventory s : result.slots()) {
			Slot slot = (Slot) s;
			c = slot.totalItems();
			if (c > 0) space += (maxStack - c);
		}
		space += (inventory.capacity() - inventory.size()) * maxStack;
		return space;
	}

	/**
	 * figure out how much of item the inventory can supply
	 */
	public int invSupply(Inventory inventory) throws FilterResolutionException {
		return filterInventory(inventory).totalItems();
	}

	public enum FilterOptions {
		/**
		 * All item NBT has to match
		 */
		NORMAL,
		/**
		 * Entries from the items at /UnsafeDamage will be removed before comparing.
		 * This includes pre 1.13 /meta values
		 */
		IGNORE_DAMAGE,
		/**
		 * Compare item types only, ignoring all NBT (including damage)
		 */
		IGNORE_NBT,
		/**
		 * Use the the item type (and meta pre 1.13) to compare
		 */
		TYPE_ONLY,
		/**
		 * Use the forge OreDict to check if items can sell
		 */
		OREDICT,
		/**
		 * Plugins can now register custom filters for things
		 * like VoteKeys
		 */
		PLUGIN;

		public static FilterOptions of(String name) {
			for (FilterOptions o : values()) {
				if (name.equalsIgnoreCase(o.name()))
					return o;
			}
			throw new NoSuchElementException("No such Filter: " + name);
		}
	}
}
