package de.dosmike.sponge.vshop.commands;

import de.dosmike.sponge.vshop.PermissionRegistra;
import de.dosmike.sponge.vshop.Utilities;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.shops.ShopEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import java.util.Optional;
import java.util.UUID;

public class cmdIdentify extends Command {

    static CommandSpec getCommandSpec() {
        return CommandSpec.builder()
                .permission(PermissionRegistra.IDENTIFY.getId())
                .arguments(
                        GenericArguments.none()
                ).executor(new cmdIdentify()).build();
    }

    @NotNull
    @Override
    public CommandResult execute(@NotNull CommandSource src, @NotNull CommandContext args) throws CommandException {
        if (!(src instanceof Player)) {
            throw new CommandException(localText("cmd.playeronly").orLiteral(src));
        }
        Player player = (Player) src;

        Optional<Entity> lookingAt = getEntityLookingAt(player, 5.0);
        Optional<ShopEntity> shopEntity = lookingAt.map(Entity::getUniqueId).flatMap(VillagerShops::getShopFromEntityId);
        if (!shopEntity.isPresent()) {
            throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                    localString("cmd.common.notarget").orLiteral(player)));
        } else {
            Optional<UUID> ownerId = shopEntity.get().getShopOwner();
            Optional<Player> ownerPlayer = ownerId.flatMap(uuid -> Sponge.getServer().getPlayer(uuid));
            Text.Builder ownername = Text.builder(ownerId.isPresent()
                    ? (ownerPlayer.isPresent()
                    ? ownerPlayer.get().getName()
                    : ownerId.get().toString())
                    : localString("cmd.identify.adminshop").orLiteral(player));
            if (ownerId.isPresent()) {
                ownername.onHover(TextActions.showText(Text.of("UUID: " + ownerId.get().toString())));
                ownername.onShiftClick(TextActions.insertText(ownerId.get().toString()));
            }

            src.sendMessage(Text.of(TextColors.GREEN, "[vShop] ",
                    localText("cmd.identify.response")
                            .replace("\\n", Text.NEW_LINE)
                            .replace("%type%", VillagerShops.getTranslator().localText(shopEntity.get().getShopOwner().isPresent() ? "shop.type.player" : "shop.type.admin"))
                            .replace("%entity%", shopEntity.get().getNpcType().getTranslation().get(Utilities.playerLocale(player)))
                            .replace("%skin%", shopEntity.get().getVariantName())
                            .replace("%name%", shopEntity.get().getDisplayName())
                            .replace("%id%",
                                    Text.builder(shopEntity.get().getIdentifier().toString())
                                            .onShiftClick(TextActions.insertText(shopEntity.get().getIdentifier().toString()))
                                            .onHover(TextActions.showText(localText("cmd.identify.shiftclick").orLiteral(src)))
                                            .build())
                            .replace("%owner%", ownername.build())
                            .orLiteral(player)));

            VillagerShops.audit("%s identified shop %s",
                    Utilities.toString(src), shopEntity.get().toString() );
            return CommandResult.success();
        }
    }

}
