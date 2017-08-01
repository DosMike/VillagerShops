package de.dosmike.sponge.vshop;

import java.io.File;
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
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3d;

public class CommandRegistra {
public static void register() {

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
					if (!(src instanceof Player)) { src.sendMessage(Text.of("Fek off!")); return CommandResult.success(); }
					Player player = (Player)src;
					
					String var = (String) args.getOne("Skin").orElse("none");
					String name = (String) args.getOne("Name").orElse("VillagerShop");
					Text displayName = TextSerializers.FORMATTING_CODE.deserialize(name);
					
					NPCguard npc = new NPCguard(UUID.randomUUID());
					InvPrep prep = new InvPrep();
					if ((npc.npcType = (EntityType) args.getOne("EntityType").orElse(null)) == null) {
						src.sendMessage(Text.of(TextColors.RED, "Invalid EntityType!"));
						return CommandResult.success();
					}
					npc.setVariant(var);
					if (!var.equalsIgnoreCase("none") && npc.getVariant() == null) {
						src.sendMessage(Text.of(TextColors.RED, "No such Style/Variant found for ", npc.npcType.getName(), "\nUse NONE or 0 to get the default Style/Variant"));
						return CommandResult.success();
					}
					npc.setDisplayName(displayName);
					npc.setPreparator(prep);
					npc.setLoc(player.getLocation());
					npc.setRot(new Vector3d(0, player.getHeadRotation().getY(), 0));
					VillagerShops.getInstance().npcs.add(npc);
					
					return CommandResult.success();
				}
			}).build());
	children.put(Arrays.asList("add"), CommandSpec.builder()
			.arguments(
					GenericArguments.onlyOne(GenericArguments.string(Text.of("BuyPrice"))),
					GenericArguments.optional(GenericArguments.string(Text.of("SellPrice")))
			) .executor(new CommandExecutor() {
				@Override
				public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
					if (!(src instanceof Player)) { src.sendMessage(Text.of("Player only!")); return CommandResult.success(); }
					Player player = (Player)src;
					
					Optional<Entity> ent = getEntityLookingAt(player, 5.0);
					Location<World> loc = ent.orElse(player).getLocation();
					
					VillagerShops vshop = VillagerShops.getInstance();
					Optional<NPCguard> npc = vshop.getNPCfromLocation(loc);
					if (!npc.isPresent()) {
						src.sendMessage(Text.of(TextColors.RED, "There is no shop at your location!"));
					} else {
						InvPrep prep = npc.get().getPreparator();
						
						Double buyFor, sellFor;
						String parse = args.getOne("BuyPrice").orElse("-").toString();
						try {
							buyFor = parse.equals("-")?null:Double.parseDouble(parse); 
						} catch (Exception e) {
							src.sendMessage(Text.of(TextColors.RED, "The BuyPrice was not '-' or a decimal"));
							return CommandResult.success();
						}
						parse = args.getOne("SellPrice").orElse("-").toString();
						try {
							sellFor = parse.equals("-")?null:Double.parseDouble(parse); 
						} catch (Exception e) {
							src.sendMessage(Text.of(TextColors.RED, "The SellPrice was not '-' or a decimal"));
							return CommandResult.success();
						}
						
						Optional<ItemStack> item = player.getItemInHand(HandTypes.MAIN_HAND);
						if (!item.isPresent() || item.get().getItem().equals(ItemTypes.AIR))
							item = player.getItemInHand(HandTypes.OFF_HAND);
						if (!item.isPresent() || item.get().getItem().equals(ItemTypes.AIR)) {
							src.sendMessage(Text.of(TextColors.RED, "Please hold the exact item you want to sell in your hand"));
							return CommandResult.success();
						}
						prep.addItem(new StockItem(item.get(), sellFor, buyFor));
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
					if (!(src instanceof Player)) { src.sendMessage(Text.of("Player only!")); return CommandResult.success(); }
					return CommandResult.success();
				}
			}).build());
	children.put(Arrays.asList("delete"), CommandSpec.builder()
			.arguments(
					GenericArguments.none()
			) .executor(new CommandExecutor() {
				@Override
				public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
					if (!(src instanceof Player)) { src.sendMessage(Text.of("Player only!")); return CommandResult.success(); }
					Player player = (Player)src;
					
					Optional<Entity> ent = getEntityLookingAt(player, 5.0);
					Location<World> loc = ent.orElse(player).getLocation();
					
					VillagerShops vshop = VillagerShops.getInstance();
					Optional<NPCguard> npc = vshop.getNPCfromLocation(loc);
					if (!npc.isPresent()) {
						src.sendMessage(Text.of(TextColors.RED, "There is no shop at your location!"));
					} else {
						vshop.terminateNPCs();
						File f = new File("config/vshop/"+npc.get().getIdentifier().toString()+".conf");
						if (f.exists() && f.isFile()) f.delete();
						vshop.npcs.remove(npc.get());
						vshop.startTimers();
						src.sendMessage(Text.of(TextColors.GREEN, "Shop deleted!"));
					}
					
					return CommandResult.success();
				}
			}).build());
	children.put(Arrays.asList("save"), CommandSpec.builder()
			.arguments(
					GenericArguments.none()
			) .executor(new CommandExecutor() {
				@Override
				public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
					VillagerShops.getInstance().saveConfigs();
					src.sendMessage(Text.of(TextColors.GREEN, "Shops saved!"));
					return CommandResult.success();
				}
			}).build());
	children.put(Arrays.asList("reload"), CommandSpec.builder()
			.arguments(
					GenericArguments.none()
			) .executor(new CommandExecutor() {
				@Override
				public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
					VillagerShops vshop = VillagerShops.getInstance();
					vshop.terminateNPCs();
					vshop.loadConfigs();
					vshop.startTimers();
					src.sendMessage(Text.of(TextColors.GREEN, "Reload complete!"));
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
			.description(Text.of("/vshop <CREATE|ADD|REMOVE|DELETE|SAVE|RELOAD> <options>"))
			.extendedDescription(Text.of(
					"ALWAYS STAND AT THE SHOP LOCATION! Options for:\n" + 
					"CREATE - <EntityType> <Skin> <Name> (Skin is NONE or any valid skin for this Mob)\n" + 
					"ADD - <-|BuyPrice> [-|SellPrice] (Hold ItemStack, - will disable that option, one required)\n" +
					"REMOVE - <INDEX> (Count through items in shop inventory, start with 0)\n" +
					"DELETE - (No arguments)\n" +
					"SAVE - (No arguments)\n" +
					"RELOAD - (No arguments)\n"
					))
			.permission("vshop.edit.admin")
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
		Vector3d src = source.getLocation().getPosition().add(0, 1.6, 0); //about head height
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
