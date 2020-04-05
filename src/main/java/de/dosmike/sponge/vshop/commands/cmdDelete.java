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

public class cmdDelete extends Command {

    static CommandSpec getCommandSpec() {
        return CommandSpec.builder()
                .arguments(
                        GenericArguments.none()
                ).executor(new cmdDelete()).build();
    }

    @NotNull
    @Override
    public CommandResult execute(@NotNull CommandSource src, @NotNull CommandContext args) throws CommandException {
        if (!(src instanceof Player)) {
            throw new CommandException(localText("cmd.playeronly").resolve(src).orElse(Text.of("[Player only]")));
        }
        Player player = (Player) src;

        Optional<Entity> ent = getEntityLookingAt(player, 5.0);

        Optional<ShopEntity> npc = VillagerShops.getNPCfromEntityUUID(ent.map(Entity::getUniqueId).orElse(null));
        if (!npc.isPresent()) {
            throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                    localString("cmd.common.notarget").resolve(player).orElse("[no target]")));
        } else {
            if (!PermissionRegistra.ADMIN.hasPermission(player) &&
                    !npc.get().isShopOwner(player.getUniqueId())) {
                throw new CommandException(Text.of(TextColors.RED,
                        localString("permission.missing").resolve(player).orElse("[permission missing]")));
            }
            VillagerShops.audit("%s deleted the shop %s",
                    Utilities.toString(src), npc.get().toString());

            VillagerShops.closeShopInventories(npc.get().getIdentifier());
            ent.get().remove();
            VillagerShops.removeNPCguard(npc.get());
            src.sendMessage(Text.of(TextColors.GREEN, "[vShop] ",
                    localString("cmd.deleted").resolve(player).orElse("[deleted]")));

            return CommandResult.success();
        }
    }

}
