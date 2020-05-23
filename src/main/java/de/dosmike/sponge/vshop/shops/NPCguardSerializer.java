package de.dosmike.sponge.vshop.shops;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.reflect.TypeToken;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.menus.InvPrep;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class NPCguardSerializer implements TypeSerializer<NPCguard> {

    @SuppressWarnings("serial")
    public static TypeToken<List<NPCguard>> tokenListNPCguard = new TypeToken<List<NPCguard>>() {
    };
    @SuppressWarnings("serial")
    static TypeToken<List<StockItem>> tokenListStockItem = new TypeToken<List<StockItem>>() {
    };
    @SuppressWarnings("serial")
    static TypeToken<Location<World>> tokenLocationWorld = new TypeToken<Location<World>>() {
    };

    @Override
    public void serialize(TypeToken<?> arg0, NPCguard npc, ConfigurationNode rootNode) throws ObjectMappingException {
        rootNode.getNode("uuid").setValue(npc.getIdentifier().toString());
        rootNode.getNode("items").setValue(tokenListStockItem, npc.getPreparator().getAllItems());
        ConfigurationNode location = rootNode.getNode("location");
        location.getNode("WorldUuid").setValue(npc.getLoc().getExtent().getUniqueId().toString());
        location.getNode("X").setValue(npc.getLoc().getX());
        location.getNode("Y").setValue(npc.getLoc().getY());
        location.getNode("Z").setValue(npc.getLoc().getZ());
        rootNode.getNode("rotation").setValue(npc.getRot().getY()); // we only need the yaw rotationb
        rootNode.getNode("entitytype").setValue(npc.getNpcType().getId());
        rootNode.getNode("variant").setValue(npc.getVariantName());
        rootNode.getNode("invulnerable").setValue(npc.getInvulnerable());
        rootNode.getNode("displayName").setValue(TextSerializers.FORMATTING_CODE.serialize(npc.getDisplayName()));
        if (npc.playershopholder != null)
            rootNode.getNode("playershop").setValue(npc.playershopholder.toString());
        if (npc.playershopcontainer != null)
            rootNode.getNode("stocklocation").setValue(tokenLocationWorld, npc.playershopcontainer);
    }

    @Override
    public NPCguard deserialize(TypeToken<?> arg0, ConfigurationNode cfg) throws ObjectMappingException {
        NPCguard npc = new NPCguard(UUID.fromString(cfg.getNode("uuid").getString()));
        InvPrep ip = new InvPrep();

        List<? extends ConfigurationNode> itemList = cfg.getNode("items").getChildrenList();
        List<StockItem> items = new ArrayList<>(itemList.size());
        for (int i = 0; i < itemList.size(); i++) {
            try {
                items.add(itemList.get(i).getValue(TypeToken.of(StockItem.class)));
            } catch (Exception e) {
                System.err.println("Could not load item "+(i+1)+" in shop "+cfg.getNode("uuid").getString("<NO ID>")+":");
                System.err.println("> ItemType seems to be "+itemList.get(i).getNode("itemstack").getNode("ItemType").getString("<NOT SET>"));
                e.printStackTrace(System.err);
                VillagerShops.w("Trying to continue parsing the config...");
            }
        }
        if (items == null) items = new LinkedList<>();
        ip.setAllItems(items);
        npc.setPreparator(ip);
        ConfigurationNode location = cfg.getNode("location");
        try {
            World w = Sponge.getServer().getWorld(UUID.fromString(location.getNode("WorldUuid").getString("??"))).get();
            Vector3d v = new Vector3d(location.getNode("X").getDouble(), location.getNode("Y").getDouble(), location.getNode("Z").getDouble());
            npc.setLoc(new Location<>(w,v));
        } catch (Exception e) {
            throw new ObjectMappingException("Could not load location for shop", e);
        }
        npc.setRot(new Vector3d(0.0, cfg.getNode("rotation").getDouble(0.0), 0.0));
        npc.setNpcType(Sponge.getRegistry().getType(EntityType.class, cfg.getNode("entitytype").getString("minecraft:villager")).orElse(null));
        npc.setVariant(cfg.getNode("variant").getString("NONE"));
        npc.setInvulnerable(cfg.getNode("invulnerable").getBoolean(false));
        npc.setDisplayName(TextSerializers.FORMATTING_CODE.deserialize(cfg.getNode("displayName").getString("VillagerShop")));
        String tmp = cfg.getNode("playershop").getString(null);
        if (tmp != null && !tmp.isEmpty())
            npc.playershopholder = UUID.fromString(tmp);
        npc.playershopcontainer = cfg.getNode("stocklocation").getValue(tokenLocationWorld);
        return npc;
    }
}
