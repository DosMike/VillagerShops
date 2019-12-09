package de.dosmike.sponge.vshop.shops;

import de.dosmike.sponge.vshop.Utilities;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.systems.GameDictHelper;
import org.spongepowered.api.GameDictionary;
import org.spongepowered.api.MinecraftVersion;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.item.DurabilityData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.item.inventory.entity.MainPlayerInventory;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class StockItem {
    private ItemStack item;

    public enum FilterOptions {
        /** All item NBT has to match */
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
        ;
        public static FilterOptions of(String v) {
            for (FilterOptions o : values()) {
                if (v.equalsIgnoreCase(o.name()))
                    return o;
            }
            throw new NoSuchElementException("No such Filter: "+v);
        }
    };
    private FilterOptions nbtfilter = FilterOptions.NORMAL;
    private String oreDictEntry = null;

    private Currency currency; //currency to use
    private Double sellprice = null, buyprice = null; //This IS the SINGLE ITEM price

    //caution: maxStock does not actually hold information about the size of the stock container!
    private int maxStock = 0, stocked = 0;

    /** This is a cache for valid itemStackSnapshots that match */
    private List<GameDictionary.Entry> oreDictEntries = new LinkedList<>();
    public List<GameDictionary.Entry> getAllOreDictEntries() {
        return oreDictEntries;
    }

    public StockItem(ItemStack itemstack, Double sellfor, Double buyfor, Currency currency, int stockLimit) {
        item = itemstack.copy();
        //legacy load
        double quantity = itemstack.getQuantity();
        this.item.setQuantity(1);
        if (sellfor != null && sellfor >= 0) this.sellprice = sellfor/quantity;
        if (buyfor != null && buyfor >= 0) this.buyprice = buyfor/quantity;
        this.currency = currency;
        this.maxStock = stockLimit;
    }
    public StockItem(ItemStack itemstack, Double sellfor, Double buyfor, Currency currency, int stockLimit, FilterOptions nbtfilter) {
        this(itemstack, sellfor, buyfor, currency, stockLimit);
        this.nbtfilter = nbtfilter;
    }
    /** forces filterOption to OREDICT
     * @throws IllegalArgumentException if oredict entry not found */
    public StockItem(String oreDictEntry, Double sellfor, Double buyfor, Currency currency, int stockLimit) {
        this.oreDictEntries = GameDictHelper.getAll(oreDictEntry);
        if (oreDictEntries.isEmpty()) throw new IllegalArgumentException("No Game Dictionary entry for "+oreDictEntry);

        this.oreDictEntry = oreDictEntry;
        item = oreDictEntries.get(0).getTemplate().createStack();
        if (sellfor != null && sellfor >= 0) this.sellprice = sellfor;
        if (buyfor != null && buyfor >= 0) this.buyprice = buyfor;
        this.currency = currency;
        this.maxStock = stockLimit;
        this.nbtfilter = FilterOptions.OREDICT;
    }

    public ItemStack getItem() {
        return item.copy();
    }

    public Double getBuyPrice() {
        return buyprice;
    }

    public Double getSellPrice() {
        return sellprice;
    }

    public void setBuyPrice(Double price) {
        buyprice = price;
    }

    public void setSellPrice(Double price) {
        sellprice = price;
    }

    public FilterOptions getNbtFilter() {
        return nbtfilter;
    }

    public Optional<String> getOreDictEntry() {
        return Optional.ofNullable(oreDictEntry);
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

    public int getStocked() {
        return maxStock > 0 ? stocked : item.getQuantity();
    }

    private static final DataQuery dqStackSize = DataQuery.of("Count"); //not interesting for filtering
    private static final DataQuery dqDamageMeta = DataQuery.of("UnsafeDamage"); //used for variants up to mc 1.12.2
    private Inventory filterInventory(Inventory inv) {
        long count = 0L;
        Inventory filtered;
        if (nbtfilter == FilterOptions.IGNORE_NBT) {
            filtered = inv.query(QueryOperationTypes.ITEM_TYPE.of(item.getType()));
        } else if (nbtfilter == FilterOptions.IGNORE_DAMAGE) {
            DataContainer j = item.toContainer()
                    .remove(dqDamageMeta)
                    .remove(dqStackSize);
            filtered = inv.query(QueryOperationTypes.ITEM_STACK_CUSTOM.of((i)->
                i.toContainer()
                        .remove(dqDamageMeta)
                        .remove(dqStackSize)
                        .equals(j)
            ));
        } else if (nbtfilter == FilterOptions.OREDICT) {
            filtered = inv.query(QueryOperationTypes.ITEM_STACK_CUSTOM.of(i ->
                oreDictEntries.stream().anyMatch(e->e.matches(i))
            ));
        } else if (nbtfilter == FilterOptions.TYPE_ONLY) {
            ItemType type = item.getType();
            int meta = (item.supports(Keys.ITEM_DURABILITY) ? 0 : item.toContainer().getInt(dqDamageMeta).orElse(0));
            filtered = inv.query(QueryOperationTypes.ITEM_STACK_CUSTOM.of(i -> {
                        if (!i.getType().equals(type)) return false;
                        int imeta = (i.supports(Keys.ITEM_DURABILITY) ? 0 : item.toContainer().getInt(dqDamageMeta).orElse(0));
                        return meta == imeta;
                    }
            ));
        } else {
            filtered = inv.query(QueryOperationTypes.ITEM_STACK_IGNORE_QUANTITY.of(item));
        }
        return filtered;
    }

    public void updateStock(Inventory container) {
        Inventory result = filterInventory(container);
        int count = 0;
        for (Inventory s : result.slots()) count += s.totalItems();
        stocked = Math.min(maxStock, count);
    }

    /**
     * Tries to take a certain amount of items represented by this item from the inventory
     *
     * @param quantity the amount of items supposed to take, overriding the default in item
     * @return the amount of items taken out of the inventory
     */
    public int getFrom(Inventory inv, int quantity) {
        int ammountLeft = quantity;
        Inventory result = filterInventory(inv);
        for (Inventory s : result.slots()) {
            ItemStack onSlot = s.poll(ammountLeft).orElse(null);
            if (onSlot == null) continue;
            if (onSlot.getQuantity() <= ammountLeft) {
                ammountLeft -= onSlot.getQuantity();
            } else {
                onSlot.setQuantity(onSlot.getQuantity() - ammountLeft);
                ammountLeft = 0;
                s.offer(onSlot);
            }
            if (ammountLeft == 0) break;
        }
        return quantity - ammountLeft;
    }

    /**
     * Tries to take the represented item with the given quantity from the inventory.
     * If the item has max stock, it will get the minimum of remaining stock and available items.
     *
     * @return the amount of items taken out of the inventory
     */
    public int getFrom(Inventory inv) {
        return getFrom(inv, maxStock <= 0 ? item.getQuantity() : Math.min(item.getQuantity(), maxStock - stocked));
    }

    public ShopResult buy(Player player, NPCguard shop, int maxAmount) {
        Optional<UniqueAccount> account = VillagerShops.getEconomy().getOrCreateAccount(player.getUniqueId());
        if (!account.isPresent()) return ShopResult.GENERIC_FAILURE;
        Account acc = account.get();
        boolean spendingsLimited = VillagerShops.getIncomeLimiter().isSpendingsLimited(player);

        Inventory playerInv = player.getInventory().query(MainPlayerInventory.class).union(player.getInventory().query(Hotbar.class));
        int amount = Math.min(invSpace(playerInv), maxAmount); //buy at max the specified amount
        if (amount <= 0) return ShopResult.CUSTOMER_INVENTORY_FULL;

        Optional<Inventory> stock = shop.getStockInventory();
        if (stock.isPresent()) {
            amount = Math.min(amount, invSupply(stock.get())); //reduce to what the stock can offer
            if (amount <= 0) return ShopResult.SHOPOWNER_MISSING_ITEMS;

            //shop owner account
            Optional<UUID> owner = shop.getShopOwner();
            if (!owner.isPresent()) return ShopResult.GENERIC_FAILURE;
            Optional<UniqueAccount> account2 = VillagerShops.getEconomy().getOrCreateAccount(owner.get());
            if (!account2.isPresent()) return ShopResult.GENERIC_FAILURE;

            //account transaction
            BigDecimal price = new BigDecimal(amount * buyprice / item.getQuantity());
            if (spendingsLimited) {
                BigDecimal spendable = VillagerShops.getIncomeLimiter().getRemainingSpendings(player).orElse(price);
                price = price.min(spendable);
                //pre check amount for special message
                int preamount = price.divide(BigDecimal.valueOf(buyprice / item.getQuantity()), RoundingMode.FLOOR).intValue();
                if (preamount < 1) return ShopResult.CUSTOMER_SPENDING_LIMIT;
                else { //recalculate amount/price for available spendings
                    amount = preamount;
                    price = new BigDecimal(amount * buyprice / item.getQuantity());
                }
            }
            TransactionResult res = acc.transfer(account2.get(), currency, price, Sponge.getCauseStackManager().getCurrentCause());

            if (res.getResult().equals(ResultType.ACCOUNT_NO_FUNDS)) { //not enough money for this amount of items
                //try to find a affordable amount
                BigDecimal balance = acc.getBalance(currency);
                if (spendingsLimited) {
                    BigDecimal spendable = VillagerShops.getIncomeLimiter().getRemainingSpendings(player).orElse(price);
                    balance = balance.min(spendable);
                    //pre check amount for special message
                    int preamount = balance.divide(BigDecimal.valueOf(buyprice / item.getQuantity()), RoundingMode.FLOOR).intValue();
                    if (preamount < 1) return ShopResult.CUSTOMER_SPENDING_LIMIT;
                }
                if (balance.compareTo(BigDecimal.ZERO) > 0) {
                    //resize stack to fit balance
                    int fixedamount = balance.divide(BigDecimal.valueOf(buyprice / item.getQuantity()), RoundingMode.FLOOR).intValue();
                    if (fixedamount < 1) return ShopResult.CUSTOMER_LOW_BALANCE;

                    //recalculate price
                    amount = fixedamount;
                    price = new BigDecimal(amount * buyprice / item.getQuantity());

                    //try again
                    TransactionResult res2 = acc.transfer(account2.get(), currency, price, Sponge.getCauseStackManager().getCurrentCause());
                    if (res2.getResult().equals(ResultType.ACCOUNT_NO_SPACE))
                        return ShopResult.SHOPOWNER_HIGH_BALANCE; //idk in what order transactions fail, so check this here as well
                    else if (!res2.getResult().equals(ResultType.SUCCESS))
                        return ShopResult.GENERIC_FAILURE;
                } else
                    return ShopResult.CUSTOMER_LOW_BALANCE;
            } else if (res.getResult().equals(ResultType.ACCOUNT_NO_SPACE)) return ShopResult.SHOPOWNER_HIGH_BALANCE;
            else if (!res.getResult().equals(ResultType.SUCCESS)) return ShopResult.GENERIC_FAILURE;

            if (spendingsLimited)
                VillagerShops.getIncomeLimiter().registerSpending(player, price);

            //item transaction
            ItemStack stack = getItem();
            stack.setQuantity(amount);
            playerInv.offer(stack);
            getFrom(stock.get(), amount);

            return ShopResult.OK(amount);
        } else {
            //account transaction
            BigDecimal price = new BigDecimal(amount * buyprice / item.getQuantity());
            if (spendingsLimited) {
                BigDecimal spendable = VillagerShops.getIncomeLimiter().getRemainingSpendings(player).orElse(price);
                price = price.min(spendable);
                //pre check amount for special message
                int preamount = price.divide(BigDecimal.valueOf(buyprice / item.getQuantity()), RoundingMode.FLOOR).intValue();
                if (preamount < 1) return ShopResult.CUSTOMER_SPENDING_LIMIT;
                else { //recalculate amount/price for available spendings
                    amount = preamount;
                    price = new BigDecimal(amount * buyprice / item.getQuantity());
                }
            }
            TransactionResult res = acc.withdraw(currency, price, Sponge.getCauseStackManager().getCurrentCause());

            if (res.getResult().equals(ResultType.ACCOUNT_NO_FUNDS)) { //not enough money for this amount of items
                //try to find a affordable amount
                BigDecimal balance = acc.getBalance(currency);
                if (spendingsLimited) {
                    BigDecimal spendable = VillagerShops.getIncomeLimiter().getRemainingSpendings(player).orElse(price);
                    balance = balance.min(spendable);
                    //pre check amount for special message
                    int preamount = balance.divide(BigDecimal.valueOf(buyprice / item.getQuantity()), RoundingMode.FLOOR).intValue();
                    if (preamount < 1) return ShopResult.CUSTOMER_SPENDING_LIMIT;
                }
                if (balance.compareTo(BigDecimal.ZERO) > 0) {
                    //resize stack to fit balance
                    int fixedamount = balance.divide(BigDecimal.valueOf(buyprice / item.getQuantity()), RoundingMode.FLOOR).intValue();
                    if (fixedamount < 1) return ShopResult.CUSTOMER_LOW_BALANCE;

                    //recalculate price
                    amount = fixedamount;
                    price = new BigDecimal(amount * buyprice / item.getQuantity());

                    //try again
                    TransactionResult res2 = acc.withdraw(currency, price, Sponge.getCauseStackManager().getCurrentCause());
                    if (!res2.getResult().equals(ResultType.SUCCESS)) return ShopResult.GENERIC_FAILURE;
                } else
                    return ShopResult.CUSTOMER_LOW_BALANCE;
            } else if (!res.getResult().equals(ResultType.SUCCESS)) return ShopResult.GENERIC_FAILURE;

            if (spendingsLimited)
                VillagerShops.getIncomeLimiter().registerSpending(player, price);

            //item transaction
            ItemStack stack = getItem();
            stack.setQuantity(amount);
            playerInv.offer(stack);

            return ShopResult.OK(amount);
        }
    }

    public ShopResult sell(Player player, NPCguard shop, int maxAmount) {
        Optional<UniqueAccount> account = VillagerShops.getEconomy().getOrCreateAccount(player.getUniqueId());
        if (!account.isPresent()) return ShopResult.GENERIC_FAILURE;
        Account acc = account.get();

        Inventory playerInv = player.getInventory().query(MainPlayerInventory.class).union(player.getInventory().query(Hotbar.class));
        int amount = Math.min(invSupply(playerInv), maxAmount); //buy at max the specified amount
        if (amount <= 0) return ShopResult.CUSTOMER_MISSING_ITEMS;

        Optional<Inventory> stock = shop.getStockInventory();
        if (stock.isPresent()) {
            amount = Math.min(amount, invSpace(stock.get())); //reduce to what the stock can offer
            if (maxStock > 0)
                amount = Math.min(amount, maxStock - stocked); //if we have a stock we may not exceed the stock limit (empty space for selling) - could be negative
            if (amount <= 0) return ShopResult.SHOPOWNER_INVENTORY_FULL;

            //shop owner account
            Optional<UUID> owner = shop.getShopOwner();
            if (!owner.isPresent()) return ShopResult.GENERIC_FAILURE;
            Optional<UniqueAccount> account2 = VillagerShops.getEconomy().getOrCreateAccount(owner.get());
            if (!account2.isPresent()) return ShopResult.GENERIC_FAILURE;

            //account transaction
            BigDecimal price = new BigDecimal(amount * sellprice / item.getQuantity());
            TransactionResult res = account2.get().transfer(acc, currency, price, Sponge.getCauseStackManager().getCurrentCause());

            if (res.getResult().equals(ResultType.ACCOUNT_NO_FUNDS)) return ShopResult.SHOPOWNER_LOW_BALANCE;
            else if (res.getResult().equals(ResultType.ACCOUNT_NO_SPACE)) return ShopResult.CUSTOMER_HIGH_BALANCE;
            else if (!res.getResult().equals(ResultType.SUCCESS)) return ShopResult.GENERIC_FAILURE;

            //item transaction
            ItemStack stack = getItem();
            stack.setQuantity(amount);
            stock.get().offer(stack);
            getFrom(playerInv, amount);

            return ShopResult.OK(amount);
        } else {
            //account transaction
            BigDecimal price = new BigDecimal(amount * sellprice / item.getQuantity());
            if (VillagerShops.getIncomeLimiter().isIncomeLimited(player)) {
                Optional<BigDecimal> limited = VillagerShops.getIncomeLimiter().getRemainingIncome(player);
                if (limited.isPresent()) {
                    price = price.min(limited.get()); //get the max possibble income for today

                    //reduce stack to march max income
                    int fixedamount = price.divide(BigDecimal.valueOf(sellprice / item.getQuantity()), RoundingMode.FLOOR).intValue();
                    if (fixedamount < 1) return ShopResult.CUSTOMER_INCOME_LIMIT;

                    //recalculate price for reduced stack
                    amount = fixedamount;
                    price = new BigDecimal(amount * sellprice / item.getQuantity());

                    //additional transaction
                    VillagerShops.getIncomeLimiter().registerIncome(player, price);
                }
            }
            TransactionResult res = acc.deposit(currency, price, Sponge.getCauseStackManager().getCurrentCause());

            if (res.getResult().equals(ResultType.ACCOUNT_NO_SPACE)) return ShopResult.CUSTOMER_HIGH_BALANCE;
            else if (!res.getResult().equals(ResultType.SUCCESS)) return ShopResult.GENERIC_FAILURE;

            //item transaction
            getFrom(playerInv, amount);

            return ShopResult.OK(amount);
        }
    }

    /**
     * figure out how much of item the inventory can hold<br><br>
     * A few months ago there Sponge asked what could be changes about the inventory API
     * it is only now, that I realize a inventory.tryOffer(ItemStack) or inventory.capacityFor(ItemStack) would be nice
     * count for each slot, how many of the item it could accept
     */
    private int invSpace(Inventory i) {
        int space = 0, c;
        Inventory result = filterInventory(i);
        for (Inventory s : result.slots()) {
            Slot slot = (Slot) s;
            c = slot.getStackSize();
            if (c > 0) space += (item.getMaxStackQuantity() - c);
        }
        space += (i.capacity() - i.size()) * item.getMaxStackQuantity();
        return space;
    }

    /**
     * figure out how much of item the inventory can supply
     */
    private int invSupply(Inventory i) {
        int available = 0;
        available = filterInventory(i).totalItems();
        return available;
    }

    /** Custom toString { info } */
    @Override
    public String toString() {
        return String.format("{ %s: %s, filter: %s buyprice: %s, sellprice: %s, currency: %s, limit: %s }",
                oreDictEntry!=null ? "oredict" : "item",
                oreDictEntry!=null ? oreDictEntry : Utilities.toString(item),
                nbtfilter.toString(),
                buyprice!=null ? buyprice.toString() : "N/A",
                sellprice!=null ? sellprice.toString() : "N/A",
                currency.getId(),
                maxStock > 0 ? maxStock : "N/A"
        );
    }
}
