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
    public void serialize(@NotNull TypeToken<?> arg0, ShopEntity shop, ConfigurationNode rootNode) throws ObjectMappingException {
        if (shop == null) return;
        rootNode.getNode("uuid").setValue(shop.getIdentifier().toString());
        rootNode.getNode("items").setValue(tokenListStockItem, shop.getMenu().getAllItems());
        ConfigurationNode location = rootNode.getNode("location");
        //save the location the shop is actually at by updating the location (if loaded/possible)
        Location<World> currentLocation = shop.getLocation();
        location.getNode("WorldUuid").setValue(currentLocation.getExtent().getUniqueId().toString());
        location.getNode("X").setValue(currentLocation.getX());
        location.getNode("Y").setValue(currentLocation.getY());
        location.getNode("Z").setValue(currentLocation.getZ());
        rootNode.getNode("rotation").setValue(shop.getRotation().getY()); // we only need the yaw rotationb
        rootNode.getNode("entitytype").setValue(shop.getNpcType().getId());
        rootNode.getNode("variant").setValue(shop.getVariantName());
        rootNode.getNode("displayName").setValue(TextSerializers.FORMATTING_CODE.serialize(shop.getDisplayName()));
        if (shop.playershopOwner != null)
            rootNode.getNode("playershop").setValue(shop.playershopOwner.toString());
        if (shop.playershopContainer != null)
            rootNode.getNode("stocklocation").setValue(tokenLocationWorld, shop.playershopContainer);
    }

    @Override
    public ShopEntity deserialize(@NotNull TypeToken<?> arg0, ConfigurationNode cfg) throws ObjectMappingException {
        UUID shopId = UUID.fromString(Objects.requireNonNull(cfg.getNode("uuid").getString()));
        ShopEntity npc = new ShopEntity(shopId);

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
        npc.getMenu().setAllItems(items);
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
            npc.playershopOwner = UUID.fromString(tmp);
        npc.playershopContainer = cfg.getNode("stocklocation").getValue(tokenLocationWorld);
        return npc;
    }
}
