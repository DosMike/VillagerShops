package de.dosmike.sponge.vshop.shops;

import de.dosmike.sponge.vshop.VillagerShops;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.Career;
import org.spongepowered.api.data.type.Profession;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.User;

import java.util.*;
import java.util.regex.Pattern;

public class FieldResolvers {

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
			KeyAttacher attacher = new FieldResolver.KeyAttacher();
			attacher.add(Keys.SKIN_UNIQUE_ID, uuid);
			return attacher;
		}

		/** can't auto this on server startup */
		@Override
		public Set<String> registerAutoComplete() {
			return new HashSet<>();
		}
	};
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

			KeyAttacher attacher = new FieldResolver.KeyAttacher();
			attacher.add(Keys.CAREER, career);
			return attacher;
		}
	};

}
