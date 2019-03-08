package de.dosmike.sponge.vshop;

import de.dosmike.sponge.languageservice.API.PluginTranslation;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ChestLinkManager {
    private static Map<UUID, UUID> activeLinker = new HashMap<>();

    public static void toggleLinker(Player player, Optional<NPCguard> shop) {
        PluginTranslation l = VillagerShops.getTranslator();

        if (activeLinker.containsKey(player.getUniqueId())) {
            activeLinker.remove(player.getUniqueId());
            player.sendMessage(Text.of(TextColors.GREEN, "[vShop] ",
                    l.local("cmd.link.cancelled").resolve(player).orElse("[linking cancelled]")));
        } else if (!shop.isPresent()) {
            player.sendMessage(Text.of(TextColors.RED, "[vShop] ",
                    l.local("cmd.common.notarget").resolve(player).orElse("[no target]")));
        } else if (!shop.get().getShopOwner().isPresent()) {
            player.sendMessage(Text.of(TextColors.RED, "[vShop] ",
                    l.local("cmd.link.adminshop").resolve(player).orElse("[admin shop]")));
        } else if (!shop.get().isShopOwner(player.getUniqueId()) && !player.hasPermission("vshop.edit.admin")) {
            player.sendMessage(Text.of(TextColors.RED, "[vShop] ",
                    l.local("cmd.link.notyourshop").resolve(player).orElse("[not your shop]")));
        } else {
            activeLinker.put(player.getUniqueId(), shop.get().getIdentifier());
            player.sendMessage(Text.of(TextColors.YELLOW, "[vShop] ",
                    l.local("cmd.link.hitachest").resolve(player).orElse("[hit a chest]")));
        }
    }

    /**
     * @return true if the player was marked as active linker and
     * the interaction event with a potential container should be cancelled
     */
    public static boolean linkChest(Player player, TileEntityCarrier carrier) {
        PluginTranslation l = VillagerShops.getTranslator();

        if (!activeLinker.containsKey(player.getUniqueId())) {
            return false;
        } else if (carrier.getInventory().capacity() < 27) {
            player.sendMessage(Text.of(TextColors.RED, "[vShop] ",
                    l.local("cmd.link.nochest").resolve(player).orElse("[not a chest]")));
        } else {
            Optional<NPCguard> npc = VillagerShops.getNPCfromShopUUID(activeLinker.get(player.getUniqueId()));
            if (!npc.isPresent()) {
                player.sendMessage(Text.of(TextColors.RED, "[vShop] ",
                        l.local("cmd.link.missingshop").resolve(player).orElse("[where's the shop?]")));
            } else {

                Optional<Integer> distance = Optional.empty();
                if (npc.get().getShopOwner().isPresent()) try {
                    distance = CommandRegistra.getMaximumStockDistance(player);
                } catch (NumberFormatException nfe) {
                    player.sendMessage(Text.of(TextColors.RED, l.local("option.invalidvalue")
                            .replace("%option%", "vshop.option.chestlink.distance")
                            .replace("%player%", player.getName())
                            .resolve(player)
                            .orElse("[option value invalid]"))
                    );
                    return true;
                }
                if (distance.isPresent() && (
                        !carrier.getLocation().getExtent().equals(npc.get().getLoc().getExtent()) ||
                                carrier.getLocation().getPosition().distance(npc.get().getLoc().getPosition()) > distance.get())) {
                    player.sendMessage(Text.of(TextColors.RED, l.local("cmd.link.distance")
                            .replace("%distance%", distance.get())
                            .resolve(player)
                            .orElse("[too far away]")));
                    return true;
                }

                npc.get().playershopcontainer = carrier.getLocation();
                player.sendMessage(Text.of(TextColors.GREEN, "[vShop] ",
                        l.local("cmd.link.success").resolve(player).orElse("[chest linked!]")));
            }
            activeLinker.remove(player.getUniqueId());
        }
        return true;
    }

    public static void cancel(Player player) {
        activeLinker.remove(player.getUniqueId());
    }
}
