package de.dosmike.sponge.vshop.integrations.toomuchstock;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.GameState;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.service.economy.Currency;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * This interface mirrors the interface PriceCalculationService from TooMuchStock.
 * The purpose of this interface is to provide a default implementation that does not
 * know anything about TooMuchStock (no imports from that plugin) in order for Java
 * to be able to load one version of the interface without that plugin being present.
 * <p>
 * JavaDocs for methods are copied from TooMuchStock's PriceCalculationService
 * <p>
 * In order for TransactionPreviews to work, they need a similar abstraction.
 */
public interface PriceCalculator {

	/**
	 * Core to super optional dependency:<br>
	 * Tries to get the PriceCalculator from the classloader.
	 * On failure it creates a new Default provider.
	 * <br><br>
	 * Doing it this way makes this class not quite know about the dependency.
	 * And the ClassLoader will not attempt to look into the dependency unless the wrapper
	 * is constructed for the first time.
	 * <br><br>
	 * This method should only be called once in {@link GameInitializationEvent}
	 */
	static PriceCalculator get() {
		assert Sponge.getGame().getState().ordinal() >= GameState.INITIALIZATION.ordinal()
				: "The Service for TooMuchStock is not yet registered!";
		try {
			Class.forName("de.dosmike.sponge.toomuchstock.service.PriceCalculationService");
			return new PriceCalculationWrapper();
		} catch (Throwable dependencyError) {
			return new PriceCalculator() {
			};
		}
	}

	/**
	 * Get pricing information for players that seek to <b>purchase</b> items from this shop.<br>
	 * Since prices are no longer linear with amount, this function returns the price for each amount from 1 up to {@code amount}.
	 * The price is based on the current price tracking for global, optionally shop and optionally player.
	 * Please use {@link Preview#confirm(int)} for actual transactions. Otherwise the prices wont change/update!
	 * For price displays {@link #getCurrentPurchasePrice} might be more performant.
	 *
	 * @param item        the item to use the tracking for
	 * @param amount      the max amount of items to calculate prices for
	 * @param staticPrice the static base-price this item shall use
	 * @param currency    the currency currently trading for income/spending limits
	 * @param shopID      the UUID of the shop, if this item is listed within a shop
	 * @param playerID    the UUID of the player that's seeking transaction (if applicable)
	 * @return TransactionPreview with price listings and amount information
	 */
	default Preview getPurchaseInformation(ItemStack item, int amount, BigDecimal staticPrice, Currency currency, @Nullable UUID shopID, @Nullable UUID playerID) {
		//Default implementation's prices do not depend on the item, but only on the staticPrice

		Player player = null;
		if (playerID != null) player = Sponge.getServer().getPlayer(playerID).orElse(null);
		return new DefaultTransactionPreview(staticPrice, amount, currency, player, true);
	}

	/**
	 * Get pricing information for players that seek to <b>purchase</b> items from this shop.<br>
	 * Since prices are no longer linear with amount, this function returns the price for each amount from 1 up to {@code amount}.
	 * The price is based on the current price tracking for global, optionally shop and optionally player.
	 * Please use {@link Preview#confirm(int)} for actual transactions. Otherwise the prices wont change/update!
	 * For price displays {@link #getCurrentPurchasePrice} might be more performant.
	 *
	 * @param item        the item to use the tracking for
	 * @param amount      the max amount of items to calculate prices for
	 * @param staticPrice the static base-price this item shall use
	 * @param currency    the currency currently trading for income/spending limits
	 * @param shopID      the UUID of the shop, if this item is listed within a shop
	 * @param playerID    the UUID of the player that's seeking transaction (if applicable)
	 * @return TransactionPreview with price listings and amount information
	 */
	default Preview getPurchaseInformation(ItemStackSnapshot item, int amount, BigDecimal staticPrice, Currency currency, @Nullable UUID shopID, @Nullable UUID playerID) {
		//Default implementation's prices do not depend on the item, but only on the staticPrice

		Player player = null;
		if (playerID != null) player = Sponge.getServer().getPlayer(playerID).orElse(null);
		return new DefaultTransactionPreview(staticPrice, amount, currency, player, true);
	}

	/**
	 * Get pricing information for players that seek to <b>sell</b> items from this shop.<br>
	 * Since prices are no longer linear with amount, this function returns the price for each amount from 1 up to {@code amount}.
	 * The price is based on the current price tracking for global, optionally shop and optionally player.
	 * Please use {@link Preview#confirm(int)} for actual transactions. Otherwise the prices wont change/update!
	 * For price displays {@link #getCurrentSellingPrice} might be more performant.
	 *
	 * @param item        the item to use the tracking for
	 * @param amount      the max amount of items to calculate prices for
	 * @param staticPrice the static base-price this item shall use
	 * @param currency    the currency currently trading for income/spending limits
	 * @param shopID      the UUID of the shop, if this item is listed within a shop
	 * @param playerID    the UUID of the player that's seeking transaction (if applicable)
	 * @return TransactionPreview with price listings and amount information
	 */
	default Preview getSellingInformation(ItemStack item, int amount, BigDecimal staticPrice, Currency currency, @Nullable UUID shopID, @Nullable UUID playerID) {
		//Default implementation's prices do not depend on the item, but only on the staticPrice

		Player player = null;
		if (playerID != null) player = Sponge.getServer().getPlayer(playerID).orElse(null);
		return new DefaultTransactionPreview(staticPrice, amount, currency, player, false);
	}

