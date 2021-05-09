package de.dosmike.sponge.vshop.commands;

import de.dosmike.sponge.vshop.PermissionRegistra;
import de.dosmike.sponge.vshop.Utilities;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.menus.ShopMenuManager;
import de.dosmike.sponge.vshop.shops.ShopEntity;
import de.dosmike.sponge.vshop.shops.StockItem;
import de.dosmike.sponge.vshop.systems.GameDictHelper;
import de.dosmike.sponge.vshop.systems.pluginfilter.FilterResolutionException;
import de.dosmike.sponge.vshop.systems.pluginfilter.PluginItemFilter;
import de.dosmike.sponge.vshop.systems.pluginfilter.PluginItemServiceImpl;
import de.dosmike.sponge.vshop.systems.ShopType;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.text.BookView;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import java.util.*;
import java.util.function.Function;

public class cmdAdd extends Command {

	static CommandSpec getCommandSpec() {
		return CommandSpec.builder()
				.arguments(GenericArguments.flags().valueFlag(
						GenericArguments.integer(Text.of("limit")), "l"
						).valueFlag(
						GenericArguments.integer(Text.of("slot")), "o"
						).valueFlag(
						//GenericArguments.enumValue(Text.of("filter"), StockItem.FilterOptions.class), "-filter"
						GenericArguments.choices(Text.of("filter"), () -> {
							List<String> choices = new LinkedList<>();
							for (StockItem.FilterOptions option : StockItem.FilterOptions.values())
								if (option != StockItem.FilterOptions.PLUGIN)
									choices.add(option.name());
							choices.addAll(PluginItemServiceImpl.getRegisteredIds());
							choices.sort(Comparator.naturalOrder());
							return choices;
						}, Function.identity(), false), "-filter"
						).buildWith(GenericArguments.seq(
						GenericArguments.onlyOne(GenericArguments.string(Text.of("BuyPrice"))),
						GenericArguments.onlyOne(GenericArguments.string(Text.of("SellPrice"))),
						GenericArguments.optional(GenericArguments.string(Text.of("Currency")))
						))
				).executor(new cmdAdd()).build();
	}

	private static void _displayAddItemOreDictSelector(Player player, UUID shopId, Collection<String> keys, Double buy, Double sell, Currency currency, int limit, int position) {
		List<Text> pages = new LinkedList<>();
		Text.Builder builder = Text.builder(
				VillagerShops.getTranslator().local("cmd.add.filter.oredictchoice").orLiteral(player)
		).append(Text.NEW_LINE);
		int i = 5;
		for (String s : keys) {
			if (i > 1) builder.append(Text.NEW_LINE);
			builder.append(Text.builder("[" + s + "]").onClick(TextActions.executeCallback((src) -> {
				StockItem item = new StockItem(s, buy, sell, currency, limit);
				_addItemToShop(src, shopId, item, position);
			})).build());
			if (++i >= 14) {
				pages.add(builder.build());
				builder = Text.builder();
				i = 1;
			}
		}
		Text last = builder.build();
		if (!last.isEmpty()) pages.add(last);
		player.sendBookView(BookView.builder()
				.title(Text.of("Adding OreDict item"))
				.addPages(pages)
				.author(Text.of("VillagerShops"))
				.build());
	}

	private static void _addItemToShop(CommandSource player, UUID shopid, StockItem item, int position) {
		Optional<ShopEntity> shopEntity = VillagerShops.getShopFromShopId(shopid);
		if (!shopEntity.isPresent()) return; //shop is gone
		ItemStackSnapshot displayItem;
		try {
			displayItem = item.getItem(ShopType.fromInstance(shopEntity.get()));
		} catch (FilterResolutionException e) {
			VillagerShops.w("%s", e.getMessage());
			return;
		}

		VillagerShops.closeShopInventories(shopEntity.get().getIdentifier()); //so players are forced to update
		VillagerShops.getInstance().markShopsDirty(shopEntity.get()); //save changes
		ShopMenuManager menu = shopEntity.get().getMenu();
		String auditOverwrite = "";
		if (position < 0) {
			menu.addItem(item);
		} else {
			auditOverwrite = menu.getItem(position).toString();
			menu.setItem(position, item);
		}
		player.sendMessage(Text.of(
				TextColors.GREEN, "[vShop] ",
				localText(position < 0 ? "cmd.add.success" : "cmd.add.replaced")
						.replace("%item%", Text.of(TextColors.RESET, displayItem.get(Keys.DISPLAY_NAME)
								.orElse(Text.of(displayItem.getType().getTranslation().get(Utilities.playerLocale(player)))), TextColors.GREEN))
						.replace("%pos%", menu.size())
						.orLiteral(player)
		));

		if (position < 0) {
			VillagerShops.audit("%s added an item %s to shop %s", Utilities.toString(player), item.toString(), shopEntity.get().toString());
		} else {
			VillagerShops.audit("%s replaced the item %s in slot %d with item %s in shop %s", Utilities.toString(player), auditOverwrite, position, item.toString(), shopEntity.get().toString());
		}
	}

