package de.dosmike.sponge.vshop;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;

public class EventListeners {

	@Listener
		public void onPlayerInteractEntity(InteractEntityEvent.Primary event) {
			Optional<Player> cause = event.getCause().first(Player.class);
			Entity target = event.getTargetEntity();
			if (cause.isPresent())
				if (InteractionHandler.clickEntity(cause.get(), target, InteractionHandler.Button.left))
	//				event.setCancelled(true);
					;//canceling this event throws some huge errors... so i wont, npcs will respawn
		}

	@Listener
	public void onPlayerInteractEntity(InteractEntityEvent.Secondary event) {
		Optional<Player> cause = event.getCause().first(Player.class);
		Entity target = event.getTargetEntity();
		if (cause.isPresent())
			if (InteractionHandler.clickEntity(cause.get(), target, InteractionHandler.Button.right))
					event.setCancelled(true);
	}

	@Listener
		public void onInventoryClosed(InteractInventoryEvent.Close event) {
			Optional<Player> cause = event.getCause().first(Player.class);
			if (cause.isPresent()) {
				UUID target = cause.get().getUniqueId();
				VillagerShops.openShops.remove(target);
				VillagerShops.actionUnstack.remove(target);
	//			l("%s closed an inventory", cause.get().getName());
			} else {
	//			l("An inventory was closed");
			}
		}

	//TODO + why this is important:
		// in case a admin/plugin opens a inventory over another inventory we will still assume we're in the shop inventory
		@Listener
		public void onInventoryOpened(InteractInventoryEvent.Open event) {
			Optional<Player> cause = event.getCause().first(Player.class);
			if (cause.isPresent()) {
	//			openShops.remove(cause.get().getUniqueId());
//				VillagerShops.l("%s opened and inventory %s", cause.get().getName(), event.getTargetInventory().getName());
			} else {
//				VillagerShops.w("Can't get player opening inventory");
			}
		}

	@Listener
	public void onInventoryClick(ClickInventoryEvent event) {
		Optional<Player> clicker = event.getCause().first(Player.class);
		if (!clicker.isPresent()) return;
		if (!VillagerShops.openShops.containsKey(clicker.get().getUniqueId())) return;
		
		if (VillagerShops.actionUnstack.contains(clicker.get().getUniqueId())) {
			event.getTransactions().forEach(action -> { action.setValid(false); });
			event.getCursorTransaction().setCustom(ItemStackSnapshot.NONE);
			event.getCursorTransaction().setValid(false);
			event.setCancelled(true);
			return;
		}
		
		int slotIndex=-1;
		boolean inTargetInventory=false;
		
		//small algorithm to determ in what inventory the event occurred
		//thanks for this great API so far ;D
		
		NPCguard g = VillagerShops.getNPCfromShopUUID(VillagerShops.openShops.get(clicker.get().getUniqueId())).get();
		
		//compare the cursor held or actioned item with the item it should
		//be at in the InvPrep, if the item matches for the action use that
		ItemStackSnapshot isnap = event.getCursorTransaction().getFinal();
		for (SlotTransaction action : event.getTransactions()) {
			Slot thisSlot = action.getSlot();
			if (isnap.isEmpty()) isnap = action.getOriginal();
			if (isnap.isEmpty() || isnap.getType().equals(ItemTypes.AIR)) continue; //can't buy/sell air mofo 
			for (SlotIndex si : thisSlot.getProperties(SlotIndex.class)) {
				InvPrep p = g.getPreparator();
				slotIndex = si.getValue();
				int i = p.slotToIndex(slotIndex);
				int a = p.isSlotBuySell(slotIndex);
				if (a > 1 || i >= p.items.size()) continue;
				StockItem s = p.getItem(i);
				if ((a==0 && s.getBuyDisplayItem().createSnapshot().equals(isnap)) ||
						s.getSellDisplayItem().createSnapshot().equals(isnap)) inTargetInventory=true;
			}
			if (inTargetInventory) break;
		}
		
		//clear cursor
		event.getCursorTransaction().setValid(false);
		event.getCursorTransaction().setCustom(ItemStackSnapshot.NONE);
		event.getTransactions().forEach(action -> { action.setValid(false); });
		
		if (inTargetInventory) { 
			InteractionHandler.clickInventory(clicker.get(), slotIndex);
			Sponge.getScheduler().createSyncExecutor(VillagerShops.getInstance())
				.schedule(() -> {
					VillagerShops.actionUnstack.remove(clicker.get().getUniqueId());
				}, 50, TimeUnit.MILLISECONDS);
		}
		event.setCancelled(true);
	}

	@Listener
	public void onDropItem(DropItemEvent event) {
		Optional<Player> clicker = event.getCause().first(Player.class);
		if (!clicker.isPresent()) return;
		if (!VillagerShops.openShops.containsKey(clicker.get().getUniqueId())) return;
		event.setCancelled(true);
	}

}
