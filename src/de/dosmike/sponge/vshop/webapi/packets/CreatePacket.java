package de.dosmike.sponge.vshop.webapi.packets;

import java.util.Collection;
import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3d;

import de.dosmike.sponge.vshop.API;
import de.dosmike.sponge.vshop.NPCguard;
import valandur.webapi.api.WebAPIAPI;
import valandur.webapi.api.cache.world.ICachedWorld;
import valandur.webapi.shadow.com.fasterxml.jackson.annotation.JsonIgnore;
import valandur.webapi.shadow.com.fasterxml.jackson.annotation.JsonProperty;

public class CreatePacket extends apiPacket {
	
	@JsonProperty
	private String name;
	@JsonIgnore
	public Text getName() {
		return TextSerializers.FORMATTING_CODE.deserialize(name);
	}
	
	@JsonProperty
	private String variation;
	@JsonIgnore
	public String getVariation() {
		return variation;
	}
	
	@JsonProperty
	private String world;
	@JsonProperty
	private Vector3d location;
	@JsonIgnore
	public Optional<Location<World>> getLocation() {
		Optional<ICachedWorld> res = WebAPIAPI.getCacheService().get().getWorld(world);
		if (res.isPresent()) {
			Optional<?> w = res.get().getLive();
			if (w.isPresent()) {
				World at = (World)w.get();
				return Optional.of(at.getLocation(location));
			}
		}
		return Optional.empty();
	}
	
	@JsonProperty
	private Double rotation;
	@JsonIgnore
	public Double getRotation() {
		return rotation;
	}
	
	@JsonProperty
	private String type;
	@JsonIgnore
	public Optional<EntityType> getEntityType() {
		Collection<EntityType> types = Sponge.getRegistry().getAllOf(EntityType.class);
		return types.stream().filter(g -> g.getId().equalsIgnoreCase(type) || g.getName().equalsIgnoreCase(type)).findAny();
	}
	
	public CreatePacket() {}
	
	/** returns true on success */
	@JsonIgnore
	public NPCguard execute() {
		Optional<EntityType> type = getEntityType();
		Optional<Location<World>> location = getLocation();
		if (type.isPresent() && location.isPresent())
			return API.create(type.get(), variation, getName(), location.get(), rotation);
		else return null;
	}
}
