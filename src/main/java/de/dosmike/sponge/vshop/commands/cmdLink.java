package de.dosmike.sponge.vshop.commands;

import de.dosmike.sponge.vshop.PermissionRegistra;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.shops.ShopEntity;
import de.dosmike.sponge.vshop.systems.ChestLinkManager;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;

import java.util.Optional;

public class cmdLink extends Command {

    static CommandSpec getCommandSpec() {
        return CommandSpec.builder()
                .permission(PermissionRegistra.LINKCHEST.getId())
                .arguments(
                        GenericArguments.none()
                ).executor(new cmdLink()).build();
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
        ChestLinkManager.toggleLinker(player, shopEntity.orElse(null));

        return CommandResult.success();
    }

}
