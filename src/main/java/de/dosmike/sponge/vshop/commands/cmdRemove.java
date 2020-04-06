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

        Optional<Entity> ent = getEntityLookingAt(player, 5.0);
        Optional<ShopEntity> npc = ent.map(Entity::getUniqueId).flatMap(VillagerShops::getShopFromEntityId);
        if (!npc.isPresent()) {
            throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                    localString("cmd.common.notarget").resolve(player).orElse("[no target]")));
        } else {
            if (!PermissionRegistra.ADMIN.hasPermission(player) &&
                    !npc.get().isShopOwner(player.getUniqueId())) {
                throw new CommandException(Text.of(TextColors.RED,
                        localString("permission.missing").resolve(player).orElse("[permission missing]")));
            }
            Integer index = (Integer) args.getOne("Index").get();
            if (index < 1 || index > npc.get().getMenu().size()) {
                throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                        localString("cmd.remove.invalidindex").resolve(player).orElse("[invalid index]")));
            } else {
                VillagerShops.closeShopInventories(npc.get().getIdentifier()); //so players are forced to update
                String auditRemoved=npc.get().getMenu().getItem(index-1).toString();
                npc.get().getMenu().removeIndex(index - 1);

                player.sendMessage(Text.of(TextColors.GREEN, "[vShop] ",
                        localString("cmd.remove.success")
                                .replace("%pos%", index)
                                .resolve(player).orElse("[success]")));

                VillagerShops.audit("%s removed the item %s from shop %s",
                        Utilities.toString(src), auditRemoved, npc.get().toString());
                return CommandResult.success();
            }
        }
    }

}