	/**
	 * Get pricing information for players that seek to <b>sell</b> items from this shop.<br>
	 * Since prices are no longer linear with amount, this function returns the price for each amount from 1 up to {@code amount}.
	 * The price is based on the current price tracking for global, optionally shop and optionally player.
	 * Please use {@link Preview#confirm(int)} for actual transactions. Otherwise the prices wont change/update!
	 * For price displays {@link #getCurrentSellingPrice} might be more performant.
	 *
	 * @param item        the item to use the tracking for
	 * @param amount      the max amount of items to calculate prices for
	 * @param staticPrice the static base-price this item shall use
	 * @param currency    the currency currently trading for income/spending limits
	 * @param shopID      the UUID of the shop, if this item is listed within a shop
	 * @param playerID    the UUID of the player that's seeking transaction (if applicable)
	 * @return TransactionPreview with price listings and amount information
	 */
	default Preview getSellingInformation(ItemStackSnapshot item, int amount, BigDecimal staticPrice, Currency currency, @Nullable UUID shopID, @Nullable UUID playerID) {
		//Default implementation's prices do not depend on the item, but only on the staticPrice

		Player player = null;
		if (playerID != null) player = Sponge.getServer().getPlayer(playerID).orElse(null);
		return new DefaultTransactionPreview(staticPrice, amount, currency, player, false);
	}

	/**
	 * Get pricing information for players that seek to <b>purchase</b> items from this shop.<br>
	 * In contrast to {@link #getPurchaseInformation} this only provides the price for the full
	 * {@code amount} of items. <br>
	 * <i>This is supposed to make displaying prices more performant, it's not meant for fetching final prices!</i><br>
	 * If you're looking to make a transaction, please use {@link #getPurchaseInformation}
	 *
	 * @param item        the item to use the tracking for
	 * @param amount      the max amount of items to calculate prices for
	 * @param staticPrice the static base-price this item shall use
	 * @param shopID      the UUID of the shop, if this item is listed within a shop
	 * @param playerID    the UUID of the player that's seeking transaction (if applicable)
	 * @return The price for the specified amount of items, assuming the player could afford it.
	 */
	default BigDecimal getCurrentPurchasePrice(ItemStack item, int amount, BigDecimal staticPrice, @Nullable UUID shopID, @Nullable UUID playerID) {
		// Standard shop implementations don't require more information for basic price display

		return staticPrice.multiply(BigDecimal.valueOf(amount));
	}

	/**
	 * Get pricing information for players that seek to <b>purchase</b> items from this shop.<br>
	 * In contrast to {@link #getPurchaseInformation} this only provides the price for the full
	 * {@code amount} of items. <br>
	 * <i>This is supposed to make displaying prices more performant, it's not meant for fetching final prices!</i><br>
	 * If you're looking to make a transaction, please use {@link #getPurchaseInformation}
	 *
	 * @param item        the item to use the tracking for
	 * @param amount      the max amount of items to calculate prices for
	 * @param staticPrice the static base-price this item shall use
	 * @param shopID      the UUID of the shop, if this item is listed within a shop
	 * @param playerID    the UUID of the player that's seeking transaction (if applicable)
	 * @return The price for the specified amount of items, assuming the player could afford it.
	 */
	default BigDecimal getCurrentPurchasePrice(ItemStackSnapshot item, int amount, BigDecimal staticPrice, @Nullable UUID shopID, @Nullable UUID playerID) {
		// Standard shop implementations don't require more information for basic price display

		return staticPrice.multiply(BigDecimal.valueOf(amount));
	}

	/**
	 * Get pricing information for players that seek to <b>sell</b> items from this shop.<br>
	 * In contrast to {@link #getSellingInformation} this only provides the price for the full
	 * {@code amount} of items. <br>
	 * <i>This is supposed to make displaying prices more performant, it's not meant for fetching final prices!</i><br>
	 * If you're looking to make a transaction, please use {@link #getSellingInformation}
	 *
	 * @param item        the item to use the tracking for
	 * @param amount      the max amount of items to calculate prices for
	 * @param staticPrice the static base-price this item shall use
	 * @param shopID      the UUID of the shop, if this item is listed within a shop
	 * @param playerID    the UUID of the player that's seeking transaction (if applicable)
	 * @return The price for the specified amount of items, assuming the player could afford it.
	 */
	default BigDecimal getCurrentSellingPrice(ItemStack item, int amount, BigDecimal staticPrice, @Nullable UUID shopID, @Nullable UUID playerID) {
		// Standard shop implementations don't require more information for basic price display

		return staticPrice.multiply(BigDecimal.valueOf(amount));
	}

	/**
	 * Get pricing information for players that seek to <b>sell</b> items from this shop.<br>
	 * In contrast to {@link #getSellingInformation} this only provides the price for the full
	 * {@code amount} of items. <br>
	 * <i>This is supposed to make displaying prices more performant, it's not meant for fetching final prices!</i><br>
	 * If you're looking to make a transaction, please use {@link #getSellingInformation}
	 *
	 * @param item        the item to use the tracking for
	 * @param amount      the max amount of items to calculate prices for
	 * @param staticPrice the static base-price this item shall use
	 * @param shopID      the UUID of the shop, if this item is listed within a shop
	 * @param playerID    the UUID of the player that's seeking transaction (if applicable)
	 * @return The price for the specified amount of items, assuming the player could afford it.
	 */
	default BigDecimal getCurrentSellingPrice(ItemStackSnapshot item, int amount, BigDecimal staticPrice, @Nullable UUID shopID, @Nullable UUID playerID) {
		// Standard shop implementations don't require more information for basic price display

		return staticPrice.multiply(BigDecimal.valueOf(amount));
	}

}
