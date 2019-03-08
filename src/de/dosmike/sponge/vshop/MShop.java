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

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class MShop extends IElementImpl implements IClickable {

    private StockItem stockItem;
    private boolean isSellSlot;
    public MShop(StockItem stockItem, boolean sell) {
        this.stockItem = stockItem;
        this.isSellSlot = sell;
    }

    private OnClickListener clickListener = (element, player, button, shift)-> {
        UUID shopID = VillagerShops.openShops.get(player.getUniqueId());
        if (shopID == null) return;
        NPCguard shop = VillagerShops.getNPCfromShopUUID(shopID).orElse(null);
        if (shop == null) return;
        int change = InteractionHandler.shopItemClicked(player, shop, stockItem, !isSellSlot);
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
        MShop copy = new MShop(stockItem, isSellSlot);
        copy.setPosition(getPosition());
        return copy;
    }

    @Override
    public IIcon getIcon(Player viewer) {
        return IIcon.of(stockItem.getItem());
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
        if (isSellSlot) {
            if (item.getQuantity() > 1) {
                lore.add(Text.of(TextColors.GREEN,
                        VillagerShops.getTranslator().localText("shop.item.sell.stack")
                                .replace("%price%", String.format("%.2f", stockItem.getSellPrice()))
                                .replace("%itemprice%", Text.of(String.format("%.2f", stockItem.getSellPrice() / (double) item.getQuantity())))
                                .replace("%currency%", currency)
                                .resolve(viewer).orElse(
                                        Text.of(TextColors.GREEN, "Sell for: ", TextColors.WHITE, String.format("%.2f", stockItem.getSellPrice()), currency, String.format(" (á %.2f", stockItem.getSellPrice() / (double) item.getQuantity()), currency, ')')
                                )
                        ));
            } else {
                lore.add(Text.of(TextColors.GREEN,
                        VillagerShops.getTranslator().localText("shop.item.sell.one")
                                .replace("%price%", String.format("%.2f", stockItem.getSellPrice()))
                                .replace("%currency%", currency)
                                .resolve(viewer).orElse(
                                        Text.of(TextColors.GREEN, "Sell for: ", TextColors.WHITE, String.format("%.2f", stockItem.getSellPrice()), currency)
                                )
                        ));
            }
        } else {
            if (item.getQuantity() > 1) {
                lore.add(Text.of(TextColors.RED,
                        VillagerShops.getTranslator().localText("shop.item.buy.stack")
                                .replace("%price%", String.format("%.2f", stockItem.getBuyPrice()))
                                .replace("%itemprice%", Text.of(String.format("%.2f", stockItem.getBuyPrice() / (double) item.getQuantity())))
                                .replace("%currency%", currency)
                                .resolve(viewer).orElse(
                                        Text.of(TextColors.RED, "Buy for: ", TextColors.WHITE, String.format("%.2f", stockItem.getBuyPrice()), currency, String.format(" (á %.2f", stockItem.getBuyPrice() / (double) item.getQuantity()), currency, ')')
                                )
                        ));
            } else {
                lore.add(Text.of(TextColors.RED,
                        VillagerShops.getTranslator().localText("shop.item.buy.one")
                                .replace("%price%", String.format("%.2f", stockItem.getBuyPrice()))
                                .replace("%currency%", currency)
                                .resolve(viewer).orElse(
                                        Text.of(TextColors.RED, "Buy for: ", TextColors.WHITE, String.format("%.2f", stockItem.getBuyPrice()), currency)
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
                            Text.of(String.format("In Stock: %d/%d", stockItem.getStocked(), stockItem.getMaxStock()))
                    )));

        return lore;
    }

}
