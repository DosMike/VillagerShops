package de.dosmike.sponge.vshop.webapi.packets;

import org.spongepowered.api.text.serializer.TextSerializers;

import de.dosmike.sponge.vshop.NPCguard;
import valandur.webapi.shadow.com.fasterxml.jackson.annotation.JsonProperty;

public class SimpleVShopPacket extends apiPacket {
	@JsonProperty
	private String name;
	
	@JsonProperty
	private String type;
	@JsonProperty
	private String variant;
	
	@JsonProperty
	private String uuid;
	
	public SimpleVShopPacket() {
		uuid="";
		name="";
		type="";
		variant="";
	}
	public SimpleVShopPacket(NPCguard g) {
		uuid = g.getIdentifier().toString();
		name = TextSerializers.FORMATTING_CODE.serialize(g.getDisplayName());
		type = g.getNpcType().getName();
		variant = g.getVariantName();
	}
}
