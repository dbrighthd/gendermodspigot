package dbrighthd.wildfiregendermodplugin.networking;

import dbrighthd.wildfiregendermodplugin.GenderModPlugin;
import dbrighthd.wildfiregendermodplugin.logging.CustomPluginLogger;
import dbrighthd.wildfiregendermodplugin.networking.minecraft.CraftOutputStream;
import dbrighthd.wildfiregendermodplugin.networking.wildfire.ModSyncPacket;
import dbrighthd.wildfiregendermodplugin.networking.wildfire.ModSyncPacketV2;
import dbrighthd.wildfiregendermodplugin.networking.wildfire.ModSyncPacketV3;
import dbrighthd.wildfiregendermodplugin.wildfire.ModUser;
import dbrighthd.wildfiregendermodplugin.wildfire.UserManager;
import dbrighthd.wildfiregendermodplugin.wildfire.setup.BreastOptions;
import dbrighthd.wildfiregendermodplugin.wildfire.setup.GeneralOptions;
import dbrighthd.wildfiregendermodplugin.wildfire.setup.GenderIdentities;
import dbrighthd.wildfiregendermodplugin.wildfire.setup.ModConfiguration;
import dbrighthd.wildfiregendermodplugin.wildfire.setup.PhysicsOptions;
import dbrighthd.wildfiregendermodplugin.wildfire.setup.UVLayouts;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Instance-level NetworkManager coverage that needs a Player/GenderModPlugin.
 * Uses Mockito rather than a full Bukkit server mock (MockBukkit) since only
 * a handful of methods on each are actually touched by NetworkManager.
 */
public class NetworkManagerTest {
    private GenderModPlugin plugin;
    private CustomPluginLogger logger;
    private UserManager userManager;
    private NetworkManager networkManager;

    @BeforeEach
    public void setUp() {
        plugin = mock(GenderModPlugin.class);
        logger = mock(CustomPluginLogger.class);
        userManager = new UserManager();
        FileConfiguration config = mock(FileConfiguration.class);

        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getCustomLogger()).thenReturn(logger);
        when(plugin.getUserManager()).thenReturn(userManager);
        // Force a known default protocol (5) rather than exercising auto-detection here.
        when(config.getInt("mod.protocol", -1)).thenReturn(5);

