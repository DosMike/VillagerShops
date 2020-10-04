package de.dosmike.sponge.vshop.integrations.protection;

import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.permission.flag.Flags;
import de.dosmike.sponge.vshop.ConfigSettings;
import de.dosmike.sponge.vshop.PermissionRegistra;
import de.dosmike.sponge.vshop.VillagerShops;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashSet;
import java.util.UUID;

/**
 * https://github.com/bloodmc/GriefDefender/blob/0819a53e7126aae9df8a510e11a53cad0038ad8f/sponge/src/main/java/com/griefdefender/permission/GDPermissionManager.java#L224
 */
public class GriefDefenderAccess implements AreaProtection {

    public GriefDefenderAccess() {
        VillagerShops.l("[Integration] Protection Plugin: GriefDefender");
    }

    @Override
    public boolean hasAccess(Player trigger, Location<World> location) {
        if (!ConfigSettings.isProtectionEnabled() || PermissionRegistra.ADMIN.hasPermission(trigger)) return true;

        Claim claim = GriefDefender.getCore()
                .getClaimManager(location.getExtent().getUniqueId())
                .getClaimAt(location.getBlockPosition());
        boolean allowed = (claim.isWilderness())
                ? ConfigSettings.protectionAllowWilderness()
                : getClaimAccessLevel(claim, trigger.getUniqueId())
                    .compareTo(ConfigSettings.requiredProtectionAccessLevel()) >= 0;
        return allowed;
    }

    private ProtectionAccessLevel getClaimAccessLevel(Claim claim, UUID playerID) {
        if (claim.isUserTrusted(playerID, TrustTypes.MANAGER)) return ProtectionAccessLevel.MODERATIVE;
        if (claim.isUserTrusted(playerID, TrustTypes.BUILDER)) return ProtectionAccessLevel.MEMBER;
        if (claim.isUserTrusted(playerID, TrustTypes.CONTAINER)) return ProtectionAccessLevel.CONTAINER;
        return ProtectionAccessLevel.IGNORED;
    }
}
