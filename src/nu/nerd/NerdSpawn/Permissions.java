package nu.nerd.NerdSpawn;

import com.nijiko.permissions.PermissionHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;


public class Permissions {
    public static PermissionHandler permissionHandler;
    public static boolean useNewPermissions = false;

    private final static String _NERDSPAWN = "nerdspawn";

    public final static String SPAWN       = _NERDSPAWN + ".spawn";
    public final static String SETSPAWN    = _NERDSPAWN + ".setspawn";

    private Permissions() {}

    public static boolean hasPermission (Player player, String node) {
        // setup permissions handler on first call
        if (permissionHandler == null && !Permissions.useNewPermissions) {
            Plugin permissionsPlugin = Bukkit.getServer().getPluginManager().getPlugin("Permissions");

            if (permissionsPlugin != null) {
                permissionHandler = ((com.nijikokun.bukkit.Permissions.Permissions) permissionsPlugin).getHandler();
            }
            else {
                useNewPermissions = true;
            }
        }

        if (useNewPermissions) {
            return player.hasPermission(node);
        }
        else {
            return permissionHandler.has(player, node);
        }
    }
}
