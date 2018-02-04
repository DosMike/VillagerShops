package de.dosmike.sponge.vshop;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.reflect.TypeToken;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

public class NPCguardSerializer implements TypeSerializer<NPCguard> {
	
	@SuppressWarnings("serial")
	static TypeToken<List<NPCguard>> tokenListNPCguard = new TypeToken<List<NPCguard>>() {};
	@SuppressWarnings("serial")
	static TypeToken<List<StockItem>> tokenListStockItem = new TypeToken<List<StockItem>>() {};
	@SuppressWarnings("serial")
	static TypeToken<Location<World>> tokenLocationWorld = new TypeToken<Location<World>>() {};
	
	@Override
	public void serialize(TypeToken<?> arg0, NPCguard npc, ConfigurationNode rootNode) throws ObjectMappingException {
		rootNode.getNode("uuid").setValue(npc.getIdentifier().toString());
		rootNode.getNode("items").setValue(tokenListStockItem, npc.getPreparator().items);
		rootNode.getNode("location").setValue(tokenLocationWorld, npc.getLoc());
		rootNode.getNode("rotation").setValue(npc.getRot().getY()); // we only need the yaw rotationb
		rootNode.getNode("entitytype").setValue(npc.getNpcType().getId());
		rootNode.getNode("variant").setValue(npc.getVariantName());
		rootNode.getNode("displayName").setValue(TextSerializers.FORMATTING_CODE.serialize(npc.getDisplayName()));
		if (npc.playershopholder!=null)
			rootNode.getNode("playershop").setValue(npc.playershopholder.toString());
		if (npc.playershopcontainer!=null)
			rootNode.getNode("stocklocation").setValue(tokenLocationWorld, npc.playershopcontainer);
	}
	
	@Override
	public NPCguard deserialize(TypeToken<?> arg0, ConfigurationNode cfg) throws ObjectMappingException {
		NPCguard npc = new NPCguard(UUID.fromString(cfg.getNode("uuid").getString()));
		InvPrep ip = new InvPrep();
		
		ip.items = cfg.getNode("items").getValue(tokenListStockItem);
		if (ip.items==null) ip.items = new LinkedList<>();
		npc.setPreparator(ip);
	    npc.setLoc(cfg.getNode("location").getValue(tokenLocationWorld));
	    npc.setRot(new Vector3d(0.0, cfg.getNode("rotation").getDouble(0.0), 0.0 ));
	    npc.setNpcType((EntityType)FieldResolver.getFinalStaticByName(EntityType.class, cfg.getNode("entitytype").getString("minecraft:villager")));
	    npc.setVariant(cfg.getNode("variant").getString("NONE"));
		npc.setDisplayName(TextSerializers.FORMATTING_CODE.deserialize(cfg.getNode("displayName").getString("VillagerShop")));
		String tmp = cfg.getNode("playershop").getString(null);
		if (tmp != null && !tmp.isEmpty())
			npc.playershopholder=UUID.fromString(tmp);
		npc.playershopcontainer=cfg.getNode("stocklocation").getValue(tokenLocationWorld);
		return npc;
	}
}
