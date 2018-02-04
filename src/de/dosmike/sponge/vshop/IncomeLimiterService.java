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
	
	void loadFromConfig(Map<UUID, BigDecimal> values, long timestamp) {
		earnings = values;
		serverDate = timestamp;
	}
	Map<UUID, BigDecimal> getEarnings() {
		return earnings;
	}
	long forTime() {
		return serverDate;
	}
	
	public BigDecimal earningFor(Player player) { dayCheck();
		return earnings.containsKey(player.getUniqueId()) ? earnings.get(player.getUniqueId()) : BigDecimal.ZERO;
	}
	
	/** checks wether this player has a limit set, and if the limit actually specifies a limit (negative values mean infinite) */
	public boolean applicableFor(Player player) {
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
	
	public Optional<BigDecimal> remainderFor(Player player) { dayCheck();
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
	
	/** increase a users earning value, reducing the remainder for the day.
	 * This method will use remainderFor and return the amount this player can hold from {@link amount} */
	public BigDecimal payout(Player player, BigDecimal amount) {
		Optional<BigDecimal> ore = remainderFor(player);
		if (!ore.isPresent()) return BigDecimal.ZERO;
		BigDecimal space = ore.get().min(amount);
		BigDecimal newbalance = earnings.containsKey(player.getUniqueId()) ? earnings.get(player.getUniqueId()) : BigDecimal.ZERO;
		newbalance = newbalance.add(space);
		earnings.put(player.getUniqueId(), newbalance);
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
