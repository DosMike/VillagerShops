package de.dosmike.sponge.vshop.shops;

import com.flowpowered.math.vector.Vector3d;
import de.dosmike.sponge.megamenus.api.IMenu;
import de.dosmike.sponge.vshop.Utilities;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.menus.InvPrep;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public class NPCguard {
    private Location<World> loc;
    private Vector3d rot;
    private UUID lastKnown;
    private InvPrep preparator;
    private Entity le = null;
    private EntityType npcType = EntityTypes.VILLAGER;
    private String variantName;
    private FieldResolver.KeyAttacher variant;
    private boolean invulnerable;
    private Text displayName;
    private UUID ident; //for configs;
    UUID playershopholder = null;
    Location<World> playershopcontainer = null;

    private int lookAroundTicks = 0;

    public NPCguard(UUID identifier) {
        ident = identifier;
    }

    public UUID getIdentifier() {
        return ident;
    }

    public Location<World> getLoc() {
        return loc;
    }

    /**
     * function to move existing shop for convenience as API
     */
    public void move(Location<World> newLoc) {
        Optional<Chunk> c = loc.getExtent().getChunkAtBlock(loc.getBiomePosition());
        if (!c.isPresent())
            throw new RuntimeException("Chunk for shop not available!");
        Chunk chunk = c.get();
        if (!chunk.isLoaded()) {
            if (!chunk.loadChunk(false))
                throw new RuntimeException("Unable to load chunk for shop to remove old entity");
        }
        if (le != null && !le.isRemoved())
            le.remove();
        loc = newLoc;
    }

    public void setLoc(Location<World> loc) {
        this.loc = new Location<>(loc.getExtent(), loc.getBlockX() + 0.5, loc.getY(), loc.getBlockZ() + 0.5);
        VillagerShops.getInstance().markNpcsDirty();
    }

    public Vector3d getRot() {
        return rot;
    }

    public void setRot(Vector3d rot) {
        this.rot = rot;
    }

    public InvPrep getPreparator() {
        return preparator;
    }

    public IMenu getMenu() {
        return preparator.getMenu();
    }

    /**
     * recounts items in the shop container for playershops
     */
    public void updateStock() {
        getStockInventory().ifPresent(inv -> preparator.updateStock(inv));
    }

    public void setPreparator(InvPrep preparator) {
        this.preparator = preparator;
        preparator.updateMenu(true);
        VillagerShops.getInstance().markNpcsDirty();
    }

    public EntityType getNpcType() {
        return npcType;
    }

    public void setNpcType(EntityType npcType) {
        if (npcType.equals(EntityTypes.PLAYER)) npcType = EntityTypes.HUMAN;
        this.npcType = npcType;
        VillagerShops.getInstance().markNpcsDirty();
    }

    public void setDisplayName(Text name) {
        displayName = name;
        VillagerShops.getInstance().markNpcsDirty();
    }

    public Text getDisplayName() {
        return displayName;
    }

    /**
     * MAY NOT BE CALLED BEFORE setNpcType()
     */
    public void setVariant(String fieldName) {
        try {
            if (npcType.equals(EntityTypes.PLAYER)) { //entity type player will shit at you, and then laugh at you for you don't know you're supposed to use sponge:human
                npcType = EntityTypes.HUMAN;
            }
            if (fieldName.equalsIgnoreCase("none")) {
                variant = null;
            } else if (npcType.equals(EntityTypes.HUMAN)) {
                variant = FieldResolver.PLAYER_SKIN.validate(fieldName);
            } else if (npcType.equals(EntityTypes.HORSE)) {
                variant = FieldResolver.HORSE_VARIANT.validate(fieldName);
            } else if (npcType.equals(EntityTypes.OCELOT)) {
                variant = FieldResolver.OCELOT_VARIANT.validate(fieldName);
            } else if (npcType.equals(EntityTypes.VILLAGER)) {
                variant = FieldResolver.VILLAGER_VARIANT.validate(fieldName);
            } else if (npcType.equals(EntityTypes.LLAMA)) {
                variant = FieldResolver.LLAMA_VARIANT.validate(fieldName);
            } else if (npcType.equals(EntityTypes.RABBIT)) {
                variant = FieldResolver.RABBIT_VARIANT.validate(fieldName);
            } else if (npcType.equals(EntityTypes.PARROT)) {
                variant = FieldResolver.PARROT_VARIANT.validate(fieldName);
            } else {
                variantName = "NONE";
                variant = null;
            }
        } catch (Exception e) { //ignore non existant fields due to lower version
            e.printStackTrace();
            variantName = "NONE";
            variant = null;
        }
        if (variant != null) {
            variantName = variant.toString();
        } else
            variantName = fieldName; //maybe valid later?

        VillagerShops.getInstance().markNpcsDirty();
    }

    public Object getVariant() {
        return variant;
    }

    public String getVariantName() {
        return variantName;
    }

    public void setInvulnerable(boolean invulnerable) {
        this.invulnerable = invulnerable;
    }

    public boolean getInvulnerable() {
        return this.invulnerable;
    }

    public Entity getLe() {
        return le;
    }

    /**
     * by setting a owner id this will try to turn into a player shop.<br>
     * If there's no chest below the shop this will throw a IllegalStateException.<br>
     * If the argument is null the playershop association is lifted
     */
    public void setPlayerShop(UUID owner) throws IllegalStateException {
        if (owner == null) {
            playershopholder = null;
            playershopcontainer = null;
            return;
        }
        Location<World> scan = getLoc().sub(0, 0.5, 0);
        Optional<TileEntity> te = scan.getTileEntity();
        if (!te.isPresent() || !(te.get() instanceof TileEntityCarrier) || ((TileEntityCarrier) te.get()).getInventory().capacity() < 27) {
            scan = scan.sub(0, 1, 0);
            te = scan.getTileEntity();
            if (!te.isPresent() || !(te.get() instanceof TileEntityCarrier) || ((TileEntityCarrier) te.get()).getInventory().capacity() < 27)
                throw new IllegalStateException("Shop is not placed above a chest");
        }

        playershopholder = owner;
        playershopcontainer = scan;
        VillagerShops.getInstance().markNpcsDirty();
    }

    public boolean isShopOwner(UUID player) {
        return playershopholder != null && playershopholder.equals(player);
    }

    public Optional<Inventory> getStockInventory() {
        if (playershopholder == null) return Optional.empty();
        try {
            Optional<TileEntity> te = playershopcontainer.getTileEntity();
            if (!te.isPresent() || !(te.get() instanceof TileEntityCarrier) || ((TileEntityCarrier) te.get()).getInventory().capacity() < 27)
                throw new RuntimeException("ContainerBlock not Chest");
            return Optional.of(((TileEntityCarrier) te.get()).getInventory());
        } catch (Exception e) {
            VillagerShops.w("Could not receive container for Playershop at " + loc.getExtent().getName() + " " + loc.getBlockPosition());
            return Optional.empty();
        }
    }

    public Optional<Location<World>> getStockContainer() {
        return playershopholder == null ? Optional.empty() : Optional.of(playershopcontainer);
    }
    /** used for internals */
    public void setStockContainerRaw(Location<World> location) {
        playershopcontainer = location;
    }

    /**
     * if present this shop is a player-shop as defined by setPlayerShop
     */
    public Optional<UUID> getShopOwner() {
        return playershopholder == null ? Optional.empty() : Optional.of(playershopholder);
    }
    /** used for internals */
    public void setShopOwnerRaw(UUID uuid) {
        playershopholder = uuid;
    }

    public void tick() {
        World w = loc.getExtent();
        Optional<Chunk> chunk = w.getChunkAtBlock(loc.getBlockPosition());
        if (le == null) {
            if (chunk.isPresent() && chunk.get().isLoaded()) {
                //first try to link the entity again:
                Collection<Entity> ents = chunk.get().getEntities();
                for (Entity ent : ents) {
                    Text entityDisplayName = ent.get(Keys.DISPLAY_NAME).orElse(null); //should not be null if name is supported as we default it to VillagerShops
                    //a bit more complex to allow null values for both entity name and set name (although set name should never be null)
                    boolean nameEquals = (!ent.supports(Keys.DISPLAY_NAME) || (entityDisplayName == null)) || //if the entity does not support display names we'll ignore the name rule
                            ((displayName == null || displayName.isEmpty()) && (entityDisplayName.isEmpty())) ||
                            (displayName != null && displayName.equals(entityDisplayName));
                    if ((ent.isLoaded() && ent.getType().equals(npcType) && (ent.getUniqueId().equals(lastKnown) ||
                        (ent.getLocation().getExtent().equals(loc.getExtent()) && ent.getLocation().getPosition().distanceSquared(loc.getPosition()) < 1))) && //can't check for bigger distances, as it will yank npcs from other shops with the same name
                        (!VillagerShops.isNPCused(ent) && nameEquals)) {    //check if npc already belongs to a different shop
                        le = ent;
                        le.setLocationAndRotation(loc, rot);

                        break;
                    }
                }
                if (le == null) { //unable to restore
                    Entity shop = w.createEntity(npcType, loc.getPosition());
                    shop.offer(Keys.AI_ENABLED, false);
                    shop.offer(Keys.IS_SILENT, true);

                    //setting variant. super consistent ;D
                    if (variant != null)
                        try {
                            variant.attach(shop);
                        } catch (Exception e) {
                            VillagerShops.l("Variant no longer supported! Did the EntityType change?");
                        }

                    if (invulnerable)
                        shop.offer(Keys.INVULNERABLE, true);

                    shop.offer(Keys.DISPLAY_NAME, displayName);

                    if (w.spawnEntity(shop)) {
                        lastKnown = shop.getUniqueId();
                    } else {
                        VillagerShops.w("Unable to spawn shop %s - Check spawn protection and chunk limits at %s %d %d %d!",
                                shop.getUniqueId().toString(),
                                shop.getLocation().getExtent().getName(),
                                shop.getLocation().getBlockX(),
                                shop.getLocation().getBlockY(),
                                shop.getLocation().getBlockZ());
                    }
                }
            }
        } else {
            if (!le.isLoaded() || !chunk.isPresent() || !chunk.get().isLoaded()) {
                le = null;    //allowing minecraft to free the resources
            } else if (le.isRemoved() || le.get(Keys.HEALTH).orElse(1.0) <= 0) {
                le = null;
            } else {
                le.setLocationAndRotation(loc, rot);
                if (le instanceof Living) {
                    if ((++lookAroundTicks > 15 && VillagerShops.rng.nextInt(10) == 0) || lookAroundTicks > 100) {
                        Living mo = (Living) le;
                        lookAroundTicks = 0;

                        mo.setHeadRotation(new Vector3d(VillagerShops.rng.nextFloat() * 30 - 14, rot.getY() + VillagerShops.rng.nextFloat() * 60 - 30, 0.0));
                    }
                }
            }
        }
    }

    /** Custom toString name [is] { more } */
    @Override
    public String toString() {
        return String.format("%s [%s] { type: %s, entity: %s, skin: %s, location: %s°%.2f }",
                displayName.toPlain(), ident.toString(),
                playershopholder != null ? "playershop" : "adminshop",
                npcType.getId(), variantName,
                Utilities.toString(loc), rot.getY()
        );
    }
}
