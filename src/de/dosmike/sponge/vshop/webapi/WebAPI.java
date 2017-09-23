package de.dosmike.sponge.vshop.webapi;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import de.dosmike.sponge.vshop.API;
import de.dosmike.sponge.vshop.NPCguard;
import de.dosmike.sponge.vshop.StockItem;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.webapi.packets.CreatePacket;
import de.dosmike.sponge.vshop.webapi.packets.OwnedLocationPacket;
import de.dosmike.sponge.vshop.webapi.packets.SimpleVShopPacket;
import de.dosmike.sponge.vshop.webapi.packets.StockItemPacket;
import de.dosmike.sponge.vshop.webapi.packets.VShopPacket;
import de.dosmike.sponge.vshop.webapi.serializer.CreatePacketSerializer;
import de.dosmike.sponge.vshop.webapi.serializer.OwnedLocationPacketSerializer;
import de.dosmike.sponge.vshop.webapi.serializer.SimpleVShopPacketSerializer;
import de.dosmike.sponge.vshop.webapi.serializer.StockItemPacketSerializer;
import de.dosmike.sponge.vshop.webapi.serializer.VShopPacketSerializer;
import valandur.webapi.api.WebAPIAPI;
import valandur.webapi.api.annotation.WebAPIEndpoint;
import valandur.webapi.api.annotation.WebAPIServlet;
import valandur.webapi.api.servlet.IServletData;
import valandur.webapi.api.servlet.IServletService;
import valandur.webapi.api.servlet.WebAPIBaseServlet;
import valandur.webapi.shadow.javax.servlet.http.HttpServletResponse;
import valandur.webapi.shadow.org.eclipse.jetty.http.HttpMethod;

@WebAPIServlet(basePath = "vshop")
public class WebAPI extends WebAPIBaseServlet {
	
	public static void init() {
		Optional<IServletService> optSrv = WebAPIAPI.getServletService();
	    if (optSrv.isPresent()) {
	        IServletService srv = optSrv.get();
	        srv.registerServlet(WebAPI.class);
	    }
	    valandur.webapi.WebAPI.getJsonService().registerSerializer(CreatePacket.class, CreatePacketSerializer.class);
	    valandur.webapi.WebAPI.getJsonService().registerSerializer(OwnedLocationPacket.class, OwnedLocationPacketSerializer.class);
	    valandur.webapi.WebAPI.getJsonService().registerSerializer(StockItemPacket.class, StockItemPacketSerializer.class);
	    valandur.webapi.WebAPI.getJsonService().registerSerializer(SimpleVShopPacket.class, SimpleVShopPacketSerializer.class);
	    valandur.webapi.WebAPI.getJsonService().registerSerializer(VShopPacket.class, VShopPacketSerializer.class);
	}
	
	@WebAPIEndpoint(method = HttpMethod.POST, path = "", perm = "vshop.webapi.edit")
    public void create(IServletData data) {
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
		
		NPCguard npc = query.execute();
		if (npc==null) {
			data.sendError(HttpServletResponse.SC_CONFLICT, "The API could not process your request");
            return;
		}
		data.addJson("uuid", npc.getIdentifier(), true);
		data.addJson("ok", true, false);
	}
	
	@WebAPIEndpoint(method = HttpMethod.GET, path = "", perm = "vshop.webapi.edit")
    public void info(IServletData data) {
		Set<SimpleVShopPacket> list = new HashSet<>();
		API.list().forEachRemaining(npc->{
			list.add(new SimpleVShopPacket(npc));
		});
		data.addJson("shops", list, true);
		
		data.addJson("ok", true, false);
	}
	
	
	@WebAPIEndpoint(method = HttpMethod.DELETE, path = "/:uuid", perm = "vshop.webapi.edit")
    public void deleteShop(IServletData data, UUID shopID) {
		NPCguard shop = VillagerShops.getNPCfromShopUUID(shopID).orElse(null);
		if (shop == null) {
            data.sendError(HttpServletResponse.SC_BAD_REQUEST, "No such shop: " + shopID.toString());
            return;
		}
		API.delete(shop);
		
		data.addJson("ok", true, false);
	}
	
