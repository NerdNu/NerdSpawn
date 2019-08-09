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

        // Player.get/setBedSpawnLocation() methods round to block coords,
        // spawning the player on the edge of the block, which warps them on top
        // of walls. Interacts particularly badly with EasySigns. Put them in
        // the middle of the block.
        Location bed = player.getBedSpawnLocation();
        if (bed != null) {
            bed.add(0.5, 0.1, 0.5);
        }

        event.setRespawnLocation(plugin.getSpawnLocation(player, deathLocations.get(player.getName()), bed));

        // Remove all potion effects. CombatTag lets players keep them when
        // their NPC double dies.
        for (PotionEffect effect : player.getPlayer().getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    /**
     * When returning from the end to the overworld, if bed spawns are disabled,
     * or the player has no bed set, put them at the world spawn.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        // plugin.getLogger().info("onPlayerPortal(): " + event.getCause() +
        // " from " + formatBlockLoc(event.getFrom()) +
        // " to " + formatBlockLoc(event.getTo()));

        if (event.getFrom().getWorld().getEnvironment() == Environment.THE_END &&
            (!plugin.getConfig().getBoolean("allow-bed-spawn") || event.getPlayer().getBedSpawnLocation() == null)) {
            event.setTo(plugin.getConfig().getBoolean("use-primary-spawn") ? plugin.getPrimarySpawn() : plugin.getRespawnLocation());
        }
    }

    // @EventHandler()
    // public void onPlayerTeleport(PlayerTeleportEvent event) {
    // plugin.getLogger().info("onPlayerTeleport(): " + event.getCause() +
    // " from " + formatBlockLoc(event.getFrom()) +
    // " to " + formatBlockLoc(event.getTo()));
    // }

    protected String formatBlockLoc(Location loc) {
        return "(" + loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + ")";
    }
}
