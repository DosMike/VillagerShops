package de.dosmike.sponge.vshop.commands;

import de.dosmike.sponge.vshop.PermissionRegistra;
import de.dosmike.sponge.vshop.Utilities;
import de.dosmike.sponge.vshop.VillagerShops;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class cmdSave extends Command {

	static CommandSpec getCommandSpec() {
		return CommandSpec.builder()
				.permission(PermissionRegistra.ADMIN.getId())
				.arguments(
						GenericArguments.none()
				).executor(new cmdSave()).build();
	}

	@NotNull
	@Override
	public CommandResult execute(@NotNull CommandSource src, @NotNull CommandContext args) throws CommandException {
		VillagerShops.getInstance().saveConfigs();
		src.sendMessage(Text.of(TextColors.GREEN, "[vShop] ",
				localString("cmd.saved").orLiteral(src)));
		VillagerShops.audit("%s saved the shops", Utilities.toString(src));
		return CommandResult.success();
	}

}
