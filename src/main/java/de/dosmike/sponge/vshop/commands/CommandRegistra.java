package de.dosmike.sponge.vshop.commands;

import de.dosmike.sponge.languageservice.API.PluginTranslation;
import de.dosmike.sponge.vshop.VillagerShops;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;

import java.util.*;

public class CommandRegistra {
	static PluginTranslation lang;

	private static CommandMapping unregRef;

	public static void register() {
		lang = VillagerShops.getTranslator();
		Map<List<String>, CommandSpec> children;

		children = new HashMap<>();
		children.put(Collections.singletonList("create"), cmdCreate.getCommandSpec());
		children.put(Arrays.asList("import", "adapt"), cmdImport.getCommandSpec());
		children.put(Collections.singletonList("add"), cmdAdd.getCommandSpec());
		children.put(Collections.singletonList("remove"), cmdRemove.getCommandSpec());
		children.put(Arrays.asList("delete", "exterminate"), cmdDelete.getCommandSpec());
		children.put(Arrays.asList("free", "release"), cmdRelease.getCommandSpec());
		children.put(Collections.singletonList("save"), cmdSave.getCommandSpec());
		children.put(Collections.singletonList("reload"), cmdReload.getCommandSpec());
		children.put(Arrays.asList("list", "get", "for"), cmdList.getCommandSpec());
		children.put(Arrays.asList("identify", "id"), cmdIdentify.getCommandSpec());
		children.put(Collections.singletonList("link"), cmdLink.getCommandSpec());
		children.put(Collections.singletonList("tphere"), cmdTPHere.getCommandSpec());
		children.put(Arrays.asList("ledger", "log"), cmdLedger.getCommandSpec());

		unregRef = Sponge.getCommandManager().register(VillagerShops.getInstance(), CommandSpec.builder()
						.description(Text.of(lang.local("cmd.description.short").toString()))
						.extendedDescription(Text.of(lang.local("cmd.description.long").replace("\\n", "\n").toString()))
						.children(children)
						.build()
				, "vshop").orElse(null);
	}

	static void remove() {
		if (unregRef != null) {
			Sponge.getCommandManager().removeMapping(unregRef);
			unregRef = null;
		}
	}

}
