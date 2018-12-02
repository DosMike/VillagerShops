package de.dosmike.sponge.vshop;

import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.Career;
import org.spongepowered.api.data.type.Profession;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.User;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FieldResolver {

    private static final String VARIANT_CONCATINATOR = "+";
    private static final Pattern VARIANT_SPLITERATOR = Pattern.compile("[+]");

    private static Map<EntityType, FieldResolver> autoResolverMapping = new HashMap<>();
    public static Optional<KeyAttacher> autoValidate(EntityType entity, String magicString) {
        if (autoResolverMapping.containsKey(entity))
            return Optional.ofNullable(autoResolverMapping.get(entity).validate(magicString));
        else
            return Optional.empty();
    }
    protected static Map<EntityType, Set<String>> autoTabCompleteMapping = new HashMap<>();

    /**
     * Stores Keys and Values to be attached to a Entity at some later point
     */
    public static class KeyAttacher {
        Map<Key, Object> values = new HashMap<>();

        private KeyAttacher(Map<Key, Object> values) {
            this.values.putAll(values);
        }

        public void attach(Entity entity) {
            for (Map.Entry<Key, Object> entry : values.entrySet()) {
                entity.offer(entry.getKey(), entry.getValue())
                        .ifNotSuccessful(() -> new RuntimeException("Key not supported"));
            }
        }

        /**
         * @return a reduced string only containing valid variant names, without duplicates or doubled types
         */
        public String toString() {
            if (values.isEmpty()) return "NONE";
            try {
                return StringUtils.join(values.values().stream().map(o -> {
                    if (o instanceof CatalogType) {
                        return ((CatalogType) o).getId();
                    } else if (o != null)
                        return o.toString();
                    return "NONE";
                }).collect(Collectors.toList()), VARIANT_CONCATINATOR);
            } catch (NullPointerException invalidSkin) {
                /* might need further investigation */
                return "NONE";
            }
        }
    }

    protected Class<CatalogType>[] catalogs;
    protected Key[] keys;

    private FieldResolver(EntityType type, Key... keys) {
        catalogs = new Class[keys.length];
        for (int i = 0; i < keys.length; i++) {
            Class<?> clz = keys[i].getElementToken().getRawType();
//            VillagerShops.l("Target Type: %s, is CT %s",clz.getSimpleName(), CatalogType.class.isAssignableFrom(clz));
            if (CatalogType.class.isAssignableFrom(clz))
                catalogs[i] = (Class<CatalogType>) clz;
            else
                catalogs[i] = null;
        }
        this.keys = keys;
        autoResolverMapping.put(type, this);
        autoTabCompleteMapping.put(type, registerAutoComplete());
    }

    /**
     * @return a KeyAttacher that applies all Variant data to a entity of the given type OR null if the magic string is incompatible
     */
    public KeyAttacher validate(String magicVariant) {
        String[] raw = VARIANT_SPLITERATOR.split(magicVariant);
        CatalogType[] values = new CatalogType[keys.length];
        Arrays.fill(values, null);
        for (String s : raw) {
            for (int i = 0; i < catalogs.length; i++)
                if (catalogs[i] != null) {
                    values[i] = Sponge.getRegistry().getType(catalogs[i], s).orElse(values[i]);
                    //VillagerShops.l("Asked registry %s for a type %s: %s", catalogs[i].getSimpleName(), s, values[i]);
                }
        }
        Map<Key, Object> result = new HashMap<>();
        for (int i = 0; i < catalogs.length; i++)
            if (values[i] != null) result.put(keys[i], values[i]);
        return new KeyAttacher(result);
    }

    public String getVariant(Entity targetEntity) {
        List<String> ls = new LinkedList<>();
        for (Key key : keys)
            targetEntity.get(key).ifPresent(variant -> {
                if (variant instanceof CatalogType)
                    ls.add(((CatalogType) variant).getId());
                else
                    ls.add(variant.toString());
            });
        return StringUtils.join(ls, VARIANT_CONCATINATOR);
    }
    public Set<String> registerAutoComplete() {
        List<List<String>> perKey = new LinkedList<>();
        for (Class<CatalogType> c : catalogs) {
            if (c != null)
                perKey.add(Sponge.getRegistry().getAllOf(c).stream().map(type->
                    type.getId().toLowerCase().startsWith("minecraft:")
                        ? type.getId().substring(type.getId().indexOf(':')+1)
                        : type.getId()
                ).collect(Collectors.toList()));
        }
        List<Set<String>> suggestions = new LinkedList<>();
        GeneratePermutations(perKey, suggestions, 0, null);
//        for (Set<String> s : suggestions)
//            VillagerShops.l("%s %s", keys[0].getName(), StringUtils.join(s, "; "));
        return suggestions.stream().filter(set->!set.contains("none"))
                .map(set->StringUtils.join(set, VARIANT_CONCATINATOR))
                .collect(Collectors.toSet());
    }
    private static void GeneratePermutations(List<List<String>> suggestionsPerKey, List<Set<String>> result, int depth, Set<String> current) {
        if(depth == suggestionsPerKey.size()) {
            if (current != null && current.size()>0) {
                boolean oneEquals = false;
                for (Set<String> other : result) {
                    boolean equals = current.size() == other.size();
                    if (equals) {
                        for (String s : current)
                            if (!other.contains(s))
                                equals = false;
                    }
                    if (equals) {
                        oneEquals = true;
                    }
                }
                if (!oneEquals) {
                    result.add(current);
                }
            }
            return;
        }

        for(int i = 0; i < suggestionsPerKey.get(depth).size(); ++i) {
            Set<String> newCurrent = new HashSet<>(current==null?1:current.size()+1);
            if (current != null)
                newCurrent.addAll(current);
            newCurrent.add(suggestionsPerKey.get(depth).get(i));
            GeneratePermutations(suggestionsPerKey, result, depth + 1, newCurrent);
        }
    }

    public static final FieldResolver HORSE_VARIANT = new FieldResolver(
            EntityTypes.HORSE, Keys.HORSE_STYLE, Keys.HORSE_COLOR
    );

    public static final FieldResolver OCELOT_VARIANT = new FieldResolver(
            EntityTypes.OCELOT, Keys.OCELOT_TYPE
    );

    public static final FieldResolver VILLAGER_VARIANT = new FieldResolver(
            EntityTypes.VILLAGER, Keys.CAREER
    ) {
        @Override
        public KeyAttacher validate(String magicVariant) {
            String[] raw = VARIANT_SPLITERATOR.split(magicVariant);
            Career career = null;
            Profession profession = null;
            for (String s : raw) {
                career = Sponge.getRegistry().getType(Career.class, s).orElse(career);
                profession = Sponge.getRegistry().getType(Profession.class, s).orElse(profession);
            }
            if (career == null && profession != null)
                career = profession.getCareers().stream().findAny().orElse(null);

            Map<Key, Object> result = new HashMap<>();
            result.put(Keys.CAREER, career);
            return new KeyAttacher(result);
        }
    };

    public static final FieldResolver LLAMA_VARIANT = new FieldResolver(
            EntityTypes.LLAMA, Keys.LLAMA_VARIANT
    );

    public static final FieldResolver RABBIT_VARIANT = new FieldResolver(
            EntityTypes.RABBIT, Keys.RABBIT_TYPE
    );

    public static final FieldResolver PARROT_VARIANT = new FieldResolver(
            EntityTypes.PARROT, Keys.PARROT_VARIANT
    );

    public static final FieldResolver PLAYER_SKIN = new FieldResolver(
            EntityTypes.HUMAN, Keys.SKIN_UNIQUE_ID
    ) {
        final Pattern patUUID = Pattern.compile("(?:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})|(?:[0-9a-f]{32})", Pattern.CASE_INSENSITIVE);

        @Override
        public KeyAttacher validate(String magicVariant) {
            UUID uuid = null;
            if (patUUID.matcher(magicVariant).matches()) {
                uuid = UUID.fromString(magicVariant);
            } else {
                User user = VillagerShops.getUserStorage().get(magicVariant).orElse(null);
                if (user == null) return null;
                uuid = user.getUniqueId();
            }
            Map<Key, Object> result = new HashMap<>();
            result.put(Keys.SKIN_UNIQUE_ID, uuid);
            return new KeyAttacher(result);
        }

        /** can't auto this on server startup */
        @Override
        public Set<String> registerAutoComplete() {
            return new HashSet<>();
        }
    };
}
