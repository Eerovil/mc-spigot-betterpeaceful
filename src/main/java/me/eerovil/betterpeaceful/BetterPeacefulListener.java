package me.eerovil.betterpeaceful;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class BetterPeacefulListener implements Listener {

    private final BetterPeaceful plugin;

    public BetterPeacefulListener(BetterPeaceful plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
    }
}
