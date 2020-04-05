package de.dosmike.sponge.vshop.commands;

import com.flowpowered.math.vector.Vector3d;
import de.dosmike.sponge.languageservice.API.Localized;
import de.dosmike.sponge.vshop.VillagerShops;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import java.util.*;

/** more of a wrapper class to provide localText, localString and other helpers */
public abstract class Command implements CommandExecutor {

    static Localized<Text> localText(String key) {
        return VillagerShops.getTranslator().localText(key);
    }
    static Localized<String> localString(String key) {
        return VillagerShops.getTranslator().local(key);
    }

    /**
     * EntityRay :D
     * Don't ask too closely how this works, I wrote this years ago...
     */
    public static Optional<Entity> getEntityLookingAt(Living source, Double maxRange) {
        Collection<Entity> ents = source.getNearbyEntities(maxRange); // get all entities in interaction range
        //we need a facing vector for the source
        Vector3d rot = source.getHeadRotation().mul(Math.PI / 180.0); //to radians
        Vector3d dir = new Vector3d(
                -Math.cos(rot.getX()) * Math.sin(rot.getY()),
                -Math.sin(rot.getX()),
                Math.cos(rot.getX()) * Math.cos(rot.getY())); //should now be a unit vector (len 1)

//		VillagerShops.l("%s\n%s", source.getHeadRotation().toString(), dir.toString());

        //Scanning for a target
        Double dist = 0.0;
        Vector3d src = source.getLocation().getPosition().add(0, 1.62, 0); //about head height
        dir = dir.normalize().div(10); //scan step in times per block
        Double curdist;
        List<Entity> marked;
        Map<Entity, Double> lastDist = new HashMap<>();
        ents.remove(source); //do not return the source ;D
        Vector3d ep; //entity pos
        while (dist < maxRange) {
            if (ents.isEmpty()) break;
//			VillagerShops.l("Scanning %.2f/%.2f @ %.2f %.2f %.2f", dist, maxRange, src.getX(), src.getY(), src.getZ());
            marked = new LinkedList<>();
            for (Entity ent : ents) {
                ep = ent.getLocation().getPosition();
                curdist = Math.min(ep.add(0, 0.5, 0).distanceSquared(src), ep.add(0, 1.5, 0).distanceSquared(src)); //assuming the entity is a humanoid
//				VillagerShops.l("Distance to %s{%s}: %.2f", ent.getType().getName(), ent.getUniqueId().toString(), curdist);
                if (lastDist.containsKey(ent) && lastDist.get(ent) - curdist < 0)
                    marked.add(ent); // entity is moving away from ray pos
                else lastDist.put(ent, curdist);

                if (curdist < 1.44) { //assume height of ~2
                    return Optional.of(ent);
                }
            }
            for (Entity ent : marked) {
//				VillagerShops.l("Dropping %s{%s}", ent.getType().getName(), ent.getUniqueId().toString());
                lastDist.remove(ent);
                ents.remove(ent);
            }
            src = src.add(dir);
            dist += 0.1;
        }
        return Optional.empty();
    }

    /**
     * @throws NumberFormatException if the option has a invalid (non positive integer) value
     */
    public static Optional<Integer> getMaximumStockDistance(Player player) throws NumberFormatException {
        Optional<String> val = player.getOption("vshop.option.chestlink.distance");
        Optional<Integer> res = val
                .filter(s -> s.matches("[1-9][0-9]*"))
                .map(Integer::valueOf);
        if (val.isPresent() && !res.isPresent())
            throw new NumberFormatException();
        return res;
    }

}
