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
            message.append(ChatColor.WHITE).append("(").append(index).append(") ");
            if (index == 1) {
                message.append(ChatColor.GRAY).append("[P] ");
            }
            message.append(String.format("%s(%d, %d, %d, %s) %sP %5.3f Y %5.3f",
                ChatColor.YELLOW, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName(),
                ChatColor.GRAY, loc.getPitch(), loc.getYaw()));
            player.sendMessage(message.toString());
            index++;
        }
    }
    
    private double get2dDistSq(Location l1, Location l2) {
        double diffX = l1.getX() - l2.getX();
        double diffZ = l1.getZ() - l2.getZ();
        return diffX * diffX + diffZ * diffZ;
    }
    
    public Location getPrimarySpawn() {
        return stringToLocation(spawnList.get(0));
    }

    public Location getSpawnLocation() {
        if (getConfig().getBoolean("many-spawn") == true) {
            Random r = new Random();
            return stringToLocation(spawnList.get(r.nextInt(spawnList.size())));
        } else {
            return stringToLocation(spawnList.get(0));
        }
    }
    
    public Location getSpawnLocation(Location bed) {
        if (bed == null) {
            return getSpawnLocation();
        } else {
            int k = new Random().nextInt(spawnList.size() + 1);
            return k == 0 ? bed : stringToLocation(spawnList.get(k - 1));
        }
    }
    
    public Location getSpawnLocation(Player player, Location center, Location bed) {
        if (getConfig().getBoolean("radial-spawning")) {
            List<Location> candidates = new ArrayList<Location>();
            double minDist = -1.0;
            Location closest = null;
            double radius = getConfig().getDouble("spawn-radius");
            double rsq = radius * radius;
            
            if (getConfig().getBoolean("allow-bed-spawn") && bed != null) {
                if (bed.getWorld() == center.getWorld()) {
                    double dsq = get2dDistSq(center, bed);
                    if (dsq <= rsq) {
                        candidates.add(bed);
                    } else if (candidates.isEmpty() && (dsq < minDist || minDist < 0)) {
                        closest = bed;
                        minDist = dsq;
                    }
                }
            }
            
            for (String s : spawnList) {
                Location loc = stringToLocation(s);
                if (loc.getWorld() == center.getWorld()) {
                    double dsq = get2dDistSq(center, loc);
                    if (dsq <= rsq) {
                        candidates.add(loc);
                    } else if (candidates.isEmpty() && (dsq < minDist || minDist < 0)) {
                        closest = loc;
                        minDist = dsq;
                    }
                }
            }
            if (!candidates.isEmpty()) {
                return candidates.get(new Random().nextInt(candidates.size()));
            } else if (closest != null) {
                return closest;
            } else { // No spawns in that World
                return getConfig().getBoolean("use-primary-spawn") ? getPrimarySpawn() : getSpawnLocation(bed);
            }
        } else {
            if (getConfig().getBoolean("allow-bed-spawn") && bed != null) {
                double radius = getConfig().getDouble("bed-radius", 15);
                if (getConfig().getBoolean("check-bed-radius") && get2dDistSq(center, bed) <= radius * radius) {
                    player.sendMessage(ChatColor.GOLD + "You died too close to your bed. Sending you back to spawn.");
                    if (getConfig().getBoolean("clear-bed-spawn")) {
                        player.setBedSpawnLocation(null);
                        player.sendMessage(ChatColor.GOLD + "You will respawn at spawn until you sleep in a bed.");
                    }
                    return getSpawnLocation();
                } else {
                    return bed;
                }
            } else {
                return getSpawnLocation();
            }
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
                if (args[0].equalsIgnoreCase("setprimary") && player.hasPermission(Permissions.SETSPAWN)) {
                    try {
                        // Parse 1-based indices.
                        int index = Integer.parseInt(args[1]);
                        if (index < 1 || index > spawnList.size()) {
                            player.sendMessage(ChatColor.RED + "Index " + index + " is out of range.");
                        } else {
                            spawnList.add(0, spawnList.remove(index - 1));
                            saveConfiguration();
                            player.sendMessage(ChatColor.GOLD + "Spawn at index " + index + " moved to index 1.");
                        }
                    } catch (Exception ex) {
                        player.sendMessage(ChatColor.RED + args[1] + " is not an integer.");
                    }
                    return true;
                }
            }
            player.sendMessage(ChatColor.GRAY + "Usage: /nerdspawn [reload | list | remove <number> | setprimary <number>]");
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
