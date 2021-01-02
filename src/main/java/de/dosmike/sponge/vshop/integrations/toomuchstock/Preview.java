package de.dosmike.sponge.vshop.integrations.toomuchstock;

import com.google.common.collect.ImmutableList;

import java.math.BigDecimal;

public interface Preview {

	/**
	 * Recalculate all values in this TransactionPreview
	 */
	void update();

	/**
	 * Holds the price for N items at List#get(N) for zero items up to List#size() items.<br>
	 * Use #update(), then fetch this list again if you want to get updated prices.
	 *
	 * @return The list, immutable.
	 */
	ImmutableList<BigDecimal> getCumulativeValueForItems();

	/**
	 * Retrieve the price for the specified amount. Shortcut for
	 * #getCumulativeValueForItems().get(nItems).<br>
	 * Use #update(), then fetch this list again if you want to get updated prices.
	 *
	 * @param nItems the amount of items to get a price for.
	 * @return the price for the specified amount of items.
	 */
	BigDecimal getCumulativeValueFor(int nItems);

	/**
	 * The TransactionPreview is built with a requested maximum amount.
	 * Additionally the maximum amount of items affordable by player account balance,
	 * monetary transaction limits and item transaction limits are fetched and condensed
	 * into this affordable amount as minimum over all these amounts.
	 * The result is used as limit for the internal ValueForItems list, defining the
	 * maximum request-able amount through this TransactionPreview
	 *
	 * @return the amount of items that can be traded at max.
	 */
	int getAffordableAmount();

	/**
	 * After you've transferred items and money around you have to
	 * notify the API about the amount of items traded, in order for
	 * prices to be adjusted. That's done through this method.
	 *
	 * @param amount the amount of items your plugin transferred.
	 */
	void confirm(int amount);

	/**
	 * This method is intended to help inform the player why the transaction
	 * might only be possible in part.
	 *
	 * @return The amount of items the player can afford by account balance,
	 * before running dry of money or reaching balance limit.
	 * This is additionally limited by requested max amount.
	 * @apiNote The EconomyService does not implement a method to receive
	 * upper balance limits, so this might not be taken into consideration!
	 */
	int getLimitAccount();

	/**
	 * This method is intended to help inform the player why the transaction
	 * might only be possible in part.
	 *
	 * @return The amount of items still trade-able, limited by the item
	 * count transaction limit. You won't be able to tell what tracker
	 * (global, shop, player) enforces this limit.
	 * This is additionally limited by requested max amount.
	 */
	int getLimitItemTransactions();

	/**
	 * This method is intended to help inform the player why the transaction
	 * might only be possible in part.
	 *
	 * @return The amount of items still trade-able, limited by the economic
	 * transaction volume limit for the specified currency.
	 * You won't be able to tell what tracker (global, shop, player)
	 * enforces this limit.
	 * This is additionally limited by requested max amount.
	 */
	int getLimitCurrencyTransactions();

}
