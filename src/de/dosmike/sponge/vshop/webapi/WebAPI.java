package de.dosmike.sponge.vshop.webapi;

import java.util.UUID;

import org.spongepowered.api.text.serializer.TextSerializers;

import de.dosmike.sponge.vshop.API;
import de.dosmike.sponge.vshop.NPCguard;
import de.dosmike.sponge.vshop.VillagerShops;
import valandur.webapi.api.annotation.WebAPIEndpoint;
import valandur.webapi.api.annotation.WebAPIServlet;
import valandur.webapi.api.servlet.IServletData;
import valandur.webapi.api.servlet.WebAPIBaseServlet;
import valandur.webapi.shadow.javax.servlet.http.HttpServletResponse;
import valandur.webapi.shadow.org.eclipse.jetty.http.HttpMethod;

@WebAPIServlet(basePath = "vshop")
public class WebAPI extends WebAPIBaseServlet {
	
	@WebAPIEndpoint(method = HttpMethod.POST, path = "/create", perm = "vshop.webapi.edit")
    public void createShop(IServletData data) {
		CreatePacket query = data.getRequestBody(CreatePacket.class).orElse(null);
		if (query==null) {
            data.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid query: " + data.getLastParseError().getMessage());
            return;
		}
		
		if (!query.getEntityType().isPresent()) {
			data.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid type: " + data.getLastParseError().getMessage());
            return;
		}
		if (!query.getLocation().isPresent()) {
			data.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid location: " + data.getLastParseError().getMessage());
            return;
		}
		
		if (!query.execute()) {
			data.sendError(HttpServletResponse.SC_CONFLICT, "The API could not process your request");
            return;
		}
		data.addJson("ok", true, false);
	}
	
	@WebAPIEndpoint(method = HttpMethod.GET, path = "/list", perm = "vshop.webapi.edit")
    public void info(IServletData data) {
		data.addJson("shops", API.list(), true);
		
		data.addJson("ok", true, false);
	}
	@WebAPIEndpoint(method = HttpMethod.GET, path = "/info", perm = "vshop.webapi.edit")
    public void info2(IServletData data, UUID shopID) {
		NPCguard shop = VillagerShops.getNPCfromShopUUID(shopID).orElse(null);
		if (shop == null) {
            data.sendError(HttpServletResponse.SC_BAD_REQUEST, "No such shop: " + shop.toString());
            return;
		}
		
		data.addJson("displayName", TextSerializers.FORMATTING_CODE.serialize(shop.getDisplayName()), true);
		data.addJson("type", shop.getNpcType().toString(), true);
		data.addJson("variant", shop.getVariant().toString(), true);
		data.addJson("location", shop.getLoc().getExtent(), true);
		data.addJson("rotation", shop.getRot().getY(), true);
		data.addJson("owner", shop.getShopOwner().orElse(null), true);
		
		data.addJson("ok", true, false);
	}
	
}
