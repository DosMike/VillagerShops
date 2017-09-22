package de.dosmike.sponge.vshop.webapi;

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
import valandur.webapi.WebAPI;
import valandur.webapi.api.cache.world.ICachedWorld;
import valandur.webapi.shadow.com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class CreatePacket {

	@JsonDeserialize
	private String name;
	public Text getName() {
		return TextSerializers.FORMATTING_CODE.deserialize(name);
	}
	
	@JsonDeserialize
	private String variation;
	public String getVariation() {
		return variation;
	}
	
	@JsonDeserialize
	private String world;
	@JsonDeserialize
	private Vector3d location;
	public Optional<Location<World>> getLocation() {
		Optional<ICachedWorld> res = WebAPI.getCacheService().getWorld(world);
		if (res.isPresent()) {
			Optional<?> w = res.get().getLive();
			if (w.isPresent()) {
				World at = (World)w.get();
				return Optional.of(at.getLocation(location));
			}
		}
		return Optional.empty();
	}
	
	@JsonDeserialize
	private Double rotation;
	public Double getRotation() {
		return rotation;
	}
	
	@JsonDeserialize
	private String type;
	public Optional<EntityType> getEntityType() {
		Collection<EntityType> types = Sponge.getRegistry().getAllOf(EntityType.class);
		return types.stream().filter(g -> g.getId().equalsIgnoreCase(type) || g.getName().equalsIgnoreCase(type)).findAny();
	}
	
	/** returns true on success */
	public NPCguard execute() {
		Optional<EntityType> type = getEntityType();
		Optional<Location<World>> location = getLocation();
		if (type.isPresent() && location.isPresent())
			return API.create(type.get(), variation, getName(), location.get(), rotation);
		else return null;
	}
}
