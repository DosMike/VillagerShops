package de.dosmike.sponge.vshop.menus;

import de.dosmike.sponge.langswitch.LocalizedText;
import de.dosmike.sponge.megamenus.api.elements.IIcon;
import de.dosmike.sponge.megamenus.api.elements.concepts.IClickable;
import de.dosmike.sponge.megamenus.api.listener.OnClickListener;
import de.dosmike.sponge.megamenus.api.state.StateObject;
import de.dosmike.sponge.megamenus.impl.elements.IElementImpl;
import de.dosmike.sponge.vshop.ConfigSettings;
import de.dosmike.sponge.vshop.Utilities;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.shops.InteractionHandler;
import de.dosmike.sponge.vshop.shops.ShopEntity;
import de.dosmike.sponge.vshop.shops.StockItem;
import de.dosmike.sponge.vshop.systems.ShopType;
import de.dosmike.sponge.vshop.systems.pluginfilter.FilterResolutionException;
import org.apache.commons.lang3.NotImplementedException;
import org.spongepowered.api.GameDictionary;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * represents a {@link StockItem} as button inside an inventory menu
 */
public final class MShopSlot extends IElementImpl implements IClickable<MShopSlot> {

	public static final String MENU_SHOP_QUANTITY = "quantity";
	private static final IIcon removeMeIcon = IIcon.of(ItemTypes.BARRIER);
	private boolean markedForRemoval = false;
	private StockItem stockItem;
	private int myPosition;
	private UUID shopIdBackRef;
	private final OnClickListener<MShopSlot> clickListener = (element, player, button, shift) -> {
		StateObject state = element.getParent().getPlayerState(player);
		boolean inRemoveMode = state.getBoolean(ShopMenuManager.MENU_REMOVEMODE).orElse(false);
		if (inRemoveMode) {
			HashSet<Integer> marked = state.<HashSet<Integer>>get(ShopMenuManager.MENU_REMOVESET).orElse(new HashSet<>());
			markedForRemoval = !markedForRemoval;
			if (markedForRemoval) {
				marked.add(myPosition);
			} else {
				marked.remove(myPosition);
			}
			state.set(ShopMenuManager.MENU_REMOVESET, marked);
			element.invalidate();
			return;
		}

		boolean doBuy;
		if (ConfigSettings.isSmartClickEnabled() && (stockItem.getBuyPrice() == null || stockItem.getSellPrice() == null)) {
			doBuy = stockItem.getBuyPrice() != null; //smart-click: can buy? do buy - otherwise has to be sell
		} else if (button == MouseEvent.BUTTON1) {
			doBuy = true;
		} else if (button == MouseEvent.BUTTON2) {
			doBuy = false;
		} else return;
		ShopEntity shop = VillagerShops.getShopFromShopId(shopIdBackRef).orElse(null);
		if (shop == null) return;

		try {
			ShopMenuManager.QuantityValues quantityValues = getParent().getPlayerState(player).getOfClass(MENU_SHOP_QUANTITY, ShopMenuManager.QuantityValues.class)
					.orElse(ConfigSettings.getShopsDefaultStackSize());
			int quantity;
			ShopType shopType = ShopType.fromInstance(shop);
			if (stockItem.getNbtFilter().equals(StockItem.FilterOptions.PLUGIN)) {
				ItemType itemType = stockItem.getItem(shopType).getType();
				quantity = stockItem.getPluginFilter().map(quantityValues::getStackSize)
						.orElseGet(() -> quantityValues.getStackSize(itemType));
			} else {
				quantity = quantityValues.getStackSize(stockItem.getItem(shopType).getType());
			}

			int change = InteractionHandler.shopItemClicked(player, shop, stockItem, doBuy, quantity);
			if (change > 0 && stockItem.getMaxStock() > 0) {
				Optional<Inventory> stock = shop.getStockInventory();
				if (stock.isPresent()) stockItem.updateStock(stock.get());
			}
		} catch (FilterResolutionException e) {
			VillagerShops.w("%s",e.getMessage());
			removeFromShop();
		}
		element.invalidate();
	};
	private IIcon icon;

	public MShopSlot(StockItem stockItem, int listPosition, UUID shopId) {
		this.stockItem = stockItem;
		myPosition = listPosition;
		shopIdBackRef = shopId;
	}

	@Override
	public OnClickListener<MShopSlot> getOnClickListener() {
		return clickListener;
	}

	@Override
	public void setOnClickListener(OnClickListener<MShopSlot> listener) {
		throw new NotImplementedException("Can't set OnClickListener for Shop Menu Elements");
	}

	@Override
	public void fireClickEvent(Player viewer, int button, boolean shift) {
		clickListener.onClick(this, viewer, button, shift);
	}

	@SuppressWarnings("unchecked")
	@Override
	public MShopSlot copy() {
		MShopSlot copy = new MShopSlot(stockItem, myPosition, shopIdBackRef);
		copy.setPosition(getPosition());
		return copy;
	}

