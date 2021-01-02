package de.dosmike.sponge.vshop.commands;

import de.dosmike.sponge.vshop.PermissionRegistra;
import de.dosmike.sponge.vshop.Utilities;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.systems.TranslationLoader;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;

public class cmdReload extends Command {

	static CommandSpec getCommandSpec() {
		return CommandSpec.builder()
				.permission(PermissionRegistra.ADMIN.getId())
				.arguments(
						GenericArguments.flags().flag("-translations").buildWith(
								GenericArguments.none()
						)
				).executor(new cmdReload()).build();
	}

	@NotNull
	@Override
	public CommandResult execute(@NotNull CommandSource src, @NotNull CommandContext args) throws CommandException {
		VillagerShops.getInstance().loadConfigs();
		VillagerShops.updateCratePlugins();
		TranslationLoader.fetchTranslations(args.hasAny("translations"));
		src.sendMessage(Text.of("Reload complete"));
		VillagerShops.audit("%s reloaded the settings", Utilities.toString(src));
		return CommandResult.success();
	}

}
