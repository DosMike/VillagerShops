package de.dosmike.sponge.vshop.integrations.protection;

import com.universeguard.region.GlobalRegion;
import com.universeguard.region.LocalRegion;
import com.universeguard.region.Region;
import com.universeguard.region.enums.EnumRegionFlag;
import com.universeguard.region.enums.EnumRegionInteract;
import com.universeguard.utils.PermissionUtils;
import com.universeguard.utils.RegionRoleUtils;
import com.universeguard.utils.RegionUtils;
import de.dosmike.sponge.vshop.ConfigSettings;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.UUID;

public class UniverseGuardAccess implements ClaimAccess {

    @Override
    public boolean canCreateEntity(Player trigger, Location<World> location, Entity entity) {
        Region region = RegionUtils.getRegion(location);
        boolean allowed;
        if (region.isGlobal()) allowed = ConfigSettings.claimsInWilderness();
        else allowed = getRegionAccessLevel((LocalRegion)region, trigger.getUniqueId())
                .compareTo(ConfigSettings.requiredClaimAccessLevel()) >= 0;
        return allowed && region.getMobSpawn(entity.getType().getId());
    }

    @Override
    public boolean canMoveEntityTo(Player trigger, Location<World> location, Entity entity) {
        Region region = RegionUtils.getRegion(location);
        boolean allowed;
        if (region.isGlobal()) allowed = ConfigSettings.claimsInWilderness();
        else allowed = getRegionAccessLevel((LocalRegion)region, trigger.getUniqueId())
                .compareTo(ConfigSettings.requiredClaimAccessLevel()) >= 0;
        // mob spawn is the flag to check if mobs are allowed in this region
        return allowed && region.getMobSpawn(entity.getType().getId());
    }

    @Override
    public boolean canAccessContainer(Player trigger, Location<World> location) {
        Region region = RegionUtils.getRegion(location);
        boolean allowed;
        if (region.isGlobal()) allowed = ConfigSettings.claimsInWilderness();
        else allowed = getRegionAccessLevel((LocalRegion)region, trigger.getUniqueId())
                .compareTo(ConfigSettings.requiredClaimAccessLevel()) >= 0;
        return allowed && region.getFlag(EnumRegionFlag.CHESTS);
    }

    private ClaimAccessLevel getRegionAccessLevel(LocalRegion region, UUID player) {
        if (region.getOwner().getUUID().equals(player))
            return ClaimAccessLevel.OWNER;
        if (region.getMembers().stream().anyMatch(m->m.getUUID().equals(player)))
            return ClaimAccessLevel.BUILDER;
        if (region.getFlag(EnumRegionFlag.CHESTS))
            return ClaimAccessLevel.CONTAINER;
        return ClaimAccessLevel.IGNORED;
    }

}