	@WebAPIEndpoint(method = HttpMethod.GET, path = "/:uuid", perm = "vshop.webapi.edit")
    public void infoShop(IServletData data, UUID shopID) {
		NPCguard shop = VillagerShops.getNPCfromShopUUID(shopID).orElse(null);
		if (shop == null) {
            data.sendError(HttpServletResponse.SC_BAD_REQUEST, "No such shop: " + shopID.toString());
            return;
		}
		data.addJson("shop", new VShopPacket(shop), true);
		
		data.addJson("ok", true, false);
	}
	
	@WebAPIEndpoint(method = HttpMethod.PUT, path = "/:uuid", perm = "vshop.webapi.edit")
    public void updateShop(IServletData data, UUID shopID) {
		NPCguard shop = VillagerShops.getNPCfromShopUUID(shopID).orElse(null);
		if (shop == null) {
            data.sendError(HttpServletResponse.SC_BAD_REQUEST, "No such shop: " + shopID.toString());
            return;
		}
		Optional<VShopPacket> query = data.getRequestBody(VShopPacket.class);
		if (!query.isPresent()) {
            data.sendError(HttpServletResponse.SC_BAD_REQUEST, "No update data for: " + shopID.toString());
            return;
		}
		query.get().execute(shop);
		
		data.addJson("ok", true, false);
	}
	
	@WebAPIEndpoint(method = HttpMethod.DELETE, path = "/:uuid/:item", perm = "vshop.webapi.edit")
    public void deleteItem(IServletData data, UUID shopID, Integer item) {
		NPCguard shop = VillagerShops.getNPCfromShopUUID(shopID).orElse(null);
		if (shop == null) {
            data.sendError(HttpServletResponse.SC_BAD_REQUEST, "No such shop: " + shopID.toString());
            return;
		}
		if (item == null || item < 0 || shop.getPreparator().size() >= shop.getPreparator().size()) {
            data.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid item ID " + item + "  on " + shopID.toString());
            return;
		}
		VillagerShops.closeShopInventories(shopID); //important to update inventories
		shop.getPreparator().removeIndex(item);
		
		data.addJson("ok", true, false);
	}
	
	@WebAPIEndpoint(method = HttpMethod.GET, path = "/:uuid/:item", perm = "vshop.webapi.edit")
    public void infoItem(IServletData data, UUID shopID, Integer item) {
		NPCguard shop = VillagerShops.getNPCfromShopUUID(shopID).orElse(null);
		if (shop == null) {
            data.sendError(HttpServletResponse.SC_BAD_REQUEST, "No such shop: " + shopID.toString());
            return;
		}
		if (item == null || item < 0 || shop.getPreparator().size() >= shop.getPreparator().size()) {
            data.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid item ID " + item + "  on " + shopID.toString());
            return;
		}
		data.addJson("item", new StockItemPacket(shop.getPreparator().getItem(item)), true);
		
		data.addJson("ok", true, false);
	}
	
	/** will auto insert item */
	@WebAPIEndpoint(method = HttpMethod.PUT, path = "/:uuid/:item", perm = "vshop.webapi.edit")
    public void updateItem(IServletData data, UUID shopID, Integer item) {
		NPCguard shop = VillagerShops.getNPCfromShopUUID(shopID).orElse(null);
		if (shop == null) {
            data.sendError(HttpServletResponse.SC_BAD_REQUEST, "No such shop: " + shopID.toString());
            return;
		}
		if (item == null || item < 0 || item > shop.getPreparator().size()) {
            data.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid item ID " + item + "  on " + shopID.toString());
            return;
		}
		Optional<StockItemPacket> query = data.getRequestBody(StockItemPacket.class);
		if (!query.isPresent()) {
            data.sendError(HttpServletResponse.SC_BAD_REQUEST, "No update data for: " + shopID.toString());
            return;
		}
		StockItem blep = query.get().execute();
		if (!query.isPresent()) {
            data.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid data");
            return;
		}
		VillagerShops.closeShopInventories(shopID);
		if (item == shop.getPreparator().size())
			shop.getPreparator().addItem(blep);
		else
			shop.getPreparator().setItem(item, blep);
		
		data.addJson("ok", true, false);
	}
}
