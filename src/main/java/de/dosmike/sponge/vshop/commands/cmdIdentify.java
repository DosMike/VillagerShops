package de.dosmike.sponge.vshop.commands;

import de.dosmike.sponge.vshop.PermissionRegistra;
import de.dosmike.sponge.vshop.Utilities;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.shops.ShopEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import java.util.Optional;
import java.util.UUID;

public class cmdIdentify extends Command {

    static CommandSpec getCommandSpec() {
        return CommandSpec.builder()
                .permission(PermissionRegistra.IDENTIFY.getId())
                .arguments(
                        GenericArguments.none()
                ).executor(new cmdIdentify()).build();
    }

    @NotNull
    @Override
    public CommandResult execute(@NotNull CommandSource src, @NotNull CommandContext args) throws CommandException {
        if (!(src instanceof Player)) {
            throw new CommandException(localText("cmd.playeronly").resolve(src).orElse(Text.of("[Player only]")));
        }
        Player player = (Player) src;

        Optional<Entity> ent = getEntityLookingAt(player, 5.0);
        Optional<ShopEntity> npc = ent.map(Entity::getUniqueId).flatMap(VillagerShops::getNPCfromEntityUUID);
        if (!npc.isPresent()) {
            throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                    localString("cmd.common.notarget").resolve(player).orElse("[no target]")));
        } else {
            Optional<UUID> owner = npc.get().getShopOwner();
            Optional<Player> powner = owner.flatMap(uuid -> Sponge.getServer().getPlayer(uuid));
            Text.Builder ownername = Text.builder(owner.isPresent()
                    ? (powner.isPresent()
                    ? powner.get().getName()
                    : owner.get().toString())
                    : localString("cmd.identify.adminshop").resolve(player).orElse("[Server]"));
            if (owner.isPresent()) {
                ownername.onHover(TextActions.showText(Text.of("UUID: " + owner.get().toString())));
                ownername.onShiftClick(TextActions.insertText(owner.get().toString()));
            }

            src.sendMessage(Text.of(TextColors.GREEN, "[vShop] ",
                    localText("cmd.identify.response")
                            .replace("\\n", Text.NEW_LINE)
                            .replace("%type%", VillagerShops.getTranslator().localText(npc.get().getShopOwner().isPresent() ? "shop.type.player" : "shop.type.admin"))
                            .replace("%entity%", npc.get().getNpcType().getTranslation().get(Utilities.playerLocale(player)))
                            .replace("%skin%", npc.get().getVariantName())
                            .replace("%name%", npc.get().getDisplayName())
                            .replace("%id%",
                                    Text.builder(npc.get().getIdentifier().toString())
                                            .onShiftClick(TextActions.insertText(npc.get().getIdentifier().toString()))
                                            .onHover(TextActions.showText(localText("cmd.identify.shiftclick").resolve(src).orElse(Text.of("Shift-click"))))
                                            .build())
                            .replace("%owner%", ownername.build())
                            .resolve(player).orElse(Text.of("[much data, such wow]"))));

            VillagerShops.audit("%s identified shop %s",
                    Utilities.toString(src), npc.get().toString() );
            return CommandResult.success();
        }
    }

}
