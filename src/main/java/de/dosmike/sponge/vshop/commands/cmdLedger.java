package de.dosmike.sponge.vshop.commands;

import de.dosmike.sponge.vshop.PermissionRegistra;
import de.dosmike.sponge.vshop.Utilities;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.systems.LedgerManager;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;

public class cmdLedger extends Command {

	static CommandSpec getCommandSpec() {
		return CommandSpec.builder()
				.permission(PermissionRegistra.LEDGER_ME.getId())
				.arguments(
						GenericArguments.flags().permissionFlag(PermissionRegistra.LEDGER_OTHERS.getId(), "t")
								.buildWith(GenericArguments.optional(
										GenericArguments.user(Text.of("Target"))
								))
				).executor(new cmdLedger()).build();
	}

	@NotNull
	@Override
	public CommandResult execute(@NotNull CommandSource src, @NotNull CommandContext args) throws CommandException {
		if (args.hasAny("Target") && args.hasAny("t")) {
			throw new CommandException(localText("cmd.ledger.invalid").orLiteral(src));
		} else if (!args.hasAny("Target") && !(src instanceof Player)) {
			throw new CommandException(localText("cmd.missingargument").orLiteral(src));
		} else {
			User target;
			if (src instanceof Player)
				target = (User) args.getOne("Target").orElse(src);
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
	}

}
