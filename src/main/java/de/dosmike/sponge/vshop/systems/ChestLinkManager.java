package de.dosmike.sponge.vshop.systems;

import de.dosmike.sponge.languageservice.API.PluginTranslation;
import de.dosmike.sponge.vshop.PermissionRegistra;
import de.dosmike.sponge.vshop.Utilities;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.commands.Command;
import de.dosmike.sponge.vshop.shops.ShopEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ChestLinkManager {
	private static final Map<UUID, UUID> activeLinker = new HashMap<>();

	public static void toggleLinker(Player player, @Nullable ShopEntity shop) {
		PluginTranslation lang = VillagerShops.getTranslator();

		if (activeLinker.containsKey(player.getUniqueId())) {
			activeLinker.remove(player.getUniqueId());
			player.sendMessage(Text.of(TextColors.GREEN, "[vShop] ",
					lang.local("cmd.link.cancelled").resolve(player).orElse("[linking cancelled]")));
			VillagerShops.audit("%s cancelled chest-linking", Utilities.toString(player));
		} else if (shop == null) {
			player.sendMessage(Text.of(TextColors.RED, "[vShop] ",
					lang.local("cmd.common.notarget").resolve(player).orElse("[no target]")));
		} else if (!shop.getShopOwner().isPresent()) {
			player.sendMessage(Text.of(TextColors.RED, "[vShop] ",
					lang.local("cmd.link.adminshop").resolve(player).orElse("[admin shop]")));
		} else if (!shop.isShopOwner(player.getUniqueId()) && !PermissionRegistra.ADMIN.hasPermission(player)) {
			player.sendMessage(Text.of(TextColors.RED, "[vShop] ",
					lang.local("cmd.link.notyourshop").resolve(player).orElse("[not your shop]")));
		} else {
			activeLinker.put(player.getUniqueId(), shop.getIdentifier());
			player.sendMessage(Text.of(TextColors.YELLOW, "[vShop] ",
					lang.local("cmd.link.hitachest").resolve(player).orElse("[hit a chest]")));
			VillagerShops.audit("%s selected shop %s for chest-linking",
					Utilities.toString(player), shop.toString());
		}
	}

	/**
	 * @return true if the player was marked as active linker and
	 * the interaction event with a potential container should be cancelled
	 */
	public static boolean linkChest(Player player, TileEntityCarrier carrier) {
		PluginTranslation lang = VillagerShops.getTranslator();

		if (!activeLinker.containsKey(player.getUniqueId())) {
			return false;
		} else if (carrier.getInventory().capacity() < 27) {
			player.sendMessage(Text.of(TextColors.RED, "[vShop] ",
					lang.local("cmd.link.nochest").resolve(player).orElse("[not a chest]")));
		} else {
			Optional<ShopEntity> npc = VillagerShops.getShopFromShopId(activeLinker.get(player.getUniqueId()));
			if (!npc.isPresent()) {
				player.sendMessage(Text.of(TextColors.RED, "[vShop] ",
						lang.local("cmd.link.missingshop").resolve(player).orElse("[where's the shop?]")));
			} else {
				if (npc.get().getShopOwner().isPresent() && //player shop
						!VillagerShops.getProtection().hasAccess(player, carrier.getLocation())) { //not checking for owner access allows admins to link shops for you
					player.sendMessage(Text.of(TextColors.RED,
							lang.local("permission.missing").orLiteral(player)));
					return true;
				}
				Optional<Integer> distance = Optional.empty();
				if (npc.get().getShopOwner().isPresent()) try {
					distance = Command.getMaximumStockDistance(player);
				} catch (NumberFormatException nfe) {
					player.sendMessage(Text.of(TextColors.RED, lang.local("option.invalidvalue")
							.replace("%option%", "vshop.option.chestlink.distance")
							.replace("%player%", player.getName())
							.resolve(player)
							.orElse("[option value invalid]"))
					);
					return true;
				}
				if (distance.isPresent() && (
						!carrier.getLocation().getExtent().equals(npc.get().getLocation().getExtent()) ||
								carrier.getLocation().getPosition().distance(npc.get().getLocation().getPosition()) > distance.get())) {
					player.sendMessage(Text.of(TextColors.RED, lang.local("cmd.link.distance")
							.replace("%distance%", distance.get())
							.resolve(player)
							.orElse("[too far away]")));
					return true;
				}

				npc.get().setStockContainerRaw(carrier.getLocation());
				player.sendMessage(Text.of(TextColors.GREEN, "[vShop] ",
						lang.local("cmd.link.success").resolve(player).orElse("[chest linked!]")));
				VillagerShops.audit("%s relinked shop %s to container at %s",
						Utilities.toString(player), npc.get().toString(), Utilities.toString(carrier.getLocation()));
			}
			activeLinker.remove(player.getUniqueId());
		}
		return true;
	}

	public static void cancel(Player player) {
		activeLinker.remove(player.getUniqueId());
	}
}
