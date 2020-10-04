package de.dosmike.sponge.vshop.integrations.protection;

import de.dosmike.sponge.vshop.ConfigSettings;
import de.dosmike.sponge.vshop.VillagerShops;
import org.spongepowered.api.GameState;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

/**
 * This is the general protection class, checking if a player is allowed to place shops
 * in a certain are. This module can be disabled in the configuration because it might
 * not be wanted.<br>
 * Implement this class once per protection plugin and append it to #get()
 */
public interface AreaProtection {

    default boolean hasAccess(Player trigger, Location<World> location) {
        return true;
    }

    /**
     * Core to super optional dependency:<br>
     * Tries to get the ClaimAccess instance from the classloader.
     * On failure it creates a new Default provider.
     * <br><br>
     * Doing it this way makes this class not quite know about the dependency.
     * And the ClassLoader will not attempt to look into the dependency unless the wrapper
     * is constructed for the first time.
     * <br><br>
     * This method should only be called once in {@link GameInitializationEvent}
     */
    static AreaProtection get() {
        assert Sponge.getGame().getState().ordinal() >= GameState.INITIALIZATION.ordinal()
                : "Dependent services might not be registered yet!";

        final AreaProtection DEFAULT_ACCESS = new AreaProtection(){};

        if (!ConfigSettings.isProtectionEnabled()) return DEFAULT_ACCESS;

        //region GriefDefender
        try {
            if (Sponge.getPluginManager().getPlugin("griefdefender").isPresent()) {
                return new GriefDefenderAccess();
            }
        } catch (Throwable dependencyError) {
            VillagerShops.w("Claim Access Integration failed: Could not load handler for GriefDefender");
        }
        //endregion
        //region UniverseGuard
        try {
            if (Sponge.getPluginManager().getPlugin("universeguard").isPresent()) {
                return new UniverseGuardAccess();
            }
        } catch (Throwable dependencyError) {
            VillagerShops.w("Claim Access Integration failed: Could not load handler for UniverseGuard");
        }
        //endregion
        //region RedProtect
        try {
            if (Sponge.getPluginManager().getPlugin("redprotect").isPresent()) {
                return new RedProtectAccess();
            }
        } catch (Throwable dependencyError) {
            VillagerShops.w("Claim Access Integration failed: Could not load handler for RedProtect");
        }
        //endregion

        return DEFAULT_ACCESS;
    }

}