	@NotNull
	@Override
	public CommandResult execute(@NotNull CommandSource src, @NotNull CommandContext args) throws CommandException {
		if (!(src instanceof Player)) {
			src.sendMessage(localText("cmd.playeronly").orLiteral(src));
			return CommandResult.success();
		}
		Player player = (Player) src;

		Optional<Entity> lookingAt = getEntityLookingAt(player, 5.0);
		Optional<ShopEntity> shopEntity = lookingAt.map(Entity::getUniqueId).flatMap(VillagerShops::getShopFromEntityId);
		if (!shopEntity.isPresent()) {
			throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
					localString("cmd.common.notarget").orLiteral(player)));
		}
		if (!PermissionRegistra.ADMIN.hasPermission(player) &&
				!shopEntity.get().isShopOwner(player.getUniqueId())) {
			throw new CommandException(Text.of(TextColors.RED,
					localString("permission.missing").orLiteral(player)));
		}

		ShopMenuManager menu = shopEntity.get().getMenu();

		int overwriteindex = -1; //-1 to append
		if (args.hasAny("slot")) {
			int testslot = args.<Integer>getOne("slot").get();
			if (testslot > menu.size() || testslot < 1) {
				throw new CommandException(Text.of(TextColors.RED,
						localString("cmd.add.overwrite.index").orLiteral(player)));
			}
			overwriteindex = testslot - 1;
		}
		Double buyFor, sellFor;
		int limit = 0;
		if (args.hasAny("limit")) {
			// check for player-shop, only those have stock
			if (!shopEntity.get().isShopOwner(player.getUniqueId())) {
				throw new CommandException(Text.of(TextColors.RED,
						localString("cmd.add.limit.adminshop").orLiteral(player)));
			} else {
				limit = args.<Integer>getOne("limit").orElse(0);
			}
		}

		String parse = args.getOne("BuyPrice").orElse("~").toString();
		try {
			buyFor = parse.equals("~") ? null : Double.parseDouble(parse);
		} catch (Exception e) {
			throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
					localString("cmd.add.buyprice").orLiteral(player)));
		}
		parse = args.getOne("SellPrice").orElse("~").toString();
		try {
			sellFor = parse.equals("~") ? null : Double.parseDouble(parse);
		} catch (Exception e) {
			throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
					localString("cmd.add.sellprice").orLiteral(player)));
		}

		if (buyFor == null && sellFor == null) {
			throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
					localString("cmd.add.noprice").orLiteral(player)));
		}
		if ((buyFor != null && buyFor < 0) || (sellFor != null && sellFor < 0)) {
			throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
					localString("cmd.add.negativeprice").orLiteral(player)));
		}

		Optional<ItemStack> item = player.getItemInHand(HandTypes.MAIN_HAND);
		if (!item.isPresent() || item.get().isEmpty())
			item = player.getItemInHand(HandTypes.OFF_HAND);
		if (!item.isPresent() || item.get().isEmpty()) {
			throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
					localString("cmd.add.itemisair").orLiteral(player)));
		}


		StockItem.FilterOptions nbtfilter = StockItem.FilterOptions.NORMAL;
		PluginItemFilter pluginItemFilter = PluginItemServiceImpl.getEnforcedFilter(item.get()).orElse(null);
		if (pluginItemFilter == null && args.hasAny("filter")) {
			String filterName = args.<String>getOne("filter").get();
			if (filterName.indexOf(':') >= 0) {
				if (!PluginItemServiceImpl.getItemFilter(filterName).isPresent())
					throw new CommandException(Text.of(TextColors.RED, "Unknown Plugin Item - This should not be selectable?"));

				Optional<PluginItemFilter> pif = PluginItemServiceImpl.getItemFilter(filterName);
				if (!pif.isPresent())
					throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
							localString("cmd.add.filter.missing")
									.replace("%filter%", filterName)
									.orLiteral(player)));

				nbtfilter = StockItem.FilterOptions.PLUGIN;
				pluginItemFilter = pif.get();
			} else {
				nbtfilter = StockItem.FilterOptions.valueOf(filterName);
				if (nbtfilter.equals(StockItem.FilterOptions.OREDICT) && !GameDictHelper.hasGameDict()) {
					throw new CommandException(Text.of(TextColors.RED,
							localString("cmd.add.filter.nooredict").orLiteral(player))
					);
				}
			}
		} else if (pluginItemFilter != null) {
			nbtfilter = StockItem.FilterOptions.PLUGIN;
		}
		if (pluginItemFilter == null && nbtfilter.equals(StockItem.FilterOptions.PLUGIN)) {
			throw new CommandException(Text.of(TextColors.RED, "[vShop] FailState: Plugin Item-Filter was null?"));
		}

		StockItem newItem;
		if (nbtfilter.equals(StockItem.FilterOptions.PLUGIN)) {
			if (!pluginItemFilter.supportShopType(ShopType.fromInstance(shopEntity.get())))
				throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
						localString(shopEntity.get().getShopOwner().isPresent() // is player-shop && denied
								? "cmd.add.filter.adminonly"
								: "cmd.add.filter.playeronly"
						).orLiteral(player)));
			if (!pluginItemFilter.isItem(item.get()))
				throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
						localString("cmd.add.filter.incomaptible").orLiteral(player)));

			newItem = new StockItem(item.get().createSnapshot(), pluginItemFilter, sellFor, buyFor,
					Utilities.CurrencyByName((String) args.getOne("Currency").orElse(null)),
					limit);
		} else if (nbtfilter.equals(StockItem.FilterOptions.OREDICT)) {
			Collection<String> keys = GameDictHelper.getKeys(item.get());
			VillagerShops.l("Found oredict entries: %s", String.join(", ", keys));
			if (keys.size() > 1) {
				_displayAddItemOreDictSelector(player, shopEntity.get().getIdentifier(), keys, sellFor, buyFor,
						Utilities.CurrencyByName((String) args.getOne("Currency").orElse(null)),
						limit, overwriteindex);

				return CommandResult.success(); //displaying the book for selection is a successful command execution
			} else if (keys.isEmpty()) { //no filter, since no oredict
				newItem = new StockItem(item.get().createSnapshot(), sellFor, buyFor,
						Utilities.CurrencyByName((String) args.getOne("Currency").orElse(null)),
						limit);
			} else {
				newItem = new StockItem(keys.iterator().next(), sellFor, buyFor,
						Utilities.CurrencyByName((String) args.getOne("Currency").orElse(null)),
						limit);
			}
		} else {
			ItemStack single = item.get().copy();
			single.setQuantity(1);
			newItem = new StockItem(single.createSnapshot(), sellFor, buyFor,
					Utilities.CurrencyByName((String) args.getOne("Currency").orElse(null)),
					limit, nbtfilter);
		}
		VillagerShops.closeShopInventories(shopEntity.get().getIdentifier()); //so players are forced to update
		VillagerShops.getInstance().markShopsDirty(shopEntity.get()); //save changes
		String auditOverwrite = "";
		if (overwriteindex < 0) {
			menu.addItem(newItem);
		} else {
			auditOverwrite = menu.getItem(overwriteindex).toString();
			menu.setItem(overwriteindex, newItem);
		}
		player.sendMessage(Text.of(
				TextColors.GREEN, "[vShop] ",
				localText(overwriteindex < 0 ? "cmd.add.success" : "cmd.add.replaced")
						.replace("%item%", Text.of(TextColors.RESET, item.get().get(Keys.DISPLAY_NAME)
								.orElse(Text.of(item.get().getType().getTranslation().get(Utilities.playerLocale(player)))), TextColors.GREEN))
						.replace("%pos%", menu.size())
						.orLiteral(player)
		));

		if (overwriteindex < 0) {
			VillagerShops.audit("%s added the item %s to shop %s",
					Utilities.toString(src), newItem.toString(), shopEntity.get().toString());
		} else {
			VillagerShops.audit("%s replaced the item %s in slot %d with the item %s in shop %s",
					Utilities.toString(src), auditOverwrite, overwriteindex, newItem.toString());
		}
		return CommandResult.success();
	}

}
