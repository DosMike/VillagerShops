package de.dosmike.sponge.vshop.webapi.packets;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3d;

import de.dosmike.sponge.vshop.API;
import de.dosmike.sponge.vshop.FieldResolver;
import de.dosmike.sponge.vshop.NPCguard;
import valandur.webapi.shadow.com.fasterxml.jackson.annotation.JsonIgnore;
import valandur.webapi.shadow.com.fasterxml.jackson.annotation.JsonProperty;
import valandur.webapi.shadow.com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class VShopPacket extends apiPacket {

	@JsonProperty
	private String shop=null;
	@JsonIgnore
	public UUID getShop() {
		return UUID.fromString(shop);
	}
	
	@JsonProperty
	private String name=null;
	@JsonIgnore
	public Optional<Text> getName() {
		return name==null
			?Optional.empty()
			:Optional.of(TextSerializers.FORMATTING_CODE.deserialize(name));
	}
	
	@JsonProperty
	private String type=null;
	@JsonIgnore
	public Optional<EntityType> getType() {
		if (type==null) return Optional.empty();
		EntityType et = (EntityType)FieldResolver.getFinalStaticAuto(EntityTypes.class, type);
		return et==null
			?Optional.empty()
			:Optional.of(et);
	}
	
	@JsonProperty
	private String variant=null;
	@JsonIgnore
	public Optional<String> getVariant() {
		return variant==null
				?Optional.empty()
				:Optional.of(variant);
	}
	
	@JsonDeserialize
	private OwnedLocationPacket playershop=null;
	@JsonIgnore
	public Optional<OwnedLocationPacket> getPlayershop() {
		return playershop==null
				?Optional.empty()
				:Optional.of(playershop);
	}
	
	@JsonProperty
	private Location<World> location=null;
	@JsonIgnore
	public Location<World> getLocation() {
		return location;
	}
	
	@JsonProperty
	private Double rotation=null;
	@JsonIgnore
	public Double getRotation() {
		return rotation;
	}
	
	@JsonProperty
	private List<StockItemPacket> items=null;
	@JsonIgnore
	public List<StockItemPacket> getItems() {
		return items;
	}
	
	public VShopPacket() {}
	public VShopPacket(NPCguard buildWith) {
		shop = buildWith.getIdentifier().toString();
		name = TextSerializers.FORMATTING_CODE.serialize(buildWith.getDisplayName());
		type = buildWith.getNpcType().getName();
		variant = buildWith.getVariantName();
		playershop = new OwnedLocationPacket(
				buildWith.getShopOwner().orElse(null),
				buildWith.getStockContainer().orElse(null));
		location = buildWith.getLoc();
		rotation = buildWith.getRot().getY();
		int m = buildWith.getPreparator().size();
		if (m>0) { items = new LinkedList<>(); for (int i = 0; i < m; i++) {
			items.add(new StockItemPacket(buildWith.getPreparator().getItem(i)));
		}}
	}
	
	@JsonIgnore
	public void execute(NPCguard target) {
		API.disintegrate(target);
		if (name!=null) target.setDisplayName(getName().get());
		if (type!=null) target.setNpcType(getType().get());
		if (variant!=null) target.setVariant(variant);
		if (playershop!=null) API.playershop(target, playershop.getOwner(), playershop.getLocation());
		if (location!=null) target.setLoc(location);
		if (rotation!=null) target.setRot(new Vector3d(0.0, rotation, 0.0));
	}
}
