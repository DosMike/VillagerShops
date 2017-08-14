package de.dosmike.sponge.vshop;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3d;

import de.dosmike.sponge.languageservice.API.PluginTranslation;

public class CommandRegistra {
	static PluginTranslation lang;
static void register() {
	lang = VillagerShops.getTranslator();
	Map<List<String>, CommandSpec> children;
	
	children = new HashMap<>();
	children.put(Arrays.asList("create"), CommandSpec.builder()
			.arguments(
					GenericArguments.onlyOne(GenericArguments.catalogedElement(Text.of("EntityType"), EntityType.class)),
					GenericArguments.onlyOne(GenericArguments.string(Text.of("Skin"))),
					GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("Name")))
			) .executor(new CommandExecutor() {
				@Override
				public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
					
					if (!(src instanceof Player)) { src.sendMessage(lang.localText("cmd.playeronly").resolve(src).orElse(Text.of("[Player only]"))); return CommandResult.success(); }
					Player player = (Player)src;
					
					if (!player.hasPermission("vshop.edit.admin") &&
							!player.hasPermission("vshop.edit.player")) {
						player.sendMessage(Text.of(TextColors.RED,
								lang.local("permission.missing").resolve(player).orElse("[permission missing]")));
						
						return CommandResult.success();
					}
					boolean playershop = !player.hasPermission("vshop.edit.admin"); 
					
					String var = (String) args.getOne("Skin").orElse("none");
					String name = (String) args.getOne("Name").orElse("VillagerShop");
					Text displayName = TextSerializers.FORMATTING_CODE.deserialize(name);
					
					NPCguard npc = new NPCguard(UUID.randomUUID());
					InvPrep prep = new InvPrep();
					npc.setNpcType((EntityType) args.getOne("EntityType").orElse(null));
					if (npc.getNpcType() == null) {
						player.sendMessage(Text.of(TextColors.RED, "[vShop] ", 
								lang.local("cmd.create.invalidtype").resolve(player).orElse("[invalid type]")));
						return CommandResult.success();
					}
					if (playershop) {
						String entityPermission = npc.getNpcType().getId();
						entityPermission = "vshop.create."+entityPermission.replace(':', '.').replace("_", "").replace("-", "");
						if (!player.hasPermission(entityPermission)) {
							player.sendMessage(Text.of(TextColors.RED, "[vShop] ", 
									lang.local("cmd.create.entitypermission").replace("%permission%", entityPermission).resolve(player).orElse("[entity permission missing]") ));
							return CommandResult.success();
						}
					}
					npc.setVariant(var);
					if (!var.equalsIgnoreCase("none") && npc.getVariant() == null) {
						player.sendMessage(Text.of(TextColors.RED, "[vShop] ", 
								lang.local("cmd.create.invalidvariant").replace("%variant%", npc.getNpcType().getName()).resolve(player).orElse("[invalid variant]")));
						return CommandResult.success();
					}
					npc.setDisplayName(displayName);
					npc.setPreparator(prep);
					npc.setLoc(player.getLocation());
					npc.setRot(new Vector3d(0, player.getHeadRotation().getY(), 0));
					try {
						npc.setPlayerShop(player.getUniqueId());
					} catch (Exception e) {
						if (playershop) {
							player.sendMessage(Text.of(TextColors.RED, "[vShop] ", 
									lang.local("cmd.create.playershop.missingcontainer").resolve(player).orElse("[no chest below]")));
							return CommandResult.success();
						}
					}
					VillagerShops.npcs.add(npc);
					
					src.sendMessage(Text.of(TextColors.GREEN, "[vShop] ", 
							lang.localText("cmd.create.success").replace("%name%", Text.of(TextColors.RESET, displayName)).resolve(player).orElse(Text.of("[success]")) ));
					
					return CommandResult.success();
				}
			}).build());
	children.put(Arrays.asList("add"), CommandSpec.builder()
			.arguments(
					GenericArguments.onlyOne(GenericArguments.string(Text.of("BuyPrice"))),
					GenericArguments.onlyOne(GenericArguments.string(Text.of("SellPrice"))),
					GenericArguments.optional(GenericArguments.string(Text.of("Currency")))
			) .executor(new CommandExecutor() {
				@Override
				public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
					if (!(src instanceof Player)) { src.sendMessage(lang.localText("cmd.playeronly").resolve(src).orElse(Text.of("[Player only]"))); return CommandResult.success(); }
					Player player = (Player)src;
					
					Optional<Entity> ent = getEntityLookingAt(player, 5.0);
					Location<World> loc = ent.orElse(player).getLocation();
					
					Optional<NPCguard> npc = VillagerShops.getNPCfromLocation(loc);
					if (!npc.isPresent()) {
						src.sendMessage(Text.of(TextColors.RED, "[vShop] ",
								lang.local("cmd.common.notarget").resolve(player).orElse("[no target]")));
					} else {
						if (!player.hasPermission("vshop.edit.admin") &&
								!npc.get().isShopOwner(player.getUniqueId())) {
							player.sendMessage(Text.of(TextColors.RED, 
									lang.local("permission.missing").resolve(player).orElse("[permission missing]")));
							return CommandResult.success();
						}
						
						InvPrep prep = npc.get().getPreparator();
						
						Double buyFor, sellFor;
						String parse = args.getOne("BuyPrice").orElse("-").toString();
						try {
							buyFor = parse.equals("-")?null:Double.parseDouble(parse); 
						} catch (Exception e) {
							player.sendMessage(Text.of(TextColors.RED, "[vShop] ",
									lang.local("cmd.add.buyprice").resolve(player).orElse("[No buy price]")));
							return CommandResult.success();
						}
						parse = args.getOne("SellPrice").orElse("-").toString();
						try {
							sellFor = parse.equals("-")?null:Double.parseDouble(parse); 
						} catch (Exception e) {
							player.sendMessage(Text.of(TextColors.RED, "[vShop] ",
									lang.local("cmd.add.sellprice").resolve(player).orElse("[No sell price]")));
							return CommandResult.success();
						}
						
						if (buyFor == null && sellFor == null) {
							player.sendMessage(Text.of(TextColors.RED, "[vShop] ",
									lang.local("cmd.add.noprice").resolve(player).orElse("[No price]")));
							return CommandResult.success();
						}
						if ((buyFor != null && buyFor < 0) || (sellFor != null && sellFor < 0)) {
							player.sendMessage(Text.of(TextColors.RED, "[vShop] ",
									lang.local("cmd.add.negativeprice").resolve(player).orElse("[Negative price]")));
							return CommandResult.success();
						}
						
						Optional<ItemStack> item = player.getItemInHand(HandTypes.MAIN_HAND);
						if (!item.isPresent() || FieldResolver.getType(item.get()).equals(FieldResolver.emptyHandItem()))
							item = player.getItemInHand(HandTypes.OFF_HAND);
						if (!item.isPresent() || FieldResolver.getType(item.get()).equals(FieldResolver.emptyHandItem())) {
							player.sendMessage(Text.of(TextColors.RED, "[vShop] ",
									lang.local("cmd.add.itemisair").resolve(player).orElse("[Item is air]")));
							return CommandResult.success();
						}
						VillagerShops.closeShopInventories(npc.get().getIdentifier()); //so players are forced to update
						prep.addItem(new StockItem(item.get(), sellFor, buyFor, VillagerShops.getInstance().CurrencyByName((String) args.getOne("Currency").orElse(null))));

						player.sendMessage(Text.of(
								TextColors.GREEN, "[vShop] ",
									lang.localText("cmd.add.success")
									.replace("%item%", Text.of( TextColors.RESET, item.get().get(Keys.DISPLAY_NAME).orElse(Text.of(FieldResolver.getType(item.get()).getTranslation().get())), TextColors.GREEN ))
									.replace("%pos%", prep.size())
									.resolve(player).orElse(Text.of("[item added]"))
								));
					}
					
					
					return CommandResult.success();
				}
			}).build());
	children.put(Arrays.asList("remove"), CommandSpec.builder()
			.arguments(
					GenericArguments.optional(GenericArguments.integer(Text.of("Index")))
			) .executor(new CommandExecutor() {
				@Override
				public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
					if (!(src instanceof Player)) { src.sendMessage(lang.localText("cmd.playeronly").resolve(src).orElse(Text.of("[Player only]"))); return CommandResult.success(); }
					Player player = (Player)src;
					
					Optional<Entity> ent = getEntityLookingAt(player, 5.0);
					Location<World> loc = ent.orElse(player).getLocation();
					
					Optional<NPCguard> npc = VillagerShops.getNPCfromLocation(loc);
					if (!npc.isPresent()) {
						player.sendMessage(Text.of(TextColors.RED, "[vShop] ",
								lang.local("cmd.common.notarget").resolve(player).orElse("[no target]")));
					} else {
						if (!player.hasPermission("vshop.edit.admin") &&
								!npc.get().isShopOwner(player.getUniqueId())) {
							player.sendMessage(Text.of(TextColors.RED, 
									lang.local("permission.missing").resolve(player).orElse("[permission missing]")));
							return CommandResult.success();
						}
						Integer index = (Integer) args.getOne("Index").get();
						if (index < 1 || index > npc.get().getPreparator().size()) {
							player.sendMessage(Text.of(TextColors.RED, "[vShop] ", 
									lang.local("cmd.remove.invalidindex").resolve(player).orElse("[invalid index]")));
						} else {
							VillagerShops.closeShopInventories(npc.get().getIdentifier()); //so players are forced to update
							npc.get().getPreparator().removeIndex(index-1);
							
							player.sendMessage(Text.of(TextColors.GREEN, "[vShop] ",
									lang.local("cmd.remove.success")
									.replace("%pos%", index)
									.resolve(player).orElse("[success]")));
						}
					}
					
					return CommandResult.success();
				}
			}).build());
	children.put(Arrays.asList("delete"), CommandSpec.builder()
			//TODO check permissions
			.arguments(
					GenericArguments.none()
			) .executor(new CommandExecutor() {
				@Override
				public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
					if (!(src instanceof Player)) { src.sendMessage(lang.localText("cmd.playeronly").resolve(src).orElse(Text.of("[Player only]"))); return CommandResult.success(); }
					Player player = (Player)src;
					
					Optional<Entity> ent = getEntityLookingAt(player, 5.0);
					Location<World> loc = ent.orElse(player).getLocation();
					
					Optional<NPCguard> npc = VillagerShops.getNPCfromLocation(loc);
					if (!npc.isPresent()) {
						src.sendMessage(Text.of(TextColors.RED, "[vShop] ",
								lang.local("cmd.common.notarget").resolve(player).orElse("[no target]")));
					} else {
						if (!player.hasPermission("vshop.edit.admin") &&
								!npc.get().isShopOwner(player.getUniqueId())) {
							player.sendMessage(Text.of(TextColors.RED, 
									lang.local("permission.missing").resolve(player).orElse("[permission missing]")));
							return CommandResult.success();
						}
						VillagerShops.terminateNPCs();
						VillagerShops.npcs.remove(npc.get());
						VillagerShops.startTimers();
						src.sendMessage(Text.of(TextColors.GREEN, "[vShop] ",
								lang.local("cmd.deleted").resolve(player).orElse("[deleted]")));
					}
					
					return CommandResult.success();
				}
			}).build());
	children.put(Arrays.asList("save"), CommandSpec.builder()
			.permission("vshop.edit.admin")
			.arguments(
					GenericArguments.none()
			) .executor(new CommandExecutor() {
				@Override
				public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
					VillagerShops.getInstance().saveConfigs();
					src.sendMessage(Text.of(TextColors.GREEN, "[vShop] ",
							lang.local("cmd.saved").resolve(src).orElse("[saved]")));
					return CommandResult.success();
				}
			}).build());
	children.put(Arrays.asList("reload"), CommandSpec.builder()
			.permission("vshop.edit.admin")
			.arguments(
					GenericArguments.none()
			) .executor(new CommandExecutor() {
				@Override
				public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
					VillagerShops.terminateNPCs();
					VillagerShops.getInstance().loadConfigs();
					VillagerShops.startTimers();
					src.sendMessage(Text.of(TextColors.GREEN, "[vShop] ",
							lang.local("cmd.reloaded").resolve(src).orElse("[reloaded]")));
					return CommandResult.success();
				}
			}).build());
	/*children.put(Arrays.asList("test"), CommandSpec.builder()
			.arguments(
					GenericArguments.none()
			) .executor(new CommandExecutor() {
				@Override
				public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
					VillagerShops vshop = VillagerShops.getInstance();
					for (Player player : Sponge.getServer().getOnlinePlayers()) {
						//player.closeInventory(Cause.builder().named("PLUGIN", VillagerShops.getInstance()).build());
						player.openInventory(player.getInventory(), Cause.builder().named("PLUGIN", VillagerShops.getInstance()).build());
					}
					src.sendMessage(Text.of(TextColors.GREEN, "Done!"));
					return CommandResult.success();
				}
			}).build());*/


	Sponge.getCommandManager().register(VillagerShops.getInstance(), CommandSpec.builder()
			.description(Text.of(lang.local("cmd.description.short").toString()))
			.extendedDescription(Text.of(lang.local("cmd.description.long").replace("\\n", "\n").toString()))
			.children(children)
			.build()
			, "vshop");
}

	/** EntityRay :D */
	private static Optional<Entity> getEntityLookingAt(Living source, Double maxRange) {
		Collection<Entity> ents = source.getNearbyEntities(maxRange); // get all entities in interaction range
		//we need a facing vector for the source
		Vector3d rot = source.getHeadRotation().mul(Math.PI/180.0); //to radians
		Vector3d dir = new Vector3d(-Math.cos(rot.getX())*Math.sin(rot.getY()), -Math.sin(rot.getX()), Math.cos(rot.getX())*Math.cos(rot.getY())); //should now be a unit vector (len 1)
		
		//Scanning for a target
		Double dist = 0.0;
		Vector3d src = source.getLocation().getPosition().add(0, 1.62, 0); //about head height
		dir = dir.normalize().div(10); //scan step in times block
		Double curdist;
		List<Entity> marked;
		Map<Entity, Double> lastDist = new HashMap<Entity, Double>();
		ents.remove(source); //do not return the source ;D
		Vector3d ep; //entity pos
		while (dist < maxRange) {
			if (ents.isEmpty()) break;
//			VillagerShops.l("Scanning %.2f/%.2f @ %.2f %.2f %.2f", dist, maxRange, src.getX(), src.getY(), src.getZ());
			marked = new LinkedList<Entity>();
			for (Entity ent : ents) { 
				ep = ent.getLocation().getPosition();
				curdist = Math.min(ep.add(0, 0.5, 0).distanceSquared(src), ep.add(0, 1.5, 0).distanceSquared(src));
//				VillagerShops.l("Distance to %s{%s}: %.2f", ent.getType().getName(), ent.getUniqueId().toString(), curdist);
				if (lastDist.containsKey(ent) && lastDist.get(ent)-curdist<0) marked.add(ent); // entity is moving away from ray pos
				else lastDist.put(ent, curdist);
				
				if (curdist < 1.44) { //assume height of ~2 
					return Optional.of(ent);
				}
			}
			for (Entity ent : marked) {
//				VillagerShops.l("Dropping %s{%s}", ent.getType().getName(), ent.getUniqueId().toString());
				lastDist.remove(ent); ents.remove(ent); }
			src = src.add(dir);
			dist += 0.1;
		}
		return Optional.empty();
	}
}
