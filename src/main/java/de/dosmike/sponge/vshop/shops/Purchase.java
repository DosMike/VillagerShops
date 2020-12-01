package de.dosmike.sponge.vshop.shops;

import com.google.inject.internal.cglib.core.$DuplicatesPredicate;
import de.dosmike.sponge.vshop.Utilities;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.integrations.toomuchstock.Preview;
import de.dosmike.sponge.vshop.systems.ShopType;
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
    private final ShopType shopType;

    private final UniqueAccount playerAccount;
    private final UniqueAccount shopAccount;
    public Purchase(StockItem item, Player player, ShopEntity shop) throws FailedPreconditionException {
        this.stockItem = item;
        this.player = player;
        this.shop = shop;
        this.shopType = ShopType.fromInstance(shop);

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

        Inventory playerInv = player.getInventory()
                .query(QueryOperationTypes.INVENTORY_TYPE.of(MainPlayerInventory.class))
                .union(player.getInventory().query(QueryOperationTypes.INVENTORY_TYPE.of(Hotbar.class)));

        //see how many items max the player can take into inventory
        int amount = Math.min(stockItem.invSpace(playerInv), maxAmount);
        if (amount <= 0) return Result.CUSTOMER_INVENTORY_FULL;

        // If we have a stock we can assume it's a player shop, because that's how they are made.
        // I purposefully do not count sales through shops towards any limits (we'll see how the community takes it)
        Optional<Inventory> stock = shop.getStockInventory();
        if (stock.isPresent()) {
            //reduce to what the stock can offer
            amount = Math.min(amount, stockItem.invSupply(stock.get()));
            if (amount <= 0) return Result.SHOPOWNER_MISSING_ITEMS;
            //shop owner account
            if (shopAccount == null) return Result.GENERIC_FAILURE;
        }

        // From the items that can be traded, get the price information
        Preview purchaseInformation = VillagerShops.getPriceCalculator().getPurchaseInformation(
                stockItem.getItem(shopType),
                amount, //using this amount here will reduce computation cost
                BigDecimal.valueOf(stockItem.getBuyPrice()),
                stockItem.getCurrency(),
                shop.getIdentifier(),
                player.getUniqueId()
        );

        if (purchaseInformation.getLimitAccount() <= 0)
            return Result.CUSTOMER_LOW_BALANCE;
        //TODO check item transaction limit for separate error message
        amount = Math.min(purchaseInformation.getAffordableAmount(), amount);
        if (amount <= 0)
            return Result.CUSTOMER_SPENDING_LIMIT;

        //Do the banking
        BigDecimal price = purchaseInformation.getCumulativeValueFor(amount);

        TransactionResult res;
        if (shopType == ShopType.PlayerShop)
            res = playerAccount.transfer(shopAccount, stockItem.getCurrency(), price, Sponge.getCauseStackManager().getCurrentCause());
        else
            res = playerAccount.withdraw(stockItem.getCurrency(), price, Sponge.getCauseStackManager().getCurrentCause());

        if (res.getResult().equals(ResultType.ACCOUNT_NO_SPACE))
            return Result.SHOPOWNER_HIGH_BALANCE; //idk in what order transactions fail, so check this here as well
        else if (!res.getResult().equals(ResultType.SUCCESS))
            return Result.GENERIC_FAILURE;

        // All checks passed, confirm purchase
        purchaseInformation.confirm(amount);

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
            playerInv.offer(stockItem.createItem(amount, shopType));

        return Result.OK(amount, price);
    }

    public Result sell(int maxAmount) {

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

        // From the items that can be traded, get the price information
        Preview sellingInformation = VillagerShops.getPriceCalculator().getSellingInformation(
                stockItem.getItem(shopType),
                amount, //using this amount here will reduce computation cost
                BigDecimal.valueOf(stockItem.getSellPrice()),
                stockItem.getCurrency(),
                shop.getIdentifier(),
                player.getUniqueId()
        );

        if (sellingInformation.getLimitAccount() <= 0)
            return Result.CUSTOMER_HIGH_BALANCE;
        //TODO check item transaction limit for separate error message
        amount = Math.min(sellingInformation.getAffordableAmount(), amount);
        if (amount <= 0)
            return Result.CUSTOMER_INCOME_LIMIT;

        //bank transaction
        BigDecimal price = sellingInformation.getCumulativeValueFor(amount);

        TransactionResult res;
        if (shopType == ShopType.PlayerShop)
            res = shopAccount.transfer(playerAccount, stockItem.getCurrency(), price, Sponge.getCauseStackManager().getCurrentCause());
        else
            res = playerAccount.deposit(stockItem.getCurrency(), price, Sponge.getCauseStackManager().getCurrentCause());

        if (res.getResult().equals(ResultType.ACCOUNT_NO_FUNDS)) return Result.SHOPOWNER_LOW_BALANCE;
        else if (res.getResult().equals(ResultType.ACCOUNT_NO_SPACE)) return Result.CUSTOMER_HIGH_BALANCE;
        else if (!res.getResult().equals(ResultType.SUCCESS)) return Result.GENERIC_FAILURE;

        sellingInformation.confirm(amount);

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
            stockItem.getPluginFilter().ifPresent(pif -> removed.forEach(item -> pif.consume(item, shopType)));
        }

        return Result.OK(amount, price);
    }

    public static class Result {
        final int count;
        final BigDecimal finalPrice;
        final String msg;

        Result(int items, BigDecimal price) {
            count = items;
            finalPrice = price;
            msg = null;
        }
        Result(String message) {
            count = 0;
            finalPrice = BigDecimal.ZERO;
            msg = message;
        }

        public int getTradedItems() {
            return count;
        }

        public String getMessage() {
            return msg;
        }

        public static final Result GENERIC_FAILURE = new Result("shop.generic.transactionfailure");
        public static final Result INCOMPATIBLE_SHOPTYPE = new Result("shop.generic.incompatibletype");
        public static final Result CUSTOMER_LOW_BALANCE = new Result("shop.customer.lowbalance");
        public static final Result SHOPOWNER_LOW_BALANCE = new Result( "shop.shopowner.lowbalance");
        public static final Result CUSTOMER_HIGH_BALANCE = new Result("shop.customer.highbalance");
        public static final Result SHOPOWNER_HIGH_BALANCE = new Result("shop.customer.highbalance");
        public static final Result CUSTOMER_MISSING_ITEMS = new Result("shop.customer.missingitems");
        public static final Result SHOPOWNER_MISSING_ITEMS = new Result("shop.shopowner.missingitems");
        public static final Result CUSTOMER_INVENTORY_FULL = new Result("shop.customer.inventoryfull");
        public static final Result SHOPOWNER_INVENTORY_FULL = new Result("shop.shopowner.inventoryfull");
        public static final Result CUSTOMER_INCOME_LIMIT = new Result("shop.customer.incomelimit");
        public static final Result CUSTOMER_SPENDING_LIMIT = new Result("shop.customer.spendinglimit");

        public static Result OK(int items, BigDecimal finalPrice) {
            return new Result(items, finalPrice);
        }

    }
}
