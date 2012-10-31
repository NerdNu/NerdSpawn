package nu.nerd.nerdspawn;

import org.bukkit.ChatColor;
import org.bukkit.World.Environment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;


public class NerdSpawnListener implements Listener
{
    private final NerdSpawn plugin;

    public NerdSpawnListener(NerdSpawn instance)
    {
        plugin = instance;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        if (!event.getPlayer().hasPlayedBefore()) {
            event.getPlayer().teleport(plugin.getSpawnLocation());
            if (plugin.getConfig().getBoolean("show-join-message")) {
                String message = plugin.getConfig().getString("join-message");
                message = message.replaceAll("%u", event.getPlayer().getName());
                for (ChatColor c : ChatColor.values())
                    message = message.replaceAll(("&" + c.getChar()), c.toString());

                plugin.getServer().broadcastMessage(message);
            }
        }
    }
        
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event)
    {
        if (!event.isBedSpawn() || !plugin.getConfig().getBoolean("allow-bed-spawn"))
            event.setRespawnLocation(plugin.getSpawnLocation());
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if(event.getFrom().getWorld().getEnvironment() == Environment.THE_END && !plugin.getConfig().getBoolean("allow-bed-spawn")) {
            event.setTo(plugin.getSpawnLocation());
        }
    }
}
