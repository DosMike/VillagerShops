package de.dosmike.sponge.vshop.shops;

import com.google.common.reflect.TypeToken;
import de.dosmike.sponge.vshop.VillagerShops;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.Career;
import org.spongepowered.api.data.type.Profession;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.User;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FieldResolver {

	public static final FieldResolver HORSE_VARIANT = new FieldResolver(
			EntityTypes.HORSE, Arrays.asList(Keys.HORSE_STYLE, Keys.HORSE_COLOR)
	);
	public static final FieldResolver OCELOT_VARIANT = new FieldResolver(
			EntityTypes.OCELOT, Collections.singletonList(Keys.OCELOT_TYPE)
	);
	public static final FieldResolver LLAMA_VARIANT = new FieldResolver(
			EntityTypes.LLAMA, Collections.singletonList(Keys.LLAMA_VARIANT)
	);
	public static final FieldResolver RABBIT_VARIANT = new FieldResolver(
			EntityTypes.RABBIT, Collections.singletonList(Keys.RABBIT_TYPE)
	);
	public static final FieldResolver PARROT_VARIANT = new FieldResolver(
			EntityTypes.PARROT, Collections.singletonList(Keys.PARROT_VARIANT)
	);
	public static final FieldResolver PLAYER_SKIN = new FieldResolver(
			EntityTypes.HUMAN, Collections.singletonList(Keys.SKIN_UNIQUE_ID)
	) {
		final Pattern patUUID = Pattern.compile("(?:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})|(?:[0-9a-f]{32})", Pattern.CASE_INSENSITIVE);

		@Override
		public KeyAttacher validate(String magicVariant) {
			UUID uuid = null;
			if (patUUID.matcher(magicVariant).matches()) {
				if (magicVariant.indexOf('-') < 0) {
					magicVariant = magicVariant.substring(0, 8) + "-" + magicVariant.substring(8, 12) + "-" + magicVariant.substring(12, 16) + "-" + magicVariant.substring(16, 20) + "-" + magicVariant.substring(20);
				}
				uuid = UUID.fromString(magicVariant);
			} else {
				try {
					User user = VillagerShops.getUserStorage()
							.get(magicVariant).orElse(null);
					if (user != null)
						uuid = user.getUniqueId();
				} catch (Exception e) {
					/* ignore invalid values */
				}
			}
			KeyAttacher attacher = new KeyAttacher();
			attacher.add(Keys.SKIN_UNIQUE_ID, uuid);
			return attacher;
		}

		/** can't auto this on server startup */
		@Override
		public Set<String> registerAutoComplete() {
			return new HashSet<>();
		}
	};
	protected static final Map<EntityType, Set<String>> autoTabCompleteMapping = new HashMap<>();
	private static final String VARIANT_CONCATINATOR = "+";
	private static final Pattern VARIANT_SPLITERATOR = Pattern.compile("\\+");
	public static final FieldResolver VILLAGER_VARIANT = new FieldResolver(
			EntityTypes.VILLAGER, Collections.singletonList(Keys.CAREER)
	) {
		@Override
		public KeyAttacher validate(String magicVariant) {
			String[] raw = VARIANT_SPLITERATOR.split(magicVariant);
			Career career = null;
			Profession profession = null;
			for (String s : raw) {
				Optional<Career> careerOptional = Sponge.getRegistry().getType(Career.class, s);
				if (careerOptional.isPresent()) career = careerOptional.get();
				Optional<Profession> professionOptional = Sponge.getRegistry().getType(Profession.class, s);
				if (professionOptional.isPresent()) profession = professionOptional.get();
			}
			if (career == null && profession != null)
				career = profession.getCareers().stream().findAny().orElse(null);

			KeyAttacher attacher = new KeyAttacher();
			attacher.add(Keys.CAREER, career);
			return attacher;
		}
	};
	private static final Map<EntityType, FieldResolver> autoResolverMapping = new HashMap<>();
	protected final ArrayList<Key<? extends BaseValue<?>>> keys;

	private FieldResolver(EntityType type, List<Key<? extends BaseValue<?>>> keyList) {
		this.keys = new ArrayList<>(keyList);
		autoResolverMapping.put(type, this);
		autoTabCompleteMapping.put(type, registerAutoComplete());
	}

	public static Optional<KeyAttacher> autoValidate(EntityType entity, String magicString) {
		if (autoResolverMapping.containsKey(entity))
			return Optional.ofNullable(autoResolverMapping.get(entity).validate(magicString));
		else
			return Optional.empty();
	}

	public static Map<EntityType, Set<String>> getAutoTabCompleteMapping() {
		return autoTabCompleteMapping;
	}

	private static void GeneratePermutations(List<List<String>> suggestionsPerKey, List<Set<String>> result, int depth, Set<String> current) {
		if (depth == suggestionsPerKey.size()) {
			if (current != null && current.size() > 0) {
				boolean oneEquals = false;
				for (Set<String> other : result) {
					boolean equals = current.size() == other.size();
					if (equals) {
						for (String s : current)
							if (!other.contains(s)) {
								equals = false;
								break;
							}
					}
					if (equals) {
						oneEquals = true;
						break;
					}
				}
				if (!oneEquals) {
					result.add(current);
				}
			}
			return;
		}

		for (int i = 0; i < suggestionsPerKey.get(depth).size(); ++i) {
			Set<String> newCurrent = new HashSet<>(current == null ? 1 : current.size() + 1);
			if (current != null)
				newCurrent.addAll(current);
			newCurrent.add(suggestionsPerKey.get(depth).get(i));
			GeneratePermutations(suggestionsPerKey, result, depth + 1, newCurrent);
		}
	}

	@SuppressWarnings({"unchecked", "UnstableApiUsage"})
	private Optional<Class<? extends CatalogType>> getCatalogType(Key<? extends BaseValue<?>> key) {
		TypeToken<?> elementToken = key.getElementToken();
		Class<?> elementClass = elementToken.getRawType();
		if (CatalogType.class.isAssignableFrom(elementClass))
			return Optional.of((Class<? extends CatalogType>) elementClass);
		else return Optional.empty();
	}

	/**
	 * @return a KeyAttacher that applies all Variant data to a entity of the given type OR null if the magic string is incompatible
	 */
	public KeyAttacher validate(String magicVariant) {
		String[] raw = VARIANT_SPLITERATOR.split(magicVariant);
		KeyAttacher attacher = new KeyAttacher();
		for (String s : raw) {
			//noinspection RedundantCast
			keys.forEach(k -> getCatalogType(k)
					.flatMap(c -> Sponge.getRegistry().getType(c, s))
					.ifPresent(v -> attacher.add((Key<? extends BaseValue<CatalogType>>) k, v)));
		}
		return attacher;
	}

	public String getVariant(Entity targetEntity) {
		String variantMagic = keys.stream()
				.map(key -> {
					Object value = targetEntity.getValue((Key<? extends BaseValue<Object>>) key)
							.map(BaseValue::get).orElse(null);
					if (value instanceof CatalogType) {
						return ((CatalogType) value).getId();
					} else {
						return value == null ? "" : value.toString();
					}
				}).collect(Collectors.joining(VARIANT_CONCATINATOR));
		return variantMagic.isEmpty() ? "none" : variantMagic;
	}

	public Set<String> registerAutoComplete() {
		List<List<String>> perKey = new LinkedList<>();
		for (Key<? extends BaseValue<?>> key : keys) {
			getCatalogType(key).ifPresent(c ->
					perKey.add(Sponge.getRegistry().getAllOf(c).stream().map(type ->
							type.getId().toLowerCase().startsWith("minecraft:")
									? type.getId().substring(type.getId().indexOf(':') + 1)
									: type.getId()
					).collect(Collectors.toList()))
			);
		}
		List<Set<String>> suggestions = new LinkedList<>();
		GeneratePermutations(perKey, suggestions, 0, null);
		return suggestions.stream().filter(set -> !set.contains("none"))
				.map(set -> StringUtils.join(set, VARIANT_CONCATINATOR))
				.collect(Collectors.toSet());
	}

	static class KeyValuePair<V, K extends BaseValue<V>> {
		private final Key<K> key;
		private final V value;

		KeyValuePair(Key<K> key, V value) {
			this.key = key;
			this.value = value;
		}

		void offer(Entity entity) {
			entity.offer(key, value).ifNotSuccessful(() -> new RuntimeException("Key not supported"));
		}

		@Override
		public String toString() {
			return value instanceof CatalogType ? ((CatalogType) value).getId() : value.toString();
		}
	}

	/**
	 * Stores Keys and Values to be attached to a Entity at some later point
	 */
	public static class KeyAttacher {
		final List<KeyValuePair<?, ?>> values = new LinkedList<>();

		private KeyAttacher() {
		}

		<V, K extends BaseValue<V>> void add(Key<K> key, V value) {
			values.add(new KeyValuePair<>(key, value));
		}

		public void attach(Entity entity) {
			values.forEach(entry -> entry.offer(entity));
		}

		/**
		 * @return a reduced string only containing valid variant names, without duplicates or doubled types
		 */
		public String toString() {
			if (values.isEmpty()) return "NONE";
			try {
				return values.stream().map(KeyValuePair::toString).collect(Collectors.joining(VARIANT_CONCATINATOR));
			} catch (NullPointerException invalidSkin) {
				/* might need further investigation */
				return "NONE";
			}
		}
	}
}
