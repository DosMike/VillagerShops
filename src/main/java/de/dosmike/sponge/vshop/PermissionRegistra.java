package de.dosmike.sponge.vshop;

import de.dosmike.sponge.vshop.systems.Permission;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.text.Text;

public class PermissionRegistra {

	public static final Permission ADMIN;
	public static final Permission PLAYER;
	public static final Permission IMPORT;
	public static final Permission IDENTIFY;
	public static final Permission LINKCHEST;
	public static final Permission MOVE;
	public static final Permission LEDGER_ME;
	public static final Permission LEDGER_OTHERS;

	static {

		ADMIN = Permission.Registry.register("edit admin", "vshop.edit.admin", Text.of("edit any shop, create admin shops, /vshop save, /vshop reload, /vshop list, /vshop tphere"), PermissionDescription.ROLE_STAFF);
		PLAYER = Permission.Registry.register("edit player", "vshop.edit.player", Text.of("required for players to create player shops"), PermissionDescription.ROLE_USER);
		IMPORT = Permission.Registry.register("import/release entity", "vshop.edit.import", Text.of("required for /vshop import, /vshop release"), PermissionDescription.ROLE_STAFF);
		IDENTIFY = Permission.Registry.register("identify", "vshop.edit.identify", Text.of("required for /vshop identify"), PermissionDescription.ROLE_USER);
		LINKCHEST = Permission.Registry.register("link chests", "vshop.edit.linkchest", Text.of("required for /vshop link"), PermissionDescription.ROLE_USER);
		MOVE = Permission.Registry.register("move", "vshop.edit.move", Text.of("required for /vshop tphere"), PermissionDescription.ROLE_STAFF);
		LEDGER_ME = Permission.Registry.register("ledger me", "vshop.ledger.base", Text.of("required for /vshop ledger"), PermissionDescription.ROLE_USER);
		LEDGER_OTHERS = Permission.Registry.register("ledger others", "vshop.ledger.others", Text.of("required for /vshop ledger <PLAYER>"), PermissionDescription.ROLE_STAFF);

	}

}
