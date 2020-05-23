package de.dosmike.sponge.vshop;

import com.flowpowered.math.vector.Vector3i;
import de.dosmike.sponge.vshop.menus.InvPrep;
import de.dosmike.sponge.vshop.shops.InteractionHandler;
import de.dosmike.sponge.vshop.shops.NPCguard;
import de.dosmike.sponge.vshop.systems.ChestLinkManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.explosive.Explosive;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.event.world.SaveWorldEvent;
import org.spongepowered.api.event.world.UnloadWorldEvent;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.extent.Extent;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EventListeners {

    @Listener(order= Order.EARLY) //as kind-of protection, run early
    public void onAttackEntity(DamageEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getTargetEntity() instanceof Living)) return;
        Living target = (Living) event.getTargetEntity();

        if (VillagerShops.isNPCused(target))
            event.setCancelled(true);
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
            if (g.getStockContainer().isPresent() &&
                    g.getStockContainer().get().getExtent().equals(tex) &&
                    g.getStockContainer().get().getBlockPosition().equals(tv3)) {
                if ((!source.get().getUniqueId().equals(g.getShopOwner().orElse(null))) &&
                    (!PermissionRegistra.ADMIN.hasPermission(source.get()))) {
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
            if (g.getStockContainer().isPresent() &&
                    event.getAffectedLocations().contains(g.getStockContainer().get())) {
                denied.add(g.getStockContainer().get());
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
                if (g.getStockContainer().isPresent() &&
                        g.getStockContainer().get().getExtent().equals(tex) &&
                        g.getStockContainer().get().getBlockPosition().equals(tv3)) {
                    trans.setValid(false);
                }
            }
        });
    }

    @Listener
    public void onPlayerDisconnect(ClientConnectionEvent.Disconnect event) {
        ChestLinkManager.cancel(event.getTargetEntity());

        /* remove the playerstates to prevent memory bloat */
        VillagerShops.getNPCguards().stream()
                .map(NPCguard::getPreparator)
                .map(InvPrep::getMenu)
                .forEach(m->m.clearPlayerState(event.getTargetEntity().getUniqueId()));

        Utilities.openShops.remove(event.getTargetEntity().getUniqueId());
    }

    @Listener
    public void onWorldSave(SaveWorldEvent event) {
        VillagerShops.instance.saveShops();
    }
    @Listener
    public void onWorldUnload(UnloadWorldEvent event) {
        if (!event.isCancelled()) {
            VillagerShops.instance.saveShops();
            VillagerShops.instance.unloadShops(event.getTargetWorld().getUniqueId());
        }
    }
    @Listener
    public void onWorldLoad(LoadWorldEvent event) {
        VillagerShops.instance.loadShops(event.getTargetWorld().getUniqueId());
    }


//    @Listener //someone that knows how to do that please :D
//    public void onAiTargetEntity(AITaskEvent event) {
//        if (event.getGoal().getType().equals(GoalTypes.TARGET)) {
//            Agent agent = event.getGoal().getOwner();
//            if (VillagerShops.isNPCused(agent)) {
//                event.getGoal().clear();
//            }
//        }
//    }
}

