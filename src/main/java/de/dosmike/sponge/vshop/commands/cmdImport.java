package de.dosmike.sponge.vshop.commands;

import com.flowpowered.math.vector.Vector3d;
import de.dosmike.sponge.vshop.PermissionRegistra;
import de.dosmike.sponge.vshop.Utilities;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.menus.ShopMenuManager;
import de.dosmike.sponge.vshop.shops.FieldResolver;
import de.dosmike.sponge.vshop.shops.ShopEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;
import java.util.UUID;

public class cmdImport extends Command {

    static CommandSpec getCommandSpec() {
        return CommandSpec.builder()
                .permission(PermissionRegistra.IMPORT.getId())
                .arguments(GenericArguments.none()).executor(new cmdImport()).build();
    }

    @NotNull
    @Override
    public CommandResult execute(@NotNull CommandSource src, @NotNull CommandContext args) throws CommandException {

        if (!(src instanceof Player)) {
            throw new CommandException(localText("cmd.playeronly").orLiteral(src));
        }
        Player player = (Player) src;

        if (!PermissionRegistra.ADMIN.hasPermission(player) &&
                !PermissionRegistra.PLAYER.hasPermission(player)) {
            throw new CommandException(Text.of(TextColors.RED,
                    localString("permission.missing").orLiteral(player)));
        }
        boolean asAdminShop = PermissionRegistra.ADMIN.hasPermission(player);

        if (!asAdminShop) {
            Optional<String> option = player.getOption("vshop.option.playershop.limit");
            int limit = -1;
            try {
                limit = Integer.parseInt(option.orElse("-1"));
            } catch (Exception e) {/**/}
            if (limit >= 0) {
                int cnt = 0;
                UUID pid = player.getUniqueId();
                for (ShopEntity npc : VillagerShops.getShops())
                    if (npc.isShopOwner(pid)) cnt++;

                if (cnt >= limit) {
                    throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                            localString("cmd.create.playershop.limit").replace("%limit%", limit).orLiteral(player)));
                }
            }
        }

        Entity entity = getEntityLookingAt(player, 5.0).orElseThrow(()->
                new CommandException(localText("cmd.import.notarget").orLiteral(player))
        );
        Location<World> findAt = entity.getLocation();
        double rotateYaw = ((entity instanceof Living) ? ((Living) entity).getHeadRotation() : entity.getRotation()).getY();

        if (VillagerShops.isLocationOccupied(findAt)) {
            throw new CommandException(Text.of(TextColors.RED, "[vshop] ", localString("cmd.import.occupied").orLiteral(player)));
        }

        String skinVariant;
        if (entity.getType().equals(EntityTypes.HORSE)) {
            skinVariant = FieldResolver.HORSE_VARIANT.getVariant(entity);
        } else if (entity.getType().equals(EntityTypes.LLAMA)) {
            skinVariant = FieldResolver.LLAMA_VARIANT.getVariant(entity);
        } else if (entity.getType().equals(EntityTypes.OCELOT)) {
            skinVariant = FieldResolver.OCELOT_VARIANT.getVariant(entity);
        } else if (entity.getType().equals(EntityTypes.PARROT)) {
            skinVariant = FieldResolver.PARROT_VARIANT.getVariant(entity);
        } else if (entity.getType().equals(EntityTypes.RABBIT)) {
            skinVariant = FieldResolver.RABBIT_VARIANT.getVariant(entity);
        } else if (entity.getType().equals(EntityTypes.VILLAGER)) {
            skinVariant = FieldResolver.VILLAGER_VARIANT.getVariant(entity);
        } else {
            skinVariant = "none";
        }

        Text displayName = entity.get(Keys.DISPLAY_NAME).orElse(Text.of("VillagerShop"));

        String entityPermission = entity.getType().getId();
        entityPermission = "vshop.create." + entityPermission.replace(':', '.').replace("_", "").replace("-", "");
        if (!player.hasPermission(entityPermission)) {
            throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                    localString("cmd.import.entitypermission").replace("%permission%", entityPermission).orLiteral(player)));
        }

        ShopEntity shopEntity = new ShopEntity(UUID.randomUUID());
        ShopMenuManager menu = new ShopMenuManager();
        shopEntity.setNpcType(entity.getType());
        shopEntity.setVariant(skinVariant);
        shopEntity.setDisplayName(displayName);
        shopEntity.setMenu(menu);
        shopEntity.setLocation(findAt);
        shopEntity.setRotation(new Vector3d(0, rotateYaw, 0));
        boolean playershop = false;
        try {
            shopEntity.setPlayerShop(player.getUniqueId());
            playershop = true;
        } catch (Exception e) {
            if (!asAdminShop) {
                throw new CommandException(Text.of(TextColors.RED, "[vShop] ",
                        localString("cmd.create.playershop.missingcontainer").orLiteral(player)));
            }
        }
        entity.offer(Keys.AI_ENABLED, false);
        entity.offer(Keys.IS_SILENT, true);
        entity.offer(Keys.INVULNERABLE, true);
        VillagerShops.addShop(shopEntity);

        src.sendMessage(Text.of(TextColors.GREEN, "[vShop] ",
                localText(playershop ? "cmd.import.playershop.success" : "cmd.import.success").replace("%name%", Text.of(TextColors.RESET, displayName)).orLiteral(player)));

        VillagerShops.audit("%s imported a shop %s", Utilities.toString(src), shopEntity.toString());
        return CommandResult.success();
    }

}
