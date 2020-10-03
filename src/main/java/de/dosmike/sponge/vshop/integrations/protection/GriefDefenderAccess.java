package de.dosmike.sponge.vshop.integrations.protection;

import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.permission.flag.Flags;
import de.dosmike.sponge.vshop.ConfigSettings;
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
public class GriefDefenderAccess implements ClaimAccess {

    public GriefDefenderAccess() {
        VillagerShops.l("[Integration] Protection Plugin: GriefDefender");
    }

    @Override
    public boolean canCreateEntity(Player trigger, Location<World> location, Entity entity) {
        if (!ConfigSettings.isClaimsEnabled()) return true;

        Claim claim = GriefDefender.getCore()
                .getClaimManager(location.getExtent().getUniqueId())
                .getClaimAt(location.getBlockPosition());
        if (claim.isWilderness())
            return ConfigSettings.claimsInWilderness();
        boolean allowed = getClaimAccessLevel(claim, trigger.getUniqueId())
                .compareTo(ConfigSettings.requiredClaimAccessLevel()) >= 0;
        return allowed && claim.getActiveFlagPermissionValue(
                Flags.ENTITY_SPAWN,
                GriefDefender.getCore().getUser(trigger.getUniqueId()),
                null,
                //from what i can see DG only checks type, but requires an entity as target for ENTITY_SPAWN
                entity,
                //I don't know wth GD wants/requires as context here
                new HashSet<>()
        ).asBoolean();
    }

    @Override
    public boolean canMoveEntityTo(Player trigger, Location<World> location, Entity entity) {
        if (!ConfigSettings.isClaimsEnabled()) return true;

        Claim claim = GriefDefender.getCore()
                .getClaimManager(location.getExtent().getUniqueId())
                .getClaimAt(location.getBlockPosition());
        if (claim.isWilderness())
            return ConfigSettings.claimsInWilderness();
        boolean allowed = getClaimAccessLevel(claim, trigger.getUniqueId())
                .compareTo(ConfigSettings.requiredClaimAccessLevel()) >= 0;
        return allowed && claim.getActiveFlagPermissionValue(
                Flags.ENTITY_TELEPORT_TO,
                GriefDefender.getCore().getUser(trigger.getUniqueId()),
                null,
                //from what i can see DG only checks type, but requires an entity as target for ENTITY_SPAWN
                entity,
                //I don't know wth GD wants/requires as context here
                new HashSet<>()
        ).asBoolean();
    }

    @Override
    public boolean canAccessContainer(Player trigger, Location<World> location) {
        if (!ConfigSettings.isClaimsEnabled()) return true;

        Claim claim = GriefDefender.getCore()
                .getClaimManager(location.getExtent().getUniqueId())
                .getClaimAt(location.getBlockPosition());
        if (claim.isWilderness())
            return ConfigSettings.claimsInWilderness();
        boolean allowed = getClaimAccessLevel(claim, trigger.getUniqueId())
                .compareTo(ConfigSettings.requiredClaimAccessLevel()) >= 0;
        return allowed && claim.getActiveFlagPermissionValue(
                Flags.INTERACT_INVENTORY,
                GriefDefender.getCore().getUser(trigger.getUniqueId()),
                //I don't know wth GD wants/requires as context here
                new HashSet<>()
        ).asBoolean();
    }

    private ClaimAccessLevel getClaimAccessLevel(Claim claim, UUID playerID) {
        if (claim.isUserTrusted(playerID, TrustTypes.MANAGER)) return ClaimAccessLevel.OWNER;
        if (claim.isUserTrusted(playerID, TrustTypes.BUILDER)) return ClaimAccessLevel.BUILDER;
        if (claim.isUserTrusted(playerID, TrustTypes.CONTAINER)) return ClaimAccessLevel.CONTAINER;
        return ClaimAccessLevel.IGNORED;
    }
}
