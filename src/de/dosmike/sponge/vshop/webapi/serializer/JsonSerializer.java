package de.dosmike.sponge.vshop.webapi.serializer;

import java.io.IOException;
import java.lang.reflect.Field;

import de.dosmike.sponge.vshop.webapi.packets.apiPacket;
import valandur.webapi.api.json.WebAPIBaseSerializer;
import valandur.webapi.shadow.com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/** since i only need a auto serialization, but that was kindly disabled i wrote a custom one */
public class JsonSerializer<T extends apiPacket> extends WebAPIBaseSerializer<T> {
	
	@Override
	protected void serialize(T object) throws IOException {
		writeStartObject();
		
		for (Field f : object.getClass().getDeclaredFields()) {
			if (f.isAnnotationPresent(JsonDeserialize.class)) {
				f.setAccessible(true);
				try {
					writeField(f.getName(), f.get(object));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		writeEndObject();
	}
	
}
