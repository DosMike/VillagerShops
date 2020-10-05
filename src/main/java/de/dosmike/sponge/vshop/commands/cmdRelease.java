package de.dosmike.sponge.vshop.commands;

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
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.Optional;

public class cmdRelease extends Command {

    static CommandSpec getCommandSpec() {
        return CommandSpec.builder()
                .permission(PermissionRegistra.IMPORT.getId())
                .arguments(
                        GenericArguments.none()
                ).executor(new cmdRelease()).build();
    }

    @NotNull
    @Override
    public CommandResult execute(@NotNull CommandSource src, @NotNull CommandContext args) throws CommandException {
        if (!(src instanceof Player)) {
            throw new CommandException(localText("cmd.playeronly").orLiteral(src));
        }
        Player player = (Player) src;

        Optional<Entity> lookingAt = getEntityLookingAt(player, 5.0);

        Optional<ShopEntity> shopEntity = VillagerShops.getShopFromEntityId(lookingAt.map(Entity::getUniqueId).orElse(null));
        if (!shopEntity.isPresent()) {
            throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                    localString("cmd.common.notarget").orLiteral(player)));
        } else {
            if (!PermissionRegistra.ADMIN.hasPermission(player) &&
                !shopEntity.get().isShopOwner(player.getUniqueId())) {
                throw new CommandException(Text.of(TextColors.RED,
                        localString("permission.missing").orLiteral(player)));
            }
            VillagerShops.audit("%s released the shop %s",
                    Utilities.toString(src), shopEntity.get().toString());

            VillagerShops.closeShopInventories(shopEntity.get().getIdentifier());
            shopEntity.get().freeEntity();
            VillagerShops.removeShop(shopEntity.get());
            src.sendMessage(Text.of(TextColors.GREEN, "[vShop] ",
                    localString("cmd.released").orLiteral(player)));

            return CommandResult.success();
        }
    }

}
