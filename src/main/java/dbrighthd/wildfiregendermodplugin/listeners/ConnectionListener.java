package dbrighthd.wildfiregendermodplugin.listeners;

import dbrighthd.wildfiregendermodplugin.GenderModPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Handles player join and quit events.
 *
 * @author winnpixie
 */
public class ConnectionListener implements Listener {
    private final GenderModPlugin plugin;

    public ConnectionListener(GenderModPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // The initial sync to this player is deferred: V5 clients receive it
        // from HelloPacketListener once the handshake is confirmed; V4 clients
        // receive it from ModPayloadListener once their payload is parsed.
        // Calling sync() here is always a no-op (player not ready yet) and
        // produces a misleading "Syncing X" log entry, so we skip it.
        plugin.getCustomLogger().debug("Player %s joined, awaiting handshake/payload", player.getName());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        plugin.getCustomLogger().debug("Removing %s", player.getName());

        // Remove configuration for a player who is no longer online.
        plugin.getUserManager().removePlayer(uuid);
    }
}
