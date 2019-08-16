package de.dosmike.sponge.vshop.menus;

import de.dosmike.sponge.megamenus.api.elements.IIcon;
import de.dosmike.sponge.megamenus.api.elements.concepts.IClickable;
import de.dosmike.sponge.megamenus.api.listener.OnClickListener;
import de.dosmike.sponge.megamenus.api.state.StateObject;
import de.dosmike.sponge.megamenus.impl.elements.IElementImpl;
import de.dosmike.sponge.vshop.ConfigSettings;
import de.dosmike.sponge.vshop.Utilities;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.shops.InteractionHandler;
import de.dosmike.sponge.vshop.shops.NPCguard;
import de.dosmike.sponge.vshop.shops.StockItem;
import org.apache.commons.lang3.NotImplementedException;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.awt.event.MouseEvent;
import java.util.*;
import java.util.stream.Collectors;

public final class MShop extends IElementImpl implements IClickable<MShop> {

    public static final String MENU_SHOP_QUANTITY = "quantity";

    private boolean markedForRemoval = false;
    private static final IIcon removeMeIcon = IIcon.of(ItemTypes.BARRIER);
    private StockItem stockItem;
    private int myPosition;
    public MShop(StockItem stockItem, int listPosition) {
        this.stockItem = stockItem;
        myPosition = listPosition;
    }

    private OnClickListener<MShop> clickListener = (element, player, button, shift)-> {
        StateObject state = element.getParent().getPlayerState(player);
        boolean inRemoveMode = state.getBoolean(InvPrep.MENU_REMOVEMODE).orElse(false);
        if (inRemoveMode) {
            HashSet<Integer> marked = (HashSet<Integer>) state.get(InvPrep.MENU_REMOVESET).orElse(new HashSet<Integer>());
            markedForRemoval=!markedForRemoval;
            if (markedForRemoval) {
                marked.add(myPosition);
            } else {
                marked.remove(myPosition);
            }
            state.set(InvPrep.MENU_REMOVESET, marked);
            element.invalidate();
            return;
        }

        boolean doBuy;
        if (button == MouseEvent.BUTTON1) {
            doBuy = true;
        } else if (button == MouseEvent.BUTTON2) {
            doBuy = false;
        } else return;
        UUID shopID = Utilities.getOpenShopFor(player);
        if (shopID == null) return;
        NPCguard shop = VillagerShops.getNPCfromShopUUID(shopID).orElse(null);
        if (shop == null) return;

        int quantity = getParent().getPlayerState(player).getOfClass(MENU_SHOP_QUANTITY, InvPrep.QuantityValues.class)
                        .orElse(ConfigSettings.getShopsDefaultStackSize())
                        .getStackSize(stockItem.getItem().getType());
        int change = InteractionHandler.shopItemClicked(player, shop, stockItem, doBuy, quantity);
        if (change > 0 && stockItem.getMaxStock()>0) {
            shop.getStockInventory().ifPresent(stock->stockItem.updateStock(stock));
            invalidate();
        }
    };

    @Override
    public void setOnClickListener(OnClickListener<MShop> listener) {
        throw new NotImplementedException("Can't set OnClickListener for Shop Menu Elements");
    }

    @Override
    public OnClickListener<MShop> getOnClickListerner() {
        return clickListener;
    }

    @Override
    public void fireClickEvent(Player viewer, int button, boolean shift) {
        clickListener.onClick(this, viewer, button, shift);
    }

    @Override
    public MShop copy() {
        MShop copy = new MShop(stockItem, myPosition);
        copy.setPosition(getPosition());
        return copy;
    }

    private IIcon icon;
    @Override
    public IIcon getIcon(Player viewer) {
        if (markedForRemoval)
            return removeMeIcon;
        int quantity = getParent().getPlayerState(viewer).getOfClass(MENU_SHOP_QUANTITY, InvPrep.QuantityValues.class)
                .orElse(ConfigSettings.getShopsDefaultStackSize())
                .getStackSize(stockItem.getItem().getType());
        if (icon == null || icon.render().getQuantity() != quantity) {
            //rebuild icon only if necessary, otherwise the animation
            // of the iicon will reset (and resources)
            if (stockItem.getOreDictEntry().isPresent()) {
                icon = IIcon.builder().addFrameItemStacks(
                        stockItem.getAllOreDictEntries().stream()
                                .map(e -> ItemStack.builder().fromSnapshot(e.getTemplate()).quantity(quantity).build())
                                .collect(Collectors.toList())
                ).setFPS(1d).build();
            } else {
                ItemStack display = stockItem.getItem().copy();
                display.setQuantity(quantity);
                icon = IIcon.of(display);
            }
        }
        return icon;
    }

