package de.dosmike.sponge.vshop;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

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
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
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
					GenericArguments.flags().valueFlag(
						GenericArguments.string(Text.of("position")), "-at"
					).valueFlag(
						GenericArguments.string(Text.of("Skin")), "-skin"
					).buildWith(GenericArguments.seq(
						GenericArguments.onlyOne(GenericArguments.catalogedElement(Text.of("EntityType"), EntityType.class)),
						GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("Name")))
					))
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
					boolean adminshop = player.hasPermission("vshop.edit.admin");
					
					if (!adminshop) {
						Optional<String> option = player.getOption("vshop.option.playershop.limit");
//						VillagerShops.l("limit:"+option.orElse("?"));
						int limit=-1;
						try { 
							limit = Integer.parseInt(option.orElse("-1")); 
						} catch (Exception e) {}
						if (limit>=0) {
							int cnt=0; UUID pid = player.getUniqueId(); for (NPCguard npc : VillagerShops.getNPCguards()) 
								if (npc.isShopOwner(pid)) cnt++;
							
							if (cnt >= limit) {
								player.sendMessage(Text.of(TextColors.RED, "[vShop] ", 
										lang.local("cmd.create.playershop.limit").replace("%limit%", limit).resolve(player).orElse("[limit reached]") ));
								return CommandResult.success();
							}
						}
					}
					
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
					if (!adminshop) {
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
					Location<World> createAt = player.getLocation();
					double rotateYaw = player.getHeadRotation().getY();
					if (args.hasAny("position")) try {
						String[] parts = args.<String>getOne("position").get().split("/");
						if (parts.length!=5) {
							throw new Exception();
						}
						Optional<World> w = Sponge.getServer().getWorld(parts[0]);
						if (!w.isPresent()) {
							player.sendMessage(Text.of(TextColors.RED, "[vShop] ", lang.local("cmd.create.invalidworld").resolve(player).orElse("[Invalid pos]")));
							return CommandResult.success();
						}
						createAt = w.get().getLocation(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
						rotateYaw = Double.parseDouble(parts[4]);
						while (rotateYaw>180) rotateYaw-=360;
						while (rotateYaw<=-180) rotateYaw+=360;
					} catch (Exception e) {
						player.sendMessage(Text.of(TextColors.RED, "[vShop] ", lang.local("cmd.create.invalidpos").resolve(player).orElse("[Invalid pos]")));
						return CommandResult.success();
					}
					
					npc.setDisplayName(displayName);
					npc.setPreparator(prep);
					npc.setLoc(createAt);
					npc.setRot(new Vector3d(0, rotateYaw, 0));
					boolean playershop=false;
					try {
						npc.setPlayerShop(player.getUniqueId());
						playershop=true;
					} catch (Exception e) {
						if (!adminshop) {
							player.sendMessage(Text.of(TextColors.RED, "[vShop] ", 
									lang.local("cmd.create.playershop.missingcontainer").resolve(player).orElse("[no chest below]")));
							return CommandResult.success();
						}
					}
					VillagerShops.addNPCguard(npc);
					
					src.sendMessage(Text.of(TextColors.GREEN, "[vShop] ", 
							lang.localText(playershop?"cmd.create.playershop.success":"cmd.create.success").replace("%name%", Text.of(TextColors.RESET, displayName)).resolve(player).orElse(Text.of("[success]")) ));
					
					return CommandResult.success();
				}
			}).build());
	children.put(Arrays.asList("add"), CommandSpec.builder()
			.arguments(GenericArguments.flags().valueFlag(
					GenericArguments.integer(Text.of("limit")), "l"
				).valueFlag(
					GenericArguments.integer(Text.of("slot")), "o"
				).buildWith(GenericArguments.seq(
					GenericArguments.onlyOne(GenericArguments.string(Text.of("BuyPrice"))),
					GenericArguments.onlyOne(GenericArguments.string(Text.of("SellPrice"))),
					GenericArguments.optional(GenericArguments.string(Text.of("Currency")))
				))
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

						int overwriteindex=-1;
						if (args.hasAny("slot")) {
							int testslot = args.<Integer>getOne("slot").get();
							if (testslot > prep.size() || testslot < 1) {
								player.sendMessage(Text.of(TextColors.RED,
										lang.local("cmd.add.overwrite.index").resolve(player).orElse("[invalid overwrite index]")));
							} else {
								overwriteindex = testslot-1;
							}
						}
						if (overwriteindex < 0 && prep.size()>=27) {
							player.sendMessage(Text.of(TextColors.RED,
									lang.local("cmd.add.itemlimit").resolve(player).orElse("[item limit reached]")));
							return CommandResult.success();
						}
						Double buyFor, sellFor;
						int limit=0;
						if (args.hasAny("limit")) {
							if (!npc.get().isShopOwner(player.getUniqueId())) {
								player.sendMessage(Text.of(TextColors.RED, 
										lang.local("cmd.add.limit.adminshop").resolve(player).orElse("[cant limit stockless]")));
								return CommandResult.success();
							} else {
								limit = args.<Integer>getOne("limit").orElse(0);
							}
						}
						
						String parse = args.getOne("BuyPrice").orElse("~").toString();
						try {
							buyFor = parse.equals("~")?null:Double.parseDouble(parse); 
						} catch (Exception e) {
							player.sendMessage(Text.of(TextColors.RED, "[vShop] ",
									lang.local("cmd.add.buyprice").resolve(player).orElse("[No buy price]")));
							return CommandResult.success();
						}
						parse = args.getOne("SellPrice").orElse("~").toString();
						try {
							sellFor = parse.equals("~")?null:Double.parseDouble(parse); 
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
						if (!item.isPresent() || item.get().isEmpty())
							item = player.getItemInHand(HandTypes.OFF_HAND);
						if (!item.isPresent() || item.get().isEmpty()) {
							player.sendMessage(Text.of(TextColors.RED, "[vShop] ",
									lang.local("cmd.add.itemisair").resolve(player).orElse("[Item is air]")));
							return CommandResult.success();
						}
						VillagerShops.closeShopInventories(npc.get().getIdentifier()); //so players are forced to update
						StockItem newItem = new StockItem(item.get(), sellFor, buyFor, VillagerShops.getInstance().CurrencyByName((String) args.getOne("Currency").orElse(null)), limit);
						if (overwriteindex < 0) {
							prep.addItem(newItem);
							
							player.sendMessage(Text.of(
									TextColors.GREEN, "[vShop] ",
									lang.localText("cmd.add.success")
									.replace("%item%", Text.of( TextColors.RESET, item.get().get(Keys.DISPLAY_NAME).orElse(Text.of(FieldResolver.getType(item.get()).getName())), TextColors.GREEN ))
									.replace("%pos%", prep.size())
									.resolve(player).orElse(Text.of("[item added]"))
									));
						} else {
							prep.setItem(overwriteindex, newItem);
							
							player.sendMessage(Text.of(
									TextColors.GREEN, "[vShop] ",
									lang.localText("cmd.add.replaced")
									.replace("%item%", Text.of( TextColors.RESET, item.get().get(Keys.DISPLAY_NAME).orElse(Text.of(FieldResolver.getType(item.get()).getName())), TextColors.GREEN ))
									.replace("%pos%", prep.size())
									.resolve(player).orElse(Text.of("[item replaced]"))
									));
						}
					}
					
					
					return CommandResult.success();
				}
			}).build());
	children.put(Arrays.asList("remove"), CommandSpec.builder()
			.arguments(
					GenericArguments.integer(Text.of("Index"))
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
						VillagerShops.stopTimers();
						VillagerShops.closeShopInventories(npc.get().getIdentifier());
						npc.get().getLe().remove();
						VillagerShops.removeNPCguard(npc.get());
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
//					src.sendMessage(Text.of("With the change to auto-save, this command became obsolete"));
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
//					VillagerShops.terminateNPCs();
//					VillagerShops.getInstance().loadConfigs();
//					VillagerShops.startTimers();
//					src.sendMessage(Text.of(TextColors.GREEN, "[vShop] ",
//							lang.local("cmd.reloaded").resolve(src).orElse("[reloaded]")));
					VillagerShops.getNPCguards().stream().filter(shop->shop.getNpcType().equals(EntityTypes.HUMAN)&&shop.getVariant() != null).forEach(shop->{
						if (shop.getVariant() instanceof UUID) {
							shop.getLe().remove(Keys.SKIN_UNIQUE_ID);
							shop.getLe().offer(Keys.SKIN_UNIQUE_ID, (UUID)shop.getVariant());
						}
					});
					src.sendMessage(Text.of("Reapplied skins to fake players"));
					return CommandResult.success();
				}
			}).build());
	children.put(Arrays.asList("list", "get", "for"), CommandSpec.builder()
			.permission("vshop.edit.admin")
			.arguments(
					GenericArguments.optional(
						GenericArguments.user(Text.of("User"))
					)
			).executor(new CommandExecutor() {
					private List<Text> pump(Collection<NPCguard> shops) {
						List<Text> pages = new ArrayList<>(shops.size()/16+1);
						Text.Builder page = Text.builder();
						int i=0;
						
						for (NPCguard shop : shops) {
							Optional<UUID> oid = shop.getShopOwner();
							Optional<User> owner = oid.isPresent()?VillagerShops.getUserStorage().get(oid.get()):Optional.empty();
							if (i>0) page.append(Text.NEW_LINE);
							page.append(entry(owner.orElse(null), shop));
							i++;
							if (i>=16) {
								pages.add(page.build());
								page = Text.builder();
								i=0;
							}
						}
						if (i>0) {
							pages.add(page.build());
							page = Text.builder();
							i=0;
						}
						return pages;
					}
					private Text entry(User user, NPCguard shop) {
						Text line = Text.builder().append(shop.getDisplayName())
								.onHover(TextActions.showText(Text.of(
									TextColors.WHITE, "Type: ", TextColors.GRAY, shop.getNpcType().getId(), Text.NEW_LINE,
									TextColors.WHITE, "Skin: ", TextColors.GRAY, shop.getVariantName(), Text.NEW_LINE,
									TextColors.WHITE, TextStyles.ITALIC, "Click to teleport"
								)))
								.onClick(TextActions.executeCallback(src->{
									if (src instanceof Player) {
										((Player) src).setLocation(shop.getLoc());
									}
								})).build();
						line = Text.of(line, TextStyles.RESET, TextColors.RESET, " by ", 
						(user == null ? Text.of(TextColors.DARK_RED, "admin") :
							Text.builder().append(Text.of(user.getName()))
								.onHover(TextActions.showText(Text.of(
									TextColors.WHITE, "UUID: ", TextColors.GRAY, user.getUniqueId(), Text.NEW_LINE
								))).build()
						) );
						
						if (shop.getStockContainer().isPresent()) {
							line = Text.of(line, Text.builder(" [Open Stock]")
									.onClick(TextActions.executeCallback(src->{
										if (src instanceof Player)
											shop.getStockInventory().ifPresent(inv->((Player)src).openInventory(inv));
									})).onHover(TextActions.showText(Text.of("Click to invsee")))
									.build() );
						}
						
						return line;
					}
					@Override
					public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
						Collection<NPCguard> filtered;
						if (args.hasAny("User")) {
							User target = (User)args.getOne("User").get();
							UUID searchID = target.getUniqueId();
							filtered = VillagerShops.getNPCguards().stream().filter(npc->npc.isShopOwner(searchID)).collect(Collectors.toList());
							
							PaginationList.builder()
								.title(Text.of("Shops owned by "+target.getName()))
								.contents(pump(filtered))
								.build()
								.sendTo(src);
						} else {
							PaginationList.builder()
							.title(Text.of("Villager Shops"))
							.contents(pump(VillagerShops.getNPCguards()))
							.build()
							.sendTo(src);
						}
						return CommandResult.success();
					}
			}).build());
	children.put(Arrays.asList("identify", "id"), CommandSpec.builder()
			.permission("vshop.edit.identify")
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
						Optional<UUID> owner = npc.get().getShopOwner();
						Optional<Player> powner = owner.isPresent() ? Sponge.getServer().getPlayer(owner.get()) : Optional.empty();
						String ownername = owner.isPresent() 
								? ( powner.isPresent() 
								  ? powner.get().getName()
								  : owner.get().toString() )
								: lang.local("cmd.identify.adminshop").resolve(player).orElse("[Server]");
						
						src.sendMessage(Text.of(TextColors.GREEN, "[vShop] ",
								lang.localText("cmd.identify.response")
								.replace("\\n", "\n")
								.replace("%type%", npc.get().getLe().getTranslation().get())
								.replace("%name%", npc.get().getDisplayName())
								.replace("%id%", 
										Text.builder(npc.get().getIdentifier().toString())
											.onShiftClick(TextActions.insertText(npc.get().getIdentifier().toString()))
											.onHover(TextActions.showText(lang.localText("cmd.identify.shiftclick").resolve(src).orElse(Text.of("Shift-click"))))
											.build())
								.replace("%owner%", ownername)
								.resolve(player).orElse(Text.of("[much data, such wow]"))));
					}
					
					return CommandResult.success();
				}
			}).build());
	children.put(Arrays.asList("link"), CommandSpec.builder()
			.permission("vshop.edit.linkchest")
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
					ChestLinkManager.toggleLinker(player, npc);
					
					return CommandResult.success();
				}
			}).build());
	children.put(Arrays.asList("tphere"), CommandSpec.builder()
			.permission("vshop.edit.move")
			.arguments(
					GenericArguments.uuid(Text.of("shopid"))
			) .executor(new CommandExecutor() {
				@Override
				public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
					if (!(src instanceof Player)) { src.sendMessage(lang.localText("cmd.playeronly").resolve(src).orElse(Text.of("[Player only]"))); return CommandResult.success(); }
					Player player = (Player)src;
					
					Optional<NPCguard> npc = VillagerShops.getNPCfromShopUUID(args.<UUID>getOne("shopid").get());
					if (!npc.isPresent()) {
						src.sendMessage(lang.localText("cmd.common.noshopforid").resolve(src).orElse(Text.of("[Shop not found]")));
					} else {
						if (!player.hasPermission("vshop.edit.admin") &&
								!npc.get().isShopOwner(player.getUniqueId())) {
							player.sendMessage(Text.of(TextColors.RED,
									lang.local("permission.missing").resolve(player).orElse("[permission missing]")));
							return CommandResult.success();
						}
						Optional<Integer> distance = Optional.empty();
						if (npc.get().getShopOwner().isPresent()) try {
							distance = getMaximumStockDistance(player);
						} catch (NumberFormatException nfe) {
							throw new CommandException(lang.localText("option.invalidvalue")
									.replace("%option%", "vshop.option.chestlink.distance")
									.replace("%player%", player.getName())
									.resolve(player)
									.orElse(Text.of("[option value invalid]"))
							);
						}
						NPCguard guard = npc.get();
						Location<World> to = player.getLocation();
						if (distance.isPresent() && guard.getStockContainer().isPresent() && (
								!to.getExtent().equals(guard.getStockContainer().get().getExtent()) ||
								to.getPosition().distance(guard.getStockContainer().get().getPosition()) > distance.get()))
							throw new CommandException(lang.localText("cmd.link.distance")
								.replace("%distance%", distance.get())
								.resolve(player)
								.orElse(Text.of("[too far away]")));

						VillagerShops.closeShopInventories(guard.getIdentifier());
						guard.move(new Location<World>(to.getExtent(), to.getBlockX() + 0.5, to.getY(), to.getBlockZ() + 0.5));
						guard.setRot(new Vector3d(0.0, player.getHeadRotation().getY(), 0.0));
					}
					return CommandResult.success();
				}
			}).build());
	children.put(Arrays.asList("ledger", "log"), CommandSpec.builder()
			.permission("vshop.ledger.base")
			.arguments(
					GenericArguments.flags().permissionFlag("vshop.ledger.others", "t")
					.buildWith(GenericArguments.optional(
						GenericArguments.user(Text.of("Target"))
					))
			) .executor(new CommandExecutor() {
				@Override
				public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
					if (args.hasAny("Target") && args.hasAny("t")) {
						throw new CommandException(lang.localText("cmd.ledger.invalid").resolve(src).orElse(Text.of("[Choose one argument]")));
//					} else if (args.hasAny("t")) {
//						if (!(src instanceof Player)) {
//							throw new CommandException(lang.localText("cmd.playeronly").resolve(src).orElse(Text.of("[Player only]")));
//						} else {
//							//TODO toggle caht spam
//						}
					} else if (!args.hasAny("Target") && !(src instanceof Player)) {
						throw new CommandException(lang.localText("cmd.missingargument").resolve(src).orElse(Text.of("[Missing argument]")));
					} else {
						User target;
						if (src instanceof Player)
							target = (User)args.getOne("Target").orElse((User)src);
						else if (args.hasAny("Target"))
							target = (User)args.getOne("Target").get();
						else throw new CommandException(Text.of("No target console, shouldn't fail"));
						src.sendMessage(Text.of("Searching Business Ledger, please wait.."));
						LedgerManager.openLedgerFor(src, target);
					}

					return CommandResult.success();
				}
			}).build());


	Sponge.getCommandManager().register(VillagerShops.getInstance(), CommandSpec.builder()
			.description(Text.of(lang.local("cmd.description.short").toString()))
			.extendedDescription(Text.of(lang.local("cmd.description.long").replace("\\n", "\n").toString()))
			.children(children)
			.build()
			, "vshop");
	}

	/** EntityRay :D
	 * Made this public so other plugins may use this for API purposes
	 * Don't ask too closely how this works, I wrote this years ago... */
	public static Optional<Entity> getEntityLookingAt(Living source, Double maxRange) {
		Collection<Entity> ents = source.getNearbyEntities(maxRange); // get all entities in interaction range
		//we need a facing vector for the source
		Vector3d rot = source.getHeadRotation().mul(Math.PI/180.0); //to radians
		Vector3d dir = new Vector3d(
				-Math.cos(rot.getX())*Math.sin(rot.getY()), 
				-Math.sin(rot.getX()), 
				Math.cos(rot.getX())*Math.cos(rot.getY())); //should now be a unit vector (len 1)
		
//		VillagerShops.l("%s\n%s", source.getHeadRotation().toString(), dir.toString());
		
		//Scanning for a target
		Double dist = 0.0;
		Vector3d src = source.getLocation().getPosition().add(0, 1.62, 0); //about head height
		dir = dir.normalize().div(10); //scan step in times per block
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
				curdist = Math.min(ep.add(0, 0.5, 0).distanceSquared(src), ep.add(0, 1.5, 0).distanceSquared(src)); //assuming the entity is a humanoid 
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

	/** throws number format exception if the option has a invalid (non positive integer) value */
	static Optional<Integer> getMaximumStockDistance(Player player) throws NumberFormatException {
		Optional<String> val = player.getOption("vshop.option.chestlink.distance");
		Optional<Integer> res = val
				.filter(s->s.matches("[1-9][0-9]*"))
				.map(Integer::valueOf);
		if (val.isPresent() && !res.isPresent())
			throw new NumberFormatException();
		return res;
	}
}