	private ItemStackSnapshot _getDisplayItem() throws FilterResolutionException {
		ItemStackSnapshot displayItem = null;
		{
			if (stockItem.getNbtFilter().equals(StockItem.FilterOptions.PLUGIN)) {
				ShopEntity shop = VillagerShops.getShopFromShopId(shopIdBackRef).orElse(null);
				if (shop != null) {
					displayItem = stockItem.getItem(ShopType.fromInstance(shop));
				}
			}
			// fallback
			if (displayItem == null)
				displayItem = stockItem.getItem(ShopType.AdminShop);
		}
		return displayItem;
	}

	@Override
	public IIcon getIcon(Player viewer) {
		if (markedForRemoval)
			return removeMeIcon;

		ItemStack displayItem;
		int quantity;
		try {
			displayItem = _getDisplayItem().createStack();

			ShopMenuManager.QuantityValues quantityValues = getParent().getPlayerState(viewer).getOfClass(MENU_SHOP_QUANTITY, ShopMenuManager.QuantityValues.class)
					.orElse(ConfigSettings.getShopsDefaultStackSize());

			if (stockItem.getNbtFilter().equals(StockItem.FilterOptions.PLUGIN)) {
				quantity = stockItem.getPluginFilter().map(quantityValues::getStackSize)
						.orElse(quantityValues.getStackSize(displayItem.getType()));
			} else {
				quantity = quantityValues.getStackSize(displayItem.getType());
			}
		} catch (FilterResolutionException e) {
			VillagerShops.w("%s", e.getMessage());
			removeFromShop();
			return IIcon.of(ItemStack.builder().itemType(ItemTypes.BARRIER).build());
		}

		if (icon == null || icon.render().getQuantity() != quantity) {
			//rebuild icon only if necessary, otherwise the animation
			// of the iicon will reset (and resources)
			if (stockItem.getFilterNameExtra().isPresent() && stockItem.getNbtFilter().equals(StockItem.FilterOptions.OREDICT)) {
				List<ItemStack> frames = new LinkedList<>();
				for (GameDictionary.Entry dicted : stockItem.getAllOreDictEntries())
					frames.add(ItemStack.builder().fromSnapshot(dicted.getTemplate()).quantity(quantity).build());
				if (frames.isEmpty())
					frames.add(ItemStack.builder().from(displayItem).quantity(quantity).build());
				icon = IIcon.builder().addFrameItemStacks(frames).setFPS(1d).build();
			} else {
				displayItem.setQuantity(quantity);
				icon = IIcon.of(displayItem);
			}
		}
		return icon;
	}

	@Override
	public Text getName(Player viewer) {
		//marked for removal replaces the icon with barrier, this we need to supply a custom name to not display "Barrier"
		if (markedForRemoval) {
			try {
				ItemStackSnapshot displayItem = _getDisplayItem();
				return displayItem.get(Keys.DISPLAY_NAME).orElse(
						Text.of(displayItem.getType().getTranslation().get(Utilities.playerLocale(viewer)))
				);
			} catch (FilterResolutionException e) {
				return Text.of(TextColors.RED, "Invalid");
			}
		} else {
			return null;
		}
	}

