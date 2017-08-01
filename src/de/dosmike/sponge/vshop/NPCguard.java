package de.dosmike.sponge.vshop;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.Career;
import org.spongepowered.api.data.type.Careers;
import org.spongepowered.api.data.type.HorseColor;
import org.spongepowered.api.data.type.HorseColors;
import org.spongepowered.api.data.type.OcelotType;
import org.spongepowered.api.data.type.OcelotTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.Villager;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.spawn.EntitySpawnCause;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3d;

public class NPCguard {
	Location<World> loc;
	Vector3d rot;
	UUID lastKnown;
	InvPrep preparator;
	Entity le = null;
	EntityType npcType = EntityTypes.VILLAGER;
	String variantName; Object variant;
	Text displayName;
	UUID ident; //for configs;
	
	int lookAroundTicks=0;
	
	public NPCguard(UUID identifier) {
		ident = identifier;
	}
	public UUID getIdentifier() {
		return ident;
	}
	
	public Location<World> getLoc() {
		return loc;
	}

	public void setLoc(Location<World> loc) {
		this.loc = new Location<World>(loc.getExtent(), loc.getBlockX()+0.5, loc.getBlockY(), loc.getBlockZ()+0.5);
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
		if (fieldName.equalsIgnoreCase("none")) {
			variant = null;
		}if (npcType.equals(EntityTypes.HORSE)) {
			variant = StringUtils.isNumeric(fieldName) ? FieldResolver.getFinalStaticByIndex(HorseColors.class, Integer.parseInt(fieldName)) : FieldResolver.getFinalStaticByName(HorseColors.class, fieldName);
		} else if (npcType.equals(EntityTypes.OCELOT)) {
			variant = StringUtils.isNumeric(fieldName) ? FieldResolver.getFinalStaticByIndex(OcelotTypes.class, Integer.parseInt(fieldName)) : FieldResolver.getFinalStaticByName(OcelotTypes.class, fieldName);
		} else if (npcType.equals(EntityTypes.VILLAGER)) {
			variant = StringUtils.isNumeric(fieldName) ? FieldResolver.getFinalStaticByIndex(Careers.class, Integer.parseInt(fieldName)) : FieldResolver.getFinalStaticByName(Careers.class, fieldName);
		} else {
			variantName = "NONE";
			variant = null;
		}
		variantName = fieldName;
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

	public void tick() {
		World w = loc.getExtent();
		Optional<Chunk> chunk = w.getChunkAtBlock(loc.getBlockPosition()); 
		if (le == null) {
			if (chunk.isPresent() && chunk.get().isLoaded()) {
				//first try to link the entity again:
				Collection<Entity> ents = chunk.get().getEntities();
				for (Entity ent : ents) {
					if (( ent.isLoaded() && ent.getType().equals(npcType) && (ent.getUniqueId().equals(lastKnown) || 
						( ent.getLocation().getExtent().equals(loc.getExtent()) && ent.getLocation().getPosition().distanceSquared(loc.getPosition())<1.5) ) ) &&
						( !VillagerShops.getInstance().isNPCused(ent) &&
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
				        
				        //setting variant. super consistent ;D
				        if (variant != null)
					        try {
						        if (npcType.equals(EntityTypes.HORSE)) {
						        	shop.tryOffer(Keys.HORSE_COLOR, (HorseColor)variant);	
		//							((Horse)shop).variant().set((HorseVariant)variant);
								} else if (npcType.equals(EntityTypes.OCELOT)) {
									shop.tryOffer(Keys.OCELOT_TYPE, (OcelotType)variant);
								} else if (npcType.equals(EntityTypes.VILLAGER)) {
									shop.tryOffer(Keys.CAREER, (Career)variant);
								}
					        } catch (Exception e) { System.err.println("Variant no longer suported! Did the EntityType change?"); }
				        
				        shop.offer(Keys.DISPLAY_NAME, displayName);
				        
				        if (w.spawnEntity(shop, Cause.source(EntitySpawnCause.builder().entity(shop).type(SpawnTypes.PLUGIN).build()).build()) ) {
				        	lastKnown = shop.getUniqueId();
				        }
//				    }
				}
			}
		} else if (le != null) {
			if (!le.isLoaded() || !chunk.isPresent() || !chunk.get().isLoaded()) {
				le = null;	//allowing minecraft to free the resources
			} else {
				if (le.isRemoved() || le.get(Keys.HEALTH).orElse(1.0)<=0) { 
					le = null;
				}else {
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
}
