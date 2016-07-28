package com.github.dosmike.sponge.vshop;

import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class InteractionHandler {
	public static enum Button { left, right }
	
	/** return true to cancel the event in the parent */
	public static boolean clickEntity(Player source, Entity target, Button side) {
		//try to get shop:
		Location<World> tl = target.getLocation();
		int index = VillagerShops.getInstance().getNPCfromLocation(tl);
		
		if (index >= 0 && index < VillagerShops.getInstance().npcs.size()) {
			VillagerShops.l("Index: %d", index);
			if (side == Button.right) {
				NPCguard npc = VillagerShops.getInstance().npcs.get(index);
				if (!npc.getPreparator().items.isEmpty()) {
					source.openInventory(npc.getPreparator().getInventory(), Cause.builder().named("PLUGIN (Shop Opened)", VillagerShops.getInstance()).build());
					VillagerShops.getInstance().openShops.put(source.getUniqueId(), npc.getIdentifier());
				}
			}
			
			return true;
		}
		return false;
	}
	
	/** return true to cancel the event in the parent */
	public static boolean clickInventory(Player source, int slot) {
		return false;
	}
}
