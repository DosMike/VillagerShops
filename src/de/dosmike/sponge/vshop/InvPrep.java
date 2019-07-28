package de.dosmike.sponge.vshop;

import de.dosmike.sponge.megamenus.MegaMenus;
import de.dosmike.sponge.megamenus.api.IMenu;
import de.dosmike.sponge.megamenus.api.elements.IIcon;
import de.dosmike.sponge.megamenus.api.elements.MSpinner;
import de.dosmike.sponge.megamenus.api.elements.PositionProvider;
import de.dosmike.sponge.megamenus.api.elements.concepts.IElement;
import de.dosmike.sponge.megamenus.impl.RenderManager;
import de.dosmike.sponge.megamenus.impl.util.MenuUtil;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.property.SlotPos;
import org.spongepowered.api.text.Text;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class InvPrep {
    public static final int[] MENU_QUANTITY_SPINNER_VALUES = new int[]{64,32,16,1};

    List<StockItem> items = new LinkedList<>();

    public void addItem(StockItem si) {
        items.add(si);
        VillagerShops.instance.npcsDirty = true;
        updateMenu(true);
    }

    public void removeIndex(int i) {
        items.remove(i);
        VillagerShops.instance.npcsDirty = true;
        updateMenu(true);
    }

    public void setItem(int index, StockItem element) {
        items.set(index, element);
        VillagerShops.instance.npcsDirty = true;
        updateMenu(true);
    }

    public StockItem getItem(int index) {
        return items.get(index);
    }

    public int size() {
        return items.size();
    }

    private IMenu menu = MegaMenus.createMenu();
    public IMenu getMenu() {
        return menu;
    }

    void updateMenu(boolean full) {
        if (full) {
            for (int i = menu.pages(); i>0; i--) menu.removePage(i);
            int p=1, y=0, x=0;
            boolean itemsOnLastPage = false;
            for (StockItem item : items) {
                itemsOnLastPage = true;
                if (item.getBuyPrice() != null ||
                    item.getSellPrice() != null) {
                    MShop button = new MShop(item);
                    button.setPosition(new SlotPos(x,y));
                    menu.add(p, button);
                }
                if (++x > 8) {
                    x = 0;
                    if (++y > 4) {
                        y = 0;
                        p++;
                        itemsOnLastPage = false;
                    }
                }
            }
            if (!itemsOnLastPage) p--;
            //add quantity option toggle
            int bottomY = Math.max(2, Math.min((int)Math.ceil(size()/9.0)+1, 6))-1;
            MSpinnerIndex spnQuantity = MSpinnerIndex.builder()
                    .setName("shop.quantity.name")
                    .addValue(IIcon.of(ItemStack.of(ItemTypes.IRON_BLOCK, 64)), "shop.quantity.items64")
                    .addValue(IIcon.of(ItemStack.of(ItemTypes.IRON_INGOT, 32)), "shop.quantity.items32")
                    .addValue(IIcon.of(ItemStack.of(ItemTypes.IRON_INGOT, 16)), "shop.quantity.items16")
                    .addValue(IIcon.of(ItemStack.of(ItemTypes.IRON_NUGGET, 1)), "shop.quantity.items1")
                    .setOnChangeListener((oldValue, newValue, element, viewer) -> {
                        element.getParent()
                                .getPlayerState(viewer).set(
                                    MShop.MENU_SHOP_QUANTITY,
                                    MENU_QUANTITY_SPINNER_VALUES[newValue]);
                        for (int page = 1; page <= element.getParent().pages(); page++) {
                            Optional<IElement> el = MenuUtil.getElementAt(element.getParent(), page, element.getPosition());
                            el.ifPresent(e->{
                                if (e instanceof MSpinnerIndex) {
                                    ((MSpinnerIndex) e).setSelectedIndex(newValue);
                                }
                            });
                        }
                    })
                    .setPosition(new SlotPos(8, bottomY))
                    .build();
            for (int i = 1; i <= p; i++) {
                menu.add(i, spnQuantity.copy());
            }
        }
        menu.invalidate();
    }

    public void updateStock(Inventory container) {
        for (StockItem item : items) item.updateStock(container);
        updateMenu(false);
    }

}
