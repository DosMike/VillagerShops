package de.dosmike.sponge.vshop.integrations.crateplugins;

import com.codehusky.huskycrates.HuskyCrates;
import com.codehusky.huskycrates.crate.virtual.Key;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.systems.pluginfilter.PluginItemFilter;
import de.dosmike.sponge.vshop.systems.ShopType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;

import java.util.Optional;
import java.util.function.Function;

public class HuskyCrateKeyFilter implements PluginItemFilter {

	private static long lastUpdate = 0L;
	private static void updateKeys() {
		long b = System.currentTimeMillis();
		if (b-lastUpdate<1000) return;
		lastUpdate = b;
		VillagerShops.updateCratePlugins();
	}

	private final String huskeyCrateKeyID;

	protected HuskyCrateKeyFilter(String keyID) {
		huskeyCrateKeyID = keyID;
	}

	private Optional<Key> physicalKey() {
		Key key = HuskyCrates.registry.getKey(huskeyCrateKeyID);
		if (key == null || key.isVirtual()) return Optional.empty();
		return Optional.of(key);
	}

	private <X, Y> Optional<Y> mapNullable(X input, Function<X, Y> mapper) {
		if (input == null) return Optional.empty();
		return Optional.ofNullable(mapper.apply(input));
	}

	private Optional<Key> getKey(ItemStack item) {
		return mapNullable(Key.extractKeyId(item), id -> HuskyCrates.registry.getKey(id));
	}

	@Override
	public boolean isItem(ItemStack item) {
		return getKey(item).map(key ->
				//quasi Key.testKey(item)
				!key.isVirtual() && huskeyCrateKeyID.equals(key.getId()) && item.getType().equals(key.getItem().getItemType())
		).orElse(false);
	}

	@Override
	public boolean enforce() {
		return true;
	}

	@Override
	public boolean supportShopType(ShopType shopType) {
		return true;
	}

	@Override
	public ItemStack supply(int amount, ShopType shopType) {
		return physicalKey().map(key -> {
			VillagerShops.l("HuskyCrates: Created %s new key(s) with id %s", amount, key.getId());
			return key.getKeyItemStack(amount);
		}).orElse(ItemStack.empty());
	}

	@Override
	public int getMaxStackSize() {
		return physicalKey().map(key -> key.getItem().getItemType().getMaxStackQuantity()).orElse(0);
	}

	@Override
	public void consume(ItemStack item, ShopType shopType) {
		physicalKey().ifPresent(key -> {
			VillagerShops.l("HuskyCrates: Destroyed %d key(s) with id %s", item.getQuantity(), key.getId());
			HuskyCrates.registry.consumeSecureKey(item, item.getQuantity());
		});
	}

	@Override
	public Optional<ItemStackSnapshot> getDisplayItem(ShopType shopType) {
		return physicalKey()
				.map(key -> key.getItem().toItemStack().createSnapshot())
				.filter(item -> !item.isEmpty() && !item.getType().equals(ItemTypes.AIR));
	}
}
