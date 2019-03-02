package de.dosmike.sponge.vshop;

import de.dosmike.sponge.megamenus.MegaMenus;
import de.dosmike.sponge.megamenus.api.IMenu;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.property.SlotPos;

import java.util.LinkedList;
import java.util.List;

public class InvPrep {
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
            menu.removePage(1);
            int y=0, x=0;
            for (StockItem item : items) {
                if (item.getBuyPrice() != null) {
                    MShop button = new MShop(item, false);
                    button.setPosition(new SlotPos(x,y));
                    menu.add(button);
                }
                if (item.getSellPrice() != null) {
                    MShop button = new MShop(item, true);
                    button.setPosition(new SlotPos(x,y+1));
                    menu.add(button);
                }
                if (++x > 8) { x = 0; y += 2; }
            }
        }
        menu.invalidate();
    }

    public void updateStock(Inventory container) {
        for (StockItem item : items) item.updateStock(container);
        updateMenu(false);
    }

}
