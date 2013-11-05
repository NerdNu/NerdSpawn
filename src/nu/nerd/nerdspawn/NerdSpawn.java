package nu.nerd.nerdspawn;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class NerdSpawn extends JavaPlugin {
    private final NerdSpawnListener listener = new NerdSpawnListener(this);
    List<String> spawnList = new ArrayList<String>();

    Formatter f;

    /**
     * Reload the configuration file.
     */
    private void loadConfiguration() {
        reloadConfig();
        spawnList.clear();
        spawnList.addAll(getConfig().getStringList("spawn.location"));
    }

    /**
     * Save the configuration file.
     */
    private void saveConfiguration() {
        getConfig().set("spawn.location", spawnList);
        saveConfig();

    }

    /**
     * Send the player a multi-line message listing all of the spawns.
     * 
     * @param player the player executing the command.
     */
    private void listSpawns(Player player) {
        player.sendMessage(ChatColor.GOLD + "Spawns: ");
        StringBuilder message = new StringBuilder();
        int index = 1;
        for (String spawn : spawnList) {
            Location loc = stringToLocation(spawn);
            message.setLength(0);
            message.append(ChatColor.WHITE).append("(").append(index++).append(") ");
            message.append(String.format("%s(%d, %d, %d, %s) %sP %5.3f Y %5.3f",
                ChatColor.YELLOW, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName(),
                ChatColor.GRAY, loc.getPitch(), loc.getYaw()));
            player.sendMessage(message.toString());
        }
    }

    public Location getSpawnLocation() {
        if (getConfig().getBoolean("many-spawn") == true) {
            Random r = new Random();
            return stringToLocation(spawnList.get(r.nextInt(spawnList.size())));
        } else {
            return stringToLocation(spawnList.get(0));
        }
    }

    public Location stringToLocation(String location) {
        String[] lp = location.split("\\|");
        World world = getServer().getWorld(lp[0]);
        return new Location(world,
                    Double.valueOf(lp[1]),
                    Double.valueOf(lp[2]),
                    Double.valueOf(lp[3]),
                    Float.valueOf(lp[4]),
                    Float.valueOf(lp[5]));
    }

    public String locationToString(Location location) {
        String re = location.getWorld().getName() + "|";
        re += location.getX() + "|";
        re += location.getY() + "|";
        re += location.getZ() + "|";
        re += location.getYaw() + "|";
        re += location.getPitch();
        return re;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfiguration();
        getServer().getPluginManager().registerEvents(listener, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Sorry, this plugin cannot be used from console");
            return true;
        }
        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("spawn")) {
            ((Player) sender).teleport(getSpawnLocation());
            return true;
        }

        if (command.getName().equalsIgnoreCase("setspawn")) {
            setSpawn((Player) sender);
            return true;
        }

        if (command.getName().equalsIgnoreCase("nerdspawn")) {
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("reload") && player.hasPermission(Permissions.RELOAD)) {
                    loadConfiguration();
                    player.sendMessage(ChatColor.GOLD + "NerdSpawn configuration reloaded.");
                    return true;
                }

                if (args[0].equalsIgnoreCase("list") && player.hasPermission(Permissions.LIST)) {
                    listSpawns(player);
                    return true;
                }
            } else if (args.length == 2) {
                // Require the same permission to remove a spawn as to set one.
                if (args[0].equalsIgnoreCase("remove") && player.hasPermission(Permissions.SETSPAWN)) {
                    try {
                        // Parse 1-based indices.
                        int index = Integer.parseInt(args[1]);
                        if (index < 1 || index > spawnList.size()) {
                            player.sendMessage(ChatColor.RED + "Index " + index + " is out of range.");
                        } else if (spawnList.size() == 1) {
                            player.sendMessage(ChatColor.RED + "There must be at least one spawn.");
                        } else {
                            spawnList.remove(index - 1);
                            saveConfiguration();
                            player.sendMessage(ChatColor.GOLD + "Removed spawn at index " + index + ".");
                        }
                    } catch (Exception ex) {
                        player.sendMessage(ChatColor.RED + args[1] + " is not an integer.");
                    }
                    return true;
                }
            }
            player.sendMessage(ChatColor.GRAY + "Usage: /nerdspawn [reload | list | remove <number>]");
            return true;
        }

        return false;
    }

    private void setSpawn(Player player) {
        Location loc = player.getLocation();
        if (getConfig().getBoolean("many-spawn") != true) {
            spawnList.clear();
        }
        spawnList.add(locationToString(loc));
        getServer().getWorlds().get(0).setSpawnLocation(
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ());
        player.sendMessage(ChatColor.GRAY + "Spawn set at " +
                           loc.getBlockX() + ", " +
                           loc.getBlockY() + ", " +
                           loc.getBlockZ());
        saveConfiguration();
    }
}
