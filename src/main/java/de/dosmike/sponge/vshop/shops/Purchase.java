package de.dosmike.sponge.vshop.shops;

import de.dosmike.sponge.vshop.Utilities;
import de.dosmike.sponge.vshop.VillagerShops;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.item.inventory.entity.MainPlayerInventory;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** I feel this class is necessary, because the act of performing a purchase is quite complicated actually */
public class Purchase {

    public static class FailedPreconditionException extends Exception {
        private final Result failedResult;
        FailedPreconditionException(Result result) {
            assert result.getTradedItems()==0 : "Illegal Non-Exceptional Result";
            failedResult = result;
        }
        Result getResult() {
            return failedResult;
        }
    }

    private final StockItem stockItem;
    private final Player player;
    private final ShopEntity shop;

    private final UniqueAccount playerAccount;
    private final UniqueAccount shopAccount;
    public Purchase(StockItem item, Player player, ShopEntity shop) throws FailedPreconditionException {
        this.stockItem = item;
        this.player = player;
        this.shop = shop;

        playerAccount = VillagerShops.getEconomy().getOrCreateAccount(player.getUniqueId())
                .orElseThrow(()->new FailedPreconditionException(Result.GENERIC_FAILURE));
        shopAccount = shop.getShopOwner().flatMap(owner->VillagerShops.getEconomy().getOrCreateAccount(owner))
                .orElse(null);
    }

    /**
     * Tries to take a certain amount of items represented by this item from the inventory
     *
     * @param quantity the amount of items supposed to take, overriding the default in item
     * @return the ItemStacks taken from the inventory
     */
    private Set<ItemStack> getFrom(Inventory inv, int quantity) {
        int ammountLeft = quantity;
        Set<ItemStack> stacks = new HashSet<>();
        Inventory result = stockItem.filterInventory(inv);
        for (Inventory s : result.slots()) {
            ItemStack onSlot = s.poll(ammountLeft).orElse(null);
            if (onSlot == null) continue;
            if (onSlot.getQuantity() <= ammountLeft) {
                stacks.add(onSlot);

                ammountLeft -= onSlot.getQuantity();
            } else {
                ItemStack taken = onSlot.copy();
                taken.setQuantity(ammountLeft);
                stacks.add(taken);

                onSlot.setQuantity(onSlot.getQuantity() - ammountLeft);
                ammountLeft = 0;
                s.offer(onSlot);
            }
            if (ammountLeft == 0) break;
        }
        return stacks;
    }

