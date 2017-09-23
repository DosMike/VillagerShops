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
import valandur.webapi.shadow.com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class VShopPacket extends apiPacket {

	@JsonDeserialize
	private String shop=null;
	public UUID getShop() {
		return UUID.fromString(shop);
	}
	
	@JsonDeserialize
	private String name=null;
	public Optional<Text> getName() {
		return name==null
			?Optional.empty()
			:Optional.of(TextSerializers.FORMATTING_CODE.deserialize(name));
	}
	
	@JsonDeserialize
	private String type=null;
	public Optional<EntityType> getType() {
		if (type==null) return Optional.empty();
		EntityType et = (EntityType)FieldResolver.getFinalStaticAuto(EntityTypes.class, type);
		return et==null
			?Optional.empty()
			:Optional.of(et);
	}
	
	@JsonDeserialize
	private String variant=null;
	public Optional<String> getVariant() {
		return variant==null
				?Optional.empty()
				:Optional.of(variant);
	}
	
	@JsonDeserialize
	private OwnedLocationPacket playershop=null;
	public Optional<OwnedLocationPacket> getPlayershop() {
		return playershop==null
				?Optional.empty()
				:Optional.of(playershop);
	}
	
	@JsonDeserialize
	private Location<World> location=null;
	public Location<World> getLocation() {
		return location;
	}
	
	@JsonDeserialize
	private Double rotation=null;
	public Double getRotation() {
		return rotation;
	}
	
	@JsonDeserialize
	private List<StockItemPacket> items=null;
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
