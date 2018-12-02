package de.dosmike.sponge.vshop;

public class ShopResult {
    int count;
    String msg;

    ShopResult(int items, String message) {
        count = items;
        msg = message;
    }

    ;

    public int getTradedItems() {
        return count;
    }

    public String getMessage() {
        return msg;
    }

    public static final ShopResult GENERIC_FAILURE = new ShopResult(0, "shop.generic.transactionfailure");
    public static final ShopResult CUSTOMER_LOW_BALANCE = new ShopResult(0, "shop.customer.lowbalance");
    public static final ShopResult SHOPOWNER_LOW_BALANCE = new ShopResult(0, "shop.shopowner.lowbalance");
    public static final ShopResult CUSTOMER_HIGH_BALANCE = new ShopResult(0, "shop.customer.highbalance");
    public static final ShopResult SHOPOWNER_HIGH_BALANCE = new ShopResult(0, "shop.customer.highbalance");
    public static final ShopResult CUSTOMER_MISSING_ITEMS = new ShopResult(0, "shop.customer.missingitems");
    public static final ShopResult SHOPOWNER_MISSING_ITEMS = new ShopResult(0, "shop.shopowner.missingitems");
    public static final ShopResult CUSTOMER_INVENTORY_FULL = new ShopResult(0, "shop.customer.inventoryfull");
    public static final ShopResult SHOPOWNER_INVENTORY_FULL = new ShopResult(0, "shop.shopowner.inventoryfull");
    public static final ShopResult CUSTOMER_INCOME_LIMIT = new ShopResult(0, "shop.customer.incomelimit");
    public static final ShopResult CUSTOMER_SPENDING_LIMIT = new ShopResult(0, "shop.customer.spendinglimit");

    public static ShopResult OK(int items) {
        return new ShopResult(items, null);
    }

}
