package com.demigodsrpg.area;

import com.demigodsrpg.DGData;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AreaSelection implements Listener {

    // -- GLOBAL CACHE -- //

    public static final ConcurrentMap<String, AreaSelection> AREA_SELECTION_CACHE = new ConcurrentHashMap<>();

    // -- META DATA -- //

    private final String playerUUID;
    private final List<Location> points;

    public AreaSelection(Player player) {
        this.playerUUID = player.getUniqueId().toString();
        points = new ArrayList<>();
    }

    @SuppressWarnings("ConstantConditions") // Locations should always be loaded when clicking
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(PlayerInteractEvent event) {
        if (event.getPlayer().getUniqueId().toString().equals(playerUUID) &&
                Action.RIGHT_CLICK_BLOCK.equals(event.getAction())) {
            // Get the point
            Location point = event.getClickedBlock().getLocation();

            if (!points.isEmpty() && !points.get(0).getWorld().equals(point.getWorld())) {
                event.getPlayer().sendMessage(ChatColor.RED + "Points must all be from the same world.");
                return;
            }

            // Cancel the click to be sure
            event.setCancelled(true);

            // Either add or remove the point
            if (!points.contains(point)) {
                points.add(point);
                event.getPlayer().sendMessage(ChatColor.YELLOW + "Point " + points.size() + " has been marked.");
            } else {
                int index = points.indexOf(point) + 1;
                points.remove(point);
                event.getPlayer().sendMessage(ChatColor.RED + "Point " + index + " has been unmarked.");
            }
        }
    }

    public List<Location> getPoints() {
        return points;
    }

    public void register() {
        // Register this listener
        Bukkit.getServer().getPluginManager().registerEvents(this, DGData.PLUGIN);
    }

    public void unregister() {
        // Unregister this listener
        HandlerList.unregisterAll(this);
    }
}
