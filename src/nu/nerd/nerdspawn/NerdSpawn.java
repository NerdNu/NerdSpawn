package nu.nerd.nerdspawn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class NerdSpawn extends JavaPlugin {
    protected final Random random = new Random();
    protected final NerdSpawnListener listener = new NerdSpawnListener(this);
    protected List<String> spawnList = new ArrayList<String>();
    protected HashMap<Player, Location> _waitingTeleport = new HashMap<Player, Location>();

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
            message.append(String.format("%s(%d, %d, %d, %s) %sPitch %5.3f Yaw %5.3f",
                                         ChatColor.YELLOW, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName(),
                                         ChatColor.GRAY, loc.getPitch(), loc.getYaw()));
            player.sendMessage(message.toString());
            index++;
        }

        if (getConfig().getBoolean("use-first-join-spawn")) {
            Location loc = getFirstJoinSpawnLocation();
            player.sendMessage(String.format("%sOn first join, players will spawn at:\n    %s(%d, %d, %d, %s) %sPitch %5.3f Yaw %5.3f",
                                             ChatColor.GOLD,
                                             ChatColor.YELLOW, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName(),
                                             ChatColor.GRAY, loc.getPitch(), loc.getYaw()));
        }
    }

    public static double get2dDistSq(Location l1, Location l2) {
        double diffX = l1.getX() - l2.getX();
        double diffZ = l1.getZ() - l2.getZ();
        return diffX * diffX + diffZ * diffZ;
    }

    /**
     * Return the first listed spawn location, excluding the first-join spawn
     * location.
     * 
     * This will be the normal respawn location for players
     * 
     * @return the main spawn location.
     */
    public Location getPrimarySpawn() {
        return stringToLocation(spawnList.get(0));
    }

    /**
     * Return a spawn location from the applicable set of fixed, server-wide
     * locations.
     * 
     * The bed spawn location is not considered as a candidate.
     * 
     * If many-spawn is configured, a location is randomly selected from all
     * spawns that have been set except the first-join spawn. Otherwise, the
     * primary (first that was defined) spawn location is returned.
     * 
     * @return a server-wide spawn location.
     */
    public Location getRespawnLocation() {
        if (getConfig().getBoolean("many-spawn") == true) {
            return stringToLocation(spawnList.get(random.nextInt(spawnList.size())));
        } else {
            return getPrimarySpawn();
        }
    }

    /**
     * Return the spawn location to be used on first join, per the
     * configuration.
     * 
     * @return the spawn location to be used on first join, per the
     *         configuration.
     */
    public Location getFirstJoinSpawnLocation() {
        return getConfig().getBoolean("use-first-join-spawn") ? stringToLocation(getConfig().getString("spawn.first-join"))
                                                              : getRespawnLocation();
    }

    public Location getSpawnLocation(Player player, Location deathLoc, Location bed) {
        if (getConfig().getBoolean("radial-spawning")) {
            List<Location> candidates = getRadialSpawnCandidates(deathLoc, bed);
            if (!candidates.isEmpty()) {
                return randomLocation(candidates);
            } else {
                // No spawn locations in that World. Send the player to the
                // primary spawn, if forced by configuration, or to a spawn
                // selected from all possible spawns including the bed.
                return getConfig().getBoolean("use-primary-spawn") ? getPrimarySpawn() : randomLocation(getAllSpawnLocations(bed));
            }
        } else {
            if (getConfig().getBoolean("allow-bed-spawn") && bed != null) {
                if (getConfig().getBoolean("check-bed-radius")) {
                    final double radius = getConfig().getDouble("bed-radius", 15);
                    if (deathLoc != null && get2dDistSq(deathLoc, bed) <= radius * radius) {
                        player.sendMessage(ChatColor.GOLD + "You died too close to your bed. Sending you back to spawn.");
                        if (getConfig().getBoolean("clear-bed-spawn")) {
                            player.setBedSpawnLocation(null);
                            player.sendMessage(ChatColor.GOLD + "You will respawn at spawn until you sleep in a bed.");
                        }
                        return getRespawnLocation();
                    }
                }
                return bed;

            } else {
                // Send the player to global spawn.
                return getRespawnLocation();
            }
        }
    }

    public static Location stringToLocation(String location) {
        String[] lp = location.split("\\|");
        World world = Bukkit.getServer().getWorld(lp[0]);
        return new Location(world,
            Double.valueOf(lp[1]),
            Double.valueOf(lp[2]),
            Double.valueOf(lp[3]),
            Float.valueOf(lp[4]),
            Float.valueOf(lp[5]));
    }

    public static String locationToString(Location location) {
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
        final Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("spawn") && player.hasPermission(Permissions.SPAWN)) {
            ((Player) sender).teleport(getRespawnLocation());
            return true;
        }

        if (command.getName().equalsIgnoreCase("spawn") && player.hasPermission(Permissions.DELAYSPAWN)) {
            // Players are already waiting, inform them and return
            if (_waitingTeleport.containsKey(player)) {
                player.sendMessage(ChatColor.AQUA + "You are already waiting to be teleported");
                return true;
            }

            // Schedule a runnable for a later time to warp our player, make
            // sure they havent moved while waiting
            getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                @Override
                public void run() {
                    if (_waitingTeleport.containsKey(player)) {
                        if (get2dDistSq(player.getLocation(), _waitingTeleport.get(player)) <= 2) {
                            player.teleport(getRespawnLocation());
                        } else {
                            player.sendMessage(ChatColor.AQUA + "You moved during spawn cooldown!");
                        }
                        _waitingTeleport.remove(player);
                    }
                }
            }, getConfig().getInt("spawn-delay", 10) * 20);

            _waitingTeleport.put(player, player.getLocation());
            player.sendMessage(ChatColor.AQUA + "You have been placed on spawn cooldown, don't move!");
            return true;
        }

        if (command.getName().equalsIgnoreCase("setspawn") && player.hasPermission(Permissions.SETSPAWN)) {
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
        getServer().getWorlds().get(0).setSpawnLocation(loc.getBlockX(),
                                                        loc.getBlockY(),
                                                        loc.getBlockZ());
        player.sendMessage(ChatColor.GRAY + "Spawn set at " +
                           loc.getBlockX() + ", " +
                           loc.getBlockY() + ", " +
                           loc.getBlockZ());
        saveConfiguration();
    }

    /**
     * Return a list of candidate spawn locations in the same world as a death
     * location, for use in radial-spawning.
     * 
     * A spawn location is a candidate if it is within the config's spawn-radius
     * of the player's death location. This means the spawn must be in the same
     * world as the death. If no spawn location is within spawn-radius of the
     * player's death, the closest location is chosen as the only candidate. If
     * there are no spawn locations in the same world, an empty list is
     * returned.
     * 
     * @param deathLoc the location of the player's death; can be null if no
     *        death since restart.
     * @param bed the location of the player's bed, or null to not be
     *        considered.
     * @return the spawn locations that are within spawn-radius of deathLoc, or
     *         failing that, the nearest spawn in the same world, or an empty
     *         list if there is no such spawn location.
     */
    protected List<Location> getRadialSpawnCandidates(Location deathLoc, Location bed) {
        List<Location> candidates = new ArrayList<>();
        if (deathLoc == null) {
            return candidates;
        }

        final double spawnRadius = getConfig().getDouble("spawn-radius");
        final double spawnRadiusSquared = spawnRadius * spawnRadius;

        Location closest = null;
        double minDistSq = Double.MAX_VALUE;

        final Location includingBed = getConfig().getBoolean("allow-bed-spawn") ? bed : null;
        for (Location loc : getAllSpawnLocations(includingBed)) {
            if (bed.getWorld() == deathLoc.getWorld()) {
                double distSq = get2dDistSq(deathLoc, loc);
                if (distSq <= spawnRadiusSquared) {
                    candidates.add(loc);
                } else if (distSq < minDistSq) {
                    minDistSq = distSq;
                    closest = loc;
                }
            }
        }

        if (candidates.isEmpty()) {
            candidates.add(closest);
        }

        return candidates;
    }

    /**
     * Return a list of all spawn locations, excluding the first join spawn, but
     * including the bed spawn location if not null.
     * 
     * @param bed the bed spawn location.
     * @return a list of all spawn locations, with the bed location first if not
     *         null.
     */
    protected List<Location> getAllSpawnLocations(Location bed) {
        List<Location> locations = new ArrayList<>();
        if (bed != null) {
            locations.add(bed);
        }
        spawnList.forEach(loc -> locations.add(stringToLocation(loc)));
        return locations;
    }

    /**
     * Return a randomly selected Location from the list of candidates.
     * 
     * @param candidates a non-empty list of candidate spawn locations.
     * @return a radnomly selected Location.
     */
    protected Location randomLocation(List<Location> candidates) {
        return candidates.get(random.nextInt(candidates.size()));
    }
}
