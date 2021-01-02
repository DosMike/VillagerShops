package de.dosmike.sponge.vshop.shops;

import com.flowpowered.math.vector.Vector3d;
import de.dosmike.sponge.vshop.Utilities;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.menus.ShopMenuManager;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.sound.SoundType;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.Agent;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;
import java.util.UUID;

public class ShopEntity {
	private final UUID shopUniqueId; //for configs;
	UUID playershopOwner = null;
	Location<World> playershopContainer = null;
	private Location<World> location;
	private Vector3d rotation;
	private ShopMenuManager menu;
	private UUID lastEntity = null;
	private EntityType npcType = EntityTypes.VILLAGER;
	private String variantName;
	private FieldResolver.KeyAttacher variant;
	private Text displayName;
	private int lookAroundTicks = 0;
	private long lastAmbientSound = 0L; //prevent some sound spam when shops are animating

	public ShopEntity(UUID identifier) {
		shopUniqueId = identifier;
		menu = new ShopMenuManager(shopUniqueId);
	}

	public ShopEntity(UUID identifier, UUID trackEntity) {
		shopUniqueId = identifier;
		lastEntity = trackEntity;
		menu = new ShopMenuManager(shopUniqueId);
	}

	public UUID getIdentifier() {
		return shopUniqueId;
	}

	/**
	 * Updates the shops location to the tracked entities location (if possible)
	 *
	 * @return The location for this shop.
	 */
	public Location<World> getLocation() {
		//update the location the shop is actually at by updating the location (if loaded/possible)
		if (location != null && location.getExtent().isLoaded()) {
			Entity ent = location.getExtent().getEntity(lastEntity).orElse(null);
			if (ent != null && ent.isLoaded() && !ent.isRemoved())
				this.location = Utilities.centerOnBlock(ent.getLocation());
		}
		return location;
	}

	public void setLocation(Location<World> location) {
		this.location = Utilities.centerOnBlock(location);
		VillagerShops.getInstance().markShopsDirty(this);
	}

	/**
	 * function to move existing shop for convenience as API
	 */
	public void move(Location<World> newLocation) {
		Optional<Chunk> optionalChunk = location.getExtent().getChunkAtBlock(location.getBlockPosition());
		if (!optionalChunk.isPresent())
			throw new RuntimeException("Chunk for shop not available!");
		Chunk chunk = optionalChunk.get();
		if (!chunk.isLoaded()) {
			if (!chunk.loadChunk(false))
				throw new RuntimeException("Unable to load chunk for shop to remove old entity");
		}
		Location<World> targetLocation = Utilities.centerOnBlock(newLocation);
		getEntity().ifPresent(le -> le.setLocation(targetLocation));
		location = targetLocation;
		VillagerShops.getInstance().markShopsDirty(this);
	}

	public Vector3d getRotation() {
		return rotation;
	}

	public void setRotation(Vector3d rotation) {
		this.rotation = rotation;
	}

	public ShopMenuManager getMenu() {
		return menu;
	}

	public void setMenu(ShopMenuManager menu) {
		this.menu = menu;
		menu.updateMenu(true);
		VillagerShops.getInstance().markShopsDirty(this);
	}

	/**
	 * recounts items in the shop container for playershops
	 */
	public void updateStock() {
		getStockInventory().ifPresent(inv -> menu.updateStock(inv));
	}

	public EntityType getNpcType() {
		return npcType;
	}

	public void setNpcType(EntityType npcType) {
		if (npcType.equals(EntityTypes.PLAYER)) npcType = EntityTypes.HUMAN;
		this.npcType = npcType;
		VillagerShops.getInstance().markShopsDirty(this);
	}

	public Text getDisplayName() {
		return displayName;
	}

	public void setDisplayName(Text name) {
		displayName = name;
		VillagerShops.getInstance().markShopsDirty(this);
	}

	public FieldResolver.KeyAttacher getVariant() {
		return variant;
	}

