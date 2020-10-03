package de.dosmike.sponge.vshop.integrations.protection;

import com.universeguard.region.LocalRegion;
import com.universeguard.region.Region;
import com.universeguard.region.enums.EnumRegionFlag;
import com.universeguard.utils.RegionUtils;
import de.dosmike.sponge.vshop.ConfigSettings;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.UUID;

public class UniverseGuardAccess implements AreaProtection {

    @Override
    public boolean canCreateEntity(Player trigger, Location<World> location, Entity entity) {
        if (!ConfigSettings.isProtectionEnabled()) return true;

        Region region = RegionUtils.getRegion(location);
        boolean allowed;
        if (region.isGlobal()) allowed = ConfigSettings.protectionAllowWilderness();
        else allowed = getRegionAccessLevel((LocalRegion)region, trigger.getUniqueId())
                .compareTo(ConfigSettings.requiredProtectionAccessLevel()) >= 0;
        return allowed && region.getMobSpawn(entity.getType().getId());
    }

    @Override
    public boolean canMoveEntityTo(Player trigger, Location<World> location, Entity entity) {
        if (!ConfigSettings.isProtectionEnabled()) return true;

        Region region = RegionUtils.getRegion(location);
        boolean allowed;
        if (region.isGlobal()) allowed = ConfigSettings.protectionAllowWilderness();
        else allowed = getRegionAccessLevel((LocalRegion)region, trigger.getUniqueId())
                .compareTo(ConfigSettings.requiredProtectionAccessLevel()) >= 0;
        // mob spawn is the flag to check if mobs are allowed in this region
        return allowed && region.getMobSpawn(entity.getType().getId());
    }

    @Override
    public boolean canAccessContainer(Player trigger, Location<World> location) {
        if (!ConfigSettings.isProtectionEnabled()) return true;

        Region region = RegionUtils.getRegion(location);
        boolean allowed;
        if (region.isGlobal()) allowed = ConfigSettings.protectionAllowWilderness();
        else allowed = getRegionAccessLevel((LocalRegion)region, trigger.getUniqueId())
                .compareTo(ConfigSettings.requiredProtectionAccessLevel()) >= 0;
        return allowed && region.getFlag(EnumRegionFlag.CHESTS);
    }

    private ProtectionAccessLevel getRegionAccessLevel(LocalRegion region, UUID player) {
        if (region.getOwner().getUUID().equals(player))
            return ProtectionAccessLevel.MODERATIVE;
        if (region.getMembers().stream().anyMatch(m->m.getUUID().equals(player)))
            return ProtectionAccessLevel.MEMBER;
        if (region.getFlag(EnumRegionFlag.CHESTS))
            return ProtectionAccessLevel.CONTAINER;
        return ProtectionAccessLevel.IGNORED;
    }

}
