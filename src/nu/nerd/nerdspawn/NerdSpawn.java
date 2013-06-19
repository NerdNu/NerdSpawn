package nu.nerd.nerdspawn;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;


public class NerdSpawn extends JavaPlugin
{
	private final NerdSpawnListener listener = new NerdSpawnListener(this);
        List<String> spawnList = new ArrayList<String>();

	public Location getSpawnLocation()
	{
		// just in case someone edited the file
		reloadConfig();
                
                spawnList.clear();
                spawnList.addAll(getConfig().getStringList("spawn.location"));

                if( getConfig().getBoolean("many-spawn") == true) {
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
	public void onEnable()
	{
		getServer().getPluginManager().registerEvents(listener, this);

		File config = new File(getDataFolder() + File.separator + "config.yml");
		if (!config.exists()) {
			getConfig().options().copyDefaults(true);
			saveConfig();
		}
                saveConfig();
                spawnList.clear();
                spawnList.addAll(getConfig().getStringList("spawn.location"));
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
				if (Permissions.hasPermission((Player) sender, Permissions.SETSPAWN)) {
					reloadConfig();
					sender.sendMessage(ChatColor.GRAY + "Spawn configuration reloaded");
					return true;
				}
			}
			((Player) sender).teleport(getSpawnLocation());
			return true;
		}

		if (command.getName().equalsIgnoreCase("setspawn")) {
			setSpawn((Player) sender);
			return true;
		}

		return false;
	}

	private void setSpawn(Player player)
	{
		Location loc = player.getLocation();
		if (!loc.getWorld().equals(getServer().getWorlds().get(0))) {
			player.sendMessage(ChatColor.GRAY + "Spawn can only be set in main world.");
			return;
		}
                
                if( getConfig().getBoolean("many-spawn") != true) {
                    spawnList.clear();
                } 
                spawnList.add(locationToString(loc));
		getConfig().set("spawn.location", spawnList);
                saveConfig();

		getServer().getWorlds().get(0).setSpawnLocation(
				loc.getBlockX(),
				loc.getBlockY(),
				loc.getBlockZ());

		player.sendMessage(ChatColor.GRAY + "Spawn set at " +
				loc.getBlockX() + ", " +
				loc.getBlockY() + ", " +
				loc.getBlockZ());
	}
}
