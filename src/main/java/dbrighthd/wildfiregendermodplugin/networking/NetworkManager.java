package dbrighthd.wildfiregendermodplugin.networking;

import dbrighthd.wildfiregendermodplugin.GenderModPlugin;
import dbrighthd.wildfiregendermodplugin.networking.minecraft.CraftInputStream;
import dbrighthd.wildfiregendermodplugin.networking.minecraft.CraftOutputStream;
import dbrighthd.wildfiregendermodplugin.networking.wildfire.*;
import dbrighthd.wildfiregendermodplugin.wildfire.ModConstants;
import dbrighthd.wildfiregendermodplugin.wildfire.ModUser;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author winnpixie
 */
public class NetworkManager {
    private static final Map<Integer, ModSyncPacket> PACKET_FORMATS;

    /**
     * The lowest protocol version with a working implementation.
     * Protocol 1 (ModSyncPacketV1) is a stub that throws
     * {@link UnsupportedOperationException} from read/write and must never
     * be used for deserialization attempts.
     */
    private static final int MIN_IMPLEMENTED_PROTOCOL = 2;

    private final GenderModPlugin plugin;

    private ModSyncPacket packetFormat;

    static {
        PACKET_FORMATS = Map.of(
                1, new ModSyncPacketV1(),
                2, new ModSyncPacketV2(),
                3, new ModSyncPacketV3(),
                4, new ModSyncPacketV4(),
                5, new ModSyncPacketV5());
    }

