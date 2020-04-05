package de.dosmike.sponge.vshop.systems;

import de.dosmike.sponge.vshop.VillagerShops;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandPermissionException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

public class Permission {
    private String id;
    private Text description;
    private String group; //role
    private Object key=null;
    public String getId() { return id; }
    public Text getDescription() { return description; }
    public String getGroup() { return group; }
    /** @return the object this permission was registered with or null */
    Object getKey() { return key; }

    private Permission(String id, @Nullable Text description, @Nullable @MagicConstant(valuesFromClass = PermissionDescription.class) String group) {
        if (id == null)
            throw new IllegalStateException("The Permission builder requires a permission id to be set");
        this.id = id;
        this.description = description;
        this.group = group;

        VillagerShops.describePermission().ifPresent(perm -> {
            perm.id(id);
            perm.description(description);
            if (group != null) perm.assign(group, true); else perm.assign(PermissionDescription.ROLE_USER, true);
            try { //in case overwriting is not supported
                perm.register();
            } catch (IllegalStateException ignore) {}
        });
    }

    /** for use in Commands
     * @param src the CommandSource to test
     * @throws CommandException if permission was denied for src */
    public void test(CommandSource src) throws CommandException {
        if (!src.hasPermission(id))
            throw new CommandPermissionException(VillagerShops.getTranslator()
                    .localText("permission.missing")
                    .resolve(src)
                    .orElse(Text.of(TextColors.RED, "You do not have the permission "+id))
            );
    }
    public boolean hasPermission(CommandSource src) {
        return src.hasPermission(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Permission that = (Permission) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static class Registry {

        static final Map<Object, Permission> permissions = new HashMap<>();
        private static final Object mutex = new Object();

        public static Permission register(Object key, String id) {
            if (permissions.containsKey(key)) unregister(key);
            Permission p = new Permission(id, null, null);
            permissions.put(key, p);
            p.key = key;
            return p;
        }
        public static Permission register(Object key, String id, Text description) {
            if (permissions.containsKey(key)) unregister(key);
            Permission p = new Permission(id, description, null);
            permissions.put(key, p);
            p.key = key;
            return p;
        }
        public static Permission register(Object key, String id, Text description, @MagicConstant(valuesFromClass = PermissionDescription.class) String group) {
            if (permissions.containsKey(key)) unregister(key);
            Permission p = new Permission(id, description, group);
            permissions.put(key, p);
            p.key = key;
            return p;
        }
        public static void unregister(Permission permission) {
            synchronized (mutex) {
                permissions.entrySet().stream()
                        .filter(e -> permission.equals(e.getValue()))
                        .map(Map.Entry::getKey)
                        .findFirst().ifPresent(permissions::remove);
            }
        }
        public static void unregister(Object key) {
            synchronized (mutex) {
                Permission p = permissions.get(key);
                if (p == null) return;
                permissions.remove(key);
            }
        }
        public static Permission getPermission(Object key) {
            return permissions.get(key);
        }
        /** short for p = getPermission; if p != null p.test();
         * @throws NoSuchElementException if no permission exists for that key
         * @throws CommandException if the permission was not granted
         */
        public static void testPermission(CommandSource src, Object key) throws CommandException, NoSuchElementException {
            Permission p = permissions.get(key);
            if (p != null) p.test(src);
            else throw new NoSuchElementException("No permission registered for key "+key.toString());
        }
        /**
         * Test if a source has permission with return
         * @throws NoSuchElementException if there was no permission registered to that key
         * @return true if the source has permission
         */
        public static boolean hasPermission(CommandSource src, Object key) throws NoSuchElementException {
            Permission p = permissions.get(key);
            if (p != null) return p.hasPermission(src);
            else throw new NoSuchElementException("No permission registered for key "+key.toString());
        }
        /**
         * Test if a source has permission with return. If no such permission was registered returns
         * the default permission grant specified
         * @param defaultGrant whether the source shall be permitted if no such permission was found
         * @return true if the source has permission
         */
        public static boolean hasPermission(CommandSource src, Object key, boolean defaultGrant) throws NoSuchElementException {
            Permission p = permissions.get(key);
            if (p != null) return p.hasPermission(src);
            else return defaultGrant;
        }
    }

}
