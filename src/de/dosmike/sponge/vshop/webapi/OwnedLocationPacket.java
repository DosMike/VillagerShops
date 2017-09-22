package de.dosmike.sponge.vshop.webapi;

import java.util.UUID;

import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import valandur.webapi.shadow.com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class OwnedLocationPacket {
	
	@JsonDeserialize
	private String owner=null;
	public UUID getOwner() {
		return owner==null?null:UUID.fromString(owner);
	}
	
	@JsonDeserialize
	private Location<World> location=null;
	public Location<World> getLocation() {
		return location;
	}
	
	public OwnedLocationPacket(UUID owner, Location<World> location) {
		this.owner=owner!=null?owner.toString():null;
		this.location=location;
	}
	
}
