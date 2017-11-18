package de.dosmike.sponge.vshop.webapi.packets;

import java.util.UUID;

import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import valandur.webapi.shadow.com.fasterxml.jackson.annotation.JsonIgnore;
import valandur.webapi.shadow.com.fasterxml.jackson.annotation.JsonProperty;

public class OwnedLocationPacket extends apiPacket {
	
	@JsonProperty
	private String owner=null;
	@JsonIgnore
	public UUID getOwner() {
		return owner==null?null:UUID.fromString(owner);
	}
	
	@JsonProperty
	private Location<World> location=null;
	@JsonIgnore
	public Location<World> getLocation() {
		return location;
	}
	
	public OwnedLocationPacket() {}
	public OwnedLocationPacket(UUID owner, Location<World> location) {
		this.owner=owner!=null?owner.toString():null;
		this.location=location;
	}
	
}
