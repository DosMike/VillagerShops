package de.dosmike.sponge.vshop.integrations.toomuchstock;

import de.dosmike.sponge.toomuchstock.service.PriceCalculationService;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.service.economy.Currency;

import java.math.BigDecimal;
import java.util.UUID;

public class PriceCalculationWrapper implements PriceCalculator {

    private final PriceCalculationService wrapped;
    PriceCalculationWrapper() {
        wrapped = Sponge.getServiceManager().provideUnchecked(PriceCalculationService.class);
    }

    @Override
    public Preview getPurchaseInformation(ItemStack item, int amount, BigDecimal staticPrice, Currency currency, @Nullable UUID shopID, @Nullable UUID playerID) {
        return new TransactionPreviewWrapper(wrapped.getPurchaseInformation(item, amount, staticPrice, currency, shopID, playerID));
    }

    @Override
    public Preview getPurchaseInformation(ItemStackSnapshot item, int amount, BigDecimal staticPrice, Currency currency, @Nullable UUID shopID, @Nullable UUID playerID) {
        return new TransactionPreviewWrapper(wrapped.getPurchaseInformation(item, amount, staticPrice, currency, shopID, playerID));
    }

    @Override
    public Preview getSellingInformation(ItemStack item, int amount, BigDecimal staticPrice, Currency currency, @Nullable UUID shopID, @Nullable UUID playerID) {
        return new TransactionPreviewWrapper(wrapped.getSellingInformation(item, amount, staticPrice, currency, shopID, playerID));
    }

    @Override
    public Preview getSellingInformation(ItemStackSnapshot item, int amount, BigDecimal staticPrice, Currency currency, @Nullable UUID shopID, @Nullable UUID playerID) {
        return new TransactionPreviewWrapper(wrapped.getSellingInformation(item, amount, staticPrice, currency, shopID, playerID));
    }

    @Override
    public BigDecimal getCurrentPurchasePrice(ItemStack item, int amount, BigDecimal staticPrice, @Nullable UUID shopID, @Nullable UUID playerID) {
        return wrapped.getCurrentPurchasePrice(item, amount, staticPrice, shopID, playerID);
    }

    @Override
    public BigDecimal getCurrentPurchasePrice(ItemStackSnapshot item, int amount, BigDecimal staticPrice, @Nullable UUID shopID, @Nullable UUID playerID) {
        return wrapped.getCurrentPurchasePrice(item, amount, staticPrice, shopID, playerID);
    }

    @Override
    public BigDecimal getCurrentSellingPrice(ItemStack item, int amount, BigDecimal staticPrice, @Nullable UUID shopID, @Nullable UUID playerID) {
        return wrapped.getCurrentSellingPrice(item, amount, staticPrice, shopID, playerID);
    }

    @Override
    public BigDecimal getCurrentSellingPrice(ItemStackSnapshot item, int amount, BigDecimal staticPrice, @Nullable UUID shopID, @Nullable UUID playerID) {
        return wrapped.getCurrentSellingPrice(item, amount, staticPrice, shopID, playerID);
    }
}
