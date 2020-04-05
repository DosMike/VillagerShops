package de.dosmike.sponge.vshop.commands;

import com.flowpowered.math.vector.Vector3d;
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
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;
import java.util.UUID;

public class cmdTPHere extends Command {

    static CommandSpec getCommandSpec() {
        return CommandSpec.builder()
                .permission(PermissionRegistra.MOVE.getId())
                .arguments(
                        GenericArguments.uuid(Text.of("shopid"))
                ).executor(new cmdTPHere()).build();
    }

    @NotNull
    @Override
    public CommandResult execute(@NotNull CommandSource src, @NotNull CommandContext args) throws CommandException {
        if (!(src instanceof Player)) {
            throw new CommandException(localText("cmd.playeronly").resolve(src).orElse(Text.of("[Player only]")));
        }
        Player player = (Player) src;

        Optional<ShopEntity> npc = VillagerShops.getNPCfromShopUUID(args.<UUID>getOne("shopid").get());
        if (!npc.isPresent()) {
            src.sendMessage(localText("cmd.common.noshopforid").resolve(src).orElse(Text.of("[Shop not found]")));
        } else {
            if (!PermissionRegistra.ADMIN.hasPermission(player) &&
                    !npc.get().isShopOwner(player.getUniqueId())) {
                throw new CommandException(Text.of(TextColors.RED,
                        localString("permission.missing").resolve(player).orElse("[permission missing]")));
            }
            Optional<Integer> distance = Optional.empty();
            if (npc.get().getShopOwner().isPresent()) try {
                distance = getMaximumStockDistance(player);
            } catch (NumberFormatException nfe) {
                throw new CommandException(localText("option.invalidvalue")
                        .replace("%option%", "vshop.option.chestlink.distance")
                        .replace("%player%", player.getName())
                        .resolve(player)
                        .orElse(Text.of("[option value invalid]"))
                );
            }
            ShopEntity guard = npc.get();
            Location<World> to = player.getLocation();
            if (distance.isPresent() && guard.getStockContainer().isPresent() && (
                    !to.getExtent().equals(guard.getStockContainer().get().getExtent()) ||
                            to.getPosition().distance(guard.getStockContainer().get().getPosition()) > distance.get()))
                throw new CommandException(localText("cmd.link.distance")
                        .replace("%distance%", distance.get())
                        .resolve(player)
                        .orElse(Text.of("[too far away]")));

            VillagerShops.audit("%s relocated shop %s to %s°%.2f, %d blocks",
                    Utilities.toString(src), guard.toString(),
                    Utilities.toString(to), player.getHeadRotation().getY(),
                    distance.orElse(-1)
            );
            VillagerShops.closeShopInventories(guard.getIdentifier());
            guard.move(new Location<>(to.getExtent(), to.getBlockX() + 0.5, to.getY(), to.getBlockZ() + 0.5));
            guard.setRotation(new Vector3d(0.0, player.getHeadRotation().getY(), 0.0));
        }
        return CommandResult.success();
    }

}
