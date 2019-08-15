package de.dosmike.sponge.vshop.systems;

import org.spongepowered.api.GameDictionary;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * While oredict entries do not follow a syntax, the community established the following standard: typeMaterialDetail<br>
 * * where type can be block, ingot, stick or similar. for unique items like egg, that's already enough<br>
 * * Material is usually a metal type like Iron, Gold but may also be other materials like e.g. Redstone<br>
 * * Detail can be additional information if type and Material are not enough, e.g. Polished (for stone)<br>
 * there are also variants for colors and varieties.<br>
 * more examples: dustRedstone, tinyPileDiamond, chunkCopper<br>
 * <b>Infered types</b> are important for aliasing and searching. the idea is that ore shall match all ores like oreIron oreGold oreCoal.
 * This is usually implemented as substring check so materials can be checked as well.<br>
 */
public class GameDictHelper {

    private static final Pattern entryPattern = Pattern.compile("[a-z]+(?:[A-Z][a-z]+)+");
    private static final Pattern wordPattern = Pattern.compile("((?:^|[A-Z])[a-z]+)");

    static GameDictionary dict = Sponge.getDictionary().orElse(null);
    /**
     * This function is not performing a word-boundary adjusted entry.contains(key), but only checks for entry.startsWith(key).<br>
     * While this does not allow for searching "gold" -> [oreGold, ingotGold] it simplifies the implementation a lot
     */
    public static List<ItemStackSnapshot> getAll(String key) {
        List<ItemStackSnapshot> result = new LinkedList<>();
        if (dict == null) return result;
        for (Map.Entry<String, GameDictionary.Entry> e : dict.getAll().entries()) {
            if (e.getKey().startsWith(key)) {
                result.add(e.getValue().getTemplate());
            }
        }
        return result;
    }
    /**
     * This function will return the full entries along prefix matches, adding up all words up to the full entry.
     * The result for oreGold would be [ore, oreGold].<br>
     * While permutations like oreGold -> Gold or tinyPileDiamond -> pileDiamond may be useful they are ignored for now.
     * @see GameDictHelper
     */
    public static Set<String> getKeys(ItemStack item) {
        Set<String> strings = new HashSet<>();
        if (dict == null) return strings;
        for (Map.Entry<String, GameDictionary.Entry> s : dict.getAll().entries()) {
            if (s.getValue().matches(item)) strings.add(s.getKey());
        }
        //find partial entries. e.g. oreGold is also an entry of ore
        Set<String> refined = new HashSet<>();
        for (String s : strings) {
            refined.add(s);
            if (entryPattern.matcher(s).matches()) {
                Matcher wordMatcher = wordPattern.matcher(s);
                String part = "";
                while (wordMatcher.find()) {
                    part += wordMatcher.group();
                    refined.add(part);
                }
            }
        }
        return refined;
    }
    public static class GameDictPredicate implements Predicate<ItemStack> {
        String key;
        public GameDictPredicate(String key) {
            this.key = key;
        }
        @Override
        public boolean test(ItemStack itemStack) {
            return getKeys(itemStack).contains(key);
        }
    }
    public static boolean hasGameDict() {
        return dict != null;
    }

}
