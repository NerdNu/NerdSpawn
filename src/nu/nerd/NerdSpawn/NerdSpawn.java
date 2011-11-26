package nu.nerd.NerdSpawn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;


public class NerdSpawn extends JavaPlugin
{
    private final NerdSpawnPlayerListener playerListener = new NerdSpawnPlayerListener(this);
    public static final Logger log = Logger.getLogger("Minecraft");
    public String worldName;

    public Location getSpawnLocation()
    {
        // just in case someone edited the file
        reloadConfig();

        return new Location(Bukkit.getServer().getWorld(worldName),
                getConfig().getDouble("spawn-location.x"),
                getConfig().getDouble("spawn-location.y"),
                getConfig().getDouble("spawn-location.z"),
                (float)getConfig().getDouble("spawn-location.yaw"),
                (float)getConfig().getDouble("spawn-location.pitch"));
    }

    @Override
    public void onDisable()
    {
        log.log(Level.INFO, "[" + getDescription().getName() + "] " + getDescription().getVersion() + " disabled.");
    }

    @Override
    public void onEnable()
    {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Event.Type.PLAYER_PRELOGIN, playerListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_JOIN,     playerListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_RESPAWN,  playerListener, Priority.Normal, this);

        try {
            Properties config = new Properties();
            config.load(new BufferedReader(new FileReader("server.properties")));
            worldName = config.getProperty("level-name");
        }
        catch (Exception e) {
            e.printStackTrace();
            pm.disablePlugin(this);
            return;
        }

        File config = new File(getDataFolder() + File.separator + "config.yml");
        if (!config.exists()) {
            getConfig().options().copyDefaults(true);
            saveConfig();
        }

        log.log(Level.INFO, "[" + getDescription().getName() +"] " + getDescription().getVersion() + " enabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args)
    {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Sorry, this plugin cannot be used from console");
            return true;
        }

        if (command.getName().equalsIgnoreCase("spawn")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (Permissions.hasPermission((Player)sender, Permissions.SETSPAWN)) {
                    reloadConfig();
                    sender.sendMessage(ChatColor.GRAY + "Spawn configuration reloaded");
                    return true;
                }
            }
            return spawn((Player)sender);
        }

        if (command.getName().equalsIgnoreCase("setspawn")) {
            return setSpawn((Player)sender);
        }

        return true;
    }

    private boolean spawn(Player player)
    {
        if (!Permissions.hasPermission(player, Permissions.SPAWN))
            return true;

        player.teleport(getSpawnLocation());

        return true;
    }

    private boolean setSpawn(Player player)
    {
        if (!Permissions.hasPermission(player, Permissions.SETSPAWN))
            return true;

        Location loc = player.getLocation();
        getConfig().set("spawn-location.x", loc.getX());
        getConfig().set("spawn-location.y", loc.getY());
        getConfig().set("spawn-location.z", loc.getZ());
        getConfig().set("spawn-location.yaw", loc.getYaw());
        getConfig().set("spawn-location.pitch", loc.getPitch());
        saveConfig();

        Bukkit.getServer().getWorld(worldName).setSpawnLocation(
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ());

        player.sendMessage(ChatColor.GRAY + "Spawn set at " +
                loc.getBlockX() + ", " +
                loc.getBlockY() + ", " +
                loc.getBlockZ());

        return true;
    }
}
