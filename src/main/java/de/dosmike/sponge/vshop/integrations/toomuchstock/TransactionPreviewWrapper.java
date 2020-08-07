package de.dosmike.sponge.vshop.integrations.toomuchstock;

import com.google.common.collect.ImmutableList;
import de.dosmike.sponge.toomuchstock.service.TransactionPreview;

import java.math.BigDecimal;

public class TransactionPreviewWrapper implements Preview {

    private final TransactionPreview wrapped;
    TransactionPreviewWrapper(TransactionPreview preview) {
        wrapped=preview;
    }

    @Override
    public void update() {
        wrapped.update();
    }

    @Override
    public ImmutableList<BigDecimal> getCumulativeValueForItems() {
        return wrapped.getCumulativeValueForItems();
    }

    @Override
    public BigDecimal getCumulativeValueFor(int nItems) {
        return wrapped.getCumulativeValueFor(nItems);
    }

    @Override
    public int getAffordableAmount() {
        return wrapped.getAffordableAmount();
    }

    @Override
    public void confirm(int amount) {
        wrapped.confirm(amount);
    }

    @Override
    public int getLimitAccount() {
        return wrapped.getLimitAccount();
    }

    @Override
    public int getLimitItemTransactions() {
        return wrapped.getLimitItemTransactions();
    }

    @Override
    public int getLimitCurrencyTransactions() {
        return wrapped.getLimitCurrencyTransactions();
    }
}
