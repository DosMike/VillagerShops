package de.dosmike.sponge.vshop.systems;

import de.dosmike.sponge.vshop.shops.ShopEntity;

public enum ShopType {
    PlayerShop,
    AdminShop;
    public static ShopType fromInstance(ShopEntity shop) {
        return shop.getShopOwner().isPresent() ? PlayerShop : AdminShop;
    }
    public static ShopType fromInternalFlag(boolean shopTypeFlag) {
        return shopTypeFlag ? AdminShop : PlayerShop;
    }
}
