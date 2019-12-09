package de.dosmike.sponge.vshop;

import com.flowpowered.math.vector.Vector3d;
import de.dosmike.sponge.languageservice.API.PluginTranslation;
import de.dosmike.sponge.vshop.menus.InvPrep;
import de.dosmike.sponge.vshop.shops.FieldResolver;
import de.dosmike.sponge.vshop.shops.NPCguard;
import de.dosmike.sponge.vshop.shops.StockItem;
import de.dosmike.sponge.vshop.systems.ChestLinkManager;
import de.dosmike.sponge.vshop.systems.GameDictHelper;
import de.dosmike.sponge.vshop.systems.LedgerManager;
import de.dosmike.sponge.vshop.systems.TranslationLoader;
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
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.BookView;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CommandRegistra {
    static PluginTranslation lang;

    static void register() {
        lang = VillagerShops.getTranslator();
        Map<List<String>, CommandSpec> children;

        Function<List<String>, Iterable<String>> CREATE_SkinSupplier = args->{
            for (Map.Entry<EntityType, Set<String>> entry : FieldResolver.getAutoTabCompleteMapping().entrySet()) {
                for (String arg : args) {
                    if (arg.equalsIgnoreCase(entry.getKey().getId())) {
                        return entry.getValue().stream().map(s->s.indexOf(' ')>=0?String.format("\"%s\"",s):s).collect(Collectors.toList());
                    }
                }
            }
            return new ArrayList<>(0);
        };

        children = new HashMap<>();
        children.put(Collections.singletonList("create"), CommandSpec.builder()
                .arguments(
                        GenericArguments.flags().valueFlag(
                                GenericArguments.string(Text.of("position")), "-at"
                        ).valueFlag(
                                DependingSuggestionElement.denenentSuggest(
                                        GenericArguments.string(Text.of("Skin")),
                                        Text.of("EntityType"),
                                        CREATE_SkinSupplier,
                                        false
                                ), "-skin"
                        ).buildWith(GenericArguments.seq(
                                GenericArguments.onlyOne(GenericArguments.catalogedElement(Text.of("EntityType"), EntityType.class)),
                                GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("Name")))
                        ))
                ).executor((src, args) -> {

                    if (!(src instanceof Player)) {
                        throw new CommandException(lang.localText("cmd.playeronly").resolve(src).orElse(Text.of("[Player only]")));
                    }
                    Player player = (Player) src;

                    if (!PermissionRegistra.ADMIN.hasPermission(player) &&
                        !PermissionRegistra.PLAYER.hasPermission(player)) {
                        throw new CommandException(Text.of(TextColors.RED,
                                lang.local("permission.missing").resolve(player).orElse("[permission missing]")));
                    }
                    boolean adminshop = PermissionRegistra.ADMIN.hasPermission(player);

                    if (!adminshop) {
                        Optional<String> option = player.getOption("vshop.option.playershop.limit");
                        int limit = -1;
                        try {
                            limit = Integer.parseInt(option.orElse("-1"));
                        } catch (Exception e) {/**/}
                        if (limit >= 0) {
                            int cnt = 0;
                            UUID pid = player.getUniqueId();
                            for (NPCguard npc : VillagerShops.getNPCguards())
                                if (npc.isShopOwner(pid)) cnt++;

                            if (cnt >= limit) {
                                throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                                        lang.local("cmd.create.playershop.limit").replace("%limit%", limit).resolve(player).orElse("[limit reached]")));
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
                        throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                                lang.local("cmd.create.invalidtype").resolve(player).orElse("[invalid type]")));
                    }
                    if (!adminshop) {
                        String entityPermission = npc.getNpcType().getId();
                        entityPermission = "vshop.create." + entityPermission.replace(':', '.').replace("_", "").replace("-", "");
                        if (!player.hasPermission(entityPermission)) {
                            throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                                    lang.local("cmd.create.entitypermission").replace("%permission%", entityPermission).resolve(player).orElse("[entity permission missing]")));
                        }
                    }
                    npc.setVariant(var);
                    //var wanted, but none returned/found
                    if (!"none".equalsIgnoreCase(var) &&
                            (npc.getVariant()==null ||
                            "none".equalsIgnoreCase(npc.getVariant().toString()))) {
                        throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                                lang.local("cmd.create.invalidvariant")
                                        .replace("%variant%", npc.getNpcType().getTranslation().get(Utilities.playerLocale(player)))
                                        .resolve(player).orElse("[invalid variant]")));
                    }
                    Location<World> createAt = player.getLocation();
                    double rotateYaw = player.getHeadRotation().getY();
                    if (args.hasAny("position")) try {
                        String[] parts = args.<String>getOne("position").get().split("/");
                        if (parts.length != 5) {
                            throw new Exception();
                        }
                        Optional<World> w = Sponge.getServer().getWorld(parts[0]);
                        if (!w.isPresent()) {
                            throw new CommandException(Text.of(TextColors.RED, "[vShop] ", lang.local("cmd.create.invalidworld").resolve(player).orElse("[Invalid pos]")));
                        }
                        createAt = w.get().getLocation(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
                        rotateYaw = Double.parseDouble(parts[4]);
                        while (rotateYaw > 180) rotateYaw -= 360;
                        while (rotateYaw <= -180) rotateYaw += 360;
                    } catch (Exception e) {
                        throw new CommandException(Text.of(TextColors.RED, "[vShop] ", lang.local("cmd.create.invalidpos").resolve(player).orElse("[Invalid pos]")));
                    }

                    npc.setDisplayName(displayName);
                    npc.setPreparator(prep);
                    npc.setLoc(createAt);
                    npc.setRot(new Vector3d(0, rotateYaw, 0));
                    boolean playershop = false;
                    try {
                        npc.setPlayerShop(player.getUniqueId());
                        playershop = true;
                    } catch (Exception e) {
                        if (!adminshop) {
                            throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                                    lang.local("cmd.create.playershop.missingcontainer").resolve(player).orElse("[no chest below]")));
                        }
                    }
                    VillagerShops.addNPCguard(npc);

                    src.sendMessage(Text.of(TextColors.GREEN, "[vShop] ",
                            lang.localText(playershop ? "cmd.create.playershop.success" : "cmd.create.success").replace("%name%", Text.of(TextColors.RESET, displayName)).resolve(player).orElse(Text.of("[success]"))));

                    VillagerShops.audit("%s created a new shop %s", Utilities.toString(src), npc.toString());
                    return CommandResult.success();
                }).build());
        children.put(Collections.singletonList("add"), CommandSpec.builder()
                .arguments(GenericArguments.flags().valueFlag(
                            GenericArguments.integer(Text.of("limit")), "l"
                        ).valueFlag(
                            GenericArguments.integer(Text.of("slot")), "o"
                        ).valueFlag(
                            GenericArguments.enumValue(Text.of("filter"), StockItem.FilterOptions.class), "-filter"
                        ).buildWith(GenericArguments.seq(
                            GenericArguments.onlyOne(GenericArguments.string(Text.of("BuyPrice"))),
                            GenericArguments.onlyOne(GenericArguments.string(Text.of("SellPrice"))),
                            GenericArguments.optional(GenericArguments.string(Text.of("Currency")))
                        ))
                ).executor((src, args) -> {
                    if (!(src instanceof Player)) {
                        src.sendMessage(lang.localText("cmd.playeronly").resolve(src).orElse(Text.of("[Player only]")));
                        return CommandResult.success();
                    }
                    Player player = (Player) src;

                    Optional<Entity> ent = getEntityLookingAt(player, 5.0);
                    Location<World> loc = ent.orElse(player).getLocation();

                    Optional<NPCguard> npc = VillagerShops.getNPCfromLocation(loc);
                    if (!npc.isPresent()) {
                        throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                                lang.local("cmd.common.notarget").resolve(player).orElse("[no target]")));
                    }
                    if (!PermissionRegistra.ADMIN.hasPermission(player) &&
                        !npc.get().isShopOwner(player.getUniqueId())) {
                        throw new CommandException(Text.of(TextColors.RED,
                                lang.local("permission.missing").resolve(player).orElse("[permission missing]")));
                    }

                    InvPrep prep = npc.get().getPreparator();

                    int overwriteindex = -1; //-1 to append
                    if (args.hasAny("slot")) {
                        int testslot = args.<Integer>getOne("slot").get();
                        if (testslot > prep.size() || testslot < 1) {
                            throw new CommandException(Text.of(TextColors.RED,
                                    lang.local("cmd.add.overwrite.index").resolve(player).orElse("[invalid overwrite index]")));
                        }
                        overwriteindex = testslot - 1;
                    }
                    Double buyFor, sellFor;
                    int limit = 0;
                    if (args.hasAny("limit")) {
                        if (!npc.get().isShopOwner(player.getUniqueId())) {
                            throw new CommandException(Text.of(TextColors.RED,
                                    lang.local("cmd.add.limit.adminshop").resolve(player).orElse("[cant limit stockless]")));
                        } else {
                            limit = args.<Integer>getOne("limit").orElse(0);
                        }
                    }
                    StockItem.FilterOptions nbtfilter = StockItem.FilterOptions.NORMAL;
                    if (args.hasAny("filter")) {
                        nbtfilter = args.<StockItem.FilterOptions>getOne("filter").orElse(StockItem.FilterOptions.NORMAL);
                        if (nbtfilter.equals(StockItem.FilterOptions.OREDICT) && !GameDictHelper.hasGameDict()) {
                            throw new CommandException(Text.of(TextColors.RED,
                                            lang.local("cmd.add.filter.nooredict").resolve(player).orElse("[no oredict]"))
                            );
                        }
                    }

                    String parse = args.getOne("BuyPrice").orElse("~").toString();
                    try {
                        buyFor = parse.equals("~") ? null : Double.parseDouble(parse);
                    } catch (Exception e) {
                        throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                                lang.local("cmd.add.buyprice").resolve(player).orElse("[No buy price]")));
                    }
                    parse = args.getOne("SellPrice").orElse("~").toString();
                    try {
                        sellFor = parse.equals("~") ? null : Double.parseDouble(parse);
                    } catch (Exception e) {
                        throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                                lang.local("cmd.add.sellprice").resolve(player).orElse("[No sell price]")));
                    }

                    if (buyFor == null && sellFor == null) {
                        throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                                lang.local("cmd.add.noprice").resolve(player).orElse("[No price]")));
                    }
                    if ((buyFor != null && buyFor < 0) || (sellFor != null && sellFor < 0)) {
                        throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                                lang.local("cmd.add.negativeprice").resolve(player).orElse("[Negative price]")));
                    }

                    Optional<ItemStack> item = player.getItemInHand(HandTypes.MAIN_HAND);
                    if (!item.isPresent() || item.get().isEmpty())
                        item = player.getItemInHand(HandTypes.OFF_HAND);
                    if (!item.isPresent() || item.get().isEmpty()) {
                        throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                                lang.local("cmd.add.itemisair").resolve(player).orElse("[Item is air]")));
                    }
                    StockItem newItem;
                    if (nbtfilter.equals(StockItem.FilterOptions.OREDICT)) {
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
                            lang.localText(overwriteindex < 0 ? "cmd.add.success" : "cmd.add.replaced")
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
                }).build());
        children.put(Collections.singletonList("remove"), CommandSpec.builder()
                .arguments(
                        GenericArguments.integer(Text.of("Index"))
                ).executor((src, args) -> {
                    if (!(src instanceof Player)) {
                        throw new CommandException(lang.localText("cmd.playeronly").resolve(src).orElse(Text.of("[Player only]")));
                    }
                    Player player = (Player) src;

                    Optional<Entity> ent = getEntityLookingAt(player, 5.0);
                    Location<World> loc = ent.orElse(player).getLocation();

                    Optional<NPCguard> npc = VillagerShops.getNPCfromLocation(loc);
                    if (!npc.isPresent()) {
                        throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                                lang.local("cmd.common.notarget").resolve(player).orElse("[no target]")));
                    } else {
                        if (!PermissionRegistra.ADMIN.hasPermission(player) &&
                                !npc.get().isShopOwner(player.getUniqueId())) {
                            throw new CommandException(Text.of(TextColors.RED,
                                    lang.local("permission.missing").resolve(player).orElse("[permission missing]")));
                        }
                        Integer index = (Integer) args.getOne("Index").get();
                        if (index < 1 || index > npc.get().getPreparator().size()) {
                            throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                                    lang.local("cmd.remove.invalidindex").resolve(player).orElse("[invalid index]")));
                        } else {
                            VillagerShops.closeShopInventories(npc.get().getIdentifier()); //so players are forced to update
                            String auditRemoved=npc.get().getPreparator().getItem(index-1).toString();
                            npc.get().getPreparator().removeIndex(index - 1);

                            player.sendMessage(Text.of(TextColors.GREEN, "[vShop] ",
                                    lang.local("cmd.remove.success")
                                            .replace("%pos%", index)
                                            .resolve(player).orElse("[success]")));

                            VillagerShops.audit("%s removed the item %s from shop %s",
                                    Utilities.toString(src), auditRemoved, npc.get().toString());
                            return CommandResult.success();
                        }
                    }
                }).build());
        children.put(Collections.singletonList("delete"), CommandSpec.builder()
                .arguments(
                        GenericArguments.none()
                ).executor((src, args) -> {
                    if (!(src instanceof Player)) {
                        throw new CommandException(lang.localText("cmd.playeronly").resolve(src).orElse(Text.of("[Player only]")));
                    }
                    Player player = (Player) src;

                    Optional<Entity> ent = getEntityLookingAt(player, 5.0);
                    Location<World> loc = ent.orElse(player).getLocation();

                    Optional<NPCguard> npc = VillagerShops.getNPCfromLocation(loc);
                    if (!npc.isPresent()) {
                        throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                                lang.local("cmd.common.notarget").resolve(player).orElse("[no target]")));
                    } else {
                        if (!PermissionRegistra.ADMIN.hasPermission(player) &&
                            !npc.get().isShopOwner(player.getUniqueId())) {
                            throw new CommandException(Text.of(TextColors.RED,
                                    lang.local("permission.missing").resolve(player).orElse("[permission missing]")));
                        }
                        VillagerShops.audit("%s deleted the shop %s",
                                Utilities.toString(src), npc.get().toString());

                        VillagerShops.stopTimers();
                        VillagerShops.closeShopInventories(npc.get().getIdentifier());
                        npc.get().getLe().remove();
                        VillagerShops.removeNPCguard(npc.get());
                        VillagerShops.startTimers();
                        src.sendMessage(Text.of(TextColors.GREEN, "[vShop] ",
                                lang.local("cmd.deleted").resolve(player).orElse("[deleted]")));

                        return CommandResult.success();
                    }
                }).build());
        children.put(Collections.singletonList("save"), CommandSpec.builder()
                .permission(PermissionRegistra.ADMIN.getId())
                .arguments(
                        GenericArguments.none()
                ).executor((src, args) -> {
                    VillagerShops.getInstance().saveShops();
                    src.sendMessage(Text.of(TextColors.GREEN, "[vShop] ",
                            lang.local("cmd.saved").resolve(src).orElse("[saved]")));
                    VillagerShops.audit("%s saved the shops", Utilities.toString(src));
                    return CommandResult.success();
                }).build());
        children.put(Collections.singletonList("reload"), CommandSpec.builder()
                .permission(PermissionRegistra.ADMIN.getId())
                .arguments(
                        GenericArguments.none()
                ).executor((src, args) -> {
                    VillagerShops.getInstance().loadConfigs();
                    TranslationLoader.fetchTranslations();
                    src.sendMessage(Text.of("Reload complete"));
                    VillagerShops.audit("%s reloaded the settings", Utilities.toString(src));
                    return CommandResult.success();
                }).build());
        children.put(Arrays.asList("list", "get", "for"), CommandSpec.builder()
                .permission(PermissionRegistra.ADMIN.getId())
                .arguments(
                        GenericArguments.optional(
                                GenericArguments.user(Text.of("User"))
                        )
                ).executor(new CommandExecutor() {
                    private List<Text> pump(Collection<NPCguard> shops) {
                        List<Text> pages = new ArrayList<>(shops.size() / 16 + 1);
                        Text.Builder page = Text.builder();
                        int i = 0;

                        for (NPCguard shop : shops) {
                            Optional<UUID> oid = shop.getShopOwner();
                            Optional<User> owner = oid.flatMap(uuid -> VillagerShops.getUserStorage().get(uuid));
                            if (i > 0) page.append(Text.NEW_LINE);
                            page.append(entry(owner.orElse(null), shop));
                            i++;
                            if (i >= 16) {
                                pages.add(page.build());
                                page = Text.builder();
                                i = 0;
                            }
                        }
                        if (i > 0) {
                            pages.add(page.build());
                        }
                        return pages;
                    }

                    private Text entry(User user, NPCguard shop) {
                        Text name = shop.getDisplayName();
                        Text line = Text.builder().append(name.toPlain().trim().isEmpty() ? Text.of("<NO NAME>") : name)
                                .onHover(TextActions.showText(Text.of(
                                        TextColors.WHITE, "Type: ", TextColors.GRAY, shop.getNpcType().getId(), Text.NEW_LINE,
                                        TextColors.WHITE, "Skin: ", TextColors.GRAY, shop.getVariantName(), Text.NEW_LINE,
                                        TextColors.WHITE, TextStyles.ITALIC, "Click to teleport"
                                )))
                                .onClick(TextActions.executeCallback(src -> {
                                    VillagerShops.audit("%s teleported to shop %s via /vshop list",
                                            Utilities.toString(src), shop.toString() );
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
                                ));

                        if (shop.getStockContainer().isPresent()) {
                            line = Text.of(line, Text.builder(" [Open Stock]")
                                    .onClick(TextActions.executeCallback(src -> {
                                        VillagerShops.audit("%s opened stock container for shop %s via /vshop list",
                                                Utilities.toString(src), shop.toString() );
                                        if (src instanceof Player)
                                            shop.getStockInventory().ifPresent(((Player) src)::openInventory);
                                    })).onHover(TextActions.showText(Text.of("Click to invsee")))
                                    .build());
                        }

                        line = Text.of(line, " ", Text.builder("[Delete Shop]")
                                .color(TextColors.RED)
                                .onClick(TextActions.executeCallback(src -> {
                                    if (!(src instanceof Player)) {
                                        src.sendMessage(Text.of(TextColors.RED, "This Action can only be performed by players"));
                                        return;
                                    }
                                    //confirmation
                                    Text text = Text.of("You are about to delete the shop ",shop.getDisplayName(),
                                            " by ",shop.getShopOwner()
                                                    .flatMap(u->VillagerShops.getUserStorage().get(u))
                                                    .map(u->Text.of(u.getName()))
                                                    .orElse(Text.builder("admin").color(TextColors.DARK_RED).build()), Text.NEW_LINE,
                                            "Shop location: ", shop.getLoc().getExtent().getName(),
                                                    " ", shop.getLoc().getBlockX(),
                                                    ",", shop.getLoc().getBlockY(),
                                                    ",", shop.getLoc().getBlockZ(), Text.NEW_LINE,
                                            "Shop ID: ", shop.getIdentifier(), Text.NEW_LINE, Text.NEW_LINE,
                                            "Please confirm your action by clicking ",
                                                Text.builder("here")
                                                    .color(TextColors.DARK_BLUE)
                                                    .style(TextStyles.UNDERLINE)
                                                    .onClick(TextActions.executeCallback((cmdsrc)->{
                                                        VillagerShops.audit("% deleted shop %s via /vshop list",
                                                                Utilities.toString(src), shop.toString() );
                                                        API.delete(shop);
                                                        cmdsrc.sendMessage(Text.of("Good bye, ", shop.getDisplayName()));
                                                    })).build()
                                            );
                                    BookView book = BookView.builder()
                                            .addPage(text)
                                            .title(Text.of("Delete VShop"))
                                            .author(Text.of("VShops"))
                                            .build();
                                    ((Player)src).sendBookView(book);
                                })).onHover(TextActions.showText(Text.of("Click to delete this shop")))
                                .build());

                        return line;
                    }

                    @Override
                    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
                        Collection<NPCguard> filtered;
                        if (args.hasAny("User")) {
                            User target = (User) args.getOne("User").get();
                            UUID searchID = target.getUniqueId();
                            filtered = VillagerShops.getNPCguards().stream().filter(npc -> npc.isShopOwner(searchID)).collect(Collectors.toList());

                            PaginationList.builder()
                                    .title(Text.of("Shops owned by " + target.getName()))
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
                .permission(PermissionRegistra.IDENTIFY.getId())
                .arguments(
                        GenericArguments.none()
                ).executor((src, args) -> {
                    if (!(src instanceof Player)) {
                        throw new CommandException(lang.localText("cmd.playeronly").resolve(src).orElse(Text.of("[Player only]")));
                    }
                    Player player = (Player) src;

                    Optional<Entity> ent = getEntityLookingAt(player, 5.0);
                    Location<World> loc = ent.orElse(player).getLocation();

                    Optional<NPCguard> npc = VillagerShops.getNPCfromLocation(loc);
                    if (!npc.isPresent()) {
                        throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                                lang.local("cmd.common.notarget").resolve(player).orElse("[no target]")));
                    } else {
                        Optional<UUID> owner = npc.get().getShopOwner();
                        Optional<Player> powner = owner.flatMap(uuid -> Sponge.getServer().getPlayer(uuid));
                        Text.Builder ownername = Text.builder(owner.isPresent()
                                ? (powner.isPresent()
                                    ? powner.get().getName()
                                    : owner.get().toString())
                                : lang.local("cmd.identify.adminshop").resolve(player).orElse("[Server]"));
                        if (owner.isPresent()) {
                            ownername.onHover(TextActions.showText(Text.of("UUID: " + owner.get().toString())));
                            ownername.onShiftClick(TextActions.insertText(owner.get().toString()));
                        }

                        src.sendMessage(Text.of(TextColors.GREEN, "[vShop] ",
                                lang.localText("cmd.identify.response")
                                        .replace("\\n", Text.NEW_LINE)
                                        .replace("%type%", VillagerShops.getTranslator().localText(npc.get().getShopOwner().isPresent() ? "shop.type.player" : "shop.type.admin"))
                                        .replace("%entity%", npc.get().getLe().getTranslation().get(Utilities.playerLocale(player)))
                                        .replace("%skin%", npc.get().getVariantName())
                                        .replace("%name%", npc.get().getDisplayName())
                                        .replace("%id%",
                                                Text.builder(npc.get().getIdentifier().toString())
                                                        .onShiftClick(TextActions.insertText(npc.get().getIdentifier().toString()))
                                                        .onHover(TextActions.showText(lang.localText("cmd.identify.shiftclick").resolve(src).orElse(Text.of("Shift-click"))))
                                                        .build())
                                        .replace("%owner%", ownername.build())
                                        .resolve(player).orElse(Text.of("[much data, such wow]"))));

                        VillagerShops.audit("%s identified shop %s",
                                Utilities.toString(src), npc.get().toString() );
                        return CommandResult.success();
                    }
                }).build());
        children.put(Collections.singletonList("link"), CommandSpec.builder()
                .permission(PermissionRegistra.LINKCHEST.getId())
                .arguments(
                        GenericArguments.none()
                ).executor((src, args) -> {
                    if (!(src instanceof Player)) {
                        throw new CommandException(lang.localText("cmd.playeronly").resolve(src).orElse(Text.of("[Player only]")));
                    }
                    Player player = (Player) src;

                    Optional<Entity> ent = getEntityLookingAt(player, 5.0);
                    Location<World> loc = ent.orElse(player).getLocation();

                    Optional<NPCguard> npc = VillagerShops.getNPCfromLocation(loc);
                    ChestLinkManager.toggleLinker(player, npc);

                    return CommandResult.success();
                }).build());
        children.put(Collections.singletonList("tphere"), CommandSpec.builder()
                .permission(PermissionRegistra.MOVE.getId())
                .arguments(
                        GenericArguments.uuid(Text.of("shopid"))
                ).executor((src, args) -> {
                    if (!(src instanceof Player)) {
                        throw new CommandException(lang.localText("cmd.playeronly").resolve(src).orElse(Text.of("[Player only]")));
                    }
                    Player player = (Player) src;

                    Optional<NPCguard> npc = VillagerShops.getNPCfromShopUUID(args.<UUID>getOne("shopid").get());
                    if (!npc.isPresent()) {
                        src.sendMessage(lang.localText("cmd.common.noshopforid").resolve(src).orElse(Text.of("[Shop not found]")));
                    } else {
                        if (!PermissionRegistra.ADMIN.hasPermission(player) &&
                            !npc.get().isShopOwner(player.getUniqueId())) {
                            throw new CommandException(Text.of(TextColors.RED,
                                    lang.local("permission.missing").resolve(player).orElse("[permission missing]")));
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

                        VillagerShops.audit("%s relocated shop %s to %s°%.2f, %d blocks",
                                Utilities.toString(src), guard.toString(),
                                Utilities.toString(to), player.getHeadRotation().getY(),
                                distance.orElse(-1)
                        );
                        VillagerShops.closeShopInventories(guard.getIdentifier());
                        guard.move(new Location<World>(to.getExtent(), to.getBlockX() + 0.5, to.getY(), to.getBlockZ() + 0.5));
                        guard.setRot(new Vector3d(0.0, player.getHeadRotation().getY(), 0.0));
                    }
                    return CommandResult.success();
                }).build());
        children.put(Arrays.asList("ledger", "log"), CommandSpec.builder()
                .permission(PermissionRegistra.LEDGER_ME.getId())
                .arguments(
                        GenericArguments.flags().permissionFlag(PermissionRegistra.LEDGER_OTHERS.getId(), "t")
                                .buildWith(GenericArguments.optional(
                                        GenericArguments.user(Text.of("Target"))
                                ))
                ).executor((src, args) -> {
                    if (args.hasAny("Target") && args.hasAny("t")) {
                        throw new CommandException(lang.localText("cmd.ledger.invalid").resolve(src).orElse(Text.of("[Choose one argument]")));
                    } else if (!args.hasAny("Target") && !(src instanceof Player)) {
                        throw new CommandException(lang.localText("cmd.missingargument").resolve(src).orElse(Text.of("[Missing argument]")));
                    } else {
                        User target;
                        if (src instanceof Player)
                            target = (User) args.getOne("Target").orElse((User) src);
                        else if (args.hasAny("Target"))
                            target = (User) args.getOne("Target").get();
                        else throw new CommandException(Text.of("No target console, shouldn't fail"));
                        src.sendMessage(Text.of("Searching Business Ledger, please wait.."));
                        LedgerManager.openLedgerFor(src, target);
                        if (src instanceof Player && ((Player) src).getUniqueId().equals(target.getUniqueId())) {
                            VillagerShops.audit("%s requested their business ledger", Utilities.toString(src));
                        } else {
                            VillagerShops.audit("%s requested the business ledger for %s", Utilities.toString(src), Utilities.toString(target));
                        }
                    }
                    return CommandResult.success();
                }).build());


        Sponge.getCommandManager().register(VillagerShops.getInstance(), CommandSpec.builder()
                        .description(Text.of(lang.local("cmd.description.short").toString()))
                        .extendedDescription(Text.of(lang.local("cmd.description.long").replace("\\n", "\n").toString()))
                        .children(children)
                        .build()
                , "vshop");
    }

    /**
     * EntityRay :D
     * Made this public so other plugins may use this for API purposes
     * Don't ask too closely how this works, I wrote this years ago...
     */
    public static Optional<Entity> getEntityLookingAt(Living source, Double maxRange) {
        Collection<Entity> ents = source.getNearbyEntities(maxRange); // get all entities in interaction range
        //we need a facing vector for the source
        Vector3d rot = source.getHeadRotation().mul(Math.PI / 180.0); //to radians
        Vector3d dir = new Vector3d(
                -Math.cos(rot.getX()) * Math.sin(rot.getY()),
                -Math.sin(rot.getX()),
                Math.cos(rot.getX()) * Math.cos(rot.getY())); //should now be a unit vector (len 1)

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
                if (lastDist.containsKey(ent) && lastDist.get(ent) - curdist < 0)
                    marked.add(ent); // entity is moving away from ray pos
                else lastDist.put(ent, curdist);

                if (curdist < 1.44) { //assume height of ~2
                    return Optional.of(ent);
                }
            }
            for (Entity ent : marked) {
//				VillagerShops.l("Dropping %s{%s}", ent.getType().getName(), ent.getUniqueId().toString());
                lastDist.remove(ent);
                ents.remove(ent);
            }
            src = src.add(dir);
            dist += 0.1;
        }
        return Optional.empty();
    }

    /**
     * throws number format exception if the option has a invalid (non positive integer) value
     */
    public static Optional<Integer> getMaximumStockDistance(Player player) throws NumberFormatException {
        Optional<String> val = player.getOption("vshop.option.chestlink.distance");
        Optional<Integer> res = val
                .filter(s -> s.matches("[1-9][0-9]*"))
                .map(Integer::valueOf);
        if (val.isPresent() && !res.isPresent())
            throw new NumberFormatException();
        return res;
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
        Optional<NPCguard> guard = VillagerShops.getNPCfromShopUUID(shopid);
        if (!guard.isPresent()) return; //shop is gone

        VillagerShops.closeShopInventories(guard.get().getIdentifier()); //so players are forced to update
        InvPrep prep = guard.get().getPreparator();
        String auditOverwrite="";
        if (position < 0) {
            prep.addItem(item);
        } else {
            auditOverwrite = prep.getItem(position).toString();
            prep.setItem(position, item);
        }
        player.sendMessage(Text.of(
                TextColors.GREEN, "[vShop] ",
                lang.localText(position < 0 ? "cmd.add.success" : "cmd.add.replaced")
                        .replace("%item%", Text.of(TextColors.RESET, item.getItem().get(Keys.DISPLAY_NAME)
                                .orElse(Text.of(item.getItem().getType().getTranslation().get(Utilities.playerLocale(player)))), TextColors.GREEN))
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
