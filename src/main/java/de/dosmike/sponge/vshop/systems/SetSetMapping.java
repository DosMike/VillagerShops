package de.dosmike.sponge.vshop.systems;

import java.util.*;
import java.util.function.Predicate;

/** links sets of keys to sets of values.
 * Multiple keys can point to sets of shared values.<br>
 * Animal example:<br>
 * canine -> dog<br>
 * canine -> wolf<br>
 * canine -> fox<br>
 * feline -> cat<br>
 * feline -> lion<br>
 * feline -> tiger<br>
 * pets -> dog<br>
 * pets -> cat<br>
 * Where each key and value is only referenced once in memory.<br>
 * The goal is to reduce lookup time for complex object relations
 */
public class SetSetMapping<K,V> {

    private final Map<K, Set<Integer>> keys = new HashMap<>(); //key -> indices mapping
    private final List<Set<K>> reverseKeySet = new LinkedList<>(); //index -> keys mapping
    private final List<V> values = new LinkedList<>(); //index -> value mapping

    public void add(Collection<K> keySet, Collection<V> valueSet) {
        if (keySet == null) throw new IllegalArgumentException("KeySet can't be null");
        if (keySet.contains(null)) throw new IllegalArgumentException("KeySet can't contain null");
        if (valueSet == null) throw new IllegalArgumentException("ValueSet can't be null");
        if (valueSet.contains(null)) throw new IllegalArgumentException("ValueSet can't contain null");

        Set<K> k = new HashSet<>(keySet);
        Set<V> v = new HashSet<>(valueSet);
        for (V value : v) {
            int where = values.indexOf(value);
            Set<K> rkeys;
            if (where < 0) {
                where = values.size();
                values.add(value);
                reverseKeySet.add(rkeys = new HashSet<>());
            } else {
                rkeys = reverseKeySet.get(where);
            }
            for (K key : k) {
                Set<Integer> indices = keys.getOrDefault(key, new HashSet<>());
                if (indices.isEmpty()) {
                    keys.put(key, indices);
                }
                indices.add(where); // map key -> index
                rkeys.add(key); //add index -> key information
            }
        }
    }

    /** @return a flattened collection of value sets where the key set contains the key */
    public Collection<V> getValues(K key) {
        if (key == null) throw new IllegalArgumentException("Key can't be null");

        Set<V> v = new HashSet<>();
        Set<Integer> indices = keys.get(key);
        if (indices == null || indices.isEmpty()) return v;
        for (int i : indices) v.add(values.get(i));
        return v;
    }

    /** for each value set that contains this value, the keys for that values set will be collected
     * @return a collection of keys that when put into getValues each returns value as part of the resulting collection */
    public Collection<K> findKeys(V value) {
        if (value == null) throw new IllegalArgumentException("Value can't be null");
        Set<Integer> indices = new HashSet<>();
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).equals(value))
                indices.add(i);
        }
        Set<K> k = new HashSet<>();
        for (int i : indices) {
            k.addAll(reverseKeySet.get(i));
        }
        return k;
    }

    /** for each value set that contains this value, the keys for that values set will be collected
     * @return a collection of keys that when put into getValues each returns value as part of the resulting collection */
    public Collection<K> findKeys(Predicate<V> valuePredicate) {
        if (valuePredicate == null) throw new IllegalArgumentException("ValuePredicate can't be null");
        Set<Integer> indices = new HashSet<>();
        for (int i = 0; i < values.size(); i++) {
            if (valuePredicate.test(values.get(i))) {
                indices.add(i);
            }
        }
        Set<K> k = new HashSet<>();
        for (int i : indices) {
            k.addAll(reverseKeySet.get(i));
        }
        return k;
    }
}
