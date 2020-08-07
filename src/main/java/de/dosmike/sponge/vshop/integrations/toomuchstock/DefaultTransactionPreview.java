package de.dosmike.sponge.vshop.integrations.toomuchstock;

import com.google.common.collect.ImmutableList;
import de.dosmike.sponge.vshop.VillagerShops;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.economy.Currency;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.List;

public class DefaultTransactionPreview implements Preview {

    private final BigDecimal unitPrice;
    private final Currency currency;
    private final int requestedAmount;
    private final List<BigDecimal> list = new LinkedList<>();

    private final Player player; //carefull!
    private final boolean buying;
    private final boolean transactionLimited;

    DefaultTransactionPreview(BigDecimal staticPrice, int amount, Currency currency, @Nullable Player player, boolean purchase) {
        assert amount >= 0 : "Amount must not be negative";
        assert staticPrice.compareTo(BigDecimal.ZERO)>0 : "Price must be greater zero";

        this.unitPrice = staticPrice;
        this.requestedAmount = amount;
        this.currency = currency;

        this.player = player;
        this.buying = purchase;

        this.transactionLimited = player!=null && (purchase
                ? VillagerShops.getIncomeLimiter().isSpendingsLimited(player)
                : VillagerShops.getIncomeLimiter().isIncomeLimited(player));

        //prepare list
        BigDecimal nextValue = unitPrice;
        list.add(BigDecimal.ZERO);
        for (int i=0; i<amount; i++, nextValue=nextValue.add(unitPrice)) {
            list.add(nextValue);
        }
    }

    @Override
    public void update() {
        //we don't need to do anything here, because the default implementation uses static pricing
    }

    @Override
    public ImmutableList<BigDecimal> getCumulativeValueForItems() {
        return ImmutableList.copyOf(list);
    }

    @Override
    public BigDecimal getCumulativeValueFor(int nItems) {
        return list.get(nItems);
    }

    @Override
    public int getAffordableAmount() {
        //Since getLimitItemTransactions always returns the max of requestedAmount
        // it's enough to check getLimitAccount and getLimitCurrencyTransaction
        //If you're implementing this in your plugin getLimitCurrencyTransactions
        // should always return requestedAmount as well, so just
        //return getLimitAccount();

        return Math.min(getLimitAccount(), getLimitCurrencyTransactions());
    }

    @Override
    public void confirm(int amount) {
        // If you implement this for your plugin you don't need to do anything here,
        // because trading items does not change the price.

        if (transactionLimited) {
            if (buying)
                VillagerShops.getIncomeLimiter().registerSpending(player, list.get(amount));
            else
                VillagerShops.getIncomeLimiter().registerIncome(player, list.get(amount));
        }
    }

    @Override
    public int getLimitAccount() {
        if (buying) {
            if (player == null) //probably displaying the price for a shop front
                return requestedAmount; //shop can display whatever quantity it wants
            return Math.min(VillagerShops.getEconomy().getOrCreateAccount(player.getUniqueId())
                    .map(acc->acc.getBalance(currency))
                    .map(balance->balance.divide(unitPrice, RoundingMode.FLOOR).intValue())
                    .orElse(0), requestedAmount);
        } else {
            // While account CAN have an upper bound, there's no way to check it in the API
            // So just assume we can hold all the money to sell the requested amount
            return requestedAmount;
        }
    }

    @Override
    public int getLimitItemTransactions() {
        // This has nothing to do with inventory space
        return requestedAmount;
    }

    @Override
    public int getLimitCurrencyTransactions() {
        if (player == null) //probably displaying the price for a shop front
            return requestedAmount; //shop can display whatever quantity it wants

        // If you're implementing this in you plugin just
        //return requestedAmount;

        return (buying
                ? VillagerShops.getIncomeLimiter().getRemainingSpendings(player)
                : VillagerShops.getIncomeLimiter().getRemainingIncome(player))
                .map(spend->spend.divide(unitPrice, RoundingMode.FLOOR).intValue())
                .orElse(requestedAmount); //no value present means, the player is not income limited
    }
}