    @Override
    public Text getName(Player viewer) {
        //marked for removal replaces the icon with barrier, this we need to supply a custom name to not display "Barrier"
        return markedForRemoval
                ? stockItem.getItem().get(Keys.DISPLAY_NAME).orElse(Text.of(stockItem.getItem().getType().getTranslation()))
                : null;
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
        ItemStack item = stockItem.getItem();
        List<Text> lore = item.get(Keys.ITEM_LORE).orElse(new LinkedList<>());

        Text currency = stockItem.getCurrency().getSymbol();
        int quantity = getParent().getPlayerState(viewer).getOfClass(MENU_SHOP_QUANTITY, InvPrep.QuantityValues.class)
                        .orElse(ConfigSettings.getShopsDefaultStackSize())
                        .getStackSize(item.getType());
        if (stockItem.getBuyPrice() != null) {
            if (quantity > 1) {
                double stackBuyPrice = quantity * stockItem.getBuyPrice();
                lore.add(Text.of(TextColors.RED,
                        VillagerShops.getTranslator().localText("shop.item.buy.stack")
                                .replace("%price%", String.format("%.2f", stackBuyPrice))
                                .replace("%itemprice%", Text.of(String.format("%.2f", stockItem.getBuyPrice())))
                                .replace("%currency%", currency)
                                .resolve(viewer).orElse(Text.of("shop.item.buy.stack"))
                        ));
            } else {
                lore.add(Text.of(TextColors.RED,
                        VillagerShops.getTranslator().localText("shop.item.buy.one")
                                .replace("%price%", String.format("%.2f", stockItem.getBuyPrice()))
                                .replace("%currency%", currency)
                                .resolve(viewer).orElse(Text.of("shop.item.buy.one"))
                        ));
            }
        }
        if (stockItem.getSellPrice() != null) {
            if (quantity > 1) {
                double stackSellPrice = quantity * stockItem.getSellPrice();
                lore.add(Text.of(TextColors.GREEN,
                        VillagerShops.getTranslator().localText("shop.item.sell.stack")
                                .replace("%price%", String.format("%.2f", stackSellPrice))
                                .replace("%itemprice%", Text.of(String.format("%.2f", stockItem.getSellPrice())))
                                .replace("%currency%", currency)
                                .resolve(viewer).orElse(Text.of("shop.item.sell.stack"))
                        ));
            } else {
                lore.add(Text.of(TextColors.GREEN,
                        VillagerShops.getTranslator().localText("shop.item.sell.one")
                                .replace("%price%", String.format("%.2f", stockItem.getSellPrice()))
                                .replace("%currency%", currency)
                                .resolve(viewer).orElse(Text.of("shop.item.sell.one"))
                        ));
            }
        }
        if (stockItem.getMaxStock() > 0) {
            lore.add(Text.of(TextColors.GRAY,
                    VillagerShops.getTranslator().localText("shop.item.stock")
                            .replace("%amount%", stockItem.getStocked())
                            .replace("%max%", stockItem.getMaxStock())
                            .resolve(viewer).orElse(Text.of("shop.item.stock"))
            ));
        }
        if (stockItem.getNbtFilter().equals(StockItem.FilterOptions.IGNORE_DAMAGE)) {
            lore.add(Text.of(TextColors.GRAY,
                    VillagerShops.getTranslator().localText("shop.item.filter.damage")
                            .resolve(viewer).orElse(Text.of("shop.item.filter.damage"))
            ));
        } else if (stockItem.getNbtFilter().equals(StockItem.FilterOptions.IGNORE_NBT)) {
            lore.add(Text.of(TextColors.GRAY,
                    VillagerShops.getTranslator().localText("shop.item.filter.nbt")
                            .resolve(viewer).orElse(Text.of("shop.item.filter.nbt"))
            ));
        } else if (stockItem.getNbtFilter().equals(StockItem.FilterOptions.OREDICT)) {
            lore.add(Text.of(TextColors.GRAY,
                    VillagerShops.getTranslator().localText("shop.item.filter.oredict")
                            .replace("%oredict%", stockItem.getOreDictEntry().get())
                            .resolve(viewer).orElse(Text.of("shop.item.filter.oredict"))
            ));
        }

        return lore;
    }

}
