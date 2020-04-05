package de.dosmike.sponge.vshop.commands;

import de.dosmike.sponge.vshop.API;
import de.dosmike.sponge.vshop.PermissionRegistra;
import de.dosmike.sponge.vshop.Utilities;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.shops.ShopEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.BookView;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import java.util.*;
import java.util.stream.Collectors;

public class cmdList extends Command {

    static CommandSpec getCommandSpec() {
        return CommandSpec.builder()
                .permission(PermissionRegistra.ADMIN.getId())
                .arguments(
                        GenericArguments.optional(
                                GenericArguments.user(Text.of("User"))
                        )
                ).executor(new cmdList()).build();
    }

    @NotNull
    @Override
    public CommandResult execute(@NotNull CommandSource src, @NotNull CommandContext args) throws CommandException {
        Collection<ShopEntity> filtered;
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

    private List<Text> pump(Collection<ShopEntity> shops) {
        List<Text> pages = new ArrayList<>(shops.size() / 16 + 1);
        Text.Builder page = Text.builder();
        int i = 0;

        for (ShopEntity shop : shops) {
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

    private Text entry(User user, ShopEntity shop) {
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
                        ((Player) src).setLocation(shop.getLocation());
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
                            "Shop location: ", shop.getLocation().getExtent().getName(),
                            " ", shop.getLocation().getBlockX(),
                            ",", shop.getLocation().getBlockY(),
                            ",", shop.getLocation().getBlockZ(), Text.NEW_LINE,
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

}
