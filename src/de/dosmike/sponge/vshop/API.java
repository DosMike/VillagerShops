package de.dosmike.sponge.vshop;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3d;

/** methods return true on success */
public class API {
	
	public static NPCguard create(EntityType type, String variant, Text displayName, Location<World> location, Double rotation) {
		if (VillagerShops.getNPCfromLocation(location).isPresent()) return null;
		NPCguard npc = new NPCguard(UUID.randomUUID());
		InvPrep prep = new InvPrep();
		npc.setNpcType(type);
		npc.setVariant(variant);
		npc.setDisplayName(displayName);
		npc.setPreparator(prep);
		npc.setLoc(location);
		npc.setRot(new Vector3d(0, rotation, 0));
		VillagerShops.addNPCguard(npc);
		return npc;
	}
	
	public static Collection<NPCguard> list() {
		return VillagerShops.getNPCguards();
	}
	
	public static void delete(NPCguard shop) {
		VillagerShops.stopTimers();
		VillagerShops.closeShopInventories(shop.getIdentifier());
		shop.getLe().remove();
		VillagerShops.removeNPCguard(shop);
		VillagerShops.startTimers();
	}
	
	public static boolean playershop(NPCguard shop, UUID user, Location<World> container) {
		if (user != null) {
			if (container != null && container.getBlockType().equals(BlockTypes.CHEST)) {
				shop.playershopholder = user;
				shop.playershopcontainer = container;
			} else return false;
		} else {
			if (container != null) return false;
			shop.playershopholder = null;
			shop.playershopcontainer = null;
		}
		return true;
	}
	
	/** something like prepare a shop to be modified */
	public static void disintegrate(NPCguard shop) {
		Optional<Chunk> c = shop.getLoc().getExtent().getChunkAtBlock(shop.getLoc().getBiomePosition());
		if (!c.isPresent()) 
			throw new RuntimeException("Chunk for shop not available!");
		Chunk chunk = c.get();
		if (!chunk.isLoaded()) {
			if (!chunk.loadChunk(false)) 
				throw new RuntimeException("Unable to load chunk for shop to remove old entity");
		}
		chunk.getEntity(shop.getLe().getUniqueId()).ifPresent(ent->{
			//if (ent instanceof Living) 
				ent.remove();
		});
	}
	
	
}