    public NetworkManager(GenderModPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean init() {
        int protocolVersion = plugin.getConfig().getInt("mod.protocol", -1);
        if (protocolVersion == -1) {
            protocolVersion = detectDefaultProtocol();
        }

        if (!isImplementedProtocol(protocolVersion))
            return false;

        packetFormat = PACKET_FORMATS.get(protocolVersion);

        plugin.getCustomLogger().info("Using default protocol %d for mod version(s) %s",
                packetFormat.getVersion(), packetFormat.getModRange());

        return true;
    }

    /**
     * Whether {@code version} has a usable (non-stub) packet format registered.
     * Protocol 1 is intentionally excluded even though it has an entry in
     * {@link #PACKET_FORMATS}, since {@link ModSyncPacketV1} is a stub that
     * throws {@link UnsupportedOperationException} from read/write.
     */
    static boolean isImplementedProtocol(int version) {
        return version >= MIN_IMPLEMENTED_PROTOCOL && PACKET_FORMATS.containsKey(version);
    }

    private int detectDefaultProtocol() {
        return detectDefaultProtocol(plugin.getServer().getBukkitVersion());
    }

    static int detectDefaultProtocol(String version) {
        try {
            String versionStr = version.split("-")[0];
            String[] parts = versionStr.split("\\.");
            int epoch = Integer.parseInt(parts[0]);

            if (epoch == 1) {
                // Legacy "1.MINOR.PATCH" scheme (up through 1.21.x).
                int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

                if (minor > 21 || (minor == 21 && patch >= 9))
                    return 5;
                if (minor == 21 && patch >= 2)
                    return 4;
                if (minor == 20 && patch >= 2)
                    return 3;
                if (minor >= 18)
                    return 2;
                return 2;
            }

            // New "YY.RELEASE[.PATCH]" scheme introduced with Minecraft 26.1 (2026+).
            // The sync packet format hasn't changed since protocol 5 was introduced,
            // so every version under this scheme uses protocol 5.
            if (epoch >= 26)
                return 5;
        } catch (Exception ignored) {
        }
        return 2;
    }

    static int detectProtocolFromLength(int length) {
        // Protocols 2-4 have fixed sizes. Protocol 5 shares V4's 53-byte
        // header and appends a variable-length UV-layout section.
        if (length == 50)
            return 2;
        if (length == 49)
            return 3;
        if (length == 53)
            return 4;
        if (length > 53)
            return 5;
        return -1;
    }

    public void sync(Collection<? extends Player> audience) {
        for (ModUser userToSync : plugin.getUserManager().getUsers().values()) {
            Map<Integer, byte[]> fabricCache = new HashMap<>();
            Map<Integer, byte[]> forgeCache = new HashMap<>();

            for (Player recipient : audience) {
                // Skip recipients whose protocol has not been confirmed yet.
                // Sending before confirmation risks a format mismatch that
                // disconnects the client (e.g. V4 client receives a V5 packet).
                if (!plugin.getUserManager().isProtocolReady(recipient.getUniqueId()))
                    continue;

                ModSyncPacket format = getPacketFormatForPlayer(recipient.getUniqueId());
                int version = format.getVersion();

                byte[] fabricData = fabricCache.computeIfAbsent(version, v -> serializeUser(userToSync, false, format));
                byte[] forgeData = forgeCache.computeIfAbsent(version, v -> serializeUser(userToSync, true, format));

                if (fabricData != null && fabricData.length > 0)
                    sendData(recipient, ModConstants.SYNC, fabricData);
                if (forgeData != null && forgeData.length > 0)
                    sendData(recipient, ModConstants.FORGE, forgeData);
            }
        }
    }

    public ModSyncPacket getPacketFormatForPlayer(UUID playerId) {
        int version = plugin.getUserManager().getProtocolVersion(playerId);
        if (version == -1) {
            return packetFormat; // Default from config
        }

        ModSyncPacket format = PACKET_FORMATS.get(version);
        if (!isImplementedProtocol(version) || format == null) {
            plugin.getCustomLogger().warning("Unsupported protocol version %d for %s, falling back to version %d",
                    version, playerId, packetFormat.getVersion());
            return packetFormat;
        }

        return format;
    }

    public ModUser deserializeUser(byte[] data, boolean forge, Player sender) {
        if (plugin.getCustomLogger().isVerbose()) {
            plugin.getCustomLogger().debug("Incoming payload [%s] -> %s",
                    forge ? ModConstants.FORGE : ModConstants.SEND_GENDER_INFO,
                    plugin.getCustomLogger().hexDump(data));
        }

        UUID senderId = sender.getUniqueId();
        ModSyncPacket format = getPacketFormatForPlayer(senderId);

        // Dynamic detection if version is unknown
        if (plugin.getUserManager().getProtocolVersion(senderId) == -1) {
            int len = data.length;
            if (forge)
                len--; // Subtract forge prefix byte

            int detectedVersion = detectProtocolFromLength(len);

            if (detectedVersion != -1) {
                ModSyncPacket candidateFormat = PACKET_FORMATS.get(detectedVersion);

                // Only commit a detected protocol after the whole payload has
                // decoded successfully and its UUID has been authenticated.
                try {
                    ModUser detectedUser = readUser(data, forge, candidateFormat);
                    if (!detectedUser.userId().equals(senderId)) {
                        plugin.getCustomLogger().warning(
                                "Protocol detection mismatch for %s: parsed UUID %s doesn't match sender, using default",
                                sender.getName(), detectedUser.userId());
                    } else {
                        plugin.getUserManager().setProtocolVersion(senderId, detectedVersion);
                        plugin.getCustomLogger().info("Auto-detected protocol V%d for %s (packet length: %d)",
                                detectedVersion, sender.getName(), len);
                        return detectedUser;
                    }
                } catch (IOException ex) {
                    plugin.getCustomLogger().warning("Protocol detection failed for %s, using default",
                            sender.getName());
                }
            }
        }

        try {
            ModUser user = readUser(data, forge, format);
            if (user != null) {
                plugin.getCustomLogger().debug("Successfully deserialized user %s (forge=%s, protocol=%d)",
                        user.userId(), forge, format.getVersion());
            }
            return user;
        } catch (IOException ex) {
            plugin.getCustomLogger().debug("Data malformed during deserialization (forge=%s, protocol=%d)",
                    forge, format.getVersion());

            // Attempt fallback to lower protocol versions before giving up.
            ModUser fallbackUser = tryFallbackProtocols(data, forge, format.getVersion(), sender);
            if (fallbackUser != null) {
                return fallbackUser;
            }

            plugin.getCustomLogger().warning(ex, "Could not deserialize user (forge=%s, protocol=%d)",
                    forge, format.getVersion());
        }

        return null;
    }

    /**
     * Attempts to deserialize {@code data} using each protocol version below
     * {@code currentVersion}, from {@code currentVersion - 1} down to
     * {@link #MIN_IMPLEMENTED_PROTOCOL} (protocol 1 is a stub that throws
     * {@link UnsupportedOperationException} and must be skipped).
     * <p>
     * If a lower version succeeds it logs an actionable WARNING telling the
     * admin which {@code protocol} value to set in the plugin config, then
     * returns the successfully deserialized user. Returns {@code null} if no
     * lower version succeeds.
     */
    private ModUser tryFallbackProtocols(byte[] data, boolean forge, int currentVersion, Player sender) {
        for (int candidate = currentVersion - 1; candidate >= MIN_IMPLEMENTED_PROTOCOL; candidate--) {
            ModSyncPacket fallbackFormat = PACKET_FORMATS.get(candidate);
            if (fallbackFormat == null) continue;

            try (CraftInputStream input = CraftInputStream.ofBytes(data)) {
                if (forge)
                    input.readByte();

                ModUser user = fallbackFormat.read(input);
                if (input.available() != 0)
                    throw new IOException("Trailing data after protocol " + candidate + " payload");
                if (user != null) {
                    if (user.userId().equals(sender.getUniqueId())) {
                        plugin.getUserManager().setProtocolVersion(sender.getUniqueId(), candidate);
                    }
                    plugin.getCustomLogger().warning(
                            "Protocol mismatch detected for player %s: server expected protocol %d but the packet "
                            + "was successfully parsed as protocol %d (mod version range: %s). "
                            + "Set 'protocol: %d' in the plugin config to match this player's client mod version, "
                            + "or ask the player to update to mod version 5.0.0+ for protocol %d.",
                            sender.getName(),
                            currentVersion,
                            candidate,
                            fallbackFormat.getModRange(),
                            candidate,
                            currentVersion);
                    plugin.getCustomLogger().debug(
                            "Fallback deserialization succeeded for %s (forge=%s, protocol=%d)",
                            user.userId(), forge, candidate);
                    return user;
                }
            } catch (IOException ex) {
                plugin.getCustomLogger().debug("Fallback candidate protocol %d failed for %s: %s",
                        candidate, sender.getName(), ex.getMessage());
            }
        }
        return null;
    }

    private ModUser readUser(byte[] data, boolean forge, ModSyncPacket format) throws IOException {
        try (CraftInputStream input = CraftInputStream.ofBytes(data)) {
            if (forge)
                input.readByte();

            ModUser user = format.read(input);
            if (input.available() != 0)
                throw new IOException("Trailing data after protocol " + format.getVersion() + " payload");
            return user;
        }
    }

    private byte[] serializeUser(ModUser user, boolean forge, ModSyncPacket format) {
        try (ByteArrayOutputStream payload = new ByteArrayOutputStream();
                CraftOutputStream output = new CraftOutputStream(payload)) {
            if (forge)
                output.writeByte(1);

            format.write(user, output);
            return payload.toByteArray();
        } catch (IOException ex) {
            plugin.getCustomLogger().warning(ex, "Could not serialize user (forge=%s, protocol=%d)",
                    forge, format.getVersion());
        }

        return new byte[0];
    }

    private void sendData(Player target, String channel, byte[] data) {
        if (plugin.getCustomLogger().isVerbose()) {
            plugin.getCustomLogger().debug("Outgoing payload [%s] -> %s",
                    channel, plugin.getCustomLogger().hexDump(data));
        }
        try {
            target.sendPluginMessage(plugin, channel, data);
        } catch (Exception ex) {
            // Guard against IllegalArgumentException (unregistered channel) or
            // other runtime errors so one bad recipient cannot abort the whole
            // sync loop and leave other players without their update.
            plugin.getCustomLogger().warning(ex, "Could not send plugin message to %s on channel %s",
                    target.getName(), channel);
        }
    }

    /**
     * Sends a dummy sync packet for a fake UUID to a player.
     * Useful for verifying that the client mod is receiving and processing data.
     */
    public void sendTestData(Player target, UUID dummyId) {
        var configOpt = plugin.getUserManager().getUsers().values().stream()
                .findFirst().map(ModUser::configuration);

        if (configOpt.isEmpty()) {
            plugin.getCustomLogger().warning("No mod users stored to use as template for test sync.");
            return;
        }

        ModSyncPacket format = getPacketFormatForPlayer(target.getUniqueId());
        ModUser dummyUser = new ModUser(dummyId, configOpt.get());

        byte[] data = serializeUser(dummyUser, false, format);
        if (data.length > 0) {
            sendData(target, ModConstants.SYNC, data);
            plugin.getCustomLogger().info("Sent dummy sync for %s to %s using protocol %d",
                    dummyId, target.getName(), format.getVersion());
        }
    }
}
