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

public class cmdRemove extends Command {

    static CommandSpec getCommandSpec() {
        return CommandSpec.builder()
                .arguments(
                        GenericArguments.integer(Text.of("Index"))
                ).executor(new cmdRemove()).build();
    }

    @NotNull
    @Override
    public CommandResult execute(@NotNull CommandSource src, @NotNull CommandContext args) throws CommandException {
        if (!(src instanceof Player)) {
            throw new CommandException(localText("cmd.playeronly").resolve(src).orElse(Text.of("[Player only]")));
        }
        Player player = (Player) src;

        Optional<Entity> lookingAt = getEntityLookingAt(player, 5.0);
        Optional<ShopEntity> shopEntity = lookingAt.map(Entity::getUniqueId).flatMap(VillagerShops::getShopFromEntityId);
        if (!shopEntity.isPresent()) {
            throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                    localString("cmd.common.notarget").resolve(player).orElse("[no target]")));
        } else {
            if (!PermissionRegistra.ADMIN.hasPermission(player) &&
                    !shopEntity.get().isShopOwner(player.getUniqueId())) {
                throw new CommandException(Text.of(TextColors.RED,
                        localString("permission.missing").resolve(player).orElse("[permission missing]")));
            }
            int index = (Integer) args.getOne("Index").get();
            if (index < 1 || index > shopEntity.get().getMenu().size()) {
                throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                        localString("cmd.remove.invalidindex").resolve(player).orElse("[invalid index]")));
            } else {
                VillagerShops.closeShopInventories(shopEntity.get().getIdentifier()); //so players are forced to update
                String auditRemoved=shopEntity.get().getMenu().getItem(index-1).toString();
                shopEntity.get().getMenu().removeIndex(index - 1);

                player.sendMessage(Text.of(TextColors.GREEN, "[vShop] ",
                        localString("cmd.remove.success")
                                .replace("%pos%", index)
                                .resolve(player).orElse("[success]")));

                VillagerShops.audit("%s removed the item %s from shop %s",
                        Utilities.toString(src), auditRemoved, shopEntity.get().toString());
                return CommandResult.success();
            }
        }
    }

}
