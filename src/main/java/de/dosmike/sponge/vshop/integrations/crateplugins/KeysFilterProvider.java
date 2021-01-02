package de.dosmike.sponge.vshop.integrations.crateplugins;

import de.dosmike.sponge.vshop.systems.PluginItemService;
import org.spongepowered.api.GameState;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.game.state.GameInitializationEvent;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public interface KeysFilterProvider {

    default void updateFilters(PluginItemService pis) {

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
    static Collection<KeysFilterProvider> scan() {
        assert Sponge.getGame().getState().ordinal() >= GameState.INITIALIZATION.ordinal()
                : "Dependent services might not be registered yet!";

        Set<KeysFilterProvider> kfp = new HashSet<>();

//        try {
//            Class.forName("Husky Crates main class");
//            kfp.add(new HuskyCrateKeys());
//        } catch (Throwable dependencyError) {
//
//        }

        return kfp;
    }

}