	@Override
	public List<Text> getLore(Player viewer) {
		if (markedForRemoval) {
			return Collections.singletonList(Text.of(TextColors.RESET,
					VillagerShops.getTranslator().localText("shop.item.markedforremoval")
							.resolve(viewer)
							.orElse(Text.of("shop.item.markedforremoval"))
					)
			);
		}
		ItemStackSnapshot item;
		try {
			item = _getDisplayItem();
		} catch (FilterResolutionException e) {
			item = ItemStack.builder().itemType(ItemTypes.BARRIER).build().createSnapshot();
		}
		List<Text> lore = item.get(Keys.ITEM_LORE).orElse(new LinkedList<>());

		Text currency = stockItem.getCurrency().getSymbol();
		int quantity = getParent().getPlayerState(viewer).getOfClass(MENU_SHOP_QUANTITY, ShopMenuManager.QuantityValues.class)
				.orElse(ConfigSettings.getShopsDefaultStackSize())
				.getStackSize(item.getType());
		ShopEntity shop = VillagerShops.getShopFromShopId(shopIdBackRef).get();
		boolean isOwner = shop.isShopOwner(viewer.getUniqueId());
		if (stockItem.getBuyPrice() != null) {
			BigDecimal priceSingle = isOwner
					? BigDecimal.valueOf(stockItem.getBuyPrice())
					: VillagerShops.getPriceCalculator().getCurrentPurchasePrice(item, 1, BigDecimal.valueOf(stockItem.getBuyPrice()), shopIdBackRef, viewer.getUniqueId());
			if (quantity > 1) {
				BigDecimal priceStack = isOwner
						? BigDecimal.valueOf(stockItem.getBuyPrice() * quantity)
						: VillagerShops.getPriceCalculator().getCurrentPurchasePrice(item, quantity, BigDecimal.valueOf(stockItem.getBuyPrice()), shopIdBackRef, viewer.getUniqueId());
				lore.add(Text.of(TextColors.RED,
						((LocalizedText) VillagerShops.getTranslator().localText("shop.item.buy.stack"))
								.replace("%price%", Utilities.nf(priceStack, Utilities.playerLocale(viewer)))
								.replace("%itemprice%", Utilities.nf(priceSingle, Utilities.playerLocale(viewer)))
								.replace("%currency%", currency)
								.setContextColor(TextColors.RED)
								.resolve(viewer).orElse(Text.of("shop.item.buy.stack"))
				));
			} else {
				lore.add(Text.of(TextColors.RED,
						((LocalizedText) VillagerShops.getTranslator().localText("shop.item.buy.one"))
								.replace("%price%", Utilities.nf(priceSingle, Utilities.playerLocale(viewer)))
								.replace("%currency%", currency)
								.setContextColor(TextColors.RED)
								.resolve(viewer).orElse(Text.of("shop.item.buy.one"))
				));
			}
		}
		if (stockItem.getSellPrice() != null) {
			BigDecimal priceSingle = isOwner
					? BigDecimal.valueOf(stockItem.getSellPrice())
					: VillagerShops.getPriceCalculator().getCurrentPurchasePrice(item, 1, BigDecimal.valueOf(stockItem.getSellPrice()), shopIdBackRef, viewer.getUniqueId());
			if (quantity > 1) {
				BigDecimal priceStack = isOwner
						? BigDecimal.valueOf(stockItem.getSellPrice() * quantity)
						: VillagerShops.getPriceCalculator().getCurrentPurchasePrice(item, quantity, BigDecimal.valueOf(stockItem.getSellPrice()), shopIdBackRef, viewer.getUniqueId());
				lore.add(Text.of(TextColors.GREEN,
						((LocalizedText) VillagerShops.getTranslator().localText("shop.item.sell.stack"))
								.replace("%price%", Utilities.nf(priceStack, Utilities.playerLocale(viewer)))
								.replace("%itemprice%", Utilities.nf(priceSingle, Utilities.playerLocale(viewer)))
								.replace("%currency%", currency)
								.setContextColor(TextColors.GREEN)
								.resolve(viewer).orElse(Text.of("shop.item.sell.stack"))
				));
			} else {
				lore.add(Text.of(TextColors.GREEN,
						((LocalizedText) VillagerShops.getTranslator().localText("shop.item.sell.one"))
								.replace("%price%", Utilities.nf(priceSingle, Utilities.playerLocale(viewer)))
								.replace("%currency%", currency)
								.setContextColor(TextColors.GREEN)
								.resolve(viewer).orElse(Text.of("shop.item.sell.one"))
				));
			}
		}
		if (stockItem.getMaxStock() > 0) {
			lore.add(Text.of(TextColors.GRAY,
					((LocalizedText) VillagerShops.getTranslator().localText("shop.item.stock"))
							.replace("%amount%", stockItem.getStocked())
							.replace("%max%", stockItem.getMaxStock())
							.setContextColor(TextColors.GRAY)
							.resolve(viewer).orElse(Text.of("shop.item.stock"))
			));
		}
		if (stockItem.getNbtFilter().equals(StockItem.FilterOptions.TYPE_ONLY)) {
			lore.add(Text.of(TextColors.GRAY,
					((LocalizedText) VillagerShops.getTranslator().localText("shop.item.filter.type"))
							.setContextColor(TextColors.GRAY)
							.resolve(viewer).orElse(Text.of("shop.item.filter.type"))
			));
		} else if (stockItem.getNbtFilter().equals(StockItem.FilterOptions.IGNORE_DAMAGE)) {
			lore.add(Text.of(TextColors.GRAY,
					((LocalizedText) VillagerShops.getTranslator().localText("shop.item.filter.damage"))
							.setContextColor(TextColors.GRAY)
							.resolve(viewer).orElse(Text.of("shop.item.filter.damage"))
			));
		} else if (stockItem.getNbtFilter().equals(StockItem.FilterOptions.IGNORE_NBT)) {
			lore.add(Text.of(TextColors.GRAY,
					((LocalizedText) VillagerShops.getTranslator().localText("shop.item.filter.nbt"))
							.setContextColor(TextColors.GRAY)
							.resolve(viewer).orElse(Text.of("shop.item.filter.nbt"))
			));
		} else if (stockItem.getNbtFilter().equals(StockItem.FilterOptions.OREDICT)) {
			lore.add(Text.of(TextColors.GRAY,
					((LocalizedText) VillagerShops.getTranslator().localText("shop.item.filter.oredict"))
							.setContextColor(TextColors.GRAY)
							.replace("%oredict%", stockItem.getFilterNameExtra().get())
							.resolve(viewer).orElse(Text.of("shop.item.filter.oredict"))
			));
		}

		return lore;
	}

	private void removeFromShop() {
		VillagerShops.getShopFromShopId(shopIdBackRef).ifPresent(shop->{
			if (shop.getMenu().items.remove(stockItem)) {
				VillagerShops.closeShopInventories(shopIdBackRef);
				VillagerShops.getInstance().markShopsDirty(shop);
			};
		});
	}

}
