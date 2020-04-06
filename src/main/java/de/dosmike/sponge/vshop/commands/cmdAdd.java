package de.dosmike.sponge.vshop.commands;

import de.dosmike.sponge.vshop.PermissionRegistra;
import de.dosmike.sponge.vshop.Utilities;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.menus.ShopMenuManager;
import de.dosmike.sponge.vshop.shops.ShopEntity;
import de.dosmike.sponge.vshop.shops.StockItem;
import de.dosmike.sponge.vshop.systems.GameDictHelper;
import de.dosmike.sponge.vshop.systems.PluginItemFilter;
import de.dosmike.sponge.vshop.systems.PluginItemServiceImpl;
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
                        GenericArguments.choices(Text.of("filter"), ()->{
                            List<String> choices = new LinkedList<>();
                            for(StockItem.FilterOptions option : StockItem.FilterOptions.values())
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

    @NotNull @Override
    public CommandResult execute(@NotNull CommandSource src, @NotNull CommandContext args) throws CommandException {
        if (!(src instanceof Player)) {
            src.sendMessage(localText("cmd.playeronly").resolve(src).orElse(Text.of("[Player only]")));
            return CommandResult.success();
        }
        Player player = (Player) src;

        Optional<Entity> ent = getEntityLookingAt(player, 5.0);
        Optional<ShopEntity> npc = ent.map(Entity::getUniqueId).flatMap(VillagerShops::getShopFromEntityId);
        if (!npc.isPresent()) {
            throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                    localString("cmd.common.notarget").resolve(player).orElse("[no target]")));
        }
        if (!PermissionRegistra.ADMIN.hasPermission(player) &&
                !npc.get().isShopOwner(player.getUniqueId())) {
            throw new CommandException(Text.of(TextColors.RED,
                    localString("permission.missing").resolve(player).orElse("[permission missing]")));
        }

        ShopMenuManager prep = npc.get().getMenu();

        int overwriteindex = -1; //-1 to append
        if (args.hasAny("slot")) {
            int testslot = args.<Integer>getOne("slot").get();
            if (testslot > prep.size() || testslot < 1) {
                throw new CommandException(Text.of(TextColors.RED,
                        localString("cmd.add.overwrite.index").resolve(player).orElse("[invalid overwrite index]")));
            }
            overwriteindex = testslot - 1;
        }
        Double buyFor, sellFor;
        int limit = 0;
        if (args.hasAny("limit")) {
            // check for player-shop, only those have stock
            if (!npc.get().isShopOwner(player.getUniqueId())) {
                throw new CommandException(Text.of(TextColors.RED,
                        localString("cmd.add.limit.adminshop").resolve(player).orElse("[cant limit stockless]")));
            } else {
                limit = args.<Integer>getOne("limit").orElse(0);
            }
        }

        String parse = args.getOne("BuyPrice").orElse("~").toString();
        try {
            buyFor = parse.equals("~") ? null : Double.parseDouble(parse);
        } catch (Exception e) {
            throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                    localString("cmd.add.buyprice").resolve(player).orElse("[No buy price]")));
        }
        parse = args.getOne("SellPrice").orElse("~").toString();
        try {
            sellFor = parse.equals("~") ? null : Double.parseDouble(parse);
        } catch (Exception e) {
            throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                    localString("cmd.add.sellprice").resolve(player).orElse("[No sell price]")));
        }

        if (buyFor == null && sellFor == null) {
            throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                    localString("cmd.add.noprice").resolve(player).orElse("[No price]")));
        }
        if ((buyFor != null && buyFor < 0) || (sellFor != null && sellFor < 0)) {
            throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                    localString("cmd.add.negativeprice").resolve(player).orElse("[Negative price]")));
        }

        Optional<ItemStack> item = player.getItemInHand(HandTypes.MAIN_HAND);
        if (!item.isPresent() || item.get().isEmpty())
            item = player.getItemInHand(HandTypes.OFF_HAND);
        if (!item.isPresent() || item.get().isEmpty()) {
            throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                    localString("cmd.add.itemisair").resolve(player).orElse("[Item is air]")));
        }


        StockItem.FilterOptions nbtfilter = StockItem.FilterOptions.NORMAL;
        PluginItemFilter pluginItemFilter = PluginItemServiceImpl.getEnforcedFilter(item.get()).orElse(null);
        if (pluginItemFilter == null && args.hasAny("filter")) {
            String filterName = args.<String>getOne("filter").get();
            if (filterName.indexOf(':')>=0) {
                nbtfilter = StockItem.FilterOptions.PLUGIN;
                if (!PluginItemServiceImpl.getItemFilter(filterName).isPresent())
                    throw new CommandException(Text.of(TextColors.RED, "Unknown Plugin Item - This should not be selectable?"));

                Optional<PluginItemFilter> pif = PluginItemServiceImpl.getItemFilter(filterName);
                if (!pif.isPresent())
                    throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                            localString("cmd.add.filter.missing")
                                    .replace("%filter%", filterName)
                                    .resolve(player).orElse("[No such filter]")));

                pluginItemFilter = pif.get();
            } else {
                nbtfilter = StockItem.FilterOptions.valueOf(filterName);
                if (nbtfilter.equals(StockItem.FilterOptions.OREDICT) && !GameDictHelper.hasGameDict()) {
                    throw new CommandException(Text.of(TextColors.RED,
                            localString("cmd.add.filter.nooredict").resolve(player).orElse("[no oredict]"))
                    );
                }
            }
        } else if (pluginItemFilter != null) {
            nbtfilter = StockItem.FilterOptions.PLUGIN;
        }

        StockItem newItem;
        if (nbtfilter.equals(StockItem.FilterOptions.PLUGIN)) {
            if (!pluginItemFilter.supportShopType(!npc.get().getShopOwner().isPresent()))
                throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                        localString(npc.get().getShopOwner().isPresent() // is player-shop && denied
                                ? "cmd.add.filter.adminonly"
                                : "cmd.add.filter.playeronly"
                        ).resolve(player).orElse("[Shop not supported]")));
            if (!pluginItemFilter.isItem(item.get()))
                throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                        localString("cmd.add.filter.incomaptible").resolve(player).orElse("[Item <-> Filter missmatch]")));

            newItem = new StockItem(item.get(), pluginItemFilter, sellFor, buyFor,
                    Utilities.CurrencyByName((String) args.getOne("Currency").orElse(null)),
                    limit);
        } else if (nbtfilter.equals(StockItem.FilterOptions.OREDICT)) {
            Collection<String> keys = GameDictHelper.getKeys(item.get());
            VillagerShops.l("Found oredict entries: %s", String.join(", ", keys));
            if (keys.size() > 1){
                _displayAddItemOreDictSelector(player, npc.get().getIdentifier(), keys, sellFor, buyFor,
                        Utilities.CurrencyByName((String) args.getOne("Currency").orElse(null)),
                        limit, overwriteindex);

                return CommandResult.success(); //displaying the book for selection is a successful command execution
            } else if (keys.isEmpty()) { //no filter, since no oredict
                newItem = new StockItem(item.get(), sellFor, buyFor,
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
            newItem = new StockItem(single, sellFor, buyFor,
                    Utilities.CurrencyByName((String) args.getOne("Currency").orElse(null)),
                    limit, nbtfilter);
        }
        VillagerShops.closeShopInventories(npc.get().getIdentifier()); //so players are forced to update
        String auditOverwrite="";
        if (overwriteindex < 0) {
            prep.addItem(newItem);
        } else {
            auditOverwrite = prep.getItem(overwriteindex).toString();
            prep.setItem(overwriteindex, newItem);
        }
        player.sendMessage(Text.of(
                TextColors.GREEN, "[vShop] ",
                localText(overwriteindex < 0 ? "cmd.add.success" : "cmd.add.replaced")
                        .replace("%item%", Text.of(TextColors.RESET, item.get().get(Keys.DISPLAY_NAME)
                                .orElse(Text.of(item.get().getType().getTranslation().get(Utilities.playerLocale(player)))), TextColors.GREEN))
                        .replace("%pos%", prep.size())
                        .resolve(player).orElse(Text.of(overwriteindex < 0 ? "[item added]" : "[item replaced]"))
        ));

        if (overwriteindex < 0) {
            VillagerShops.audit("%s added the item %s to shop %s",
                    Utilities.toString(src), newItem.toString(), npc.get().toString());
        } else {
            VillagerShops.audit("%s replaced the item %s in slot %d with the item %s in shop %s",
                    Utilities.toString(src), auditOverwrite, overwriteindex, newItem.toString());
        }
        return CommandResult.success();
    }

    private static void _displayAddItemOreDictSelector(Player player, UUID shopid, Collection<String> keys, Double buy, Double sell, Currency currency, int limit, int position) {
        List<Text> pages = new LinkedList<>();
        Text.Builder builder = Text.builder(
                VillagerShops.getTranslator().local("cmd.add.filter.oredictchoice").resolve(player).orElse("cmd.add.filter.oredictchoice")
        ).append(Text.NEW_LINE);
        int i = 5;
        for (String s : keys) {
            if (i > 1) builder.append(Text.NEW_LINE);
            builder.append(Text.builder("["+s+"]").onClick(TextActions.executeCallback((src)->{
                StockItem item = new StockItem(s, buy, sell, currency, limit);
                _addItemToShop(src, shopid, item, position);
            })).build());
            if (++i>=14) {
                pages.add(builder.build());
                builder=Text.builder();
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
        Optional<ShopEntity> guard = VillagerShops.getShopFromShopId(shopid);
        if (!guard.isPresent()) return; //shop is gone

        VillagerShops.closeShopInventories(guard.get().getIdentifier()); //so players are forced to update
        ShopMenuManager prep = guard.get().getMenu();
        String auditOverwrite="";
        if (position < 0) {
            prep.addItem(item);
        } else {
            auditOverwrite = prep.getItem(position).toString();
            prep.setItem(position, item);
        }
        ItemStack displayItem = item.getItem(!guard.get().getShopOwner().isPresent());
        player.sendMessage(Text.of(
                TextColors.GREEN, "[vShop] ",
                localText(position < 0 ? "cmd.add.success" : "cmd.add.replaced")
                        .replace("%item%", Text.of(TextColors.RESET, displayItem.get(Keys.DISPLAY_NAME)
                                .orElse(Text.of(displayItem.getType().getTranslation().get(Utilities.playerLocale(player)))), TextColors.GREEN))
                        .replace("%pos%", prep.size())
                        .resolve(player).orElse(Text.of(position < 0 ? "[item added]" : "[item replaced]"))
        ));

        if (position < 0) {
            VillagerShops.audit("%s added an item %s to shop %s", Utilities.toString(player), item.toString(), guard.get().toString());
        } else {
            VillagerShops.audit("%s replaced the item %s in slot %d with item %s in shop %s", Utilities.toString(player), auditOverwrite, position, item.toString(), guard.get().toString());
        }
    }

}
