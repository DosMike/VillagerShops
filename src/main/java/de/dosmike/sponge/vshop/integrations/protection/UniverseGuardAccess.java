package de.dosmike.sponge.vshop.integrations.protection;

import com.universeguard.region.LocalRegion;
import com.universeguard.region.Region;
import com.universeguard.region.enums.EnumRegionFlag;
import com.universeguard.region.enums.RegionPermission;
import com.universeguard.utils.PermissionUtils;
import com.universeguard.utils.RegionUtils;
import de.dosmike.sponge.vshop.ConfigSettings;
import de.dosmike.sponge.vshop.PermissionRegistra;
import de.dosmike.sponge.vshop.VillagerShops;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class UniverseGuardAccess implements AreaProtection {

	public UniverseGuardAccess() {
		VillagerShops.l("[Integration] Protection Plugin: UniverseGuard");
	}

	@Override
	public boolean hasAccess(Player trigger, Location<World> location) {
		if (!ConfigSettings.isProtectionEnabled() || PermissionRegistra.ADMIN.hasPermission(trigger)) return true;

		Region region = RegionUtils.getRegion(location);
		if (region.isGlobal())
			return ConfigSettings.protectionAllowWilderness();
		else
			return getRegionAccessLevel((LocalRegion) region, trigger)
					.compareTo(ConfigSettings.requiredProtectionAccessLevel()) >= 0;
	}

	private ProtectionAccessLevel getRegionAccessLevel(LocalRegion region, Player player) {
		if (PermissionUtils.hasPermission(player, RegionPermission.REGION) ||
				(region.getOwner() != null && player.getUniqueId().equals(region.getOwner().getUUID())))
			return ProtectionAccessLevel.MODERATIVE;
		if (region.getMembers().stream().anyMatch(m -> m.getUUID().equals(player.getUniqueId())))
			return ProtectionAccessLevel.MEMBER;
		if (region.getFlag(EnumRegionFlag.CHESTS))
			return ProtectionAccessLevel.CONTAINER;
		return ProtectionAccessLevel.IGNORED;
	}

}
