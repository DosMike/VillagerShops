package de.dosmike.sponge.vshop.integrations.protection;

import br.net.fabiozumbi12.RedProtect.Sponge.RedProtect;
import br.net.fabiozumbi12.RedProtect.Sponge.Region;
import de.dosmike.sponge.vshop.ConfigSettings;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class RedProtectAccess implements AreaProtection {

    @Override
    public boolean canCreateEntity(Player trigger, Location<World> location, Entity entity) {
        Region region = RedProtect.get().getAPI().getRegion(location);
        if (region == null) { //global
            return ConfigSettings.protectionAllowWilderness();
        }
        return getRegionAccessLevel(region, trigger)
                .compareTo(ConfigSettings.requiredProtectionAccessLevel()) >= 0;
        //can't check for specific entity types with rp, only categories -> use permissions
    }

    @Override
    public boolean canMoveEntityTo(Player trigger, Location<World> location, Entity entity) {
        Region region = RedProtect.get().getAPI().getRegion(location);
        if (region == null) { //global
            return ConfigSettings.protectionAllowWilderness();
        }
        return getRegionAccessLevel(region, trigger)
                .compareTo(ConfigSettings.requiredProtectionAccessLevel()) >= 0;
        //can't check for specific entity types with rp, only categories -> use permissions
    }

    @Override
    public boolean canAccessContainer(Player trigger, Location<World> location) {
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
