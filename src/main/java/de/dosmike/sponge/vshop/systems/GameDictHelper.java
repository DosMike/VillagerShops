package de.dosmike.sponge.vshop.systems;

import org.spongepowered.api.GameDictionary;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.item.inventory.ItemStack;

import java.util.*;
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
    private static final SetSetMapping<String, GameDictionary.Entry> cache = new SetSetMapping<>();

    static final GameDictionary dict = Sponge.getDictionary().orElse(null);
    public static boolean hasGameDict() {
        return dict != null;
    }
    static {
        if (dict != null) {
            for (Map.Entry<String, Collection<GameDictionary.Entry>> e : dict.getAll().asMap().entrySet()) {
                //make words
                Set<String> words = new HashSet<>();
                String s = e.getKey();
                words.add(s); //add key itself
                if (entryPattern.matcher(s).matches()) {
                    Matcher wordMatcher = wordPattern.matcher(s);
                    String part = "";
                    while (wordMatcher.find()) {
                        part += wordMatcher.group();
                        words.add(part); //add partial as new word, e.g. ore from oreGold
                    }
                }
                // link all these words to this set of entries
                cache.add(words, e.getValue());
            }
        }
    }
    /**
     * This function is not performing a word-boundary adjusted entry.contains(key), but only checks for entry.startsWith(key).<br>
     * While this does not allow for searching "gold" -> [oreGold, ingotGold] it simplifies the implementation a lot
     */
    public static List<GameDictionary.Entry> getAll(String key) {
        return new LinkedList<>(cache.getValues(key));
    }
    /**
     * This function will return the full entries along prefix matches, adding up all words up to the full entry.
     * The result for oreGold would be [ore, oreGold].<br>
     * While permutations like oreGold -> Gold or tinyPileDiamond -> pileDiamond may be useful they are ignored for now.
     * @see GameDictHelper
     * @return the collection of keys that are valid oreDict entries for this items stack
     */
    public static Collection<String> getKeys(ItemStack item) {
        return cache.findKeys(entry->entry.matches(item));
    }

}