        networkManager = new NetworkManager(plugin);
        assertTrue(networkManager.init(), "init() should succeed for a valid configured protocol");
    }

    @Test
    public void testGetPacketFormatForPlayerUsesDefaultWhenUnknown() {
        UUID uuid = UUID.randomUUID();
        ModSyncPacket format = networkManager.getPacketFormatForPlayer(uuid);
        assertEquals(5, format.getVersion());
    }

    @Test
    public void testGetPacketFormatForPlayerUsesTrackedVersion() {
        UUID uuid = UUID.randomUUID();
        userManager.setProtocolVersion(uuid, 3);

        ModSyncPacket format = networkManager.getPacketFormatForPlayer(uuid);
        assertEquals(3, format.getVersion());
    }

    @Test
    public void testGetPacketFormatForPlayerFallsBackOnUnsupportedTrackedVersion() {
        UUID uuid = UUID.randomUUID();
        userManager.setProtocolVersion(uuid, 42);

        ModSyncPacket format = networkManager.getPacketFormatForPlayer(uuid);
        assertEquals(5, format.getVersion(), "Unsupported tracked protocol must fall back to the default");
        verify(logger).warning(anyString(), eq(42), eq(uuid), eq(5));
    }

    /**
     * A valid-length payload whose embedded UUID matches the sender should have
     * its protocol auto-detected and committed for future lookups. Protocol 3 is
     * used here since a real ModSyncPacketV3 write() output is exactly the 49
     * bytes NetworkManager.detectProtocolFromLength maps to protocol 3.
     */
    @Test
    public void testDeserializeUserAutoDetectsAndCommitsProtocolOnUuidMatch() throws IOException {
        UUID senderId = UUID.randomUUID();
        Player sender = mock(Player.class);
        when(sender.getUniqueId()).thenReturn(senderId);
        when(sender.getName()).thenReturn("Tester");

        byte[] v3Data = buildV3Packet(senderId);
        assertEquals(49, v3Data.length, "sanity check: must match detectProtocolFromLength's V3 case");
        assertEquals(-1, userManager.getProtocolVersion(senderId));

        ModUser user = networkManager.deserializeUser(v3Data, false, sender);

        assertNotNull(user);
        assertEquals(senderId, user.userId());
        assertEquals(3, userManager.getProtocolVersion(senderId),
                "A validated payload (UUID matches sender) must commit the detected protocol");
    }

    /**
     * Regression: a length-plausible payload whose embedded UUID does NOT match
     * the sender must never have its protocol silently trusted/committed, even
     * if a lower-protocol fallback parse of the raw bytes happens to succeed.
     */
    @Test
    public void testDeserializeUserDoesNotCommitProtocolOnUuidMismatch() throws IOException {
        UUID senderId = UUID.randomUUID();
        UUID otherPlayerId = UUID.randomUUID();
        Player sender = mock(Player.class);
        when(sender.getUniqueId()).thenReturn(senderId);
        when(sender.getName()).thenReturn("Tester");

        byte[] v3Data = buildV3Packet(otherPlayerId);

        networkManager.deserializeUser(v3Data, false, sender);

        assertEquals(-1, userManager.getProtocolVersion(senderId),
                "Sender's protocol must not be confirmed from a payload whose UUID doesn't match them");
    }

    @Test
    public void testDeserializeUserDetectsV2UsingSerializerLength() throws IOException {
        UUID senderId = UUID.randomUUID();
        Player sender = mock(Player.class);
        when(sender.getUniqueId()).thenReturn(senderId);
        when(sender.getName()).thenReturn("Tester");

        byte[] v2Data = buildPacket(senderId, new ModSyncPacketV2());
        assertEquals(50, v2Data.length);

        ModUser user = networkManager.deserializeUser(v2Data, false, sender);

        assertNotNull(user);
        assertEquals(2, userManager.getProtocolVersion(senderId));
    }

    @Test
    public void testDetectionDoesNotCommitMalformedPayloadWithMatchingUuid() throws IOException {
        UUID senderId = UUID.randomUUID();
        Player sender = mock(Player.class);
        when(sender.getUniqueId()).thenReturn(senderId);
        when(sender.getName()).thenReturn("Tester");

        byte[] malformed = buildV3Packet(senderId);
        malformed[16] = 99; // Invalid GenderIdentities ordinal.

        assertNull(networkManager.deserializeUser(malformed, false, sender));
        assertEquals(-1, userManager.getProtocolVersion(senderId));
    }

    @Test
    public void testSuccessfulFallbackUpdatesTrackedProtocol() throws IOException {
        UUID senderId = UUID.randomUUID();
        Player sender = mock(Player.class);
        when(sender.getUniqueId()).thenReturn(senderId);
        when(sender.getName()).thenReturn("Tester");
        userManager.setProtocolVersion(senderId, 5);

        ModUser user = networkManager.deserializeUser(buildV3Packet(senderId), false, sender);

        assertNotNull(user);
        assertEquals(3, userManager.getProtocolVersion(senderId),
                "A successful authenticated fallback must be used for future outbound packets");
    }

    private byte[] buildV3Packet(UUID userId) throws IOException {
        return buildPacket(userId, new ModSyncPacketV3());
    }

    private byte[] buildPacket(UUID userId, ModSyncPacket packet) throws IOException {
        ModConfiguration config = new ModConfiguration(
                new GeneralOptions(GenderIdentities.FEMALE, true, 1.0f, true),
                new PhysicsOptions(true, false, 0.333f, 0.75f),
                new BreastOptions(0.6f, 0.0f, 0.0f, 0.0f, true, 0.0f),
                UVLayouts.defaultLayouts());
        ModUser user = new ModUser(userId, config);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (CraftOutputStream out = new CraftOutputStream(bytes)) {
            packet.write(user, out);
        }
        return bytes.toByteArray();
    }
}
