package de.dosmike.sponge.vshop;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.CatalogType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.Career;
import org.spongepowered.api.data.type.HorseColor;
import org.spongepowered.api.data.type.LlamaVariant;
import org.spongepowered.api.data.type.OcelotType;
import org.spongepowered.api.data.type.ParrotVariant;
import org.spongepowered.api.data.type.Profession;
import org.spongepowered.api.data.type.RabbitType;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryArchetypes;
import org.spongepowered.api.item.inventory.property.InventoryTitle;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3d;

public class NPCguard {
	private Location<World> loc;
	private Vector3d rot;
	private UUID lastKnown;
	private InvPrep preparator;
	private Entity le = null;
	private EntityType npcType = EntityTypes.VILLAGER;
	private String variantName; private CatalogType variant;
	private Text displayName;
	private UUID ident; //for configs;
	UUID playershopholder = null;
	Location<World> playershopcontainer = null;
	
	private int lookAroundTicks=0;
	
	public NPCguard(UUID identifier) {
		ident = identifier;
	}
	public UUID getIdentifier() {
		return ident;
	}
	
	public Location<World> getLoc() {
		return loc;
	}

	/** function to move existing shop for convenience as API */
	public void move(Location<World> newLoc) {
		Optional<Chunk> c = loc.getExtent().getChunkAtBlock(loc.getBiomePosition());
		if (!c.isPresent()) 
			throw new RuntimeException("Chunk for shop not available!");
		Chunk chunk = c.get();
		if (!chunk.isLoaded()) {
			if (!chunk.loadChunk(false)) 
				throw new RuntimeException("Unable to load chunk for shop to remove old entity");
		}
		chunk.getEntity(le.getUniqueId()).ifPresent(ent->{
			if (ent instanceof Living) ent.remove();
		});
		loc = newLoc;
	}
	public void setLoc(Location<World> loc) {
		this.loc = new Location<World>(loc.getExtent(), loc.getBlockX()+0.5, loc.getY(), loc.getBlockZ()+0.5);
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
	/** @param forPlayer translate items descriptions for this player if applicable */
	public Inventory getInventory(UUID forPlayer) {
		ShopInventoryListener sil = new ShopInventoryListener(this);
		Inventory.Builder builder = Inventory.builder().of(InventoryArchetypes.CHEST)
				.property("inventorytitle", new InventoryTitle(Text.of(TextColors.DARK_AQUA,
						VillagerShops.getTranslator().localText("shop.title")
							.replace("%name%", Text.of(TextColors.RESET, displayName==null?Text.of():displayName))
							.resolve(forPlayer).orElse(Text.of("[vShop] ", displayName==null?Text.of():displayName)))
					))
				.listener(ClickInventoryEvent.class, sil);
		Inventory inv = preparator.getInventory(builder, forPlayer);
		
		return inv;
	}
	
	/** recounts items in the shop container for playershops */
	public void updateStock() {
		getStockInventory().ifPresent(inv->preparator.updateStock(inv));
	}

	public void setPreparator(InvPrep preparator) {
		this.preparator = preparator;
	}

	public EntityType getNpcType() {
		return npcType;
	}

	public void setNpcType(EntityType npcType) {
		this.npcType = npcType;
	}

	public void setDisplayName(Text name) {
		displayName = name;
	}
	public Text getDisplayName() {
		return displayName;
	}
	
	/** MAY NOT BE CALLED BEFORE setNpcType() */
	public void setVariant(String fieldName) {
		try {
			if (fieldName.equalsIgnoreCase("none")) {
				variant = null;
			}if (npcType.equals(EntityTypes.HORSE)) {
				variant = FieldResolver.getFinalStaticAuto(HorseColor.class, fieldName);
			} else if (npcType.equals(EntityTypes.OCELOT)) {
				variant = FieldResolver.getFinalStaticAuto(OcelotType.class, fieldName);
			} else if (npcType.equals(EntityTypes.VILLAGER)) {
				variant = FieldResolver.getFinalStaticAuto(Profession.class, fieldName);
				if (variant == null) { //maybe a career was specified
					variant = FieldResolver.getFinalStaticAuto(Career.class, fieldName);
					if (variant != null) variant = ((Career)variant).getProfession(); ///professions give skins, so we won't bother the career any further 
				}
			} else if (npcType.equals(EntityTypes.LLAMA)) {
				variant = FieldResolver.getFinalStaticAuto(LlamaVariant.class, fieldName);
			} else if (npcType.equals(EntityTypes.RABBIT)) {
				variant = FieldResolver.getFinalStaticAuto(RabbitType.class, fieldName);
			} else if (npcType.equals(EntityTypes.PARROT)) {
				variant = FieldResolver.getFinalStaticAuto(ParrotVariant.class, fieldName);
			} else {
				variantName = "NONE";
				variant = null;
			}
		} catch (Exception e) { //ignore non existant fields due to lower version
			variantName = "NONE";
			variant = null;
		}
		if (variant != null)
			variantName = variant.getId();
		else 
			variantName = fieldName; //maybe valid later?
	}
	public Object getVariant() {
		return variant;
	}
	public String getVariantName() {
		return variantName;
	}
	
	public Entity getLe() {
		return le;
	}
	/** by setting a owner id this will try to turn into a player shop.<br>
	 * If there's no chest below the shop this will throw a IllegalStateException.<br>
	 * If the argument is null the playershop accociation is lifted */
	public void setPlayerShop(UUID owner) throws IllegalStateException {
		if (owner == null) {
			playershopholder = null;
			playershopcontainer = null;
			return;
		}
		Location<World> scan = getLoc().sub(0, 0.5, 0);
		if (!scan.getBlockType().equals(BlockTypes.CHEST)) 
			scan = scan.sub(0, 1, 0);
		if (!scan.getBlockType().equals(BlockTypes.CHEST))
			throw new IllegalStateException("Shop is not placed above a chest");
		
		playershopholder = owner;
		playershopcontainer = scan;
	}
	public boolean isShopOwner(UUID player) {
		return playershopholder==null?false:playershopholder.equals(player);
	}
	public Optional<Inventory> getStockInventory() {
		if (playershopholder==null) return Optional.empty();
		try {
			if (!playershopcontainer.getBlockType().equals(BlockTypes.CHEST))
				throw new RuntimeException("ContainerBlock not Chest");
			TileEntityCarrier chest = (TileEntityCarrier) playershopcontainer.getTileEntity().get();
			return Optional.of(chest.getInventory());
		} catch (Exception e) {
			VillagerShops.w("Could not receive container for Playershop at " + loc.getExtent().getName() + " " + loc.getBlockPosition());
			return Optional.empty();
		}
	}
	public Optional<Location<World>> getStockContainer() {
		return playershopholder==null?Optional.empty():Optional.of(playershopcontainer);
	}
	public Optional<UUID> getShopOwner() {
		return playershopholder==null?Optional.empty():Optional.of(playershopholder);
	}

	public void tick() {
		World w = loc.getExtent();
		Optional<Chunk> chunk = w.getChunkAtBlock(loc.getBlockPosition()); 
		if (le == null) {
			if (chunk.isPresent() && chunk.get().isLoaded()) {
				//first try to link the entity again:
				Collection<Entity> ents = chunk.get().getEntities();
				for (Entity ent : ents) {
					if (( ent.isLoaded() && ent.getType().equals(npcType) && (ent.getUniqueId().equals(lastKnown) || 
						( ent.getLocation().getExtent().equals(loc.getExtent()) && ent.getLocation().getPosition().distanceSquared(loc.getPosition())<1) ) ) && //can't check for bigger distances, as it will yank npcs from other shops with the same name
						( !VillagerShops.isNPCused(ent) &&
						  displayName.equals(ent.get(Keys.DISPLAY_NAME).orElse(null))))
					{	//check if npc already belongs to a different shop
							le = ent; le.setLocationAndRotation(loc, rot); break;
					}
				}
				if (le == null) { //unable to restore
//					Optional<Entity> ent = w.createEntity(npcType, loc.getPosition());
					Entity shop = w.createEntity(npcType, loc.getPosition());
//				    if (ent.isPresent()) {
//				        Entity shop = ent.get();
				        shop.offer(Keys.AI_ENABLED, false);
				        shop.offer(Keys.IS_SILENT, true);
				        
				        //setting variant. super consistent ;D
				        if (variant != null)
					        try {
						        if (npcType.equals(EntityTypes.HORSE)) {
						        	shop.tryOffer(Keys.HORSE_COLOR, (HorseColor)variant);	
		//							((Horse)shop).variant().set((HorseVariant)variant);
								} else if (npcType.equals(EntityTypes.OCELOT)) {
									shop.tryOffer(Keys.OCELOT_TYPE, (OcelotType)variant);
								} else if (npcType.equals(EntityTypes.VILLAGER)) {
									shop.tryOffer(Keys.CAREER, ((Profession)variant).getCareers().iterator().next()); //Since I think I have to offer a career, that would define trades, but we don't really care take the first available career for the profession
								} else if (npcType.equals(EntityTypes.LLAMA)) {
									shop.tryOffer(Keys.LLAMA_VARIANT, (LlamaVariant)variant);
								} else if (npcType.equals(EntityTypes.RABBIT)) {
									shop.tryOffer(Keys.RABBIT_TYPE, (RabbitType)variant);
								} else if (npcType.equals(EntityTypes.PARROT)) {
									shop.tryOffer(Keys.PARROT_VARIANT, (ParrotVariant)variant);
								}
					        } catch (Exception e) { System.err.println("Variant no longer suported! Did the EntityType change?"); }
				        
				        shop.offer(Keys.DISPLAY_NAME, displayName);
				        
				        if (w.spawnEntity(shop)) {
				        	lastKnown = shop.getUniqueId();
				        }
//				    }
				}
			}
		} else if (le != null) {
			if (!le.isLoaded() || !chunk.isPresent() || !chunk.get().isLoaded()) {
				le = null;	//allowing minecraft to free the resources
			} else  if (le.isRemoved() || le.get(Keys.HEALTH).orElse(1.0)<=0) { 
				le = null;
			} else {
				le.setLocationAndRotation(loc, rot);
				if (le instanceof Living) {
					if ((++lookAroundTicks>15 && VillagerShops.rng.nextInt(10) == 0) || lookAroundTicks>100) {
						Living mo = (Living)le;
						lookAroundTicks = 0;
						mo.setHeadRotation(new Vector3d(VillagerShops.rng.nextFloat()*30-14, rot.getY()+VillagerShops.rng.nextFloat()*60-30, 0.0));
					}
				}
			}
		}
	}
}
