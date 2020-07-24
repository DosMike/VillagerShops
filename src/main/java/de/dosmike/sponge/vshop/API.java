package de.dosmike.sponge.vshop;

import com.flowpowered.math.vector.Vector3d;
import de.dosmike.sponge.vshop.menus.ShopMenuManager;
import de.dosmike.sponge.vshop.shops.ShopEntity;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * methods return true on success
 */
@SuppressWarnings("unused")
public class API {

    public static ShopEntity create(EntityType type, String variant, Text displayName, Location<World> location, Double rotation) {
        if (VillagerShops.isLocationOccupied(location)) return null;
        ShopEntity shopEntity = new ShopEntity(UUID.randomUUID());
        ShopMenuManager prep = new ShopMenuManager();
        shopEntity.setNpcType(type);
        shopEntity.setVariant(variant);
        shopEntity.setDisplayName(displayName);
        shopEntity.setMenu(prep);
        shopEntity.setLocation(location);
        shopEntity.setRotation(new Vector3d(0, rotation, 0));
        VillagerShops.addShop(shopEntity, true);
        VillagerShops.audit("The shop %s [%s] was created via API { entity: %s, skin: %s, location: %s }",
                displayName.toPlain(), shopEntity.getIdentifier().toString(),
                type.getId(), shopEntity.getVariantName(),
                location.getExtent().getName()+"@"+location.getBlockX()+"/"+location.getBlockY()+"/"+location.getBlockZ()+"Y"+rotation
        );
        return shopEntity;
    }

    public static Collection<ShopEntity> list() {
        return VillagerShops.getShops();
    }

    public static void delete(ShopEntity shop) {
        VillagerShops.stopTimers();
        VillagerShops.closeShopInventories(shop.getIdentifier());
        disintegrate(shop);//shop.getLe().remove();
        VillagerShops.removeShop(shop);
        VillagerShops.startTimers();
        VillagerShops.audit("The shop %s [%s] was deleted via API { entity: %s, skin: %s, location: %s }",
                shop.getDisplayName().toPlain(), shop.getIdentifier().toString(),
                shop.getNpcType().getId(), shop.getVariantName(),
                shop.getLocation().getExtent().getName()+"@"+shop.getLocation().getBlockX()+"/"+shop.getLocation().getBlockY()+"/"+shop.getLocation().getBlockZ()+"Y"+shop.getRotation()
        );
    }

    public static boolean playershop(ShopEntity shop, UUID user, Location<World> container) {
        if (user != null) {
            if (container != null && container.getBlockType().equals(BlockTypes.CHEST)) {
                shop.setShopOwnerRaw(user);
                shop.setStockContainerRaw(container);
                VillagerShops.audit("The shop owner for %s [%s] was changed to [%s] via API { entity: %s, skin: %s, location: %s }",
                        shop.getDisplayName().toPlain(), shop.getIdentifier().toString(),
                        user.toString(),
                        shop.getNpcType().getId(), shop.getVariantName(),
                        shop.getLocation().getExtent().getName()+"@"+shop.getLocation().getBlockX()+"/"+shop.getLocation().getBlockY()+"/"+shop.getLocation().getBlockZ()+"Y"+shop.getRotation()
                );
            } else return false;
        } else {
            if (container != null) return false;
            shop.setShopOwnerRaw(null);
            shop.setStockContainerRaw(null);
            VillagerShops.audit("The shop owner for %s [%s] was removed via API { entity: %s, skin: %s, location: %s }",
                    shop.getDisplayName().toPlain(), shop.getIdentifier().toString(),
                    shop.getNpcType().getId(), shop.getVariantName(),
                    shop.getLocation().getExtent().getName()+"@"+shop.getLocation().getBlockX()+"/"+shop.getLocation().getBlockY()+"/"+shop.getLocation().getBlockZ()+"Y"+shop.getRotation()
            );
        }
        return true;
    }

    /**
     * something like prepare a shop to be modified
     */
    public static void disintegrate(ShopEntity shop) {
        Optional<Chunk> c = shop.getLocation().getExtent().getChunkAtBlock(shop.getLocation().getBlockPosition());
        if (!c.isPresent())
            throw new RuntimeException("Chunk for shop not available!");
        Chunk chunk = c.get();
        if (!chunk.isLoaded()) {
            if (!chunk.loadChunk(false))
                throw new RuntimeException("Unable to load chunk for shop to remove old entity");
        }
        shop.getEntity().ifPresent(Entity::remove);
    }


}
