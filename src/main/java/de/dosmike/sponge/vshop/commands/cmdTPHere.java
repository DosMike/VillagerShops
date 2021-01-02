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
			throw new CommandException(localText("cmd.playeronly").orLiteral(src));
		}
		Player player = (Player) src;

		Optional<ShopEntity> optionalShopEntity = VillagerShops.getShopFromShopId(args.<UUID>getOne("shopid").get());
		if (!optionalShopEntity.isPresent()) {
			src.sendMessage(localText("cmd.common.noshopforid").orLiteral(src));
		} else {
			if (!PermissionRegistra.ADMIN.hasPermission(player) &&
					!optionalShopEntity.get().isShopOwner(player.getUniqueId())) {
				throw new CommandException(Text.of(TextColors.RED,
						localString("permission.missing").orLiteral(player)));
			}
			Optional<Integer> distance = Optional.empty();
			if (optionalShopEntity.get().getShopOwner().isPresent()) try {
				distance = getMaximumStockDistance(player);
			} catch (NumberFormatException nfe) {
				throw new CommandException(localText("option.invalidvalue")
						.replace("%option%", "vshop.option.chestlink.distance")
						.replace("%player%", player.getName())
						.orLiteral(player)
				);
			}
			ShopEntity shopEntity = optionalShopEntity.get();
			Location<World> destination = player.getLocation();
			if (distance.isPresent() && shopEntity.getStockContainer().isPresent() && (
					!destination.getExtent().equals(shopEntity.getStockContainer().get().getExtent()) ||
							destination.getPosition().distance(shopEntity.getStockContainer().get().getPosition()) > distance.get()))
				throw new CommandException(localText("cmd.link.distance")
						.replace("%distance%", distance.get())
						.orLiteral(player));

			if (shopEntity.getShopOwner().isPresent() && //player shop
					!VillagerShops.getProtection().hasAccess(player, player.getLocation())) { //not checking for owner allows admins to move shops for you
				throw new CommandException(Text.of(TextColors.RED,
						localString("permission.missing").orLiteral(player)));
			}

			VillagerShops.audit("%s relocated shop %s to %s°%.2f, %d blocks",
					Utilities.toString(src), shopEntity.toString(),
					Utilities.toString(destination), player.getHeadRotation().getY(),
					distance.orElse(-1)
			);
			VillagerShops.closeShopInventories(shopEntity.getIdentifier());
			shopEntity.move(new Location<>(destination.getExtent(), destination.getBlockX() + 0.5, destination.getY(), destination.getBlockZ() + 0.5));
			shopEntity.setRotation(new Vector3d(0.0, player.getHeadRotation().getY(), 0.0));
		}
		return CommandResult.success();
	}

}
