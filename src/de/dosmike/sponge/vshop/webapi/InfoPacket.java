package de.dosmike.sponge.vshop.webapi;

import java.util.UUID;

import valandur.webapi.shadow.com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class InfoPacket {

	@JsonDeserialize
	private UUID shop;
	public UUID getShop() {
		return shop;
	}
	
}
