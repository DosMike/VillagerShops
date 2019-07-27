package de.dosmike.sponge.vshop;

import de.dosmike.sponge.megamenus.api.elements.IIcon;
import de.dosmike.sponge.megamenus.api.elements.concepts.IClickable;
import de.dosmike.sponge.megamenus.api.listener.OnClickListener;
import de.dosmike.sponge.megamenus.impl.elements.IElementImpl;
import org.apache.commons.lang3.NotImplementedException;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public final class MShop extends IElementImpl implements IClickable {

    public static final String MENU_SHOP_QUANTITY = "quantity";

    private StockItem stockItem;
    public MShop(StockItem stockItem) {
        this.stockItem = stockItem;
    }

    private OnClickListener clickListener = (element, player, button, shift)-> {
        boolean doBuy;
        if (button == MouseEvent.BUTTON1) {
            doBuy = true;
        } else if (button == MouseEvent.BUTTON2) {
            doBuy = false;
        } else return;
        UUID shopID = VillagerShops.openShops.get(player.getUniqueId());
        if (shopID == null) return;
        NPCguard shop = VillagerShops.getNPCfromShopUUID(shopID).orElse(null);
        if (shop == null) return;

        int quantity = Math.min(
                getParent().getPlayerState(player).getInt(MENU_SHOP_QUANTITY).orElse(InvPrep.MENU_QUANTITY_SPINNER_VALUES[0]),
                stockItem.getItem().getMaxStackQuantity()
        );
        int change = InteractionHandler.shopItemClicked(player, shop, stockItem, doBuy, quantity);
        if (change > 0 && stockItem.getMaxStock()>0) {
            shop.getStockInventory().ifPresent(stock->stockItem.updateStock(stock));
            invalidate();
        }
    };

    @Override
    public void setOnClickListener(OnClickListener listener) {
        throw new NotImplementedException("Can't set OnClickListener for Shop Menu Elements");
    }

    @Override
    public OnClickListener getOnClickListerner() {
        return clickListener;
    }

    @Override
    public void fireClickEvent(Player viewer, int button, boolean shift) {
        clickListener.onClick(this, viewer, button, shift);
    }

    @Override
    public MShop copy() {
        MShop copy = new MShop(stockItem);
        copy.setPosition(getPosition());
        return copy;
    }

    @Override
    public IIcon getIcon(Player viewer) {
        int quantity = Math.min(
                getParent().getPlayerState(viewer).getInt(MENU_SHOP_QUANTITY).orElse(InvPrep.MENU_QUANTITY_SPINNER_VALUES[0]),
                stockItem.getItem().getMaxStackQuantity()
        );
        ItemStack display = stockItem.getItem().copy();
        display.setQuantity(quantity);
        return IIcon.of(display);
    }

    @Override
    public Text getName(Player viewer) {
        return null; //default name - specified by icon
    }

    @Override
    public List<Text> getLore(Player viewer) {
        ItemStack item = stockItem.getItem();
        List<Text> lore = stockItem.getItem().get(Keys.ITEM_LORE).orElse(new LinkedList<>());

        Text currency = stockItem.getCurrency().getSymbol();
        int quantity = Math.min(
                getParent().getPlayerState(viewer).getInt(MENU_SHOP_QUANTITY).orElse(InvPrep.MENU_QUANTITY_SPINNER_VALUES[0]),
                stockItem.getItem().getMaxStackQuantity()
        );
        if (stockItem.getBuyPrice() != null) {
            if (quantity > 1) {
                double stackBuyPrice = quantity * stockItem.getBuyPrice();
                lore.add(Text.of(TextColors.RED,
                        VillagerShops.getTranslator().localText("shop.item.buy.stack")
                                .replace("%price%", String.format("%.2f", stackBuyPrice))
                                .replace("%itemprice%", Text.of(String.format("%.2f", stockItem.getBuyPrice())))
                                .replace("%currency%", currency)
                                .resolve(viewer).orElse(
                                Text.of("shop.item.buy.stack")
                        )
                ));
            } else {
                lore.add(Text.of(TextColors.RED,
                        VillagerShops.getTranslator().localText("shop.item.buy.one")
                                .replace("%price%", String.format("%.2f", stockItem.getBuyPrice()))
                                .replace("%currency%", currency)
                                .resolve(viewer).orElse(
                                Text.of("shop.item.buy.one")
                        )
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
                                .resolve(viewer).orElse(
                                        Text.of("shop.item.sell.stack")
                                )
                        ));
            } else {
                lore.add(Text.of(TextColors.GREEN,
                        VillagerShops.getTranslator().localText("shop.item.sell.one")
                                .replace("%price%", String.format("%.2f", stockItem.getSellPrice()))
                                .replace("%currency%", currency)
                                .resolve(viewer).orElse(
                                        Text.of("shop.item.sell.one")
                                )
                        ));
            }
        }
        if (stockItem.getMaxStock() > 0)
            lore.add(Text.of(TextColors.GRAY,
                    VillagerShops.getTranslator().localText("shop.item.stock")
                            .replace("%amount%", stockItem.getStocked())
                            .replace("%max%", stockItem.getMaxStock())
                            .resolve(viewer).orElse(
                            Text.of("shop.item.stock")
                    )));

        return lore;
    }

}
