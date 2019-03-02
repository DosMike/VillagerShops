package de.dosmike.sponge.vshop;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.explosive.Explosive;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.extent.Extent;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EventListeners {

    @Listener
    public void onAttackEntity(DamageEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getTargetEntity() instanceof Living)) return;
        Living target = (Living) event.getTargetEntity();

        if (VillagerShops.isNPCused(target)) event.setCancelled(true);
    }

    @Listener
    public void onPlayerInteractEntity(InteractEntityEvent.Secondary event) {
        Optional<Player> cause = event.getCause().first(Player.class);
        Entity target = event.getTargetEntity();
        if (cause.isPresent())
            if (InteractionHandler.clickEntity(cause.get(), target/*, InteractionHandler.Button.right*/))
                event.setCancelled(true);
    }

    @Listener
    public void onPlayerInteractBlock(InteractBlockEvent.Secondary event) {
        Optional<Player> cause = event.getCause().first(Player.class);
        Optional<Location<World>> location = event.getTargetBlock().getLocation();
        if (cause.isPresent() && location.isPresent()) {
            Optional<TileEntity> entity = location.get().getTileEntity();
            if (entity.isPresent() && entity.get() instanceof TileEntityCarrier) {
                if (ChestLinkManager.linkChest(cause.get(), (TileEntityCarrier) entity.get()))
                    event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onInventoryClosed(InteractInventoryEvent.Close event) {
        Optional<Player> cause = event.getCause().first(Player.class);
        if (cause.isPresent()) {
            UUID target = cause.get().getUniqueId();
            VillagerShops.openShops.remove(target);
            VillagerShops.actionUnstack.remove(target);
        }
    }

    @Listener
    public void onDropItem(DropItemEvent event) {
        Optional<Player> clicker = event.getCause().first(Player.class);
        if (!clicker.isPresent()) return;
        if (!VillagerShops.openShops.containsKey(clicker.get().getUniqueId())) return;
        event.setCancelled(true);
    }

    /**
     * protect playershop crates
     */

    @Listener
    public void onInteract(InteractBlockEvent event) {
        Optional<Player> source = event.getCause().first(Player.class);
        if (!source.isPresent()) return;
        if (!event.getTargetBlock().getLocation().isPresent()) return;
        Extent tex = event.getTargetBlock().getLocation().get().getExtent();
        Vector3i tv3 = event.getTargetBlock().getPosition();
        for (NPCguard g : VillagerShops.getNPCguards())
            if (g.playershopcontainer != null &&
                    g.playershopcontainer.getExtent().equals(tex) &&
                    g.playershopcontainer.getBlockPosition().equals(tv3)) {
                if ((g.playershopholder != null && !g.playershopholder.equals(source.get().getUniqueId())) &&
                        (!source.get().hasPermission("vshop.edit.admin"))) {
                    event.setCancelled(true);
                    return;
                }
            }
    }

    @Listener
    public void onExplosion(ExplosionEvent.Detonate event) {
        Optional<Player> source = event.getCause().first(Player.class);
        if (!source.isPresent() && event.getExplosion().getSourceExplosive().isPresent()) {
            Explosive e = event.getExplosion().getSourceExplosive().get();
            Optional<UUID> creator = e.getCreator();
            if (creator.isPresent()) source = Sponge.getServer().getPlayer(creator.get());
        }

        List<Location<World>> denied = new LinkedList<>();
        for (NPCguard g : VillagerShops.getNPCguards()) {
            if (g.playershopcontainer != null &&
                    event.getAffectedLocations().contains(g.playershopcontainer)) {
                denied.add(g.playershopcontainer);
            }
        }
        event.getAffectedLocations().removeAll(denied);
    }

    @Listener
    public void onBlockBreak(ChangeBlockEvent.Break event) {
        event.getTransactions().forEach(trans -> {
            Optional<Location<World>> w = trans.getOriginal().getLocation();
            if (!w.isPresent()) return;
            Extent tex = w.get().getExtent();
            Vector3i tv3 = w.get().getBlockPosition();
            for (NPCguard g : VillagerShops.getNPCguards()) {
                if (g.playershopcontainer != null &&
                        g.playershopcontainer.getExtent().equals(tex) &&
                        g.playershopcontainer.getBlockPosition().equals(tv3)) {
                    trans.setValid(false);
                }
            }
        });
    }


    @Listener
    public void onPlayerDisconnect(ClientConnectionEvent.Disconnect event) {
        ChestLinkManager.cancel(event.getTargetEntity());
    }
	
	
	/*@Listener //uncomment when ready
	public void onAiTargetEntity(SetAttackTargetEvent event) {
		event.getTarget().ifPresent(target->{
			if (VillagerShops.isNPCused(target)) 
				event.setCancelled(true); //would probably be better to set a different target
		});
	}*/
}
