package nu.nerd.NerdSpawn;

import java.io.File;
import java.util.ArrayList;
import net.minecraft.server.EntityPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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
                EntityPlayer entity = ((CraftPlayer)event.getPlayer()).getHandle();
                Location loc = new Location(Bukkit.getWorld(plugin.worldName),
                        entity.lastX,
                        entity.lastY,
                        entity.lastZ,
                        entity.lastYaw,
                        entity.lastPitch);
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