    public Result buy(int maxAmount) {

        boolean spendingsLimited = VillagerShops.getIncomeLimiter().isSpendingsLimited(player);

        Inventory playerInv = player.getInventory()
                .query(QueryOperationTypes.INVENTORY_TYPE.of(MainPlayerInventory.class))
                .union(player.getInventory().query(QueryOperationTypes.INVENTORY_TYPE.of(Hotbar.class)));

        //buy at max the specified amount
        int amount = Math.min(stockItem.invSpace(playerInv), maxAmount);
        if (amount <= 0) return Result.CUSTOMER_INVENTORY_FULL;

        Optional<Inventory> stock = shop.getStockInventory();
        if (stock.isPresent()) {
            //reduce to what the stock can offer
            amount = Math.min(amount, stockItem.invSupply(stock.get()));
            if (amount <= 0) return Result.SHOPOWNER_MISSING_ITEMS;
            //shop owner account
            if (shopAccount == null) return Result.GENERIC_FAILURE;
        }
        //temporary price based on space & choice
        BigDecimal price = new BigDecimal(amount * stockItem.getBuyPrice());
        //try to find a affordable amount
        BigDecimal balance = playerAccount.getBalance(stockItem.getCurrency());
        if (spendingsLimited) {
            BigDecimal spendable = VillagerShops.getIncomeLimiter().getRemainingSpendings(player).orElse(price);
            balance = balance.min(spendable);
            //pre check amount for special message
            int preamount = balance.divide(BigDecimal.valueOf(stockItem.getBuyPrice()), RoundingMode.FLOOR).intValue();
            if (preamount < 1) return Result.CUSTOMER_SPENDING_LIMIT;
        }
        if (balance.compareTo(BigDecimal.ZERO) <= 0)
            return Result.CUSTOMER_LOW_BALANCE;

        //resize stack and price to 'fit' the balance
        amount = Math.min(amount, balance.divide(BigDecimal.valueOf(stockItem.getBuyPrice()), RoundingMode.FLOOR).intValue());
        if (amount < 1) return Result.CUSTOMER_LOW_BALANCE;
        price = new BigDecimal(amount * stockItem.getBuyPrice());

        //Do the banking
        TransactionResult res;
        if (stock.isPresent())
            res = playerAccount.transfer(shopAccount, stockItem.getCurrency(), price, Sponge.getCauseStackManager().getCurrentCause());
        else
            res = playerAccount.withdraw(stockItem.getCurrency(), price, Sponge.getCauseStackManager().getCurrentCause());

        if (res.getResult().equals(ResultType.ACCOUNT_NO_SPACE))
            return Result.SHOPOWNER_HIGH_BALANCE; //idk in what order transactions fail, so check this here as well
        else if (!res.getResult().equals(ResultType.SUCCESS))
            return Result.GENERIC_FAILURE;

        if (spendingsLimited)
            VillagerShops.getIncomeLimiter().registerSpending(player, price);

        //item transaction
        //is trying to actually move items a good idea?
        if (stock.isPresent())
            getFrom(stock.get(),amount).forEach(stack->{
                InventoryTransactionResult result = playerInv.offer(stack);
                result.getRejectedItems().forEach(rejected->{ // drop the items into the world if they for some reason do not fit
                    Item drop = (Item)player.getLocation().getExtent().createEntity(EntityTypes.ITEM, player.getPosition());
                    drop.offer(Keys.REPRESENTED_ITEM, rejected);
                    player.getLocation().getExtent().spawnEntity(drop);
                });
            });
        else
            playerInv.offer(stockItem.createItem(amount, !shop.getShopOwner().isPresent()));

        return Result.OK(amount);
    }