	/**
	 * MAY NOT BE CALLED BEFORE setNpcType()
	 *
	 * @param magicVariant is a magic string that represents a variant, skin, career or similar id
	 *                     and is automatically resolved into a CatalogType for the entity type of
	 *                     this shop. If no matching "skin" can be found, no variant will be picked
	 */
	public void setVariant(String magicVariant) {
		try {
			if (npcType.equals(EntityTypes.PLAYER)) { //entity type player will shit at you, and then laugh at you for you don't know you're supposed to use sponge:human
				npcType = EntityTypes.HUMAN;
			}
			if (magicVariant.equalsIgnoreCase("none")) {
				variant = null;
			} else if (npcType.equals(EntityTypes.HUMAN)) {
				variant = FieldResolvers.PLAYER_SKIN.validate(magicVariant);
			} else if (npcType.equals(EntityTypes.HORSE)) {
				variant = FieldResolvers.HORSE_VARIANT.validate(magicVariant);
			} else if (npcType.equals(EntityTypes.OCELOT)) {
				variant = FieldResolvers.OCELOT_VARIANT.validate(magicVariant);
			} else if (npcType.equals(EntityTypes.VILLAGER)) {
				variant = FieldResolvers.VILLAGER_VARIANT.validate(magicVariant);
			} else if (npcType.equals(EntityTypes.LLAMA)) {
				variant = FieldResolvers.LLAMA_VARIANT.validate(magicVariant);
			} else if (npcType.equals(EntityTypes.RABBIT)) {
				variant = FieldResolvers.RABBIT_VARIANT.validate(magicVariant);
			} else if (npcType.equals(EntityTypes.PARROT)) {
				variant = FieldResolvers.PARROT_VARIANT.validate(magicVariant);
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
			variantName = magicVariant; //maybe valid later?

		VillagerShops.getInstance().markShopsDirty(this);
	}

	public String getVariantName() {
		return variantName;
	}

	/**
	 * Equals getLocation().getExtent().getEntity(getEntityUniqueID().get()).
	 * This method does not update any internal values.
	 *
	 * @return the actual entity if loaded
	 */
	public Optional<Entity> getEntity() {
		return location.getExtent().getEntity(lastEntity);
	}

	public Optional<UUID> getEntityUniqueID() {
		return Optional.ofNullable(lastEntity);
	}

	/**
	 * by setting a owner id this will try to turn into a player shop.<br>
	 * If there's no chest below the shop this will throw a IllegalStateException.<br>
	 * If the argument is null the playershop association is lifted
	 */
	public void setPlayerShop(UUID ownerId) throws IllegalStateException {
		if (ownerId == null) {
			playershopOwner = null;
			playershopContainer = null;
			return;
		}
		Location<World> scan = Utilities.centerOnBlock(getLocation()).getBlockRelative(Direction.DOWN);
		Optional<TileEntity> te = scan.getTileEntity();
		if (!te.isPresent() || !(te.get() instanceof TileEntityCarrier) || ((TileEntityCarrier) te.get()).getInventory().capacity() < 27) {
			scan = scan.getBlockRelative(Direction.DOWN);
			te = scan.getTileEntity();
			if (!te.isPresent() || !(te.get() instanceof TileEntityCarrier) || ((TileEntityCarrier) te.get()).getInventory().capacity() < 27)
				throw new IllegalStateException("Shop is not placed above a chest");
		}

		playershopOwner = ownerId;
		playershopContainer = scan;
		VillagerShops.getInstance().markShopsDirty(this);
	}

	public boolean isShopOwner(UUID playerId) {
		return playershopOwner != null && playershopOwner.equals(playerId);
	}

	public Optional<Inventory> getStockInventory() {
		if (playershopOwner == null) return Optional.empty();
		try {
			Optional<TileEntity> te = playershopContainer.getTileEntity();
			if (!te.isPresent() || !(te.get() instanceof TileEntityCarrier) || ((TileEntityCarrier) te.get()).getInventory().capacity() < 27)
				throw new RuntimeException("ContainerBlock not Chest");
			return Optional.of(((TileEntityCarrier) te.get()).getInventory());
		} catch (Exception e) {
			VillagerShops.w("Could not receive container for Playershop at " + location.getExtent().getName() + " " + location.getBlockPosition());
			return Optional.empty();
		}
	}

	public Optional<Location<World>> getStockContainer() {
		return playershopOwner == null ? Optional.empty() : Optional.of(playershopContainer);
	}

	/**
	 * used for internals
	 */
	public void setStockContainerRaw(Location<World> containerLocation) {
		playershopContainer = containerLocation;
	}

	/**
	 * if present this shop is a player-shop as defined by setPlayerShop
	 */
	public Optional<UUID> getShopOwner() {
		return Optional.ofNullable(playershopOwner);
	}

	/**
	 * used for internals
	 */
	public void setShopOwnerRaw(UUID shopOwnerRawId) {
		playershopOwner = shopOwnerRawId;
	}

	/**
	 * This method is only called one every ~5 ticks IF animated npcs are enabled in the config
	 * the purpose of this method is to make this entity look around a bit and make it feel less
	 * stiff.
	 */
	public void tick() {
		getEntity().ifPresent(le -> {
			try {//because anonymous & in scheduler exceptions might otherwise be lost
				if (le instanceof Living) {
					// From my tests agents always return AI_ENABLED even if AI was disabled
					// Todo: find another way to check?
					//if (le.get(Keys.AI_ENABLED).orElse(false)) return; // this entity is acting on it's own

					//check if a player is nearby, try to apply lookAtNear AITask
					Optional<Player> viewTarget = le.getLocation().getExtent()
							.getNearbyEntities(le.getLocation().getPosition(), 5.0).stream()
							.filter(e -> e instanceof Player).map(e -> (Player) e).findFirst();
					if (le instanceof Agent && viewTarget.isPresent()) {
						//check if player is visible:
						// check if placer and shop are facing each other and the player is not behind the shop
						// (would look strange if they track "forever" -> creepy eyes)
						// The dot product for two vectors is zero if they are orthogonal, and <0 if the vectors
						// face each other. i think that's also called similarity
						// "invert" player direction to check if looking TOWARDS each other (i made that mistake...)
						double playerYaw = Utilities.clampAngleDeg(viewTarget.get().getHeadRotation().getY() + 180);
						double shopYaw = Utilities.clampAngleDeg(rotation.getY());
						double agentYaw = Utilities.clampAngleDeg(((Agent) le).getHeadRotation().getY());
						Vector3d playerToAgent = le.getLocation().getPosition().sub(viewTarget.get().getPosition()).normalize();
						boolean facing =
								Math.abs(Utilities.clampAngleDeg(playerYaw - agentYaw)) < 45 && //looking at each other within 45°
										Math.abs(Utilities.clampAngleDeg(playerYaw - shopYaw)) < 80 && //standing in front of the shop
										playerToAgent.dot(Utilities.directiond(rotation)) < 0; //ensure the player is standing in front of the shop
						if (facing) {
							// try to play the mobs ambient sound when noticing a player
							// with a probability increasing over idle time (min 50%)
							// To prevent frequent sounds when a player repeatedly looks at a shop use the delay var
							if (lookAroundTicks != 0 && 50 + (lookAroundTicks >> 1) > VillagerShops.rng.nextInt(100) && System.currentTimeMillis() - lastAmbientSound > 7000) {
								//should be faster than split
								String eid = le.getType().getId();
								int off = eid.indexOf(':');
								if (off >= 0)
									le.getLocation().getExtent().playSound(SoundType.of("entity." + eid.substring(off + 1) + ".ambient"), le.getLocation().getPosition(), 1f);
								lastAmbientSound = System.currentTimeMillis(); // i guess it's up to personal preference whether to put this inside the if block or not
							}
							lookAroundTicks = 0;
							((Agent) le).lookAt(viewTarget.get().getLocation().getPosition().add(0, 1.62f, 0)); //1.62 is player eye height
							return;
						}
					}
					if ((++lookAroundTicks > 15 && VillagerShops.rng.nextInt(10) == 0) || lookAroundTicks > 100) {
						Living mo = (Living) le;
						lookAroundTicks = 0;
						le.setLocationAndRotation(location, rotation);
						mo.setHeadRotation(new Vector3d(VillagerShops.rng.nextFloat() * 30 - 14, rotation.getY() + VillagerShops.rng.nextFloat() * 60 - 30, 0.0));
					}
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		});
	}

	/**
	 * Tries to find a matching entity for this shop.
	 * If there was a previously tracked entity (e.g. by importing an entity into a shop)
	 * this method will adjust this shops location to that of the entity.
	 * If there was no entity tracked, the last known/set location will be searched for
	 * an arbitrary entity. There are no further check in case some mod replaces entities (type and uuid changes).
	 * The method spawn() will be called if there was not entity at the expected location.
	 * Any newly tracked entity will be repositioned into the tracked location/rotation.
	 *
	 * @return empty if the chunk/extent is not currently loaded
	 */
	public Optional<Entity> findOrCreate() {
		// Check if the tracked entity still exists (will fail on new shops)
		Optional<Entity> existing = getEntity();
		if (existing.isPresent() && !existing.get().isRemoved()) {
			//update the location of this shop, in case it's ai enabled and runs around
			getLocation();
			return existing;
		}
		// There's no tracked entity, find a matching one at the specified location
		Optional<Chunk> chunk = location.getExtent().getChunk(location.getChunkPosition());
		if (chunk.isPresent() && chunk.get().isLoaded()) {
			//first try to link the entity again:
			Entity shop = chunk.get().getNearbyEntities(location.getPosition(), 0.4).stream()
					//prevent picking up the player
					.filter(ent -> !ent.getType().equals(EntityTypes.PLAYER))
					//either this IS this entity -> pass OR that entity was not yet assigned to a shop -> scoop up
					.filter(ent -> ent.getUniqueId().equals(lastEntity) || !VillagerShops.isEntityShop(ent))
					.findFirst().orElseGet(this::spawn); // or create new one
			shop.setLocationAndRotation(location, rotation);
			lastEntity = shop.getUniqueId();
			return Optional.of(shop);
		} else return Optional.empty();
	}

	public Entity spawn() {
		Entity shop = location.getExtent().createEntity(npcType, location.getPosition());
		shop.offer(Keys.AI_ENABLED, false);
		shop.offer(Keys.IS_SILENT, true);
		shop.offer(Keys.INVULNERABLE, true);
		shop.offer(Keys.DISPLAY_NAME, displayName);

		//setting variant. super consistent ;D
		if (variant != null)
			try {
				variant.attach(shop);
			} catch (Exception e) {
				VillagerShops.l("Variant no longer supported! Did the EntityType change?");
			}

		if (location.getExtent().spawnEntity(shop)) {
			lastEntity = shop.getUniqueId();
		} else {
			VillagerShops.w("Unable to spawn shop %s - Check spawn protection and chunk limits at %s %d %d %d!",
					shop.getUniqueId().toString(),
					shop.getLocation().getExtent().getName(),
					shop.getLocation().getBlockX(),
					shop.getLocation().getBlockY(),
					shop.getLocation().getBlockZ());
		}
		return shop;
	}

	public void freeEntity() {
		getEntity().ifPresent(shop -> {
			shop.offer(Keys.AI_ENABLED, true);
			shop.offer(Keys.IS_SILENT, false);
			shop.offer(Keys.INVULNERABLE, false);
			lastEntity = null;
		});
	}

	/**
	 * Custom toString name [id] { more }
	 */
	@Override
	public String toString() {
		return String.format("%s [%s] { type: %s, entity: %s, skin: %s, location: %s°%.2f }",
				displayName.toPlain(), shopUniqueId.toString(),
				playershopOwner != null ? "playershop" : "adminshop",
				npcType.getId(), variantName,
				Utilities.toString(location), rotation.getY()
		);
	}

}
