package nu.nerd.NerdSpawn;

import java.io.File;
import java.util.ArrayList;
import net.minecraft.server.EntityPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerPreLoginEvent;
import org.bukkit.event.player.PlayerRespawnEvent;


public class NerdSpawnPlayerListener extends PlayerListener
{
    private final NerdSpawn plugin;
    private ArrayList<String> firstLogin = new ArrayList<String> ();

    public NerdSpawnPlayerListener(NerdSpawn instance)
    {
        plugin = instance;
    }

    @Override
    public void onPlayerPreLogin(PlayerPreLoginEvent event) {
        File data = new File(plugin.worldName + File.separator + "players" +
                File.separator + event.getName() + ".dat");
        if (!data.exists()) {
            firstLogin.add(event.getName());
        }
    }

    @Override
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        if (firstLogin.contains(event.getPlayer().getName())) {
            firstLogin.remove(event.getPlayer().getName());
            event.getPlayer().teleport(plugin.getSpawnLocation());

            if (plugin.getConfig().getBoolean("show-join-message")) {
                String message = plugin.getConfig().getString("join-message");
                message = message.replaceAll("%u", event.getPlayer().getName());
                for (ChatColor c : ChatColor.values())
                    message = message.replaceAll("&" + Integer.toHexString(c.getCode()), c.toString());

                Bukkit.getServer().broadcastMessage(message);
            }
        }
        else {
            if (plugin.getConfig().getBoolean("stop-login-teleport")) {
                //we only want to stop certain teleportation cases
                EntityPlayer entity = ((CraftPlayer)event.getPlayer()).getHandle();
                World world = Bukkit.getWorld(plugin.worldName);
                Location loc = new Location(world,
                        entity.lastX,
                        entity.lastY,
                        entity.lastZ,
                        entity.lastYaw,
                        entity.lastPitch);
                Location loc2 = new Location(world,
                        entity.lastX,
                        entity.lastY + 1,
                        entity.lastZ,
                        entity.lastYaw,
                        entity.lastPitch);

                Material feet = world.getBlockAt(loc).getType();
                Material head = world.getBlockAt(loc2).getType();
                boolean teleport = false;

                if (head == Material.TRAP_DOOR || feet == Material.TRAP_DOOR)
                    teleport = true;
                if (head == Material.SAND || feet == Material.SAND)
                    teleport = true;
                if (head == Material.GRAVEL || feet == Material.GRAVEL)
                    teleport = true;
                if (head == Material.DETECTOR_RAIL || feet == Material.DETECTOR_RAIL)
                    teleport = true;
                if (head == Material.POWERED_RAIL || feet == Material.POWERED_RAIL)
                    teleport = true;
                if (head == Material.RAILS || feet == Material.RAILS)
                    teleport = true;

                if (teleport)
                    event.getPlayer().teleport(loc);
            }
        }
    }
        
    @Override
    public void onPlayerRespawn(PlayerRespawnEvent event)
    {
        if (!event.isBedSpawn() || !plugin.getConfig().getBoolean("allow-bed-spawn"))
            event.setRespawnLocation(plugin.getSpawnLocation());
    }
}
