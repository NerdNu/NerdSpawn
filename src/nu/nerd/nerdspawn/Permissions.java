package nu.nerd.nerdspawn;

import org.bukkit.entity.Player;


public class Permissions {
    private final static String _NERDSPAWN = "nerdspawn";

    public final static String SPAWN       = _NERDSPAWN + ".spawn";
    public final static String SETSPAWN    = _NERDSPAWN + ".setspawn";

    private Permissions() {}

    public static boolean hasPermission (Player player, String node) {
        return player.hasPermission(node);
    }
}
