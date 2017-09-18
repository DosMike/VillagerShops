package de.dosmike.sponge.vshop;

import java.util.Collection;
import java.util.UUID;

import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3d;

/** methods return true on success */
public class API {
	
	public static boolean create(EntityType type, String variant, Text displayName, Location<World> location, Double rotation) {
		if (VillagerShops.getNPCfromLocation(location).isPresent()) return false;
		NPCguard npc = new NPCguard(UUID.randomUUID());
		InvPrep prep = new InvPrep();
		npc.setNpcType(type);
		npc.setVariant(variant);
		npc.setDisplayName(displayName);
		npc.setPreparator(prep);
		npc.setLoc(location);
		npc.setRot(new Vector3d(0, rotation, 0));
		VillagerShops.npcs.add(npc);
		return true;
	}
	
	public static Collection<NPCguard> list() {
		return VillagerShops.npcs;
	}
	
	public static boolean playershop(NPCguard shop, UUID user) {
		try {
			shop.setPlayerShop(user);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	
}
