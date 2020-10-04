package de.dosmike.sponge.vshop.integrations.protection;

import br.net.fabiozumbi12.RedProtect.Sponge.RedProtect;
import br.net.fabiozumbi12.RedProtect.Sponge.Region;
import de.dosmike.sponge.vshop.ConfigSettings;
import de.dosmike.sponge.vshop.PermissionRegistra;
import de.dosmike.sponge.vshop.VillagerShops;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class RedProtectAccess implements AreaProtection {

    public RedProtectAccess() {
        VillagerShops.l("[Integration] Protection Plugin: RedProtect");
    }

    @Override
    public boolean hasAccess(Player trigger, Location<World> location) {
        if (!ConfigSettings.isProtectionEnabled() || PermissionRegistra.ADMIN.hasPermission(trigger)) return true;

        Region region = RedProtect.get().getAPI().getRegion(location);
        if (region == null) { //global
            return ConfigSettings.protectionAllowWilderness();
        }
        boolean allow = getRegionAccessLevel(region, trigger)
                .compareTo(ConfigSettings.requiredProtectionAccessLevel()) >= 0;
        return allow && region.canChest(trigger);
    }

    public ProtectionAccessLevel getRegionAccessLevel(Region region, Player player) {
        if (region.isAdmin(player)||region.isLeader(player)) return ProtectionAccessLevel.MODERATIVE;
        if (region.isMember(player)) return ProtectionAccessLevel.MEMBER;
        if (region.canChest(player)) return ProtectionAccessLevel.CONTAINER;
        return ProtectionAccessLevel.IGNORED;
    }
}
