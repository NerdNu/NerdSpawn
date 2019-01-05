package nu.nerd.nerdspawn;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;

public class NerdSpawnListener implements Listener {
    private final NerdSpawn plugin;

    /**
     * Map from player name to most recent death location. Does not survive
     * restarts, sorry.
     */
    private final HashMap<String, Location> deathLocations = new HashMap<String, Location>();

    public NerdSpawnListener(NerdSpawn instance) {
        plugin = instance;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // NOTE: hasPlayedBefore() will return false until the first logout.
        if (!player.hasPlayedBefore()) {
            // Teleport the player to first spawn after Multiverse has done it's
            // spawn thing (2 ticks).
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                player.teleport(plugin.getFirstJoinSpawnLocation());
            }, 2);

            if (plugin.getConfig().getBoolean("show-join-message")) {
                String message = ChatColor.translateAlternateColorCodes('&',
                                                                        plugin.getConfig().getString("join-message")
                                                                        .replaceAll("%u", event.getPlayer().getName()));
                plugin.getServer().broadcastMessage(message);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        deathLocations.put(event.getEntity().getName(), event.getEntity().getLocation());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (deathLocations.containsKey(player.getName())) {
            event.setRespawnLocation(plugin.getSpawnLocation(player, deathLocations.get(player.getName()), player.getBedSpawnLocation()));
        } else {
            event.setRespawnLocation(plugin.getSpawnLocation(player.getBedSpawnLocation()));
        }

        // Remove all potion effects. CombatTag lets players keep them when
        // their NPC double dies.
        for (PotionEffect effect : player.getPlayer().getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getFrom().getWorld().getEnvironment() == Environment.THE_END &&
            (!plugin.getConfig().getBoolean("allow-bed-spawn") || event.getPlayer().getBedSpawnLocation() == null)) {
            event.setTo(plugin.getConfig().getBoolean("use-primary-spawn") ? plugin.getPrimarySpawn() : plugin.getSpawnLocation());
        }
    }
}
