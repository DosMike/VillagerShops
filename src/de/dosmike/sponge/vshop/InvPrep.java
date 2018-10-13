package de.dosmike.sponge.vshop;

import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.property.InventoryDimension;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.item.inventory.property.SlotPos;
import org.spongepowered.api.item.inventory.type.OrderedInventory;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class InvPrep {

    public static final int RowsPerItem = 2; // 2 and 3 tested, 2 now recommended

    List<StockItem> items = new LinkedList<>();

    public void addItem(StockItem si) {
        items.add(si);
        VillagerShops.instance.npcsDirty = true;
    }

    public void removeIndex(int i) {
        items.remove(i);
        VillagerShops.instance.npcsDirty = true;
    }

    public void setItem(int index, StockItem element) {
        items.set(index, element);
        VillagerShops.instance.npcsDirty = true;
    }

    public StockItem getItem(int index) {
        return items.get(index);
    }

    public int size() {
        return items.size();
    }

    public Inventory getInventory(Inventory.Builder ib, UUID player) {
        Inventory inv = ib
                .property("inventorydimension", new InventoryDimension(9, 2 +
                        ((int) Math.ceil((double) items.size() / 9.0) - 1) * RowsPerItem)
                )
                .build(VillagerShops.getInstance());

        int row = 0, col = 0;
        for (int i = 0; i < items.size(); i++) {

            inv.query(SlotPos.of(col, row)).offer(items.get(i).getBuyDisplayItem(row * 9 + col, player));
            inv.query(SlotPos.of(col, row + 1)).offer(items.get(i).getSellDisplayItem((row + 1) * 9 + col, player));

            if (++col >= 9) {
                col = 0;
                row += RowsPerItem;
            }
        }

        return inv;
    }

    /**
     * @return 0 for buy, 1 for sell, >1 for spacer
     */
    public static int isSlotBuySell(int inventoryIndex) {
        return ((int) (inventoryIndex / 9)) % RowsPerItem;
    }

    /**
     * @return the items index for the given inventory slot or -1 if the slot is a spacer
     */
    public static int slotToIndex(int inventorySlot) {
        int y = (int) (inventorySlot / 9);
        int a = y % RowsPerItem;
        y /= RowsPerItem;
        if (a > 1) return -1;
        int x = inventorySlot % 9;
        return y * 9 + x;
    }

    public void updateStock(Inventory container) {
        for (StockItem item : items) item.updateStock(container);
    }

    /**
     * dealing with open inventories this will target a OrderedInventory
     * (more precisely a OrderedInventoryAdapter at runtime)
     */
    public void updateInventory(Inventory view, UUID player) {

        if (!(view instanceof OrderedInventory)) {
            VillagerShops.w("Can't update view, Inventory is not ordered?");
            return;
        }
        OrderedInventory oi = (OrderedInventory) view;

        int row = 0, col = 0;
        for (int i = 0; i < items.size(); i++) {

            oi.set(new SlotIndex(row * 9 + col), items.get(i).getBuyDisplayItem(row * 9 + col, player));
            oi.set(new SlotIndex((row + 1) * 9 + col), items.get(i).getSellDisplayItem((row + 1) * 9 + col, player));

            if (++col >= 9) {
                col = 0;
                row += RowsPerItem;
            }
        }
    }
}