    public Result sell(int maxAmount) {

        boolean incomeLimited = VillagerShops.getIncomeLimiter().isIncomeLimited(player);

        Inventory playerInv = player.getInventory()
                .query(QueryOperationTypes.INVENTORY_TYPE.of(MainPlayerInventory.class))
                .union(player.getInventory().query(QueryOperationTypes.INVENTORY_TYPE.of(Hotbar.class)));

        //buy at max the specified amount
        int amount = Math.min(stockItem.invSupply(playerInv), maxAmount);
        if (amount <= 0) return Result.CUSTOMER_MISSING_ITEMS;
        //limit by stock capacity
        Optional<Inventory> stock = shop.getStockInventory();
        if (stock.isPresent()) {
            amount = Math.min(amount, stockItem.invSpace(stock.get())); //reduce to what the stock can offer
            if (stockItem.getMaxStock() > 0)
                amount = Math.min(amount, stockItem.getMaxStock() - stockItem.getStocked()); //if we have a stock we may not exceed the stock limit (empty space for selling) - could be negative
            if (amount <= 0) return Result.SHOPOWNER_INVENTORY_FULL;
            //shop owner account
            if (shopAccount == null) return Result.GENERIC_FAILURE;
        }
        //temporary price for back checking
        BigDecimal price = new BigDecimal(amount * stockItem.getSellPrice());
        if (incomeLimited) {
            Optional<BigDecimal> limited = VillagerShops.getIncomeLimiter().getRemainingIncome(player);
            if (limited.isPresent()) {
                price = price.min(limited.get()); //get the max possibble income for today

                //reduce stack to match max income
                amount = Math.min(amount, price.divide(BigDecimal.valueOf(stockItem.getSellPrice()), RoundingMode.FLOOR).intValue());
                if (amount < 1) return Result.CUSTOMER_INCOME_LIMIT;
                //recalculate price for reduced stack
                price = new BigDecimal(amount * stockItem.getSellPrice());

            }
        }
        //bank transaction
        TransactionResult res;
        if (stock.isPresent())
            res = shopAccount.transfer(playerAccount, stockItem.getCurrency(), price, Sponge.getCauseStackManager().getCurrentCause());
        else
            res = playerAccount.deposit(stockItem.getCurrency(), price, Sponge.getCauseStackManager().getCurrentCause());

        if (res.getResult().equals(ResultType.ACCOUNT_NO_FUNDS)) return Result.SHOPOWNER_LOW_BALANCE;
        else if (res.getResult().equals(ResultType.ACCOUNT_NO_SPACE)) return Result.CUSTOMER_HIGH_BALANCE;
        else if (!res.getResult().equals(ResultType.SUCCESS)) return Result.GENERIC_FAILURE;

        if (incomeLimited)
            VillagerShops.getIncomeLimiter().registerIncome(player, price);

        //item transaction
        if (stock.isPresent()) {
            getFrom(playerInv,amount).forEach(stack->{
                InventoryTransactionResult result = stock.get().offer(stack);
                result.getRejectedItems().forEach(rejected->{
                    // drop the items into the world if they for some reason do not fit
//                    Item drop = (Item)player.getLocation().getExtent().createEntity(EntityTypes.ITEM, player.getPosition());
//                    drop.offer(Keys.REPRESENTED_ITEM, rejected);
//                    player.getLocation().getExtent().spawnEntity(drop);
                    VillagerShops.w("Please Report this: Could not stack item into stock container and deleted it! " +
                            "This means that a player-shop was scammed out of items due to inaccurate capacity predictions. " +
                            "Does that item not stack properly?\n  Item: %s", Utilities.toString(rejected));
                });
            });
        } else {
            Set<ItemStack> removed = getFrom(playerInv, amount);
            //notify the plugin, that these items are now gone
            stockItem.getPluginFilter().ifPresent(pif -> removed.forEach(item -> pif.consume(item, !shop.getShopOwner().isPresent())));
        }

        return Result.OK(amount);
    }

    public static class Result {
        final int count;
        final String msg;

        Result(int items, String message) {
            count = items;
            msg = message;
        }

        public int getTradedItems() {
            return count;
        }

        public String getMessage() {
            return msg;
        }

        public static final Result GENERIC_FAILURE = new Result(0, "shop.generic.transactionfailure");
        public static final Result INCOMPATIBLE_SHOPTYPE = new Result(0, "shop.generic.incompatibletype");
        public static final Result CUSTOMER_LOW_BALANCE = new Result(0, "shop.customer.lowbalance");
        public static final Result SHOPOWNER_LOW_BALANCE = new Result(0, "shop.shopowner.lowbalance");
        public static final Result CUSTOMER_HIGH_BALANCE = new Result(0, "shop.customer.highbalance");
        public static final Result SHOPOWNER_HIGH_BALANCE = new Result(0, "shop.customer.highbalance");
        public static final Result CUSTOMER_MISSING_ITEMS = new Result(0, "shop.customer.missingitems");
        public static final Result SHOPOWNER_MISSING_ITEMS = new Result(0, "shop.shopowner.missingitems");
        public static final Result CUSTOMER_INVENTORY_FULL = new Result(0, "shop.customer.inventoryfull");
        public static final Result SHOPOWNER_INVENTORY_FULL = new Result(0, "shop.shopowner.inventoryfull");
        public static final Result CUSTOMER_INCOME_LIMIT = new Result(0, "shop.customer.incomelimit");
        public static final Result CUSTOMER_SPENDING_LIMIT = new Result(0, "shop.customer.spendinglimit");

        public static Result OK(int items) {
            return new Result(items, null);
        }

    }
}
