package de.dosmike.sponge.vshop.shops;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.reflect.TypeToken;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.menus.ShopMenuManager;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class ShopEntitySerializer implements TypeSerializer<ShopEntity> {

    @SuppressWarnings("serial")
    public static final TypeToken<List<ShopEntity>> tokenListNPCguard = new TypeToken<List<ShopEntity>>() {
    };
    @SuppressWarnings("serial")
    static final TypeToken<List<StockItem>> tokenListStockItem = new TypeToken<List<StockItem>>() {
    };
    @SuppressWarnings("serial")
    static final TypeToken<Location<World>> tokenLocationWorld = new TypeToken<Location<World>>() {
    };

    @Override
    public void serialize(@NotNull TypeToken<?> arg0, ShopEntity npc, ConfigurationNode rootNode) throws ObjectMappingException {
        rootNode.getNode("uuid").setValue(npc.getIdentifier().toString());
        rootNode.getNode("items").setValue(tokenListStockItem, npc.getPreparator().getAllItems());
        ConfigurationNode location = rootNode.getNode("location");
        location.getNode("WorldUuid").setValue(npc.getLocation().getExtent().getUniqueId().toString());
        location.getNode("X").setValue(npc.getLocation().getX());
        location.getNode("Y").setValue(npc.getLocation().getY());
        location.getNode("Z").setValue(npc.getLocation().getZ());
        rootNode.getNode("rotation").setValue(npc.getRotation().getY()); // we only need the yaw rotationb
        rootNode.getNode("entitytype").setValue(npc.getNpcType().getId());
        rootNode.getNode("variant").setValue(npc.getVariantName());
        rootNode.getNode("displayName").setValue(TextSerializers.FORMATTING_CODE.serialize(npc.getDisplayName()));
        if (npc.playershopholder != null)
            rootNode.getNode("playershop").setValue(npc.playershopholder.toString());
        if (npc.playershopcontainer != null)
            rootNode.getNode("stocklocation").setValue(tokenLocationWorld, npc.playershopcontainer);
    }

    @Override
    public ShopEntity deserialize(@NotNull TypeToken<?> arg0, ConfigurationNode cfg) throws ObjectMappingException {
        ShopEntity npc = new ShopEntity(UUID.fromString(Objects.requireNonNull(cfg.getNode("uuid").getString())));
        ShopMenuManager ip = new ShopMenuManager();

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
        ip.setAllItems(items);
        npc.setPreparator(ip);
        ConfigurationNode location = cfg.getNode("location");
        try {
            World w = Sponge.getServer().getWorld(UUID.fromString(location.getNode("WorldUuid").getString("??")))
                    .orElseThrow(()->new ObjectMappingException("Could not find world by uuid"));
            Vector3d v = new Vector3d(location.getNode("X").getDouble(), location.getNode("Y").getDouble(), location.getNode("Z").getDouble());
            npc.setLocation(new Location<>(w,v));
        } catch (Exception e) {
            throw new ObjectMappingException("Could not load location for shop", e);
        }
        npc.setRotation(new Vector3d(0.0, cfg.getNode("rotation").getDouble(0.0), 0.0));
        Optional<EntityType> type = Sponge.getRegistry().getType(EntityType.class, cfg.getNode("entitytype").getString("minecraft:villager"));
        npc.setNpcType(type.orElseThrow(()->new ObjectMappingException("Could not read entity type")));
        npc.setVariant(cfg.getNode("variant").getString("NONE"));
        npc.setDisplayName(TextSerializers.FORMATTING_CODE.deserialize(cfg.getNode("displayName").getString("VillagerShop")));
        String tmp = cfg.getNode("playershop").getString(null);
        if (tmp != null && !tmp.isEmpty())
            npc.playershopholder = UUID.fromString(tmp);
        npc.playershopcontainer = cfg.getNode("stocklocation").getValue(tokenLocationWorld);
        return npc;
    }
}
