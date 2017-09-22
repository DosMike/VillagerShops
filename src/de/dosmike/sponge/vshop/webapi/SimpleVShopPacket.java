package de.dosmike.sponge.vshop.webapi;

import org.spongepowered.api.text.serializer.TextSerializers;

import de.dosmike.sponge.vshop.NPCguard;
import valandur.webapi.shadow.com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class SimpleVShopPacket {
	@JsonDeserialize
	private String name;
	
	@JsonDeserialize
	private String type;
	@JsonDeserialize
	private String variant;
	
	@JsonDeserialize
	private String uuid;
	
	public SimpleVShopPacket() {
		uuid="";
		name="";
		type="";
		variant="";
	};
	public SimpleVShopPacket(NPCguard g) {
		uuid = g.getIdentifier().toString();
		name = TextSerializers.FORMATTING_CODE.serialize(g.getDisplayName());
		type = g.getNpcType().getName();
		variant = g.getVariantName();
	}
}
