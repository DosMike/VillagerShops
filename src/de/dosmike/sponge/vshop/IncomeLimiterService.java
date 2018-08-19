package de.dosmike.sponge.vshop;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.entity.living.player.Player;

public class IncomeLimiterService {
	long serverDate = System.currentTimeMillis();
	
	Map<UUID, BigDecimal> earnings = new HashMap<>();
	Map<UUID, BigDecimal> spendings = new HashMap<>();

	void loadFromConfig(Map<UUID, BigDecimal> eaningValues, Map<UUID, BigDecimal> spendingValues, long timestamp) {
		earnings = eaningValues;
		spendings = spendingValues;
		serverDate = timestamp;
	}
	Map<UUID, BigDecimal> getEarnings() {
		return earnings;
	}
	Map<UUID, BigDecimal> getSpendings() {
		return spendings;
	}
	long forTime() {
		return serverDate;
	}
	
	public BigDecimal earningFor(Player player) { dayCheck();
		return earnings.containsKey(player.getUniqueId()) ? earnings.get(player.getUniqueId()) : BigDecimal.ZERO;
	}
	
	/** checks wether this player has a limit set, and if the limit actually specifies a limit (negative values mean infinite) */
	public boolean isIncomeLimited(Player player) {
		Optional<String> oil = player.getOption("vshop.option.dailyincome.limit");
		if (!oil.isPresent()) {
			return false;
		}
		BigDecimal limit = null;
		try {
			limit = new BigDecimal(oil.get());
		} catch (Exception e) {
			VillagerShops.w("Illegal value for 'vshop.option.dailyincome.limit' on %s: \"%s\" was not a decimal", player.getName(), oil.get());
			return false;
		}
		return (limit.compareTo(BigDecimal.ZERO)>0);
	}

	/** checks whether this players spendings are limited. kinda like child protection but in crappy */
	public boolean isSpendingsLimited(Player player) {
		Optional<String> oil = player.getOption("vshop.option.dailyspendings.limit");
		if (!oil.isPresent()) {
			return false;
		}
		BigDecimal limit = null;
		try {
			limit = new BigDecimal(oil.get());
		} catch (Exception e) {
			VillagerShops.w("Illegal value for 'vshop.option.dailyspendings.limit' on %s: \"%s\" was not a decimal", player.getName(), oil.get());
			return false;
		}
		return (limit.compareTo(BigDecimal.ZERO)>0);
	}
	
	public Optional<BigDecimal> getRemainingIncome(Player player) { dayCheck();
		Optional<String> oil = player.getOption("vshop.option.dailyincome.limit");
		if (!oil.isPresent()) return Optional.empty();
		BigDecimal limit = null;
		try {
			limit = new BigDecimal(oil.get());
		} catch (Exception e) {
			VillagerShops.w("Illegal value for 'vshop.option.dailyincome.limit' on %s: \"%s\" was not a decimal", player.getName(), oil.get());
			return Optional.empty();
		}
		if (limit.compareTo(BigDecimal.ZERO)>0) { //applicable
			if (!earnings.containsKey(player.getUniqueId()))
				return Optional.of(limit);
			return Optional.of(limit.subtract(earnings.get(player.getUniqueId())));
		} else {
			return Optional.of(limit);
		}
	}

	public Optional<BigDecimal> getRemainingSpendings(Player player) {
		Optional<String> oil = player.getOption("vshop.option.dailyspendings.limit");
		if (!oil.isPresent()) return Optional.empty();
		BigDecimal limit = null;
		try {
			limit = new BigDecimal(oil.get());
		} catch (Exception e) {
			VillagerShops.w("Illegal value for 'vshop.option.dailyspendings.limit' on %s: \"%s\" was not a decimal", player.getName(), oil.get());
			return Optional.empty();
		}
		if (limit.compareTo(BigDecimal.ZERO)>0) { //applicable
			if (!spendings.containsKey(player.getUniqueId()))
				return Optional.of(limit);
			return Optional.of(limit.subtract(spendings.get(player.getUniqueId())));
		} else {
			return Optional.of(limit);
		}
	}
	
	/** increase a users earning value, reducing the remainder for the day.
	 * This method will use getRemainingIncome and return the amount this player can hold from {@param amount} */
	public BigDecimal registerIncome(Player player, BigDecimal amount) {
		Optional<BigDecimal> ore = getRemainingIncome(player);
		if (!ore.isPresent()) return BigDecimal.ZERO;
		BigDecimal space = ore.get().min(amount);
		BigDecimal newbalance = earnings.containsKey(player.getUniqueId()) ? earnings.get(player.getUniqueId()) : BigDecimal.ZERO;
		newbalance = newbalance.add(space);
		earnings.put(player.getUniqueId(), newbalance);
		return space;
	}

	/** increase the spendings counuter and return the remaining money spendable.
	 * Usage: getspendings(), get min between price, make purchase, call this with final price */
	public BigDecimal registerSpending(Player player, BigDecimal amount) {
		Optional<BigDecimal> ore = getRemainingIncome(player);
		if (!ore.isPresent()) return BigDecimal.ZERO;
		BigDecimal space = ore.get().min(amount);
		BigDecimal newbalance = spendings.containsKey(player.getUniqueId()) ? spendings.get(player.getUniqueId()) : BigDecimal.ZERO;
		newbalance = newbalance.add(space);
		spendings.put(player.getUniqueId(), newbalance);
		return space;
	}
	
	private void dayCheck() {
		Calendar today = Calendar.getInstance();
		Calendar sday = Calendar.getInstance();
		sday.setTimeInMillis(serverDate);
		if (sday.get(Calendar.DAY_OF_YEAR) != today.get(Calendar.DAY_OF_YEAR)) {
			earnings.clear();
			serverDate = System.currentTimeMillis();
		}
	}
}
