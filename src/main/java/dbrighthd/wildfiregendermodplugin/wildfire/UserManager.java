package dbrighthd.wildfiregendermodplugin.wildfire;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager {
    private final Map<UUID, ModUser> users = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> protocolVersions = new ConcurrentHashMap<>();

    /**
     * Players whose protocol version has been confirmed via either a Hello
     * handshake or a successfully-parsed payload. Only ready players are
     * sent sync packets — this prevents sending a V5-format packet to a
     * V4 client before the server knows what format they speak.
     */
    private final Set<UUID> readyPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * A map to link players (by {@link UUID}) to their {@link ModUser}.
     *
     * @return The underlying map.
     */
    public Map<UUID, ModUser> getUsers() {
        return users;
    }

    /**
     * Store the identified protocol version for a player.
     *
     * @param uuid    The player's UUID.
     * @param version The protocol version.
     */
    public void setProtocolVersion(UUID uuid, int version) {
        protocolVersions.put(uuid, version);
    }

    /**
     * Get the identified protocol version for a player.
     *
     * @param uuid The player's UUID.
     * @return The protocol version, or -1 if not identified.
     */
    public int getProtocolVersion(UUID uuid) {
        return protocolVersions.getOrDefault(uuid, -1);
    }

    /**
     * Mark a player as ready to receive sync packets. Called after a
     * successful Hello handshake (V5+) or a successful payload parse (all
     * versions). Until this is called the player is skipped by sync().
     *
     * @param uuid The player's UUID.
     */
    public void setProtocolReady(UUID uuid) {
        readyPlayers.add(uuid);
    }

    /**
     * Returns {@code true} if the player's protocol has been confirmed and
     * they are safe to receive sync packets.
     *
     * @param uuid The player's UUID.
     */
    public boolean isProtocolReady(UUID uuid) {
        return readyPlayers.contains(uuid);
    }

    /**
     * Remove all stored data for a player.
     *
     * @param uuid The player's UUID.
     */
    public void removePlayer(UUID uuid) {
        users.remove(uuid);
        protocolVersions.remove(uuid);
        readyPlayers.remove(uuid);
    }
}
